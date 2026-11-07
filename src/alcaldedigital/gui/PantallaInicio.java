package alcaldedigital.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * Pantalla de portada.
 *
 * Todo el texto se DIBUJA; no viene quemado en la imagen. Antes el nombre del
 * juego era parte del JPG, asi que cambiarle una coma obligaba a regenerar la
 * ilustracion, y por eso llegaron a convivir dos nombres distintos en la misma
 * pantalla.
 *
 * Decisiones de diseno, por si alguien las revisa:
 *
 *  - Jerarquia de tres niveles: el nombre oficial arriba en pequeno, el nombre
 *    de fantasia grande, y la frase que explica de que va el juego. El orden de
 *    lectura deja claro cual es cual sin que compitan.
 *  - Los colores salen de la propia ilustracion (el dorado del sol, el marfil
 *    de las nubes, el pardo de la piedra). Nada inventado.
 *  - Las mayusculas van con espaciado entre letras. Es lo que separa un titulo
 *    compuesto de un texto puesto ahi con la fuente que habia.
 *  - Los botones son de contorno y no bloques opacos: sobre una ilustracion,
 *    tres rectangulos rellenos tapan justo lo que se quiere mostrar.
 *  - Enter entra y Escape sale. En una feria el mouse falla o lo manosean diez
 *    personas; el teclado siempre responde.
 */
public class PantallaInicio extends JFrame {

    private static final String RUTA_PORTADA = "/alcaldedigital/recursos/portada.jpg";
    /** Portada anterior, por si la nueva no esta. */
    private static final String RUTA_RESPALDO = "/alcaldedigital/recursos/inicio.jpg";

    private static final String NOMBRE_OFICIAL = "ALCALDE DIGITAL";
    private static final String TITULO = "RUMOR MILL";
    private static final String SUBTITULO =
            "Cuatro decisiones. Ocho vecinos. Un rumor que ya empezó a correr.";
    private static final String CREDITOS =
            "Universidad del Norte  ·  Estructura de Datos II  ·  2026-2";

    // ------------------------------------------------------------------
    // Paleta, tomada de la ilustracion
    // ------------------------------------------------------------------
    /** El dorado del sol sobre el rio. */
    private static final Color ORO = new Color(222, 173, 100);
    /** Marfil de las nubes altas. */
    private static final Color MARFIL = new Color(247, 240, 227);
    /** Pardo claro de la piedra de la terraza. */
    private static final Color PIEDRA = new Color(206, 190, 166);
    /** Sombra: el pardo oscuro de las farolas. */
    private static final Color SOMBRA = new Color(26, 17, 10);

    /**
     * Familia del titulo. Se busca una con mas caracter que la del cuerpo del
     * juego; si no hay ninguna, cae en la misma serif y no pasa nada.
     */
    private static final String FAMILIA_TITULO = elegirFuente(
            "Constantia", "Palatino Linotype", "Book Antiqua", "Garamond",
            "Georgia", "Times New Roman", "Serif");

    private final BufferedImage portada;

