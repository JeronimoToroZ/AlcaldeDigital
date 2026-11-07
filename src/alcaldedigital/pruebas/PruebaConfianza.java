package alcaldedigital.pruebas;

import alcaldedigital.logica.Partida;
import alcaldedigital.modelo.Accion;
import alcaldedigital.modelo.Jugador;
import alcaldedigital.modelo.Rol;

import java.util.ArrayList;
import java.util.List;

/**
 * Banco de pruebas de las dos reglas nuevas del paso 3:
 *   - verificar cuesta y se acaba
 *   - la confianza sube lento y baja rapido
 *
 * Para ejecutarlo en NetBeans: clic derecho sobre este archivo -> Run File.
 */
public class PruebaConfianza {

    private static int fallos = 0;

    public static void main(String[] args) {

        imprimirTitulo("CASO 1 - El presupuesto de verificaciones se acaba");
        Partida p1 = nuevaPartida();
        System.out.println("Al empezar: " + p1.getVerificacionesRestantes() + " verificaciones");
        comprobar("Arranca con " + Partida.VERIFICACIONES_POR_PARTIDA,
                p1.getVerificacionesRestantes() == Partida.VERIFICACIONES_POR_PARTIDA);
        comprobar("Al empezar SI puede verificar", p1.puedeVerificar());

        for (int i = 1; i <= Partida.VERIFICACIONES_POR_PARTIDA; i++) {
            p1.ejecutar(Accion.VERIFICAR);
            System.out.println("  Despues de verificar " + i + " vez/veces: quedan "
                    + p1.getVerificacionesRestantes());
        }
        comprobar("Se agotaron las verificaciones", p1.getVerificacionesRestantes() == 0);
        comprobar("Ya NO puede verificar", !p1.puedeVerificar());

        p1.ejecutar(Accion.VERIFICAR);
        comprobar("No baja de cero aunque insista", p1.getVerificacionesRestantes() == 0);

        imprimirTitulo("CASO 2 - La confianza baja mas rapido de lo que sube");
        System.out.println("Subir una vez acertando: +" + Partida.CONFIANZA_SUBE);
        System.out.println("Bajar una vez difundiendo una mentira: -" + Partida.CONFIANZA_BAJA);
        comprobar("Baja mas de lo que sube", Partida.CONFIANZA_BAJA > Partida.CONFIANZA_SUBE);
        System.out.println("Hacen falta " + (int) Math.ceil(
                (double) Partida.CONFIANZA_BAJA / Partida.CONFIANZA_SUBE)
                + " aciertos para reparar UN error.");

        imprimirTitulo("CASO 3 - Diez turnos bien, uno mal");
        Partida p3 = nuevaPartida();
        int inicial = p3.getConfianza();
        System.out.println("Confianza inicial: " + inicial);

        int turnosBuenos = 0;
        while (p3.hayTurnoPendiente() && turnosBuenos < 6) {
            if (!p3.publicacionActual().esFalsa()) {
                p3.ejecutar(Accion.COMPARTIR);     // compartir algo cierto: bien
            } else {
                p3.ejecutar(Accion.REPORTAR);      // reportar algo falso: bien
            }
            turnosBuenos++;
        }
        int tras = p3.getConfianza();
        System.out.println("Tras " + turnosBuenos + " turnos acertados: " + tras);
        comprobar("La confianza subio", tras > inicial);

        boolean encontroFalsa = false;
        while (p3.hayTurnoPendiente() && !encontroFalsa) {
            if (p3.publicacionActual().esFalsa()) {
                int antes = p3.getConfianza();
                p3.ejecutar(Accion.COMPARTIR);     // difundir una mentira: el error grave
                System.out.println("Un solo error (compartir algo falso): "
                        + antes + " -> " + p3.getConfianza());
                comprobar("Un error cuesta " + Partida.CONFIANZA_BAJA,
                        antes - p3.getConfianza() == Partida.CONFIANZA_BAJA);
                encontroFalsa = true;
            } else {
                p3.ejecutar(Accion.IGNORAR);
            }
        }
        comprobar("Se pudo probar el error grave", encontroFalsa);

        imprimirTitulo("CASO 4 - Reportar algo CIERTO tambien hace dano");
        Partida p4 = nuevaPartida();
        boolean probado = false;
        while (p4.hayTurnoPendiente() && !probado) {
            if (!p4.publicacionActual().esFalsa()) {
                int antes = p4.getConfianza();
                p4.ejecutar(Accion.REPORTAR);
                System.out.println("Reportar una publicacion cierta: " + antes + " -> " + p4.getConfianza());
                comprobar("Baja, pero menos que difundir una mentira",
                        p4.getConfianza() < antes
                        && (antes - p4.getConfianza()) < Partida.CONFIANZA_BAJA);
                probado = true;
            } else {
                p4.ejecutar(Accion.IGNORAR);
            }
        }
        comprobar("Se pudo probar el caso", probado);

        imprimirTitulo("CASO 5 - Queda registro de todo para el informe final");
        Partida p5 = nuevaPartida();
        int turnos = 0;
        while (p5.hayTurnoPendiente()) {
            p5.ejecutar(turnos % 2 == 0 ? Accion.COMPARTIR : Accion.IGNORAR);
            turnos++;
        }
        System.out.println("Turnos jugados   : " + turnos);
        System.out.println("Decisiones guardadas: " + p5.getDecisiones().size());
        System.out.println("Aciertos         : " + p5.totalAciertos());
        System.out.println("Falsas difundidas: " + p5.falsasCompartidas());
        System.out.println("Confianza final  : " + p5.getConfianza());
        comprobar("Hay una decision guardada por turno", p5.getDecisiones().size() == turnos);
        comprobar("La confianza nunca salio del rango 0-100",
                p5.getConfianza() >= 0 && p5.getConfianza() <= 100);

        imprimirTitulo("CASO 6 - Compartir todo a ciegas hunde la confianza");
        Partida p6 = nuevaPartida();
        while (p6.hayTurnoPendiente()) {
            p6.ejecutar(Accion.COMPARTIR);
        }
        System.out.println("Confianza tras compartir TODO sin mirar: " + p6.getConfianza());
        System.out.println("Falsas difundidas: " + p6.falsasCompartidas());
        comprobar("Compartir sin verificar deja la confianza por debajo de la inicial",
                p6.getConfianza() < Partida.CONFIANZA_INICIAL);

        imprimirTitulo("RESULTADO");
        if (fallos == 0) {
            System.out.println("Las comprobaciones pasaron. El balance del paso 3 funciona.");
        } else {
            System.out.println("FALLARON " + fallos + " comprobaciones. Revisar arriba.");
        }
    }

    private static Partida nuevaPartida() {
        List<Jugador> js = new ArrayList<>();
        js.add(new Jugador("Jugador 1", Rol.CIUDADANO));
        js.add(new Jugador("Jugador 2", Rol.CIUDADANO));
        return new Partida(js, 4);
    }

    private static void comprobar(String descripcion, boolean condicion) {
        System.out.println((condicion ? "  OK      " : "  FALLO   ") + descripcion);
        if (!condicion) {
            fallos++;
        }
    }

    private static void imprimirTitulo(String titulo) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println(" " + titulo);
        System.out.println("============================================================");
    }
}
