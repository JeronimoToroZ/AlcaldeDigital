package alcaldedigital.logica;

import alcaldedigital.estructuras.ArbolAVL;
import alcaldedigital.estructuras.ArbolDecision;
import alcaldedigital.estructuras.GrafoNiveles;
import alcaldedigital.estructuras.NodoNivel;
import alcaldedigital.modelo.Accion;
import alcaldedigital.modelo.EstadoCiudad;
import alcaldedigital.modelo.Jugador;
import alcaldedigital.modelo.Publicacion;
import alcaldedigital.modelo.TipoContenido;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Motor de la campaña (de 1 a 4 jugadores por turnos). Es el único punto donde se conectan
 * las estructuras de datos con las reglas del juego:
 *
 *   GrafoNiveles  -> qué niveles se pueden jugar (ganar uno desbloquea su subárbol).
 *   ArbolDecision -> qué burbuja es la correcta y qué consecuencia tiene cada una.
 *   ArbolAVL      -> el feed de Civitas ordenado por credibilidad; lo reportado se elimina.
 *   GrafoSocial   -> por dónde se propaga el rumor del nivel (BFS) según los errores.
 *
 * La interfaz gráfica no toca las estructuras directamente: solo habla con esta clase.
 */
public class Partida {

    private final List<Jugador> jugadores;
    private int indiceTurno;
    private final EstadoCiudad ciudad;
    private final ArbolAVL feed;
    private final ArbolDecision arbolDecision;
    private final BancoPublicaciones banco;
    private final GrafoNiveles niveles;
    private final GrafoSocial red;
    private final List<String> bitacora = new ArrayList<>();
    private final List<String> textosUsados = new ArrayList<>();
    private int siguienteId = 1;

    /** Partida de un solo jugador (útil para pruebas). */
    public Partida(Jugador jugador) {
        this(Collections.singletonList(jugador));
    }

    /**
     * Partida por turnos: todos los jugadores juegan cada nivel, uno tras otro,
     * sobre la MISMA ciudad (sus decisiones suman o restan para todos).
     * Los roles no se pueden repetir.
     */
    public Partida(List<Jugador> jugadores) {
        if (jugadores == null || jugadores.isEmpty() || jugadores.size() > 4) {
            throw new IllegalArgumentException("La partida admite de 1 a 4 jugadores.");
        }
        for (int i = 0; i < jugadores.size(); i++) {
            for (int k = i + 1; k < jugadores.size(); k++) {
                if (jugadores.get(i).getRol() == jugadores.get(k).getRol()) {
                    throw new IllegalArgumentException("Los roles no se pueden repetir.");
                }
            }
        }
        this.jugadores = new ArrayList<>(jugadores);
        this.ciudad = new EstadoCiudad();
        this.feed = new ArbolAVL();
        this.arbolDecision = new ArbolDecision();
        this.banco = new BancoPublicaciones();
        this.niveles = GrafoNiveles.crearCampana();
        this.red = new GrafoSocial();
        this.red.inicializarMapaPrueba();
        StringBuilder nombres = new StringBuilder();
        for (Jugador j : this.jugadores) {
            nombres.append(nombres.length() == 0 ? "" : ", ").append(j.getNombre())
                   .append(" (").append(j.getRol().getEtiqueta()).append(")");
        }
        bitacora.add("Arranca la campana electoral en Ciudad Nova. Juegan: " + nombres
                + ". Hay " + niveles.cantidadVertices() + " niveles por delante.");
    }

    // ------------------------------------------------------------------
    // Turnos
    // ------------------------------------------------------------------

    /** Indica a quién le toca jugar (0 = primer jugador). */
    public void setTurno(int indice) {
        if (indice < 0 || indice >= jugadores.size()) {
            throw new IllegalArgumentException("Turno fuera de rango: " + indice);
        }
        indiceTurno = indice;
    }

    /** El jugador al que le toca: sus decisiones usan los multiplicadores de SU rol. */
    public Jugador getJugador() {
        return jugadores.get(indiceTurno);
    }

    public List<Jugador> getJugadores() {
        return Collections.unmodifiableList(jugadores);
    }

