package alcaldedigital.logica;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Un vértice del grafo social: un punto de la ciudad donde circula informacion.
 *
 * Guarda tres cosas distintas que conviene no confundir:
 *   - datos de presentacion (nombre, x, y) para poder dibujarlo;
 *   - estado del juego (estaInfectado);
 *   - estructura del grafo (vecinos y el peso de cada arista).
 *
 * @author nplopez
 */
public class NodoRed {

    /** Nombre visible del lugar. */
    public String nombre;

    /** Coordenadas en pixeles dentro del mapa de 1000x800 para pintar el circulo. */
    public int x, y;

    /** true si el rumor falso ya llego aqui. */
    public boolean estaInfectado = false;

    /** Lista de adyacencia: los vecinos directos de este nodo. */
    public List<NodoRed> vecinos;

    /**
     * Peso de la arista hacia cada vecino: cuanto cuesta llevar una campana
     * de verificacion por ese tramo. Se usa en Dijkstra.
     */
    public Map<NodoRed, Integer> pesos;

    public NodoRed(String nombre, int x, int y) {
        this.nombre = nombre;
        this.x = x;
        this.y = y;
        this.vecinos = new ArrayList<>();
        this.pesos = new HashMap<>();
    }

    /**
     * Conecta este nodo con otro. El grafo es NO DIRIGIDO, por eso la arista
     * se registra en los dos sentidos.
     *
     * @param vecino el nodo con el que se conecta
     * @param peso   costo de recorrer esa arista (debe ser positivo para Dijkstra)
     */
    public void agregarConexion(NodoRed vecino, int peso) {
        if (vecino == null || vecino == this) {
            return;
        }
        if (!this.vecinos.contains(vecino)) {
            this.vecinos.add(vecino);
            vecino.vecinos.add(this);
        }
        this.pesos.put(vecino, peso);
        vecino.pesos.put(this, peso);
    }

    /** Conexion sin peso explicito: se asume costo 1. */
    public void agregarConexion(NodoRed vecino) {
        agregarConexion(vecino, 1);
    }

    /** Costo de ir de este nodo al vecino. Integer.MAX_VALUE si no son vecinos. */
    public int pesoHacia(NodoRed vecino) {
        return pesos.getOrDefault(vecino, Integer.MAX_VALUE);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
