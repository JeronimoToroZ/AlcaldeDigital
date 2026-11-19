package influenceinc.estructuras;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * GRAFO DE LA CIUDAD
 * ==================
 * Grafo NO DIRIGIDO y PONDERADO con listas de adyacencia.
 *   Vertices = lugares de Ciudad Nova (Plaza, Torre Verifica, Alcaldia...).
 *   Aristas  = calles. El peso es el largo de la calle.
 *
 * Problema que resuelve: que los personajes sepan MOVERSE por la ciudad.
 * Cada ciudadano, cada rival de la computadora y el GPS del jugador piden
 * "el camino mas corto de aqui a alla" y lo calcula Dijkstra sobre este grafo.
 * Las calles son las unicas zonas libres de edificios, asi que seguir el camino
 * del grafo es seguir la calle.
 *
 * Es no dirigido porque las calles son de doble sentido para los peatones.
 */
public class GrafoCiudad {

    /** Un lugar de la ciudad. */
    public static class Zona {
        public final int id;
        public final String nombre;
        public final double x, z;
        final List<int[]> calles = new ArrayList<>();   // {vecino, largo}

        Zona(int id, String nombre, double x, double z) {
            this.id = id;
            this.nombre = nombre;
            this.x = x;
            this.z = z;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private final List<Zona> zonas = new ArrayList<>();

    public Zona agregarZona(String nombre, double x, double z) {
        Zona zn = new Zona(zonas.size(), nombre, x, z);
        zonas.add(zn);
        return zn;
    }

    /** Calle de doble sentido; el peso es la distancia en linea recta. */
    public void agregarCalle(int a, int b) {
        Zona za = zonas.get(a), zb = zonas.get(b);
        int largo = (int) Math.round(Math.hypot(za.x - zb.x, za.z - zb.z));
        za.calles.add(new int[]{b, largo});
        zb.calles.add(new int[]{a, largo});
    }

    /** Cierra una calle (en ambos sentidos). */
    public void quitarCalle(int a, int b) {
        quitar(zonas.get(a).calles, b);
        quitar(zonas.get(b).calles, a);
    }

    private static void quitar(List<int[]> calles, int vecino) {
        for (int i = calles.size() - 1; i >= 0; i--) {
            if (calles.get(i)[0] == vecino) {
                calles.remove(i);
            }
        }
    }

    public List<Zona> getZonas() {
        return zonas;
    }

    public Zona zona(int id) {
        return zonas.get(id);
    }

    /** Pares {a, b} de todas las calles, sin repetir. */
    public List<int[]> calles() {
        List<int[]> l = new ArrayList<>();
        for (Zona z : zonas) {
            for (int[] c : z.calles) {
                if (z.id < c[0]) {
                    l.add(new int[]{z.id, c[0]});
                }
            }
        }
        return l;
    }

    public List<Zona> vecinos(Zona z) {
        List<Zona> l = new ArrayList<>();
        for (int[] c : z.calles) {
            l.add(zonas.get(c[0]));
        }
        return l;
    }

    /** La zona mas cercana a un punto (O(V)). */
    public Zona masCercana(double x, double z) {
        Zona mejor = null;
        double d = Double.MAX_VALUE;
        for (Zona zn : zonas) {
            double dd = Math.hypot(zn.x - x, zn.z - z);
            if (dd < d) {
                d = dd;
                mejor = zn;
            }
        }
        return mejor;
    }

    /**
     * DIJKSTRA: camino mas corto entre dos zonas.
     * Complejidad O((V + E) log V) con cola de prioridad.
     * @return las zonas del camino, de origen a destino (vacia si no hay)
     */
    public List<Zona> caminoMasCorto(int origen, int destino) {
        int n = zonas.size();
        int[] dist = new int[n];
        int[] previo = new int[n];
        boolean[] cerrado = new boolean[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(previo, -1);
        dist[origen] = 0;
        PriorityQueue<int[]> cola = new PriorityQueue<>(new java.util.Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return a[1] != b[1] ? Integer.compare(a[1], b[1]) : Integer.compare(a[0], b[0]);
            }
        });
        cola.add(new int[]{origen, 0});
        while (!cola.isEmpty()) {
            int u = cola.poll()[0];
            if (cerrado[u]) {
                continue;
            }
            cerrado[u] = true;
            if (u == destino) {
                break;
            }
            for (int[] c : zonas.get(u).calles) {
                int v = c[0];
                if (!cerrado[v] && dist[u] + c[1] < dist[v]) {     // relajacion
                    dist[v] = dist[u] + c[1];
                    previo[v] = u;
                    cola.add(new int[]{v, dist[v]});
                }
            }
        }
        List<Zona> camino = new ArrayList<>();
        if (dist[destino] == Integer.MAX_VALUE) {
            return camino;
        }
        for (int v = destino; v != -1; v = previo[v]) {
            camino.add(zonas.get(v));
        }
        Collections.reverse(camino);
        return camino;
    }