    // ------------------------------------------------------------------
    // Inicio de nivel
    // ------------------------------------------------------------------

    /**
     * Prepara un nivel: siembra un rumor en un lugar aleatorio de la ciudad y
     * elige las 4 publicaciones que saldrán. En los niveles 1 y 2 sale una de
     * cada tipo (para aprender los 4 símbolos); después el orden es libre.
     * Cada publicación entra al AVL del feed.
     */
    public List<Publicacion> iniciarNivel(int numero) {
        if (!niveles.estaDesbloqueado(numero)) {
            throw new IllegalStateException("El nivel " + numero + " sigue bloqueado.");
        }
        NodoRed origen = red.sembrarRumor(banco.getAzar());

        List<TipoContenido> tipos = new ArrayList<>(Arrays.asList(TipoContenido.values()));
        Collections.shuffle(tipos, banco.getAzar());
        if (numero > 2) {
            for (int i = 0; i < tipos.size(); i++) {
                if (banco.getAzar().nextInt(100) < 40) {
                    tipos.set(i, TipoContenido.values()[banco.getAzar().nextInt(TipoContenido.values().length)]);
                }
            }
        }

        List<Publicacion> lista = new ArrayList<>();
        for (int i = 0; i < DificultadNivel.PUBLICACIONES_POR_NIVEL; i++) {
            Publicacion p = banco.publicacionAleatoria(nuevoId(), tipos.get(i % tipos.size()), textosUsados);
            textosUsados.add(p.getTexto());
            feed.insertar(p);           // el feed (AVL) crece con cada publicación que aparece
            lista.add(p);
        }
        bitacora.add("NIVEL " + numero + ": aparece un rumor en " + origen.nombre
                + ". Feed: " + feed.getTamano() + " publicaciones, altura del AVL " + feed.getAlturaArbol() + ".");
        return lista;
    }

    /** Id único para la clave del AVL (1..999); si ya existe en el feed, busca el siguiente. */
    private int nuevoId() {
        for (int intentos = 0; intentos < 999; intentos++) {
            int id = siguienteId;
            siguienteId = siguienteId % 999 + 1;
            if (!idEnFeed(id)) {
                return id;
            }
        }
        throw new IllegalStateException("El feed esta lleno.");
    }

    private boolean idEnFeed(int id) {
        for (Publicacion p : feed.inorden()) {
            if (p.getId() == id) {
                return true;
            }
        }
        return false;
    }

    /** La burbuja correcta para esta publicación la decide el árbol de decisión. */
    public Accion accionCorrecta(Publicacion p) {
        return arbolDecision.accionIdeal(p.getTipo());
    }

    // ------------------------------------------------------------------
    // Decisiones durante el nivel
    // ------------------------------------------------------------------

    /**
     * Resuelve una decisión del jugador sobre una publicación:
     *  1) baja por el árbol de decisión (acción, tipo real) y aplica el efecto a la ciudad;
     *  2) si el efecto lo pide, ELIMINA la publicación del AVL (fue reportada);
     *  3) mueve el rumor por el grafo: acierto = se contiene, error = avanza un salto (BFS).
     * Se llama una vez al completar una publicación (acierto) y una vez por cada burbuja
     * equivocada (error).
     */
    public ResultadoDecision resolver(Publicacion p, Accion accion) {
        boolean fueVerificacion = (accion == Accion.VERIFICAR);
        boolean correcta = (accion == accionCorrecta(p));

        Efecto efecto = arbolDecision.resolver(accion, p.getTipo());
        Jugador jugador = getJugador();
        efecto.aplicarSobre(ciudad, jugador.getRol(), fueVerificacion);
        int puntos = efecto.puntosPara(jugador.getRol(), fueVerificacion);
        int reputacion = efecto.reputacionPara(jugador.getRol());
        jugador.sumarPuntos(puntos);
        jugador.sumarReputacion(reputacion);

        if (fueVerificacion) {
            p.marcarVerificada();
        }
        if (efecto.debeRetirarPublicacion() && !p.estaRetirada()) {
            feed.eliminar(p.claveAVL());   // eliminación real en el AVL
            p.marcarRetirada();
        }

        List<NodoRed> contagiados = new ArrayList<>();
        List<NodoRed> limpiados = new ArrayList<>();
        if (correcta) {
            limpiados = red.contener();
        } else {
            contagiados = red.propagarPaso();
            ciudad.aplicar(0, 0, 0, 0, contagiados.size(), 0);
        }

        String ruta = arbolDecision.rutaDe(accion, p.getTipo());
        bitacora.add(jugador.getNombre() + ": " + (correcta ? "ACIERTO " : "ERROR ") + accion.getEtiqueta() + " -> \"" + recortar(p.getTexto())
                + "\" | " + efecto.getMensaje() + " (" + (puntos >= 0 ? "+" : "") + puntos + " pts)"
                + (contagiados.isEmpty() ? "" : " El rumor llego a " + nombres(contagiados) + ".")
                + (limpiados.isEmpty() ? "" : " Se limpio " + nombres(limpiados) + "."));
        return new ResultadoDecision(p, accion, correcta, efecto, puntos, reputacion, ruta, contagiados, limpiados);
    }

