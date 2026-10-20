package alcaldedigital.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

/**
 * Base visual de todas las ventanas emergentes del juego (instrucciones,
 * ayuda, resultado de cada situación y resultados finales).
 *
 * Usa el mismo estilo que {@link DialogoAjustes}: fondo café con degradado,
 * borde dorado redondeado, título en Georgia itálica y botones de imagen.
 * Así ninguna ventana se ve como un JOptionPane del sistema.
 *
 * Uso: crear la subclase, llamar a {@link #setContenido(JComponent)},
 * agregar botones con {@link #agregarBoton(JButton)} y al final {@link #mostrar()}.
 */
public class DialogoJuego extends JDialog {

    // ---- Paleta compartida (misma que DialogoAjustes) ----
    public static final Color ORO = new Color(0xE9, 0xC4, 0x7A);
    public static final Color CREMA = new Color(0xF3, 0xE3, 0xC3);
    public static final Color CREMA_SUAVE = new Color(0xD9, 0xC3, 0x9E);
    public static final Color CAFE_CLARO = new Color(0x4A, 0x2C, 0x1E);
    public static final Color CAFE_OSCURO = new Color(0x26, 0x15, 0x0E);
    public static final Color VERDE = new Color(0x8F, 0xD6, 0x9A);
    public static final Color ROJO = new Color(0xF0, 0x8A, 0x80);
    public static final String FUENTE = "Georgia";
    public static final String RUTA = "/alcaldedigital/recursos/";

    private static final int RADIO = 28;

