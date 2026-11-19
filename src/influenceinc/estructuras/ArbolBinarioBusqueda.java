package influenceinc.estructuras;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

/**
 * ARBOL BINARIO DE BUSQUEDA (ABB)
 * ===============================
 * Cada nodo tiene una clave; todo lo que esta a su izquierda es menor y todo
 * lo que esta a su derecha es mayor. No se balancea (para eso esta el AVL del
 * directorio): aqui lo que interesa es practicar las operaciones basicas y
 * los RECORRIDOS, en version recursiva y en version iterativa.
 *
 * En el juego guarda la AGENDA DE CONTACTOS del celular, ordenada por nombre.
 * El recorrido INORDEN da la lista alfabetica que se ve en la app Telefono.
 *
 * Operaciones: insertar, buscar, eliminar (hoja, un hijo, dos hijos),
 * minimo, maximo, altura, tamano.
 * Recorridos recursivos: preorden, inorden, postorden.
 * Recorridos iterativos: preorden e inorden con PILA, postorden con DOS pilas
 * y por niveles con COLA.
 */
public class ArbolBinarioBusqueda<K extends Comparable<K>, V> {

    /** Nodo publico de solo lectura (para dibujarlo en el visor de estructuras). */
    public static final class Nodo<K, V> {
        public final K clave;
        public V valor;
        Nodo<K, V> izq, der;

        Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }

        public Nodo<K, V> getIzq() {
            return izq;
        }

