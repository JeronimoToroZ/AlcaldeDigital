package alcaldedigital.red;

import alcaldedigital.logica.GrafoSocial;
import alcaldedigital.logica.NodoRed;
import alcaldedigital.logica.Partida;
import alcaldedigital.modelo.Accion;
import alcaldedigital.modelo.Cuenta;
import alcaldedigital.modelo.Jugador;
import alcaldedigital.modelo.Publicacion;
import alcaldedigital.modelo.Rol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Servidor de una partida en red.
 *
 * Es el dueno de la partida: aqui viven el grafo, la confianza y los turnos.
 * Los clientes no calculan nada, solo mandan la decision de su jugador y
 * dibujan lo que el servidor les responde. Esa es la diferencia entre un
 * multijugador de verdad y cuatro copias del juego que no se hablan: si cada
 * cliente llevara su propia cuenta, bastaria con que uno fallara un mensaje
 * para que vieran ciudades distintas.
 *
 * Diferencia con el ejemplo de clase: aquel atendia a UN cliente a la vez,
 *
 *     while (true) { socket.accept(); ...resolver...; socket.close(); }
 *
 * lo cual sirve para ordenar un arreglo, pero aqui el segundo jugador en
 * conectarse quedaria esperando a que el primero terminara la partida entera.
 * Por eso cada cliente tiene su propio hilo leyendo su socket y dejando las
 * lineas en una cola; el hilo principal corre la partida y saca de la cola del
 * jugador al que le toca.
 */
public class ServidorPartida {

    /** Cuanto se espera la decision de un jugador antes de jugar por el. */
    private static final int SEGUNDOS_ESPERA = 60;

    private final int puerto;
    private final int jugadoresEsperados;
    private final int publicacionesPorJugador;
    private final List<Conexion> conexiones = new ArrayList<>();
    private final GrafoSocial grafo = new GrafoSocial();

    private Partida partida;
    private ServerSocket servidor;

    public ServidorPartida(int puerto, int jugadoresEsperados) {
        this(puerto, jugadoresEsperados, 2);
    }

    /**
     * @param publicacionesPorJugador cuantas situaciones le tocan a cada uno.
     *        Con 2 jugadores y 1 cada uno la partida duraba dos turnos y se
     *        acababa antes de que se notara que es multijugador.
     */
    public ServidorPartida(int puerto, int jugadoresEsperados, int publicacionesPorJugador) {
        this.puerto = puerto;
        this.jugadoresEsperados = Math.max(2, Math.min(4, jugadoresEsperados));
        this.publicacionesPorJugador = Math.max(1, publicacionesPorJugador);
    }

    /** Levanta el servidor, espera a los jugadores y corre la partida. */
    public void arrancar() throws IOException {
        grafo.inicializarMapaPrueba();

        servidor = new ServerSocket(puerto);
        log("escuchando en el puerto " + puerto
                + ", esperando " + jugadoresEsperados + " jugadores");

        try {
            aceptarJugadores();
            jugar();
        } finally {
            cerrarTodo();
        }
    }

    // ------------------------------------------------------------------
    // Conexion
    // ------------------------------------------------------------------

    private void aceptarJugadores() throws IOException {
        Rol[] roles = {Rol.CIUDADANO, Rol.PERIODISTA, Rol.INFLUENCER, Rol.CANDIDATO};

        while (conexiones.size() < jugadoresEsperados) {
            Socket socket = servidor.accept();
            Conexion c = new Conexion(socket, conexiones.size());

            // El primer mensaje tiene que ser el nombre. Si manda otra cosa,
            // se le asigna uno y se sigue: un cliente mal portado no puede
            // dejar la partida colgada esperando.
            String linea = c.esperarLinea(10);
            String nombre = Protocolo.NOMBRE.equals(Protocolo.comando(linea))
                    ? Protocolo.argumento(linea)
                    : "";
            if (nombre.isEmpty()) {
                nombre = "Jugador " + (conexiones.size() + 1);
            }

            c.jugador = new Jugador(nombre, roles[conexiones.size() % roles.length]);
            conexiones.add(c);
            c.enviar(Protocolo.BIENVENIDA, c.numero, nombre, c.jugador.getRol().name());
            log("conectado: " + nombre + " (" + c.jugador.getRol().name() + ")");

            difundir(Protocolo.ESPERANDO, conexiones.size(), jugadoresEsperados);
        }
    }

