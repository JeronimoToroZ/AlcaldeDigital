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

    public static final int SEGUNDOS_POR_TURNO = 10;

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

    public Partida(List<Jugador> jugadores, int publicacionesPorJugador) {
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
        this.totalTurnos = jugadores.size() * publicacionesPorJugador;

        for (Publicacion p : banco.generarFeed(totalTurnos)) {
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

    public boolean hayTurnoPendiente() {
        return !pendientes.isEmpty();
    }

    public Publicacion publicacionActual() {
        return pendientes.peekFirst();
    }

    public Jugador jugadorActual() {
        return jugadores.get(indiceJugador);
    }

    public int getTurno() {
        return turno + 1;
    }

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

        String ruta = arbolDecision.rutaDe(accion, p.getTipo());
        bitacora.add("T" + getTurno() + " " + jugador.getNombre() + " (" + jugador.getRol().getEtiqueta()
                + ") -> " + accion.getEtiqueta() + " | " + efecto.getMensaje()
                + " [" + efecto.resumenDeltas() + "] " + (puntos >= 0 ? "+" : "") + puntos + " pts");

        String evento = null;
        turno++;
        indiceJugador = (indiceJugador + 1) % jugadores.size();
        if (hayTurnoPendiente()) {
            evento = banco.eventoAleatorio(ciudad);
            bitacora.add(evento);
        }

        return new ResultadoTurno(p, accion, efecto, puntos, reputacion, ruta, retirada, evento);
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