    private final JPanel fondo;
    private final JPanel pie = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));

    public DialogoJuego(Window padre, String titulo) {
        super(padre, titulo, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        fondo = new JPanel(new BorderLayout(0, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, CAFE_CLARO, 0, getHeight(), CAFE_OSCURO));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIO, RADIO);
                g2.setColor(ORO);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, RADIO, RADIO);
                // filete interior fino, para darle un acabado "de marco"
                g2.setColor(conAlfa(ORO, 70));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(8, 8, getWidth() - 17, getHeight() - 17, RADIO - 8, RADIO - 8);
                g2.dispose();
            }

            /** El brillo de Ajustes también oscurece estas ventanas. */
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                Configuracion.pintarBrillo(g, getWidth(), getHeight());
            }
        };
        fondo.setOpaque(false);
        fondo.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setFont(new Font(FUENTE, Font.BOLD | Font.ITALIC, 30));
        lblTitulo.setForeground(ORO);

        JPanel cabecera = new JPanel(new BorderLayout(0, 6));
        cabecera.setOpaque(false);
        cabecera.add(lblTitulo, BorderLayout.CENTER);
        cabecera.add(new Separador(), BorderLayout.SOUTH);
        fondo.add(cabecera, BorderLayout.NORTH);

        pie.setOpaque(false);
        fondo.add(pie, BorderLayout.SOUTH);

        setContentPane(fondo);
        setBackground(new Color(0, 0, 0, 0));   // esquinas redondeadas transparentes
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Coloca el contenido principal del diálogo (entre el título y los botones). */
    protected void setContenido(JComponent contenido) {
        fondo.add(contenido, BorderLayout.CENTER);
    }

    /** Agrega un botón al pie del diálogo. */
    protected void agregarBoton(JButton boton) {
        pie.add(boton);
    }

    /** Botón "Aceptar" con la imagen acep.png; cierra el diálogo. Enter también lo pulsa. */
    protected BotonImagen botonAceptar() {
        BotonImagen aceptar = new BotonImagen(RUTA + "acep.png", "Aceptar");
        aceptar.setPreferredSize(new Dimension(180, aceptar.altoParaAncho(180)));
        aceptar.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(aceptar);
        return aceptar;
    }

    /** Ajusta el tamaño al contenido, centra sobre la ventana padre y muestra (bloquea hasta cerrar). */
    public void mostrar() {
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    // ------------------------------------------------------------------
    // Utilidades de estilo reutilizables por las subclases
    // ------------------------------------------------------------------

    public static Font fuente(int estilo, int tamano) {
        return new Font(FUENTE, estilo, tamano);
    }

    public static JLabel etiqueta(String texto, int estilo, int tamano, Color color) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente(estilo, tamano));
        l.setForeground(color);
        return l;
    }

    /** Etiqueta con salto de línea automático al ancho indicado (en píxeles). */
    public static JLabel parrafo(String texto, int ancho, int tamano, Color color) {
        JLabel l = new JLabel("<html><div style='width:" + ancho + "px'>" + html(texto) + "</div></html>");
        l.setFont(fuente(Font.PLAIN, tamano));
        l.setForeground(color);
        return l;
    }

    /** Escapa caracteres especiales para poder poner texto libre dentro de HTML. */
    public static String html(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    /** Espacio vertical vacío (para separar bloques dentro de un BoxLayout). */
    public static JComponent espacio(int alto) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(1, alto));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
        return p;
    }

    public static Color conAlfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alfa);
    }

    /** Carga un PNG de recursos y lo escala con buena calidad a un ícono cuadrado. */
    public static ImageIcon icono(String archivo, int lado) {
        BufferedImage img = BotonImagen.cargar(RUTA + archivo);
        if (img == null) {
            return null;
        }
        img = BotonImagen.recortarTransparencia(img);   // quita el margen vacío del PNG
        BufferedImage out = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img.getScaledInstance(lado, lado, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return new ImageIcon(out);
    }

    // ------------------------------------------------------------------
    // Componentes pequeños
    // ------------------------------------------------------------------

    /** Línea dorada decorativa bajo el título, con un rombo en el centro. */
    static class Separador extends JComponent {

        Separador() {
            setPreferredSize(new Dimension(10, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            int largo = Math.min(getWidth() / 2 - 20, 170);
            g2.setPaint(new GradientPaint(cx - largo, cy, conAlfa(ORO, 0), cx - 10, cy, ORO));
            g2.fillRect(cx - largo, cy, largo - 10, 1);
            g2.setPaint(new GradientPaint(cx + 10, cy, ORO, cx + largo, cy, conAlfa(ORO, 0)));
            g2.fillRect(cx + 10, cy, largo - 10, 1);
            g2.setColor(ORO);
            g2.fillPolygon(new int[]{cx, cx + 5, cx, cx - 5}, new int[]{cy - 5, cy, cy + 5, cy}, 4);
            g2.dispose();
        }
    }

    /** Tarjeta semitransparente con borde dorado suave, para agrupar contenido. */
    public static class Tarjeta extends JPanel {

        private final Color tinte;

        public Tarjeta(java.awt.LayoutManager layout) {
            this(layout, null);
        }

        /** @param tinte color de acento del borde (null = dorado). */
        public Tarjeta(java.awt.LayoutManager layout, Color tinte) {
            super(layout);
            this.tinte = tinte;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(conAlfa(tinte != null ? tinte : ORO, tinte != null ? 170 : 90));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Botón dibujado a mano con el estilo dorado del juego (para textos sin imagen). */
    public static class BotonDorado extends JButton {

        private final boolean principal;

        /** @param principal true = relleno dorado; false = solo contorno. */
        public BotonDorado(String texto, boolean principal) {
            super(texto);
            this.principal = principal;
            setFont(fuente(Font.BOLD, 16));
            setForeground(principal ? CAFE_OSCURO : CREMA);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            FontMetrics fm = getFontMetrics(getFont());
            setPreferredSize(new Dimension(Math.max(170, fm.stringWidth(texto) + 48), 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean encima = getModel().isRollover();
            boolean presionado = getModel().isPressed();
            if (principal) {
                Color arriba = encima ? new Color(0xF6, 0xDA, 0x9A) : ORO;
                Color abajo = presionado ? new Color(0xB8, 0x8E, 0x48) : new Color(0xC9, 0x9C, 0x55);
                g2.setPaint(new GradientPaint(0, 0, arriba, 0, h, abajo));
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            } else {
                g2.setColor(encima ? conAlfa(ORO, 45) : new Color(0, 0, 0, 60));
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            }
            g2.setColor(ORO);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, h - 2, h - 2);

            g2.setFont(getFont());
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }
}
