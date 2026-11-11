package influenceinc.estructuras;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * GRAFO SOCIAL DE CIVITAS
 * =======================
 * Grafo DIRIGIDO y PONDERADO representado con LISTAS DE ADYACENCIA.
 *
 * Una arista A -> B significa "lo que publica A le llega a B" (B sigue a A).
 * Es dirigido porque en una red social seguir no es reciproco: miles de
 * personas siguen a un influencer, pero el influencer no los sigue a ellos.
 *
 * Por que lista y no matriz: el grafo es disperso (unas 40 cuentas y unas 120
 * aristas). Una matriz necesitaria 40 x 40 = 1.600 casillas casi todas vacias.
 * Ademas la operacion que mas se repite es "recorrer los vecinos de una
 * cuenta", que en lista cuesta O(grado) y en matriz O(V).
 *
 * El orden de insercion se conserva (LinkedHashMap y ArrayList) para que los
 * recorridos sean DETERMINISTAS: con la misma semilla, dos computadores
 * obtienen exactamente el mismo resultado. Eso es lo que permite el
 * multijugador sincronizado.
 *
 * Operaciones:
 *   agregarVertice, eliminarVertice, conectar, desconectar, vecinos,
 *   bfsOleadas (BFS), dfsAlcanzables y comunidades (DFS), caminoMinimo
 *   (Dijkstra), propagar (BFS probabilistico) y corregir (BFS con tope).
 *
 * Basado en el GrafoSocial de la segunda entrega (BFS por oleadas, DFS de
 * componentes y Dijkstra), ampliado a grafo dirigido y a posiciones 3D.
 */
public class GrafoRed {

    private final Map<String, Vertice> vertices = new LinkedHashMap<>();

    // ==================================================================
    // Operaciones basicas
    // ==================================================================

    /** Inserta una cuenta. Lanza excepcion si el id ya existe. O(1). */
    public void agregarVertice(Vertice v) {
        if (vertices.containsKey(v.id)) {
            throw new IllegalArgumentException("Ya existe una cuenta con id " + v.id);
        }
        vertices.put(v.id, v);
    }

    /**
     * Elimina una cuenta y TODAS sus aristas (las que salen y las que entran).
     * En el juego: cuando se reporta un bulo, la cuenta falsa que lo origino se
     * suspende y desaparece de la red. O(V + E) en el peor caso.
     *
     * @return el vertice eliminado, o null si no existia
     */
    public Vertice eliminarVertice(String id) {
        Vertice v = vertices.remove(id);
        if (v == null) {
            return null;
        }
        for (Arista a : new ArrayList<>(v.salientes)) {
            a.destino.entrantes.remove(a);
        }
        for (Arista a : new ArrayList<>(v.entrantes)) {
            a.origen.salientes.remove(a);
        }
        v.salientes.clear();
        v.entrantes.clear();
        return v;
    }

    /**
     * Crea (o actualiza) la arista dirigida origen -> destino.
     * En el juego: cuando una agencia gana un seguidor se crea agencia -> usuario.
     */
    public Arista conectar(Vertice origen, Vertice destino, double probabilidad, int costo) {
        if (origen == null || destino == null || origen == destino) {
            return null;
        }
        Arista existente = origen.aristaHacia(destino);
        if (existente != null) {
            existente.probabilidad = probabilidad;
            existente.costo = costo;
            return existente;
        }
        Arista a = new Arista(origen, destino, probabilidad, costo);
        origen.salientes.add(a);
        destino.entrantes.add(a);
        return a;
    }

    /** Amistad: dos aristas, una en cada sentido. */
    public void conectarMutuo(Vertice a, Vertice b, double probabilidad, int costo) {
        conectar(a, b, probabilidad, costo);
        conectar(b, a, probabilidad, costo);
    }

    /**
     * Elimina la arista origen -> destino.
     * En el juego: cuando otra agencia te "roba" un seguidor, se borra tu arista.
     *
     * @return true si existia
     */
    public boolean desconectar(Vertice origen, Vertice destino) {
        Arista a = origen.aristaHacia(destino);
        if (a == null) {
            return false;
        }
        origen.salientes.remove(a);
        destino.entrantes.remove(a);
        return true;
    }

