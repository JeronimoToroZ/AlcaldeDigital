package influenceinc.red;

import influenceinc.estructuras.Ordenamientos;
import influenceinc.estructuras.TablaHash;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SERVIDOR de la partida en red (cliente-servidor con sockets TCP).
 *
 * Cada verificador juega en su computador con la MISMA semilla (mismos casos,
 * mismos eventos). El servidor guarda y SINCRONIZA el estado compartido:
 *   - JUGADORES: tabla hash nombre -> datos (rol, puntos, dia, advertencias...)
 *   - PUBLICACIONES selladas por cada uno (se reenvian como noticia)
 *   - RELACIONES: cuentas falsas eliminadas del grafo social (se eliminan
 *     tambien en el grafo de los demas)
 *   - EVENTOS del dia (apagon, viral, panico...)
 *   - PUNTUACIONES: el marcador, ordenado por insercion
 *   - TURNOS: nadie empieza el dia d+1 hasta que todos terminen el dia d
 *
 * HILOS: un hilo acepta conexiones y cada conexion tiene su propio hilo que
 * lee lineas. Todos tocan el mismo estado, por eso los metodos que lo
 * modifican son synchronized (uno a la vez).
 *
 * Protocolo de texto, una linea por mensaje:
 *   cliente -> servidor:  HOLA nombre
 *                         RES nombre|puntos|dia|aciertos|advertencias|terminado
 *                         PUB sello|titular          (publicacion sellada)
 *                         REL idCuenta|nombreCuenta  (cuenta falsa eliminada)
 *                         EVT texto                  (evento del dia)
 *                         FIN dia                    (termino ese dia)
 *   servidor -> cliente:  LOBBY nombre1,nombre2,...
 *                         INICIO semilla|relajado
 *                         MARCADOR nombre:puntos:dia:aciertos:adv:fin;...
 *                         NOTICIA nombre|texto
 *                         RELACION nombre|idCuenta|nombreCuenta
 *                         ESPERA dia|nombre1,nombre2   (quienes faltan por terminar)
 *                         TURNO dia                    (todos pueden empezar ese dia)
 *                         ERROR texto
 */
public class ServidorOficina {

    public static final int PUERTO = 5077;

    /** Lo que el servidor sabe de cada jugador. */
    static class JugadorRed {
        final String nombre;
        int puntos, dia = 1, aciertos, advertencias;
        boolean termino;          // termino la partida (o lo despidieron)
        int diaCerrado;           // ultimo dia que termino
        boolean conectado = true;

        JugadorRed(String nombre) {
            this.nombre = nombre;
        }

        String fila() {
            return nombre + ":" + puntos + ":" + dia + ":" + aciertos + ":" + advertencias + ":" + (termino ? 1 : 0);
        }
    }

