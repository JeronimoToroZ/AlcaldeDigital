package influenceinc.red;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CLIENTE de la partida en red. Escucha al servidor en un hilo aparte y avisa
 * al juego con un Oyente (la interfaz grafica pasa los avisos al hilo de Swing).
 */
public class ClienteOficina {

    public interface Oyente {
        void alLobby(List<String> nombres);

        void alIniciar(long semilla, boolean relajado);

        /** Cada entrada: {nombre, puntos, dia, aciertos, advertencias, terminado}. */
        void alMarcador(List<String[]> filas);

        void alError(String texto);

        /** Otro jugador destapo algo: llega como "ultima hora" al noticiero de todos. */
        void alNoticia(String quien, String texto);

        /** Otro jugador elimino una cuenta falsa: tambien sale del grafo social propio. */
        void alRelacion(String quien, String idCuenta, String nombreCuenta);

        /** Faltan estos jugadores por terminar el dia. */
        void alEspera(int dia, List<String> faltan);

        /** Todos terminaron: empieza este dia. */
        void alTurno(int dia);
    }

    private final Socket socket;
    private final PrintWriter out;
    private final String nombre;

    public ClienteOficina(String host, int puerto, String nombre, Oyente oyente) throws IOException {
        this.nombre = nombre;
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, puerto), 4000);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        Thread t = new Thread(() -> escuchar(oyente), "cliente-oficina");
        t.setDaemon(true);
        t.start();
        out.println("HOLA " + nombre);
    }

    private void escuchar(Oyente o) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = in.readLine()) != null) {
                interpretar(l, o);
            }
        } catch (IOException e) {
            o.alError("Se perdió la conexión con el servidor.");
        }
    }

    /** Separado para poder probarlo sin red. */
    public static void interpretar(String l, Oyente o) {
        if (l.startsWith("LOBBY")) {
            String r = l.length() > 6 ? l.substring(6) : "";
            o.alLobby(r.isEmpty() ? new ArrayList<String>() : Arrays.asList(r.split(",")));
        } else if (l.startsWith("INICIO ")) {
            String[] p = l.substring(7).split("\\|");
            o.alIniciar(Long.parseLong(p[0]), p.length > 1 && Boolean.parseBoolean(p[1]));
        } else if (l.startsWith("MARCADOR ")) {
            List<String[]> filas = new ArrayList<>();
            for (String f : l.substring(9).split(";")) {
                String[] c = f.split(":");
                if (c.length >= 6) {
                    filas.add(c);
                }
            }
            o.alMarcador(filas);
        } else if (l.startsWith("ERROR ")) {
            o.alError(l.substring(6));
        } else if (l.startsWith("NOTICIA ")) {
            String[] p = l.substring(8).split("\\|", 2);
            if (p.length == 2) {
                o.alNoticia(p[0], p[1]);
            }
        } else if (l.startsWith("RELACION ")) {
            String[] p = l.substring(9).split("\\|", 3);
            if (p.length == 3) {
                o.alRelacion(p[0], p[1], p[2]);
            }
        } else if (l.startsWith("ESPERA ")) {
            String[] p = l.substring(7).split("\\|", 2);
            List<String> faltan = p.length < 2 || p[1].isEmpty() ? new ArrayList<String>() : Arrays.asList(p[1].split(","));
            o.alEspera(entero(p[0]), faltan);
        } else if (l.startsWith("TURNO ")) {
            o.alTurno(entero(l.substring(6)));
        }
    }

    public void enviarResultado(int puntos, int dia, int aciertos, int advertencias, boolean terminado) {
        out.println("RES " + nombre + "|" + puntos + "|" + dia + "|" + aciertos + "|" + advertencias + "|"
                + (terminado ? 1 : 0));
    }

    private static int entero(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Publicacion sellada: el servidor la guarda y la manda como noticia a los demas. */
    public void enviarPublicacion(String sello, String titular) {
        out.println("PUB " + limpio(sello) + "|" + limpio(titular));
    }

    /** Cuenta falsa eliminada del grafo social: el servidor la elimina en el grafo de todos. */
    public void enviarRelacion(String idCuenta, String nombreCuenta) {
        out.println("REL " + limpio(idCuenta) + "|" + limpio(nombreCuenta));
    }

    /** Evento del dia (apagon, viral, panico...). */
    public void enviarEvento(String texto) {
        out.println("EVT " + limpio(texto));
    }

    /** Termine este dia: el servidor decide cuando empieza el siguiente para todos. */
    public void enviarFinDia(int dia) {
        out.println("FIN " + dia);
    }

    private static String limpio(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('|', '/');
    }

    public void cerrar() {
        try {
            socket.close();
        } catch (IOException e) {
            // nada
        }
    }
}
