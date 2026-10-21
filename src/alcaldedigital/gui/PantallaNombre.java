package alcaldedigital.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Pantalla que aparece después de START: el jugador escribe su nombre y
 * pasa directo al mapa de niveles. (Ya no hay varios jugadores ni roles.)
 */
public class PantallaNombre extends JFrame {

    private static final String RUTA = "/alcaldedigital/recursos/";
    private static final Color TEXTO = new Color(0x4A, 0x2A, 0x20);
    private static final String FUENTE = "Georgia";

    public PantallaNombre(Consumer<String> alAceptar) {
        setTitle("Alcalde Digital");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Pagina pagina = new Pagina(nombre -> {
            dispose();
            alAceptar.accept(nombre);
        });
        pagina.setPreferredSize(new Dimension(1000, 562));
        setContentPane(pagina);
        pack();
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(pagina::enfocar);
    }

    private class Pagina extends PanelConFondo {

        private final JLabel lblBienvenido = etiqueta("Bienvenido");
        private final JLabel lblPregunta = etiqueta("¿Cómo te llamas?");
        private final JTextField txtNombre = new JTextField();
        private final JLabel lblAyuda = etiqueta("Así te verán los ciudadanos de Ciudad Nova");
        private final BotonImagen acep = new BotonImagen(RUTA + "acep.png", "Aceptar");
        private final BotonImagen ajuste = new BotonImagen(RUTA + "ajuste.png", "Ajustes");
        private final JButton salir = new JButton();
        private final JPanel tarjeta = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 170));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.setColor(new Color(0x8A, 0x5A, 0x3C, 200));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
                g2.dispose();
            }
        };

        Pagina(Consumer<String> alAceptar) {
            super(null, RUTA + "f1.jpg");
            tarjeta.setOpaque(false);
            txtNombre.setHorizontalAlignment(SwingConstants.CENTER);
            txtNombre.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x8A, 0x5A, 0x3C), 2),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)));
            txtNombre.setBackground(new Color(0xFF, 0xF8, 0xEE));
            txtNombre.setForeground(TEXTO);
            tarjeta.add(lblPregunta);
            tarjeta.add(txtNombre);
            tarjeta.add(lblAyuda);
            add(lblBienvenido);
            add(tarjeta);
            add(acep);

            Runnable aceptar = () -> {
                String nombre = txtNombre.getText().trim();
                alAceptar.accept(nombre.isEmpty() ? "Ciudadano" : nombre);
            };
            acep.addActionListener(e -> aceptar.run());
            txtNombre.addActionListener(e -> aceptar.run());   // Enter también acepta

            ajuste.setToolTipText("Ajustes");
            ajuste.addActionListener(e -> new DialogoAjustes(PantallaNombre.this).setVisible(true));
            add(ajuste);

            // el botón "Exit" viene dibujado en f1.jpg: encima va un botón invisible
            salir.setOpaque(false);
            salir.setContentAreaFilled(false);
            salir.setBorderPainted(false);
            salir.setFocusPainted(false);
            salir.setToolTipText("Salir");
            salir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            salir.addActionListener(e -> System.exit(0));
            add(salir);

            Configuracion.agregarOyente(this::repaint);
        }

        void enfocar() {
            txtNombre.requestFocusInWindow();
        }

        private JLabel etiqueta(String texto) {
            JLabel l = new JLabel(texto, SwingConstants.CENTER);
            l.setForeground(TEXTO);
            return l;
        }

        private Font fuente(int estilo, double tamano, int h) {
            return new Font(FUENTE, estilo, (int) Math.max(10, tamano * h / 562.0));
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Configuracion.pintarBrillo(g, getWidth(), getHeight());
        }

        @Override
        public void doLayout() {
            int w = getWidth(), h = getHeight();
            salir.setBounds((int) (w * 0.865), (int) (h * 0.875), (int) (w * 0.12), (int) (h * 0.085));
            ajuste.centrarEn((int) (w * 0.935), (int) (h * 0.095), (int) (w * 0.075));

            lblBienvenido.setFont(fuente(Font.BOLD | Font.ITALIC, 58, h));
            lblBienvenido.setBounds(0, (int) (h * 0.08), w, (int) (h * 0.17));

            int tw = (int) (w * 0.42), th = (int) (h * 0.34);
            tarjeta.setBounds((w - tw) / 2, (int) (h * 0.30), tw, th);
            lblPregunta.setFont(fuente(Font.BOLD | Font.ITALIC, 28, h));
            txtNombre.setFont(fuente(Font.PLAIN, 20, h));
            lblAyuda.setFont(fuente(Font.ITALIC, 14, h));
            int m = (int) (tw * 0.1);
            lblPregunta.setBounds(0, (int) (th * 0.08), tw, (int) (th * 0.28));
            txtNombre.setBounds(m, (int) (th * 0.42), tw - 2 * m, (int) (th * 0.22));
            lblAyuda.setBounds(0, (int) (th * 0.70), tw, (int) (th * 0.2));

            acep.centrarEn(w / 2, (int) (h * 0.76), (int) (w * 0.22));
        }
    }
}
