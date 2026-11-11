package influenceinc.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JEditorPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Colores y fuentes del juego, con MODO DE ALTO CONTRASTE.
 *
 * Los colores de las agencias son la paleta Okabe-Ito, pensada para que
 * se distingan con los tipos mas comunes de daltonismo. Ademas cada agencia
 * tiene un SIMBOLO (circulo, cuadrado, triangulo, rombo) y las cuentas
 * desinformadas llevan una X: nada depende solo del color.
 */
public final class Tema {

    public static boolean altoContraste = false;
    public static boolean sonido = true;
    /** Subtitulos de los sonidos en pantalla (componente inclusivo). */
    public static boolean subtitulos = true;
    /** Texto 15 % mas grande (componente inclusivo). */
    public static boolean textoGrande = false;

    private static final Color[] AGENCIAS = {
        new Color(230, 159, 0),     // naranja
        new Color(86, 180, 233),    // azul cielo
        new Color(0, 158, 115),     // verde azulado
        new Color(204, 121, 167)    // rosado
    };
    private static final Color[] AGENCIAS_AC = {
        new Color(255, 200, 0), new Color(0, 220, 255), new Color(0, 255, 120), new Color(255, 110, 255)
    };

    public static final String FUENTE = "SansSerif";

    private Tema() {
    }

    public static Color agencia(int i) {
        if (i < 0) {
            return neutro();
        }
        return (altoContraste ? AGENCIAS_AC : AGENCIAS)[i % 4];
    }

    public static Color fondo() {
        return altoContraste ? Color.BLACK : new Color(10, 12, 30);
    }

    public static Color panel() {
        return altoContraste ? Color.BLACK : new Color(18, 20, 44);
    }

    public static Color tarjeta() {
        return altoContraste ? new Color(10, 10, 10) : new Color(28, 30, 62);
    }

    public static Color borde() {
        return altoContraste ? Color.WHITE : new Color(70, 76, 140);
    }

    public static Color texto() {
        return altoContraste ? Color.WHITE : new Color(232, 234, 255);
    }

    public static Color textoSuave() {
        return altoContraste ? new Color(255, 255, 160) : new Color(150, 156, 200);
    }

    public static Color acento() {
        return altoContraste ? new Color(255, 230, 0) : new Color(124, 92, 255);
    }

    public static Color acento2() {
        return altoContraste ? new Color(0, 255, 255) : new Color(0, 214, 255);
    }

    public static Color bien() {
        return altoContraste ? new Color(0, 255, 0) : new Color(46, 204, 140);
    }

    public static Color mal() {
        return altoContraste ? new Color(255, 60, 60) : new Color(213, 94, 0);
    }

    public static Color neutro() {
        return altoContraste ? new Color(200, 200, 200) : new Color(120, 132, 175);
    }

    public static Color bulo() {
        return altoContraste ? new Color(255, 40, 40) : new Color(235, 64, 52);
    }

    public static Font fuente(int estilo, float tam) {
        return new Font(FUENTE, estilo, Math.round(textoGrande ? tam * 1.15f : tam));
    }

    public static Border bordeTarjeta() {
        return new CompoundBorder(new LineBorder(borde(), altoContraste ? 2 : 1, true),
                new EmptyBorder(10, 12, 10, 12));
    }

    /**
     * Recorre todos los componentes y les pone los colores del tema actual.
     * Se llama al activar o desactivar el alto contraste.
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
