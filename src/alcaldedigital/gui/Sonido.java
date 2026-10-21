package alcaldedigital.gui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Efectos de sonido sintetizados (ondas simples), sin archivos de audio.
 * Respetan el volumen y el silencio de Ajustes. Suenan en un hilo aparte para
 * no congelar el juego; si el equipo no tiene audio, simplemente no suenan.
 *
 * Cada sonido tiene también una señal visual en pantalla (texto flotante,
 * destello o mensaje), para quien juega sin audio: componente inclusivo.
 */
public final class Sonido {

    private static final float FRECUENCIA_MUESTREO = 22050f;

    private Sonido() {
    }

    public static void burbujaCorrecta() { tocar(new int[]{660, 990}, 70); }
    public static void burbujaIncorrecta() { tocar(new int[]{220, 150}, 140); }
    public static void salto() { tocar(new int[]{420}, 45); }
    public static void publicacionResuelta() { tocar(new int[]{523, 659, 784}, 90); }
    public static void nivelGanado() { tocar(new int[]{523, 659, 784, 1046}, 130); }
    public static void nivelPerdido() { tocar(new int[]{392, 330, 262, 196}, 150); }
    public static void caida() { tocar(new int[]{300, 200, 120}, 90); }

    /** Toca una secuencia de notas (Hz), cada una durante "ms" milisegundos. */
    private static void tocar(final int[] notas, final int ms) {
        if (!Configuracion.isMusicaActiva() || Configuracion.getVolumenMusica() == 0) {
            return;
        }
        final double volumen = Configuracion.getVolumenMusica() / 100.0 * 0.35;
        Thread hilo = new Thread(() -> {
            try {
                AudioFormat formato = new AudioFormat(FRECUENCIA_MUESTREO, 8, 1, true, false);
                SourceDataLine linea = AudioSystem.getSourceDataLine(formato);
                linea.open(formato);
                linea.start();
                for (int nota : notas) {
                    int muestras = (int) (FRECUENCIA_MUESTREO * ms / 1000);
                    byte[] buffer = new byte[muestras];
                    for (int i = 0; i < muestras; i++) {
                        double t = i / FRECUENCIA_MUESTREO;
                        double envolvente = Math.min(1.0, (muestras - i) / (muestras * 0.3));
                        buffer[i] = (byte) (Math.sin(2 * Math.PI * nota * t) * 127 * volumen * envolvente);
                    }
                    linea.write(buffer, 0, buffer.length);
                }
                linea.drain();
                linea.close();
            } catch (Exception sinAudio) {
                // Sin tarjeta de sonido o sin permisos: el juego sigue en silencio.
            }
        }, "sonido");
        hilo.setDaemon(true);
        hilo.start();
    }
}
