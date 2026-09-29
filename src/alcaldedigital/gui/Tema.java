package alcaldedigital.gui;
 
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
 
/**
 * Componente inclusivo que centraliza la paleta de colores y tipografías del sistema,
 * permitiendo alternar en caliente a un modo de alto contraste para atender
 * requerimientos de accesibilidad (baja visión y daltonismo).
 *
 * Garantiza que la información nunca dependa exclusivamente del color,
 * combinando altos índices de contraste, patrones visuales y símbolos numéricos.
 *
 * Además incluye componentes visuales reutilizables: Tarjeta, Boton, PestanasUI
 * y barras de desplazamiento delgadas.
 *
 * @author Naty
 */
public final class Tema {
 
    private static boolean altoContraste = false;
 
    /** Familias tipográficas: se usa la primera que exista en el sistema. */
    private static final String FAMILIA = elegirFuente(
            "Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Ubuntu", "Roboto", "SansSerif");
    private static final String FAMILIA_MONO = elegirFuente(
            "JetBrains Mono", "Cascadia Mono", "Consolas", "Menlo", "DejaVu Sans Mono", "Monospaced");
 
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
    // Colores base
    // ------------------------------------------------------------------
 
    public static Color fondo() {
        return altoContraste ? Color.BLACK : new Color(15, 19, 30);
    }
 
    public static Color panel() {
        return altoContraste ? Color.BLACK : new Color(25, 31, 46);
    }
 
    public static Color texto() {
        return altoContraste ? Color.WHITE : new Color(236, 240, 250);
    }
 
    public static Color textoSuave() {
        return altoContraste ? Color.WHITE : new Color(150, 162, 188);
    }
 
    public static Color borde() {
        return altoContraste ? Color.WHITE : new Color(50, 60, 84);
    }
 
    // ------------------------------------------------------------------
    // Colores funcionales
    // ------------------------------------------------------------------
 
    public static Color acento() {
        return altoContraste ? new Color(255, 255, 0) : new Color(96, 165, 250);
    }
 
    public static Color positivo() {
        return altoContraste ? new Color(0, 255, 128) : new Color(74, 208, 142);
    }
 
    public static Color negativo() {
        return altoContraste ? new Color(255, 128, 0) : new Color(244, 100, 112);
    }
 
    public static Color nodo() {
        return altoContraste ? Color.BLACK : new Color(40, 49, 70);
    }
 
    // ------------------------------------------------------------------
    // Tipografías
    // ------------------------------------------------------------------
 
    public static Font titulo() {
        return new Font(FAMILIA, Font.BOLD, altoContraste ? 24 : 22);
    }
 
    public static Font subtitulo() {
        return new Font(FAMILIA, Font.BOLD, altoContraste ? 17 : 15);
    }
 
    public static Font normal() {
        return new Font(FAMILIA, Font.PLAIN, altoContraste ? 16 : 14);
    }
 
    /** Fuente para la frase principal de la publicación (grande y legible). */
    public static Font destacado() {
        return new Font(FAMILIA, Font.PLAIN, altoContraste ? 24 : 22);
    }
 
    /** Fuente para las etiquetas de indicadores. */
    public static Font indicador() {
        return new Font(FAMILIA, Font.PLAIN, altoContraste ? 15 : 13);
    }
 
    public static Font indicadorNegrita() {
        return new Font(FAMILIA, Font.BOLD, altoContraste ? 15 : 13);
    }
 
