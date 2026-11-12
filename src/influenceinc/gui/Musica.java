package influenceinc.gui;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

/**
 * Musica de fondo, compuesta en codigo con el sintetizador MIDI de Java.
 *
 * Sigue la misma idea que Sonido: el juego no carga archivos de audio. Aqui
 * eso importa mas todavia, porque un bucle de dos minutos en WAV son unos
 * 15 MB y estas dos piezas juntas ocupan unos pocos cientos de bytes en
 * forma de notas. Ademas el bucle MIDI es exacto: no deja el saltico que
 * queda al reiniciar un archivo comprimido.
 *
 * Las dos piezas:
 *
 *   MENU        Lenta y calida, con algo de melancolia. No es heroica a
 *               proposito: el jugador no va a salvar el mundo, va a sellar
 *               papeles en una oficina durante una semana electoral.
 *
 *   VENTANILLA  Sin melodia. Suena veinte minutos seguidos, y lo que se
 *               pega al tercer bucle se odia al decimo. Es pulso, bajo y
 *               una nota larga de fondo: acompana el ritmo de atender gente
 *               sin pedir atencion.
 *
 * Las dos estan en LA MENOR y comparten tempo relativo, asi que pasar del
 * menu a la oficina no suena a corte.
 *
 * Todo se mantiene en graves y medios. Los efectos de Sonido son ondas
 * senoidales entre 90 y 3100 Hz, con mucho contenido agudo: si la musica
 * ocupara ese rango se los comeria y el jugador dejaria de oir el sello,
 * el telefono y la alerta, que son los que llevan informacion.
 */
public final class Musica {

    /** Las dos piezas del juego. */
    public enum Pista {
        MENU,
        VENTANILLA
    }

    // ------------------------------------------------------------------
    // Notas (numero MIDI). 60 es el do central.
    // ------------------------------------------------------------------
    private static final int LA2 = 45, DO3 = 48, RE3 = 50, MI3 = 52, FA3 = 53, SOL3 = 55;
    private static final int LA3 = 57, DO4 = 60, RE4 = 62, MI4 = 64, FA4 = 65, SOL4 = 67;
    private static final int LA4 = 69, DO5 = 72, RE5 = 74, MI5 = 76;
    /** Si natural: solo aparece en el acorde de Sol mayor. */
    private static final int SI = 59;

    /** Pulsos por negra. Toda la duracion se cuenta en estas unidades. */
    private static final int PPN = 96;

    /** Volumen general, de 0 a 127. Bajo a proposito: es fondo. */
    private static final int VOLUMEN_MENU = 58;
    private static final int VOLUMEN_OFICINA = 38;

    private static Sequencer secuenciador;
    private static Pista sonando;
    private static boolean disponible = true;

    private Musica() {
    }

    // ------------------------------------------------------------------
    // Control
    // ------------------------------------------------------------------

    /**
     * Pone una pista en bucle. Si ya estaba sonando esa misma, no hace nada:
     * reiniciarla en cada cambio de pantalla se oiria como un tropiezo.
     */
    public static void reproducir(Pista pista) {
        if (!disponible) {
            return;
        }
        if (!Tema.sonido) {
            detener();
            return;
        }
        if (pista == sonando && secuenciador != null && secuenciador.isRunning()) {
            return;
        }
        try {
            abrir();
            secuenciador.stop();
            secuenciador.setSequence(pista == Pista.MENU ? menu() : ventanilla());
            secuenciador.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            secuenciador.setTickPosition(0);
            volumen(pista == Pista.MENU ? VOLUMEN_MENU : VOLUMEN_OFICINA);
            secuenciador.start();
            sonando = pista;
        } catch (Exception | LinkageError e) {
            // Sin tarjeta de sonido o sin sintetizador: el juego sigue igual.
            // Es lo mismo que hace Sonido, y por la misma razon: la musica
            // nunca puede ser motivo de que el juego no arranque.
            disponible = false;
            sonando = null;
        }
    }

    /** Corta la musica. */
    public static void detener() {
        sonando = null;
        if (secuenciador != null && secuenciador.isRunning()) {
            secuenciador.stop();
        }
    }

    /** Vuelve a aplicar el interruptor de sonido de los ajustes. */
    public static void refrescar() {
        if (!Tema.sonido) {
            detener();
        } else if (sonando == null) {
            reproducir(Pista.MENU);
        }
    }

    /** Suelta los recursos al cerrar el juego. */
    public static void cerrar() {
        detener();
        if (secuenciador != null && secuenciador.isOpen()) {
            secuenciador.close();
        }
        secuenciador = null;
    }

    private static void abrir() throws Exception {
        if (secuenciador == null || !secuenciador.isOpen()) {
            secuenciador = MidiSystem.getSequencer();
            secuenciador.open();
            // Al terminar una vuelta no hay que hacer nada: el bucle lo lleva
            // el propio secuenciador. El oyente queda por si alguna
            // implementacion no soporta LOOP_CONTINUOUSLY y hay que reiniciar.
            secuenciador.addMetaEventListener(new MetaEventListener() {
                @Override
                public void meta(javax.sound.midi.MetaMessage m) {
                    if (m.getType() == 47 && sonando != null && Tema.sonido) {
                        secuenciador.setTickPosition(0);
                        secuenciador.start();
                    }
                }
            });
        }
    }

