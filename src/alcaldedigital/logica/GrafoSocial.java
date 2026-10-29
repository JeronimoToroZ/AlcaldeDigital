package alcaldedigital.logica;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Grafo NO DIRIGIDO y PONDERADO de la ciudad, representado con LISTA DE ADYACENCIA
 * (cada NodoRed guarda su propia lista de vecinos).
 *
 * Se eligio lista y no matriz de adyacencia porque el grafo es disperso: 8 nodos
 * y 8 aristas, cuando una matriz necesitaria 8x8 = 64 casillas y 56 estarian
 * vacias. Ademas la operacion mas frecuente del juego es "recorrer los vecinos
 * de un nodo", que en lista es O(grado) y en matriz seria O(V).
 *
 * Implementa tres algoritmos:
 *   - BFS  -> propagacion del rumor por oleadas
 *   - DFS  -> deteccion de componentes conexas (camaras de eco)
 *   - Dijkstra -> ruta mas barata para una campana de verificacion
 *
 * @author Naty
 */
public class GrafoSocial {

    public List<NodoRed> nodos;

    public GrafoSocial() {
        this.nodos = new ArrayList<>();
    }

    public void agregarNodo(NodoRed nodo) {
        nodos.add(nodo);
    }

    /** Busca un nodo por su nombre. Devuelve null si no existe. */
    public NodoRed buscar(String nombre) {
        for (NodoRed n : nodos) {
            if (n.nombre.equalsIgnoreCase(nombre)) {
                return n;
            }
        }
        return null;
    }

