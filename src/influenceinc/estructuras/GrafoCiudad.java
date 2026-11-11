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
        zonas.get(a).calles.removeIf(c -> c[0] == b);
        zonas.get(b).calles.removeIf(c -> c[0] == a);
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
        PriorityQueue<int[]> cola = new PriorityQueue<>((a, b) ->
                a[1] != b[1] ? Integer.compare(a[1], b[1]) : Integer.compare(a[0], b[0]));
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

    /** Distancia de un punto al segmento de una calle (para no poner edificios encima). */
    public static double distanciaASegmento(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double t = ((px - ax) * dx + (pz - az) * dz) / (dx * dx + dz * dz);
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
    }
}
