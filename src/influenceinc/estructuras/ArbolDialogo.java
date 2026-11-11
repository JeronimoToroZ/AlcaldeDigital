package influenceinc.estructuras;

import java.util.ArrayList;
import java.util.List;

/**
 * ARBOL DE DIALOGO N-ARIO
 * =======================
 * La conversacion con cada visitante es un arbol:
 *   raiz  = el saludo del visitante
 *   hijos = las preguntas que puedes hacer
 *   nietos = las repreguntas que se abren segun lo que respondio
 *
 * Es un arbol (y no una lista) porque cada respuesta abre caminos distintos.
 *
 * INSERCION EN VIVO: cuando una herramienta descubre una contradiccion (la
 * cuenta se creo hace 3 dias, la foto de la credencial no coincide...), el
 * juego INSERTA una pregunta nueva de confrontacion bajo la raiz. Si el
 * visitante es un impostor, ahi se quiebra.
 */
public class ArbolDialogo {

    private final NodoDialogo raiz;

    public ArbolDialogo(String saludo, String expresion) {
        raiz = new NodoDialogo(null, saludo, expresion, null);
        raiz.visitado = true;
    }

    public NodoDialogo getRaiz() {
        return raiz;
    }

    /** Agrega una pregunta bajo la raiz. Devuelve el nodo para colgarle repreguntas. */
    public NodoDialogo pregunta(String pregunta, String respuesta, String expresion, String pista) {
        NodoDialogo n = new NodoDialogo(pregunta, respuesta, expresion, pista);
        raiz.hijo(n);
        return n;
    }

    /** INSERTA un nodo como hijo de otro (al principio, para que se vea primero). */
    public void insertar(NodoDialogo padre, NodoDialogo nuevo) {
        nuevo.padre = padre;
        nuevo.insertado = true;
        padre.hijos.add(0, nuevo);
    }

    /** ELIMINA un subarbol (por ejemplo, una pregunta que ya no tiene sentido). */
    public boolean eliminar(NodoDialogo nodo) {
        if (nodo == raiz || nodo.padre == null) {
            return false;
        }
        boolean ok = nodo.padre.hijos.remove(nodo);
        nodo.padre = null;
        return ok;
    }

    /**
     * Opciones disponibles estando en un nodo: sus hijos sin visitar; si ya no
     * quedan, se vuelve a las preguntas de la raiz que faltan.
     */
    public List<NodoDialogo> opciones(NodoDialogo actual) {
        List<NodoDialogo> l = new ArrayList<>();
        for (NodoDialogo h : actual.hijos) {
            if (!h.visitado) {
                l.add(h);
            }
        }
        if (l.isEmpty() && actual != raiz) {
            for (NodoDialogo h : raiz.hijos) {
                if (!h.visitado) {
                    l.add(h);
                }
            }
        }
        return l;
    }

    /** PREORDEN, para la vista del arbol y la bitacora. */
    public List<NodoDialogo> preorden() {
        List<NodoDialogo> l = new ArrayList<>();
        preorden(raiz, l);
        return l;
    }

    private void preorden(NodoDialogo n, List<NodoDialogo> l) {
        l.add(n);
        for (NodoDialogo h : n.hijos) {
            preorden(h, l);
        }
    }

    public int cantidadNodos() {
        return preorden().size();
    }

    public int altura() {
        return altura(raiz);
    }

    private int altura(NodoDialogo n) {
        int m = -1;
        for (NodoDialogo h : n.hijos) {
            m = Math.max(m, altura(h));
        }
        return m + 1;
    }

    public int visitados() {
        int c = 0;
        for (NodoDialogo n : preorden()) {
            if (n.visitado) {
                c++;
            }
        }
        return c;
    }
}
