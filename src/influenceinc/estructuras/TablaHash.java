package influenceinc.estructuras;

import java.util.ArrayList;
import java.util.List;

/**
 * TABLA HASH (DICCIONARIO) CON ENCADENAMIENTO
 * ==========================================
 * Arreglo de "cubetas"; cada cubeta es una lista enlazada de pares
 * (clave, valor). La cubeta de una clave sale de su hashCode modulo el
 * tamano del arreglo. Si dos claves caen en la misma cubeta (colision) se
 * encadenan en la misma lista.
 *
 * Cuando el FACTOR DE CARGA (elementos / cubetas) pasa de 0,75 el arreglo se
 * duplica y todo se vuelve a repartir (rehash). Asi las listas se mantienen
 * cortas y buscar, insertar y eliminar cuestan O(1) en promedio.
 *
 * En el juego:
 *   - los MENSAJES del celular: contacto -> conversacion;
 *   - las PUBLICACIONES de la historia: id -> publicacion.
 */
public class TablaHash<K, V> {

    private static final double CARGA_MAXIMA = 0.75;

    /** Un par de la lista de una cubeta. */
    private static final class Entrada<K, V> {
        final K clave;
        V valor;
        Entrada<K, V> siguiente;

        Entrada(K clave, V valor, Entrada<K, V> siguiente) {
            this.clave = clave;
            this.valor = valor;
            this.siguiente = siguiente;
        }
    }

    private Entrada<K, V>[] cubetas;
    private int tamano;
    private int colisiones;
    /** Orden en que se insertaron las claves (para recorrer siempre igual). */
    private final List<K> orden = new ArrayList<>();

    public TablaHash() {
        this(16);
    }

    @SuppressWarnings("unchecked")
    public TablaHash(int capacidad) {
        cubetas = (Entrada<K, V>[]) new Entrada[Math.max(4, capacidad)];
    }

    /** Funcion hash: el hashCode de la clave llevado a una posicion valida del arreglo. */
    private int indice(K clave, int largo) {
        int h = clave == null ? 0 : clave.hashCode();
        h = h ^ (h >>> 16);                // mezcla los bits altos con los bajos
        return Math.abs(h % largo);
    }

    /** Inserta o reemplaza. O(1) promedio. */
    public void poner(K clave, V valor) {
        int i = indice(clave, cubetas.length);
        for (Entrada<K, V> e = cubetas[i]; e != null; e = e.siguiente) {
            if (iguales(e.clave, clave)) {
                e.valor = valor;
                return;
            }
        }
        if (cubetas[i] != null) {
            colisiones++;
        }
        cubetas[i] = new Entrada<>(clave, valor, cubetas[i]);
        tamano++;
        orden.add(clave);
        if ((double) tamano / cubetas.length > CARGA_MAXIMA) {
            redimensionar();
        }
    }

    /** Busca. null si no esta. O(1) promedio. */
    public V obtener(K clave) {
        for (Entrada<K, V> e = cubetas[indice(clave, cubetas.length)]; e != null; e = e.siguiente) {
            if (iguales(e.clave, clave)) {
                return e.valor;
            }
        }
        return null;
    }

    /** Igual que obtener (para usarla como un Map). */
    public V get(K clave) {
        return obtener(clave);
    }

    /** Igual que poner (para usarla como un Map). */
    public void put(K clave, V valor) {
        poner(clave, valor);
    }

    public boolean contiene(K clave) {
        for (Entrada<K, V> e = cubetas[indice(clave, cubetas.length)]; e != null; e = e.siguiente) {
            if (iguales(e.clave, clave)) {
                return true;
            }
        }
        return false;
    }

    /** Elimina la clave. @return el valor que tenia (o null). */
    public V eliminar(K clave) {
        int i = indice(clave, cubetas.length);
        Entrada<K, V> antes = null;
        for (Entrada<K, V> e = cubetas[i]; e != null; e = e.siguiente) {
            if (iguales(e.clave, clave)) {
                if (antes == null) {
                    cubetas[i] = e.siguiente;
                } else {
                    antes.siguiente = e.siguiente;
                }
                tamano--;
                orden.remove(clave);
                return e.valor;
            }
            antes = e;
        }
        return null;
    }

    /** REHASH: duplica el arreglo y reparte otra vez todas las entradas. */
    @SuppressWarnings("unchecked")
    private void redimensionar() {
        Entrada<K, V>[] viejas = cubetas;
        cubetas = (Entrada<K, V>[]) new Entrada[viejas.length * 2];
        colisiones = 0;
        for (Entrada<K, V> lista : viejas) {
            for (Entrada<K, V> e = lista; e != null; e = e.siguiente) {
                int i = indice(e.clave, cubetas.length);
                if (cubetas[i] != null) {
                    colisiones++;
                }
                cubetas[i] = new Entrada<>(e.clave, e.valor, cubetas[i]);
            }
        }
    }

    private static boolean iguales(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /** Las claves en el orden en que llegaron. */
    public List<K> claves() {
        return new ArrayList<>(orden);
    }

    public int tamano() {
        return tamano;
    }

    public int cantidadCubetas() {
        return cubetas.length;
    }

    public double factorCarga() {
        return (double) tamano / cubetas.length;
    }

    public int getColisiones() {
        return colisiones;
    }

    /** Largo de la lista mas larga (si la funcion hash es buena, es pequeno). */
    public int cadenaMasLarga() {
        int m = 0;
        for (Entrada<K, V> lista : cubetas) {
            int c = 0;
            for (Entrada<K, V> e = lista; e != null; e = e.siguiente) {
                c++;
            }
            m = Math.max(m, c);
        }
        return m;
    }
}
