package influenceinc.gui;

import influenceinc.modelo.Rasgos;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Dibuja personas a partir de sus Rasgos, sin imagenes: todo es Java2D.
 * Asi cada partida tiene caras distintas y la foto de la credencial se
 * dibuja con el mismo codigo (si un rasgo cambia, se nota).
 */
public final class Caras {

    static final Color[] PIEL = {new Color(255, 224, 196), new Color(241, 194, 150), new Color(207, 150, 104),
        new Color(160, 105, 70), new Color(104, 66, 45)};
    static final Color[] PELO = {new Color(30, 24, 22), new Color(96, 60, 32), new Color(196, 150, 80),
        new Color(160, 60, 30), new Color(150, 150, 155)};
    static final Color[] OJOS = {new Color(70, 45, 25), new Color(40, 110, 170), new Color(60, 130, 70),
        new Color(30, 30, 30)};
    static final Color[] ROPA = {new Color(60, 90, 160), new Color(170, 60, 60), new Color(60, 140, 100),
        new Color(90, 90, 100), new Color(200, 160, 60), new Color(130, 80, 160), new Color(40, 40, 50)};

    private Caras() {
    }

    /**
     * Dibuja medio cuerpo centrado en (cx, cy) = centro de la cara.
     * @param s       escala (1 = cara de 100 px de alto)
     * @param expr    neutral, feliz, nervioso, enojado, triste, roto
     * @param parpadeo true = ojos cerrados
     * @param habla   0..1 apertura de la boca
     */
    public static void dibujar(Graphics2D g, Rasgos r, double cx, double cy, double s, String expr,
                               boolean parpadeo, double habla) {
        String e = normalizar(expr);
        Color piel = PIEL[r.piel % PIEL.length];
        Color pelo = r.edad == 2 ? PELO[4] : PELO[r.colorPelo % PELO.length];
        Color ropa = ROPA[r.ropa % ROPA.length];
        double w = (r.forma == 0 ? 78 : r.forma == 1 ? 86 : 72) * s;
        double h = (r.forma == 2 ? 108 : 100) * s;
        g.setStroke(new BasicStroke((float) Math.max(1, 2.2 * s)));
        Color linea = new Color(40, 25, 20);

        // cuerpo
        double hombros = 170 * s;
        Path2D cuerpo = new Path2D.Double();
        cuerpo.moveTo(cx - hombros / 2, cy + 190 * s);
        cuerpo.curveTo(cx - hombros / 2, cy + 90 * s, cx - 40 * s, cy + 70 * s, cx, cy + 70 * s);
        cuerpo.curveTo(cx + 40 * s, cy + 70 * s, cx + hombros / 2, cy + 90 * s, cx + hombros / 2, cy + 190 * s);
        cuerpo.closePath();
        g.setPaint(new GradientPaint((float) cx, (float) (cy + 70 * s), ropa.brighter(), (float) cx,
                (float) (cy + 190 * s), ropa.darker()));
        g.fill(cuerpo);
        g.setColor(linea);
        g.draw(cuerpo);
        // cuello
        g.setColor(piel.darker());
        g.fill(new RoundRectangle2D.Double(cx - 14 * s, cy + 38 * s, 28 * s, 38 * s, 10 * s, 10 * s));
        // accesorio de ropa
        if (r.accesorio == 1) {          // corbata
            GeneralPath c = new GeneralPath();
            c.moveTo(cx - 7 * s, cy + 74 * s);
            c.lineTo(cx + 7 * s, cy + 74 * s);
            c.lineTo(cx + 11 * s, cy + 150 * s);
            c.lineTo(cx, cy + 165 * s);
            c.lineTo(cx - 11 * s, cy + 150 * s);
            c.closePath();
            g.setColor(new Color(150, 30, 40));
            g.fill(c);
            g.setColor(linea);
            g.draw(c);
        } else if (r.accesorio == 3) {   // carnet colgado
            g.setColor(new Color(40, 90, 200));
            g.drawLine((int) (cx - 26 * s), (int) (cy + 76 * s), (int) (cx - 10 * s), (int) (cy + 130 * s));
            g.drawLine((int) (cx + 26 * s), (int) (cy + 76 * s), (int) (cx + 10 * s), (int) (cy + 130 * s));
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Double(cx - 16 * s, cy + 128 * s, 32 * s, 40 * s, 6 * s, 6 * s));
            g.setColor(new Color(40, 90, 200));
            g.fill(new RoundRectangle2D.Double(cx - 16 * s, cy + 128 * s, 32 * s, 10 * s, 6 * s, 6 * s));
        }

