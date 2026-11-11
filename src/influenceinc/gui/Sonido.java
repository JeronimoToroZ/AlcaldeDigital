package influenceinc.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Efectos de sonido SINTETIZADOS (ondas senoidales generadas en el codigo):
 * el juego no necesita archivos de audio.
 *
 * Cada sonido avisa tambien con un SUBTITULO en pantalla ("[sonido: alerta]"),
 * para quien no oye o juega sin volumen. Ese es uno de los componentes
 * inclusivos del juego.
 */
public final class Sonido {

    public enum Tipo {
        CLIC("clic", new double[][]{{880, 0.04}}),
        BIEN("acierto", new double[][]{{523, 0.08}, {659, 0.08}, {784, 0.14}}),
        MAL("error", new double[][]{{392, 0.12}, {311, 0.12}, {220, 0.22}}),
        ALERTA("alerta", new double[][]{{988, 0.09}, {0, 0.05}, {988, 0.09}}),
        LOGRO("logro desbloqueado", new double[][]{{523, 0.07}, {659, 0.07}, {784, 0.07}, {1047, 0.22}}),
        TIC("tic del reloj", new double[][]{{1400, 0.025}}),
        TURNO("nuevo turno", new double[][]{{660, 0.07}, {880, 0.1}}),
        DIFUSION("propagación", new double[][]{{300, 0.05}, {420, 0.05}, {560, 0.05}, {740, 0.08}}),
        TECLEO("tecleo", new double[][]{{1900, 0.012}}),
        SELLO("¡sello!", new double[][]{{140, 0.05}, {90, 0.12}}),
        TELEFONO("teléfono sonando", new double[][]{{1180, 0.12}, {1320, 0.12}, {0, 0.08}, {1180, 0.12}, {1320, 0.12}}),
        GLITCH("interferencia extraña", new double[][]{{2400, 0.02}, {180, 0.03}, {3100, 0.02}, {95, 0.06}}),
        ESCANER("escáner", new double[][]{{600, 0.05}, {800, 0.05}, {1000, 0.05}, {1200, 0.05}}),
        PUERTA("alguien llega a la ventanilla", new double[][]{{520, 0.06}, {390, 0.1}});

        final String subtitulo;
        final double[][] notas;

        Tipo(String subtitulo, double[][] notas) {
            this.subtitulo = subtitulo;
            this.notas = notas;
        }
    }

    /** Quien muestra el subtitulo. */
    public interface Subtitulos {
        void mostrar(String texto);
    }

    private static final ExecutorService HILO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sonido");
        t.setDaemon(true);
        return t;
    });
    private static Subtitulos subtitulos;
    private static boolean disponible = true;

    private Sonido() {
    }

    public static void setSubtitulos(Subtitulos s) {
        subtitulos = s;
    }

    public static void tocar(Tipo tipo) {
        if (subtitulos != null && tipo != Tipo.CLIC && tipo != Tipo.TIC && tipo != Tipo.TECLEO) {
            subtitulos.mostrar("[sonido: " + tipo.subtitulo + "]");
        }
        if (!Tema.sonido || !disponible) {
            return;
        }
        HILO.submit(() -> reproducir(tipo.notas));
    }

    private static void reproducir(double[][] notas) {
        float tasa = 22050f;
        try {
            AudioFormat f = new AudioFormat(tasa, 8, 1, true, false);
            SourceDataLine linea = AudioSystem.getSourceDataLine(f);
            linea.open(f, 4096);
            linea.start();
            for (double[] n : notas) {
                int muestras = (int) (n[1] * tasa);
                byte[] buf = new byte[muestras];
                for (int i = 0; i < muestras; i++) {
                    double env = Math.min(1, Math.min(i / 200.0, (muestras - i) / 400.0));
                    buf[i] = (byte) (n[0] <= 0 ? 0 : Math.sin(2 * Math.PI * n[0] * i / tasa) * 38 * env);
                }
                linea.write(buf, 0, buf.length);
            }
            linea.drain();
            linea.close();
        } catch (Exception | LinkageError e) {
            disponible = false;     // sin tarjeta de sonido: el juego sigue igual
        }
    }
}
