package alcaldedigital.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Ventana de instrucciones del minijuego de burbujas, con el mismo estilo
 * que los Ajustes. Se usa en dos momentos:
 *   - al iniciar la partida ({@link #instruccionesIniciales(Window)}), y
 *   - desde el botón Ayuda de la barra superior ({@link #ayuda(Window)}).
 *
 * Cada acción se muestra con la imagen de su burbuja al lado del nombre,
 * para que el jugador reconozca los símbolos durante el juego.
 */
public class DialogoInstrucciones extends DialogoJuego {

    private static final int ANCHO_TEXTO = 470;

    /** Datos de cada símbolo: imagen (en recursos), nombre, color y cuándo usarlo. */
    private static final String[][] SIMBOLOS = {
        {"compartir.png", "Compartir", "Información útil y confiable para la comunidad: ayúdala a circular."},
        {"verificar.png", "Verificar", "Dice ser oficial, pero hay que confirmar la fuente antes de creerle."},
        {"ignorar.png", "Ignorar", "Opiniones o rumores sin sustento: no les des más alcance."},
        {"reportar.png", "Reportar", "Acusaciones graves sin pruebas: denúncialas para frenar el daño."},
    };

    private static final Color[] COLORES = {
        new Color(0xB9, 0x8C, 0xE6),   // morado - compartir
        new Color(0x8F, 0xD6, 0x9A),   // verde  - verificar
        new Color(0x86, 0xC5, 0xF0),   // azul   - ignorar
        new Color(0xF0, 0x8A, 0x80),   // rojo   - reportar
    };

    private DialogoInstrucciones(Window padre, String titulo, String subtitulo,
                                 String[] pasos, String cierre) {
        super(padre, titulo);

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        JLabel lblSub = etiqueta(subtitulo, Font.ITALIC, 17, CREMA);
        lblSub.setHorizontalAlignment(SwingConstants.CENTER);
        lblSub.setAlignmentX(0.5f);
        cuerpo.add(lblSub);
        cuerpo.add(espacio(12));

        // --- Pasos numerados ---
        JPanel listaPasos = new JPanel(new GridBagLayout());
        listaPasos.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(3, 0, 3, 10);
        for (int i = 0; i < pasos.length; i++) {
            c.gridy = i;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            listaPasos.add(new Numero(i + 1), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            listaPasos.add(parrafo(pasos[i], ANCHO_TEXTO - 40, 15, CREMA), c);
        }
        listaPasos.setAlignmentX(0.5f);
        cuerpo.add(listaPasos);
        cuerpo.add(espacio(14));

        // --- Símbolos con su imagen ---
        JLabel lblSimbolos = etiqueta("Los símbolos de las burbujas", Font.BOLD, 17, ORO);
        lblSimbolos.setAlignmentX(0.5f);
        cuerpo.add(lblSimbolos);
        cuerpo.add(espacio(8));

        JPanel grilla = new JPanel(new GridLayout(2, 2, 12, 12));
        grilla.setOpaque(false);
        for (int i = 0; i < SIMBOLOS.length; i++) {
            grilla.add(tarjetaSimbolo(SIMBOLOS[i][0], SIMBOLOS[i][1], SIMBOLOS[i][2], COLORES[i]));
        }
        grilla.setAlignmentX(0.5f);
        cuerpo.add(grilla);

        if (cierre != null) {
            cuerpo.add(espacio(14));
            JLabel lblCierre = parrafo(cierre, ANCHO_TEXTO, 15, ORO);
            lblCierre.setFont(fuente(Font.BOLD | Font.ITALIC, 15));
            lblCierre.setHorizontalAlignment(SwingConstants.CENTER);
            lblCierre.setAlignmentX(0.5f);
            cuerpo.add(lblCierre);
        }

        setContenido(cuerpo);
        agregarBoton(botonAceptar());
    }

    /** Instrucciones que aparecen al empezar la partida. */
    public static void instruccionesIniciales(Window padre) {
        new DialogoInstrucciones(padre, "Instrucciones",
                "¡Bienvenido a Alcalde Digital!",
                new String[]{
                    "Analiza el post que aparece en pantalla durante 5 segundos.",
                    "Dispara rápidamente a 8 burbujas correctas entre 20 burbujas flotantes.",
                    "¡Atención! Los símbolos de las burbujas cambian cada 1.8 segundos.",
                    "Si tocas una burbuja equivocada, la situación se resuelve como error."
                },
                "¡Menor tiempo de reacción otorga mayor puntuación extra en cada turno!").mostrar();
    }

    /** Ayuda disponible en cualquier momento desde la barra superior del juego. */
    public static void ayuda(Window padre) {
        new DialogoInstrucciones(padre, "Ayuda",
                "Instrucciones del minijuego de burbujas",
                new String[]{
                    "Lee el post dentro del recuadro central.",
                    "Dispara con el ratón a 8 burbujas correctas entre 20 burbujas flotantes "
                            + "que se desplazan por la pantalla.",
                    "Los símbolos de las burbujas cambian cada 1.8 segundos."
                },
                "Gana más puntos quien lo haga más rápido en las 4 situaciones.").mostrar();
    }

    // ------------------------------------------------------------------

    private static JPanel tarjetaSimbolo(String imagen, String nombre, String descripcion, Color color) {
        Tarjeta t = new Tarjeta(new BorderLayout(12, 0), color);
        ImageIcon ic = icono(imagen, 58);
        JLabel lblImg = new JLabel(ic);
        if (ic == null) {
            lblImg.setText("?");
        }
        t.add(lblImg, BorderLayout.WEST);

        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        textos.add(etiqueta(nombre, Font.BOLD, 18, color), BorderLayout.NORTH);
        textos.add(parrafo(descripcion, 150, 13, CREMA), BorderLayout.CENTER);
        t.add(textos, BorderLayout.CENTER);
        return t;
    }

    /** Círculo dorado con el número del paso. */
    private static class Numero extends JComponent {

        private final int n;

        Numero(int n) {
            this.n = n;
            setPreferredSize(new Dimension(24, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(ORO);
            g2.fillOval(1, 1, 22, 22);
            g2.setColor(CAFE_OSCURO);
            g2.setFont(fuente(Font.BOLD, 13));
            String s = String.valueOf(n);
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, 12 - fm.stringWidth(s) / 2, 12 + fm.getAscent() / 2 - 1);
            g2.dispose();
        }
    }
}
