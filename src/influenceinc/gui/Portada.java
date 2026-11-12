package influenceinc.gui;

import influenceinc.modelo.Rasgos;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/**
 * PORTADA: la imagen "fondo" cubre toda la ventana, el titulo y los botones
 * van al centro (la zona libre del escritorio) y la fila de ciudadanos desfila
 * abajo con el MISMO movimiento de siempre:
 *     x = ((i * 230 + t * 40) % (ancho + 300)) - 150,  y = alto - 170
 */
public class Portada extends Lienzo {

    /** Lo que la ventana hace con cada boton. */
    public interface Acciones {
        void jugarSolo();

        void jugarLocal();

        void crearRed();

        void unirseRed();

        void salir();
    }

    private final Acciones acc;
    private final Rasgos[] caras = new Rasgos[7];
    private boolean opciones;
    /** Modo relajado: lo lee la ventana al empezar la partida. */
    public boolean relajado;

    public Portada(Acciones acc) {
        super(1180, 860, true);
        this.acc = acc;
        Random r = new Random();
        for (int i = 0; i < caras.length; i++) {
            caras[i] = Rasgos.azar(r);
        }
    }

    @Override
    protected void fondoVentana(Graphics2D g, int w, int h) {
        g.setColor(Tema.MADERA);
        g.fillRect(0, 0, w, h);
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
            return;
        }
        Recursos.cubrir(g, "fondo.jpg", 0, 0, w, h);
    }

    @Override
    protected void pintar(Graphics2D g, double dt) {
        int w = lw, h = lh;
        int cx = w / 2;
        // luz calida de la lampara que "respira" y vineta para centrar la mirada
        if (!Tema.altoContraste) {
            float a = (float) (0.16 + 0.04 * Math.sin(t * 1.3));
            g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, h * 0.42), (float) (Math.max(w, h) * 0.55),
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(255, 214, 160, (int) (a * 255)), new Color(80, 40, 20, 0), new Color(20, 8, 4, 150)}));
            g.fillRect(0, 0, w, h);
        }
        personajes(g, w, h);
        titulo(g, cx);
        botones(g, cx);
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(Tema.alfa(Tema.CREMA, 210));
        Dibujo.centrado(g, "Universidad del Norte  ·  Estructura de Datos II  ·  2026", cx, h - 22);
        if (opciones) {
            opciones(g, w, h);
        }
    }

    // ------------------------------------------------------------------ titulo
    private void titulo(Graphics2D g, int cx) {
        // insignia de Civitas
        g.setFont(Tema.fuente(Font.BOLD, 13));
        String ins = "SEMANA ELECTORAL EN CIUDAD NOVA";
        FontMetrics fm = g.getFontMetrics();
        int iw = fm.stringWidth(ins) + 40;
        RoundRectangle2D pill = new RoundRectangle2D.Double(cx - iw / 2.0, 40, iw, 30, 30, 30);
        g.setColor(Tema.altoContraste ? Color.BLACK : Tema.alfa(Tema.LILA, 225));
        g.fill(pill);
        g.setColor(Tema.altoContraste ? Color.WHITE : Tema.LILA_CLARO);
        g.draw(pill);
        g.setColor(Tema.altoContraste ? Color.WHITE : Tema.CREMA);
        g.fill(new Ellipse2D.Double(cx - iw / 2.0 + 14, 51, 8, 8));
        g.drawString(ins, cx - iw / 2 + 28, 60);

        Font ft = Tema.fuente(Font.BOLD, 70);
        g.setFont(ft);
        String t1 = "Alcalde Digital";
        if (!Tema.altoContraste) {
            g.setColor(new Color(30, 12, 6, 140));
            Dibujo.centrado(g, t1, cx + 3, 146 + 4);
            g.setPaint(new GradientPaint(0, 88, new Color(255, 236, 205), 0, 148, Tema.DORADO));
        } else {
            g.setColor(Color.WHITE);
        }
        Dibujo.centrado(g, t1, cx, 146);
        g.setFont(Tema.fuente(Font.PLAIN, 20));
        g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.ROSA_CLARO);
        Dibujo.centrado(g, "Mesa de Verificación  ·  Civitas", cx, 180);
        g.setFont(Tema.fuente(Font.ITALIC, 15));
        g.setColor(Tema.altoContraste ? Color.WHITE : Tema.alfa(Tema.CREMA, 225));
        Dibujo.centrado(g, "Todos quieren publicar algo. No todos dicen la verdad.", cx, 208);
    }

    // ------------------------------------------------------------------ botones
    private void botones(Graphics2D g, int cx) {
        String[][] b = {
            {"Jugar solo", "Escoge tu nombre y tu rol · 5 días · 3 casos por día"},
            {"Partida local", "2 a 4 jugadores en este computador, por turnos"},
            {"Crear partida en red", "Eres el anfitrión: los demás se unen con tu IP"},
            {"Unirse a partida en red", "Escribe la IP del anfitrión y tu rol"},
            {"Opciones", null},
            {"Salir", null}
        };
        Runnable[] a = {acc::jugarSolo, acc::jugarLocal, acc::crearRed, acc::unirseRed, () -> opciones = true, acc::salir};
        int bw = 440, y = 236;
        for (int i = 0; i < b.length; i++) {
            Rectangle r;
            if (i < 4) {
                r = new Rectangle(cx - bw / 2, y, bw, 58);
                y += 68;
            } else {
                int half = (bw - 12) / 2;
                r = new Rectangle(i == 4 ? cx - bw / 2 : cx - bw / 2 + half + 12, y, half, 48);
            }
            boolean hover = !opciones && encima(r);
            botonPortada(g, r, b[i][0], b[i][1], i, hover);
            if (!opciones) {
                zona(r, a[i]);
            }
        }
    }

    /**
     * Boton de la portada: tarjeta crema con borde cobre, icono redondo con el
     * color de la portada y texto en Poppins. Al pasar el mouse se eleva.
     */
    private void botonPortada(Graphics2D g, Rectangle r, String titulo, String sub, int i, boolean hover) {
        int lift = hover ? 3 : 0;
        RoundRectangle2D s = new RoundRectangle2D.Double(r.x, r.y - lift, r.width, r.height, 22, 22);
        boolean principal = i == 0;
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
            g.fill(s);
            g.setColor(hover ? Color.YELLOW : Color.WHITE);
            g.setStroke(new BasicStroke(hover ? 3 : 2));
            g.draw(s);
        } else {
            g.setColor(new Color(30, 12, 6, hover ? 110 : 70));
            g.fill(new RoundRectangle2D.Double(r.x + 2, r.y + 5, r.width, r.height, 22, 22));
            Color c1 = principal ? new Color(226, 140, 86) : i == 5 ? new Color(96, 56, 40) : Tema.PAPEL;
            Color c2 = principal ? new Color(176, 84, 44) : i == 5 ? new Color(64, 35, 25) : Tema.ROSA_CLARO;
            if (hover) {
                c1 = Tema.mezclar(c1, Color.WHITE, 0.10);
            }
            g.setPaint(new GradientPaint(r.x, r.y - lift, c1, r.x, r.y + r.height - lift, c2));
            g.fill(s);
            g.setColor(new Color(255, 255, 255, principal ? 34 : i == 5 ? 14 : 90));
            g.fill(new RoundRectangle2D.Double(r.x + 4, r.y - lift + 3, r.width - 8, r.height * 0.36, 16, 16));
            g.setColor(hover ? Tema.DORADO : principal ? new Color(122, 56, 28) : i == 5 ? new Color(150, 96, 70)
                    : new Color(190, 132, 108));
            g.setStroke(new BasicStroke(hover ? 2.4f : 1.5f));
            g.draw(s);
        }
        g.setStroke(new BasicStroke(1));
        boolean claro = !principal && i != 5 && !Tema.altoContraste;
        Color txt = Tema.altoContraste ? (hover ? Color.YELLOW : Color.WHITE) : claro ? Tema.TINTA : Tema.CREMA;
        Color txt2 = Tema.altoContraste ? Color.WHITE : claro ? Tema.TINTA_SUAVE : new Color(255, 226, 200);
        // icono
        double icx = r.x + 34, icy = r.getCenterY() - lift;
        double rad = r.height * 0.32;
        if (!Tema.altoContraste) {
            Color ci = principal ? Tema.CREMA : i == 5 ? Tema.ROSA : i >= 2 && i <= 3 ? Tema.LILA : i == 4 ? Tema.SALVIA : Tema.COBRE;
            g.setColor(principal ? new Color(255, 255, 255, 60) : Tema.alfa(ci, 40));
            g.fill(new Ellipse2D.Double(icx - rad - 4, icy - rad - 4, 2 * rad + 8, 2 * rad + 8));
            g.setColor(principal ? Tema.CREMA : ci);
        } else {
            g.setColor(Color.WHITE);
        }
        icono(g, i, icx, icy, rad);
        // textos
        int tx = r.x + 66;
        if (sub == null && r.width < 300) {
            tx = r.x + 60;
        }
        if (sub == null) {
            g.setFont(Tema.fuente(Font.BOLD, 17));
            g.setColor(txt);
            g.drawString(titulo, tx, (int) (r.getCenterY() - lift + 6));
        } else {
            g.setFont(Tema.fuente(Font.BOLD, 17.5f));
            g.setColor(txt);
            g.drawString(titulo, tx, r.y - lift + 27);
            Font fs = Tema.fuente(Font.PLAIN, 12.5f);
            g.setFont(fs);
            g.setColor(txt2);
            g.drawString(Dibujo.recortar(g.getFontMetrics(), sub, r.width - 90), tx, r.y - lift + 47);
        }
        // flechita
        if (hover) {
            Dibujo.flecha(g, r.x + r.width - 40, r.getCenterY() - lift, r.x + r.width - 22, txt, 2.2f);
        }
    }

    /** Iconos dibujados de cada boton (play, personas, servidor, enchufe, engranaje, puerta). */
    private void icono(Graphics2D g, int i, double cx, double cy, double r) {
        Graphics2D s = (Graphics2D) g.create();
        s.setStroke(new BasicStroke((float) (r * 0.16), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (i) {
            case 0: {
                Path2D p = new Path2D.Double();
                p.moveTo(cx - r * 0.35, cy - r * 0.55);
                p.lineTo(cx + r * 0.6, cy);
                p.lineTo(cx - r * 0.35, cy + r * 0.55);
                p.closePath();
                s.fill(p);
                break;
            }
            case 1:
                s.fill(new Ellipse2D.Double(cx - r * 0.75, cy - r * 0.55, r * 0.5, r * 0.5));
                s.fill(new Ellipse2D.Double(cx + r * 0.2, cy - r * 0.55, r * 0.5, r * 0.5));
                s.draw(new java.awt.geom.Arc2D.Double(cx - r * 0.95, cy, r * 0.9, r * 0.9, 0, 180, java.awt.geom.Arc2D.OPEN));
                s.draw(new java.awt.geom.Arc2D.Double(cx, cy, r * 0.9, r * 0.9, 0, 180, java.awt.geom.Arc2D.OPEN));
                break;
            case 2:
                s.draw(new RoundRectangle2D.Double(cx - r * 0.6, cy - r * 0.6, r * 1.2, r * 0.5, 4, 4));
                s.draw(new RoundRectangle2D.Double(cx - r * 0.6, cy + r * 0.05, r * 1.2, r * 0.5, 4, 4));
                s.fill(new Ellipse2D.Double(cx + r * 0.25, cy - r * 0.42, r * 0.16, r * 0.16));
                s.fill(new Ellipse2D.Double(cx + r * 0.25, cy + r * 0.22, r * 0.16, r * 0.16));
                break;
            case 3:
                s.draw(new java.awt.geom.Arc2D.Double(cx - r * 0.7, cy - r * 0.5, r * 1.4, r * 1.4, 30, 120, java.awt.geom.Arc2D.OPEN));
                s.draw(new java.awt.geom.Arc2D.Double(cx - r * 0.42, cy - r * 0.2, r * 0.84, r * 0.84, 30, 120, java.awt.geom.Arc2D.OPEN));
                s.fill(new Ellipse2D.Double(cx - r * 0.12, cy + r * 0.25, r * 0.24, r * 0.24));
                break;
            case 4:
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    s.draw(new java.awt.geom.Line2D.Double(cx + Math.cos(a) * r * 0.42, cy + Math.sin(a) * r * 0.42,
                            cx + Math.cos(a) * r * 0.68, cy + Math.sin(a) * r * 0.68));
                }
                s.draw(new Ellipse2D.Double(cx - r * 0.42, cy - r * 0.42, r * 0.84, r * 0.84));
                break;
            default:
                s.draw(new RoundRectangle2D.Double(cx - r * 0.45, cy - r * 0.6, r * 0.8, r * 1.2, 4, 4));
                s.fill(new Ellipse2D.Double(cx + r * 0.12, cy - r * 0.05, r * 0.16, r * 0.16));
                break;
        }
        s.dispose();
    }

    // ------------------------------------------------------------------ personajes
    /**
     * La fila de ciudadanos que desfila. El MOVIMIENTO es el mismo de antes
     * (misma formula de x, misma altura, mismas expresiones y parpadeo); solo
     * se mejoro el aspecto: sombra en el piso, halo calido detras, borde de luz
     * y una burbuja de Civitas que flota sobre algunos.
     */
    private void personajes(Graphics2D g, int w, int h) {
        double tt = t;     // t avanza 1 por segundo, igual que antes (0,04 cada 40 ms)
        for (int i = 0; i < caras.length; i++) {
            double x = ((i * 230 + tt * 40) % (w + 300)) - 150;
            double y = h - 170;
            if (!Tema.altoContraste) {
                // halo calido detras
                g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y + 20), 120f, new float[]{0f, 1f},
                        new Color[]{new Color(255, 200, 140, 70), new Color(255, 200, 140, 0)}));
                g.fill(new Ellipse2D.Double(x - 120, y - 100, 240, 240));
                // sombra en el piso
                g.setColor(new Color(20, 8, 4, 90));
                g.fill(new Ellipse2D.Double(x - 70, h - 20, 140, 22));
            }
            Caras.dibujar(g, caras[i], x, y, 0.9, i % 3 == 0 ? "nervioso" : "neutral", ((int) (tt * 3 + i)) % 17 == 0, 0);
            // burbuja de reaccion de Civitas (corazon, comentario o chulito)
            if (i % 2 == 0) {
                double by = y - 92 + Math.sin(tt * 2 + i) * 5;
                burbuja(g, x + 38, by, i / 2 % 3);
            }
        }
        // franja del mostrador (antes era negra): madera translucida
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
        } else {
            g.setPaint(new GradientPaint(0, h - 92, new Color(74, 40, 28, 150), 0, h, new Color(42, 23, 18, 235)));
        }
        g.fillRect(0, h - 90, w, 90);
        if (!Tema.altoContraste) {
            g.setColor(new Color(242, 179, 94, 90));
            g.fillRect(0, h - 91, w, 2);
        }
    }

    private void burbuja(Graphics2D g, double cx, double cy, int tipo) {
        RoundRectangle2D b = new RoundRectangle2D.Double(cx - 17, cy - 15, 34, 28, 14, 14);
        g.setColor(Tema.altoContraste ? Color.BLACK : Tema.PAPEL);
        g.fill(b);
        Path2D cola = new Path2D.Double();
        cola.moveTo(cx - 8, cy + 12);
        cola.lineTo(cx - 14, cy + 20);
        cola.lineTo(cx, cy + 12);
        g.fill(cola);
        g.setColor(Tema.altoContraste ? Color.WHITE : Tema.ROSA);
        g.draw(b);
        Color c = tipo == 0 ? Tema.LADRILLO : tipo == 1 ? Tema.LILA : Tema.SALVIA;
        g.setColor(Tema.altoContraste ? Color.YELLOW : c);
        if (tipo == 0) {         // corazon
            Path2D p = new Path2D.Double();
            p.moveTo(cx, cy + 7);
            p.curveTo(cx - 12, cy - 1, cx - 6, cy - 11, cx, cy - 4);
            p.curveTo(cx + 6, cy - 11, cx + 12, cy - 1, cx, cy + 7);
            g.fill(p);
        } else if (tipo == 1) {  // comentario
            for (int k = -1; k <= 1; k++) {
                g.fill(new Ellipse2D.Double(cx + k * 7 - 2.5, cy - 2.5, 5, 5));
            }
        } else {                 // chulito
            g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Double();
            p.moveTo(cx - 7, cy);
            p.lineTo(cx - 2, cy + 5);
            p.lineTo(cx + 8, cy - 6);
            g.draw(p);
            g.setStroke(new BasicStroke(1));
        }
    }

    // ------------------------------------------------------------------ opciones
    private void opciones(Graphics2D g, int w, int h) {
        g.setColor(new Color(20, 8, 4, 170));
        g.fillRect(0, 0, w, h);
        Rectangle r = new Rectangle(w / 2 - 270, h / 2 - 250, 540, 500);
        Dibujo.tarjetaPapel(g, r, 26);
        Dibujo.tituloTarjeta(g, "Opciones y accesibilidad", r.x + 32, r.y + 52, Tema.acento(), Tema.tinta(), 24);
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(Tema.tintaSuave());
        g.drawString("Pensadas para que todas las personas puedan jugar.", r.x + 45, r.y + 78);
        String[][] op = {
            {"Alto contraste", "Colores fuertes sobre negro para baja visión"},
            {"Texto grande", "Todo el texto un 15 % más grande"},
            {"Sonido", "Efectos sintetizados, sin archivos de audio"},
            {"Subtítulos de sonido", "Cada sonido se escribe en pantalla"},
            {"Modo relajado", "Sin reloj: para aprender o leer con calma"}
        };
        boolean[] v = {Tema.altoContraste, Tema.textoGrande, Tema.sonido, Tema.subtitulos, relajado};
        int y = r.y + 104;
        for (int i = 0; i < op.length; i++) {
            Rectangle fila = new Rectangle(r.x + 30, y, r.width - 60, 58);
            boolean hv = encima(fila);
            g.setColor(Tema.altoContraste ? Color.BLACK : hv ? Tema.ROSA_CLARO : new Color(255, 255, 255, 120));
            g.fillRoundRect(fila.x, fila.y, fila.width, fila.height, 16, 16);
            g.setFont(Tema.fuente(Font.BOLD, 15.5f));
            g.setColor(Tema.tinta());
            g.drawString(op[i][0], fila.x + 18, fila.y + 25);
            g.setFont(Tema.fuente(Font.PLAIN, 12));
            g.setColor(Tema.tintaSuave());
            g.drawString(op[i][1], fila.x + 18, fila.y + 44);
            interruptor(g, fila.x + fila.width - 70, fila.y + 16, v[i]);
            final int k = i;
            zona(fila, () -> alternar(k));
            y += 66;
        }
        boton(g, new Rectangle(r.x + r.width / 2 - 100, r.y + r.height - 66, 200, 46), "Listo", Dibujo.Estilo.PRINCIPAL,
                () -> opciones = false);
    }

    private void interruptor(Graphics2D g, int x, int y, boolean on) {
        RoundRectangle2D b = new RoundRectangle2D.Double(x, y, 52, 26, 26, 26);
        g.setColor(Tema.altoContraste ? (on ? Color.YELLOW : Color.DARK_GRAY) : on ? Tema.SALVIA : new Color(200, 170, 158));
        g.fill(b);
        g.setColor(Tema.altoContraste ? Color.WHITE : Color.WHITE);
        g.fill(new Ellipse2D.Double(on ? x + 28 : x + 3, y + 3, 20, 20));
        g.setFont(Tema.fuente(Font.BOLD, 10));
        g.setColor(Tema.altoContraste ? Color.WHITE : Tema.TINTA);
        g.drawString(on ? "SÍ" : "NO", x - 26, y + 18);
    }

    private void alternar(int i) {
        switch (i) {
            case 0: Tema.altoContraste = !Tema.altoContraste; break;
            case 1: Tema.textoGrande = !Tema.textoGrande; break;
            case 2: Tema.sonido = !Tema.sonido; break;
            case 3: Tema.subtitulos = !Tema.subtitulos; break;
            default: relajado = !relajado; break;
        }
    }

    @Override
    protected void tecla(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            opciones = false;
        } else if (!opciones && e.getKeyCode() == KeyEvent.VK_ENTER) {
            acc.jugarSolo();
        }
    }

    public void cerrarOpciones() {
        opciones = false;
    }
}
