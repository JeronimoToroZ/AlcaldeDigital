package alcaldedigital.gui;

import alcaldedigital.estructuras.GrafoNiveles;
import alcaldedigital.estructuras.NodoNivel;
import alcaldedigital.logica.Partida;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Mapa de la campaña: dibuja el GRAFO DE NIVELES sobre el fondo f1.
 *
 *  - Vértices: las imágenes n1..n7.  Aristas: líneas entre niveles.
 *  - Bloqueado (gris con candado): ningún predecesor ha sido ganado.
 *  - Disponible (brilla): se puede jugar.
 *  - Ganado (anillo dorado y estrellas).
 * Ganar un nivel desbloquea a sus sucesores, es decir, el subárbol que
 * se desprende de ese nodo. La regla la decide {@link GrafoNiveles}.
 */
public class PantallaMapa extends JFrame {

    private static final String RUTA = "/alcaldedigital/recursos/";

    private final Partida partida;
    private final Runnable alVolverAlInicio;
    private final PanelMapa panel;
    private boolean eleccionMostrada;
    private boolean instruccionesMostradas;

    public PantallaMapa(Partida partida, Runnable alVolverAlInicio) {
        this.partida = partida;
        this.alVolverAlInicio = alVolverAlInicio;
        setTitle("Alcalde Digital - Mapa de la campaña");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panel = new PanelMapa();
        panel.setPreferredSize(new Dimension(1100, 619));
        setContentPane(panel);
        pack();
        setMinimumSize(new Dimension(900, 520));
        setLocationRelativeTo(null);
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b && !instruccionesMostradas) {
            instruccionesMostradas = true;
            SwingUtilities.invokeLater(() -> DialogoInstrucciones.instruccionesIniciales(this));
        }
    }

    // ------------------------------------------------------------------
    // Flujo entre el mapa y los niveles
    // ------------------------------------------------------------------

    private void intentarJugar(NodoNivel n) {
        GrafoNiveles g = partida.getNiveles();
        if (!g.estaDesbloqueado(n.getNumero())) {
            List<Integer> previos = g.predecesores(n.getNumero());
            panel.mostrarMensaje("Nivel bloqueado: gana el nivel " + previos.get(0) + " para abrirlo.");
            return;
        }
        if (new DialogoPreviaNivel(this, partida, n).elegir()) {
            jugar(n.getNumero());
        }
    }

    private void jugar(int numero) {
        setVisible(false);
        new VentanaNivel(partida, numero, (desbloqueados, reintentar) -> {
            if (reintentar) {
                jugar(numero);
                return;
            }
            setVisible(true);
            panel.animarDesbloqueo(desbloqueados);
            if (partida.eleccionDisponible() && !eleccionMostrada) {
                eleccionMostrada = true;
                SwingUtilities.invokeLater(this::mostrarEleccion);
            }
        }).setVisible(true);
    }

    private void mostrarEleccion() {
        if (new DialogoEleccion(this, partida).elegir() == DialogoEleccion.Opcion.INICIO) {
            dispose();
            alVolverAlInicio.run();
        }
        panel.repaint();
    }

    // ------------------------------------------------------------------
    // Panel del mapa
    // ------------------------------------------------------------------

    private class PanelMapa extends PanelConFondo {

        private final Map<Integer, BufferedImage> imagenes = new HashMap<>();
        private final Map<Integer, BufferedImage> imagenesGris = new HashMap<>();
        private final Timer animacion;
        private final BotonImagen ayuda = new BotonImagen(RUTA + "ayuda.png", "Ayuda", true);
        private final BotonImagen ajustes = new BotonImagen(RUTA + "ajuste.png", "Ajustes", true);
        private final DialogoJuego.BotonDorado estructuras = new DialogoJuego.BotonDorado("Ciudad y estructuras", false);
        private final DialogoJuego.BotonDorado elecciones = new DialogoJuego.BotonDorado("Ver elecciones", true);
        private final JButton salir = new JButton();

        private long tick;
        private int hover = -1;
        private String mensaje;
        private int ticksMensaje;
        private final List<Integer> recienDesbloqueados = new ArrayList<>();
        private int ticksDesbloqueo;

        PanelMapa() {
            super(null, RUTA + "f1.jpg");
            for (NodoNivel n : partida.getNiveles().getNodos()) {
                BufferedImage img = BotonImagen.cargar(RUTA + n.getImagen());
                if (img != null) {
                    img = BotonImagen.recortarTransparencia(img);
                    imagenes.put(n.getNumero(), img);
                    imagenesGris.put(n.getNumero(), aGris(img));
                }
            }

            ayuda.setToolTipText("Ayuda");
            ayuda.addActionListener(e -> DialogoInstrucciones.ayuda(PantallaMapa.this));
            ajustes.setToolTipText("Ajustes");
            ajustes.addActionListener(e -> new DialogoAjustes(PantallaMapa.this).setVisible(true));
            estructuras.addActionListener(e -> new DialogoEstructuras(PantallaMapa.this, partida).setVisible(true));
            elecciones.addActionListener(e -> mostrarEleccion());
            // el botón "Exit" viene dibujado en f1.jpg: encima va un botón invisible
            salir.setOpaque(false);
            salir.setContentAreaFilled(false);
            salir.setBorderPainted(false);
            salir.setFocusPainted(false);
            salir.setToolTipText("Salir");
            salir.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            salir.addActionListener(e -> {
                if (DialogoJuego.confirmar(PantallaMapa.this, "¿Salir del juego?",
                        "Tu progreso en esta campaña no se guarda al cerrar.", "Salir", "Quedarme")) {
                    System.exit(0);
                }
            });
            add(ayuda);
            add(ajustes);
            add(estructuras);
            add(elecciones);
            add(salir);
            elecciones.setVisible(false);   // aparece al ganar un nivel final

            MouseAdapter raton = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int nuevo = nodoEn(e.getX(), e.getY());
                    if (nuevo != hover) {
                        hover = nuevo;
                        setCursor(Cursor.getPredefinedCursor(nuevo >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    int n = nodoEn(e.getX(), e.getY());
                    if (n >= 0) {
                        intentarJugar(partida.getNiveles().getNodo(n));
                    }
                }
            };
            addMouseListener(raton);
            addMouseMotionListener(raton);

            animacion = new Timer(40, e -> {
                tick++;
                if (ticksMensaje > 0) ticksMensaje--;
                if (ticksDesbloqueo > 0) ticksDesbloqueo--;
                repaint();
            });
            Configuracion.agregarOyente(this::repaint);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            animacion.start();
        }

        @Override
        public void removeNotify() {
            animacion.stop();
            super.removeNotify();
        }

        void mostrarMensaje(String texto) {
            mensaje = texto;
            ticksMensaje = 75;
        }

        void animarDesbloqueo(List<Integer> nuevos) {
            elecciones.setVisible(partida.eleccionDisponible());
            recienDesbloqueados.clear();
            recienDesbloqueados.addAll(nuevos);
            ticksDesbloqueo = nuevos.isEmpty() ? 0 : 50;
            if (!nuevos.isEmpty()) {
                mostrarMensaje(nuevos.size() == 1 ? "¡Se desbloqueó el nivel " + nuevos.get(0) + "!"
                        : "¡Se desbloquearon los niveles " + nuevos.get(0) + " y " + nuevos.get(1) + "!");
            }
        }

        @Override
        public void doLayout() {
            int w = getWidth(), h = getHeight();
            int lado = (int) (w * 0.052);
            ajustes.centrarEn((int) (w * 0.955), (int) (h * 0.075), lado);
            ayuda.centrarEn((int) (w * 0.955) - lado - 12, (int) (h * 0.075), lado);
            Dimension de = estructuras.getPreferredSize();
            estructuras.setBounds(20, h - de.height - 18, de.width, de.height);
            Dimension dl = elecciones.getPreferredSize();
            elecciones.setBounds(20 + de.width + 12, h - dl.height - 18, dl.width, dl.height);
            salir.setBounds((int) (w * 0.865), (int) (h * 0.875), (int) (w * 0.12), (int) (h * 0.085));
        }

        private double radio() {
            return getHeight() * 0.075;
        }

        private int nodoEn(int mx, int my) {
            double r = radio();
            for (NodoNivel n : partida.getNiveles().getNodos()) {
                double dx = mx - n.getX() * getWidth(), dy = my - n.getY() * getHeight();
                if (dx * dx + dy * dy <= r * r) {
                    return n.getNumero();
                }
            }
            return -1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);   // fondo f1
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            boolean contraste = Tema.isAltoContraste();
            int w = getWidth(), h = getHeight();
            if (contraste) {
                g2.setColor(new Color(0, 0, 0, 190));
                g2.fillRect(0, 0, w, h);
            }
            GrafoNiveles grafo = partida.getNiveles();
            double r = radio();

            // ---- aristas ----
            for (int[] a : grafo.aristas()) {
                NodoNivel o = grafo.getNodo(a[0]), d = grafo.getNodo(a[1]);
                double x1 = o.getX() * w, y1 = o.getY() * h, x2 = d.getX() * w, y2 = d.getY() * h;
                boolean abierta = o.isCompletado();
                if (abierta) {
                    g2.setColor(new Color(0xE9, 0xC4, 0x7A, 90));
                    g2.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
                    g2.setColor(contraste ? new Color(255, 255, 0) : new Color(0xC9, 0x8E, 0x2B));
                    g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                } else {
                    g2.setColor(contraste ? Color.WHITE : new Color(0x5A, 0x40, 0x30, 150));
                    g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                            new float[]{10, 10}, (float) (tick % 20)));
                }
                g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
            }

            // ---- vértices ----
            for (NodoNivel n : grafo.getNodos()) {
                dibujarNodo(g2, n, grafo.estaDesbloqueado(n.getNumero()), r, w, h, contraste);
            }

            dibujarCabecera(g2, contraste);
            if (hover >= 0) {
                dibujarInfo(g2, grafo.getNodo(hover), grafo, r, w, h);
            }
            if (ticksMensaje > 0 && mensaje != null) {
                dibujarMensaje(g2, w, h);
            }
            g2.dispose();
        }

        private void dibujarNodo(Graphics2D g2, NodoNivel n, boolean abierto, double r, int w, int h,
                                 boolean contraste) {
            double cx = n.getX() * w, cy = n.getY() * h;
            double escala = 1.0;
            if (n.getNumero() == hover && abierto) {
                escala = 1.08;
            }
            if (ticksDesbloqueo > 0 && recienDesbloqueados.contains(n.getNumero())) {
                escala += Math.sin(ticksDesbloqueo * 0.35) * 0.12;
            }
            double rr = r * escala;

            if (abierto && !n.isCompletado()) {
                // halo que late: "¡juégame!"
                double halo = rr + 6 + Math.abs(Math.sin(tick * 0.12)) * 10;
                g2.setColor(contraste ? new Color(255, 255, 0, 120) : new Color(0xFF, 0xE7, 0xA8, 120));
                g2.fill(new java.awt.geom.Ellipse2D.Double(cx - halo, cy - halo, halo * 2, halo * 2));
            }
            if (n.isCompletado()) {
                g2.setColor(contraste ? new Color(255, 255, 0) : DialogoJuego.ORO);
                g2.setStroke(new BasicStroke(5f));
                g2.draw(new java.awt.geom.Ellipse2D.Double(cx - rr - 5, cy - rr - 5, (rr + 5) * 2, (rr + 5) * 2));
            }
            BufferedImage img = (abierto ? imagenes : imagenesGris).get(n.getNumero());
            if (img != null) {
                g2.drawImage(img, (int) (cx - rr), (int) (cy - rr), (int) (rr * 2), (int) (rr * 2), null);
            }
            if (!abierto) {
                g2.setColor(new Color(0, 0, 0, 110));
                g2.fill(new java.awt.geom.Ellipse2D.Double(cx - rr, cy - rr, rr * 2, rr * 2));
                dibujarCandado(g2, cx + rr * 0.55, cy + rr * 0.55, rr * 0.42);
            }
            if (n.isCompletado()) {
                for (int i = 0; i < 3; i++) {
                    double sx = cx + (i - 1) * rr * 0.55, sy = cy + rr + 12;
                    java.awt.geom.Path2D e = DialogoFinNivel.Estrellas.estrella(sx, sy, rr * 0.2, rr * 0.09);
                    g2.setColor(i < n.getEstrellas() ? new Color(0xF4, 0xC8, 0x4A) : new Color(0, 0, 0, 90));
                    g2.fill(e);
                    g2.setColor(new Color(0x5A, 0x34, 0x1C));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(e);
                }
            }
        }

        private void dibujarCandado(Graphics2D g2, double cx, double cy, double t) {
            g2.setColor(new Color(0x26, 0x15, 0x0E));
            g2.fill(new java.awt.geom.Ellipse2D.Double(cx - t, cy - t, t * 2, t * 2));
            g2.setColor(DialogoJuego.ORO);
            g2.setStroke(new BasicStroke((float) (t * 0.18)));
            g2.draw(new java.awt.geom.Arc2D.Double(cx - t * 0.38, cy - t * 0.75, t * 0.76, t * 0.8, 0, 180,
                    java.awt.geom.Arc2D.OPEN));
            g2.fill(new java.awt.geom.RoundRectangle2D.Double(cx - t * 0.55, cy - t * 0.38, t * 1.1, t * 0.9,
                    t * 0.25, t * 0.25));
            g2.setColor(new Color(0x26, 0x15, 0x0E));
            g2.fill(new java.awt.geom.Ellipse2D.Double(cx - t * 0.12, cy - t * 0.05, t * 0.24, t * 0.24));
        }

        private void dibujarCabecera(Graphics2D g2, boolean contraste) {
            int x = 20, y = 16, ancho = 360, alto = 74;
            g2.setPaint(contraste ? Color.BLACK : new GradientPaint(0, y, new Color(0x4A, 0x2C, 0x1E, 235),
                    0, y + alto, new Color(0x26, 0x15, 0x0E, 235)));
            g2.fillRoundRect(x, y, ancho, alto, 20, 20);
            g2.setColor(contraste ? Color.WHITE : DialogoJuego.ORO);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, ancho, alto, 20, 20);
            g2.setFont(new Font(DialogoJuego.FUENTE, Font.BOLD | Font.ITALIC, 24));
            g2.drawString("Mapa de la campaña", x + 18, y + 32);
            g2.setFont(new Font(DialogoJuego.FUENTE, Font.PLAIN, 14));
            g2.setColor(contraste ? Color.WHITE : DialogoJuego.CREMA);
            int ganados = 0;
            for (NodoNivel n : partida.getNiveles().getNodos()) {
                if (n.isCompletado()) ganados++;
            }
            g2.drawString(partida.getJugador().getNombre() + "  ·  " + partida.getJugador().getPuntos() + " pts  ·  "
                    + ganados + "/" + partida.getNiveles().cantidadVertices() + " niveles", x + 18, y + 56);
        }

        private void dibujarInfo(Graphics2D g2, NodoNivel n, GrafoNiveles grafo, double r, int w, int h) {
            boolean abierto = grafo.estaDesbloqueado(n.getNumero());
            String estado = n.isCompletado() ? "Ganado · " + n.getEstrellas() + " estrella(s)"
                    : abierto ? "Disponible: haz clic para jugar" : "Bloqueado";
            String[] lineas = {"Nivel " + n.getNumero(), estado,
                               new alcaldedigital.logica.DificultadNivel(n.getNumero()).burbujasRequeridas
                                       + " burbujas correctas por publicación"};
            g2.setFont(new Font(DialogoJuego.FUENTE, Font.PLAIN, 13));
            FontMetrics fm = g2.getFontMetrics();
            int ancho = 0;
            for (String l : lineas) ancho = Math.max(ancho, fm.stringWidth(l));
            ancho += 28;
            int alto = lineas.length * 19 + 16;
            int x = (int) (n.getX() * w + r + 14), y = (int) (n.getY() * h - alto / 2);
            if (x + ancho > w - 10) x = (int) (n.getX() * w - r - 14 - ancho);
            g2.setColor(new Color(0x26, 0x15, 0x0E, 235));
            g2.fillRoundRect(x, y, ancho, alto, 14, 14);
            g2.setColor(DialogoJuego.ORO);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, ancho, alto, 14, 14);
            for (int i = 0; i < lineas.length; i++) {
                g2.setFont(new Font(DialogoJuego.FUENTE, i == 0 ? Font.BOLD : Font.PLAIN, i == 0 ? 15 : 13));
                g2.setColor(i == 0 ? DialogoJuego.ORO : DialogoJuego.CREMA);
                g2.drawString(lineas[i], x + 14, y + 24 + i * 19);
            }
        }

        private void dibujarMensaje(Graphics2D g2, int w, int h) {
            g2.setFont(new Font(DialogoJuego.FUENTE, Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            int ancho = fm.stringWidth(mensaje) + 40, alto = 40;
            int x = (w - ancho) / 2, y = (int) (h * 0.075) - alto / 2;
            float alfa = Math.min(1f, ticksMensaje / 15f);
            java.awt.Composite c = g2.getComposite();
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alfa));
            g2.setColor(new Color(0x26, 0x15, 0x0E, 235));
            g2.fillRoundRect(x, y, ancho, alto, alto, alto);
            g2.setColor(DialogoJuego.ORO);
            g2.drawRoundRect(x, y, ancho, alto, alto, alto);
            g2.setColor(DialogoJuego.CREMA);
            g2.drawString(mensaje, x + 20, y + 26);
            g2.setComposite(c);
        }

        /** El brillo de Ajustes oscurece todo el mapa, botones incluidos. */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Configuracion.pintarBrillo(g, getWidth(), getHeight());
        }

        private BufferedImage aGris(BufferedImage img) {
            BufferedImage copia = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            copia.getGraphics().drawImage(img, 0, 0, null);
            BufferedImage gris = new ColorConvertOp(java.awt.color.ColorSpace.getInstance(
                    java.awt.color.ColorSpace.CS_GRAY), null).filter(copia, null);
            BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int a = copia.getRGB(x, y) >>> 24;
                    int v = gris.getRGB(x, y) & 0xFF;
                    out.setRGB(x, y, (a << 24) | (v << 16) | (v << 8) | v);
                }
            }
            return out;
        }
    }
}