        // pelo de atras
        g.setColor(pelo);
        if (r.pelo == 2 || r.pelo == 4) {
            g.fill(new RoundRectangle2D.Double(cx - w / 2 - 10 * s, cy - h / 2, w + 20 * s, h * (r.pelo == 4 ? 1.15 : 0.95),
                    40 * s, 40 * s));
        }
        // orejas
        g.setColor(piel);
        g.fill(new Ellipse2D.Double(cx - w / 2 - 9 * s, cy - 8 * s, 18 * s, 26 * s));
        g.fill(new Ellipse2D.Double(cx + w / 2 - 9 * s, cy - 8 * s, 18 * s, 26 * s));
        if (r.accesorio == 2) {          // aretes
            g.setColor(new Color(240, 200, 60));
            g.fill(new Ellipse2D.Double(cx - w / 2 - 5 * s, cy + 16 * s, 8 * s, 8 * s));
            g.fill(new Ellipse2D.Double(cx + w / 2 - 3 * s, cy + 16 * s, 8 * s, 8 * s));
        }
        // cabeza
        Ellipse2D cabeza = new Ellipse2D.Double(cx - w / 2, cy - h / 2, w, h);
        g.setPaint(new GradientPaint((float) (cx - w / 2), (float) cy, piel, (float) (cx + w / 2), (float) cy,
                piel.darker()));
        g.fill(cabeza);
        g.setColor(linea);
        g.draw(cabeza);
        // arrugas (edad)
        if (r.edad == 2) {
            g.setColor(new Color(0, 0, 0, 60));
            g.draw(new Arc2D.Double(cx - 20 * s, cy - 36 * s, 40 * s, 10 * s, 0, 180, Arc2D.OPEN));
            g.draw(new Arc2D.Double(cx - 26 * s, cy + 14 * s, 14 * s, 18 * s, 90, 120, Arc2D.OPEN));
            g.draw(new Arc2D.Double(cx + 12 * s, cy + 14 * s, 14 * s, 18 * s, -30, 120, Arc2D.OPEN));
        }
        // barba
        if (r.barba > 0) {
            g.setColor(new Color(pelo.getRed(), pelo.getGreen(), pelo.getBlue(), r.barba == 1 ? 110 : 230));
            Path2D b = new Path2D.Double();
            b.moveTo(cx - w / 2 + 4 * s, cy + 2 * s);
            b.curveTo(cx - w / 2 + 6 * s, cy + h / 2 + 4 * s, cx + w / 2 - 6 * s, cy + h / 2 + 4 * s,
                    cx + w / 2 - 4 * s, cy + 2 * s);
            b.curveTo(cx + 20 * s, cy + 28 * s, cx - 20 * s, cy + 28 * s, cx - w / 2 + 4 * s, cy + 2 * s);
            g.fill(b);
        }
        // pelo de arriba
        g.setColor(pelo);
        switch (r.pelo) {
            case 0:     // corto
                g.fill(new Arc2D.Double(cx - w / 2 - 2 * s, cy - h / 2 - 6 * s, w + 4 * s, h * 0.7, 0, 180, Arc2D.CHORD));
                break;
            case 1:     // calvo con lados
                g.fill(new Arc2D.Double(cx - w / 2 - 3 * s, cy - 24 * s, 16 * s, 34 * s, 90, 180, Arc2D.CHORD));
                g.fill(new Arc2D.Double(cx + w / 2 - 13 * s, cy - 24 * s, 16 * s, 34 * s, -90, 180, Arc2D.CHORD));
                break;
            case 3:     // afro / rizado
                for (int i = 0; i < 9; i++) {
                    double a = Math.PI + i * Math.PI / 8;
                    g.fill(new Ellipse2D.Double(cx + Math.cos(a) * w * 0.5 - 16 * s, cy - 18 * s + Math.sin(a) * h * 0.5 - 16 * s,
                            32 * s, 32 * s));
                }
                break;
            case 5:     // gorra (los que no quieren que les vean la cara)
                g.setColor(new Color(30, 30, 36));
                g.fill(new Arc2D.Double(cx - w / 2 - 4 * s, cy - h / 2 - 8 * s, w + 8 * s, h * 0.72, 0, 180, Arc2D.CHORD));
                g.fill(new RoundRectangle2D.Double(cx - 10 * s, cy - h * 0.2, w * 0.75, 10 * s, 8 * s, 8 * s));
                break;
            default:    // largo / con flequillo
                g.fill(new Arc2D.Double(cx - w / 2 - 4 * s, cy - h / 2 - 8 * s, w + 8 * s, h * 0.78, 0, 180, Arc2D.CHORD));
                g.fill(new Arc2D.Double(cx - w / 2 - 4 * s, cy - h / 2 + 4 * s, w * 0.7, h * 0.4, 180, 180, Arc2D.CHORD));
                break;
        }

