package alcaldedigital.gui;
 
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
 
/**
 * Componente inclusivo que centraliza la paleta de colores y tipografías del sistema,
 * permitiendo alternar en caliente a un modo de alto contraste para atender
 * requerimientos de accesibilidad (baja visión y daltonismo).
 *
 * Estilo visual: "periódico / prensa" (papel crema, tinta negra, tipografía serif,
 * bordes gruesos y sombras duras). Garantiza que la información nunca dependa
 * exclusivamente del color, combinando altos índices de contraste, patrones
 * visuales y símbolos numéricos.
 *
 * @author Naty
 */
public final class Tema {
 
    private static boolean altoContraste = false;
 
    /** Grosor de la sombra dura de tarjetas y botones. */
    private static final int SOMBRA = 5;
 
    /** Familias tipográficas: se usa la primera que exista en el sistema. */
    private static final String FAMILIA = elegirFuente(
            "Georgia", "Cambria", "Palatino Linotype", "Book Antiqua", "Times New Roman", "Serif");
    private static final String FAMILIA_MONO = elegirFuente(
            "Courier New", "Courier Prime", "Consolas", "Monospaced");
 
    private Tema() {
    }
 
    private static String elegirFuente(String... candidatas) {
        Set<String> disponibles = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : candidatas) {
            if (disponibles.contains(f)) {
                return f;
            }
        }
        return candidatas[candidatas.length - 1];
    }
 
    /** Activa o desactiva globalmente el modo de alto contraste. */
    public static void setAltoContraste(boolean valor) {
        altoContraste = valor;
    }
 
    /** Consulta si el modo de alto contraste está habilitado. */
    public static boolean isAltoContraste() {
        return altoContraste;
    }
 
    // ------------------------------------------------------------------
    // Colores base (papel + tinta)
    // ------------------------------------------------------------------
 
    /** Papel de fondo. */
    public static Color fondo() {
        return altoContraste ? Color.BLACK : new Color(238, 229, 208);
    }
 
    /** Papel más claro para tarjetas y áreas de lectura. */
    public static Color panel() {
        return altoContraste ? Color.BLACK : new Color(250, 246, 234);
    }
 
    /** Tinta. */
    public static Color texto() {
        return altoContraste ? Color.WHITE : new Color(26, 26, 30);
    }
 
    public static Color textoSuave() {
        return altoContraste ? Color.WHITE : new Color(98, 90, 78);
    }
 
    public static Color borde() {
        return altoContraste ? Color.WHITE : new Color(26, 26, 30);
    }
 
    // ------------------------------------------------------------------
    // Colores funcionales
    // ------------------------------------------------------------------
 
    public static Color acento() {
        return altoContraste ? new Color(255, 255, 0) : new Color(28, 70, 156);
    }
 
    public static Color positivo() {
        return altoContraste ? new Color(0, 255, 128) : new Color(20, 126, 72);
    }
 
    public static Color negativo() {
        return altoContraste ? new Color(255, 128, 0) : new Color(200, 38, 46);
    }
 
    public static Color nodo() {
        return altoContraste ? Color.BLACK : new Color(255, 252, 242);
    }
 
    /** Color de "marcador fluorescente" (usado en el cronómetro). */
    public static Color resaltado() {
        return altoContraste ? Color.BLACK : new Color(255, 212, 0);
    }
 
    // ------------------------------------------------------------------
    // Tipografías
    // ------------------------------------------------------------------
 
    public static Font titulo() {
        return new Font(FAMILIA, Font.BOLD, altoContraste ? 28 : 26);
    }
 
    public static Font subtitulo() {
        return new Font(FAMILIA, Font.BOLD, altoContraste ? 17 : 15);
    }
 
    public static Font normal() {
        return new Font(FAMILIA, Font.PLAIN, altoContraste ? 16 : 14);
    }
 
    /** Fuente para la frase principal de la publicación (estilo titular). */
    public static Font destacado() {
        return new Font(FAMILIA, Font.BOLD, altoContraste ? 26 : 24);
    }
 
    /** Fuente para las etiquetas de indicadores. */
    public static Font indicador() {
        return new Font(FAMILIA, Font.PLAIN, altoContraste ? 15 : 13);
    }
 
    public static Font indicadorNegrita() {
        return new Font(FAMILIA, Font.BOLD, altoContraste ? 15 : 13);
    }
 
    public static Font mono() {
        return new Font(FAMILIA_MONO, Font.BOLD, altoContraste ? 15 : 13);
    }
 
    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------
 
    /** Activa suavizado de bordes y de texto en el contexto gráfico. */
    public static void suavizar(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
 
    /** Devuelve el mismo color con una transparencia dada (0-255). */
    public static Color conAlfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alfa);
    }
 
    /** Aplica el tema a componentes globales (tooltips). */
    public static void instalarUI() {
        UIManager.put("ToolTip.background", panel());
        UIManager.put("ToolTip.foreground", texto());
        UIManager.put("ToolTip.font", normal());
    }
 
    /**
     * Muestra un cuadro de diálogo modal con el estilo del juego (reemplaza a JOptionPane).
     */
    public static void mostrarDialogo(Component padre, String titulo, String mensaje) {
        Window dueno = padre instanceof Window ? (Window) padre : SwingUtilities.getWindowAncestor(padre);
        final JDialog d = new JDialog(dueno, titulo, Dialog.ModalityType.APPLICATION_MODAL);
 
        JTextArea area = new JTextArea(mensaje);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(indicador());
        area.setForeground(texto());
        area.setFocusable(false);
        area.setSize(new Dimension(520, Short.MAX_VALUE));
        int alto = Math.min(420, area.getPreferredSize().height + 6);
 
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setPreferredSize(new Dimension(540, alto));
        estilizarScroll(sp);
 
        Boton ok = new Boton("ACEPTAR", Tema::acento);
        ok.setPreferredSize(new Dimension(160, 46));
        ok.addActionListener(e -> d.dispose());
 
        JPanel abajo = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        abajo.setOpaque(false);
        abajo.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        abajo.add(ok);
 
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(fondo());
        raiz.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borde(), 3),
                BorderFactory.createEmptyBorder(22, 26, 22, 26)));
        raiz.add(sp, BorderLayout.CENTER);
        raiz.add(abajo, BorderLayout.SOUTH);
 
        d.setContentPane(raiz);
        d.getRootPane().setDefaultButton(ok);
        d.pack();
        d.setResizable(false);
        d.setLocationRelativeTo(padre);
        d.setVisible(true);
    }
 
    /** Aplica barras de desplazamiento cuadradas y planas a un JScrollPane. */
    public static void estilizarScroll(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new BarraFina());
        sp.getHorizontalScrollBar().setUI(new BarraFina());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getHorizontalScrollBar().setUnitIncrement(16);
    }
 
    // ------------------------------------------------------------------
    // Componentes visuales
    // ------------------------------------------------------------------
 
    /**
     * Panel estilo "recorte de periódico": esquinas rectas, borde grueso de tinta
     * y sombra dura desplazada. El contenido debe dejar un margen extra a la derecha
     * y abajo (alrededor de 6 px) para no pisar la sombra.
     */
    public static class Tarjeta extends JPanel {
 
        public Tarjeta(LayoutManager layout, int radio) {
            super(layout);
            setOpaque(false);
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth() - SOMBRA - 1;
            int h = getHeight() - SOMBRA - 1;
 
            g2.setColor(borde());
            g2.fillRect(SOMBRA, SOMBRA, w, h);          // sombra dura
            g2.setColor(panel());
            g2.fillRect(0, 0, w, h);                    // hoja
            g2.setColor(borde());
            g2.setStroke(new BasicStroke(altoContraste ? 3f : 2.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g2.drawRect(1, 1, w - 2, h - 2);            // borde
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
    /**
     * Botón cuadrado con sombra de color: al pasar el mouse se rellena con su color
     * y al presionarlo "se hunde" sobre su sombra.
     */
    public static class Boton extends JButton {
 
        private final Supplier<Color> colorAcento;
        private boolean encima;
 
        public Boton(String texto, Supplier<Color> colorAcento) {
            super(texto);
            this.colorAcento = colorAcento;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            putClientProperty("moderno", Boolean.TRUE);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    encima = true;
                    repaint();
                }
 
                @Override
                public void mouseExited(MouseEvent e) {
                    encima = false;
                    repaint();
                }
            });
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            Color ac = colorAcento.get();
            boolean activo = isEnabled();
            boolean presionado = activo && getModel().isPressed();
            boolean resaltar = activo && encima;
 
            int bw = getWidth() - SOMBRA - 1;
            int bh = getHeight() - SOMBRA - 1;
            int off = presionado ? SOMBRA : 0;
 
            if (activo && !presionado) {
                g2.setColor(ac);
                g2.fillRect(SOMBRA, SOMBRA, bw, bh);     // sombra de color
            }
            g2.setColor(resaltar ? ac : (activo ? panel() : fondo()));
            g2.fillRect(off, off, bw, bh);
            g2.setColor(activo ? borde() : textoSuave());
            g2.setStroke(new BasicStroke(altoContraste ? 3f : 2.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g2.drawRect(off + 1, off + 1, bw - 2, bh - 2);
 
            g2.setFont(subtitulo());
            FontMetrics fm = g2.getFontMetrics();
            String t = getText();
            Color colTexto = !activo ? textoSuave()
                    : (resaltar ? (altoContraste ? Color.BLACK : Color.WHITE) : texto());
            g2.setColor(colTexto);
            g2.drawString(t, off + (bw - fm.stringWidth(t)) / 2,
                    off + (bh - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
 
    /** Pestañas tipo "fichas" de periódico: bloque de tinta para la activa. */
    public static class PestanasUI extends BasicTabbedPaneUI {
 
        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(9, 24, 9, 24);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(8, 12, 0, 12);
        }
 
        @Override
        protected void paintTabBackground(Graphics g, int tp, int i, int x, int y, int w, int h, boolean sel) {
            g.setColor(sel ? texto() : panel());
            g.fillRect(x + 3, y + 2, w - 6, h - 2);
        }
 
        @Override
        protected void paintTabBorder(Graphics g, int tp, int i, int x, int y, int w, int h, boolean sel) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(borde());
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g2.drawRect(x + 4, y + 3, w - 8, h - 3);
            g2.dispose();
        }
 
        @Override
        protected void paintText(Graphics g, int tp, Font font, FontMetrics fm, int i,
                String titulo, Rectangle r, boolean sel) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            g2.setFont(font);
            g2.setColor(sel ? fondo() : texto());
            g2.drawString(titulo, r.x, r.y + fm.getAscent());
            g2.dispose();
        }
 
        @Override
        protected void paintFocusIndicator(Graphics g, int tp, Rectangle[] rects, int i,
                Rectangle icon, Rectangle text, boolean sel) {
            // sin recuadro de foco
        }
 
        @Override
        protected void paintContentBorder(Graphics g, int tp, int sel) {
            Insets in = tabPane.getInsets();
            int y = in.top + calculateTabAreaHeight(tp, runCount, maxTabHeight) - 2;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(borde());
            g2.fillRect(in.left, y, tabPane.getWidth() - in.left - in.right, 3);
            g2.dispose();
        }
 
        @Override
        protected int getTabLabelShiftX(int tp, int i, boolean sel) {
            return 0;
        }
 
        @Override
        protected int getTabLabelShiftY(int tp, int i, boolean sel) {
            return 0;
        }
    }
 
    /** Barra de desplazamiento cuadrada, sin flechas. */
    private static class BarraFina extends BasicScrollBarUI {
 
        private JButton vacio() {
            JButton b = new JButton();
            Dimension d = new Dimension(0, 0);
            b.setPreferredSize(d);
            b.setMinimumSize(d);
            b.setMaximumSize(d);
            return b;
        }
 
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return vacio();
        }
 
        @Override
        protected JButton createIncreaseButton(int orientation) {
            return vacio();
        }
 
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(fondo());
            g.fillRect(r.x, r.y, r.width, r.height);
        }
 
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            g.setColor(borde());
            g.fillRect(r.x + 2, r.y + 1, r.width - 4, r.height - 2);
        }
    }
}