    /** Baja el volumen de todos los canales del sintetizador. */
    private static void volumen(int v) {
        try {
            Synthesizer s = MidiSystem.getSynthesizer();
            if (!s.isOpen()) {
                s.open();
            }
            for (MidiChannel c : s.getChannels()) {
                if (c != null) {
                    c.controlChange(7, v);   // 7 = volumen de canal
                }
            }
        } catch (Exception | LinkageError ignorada) {
            // Si no se puede ajustar, suena al volumen por defecto.
        }
    }

    // ------------------------------------------------------------------
    // Las piezas
    // ------------------------------------------------------------------

    /**
     * MENU. Ocho compases en la menor, 66 negras por minuto.
     *
     * Progresion Am - F - C - G: es la vuelta mas comun que existe, y se usa
     * justamente por eso. En una portada no se trata de sorprender sino de
     * que el jugador se quede leyendo las opciones sin que la musica le pida
     * atencion.
     *
     * Piano electrico para los acordes, contrabajo para la fundamental y un
     * vibrafono que deja caer tres notas por compas. Nada mas: el silencio
     * entre notas es parte de la pieza.
     */
    /**
     * Swing: coloca la corchea del contratiempo a dos tercios del tiempo en
     * vez de a la mitad.
     *
     * Es el truco que separa una musica que suena "programada" de una que
     * suena tocada. Con corcheas rectas todo cae en una cuadricula perfecta y
     * el oido lo lee como plano; corriendo la segunda corchea un poco tarde
     * aparece el balanceo.
     *
     * @param tiempo  numero de tiempo (negra) dentro de la pieza
     * @param mitad   false la primera corchea, true la segunda
     */
    private static int swing(int tiempo, boolean mitad) {
        return tiempo * PPN + (mitad ? (PPN * 2) / 3 : 0);
    }

    /**
     * MENU. Cuatro compases en la menor, 96 negras por minuto, con swing.
     *
     * Armonia de septimas -Am7, Dm7, G7, Cmaj7- en vez de triadas. Una triada
     * tiene tres notas y suena cerrada; la septima agrega una cuarta que deja
     * el acorde "abierto" y pidiendo el siguiente. Es lo que hace que una
     * vuelta de cuatro acordes no canse al repetirse.
     *
     * El bajo CAMINA: cambia de nota en cada tiempo en vez de repetir la
     * fundamental. Eso solo ya quita la mitad de la sensacion de plano,
     * porque hay movimiento constante en el registro grave.
     *
     * Los acordes entran a contratiempo y la marimba lleva la melodia.
     */
    private static Sequence menu() throws Exception {
        Sequence s = new Sequence(Sequence.PPQ, PPN);
        Track t = s.createTrack();

        tempo(t, 96);
        instrumento(t, 0, 11);   // vibrafono: acordes a contratiempo
        instrumento(t, 1, 32);   // contrabajo: el bajo caminante
        instrumento(t, 3, 12);   // marimba: la melodia

        // Am7 - Dm7 - G7 - Cmaj7
        int[][] acordes = {
            {LA3, DO4, MI4, SOL4},
            {RE4, FA4, LA4, DO5},
            {SOL3, SI, RE4, FA4},
            {DO4, MI4, SOL4, SI + 12}
        };

        // Un paso por tiempo. Las notas de paso entre acorde y acorde son lo
        // que hace que el bajo suene como una linea y no como un ancla.
        int[][] bajo = {
            {LA2, DO3, RE3, MI3},
            {RE3, FA3, SOL3, LA3 - 12},
            {SOL3 - 12, SI - 12, DO3, RE3},
            {DO3, MI3, SOL3, MI3}
        };

        int[][] melodia = {
            {MI5, DO5, RE5, MI5},
            {FA4, LA4, DO5, LA4},
            {SI + 12, RE5, DO5, SOL4},
            {MI5, SOL4, DO5, -1}
        };

        for (int c = 0; c < 4; c++) {
            int base = c * 4;

            // Bajo: una negra por tiempo, caminando
            for (int p = 0; p < 4; p++) {
                nota(t, 1, bajo[c][p], swing(base + p, false), PPN - 14,
                     p == 0 ? 72 : 58);
            }

            // Acordes a contratiempo: en la segunda corchea del 2 y del 4.
            // Entrar tarde es lo que da el empuje; en el golpe suena a himno.
            for (int golpe : new int[]{1, 3}) {
                for (int nota : acordes[c]) {
                    nota(t, 0, nota, swing(base + golpe, true), PPN - 20, 40);
                }
            }

            // Marimba: corcheas con swing, saltando alguna para que respire
            int[] m = melodia[c];
            for (int p = 0; p < 4; p++) {
                if (m[p] < 0) {
                    continue;
                }
                nota(t, 3, m[p], swing(base + p, p % 2 == 1), PPN / 2, 52 - p * 2);
            }

            // Escobilla: charles cerrado en todas las corcheas, muy bajito.
            // Es el relleno que impide que haya un solo instante vacio.
            for (int p = 0; p < 4; p++) {
                nota(t, 9, 42, swing(base + p, false), 10, 26);
                nota(t, 9, 42, swing(base + p, true), 10, 18);
            }
        }
        return s;
    }

