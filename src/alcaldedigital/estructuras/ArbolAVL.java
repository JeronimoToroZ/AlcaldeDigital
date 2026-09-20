package alcaldedigital.estructuras;

import alcaldedigital.modelo.Publicacion;
import java.util.ArrayList;
import java.util.List;

/**
 * ARBOL AVL DE PUBLICACIONES
 * ==========================
 *
 * 1. QUE PROBLEMA RESUELVE
 *    El feed de Civitas crece durante toda la partida y el juego necesita, en cada
 *    turno, responder rapido tres preguntas:
 *      - buscar una publicacion concreta (para actualizar su estado),
 *      - listar las publicaciones ordenadas de menos a mas creible
 *        (es la habilidad del Periodista: "ver lo mas sospechoso del feed"),
 *      - retirar del feed una publicacion reportada.
 *    Con una lista eso costaria O(n) por consulta. Con un arbol de busqueda cuesta
 *    O(altura).
 *
 * 2. POR QUE AVL Y NO ABB SIMPLE
 *    Las publicaciones no llegan en orden aleatorio: los eventos del juego tienden
 *    a generar rachas (varias noticias falsas seguidas, es decir, credibilidades
 *    bajas en orden creciente o decreciente). Con esas rachas un ABB degenera en
 *    una lista enlazada y la altura pasa a ser O(n). El AVL rebalancea con
 *    rotaciones y garantiza altura O(log n) SIEMPRE, sin importar el orden de
 *    insercion. Ese es el argumento exacto para la sustentacion.
 *
 * 3. FACTOR DE EQUILIBRIO
 *    factorEquilibrio(nodo) = altura(hijo izquierdo) - altura(hijo derecho)
 *    Un nodo esta desbalanceado cuando ese valor es +2 (cargado a la izquierda)
 *    o -2 (cargado a la derecha).
 */
public class ArbolAVL {

    private NodoAVL raiz;
    private int tamano;
    private int rotacionesRealizadas;

    public ArbolAVL() {
        this.raiz = null;
        this.tamano = 0;
        this.rotacionesRealizadas = 0;
    }

    // ------------------------------------------------------------------
    // Utilidades basicas
    // ------------------------------------------------------------------

    /** Altura de un subarbol. El subarbol vacio mide -1 para que una hoja mida 0. */
    private int altura(NodoAVL nodo) {
        return (nodo == null) ? -1 : nodo.getAltura();
    }

    /** altura_izquierda - altura_derecha. Cargado a la izquierda => positivo. */
    public int factorEquilibrio(NodoAVL nodo) {
        if (nodo == null) {
            return 0;
        }
        return altura(nodo.getIzquierdo()) - altura(nodo.getDerecho());
    }

    private void actualizarAltura(NodoAVL nodo) {
        nodo.setAltura(1 + Math.max(altura(nodo.getIzquierdo()), altura(nodo.getDerecho())));
    }

    // ------------------------------------------------------------------
    // Rotaciones
    // ------------------------------------------------------------------

    /**
     * Rotacion simple a la derecha. Se usa cuando el nodo esta cargado a la
     * izquierda y su hijo izquierdo tambien (caso Izquierda-Izquierda).
     *
     *        z                y
     *       / \              / \
     *      y   D    ==>     x   z
     *     / \                  / \
     *    x   C                C   D
     */
    private NodoAVL rotarDerecha(NodoAVL z) {
        NodoAVL y = z.getIzquierdo();
        NodoAVL subarbolC = y.getDerecho();

        y.setDerecho(z);
        z.setIzquierdo(subarbolC);

        actualizarAltura(z); // primero el que quedo abajo
        actualizarAltura(y);
        rotacionesRealizadas++;
        return y; // y pasa a ser la nueva raiz de este subarbol
    }

    /**
     * Rotacion simple a la izquierda. Caso Derecha-Derecha.
     *
     *      z                    y
     *     / \                  / \
     *    A   y      ==>       z   x
     *       / \              / \
     *      B   x            A   B
     */
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

    /**
     * Decide que rotacion aplicar segun el factor de equilibrio.
     * Los cuatro casos clasicos: II, DD, ID, DI.
     */
    private NodoAVL rebalancear(NodoAVL nodo) {
        actualizarAltura(nodo);
        int fe = factorEquilibrio(nodo);

        if (fe > 1) { // cargado a la izquierda
            if (factorEquilibrio(nodo.getIzquierdo()) < 0) {
                // Caso Izquierda-Derecha: primero se endereza el hijo
                nodo.setIzquierdo(rotarIzquierda(nodo.getIzquierdo()));
            }
            return rotarDerecha(nodo); // Caso Izquierda-Izquierda
        }

        if (fe < -1) { // cargado a la derecha
            if (factorEquilibrio(nodo.getDerecho()) > 0) {
                // Caso Derecha-Izquierda
                nodo.setDerecho(rotarDerecha(nodo.getDerecho()));
            }
            return rotarIzquierda(nodo); // Caso Derecha-Derecha
        }

        return nodo; // ya estaba balanceado
    }