    public Vertice buscar(String id) {
        return vertices.get(id);
    }

    public boolean contiene(String id) {
        return vertices.containsKey(id);
    }

    /** Todas las cuentas, en orden de insercion. */
    public List<Vertice> getVertices() {
        return new ArrayList<>(vertices.values());
    }

    public int cantidadVertices() {
        return vertices.size();
    }

    public int cantidadAristas() {
        int total = 0;
        for (Vertice v : vertices.values()) {
            total += v.salientes.size();
        }
        return total;
    }

    /** Todas las aristas, para dibujarlas. */
    public List<Arista> getAristas() {
        List<Arista> lista = new ArrayList<>();
        for (Vertice v : vertices.values()) {
            lista.addAll(v.salientes);
        }
        return lista;
    }

    /** Vecinos a los que les llega lo que publica v. */
    public List<Vertice> vecinosSalida(Vertice v) {
        List<Vertice> lista = new ArrayList<>();
        for (Arista a : v.salientes) {
            lista.add(a.destino);
        }
        return lista;
    }

    /**
     * Vecinos ignorando la direccion (salida + entrada, sin repetir).
     * Sirve para las comunidades y para mover equipos de verificacion.
     */
    public List<Vertice> vecinosNoDirigidos(Vertice v) {
        List<Vertice> lista = new ArrayList<>();
        Set<Vertice> vistos = new HashSet<>();
        for (Arista a : v.salientes) {
            if (vistos.add(a.destino)) {
                lista.add(a.destino);
            }
        }
        for (Arista a : v.entrantes) {
            if (vistos.add(a.origen)) {
                lista.add(a.origen);
            }
        }
        return lista;
    }

    /** Costo del tramo entre dos vecinos, en cualquier sentido. */
    private int costoEntre(Vertice a, Vertice b) {
        Arista ab = a.aristaHacia(b);
        Arista ba = b.aristaHacia(a);
        if (ab != null && ba != null) {
            return Math.min(ab.costo, ba.costo);
        }
        if (ab != null) {
            return ab.costo;
        }
        if (ba != null) {
            return ba.costo;
        }
        return Integer.MAX_VALUE;
    }

    // ==================================================================
    // BFS - alcance por oleadas
    // ==================================================================

    /**
     * RECORRIDO EN ANCHURA siguiendo la direccion de las aristas.
     *
     * Devuelve las OLEADAS: la posicion 0 es el origen, la 1 son las cuentas a
     * las que les llega directamente, la 2 las que estan a dos saltos, etc.
     * Es exactamente como viaja una publicacion.
     *
     * No modifica nada. Complejidad O(V + E).
     *
     * @param maxNiveles cuantos saltos como maximo (-1 = sin limite)
     */
    public List<List<Vertice>> bfsOleadas(Vertice origen, int maxNiveles) {
        List<List<Vertice>> oleadas = new ArrayList<>();
        if (origen == null) {
            return oleadas;
        }
        Set<Vertice> visitados = new HashSet<>();
        Queue<Vertice> cola = new ArrayDeque<>();   // FIFO: esto es lo que lo hace BFS
        cola.add(origen);
        visitados.add(origen);
        int nivel = 0;

        while (!cola.isEmpty() && (maxNiveles < 0 || nivel <= maxNiveles)) {
            int tamanoNivel = cola.size();
            List<Vertice> oleada = new ArrayList<>();
            for (int i = 0; i < tamanoNivel; i++) {
                Vertice actual = cola.poll();
                oleada.add(actual);
                for (Arista a : actual.salientes) {
                    if (visitados.add(a.destino)) { // se marca al encolar, no al sacar
                        cola.add(a.destino);
                    }
                }
            }
            oleadas.add(oleada);
            nivel++;
        }
        return oleadas;
    }

