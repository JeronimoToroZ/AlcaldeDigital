package influenceinc.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Iconos dibujados con Java2D. No se usan emojis porque no todas las fuentes
 * de Windows los traen y se verian como cuadritos.
 *
 * Los textos de las herramientas marcan cada linea con un prefijo:
 * "[!] " alerta, "[ok] " confirmado, "[x] " negativo. Aqui se cambian por iconos.
 */
public final class Iconos {

    public static final Color ALERTA = new Color(242, 179, 94);
    public static final Color OK = new Color(126, 176, 96);
    public static final Color NO = new Color(214, 92, 74);

    private Iconos() {
    }

    /** 0 normal, 1 alerta, 2 ok, 3 no. */
    public static int tipo(String s) {
        return s.startsWith("[!]") ? 1 : s.startsWith("[ok]") ? 2 : s.startsWith("[x]") ? 3 : 0;
    }

    public static String sinPrefijo(String s) {
        int t = tipo(s);
        if (t == 0) {
            return s;
        }
        return s.substring(s.indexOf(']') + 1).trim();
    }

    public static Color color(int tipo, Color normal) {
        return tipo == 1 ? ALERTA : tipo == 2 ? OK : tipo == 3 ? NO : normal;
    }

    /** Dibuja el icono de una marca con su centro en (cx, cy). */
    public static void marca(Graphics2D g, int tipo, double cx, double cy, double r) {
        Graphics2D s = (Graphics2D) g.create();
        s.setStroke(new BasicStroke((float) Math.max(1.6, r * 0.32), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (tipo) {
            case 1: {
                Path2D p = new Path2D.Double();
                p.moveTo(cx, cy - r);
                p.lineTo(cx + r, cy + r * 0.8);
                p.lineTo(cx - r, cy + r * 0.8);
                p.closePath();
                s.setColor(ALERTA);
                s.fill(p);
                s.setColor(new Color(59, 34, 25));
                s.setStroke(new BasicStroke((float) Math.max(1.4, r * 0.24)));
                s.drawLine((int) cx, (int) (cy - r * 0.35), (int) cx, (int) (cy + r * 0.2));
                s.fillRect((int) cx - 1, (int) (cy + r * 0.42), 2, 2);
                break;
            }
            case 2:
                s.setColor(OK);
                s.drawLine((int) (cx - r * 0.8), (int) cy, (int) (cx - r * 0.2), (int) (cy + r * 0.6));
                s.drawLine((int) (cx - r * 0.2), (int) (cy + r * 0.6), (int) (cx + r * 0.9), (int) (cy - r * 0.7));
                break;
            case 3:
                s.setColor(NO);
                s.drawLine((int) (cx - r * 0.7), (int) (cy - r * 0.7), (int) (cx + r * 0.7), (int) (cy + r * 0.7));
                s.drawLine((int) (cx - r * 0.7), (int) (cy + r * 0.7), (int) (cx + r * 0.7), (int) (cy - r * 0.7));
                break;
            case 4: {   // estrella (logros)
                Path2D p = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 == 0 ? r : r * 0.45;
                    if (i == 0) {
                        p.moveTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
                    } else {
                        p.lineTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
                    }
                }
                p.closePath();
                s.setColor(new Color(242, 179, 94));
                s.fill(p);
                break;
            }
            default:
                break;
        }
        s.dispose();
    }

    /** Escribe una linea con su icono. @return el ancho usado por el icono */
    public static void linea(Graphics2D g, String s, int x, int y, Color normal) {
        int t = tipo(s);
        if (t == 0) {
            g.setColor(normal);
            g.drawString(s, x, y);
            return;
        }
        int h = g.getFontMetrics().getAscent();
        marca(g, t, x + 6, y - h * 0.38, h * 0.42);
        g.setColor(color(t, normal));
        g.drawString(sinPrefijo(s), x + 16, y);
    }

    /** Candado (herramienta bloqueada). */
    public static void candado(Graphics2D g, double cx, double cy, double r, Color c) {
        Graphics2D s = (Graphics2D) g.create();
        s.setColor(c);
        s.setStroke(new BasicStroke((float) (r * 0.25)));
        s.drawArc((int) (cx - r * 0.55), (int) (cy - r * 1.1), (int) (r * 1.1), (int) (r * 1.2), 0, 180);
        s.fill(new RoundRectangle2D.Double(cx - r * 0.8, cy - r * 0.5, r * 1.6, r * 1.3, r * 0.3, r * 0.3));
        s.dispose();
    }

    /** Icono de cada herramienta, en el orden del enum Herramienta. */
    public static void herramienta(Graphics2D g, int i, double cx, double cy, double r, Color c) {
        Graphics2D s = (Graphics2D) g.create();
        s.setColor(c);
        s.setStroke(new BasicStroke((float) Math.max(1.5, r * 0.18), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (i) {
            case 0:     // perfil: cabeza y hombros
                s.fill(new Ellipse2D.Double(cx - r * 0.35, cy - r * 0.9, r * 0.7, r * 0.7));
                s.fill(new java.awt.geom.Arc2D.Double(cx - r * 0.75, cy - r * 0.1, r * 1.5, r * 1.6, 0, 180,
                        java.awt.geom.Arc2D.CHORD));
                break;
            case 1:     // directorio: libro con renglones
                s.draw(new RoundRectangle2D.Double(cx - r * 0.7, cy - r * 0.85, r * 1.4, r * 1.7, r * 0.2, r * 0.2));
                for (int k = 0; k < 3; k++) {
                    s.drawLine((int) (cx - r * 0.4), (int) (cy - r * 0.4 + k * r * 0.4), (int) (cx + r * 0.4),
                            (int) (cy - r * 0.4 + k * r * 0.4));
                }
                break;
            case 2:     // lupa
                s.draw(new Ellipse2D.Double(cx - r * 0.75, cy - r * 0.75, r * 1.1, r * 1.1));
                s.setStroke(new BasicStroke((float) (r * 0.3), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                s.drawLine((int) (cx + r * 0.2), (int) (cy + r * 0.2), (int) (cx + r * 0.8), (int) (cy + r * 0.8));
                break;
            case 3: {   // imagen: marco con montana
                s.draw(new RoundRectangle2D.Double(cx - r * 0.85, cy - r * 0.7, r * 1.7, r * 1.4, r * 0.2, r * 0.2));
                Path2D m = new Path2D.Double();
                m.moveTo(cx - r * 0.7, cy + r * 0.55);
                m.lineTo(cx - r * 0.15, cy - r * 0.15);
                m.lineTo(cx + r * 0.2, cy + r * 0.25);
                m.lineTo(cx + r * 0.45, cy);
                m.lineTo(cx + r * 0.7, cy + r * 0.55);
                m.closePath();
                s.fill(m);
                s.fill(new Ellipse2D.Double(cx + r * 0.25, cy - r * 0.55, r * 0.3, r * 0.3));
                break;
            }
            case 4:     // corresponsal: moto (dos ruedas)
                s.draw(new Ellipse2D.Double(cx - r * 0.95, cy, r * 0.7, r * 0.7));
                s.draw(new Ellipse2D.Double(cx + r * 0.25, cy, r * 0.7, r * 0.7));
                s.drawLine((int) (cx - r * 0.6), (int) (cy + r * 0.35), (int) (cx), (int) (cy - r * 0.2));
                s.drawLine((int) cx, (int) (cy - r * 0.2), (int) (cx + r * 0.6), (int) (cy + r * 0.35));
                s.drawLine((int) (cx + r * 0.3), (int) (cy - r * 0.6), (int) (cx + r * 0.5), (int) (cy + r * 0.1));
                break;
            default: {  // red: tres nodos conectados
                double[][] p = {{cx, cy - r * 0.7}, {cx - r * 0.75, cy + r * 0.55}, {cx + r * 0.75, cy + r * 0.55}};
                for (int a = 0; a < 3; a++) {
                    s.drawLine((int) p[a][0], (int) p[a][1], (int) p[(a + 1) % 3][0], (int) p[(a + 1) % 3][1]);
                }
                for (double[] q : p) {
                    s.fill(new Ellipse2D.Double(q[0] - r * 0.25, q[1] - r * 0.25, r * 0.5, r * 0.5));
                }
                break;
            }
        }
        s.dispose();
    }
}