    // ------------------------------------------------------------------
    // Insercion
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
            nodo.setPublicacion(p); // clave repetida: se sobreescribe, no se duplica
            return nodo;
        }

        // Al volver de la recursion se corrige el equilibrio de abajo hacia arriba.
        return rebalancear(nodo);
    }

    // ------------------------------------------------------------------
    // Busqueda
    // ------------------------------------------------------------------

    /** Busqueda por clave compuesta. O(log n). */
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
    // Eliminacion (los tres casos)
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
            // CASO 1: hoja  -> desaparece
            // CASO 2: un solo hijo -> lo reemplaza el hijo
            if (nodo.getIzquierdo() == null) {
                return nodo.getDerecho();
            }
            if (nodo.getDerecho() == null) {
                return nodo.getIzquierdo();
            }
            // CASO 3: dos hijos -> se reemplaza por el sucesor inorden
            // (el menor del subarbol derecho) y se elimina ese sucesor.
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

    /** Inorden: izquierda - raiz - derecha. Devuelve el feed ordenado por credibilidad. */
    public List<Publicacion> inorden() {
        List<Publicacion> salida = new ArrayList<>();
        inordenRec(raiz, salida);
        return salida;
    }

    private void inordenRec(NodoAVL nodo, List<Publicacion> salida) {
        if (nodo == null) {
            return;
        }
        inordenRec(nodo.getIzquierdo(), salida);
        salida.add(nodo.getPublicacion());
        inordenRec(nodo.getDerecho(), salida);
    }

    /** Preorden: raiz - izquierda - derecha. Sirve para clonar o serializar el arbol. */
    public List<Publicacion> preorden() {
        List<Publicacion> salida = new ArrayList<>();
        preordenRec(raiz, salida);
        return salida;
    }

    private void preordenRec(NodoAVL nodo, List<Publicacion> salida) {
        if (nodo == null) {
            return;
        }
        salida.add(nodo.getPublicacion());
        preordenRec(nodo.getIzquierdo(), salida);
        preordenRec(nodo.getDerecho(), salida);
    }

    /** Postorden: izquierda - derecha - raiz. Sirve para liberar o cerrar el feed. */
    public List<Publicacion> postorden() {
        List<Publicacion> salida = new ArrayList<>();
        postordenRec(raiz, salida);
        return salida;
    }

    private void postordenRec(NodoAVL nodo, List<Publicacion> salida) {
        if (nodo == null) {
            return;
        }
        postordenRec(nodo.getIzquierdo(), salida);
        postordenRec(nodo.getDerecho(), salida);
        salida.add(nodo.getPublicacion());
    }

    /**
     * Consulta por rango: publicaciones con credibilidad por debajo del limite.
     * Es la habilidad del Periodista dentro del juego. Poda las ramas que no
     * pueden contener resultados, asi que no recorre el arbol completo.
     */
    public List<Publicacion> menosCreiblesQue(int limiteCredibilidad) {
        List<Publicacion> salida = new ArrayList<>();
        rangoRec(raiz, limiteCredibilidad * 1000, salida);
        return salida;
    }

    private void rangoRec(NodoAVL nodo, int claveTope, List<Publicacion> salida) {
        if (nodo == null) {
            return;
        }
        rangoRec(nodo.getIzquierdo(), claveTope, salida);
        if (nodo.getClave() < claveTope) {
            salida.add(nodo.getPublicacion());
            rangoRec(nodo.getDerecho(), claveTope, salida); // poda: solo si aun cabe
        }
    }

    // ------------------------------------------------------------------
    // Estado del arbol
    // ------------------------------------------------------------------

    public NodoAVL getRaiz() {
        return raiz;
    }

    public int getTamano() {
        return tamano;
    }

    public int getAlturaArbol() {
        return altura(raiz);
    }

    public int getRotacionesRealizadas() {
        return rotacionesRealizadas;
    }

    public boolean estaVacio() {
        return raiz == null;
    }

    /** Verificacion de invariante AVL: util para las pruebas de la sustentacion. */
    public boolean estaBalanceado() {
        return balanceadoRec(raiz);
    }

    private boolean balanceadoRec(NodoAVL nodo) {
        if (nodo == null) {
            return true;
        }
        if (Math.abs(factorEquilibrio(nodo)) > 1) {
            return false;
        }
        return balanceadoRec(nodo.getIzquierdo()) && balanceadoRec(nodo.getDerecho());
    }
}
