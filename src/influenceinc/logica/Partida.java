package influenceinc.logica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * La partida completa: 5 dias, uno o varios verificadores.
 *
 *  - SOLO: un jugador.
 *  - DUELO (mismo computador): 2 a 4 jugadores. Cada dia lo juegan todos por
 *    turnos, con los MISMOS casos (misma semilla) y cada uno con su ciudad.
 *  - RED: cada jugador juega en su computador con la misma semilla; el
 *    servidor reparte el marcador en vivo.
 */
public class Partida {

    public enum Modo { SOLO, DUELO, RED }

    public final Modo modo;
    public final long semilla;
    public final boolean relajado;
    public final List<JugadorVerificador> jugadores = new ArrayList<>();
    private int dia = 1;
    private int turno = 0;
    private Jornada jornada;

    public Partida(Modo modo, List<String> nombres, long semilla, boolean relajado) {
        this.modo = modo;
        this.semilla = semilla;
        this.relajado = relajado;
        for (String n : nombres) {
            jugadores.add(new JugadorVerificador(n, semilla));
        }
        if (jugadores.isEmpty()) {
            jugadores.add(new JugadorVerificador("Verificador", semilla));
        }
    }

    /** Empieza el dia del jugador de turno. */
    public Jornada iniciarJornada() {
        jornada = new Jornada(dia, jugadorDeTurno(), semilla, relajado);
        return jornada;
    }

    public Jornada getJornada() {
        return jornada;
    }

    public JugadorVerificador jugadorDeTurno() {
        return jugadores.get(turno);
    }

    public int getDia() {
        return dia;
    }

    public int getTurno() {
        return turno;
    }

    /**
     * Pasa al siguiente turno (otro jugador el mismo dia, o el dia siguiente).
     * Los despedidos ya no juegan.
     * @return false si la partida termino
     */
    public boolean siguienteTurno() {
        for (int intentos = 0; intentos < jugadores.size() * (Historia.DIAS + 1); intentos++) {
            turno++;
            if (turno >= jugadores.size()) {
                turno = 0;
                dia++;
            }
            if (dia > Historia.DIAS) {
                return false;
            }
            if (!jugadores.get(turno).despedido) {
                return true;
            }
        }
        return false;
    }

    /** true si es el primer jugador del dia (para mostrar la llamada del jefe una vez por jugador). */
    public boolean esUltimoTurnoDelDia() {
        for (int t = turno + 1; t < jugadores.size(); t++) {
            if (!jugadores.get(t).despedido) {
                return false;
            }
        }
        return true;
    }

    public boolean terminada() {
        if (dia > Historia.DIAS) {
            return true;
        }
        for (JugadorVerificador j : jugadores) {
            if (!j.despedido) {
                return false;
            }
        }
        return true;
    }

    public List<JugadorVerificador> ranking() {
        List<JugadorVerificador> l = new ArrayList<>(jugadores);
        Collections.sort(l, new Comparator<JugadorVerificador>() {
            @Override
            public int compare(JugadorVerificador a, JugadorVerificador b) {
                return Integer.compare(b.puntajeFinal(), a.puntajeFinal());
            }
        });
        return l;
    }
}
