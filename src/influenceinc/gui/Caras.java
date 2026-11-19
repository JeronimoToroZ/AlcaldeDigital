package influenceinc.gui;

import influenceinc.modelo.Rasgos;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Dibuja personas a partir de sus Rasgos, sin imagenes: todo es Java2D.
 * Asi cada partida tiene caras distintas y la foto de la credencial se
 * dibuja con el mismo codigo (si un rasgo cambia, se nota).
 *
 * Version 4: trazo mas fino y calido. La cabeza ya no es una elipse: tiene
 * craneo, mandibula y menton; los ojos tienen parpado, iris con brillo y
 * pestanas; las cejas son formas rellenas; el pelo se recorta contra la
 * cabeza (con Area) para que "abrace" el craneo; la ropa tiene cuello.
 *
 * Las posiciones que usa la lupa NO cambiaron: ojos en (cx +/- 18s, cy - 4s),
 * cejas encima, lunar en (cx + 21s, cy + 15s) y el pelo arriba de cy - 40s.
 */
public final class Caras {

    static final Color[] PIEL = {new Color(255, 223, 196), new Color(240, 192, 152), new Color(206, 148, 106),
        new Color(158, 104, 72), new Color(108, 70, 50)};
    static final Color[] PELO = {new Color(38, 28, 26), new Color(104, 64, 36), new Color(206, 160, 92),
        new Color(168, 70, 40), new Color(176, 172, 172)};
    static final Color[] OJOS = {new Color(92, 58, 32), new Color(52, 116, 170), new Color(70, 132, 78),
        new Color(40, 34, 32)};
    /** Ropa en la paleta del juego: cobre, lila, salvia, gris calido, dorado, rosa y madera. */
    static final Color[] ROPA = {new Color(200, 105, 58), new Color(122, 92, 153), new Color(110, 158, 88),
        new Color(140, 110, 100), new Color(222, 160, 80), new Color(196, 132, 120), new Color(74, 40, 28)};

    private Caras() {
    }

