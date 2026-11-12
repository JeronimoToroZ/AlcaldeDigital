package influenceinc.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Base de TODAS las pantallas del juego: un lienzo logico que se escala a la
 * ventana. Asi todo cabe en cualquier tamano o resolucion sin desbordarse
 * (DISENO RESPONSIVO):
 *
 *  - Modo AJUSTAR: el lienzo mide siempre W x H logicos y se escala para caber
 *    entero (la oficina). Las franjas que sobran se cubren con el fondo.
 *  - Modo LLENAR: la altura logica es fija y el ancho logico crece con la
 *    ventana (portada, configuracion, final): el fondo ocupa todo y el
 *    contenido se centra. Si la ventana es muy angosta, se reduce la escala.
 *
 * Tambien maneja las zonas clicables, el cursor, los campos de texto y el
 * arrastre (los sellos y las tarjetas se arrastran).
 */
public abstract class Lienzo extends JPanel {

    /** Una zona clicable registrada durante el dibujo. */
    protected static final class Zona {
        final Rectangle r;
        final Runnable accion;

        Zona(Rectangle r, Runnable accion) {
            this.r = r;
            this.accion = accion;
        }
    }

    /** Campo de texto dibujado en el lienzo. */
    public static final class Campo {
        public final StringBuilder texto = new StringBuilder();
        public final int maximo;
        public Rectangle r = new Rectangle();
        public String sugerencia = "";
        public Runnable alEnter;

        public Campo(int maximo) {
            this.maximo = maximo;
        }

        public String valor() {
            return texto.toString().trim();
        }

        public void poner(String s) {
            texto.setLength(0);
            texto.append(s == null ? "" : s);
        }
    }

    protected final boolean llenar;
    protected final int baseW, baseH;
    /** Tamano logico actual (en LLENAR el ancho cambia con la ventana). */
    protected int lw, lh;
    protected double esc = 1, offX, offY;
    protected int mx = -1000, my = -1000;
    protected double t;
    private long ultimoNs = System.nanoTime();
    protected final List<Zona> zonas = new ArrayList<>();
    protected final List<Campo> campos = new ArrayList<>();
    protected Campo enfocado;
    private final Timer anim;
    private boolean errorInformado;

