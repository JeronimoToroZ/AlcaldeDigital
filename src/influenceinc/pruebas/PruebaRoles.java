package influenceinc.pruebas;

import influenceinc.logica.Caso;
import influenceinc.logica.Habilidades;
import influenceinc.logica.Historia;
import influenceinc.logica.Jornada;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Noticiero;
import influenceinc.logica.Partida;
import influenceinc.logica.PistaSellos;
import influenceinc.logica.Telefono;
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
                "periodista = todas las apps; los demás tienen mini-juego");

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

        System.out.println("\n=== PISTA DE SELLOS (estilo Pump It Up) ===");
        comprobar(PistaSellos.VELOCIDAD == 1.1, "los círculos caen a velocidad 1.1");
        comprobar(PistaSellos.CARRILES == 4 && PistaSellos.TECLAS.equals("ZXCV"), "4 carriles: Z X C V");
        Partida ps = new Partida(Partida.Modo.SOLO, Arrays.asList("Ana"), Arrays.asList(RolJugador.PERIODISTA), 21, false);
        Jornada jp = ps.iniciarJornada();
        jp.siguiente();
        // 1. se juega "perfecto": el primer círculo que llega decide; luego solo se pisa ese sello
        PistaSellos buena = jp.prepararPista(360);
        double previa = -1;
        boolean enOrden = true;
        boolean variosSellos = false;
        for (int paso = 0; paso < 6000 && !buena.terminado(); paso++) {
            buena.actualizar(1 / 120.0);
            for (PistaSellos.Nota n : buena.getEnPista()) {
                boolean mio = buena.getElegido() == null || n.sello == buena.getElegido();
                if (n.estado == 0 && mio && Math.abs(buena.getT() - n.llegada) < 0.01) {
                    buena.pisar(n.carril);
                }
            }
        }
        for (PistaSellos.Nota n : buena.getEnPista()) {
            enOrden &= n.llegada >= previa;
            previa = n.llegada;
            variosSellos |= n.sello != buena.getEnPista().get(0).sello;
        }
        comprobar(enOrden, "las notas salen de la cola en orden de tiempo (heap sort)");
        comprobar(variosSellos, "caen todos los sellos del día mezclados (no se escoge antes)");
        comprobar(buena.exito() && buena.getAciertos() == PistaSellos.ACIERTOS && buena.getElegido() != null,
                "el primer acierto decide el sello y con 4 iguales se activa (" + buena.getElegido().sello + ")");
        // 2. no pisar nada: no castiga, la pista se cierra al pasar 10
        PistaSellos quieta = jp.prepararPista(360);
        int adv0 = jp.jugador.advertencias;
        for (int paso = 0; paso < 12000 && !quieta.terminado(); paso++) {
            quieta.actualizar(1 / 120.0);
        }
        comprobar(quieta.agotada() && quieta.getPasadas() == PistaSellos.MARGEN && jp.jugador.advertencias == adv0,
                "dejar pasar 10 círculos cierra la pista sin perder vidas");
        // 3. pisar un círculo de otro sello: es la única forma de perder una vida
        PistaSellos mala = jp.prepararPista(360);
        PistaSellos.Juicio errado = null;
        for (int paso = 0; paso < 12000 && errado == null && !mala.terminado(); paso++) {
            mala.actualizar(1 / 120.0);
            for (PistaSellos.Nota n : mala.getEnPista()) {
                if (n.estado == 0 && Math.abs(mala.getT() - n.llegada) < 0.01 && errado == null) {
                    if (mala.getElegido() == null || n.sello == mala.getElegido()) {
                        if (mala.getElegido() == null) {
                            mala.pisar(n.carril);
                        }
                    } else {
                        errado = mala.pisar(n.carril);
                    }
                }
            }
        }
        comprobar(errado == PistaSellos.Juicio.EQUIVOCADO && mala.getEquivocadas() == 1, "pisar otro sello es un error");
        int adv = jp.jugador.advertencias;
        comprobar(jp.fallarSello() && jp.jugador.advertencias == adv + 1, "ese error suma una advertencia al instante (pierde una vida)");
        jp.jugador.seguros = 1;
        comprobar(!jp.fallarSello() && jp.jugador.advertencias == adv + 1, "el seguro de la tienda absorbe esa advertencia");
        PistaSellos vacia = jp.prepararPista(360);
        comprobar(vacia.pisar(2) == PistaSellos.Juicio.FUERA && vacia.getEquivocadas() == 0, "pisar un carril vacío no castiga");

        System.out.println("\n=== CELULAR Y NOTICIERO SEGÚN EL ROL ===");
        for (RolJugador r : RolJugador.values()) {
            JugadorVerificador jr = new JugadorVerificador("X", r, 4);
            List<Historia.MensajeProgramado> ms = Historia.mensajesDelDia(2, r);
            comprobar(ms.size() >= 2 && ms.get(0).de.equals(Telefono.contactoDelRol(r)),
                    r.nombre + ": le escribe " + Telefono.contactoDelRol(r));
            List<Noticiero.Segmento> man = Noticiero.manana(3, jr);
            comprobar(man.size() == 4 && man.get(2).tipo.equals("rol"), r.nombre + ": el noticiero tiene un bloque de su rol");
        }
        JugadorVerificador jcel = new JugadorVerificador("X", RolJugador.CIUDADANO, 4);
        jcel.telefono.recibir("Zoe", "hola", "09:10", false);
        jcel.telefono.recibir("Ana", "otra", "09:12", false);
        comprobar(jcel.telefono.noLeidos() == 2 && jcel.telefono.conversaciones().get(0).contacto.equals("Ana"),
                "mensajes: el chat más reciente queda arriba");
        comprobar(jcel.telefono.siguienteNotificacion().texto.equals("hola"), "notificaciones en cola FIFO");
        List<Telefono.Contacto> agenda = jcel.telefono.contactos();
        boolean alfabetica = true;
        for (int i = 1; i < agenda.size(); i++) {
            alfabetica &= agenda.get(i - 1).nombre.compareTo(agenda.get(i).nombre) < 0;
        }
        comprobar(alfabetica && agenda.size() > 10, "la agenda (ABB) sale en orden alfabético: " + agenda.size() + " contactos");
        Partida pn = new Partida(Partida.Modo.SOLO, Arrays.asList("Ana"), Arrays.asList(RolJugador.CANDIDATO), 5, false);
        Jornada jn = pn.iniciarJornada();
        Caso cn;
        while ((cn = jn.siguiente()) != null) {
            jn.sellar(cn.correcto);
        }
        jn.cerrar();
        List<Noticiero.Segmento> noche = Noticiero.noche(jn);
        comprobar(noche.size() >= 5 && noche.get(1).tipo.equals("titular"), "noticiero de la noche con los titulares del día");
        comprobar(Noticiero.impacto(jn.getResultados().get(0)) >= 0, "los titulares se ordenan por impacto (inserción)");
        List<Integer> claves = jn.jugador.archivo.inordenIterativo();
        boolean crece = true;
        for (int i = 1; i < claves.size(); i++) {
            crece &= claves.get(i - 1) < claves.get(i);
        }
        comprobar(claves.size() == 3 && crece, "ABB del archivo: las publicaciones selladas quedan clasificadas por credibilidad");

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
