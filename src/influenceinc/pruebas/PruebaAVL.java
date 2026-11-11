package influenceinc.pruebas;

import influenceinc.estructuras.ArbolAVLGenerico;
import influenceinc.logica.Historia;
import influenceinc.modelo.CuentaOficial;
import java.util.List;

/**
 * BANCO DE PRUEBAS DEL AVL DEL DIRECTORIO OFICIAL (consola).
 * Ejecutar: clic derecho → Run File en NetBeans.
 */
public class PruebaAVL {

    private static int fallos = 0;

    public static void main(String[] args) {
        System.out.println("=== INSERTAR en orden (el peor caso de un ABB) ===");
        ArbolAVLGenerico<Integer, String> a = new ArbolAVLGenerico<>();
        for (int i = 1; i <= 15; i++) {
            a.insertar(i, "v" + i);
        }
        comprobar(a.getTamano() == 15 && a.estaBalanceado(), "15 claves y sigue balanceado");
        comprobar(a.getAltura() == 3, "altura 3, contada en aristas (un ABB sin balancear tendría 14)");
        comprobar(a.getRotaciones() > 0, "hizo " + a.getRotaciones() + " rotaciones");
        comprobar("v7".equals(a.buscar(7)) && a.getUltimaBusquedaPasos() <= 4, "buscar 7 en <= 4 pasos");

        System.out.println("\n=== ELIMINAR (hoja, un hijo, dos hijos) ===");
        comprobar(a.eliminar(1), "eliminar una hoja");
        comprobar(a.eliminar(2), "eliminar un nodo con un hijo");
        comprobar(a.eliminar(8), "eliminar la raíz (dos hijos: sube el sucesor)");
        comprobar(!a.contiene(8) && a.getTamano() == 12 && a.estaBalanceado(), "12 claves, balanceado");
        comprobar(!a.eliminar(99), "eliminar algo que no existe devuelve false");
        List<String> in = a.inorden();
        comprobar(in.get(0).equals("v3") && in.get(in.size() - 1).equals("v15"), "inorden sigue ordenado");

        System.out.println("\n=== EL DIRECTORIO DEL JUEGO ===");
        ArbolAVLGenerico<String, CuentaOficial> d = new ArbolAVLGenerico<>();
        for (CuentaOficial c : Historia.directorioInicial()) {
            d.insertar(c.arroba, c);
        }
        comprobar(d.getTamano() == 11 && d.estaBalanceado(), "11 cuentas oficiales");
        comprobar(d.buscar("@acueducto_nova") != null, "@acueducto_nova es oficial");
        comprobar(d.buscar("@acueducto_nova_oficial") == null, "@acueducto_nova_oficial NO está: es la impostora");
        comprobar(d.conPrefijo("@acued").size() == 1, "sugerencia por prefijo: ¿quisiste decir @acueducto_nova?");
        d.eliminar("@alcaldia_nova_2019");
        d.insertar("@registraduria_nova", Historia.registraduria());
        comprobar(d.getTamano() == 11 && d.estaBalanceado() && d.contiene("@registraduria_nova"),
                "día 3 elimina y día 5 inserta: sigue balanceado");

        System.out.println(fallos == 0 ? "\nTODAS LAS PRUEBAS PASARON" : "\nFALLOS: " + fallos);
    }

    private static void comprobar(boolean ok, String que) {
        System.out.println((ok ? "  [OK]    " : "  [FALLA] ") + que);
        if (!ok) {
            fallos++;
        }
    }
}