        public Nodo<K, V> getDer() {
            return der;
        }
    }

    private Nodo<K, V> raiz;
    private int tamano;
    /** Comparaciones de la ultima busqueda (para mostrar el costo). */
    private int ultimaBusqueda;

    // ==================================================================
    // Insertar / buscar / eliminar
    // ==================================================================

    /** Inserta o reemplaza el valor de la clave. O(h). */
    public void insertar(K clave, V valor) {
        raiz = insertar(raiz, clave, valor);
    }

    private Nodo<K, V> insertar(Nodo<K, V> n, K clave, V valor) {
        if (n == null) {
            tamano++;
            return new Nodo<>(clave, valor);
        }
        int c = clave.compareTo(n.clave);
        if (c < 0) {
            n.izq = insertar(n.izq, clave, valor);
        } else if (c > 0) {
            n.der = insertar(n.der, clave, valor);
        } else {
            n.valor = valor;
        }
        return n;
    }

    /** Busqueda ITERATIVA: baja por un solo camino. O(h). */
    public V buscar(K clave) {
        ultimaBusqueda = 0;
        Nodo<K, V> n = raiz;
        while (n != null) {
            ultimaBusqueda++;
            int c = clave.compareTo(n.clave);
            if (c == 0) {
                return n.valor;
            }
            n = c < 0 ? n.izq : n.der;
        }
        return null;
    }

    public boolean contiene(K clave) {
        return buscar(clave) != null;
    }

    /** Elimina la clave. Con dos hijos se reemplaza por el SUCESOR (minimo del subarbol derecho). */
    public boolean eliminar(K clave) {
        int antes = tamano;
        raiz = eliminar(raiz, clave);
        return tamano < antes;
    }

    private Nodo<K, V> eliminar(Nodo<K, V> n, K clave) {
        if (n == null) {
            return null;
        }
        int c = clave.compareTo(n.clave);
        if (c < 0) {
            n.izq = eliminar(n.izq, clave);
            return n;
        }
        if (c > 0) {
            n.der = eliminar(n.der, clave);
            return n;
        }
        // encontrado
        if (n.izq == null) {
            tamano--;
            return n.der;
        }
        if (n.der == null) {
            tamano--;
            return n.izq;
        }
        Nodo<K, V> sucesor = minimo(n.der);
        Nodo<K, V> nuevo = new Nodo<>(sucesor.clave, sucesor.valor);
        nuevo.izq = n.izq;
        nuevo.der = eliminar(n.der, sucesor.clave);   // aqui se descuenta el tamano
        return nuevo;
    }

    private Nodo<K, V> minimo(Nodo<K, V> n) {
        while (n.izq != null) {
            n = n.izq;
        }
        return n;
    }

    public K minimo() {
        return raiz == null ? null : minimo(raiz).clave;
    }

    public K maximo() {
        if (raiz == null) {
            return null;
        }
        Nodo<K, V> n = raiz;
        while (n.der != null) {
            n = n.der;
        }
        return n.clave;
    }

    public int altura() {
        return altura(raiz);
    }

    private int altura(Nodo<K, V> n) {
        if (n == null) {
            return -1;
        }
        return 1 + Math.max(altura(n.izq), altura(n.der));
    }

    // ==================================================================
    // Recorridos RECURSIVOS
    // ==================================================================

    public List<K> preordenRecursivo() {
        List<K> l = new ArrayList<>();
        preorden(raiz, l);
        return l;
    }

    private void preorden(Nodo<K, V> n, List<K> l) {
        if (n == null) {
            return;
        }
        l.add(n.clave);
        preorden(n.izq, l);
        preorden(n.der, l);
    }

    public List<K> inordenRecursivo() {
        List<K> l = new ArrayList<>();
        inorden(raiz, l);
        return l;
    }

    private void inorden(Nodo<K, V> n, List<K> l) {
        if (n == null) {
            return;
        }
        inorden(n.izq, l);
        l.add(n.clave);
        inorden(n.der, l);
    }

    public List<K> postordenRecursivo() {
        List<K> l = new ArrayList<>();
        postorden(raiz, l);
        return l;
    }

    private void postorden(Nodo<K, V> n, List<K> l) {
        if (n == null) {
            return;
        }
        postorden(n.izq, l);
        postorden(n.der, l);
        l.add(n.clave);
    }

    // ==================================================================
    // Recorridos ITERATIVOS (pilas y cola en vez de recursion)
    // ==================================================================

    /** Preorden con una pila: se saca un nodo, se visita y se apilan der e izq (izq queda arriba). */
    public List<K> preordenIterativo() {
        List<K> l = new ArrayList<>();
        if (raiz == null) {
            return l;
        }
        Deque<Nodo<K, V>> pila = new ArrayDeque<>();
        pila.push(raiz);
        while (!pila.isEmpty()) {
            Nodo<K, V> n = pila.pop();
            l.add(n.clave);
            if (n.der != null) {
                pila.push(n.der);
            }
            if (n.izq != null) {
                pila.push(n.izq);
            }
        }
        return l;
    }

    /** Inorden con una pila: bajar a la izquierda apilando, visitar, y seguir por la derecha. */
    public List<K> inordenIterativo() {
        List<K> l = new ArrayList<>();
        Deque<Nodo<K, V>> pila = new ArrayDeque<>();
        Nodo<K, V> n = raiz;
        while (n != null || !pila.isEmpty()) {
            while (n != null) {
                pila.push(n);
                n = n.izq;
            }
            n = pila.pop();
            l.add(n.clave);
            n = n.der;
        }
        return l;
    }

    /** Postorden con dos pilas: la primera genera raiz-der-izq y la segunda lo invierte. */
    public List<K> postordenIterativo() {
        List<K> l = new ArrayList<>();
        if (raiz == null) {
            return l;
        }
        Deque<Nodo<K, V>> p1 = new ArrayDeque<>();
        Deque<Nodo<K, V>> p2 = new ArrayDeque<>();
        p1.push(raiz);
        while (!p1.isEmpty()) {
            Nodo<K, V> n = p1.pop();
            p2.push(n);
            if (n.izq != null) {
                p1.push(n.izq);
            }
            if (n.der != null) {
                p1.push(n.der);
            }
        }
        while (!p2.isEmpty()) {
            l.add(p2.pop().clave);
        }
        return l;
    }

    /** Por niveles (BFS) con una cola. */
    public List<K> porNiveles() {
        List<K> l = new ArrayList<>();
        if (raiz == null) {
            return l;
        }
        Queue<Nodo<K, V>> cola = new ArrayDeque<>();
        cola.add(raiz);
        while (!cola.isEmpty()) {
            Nodo<K, V> n = cola.poll();
            l.add(n.clave);
            if (n.izq != null) {
                cola.add(n.izq);
            }
            if (n.der != null) {
                cola.add(n.der);
            }
        }
        return l;
    }

    /** Los valores en orden de clave (inorden iterativo). Es la lista de contactos. */
    public List<V> valoresEnOrden() {
        List<V> l = new ArrayList<>();
        Deque<Nodo<K, V>> pila = new ArrayDeque<>();
        Nodo<K, V> n = raiz;
        while (n != null || !pila.isEmpty()) {
            while (n != null) {
                pila.push(n);
                n = n.izq;
            }
            n = pila.pop();
            l.add(n.valor);
            n = n.der;
        }
        return l;
    }

    public Nodo<K, V> getRaiz() {
        return raiz;
    }

    public int getTamano() {
        return tamano;
    }

    public int getUltimaBusqueda() {
        return ultimaBusqueda;
    }
}
