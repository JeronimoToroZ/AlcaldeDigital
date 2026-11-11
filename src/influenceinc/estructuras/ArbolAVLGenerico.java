package influenceinc.estructuras;

import java.util.ArrayList;
import java.util.List;

/**
 * AVL GENERICO (clave comparable -> valor).
 *
 * Lo usa el DIRECTORIO OFICIAL de cuentas: la clave es el @usuario y el valor
 * los datos de la cuenta registrada. Cuando el jugador escanea una cuenta, el
 * juego la busca aqui en O(log n). Cada dia se registran cuentas nuevas
 * (insertar) y alguna se da de baja (eliminar).
 *
 * Es la misma logica del ArbolAVL de las publicaciones (4 rotaciones, factor
 * de equilibrio = altura izquierda - altura derecha), escrita con genericos
 * para poder usar texto como clave.
 */
public class ArbolAVLGenerico<K extends Comparable<K>, V> {

    /** Nodo publico de solo lectura para poder dibujar el arbol. */
    public static final class Nodo<K, V> {
        public final K clave;
        public V valor;
        Nodo<K, V> izq, der;
        int altura;

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

        public int getAltura() {
            return altura;
        }
    }

    private Nodo<K, V> raiz;
    private int tamano;
    private int rotaciones;
    /** Cuantos nodos visito la ultima busqueda (para mostrar que es O(log n)). */
    private int ultimaBusquedaPasos;

    private int h(Nodo<K, V> n) {
        return n == null ? -1 : n.altura;
    }

    public int factorEquilibrio(Nodo<K, V> n) {
        return n == null ? 0 : h(n.izq) - h(n.der);
    }

    private void actualizar(Nodo<K, V> n) {
        n.altura = 1 + Math.max(h(n.izq), h(n.der));
    }

    private Nodo<K, V> rotarDerecha(Nodo<K, V> z) {
        Nodo<K, V> y = z.izq;
        z.izq = y.der;
        y.der = z;
        actualizar(z);
        actualizar(y);
        rotaciones++;
        return y;
    }

    private Nodo<K, V> rotarIzquierda(Nodo<K, V> z) {
        Nodo<K, V> y = z.der;
        z.der = y.izq;
        y.izq = z;
        actualizar(z);
        actualizar(y);
        rotaciones++;
        return y;
    }

    private Nodo<K, V> balancear(Nodo<K, V> n) {
        actualizar(n);
        int fe = factorEquilibrio(n);
        if (fe > 1) {
            if (factorEquilibrio(n.izq) < 0) {
                n.izq = rotarIzquierda(n.izq);      // caso izquierda-derecha
            }
            return rotarDerecha(n);                 // caso izquierda-izquierda
        }
        if (fe < -1) {
            if (factorEquilibrio(n.der) > 0) {
                n.der = rotarDerecha(n.der);        // caso derecha-izquierda
            }
            return rotarIzquierda(n);               // caso derecha-derecha
        }
        return n;
    }

    /** Inserta o reemplaza. O(log n). */
    public void insertar(K clave, V valor) {
        raiz = insertar(raiz, clave, valor);
    }

    private Nodo<K, V> insertar(Nodo<K, V> n, K k, V v) {
        if (n == null) {
            tamano++;
            Nodo<K, V> nuevo = new Nodo<>(k, v);
            return nuevo;
        }
        int c = k.compareTo(n.clave);
        if (c < 0) {
            n.izq = insertar(n.izq, k, v);
        } else if (c > 0) {
            n.der = insertar(n.der, k, v);
        } else {
            n.valor = v;
            return n;
        }
        return balancear(n);
    }

    /** Busqueda binaria. O(log n). null si no existe. */
    public V buscar(K clave) {
        ultimaBusquedaPasos = 0;
        Nodo<K, V> n = raiz;
        while (n != null) {
            ultimaBusquedaPasos++;
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

    /** Elimina (3 casos: hoja, un hijo, dos hijos con sucesor inorden). O(log n). */
    public boolean eliminar(K clave) {
        int antes = tamano;
        raiz = eliminar(raiz, clave);
        return tamano < antes;
    }

    private Nodo<K, V> eliminar(Nodo<K, V> n, K k) {
        if (n == null) {
            return null;
        }
        int c = k.compareTo(n.clave);
        if (c < 0) {
            n.izq = eliminar(n.izq, k);
        } else if (c > 0) {
            n.der = eliminar(n.der, k);
        } else {
            if (n.izq == null || n.der == null) {
                tamano--;
                return n.izq != null ? n.izq : n.der;
            }
            Nodo<K, V> s = n.der;
            while (s.izq != null) {
                s = s.izq;
            }
            Nodo<K, V> reemplazo = new Nodo<>(s.clave, s.valor);
            reemplazo.izq = n.izq;
            reemplazo.der = eliminar(n.der, s.clave);
            n = reemplazo;
        }
        return balancear(n);
    }

    /** Recorrido INORDEN: las claves en orden alfabetico. */
    public List<V> inorden() {
        List<V> l = new ArrayList<>();
        inorden(raiz, l);
        return l;
    }

    private void inorden(Nodo<K, V> n, List<V> l) {
        if (n != null) {
            inorden(n.izq, l);
            l.add(n.valor);
            inorden(n.der, l);
        }
    }

    /** Claves que empiezan con un prefijo (autocompletar del buscador). */
    public List<V> conPrefijo(String prefijo) {
        List<V> l = new ArrayList<>();
        prefijo(raiz, prefijo, l);
        return l;
    }

    private void prefijo(Nodo<K, V> n, String p, List<V> l) {
        if (n == null) {
            return;
        }
        String k = n.clave.toString();
        if (k.compareTo(p) > 0) {
            prefijo(n.izq, p, l);
        }
        if (k.startsWith(p)) {
            l.add(n.valor);
        }
        if (k.compareTo(p) < 0 || k.startsWith(p)) {
            prefijo(n.der, p, l);
        }
    }

    public boolean estaBalanceado() {
        return balanceado(raiz);
    }

    private boolean balanceado(Nodo<K, V> n) {
        return n == null || (Math.abs(factorEquilibrio(n)) <= 1 && balanceado(n.izq) && balanceado(n.der));
    }

    public Nodo<K, V> getRaiz() {
        return raiz;
    }

    public int getTamano() {
        return tamano;
    }

    public int getAltura() {
        return h(raiz);
    }

    public int getRotaciones() {
        return rotaciones;
    }

    public int getUltimaBusquedaPasos() {
        return ultimaBusquedaPasos;
    }
}