        // ojos
        double ojoY = cy - 4 * s, sep = 18 * s;
        double abre = e.equals("nervioso") ? 1.25 : e.equals("roto") ? 1.4 : e.equals("feliz") ? 0.7 : 1;
        for (int lado = -1; lado <= 1; lado += 2) {
            double ox = cx + lado * sep;
            if (parpadeo) {
                g.setColor(linea);
                g.drawLine((int) (ox - 7 * s), (int) ojoY, (int) (ox + 7 * s), (int) ojoY);
            } else {
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(ox - 8 * s, ojoY - 6 * s * abre, 16 * s, 12 * s * abre));
                g.setColor(OJOS[r.ojos % OJOS.length]);
                double mirar = e.equals("nervioso") ? 3 * s * Math.sin(System.currentTimeMillis() / 300.0) : 0;
                double pr = e.equals("roto") ? 2.5 : 4.5;
                g.fill(new Ellipse2D.Double(ox - pr * s + mirar, ojoY - pr * s, pr * 2 * s, pr * 2 * s));
                g.setColor(Color.BLACK);
                g.fill(new Ellipse2D.Double(ox - 1.8 * s + mirar, ojoY - 1.8 * s, 3.6 * s, 3.6 * s));
            }
            // cejas
            g.setColor(pelo.darker());
            g.setStroke(new BasicStroke((float) ((r.cejas == 1 ? 4.5 : 2.5) * s), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            double inc = e.equals("enojado") ? 5 : e.equals("triste") || e.equals("nervioso") ? -4 : 0;
            double by = ojoY - 13 * s - (e.equals("roto") ? 4 * s : 0);
            if (inc == 0) {
                g.drawLine((int) (ox - 8 * s), (int) by, (int) (ox + 8 * s), (int) by);
            } else {
                // ceja inclinada: el extremo interior sube (triste) o baja (enojado)
                double interior = lado == -1 ? ox + 8 * s : ox - 8 * s;
                double exterior = lado == -1 ? ox - 8 * s : ox + 8 * s;
                g.drawLine((int) exterior, (int) by, (int) interior, (int) (by + inc * s));
            }
            g.setStroke(new BasicStroke((float) Math.max(1, 2.2 * s)));
        }
        // gafas
        if (r.gafas) {
            g.setColor(new Color(20, 20, 20));
            g.setStroke(new BasicStroke((float) (2.6 * s)));
            g.draw(new RoundRectangle2D.Double(cx - sep - 12 * s, ojoY - 10 * s, 24 * s, 20 * s, 8 * s, 8 * s));
            g.draw(new RoundRectangle2D.Double(cx + sep - 12 * s, ojoY - 10 * s, 24 * s, 20 * s, 8 * s, 8 * s));
            g.drawLine((int) (cx - sep + 12 * s), (int) ojoY, (int) (cx + sep - 12 * s), (int) ojoY);
            g.setStroke(new BasicStroke((float) Math.max(1, 2.2 * s)));
        }
        // lunar
        if (r.lunar) {
            g.setColor(new Color(70, 40, 30));
            g.fill(new Ellipse2D.Double(cx + 20 * s, cy + 14 * s, 5 * s, 5 * s));
        }
        // nariz
        g.setColor(piel.darker().darker());
        g.draw(new Arc2D.Double(cx - 6 * s, cy + 2 * s, 12 * s, 14 * s, 200, 140, Arc2D.OPEN));
        // boca
        double by = cy + 26 * s;
        g.setColor(new Color(110, 30, 40));
        if (habla > 0.05) {
            g.fill(new Ellipse2D.Double(cx - 9 * s, by - 3 * s, 18 * s, (4 + 10 * habla) * s));
        } else if (e.equals("feliz")) {
            g.fill(new Arc2D.Double(cx - 14 * s, by - 9 * s, 28 * s, 18 * s, 180, 180, Arc2D.CHORD));
        } else if (e.equals("enojado") || e.equals("triste")) {
            g.setStroke(new BasicStroke((float) (3 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(cx - 12 * s, by, 24 * s, 12 * s, 20, 140, Arc2D.OPEN));
        } else if (e.equals("nervioso")) {
            g.setStroke(new BasicStroke((float) (2.5 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D z = new Path2D.Double();
            z.moveTo(cx - 12 * s, by);
            for (int i = 1; i <= 6; i++) {
                z.lineTo(cx - 12 * s + i * 4 * s, by + (i % 2 == 0 ? 0 : 3 * s));
            }
            g.draw(z);
        } else if (e.equals("roto")) {
            g.setColor(new Color(20, 0, 0));
            g.fill(new Ellipse2D.Double(cx - 12 * s, by - 6 * s, 24 * s, 16 * s));
        } else {
            g.setStroke(new BasicStroke((float) (3 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int) (cx - 10 * s), (int) by, (int) (cx + 10 * s), (int) by);
        }
        g.setStroke(new BasicStroke(1));
        // sudor
        if (e.equals("nervioso") || e.equals("roto")) {
            g.setColor(new Color(140, 200, 255, 200));
            double sy = cy - 30 * s + (System.currentTimeMillis() / 20 % 30) * s;
            g.fill(new Ellipse2D.Double(cx + w / 2 - 12 * s, sy, 6 * s, 9 * s));
        }
    }

    /** Foto tipo carnet (para la credencial). */
    public static BufferedImage foto(Rasgos r, int ancho, int alto) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(190, 215, 235));
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
