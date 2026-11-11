package influenceinc.pruebas;

import influenceinc.estructuras.ArbolConsecuencias;
import influenceinc.estructuras.ArbolDialogo;
import influenceinc.estructuras.GrafoCiudad;
import influenceinc.estructuras.GrafoRed;
import influenceinc.estructuras.NodoDialogo;
import influenceinc.estructuras.Vertice;
import influenceinc.modelo.TipoCuenta;
import influenceinc.modelo.Veredicto;
import influenceinc.modelo.Verdad;
import java.util.List;
import java.util.Random;

/**
 * BANCO DE PRUEBAS DE LOS GRAFOS Y LOS ARBOLES N-ARIOS (consola)
 *
 * Para la sustentacion: demuestra cada operacion sobre un grafo pequeno que se
 * puede dibujar en el tablero.
 *
 *        Ana ----> Luis ----> Carlos
 *         |  \                  |
 *         v   v                 v
 *       Pedro  Maria <-------- Sofi          Bot (aislado al principio)
 */
public class PruebaEstructuras {

    private static int fallos = 0;

    public static void main(String[] args) {
        GrafoRed g = new GrafoRed();
        Vertice ana = v(g, "ana", "Ana");
        Vertice luis = v(g, "luis", "Luis");
        Vertice carlos = v(g, "carlos", "Carlos");
        Vertice pedro = v(g, "pedro", "Pedro");
        Vertice maria = v(g, "maria", "Maria");
        Vertice sofi = v(g, "sofi", "Sofi");
        g.conectar(ana, luis, 0.9, 2);
        g.conectar(luis, carlos, 0.9, 2);
        g.conectar(ana, pedro, 0.9, 7);
        g.conectar(ana, maria, 0.9, 9);
        g.conectar(carlos, sofi, 0.9, 1);
        g.conectar(sofi, maria, 0.9, 1);

        System.out.println("=== GRAFO: operaciones basicas ===");
        comprobar(g.cantidadVertices() == 6 && g.cantidadAristas() == 6, "6 vertices y 6 aristas");
        comprobar(g.vecinosSalida(ana).size() == 3, "Ana le llega a 3 cuentas (grado de salida)");
        comprobar(maria.gradoEntrada() == 2, "Maria recibe de 2 cuentas (grado de entrada)");
        comprobar(luis.aristaHacia(ana) == null, "el grafo es dirigido: Luis no le publica a Ana");

        System.out.println("\n=== BFS por oleadas desde Ana ===");
        List<List<Vertice>> olas = g.bfsOleadas(ana, -1);
        for (int i = 0; i < olas.size(); i++) {
            System.out.println("   oleada " + i + ": " + olas.get(i));
        }
        comprobar(olas.size() == 4 && olas.get(1).size() == 3, "oleada 1 = Luis, Pedro, Maria");
        comprobar(g.alcanceBFS(ana, 1) == 3, "alcance a 1 salto = 3");

        System.out.println("\n=== DFS (audiencia potencial) desde Luis ===");
        System.out.println("   orden DFS: " + g.dfsAlcanzables(luis));
        comprobar(g.dfsAlcanzables(luis).size() == 4, "desde Luis se llega a Luis, Carlos, Sofi y Maria");

        System.out.println("\n=== Dijkstra: equipo de verificación de Ana a Maria ===");
        GrafoRed.Ruta r = g.caminoMinimo(ana, maria);
        System.out.println("   " + r);
        comprobar(r.costo == 6, "la ruta larga (costo 6) le gana a la directa (costo 9)");

        System.out.println("\n=== Propagacion y desmentido ===");
        GrafoRed.ResultadoPropagacion p = g.propagar(ana, 3, a -> 1.0, x -> true, new Random(1));
        comprobar(p.alcance() == 5, "con probabilidad 1 la publicación llega a las 5 cuentas");
        GrafoRed.ResultadoPropagacion p0 = g.propagar(ana, 3, a -> 0.0, x -> true, new Random(1));
        comprobar(p0.alcance() == 0, "con probabilidad 0 no llega a nadie");
        for (Vertice x : p.alcanzados()) {
            x.desinformado = true;
        }
        luis.yaRetransmitio = true;
        GrafoRed.ResultadoCorreccion c = g.corregir(carlos, 1);
        System.out.println("   desmentido desde Carlos (1 salto) corrigió a: " + c.corregidos);
        comprobar(!c.corregidos.contains(luis) && luis.desinformado,
                "a Luis no lo corrige: ya había reenviado el bulo");
        comprobar(maria.desinformado, "Maria esta a 2 saltos: el desmentido no le llega");

        System.out.println("\n=== Insertar y eliminar ===");
        Vertice bot = v(g, "bot", "@bot");
        bot.desinformado = false;
        comprobar(g.comunidades().size() == 2, "el bot sin conexiones forma su propia comunidad (DFS)");
        g.conectar(bot, pedro, 0.8, 2);
        comprobar(g.comunidades().size() == 1, "al conectarlo, la red vuelve a ser una sola");
        g.eliminarVertice("bot");
        comprobar(!g.contiene("bot") && pedro.gradoEntrada() == 1, "eliminar el bot borra también sus aristas");
        g.desconectar(ana, pedro);
        comprobar(g.comunidades().size() == 2, "sin la arista Ana->Pedro, Pedro queda aislado");

        System.out.println("\n=== ÁRBOL N-ARIO DE DIÁLOGO ===");
        ArbolDialogo d = new ArbolDialogo("Hola", "neutral");
        NodoDialogo p1 = d.pregunta("¿Fuente?", "WhatsApp", "nervioso", "fuente: WhatsApp");
        d.pregunta("¿Le pagaron?", "No", "neutral", null);
        p1.hijo(new NodoDialogo("¿Vio el documento?", "No", "nervioso", "sin documento"));
        comprobar(d.cantidadNodos() == 4 && d.altura() == 2, "4 nodos, altura 2");
        comprobar(d.opciones(d.getRaiz()).size() == 2, "desde el saludo hay 2 preguntas");
        p1.visitado = true;
        comprobar(d.opciones(p1).size() == 1, "al bajar por ¿Fuente? aparece su pregunta hija");
        NodoDialogo conf = new NodoDialogo("Su cuenta es falsa", "...", "roto", "confesó");
        d.insertar(d.getRaiz(), conf);
        comprobar(d.cantidadNodos() == 5 && conf.insertado && d.getRaiz().getHijos().get(0) == conf,
                "INSERTAR una confrontación: queda de primera y marcada");
        comprobar(d.preorden().get(0) == d.getRaiz() && d.preorden().size() == 5, "preorden recorre los 5 nodos");
        comprobar(d.eliminar(conf) && d.cantidadNodos() == 4, "ELIMINAR la confrontación");

        System.out.println("\n=== ÁRBOL DE CONSECUENCIAS ===");
        ArbolConsecuencias ac = new ArbolConsecuencias();
        comprobar(ac.cantidadNodos() == 31 && ac.altura() == 2, "raíz + 5 sellos + 25 hojas = 31 nodos");
        ArbolConsecuencias.Nodo h = ac.resolver(Veredicto.DENUNCIAR, Verdad.IMPOSTOR);
        comprobar(h.esHoja() && h.efecto.acierto, "Denunciar -> Impostor es un acierto");
        System.out.println("   ruta: " + ac.rutaHasta(h));
        comprobar(ac.resolver(Veredicto.PUBLICAR, Verdad.FALSA).efecto.grave, "Publicar un bulo es error grave");
        comprobar(ac.eliminar(Veredicto.OPINION) && ac.sellos().size() == 4 && ac.resolver(Veredicto.OPINION,
                Verdad.OPINION) == null, "veda electoral: ELIMINAR la rama OPINIÓN");
        comprobar(ac.insertar(Veredicto.OPINION) && ac.sellos().get(1) == Veredicto.OPINION,
                "INSERTAR la rama de nuevo, en su posición");
        int aciertos = 0;
        for (Veredicto v : Veredicto.values()) {
            for (Verdad t : Verdad.values()) {
                if (ac.resolver(v, t).efecto.acierto) {
                    aciertos++;
                }
            }
        }
        comprobar(aciertos == 5, "exactamente un sello correcto por cada tipo de verdad");

        System.out.println("\n=== GRAFO DE LA CIUDAD (Dijkstra del corresponsal, BFS del pánico) ===");
        GrafoCiudad ciudad = new GrafoCiudad();
        ciudad.agregarZona("Plaza", 0, 0);
        ciudad.agregarZona("Torre", 300, 0);
        ciudad.agregarZona("Norte", 100, 100);
        ciudad.agregarZona("Sur", 200, -100);
        ciudad.agregarCalle(0, 1);      // directa: 300
        ciudad.agregarCalle(0, 2);
        ciudad.agregarCalle(2, 3);
        ciudad.agregarCalle(3, 1);
        java.util.List<GrafoCiudad.Zona> camino = ciudad.caminoMasCorto(0, 1);
        System.out.println("   Plaza -> Torre: " + camino + " (largo " + ciudad.largo(camino) + ")");
        comprobar(camino.size() == 2 && ciudad.largo(camino) == 300, "la calle directa es la más corta");
        ciudad.quitarCalle(0, 1);
        camino = ciudad.caminoMasCorto(0, 1);
        System.out.println("   con la calle cerrada: " + camino + " (largo " + ciudad.largo(camino) + ")");
        comprobar(camino.size() == 4, "al cerrar la calle, Dijkstra encuentra el desvio");
        comprobar(ciudad.saltosDesde(0)[1] == 3, "BFS: la torre queda a 3 calles");

        System.out.println(fallos == 0 ? "\nTODAS LAS PRUEBAS PASARON" : "\nFALLOS: " + fallos);
    }

    private static Vertice v(GrafoRed g, String id, String nombre) {
        Vertice x = new Vertice(id, nombre, TipoCuenta.USUARIO, 0, 0, 0, 0);
        g.agregarVertice(x);
        return x;
    }

    private static void comprobar(boolean ok, String que) {
        System.out.println((ok ? "   [OK]    " : "   [FALLA] ") + que);
        if (!ok) {
            fallos++;
        }
    }
}
