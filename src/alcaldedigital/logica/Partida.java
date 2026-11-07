package alcaldedigital.logica;

import alcaldedigital.estructuras.ArbolAVL;
import alcaldedigital.estructuras.ArbolDecision;
import alcaldedigital.modelo.Accion;
import alcaldedigital.modelo.EstadoCiudad;
import alcaldedigital.modelo.Jugador;
import alcaldedigital.modelo.Publicacion;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Motor de la partida. Es el unico punto donde se conectan las dos estructuras
 * con las reglas del juego:
 *
 *   ArbolDecision -> decide QUE consecuencia tiene la accion.
 *   ArbolAVL      -> mantiene el feed ordenado por credibilidad y permite
 *                    buscar, consultar por rango y retirar publicaciones.
 *
 * La GUI no conoce ninguna estructura de datos: solo habla con esta clase.
 */
public class Partida {

    public static final int SEGUNDOS_POR_TURNO = 30;

    /**
     * Cuantas verificaciones puede gastar el jugador en toda la partida.
     *
     * POR QUE ES LIMITADO: si verificar fuera gratis, la estrategia optima seria
     * verificar todo y el jugador no aprenderia nada. En la vida real verificar
     * cuesta tiempo, y por eso nadie verifica todo lo que ve. La habilidad que
     * importa no es verificar: es saber QUE vale la pena verificar.
     */
    public static final int VERIFICACIONES_POR_PARTIDA = 3;

    /** La confianza arranca a la mitad: ni heroe ni sospechoso. */
    public static final int CONFIANZA_INICIAL = 50;

    /**
     * La confianza sube de a poco y baja de golpe.
     *
     * No es un numero caprichoso: asi funciona la reputacion de verdad. Hacer las
     * cosas bien diez veces construye poco; difundir una mentira una sola vez
     * destruye mucho. El jugador lo aprende sin que nadie se lo explique.
     */
    public static final int CONFIANZA_SUBE = 10;
    public static final int CONFIANZA_BAJA = 25;

    private final List<Jugador> jugadores;
    private final EstadoCiudad ciudad;
    private final ArbolAVL feed;
    private final ArbolDecision arbolDecision;
    private final BancoPublicaciones banco;
    private final Deque<Publicacion> pendientes;
    private final List<String> bitacora;

    private int turno;
    private int indiceJugador;
    private final int totalTurnos;

    /** Presupuesto de verificaciones que queda. */
    private int verificacionesRestantes;
    /** Confianza de la ciudad en el jugador, de 0 a 100. */
    private int confianza;
    /** Todo lo que decidio el jugador, para armar el informe final. */
    private final List<DecisionTomada> decisiones;

    /**
     * Inicializa los componentes de la partida, valida la cantidad de jugadores,
     * genera el feed y lo distribuye entre el Árbol AVL y la cola de turnos pendientes.
     */
    public Partida(List<Jugador> jugadores, int publicacionesPorJugador) {
        this(jugadores, publicacionesPorJugador, false);
    }

    /**
     * @param conPistas true para jugar con las 12 publicaciones que traen cuenta,
     *                  senales y explicacion; false para el feed antiguo.
     *
     * Con pistas, el numero de turnos lo decide el banco, no la cuenta de jugadores:
     * las publicaciones son fijas y no se repiten.
     */
    public Partida(List<Jugador> jugadores, int publicacionesPorJugador, boolean conPistas) {
        if (jugadores == null || jugadores.size() < 2 || jugadores.size() > 4) {
            throw new IllegalArgumentException("La partida admite de 2 a 4 jugadores.");
        }
        this.jugadores = jugadores;
        this.ciudad = new EstadoCiudad();
        this.feed = new ArbolAVL();
        this.arbolDecision = new ArbolDecision();
        this.banco = new BancoPublicaciones();
        this.pendientes = new ArrayDeque<>();
        this.bitacora = new ArrayList<>();
        this.turno = 0;
        this.indiceJugador = 0;
        this.verificacionesRestantes = VERIFICACIONES_POR_PARTIDA;
        this.confianza = CONFIANZA_INICIAL;
        this.decisiones = new ArrayList<>();

        int pedidas = jugadores.size() * publicacionesPorJugador;
        List<Publicacion> generadas = conPistas
                ? banco.generarFeedConPistas(pedidas)
                : banco.generarFeed(pedidas);

        // El total real puede ser menor que el pedido: el banco con pistas es finito.
        this.totalTurnos = generadas.size();

        for (Publicacion p : generadas) {
            feed.insertar(p);      // entra al AVL ordenada por credibilidad
            pendientes.addLast(p); // y a la cola de turnos
        }
        bitacora.add("Arranca la campana electoral en Ciudad Nova. "
                + feed.getTamano() + " publicaciones en el feed, altura del AVL: "
                + feed.getAlturaArbol() + ".");
    }