    // ------------------------------------------------------------------
    // La partida
    // ------------------------------------------------------------------

    private void jugar() {
        List<Jugador> jugadores = new ArrayList<>();
        for (Conexion c : conexiones) {
            jugadores.add(c.jugador);
        }
        partida = new Partida(jugadores, publicacionesPorJugador, true);

        difundir(Protocolo.INICIO, partida.getTotalTurnos());
        log("arranca la partida: " + partida.getTotalTurnos() + " turnos");

        while (partida.hayTurnoPendiente() && !partida.terminada()) {
            Conexion enTurno = conexionDe(partida.jugadorActual());
            Publicacion p = partida.publicacionActual();

            anunciarTurno(enTurno, p);

            Accion accion = esperarAccion(enTurno);
            Partida.ResultadoTurno r = partida.ejecutar(accion);

            // La decision mueve el rumor: una falsa compartida lo empuja una
            // oleada mas. Asi la red reacciona a lo que hacen los jugadores y
            // no a un cronometro.
            if (accion == Accion.COMPARTIR && p.esFalsa()) {
                grafo.expandirUnaOleada();
            }

            boolean acerto = r.puntos > 0;
            difundir(Protocolo.RESULTADO, enTurno.jugador.getNombre(),
                    accion.name(), acerto ? "SI" : "NO", r.puntos);
            difundirEstado();
        }

        Jugador ganador = partida.alcaldeElecto();
        difundir(Protocolo.FIN,
                ganador != null ? ganador.getNombre() : "nadie",
                partida.getConfianza());
        log("partida terminada");
    }

    private void anunciarTurno(Conexion enTurno, Publicacion p) {
        for (Conexion c : conexiones) {
            c.enviar(Protocolo.TURNO, partida.getTurno(),
                    enTurno.jugador.getNombre(), c == enTurno ? "TU" : "OTRO");
        }
        difundir(Protocolo.PUBLICACION, p.getTexto());

        Cuenta cuenta = p.getCuenta();
        if (cuenta != null) {
            difundir(Protocolo.CUENTA, cuenta.getArroba(), cuenta.antiguedadTexto(),
                    cuenta.seguidoresTexto(),
                    p.getFuenteCitada() == null || p.getFuenteCitada().isEmpty()
                            ? "sin fuente" : p.getFuenteCitada());
        }
        difundirEstado();
    }

    /**
     * Espera la decision del jugador al que le toca.
     *
     * Si no contesta, se juega IGNORAR por el. Sin este limite, un jugador que
     * cierra la ventana deja a los demas esperando para siempre: el servidor
     * se quedaria bloqueado en una lectura que nunca llega.
     */
    private Accion esperarAccion(Conexion enTurno) {
        long limite = System.currentTimeMillis() + SEGUNDOS_ESPERA * 1000L;

        while (System.currentTimeMillis() < limite) {
            String linea = enTurno.esperarLinea(2);
            if (linea == null) {
                continue;
            }
            String comando = Protocolo.comando(linea);

            if (Protocolo.ACCION.equals(comando)) {
                try {
                    return Accion.valueOf(Protocolo.argumento(linea).toUpperCase());
                } catch (IllegalArgumentException ex) {
                    enTurno.enviar(Protocolo.ERROR, "accion desconocida: "
                            + Protocolo.argumento(linea));
                }
            } else if (Protocolo.DESMENTIR.equals(comando)) {
                desmentir(enTurno, Protocolo.argumento(linea));
            } else if (Protocolo.SALIR.equals(comando)) {
                return Accion.IGNORAR;
            }
        }

        enTurno.enviar(Protocolo.AVISO, "Se acabó el tiempo: se ignoró la publicación.");
        return Accion.IGNORAR;
    }