    /**
     * @param siguiente fabrica de la pantalla que sigue. Se recibe como
     *                  fabrica y no como accion porque la transicion necesita
     *                  CREAR la ventana nueva mientras la pantalla esta en
     *                  negro, y mostrarla antes de cerrar esta.
     */
    public PantallaInicio(java.util.function.Supplier<JFrame> siguiente) {
        BufferedImage img = cargar(RUTA_PORTADA);
        portada = img != null ? img : cargar(RUTA_RESPALDO);

        setTitle(NOMBRE_OFICIAL);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BotonPortada btnJugar = new BotonPortada("JUGAR", true);
        btnJugar.addActionListener(e -> Transicion.cambiar(this, siguiente));

        BotonPortada btnComo = new BotonPortada("CÓMO SE JUEGA", false);
        btnComo.addActionListener(e -> DialogoInstrucciones.ayuda(this));

        BotonPortada btnSalir = new BotonPortada("SALIR", false);
        btnSalir.addActionListener(e -> System.exit(0));

        JPanel botones = new JPanel();
        botones.setOpaque(false);
        botones.setLayout(new BoxLayout(botones, BoxLayout.Y_AXIS));
        botones.add(btnJugar);
        botones.add(Box.createVerticalStrut(14));
        botones.add(btnComo);
        botones.add(Box.createVerticalStrut(10));
        botones.add(btnSalir);

        JPanel abajo = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        abajo.setOpaque(false);
        abajo.add(botones);

        PanelPortada panel = new PanelPortada();
        Configuracion.agregarOyente(panel::repaint);
        panel.setLayout(new BorderLayout());
        panel.add(abajo, BorderLayout.SOUTH);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 54, 0));

        int w = portada != null ? Math.min(portada.getWidth(), 1040) : 960;
        int h = portada != null ? portada.getHeight() * w / portada.getWidth() : 620;
        panel.setPreferredSize(new Dimension(w, Math.max(h, 640)));

        setContentPane(panel);

        // Enter entra, Escape sale. Un stand de feria no siempre tiene mouse sano.
        getRootPane().setDefaultButton(btnJugar);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "salir");
        getRootPane().getActionMap().put("salir", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                System.exit(0);
            }
        });

        pack();
        setLocationRelativeTo(null);
        btnJugar.requestFocusInWindow();
    }

    private static String elegirFuente(String... candidatas) {
        Set<String> hay = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : candidatas) {
            if (hay.contains(f)) {
                return f;
            }
        }
        return candidatas[candidatas.length - 1];
    }

    private static BufferedImage cargar(String ruta) {
        try {
            return ImageIO.read(PantallaInicio.class.getResource(ruta));
        } catch (IOException | IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Texto con espaciado entre letras
    // ------------------------------------------------------------------

    /**
     * Ancho de un texto dibujado con espaciado extra entre letras.
     * Java no tiene "tracking" como un programa de diseno, asi que hay que
     * dibujar letra por letra y calcular el ancho a mano.
     */
    private static int anchoEspaciado(FontMetrics fm, String s, double extra) {
        if (s.isEmpty()) {
            return 0;
        }
        int ancho = 0;
        for (int i = 0; i < s.length(); i++) {
            ancho += fm.charWidth(s.charAt(i));
        }
        return ancho + (int) Math.round(extra * (s.length() - 1));
    }

    /** Dibuja el texto centrado en cx, con espaciado entre letras. */
    private static void espaciado(Graphics2D g2, String s, int cx, int base, double extra) {
        FontMetrics fm = g2.getFontMetrics();
        double x = cx - anchoEspaciado(fm, s, extra) / 2.0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            g2.drawString(String.valueOf(c), (float) x, base);
            x += fm.charWidth(c) + extra;
        }
    }

    /** Lo mismo, pero con una sombra suave debajo para que se lea sobre la foto. */
    private static void espaciadoConSombra(Graphics2D g2, String s, int cx, int base,
                                           double extra, Color color, int desplazamiento) {
        Color antes = g2.getColor();
        g2.setColor(new Color(SOMBRA.getRed(), SOMBRA.getGreen(), SOMBRA.getBlue(), 120));
        espaciado(g2, s, cx + desplazamiento, base + desplazamiento, extra);
        g2.setColor(color);
        espaciado(g2, s, cx, base, extra);
        g2.setColor(antes);
    }

    // ------------------------------------------------------------------
    // Boton de portada
    // ------------------------------------------------------------------

    /**
     * Boton de contorno, pensado para ir encima de una ilustracion.
     *
     * Los botones del juego (Tema.Boton) son bloques opacos con sombra dura:
     * funcionan sobre papel, pero sobre una foto son tres parches que tapan
     * justo lo que se quiere lucir. Este deja ver el fondo y solo marca el
     * contorno; el principal si va relleno, porque tiene que ganar la mirada.
     */
    private class BotonPortada extends JButton {

        private final boolean principal;
        private boolean encima;

        BotonPortada(String texto, boolean principal) {
            super(texto);
            this.principal = principal;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(CENTER_ALIGNMENT);

            Dimension d = new Dimension(262, principal ? 56 : 46);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);

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
            Tema.suavizar(g2);

            int w = getWidth();
            int h = getHeight();
            boolean activo = encima || isFocusOwner();
            boolean hundido = getModel().isPressed();
            int dy = hundido ? 1 : 0;

            // Relleno
            if (principal) {
                g2.setColor(activo ? new Color(238, 196, 128) : ORO);
                g2.fillRect(0, dy, w, h - 1);
            } else {
                // Velo oscuro tenue: sin el, el texto depende de que debajo
                // haya piedra clara y no cielo.
                g2.setColor(new Color(26, 17, 10, activo ? 150 : 95));
                g2.fillRect(0, dy, w, h - 1);
            }

            // Contorno
            g2.setColor(principal ? new Color(150, 106, 46)
                                  : new Color(ORO.getRed(), ORO.getGreen(), ORO.getBlue(),
                                              activo ? 255 : 170));
            g2.setStroke(new BasicStroke(activo && !principal ? 2.2f : 1.6f));
            g2.drawRect(1, dy + 1, w - 3, h - 4);

            // Texto
            int cuerpo = principal ? 17 : 14;
            g2.setFont(new Font(FAMILIA_TITULO, Font.BOLD, cuerpo));
            FontMetrics fm = g2.getFontMetrics();
            int base = dy + (h - fm.getHeight()) / 2 + fm.getAscent() - 1;
            g2.setColor(principal ? new Color(42, 26, 12)
                                  : (activo ? MARFIL : PIEDRA));
            espaciado(g2, getText(), w / 2, base, 2.2);

            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // El lienzo
    // ------------------------------------------------------------------

    private class PanelPortada extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizar(g2);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();

            if (portada != null) {
                // CUBRIR: llena la ventana sin deformar la ilustracion.
                double escala = Math.max(w / (double) portada.getWidth(),
                                         h / (double) portada.getHeight());
                int dw = (int) Math.round(portada.getWidth() * escala);
                int dh = (int) Math.round(portada.getHeight() * escala);
                g2.drawImage(portada, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            } else {
                g2.setColor(Tema.fondo());
                g2.fillRect(0, 0, w, h);
            }

            velos(g2, w, h);

            // --- Bloque de titulo -----------------------------------------
            int cuerpo = Math.max(32, Math.min(70, w / 12));
            int yTitulo = (int) (h * 0.33);

            // Nombre oficial, arriba y pequeno: se lee primero, no compite.
            // (el nombre oficial se dibuja mas abajo, junto a sus filetes)

            // Dos filetes FLANQUEANDO el nombre oficial. Debajo no caben: ahi
            // empiezan las mayusculas del titulo y lo tapaban.
            int yOjo = yTitulo - (int) (cuerpo * 0.80);
            g2.setFont(new Font(FAMILIA_TITULO, Font.BOLD, Math.max(12, cuerpo / 5)));
            int medio = anchoEspaciado(g2.getFontMetrics(), NOMBRE_OFICIAL,
                    Math.max(4.0, cuerpo * 0.13)) / 2;
            int largo = (int) (cuerpo * 0.75);
            int hueco = 16;
            g2.setColor(new Color(ORO.getRed(), ORO.getGreen(), ORO.getBlue(), 135));
            g2.setStroke(new BasicStroke(1.3f));
            int yf = yOjo - Math.max(3, cuerpo / 14);
            g2.drawLine(w / 2 - medio - hueco - largo, yf, w / 2 - medio - hueco, yf);
            g2.drawLine(w / 2 + medio + hueco, yf, w / 2 + medio + hueco + largo, yf);

            espaciadoConSombra(g2, NOMBRE_OFICIAL, w / 2, yOjo,
                    Math.max(4.0, cuerpo * 0.13), ORO, 2);

            // Titulo grande
            g2.setFont(new Font(FAMILIA_TITULO, Font.BOLD, cuerpo));
            espaciadoConSombra(g2, TITULO, w / 2, yTitulo, cuerpo * 0.10, MARFIL, 3);

            // Subtitulo
            g2.setFont(new Font(FAMILIA_TITULO, Font.PLAIN, Math.max(14, (int) (cuerpo * 0.27))));
            espaciadoConSombra(g2, SUBTITULO, w / 2, yTitulo + (int) (cuerpo * 0.80),
                    0.9, PIEDRA, 2);

            // --- Creditos --------------------------------------------------
            g2.setFont(new Font(FAMILIA_TITULO, Font.PLAIN, 12));
            FontMetrics fm = g2.getFontMetrics();
            int anchoCred = anchoEspaciado(fm, CREDITOS, 1.1);
            int xCred = w - anchoCred - 22;
            g2.setColor(new Color(SOMBRA.getRed(), SOMBRA.getGreen(), SOMBRA.getBlue(), 110));
            espaciado(g2, CREDITOS, xCred + anchoCred / 2 + 1, h - 15, 1.1);
            g2.setColor(new Color(PIEDRA.getRed(), PIEDRA.getGreen(), PIEDRA.getBlue(), 180));
            espaciado(g2, CREDITOS, xCred + anchoCred / 2, h - 16, 1.1);

            g2.dispose();
        }

        /**
         * Velos de legibilidad.
         *
         * No son decoracion: el titulo cae sobre cielo y los botones sobre
         * piedra, y sin ellos que se lea o no depende de que nadie cambie la
         * ilustracion. El halo redondo detras del titulo apaga ademas el
         * resplandor del sol, que queda justo detras de las ultimas letras.
         */
        private void velos(Graphics2D g2, int w, int h) {
            int corte = (int) (h * 0.46f);
            g2.setPaint(new GradientPaint(0, 0, new Color(24, 16, 10, 92),
                                          0, corte, new Color(24, 16, 10, 0)));
            g2.fillRect(0, 0, w, corte);

            g2.setPaint(new GradientPaint(0, (int) (h * 0.58f), new Color(24, 16, 10, 0),
                                          0, h, new Color(24, 16, 10, 140)));
            g2.fillRect(0, (int) (h * 0.58f), w, h - (int) (h * 0.58f));

            Graphics2D halo = (Graphics2D) g2.create();
            halo.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            halo.setPaint(new RadialGradientPaint(
                    new java.awt.geom.Point2D.Float(w / 2f, h * 0.30f),
                    w * 0.46f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(24, 16, 10, 130), new Color(24, 16, 10, 0)}));
            halo.fillRect(0, 0, w, (int) (h * 0.62f));
            halo.dispose();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Configuracion.pintarBrillo(g, getWidth(), getHeight());
        }
    }
}