    // ------------------------------------------------------------------
    // Flujo de turnos
    // ------------------------------------------------------------------

    /** Verifica si quedan turnos pendientes en la partida. */
    public boolean hayTurnoPendiente() {
        return !pendientes.isEmpty();
    }

    /** Retorna la publicacion correspondiente al turno actual sin removerla. */
    public Publicacion publicacionActual() {
        return pendientes.peekFirst();
    }

    /** Retorna el jugador al que le corresponde tomar la decision en este turno. */
    public Jugador jugadorActual() {
        return jugadores.get(indiceJugador);
    }

    /** Retorna el numero del turno actual (base 1). */
    public int getTurno() {
        return turno + 1;
    }

    /** Retorna el total de turnos programados para la partida. */
    public int getTotalTurnos() {
        return totalTurnos;
    }

    /**
     * Resuelve el turno actual.
     *
     * Paso 1: se baja por el arbol de decision con (accion, tipo real) -> Efecto.
     * Paso 2: el efecto se escala por el rol y se aplica sobre la ciudad y el jugador.
     * Paso 3: si el efecto lo pide, la publicacion se ELIMINA del AVL (queda retirada).
     * Paso 4: pasa el turno y se dispara un evento aleatorio.
     */
    public ResultadoTurno ejecutar(Accion accion) {
        Publicacion p = pendientes.pollFirst();
        if (p == null) {
            throw new IllegalStateException("No hay publicacion pendiente.");
        }
        Jugador jugador = jugadorActual();
        boolean fueVerificacion = (accion == Accion.VERIFICAR);

        // Verificar cuesta: se descuenta del presupuesto de la partida.
        boolean gastoVerificacion = false;
        if (fueVerificacion && verificacionesRestantes > 0) {
            verificacionesRestantes--;
            gastoVerificacion = true;
        }

        Efecto efecto = arbolDecision.resolver(accion, p.getTipo());
        efecto.aplicarSobre(ciudad, jugador.getRol(), fueVerificacion);

        int puntos = efecto.puntosPara(jugador.getRol(), fueVerificacion);
        int reputacion = efecto.reputacionPara(jugador.getRol());
        jugador.sumarPuntos(puntos);
        jugador.sumarReputacion(reputacion);

        if (fueVerificacion) {
            p.marcarVerificada();
        }
        boolean retirada = false;
        if (efecto.debeRetirarPublicacion()) {
            retirada = feed.eliminar(p.claveAVL()); // eliminacion real en el AVL
            p.marcarRetirada();
        }

        int confianzaAntes = confianza;
        ajustarConfianza(accion, p);
        boolean acerto = fueAcierto(accion, p);
        decisiones.add(new DecisionTomada(p, accion, gastoVerificacion, acerto,
                                          confianza - confianzaAntes));

        String ruta = arbolDecision.rutaDe(accion, p.getTipo());
        bitacora.add("T" + getTurno() + " " + jugador.getNombre() + " (" + jugador.getRol().getEtiqueta()
                + ") -> " + accion.getEtiqueta() + " | " + efecto.getMensaje()
                + " [" + efecto.resumenDeltas() + "] " + (puntos >= 0 ? "+" : "") + puntos + " pts");

        if (gastoVerificacion) {
            bitacora.add("Verificacion gastada. Quedan " + verificacionesRestantes + " de "
                    + VERIFICACIONES_POR_PARTIDA + ".");
        }
        if (confianza != confianzaAntes) {
            bitacora.add("Confianza de la ciudad: " + confianzaAntes + " -> " + confianza
                    + " (" + (confianza > confianzaAntes ? "+" : "") + (confianza - confianzaAntes) + ")");
        }

        String evento = null;
        turno++;
        indiceJugador = (indiceJugador + 1) % jugadores.size();
        if (hayTurnoPendiente()) {
            evento = banco.eventoAleatorio(ciudad);
            bitacora.add(evento);
        }

        return new ResultadoTurno(p, accion, efecto, puntos, reputacion, ruta, retirada, evento);
    }