    private static Color mezcla(Color a, Color b, double k) {
        k = Math.max(0, Math.min(1, k));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * k));
    }

    private static Color alfa(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    private static BasicStroke trazo(double w) {
        return new BasicStroke((float) Math.max(0.6, w), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    /**
     * Dibuja medio cuerpo centrado en (cx, cy) = centro de la cara.
     * @param s       escala (1 = cara de 100 px de alto)
     * @param expr    neutral, feliz, nervioso, enojado, triste, roto
     * @param parpadeo true = ojos cerrados
     * @param habla   0..1 apertura de la boca
     */
    public static void dibujar(Graphics2D g0, Rasgos r, double cx, double cy, double s, String expr,
                               boolean parpadeo, double habla) {
        Graphics2D g = (Graphics2D) g0.create();
        String e = normalizar(expr);
        Color piel = PIEL[r.piel % PIEL.length];
        Color pelo = r.edad == 2 ? PELO[4] : PELO[r.colorPelo % PELO.length];
        Color ropa = ROPA[r.ropa % ROPA.length];
        Color linea = alfa(mezcla(piel, new Color(52, 26, 20), 0.72), 215);
        Color sombraPiel = mezcla(piel, new Color(120, 52, 46), 0.28);
        double w = (r.forma == 0 ? 78 : r.forma == 1 ? 86 : 74) * s;
        double h = (r.forma == 2 ? 106 : 100) * s;
        double mandibula = r.forma == 1 ? 1.18 : r.forma == 2 ? 0.86 : 1.0;

        // -------------------------------------------------- cuerpo y ropa
        cuerpo(g, r, cx, cy, s, piel, sombraPiel, ropa, linea);

        // -------------------------------------------------- pelo de atras
        peloAtras(g, r, cx, cy, w, h, s, pelo, linea);

        // -------------------------------------------------- orejas
        for (int lado = -1; lado <= 1; lado += 2) {
            double ox = cx + lado * (w / 2 - 1 * s);
            Ellipse2D oreja = new Ellipse2D.Double(ox - 8 * s, cy - 10 * s, 16 * s, 25 * s);
            g.setColor(piel);
            g.fill(oreja);
            g.setColor(sombraPiel);
            g.setStroke(trazo(1.8 * s));
            g.draw(new Arc2D.Double(ox - 4 * s, cy - 5 * s, 8 * s, 15 * s, lado < 0 ? 90 : -90, 180, Arc2D.OPEN));
            g.setColor(linea);
            g.setStroke(trazo(1.4 * s));
            g.draw(oreja);
            if (r.accesorio == 2) {          // aretes
                g.setColor(new Color(236, 192, 86));
                g.fill(new Ellipse2D.Double(ox - 3.5 * s, cy + 13 * s, 7 * s, 7 * s));
                g.setColor(new Color(255, 236, 170));
                g.fill(new Ellipse2D.Double(ox - 2 * s, cy + 14 * s, 2.5 * s, 2.5 * s));
            }
        }

        // -------------------------------------------------- cabeza
        Shape cabeza = cabeza(cx, cy, w, h, mandibula);
        g.setPaint(new GradientPaint((float) cx, (float) (cy - h / 2), mezcla(piel, Color.WHITE, 0.06),
                (float) cx, (float) (cy + h / 2), piel));
        g.fill(cabeza);
        // volumen: sombra suave de un lado y bajo el pelo
        Graphics2D v = (Graphics2D) g.create();
        v.clip(cabeza);
        v.setPaint(new GradientPaint((float) (cx + w * 0.05), (float) cy, alfa(sombraPiel, 0), (float) (cx + w * 0.5),
                (float) cy, alfa(sombraPiel, 150)));
        v.fill(new Rectangle2D.Double(cx, cy - h, w, 2 * h));
        v.setPaint(new GradientPaint((float) cx, (float) (cy + h * 0.25), alfa(sombraPiel, 0), (float) cx,
                (float) (cy + h * 0.5), alfa(sombraPiel, 110)));
        v.fill(new Rectangle2D.Double(cx - w, cy, 2 * w, h));
        v.setColor(alfa(Color.WHITE, 34));
        v.fill(new Ellipse2D.Double(cx - w * 0.38, cy - h * 0.38, w * 0.36, h * 0.3));
        // rubor
        double rubor = e.equals("feliz") ? 120 : e.equals("nervioso") || e.equals("roto") ? 95 : 60;
        for (int lado = -1; lado <= 1; lado += 2) {
            double bx = cx + lado * 23 * s, byy = cy + 13 * s;
            v.setPaint(new RadialGradientPaint(new Point2D.Double(bx, byy), (float) (11 * s), new float[]{0f, 1f},
                    new Color[]{new Color(236, 110, 110, (int) rubor), new Color(236, 110, 110, 0)}));
            v.fill(new Ellipse2D.Double(bx - 11 * s, byy - 11 * s, 22 * s, 22 * s));
        }
        v.dispose();
        g.setColor(linea);
        g.setStroke(trazo(1.7 * s));
        g.draw(cabeza);

        // -------------------------------------------------- arrugas (edad)
        if (r.edad == 2) {
            g.setColor(alfa(sombraPiel.darker(), 120));
            g.setStroke(trazo(1.2 * s));
            g.draw(new Arc2D.Double(cx - 16 * s, cy - 36 * s, 32 * s, 8 * s, 20, 140, Arc2D.OPEN));
            g.draw(new Arc2D.Double(cx - 12 * s, cy - 31 * s, 24 * s, 6 * s, 20, 140, Arc2D.OPEN));
            g.draw(new Arc2D.Double(cx - 24 * s, cy + 6 * s, 14 * s, 26 * s, 100, 80, Arc2D.OPEN));
            g.draw(new Arc2D.Double(cx + 10 * s, cy + 6 * s, 14 * s, 26 * s, 0, 80, Arc2D.OPEN));
            for (int lado = -1; lado <= 1; lado += 2) {
                double px = cx + lado * 31 * s;
                g.draw(new java.awt.geom.Line2D.Double(px, cy - 6 * s, px + lado * 4 * s, cy - 8 * s));
                g.draw(new java.awt.geom.Line2D.Double(px, cy - 3 * s, px + lado * 4.5 * s, cy - 2 * s));
            }
        }

        // -------------------------------------------------- barba
        if (r.barba > 0) {
            barba(g, r, cx, cy, w, h, s, mandibula, pelo, cabeza);
        }

        // -------------------------------------------------- pelo de adelante
        peloAdelante(g, r, cx, cy, w, h, s, pelo, linea, cabeza);

        // -------------------------------------------------- ojos y cejas
        ojos(g, r, cx, cy, s, e, parpadeo, piel, pelo, linea);

        // -------------------------------------------------- gafas
        if (r.gafas) {
            double ojoY = cy - 4 * s, sep = 18 * s;
            g.setStroke(trazo(2.3 * s));
            for (int lado = -1; lado <= 1; lado += 2) {
                RoundRectangle2D lente = new RoundRectangle2D.Double(cx + lado * sep - 12.5 * s, ojoY - 10 * s,
                        25 * s, 20 * s, 10 * s, 10 * s);
                g.setColor(new Color(255, 255, 255, 40));
                g.fill(lente);
                g.setColor(new Color(255, 255, 255, 110));
                g.setStroke(trazo(1.6 * s));
                g.draw(new java.awt.geom.Line2D.Double(cx + lado * sep - 6 * s, ojoY - 6 * s, cx + lado * sep - 1 * s, ojoY - 9 * s));
                g.setColor(new Color(44, 30, 28));
                g.setStroke(trazo(2.3 * s));
                g.draw(lente);
            }
            g.draw(new Arc2D.Double(cx - sep + 12.5 * s, ojoY - 4 * s, 2 * sep - 25 * s, 6 * s, 0, 180, Arc2D.OPEN));
            g.draw(new java.awt.geom.Line2D.Double(cx - sep - 12.5 * s, ojoY - 3 * s, cx - w / 2 + 2 * s, ojoY - 5 * s));
            g.draw(new java.awt.geom.Line2D.Double(cx + sep + 12.5 * s, ojoY - 3 * s, cx + w / 2 - 2 * s, ojoY - 5 * s));
        }

        // -------------------------------------------------- lunar
        if (r.lunar) {
            g.setColor(new Color(88, 52, 40));
            g.fill(new Ellipse2D.Double(cx + 18.5 * s, cy + 12.5 * s, 5 * s, 5 * s));
        }

        // -------------------------------------------------- nariz
        g.setColor(alfa(sombraPiel.darker(), 150));
        Path2D nariz = new Path2D.Double();
        nariz.moveTo(cx - 1.5 * s, cy + 1 * s);
        nariz.curveTo(cx + 3 * s, cy + 7 * s, cx + 6 * s, cy + 11 * s, cx + 2 * s, cy + 14 * s);
        g.setStroke(trazo(1.6 * s));
        g.draw(nariz);
        g.setColor(alfa(sombraPiel.darker(), 90));
        g.fill(new Ellipse2D.Double(cx - 6 * s, cy + 11.5 * s, 4.5 * s, 3 * s));
        g.fill(new Ellipse2D.Double(cx + 1.5 * s, cy + 11.5 * s, 4.5 * s, 3 * s));
        g.setColor(alfa(Color.WHITE, 70));
        g.fill(new Ellipse2D.Double(cx - 2.5 * s, cy + 6 * s, 3 * s, 4 * s));

        // -------------------------------------------------- boca
        boca(g, cx, cy + 25 * s, s, e, habla, piel, r.barba == 2 ? pelo : null);

        // -------------------------------------------------- sudor
        if (e.equals("nervioso") || e.equals("roto")) {
            double sy = cy - 30 * s + (System.currentTimeMillis() / 20 % 30) * s;
            Path2D gota = new Path2D.Double();
            double gx = cx + w / 2 - 10 * s;
            gota.moveTo(gx, sy);
            gota.curveTo(gx + 4 * s, sy + 6 * s, gx + 4 * s, sy + 10 * s, gx, sy + 10 * s);
            gota.curveTo(gx - 4 * s, sy + 10 * s, gx - 4 * s, sy + 6 * s, gx, sy);
            g.setColor(new Color(170, 214, 245, 220));
            g.fill(gota);
            g.setColor(new Color(255, 255, 255, 200));
            g.fill(new Ellipse2D.Double(gx - 1.6 * s, sy + 5 * s, 1.8 * s, 2.6 * s));
        }
        g.dispose();
    }

    /** Silueta de la cabeza: craneo redondo, mejillas y menton. */
    private static Shape cabeza(double cx, double cy, double w, double h, double mandibula) {
        double top = cy - h / 2, menton = cy + h / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(cx, top);
        p.curveTo(cx + w * 0.40, top, cx + w * 0.5, cy - h * 0.30, cx + w * 0.5, cy - h * 0.04);
        p.curveTo(cx + w * 0.5, cy + h * 0.20, cx + w * 0.30 * mandibula, menton - h * 0.03, cx, menton);
        p.curveTo(cx - w * 0.30 * mandibula, menton - h * 0.03, cx - w * 0.5, cy + h * 0.20, cx - w * 0.5, cy - h * 0.04);
        p.curveTo(cx - w * 0.5, cy - h * 0.30, cx - w * 0.40, top, cx, top);
        p.closePath();
        return p;
    }

    // ==================================================================
    // Cuerpo
    // ==================================================================

    private static void cuerpo(Graphics2D g, Rasgos r, double cx, double cy, double s, Color piel, Color sombraPiel,
                               Color ropa, Color linea) {
        double ab = cy + 200 * s;
        double hom = 88 * s;
        // cuello
        Path2D cuello = new Path2D.Double();
        cuello.moveTo(cx - 13 * s, cy + 34 * s);
        cuello.lineTo(cx + 13 * s, cy + 34 * s);
        cuello.curveTo(cx + 13 * s, cy + 58 * s, cx + 16 * s, cy + 70 * s, cx + 22 * s, cy + 78 * s);
        cuello.lineTo(cx - 22 * s, cy + 78 * s);
        cuello.curveTo(cx - 16 * s, cy + 70 * s, cx - 13 * s, cy + 58 * s, cx - 13 * s, cy + 34 * s);
        cuello.closePath();
        g.setColor(piel);
        g.fill(cuello);
        g.setPaint(new GradientPaint((float) cx, (float) (cy + 36 * s), alfa(sombraPiel.darker(), 170), (float) cx,
                (float) (cy + 54 * s), alfa(sombraPiel, 0)));
        g.fill(cuello);
        // torso
        Path2D t = new Path2D.Double();
        t.moveTo(cx - hom - 6 * s, ab);
        t.curveTo(cx - hom - 4 * s, cy + 120 * s, cx - hom + 6 * s, cy + 86 * s, cx - 30 * s, cy + 76 * s);
        t.lineTo(cx + 30 * s, cy + 76 * s);
        t.curveTo(cx + hom - 6 * s, cy + 86 * s, cx + hom + 4 * s, cy + 120 * s, cx + hom + 6 * s, ab);
        t.closePath();
        boolean saco = r.accesorio == 1;
        Color tela = saco ? mezcla(ropa, new Color(40, 30, 34), 0.35) : ropa;
        g.setPaint(new GradientPaint((float) cx, (float) (cy + 76 * s), mezcla(tela, Color.WHITE, 0.12), (float) cx,
                (float) ab, mezcla(tela, Color.BLACK, 0.22)));
        g.fill(t);
        // pliegues de los hombros
        g.setColor(alfa(Color.BLACK, 38));
        g.setStroke(trazo(2 * s));
        g.draw(new Arc2D.Double(cx - hom + 4 * s, cy + 100 * s, 30 * s, 90 * s, 90, 70, Arc2D.OPEN));
        g.draw(new Arc2D.Double(cx + hom - 34 * s, cy + 100 * s, 30 * s, 90 * s, 20, 70, Arc2D.OPEN));
        if (saco) {
            // camisa blanca en V, solapas y corbata
            Path2D camisa = new Path2D.Double();
            camisa.moveTo(cx - 24 * s, cy + 76 * s);
            camisa.lineTo(cx + 24 * s, cy + 76 * s);
            camisa.lineTo(cx, cy + 150 * s);
            camisa.closePath();
            g.setColor(new Color(250, 246, 240));
            g.fill(camisa);
            Path2D corbata = new Path2D.Double();
            corbata.moveTo(cx - 6 * s, cy + 80 * s);
            corbata.lineTo(cx + 6 * s, cy + 80 * s);
            corbata.lineTo(cx + 4 * s, cy + 88 * s);
            corbata.lineTo(cx + 10 * s, cy + 140 * s);
            corbata.lineTo(cx, cy + 152 * s);
            corbata.lineTo(cx - 10 * s, cy + 140 * s);
            corbata.lineTo(cx - 4 * s, cy + 88 * s);
            corbata.closePath();
            g.setColor(new Color(176, 62, 52));
            g.fill(corbata);
            g.setColor(alfa(Color.BLACK, 50));
            g.draw(new java.awt.geom.Line2D.Double(cx - 4 * s, cy + 88 * s, cx + 4 * s, cy + 88 * s));
            for (int lado = -1; lado <= 1; lado += 2) {
                Path2D sol = new Path2D.Double();
                sol.moveTo(cx + lado * 24 * s, cy + 76 * s);
                sol.lineTo(cx + lado * 34 * s, cy + 82 * s);
                sol.lineTo(cx + lado * 22 * s, cy + 108 * s);
                sol.lineTo(cx + lado * 30 * s, cy + 116 * s);
                sol.lineTo(cx + lado * 2 * s, cy + 160 * s);
                sol.closePath();
                g.setColor(mezcla(tela, Color.WHITE, 0.1));
                g.fill(sol);
                g.setColor(alfa(Color.BLACK, 60));
                g.setStroke(trazo(1.2 * s));
                g.draw(sol);
            }
        } else {
            // cuello de la camisa
            for (int lado = -1; lado <= 1; lado += 2) {
                Path2D cu = new Path2D.Double();
                cu.moveTo(cx + lado * 4 * s, cy + 84 * s);
                cu.lineTo(cx + lado * 26 * s, cy + 74 * s);
                cu.lineTo(cx + lado * 30 * s, cy + 90 * s);
                cu.lineTo(cx + lado * 8 * s, cy + 98 * s);
                cu.closePath();
                g.setColor(mezcla(ropa, Color.WHITE, 0.32));
                g.fill(cu);
                g.setColor(alfa(Color.BLACK, 55));
                g.setStroke(trazo(1.2 * s));
                g.draw(cu);
            }
            g.setColor(alfa(Color.BLACK, 40));
            g.fill(new Ellipse2D.Double(cx - 2 * s, cy + 110 * s, 4 * s, 4 * s));
            g.fill(new Ellipse2D.Double(cx - 2 * s, cy + 132 * s, 4 * s, 4 * s));
        }
        if (r.accesorio == 3) {          // carnet colgado
            g.setColor(new Color(122, 92, 153));
            g.setStroke(trazo(3 * s));
            g.draw(new java.awt.geom.Line2D.Double(cx - 22 * s, cy + 78 * s, cx - 8 * s, cy + 128 * s));
            g.draw(new java.awt.geom.Line2D.Double(cx + 22 * s, cy + 78 * s, cx + 8 * s, cy + 128 * s));
            RoundRectangle2D c = new RoundRectangle2D.Double(cx - 15 * s, cy + 126 * s, 30 * s, 38 * s, 6 * s, 6 * s);
            g.setColor(new Color(252, 248, 242));
            g.fill(c);
            g.setColor(new Color(122, 92, 153));
            g.fill(new RoundRectangle2D.Double(cx - 15 * s, cy + 126 * s, 30 * s, 9 * s, 6 * s, 6 * s));
            g.setColor(new Color(200, 180, 170));
            g.fill(new Rectangle2D.Double(cx - 10 * s, cy + 140 * s, 9 * s, 11 * s));
            g.fill(new Rectangle2D.Double(cx + 2 * s, cy + 141 * s, 8 * s, 2 * s));
            g.fill(new Rectangle2D.Double(cx + 2 * s, cy + 146 * s, 6 * s, 2 * s));
        }
        g.setColor(linea);
        g.setStroke(trazo(1.6 * s));
        g.draw(t);
    }

    // ==================================================================
    // Pelo
    // ==================================================================

    private static void peloAtras(Graphics2D g, Rasgos r, double cx, double cy, double w, double h, double s,
                                  Color pelo, Color linea) {
        Shape atras = null;
        switch (r.pelo) {
            case 2: {          // melena corta (bob)
                Path2D p = new Path2D.Double();
                p.moveTo(cx - w / 2 - 9 * s, cy + h * 0.32);
                p.curveTo(cx - w / 2 - 14 * s, cy - h * 0.2, cx - w * 0.4, cy - h / 2 - 8 * s, cx, cy - h / 2 - 8 * s);
                p.curveTo(cx + w * 0.4, cy - h / 2 - 8 * s, cx + w / 2 + 14 * s, cy - h * 0.2, cx + w / 2 + 9 * s, cy + h * 0.32);
                p.quadTo(cx, cy + h * 0.42, cx - w / 2 - 9 * s, cy + h * 0.32);
                p.closePath();
                atras = p;
                break;
            }
            case 3: {          // afro / rizado: una nube de rizos
                Area a = new Area();
                for (int i = 0; i < 13; i++) {
                    double ang = Math.PI * 0.95 + i * Math.PI * 1.1 / 12;
                    double rx = cx + Math.cos(ang) * w * 0.56, ry = cy - 10 * s + Math.sin(ang) * h * 0.56;
                    a.add(new Area(new Ellipse2D.Double(rx - 17 * s, ry - 17 * s, 34 * s, 34 * s)));
                }
                a.add(new Area(new Ellipse2D.Double(cx - w * 0.62, cy - h * 0.7, w * 1.24, h * 0.95)));
                atras = a;
                break;
            }
            case 4: {          // largo
                Path2D p = new Path2D.Double();
                p.moveTo(cx - w / 2 - 9 * s, cy + h * 0.78);
                p.curveTo(cx - w / 2 - 15 * s, cy + h * 0.2, cx - w / 2 - 14 * s, cy - h / 2 - 6 * s, cx, cy - h / 2 - 9 * s);
                p.curveTo(cx + w / 2 + 14 * s, cy - h / 2 - 6 * s, cx + w / 2 + 15 * s, cy + h * 0.2, cx + w / 2 + 9 * s, cy + h * 0.78);
                p.quadTo(cx + w * 0.25, cy + h * 0.86, cx, cy + h * 0.72);
                p.quadTo(cx - w * 0.25, cy + h * 0.86, cx - w / 2 - 9 * s, cy + h * 0.78);
                p.closePath();
                atras = p;
                break;
            }
            default:
                break;
        }
        if (atras != null) {
            g.setPaint(new GradientPaint((float) cx, (float) (cy - h / 2), mezcla(pelo, Color.WHITE, 0.1), (float) cx,
                    (float) (cy + h), mezcla(pelo, Color.BLACK, 0.25)));
            g.fill(atras);
            g.setColor(linea);
            g.setStroke(trazo(1.5 * s));
            g.draw(atras);
        }
    }

    private static void peloAdelante(Graphics2D g, Rasgos r, double cx, double cy, double w, double h, double s,
                                     Color pelo, Color linea, Shape cabeza) {
        // "casco" que abraza el craneo: la cabeza un poco mas grande y subida
        Area casco = new Area(cabeza(cx, cy - 4 * s, w + 9 * s, h + 8 * s, 1));
        Area forma = null;
        Color oscuro = mezcla(pelo, Color.BLACK, 0.3);
        switch (r.pelo) {
            case 0: {          // corto con raya al lado
                Path2D linea0 = new Path2D.Double();
                linea0.moveTo(cx - w / 2 - 10 * s, cy - h * 0.02);
                linea0.curveTo(cx - w * 0.45, cy - h * 0.30, cx - w * 0.2, cy - h * 0.30, cx - w * 0.08, cy - h * 0.26);
                linea0.curveTo(cx + w * 0.2, cy - h * 0.34, cx + w * 0.45, cy - h * 0.22, cx + w / 2 + 10 * s, cy - h * 0.06);
                linea0.lineTo(cx + w, cy - h);
                linea0.lineTo(cx - w, cy - h);
                linea0.closePath();
                forma = new Area(linea0);
                forma.intersect(casco);
                break;
            }
            case 1: {          // calvo con los lados
                forma = new Area(new Ellipse2D.Double(cx - w * 0.57, cy - h * 0.30, w * 0.2, h * 0.30));
                forma.add(new Area(new Ellipse2D.Double(cx + w * 0.37, cy - h * 0.30, w * 0.2, h * 0.30)));
                forma.intersect(new Area(cabeza(cx, cy - 1 * s, w + 6 * s, h + 4 * s, 1)));
                break;
            }
            case 2: {          // flequillo recto
                Path2D f = new Path2D.Double();
                f.moveTo(cx - w, cy + h * 0.30);
                f.lineTo(cx - w / 2 + 2 * s, cy + h * 0.30);
                f.curveTo(cx - w / 2 + 4 * s, cy - h * 0.05, cx - w * 0.35, cy - h * 0.18, cx - w * 0.2, cy - h * 0.2);
                f.quadTo(cx, cy - h * 0.16, cx + w * 0.2, cy - h * 0.2);
                f.curveTo(cx + w * 0.35, cy - h * 0.18, cx + w / 2 - 4 * s, cy - h * 0.05, cx + w / 2 - 2 * s, cy + h * 0.30);
                f.lineTo(cx + w, cy + h * 0.30);
                f.lineTo(cx + w, cy - h);
                f.lineTo(cx - w, cy - h);
                f.closePath();
                forma = new Area(f);
                Area lados = new Area(cabeza(cx, cy - 2 * s, w + 16 * s, h + 10 * s, 1));
                forma.intersect(lados);
                break;
            }
            case 3: {          // rizos de la frente
                forma = new Area();
                for (int i = 0; i < 7; i++) {
                    double rx = cx - w * 0.42 + i * w * 0.14, ry = cy - h * 0.30 - Math.sin(i * Math.PI / 6) * 5 * s;
                    forma.add(new Area(new Ellipse2D.Double(rx - 10 * s, ry - 10 * s, 20 * s, 20 * s)));
                }
                forma.add(new Area(new Rectangle2D.Double(cx - w, cy - h, 2 * w, h * 0.66)));
                forma.intersect(casco);
                break;
            }
            case 5: {          // gorra: se ven las patillas
                forma = new Area(new Ellipse2D.Double(cx - w * 0.57, cy - h * 0.26, w * 0.2, h * 0.26));
                forma.add(new Area(new Ellipse2D.Double(cx + w * 0.37, cy - h * 0.26, w * 0.2, h * 0.26)));
                forma.intersect(new Area(cabeza(cx, cy - 1 * s, w + 6 * s, h + 4 * s, 1)));
                break;
            }
            default: {         // largo con mechon en diagonal
                Path2D f = new Path2D.Double();
                f.moveTo(cx - w / 2 - 10 * s, cy + h * 0.08);
                f.curveTo(cx - w * 0.42, cy - h * 0.14, cx - w * 0.2, cy - h * 0.2, cx + w * 0.02, cy - h * 0.25);
                f.curveTo(cx + w * 0.22, cy - h * 0.30, cx + w * 0.42, cy - h * 0.24, cx + w / 2 + 10 * s, cy - h * 0.02);
                f.lineTo(cx + w, cy - h);
                f.lineTo(cx - w, cy - h);
                f.closePath();
                forma = new Area(f);
                forma.intersect(casco);
                break;
            }
        }
        if (forma != null && !forma.isEmpty()) {
            g.setPaint(new GradientPaint((float) cx, (float) (cy - h / 2 - 6 * s), mezcla(pelo, Color.WHITE, 0.14),
                    (float) cx, (float) (cy - h * 0.1), pelo));
            g.fill(forma);
            // mechones (textura)
            Graphics2D m = (Graphics2D) g.create();
            m.clip(forma);
            m.setColor(alfa(oscuro, 120));
            m.setStroke(trazo(1.4 * s));
            for (int i = -3; i <= 3; i++) {
                double x0 = cx + i * w * 0.12;
                m.draw(new Arc2D.Double(x0 - 12 * s, cy - h * 0.62, 24 * s, h * 0.5, 200, 60, Arc2D.OPEN));
            }
            m.setColor(alfa(mezcla(pelo, Color.WHITE, 0.5), 70));
            m.setStroke(trazo(3 * s));
            m.draw(new Arc2D.Double(cx - w * 0.3, cy - h * 0.52, w * 0.45, h * 0.3, 110, 60, Arc2D.OPEN));
            m.dispose();
            g.setColor(linea);
            g.setStroke(trazo(1.4 * s));
            g.draw(forma);
        }
        if (r.pelo == 1) {
            // brillo de la cabeza calva
            g.setColor(new Color(255, 255, 255, 60));
            g.fill(new Ellipse2D.Double(cx - w * 0.22, cy - h * 0.46, w * 0.28, h * 0.12));
        }
        if (r.pelo == 5) {
            gorra(g, cx, cy, w, h, s, linea);
        }
    }

    private static void gorra(Graphics2D g, double cx, double cy, double w, double h, double s, Color linea) {
        Color c = new Color(74, 40, 28);
        Area copa = new Area(cabeza(cx, cy - 7 * s, w + 10 * s, h + 10 * s, 1));
        copa.intersect(new Area(new Rectangle2D.Double(cx - w, cy - h, 2 * w, h * 0.78)));
        g.setPaint(new GradientPaint((float) cx, (float) (cy - h / 2), mezcla(c, Color.WHITE, 0.18), (float) cx,
                (float) (cy - h * 0.2), c));
        g.fill(copa);
        g.setColor(alfa(Color.WHITE, 40));
        g.setStroke(trazo(1.2 * s));
        g.draw(new Arc2D.Double(cx - w * 0.15, cy - h * 0.58, w * 0.3, h * 0.6, 60, 60, Arc2D.OPEN));
        // visera
        Path2D v = new Path2D.Double();
        v.moveTo(cx - w * 0.52, cy - h * 0.22);
        v.quadTo(cx, cy - h * 0.31, cx + w * 0.52, cy - h * 0.22);
        v.quadTo(cx + w * 0.62, cy - h * 0.12, cx + w * 0.4, cy - h * 0.1);
        v.quadTo(cx, cy - h * 0.16, cx - w * 0.4, cy - h * 0.1);
        v.quadTo(cx - w * 0.62, cy - h * 0.12, cx - w * 0.52, cy - h * 0.22);
        v.closePath();
        g.setColor(mezcla(c, Color.BLACK, 0.2));
        g.fill(v);
        g.setColor(new Color(242, 179, 94));
        g.fill(new Ellipse2D.Double(cx - 3 * s, cy - h * 0.58, 6 * s, 4 * s));
        g.setColor(linea);
        g.setStroke(trazo(1.4 * s));
        g.draw(copa);
        g.draw(v);
    }

    private static void barba(Graphics2D g, Rasgos r, double cx, double cy, double w, double h, double s,
                              double mandibula, Color pelo, Shape cabeza) {
        Graphics2D b = (Graphics2D) g.create();
        b.clip(cabeza);
        Path2D p = new Path2D.Double();
        p.moveTo(cx - w / 2, cy + 2 * s);
        p.curveTo(cx - w / 2, cy + h * 0.6, cx + w / 2, cy + h * 0.6, cx + w / 2, cy + 2 * s);
        p.curveTo(cx + 22 * s, cy + 30 * s, cx + 12 * s, cy + 18 * s, cx, cy + 19 * s);
        p.curveTo(cx - 12 * s, cy + 18 * s, cx - 22 * s, cy + 30 * s, cx - w / 2, cy + 2 * s);
        p.closePath();
        if (r.barba == 1) {
            // barba de tres dias: puntitos
            b.clip(p);
            b.setColor(alfa(pelo, 120));
            java.util.Random rr = new java.util.Random(7);
            for (int i = 0; i < 220; i++) {
                double x = cx - w / 2 + rr.nextDouble() * w, y = cy + rr.nextDouble() * h * 0.55;
                b.fill(new Ellipse2D.Double(x, y, 1.3 * s, 1.3 * s));
            }
        } else {
            b.setColor(pelo);
            b.fill(p);
            b.setColor(alfa(mezcla(pelo, Color.BLACK, 0.4), 120));
            b.setStroke(trazo(1.2 * s));
            for (int i = -4; i <= 4; i++) {
                b.draw(new Arc2D.Double(cx + i * 7 * s - 6 * s, cy + 22 * s, 12 * s, 26 * s, 230, 80, Arc2D.OPEN));
            }
        }
        b.dispose();
    }

    // ==================================================================
    // Ojos, cejas y boca
    // ==================================================================

    private static void ojos(Graphics2D g, Rasgos r, double cx, double cy, double s, String e, boolean parpadeo,
                             Color piel, Color pelo, Color linea) {
        double ojoY = cy - 4 * s, sep = 18 * s;
        double abre = e.equals("nervioso") ? 1.25 : e.equals("roto") ? 1.4 : e.equals("feliz") ? 0.78 : 1;
        Color pestana = new Color(44, 28, 26);
        double mirar = e.equals("nervioso") ? 2.6 * s * Math.sin(System.currentTimeMillis() / 300.0) : 0;
        for (int lado = -1; lado <= 1; lado += 2) {
            double ox = cx + lado * sep;
            double ew = 8.6 * s, eh = 6.2 * s * abre;
            if (parpadeo) {
                g.setColor(pestana);
                g.setStroke(trazo(2.2 * s));
                g.draw(new Arc2D.Double(ox - ew, ojoY - 3 * s, 2 * ew, 7 * s, 200, 140, Arc2D.OPEN));
            } else {
                Area ojo = new Area(new Ellipse2D.Double(ox - ew, ojoY - eh, 2 * ew, 2 * eh));
                if (e.equals("enojado")) {
                    // el parpado de arriba baja hacia el centro
                    Path2D p = new Path2D.Double();
                    p.moveTo(ox - ew - 2 * s, ojoY - eh - 4 * s);
                    p.lineTo(ox + ew + 2 * s, ojoY - eh - 4 * s);
                    double interior = lado < 0 ? ox + ew : ox - ew;
                    double exterior = lado < 0 ? ox - ew : ox + ew;
                    p.lineTo(interior + lado * -2 * s, ojoY - eh * 0.1);
                    p.lineTo(exterior, ojoY - eh * 0.75);
                    p.closePath();
                    ojo.subtract(new Area(p));
                } else if (e.equals("feliz")) {
                    ojo.subtract(new Area(new Ellipse2D.Double(ox - ew * 1.4, ojoY + eh * 0.25, ew * 2.8, eh * 2)));
                } else if (e.equals("triste")) {
                    Path2D p = new Path2D.Double();
                    double interior = lado < 0 ? ox + ew : ox - ew;
                    double exterior = lado < 0 ? ox - ew : ox + ew;
                    p.moveTo(interior, ojoY - eh - 4 * s);
                    p.lineTo(exterior + lado * 3 * s, ojoY - eh - 4 * s);
                    p.lineTo(exterior + lado * 3 * s, ojoY - eh * 0.25);
                    p.closePath();
                    ojo.subtract(new Area(p));
                }
                g.setColor(new Color(253, 250, 246));
                g.fill(ojo);
                Graphics2D io = (Graphics2D) g.create();
                io.clip(ojo);
                // sombra del parpado sobre el ojo
                io.setColor(alfa(mezcla(piel, Color.BLACK, 0.4), 70));
                io.fill(new Ellipse2D.Double(ox - ew, ojoY - eh - 3 * s, 2 * ew, eh * 0.9));
                double ir = e.equals("roto") ? 3.6 * s : 5.1 * s;
                double ix = ox + mirar, iy = ojoY + 0.4 * s;
                Color iris = OJOS[r.ojos % OJOS.length];
                io.setPaint(new RadialGradientPaint(new Point2D.Double(ix, iy + ir * 0.3), (float) ir, new float[]{0f, 1f},
                        new Color[]{mezcla(iris, Color.WHITE, 0.25), mezcla(iris, Color.BLACK, 0.3)}));
                io.fill(new Ellipse2D.Double(ix - ir, iy - ir, 2 * ir, 2 * ir));
                io.setColor(new Color(20, 14, 14));
                double pr = e.equals("roto") ? 1.3 * s : e.equals("nervioso") ? 1.8 * s : 2.3 * s;
                io.fill(new Ellipse2D.Double(ix - pr, iy - pr, 2 * pr, 2 * pr));
                io.setColor(Color.WHITE);
                io.fill(new Ellipse2D.Double(ix - ir * 0.55, iy - ir * 0.6, ir * 0.55, ir * 0.55));
                io.fill(new Ellipse2D.Double(ix + ir * 0.3, iy + ir * 0.25, ir * 0.22, ir * 0.22));
                io.dispose();
                // parpado de arriba con pestanas
                g.setColor(pestana);
                g.setStroke(trazo(2.2 * s));
                Path2D sup = new Path2D.Double();
                double top = e.equals("enojado") ? ojoY - eh * 0.45 : ojoY - eh;
                sup.moveTo(ox - ew - 0.5 * s, ojoY + 0.5 * s);
                sup.curveTo(ox - ew * 0.6, top - 0.5 * s, ox + ew * 0.6, top - 0.5 * s, ox + ew + 0.5 * s, ojoY + 0.5 * s);
                g.draw(sup);
                double ex = ox + lado * ew;
                g.draw(new java.awt.geom.Line2D.Double(ex, ojoY - 0.5 * s, ex + lado * 2.8 * s, ojoY - 2.6 * s));
                g.setColor(alfa(linea, 90));
                g.setStroke(trazo(1 * s));
                g.draw(new Arc2D.Double(ox - ew * 0.8, ojoY - eh * 0.6, ew * 1.6, eh * 1.6, 210, 120, Arc2D.OPEN));
            }
            // ceja: forma rellena que se afina hacia afuera
            double gro = r.cejas == 1 ? 3.6 * s : 2.3 * s;
            double inc = e.equals("enojado") ? 5 : e.equals("triste") || e.equals("nervioso") ? -4.5 : e.equals("feliz") ? -1 : 0;
            double by = ojoY - 13.5 * s - (e.equals("roto") ? 4 * s : 0) - (e.equals("feliz") ? 1.5 * s : 0);
            double interior = lado < 0 ? ox + 9 * s : ox - 9 * s;
            double exterior = lado < 0 ? ox - 10 * s : ox + 10 * s;
            double yi = by + inc * s, ye = by + 1 * s;
            Path2D ce = new Path2D.Double();
            ce.moveTo(interior, yi - gro / 2);
            ce.quadTo((interior + exterior) / 2, Math.min(yi, ye) - gro * 0.9 - 1.5 * s, exterior, ye - gro * 0.15);
            ce.quadTo((interior + exterior) / 2, Math.min(yi, ye) + gro * 0.2 - 1 * s, interior, yi + gro / 2);
            ce.closePath();
            g.setColor(mezcla(pelo, Color.BLACK, 0.35));
            g.fill(ce);
        }
    }

    private static void boca(Graphics2D g, double cx, double by, double s, String e, double habla, Color piel, Color bigote) {
        Color labio = mezcla(piel, new Color(178, 78, 82), 0.5);
        Color interior = new Color(96, 30, 38);
        g.setStroke(trazo(2.2 * s));
        if (habla > 0.05) {
            double ab = (4 + 9 * habla) * s;
            Ellipse2D b = new Ellipse2D.Double(cx - 9 * s, by - 3 * s, 18 * s, ab);
            g.setColor(interior);
            g.fill(b);
            Graphics2D c = (Graphics2D) g.create();
            c.clip(b);
            c.setColor(new Color(250, 246, 240));
            c.fill(new Rectangle2D.Double(cx - 9 * s, by - 3 * s, 18 * s, 2.8 * s));
            c.setColor(new Color(214, 104, 112));
            c.fill(new Ellipse2D.Double(cx - 6 * s, by - 3 * s + ab * 0.6, 12 * s, ab * 0.7));
            c.dispose();
            g.setColor(labio);
            g.draw(b);
        } else if (e.equals("feliz")) {
            Path2D p = new Path2D.Double();
            p.moveTo(cx - 13 * s, by - 3 * s);
            p.quadTo(cx, by - 1 * s, cx + 13 * s, by - 3 * s);
            p.quadTo(cx, by + 15 * s, cx - 13 * s, by - 3 * s);
            p.closePath();
            g.setColor(interior);
            g.fill(p);
            Graphics2D c = (Graphics2D) g.create();
            c.clip(p);
            c.setColor(new Color(250, 246, 240));
            c.fill(new Rectangle2D.Double(cx - 13 * s, by - 4 * s, 26 * s, 4.5 * s));
            c.setColor(new Color(214, 104, 112));
            c.fill(new Ellipse2D.Double(cx - 7 * s, by + 4 * s, 14 * s, 8 * s));
            c.dispose();
            g.setColor(labio);
            g.draw(p);
        } else if (e.equals("enojado") || e.equals("triste")) {
            g.setColor(labio);
            g.setStroke(trazo(3 * s));
            double k = e.equals("enojado") ? 6 : 9;
            g.draw(new Arc2D.Double(cx - 11 * s, by + 1 * s, 22 * s, k * s, 25, 130, Arc2D.OPEN));
        } else if (e.equals("nervioso")) {
            g.setColor(labio);
            g.setStroke(trazo(2.4 * s));
            Path2D z = new Path2D.Double();
            z.moveTo(cx - 11 * s, by + 1 * s);
            for (int i = 1; i <= 6; i++) {
                z.lineTo(cx - 11 * s + i * 3.7 * s, by + 1 * s + (i % 2 == 0 ? 0 : 2.6 * s));
            }
            g.draw(z);
        } else if (e.equals("roto")) {
            g.setColor(new Color(30, 6, 8));
            g.fill(new Ellipse2D.Double(cx - 11 * s, by - 6 * s, 22 * s, 15 * s));
            g.setColor(labio);
            g.draw(new Ellipse2D.Double(cx - 11 * s, by - 6 * s, 22 * s, 15 * s));
        } else {
            // neutral: media sonrisa tranquila con labio de abajo
            g.setColor(labio);
            g.setStroke(trazo(2.4 * s));
            Path2D p = new Path2D.Double();
            p.moveTo(cx - 10 * s, by - 0.5 * s);
            p.quadTo(cx, by + 3 * s, cx + 10 * s, by - 1 * s);
            g.draw(p);
            g.setColor(alfa(labio, 120));
            g.fill(new Ellipse2D.Double(cx - 5 * s, by + 2.6 * s, 10 * s, 3.4 * s));
        }
        if (bigote != null) {
            g.setColor(bigote);
            Path2D m = new Path2D.Double();
            m.moveTo(cx, by - 6 * s);
            m.curveTo(cx - 8 * s, by - 9 * s, cx - 16 * s, by - 5 * s, cx - 14 * s, by - 1 * s);
            m.curveTo(cx - 8 * s, by - 4 * s, cx - 4 * s, by - 3 * s, cx, by - 4 * s);
            m.curveTo(cx + 4 * s, by - 3 * s, cx + 8 * s, by - 4 * s, cx + 14 * s, by - 1 * s);
            m.curveTo(cx + 16 * s, by - 5 * s, cx + 8 * s, by - 9 * s, cx, by - 6 * s);
            m.closePath();
            g.fill(m);
        }
    }

    /** Foto tipo carnet (para la credencial). */
    public static BufferedImage foto(Rasgos r, int ancho, int alto) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(240, 220, 212), 0, alto, new Color(222, 196, 186)));
        g.fillRect(0, 0, ancho, alto);
        double s = alto / 230.0;
        dibujar(g, r, ancho / 2.0, alto * 0.42, s, "neutral", false, 0);
        g.dispose();
        return img;
    }

    /** Agrupa las variantes de la historia (nerviosa, enojada...) en 6 expresiones. */
    public static String normalizar(String e) {
        if (e == null) {
            return "neutral";
        }
        if (e.startsWith("nervios")) {
            return "nervioso";
        }
        if (e.startsWith("enojad")) {
            return "enojado";
        }
        if (e.startsWith("trist")) {
            return "triste";
        }
        if (e.startsWith("feliz")) {
            return "feliz";
        }
        if (e.startsWith("roto")) {
            return "roto";
        }
        return "neutral";
    }
}