    protected Lienzo(int baseW, int baseH, boolean llenar) {
        this.baseW = baseW;
        this.baseH = baseH;
        this.llenar = llenar;
        this.lw = baseW;
        this.lh = baseH;
        setBackground(Tema.MADERA);
        setFocusable(true);
        setDoubleBuffered(true);
        setFocusTraversalKeysEnabled(false);
        anim = new Timer(16, e -> repaint());
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    ultimoNs = System.nanoTime();
                    anim.start();
                    alMostrar();
                } else {
                    anim.stop();
                }
            }
        });
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mover(e);
                boolean mano = false;
                for (Zona z : zonas) {
                    if (z.r.contains(mx, my)) {
                        mano = true;
                        break;
                    }
                }
                for (Campo c : campos) {
                    if (c.r.contains(mx, my)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                        return;
                    }
                }
                setCursor(Cursor.getPredefinedCursor(mano || cursorMano() ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mover(e);
                arrastrar(mx, my);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                mover(e);
                for (Campo c : campos) {
                    if (c.r.contains(mx, my)) {
                        enfocado = c;
                        return;
                    }
                }
                if (presionar(mx, my)) {
                    return;
                }
                for (int i = zonas.size() - 1; i >= 0; i--) {
                    Zona z = zonas.get(i);
                    if (z.r.contains(mx, my)) {
                        Sonido.tocar(Sonido.Tipo.CLIC);
                        z.accion.run();
                        repaint();
                        return;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mover(e);
                soltar(mx, my);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                rueda(e.getWheelRotation());
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addMouseWheelListener(m);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (enfocado != null) {
                    int k = e.getKeyCode();
                    if (k == KeyEvent.VK_BACK_SPACE && enfocado.texto.length() > 0) {
                        enfocado.texto.setLength(enfocado.texto.length() - 1);
                        return;
                    }
                    if (k == KeyEvent.VK_TAB) {
                        int i = campos.indexOf(enfocado);
                        enfocado = campos.isEmpty() ? null : campos.get((i + (e.isShiftDown() ? campos.size() - 1 : 1))
                                % campos.size());
                        return;
                    }
                    if (k == KeyEvent.VK_ENTER && enfocado.alEnter != null) {
                        enfocado.alEnter.run();
                        return;
                    }
                    if (k == KeyEvent.VK_ESCAPE) {
                        enfocado = null;
                        return;
                    }
                    if (k == KeyEvent.VK_BACK_SPACE) {
                        return;
                    }
                    if (!e.isActionKey() && k != KeyEvent.VK_ENTER && k != KeyEvent.VK_SHIFT
                            && k != KeyEvent.VK_CONTROL && k != KeyEvent.VK_ALT) {
                        return;     // los caracteres llegan por keyTyped
                    }
                }
                tecla(e);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (enfocado != null && c >= 32 && c != 127 && "|:;,".indexOf(c) < 0
                        && enfocado.texto.length() < enfocado.maximo) {
                    enfocado.texto.append(c);
                }
            }
        });
    }

    private void mover(MouseEvent e) {
        mx = (int) Math.round((e.getX() - offX) / esc);
        my = (int) Math.round((e.getY() - offY) / esc);
    }

    // ------------------------------------------------------------------ ganchos
    /** Dibuja la pantalla en coordenadas logicas (lw x lh). */
    protected abstract void pintar(Graphics2D g, double dt);

    /** Fondo que cubre toda la ventana (incluidas las franjas en modo AJUSTAR). */
    protected void fondoVentana(Graphics2D g, int w, int h) {
        g.setColor(Tema.fondo());
        g.fillRect(0, 0, w, h);
    }

    protected boolean presionar(int x, int y) {
        return false;
    }

    protected void arrastrar(int x, int y) {
    }

    protected void soltar(int x, int y) {
    }

    protected void rueda(int clics) {
    }

    protected void tecla(KeyEvent e) {
    }

    protected boolean cursorMano() {
        return false;
    }

    protected void alMostrar() {
    }

    // ------------------------------------------------------------------ dibujo
    @Override
    protected void paintComponent(Graphics g0) {
        long ahora = System.nanoTime();
        double dt = Math.min(0.1, (ahora - ultimoNs) / 1e9);
        ultimoNs = ahora;
        t += dt;
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        fondoVentana(g, w, h);
        if (llenar) {
            esc = h / (double) baseH;
            if (w / esc < baseW) {           // ventana angosta: se reduce para que todo quepa
                esc = w / (double) baseW;
            }
            lw = (int) Math.ceil(w / esc);
            lh = (int) Math.ceil(h / esc);
            offX = 0;
            offY = 0;
        } else {
            esc = Math.min(w / (double) baseW, h / (double) baseH);
            lw = baseW;
            lh = baseH;
            offX = (w - baseW * esc) / 2;
            offY = (h - baseH * esc) / 2;
        }
        g.translate(offX, offY);
        g.scale(esc, esc);
        if (!llenar) {
            g.setClip(0, 0, baseW, baseH);
        }
        zonas.clear();
        try {
            pintar(g, dt);
        } catch (RuntimeException ex) {
            // un error de dibujo no debe congelar el juego: se informa una vez y se sigue
            if (!errorInformado) {
                errorInformado = true;
                ex.printStackTrace();
            }
        }
        g.dispose();
    }

    // ------------------------------------------------------------------ utilidades
    protected void zona(Rectangle r, Runnable accion) {
        zonas.add(new Zona(r, accion));
    }

    protected boolean encima(Rectangle r) {
        return r.contains(mx, my);
    }

    /** Boton estandar del juego (registra la zona). */
    protected void boton(Graphics2D g, Rectangle r, String txt, Dibujo.Estilo estilo, Runnable accion) {
        Dibujo.boton(g, r, txt, estilo, encima(r) && accion != null, accion != null);
        if (accion != null) {
            zona(r, accion);
        }
    }

    /** Boton redondo con una imagen (ayuda, estructura, pausa, mapa...). */
    protected void botonImagen(Graphics2D g, String imagen, Rectangle r, String etiqueta, Runnable accion) {
        Dibujo.botonImagen(g, imagen, r, etiqueta, encima(r), t);
        zona(r, accion);
    }

    /** Campo de texto con estilo del juego. */
    protected void campo(Graphics2D g, Campo c, Rectangle r, String titulo) {
        c.r = r;
        if (!campos.contains(c)) {
            campos.add(c);
        }
        Dibujo.campo(g, c, r, titulo, c == enfocado, t);
    }

    protected Color colorTexto() {
        return Tema.texto();
    }
}
