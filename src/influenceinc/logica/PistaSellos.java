package influenceinc.logica;

import influenceinc.estructuras.Ordenamientos;
import influenceinc.modelo.Veredicto;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * LA PISTA DE LOS SELLOS (estilo Pump It Up). Solo la logica: el dibujo
 * esta en gui/JuegoSellos.
 *
 * Empieza enseguida, sin escoger el sello antes. Por 4 carriles (Z X C V)
 * caen circulos con los sellos del dia, mezclados.
 *  - El PRIMER icono que el jugador acierta decide su sello.
 *  - Con 4 aciertos de ese mismo sello, el sello se activa.
 *  - Pisar un icono de OTRO sello es el unico error: cuesta una vida
 *    (advertencia) en ese mismo instante.
 *  - Dejar pasar iconos no castiga: hay un margen de 10 para pensar.
 *    Si se pasan 10 sin decidir, la pista se cierra sin castigo y se
 *    puede volver a abrir.
 *
 * Estructuras:
 *  - las notas se generan y despues se ordenan por tiempo con HEAP SORT;
 *  - las que todavia no salen esperan en una COLA (FIFO): salen en orden.
 */
public class PistaSellos {

    /** Velocidad de caida pedida para el minijuego. */
    public static final double VELOCIDAD = 1.0;
    /** Pixeles del lienzo por segundo cuando la velocidad es 1. */
    public static final double PIXELES_POR_SEGUNDO = 200;
    public static final int CARRILES = 4;
    public static final String TECLAS = "ZXCV";
    public static final int ACIERTOS = 4;
    /** Iconos que pueden pasar sin pisarse antes de que la pista se cierre (sin castigo). */
    public static final int MARGEN = 10;
    /** Ventanas de tiempo (segundos) alrededor de la linea. */
    public static final double PERFECTO = 0.08, BIEN = 0.17;
    private static final int NOTAS = 50;

    public enum Juicio { PERFECTO, BIEN, EQUIVOCADO, FUERA }

    /** Un icono que cae. */
    public static class Nota {
        /** Segundo en que llega a la linea. */
        public final double llegada;
        public final int carril;
        public final Veredicto sello;
        /** 0 cayendo, 1 acertada, 2 equivocada, 3 paso sin pisarse. */
        public int estado;
        /** Segundo en que se resolvio (para la animacion). */
        public double cuando;

        Nota(double llegada, int carril, Veredicto sello) {
            this.llegada = llegada;
            this.carril = carril;
            this.sello = sello;
        }
    }

    private final Queue<Nota> porSalir = new ArrayDeque<>();
    private final List<Nota> enPista = new ArrayList<>();
    private final double tiempoCaida;
    private Veredicto elegido;
    private double t;
    private int aciertos, equivocadas, pasadas;
    private Juicio ultimoJuicio;
    private int ultimoCarril = -1;
    private double ultimoJuicioT = -10;

    /**
     * @param disponibles los sellos que existen hoy (todos caen mezclados)
     * @param altoPista   distancia en pixeles desde donde aparece el icono hasta la linea
     */
    public PistaSellos(List<Veredicto> disponibles, double altoPista, Random azar) {
        this.tiempoCaida = altoPista / (VELOCIDAD * PIXELES_POR_SEGUNDO);
        // 1. "bolsa": en cada ronda salen TODOS los sellos una vez, en orden barajado;
        //    asi ningun sello se queda mucho tiempo sin caer
        List<Nota> notas = new ArrayList<>();
        List<Veredicto> bolsa = new ArrayList<>();
        double tc = tiempoCaida + 0.8;
        for (int i = 0; i < NOTAS; i++) {
            if (bolsa.isEmpty()) {
                bolsa.addAll(disponibles);
            }
            Veredicto s = bolsa.remove(azar.nextInt(bolsa.size()));
            notas.add(new Nota(tc + azar.nextDouble() * 0.12, azar.nextInt(CARRILES), s));
            tc += 0.5 + azar.nextDouble() * 0.3;
        }
        // 2. ordenadas por la hora en que llegan a la linea: HEAP SORT
        Ordenamientos.heap(notas, new Comparator<Nota>() {
            @Override
            public int compare(Nota a, Nota b) {
                return Double.compare(a.llegada, b.llegada);
            }
        });
        // 3. todas a la cola
        for (Nota n : notas) {
            porSalir.add(n);
        }
    }

