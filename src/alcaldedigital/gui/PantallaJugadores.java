package alcaldedigital.gui;

import alcaldedigital.modelo.Jugador;
import alcaldedigital.modelo.Rol;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Ventana de registro de jugadores (reemplaza a los JOptionPane).
 * Tiene dos páginas sobre el fondo f1:
 *   1) "Bienvenido / Jugadores" con los botones j2, j3 y j4.
 *   2) Nombre + rol de cada jugador, con el botón acep (Aceptar).
 * Al terminar, entrega la lista de jugadores a quien la creó.
 */
public class PantallaJugadores extends JFrame {

    private static final String RUTA = "/alcaldedigital/recursos/";
    private static final String FONDO = RUTA + "f1.jpg";

    private static final Color TEXTO = new Color(0x4A, 0x2A, 0x20);   // marrón oscuro
    private static final String FUENTE = "Georgia";

    private final Consumer<List<Jugador>> alTerminar;
    private final List<Jugador> jugadores = new ArrayList<>();
    private final CardLayout cartas = new CardLayout();
    private final JPanel contenedor = new JPanel(cartas);
    private final PaginaRegistro paginaRegistro = new PaginaRegistro();
    private int cantidad;

    public PantallaJugadores(Consumer<List<Jugador>> alTerminar) {
        this.alTerminar = alTerminar;
        setTitle("Alcalde Digital");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        contenedor.add(new PaginaCantidad(), "cantidad");
        contenedor.add(paginaRegistro, "registro");
        contenedor.setPreferredSize(new Dimension(1000, 562));
        setContentPane(contenedor);
        pack();
        setLocationRelativeTo(null);
    }

    private void elegirCantidad(int n) {
        cantidad = n;
        jugadores.clear();
        paginaRegistro.mostrarJugador(0);
        cartas.show(contenedor, "registro");
        SwingUtilities.invokeLater(paginaRegistro::enfocarNombre);
    }

    private void registrar(String nombre, Rol rol, int indice) {
        if (nombre.trim().isEmpty()) {
            nombre = "Jugador " + (indice + 1);
        }
        jugadores.add(new Jugador(nombre.trim(), rol));
        if (indice + 1 < cantidad) {
            paginaRegistro.mostrarJugador(indice + 1);
        } else {
            dispose();
            alTerminar.accept(jugadores);
        }
    }

    // ------------------------------------------------------------------
    // Base común: fondo f1 + zona "Exit" dibujada en la imagen
    // ------------------------------------------------------------------

    private abstract class Pagina extends PanelConFondo {

        private final BotonTransparente salir = new BotonTransparente();
        private final BotonImagen ajuste = new BotonImagen(RUTA + "ajuste.png", "Ajustes");

        Pagina() {
            super(null, FONDO);
            salir.setToolTipText("Salir");
            salir.addActionListener(e -> System.exit(0));
            add(salir);

            // Botón de ajustes (arriba a la derecha)
            ajuste.setToolTipText("Ajustes");
            ajuste.addActionListener(e ->
                    new DialogoAjustes(PantallaJugadores.this).setVisible(true));
            add(ajuste);

            Configuracion.agregarOyente(this::repaint);   // para que el brillo se vea en vivo
        }

        /** El brillo oscurece toda la página, incluidos los botones. */
        @Override
        public void paint(java.awt.Graphics g) {
            super.paint(g);
            Configuracion.pintarBrillo(g, getWidth(), getHeight());
        }

        /** Cada página coloca sus componentes según el tamaño actual. */
        abstract void colocar(int w, int h);

        @Override
        public void doLayout() {
            int w = getWidth(), h = getHeight();
            // El botón "Exit" viene dibujado en f1.jpg (abajo a la derecha)
            salir.setBounds((int) (w * 0.865), (int) (h * 0.875), (int) (w * 0.12), (int) (h * 0.085));
            ajuste.centrarEn((int) (w * 0.935), (int) (h * 0.095), (int) (w * 0.075));
            colocar(w, h);
        }

        Font fuente(int estilo, double tamano, int h) {
            return new Font(FUENTE, estilo, (int) Math.max(10, tamano * h / 562.0));
        }

        JLabel etiqueta(String texto) {
            JLabel l = new JLabel(texto, SwingConstants.CENTER);
            l.setForeground(TEXTO);
            return l;
        }
    }

    /** Botón invisible que se coloca encima del "Exit" de la imagen. */
    private static class BotonTransparente extends javax.swing.JButton {

        BotonTransparente() {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }
    }

    // ------------------------------------------------------------------
    // Página 1: Bienvenido / Jugadores / j2 j3 j4
    // ------------------------------------------------------------------

    private class PaginaCantidad extends Pagina {

        private final JLabel lblBienvenido = etiqueta("Bienvenido");
        private final JLabel lblJugadores = etiqueta("Jugadores");
        private final BotonImagen j2 = new BotonImagen(RUTA + "j2.png", "2 Jugadores");
        private final BotonImagen j3 = new BotonImagen(RUTA + "j3.png", "3 Jugadores");
        private final BotonImagen j4 = new BotonImagen(RUTA + "j4.png", "4 Jugadores");

