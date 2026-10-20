package alcaldedigital.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.plaf.LayerUI;

/**
 * Ajustes del juego (modo visual, brillo y música). Los valores se guardan
 * en el equipo del usuario, así que se recuerdan al abrir el juego otra vez.
 *
 * Música: cuando exista el reproductor, basta con leer {@link #getVolumenMusica()}
 * y {@link #isMusicaActiva()}, y registrar un oyente con {@link #agregarOyente(Runnable)}
 * para actualizar el volumen en vivo.
 */
public final class Configuracion {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Configuracion.class);
    private static final List<Runnable> OYENTES = new ArrayList<>();

    private static boolean altoContraste = PREFS.getBoolean("altoContraste", false);
    private static int brillo = PREFS.getInt("brillo", 100);          // 40..100
    private static int volumenMusica = PREFS.getInt("volumenMusica", 70); // 0..100
    private static boolean musicaActiva = PREFS.getBoolean("musicaActiva", true);

    private Configuracion() {
    }

    /** Aplica al tema del juego lo que quedó guardado. Llamar al iniciar. */
    public static void aplicarGuardada() {
        Tema.setAltoContraste(altoContraste);
    }

    // ---- getters ----
    public static boolean isAltoContraste() { return altoContraste; }
    public static int getBrillo() { return brillo; }
    public static int getVolumenMusica() { return volumenMusica; }
    public static boolean isMusicaActiva() { return musicaActiva; }

    // ---- setters (guardan y avisan a las pantallas abiertas) ----
    public static void setAltoContraste(boolean v) {
        altoContraste = v;
        Tema.setAltoContraste(v);
        PREFS.putBoolean("altoContraste", v);
        avisar();
    }

    public static void setBrillo(int v) {
        brillo = Math.max(40, Math.min(100, v));
        PREFS.putInt("brillo", brillo);
        avisar();
    }

    public static void setVolumenMusica(int v) {
        volumenMusica = Math.max(0, Math.min(100, v));
        PREFS.putInt("volumenMusica", volumenMusica);
        avisar();
    }

    public static void setMusicaActiva(boolean v) {
        musicaActiva = v;
        PREFS.putBoolean("musicaActiva", v);
        avisar();
    }

    public static void agregarOyente(Runnable r) {
        OYENTES.add(r);
    }

    private static void avisar() {
        for (Runnable r : new ArrayList<>(OYENTES)) {
            r.run();
        }
    }

    // ---- brillo ----

    /** Oscurece lo dibujado hasta ahora según el brillo. Llamar al final del paint. */
    public static void pintarBrillo(Graphics g, int ancho, int alto) {
        if (brillo >= 100) {
            return;
        }
        int alfa = (int) ((100 - brillo) / 100.0 * 0.85 * 255);
        g.setColor(new Color(0, 0, 0, alfa));
        g.fillRect(0, 0, ancho, alto);
    }

    /** Hace que una ventana completa respete el brillo (capa transparente encima). */
    public static void instalarBrillo(JFrame ventana) {
        LayerUI<java.awt.Container> capa = new LayerUI<java.awt.Container>() {
            @Override
            public void paint(Graphics g, javax.swing.JComponent c) {
                super.paint(g, c);
                pintarBrillo(g, c.getWidth(), c.getHeight());
            }
        };
        JLayer<java.awt.Container> layer = new JLayer<>(ventana.getContentPane(), capa);
        ventana.setContentPane(layer);
        agregarOyente(layer::repaint);
    }
}