    /**
     * VENTANILLA. Cuatro compases en la menor, 84 negras por minuto.
     *
     * No tiene melodia, y eso no es pereza: es el requisito. Lo que lleva la
     * pieza es un bajo en negras -el ritmo al que se atiende a la gente- y un
     * bloque de madera en el contratiempo, que es casi el tic del reloj que
     * ya existe como efecto. Encima, una nota larga de cuerdas que cambia
     * cada dos compases y es lo unico que se mueve.
     *
     * La marimba suelta dos notas sueltas por vuelta, en sitios distintos,
     * para que el bucle no se note tanto.
     */
    private static Sequence ventanilla() throws Exception {
        Sequence s = new Sequence(Sequence.PPQ, PPN);
        Track t = s.createTrack();

        tempo(t, 80);
        instrumento(t, 0, 32);   // contrabajo: el bajo caminante
        instrumento(t, 2, 48);   // cuerdas: el colchon que nunca se corta
        instrumento(t, 3, 11);   // vibrafono: una nota cada dos compases

        // Am7 - Dm7 - Fmaj7 - G7. Comparte tonalidad con el menu, asi que
        // pasar de una pantalla a otra no suena a corte, pero no es la
        // misma vuelta: si lo fuera, el jugador oiria la misma pieza toda
        // la partida.
        int[][] bajo = {
            {LA2, DO3, MI3, DO3},
            {RE3, FA3, LA3 - 12, FA3},
            {FA3 - 12, LA2, DO3, LA2},
            {SOL3 - 12, SI - 12, RE3, SI - 12}
        };
        int[][] colchon = {
            {DO4, MI4, SOL4},
            {FA4, LA4, DO5},
            {FA4, DO5, MI5},
            {FA4, SOL4, SI + 12}
        };

        int compas = PPN * 4;

        for (int c = 0; c < 4; c++) {
            int inicio = c * compas;

            // Bajo caminante, una negra por tiempo. Aqui esta casi todo el
            // movimiento de la pieza: sin melodia, el bajo es lo que avanza.
            for (int p = 0; p < 4; p++) {
                nota(t, 0, bajo[c][p], swing(c * 4 + p, false), PPN - 16,
                     p == 0 ? 54 : 42);
            }

            // Cuerdas: el acorde entero sostenido todo el compas. Es lo que
            // tapa los huecos; las cuerdas no decaen como el piano.
            for (int nota : colchon[c]) {
                nota(t, 2, nota, inicio, compas - 6, 24);
            }

            // Escobilla con swing, mas bajita que en el menu. Marca el
            // balanceo sin pedir atencion.
            for (int p = 0; p < 4; p++) {
                nota(t, 9, 42, swing(c * 4 + p, false), 10, 18);
                nota(t, 9, 42, swing(c * 4 + p, true), 10, 12);
            }

            // Vibrafono: UNA nota cada dos compases, a contratiempo. No es
            // melodia a proposito: esto suena veinte minutos seguidos, y lo
            // que se tararea al tercer bucle se odia al decimo.
            if (c % 2 == 1) {
                nota(t, 3, c == 1 ? LA4 : DO5, swing(c * 4 + 2, true), PPN, 30);
            }
        }
        return s;
    }

    // ------------------------------------------------------------------
    // Utilidades para escribir notas
    // ------------------------------------------------------------------

    private static void nota(Track t, int canal, int altura, int inicio, int duracion,
                             int fuerza) throws Exception {
        t.add(new javax.sound.midi.MidiEvent(
                new ShortMessage(ShortMessage.NOTE_ON, canal, altura, fuerza), inicio));
        t.add(new javax.sound.midi.MidiEvent(
                new ShortMessage(ShortMessage.NOTE_OFF, canal, altura, 0), inicio + duracion));
    }

    private static void instrumento(Track t, int canal, int programa) throws Exception {
        t.add(new javax.sound.midi.MidiEvent(
                new ShortMessage(ShortMessage.PROGRAM_CHANGE, canal, programa, 0), 0));
    }

    /** Mensaje meta de tempo: microsegundos por negra. */
    private static void tempo(Track t, int negrasPorMinuto) throws Exception {
        int usPorNegra = 60000000 / negrasPorMinuto;
        byte[] datos = {
            (byte) (usPorNegra >> 16 & 0xFF),
            (byte) (usPorNegra >> 8 & 0xFF),
            (byte) (usPorNegra & 0xFF)
        };
        t.add(new javax.sound.midi.MidiEvent(
                new javax.sound.midi.MetaMessage(0x51, datos, 3), 0));
    }
}
