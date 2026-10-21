package alcaldedigital.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Instrucciones y Ayuda del juego, con el mismo estilo que los Ajustes.
 *   - {@link #instruccionesIniciales(Window)}: versión corta al llegar al mapa.
 *   - {@link #ayuda(Window)}: sección AYUDA completa (objetivo, reglas, controles,
 *     botones, símbolos, mapa de niveles, cómo ganar e indicadores).
 * Cada símbolo se muestra con la imagen de su burbuja al lado del nombre.
 */
public class DialogoInstrucciones extends DialogoJuego {

    private static final int ANCHO_TEXTO = 560;

    /** imagen, nombre, cuándo usarlo */
    private static final String[][] SIMBOLOS = {
        {"compartir.png", "Compartir", "Información útil y confiable para la comunidad: ayúdala a circular."},
        {"verificar.png", "Verificar", "Dice ser oficial o \"dicen que...\", pero no trae fuente: hay que confirmarla."},
        {"ignorar.png", "Ignorar", "Opiniones y gustos personales: no les des más alcance ni pelees."},
        {"reportar.png", "Reportar", "Acusaciones graves o noticias falsas que hacen daño: denúncialas."},
    };

    private static final Color[] COLORES = {
        new Color(0xB9, 0x8C, 0xE6),   // morado - compartir
        new Color(0x8F, 0xD6, 0x9A),   // verde  - verificar
        new Color(0x86, 0xC5, 0xF0),   // azul   - ignorar
        new Color(0xF0, 0x8A, 0x80),   // rojo   - reportar
    };

    private final JPanel cuerpo = new JPanel();

    private DialogoInstrucciones(Window padre, String titulo, String subtitulo) {
        super(padre, titulo);
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        JLabel lblSub = etiqueta(subtitulo, Font.ITALIC, 17, CREMA);
        lblSub.setAlignmentX(0.5f);
        cuerpo.add(lblSub);
        cuerpo.add(espacio(8));
    }

    // ------------------------------------------------------------------
    // Las dos versiones
    // ------------------------------------------------------------------

    /** Instrucciones cortas que aparecen la primera vez que se abre el mapa. */
    public static void instruccionesIniciales(Window padre) {
        DialogoInstrucciones d = new DialogoInstrucciones(padre, "Instrucciones", "¡Bienvenido a Alcalde Digital!");
        d.pasos(new String[]{
            "En el mapa, haz clic en el nivel 1. Al ganar un nivel se abren los dos que salen de él.",
            "Arriba verás una publicación de Civitas. Decide qué harías con ella.",
            "Muévete con ← → y salta con Espacio. Recolecta SOLO las burbujas de esa decisión.",
            "Una burbuja equivocada, caer en un hueco o quedarte sin tiempo te quita una vida (tienes 3).",
            "Resuelve 4 publicaciones para ganar el nivel."
        });
        d.simbolos();
        d.cierre("Cada decisión cambia la ciudad: los errores esparcen rumores por Ciudad Nova.");
        d.finalizar(false);
    }

    /** Sección AYUDA completa, disponible desde el mapa y desde cada nivel. */
    public static void ayuda(Window padre) {
        DialogoInstrucciones d = new DialogoInstrucciones(padre, "Ayuda", "Todo lo que necesitas para jugar");
        d.seccion("Objetivo del juego",
                "Ciudad Nova está en elecciones y en Civitas circulan rumores, opiniones y noticias falsas. "
                + "Tu misión es tomar decisiones responsables para que la ciudad llegue al día de elecciones "
                + "informada, con confianza y sin conflictos.");
        d.seccion("Controles",
                "← / A: izquierda    → / D: derecha    Espacio / ↑ / W: saltar (mantén para saltar más alto)    "
                + "P o Esc: pausa.");
        d.seccion("Reglas de cada nivel",
                "• Arriba aparece una publicación. Recolecta solo las burbujas de la acción correcta.\n"
                + "• Cada nivel pide más burbujas correctas por publicación (nivel 1: 3, nivel 7: 9).\n"
                + "• Burbuja equivocada, caer en un hueco o tiempo agotado = pierdes una vida. Tienes 3.\n"
                + "• Resuelve 4 publicaciones para ganar. Las vidas que te queden son tus estrellas.\n"
                + "• Desde el nivel 3 las burbujas se mueven, desde el 4 hay plataformas móviles "
                + "y desde el 5 los símbolos cambian solos (parpadean justo antes).");
        d.simbolos();
        d.seccion("Mapa de niveles",
                "Los niveles forman un grafo: el 1 abre el 2 y el 3; el 2 abre el 4 y el 5; el 3 abre el 6 y el 7. "
                + "Un nivel gris con candado sigue bloqueado. El que brilla está disponible. "
                + "Los ganados tienen anillo dorado y estrellas.");
        d.seccion("Cómo ganar",
                "Al ganar un nivel final (4, 5, 6 o 7) se abren las elecciones. Tus votos dependen en un 60% "
                + "de la salud de la ciudad y en un 40% de tu reputación. Con 50% o más eres el nuevo alcalde.");
        d.seccion("Indicadores de la ciudad",
                "Información verificada, Confianza, Convivencia y Bienestar digital: conviene que suban.\n"
                + "▼ Desinformación y ▼ Conflictos: conviene que bajen.\n"
                + "La red de la ciudad (panel derecho del nivel) muestra en rojo los lugares a los que llegó el rumor.");
        d.seccion("Botones",
                "? Ayuda: esta ventana.   ⚙ Ajustes: modo normal o alto contraste, brillo y sonido.   "
                + "Pausa: detiene el nivel.   Ciudad y estructuras: indicadores, bitácora, grafos y árboles del juego.");
        d.finalizar(true);
    }

    // ------------------------------------------------------------------
    // Bloques de contenido
    // ------------------------------------------------------------------

    private void pasos(String[] pasos) {
        JPanel lista = new JPanel(new GridBagLayout());
        lista.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(3, 0, 3, 10);
        for (int i = 0; i < pasos.length; i++) {
            c.gridy = i;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            lista.add(new Numero(i + 1), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            lista.add(parrafo(pasos[i], ANCHO_TEXTO - 34, 15, CREMA), c);
        }
        lista.setAlignmentX(0.5f);
        cuerpo.add(lista);
        cuerpo.add(espacio(10));
    }

    private void seccion(String titulo, String texto) {
        Tarjeta t = new Tarjeta(new BorderLayout(0, 4));
        t.add(etiqueta(titulo, Font.BOLD, 16, ORO), BorderLayout.NORTH);
        t.add(parrafo(texto, ANCHO_TEXTO + 8, 14, CREMA), BorderLayout.CENTER);
        t.setAlignmentX(0.5f);
        t.setMaximumSize(new Dimension(ANCHO_TEXTO + 40, Integer.MAX_VALUE));
        cuerpo.add(t);
        cuerpo.add(espacio(10));
    }

    private void simbolos() {
        JLabel lbl = etiqueta("Los símbolos de las burbujas", Font.BOLD, 17, ORO);
        lbl.setAlignmentX(0.5f);
        cuerpo.add(lbl);
        cuerpo.add(espacio(8));
        JPanel grilla = new JPanel(new GridLayout(2, 2, 12, 12));
        grilla.setOpaque(false);
        for (int i = 0; i < SIMBOLOS.length; i++) {
            grilla.add(tarjetaSimbolo(SIMBOLOS[i][0], SIMBOLOS[i][1], SIMBOLOS[i][2], COLORES[i]));
        }
        grilla.setAlignmentX(0.5f);
        grilla.setMaximumSize(new Dimension(ANCHO_TEXTO + 40, Integer.MAX_VALUE));
        cuerpo.add(grilla);
        cuerpo.add(espacio(10));
    }

    private void cierre(String texto) {
        JLabel l = parrafo(texto, ANCHO_TEXTO, Font.BOLD | Font.ITALIC, 15, ORO);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setAlignmentX(0.5f);
        cuerpo.add(l);
    }

    /** Coloca el contenido (con desplazamiento si es largo) y muestra el diálogo. */
    private void finalizar(boolean conScroll) {
        if (conScroll) {
            JScrollPane sp = new JScrollPane(cuerpo);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.getVerticalScrollBar().setUI(new BarraDorada());
            sp.getVerticalScrollBar().setOpaque(false);
            sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
            sp.getVerticalScrollBar().setUnitIncrement(18);
            sp.setPreferredSize(new Dimension(cuerpo.getPreferredSize().width + 24, 470));
            setContenido(sp);
        } else {
            setContenido(cuerpo);
        }
        agregarBoton(botonAceptar());
        mostrar();
    }

    private static JPanel tarjetaSimbolo(String imagen, String nombre, String descripcion, Color color) {
        Tarjeta t = new Tarjeta(new BorderLayout(12, 0), color);
        ImageIcon ic = icono(imagen, 56);
        JLabel lblImg = new JLabel(ic);
        if (ic == null) {
            lblImg.setText("?");
        }
        t.add(lblImg, BorderLayout.WEST);
        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        textos.add(etiqueta(nombre, Font.BOLD, 18, color), BorderLayout.NORTH);
        textos.add(parrafo(descripcion, 180, 13, CREMA), BorderLayout.CENTER);
        t.add(textos, BorderLayout.CENTER);
        return t;
    }

    /** Barra de desplazamiento delgada y dorada. */
    private static class BarraDorada extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = ORO;
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected javax.swing.JButton createDecreaseButton(int o) {
            return sinBoton();
        }

        @Override
        protected javax.swing.JButton createIncreaseButton(int o) {
            return sinBoton();
        }

        private static javax.swing.JButton sinBoton() {
            javax.swing.JButton b = new javax.swing.JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle r) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(conAlfa(ORO, 170));
            g2.fillRoundRect(r.x + 2, r.y, r.width - 4, r.height, 6, 6);
            g2.dispose();
        }
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
