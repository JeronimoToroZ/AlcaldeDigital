package alcaldedigital.pruebas;

import alcaldedigital.logica.GrafoSocial;
import alcaldedigital.logica.NodoRed;

import java.util.List;

/**
 * Banco de pruebas por consola del grafo social.
 *
 * Se corre sin abrir la interfaz: demuestra que BFS, DFS y Dijkstra funcionan
 * de verdad sobre la estructura, independiente del juego.
 *
 * Para ejecutarlo en NetBeans: clic derecho sobre este archivo -> Run File.
 */
public class PruebaGrafoSocial {

    public static void main(String[] args) {
        GrafoSocial grafo = new GrafoSocial();
        grafo.inicializarMapaPrueba();

        imprimirTitulo("CASO 1 - Lista de adyacencia");
        for (NodoRed n : grafo.nodos) {
            StringBuilder sb = new StringBuilder();
            sb.append(ajustar(n.nombre)).append(" -> ");
            for (int i = 0; i < n.vecinos.size(); i++) {
                NodoRed v = n.vecinos.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(v.nombre).append("(").append(n.pesoHacia(v)).append(")");
            }
            System.out.println(sb);
        }
        System.out.println("Nodos: " + grafo.nodos.size());

        imprimirTitulo("CASO 2 - BFS por oleadas desde el Parque");
        NodoRed parque = grafo.buscar("Parque");
        List<List<NodoRed>> oleadas = grafo.propagarRumorBFS(parque);
        for (int i = 0; i < oleadas.size(); i++) {
            System.out.println("Oleada " + i + " (a " + i + " pasos): " + oleadas.get(i));
        }
        System.out.println("El BFS alcanzo " + contar(oleadas) + " de " + grafo.nodos.size() + " nodos.");
        System.out.println("Comprobacion: ningun nodo aparece dos veces -> "
                + (contar(oleadas) == grafo.nodos.size() ? "OK" : "revisar"));

        imprimirTitulo("CASO 3 - Propagacion paso a paso (lo que anima el juego)");
        System.out.println("Infectados al inicio: " + grafo.infectados());
        int paso = 1;
        List<NodoRed> nuevos;
        while (!(nuevos = grafo.expandirUnaOleada()).isEmpty()) {
            System.out.println("Paso " + paso + ": se contagian " + nuevos);
            paso++;
        }
        System.out.println("Infectados al final: " + grafo.infectados().size() + " de " + grafo.nodos.size());

        imprimirTitulo("CASO 4 - DFS: componentes conexas");
        List<List<NodoRed>> componentes = grafo.componentesConexas();
        System.out.println("Componentes encontradas: " + componentes.size());
        for (int i = 0; i < componentes.size(); i++) {
            System.out.println("  Componente " + (i + 1) + ": " + componentes.get(i));
        }
        System.out.println("El grafo es conexo: " + grafo.esConexo());

        imprimirTitulo("CASO 5 - DFS detecta una camara de eco");
        GrafoSocial aislado = new GrafoSocial();
        aislado.inicializarMapaPrueba();
        NodoRed villaMarina = new NodoRed("Villa Marina", 880, 120);
        NodoRed muelle = new NodoRed("Muelle", 930, 300);
        villaMarina.agregarConexion(muelle, 2);
        aislado.agregarNodo(villaMarina);
        aislado.agregarNodo(muelle);
        System.out.println("Se agrego un barrio sin conexion al resto de la ciudad.");
        List<List<NodoRed>> comps2 = aislado.componentesConexas();
        System.out.println("Componentes ahora: " + comps2.size() + " (se esperaban 2)");
        for (List<NodoRed> c : comps2) {
            System.out.println("  " + c);
        }
        System.out.println("Es conexo: " + aislado.esConexo() + " (se esperaba false)");

        imprimirTitulo("CASO 6 - Dijkstra: campana desde la Alcaldia");
        GrafoSocial g2 = new GrafoSocial();
        g2.inicializarMapaPrueba();
        NodoRed alcaldia = g2.buscar("Alcaldía");
        String[] destinos = {"Café", "Parque", "Biblioteca", "Mercado"};
        for (String destino : destinos) {
            GrafoSocial.Ruta r = g2.caminoMinimo(alcaldia, g2.buscar(destino));
            System.out.println("Alcaldia -> " + ajustar(destino) + " : " + r);
        }

        imprimirTitulo("CASO 7 - Dijkstra elige el foco mas barato de apagar");
        GrafoSocial g3 = new GrafoSocial();
        g3.inicializarMapaPrueba();
        g3.buscar("Café").estaInfectado = true;
        g3.buscar("Mercado").estaInfectado = true;
        System.out.println("Focos activos: " + g3.infectados());
        GrafoSocial.Ruta mejor = g3.rutaAlInfectadoMasCercano(g3.buscar("Alcaldía"));
        System.out.println("La campana sale hacia: " + mejor);

        imprimirTitulo("FIN");
        System.out.println("Si los 7 casos imprimieron lo esperado, los tres algoritmos funcionan.");
    }

    private static int contar(List<List<NodoRed>> oleadas) {
        int total = 0;
        for (List<NodoRed> o : oleadas) {
            total += o.size();
        }
        return total;
    }

    private static String ajustar(String texto) {
        StringBuilder sb = new StringBuilder(texto);
        while (sb.length() < 12) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static void imprimirTitulo(String titulo) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println(" " + titulo);
        System.out.println("============================================================");
    }
}
