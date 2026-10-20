package alcaldedigital.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Pantalla de portada: muestra la imagen de portada (JPG) como fondo y el
 * botón START (PNG con transparencia) encima. Al pulsarlo se ejecuta la
 * acción recibida (por ejemplo, abrir el registro de jugadores).
 */
public class PantallaInicio extends JFrame {

    // Rutas dentro del paquete (carpeta src/alcaldedigital/recursos/)
    private static final String RUTA_PORTADA = "/alcaldedigital/recursos/inicio.jpg";
    private static final String RUTA_START = "/alcaldedigital/recursos/start.png";

    // Posición del centro del botón respecto a la portada (0.0 a 1.0).
    // Ajusta estos dos números para mover el botón.
    private static final double POS_X = 0.50;
    private static final double POS_Y = 0.83;

    // Ancho del botón como fracción del ancho de la portada.
    private static final double ANCHO_BOTON = 0.20;

    private final BufferedImage portada;
    private final BufferedImage imgStart;
    private final JButton btnStart = new JButton();

    public PantallaInicio(Runnable alPulsarStart) {
        portada = cargar(RUTA_PORTADA);
        imgStart = cargar(RUTA_START);

        setTitle("Alcalde Digital");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Botón transparente: solo se ve la imagen PNG
        btnStart.setBorderPainted(false);
        btnStart.setContentAreaFilled(false);
        btnStart.setFocusPainted(false);
        btnStart.setOpaque(false);
        btnStart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (imgStart == null) {
            btnStart.setText("START");
        }
        btnStart.addActionListener(e -> {
            dispose();
            alPulsarStart.run();
        });

        PanelPortada panel = new PanelPortada();
        Configuracion.agregarOyente(panel::repaint);
        panel.setLayout(null);   // posicionamos el botón a mano, encima de la imagen
        panel.add(btnStart);

        int w = portada != null ? Math.min(portada.getWidth(), 1000) : 900;
        int h = portada != null ? portada.getHeight() * w / portada.getWidth() : 600;
        panel.setPreferredSize(new Dimension(w, h));

        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    private static BufferedImage cargar(String ruta) {
        try {
            return ImageIO.read(PantallaInicio.class.getResource(ruta));
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("No se pudo cargar la imagen: " + ruta);
            return null;
        }
    }

    /** Panel que dibuja la portada escalada y reubica el botón al redimensionar. */
    private class PanelPortada extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (portada != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(portada, 0, 0, getWidth(), getHeight(), null);
                g2.dispose();
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Configuracion.pintarBrillo(g, getWidth(), getHeight());
        }

        @Override
        public void doLayout() {
            int bw = (int) (getWidth() * ANCHO_BOTON);
            int bh = imgStart != null ? bw * imgStart.getHeight() / imgStart.getWidth() : 50;
            if (imgStart != null) {
                btnStart.setIcon(new ImageIcon(
                        imgStart.getScaledInstance(bw, bh, Image.SCALE_SMOOTH)));
            }
            btnStart.setBounds((int) (getWidth() * POS_X) - bw / 2,
                    (int) (getHeight() * POS_Y) - bh / 2, bw, bh);
        }
    }
}
