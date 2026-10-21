package alcaldedigital.estructuras;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GRAFO DIRIGIDO DE NIVELES (mapa de la campaña)
 * ==============================================
 * Cada vértice es un nivel y cada arista dirigida A -> B significa
 * "ganar A desbloquea B". Se implementa con LISTAS DE ADYACENCIA
 * (sucesores y predecesores de cada vértice).
 *
 * ¿Qué problema resuelve?
 *   Decidir qué niveles puede jugar el jugador. Un nivel está desbloqueado si
 *   no tiene predecesores (es el inicial) o si ALGUNO de sus predecesores ya fue
 *   ganado. Con eso, al ganar N2 se abren N4 y N5 (su subárbol), pero no N6 ni N7.
 *
 * ¿Por qué grafo y no árbol?
 *   La campaña actual tiene forma de árbol binario (N1 -> N2,N3 -> N4..N7), que es
 *   un caso particular de grafo dirigido acíclico. Modelarlo como grafo permite,
 *   sin cambiar el código, agregar un nivel final al que lleguen VARIAS ramas
 *   (un vértice con dos predecesores), cosa que un árbol no admite.
 *
 * Operaciones: agregar vértice/arista, sucesores, predecesores, BFS, DFS,
 * camino más corto (BFS), profundidad, hojas y lista de adyacencia.
 */
public class GrafoNiveles {

    private final Map<Integer, NodoNivel> vertices = new LinkedHashMap<>();
    private final Map<Integer, List<Integer>> sucesores = new HashMap<>();
    private final Map<Integer, List<Integer>> predecesores = new HashMap<>();

    /** Construye el mapa de la campaña: 7 niveles en forma de árbol binario. */
    public static GrafoNiveles crearCampana() {
        GrafoNiveles g = new GrafoNiveles();
        g.agregarVertice(new NodoNivel(1, "n1.png", 0.50, 0.27));
        g.agregarVertice(new NodoNivel(2, "n2.png", 0.30, 0.50));
        g.agregarVertice(new NodoNivel(3, "n3.png", 0.70, 0.50));
        g.agregarVertice(new NodoNivel(4, "n4.png", 0.17, 0.74));
        g.agregarVertice(new NodoNivel(5, "n5.png", 0.39, 0.74));
        g.agregarVertice(new NodoNivel(6, "n6.png", 0.61, 0.74));
        g.agregarVertice(new NodoNivel(7, "n7.png", 0.83, 0.74));
        g.agregarArista(1, 2);
        g.agregarArista(1, 3);
        g.agregarArista(2, 4);
        g.agregarArista(2, 5);
        g.agregarArista(3, 6);
        g.agregarArista(3, 7);
        return g;
    }

    // ------------------------------------------------------------------
    // Construcción
    // ------------------------------------------------------------------

    public void agregarVertice(NodoNivel nodo) {
        vertices.put(nodo.getNumero(), nodo);
        sucesores.put(nodo.getNumero(), new ArrayList<>());
        predecesores.put(nodo.getNumero(), new ArrayList<>());
    }

    /** Arista dirigida: ganar "origen" desbloquea "destino". */
    public void agregarArista(int origen, int destino) {
        if (!vertices.containsKey(origen) || !vertices.containsKey(destino)) {
            throw new IllegalArgumentException("Vértice inexistente: " + origen + " -> " + destino);
        }
        sucesores.get(origen).add(destino);
        predecesores.get(destino).add(origen);
    }

    // ------------------------------------------------------------------
    // Consultas básicas
    // ------------------------------------------------------------------

    public NodoNivel getNodo(int numero) {
        return vertices.get(numero);
    }

    public List<NodoNivel> getNodos() {
        return new ArrayList<>(vertices.values());
    }

    public List<Integer> sucesores(int n) {
        return Collections.unmodifiableList(sucesores.get(n));
    }

    public List<Integer> predecesores(int n) {
        return Collections.unmodifiableList(predecesores.get(n));
    }

