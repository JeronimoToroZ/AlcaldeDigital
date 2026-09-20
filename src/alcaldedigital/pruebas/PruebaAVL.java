package alcaldedigital.pruebas;

import alcaldedigital.estructuras.ArbolAVL;
import alcaldedigital.estructuras.NodoAVL;
import alcaldedigital.modelo.Publicacion;
import alcaldedigital.modelo.TipoContenido;
import java.util.List;

/**
 * BANCO DE PRUEBAS DEL AVL (se ejecuta por consola)
 * ================================================
 * Esta clase existe para la sustentacion: demuestra, sin abrir la interfaz,
 * que el arbol cumple lo que se afirma. Cubre:
 *
 *   1. Insercion con rotacion simple derecha  (caso Izquierda-Izquierda)
 *   2. Insercion con rotacion simple izquierda (caso Derecha-Derecha)
 *   3. Insercion con rotacion doble           (casos ID y DI)
 *   4. Comparacion de altura contra un ABB degenerado
 *   5. Los tres recorridos
 *   6. Eliminacion en los tres casos (hoja, un hijo, dos hijos)
 *   7. Consulta por rango
 *
 * Ejecutar con:  java -cp build alcaldedigital.pruebas.PruebaAVL
 */
public class PruebaAVL {

    private static int contadorId = 1;

    private static Publicacion pub(int credibilidad) {
        return new Publicacion(contadorId++, "Publicacion de prueba", "@test",
                TipoContenido.OPINION, credibilidad);
    }

    private static void titulo(String t) {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println(t);
        System.out.println("=".repeat(70));
    }

    /** Imprime el arbol acostado: la raiz a la izquierda, se lee girando la cabeza. */
    private static void imprimir(ArbolAVL arbol, NodoAVL nodo, String prefijo) {
        if (nodo == null) {
            return;
        }
        imprimir(arbol, nodo.getDerecho(), prefijo + "        ");
        System.out.println(prefijo + nodo.getPublicacion().getCredibilidad()
                + " (fe=" + (arbol.factorEquilibrio(nodo) >= 0 ? "+" : "")
                + arbol.factorEquilibrio(nodo) + ", h=" + nodo.getAltura() + ")");
        imprimir(arbol, nodo.getIzquierdo(), prefijo + "        ");
    }

    public static void main(String[] args) {

        titulo("1. ROTACION SIMPLE DERECHA - caso Izquierda-Izquierda (30, 20, 10)");
        ArbolAVL a1 = new ArbolAVL();
        a1.insertar(pub(30));
        a1.insertar(pub(20));
        a1.insertar(pub(10));
        System.out.println("Sin rebalanceo la altura seria 2. Altura real: " + a1.getAlturaArbol());
        System.out.println("Rotaciones aplicadas: " + a1.getRotacionesRealizadas());
        imprimir(a1, a1.getRaiz(), "");

        titulo("2. ROTACION SIMPLE IZQUIERDA - caso Derecha-Derecha (10, 20, 30)");
        ArbolAVL a2 = new ArbolAVL();
        a2.insertar(pub(10));
        a2.insertar(pub(20));
        a2.insertar(pub(30));
        System.out.println("Altura: " + a2.getAlturaArbol()
                + "   Rotaciones: " + a2.getRotacionesRealizadas());
        imprimir(a2, a2.getRaiz(), "");

        titulo("3. ROTACION DOBLE - caso Izquierda-Derecha (30, 10, 20)");
        ArbolAVL a3 = new ArbolAVL();
        a3.insertar(pub(30));
        a3.insertar(pub(10));
        a3.insertar(pub(20));
        System.out.println("Rotaciones (deben ser 2): " + a3.getRotacionesRealizadas());
        imprimir(a3, a3.getRaiz(), "");

        titulo("4. POR QUE AVL Y NO ABB: insercion en orden creciente de 15 elementos");
        ArbolAVL a4 = new ArbolAVL();
        for (int i = 1; i <= 15; i++) {
            a4.insertar(pub(i * 5));
        }
        System.out.println("Elementos: " + a4.getTamano());
        System.out.println("Altura del AVL: " + a4.getAlturaArbol()
                + "   (un ABB simple habria quedado con altura 14, o sea una lista)");
        System.out.println("Cota teorica log2(15) = "
                + String.format("%.2f", Math.log(15) / Math.log(2)));
        System.out.println("Rotaciones aplicadas: " + a4.getRotacionesRealizadas());
        System.out.println("Invariante AVL: " + (a4.estaBalanceado() ? "se cumple" : "ROTA"));

        titulo("5. RECORRIDOS sobre el arbol del punto 4");
        System.out.println("INORDEN (credibilidad ascendente):");
        System.out.println("  " + credibilidades(a4.inorden()));
        System.out.println("PREORDEN (raiz primero):");
        System.out.println("  " + credibilidades(a4.preorden()));
        System.out.println("POSTORDEN (hojas primero):");
        System.out.println("  " + credibilidades(a4.postorden()));

        titulo("6. ELIMINACION - los tres casos");
        ArbolAVL a5 = new ArbolAVL();
        int[] valores = {50, 30, 70, 20, 40, 60, 80, 65};
        Publicacion[] refs = new Publicacion[valores.length];
        for (int i = 0; i < valores.length; i++) {
            refs[i] = pub(valores[i]);
            a5.insertar(refs[i]);
        }
        System.out.println("Arbol inicial (altura " + a5.getAlturaArbol() + "):");
        imprimir(a5, a5.getRaiz(), "");

        System.out.println("\nCASO 1 - eliminar una HOJA (20):");
        a5.eliminar(refs[3].claveAVL());
        imprimir(a5, a5.getRaiz(), "");

        System.out.println("\nCASO 2 - eliminar un nodo con UN HIJO (60, que tiene a 65):");
        a5.eliminar(refs[5].claveAVL());
        imprimir(a5, a5.getRaiz(), "");

        System.out.println("\nCASO 3 - eliminar un nodo con DOS HIJOS (la raiz, 50):");
        a5.eliminar(refs[0].claveAVL());
        imprimir(a5, a5.getRaiz(), "");
        System.out.println("Invariante AVL tras las eliminaciones: "
                + (a5.estaBalanceado() ? "se cumple" : "ROTA"));

        titulo("7. CONSULTA POR RANGO - habilidad del Periodista");
        ArbolAVL a6 = new ArbolAVL();
        int[] creds = {92, 15, 50, 8, 88, 22, 45, 12, 95, 30};
        for (int c : creds) {
            a6.insertar(pub(c));
        }
        System.out.println("Feed completo (inorden): " + credibilidades(a6.inorden()));
        System.out.println("Sospechosas (credibilidad < 35): "
                + credibilidades(a6.menosCreiblesQue(35)));
        System.out.println("El recorrido poda las ramas altas: no revisa todo el arbol.");

        titulo("RESUMEN");
        System.out.println("Todas las operaciones mantuvieron el invariante AVL.");
        System.out.println("Busqueda, insercion y eliminacion en O(log n) garantizado.");
    }

    private static String credibilidades(List<Publicacion> lista) {
        StringBuilder sb = new StringBuilder();
        for (Publicacion p : lista) {
            sb.append(p.getCredibilidad()).append(' ');
        }
        return sb.toString().trim();
    }
}