        PaginaCantidad() {
            add(lblBienvenido);
            add(lblJugadores);
            add(j2);
            add(j3);
            add(j4);
            j2.addActionListener(e -> elegirCantidad(2));
            j3.addActionListener(e -> elegirCantidad(3));
            j4.addActionListener(e -> elegirCantidad(4));
        }

        @Override
        void colocar(int w, int h) {
            lblBienvenido.setFont(fuente(Font.BOLD | Font.ITALIC, 58, h));
            lblJugadores.setFont(fuente(Font.ITALIC, 34, h));
            lblBienvenido.setBounds(0, (int) (h * 0.08), w, (int) (h * 0.17));
            lblJugadores.setBounds(0, (int) (h * 0.25), w, (int) (h * 0.09));

            int ancho = (int) (w * 0.23);   // ancho de cada botón
            j2.centrarEn(w / 2, (int) (h * 0.45), ancho);
            j3.centrarEn(w / 2, (int) (h * 0.615), ancho);
            j4.centrarEn(w / 2, (int) (h * 0.78), ancho);
        }
    }

    // ------------------------------------------------------------------
    // Página 2: nombre + rol + Aceptar
    // ------------------------------------------------------------------

    private class PaginaRegistro extends Pagina {

        private final JLabel lblTitulo = etiqueta("");
        private final JLabel lblNombre = etiqueta("Nombre");
        private final JLabel lblRol = etiqueta("Rol");
        private final JTextField txtNombre = new JTextField();
        private final JComboBox<Rol> cmbRol = new JComboBox<>(Rol.values());
        private final JTextArea txtDescripcion = new JTextArea();
        private final BotonImagen acep = new BotonImagen(RUTA + "acep.png", "Aceptar");
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
        private int indice;

        PaginaRegistro() {
            tarjeta.setOpaque(false);
            lblNombre.setHorizontalAlignment(SwingConstants.LEFT);
            lblRol.setHorizontalAlignment(SwingConstants.LEFT);
            txtNombre.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x8A, 0x5A, 0x3C), 2),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)));
            txtNombre.setBackground(new Color(0xFF, 0xF8, 0xEE));
            txtNombre.setForeground(TEXTO);
            cmbRol.setBackground(new Color(0xFF, 0xF8, 0xEE));
            cmbRol.setForeground(TEXTO);
            txtDescripcion.setEditable(false);
            txtDescripcion.setFocusable(false);
            txtDescripcion.setOpaque(false);
            txtDescripcion.setLineWrap(true);
            txtDescripcion.setWrapStyleWord(true);
            txtDescripcion.setForeground(TEXTO);

            tarjeta.add(lblTitulo);
            tarjeta.add(lblNombre);
            tarjeta.add(txtNombre);
            tarjeta.add(lblRol);
            tarjeta.add(cmbRol);
            tarjeta.add(txtDescripcion);
            add(tarjeta);
            add(acep);

            cmbRol.addActionListener(e -> {
                Rol r = (Rol) cmbRol.getSelectedItem();
                txtDescripcion.setText(r == null ? "" : r.getDescripcion());
            });
            acep.addActionListener(e -> registrar(txtNombre.getText(), (Rol) cmbRol.getSelectedItem(), indice));
            txtNombre.addActionListener(e -> acep.doClick());   // Enter también acepta
        }

        void mostrarJugador(int i) {
            indice = i;
            lblTitulo.setText("Jugador " + (i + 1) + " de " + cantidad);
            txtNombre.setText("");
            Rol[] roles = Rol.values();
            cmbRol.setSelectedItem(roles[i % roles.length]);   // sugerido distinto por jugador
            txtDescripcion.setText(((Rol) cmbRol.getSelectedItem()).getDescripcion());
            enfocarNombre();
        }

        void enfocarNombre() {
            txtNombre.requestFocusInWindow();
        }

        @Override
        void colocar(int w, int h) {
            int tw = (int) (w * 0.44), th = (int) (h * 0.56);
            tarjeta.setBounds((w - tw) / 2, (int) (h * 0.12), tw, th);

            Font titulo = fuente(Font.BOLD | Font.ITALIC, 30, h);
            Font normal = fuente(Font.PLAIN, 18, h);
            lblTitulo.setFont(titulo);
            lblNombre.setFont(fuente(Font.BOLD, 17, h));
            lblRol.setFont(fuente(Font.BOLD, 17, h));
            txtNombre.setFont(normal);
            cmbRol.setFont(normal);
            txtDescripcion.setFont(fuente(Font.ITALIC, 15, h));

            int m = (int) (tw * 0.08);      // margen interior
            int cw = tw - 2 * m;            // ancho útil
            int campo = (int) (th * 0.12);  // alto de cada campo
            lblTitulo.setBounds(0, (int) (th * 0.05), tw, (int) (th * 0.15));
            lblNombre.setBounds(m, (int) (th * 0.24), cw, (int) (th * 0.09));
            txtNombre.setBounds(m, (int) (th * 0.33), cw, campo);
            lblRol.setBounds(m, (int) (th * 0.50), cw, (int) (th * 0.09));
            cmbRol.setBounds(m, (int) (th * 0.59), cw, campo);
            txtDescripcion.setBounds(m, (int) (th * 0.76), cw, (int) (th * 0.2));

            acep.centrarEn(w / 2, (int) (h * 0.80), (int) (w * 0.22));
        }
    }
}