    /** Cuantas cuentas alcanza el origen en a lo sumo maxNiveles saltos (sin contarse). */
    public int alcanceBFS(Vertice origen, int maxNiveles) {
        int total = 0;
        for (List<Vertice> oleada : bfsOleadas(origen, maxNiveles)) {
            total += oleada.size();
        }
        return Math.max(0, total - 1);
    }

    // ==================================================================
    // DFS - audiencia potencial y comunidades
    // ==================================================================

    /**
     * RECORRIDO EN PROFUNDIDAD con pila explicita, siguiendo las aristas.
     *
     * Devuelve todas las cuentas a las que, tarde o temprano, PODRIA llegar algo
     * publicado desde el origen. En el juego es la "audiencia potencial" que se
     * muestra al elegir a que influencer contratar.
     *
     * Complejidad O(V + E).
     */
    public List<Vertice> dfsAlcanzables(Vertice origen) {
        List<Vertice> orden = new ArrayList<>();
        if (origen == null) {
            return orden;
        }
        Set<Vertice> visitados = new HashSet<>();
        Deque<Vertice> pila = new ArrayDeque<>();   // LIFO: esto lo hace DFS
        pila.push(origen);

        while (!pila.isEmpty()) {
            Vertice actual = pila.pop();
            if (!visitados.add(actual)) {
                continue;
            }
            orden.add(actual);
            // Se apilan al reves para visitar los vecinos en su orden natural
            List<Arista> sal = actual.salientes;
            for (int i = sal.size() - 1; i >= 0; i--) {
                Vertice vecino = sal.get(i).destino;
                if (!visitados.contains(vecino)) {
                    pila.push(vecino);
                }
            }
        }
        return orden;
    }

    /**
     * COMPONENTES CONEXAS (debiles) con DFS, ignorando la direccion.
     *
     * Cada grupo es una comunidad aislada: un conjunto de cuentas que se
     * comunican entre si pero no con el resto. En el juego aparecen cuando se
     * suspenden cuentas y la red se parte: son camaras de eco donde un bulo se
     * queda dando vueltas. Si devuelve un solo grupo, la red esta conectada.
     */
    public List<List<Vertice>> comunidades() {
        List<List<Vertice>> grupos = new ArrayList<>();
        Set<Vertice> visitados = new HashSet<>();

        for (Vertice inicio : vertices.values()) {
            if (visitados.contains(inicio)) {
                continue;
            }
            List<Vertice> grupo = new ArrayList<>();
            Deque<Vertice> pila = new ArrayDeque<>();
            pila.push(inicio);
            visitados.add(inicio);
            while (!pila.isEmpty()) {
                Vertice actual = pila.pop();
                grupo.add(actual);
                for (Vertice vecino : vecinosNoDirigidos(actual)) {
                    if (visitados.add(vecino)) {
                        pila.push(vecino);
                    }
                }
            }
            grupos.add(grupo);
        }
        return grupos;
    }

    // ==================================================================
    // Dijkstra - ruta mas barata para un equipo de verificacion
    // ==================================================================

    /** Resultado de Dijkstra: el camino y su costo total. */
    public static class Ruta {
        public final List<Vertice> camino;
        public final int costo;

        public Ruta(List<Vertice> camino, int costo) {
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
            return sb.append("  (costo ").append(costo).append(')').toString();
        }
    }