    /** Largo total de un camino. */
    public int largo(List<Zona> camino) {
        int t = 0;
        for (int i = 1; i < camino.size(); i++) {
            Zona a = camino.get(i - 1), b = camino.get(i);
            t += (int) Math.round(Math.hypot(a.x - b.x, a.z - b.z));
        }
        return t;
    }

    /** BFS: a cuantas calles de distancia esta cada zona del origen. */
    public int[] saltosDesde(int origen) {
        int[] d = new int[zonas.size()];
        Arrays.fill(d, -1);
        Queue<Integer> cola = new ArrayDeque<>();
        d[origen] = 0;
        cola.add(origen);
        while (!cola.isEmpty()) {
            int u = cola.poll();
            for (int[] c : zonas.get(u).calles) {
                if (d[c[0]] < 0) {
                    d[c[0]] = d[u] + 1;
                    cola.add(c[0]);
                }
            }
        }
        return d;
    }

    /** Largo de la calle a-b (o -1 si no existe). */
    public int largoCalle(int a, int b) {
        for (int[] c : zonas.get(a).calles) {
            if (c[0] == b) {
                return c[1];
            }
        }
        return -1;
    }

    // ==================================================================
    // FLOYD-WARSHALL: distancias entre TODOS los pares
    // ==================================================================

    /** Valor que representa "no hay camino" en la matriz. */
    public static final int INF = Integer.MAX_VALUE / 4;