    /** Se acabó el tiempo de una publicación: nadie actuó y el rumor avanza. */
    public ResultadoDecision tiempoAgotado(Publicacion p) {
        ResultadoDecision r = resolver(p, Accion.IGNORAR);
        if (r.correcta) {
            // si justo era para ignorar, el tiempo agotado no se premia: el rumor avanza igual
            red.propagarPaso();
        }
        ciudad.aplicar(0, 0, 0, -1, 2, 1);
        bitacora.add("Se acabo el tiempo: la publicacion siguio circulando sin que nadie actuara.");
        return r;
    }

    /** Puntos por cada burbuja correcta recolectada (no cuenta como acierto de decisión). */
    public void sumarBurbuja(int puntos) {
        getJugador().sumarBonus(puntos);
    }

    /** Evento aleatorio entre publicaciones (requisito de componente aleatorio). */
    public String eventoAleatorio() {
        String e = banco.eventoAleatorio(ciudad);
        bitacora.add(e);
        return e;
    }

    // ------------------------------------------------------------------
    // Fin de nivel y de campaña
    // ------------------------------------------------------------------

    /** Segundos de referencia: terminar antes de esto da puntos extra (3 por segundo ahorrado). */
    public static final int SEGUNDOS_REFERENCIA = 240;

    /** Puntos extra por velocidad: entre menos tiempo tarde el jugador, más puntos. */
    public static int bonoPorTiempo(double segundos) {
        return (int) Math.max(0, Math.round((SEGUNDOS_REFERENCIA - segundos) * 3));
    }

    /**
     * Cierra la ronda del jugador de turno en un nivel. Si lo superó, recibe
     * 25 puntos por cada vida que le quedó y el bono por tiempo.
     * Devuelve el bono por tiempo (0 si no lo superó).
     */
    public int registrarRonda(int numero, boolean gano, int vidasRestantes, double segundos) {
        Jugador j = getJugador();
        if (!gano) {
            bitacora.add("NIVEL " + numero + ": " + j.getNombre() + " no lo supero ("
                    + String.format("%.1f", segundos) + " s). El rumor quedo en "
                    + red.cantidadInfectados() + " lugares.");
            return 0;
        }
        int bono = bonoPorTiempo(segundos);
        j.sumarBonus(vidasRestantes * 25 + bono);
        bitacora.add("NIVEL " + numero + ": " + j.getNombre() + " lo supero en "
                + String.format("%.1f", segundos) + " s con " + vidasRestantes + " vida(s). Bono por tiempo: +"
                + bono + ".");
        return bono;
    }

    /**
     * Cuando todos jugaron el nivel y AL MENOS UNO lo superó, se marca como ganado
     * en el grafo. Devuelve los niveles que se acaban de desbloquear.
     */
    public List<Integer> completarNivel(int numero, int estrellas, int mejorPuntaje) {
        List<Integer> nuevos = niveles.completar(numero, Math.max(1, Math.min(3, estrellas)), mejorPuntaje);
        bitacora.add("NIVEL " + numero + " superado por el equipo. "
                + (nuevos.isEmpty() ? "" : "Se desbloquean: " + nuevos + "."));
        return nuevos;
    }