    // ------------------------------------------------------------------
    // Verificar cuesta
    // ------------------------------------------------------------------

    public int getVerificacionesRestantes() {
        return verificacionesRestantes;
    }

    /** true si todavia queda presupuesto para verificar. La GUI apaga el boton si es false. */
    public boolean puedeVerificar() {
        return verificacionesRestantes > 0;
    }

    // ------------------------------------------------------------------
    // Confianza: sube lento, baja rapido
    // ------------------------------------------------------------------

    public int getConfianza() {
        return confianza;
    }

    /**
     * Mueve la confianza segun lo que hizo el jugador.
     *
     * El unico movimiento grande es hacia abajo, y es por difundir una mentira.
     * Reportar algo cierto tambien baja, menos: censurar informacion verdadera
     * tambien hace dano, aunque la intencion sea buena.
     */
    private void ajustarConfianza(Accion accion, Publicacion p) {
        int delta = 0;

        switch (accion) {
            case COMPARTIR:
                delta = p.esFalsa() ? -CONFIANZA_BAJA : CONFIANZA_SUBE / 2;
                break;
            case VERIFICAR:
                delta = CONFIANZA_SUBE;
                break;
            case REPORTAR:
                delta = p.esFalsa() ? CONFIANZA_SUBE : -(CONFIANZA_BAJA / 2);
                break;
            case IGNORAR:
            default:
                delta = 0;   // no ayuda ni hace dano
                break;
        }

        confianza = Math.max(0, Math.min(100, confianza + delta));
    }

    /**
     * Si la decision fue la adecuada para esa publicacion.
     *
     * Hay exactamente dos errores: difundir una mentira y reportar algo cierto.
     * Ignorar nunca es un error grave, pero tampoco ayuda.
     */
    private boolean fueAcierto(Accion accion, Publicacion p) {
        if (accion == Accion.VERIFICAR) {
            return true;                       // detenerse a mirar siempre es prudente
        }
        if (p.esFalsa()) {
            return accion != Accion.COMPARTIR;
        }
        return accion != Accion.REPORTAR;
    }

    /** Todo lo que el jugador decidio, en orden. Es la materia prima del informe final. */
    public List<DecisionTomada> getDecisiones() {
        return java.util.Collections.unmodifiableList(decisiones);
    }

    /** Cuantas publicaciones falsas alcanzo a difundir el jugador. */
    public int falsasCompartidas() {
        int n = 0;
        for (DecisionTomada d : decisiones) {
            if (d.accion == Accion.COMPARTIR && d.publicacion.esFalsa()) {
                n++;
            }
        }
        return n;
    }

    /** Cuantas decisiones fueron las adecuadas. */
    public int totalAciertos() {
        int n = 0;
        for (DecisionTomada d : decisiones) {
            if (d.acerto) {
                n++;
            }
        }
        return n;
    }

    /** Una decision del jugador, guardada para poder explicarsela al final. */
    public static class DecisionTomada {
        public final Publicacion publicacion;
        public final Accion accion;
        /** Si gasto una verificacion antes de decidir. */
        public final boolean verifico;
        /** Si la decision fue la adecuada para esa publicacion. */
        public final boolean acerto;
        /** Cuanto movio la confianza esta decision. */
        public final int cambioConfianza;

