package influenceinc.pruebas;

import influenceinc.estructuras.ArbolBinarioBusqueda;
import influenceinc.estructuras.GrafoCiudad;
import influenceinc.estructuras.GrafoRed;
import influenceinc.estructuras.Ordenamientos;
import influenceinc.estructuras.TablaHash;
import influenceinc.estructuras.Vertice;
import influenceinc.logica.CiudadEstado;
import influenceinc.modelo.TipoCuenta;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * PRUEBAS DEL TEMARIO DEL SEMESTRE (consola).
 * Cada bloque se puede mostrar en la sustentacion:
 *   1. ABB y sus recorridos (recursivos = iterativos)
 *   2. Tabla hash (colisiones y rehash)
 *   3. Los cinco ordenamientos dan el mismo resultado
 *   4. Floyd-Warshall coincide con Dijkstra
 *   5. Kruskal y Prim pesan lo mismo
 *   6. Ford-Fulkerson en una red pequena
 *   7. Bellman-Ford encuentra la camara de eco de los bots
 */
public class PruebaTemario {

    private static int fallos = 0;

    public static void main(String[] args) {
        abb();
        hash();
        ordenamientos();
        grafosCiudad();
        bellmanFord();
        System.out.println(fallos == 0 ? "\nTODAS LAS PRUEBAS PASARON" : "\nFALLARON " + fallos + " PRUEBAS");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ 1
    private static void abb() {
        System.out.println("=== ABB (agenda de contactos) ===");
        ArbolBinarioBusqueda<String, String> a = new ArbolBinarioBusqueda<>();
        //            Lorena
        //          /       \
        //       Daniela    Ramiro
        //       /    \        \
        //   Corresp.  Kata    Vecinos
        String[] nombres = {"Lorena", "Daniela", "Ramiro", "Corresponsal", "Kata", "Vecinos"};
        for (String n : nombres) {
            a.insertar(n, "tel. de " + n);
        }
        System.out.println("   preorden  : " + a.preordenRecursivo());
        System.out.println("   inorden   : " + a.inordenRecursivo());
        System.out.println("   postorden : " + a.postordenRecursivo());
        System.out.println("   niveles   : " + a.porNiveles());
        comprobar(a.preordenRecursivo().equals(a.preordenIterativo()), "preorden recursivo = iterativo (pila)");
        comprobar(a.inordenRecursivo().equals(a.inordenIterativo()), "inorden recursivo = iterativo (pila)");
        comprobar(a.postordenRecursivo().equals(a.postordenIterativo()), "postorden recursivo = iterativo (dos pilas)");
        comprobar(a.porNiveles().equals(Arrays.asList("Lorena", "Daniela", "Ramiro", "Corresponsal", "Kata", "Vecinos")),
                "por niveles (cola)");
        List<String> orden = new ArrayList<>(Arrays.asList(nombres));
        java.util.Collections.sort(orden);
        comprobar(a.inordenIterativo().equals(orden), "el inorden de un ABB sale ordenado alfabeticamente");
        comprobar(a.buscar("Kata") != null && a.getUltimaBusqueda() == 3, "buscar Kata: 3 comparaciones (Lorena, Daniela, Kata)");
        comprobar(a.buscar("Nadie") == null, "buscar algo que no esta da null");
        comprobar(a.altura() == 2, "altura 2");
        a.eliminar("Daniela");                 // dos hijos: lo reemplaza el sucesor (Kata)
        comprobar(a.porNiveles().get(1).equals("Kata") && a.getTamano() == 5, "eliminar con dos hijos: sube el sucesor");
        a.eliminar("Vecinos");                 // hoja
        a.eliminar("Ramiro");                  // queda sin hijos
        comprobar(a.inordenIterativo().equals(Arrays.asList("Corresponsal", "Kata", "Lorena")), "eliminar hoja y nodo con un hijo");
        // aleatorio grande: los recorridos coinciden siempre
        ArbolBinarioBusqueda<Integer, Integer> b = new ArbolBinarioBusqueda<>();
        Random r = new Random(3);
        for (int i = 0; i < 300; i++) {
            int x = r.nextInt(1000);
            b.insertar(x, x);
        }
        comprobar(b.preordenRecursivo().equals(b.preordenIterativo()) && b.inordenRecursivo().equals(b.inordenIterativo())
                && b.postordenRecursivo().equals(b.postordenIterativo()), "300 claves al azar: recorridos iguales");
    }

    // ------------------------------------------------------------------ 2
    private static void hash() {
        System.out.println("\n=== TABLA HASH (mensajes del celular) ===");
        TablaHash<String, Integer> t = new TablaHash<>(4);
        for (int i = 0; i < 40; i++) {
            t.poner("contacto" + i, i);
        }
        System.out.println("   40 claves -> " + t.cantidadCubetas() + " cubetas, factor de carga "
                + String.format("%.2f", t.factorCarga()) + ", cadena mas larga " + t.cadenaMasLarga());
        comprobar(t.tamano() == 40 && t.cantidadCubetas() >= 64, "rehash: el arreglo crecio de 4 a " + t.cantidadCubetas());
        comprobar(t.factorCarga() <= 0.75, "el factor de carga queda por debajo de 0,75");
        comprobar(t.obtener("contacto17") == 17, "obtener");
        t.poner("contacto17", 99);
        comprobar(t.obtener("contacto17") == 99 && t.tamano() == 40, "poner una clave existente reemplaza");
        comprobar(t.eliminar("contacto3") == 3 && !t.contiene("contacto3") && t.tamano() == 39, "eliminar");
        comprobar(t.claves().get(0).equals("contacto0"), "recorrido en orden de llegada");
        // colisiones a proposito: "Aa" y "BB" tienen el mismo hashCode en Java
        TablaHash<String, String> c = new TablaHash<>(8);
        c.poner("Aa", "uno");
        c.poner("BB", "dos");
        comprobar("Aa".hashCode() == "BB".hashCode() && c.getColisiones() == 1, "\"Aa\" y \"BB\" chocan en la misma cubeta");
        comprobar(c.obtener("Aa").equals("uno") && c.obtener("BB").equals("dos"), "el encadenamiento las separa");
    }

    // ------------------------------------------------------------------ 3
    private static void ordenamientos() {
        System.out.println("\n=== ORDENAMIENTOS ===");
        Random r = new Random(11);
        List<Integer> base = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            base.add(r.nextInt(500));
        }
        Comparator<Integer> asc = new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Integer.compare(a, b);
            }
        };
        List<Integer> esperado = new ArrayList<>(base);
        java.util.Collections.sort(esperado);
        String[] nombres = {"burbuja", "insercion", "shell", "quick", "heap"};
        for (String n : nombres) {
            List<Integer> l = new ArrayList<>(base);
            int comp;
            switch (n) {
                case "burbuja": comp = Ordenamientos.burbuja(l, asc); break;
                case "insercion": comp = Ordenamientos.insercion(l, asc); break;
                case "shell": comp = Ordenamientos.shell(l, asc); break;
                case "quick": comp = Ordenamientos.quick(l, asc); break;
                default: comp = Ordenamientos.heap(l, asc); break;
            }
            comprobar(l.equals(esperado), String.format("%-9s ordena 200 numeros (%d comparaciones)", n, comp));
        }
        List<Integer> casi = new ArrayList<>(esperado);
        java.util.Collections.swap(casi, 10, 11);
        int compIns = Ordenamientos.insercion(casi, asc);
        comprobar(compIns < 250, "insercion con una lista casi ordenada: solo " + compIns + " comparaciones (casi O(n))");
        List<Integer> vacia = new ArrayList<>();
        Ordenamientos.quick(vacia, asc);
        Ordenamientos.heap(vacia, asc);
        comprobar(vacia.isEmpty(), "listas vacias no fallan");
    }

    // ------------------------------------------------------------------ 4, 5, 6
    private static void grafosCiudad() {
        System.out.println("\n=== GRAFO DE LA CIUDAD: Floyd-Warshall, Kruskal, Prim, Ford-Fulkerson ===");
        CiudadEstado c = new CiudadEstado(1);
        GrafoCiudad m = c.mapa;
        int[][] d = m.floydWarshall();
        boolean iguales = true;
        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d.length; j++) {
                if (d[i][j] != m.largo(m.caminoMasCorto(i, j))) {
                    iguales = false;
                }
            }
        }
        comprobar(iguales, "Floyd-Warshall da lo mismo que Dijkstra para los " + d.length * d.length + " pares");
        int centro = GrafoCiudad.centro(d);
        System.out.println("   centro del grafo (minima excentricidad): " + m.zona(centro).nombre);
        comprobar(centro == 0, "el centro es la Plaza Central");

        List<int[]> k = m.kruskal();
        List<int[]> p = m.prim(0);
        List<int[]> p2 = m.prim(5);
        System.out.println("   Kruskal: " + aristas(m, k) + "  peso " + GrafoCiudad.pesoTotal(k));
        System.out.println("   Prim   : " + aristas(m, p) + "  peso " + GrafoCiudad.pesoTotal(p));
        comprobar(k.size() == d.length - 1 && p.size() == d.length - 1, "los dos arboles tienen V-1 = " + (d.length - 1) + " calles");
        comprobar(GrafoCiudad.pesoTotal(k) == GrafoCiudad.pesoTotal(p), "Kruskal y Prim pesan lo mismo");
        comprobar(GrafoCiudad.pesoTotal(p) == GrafoCiudad.pesoTotal(p2), "Prim da el mismo peso desde cualquier zona");
        int todas = 0;
        for (int[] calle : m.calles()) {
            todas += m.largoCalle(calle[0], calle[1]);
        }
        comprobar(GrafoCiudad.pesoTotal(k) < todas, "la red de alerta usa menos cable que todas las calles");

        // red clasica de libro: flujo maximo 23
        int[][] cap = new int[6][6];
        cap[0][1] = 16;
        cap[0][2] = 13;
        cap[1][2] = 10;
        cap[2][1] = 4;
        cap[1][3] = 12;
        cap[3][2] = 9;
        cap[2][4] = 14;
        cap[4][3] = 7;
        cap[3][5] = 20;
        cap[4][5] = 4;
        comprobar(GrafoCiudad.flujoMaximo(cap, 0, 5) == 23, "Ford-Fulkerson: flujo maximo de la red de ejemplo = 23");
        comprobar(cap[0][1] == 16, "la matriz de capacidades no se modifica");
        int libre = c.flujoVotantes();
        for (int z = 2; z < c.panico.length; z++) {
            c.panico[z] = 1;
        }
        int conPanico = c.flujoVotantes();
        System.out.println("   votantes por hora hacia las urnas: " + libre + " (con panico en todos los barrios: " + conPanico + ")");
        comprobar(conPanico < libre, "el panico en los barrios reduce el flujo de votantes");
    }

    private static String aristas(GrafoCiudad m, List<int[]> l) {
        StringBuilder sb = new StringBuilder();
        for (int[] a : l) {
            sb.append(sb.length() == 0 ? "" : ", ").append(m.zona(a[0]).nombre.split(" ")[0]).append('-')
                    .append(m.zona(a[1]).nombre.split(" ")[0]);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 7
    private static void bellmanFord() {
        System.out.println("\n=== BELLMAN-FORD (camaras de eco) ===");
        GrafoRed g = new GrafoRed();
        Vertice ana = new Vertice("ana", "Ana", TipoCuenta.USUARIO, 0, 0, 0, 0);
        Vertice luis = new Vertice("luis", "Luis", TipoCuenta.USUARIO, 0, 0, 0, 0);
        Vertice b1 = new Vertice("b1", "@bot1", TipoCuenta.BOT, -1, 0, 0, 0);
        Vertice b2 = new Vertice("b2", "@bot2", TipoCuenta.BOT, -1, 0, 0, 0);
        Vertice b3 = new Vertice("b3", "@bot3", TipoCuenta.BOT, -1, 0, 0, 0);
        for (Vertice v : new Vertice[]{ana, luis, b1, b2, b3}) {
            g.agregarVertice(v);
        }
        g.conectar(ana, luis, 0.5, 3);
        g.conectar(b1, b2, 0.9, 1);
        g.conectar(b2, b3, 0.9, 1);
        g.conectar(b3, b1, 0.9, 1);
        g.conectar(b1, ana, 0.9, 2);
        GrafoRed.ResultadoBellmanFord desdeAna = g.bellmanFord(ana);
        comprobar(!desdeAna.cicloNegativo && desdeAna.distancia.get("luis") == 3, "desde Ana: sin ciclo, Luis a costo 3");
        GrafoRed.ResultadoBellmanFord desdeBot = g.bellmanFord(b1);
        comprobar(desdeBot.cicloNegativo, "desde un bot: el anillo b1->b2->b3->b1 pesa -3 (camara de eco)");
        comprobar(desdeBot.enCiclo.contains(b2), "las cuentas del anillo quedan marcadas");
        CiudadEstado c = new CiudadEstado(2);
        comprobar(c.red.bellmanFord(c.bots.get(0)).cicloNegativo, "en Ciudad Nova el anillo de cuentas falsas es una camara de eco");
        comprobar(!c.red.bellmanFord(c.medio).cicloNegativo, "desde Noticias Nova no se llega a ninguna");
    }

    private static void comprobar(boolean ok, String que) {
        System.out.println("   " + (ok ? "[OK]   " : "[FALLA]") + " " + que);
        if (!ok) {
            fallos++;
        }
    }
}