    /** La elección se habilita cuando el jugador gana un nivel final (una hoja del grafo). */
    public boolean eleccionDisponible() {
        for (NodoNivel n : niveles.getNodos()) {
            if (niveles.esHoja(n.getNumero()) && n.isCompletado()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Peso electoral de un jugador: sus puntos más el doble de su reputación,
     * escalado por la salud de la ciudad (una ciudad sana cree más en sus líderes).
     */
    private double valorElectoral(Jugador j) {
        double base = Math.max(1, j.getPuntos() + j.getReputacion() * 2);
        return base * (0.5 + ciudad.saludGlobal() / 200.0);
    }

    /** El alcalde electo es el jugador con mayor peso electoral. */
    public Jugador alcaldeElecto() {
        Jugador mejor = jugadores.get(0);
        for (Jugador j : jugadores) {
            if (valorElectoral(j) > valorElectoral(mejor)) {
                mejor = j;
            }
        }
        return mejor;
    }

    /**
     * Porcentaje de votos de un jugador. Con varios jugadores se reparten según
     * su peso electoral; con uno solo, depende de la salud de la ciudad y su reputación.
     */
    public int porcentajeVotos(Jugador j) {
        if (jugadores.size() == 1) {
            double v = ciudad.saludGlobal() * 0.6 + j.getReputacion() * 0.4;
            return (int) Math.max(5, Math.min(95, Math.round(v)));
        }
        double total = 0;
        for (Jugador otro : jugadores) {
            total += valorElectoral(otro);
        }
        return (int) Math.round(valorElectoral(j) * 100 / total);
    }

    /** Retorna el veredicto final de la ciudad en funcion de su salud global. */
    public String veredicto() {
        int salud = ciudad.saludGlobal();
        if (salud >= 75) {
            return "Ciudad Nova cerro las elecciones con una comunidad digital saludable. Lo lograste.";
        }
        if (salud >= 50) {
            return "Ciudad Nova sobrevivio la campana, pero quedaron heridas: aun circula desinformacion.";
        }
        return "Ciudad Nova termino dividida y desinformada. La proxima vez, verifica antes de compartir.";
    }

    // ------------------------------------------------------------------
    // Accesores para la GUI
    // ------------------------------------------------------------------

    public EstadoCiudad getCiudad() { return ciudad; }
    public ArbolAVL getFeed() { return feed; }
    public ArbolDecision getArbolDecision() { return arbolDecision; }
    public GrafoNiveles getNiveles() { return niveles; }
    public GrafoSocial getRed() { return red; }
    public List<String> getBitacora() { return bitacora; }

    private static String recortar(String t) {
        return t.length() <= 45 ? t : t.substring(0, 42) + "...";
    }

    private static String nombres(List<NodoRed> lista) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(lista.get(i).nombre);
        }
        return sb.toString();
    }

    /** Resultado inmutable de una decisión, para que la GUI lo muestre. */
    public static class ResultadoDecision {
        public final Publicacion publicacion;
        public final Accion accion;
        public final boolean correcta;
        public final Efecto efecto;
        public final int puntos;
        public final int reputacion;
        public final String rutaArbol;
        public final List<NodoRed> contagiados;
        public final List<NodoRed> limpiados;

        public ResultadoDecision(Publicacion publicacion, Accion accion, boolean correcta, Efecto efecto,
                                 int puntos, int reputacion, String rutaArbol,
                                 List<NodoRed> contagiados, List<NodoRed> limpiados) {
            this.publicacion = publicacion;
            this.accion = accion;
            this.correcta = correcta;
            this.efecto = efecto;
            this.puntos = puntos;
            this.reputacion = reputacion;
            this.rutaArbol = rutaArbol;
            this.contagiados = contagiados;
            this.limpiados = limpiados;
        }
    }
}
