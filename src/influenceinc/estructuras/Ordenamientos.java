package influenceinc.estructuras;

import java.util.Comparator;
import java.util.List;

/**
 * ALGORITMOS DE ORDENAMIENTO
 * ==========================
 * Todos ordenan una lista EN SU LUGAR segun un Comparator y devuelven
 * cuantas comparaciones hicieron (para comparar su costo en las pruebas).
 *
 *   burbuja    O(n^2)          marcador de la partida en red (2 a 4 filas)
 *   insercion  O(n^2), O(n) si ya viene casi ordenada
 *                              titulares del noticiero (pocos y casi ordenados)
 *   shell      ~O(n^1.3)       tendencias de la app Noticias
 *   quick      O(n log n) promedio
 *                              ranking final de jugadores
 *   heap       O(n log n) siempre, sin memoria extra
 *                              notas del minijuego de sellos (por tiempo)
 *
 * Se escogio uno distinto para cada lugar segun el tamano y la forma de los
 * datos; en la seccion 11 de SUSTENTACION.md esta la justificacion.
 */
public final class Ordenamientos {

    private Ordenamientos() {
    }

    private static <T> void intercambiar(List<T> l, int i, int j) {
        T x = l.get(i);
        l.set(i, l.get(j));
        l.set(j, x);
    }

    // ------------------------------------------------------------ burbuja
    /** BURBUJA: en cada pasada el mayor "sube" al final. Si una pasada no cambia nada, ya esta ordenada. */
    public static <T> int burbuja(List<T> l, Comparator<T> c) {
        int comp = 0;
        int n = l.size();
        for (int pasada = 0; pasada < n - 1; pasada++) {
            boolean cambio = false;
            for (int i = 0; i < n - 1 - pasada; i++) {
                comp++;
                if (c.compare(l.get(i), l.get(i + 1)) > 0) {
                    intercambiar(l, i, i + 1);
                    cambio = true;
                }
            }
            if (!cambio) {
                break;
            }
        }
        return comp;
    }

    // ------------------------------------------------------------ insercion
    /** INSERCION: cada elemento se corre hacia la izquierda hasta su lugar. */
    public static <T> int insercion(List<T> l, Comparator<T> c) {
        int comp = 0;
        for (int i = 1; i < l.size(); i++) {
            T x = l.get(i);
            int j = i - 1;
            while (j >= 0) {
                comp++;
                if (c.compare(l.get(j), x) <= 0) {
                    break;
                }
                l.set(j + 1, l.get(j));
                j--;
            }
            l.set(j + 1, x);
        }
        return comp;
    }

    // ------------------------------------------------------------ shell
    /** SHELL: insercion con saltos que se van reduciendo a la mitad (n/2, n/4, ..., 1). */
    public static <T> int shell(List<T> l, Comparator<T> c) {
        int comp = 0;
        for (int salto = l.size() / 2; salto > 0; salto /= 2) {
            for (int i = salto; i < l.size(); i++) {
                T x = l.get(i);
                int j = i;
                while (j >= salto) {
                    comp++;
                    if (c.compare(l.get(j - salto), x) <= 0) {
                        break;
                    }
                    l.set(j, l.get(j - salto));
                    j -= salto;
                }
                l.set(j, x);
            }
        }
        return comp;
    }

    // ------------------------------------------------------------ quick
    /** QUICK SORT: se escoge un pivote, se parte en menores y mayores y se ordena cada parte. */
    public static <T> int quick(List<T> l, Comparator<T> c) {
        int[] comp = new int[1];
        quick(l, 0, l.size() - 1, c, comp);
        return comp[0];
    }

    private static <T> void quick(List<T> l, int ini, int fin, Comparator<T> c, int[] comp) {
        if (ini >= fin) {
            return;
        }
        int p = particion(l, ini, fin, c, comp);
        quick(l, ini, p - 1, c, comp);
        quick(l, p + 1, fin, c, comp);
    }

    /** Particion de Lomuto con el elemento del MEDIO como pivote (evita el peor caso con listas ordenadas). */
    private static <T> int particion(List<T> l, int ini, int fin, Comparator<T> c, int[] comp) {
        intercambiar(l, (ini + fin) / 2, fin);
        T pivote = l.get(fin);
        int i = ini;
        for (int j = ini; j < fin; j++) {
            comp[0]++;
            if (c.compare(l.get(j), pivote) < 0) {
                intercambiar(l, i, j);
                i++;
            }
        }
        intercambiar(l, i, fin);
        return i;
    }

    // ------------------------------------------------------------ heap
    /**
     * HEAP SORT: se arma un monticulo de maximos sobre la misma lista y se
     * saca la raiz (el mayor) al final, una y otra vez.
     */
    public static <T> int heap(List<T> l, Comparator<T> c) {
        int[] comp = new int[1];
        int n = l.size();
        for (int i = n / 2 - 1; i >= 0; i--) {
            hundir(l, i, n, c, comp);
        }
        for (int fin = n - 1; fin > 0; fin--) {
            intercambiar(l, 0, fin);
            hundir(l, 0, fin, c, comp);
        }
        return comp[0];
    }

    /** Baja el elemento i hasta que sea mayor que sus dos hijos (dentro de los primeros n). */
    private static <T> void hundir(List<T> l, int i, int n, Comparator<T> c, int[] comp) {
        while (true) {
            int mayor = i, izq = 2 * i + 1, der = 2 * i + 2;
            if (izq < n) {
                comp[0]++;
                if (c.compare(l.get(izq), l.get(mayor)) > 0) {
                    mayor = izq;
                }
            }
            if (der < n) {
                comp[0]++;
                if (c.compare(l.get(der), l.get(mayor)) > 0) {
                    mayor = der;
                }
            }
            if (mayor == i) {
                return;
            }
            intercambiar(l, i, mayor);
            i = mayor;
        }
    }

    /** true si la lista quedo en orden (para las pruebas). */
    public static <T> boolean ordenada(List<T> l, Comparator<T> c) {
        for (int i = 1; i < l.size(); i++) {
            if (c.compare(l.get(i - 1), l.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }
}
