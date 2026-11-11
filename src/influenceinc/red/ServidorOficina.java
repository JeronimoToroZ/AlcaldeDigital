package influenceinc.red;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SERVIDOR de la partida en red (cliente-servidor con sockets TCP).
 *
 * Cada verificador juega en su computador con la MISMA semilla (mismos casos,
 * mismos eventos). El servidor no juega: recibe los resultados de todos y
 * reparte el marcador en vivo. Protocolo de texto, una linea por mensaje:
 *
 *   cliente → servidor:  HOLA nombre
 *                        RES nombre|puntos|dia|aciertos|advertencias|terminado
 *   servidor → cliente:  LOBBY nombre1,nombre2,...
 *                        INICIO semilla|relajado
 *                        MARCADOR nombre:puntos:dia:aciertos:adv:fin;...
 *                        ERROR texto
 */
public class ServidorOficina {

    public static final int PUERTO = 5077;

    private final ServerSocket socket;
    private final List<Conexion> conexiones = new ArrayList<>();
    private final Map<String, String> marcador = new LinkedHashMap<>();
    private volatile boolean iniciada;

    private class Conexion implements Runnable {
        final Socket s;
        final PrintWriter out;
        String nombre;

        Conexion(Socket s) throws IOException {
            this.s = s;
            out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = in.readLine()) != null) {
                    procesar(this, linea.trim());
                }
            } catch (IOException e) {
                // el cliente se fue
            } finally {
                synchronized (ServidorOficina.this) {
                    conexiones.remove(this);
                }
                difundirLobby();
            }
        }
    }

    public ServidorOficina(int puerto) throws IOException {
        socket = new ServerSocket(puerto);
        Thread t = new Thread(this::aceptar, "servidor-oficina");
        t.setDaemon(true);
        t.start();
    }

    private void aceptar() {
        while (!socket.isClosed()) {
            try {
                Socket s = socket.accept();
                Conexion c = new Conexion(s);
                synchronized (this) {
                    conexiones.add(c);
                }
                Thread t = new Thread(c, "conexion");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    synchronized void procesar(Conexion c, String linea) {
        if (linea.startsWith("HOLA ")) {
            String n = linea.substring(5).replaceAll("[|:;,]", " ").trim();
            if (n.isEmpty()) {
                n = "Verificador";
            }
            String base = n;
            int k = 2;
            while (marcador.containsKey(n)) {
                n = base + " " + k++;
            }
            if (iniciada) {
                c.out.println("ERROR la partida ya empezó");
                return;
            }
            c.nombre = n;
            marcador.put(n, n + ":0:1:0:0:0");
            difundirLobby();
        } else if (linea.startsWith("RES ") && c.nombre != null) {
            String[] p = linea.substring(4).split("\\|");
            if (p.length >= 6) {
                marcador.put(c.nombre, c.nombre + ":" + num(p[1]) + ":" + num(p[2]) + ":" + num(p[3]) + ":"
                        + num(p[4]) + ":" + num(p[5]));
                difundir("MARCADOR " + String.join(";", marcador.values()));
            }
        }
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private synchronized void difundirLobby() {
        List<String> nombres = new ArrayList<>();
        for (Conexion c : conexiones) {
            if (c.nombre != null) {
                nombres.add(c.nombre);
            }
        }
        difundir("LOBBY " + String.join(",", nombres));
    }

    private synchronized void difundir(String m) {
        for (Conexion c : conexiones) {
            c.out.println(m);
        }
    }

    /** El anfitrion arranca: todos reciben la misma semilla. */
    public synchronized void iniciar(long semilla, boolean relajado) {
        iniciada = true;
        difundir("INICIO " + semilla + "|" + relajado);
    }

    public synchronized int jugadores() {
        int n = 0;
        for (Conexion c : conexiones) {
            if (c.nombre != null) {
                n++;
            }
        }
        return n;
    }

    public void cerrar() {
        try {
            socket.close();
        } catch (IOException e) {
            // nada
        }
        synchronized (this) {
            for (Conexion c : conexiones) {
                try {
                    c.s.close();
                } catch (IOException e) {
                    // nada
                }
            }
        }
    }
}