    /**
     * FLOYD-WARSHALL. dist[i][j] = largo del camino mas corto de i a j.
     * Idea: para cada zona k, se pregunta si pasar por k acorta el camino de i a j.
     * Complejidad O(V^3); con 8 zonas son 512 pasos, y se calcula UNA vez.
     * Lo usan la app Mapa (minutos a cada barrio) y el noticiero (el centro de la ciudad).
     */
    public int[][] floydWarshall() {
        int n = zonas.size();
        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(d[i], INF);
            d[i][i] = 0;
            for (int[] c : zonas.get(i).calles) {
                d[i][c[0]] = Math.min(d[i][c[0]], c[1]);
            }
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (d[i][k] + d[k][j] < d[i][j]) {
                        d[i][j] = d[i][k] + d[k][j];
                    }
                }
            }
        }
        return d;
    }

    /**
     * El CENTRO del grafo: la zona cuya distancia a la zona mas lejana es la menor
     * (minima excentricidad). Sale de la matriz de Floyd-Warshall.
     */
    public static int centro(int[][] d) {
        int mejor = 0, menor = INF;
        for (int i = 0; i < d.length; i++) {
            int exc = 0;
            for (int j = 0; j < d.length; j++) {
                exc = Math.max(exc, d[i][j]);
            }
            if (exc < menor) {
                menor = exc;
                mejor = i;
            }
        }
        return mejor;
    }

    // ==================================================================
    // ARBOL DE EXPANSION MINIMA: Kruskal y Prim
    // ==================================================================

    /** Conjuntos disjuntos (union-find) para Kruskal, con compresion de caminos. */
    private static int raizDe(int[] padre, int x) {
        while (padre[x] != x) {
            padre[x] = padre[padre[x]];
            x = padre[x];
        }
        return x;
    }

    /**
     * KRUSKAL: se ordenan TODAS las calles de la mas corta a la mas larga y se
     * van tomando, saltando las que cerrarian un ciclo (las dos puntas ya estan
     * en el mismo conjunto). O(E log E).
     * En el juego es la RED DE ALERTA TEMPRANA: el cable mas corto que conecta
     * todos los barrios. Cuando aciertas sobre un barrio, la calma viaja por ella.
     * @return las calles escogidas como {a, b, largo}
     */
    public List<int[]> kruskal() {
        List<int[]> todas = new ArrayList<>();
        for (int[] c : calles()) {
            todas.add(new int[]{c[0], c[1], largoCalle(c[0], c[1])});
        }
        // ordenar por largo (insercion: son pocas calles)
        Ordenamientos.insercion(todas, new java.util.Comparator<int[]>() {
            @Override
            public int compare(int[] a, int[] b) {
                return a[2] != b[2] ? Integer.compare(a[2], b[2]) : Integer.compare(a[0] * 100 + a[1], b[0] * 100 + b[1]);
            }
        });
        int[] padre = new int[zonas.size()];
        for (int i = 0; i < padre.length; i++) {
            padre[i] = i;
        }
        List<int[]> arbol = new ArrayList<>();
        for (int[] c : todas) {
            int ra = raizDe(padre, c[0]), rb = raizDe(padre, c[1]);
            if (ra != rb) {
                padre[ra] = rb;              // union
                arbol.add(c);
                if (arbol.size() == zonas.size() - 1) {
                    break;
                }
            }
        }
        return arbol;
    }

    /**
     * PRIM: el arbol crece desde una zona; en cada paso se agrega la calle mas
     * corta que sale del arbol hacia una zona nueva. O(V^2) con arreglos
     * (suficiente para 8 zonas).
     * En el juego es la COBERTURA DEL NOTICIERO: la senal en vivo sale del
     * estudio y llega a los barrios en el orden en que Prim los agrega.
     * @return las calles en el orden en que se agregaron, como {desde, hasta, largo}
     */
    public List<int[]> prim(int inicio) {
        int n = zonas.size();
        boolean[] dentro = new boolean[n];
        int[] mejor = new int[n];
        int[] desde = new int[n];
        Arrays.fill(mejor, INF);
        Arrays.fill(desde, -1);
        mejor[inicio] = 0;
        List<int[]> arbol = new ArrayList<>();
        for (int paso = 0; paso < n; paso++) {
            int u = -1;
            for (int v = 0; v < n; v++) {
                if (!dentro[v] && (u == -1 || mejor[v] < mejor[u])) {
                    u = v;
                }
            }
            if (u == -1 || mejor[u] == INF) {
                break;                        // grafo no conexo
            }
            dentro[u] = true;
            if (desde[u] >= 0) {
                arbol.add(new int[]{desde[u], u, mejor[u]});
            }
            for (int[] c : zonas.get(u).calles) {
                if (!dentro[c[0]] && c[1] < mejor[c[0]]) {
                    mejor[c[0]] = c[1];
                    desde[c[0]] = u;
                }
            }
        }
        return arbol;
    }

    /** Suma de los largos de un arbol de expansion. */
    public static int pesoTotal(List<int[]> arbol) {
        int t = 0;
        for (int[] c : arbol) {
            t += c[2];
        }
        return t;
    }

    // ==================================================================
    // FLUJO MAXIMO: Ford-Fulkerson
    // ==================================================================

    /**
     * FORD-FULKERSON sobre una matriz de capacidades: mientras exista un camino
     * de s a t con capacidad sobrante en el grafo residual (se busca con DFS),
     * se empuja por el la capacidad minima del camino.
     * La matriz NO se modifica (se trabaja sobre una copia).
     * En el juego: cuantos votantes por hora pueden llegar a las urnas el dia
     * de la eleccion por las calles de la ciudad.
     */
    public static int flujoMaximo(int[][] capacidad, int s, int t) {
        int n = capacidad.length;
        int[][] residual = new int[n][n];
        for (int i = 0; i < n; i++) {
            residual[i] = Arrays.copyOf(capacidad[i], n);
        }
        int total = 0;
        int[] previo = new int[n];
        while (caminoDFS(residual, s, t, previo)) {
            int cuello = INF;
            for (int v = t; v != s; v = previo[v]) {
                cuello = Math.min(cuello, residual[previo[v]][v]);
            }
            for (int v = t; v != s; v = previo[v]) {
                residual[previo[v]][v] -= cuello;
                residual[v][previo[v]] += cuello;   // arista de regreso
            }
            total += cuello;
        }
        return total;
    }

    /** DFS iterativo (con pila) que busca un camino aumentante de s a t. */
    private static boolean caminoDFS(int[][] r, int s, int t, int[] previo) {
        int n = r.length;
        boolean[] visto = new boolean[n];
        Arrays.fill(previo, -1);
        java.util.Deque<Integer> pila = new ArrayDeque<>();
        pila.push(s);
        visto[s] = true;
        while (!pila.isEmpty()) {
            int u = pila.pop();
            if (u == t) {
                return true;
            }
            for (int v = n - 1; v >= 0; v--) {
                if (!visto[v] && r[u][v] > 0) {
                    visto[v] = true;
                    previo[v] = u;
                    pila.push(v);
                }
            }
        }
        return false;
    }

    /** Distancia de un punto al segmento de una calle (para no poner edificios encima). */
    public static double distanciaASegmento(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double t = ((px - ax) * dx + (pz - az) * dz) / (dx * dx + dz * dz);
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
    }
}
