package alcaldedigital.logica;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Un vertice del grafo social: un punto de la ciudad donde circula informacion,
 * y la persona que la recibe ahi.
 *
 * Guarda cuatro cosas distintas que conviene no confundir:
 *   - datos de presentacion (nombre del lugar, x, y) para poder dibujarlo;
 *   - quien vive o trabaja ahi (persona), para que las consecuencias tengan nombre;
 *   - estado del rumor (estaInfectado, yaRetransmitio, contagiadoPor);
 *   - estructura del grafo (vecinos y el peso de cada arista).
 *
 * @author nplopez
 */
public class NodoRed {

    /** Nombre del lugar, tal como aparece escrito en el mapa de la ciudad. */
    public String nombre;

    /** Quien recibe el rumor en ese lugar. Sirve para contar las consecuencias. */
    public String persona;

    /** Coordenadas en pixeles dentro del mapa de 1000x800 para pintar el circulo. */
    public int x, y;

    /** true si el rumor falso ya llego aqui. */
    public boolean estaInfectado = false;

    /**
     * true si esta persona ya le paso el rumor a alguien mas.
     *
     * Es la clave de la mecanica de correccion: a quien ya lo contó, el desmentido
     * le llega tarde. Su contagio ya siguió camino y no se puede deshacer.
     */
    public boolean yaRetransmitio = false;

    /** Quien le paso el rumor a este nodo. null si es el paciente cero o esta sano. */
    public NodoRed contagiadoPor = null;

    /**
     * Esta persona ya escucho un desmentido y no se la vuelve a convencer.
     *
     * Sin esto, el rumor volvia a infectar en la siguiente oleada a quien el
     * jugador acababa de corregir, y desmentir no servia para nada. Que quede
     * memoria tambien es lo realista: a alguien que ya le explicaron que era
     * mentira no se le vuelve a vender la misma mentira.
     */
    public boolean escuchoDesmentido = false;

    /** Lista de adyacencia: los vecinos directos de este nodo. */
    public List<NodoRed> vecinos;

    /**
     * Peso de la arista hacia cada vecino: cuanto cuesta llevar una campana
     * de verificacion por ese tramo. Se usa en Dijkstra.
     */
    public Map<NodoRed, Integer> pesos;

    public NodoRed(String nombre, String persona, int x, int y) {
        this.nombre = nombre;
        this.persona = persona;
        this.x = x;
        this.y = y;
        this.vecinos = new ArrayList<>();
        this.pesos = new HashMap<>();
    }

    /** Nodo sin persona asignada. Se mantiene para no romper codigo anterior. */
    public NodoRed(String nombre, int x, int y) {
        this(nombre, nombre, x, y);
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

    /** Texto para la bitacora: "don Humberto (Parque)". */
    public String descripcion() {
        if (persona == null || persona.equals(nombre)) {
            return nombre;
        }
        return persona + " (" + nombre + ")";
    }

    @Override
    public String toString() {
        return nombre;
    }
}
