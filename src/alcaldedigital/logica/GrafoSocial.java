package alcaldedigital.logica;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * GRAFO NO DIRIGIDO DE CIUDAD NOVA (red por donde viajan las publicaciones)
 * ========================================================================
 * Vértices: lugares de la ciudad (Parque, Hospital, Alcaldía...).
 * Aristas: conexiones entre ellos (la gente de un lugar habla con la del otro).
 * Implementado con listas de adyacencia (cada NodoRed guarda sus vecinos).
 *
 * Integración con el juego:
 *   - Al empezar un nivel aparece un rumor en un lugar ALEATORIO (origen).
 *   - Cada error del jugador (burbuja equivocada o tiempo agotado) hace avanzar
 *     el rumor UN salto en anchura (BFS): todos los vecinos sanos se contagian.
 *   - Cada publicación bien resuelta CONTIENE el rumor: se limpian los lugares
 *     más lejanos del origen (la frontera del BFS).
 * Así el BFS responde lo que pide el enunciado: quiénes reciben la publicación,
 * cuántos, qué tan rápido (en saltos) y qué consecuencias genera.
 *
 * @author Naty
 */
public class GrafoSocial {

    public List<NodoRed> nodos;
    private NodoRed origen;

    public GrafoSocial() {
        this.nodos = new ArrayList<>();
    }

    public void agregarNodo(NodoRed nodo) {
        nodos.add(nodo);
    }

    // Coordenadas ajustadas exactamente sobre los nombres escritos en el mapa de la ciudad
    public void inicializarMapaPrueba() {
        NodoRed parque = new NodoRed("Parque", 230, 215);
        NodoRed hospital = new NodoRed("Hospital", 460, 160);
        NodoRed biblioteca = new NodoRed("Biblioteca", 655, 220);
        NodoRed estacion = new NodoRed("Estación", 250, 495);
        NodoRed alcaldia = new NodoRed("Alcaldía", 515, 550);
        NodoRed teatro = new NodoRed("Teatro", 770, 490);
        NodoRed mercado = new NodoRed("Mercado", 360, 750);
        NodoRed cafe = new NodoRed("Café", 705, 745);

        agregarNodo(parque);
        agregarNodo(hospital);
        agregarNodo(biblioteca);
        agregarNodo(estacion);
        agregarNodo(alcaldia);
        agregarNodo(teatro);
        agregarNodo(mercado);
        agregarNodo(cafe);

        // Conexiones del grafo
        alcaldia.agregarConexion(hospital);
        alcaldia.agregarConexion(estacion);
        alcaldia.agregarConexion(teatro);
        parque.agregarConexion(estacion);
        biblioteca.agregarConexion(teatro);
        hospital.agregarConexion(biblioteca);
        estacion.agregarConexion(mercado);
        teatro.agregarConexion(cafe);
    }

    // ------------------------------------------------------------------
    // Operaciones del juego
    // ------------------------------------------------------------------

    /** Limpia toda la ciudad y siembra un rumor nuevo en un lugar al azar. */
    public NodoRed sembrarRumor(Random azar) {
        for (NodoRed n : nodos) {
            n.estaInfectado = false;
        }
        origen = nodos.get(azar.nextInt(nodos.size()));
        origen.estaInfectado = true;
        return origen;
    }

    /**
     * Un paso de propagación en anchura: cada lugar infectado contagia a TODOS
     * sus vecinos sanos. Devuelve los lugares que acaban de recibir el rumor.
     */
    public List<NodoRed> propagarPaso() {
        List<NodoRed> nuevos = new ArrayList<>();
        for (NodoRed nodo : nodos) {
            if (nodo.estaInfectado) {
                for (NodoRed vecino : nodo.vecinos) {
                    if (!vecino.estaInfectado && !nuevos.contains(vecino)) {
                        nuevos.add(vecino);
                    }
                }
            }
        }
        for (NodoRed n : nuevos) {
            n.estaInfectado = true;
        }
        return nuevos;
    }

    /**
     * Contiene el rumor un paso: limpia los lugares infectados que están más lejos
     * del origen (según BFS). Si solo queda el origen, lo limpia también.
     * Devuelve los lugares que quedaron limpios.
     */
    public List<NodoRed> contener() {
        List<NodoRed> limpiados = new ArrayList<>();
        if (origen == null) {
            return limpiados;
        }
        Map<NodoRed, Integer> dist = distanciasDesde(origen);
        int maxDist = -1;
        for (NodoRed n : nodos) {
            if (n.estaInfectado && dist.containsKey(n)) {
                maxDist = Math.max(maxDist, dist.get(n));
            }
        }
        for (NodoRed n : nodos) {
            if (n.estaInfectado && dist.containsKey(n) && dist.get(n) == maxDist) {
                n.estaInfectado = false;
                limpiados.add(n);
            }
        }
        return limpiados;
    }

    /** Clic del jugador sobre un lugar en el panel de la red: lo limpia a mano. */
    public void limpiar(NodoRed n) {
        n.estaInfectado = false;
    }

    // ------------------------------------------------------------------
    // Consultas (BFS)
    // ------------------------------------------------------------------

    /** BFS: cuántos saltos hay desde "inicio" hasta cada lugar alcanzable. */
    public Map<NodoRed, Integer> distanciasDesde(NodoRed inicio) {
        Map<NodoRed, Integer> dist = new HashMap<>();
        Deque<NodoRed> cola = new ArrayDeque<>();
        dist.put(inicio, 0);
        cola.add(inicio);
        while (!cola.isEmpty()) {
            NodoRed actual = cola.poll();
            for (NodoRed v : actual.vecinos) {
                if (!dist.containsKey(v)) {
                    dist.put(v, dist.get(actual) + 1);
                    cola.add(v);
                }
            }
        }
        return dist;
    }

    /** Orden en que el rumor alcanzaría cada lugar si nadie lo detiene (BFS desde el origen). */
    public List<NodoRed> ordenDePropagacion() {
        List<NodoRed> orden = new ArrayList<>();
        if (origen == null) {
            return orden;
        }
        Deque<NodoRed> cola = new ArrayDeque<>();
        List<NodoRed> vistos = new ArrayList<>();
        cola.add(origen);
        vistos.add(origen);
        while (!cola.isEmpty()) {
            NodoRed actual = cola.poll();
            orden.add(actual);
            for (NodoRed v : actual.vecinos) {
                if (!vistos.contains(v)) {
                    vistos.add(v);
                    cola.add(v);
                }
            }
        }
        return orden;
    }

    public int cantidadInfectados() {
        int c = 0;
        for (NodoRed n : nodos) {
            if (n.estaInfectado) {
                c++;
            }
        }
        return c;
    }

    public int cantidadAristas() {
        int grados = 0;
        for (NodoRed n : nodos) {
            grados += n.vecinos.size();
        }
        return grados / 2;   // cada arista no dirigida se cuenta dos veces
    }

    public NodoRed getOrigen() {
        return origen;
    }
}