    /**
     * DIJKSTRA: ruta de costo minimo entre dos cuentas.
     *
     * El equipo de verificacion se mueve por la red sin importar la direccion
     * de las aristas (puede escribirle a quien sigue y a quien lo sigue), por eso
     * aqui se usan los vecinos no dirigidos.
     *
     * Solo funciona con costos positivos: Dijkstra da por cerrado un vertice
     * apenas lo saca de la cola, y un costo negativo podria abaratarlo despues.
     * Los empates se rompen por id para que el resultado sea determinista.
     *
     * Complejidad O((V + E) log V).
     */
    public Ruta caminoMinimo(Vertice origen, Vertice destino) {
        if (origen == null || destino == null) {
            return new Ruta(new ArrayList<Vertice>(), Integer.MAX_VALUE);
        }
        final Map<Vertice, Integer> distancia = new HashMap<>();
        Map<Vertice, Vertice> anterior = new HashMap<>();
        Set<Vertice> cerrados = new HashSet<>();
        for (Vertice v : vertices.values()) {
            distancia.put(v, Integer.MAX_VALUE);
        }
        distancia.put(origen, 0);

        PriorityQueue<Vertice> cola = new PriorityQueue<>((a, b) -> {
            int c = Integer.compare(distancia.get(a), distancia.get(b));
            return c != 0 ? c : a.id.compareTo(b.id);
        });
        cola.add(origen);

        while (!cola.isEmpty()) {
            Vertice actual = cola.poll();
            if (!cerrados.add(actual)) {
                continue;               // entrada vieja de la cola
            }
            if (actual == destino) {
                break;
            }
            for (Vertice vecino : vecinosNoDirigidos(actual)) {
                if (cerrados.contains(vecino)) {
                    continue;
                }
                int peso = costoEntre(actual, vecino);
                int nueva = distancia.get(actual) + peso;
                if (nueva < distancia.get(vecino)) {   // RELAJACION
                    distancia.put(vecino, nueva);
                    anterior.put(vecino, actual);
                    cola.add(vecino);
                }
            }
        }

        if (distancia.get(destino) == Integer.MAX_VALUE) {
            return new Ruta(new ArrayList<Vertice>(), Integer.MAX_VALUE);
        }
        List<Vertice> camino = new ArrayList<>();
        for (Vertice paso = destino; paso != null; paso = anterior.get(paso)) {
            camino.add(paso);
        }
        Collections.reverse(camino);
        return new Ruta(camino, distancia.get(destino));
    }

    /** Ruta mas barata desde el origen hasta la cuenta desinformada mas cercana. */
    public Ruta rutaAlDesinformadoMasCercano(Vertice origen) {
        Ruta mejor = new Ruta(new ArrayList<Vertice>(), Integer.MAX_VALUE);
        for (Vertice objetivo : vertices.values()) {
            if (!objetivo.desinformado || objetivo == origen) {
                continue;
            }
            Ruta r = caminoMinimo(origen, objetivo);
            if (r.existe() && r.costo < mejor.costo) {
                mejor = r;
            }
        }
        return mejor;
    }

    // ==================================================================
    // Propagacion: BFS probabilistico
    // ==================================================================

    /** Lo que hizo una propagacion: quien recibio y por que arista, oleada por oleada. */
    public static class ResultadoPropagacion {
        /** oleadas.get(0) = [origen]; oleadas.get(k) = quienes la recibieron en el salto k. */
        public final List<List<Vertice>> oleadas = new ArrayList<>();
        /** aristas.get(k) = por donde viajo la publicacion para llegar a la oleada k. */
        public final List<List<Arista>> aristas = new ArrayList<>();

        /** Total de cuentas alcanzadas, sin contar el origen. */
        public int alcance() {
            int total = 0;
            for (int i = 1; i < oleadas.size(); i++) {
                total += oleadas.get(i).size();
            }
            return total;
        }

        /** Todas las cuentas alcanzadas, sin el origen. */
        public List<Vertice> alcanzados() {
            List<Vertice> lista = new ArrayList<>();
            for (int i = 1; i < oleadas.size(); i++) {
                lista.addAll(oleadas.get(i));
            }
            return lista;
        }
    }