    /** Todos los nodos que ahora mismo tienen el rumor. */
    public List<NodoRed> infectados() {
        List<NodoRed> lista = new ArrayList<>();
        for (NodoRed n : nodos) {
            if (n.estaInfectado) {
                lista.add(n);
            }
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // Construccion del mapa
    // ------------------------------------------------------------------

    /**
     * Coordenadas ajustadas sobre los nombres escritos en el mapa de la ciudad.
     * Los pesos representan lo que cuesta llevar una campana de verificacion
     * por ese tramo.
     */
    public void inicializarMapaPrueba() {
        NodoRed parque     = new NodoRed("Parque", 230, 215);
        NodoRed hospital   = new NodoRed("Hospital", 460, 160);
        NodoRed biblioteca = new NodoRed("Biblioteca", 655, 220);
        NodoRed estacion   = new NodoRed("Estación", 250, 495);
        NodoRed alcaldia   = new NodoRed("Alcaldía", 515, 550);
        NodoRed teatro     = new NodoRed("Teatro", 770, 490);
        NodoRed mercado    = new NodoRed("Mercado", 360, 750);
        NodoRed cafe       = new NodoRed("Café", 705, 745);

        agregarNodo(parque);
        agregarNodo(hospital);
        agregarNodo(biblioteca);
        agregarNodo(estacion);
        agregarNodo(alcaldia);
        agregarNodo(teatro);
        agregarNodo(mercado);
        agregarNodo(cafe);

        // Aristas con peso (costo de la campana de verificacion)
        alcaldia.agregarConexion(hospital, 4);
        alcaldia.agregarConexion(estacion, 3);
        alcaldia.agregarConexion(teatro, 5);
        parque.agregarConexion(estacion, 2);
        biblioteca.agregarConexion(teatro, 3);
        hospital.agregarConexion(biblioteca, 6);
        estacion.agregarConexion(mercado, 4);
        teatro.agregarConexion(cafe, 2);

        // Paciente cero
        parque.estaInfectado = true;
    }

    // ------------------------------------------------------------------
    // BFS - propagacion del rumor
    // ------------------------------------------------------------------

    /**
     * RECORRIDO EN ANCHURA (BFS) desde un nodo origen.
     *
     * Devuelve las OLEADAS: la posicion 0 es el origen, la 1 son sus vecinos
     * directos, la 2 los vecinos de esos, y asi. Es exactamente como se propaga
     * un rumor en la vida real: primero los que estan al lado, despues los que
     * estan a dos pasos.
     *
     * NO modifica el estado del juego; solo calcula y devuelve el recorrido.
     * Complejidad O(V + E).
     *
     * @param origen nodo desde donde arranca el rumor
     * @return lista de oleadas; vacia si el origen es null
     */
    public List<List<NodoRed>> propagarRumorBFS(NodoRed origen) {
        List<List<NodoRed>> oleadas = new ArrayList<>();
        if (origen == null) {
            return oleadas;
        }

        Set<NodoRed> visitados = new HashSet<>();   // para no volver a pasar por un nodo
        Queue<NodoRed> cola = new ArrayDeque<>();   // FIFO: lo que hace que esto sea BFS

        cola.add(origen);
        visitados.add(origen);

        while (!cola.isEmpty()) {
            int tamanoNivel = cola.size();          // cuantos nodos hay en esta oleada
            List<NodoRed> oleadaActual = new ArrayList<>();

            for (int i = 0; i < tamanoNivel; i++) {
                NodoRed actual = cola.poll();
                oleadaActual.add(actual);

                for (NodoRed vecino : actual.vecinos) {
                    if (!visitados.contains(vecino)) {
                        visitados.add(vecino);      // se marca al ENCOLAR, no al sacar
                        cola.add(vecino);
                    }
                }
            }
            oleadas.add(oleadaActual);
        }
        return oleadas;
    }

    /**
     * Avanza la propagacion UNA oleada: contagia de golpe a todos los vecinos
     * sanos de los nodos ya infectados.
     *
     * Es el mismo BFS de arriba pero ejecutado un nivel por llamada, para que
     * el Timer de la interfaz pueda animarlo. La frontera actual son los
     * infectados; sus vecinos sanos son el siguiente nivel.
     *
     * @return los nodos que se infectaron en esta oleada (vacia si ya no queda nadie)
     */
    public List<NodoRed> expandirUnaOleada() {
        Queue<NodoRed> frontera = new ArrayDeque<>(infectados());
        List<NodoRed> nuevos = new ArrayList<>();
        Set<NodoRed> yaEnLaOleada = new HashSet<>();

        while (!frontera.isEmpty()) {
            NodoRed actual = frontera.poll();
            for (NodoRed vecino : actual.vecinos) {
                if (!vecino.estaInfectado && !yaEnLaOleada.contains(vecino)) {
                    yaEnLaOleada.add(vecino);
                    nuevos.add(vecino);
                }
            }
        }

        for (NodoRed n : nuevos) {
            n.estaInfectado = true;
        }
        return nuevos;
    }

    // ------------------------------------------------------------------
    // DFS - componentes conexas (camaras de eco)
    // ------------------------------------------------------------------

    /**
     * RECORRIDO EN PROFUNDIDAD (DFS) con pila explicita, repetido hasta cubrir
     * todos los nodos.
     *
     * Cada grupo devuelto es una COMPONENTE CONEXA: un conjunto de nodos que se
     * alcanzan entre si pero no tienen camino hacia el resto. En el juego eso es
     * una camara de eco: un barrio aislado donde la desinformacion se queda dando
     * vueltas sin que nadie de afuera pueda desmentirla.
     *
     * Si devuelve una sola lista, el grafo es conexo.
     * Complejidad O(V + E).
     */
    public List<List<NodoRed>> componentesConexas() {
        List<List<NodoRed>> componentes = new ArrayList<>();
        Set<NodoRed> visitados = new HashSet<>();

        for (NodoRed inicio : nodos) {
            if (visitados.contains(inicio)) {
                continue;
            }

            List<NodoRed> componente = new ArrayList<>();
            Deque<NodoRed> pila = new ArrayDeque<>();   // LIFO: esto lo hace DFS

            pila.push(inicio);
            visitados.add(inicio);

            while (!pila.isEmpty()) {
                NodoRed actual = pila.pop();
                componente.add(actual);

                for (NodoRed vecino : actual.vecinos) {
                    if (!visitados.contains(vecino)) {
                        visitados.add(vecino);
                        pila.push(vecino);
                    }
                }
            }
            componentes.add(componente);
        }
        return componentes;
    }

    /** true si existe camino entre cualquier par de nodos. */
    public boolean esConexo() {
        return componentesConexas().size() <= 1;
    }

    // ------------------------------------------------------------------
    // Dijkstra - campana de verificacion
    // ------------------------------------------------------------------

    /** Resultado de Dijkstra: la ruta completa y lo que cuesta recorrerla. */
    public static class Ruta {
        public final List<NodoRed> camino;
        public final int costo;

        public Ruta(List<NodoRed> camino, int costo) {
            this.camino = camino;
            this.costo = costo;
        }

        public boolean existe() {
            return !camino.isEmpty();
        }

        @Override
        public String toString() {
            if (!existe()) {
                return "sin ruta";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < camino.size(); i++) {
                if (i > 0) {
                    sb.append(" -> ");
                }
                sb.append(camino.get(i).nombre);
            }
            sb.append("  (costo ").append(costo).append(")");
            return sb.toString();
        }
    }

    /**
     * DIJKSTRA: ruta de costo minimo entre dos nodos.
     *
     * Se usa para mandar una campana de verificacion desde la Alcaldia hasta un
     * punto infectado gastando lo menos posible.
     *
     * Funciona solo con pesos positivos: el algoritmo da por cerrado un nodo
     * apenas lo saca de la cola de prioridad, y un peso negativo podria abaratar
     * ese camino despues, cuando ya es tarde para corregirlo. Para pesos
     * negativos haria falta Bellman-Ford.
     *
     * Complejidad O((V + E) log V) con cola de prioridad.
     *
     * @return la Ruta; con camino vacio y costo Integer.MAX_VALUE si no hay camino
     */
    public Ruta caminoMinimo(NodoRed origen, NodoRed destino) {
        if (origen == null || destino == null) {
            return new Ruta(new ArrayList<>(), Integer.MAX_VALUE);
        }

        Map<NodoRed, Integer> distancia = new HashMap<>();
        Map<NodoRed, NodoRed> anterior = new HashMap<>();
        Set<NodoRed> cerrados = new HashSet<>();

        for (NodoRed n : nodos) {
            distancia.put(n, Integer.MAX_VALUE);
        }
        distancia.put(origen, 0);

        PriorityQueue<NodoRed> cola =
                new PriorityQueue<>((a, b) -> Integer.compare(distancia.get(a), distancia.get(b)));
        cola.add(origen);

        while (!cola.isEmpty()) {
            NodoRed actual = cola.poll();

            if (!cerrados.add(actual)) {
                continue;               // ya estaba cerrado: entrada vieja de la cola
            }
            if (actual == destino) {
                break;                  // llegamos; lo demas ya no importa
            }

            for (NodoRed vecino : actual.vecinos) {
                if (cerrados.contains(vecino)) {
                    continue;
                }
                int peso = actual.pesoHacia(vecino);
                if (peso == Integer.MAX_VALUE || distancia.get(actual) == Integer.MAX_VALUE) {
                    continue;
                }

                int nuevaDistancia = distancia.get(actual) + peso;

                // RELAJACION de la arista
                if (nuevaDistancia < distancia.get(vecino)) {
                    distancia.put(vecino, nuevaDistancia);
                    anterior.put(vecino, actual);
                    cola.add(vecino);
                }
            }
        }

        if (distancia.get(destino) == Integer.MAX_VALUE) {
            return new Ruta(new ArrayList<>(), Integer.MAX_VALUE);
        }

        // Reconstruccion del camino hacia atras, desde el destino
        List<NodoRed> camino = new ArrayList<>();
        for (NodoRed paso = destino; paso != null; paso = anterior.get(paso)) {
            camino.add(paso);
        }
        Collections.reverse(camino);

        return new Ruta(camino, distancia.get(destino));
    }

    /**
     * Ruta mas barata desde un origen hasta el punto infectado mas cercano.
     * Es lo que usa el juego cuando el jugador acierta: la campana sale de la
     * Alcaldia y apaga el foco que mas a la mano queda.
     *
     * @return la Ruta hasta ese infectado, o una Ruta vacia si no hay ninguno alcanzable
     */
    public Ruta rutaAlInfectadoMasCercano(NodoRed origen) {
        Ruta mejor = new Ruta(new ArrayList<>(), Integer.MAX_VALUE);
        for (NodoRed objetivo : infectados()) {
            if (objetivo == origen) {
                continue;
            }
            Ruta r = caminoMinimo(origen, objetivo);
            if (r.existe() && r.costo < mejor.costo) {
                mejor = r;
            }
        }
        return mejor;
    }
}