        public DecisionTomada(Publicacion publicacion, Accion accion,
                              boolean verifico, boolean acerto, int cambioConfianza) {
            this.publicacion = publicacion;
            this.accion = accion;
            this.verifico = verifico;
            this.acerto = acerto;
            this.cambioConfianza = cambioConfianza;
        }
    }

    /** Se llama cuando al jugador se le acaba el tiempo: cuenta como IGNORAR con castigo. */
    public ResultadoTurno tiempoAgotado() {
        ResultadoTurno r = ejecutar(Accion.IGNORAR);
        ciudad.aplicar(0, 0, 0, -1, 2, 1);
        bitacora.add("Se acabo el tiempo: la publicacion siguio circulando sin que nadie actuara.");
        return r;
    }

    // ------------------------------------------------------------------
    // Cierre de la partida
    // ------------------------------------------------------------------

    /** Indica si la partida ha finalizado al vaciarse la cola de publicaciones. */
    public boolean terminada() {
        return pendientes.isEmpty();
    }

    /** El alcalde electo sale del jugador con mejor combinacion de puntos y reputacion. */
    public Jugador alcaldeElecto() {
        Jugador mejor = jugadores.get(0);
        for (Jugador j : jugadores) {
            if (valorElectoral(j) > valorElectoral(mejor)) {
                mejor = j;
            }
        }
        return mejor;
    }

    private int valorElectoral(Jugador j) {
        return j.getPuntos() + j.getReputacion() * 2;
    }

    /** Retorna el veredicto final de la ciudad en funcion de su salud global. */
    public String veredicto() {
        int salud = ciudad.saludGlobal();
        if (salud >= 75) {
            return "Ciudad Nova cerro las elecciones con una comunidad digital saludable. Lo lograron.";
        }
        if (salud >= 50) {
            return "Ciudad Nova sobrevivio la campana, pero quedaron heridas: aun circula desinformacion.";
        }
        return "Ciudad Nova termino dividida y desinformada. La proxima vez, verifiquen antes de compartir.";
    }

    // ------------------------------------------------------------------
    // Accesores para la GUI
    // ------------------------------------------------------------------

    public EstadoCiudad getCiudad() {
        return ciudad;
    }

    public ArbolAVL getFeed() {
        return feed;
    }

    public ArbolDecision getArbolDecision() {
        return arbolDecision;
    }

    public List<Jugador> getJugadores() {
        return jugadores;
    }

    public List<String> getBitacora() {
        return bitacora;
    }

    /**
     * Registra un evento externo en la bitacora de la partida (por ejemplo lo
     * que ocurre en el grafo social). Existe para que la interfaz no tenga que
     * escribir dentro de la lista interna.
     */
    public void registrarEnBitacora(String linea) {
        if (linea != null && !linea.isBlank()) {
            bitacora.add(linea);
        }
    }

    /** Habilidad del Periodista: lo mas sospechoso del feed, via consulta por rango en el AVL. */
    public List<Publicacion> publicacionesSospechosas() {
        return feed.menosCreiblesQue(35);
    }

    /** Resultado inmutable de un turno, para que la GUI lo muestre. */
    public static class ResultadoTurno {
        public final Publicacion publicacion;
        public final Accion accion;
        public final Efecto efecto;
        public final int puntos;
        public final int reputacion;
        public final String rutaArbol;
        public final boolean retirada;
        public final String evento;

        public ResultadoTurno(Publicacion publicacion, Accion accion, Efecto efecto, int puntos,
                            int reputacion, String rutaArbol, boolean retirada, String evento) {
            this.publicacion = publicacion;
            this.accion = accion;
            this.efecto = efecto;
            this.puntos = puntos;
            this.reputacion = reputacion;
            this.rutaArbol = rutaArbol;
            this.retirada = retirada;
            this.evento = evento;
        }
    }
}