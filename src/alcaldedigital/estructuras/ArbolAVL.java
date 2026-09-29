package alcaldedigital.estructuras;

import alcaldedigital.modelo.Publicacion;
import java.util.ArrayList;
import java.util.List;

/**
 * Estructura de datos de Árbol AVL para la gestión ordenada y auto-balanceada 
 * de las publicaciones dentro del juego. Garantiza operaciones eficientes 
 * de inserción, búsqueda y eliminación con complejidad logarítmica.
 * 
 * @author Naty
 */
public class ArbolAVL {

    private NodoAVL raiz;
    private int tamano;
    private int rotacionesRealizadas;

    /**
     * Inicializa un árbol AVL vacío con sus contadores en cero.
     */
    public ArbolAVL() {
        this.raiz = null;
        this.tamano = 0;
        this.rotacionesRealizadas = 0;
    }

    /**
     * Métodos auxiliares de control estructural:
     * Calcula la altura de los subárboles (retornando -1 si son nulos) 
     * y gestiona el factor de equilibrio y actualización de alturas.
     */
    private int altura(NodoAVL nodo) {
        return (nodo == null) ? -1 : nodo.getAltura();
    }

    public int factorEquilibrio(NodoAVL nodo) {
        if (nodo == null) {
            return 0;
        }
        return altura(nodo.getIzquierdo()) - altura(nodo.getDerecho());
    }

    private void actualizarAltura(NodoAVL nodo) {
        nodo.setAltura(1 + Math.max(altura(nodo.getIzquierdo()), altura(nodo.getDerecho())));
    }

    /**
     * Rotaciones simples (derecha e izquierda) y rebalanceo general del árbol 
     * para mantener la propiedad AVL tras modificaciones en los nodos.
     */
    private NodoAVL rotarDerecha(NodoAVL z) {
        NodoAVL y = z.getIzquierdo();
        NodoAVL subarbolC = y.getDerecho();

        y.setDerecho(z);
        z.setIzquierdo(subarbolC);

        actualizarAltura(z);
        actualizarAltura(y);
        rotacionesRealizadas++;
        return y; 
    }

    private NodoAVL rotarIzquierda(NodoAVL z) {
        NodoAVL y = z.getDerecho();
        NodoAVL subarbolB = y.getIzquierdo();

        y.setIzquierdo(z);
        z.setDerecho(subarbolB);

        actualizarAltura(z);
        actualizarAltura(y);
        rotacionesRealizadas++;
        return y;
    }

    private NodoAVL rebalancear(NodoAVL nodo) {
        actualizarAltura(nodo);
        int fe = factorEquilibrio(nodo);

        if (fe > 1) { // Desbalanceado a la izquierda
            if (factorEquilibrio(nodo.getIzquierdo()) < 0) {
                nodo.setIzquierdo(rotarIzquierda(nodo.getIzquierdo()));
            }
            return rotarDerecha(nodo);
        }

        if (fe < -1) { // Desbalanceado a la derecha
            if (factorEquilibrio(nodo.getDerecho()) > 0) {
                nodo.setDerecho(rotarDerecha(nodo.getDerecho()));
            }
            return rotarIzquierda(nodo);
        }

        return nodo;
    }

    // ------------------------------------------------------------------
    // Inserción
    // ------------------------------------------------------------------

    public void insertar(Publicacion publicacion) {
        raiz = insertarRec(raiz, publicacion);
    }

    private NodoAVL insertarRec(NodoAVL nodo, Publicacion p) {
        if (nodo == null) {
            tamano++;
            return new NodoAVL(p);
        }

        int clave = p.claveAVL();
        if (clave < nodo.getClave()) {
            nodo.setIzquierdo(insertarRec(nodo.getIzquierdo(), p));
        } else if (clave > nodo.getClave()) {
            nodo.setDerecho(insertarRec(nodo.getDerecho(), p));
        } else {
            nodo.setPublicacion(p); // Actualiza si la clave ya existe
            return nodo;
        }

        return rebalancear(nodo);
    }

    // ------------------------------------------------------------------
    // Búsqueda
    // ------------------------------------------------------------------

    /**
     * Busca y retorna una publicación mediante su clave numérica con tiempo O(log n).
     */
    public Publicacion buscar(int clave) {
        NodoAVL actual = raiz;
        while (actual != null) {
            if (clave < actual.getClave()) {
                actual = actual.getIzquierdo();
            } else if (clave > actual.getClave()) {
                actual = actual.getDerecho();
            } else {
                return actual.getPublicacion();
            }
        }
        return null;
    }

    public boolean contiene(int clave) {
        return buscar(clave) != null;
    }

    // ------------------------------------------------------------------
    // Eliminación
    // ------------------------------------------------------------------