    /** Un jugador habla con un vecino del mapa para desmentir el rumor. */
    private void desmentir(Conexion quien, String lugar) {
        NodoRed origen = grafo.buscar(lugar);
        if (origen == null) {
            quien.enviar(Protocolo.ERROR, "no existe ese lugar: " + lugar);
            return;
        }
        GrafoSocial.ResultadoCorreccion r = grafo.corregir(origen, 1);
        difundir(Protocolo.AVISO, quien.jugador.getNombre() + " habló con "
                + origen.descripcion() + ": se retractaron " + r.corregidos.size()
                + ", siguen desinformados " + r.siguenDesinformados.size() + ".");
        difundirEstado();
    }

    // ------------------------------------------------------------------
    // Estado compartido
    // ------------------------------------------------------------------

    private void difundirEstado() {
        difundir(Protocolo.CIUDAD, partida.getConfianza(),
                grafo.infectados().size(), grafo.nodos.size());

        StringBuilder mapa = new StringBuilder();
        for (NodoRed n : grafo.nodos) {
            if (mapa.length() > 0) {
                mapa.append(Protocolo.SEP);
            }
            mapa.append(n.nombre).append('=').append(n.estaInfectado ? 'R' : 'V');
        }
        difundir(Protocolo.linea(Protocolo.MAPA, mapa.toString()));
    }

    private void difundir(String comando, Object... campos) {
        difundir(Protocolo.linea(comando, campos));
    }

    private void difundir(String lineaCompleta) {
        for (Conexion c : conexiones) {
            c.enviarLinea(lineaCompleta);
        }
    }

    private Conexion conexionDe(Jugador j) {
        for (Conexion c : conexiones) {
            if (c.jugador == j) {
                return c;
            }
        }
        return conexiones.get(0);
    }

    private void cerrarTodo() {
        for (Conexion c : conexiones) {
            c.cerrar();
        }
        try {
            if (servidor != null) {
                servidor.close();
            }
        } catch (IOException ignorada) {
            // cerrar el servidor no puede impedir que el programa termine
        }
    }

    private static void log(String mensaje) {
        System.out.println("[servidor] " + mensaje);
    }

    // ------------------------------------------------------------------
    // Una conexion: su socket, su hilo lector y su cola de lineas
    // ------------------------------------------------------------------

    /**
     * El hilo lector existe para que el servidor nunca se quede bloqueado en
     * readLine(). Lee sin parar y deja las lineas en una cola; el hilo de la
     * partida saca de ahi con un plazo. Asi un jugador que no responde retrasa
     * su turno, pero no congela la partida de los demas.
     */
    private static class Conexion {

        final Socket socket;
        final int numero;
        final PrintWriter salida;
        final BlockingQueue<String> recibidas = new ArrayBlockingQueue<>(64);
        final Thread lector;

        Jugador jugador;
        volatile boolean viva = true;

        Conexion(Socket socket, int numero) throws IOException {
            this.socket = socket;
            this.numero = numero;
            this.salida = new PrintWriter(new java.io.OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8), true);

            BufferedReader entrada = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));

            lector = new Thread(() -> {
                try {
                    String linea;
                    while (viva && (linea = entrada.readLine()) != null) {
                        recibidas.offer(linea);
                    }
                } catch (IOException ex) {
                    // se cayo la conexion: se marca y el hilo termina
                } finally {
                    viva = false;
                }
            }, "lector-" + numero);
            lector.setDaemon(true);
            lector.start();
        }

        /** Saca una linea de la cola, esperando como mucho los segundos dados. */
        String esperarLinea(int segundos) {
            try {
                return recibidas.poll(segundos, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        void enviar(String comando, Object... campos) {
            enviarLinea(Protocolo.linea(comando, campos));
        }

        void enviarLinea(String linea) {
            salida.println(linea);
        }

        void cerrar() {
            viva = false;
            try {
                socket.close();
            } catch (IOException ignorada) {
                // ya estaba cerrado
            }
        }
    }

    // ------------------------------------------------------------------

    /** Arranca el servidor. Argumentos: [puerto] [numero de jugadores]. */
    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : Protocolo.PUERTO;
        int jugadores = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        int porJugador = args.length > 2 ? Integer.parseInt(args[2]) : 2;
        new ServidorPartida(puerto, jugadores, porJugador).arrancar();
    }
}