    /**
     * Avanza el tiempo: saca de la cola las notas que ya deben verse y marca
     * las que pasaron la linea sin pisarse.
     * @return las que acaban de pasar sin pisarse
     */
    public List<Nota> actualizar(double dt) {
        t += dt;
        while (!porSalir.isEmpty() && porSalir.peek().llegada - t <= tiempoCaida) {
            enPista.add(porSalir.poll());
        }
        List<Nota> pasaron = new ArrayList<>();
        if (terminado()) {
            return pasaron;
        }
        for (Nota n : enPista) {
            if (n.estado == 0 && t - n.llegada > BIEN) {
                n.estado = 3;
                n.cuando = t;
                // cuentan las que el jugador podria haber pisado: todas antes de decidir, y despues solo las de su sello
                if (elegido == null || n.sello == elegido) {
                    pasadas++;
                    pasaron.add(n);
                }
            }
        }
        return pasaron;
    }

    /** El jugador pisa un carril (0..3). */
    public Juicio pisar(int carril) {
        if (terminado() || carril < 0 || carril >= CARRILES) {
            return Juicio.FUERA;
        }
        Nota mejor = null;
        for (Nota n : enPista) {
            if (n.estado == 0 && n.carril == carril && Math.abs(t - n.llegada) <= BIEN
                    && (mejor == null || Math.abs(t - n.llegada) < Math.abs(t - mejor.llegada))) {
                mejor = n;
            }
        }
        if (mejor == null) {
            marcar(Juicio.FUERA, carril);      // carril vacio: no pasa nada
            return Juicio.FUERA;
        }
        mejor.cuando = t;
        Juicio j;
        if (elegido == null || mejor.sello == elegido) {
            elegido = mejor.sello;             // el primer acierto decide el sello
            mejor.estado = 1;
            aciertos++;
            j = Math.abs(t - mejor.llegada) <= PERFECTO ? Juicio.PERFECTO : Juicio.BIEN;
        } else {
            mejor.estado = 2;
            equivocadas++;
            j = Juicio.EQUIVOCADO;
        }
        marcar(j, carril);
        return j;
    }

    private void marcar(Juicio j, int carril) {
        ultimoJuicio = j;
        ultimoCarril = carril;
        ultimoJuicioT = t;
    }

    public boolean exito() {
        return aciertos >= ACIERTOS;
    }

    /** Se acabo el margen (o los iconos) sin completar el sello. No es castigo. */
    public boolean agotada() {
        return !exito() && (pasadas >= MARGEN || (porSalir.isEmpty() && cayendo() == 0));
    }

    public boolean terminado() {
        return exito() || agotada();
    }

    private int cayendo() {
        int c = 0;
        for (Nota n : enPista) {
            c += n.estado == 0 ? 1 : 0;
        }
        return c;
    }

    /** Altura (en pixeles sobre la linea) a la que va una nota ahora. */
    public double alturaSobreLinea(Nota n) {
        return (n.llegada - t) * VELOCIDAD * PIXELES_POR_SEGUNDO;
    }

    public Veredicto getElegido() {
        return elegido;
    }

    public List<Nota> getEnPista() {
        return enPista;
    }

    public double getT() {
        return t;
    }

    public int getAciertos() {
        return aciertos;
    }

    public int getEquivocadas() {
        return equivocadas;
    }

    public int getPasadas() {
        return pasadas;
    }

    public Juicio getUltimoJuicio() {
        return ultimoJuicio;
    }

    public int getUltimoCarril() {
        return ultimoCarril;
    }

    public double getUltimoJuicioT() {
        return ultimoJuicioT;
    }
}