    public boolean eliminar(int clave) {
        if (!contiene(clave)) {
            return false;
        }
        raiz = eliminarRec(raiz, clave);
        tamano--;
        return true;
    }

    private NodoAVL eliminarRec(NodoAVL nodo, int clave) {
        if (nodo == null) {
            return null;
        }

        if (clave < nodo.getClave()) {
            nodo.setIzquierdo(eliminarRec(nodo.getIzquierdo(), clave));
        } else if (clave > nodo.getClave()) {
            nodo.setDerecho(eliminarRec(nodo.getDerecho(), clave));
        } else {
            // Manejo de los tres casos de eliminación (hoja, un hijo o dos hijos)
            if (nodo.getIzquierdo() == null) {
                return nodo.getDerecho();
            }
            if (nodo.getDerecho() == null) {
                return nodo.getIzquierdo();
            }
            
            NodoAVL sucesor = minimo(nodo.getDerecho());
            NodoAVL reemplazo = new NodoAVL(sucesor.getPublicacion());
            reemplazo.setIzquierdo(nodo.getIzquierdo());
            reemplazo.setDerecho(eliminarRec(nodo.getDerecho(), sucesor.getClave()));
            nodo = reemplazo;
        }

        return rebalancear(nodo);
    }

    private NodoAVL minimo(NodoAVL nodo) {
        while (nodo.getIzquierdo() != null) {
            nodo = nodo.getIzquierdo();
        }
        return nodo;
    }

    // ------------------------------------------------------------------
    // Recorridos
    // ------------------------------------------------------------------

    /** Retorna las publicaciones ordenadas en Inorden (feed del juego). */
    public List<Publicacion> inorden() {
        List<Publicacion> salida = new ArrayList<>();
        inordenRec(raiz, salida);
        return salida;
    }

    private void inordenRec(NodoAVL nodo, List<Publicacion> salida) {
        if (nodo == null) return;
        inordenRec(nodo.getIzquierdo(), salida);
        salida.add(nodo.getPublicacion());
        inordenRec(nodo.getDerecho(), salida);
    }

    /** Retorna el árbol en Preorden (ideal para serialización). */
    public List<Publicacion> preorden() {
        List<Publicacion> salida = new ArrayList<>();
        preordenRec(raiz, salida);
        return salida;
    }

    private void preordenRec(NodoAVL nodo, List<Publicacion> salida) {
        if (nodo == null) return;
        salida.add(nodo.getPublicacion());
        preordenRec(nodo.getIzquierdo(), salida);
        preordenRec(nodo.getDerecho(), salida);
    }

    /** Retorna el árbol en Postorden. */
    public List<Publicacion> postorden() {
        List<Publicacion> salida = new ArrayList<>();
        postordenRec(raiz, salida);
        return salida;
    }

    private void postordenRec(NodoAVL nodo, List<Publicacion> salida) {
        if (nodo == null) return;
        postordenRec(nodo.getIzquierdo(), salida);
        postordenRec(nodo.getDerecho(), salida);
        salida.add(nodo.getPublicacion());
    }

    /**
     * Consulta optimizada por rango: filtra publicaciones con menor credibilidad
     * aplicando la habilidad especial del Periodista mediante poda de ramas.
     */
    public List<Publicacion> menosCreiblesQue(int limiteCredibilidad) {
        List<Publicacion> salida = new ArrayList<>();
        rangoRec(raiz, limiteCredibilidad * 1000, salida);
        return salida;
    }

    private void rangoRec(NodoAVL nodo, int claveTope, List<Publicacion> salida) {
        if (nodo == null) return;
        rangoRec(nodo.getIzquierdo(), claveTope, salida);
        if (nodo.getClave() < claveTope) {
            salida.add(nodo.getPublicacion());
            rangoRec(nodo.getDerecho(), claveTope, salida);
        }
    }

    // ------------------------------------------------------------------
    // Estado y Validación del Árbol
    // ------------------------------------------------------------------

    public NodoAVL getRaiz() { return raiz; }
    public int getTamano() { return tamano; }
    public int getAlturaArbol() { return altura(raiz); }
    public int getRotacionesRealizadas() { return rotacionesRealizadas; }
    public boolean estaVacio() { return raiz == null; }

    /** Valida el invariante de balanceo AVL (útil para pruebas y sustentación). */
    public boolean estaBalanceado() {
        return balanceadoRec(raiz);
    }

    private boolean balanceadoRec(NodoAVL nodo) {
        if (nodo == null) return true;
        if (Math.abs(factorEquilibrio(nodo)) > 1) return false;
        return balanceadoRec(nodo.getIzquierdo()) && balanceadoRec(nodo.getDerecho());
    }
}