    /** Todas las aristas como pares {origen, destino}, para dibujarlas. */
    public List<int[]> aristas() {
        List<int[]> lista = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> e : sucesores.entrySet()) {
            for (int destino : e.getValue()) {
                lista.add(new int[]{e.getKey(), destino});
            }
        }
        return lista;
    }

    /** Vértices sin predecesores: donde empieza la campaña. */
    public List<Integer> raices() {
        List<Integer> r = new ArrayList<>();
        for (int n : vertices.keySet()) {
            if (predecesores.get(n).isEmpty()) {
                r.add(n);
            }
        }
        return r;
    }

    /** Un nivel es hoja si no desbloquea a nadie: es un final de rama. */
    public boolean esHoja(int n) {
        return sucesores.get(n).isEmpty();
    }

    // ------------------------------------------------------------------
    // Reglas del juego sobre el grafo
    // ------------------------------------------------------------------

    /** Desbloqueado = es raíz, o alguno de sus predecesores ya fue ganado. */
    public boolean estaDesbloqueado(int n) {
        List<Integer> previos = predecesores.get(n);
        if (previos.isEmpty()) {
            return true;
        }
        for (int p : previos) {
            if (vertices.get(p).isCompletado()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Marca un nivel como ganado y devuelve los niveles que se desbloquearon
     * gracias a esta victoria (sus sucesores que antes estaban cerrados).
     */
    public List<Integer> completar(int n, int estrellas, int puntaje) {
        List<Integer> antes = new ArrayList<>();
        for (int s : sucesores.get(n)) {
            if (!estaDesbloqueado(s)) {
                antes.add(s);
            }
        }
        vertices.get(n).registrarVictoria(estrellas, puntaje);
        List<Integer> nuevos = new ArrayList<>();
        for (int s : antes) {
            if (estaDesbloqueado(s)) {
                nuevos.add(s);
            }
        }
        return nuevos;
    }

    // ------------------------------------------------------------------
    // Recorridos
    // ------------------------------------------------------------------

    /** Recorrido en anchura (por niveles de profundidad) usando una cola. */
    public List<Integer> recorridoBFS(int origen) {
        List<Integer> orden = new ArrayList<>();
        Set<Integer> visitados = new HashSet<>();
        Deque<Integer> cola = new ArrayDeque<>();
        cola.add(origen);
        visitados.add(origen);
        while (!cola.isEmpty()) {
            int actual = cola.poll();
            orden.add(actual);
            for (int s : sucesores.get(actual)) {
                if (visitados.add(s)) {
                    cola.add(s);
                }
            }
        }
        return orden;
    }

    /** Recorrido en profundidad (recursivo): baja por cada rama hasta el final. */
    public List<Integer> recorridoDFS(int origen) {
        List<Integer> orden = new ArrayList<>();
        dfs(origen, new HashSet<>(), orden);
        return orden;
    }

    private void dfs(int actual, Set<Integer> visitados, List<Integer> orden) {
        if (!visitados.add(actual)) {
            return;
        }
        orden.add(actual);
        for (int s : sucesores.get(actual)) {
            dfs(s, visitados, orden);
        }
    }

    /**
     * Camino más corto (en número de aristas) entre dos niveles, con BFS y
     * un mapa de "padres". Devuelve lista vacía si no hay camino.
     */
    public List<Integer> camino(int origen, int destino) {
        Map<Integer, Integer> padre = new HashMap<>();
        Deque<Integer> cola = new ArrayDeque<>();
        cola.add(origen);
        padre.put(origen, null);
        while (!cola.isEmpty()) {
            int actual = cola.poll();
            if (actual == destino) {
                break;
            }
            for (int s : sucesores.get(actual)) {
                if (!padre.containsKey(s)) {
                    padre.put(s, actual);
                    cola.add(s);
                }
            }
        }
        List<Integer> ruta = new ArrayList<>();
        if (!padre.containsKey(destino)) {
            return ruta;
        }
        for (Integer v = destino; v != null; v = padre.get(v)) {
            ruta.add(0, v);
        }
        return ruta;
    }

    /** Profundidad = aristas desde la raíz. Se usa para la dificultad del nivel. */
    public int profundidad(int n) {
        int mejor = Integer.MAX_VALUE;
        for (int r : raices()) {
            List<Integer> c = camino(r, n);
            if (!c.isEmpty()) {
                mejor = Math.min(mejor, c.size() - 1);
            }
        }
        return mejor == Integer.MAX_VALUE ? 0 : mejor;
    }

    /** Lista de adyacencia legible, para el panel de estructuras. */
    public String listaAdyacencia() {
        StringBuilder sb = new StringBuilder();
        for (int n : vertices.keySet()) {
            sb.append("N").append(n).append(" -> ");
            List<Integer> s = sucesores.get(n);
            if (s.isEmpty()) {
                sb.append("(hoja: final de rama)");
            }
            for (int i = 0; i < s.size(); i++) {
                sb.append(i == 0 ? "" : ", ").append("N").append(s.get(i));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public int cantidadVertices() {
        return vertices.size();
    }

    public int cantidadAristas() {
        return aristas().size();
    }
}