    public static Font mono() {
        return new Font(FAMILIA_MONO, Font.PLAIN, altoContraste ? 15 : 13);
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
 
    /** Aplica el tema a los cuadros de diálogo (JOptionPane) y otros componentes globales. */
    public static void instalarUI() {
        UIManager.put("Panel.background", fondo());
        UIManager.put("ToolTip.background", panel());
        UIManager.put("ToolTip.foreground", texto());
        UIManager.put("ToolTip.font", normal());
    }
 
    /**
     * Muestra un cuadro de diálogo modal con el estilo del juego (reemplaza a JOptionPane).
     */
    public static void mostrarDialogo(java.awt.Component padre, String titulo, String mensaje) {
        java.awt.Window dueno = padre instanceof java.awt.Window
                ? (java.awt.Window) padre : javax.swing.SwingUtilities.getWindowAncestor(padre);
        final javax.swing.JDialog d = new javax.swing.JDialog(dueno, titulo,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
 
        javax.swing.JTextArea area = new javax.swing.JTextArea(mensaje);
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
        ok.setPreferredSize(new Dimension(150, 42));
        ok.addActionListener(e -> d.dispose());
 
        JPanel abajo = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        abajo.setOpaque(false);
        abajo.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        abajo.add(ok);
 
        JPanel raiz = new JPanel(new java.awt.BorderLayout());
        raiz.setBackground(fondo());
        raiz.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        raiz.add(sp, java.awt.BorderLayout.CENTER);
        raiz.add(abajo, java.awt.BorderLayout.SOUTH);
 
        d.setContentPane(raiz);
        d.getRootPane().setDefaultButton(ok);
        d.pack();
        d.setResizable(false);
        d.setLocationRelativeTo(padre);
        d.setVisible(true);
    }
 
    /** Aplica barras de desplazamiento delgadas y planas a un JScrollPane. */
    public static void estilizarScroll(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new BarraFina());
        sp.getHorizontalScrollBar().setUI(new BarraFina());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getHorizontalScrollBar().setUnitIncrement(16);
    }
 
    // ------------------------------------------------------------------
    // Componentes visuales
    // ------------------------------------------------------------------
 
    /** Panel con esquinas redondeadas, relleno y borde según el tema activo. */
    public static class Tarjeta extends JPanel {
 
        private final int radio;
 
        public Tarjeta(LayoutManager layout, int radio) {
            super(layout);
            this.radio = radio;
            setOpaque(false);
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            g2.setColor(panel());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            g2.setColor(borde());
            g2.setStroke(new java.awt.BasicStroke(altoContraste ? 2f : 1.2f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
    /** Botón redondeado con efecto al pasar el mouse y color de acento propio. */
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
            int w = getWidth() - 1;
            int h = getHeight() - 1;
 
            g2.setColor(activo && encima ? conAlfa(ac, altoContraste ? 255 : 55) : panel());
            g2.fillRoundRect(0, 0, w, h, 16, 16);
            g2.setColor(activo ? ac : borde());
            g2.setStroke(new java.awt.BasicStroke(altoContraste ? 2.5f : 1.6f));
            g2.drawRoundRect(0, 0, w, h, 16, 16);
 
            g2.setFont(subtitulo());
            FontMetrics fm = g2.getFontMetrics();
            String t = getText();
            boolean invertir = activo && encima && altoContraste;
            g2.setColor(invertir ? Color.BLACK : (activo ? texto() : textoSuave()));
            g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
 
    /** Diseño plano de pestañas: texto simple con subrayado de acento en la activa. */
    public static class PestanasUI extends BasicTabbedPaneUI {
 
        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(10, 22, 10, 22);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(4, 12, 0, 12);
        }
 
        @Override
        protected void paintTabBackground(Graphics g, int tp, int i, int x, int y, int w, int h, boolean sel) {
            // sin fondo: las pestañas son planas
        }
 
        @Override
        protected void paintTabBorder(Graphics g, int tp, int i, int x, int y, int w, int h, boolean sel) {
            if (sel) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(acento());
                g2.fillRoundRect(x + 10, y + h - 3, w - 20, 3, 3, 3);
                g2.dispose();
            }
        }
 
        @Override
        protected void paintText(Graphics g, int tp, Font font, FontMetrics fm, int i,
                String titulo, Rectangle r, boolean sel) {
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            g2.setFont(font);
            g2.setColor(sel ? texto() : textoSuave());
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
            int y = in.top + calculateTabAreaHeight(tp, runCount, maxTabHeight) - 1;
            g.setColor(borde());
            g.drawLine(in.left, y, tabPane.getWidth() - in.right, y);
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
 
    /** Barra de desplazamiento delgada, sin flechas. */
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
            g.setColor(panel());
            g.fillRect(r.x, r.y, r.width, r.height);
        }
 
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            suavizar(g2);
            g2.setColor(altoContraste ? Color.WHITE : new Color(75, 88, 118));
            g2.fillRoundRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 8, 8);
            g2.dispose();
        }
    }
}