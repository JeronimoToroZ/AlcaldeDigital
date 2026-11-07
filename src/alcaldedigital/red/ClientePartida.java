package alcaldedigital.red;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Cliente de una partida en red.
 *
 * No decide nada: manda la accion del jugador y dibuja lo que el servidor
 * responde. Toda la logica -confianza, turnos, grafo- vive en el servidor.
 *
 * La clase sirve para dos cosas a la vez:
 *
 *  - Como programa de consola, para probar la red sin interfaz. Depurar
 *    sockets a traves de una ventana es horrible, porque cuando algo no
 *    aparece no se sabe si fallo la red o el dibujo.
 *  - Como pieza para la interfaz: quien la use le pasa un escuchador y recibe
 *    cada linea del servidor para pintarla donde quiera.
 */
public class ClientePartida {

    private final String host;
    private final int puerto;
    private final String nombre;

    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private Thread lector;
    private volatile boolean conectado;

    public ClientePartida(String host, int puerto, String nombre) {
        this.host = host;
        this.puerto = puerto;
        this.nombre = nombre;
    }

    /**
     * Se conecta y arranca un hilo que escucha al servidor.
     *
     * El hilo es necesario porque los mensajes del servidor llegan cuando el
     * servidor quiere -no solo como respuesta a algo nuestro-: cuando otro
     * jugador decide, cuando el rumor avanza, cuando termina la partida.
     * Leyendo solo despues de escribir, esos mensajes se perderian o llegarian
     * tarde.
     *
     * @param alRecibir se invoca con cada linea; corre en el hilo lector, asi
     *                  que una interfaz debe reenviarla a su propio hilo
     */
    public void conectar(Consumer<String> alRecibir) throws IOException {
        socket = new Socket(host, puerto);
        salida = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream(), StandardCharsets.UTF_8), true);
        entrada = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
        conectado = true;

        lector = new Thread(() -> {
            try {
                String linea;
                while (conectado && (linea = entrada.readLine()) != null) {
                    alRecibir.accept(linea);
                }
            } catch (IOException ex) {
                if (conectado) {
                    alRecibir.accept(Protocolo.linea(Protocolo.ERROR,
                            "se perdió la conexión con el servidor"));
                }
            } finally {
                conectado = false;
            }
        }, "cliente-lector");
        lector.setDaemon(true);
        lector.start();

        enviar(Protocolo.NOMBRE, nombre);
    }

    public void enviar(String comando, String argumento) {
        if (salida != null) {
            salida.println(Protocolo.linea(comando, argumento));
        }
    }

    public boolean estaConectado() {
        return conectado;
    }

    public void cerrar() {
        conectado = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignorada) {
            // ya estaba cerrado
        }
    }

    // ------------------------------------------------------------------
    // Modo consola
    // ------------------------------------------------------------------

    /**
     * Traduce una linea del protocolo a algo legible.
     * Se mantiene aparte del dibujo para que la interfaz grafica pueda
     * interpretar las mismas lineas sin heredar este formato de consola.
     */
    private static String legible(String linea) {
        String comando = Protocolo.comando(linea);
        String[] c = Protocolo.campos(Protocolo.argumento(linea));

        switch (comando) {
            case Protocolo.BIENVENIDA:
                return "Conectado como " + c[1] + " (" + c[2] + ")";
            case Protocolo.ESPERANDO:
                return "Jugadores conectados: " + c[0] + " de " + c[1];
            case Protocolo.INICIO:
                return "\n=== ARRANCA LA PARTIDA (" + c[0] + " situaciones) ===";
            case Protocolo.TURNO:
                return "\n--- Situación " + c[0] + " · turno de " + c[1]
                        + ("TU".equals(c[2]) ? "  <<< TE TOCA" : "");
            case Protocolo.PUBLICACION:
                return "  \"" + Protocolo.argumento(linea) + "\"";
            case Protocolo.CUENTA:
                return "  cuenta " + c[0] + " · " + c[1] + " · " + c[2] + " · " + c[3];
            case Protocolo.CIUDAD:
                return "  ciudad: confianza " + c[0] + " · desinformados "
                        + c[1] + " de " + c[2];
            case Protocolo.RESULTADO:
                return "  " + c[0] + " eligió " + c[1] + " -> "
                        + ("SI".equals(c[2]) ? "acertó" : "falló") + " (" + c[3] + " pts)";
            case Protocolo.AVISO:
                return "  * " + Protocolo.argumento(linea);
            case Protocolo.FIN:
                return "\n=== FIN === alcalde electo: " + c[0]
                        + " · confianza final " + c[1];
            case Protocolo.ERROR:
                return "  [error] " + Protocolo.argumento(linea);
            default:
                return "  ? " + linea;
        }
    }

    /** Argumentos: [host] [puerto] [nombre]. */
    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int puerto = args.length > 1 ? Integer.parseInt(args[1]) : Protocolo.PUERTO;
        String nombre = args.length > 2 ? args[2] : "Jugador";

        ClientePartida cliente = new ClientePartida(host, puerto, nombre);
        final boolean[] meToca = {false};
        final boolean[] termino = {false};

        cliente.conectar(linea -> {
            System.out.println(legible(linea));
            String comando = Protocolo.comando(linea);
            if (Protocolo.TURNO.equals(comando)) {
                String[] c = Protocolo.campos(Protocolo.argumento(linea));
                meToca[0] = "TU".equals(c[2]);
                if (meToca[0]) {
                    System.out.println("  acciones: compartir | verificar | reportar | ignorar");
                    System.out.print("  tu decisión> ");
                }
            } else if (Protocolo.FIN.equals(comando)) {
                termino[0] = true;
            }
        });

        BufferedReader teclado = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String escrito;
        while (!termino[0] && (escrito = teclado.readLine()) != null) {
            escrito = escrito.trim();
            if (escrito.isEmpty()) {
                continue;
            }
            if (escrito.toLowerCase().startsWith("desmentir ")) {
                cliente.enviar(Protocolo.DESMENTIR, escrito.substring(10).trim());
            } else {
                cliente.enviar(Protocolo.ACCION, escrito.toUpperCase());
            }
        }
        cliente.cerrar();
    }
}
