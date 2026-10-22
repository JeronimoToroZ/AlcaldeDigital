package alcaldedigital.gui;

import alcaldedigital.modelo.Jugador;
import alcaldedigital.modelo.Rol;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Registro de jugadores, con el fondo f1. Tiene dos páginas:
 *   1) ¿Cuántos van a jugar?  -> botones j2, j3 y j4.
 *   2) Nombre y rol de cada jugador (los roles NO se pueden repetir) -> botón acep.
 * Al aceptar, entrega la lista de jugadores a quien la creó.
 */
public class PantallaJugadores extends JFrame {

    private static final String RUTA = "/alcaldedigital/recursos/";
    private static final String FONDO = RUTA + "f1.jpg";
    private static final Color TEXTO = new Color(0x4A, 0x2A, 0x20);   // marrón oscuro
    private static final Color COLOR_ERROR = new Color(0xB3, 0x26, 0x1E);
    private static final String FUENTE = "Georgia";

    private final Consumer<List<Jugador>> alTerminar;
    private final CardLayout cartas = new CardLayout();
    private final JPanel contenedor = new JPanel(cartas);
    private final PaginaRegistro paginaRegistro = new PaginaRegistro();

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
        paginaRegistro.preparar(n);
        cartas.show(contenedor, "registro");
        SwingUtilities.invokeLater(paginaRegistro::enfocarPrimero);
    }

    // ------------------------------------------------------------------
    // Base común: fondo f1, botón de ajustes y zona "Exit" dibujada en la imagen
    // ------------------------------------------------------------------

    private abstract class Pagina extends PanelConFondo {

        private final JButton salir = new JButton();
        private final BotonImagen ajuste = new BotonImagen(RUTA + "ajuste.png", "Ajustes");

        Pagina() {
            super(null, FONDO);
            salir.setOpaque(false);
            salir.setContentAreaFilled(false);
            salir.setBorderPainted(false);
            salir.setFocusPainted(false);
            salir.setToolTipText("Salir");
            salir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            salir.addActionListener(e -> System.exit(0));
            add(salir);

            ajuste.setToolTipText("Ajustes");
            ajuste.addActionListener(e -> new DialogoAjustes(PantallaJugadores.this).setVisible(true));
            add(ajuste);

            Configuracion.agregarOyente(this::repaint);   // el brillo se ve en vivo
        }

        /** El brillo oscurece toda la página, incluidos los botones. */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Configuracion.pintarBrillo(g, getWidth(), getHeight());
        }

        abstract void colocar(int w, int h);

        @Override
        public void doLayout() {
            int w = getWidth(), h = getHeight();
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

    // ------------------------------------------------------------------
    // Página 1: ¿cuántos jugadores? (j2, j3, j4)
    // ------------------------------------------------------------------

    private class PaginaCantidad extends Pagina {

        private final JLabel lblTitulo = etiqueta("¿Cuántos van a jugar?");
        private final JLabel lblSub = etiqueta("Cada uno juega su turno en cada nivel");
        private final BotonImagen j2 = new BotonImagen(RUTA + "j2.png", "2 Jugadores");
        private final BotonImagen j3 = new BotonImagen(RUTA + "j3.png", "3 Jugadores");
        private final BotonImagen j4 = new BotonImagen(RUTA + "j4.png", "4 Jugadores");

        PaginaCantidad() {
            add(lblTitulo);
            add(lblSub);
            add(j2);
            add(j3);
            add(j4);
            j2.addActionListener(e -> elegirCantidad(2));
            j3.addActionListener(e -> elegirCantidad(3));
            j4.addActionListener(e -> elegirCantidad(4));
        }

        @Override
        void colocar(int w, int h) {
            lblTitulo.setFont(fuente(Font.BOLD | Font.ITALIC, 50, h));
            lblSub.setFont(fuente(Font.ITALIC, 22, h));
            lblTitulo.setBounds(0, (int) (h * 0.08), w, (int) (h * 0.15));
            lblSub.setBounds(0, (int) (h * 0.23), w, (int) (h * 0.08));
            int ancho = (int) (w * 0.23);
            j2.centrarEn(w / 2, (int) (h * 0.43), ancho);
            j3.centrarEn(w / 2, (int) (h * 0.60), ancho);
            j4.centrarEn(w / 2, (int) (h * 0.77), ancho);
        }
    }

    // ------------------------------------------------------------------
    // Página 2: nombre y rol de cada jugador
    // ------------------------------------------------------------------

    private class PaginaRegistro extends Pagina {

        private final JLabel lblTitulo = etiqueta("Registro de jugadores");
        private final JLabel lblError = etiqueta(" ");
        private final BotonImagen acep = new BotonImagen(RUTA + "acep.png", "Aceptar");
        private final DialogoJuego.BotonDorado volver = new DialogoJuego.BotonDorado("← Cambiar cantidad", false);
        private final List<TarjetaJugador> tarjetas = new ArrayList<>();

        PaginaRegistro() {
            add(lblTitulo);
            add(lblError);
            add(acep);
            add(volver);
            lblError.setForeground(COLOR_ERROR);
            acep.addActionListener(e -> aceptar());
            volver.addActionListener(e -> cartas.show(contenedor, "cantidad"));
        }

        /** Crea una tarjeta por jugador, cada una con un rol distinto sugerido. */
        void preparar(int cantidad) {
            for (TarjetaJugador t : tarjetas) {
                remove(t);
            }
            tarjetas.clear();
            Rol[] roles = Rol.values();
            for (int i = 0; i < cantidad; i++) {
                TarjetaJugador t = new TarjetaJugador(i + 1, roles[i % roles.length]);
                tarjetas.add(t);
                add(t);
            }
            lblError.setText(" ");
            revalidate();
            repaint();
        }

        void enfocarPrimero() {
            if (!tarjetas.isEmpty()) {
                tarjetas.get(0).txtNombre.requestFocusInWindow();
            }
        }

        /** Marca en rojo las tarjetas con rol repetido y devuelve true si no hay repetidos. */
        private boolean revisarRoles() {
            boolean ok = true;
            String repetido = null;
            for (TarjetaJugador t : tarjetas) {
                t.setRepetido(false);
            }
            for (int i = 0; i < tarjetas.size(); i++) {
                for (int k = i + 1; k < tarjetas.size(); k++) {
                    if (tarjetas.get(i).rol() == tarjetas.get(k).rol()) {
                        tarjetas.get(i).setRepetido(true);
                        tarjetas.get(k).setRepetido(true);
                        repetido = tarjetas.get(i).rol().getEtiqueta();
                        ok = false;
                    }
                }
            }
            lblError.setText(ok ? " " : "Los roles no se pueden repetir: hay dos jugadores con \"" + repetido + "\".");
            return ok;
        }

        private void aceptar() {
            if (!revisarRoles()) {
                return;
            }
            List<Jugador> jugadores = new ArrayList<>();
            for (TarjetaJugador t : tarjetas) {
                String nombre = t.txtNombre.getText().trim();
                if (nombre.isEmpty()) {
                    nombre = "Jugador " + t.numero;
                }
                jugadores.add(new Jugador(nombre, t.rol()));
            }
            dispose();
            alTerminar.accept(jugadores);
        }

        @Override
        void colocar(int w, int h) {
            lblTitulo.setFont(fuente(Font.BOLD | Font.ITALIC, 40, h));
            lblTitulo.setBounds(0, (int) (h * 0.05), w, (int) (h * 0.12));
            int n = Math.max(1, tarjetas.size());
            int espacio = (int) (w * 0.018);
            int tw = Math.min((int) (w * 0.27), (int) ((w * 0.86 - espacio * (n - 1)) / n));
            int th = (int) (h * 0.52);
            int x0 = (w - (tw * n + espacio * (n - 1))) / 2;
            int y0 = (int) (h * 0.19);
            for (int i = 0; i < tarjetas.size(); i++) {
                tarjetas.get(i).setBounds(x0 + i * (tw + espacio), y0, tw, th);
                tarjetas.get(i).colocar(tw, th, h);
            }
            lblError.setFont(fuente(Font.BOLD, 15, h));
            lblError.setBounds(0, (int) (h * 0.72), w, (int) (h * 0.05));
            acep.centrarEn(w / 2, (int) (h * 0.84), (int) (w * 0.2));
            Dimension dv = volver.getPreferredSize();
            volver.setBounds(20, h - dv.height - 18, dv.width, dv.height);
        }

        /** Tarjeta de un jugador: título, nombre, rol y qué hace ese rol. */
        private class TarjetaJugador extends JPanel {

            final int numero;
            final JLabel lblTitulo = new JLabel("", SwingConstants.CENTER);
            final JLabel lblNombre = new JLabel("Nombre");
            final JTextField txtNombre = new JTextField();
            final JLabel lblRol = new JLabel("Rol");
            final JComboBox<Rol> cmbRol = new JComboBox<>(Rol.values());
            final JTextArea txtDescripcion = new JTextArea();
            private boolean repetido;

            TarjetaJugador(int numero, Rol sugerido) {
                super(null);
                this.numero = numero;
                setOpaque(false);
                lblTitulo.setText("Jugador " + numero);
                for (JLabel l : new JLabel[]{lblTitulo, lblNombre, lblRol}) {
                    l.setForeground(TEXTO);
                    add(l);
                }
                txtNombre.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x8A, 0x5A, 0x3C), 2),
                        BorderFactory.createEmptyBorder(2, 8, 2, 8)));
                txtNombre.setBackground(new Color(0xFF, 0xF8, 0xEE));
                txtNombre.setForeground(TEXTO);
                cmbRol.setBackground(new Color(0xFF, 0xF8, 0xEE));
                cmbRol.setForeground(TEXTO);
                cmbRol.setSelectedItem(sugerido);
                txtDescripcion.setEditable(false);
                txtDescripcion.setFocusable(false);
                txtDescripcion.setOpaque(false);
                txtDescripcion.setLineWrap(true);
                txtDescripcion.setWrapStyleWord(true);
                txtDescripcion.setForeground(TEXTO);
                txtDescripcion.setText(sugerido.getDescripcion());
                cmbRol.addActionListener(e -> {
                    txtDescripcion.setText(rol().getDescripcion());
                    revisarRoles();   // avisa al instante si el rol ya está tomado
                });
                txtNombre.addActionListener(e -> acep.doClick());   // Enter también acepta
                add(txtNombre);
                add(cmbRol);
                add(txtDescripcion);
            }

            Rol rol() {
                return (Rol) cmbRol.getSelectedItem();
            }

            void setRepetido(boolean r) {
                repetido = r;
                repaint();
            }

            void colocar(int tw, int th, int h) {
                lblTitulo.setFont(fuente(Font.BOLD | Font.ITALIC, 22, h));
                lblNombre.setFont(fuente(Font.BOLD, 15, h));
                lblRol.setFont(fuente(Font.BOLD, 15, h));
                txtNombre.setFont(fuente(Font.PLAIN, 16, h));
                cmbRol.setFont(fuente(Font.PLAIN, 15, h));
                txtDescripcion.setFont(fuente(Font.ITALIC, 12, h));
                int m = (int) (tw * 0.08), cw = tw - 2 * m, campo = (int) (th * 0.12);
                lblTitulo.setBounds(0, (int) (th * 0.04), tw, (int) (th * 0.14));
                lblNombre.setBounds(m, (int) (th * 0.20), cw, (int) (th * 0.09));
                txtNombre.setBounds(m, (int) (th * 0.29), cw, campo);
                lblRol.setBounds(m, (int) (th * 0.45), cw, (int) (th * 0.09));
                cmbRol.setBounds(m, (int) (th * 0.54), cw, campo);
                txtDescripcion.setBounds(m, (int) (th * 0.70), cw, (int) (th * 0.27));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 175));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
                g2.setColor(repetido ? COLOR_ERROR : new Color(0x8A, 0x5A, 0x3C, 200));
                g2.setStroke(new BasicStroke(repetido ? 3f : 1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 26, 26);
                g2.dispose();
            }
        }
    }
}
