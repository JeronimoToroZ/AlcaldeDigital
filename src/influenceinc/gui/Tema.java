package influenceinc.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * PALETA y TIPOGRAFIA de Alcalde Digital, con MODO DE ALTO CONTRASTE.
 *
 * Toda la paleta sale de la imagen de portada (escritorio de madera a la luz
 * de una lampara): cafes de madera, cobre, crema de papel, rosa empolvado y el
 * lila de la app Civitas. Ningun elemento del juego usa colores fuera de ella.
 *
 *   madera oscura  #2A1712   panel     #3B2119   cobre   #C8693A
 *   crema / papel  #F7EBDD   tinta     #3B2219   dorado  #F2B35E
 *   rosa empolvado #D9B2A6   lila      #7A5C99   salvia  #6E9E58   ladrillo #C0473A
 *
 * Los colores de "bien" y "mal" no son la unica pista: siempre van con un
 * icono o simbolo (componente inclusivo para daltonismo).
 */
public final class Tema {

    public static boolean altoContraste = false;
    public static boolean sonido = true;
    /** Subtitulos de los sonidos en pantalla (componente inclusivo). */
    public static boolean subtitulos = true;
    /** Texto 15 % mas grande (componente inclusivo). */
    public static boolean textoGrande = false;

    // ---- la paleta ----
    public static final Color MADERA = new Color(42, 23, 18);
    public static final Color MADERA_MEDIA = new Color(74, 40, 28);
    public static final Color CAOBA = new Color(110, 54, 33);
    public static final Color COBRE = new Color(200, 105, 58);
    public static final Color DORADO = new Color(242, 179, 94);
    public static final Color CREMA = new Color(247, 235, 221);
    public static final Color PAPEL = new Color(252, 245, 236);
    public static final Color ROSA = new Color(217, 178, 166);
    public static final Color ROSA_CLARO = new Color(238, 214, 205);
    public static final Color LILA = new Color(122, 92, 153);
    public static final Color LILA_CLARO = new Color(196, 178, 214);
    public static final Color SALVIA = new Color(110, 158, 88);
    public static final Color LADRILLO = new Color(192, 71, 58);
    public static final Color TINTA = new Color(59, 34, 25);
    public static final Color TINTA_SUAVE = new Color(138, 102, 86);

    private static final Color[] AGENCIAS = {COBRE, LILA, SALVIA, DORADO};
    private static final Color[] AGENCIAS_AC = {
        new Color(255, 200, 0), new Color(0, 220, 255), new Color(0, 255, 120), new Color(255, 110, 255)
    };

    public static final String FUENTE = "Poppins";

    private Tema() {
    }

    public static Color agencia(int i) {
        if (i < 0) {
            return neutro();
        }
        return (altoContraste ? AGENCIAS_AC : AGENCIAS)[i % 4];
    }

    /** Fondo oscuro general (madera). */
    public static Color fondo() {
        return altoContraste ? Color.BLACK : MADERA;
    }

    /** Paneles oscuros (pantallas, tarjetas sobre madera). */
    public static Color panel() {
        return altoContraste ? Color.BLACK : new Color(59, 33, 25);
    }

    public static Color tarjeta() {
        return altoContraste ? new Color(10, 10, 10) : MADERA_MEDIA;
    }

    public static Color borde() {
        return altoContraste ? Color.WHITE : new Color(140, 88, 64);
    }

    /** Texto sobre superficies oscuras. */
    public static Color texto() {
        return altoContraste ? Color.WHITE : CREMA;
    }

    public static Color textoSuave() {
        return altoContraste ? new Color(255, 255, 160) : new Color(214, 177, 158);
    }

    /** Superficie clara (papel, tarjetas claras). */
    public static Color papel() {
        return altoContraste ? Color.BLACK : PAPEL;
    }

    /** Texto sobre superficies claras. */
    public static Color tinta() {
        return altoContraste ? Color.WHITE : TINTA;
    }

    public static Color tintaSuave() {
        return altoContraste ? new Color(255, 255, 160) : TINTA_SUAVE;
    }

    public static Color acento() {
        return altoContraste ? new Color(255, 230, 0) : COBRE;
    }

    public static Color acento2() {
        return altoContraste ? new Color(0, 255, 255) : DORADO;
    }

    public static Color lila() {
        return altoContraste ? new Color(255, 110, 255) : LILA;
    }

    public static Color bien() {
        return altoContraste ? new Color(0, 255, 0) : SALVIA;
    }

    public static Color mal() {
        return altoContraste ? new Color(255, 60, 60) : LADRILLO;
    }

    public static Color neutro() {
        return altoContraste ? new Color(200, 200, 200) : new Color(156, 130, 118);
    }

    public static Color bulo() {
        return altoContraste ? new Color(255, 40, 40) : LADRILLO;
    }

    /** Mezcla dos colores (k = 0 → a, k = 1 → b). */
    public static Color mezclar(Color a, Color b, double k) {
        k = Math.max(0, Math.min(1, k));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * k));
    }

    public static Color alfa(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    /** La tipografia unica del juego: Poppins (con texto grande si esta activado). */
    public static Font fuente(int estilo, float tam) {
        return Recursos.fuente(estilo, textoGrande ? tam * 1.15f : tam);
    }

    /** Poppins Medium: para parrafos que deben leerse con comodidad. */
    public static Font media(float tam) {
        return Recursos.media(textoGrande ? tam * 1.15f : tam);
    }

    public static Border bordeTarjeta() {
        return new CompoundBorder(new LineBorder(borde(), altoContraste ? 2 : 1, true),
                new EmptyBorder(10, 12, 10, 12));
    }

    /**
     * Recorre todos los componentes Swing y les pone los colores del tema actual.
     */
    public static void aplicar(Component c) {
        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;
            Object rol = l.getClientProperty("rol");
            if ("suave".equals(rol)) {
                l.setForeground(textoSuave());
            } else if ("acento".equals(rol)) {
                l.setForeground(acento2());
            } else if (!"fijo".equals(rol)) {
                l.setForeground(texto());
            }
        } else if (c instanceof JTextArea) {
            c.setForeground(texto());
            c.setBackground(tarjeta());
        } else if (c instanceof JEditorPane) {
            c.setBackground(tarjeta());
        } else if (c instanceof JScrollPane) {
            c.setBackground(panel());
            ((JScrollPane) c).getViewport().setBackground(tarjeta());
        } else if (c instanceof JPanel && ((JPanel) c).isOpaque()
                && !"fijo".equals(((JComponent) c).getClientProperty("rol"))) {
            Object rol = ((JComponent) c).getClientProperty("rol");
            c.setBackground("tarjeta".equals(rol) ? tarjeta() : panel());
            if ("tarjeta".equals(rol)) {
                ((JPanel) c).setBorder(bordeTarjeta());
            }
        } else if (c instanceof AbstractButton && !(c instanceof BotonNeon)) {
            c.setForeground(texto());
        }
        if (c instanceof Container) {
            for (Component h : ((Container) c).getComponents()) {
                aplicar(h);
            }
        }
        c.repaint();
    }

    public static JLabel etiqueta(String texto, int estilo, float tam) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente(estilo, tam));
        l.setForeground(texto());
        return l;
    }

    public static JLabel suave(String texto, float tam) {
        JLabel l = etiqueta(texto, Font.PLAIN, tam);
        l.putClientProperty("rol", "suave");
        l.setForeground(textoSuave());
        return l;
    }

    public static JPanel tarjetaPanel() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.putClientProperty("rol", "tarjeta");
        p.setBackground(tarjeta());
        p.setBorder(bordeTarjeta());
        return p;
    }

    public static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
