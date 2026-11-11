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
        }
    }

    public void enviarResultado(int puntos, int dia, int aciertos, int advertencias, boolean terminado) {
        out.println("RES " + nombre + "|" + puntos + "|" + dia + "|" + aciertos + "|" + advertencias + "|"
                + (terminado ? 1 : 0));
    }

    public void cerrar() {
        try {
            socket.close();
        } catch (IOException e) {
            // nada
        }
    }
}