    private final ServerSocket socket;
    private final List<Conexion> conexiones = new ArrayList<>();
    private final TablaHash<String, JugadorRed> jugadores = new TablaHash<>();
    private final List<String> publicaciones = new ArrayList<>();
    private final List<String> relaciones = new ArrayList<>();
    private final List<String> eventos = new ArrayList<>();
    private int turno = 1;
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
                desconectar(this);
            }
        }
    }

    public ServidorOficina(int puerto) throws IOException {
        socket = new ServerSocket(puerto);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                aceptar();
            }
        }, "servidor-oficina");
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
            if (iniciada) {
                c.out.println("ERROR la partida ya empezó");
                return;
            }
            String base = n;
            int k = 2;
            while (jugadores.contiene(n)) {
                n = base + " " + k++;
            }
            c.nombre = n;
            jugadores.poner(n, new JugadorRed(n));
            difundirLobby();
            return;
        }
        if (c.nombre == null) {
            return;
        }
        JugadorRed jr = jugadores.obtener(c.nombre);
        if (linea.startsWith("RES ")) {
            String[] p = linea.substring(4).split("\\|");
            if (p.length >= 6) {
                jr.puntos = num(p[1]);
                jr.dia = num(p[2]);
                jr.aciertos = num(p[3]);
                jr.advertencias = num(p[4]);
                jr.termino = num(p[5]) == 1;
                difundirMarcador();
                if (jr.termino) {
                    revisarTurno();
                }
            }
        } else if (linea.startsWith("PUB ") && iniciada) {
            String[] p = linea.substring(4).split("\\|", 2);
            if (p.length == 2) {
                publicaciones.add(c.nombre + "|" + jr.dia + "|" + p[0] + "|" + p[1]);
                difundirMenos(c, "NOTICIA " + c.nombre + "|" + p[1] + " (sello " + p[0] + ")");
            }
        } else if (linea.startsWith("REL ") && iniciada) {
            String[] p = linea.substring(4).split("\\|", 2);
            if (p.length == 2 && !relaciones.contains(p[0])) {
                relaciones.add(p[0]);
                difundirMenos(c, "RELACION " + c.nombre + "|" + p[0] + "|" + p[1]);
            }
        } else if (linea.startsWith("EVT ") && iniciada) {
            eventos.add(c.nombre + "|" + linea.substring(4));
            difundirMenos(c, "NOTICIA " + c.nombre + "|" + linea.substring(4));
        } else if (linea.startsWith("FIN ") && iniciada) {
            jr.diaCerrado = Math.max(jr.diaCerrado, num(linea.substring(4)));
            revisarTurno();
        }
    }

    /**
     * TURNOS: si todos los jugadores conectados que siguen en la partida ya
     * cerraron el dia actual, todos pasan al siguiente. Si no, se avisa a
     * quienes faltan.
     */
    private void revisarTurno() {
        List<String> faltan = new ArrayList<>();
        for (String n : jugadores.claves()) {
            JugadorRed x = jugadores.obtener(n);
            if (x.conectado && !x.termino && x.diaCerrado < turno) {
                faltan.add(n);
            }
        }
        if (faltan.isEmpty()) {
            turno++;
            difundir("TURNO " + turno);
        } else {
            difundir("ESPERA " + turno + "|" + String.join(",", faltan));
        }
    }

    private synchronized void desconectar(Conexion c) {
        conexiones.remove(c);
        if (c.nombre != null) {
            JugadorRed x = jugadores.obtener(c.nombre);
            if (x != null) {
                x.conectado = false;
            }
            if (!iniciada) {
                jugadores.eliminar(c.nombre);
            } else {
                revisarTurno();         // quien se fue ya no detiene a los demas
            }
        }
        difundirLobby();
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** El marcador, de mayor a menor puntaje (insercion: son 2 a 4 jugadores). */
    private void difundirMarcador() {
        List<JugadorRed> l = new ArrayList<>();
        for (String n : jugadores.claves()) {
            l.add(jugadores.obtener(n));
        }
        Ordenamientos.insercion(l, new Comparator<JugadorRed>() {
            @Override
            public int compare(JugadorRed a, JugadorRed b) {
                return Integer.compare(b.puntos, a.puntos);
            }
        });
        List<String> filas = new ArrayList<>();
        for (JugadorRed x : l) {
            filas.add(x.fila());
        }
        difundir("MARCADOR " + String.join(";", filas));
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

    /** A todos menos a quien lo mando (el ya lo sabe). */
    private synchronized void difundirMenos(Conexion origen, String m) {
        for (Conexion c : conexiones) {
            if (c != origen) {
                c.out.println(m);
            }
        }
    }

    /** El anfitrion arranca: todos reciben la misma semilla y empiezan el dia 1. */
    public synchronized void iniciar(long semilla, boolean relajado) {
        iniciada = true;
        turno = 1;
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

    /** Para las pruebas: cuantas publicaciones, relaciones y eventos se han sincronizado. */
    public synchronized int[] resumen() {
        return new int[]{publicaciones.size(), relaciones.size(), eventos.size(), turno};
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