    /**
     * BFS PROBABILISTICO: simula como viaja una publicacion.
     *
     * Es el mismo BFS por oleadas, con una diferencia: cada arista solo se
     * "cruza" si una tirada aleatoria sale menor que su probabilidad. Asi la
     * misma campana no llega siempre a la misma gente, pero las cuentas mejor
     * conectadas y con aristas mas fuertes llegan mas lejos.
     *
     * No modifica el estado de los vertices: el motor decide que hacer con
     * los alcanzados (ganarlos como audiencia, desinformarlos, etc.).
     *
     * @param maxOleadas  saltos maximos
     * @param probabilidad funcion que da la probabilidad final de cada arista
     *                    (la del grafo multiplicada por estrategia, confianza...)
     * @param puedeRecibir filtro: quien puede recibirla (p. ej. no a otra agencia)
     * @param azar        generador con semilla compartida
     */
    public ResultadoPropagacion propagar(Vertice origen, int maxOleadas,
                                         ToDoubleFunction<Arista> probabilidad,
                                         Predicate<Vertice> puedeRecibir, Random azar) {
        ResultadoPropagacion r = new ResultadoPropagacion();
        if (origen == null) {
            return r;
        }
        Set<Vertice> alcanzados = new HashSet<>();
        alcanzados.add(origen);
        List<Vertice> frontera = new ArrayList<>();
        frontera.add(origen);
        r.oleadas.add(new ArrayList<>(frontera));
        r.aristas.add(new ArrayList<Arista>());

        for (int nivel = 1; nivel <= maxOleadas && !frontera.isEmpty(); nivel++) {
            List<Vertice> siguiente = new ArrayList<>();
            List<Arista> usadas = new ArrayList<>();
            for (Vertice emisor : frontera) {
                for (Arista a : emisor.salientes) {
                    Vertice receptor = a.destino;
                    if (alcanzados.contains(receptor) || !puedeRecibir.test(receptor)) {
                        continue;
                    }
                    double p = probabilidad.applyAsDouble(a);
                    if (azar.nextDouble() < p) {
                        alcanzados.add(receptor);
                        siguiente.add(receptor);
                        usadas.add(a);
                    }
                }
            }
            if (siguiente.isEmpty()) {
                break;
            }
            r.oleadas.add(siguiente);
            r.aristas.add(usadas);
            frontera = siguiente;
        }
        return r;
    }

    // ==================================================================
    // Correccion: BFS con tope de profundidad
    // ==================================================================

    /** Lo que consiguio un desmentido. */
    public static class ResultadoCorreccion {
        public final List<Vertice> corregidos = new ArrayList<>();
        /** Niveles recorridos (para animarlo). */
        public final List<List<Vertice>> niveles = new ArrayList<>();
    }

    /**
     * BFS CON TOPE: un desmentido sale desde un vertice y viaja como maximo
     * {@code alcanceMaximo} saltos, sin importar la direccion.
     *
     * Las dos diferencias con la propagacion son la leccion del juego:
     *   1. Se apaga rapido: una mentira se reenvia sola; un desmentido solo lo
     *      comparte quien se toma el trabajo.
     *   2. No limpia a quien ya retransmitio el bulo: esa persona ya se lo conto
     *      a otros, y ese contagio ya siguio su camino.
     */
    public ResultadoCorreccion corregir(Vertice origen, int alcanceMaximo) {
        ResultadoCorreccion r = new ResultadoCorreccion();
        if (origen == null) {
            return r;
        }
        Set<Vertice> visitados = new HashSet<>();
        Queue<Vertice> cola = new ArrayDeque<>();
        cola.add(origen);
        visitados.add(origen);
        int nivel = 0;

        while (!cola.isEmpty() && nivel <= alcanceMaximo) {
            int tam = cola.size();
            List<Vertice> esteNivel = new ArrayList<>();
            for (int i = 0; i < tam; i++) {
                Vertice actual = cola.poll();
                esteNivel.add(actual);
                if (actual.desinformado && !actual.yaRetransmitio) {
                    actual.desinformado = false;
                    actual.escuchoDesmentido = true;
                    r.corregidos.add(actual);
                }
                if (nivel < alcanceMaximo) {
                    for (Vertice vecino : vecinosNoDirigidos(actual)) {
                        if (visitados.add(vecino)) {
                            cola.add(vecino);
                        }
                    }
                }
            }
            r.niveles.add(esteNivel);
            nivel++;
        }
        return r;
    }

    /** Cuentas desinformadas en este momento. */
    public List<Vertice> desinformados() {
        List<Vertice> lista = new ArrayList<>();
        for (Vertice v : vertices.values()) {
            if (v.desinformado) {
                lista.add(v);
            }
        }
        return lista;
    }
}
