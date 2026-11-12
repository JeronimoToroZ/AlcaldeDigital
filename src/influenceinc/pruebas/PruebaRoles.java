package influenceinc.pruebas;

import influenceinc.logica.Caso;
import influenceinc.logica.Habilidades;
import influenceinc.logica.Jornada;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Partida;
import influenceinc.logica.Tienda;
import influenceinc.modelo.Herramienta;
import influenceinc.modelo.RolJugador;
import influenceinc.modelo.Veredicto;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * PRUEBA DE LOS ROLES (consola): herramientas por rol, habilidades unicas,
 * impacto del influencer, rueda de prensa del candidato y alerta viral.
 */
public class PruebaRoles {

    private static int fallos = 0;

    public static void main(String[] args) {
        System.out.println("=== HERRAMIENTAS POR ROL ===");
        comprobar(RolJugador.PERIODISTA.puedeUsar(Herramienta.CORRESPONSAL), "el periodista tiene el corresponsal");
        comprobar(!RolJugador.CIUDADANO.puedeUsar(Herramienta.CORRESPONSAL), "el ciudadano no tiene el corresponsal");
        comprobar(RolJugador.INFLUENCER.puedeUsar(Herramienta.RED) && !RolJugador.INFLUENCER.puedeUsar(Herramienta.LUPA),
                "el influencer tiene escáner de red pero no lupa");
        comprobar(RolJugador.CANDIDATO.puedeUsar(Herramienta.DIRECTORIO), "el candidato tiene el directorio");
        comprobar(!RolJugador.PERIODISTA.tieneMiniJuego() && RolJugador.CIUDADANO.tieneMiniJuego(),
                "periodista = monitor completo; los demás tienen mini-juego");

        System.out.println("\n=== PARTIDA CON 4 ROLES ===");
        Partida p = new Partida(Partida.Modo.DUELO, Arrays.asList("Ana", "Luis", "María", "Pedro"),
                Arrays.asList(RolJugador.CIUDADANO, RolJugador.PERIODISTA, RolJugador.INFLUENCER, RolJugador.CANDIDATO), 7, false);
        comprobar(p.jugadores.get(2).rol == RolJugador.INFLUENCER, "cada jugador guarda su rol");
        Jornada j = p.iniciarJornada();          // Ana, ciudadana
        Caso c = j.siguiente();
        comprobar(j.usar(Herramienta.CORRESPONSAL).get(0).contains("no usa"), "el ciudadano no puede usar el corresponsal");
        comprobar(j.puedeUsarHabilidad(), "el ciudadano puede usar su habilidad");
        Habilidades.Cadena cad = j.prepararCadena();
        comprobar(cad.camino.size() >= 2 && cad.pasos.size() == cad.camino.size() + 1,
                "la cadena sale de un camino del grafo social (" + cad.camino.size() + " personas)");
        boolean unaCorrecta = true;
        for (List<Habilidades.Eslabon> op : cad.pasos) {
            int k = 0;
            for (Habilidades.Eslabon e : op) {
                k += e.correcto ? 1 : 0;
            }
            unaCorrecta &= k == 1;
        }
        comprobar(unaCorrecta, "cada paso de la cadena tiene exactamente una opción correcta");
        int antes = c.dialogo.cantidadNodos();
        j.terminarHabilidad(true, 6);
        comprobar(!j.puedeUsarHabilidad() && !c.pistasHabilidad.isEmpty(), "la habilidad se usa una vez y anota pistas");
        comprobar(c.dialogo.cantidadNodos() >= antes, "la habilidad puede INSERTAR una confrontación en el árbol");

        System.out.println("\n=== INFLUENCER: impacto x1,5 ===");
        Partida a = new Partida(Partida.Modo.SOLO, Arrays.asList("P"), Arrays.asList(RolJugador.PERIODISTA), 3, false);
        Partida b = new Partida(Partida.Modo.SOLO, Arrays.asList("I"), Arrays.asList(RolJugador.INFLUENCER), 3, false);
        Jornada ja = a.iniciarJornada(), jb = b.iniciarJornada();
        Caso ca = ja.siguiente(), cb = jb.siguiente();
        Jornada.Resultado ra = ja.sellar(ca.correcto), rb = jb.sellar(cb.correcto);
        comprobar(rb.puntos == Math.round(ra.puntos * 1.5), "el influencer gana x1,5 (" + ra.puntos + " vs " + rb.puntos + ")");
        List<Habilidades.Perfil> perf = jb.prepararPerfiles();
        comprobar(!perf.isEmpty(), "el cazabots tiene perfiles del BFS (" + perf.size() + ")");

        System.out.println("\n=== CANDIDATO: rueda de prensa ===");
        Partida pc = new Partida(Partida.Modo.SOLO, Arrays.asList("C"), Arrays.asList(RolJugador.CANDIDATO), 5, false);
        Jornada jc = pc.iniciarJornada();
        jc.siguiente();
        JugadorVerificador cand = pc.jugadores.get(0);
        int conf = cand.ciudad.confianza, voto = cand.apoyoElectoral();
        List<Habilidades.Pregunta> rueda = jc.prepararRueda();
        comprobar(rueda.size() == 3, "3 preguntas en la rueda de prensa");
        boolean todasR = true;
        for (Habilidades.Pregunta q : rueda) {
            int r = 0;
            for (Habilidades.Respuesta x : q.respuestas) {
                r += x.tipo == 'R' ? 1 : 0;
            }
            todasR &= r == 1;
            jc.respuestaPrensa('R');
        }
        comprobar(todasR, "cada pregunta tiene una sola respuesta responsable");
        comprobar(cand.ciudad.confianza > conf && cand.apoyoElectoral() > voto, "responder bien sube confianza e intención de voto");
        int confl = cand.ciudad.conflictos;
        jc.respuestaPrensa('A');
        comprobar(cand.ciudad.conflictos > confl, "atacar sube los conflictos");

        System.out.println("\n=== ALERTA VIRAL (10 segundos) ===");
        Partida pv = new Partida(Partida.Modo.SOLO, Arrays.asList("V"), 11, false);
        Jornada jv = pv.iniciarJornada();
        Habilidades.Viral falsa = null;
        Random rr = new Random(1);
        while (falsa == null || falsa.verdadera) {
            falsa = Habilidades.viral(rr);
        }
        String[] m = new String[1];
        int[] r1 = jv.resolverViral(falsa, 1, m);
        comprobar(r1[1] == 1 && r1[0] > 0, "reportar un bulo viral es acierto (+" + r1[0] + ")");
        int[] r2 = jv.resolverViral(falsa, 3, m);
        comprobar(r2[1] == 0 && r2[0] < 0, "compartir un bulo viral resta (" + r2[0] + ")");

        System.out.println("\n=== HERRAMIENTAS QUE NO APLICAN Y SE REINICIAN ===");
        Partida ph = new Partida(Partida.Modo.SOLO, Arrays.asList("H"), 13, false);
        for (int d = 1; d < 3; d++) {
            Jornada jd = ph.iniciarJornada();
            Caso cd;
            while ((cd = jd.siguiente()) != null) {
                jd.sellar(cd.correcto);
            }
            jd.cerrar();
            ph.siguienteTurno();
        }
        Jornada jh = ph.iniciarJornada();          // dia 3
        Caso sinCred = jh.siguiente();
        comprobar(sinCred.visitante.credencial == null && jh.motivoNoAplica(Herramienta.LUPA) != null,
                "sin credencial la lupa NO aplica: " + jh.motivoNoAplica(Herramienta.LUPA));
        jh.sellar(sinCred.correcto);
        Caso conCred = jh.siguiente();
        comprobar(conCred.visitante.credencial != null && jh.motivoNoAplica(Herramienta.LUPA) == null,
                "con el cliente siguiente la lupa vuelve a estar disponible");
        jh.usar(Herramienta.LUPA);
        jh.sellar(conCred.correcto);
        Caso otro = jh.siguiente();
        comprobar(jh.usadasConEsteCliente() == 0 && jh.resultados(Herramienta.LUPA) == null
                && jh.motivoNoAplica(Herramienta.LUPA) == null, "cliente nuevo: herramientas reiniciadas");

        System.out.println("\n=== TIENDA ===");
        Partida pt = new Partida(Partida.Modo.SOLO, Arrays.asList("T"), 17, false);
        Jornada jt = pt.iniciarJornada();
        JugadorVerificador comp = pt.jugadores.get(0);
        comp.dinero = 50;
        comprobar(Tienda.comprar(Tienda.Articulo.CAMPANA, jt).startsWith("[x]"), "sin dinero suficiente no se puede comprar");
        comp.dinero = 1000;
        int des = comp.ciudad.desinformacion();
        Tienda.comprar(Tienda.Articulo.CAMPANA, jt);
        comprobar(comp.dinero == 750 && comp.ciudad.desinformacion() < des, "la campaña cuesta $250 y baja la desinformación");
        Tienda.comprar(Tienda.Articulo.SEGURO, jt);
        Caso malo = jt.siguiente();
        Jornada.Resultado rm = jt.sellar(Veredicto.PUBLICAR);
        comprobar(rm.seguroUsado && comp.advertencias == 0, "el seguro de reputación anula la advertencia");
        double antesMin = jt.getMinuto();
        Tienda.comprar(Tienda.Articulo.HORAS_EXTRA, jt);
        comprobar(jt.getMinuto() <= antesMin, "las horas extra devuelven minutos al reloj");

        System.out.println(fallos == 0 ? "\nTODAS LAS PRUEBAS PASARON" : "\nFALLOS: " + fallos);
        System.exit(fallos == 0 ? 0 : 1);
    }

    private static void comprobar(boolean ok, String que) {
        System.out.println((ok ? "  [OK]    " : "  [FALLA] ") + que);
        if (!ok) {
            fallos++;
        }
    }
}
