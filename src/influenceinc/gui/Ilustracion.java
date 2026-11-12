package influenceinc.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.text.Normalizer;

/**
 * La ILUSTRACION ALUSIVA de cada noticia dentro del celular. Se escoge por
 * las palabras de la publicacion (colegio, agua, votos, pizza...) y se dibuja
 * con Java2D en la paleta del juego, siempre dentro del marco que se le da.
 */
public final class Ilustracion {

    private Ilustracion() {
    }

    private static String sinTildes(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Tema de la ilustracion segun el texto. */
    public static String tema(String texto) {
        String s = sinTildes(texto);
        String[][] reglas = {
            {"pizza", "pizza"}, {"cerebral", "salud"}, {"aguacero", "lluvia"}, {"cafe", "cafe"}, {"energy", "lata"}, {"caiman", "rio"}, {"cano", "rio"},
            {"gas", "alerta"}, {"emergencia", "alerta"}, {"colegio", "colegio"}, {"escuela", "colegio"},
            {"agua", "agua"}, {"inund", "lluvia"}, {"avenida", "lluvia"},
            {"pasaje", "bus"}, {"transporte", "bus"}, {"rutas", "bus"}, {"debate", "debate"},
            {"votos", "urna"}, {"votacion", "urna"}, {"electoral", "urna"}, {"elecciones", "urna"},
            {"impuesto", "dinero"}, {"plata", "dinero"}, {"millones", "casa"}, {"casa", "casa"},
            {"multo", "documento"}, {"resolucion", "documento"}, {"estudio", "salud"}, {"cerebral", "salud"},
            {"policia", "barrio"}, {"barrio", "barrio"}, {"influencer", "opinion"}, {"creo que", "opinion"},
            {"para mi", "opinion"}, {"parque", "parque"}
        };
        for (String[] r : reglas) {
            if (s.contains(r[0])) {
                return r[1];
            }
        }
        return "ciudad";
    }

    /** Dibuja la ilustracion recortada a r. */
    public static void dibujar(Graphics2D g0, Rectangle r, String texto, boolean video, double t) {
        Graphics2D g = (Graphics2D) g0.create();
        RoundRectangle2D marco = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 12, 12);
        g.clip(marco);
        String tema = tema(texto);
        boolean noche = tema.equals("barrio") || tema.equals("alerta");
        Color c1 = noche ? new Color(70, 50, 96) : tema.equals("lluvia") ? new Color(150, 130, 150) : new Color(247, 206, 170);
        Color c2 = noche ? new Color(150, 96, 120) : tema.equals("lluvia") ? new Color(206, 180, 176) : new Color(252, 236, 218);
        if (Tema.altoContraste) {
            c1 = Color.BLACK;
            c2 = new Color(40, 40, 40);
        }
        g.setPaint(new GradientPaint(r.x, r.y, c1, r.x, r.y + r.height, c2));
        g.fill(marco);
        double cx = r.getCenterX(), cy = r.getCenterY(), k = Math.min(r.width, r.height) / 150.0;
        // piso
        g.setColor(Tema.altoContraste ? Color.DARK_GRAY : new Color(217, 178, 166));
        g.fill(new Ellipse2D.Double(r.x - 20, r.y + r.height * 0.78, r.width + 40, r.height * 0.6));
        Color oscuro = Tema.altoContraste ? Color.WHITE : new Color(110, 54, 33);
        Color cobre = Tema.altoContraste ? Color.YELLOW : Tema.COBRE;
        Color crema = Tema.altoContraste ? Color.BLACK : Tema.PAPEL;
        g.setStroke(new BasicStroke((float) (3 * k), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (tema) {
            case "colegio": {
                g.setColor(cobre);
                g.fill(new Rectangle2Dd(cx - 55 * k, cy - 20 * k, 110 * k, 60 * k).r());
                Path2D techo = new Path2D.Double();
                techo.moveTo(cx - 64 * k, cy - 18 * k);
                techo.lineTo(cx, cy - 52 * k);
                techo.lineTo(cx + 64 * k, cy - 18 * k);
                techo.closePath();
                g.setColor(oscuro);
                g.fill(techo);
                g.setColor(crema);
                for (int i = -1; i <= 1; i += 2) {
                    g.fill(new Rectangle2Dd(cx + i * 34 * k - 10 * k, cy - 6 * k, 20 * k, 16 * k).r());
                }
                g.fill(new RoundRectangle2D.Double(cx - 11 * k, cy + 12 * k, 22 * k, 28 * k, 6 * k, 6 * k));
                g.setColor(oscuro);
                g.draw(new Line2D.Double(cx, cy - 52 * k, cx, cy - 74 * k));
                g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.LILA);
                g.fill(new Rectangle2Dd(cx, cy - 74 * k, 20 * k, 12 * k).r());
                break;
            }
            case "agua": {
                g.setColor(Tema.altoContraste ? Color.CYAN : new Color(122, 92, 153));
                Path2D gota = new Path2D.Double();
                gota.moveTo(cx, cy - 48 * k);
                gota.curveTo(cx + 36 * k, cy - 4 * k, cx + 30 * k, cy + 34 * k, cx, cy + 34 * k);
                gota.curveTo(cx - 30 * k, cy + 34 * k, cx - 36 * k, cy - 4 * k, cx, cy - 48 * k);
                g.fill(gota);
                g.setColor(new Color(255, 255, 255, 140));
                g.fill(new Ellipse2D.Double(cx - 14 * k, cy - 4 * k, 9 * k, 16 * k));
                g.setColor(oscuro);
                g.setStroke(new BasicStroke((float) (6 * k), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(cx - 40 * k, cy - 60 * k, cx - 10 * k, cy - 60 * k));
                break;
            }
            case "lluvia": {
                nube(g, cx, cy - 26 * k, k, Tema.altoContraste ? Color.WHITE : new Color(120, 100, 120));
                g.setColor(Tema.altoContraste ? Color.CYAN : Tema.LILA_CLARO);
                for (int i = 0; i < 9; i++) {
                    double x = cx - 50 * k + i * 12 * k, y = cy + ((t * 60 + i * 13) % 40) * k;
                    g.draw(new Line2D.Double(x, y, x - 4 * k, y + 10 * k));
                }
                break;
            }
            case "bus": {
                g.setColor(cobre);
                g.fill(new RoundRectangle2D.Double(cx - 62 * k, cy - 28 * k, 124 * k, 56 * k, 14 * k, 14 * k));
                g.setColor(crema);
                for (int i = 0; i < 4; i++) {
                    g.fill(new RoundRectangle2D.Double(cx - 54 * k + i * 28 * k, cy - 20 * k, 22 * k, 20 * k, 5 * k, 5 * k));
                }
                g.setColor(oscuro);
                g.fill(new Ellipse2D.Double(cx - 46 * k, cy + 18 * k, 20 * k, 20 * k));
                g.fill(new Ellipse2D.Double(cx + 26 * k, cy + 18 * k, 20 * k, 20 * k));
                break;
            }
            case "urna": {
                g.setColor(crema);
                g.fill(new RoundRectangle2D.Double(cx - 50 * k, cy - 18 * k, 100 * k, 62 * k, 10 * k, 10 * k));
                g.setColor(oscuro);
                g.draw(new RoundRectangle2D.Double(cx - 50 * k, cy - 18 * k, 100 * k, 62 * k, 10 * k, 10 * k));
                g.fill(new Rectangle2Dd(cx - 26 * k, cy - 20 * k, 52 * k, 6 * k).r());
                g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.LILA);
                double up = Math.sin(t * 2) * 4 * k;
                g.fill(new Rectangle2Dd(cx - 16 * k, cy - 56 * k + up, 32 * k, 40 * k).r());
                g.setColor(crema);
                g.setStroke(new BasicStroke((float) (4 * k), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D ch = new Path2D.Double();
                ch.moveTo(cx - 8 * k, cy - 38 * k + up);
                ch.lineTo(cx - 2 * k, cy - 31 * k + up);
                ch.lineTo(cx + 9 * k, cy - 46 * k + up);
                g.draw(ch);
                g.setColor(cobre);
                g.setFont(Tema.fuente(Font.BOLD, (float) (13 * k)));
                Dibujo.centrado(g, "VOTO", cx, cy + 22 * k);
                break;
            }
            case "debate": {
                for (int i = -1; i <= 1; i += 2) {
                    g.setColor(i < 0 ? cobre : (Tema.altoContraste ? Color.CYAN : Tema.LILA));
                    g.fill(new Rectangle2Dd(cx + i * 38 * k - 18 * k, cy, 36 * k, 40 * k).r());
                    g.setColor(oscuro);
                    g.fill(new Ellipse2D.Double(cx + i * 38 * k - 11 * k, cy - 34 * k, 22 * k, 22 * k));
                    g.fill(new Arc2D.Double(cx + i * 38 * k - 17 * k, cy - 14 * k, 34 * k, 30 * k, 0, 180, Arc2D.CHORD));
                }
                break;
            }
            case "dinero": {
                for (int i = 0; i < 4; i++) {
                    g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.DORADO);
                    g.fill(new Ellipse2D.Double(cx - 40 * k + i * 4 * k, cy + 10 * k - i * 12 * k, 60 * k, 20 * k));
                    g.setColor(oscuro);
                    g.draw(new Ellipse2D.Double(cx - 40 * k + i * 4 * k, cy + 10 * k - i * 12 * k, 60 * k, 20 * k));
                }
                g.setFont(Tema.fuente(Font.BOLD, (float) (30 * k)));
                g.setColor(oscuro);
                g.drawString("$", (float) (cx + 30 * k), (float) (cy - 18 * k));
                break;
            }
            case "casa": {
                g.setColor(cobre);
                g.fill(new Rectangle2Dd(cx - 40 * k, cy - 10 * k, 80 * k, 50 * k).r());
                Path2D te = new Path2D.Double();
                te.moveTo(cx - 50 * k, cy - 8 * k);
                te.lineTo(cx, cy - 46 * k);
                te.lineTo(cx + 50 * k, cy - 8 * k);
                te.closePath();
                g.setColor(oscuro);
                g.fill(te);
                g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.DORADO);
                g.setFont(Tema.fuente(Font.BOLD, (float) (24 * k)));
                Dibujo.centrado(g, "$$", cx, cy + 28 * k);
                g.setColor(Tema.altoContraste ? Color.WHITE : Tema.LADRILLO);
                g.setFont(Tema.fuente(Font.BOLD, (float) (34 * k)));
                g.drawString("?", (float) (cx + 48 * k), (float) (cy - 30 * k));
                break;
            }
            case "pizza": {
                Path2D p = new Path2D.Double();
                p.moveTo(cx - 50 * k, cy - 30 * k);
                p.lineTo(cx + 50 * k, cy - 30 * k);
                p.lineTo(cx, cy + 46 * k);
                p.closePath();
                g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.DORADO);
                g.fill(p);
                g.setColor(oscuro);
                g.fill(new RoundRectangle2D.Double(cx - 54 * k, cy - 40 * k, 108 * k, 14 * k, 10 * k, 10 * k));
                g.setColor(Tema.altoContraste ? Color.RED : Tema.LADRILLO);
                g.fill(new Ellipse2D.Double(cx - 22 * k, cy - 20 * k, 14 * k, 14 * k));
                g.fill(new Ellipse2D.Double(cx + 8 * k, cy - 16 * k, 12 * k, 12 * k));
                g.fill(new Ellipse2D.Double(cx - 6 * k, cy + 6 * k, 12 * k, 12 * k));
                break;
            }
            case "lata": {
                g.setColor(Tema.altoContraste ? Color.CYAN : Tema.LILA);
                g.fill(new RoundRectangle2D.Double(cx - 24 * k, cy - 46 * k, 48 * k, 88 * k, 12 * k, 12 * k));
                g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.DORADO);
                Path2D ray = new Path2D.Double();
                ray.moveTo(cx + 4 * k, cy - 30 * k);
                ray.lineTo(cx - 10 * k, cy + 2 * k);
                ray.lineTo(cx + 2 * k, cy + 2 * k);
                ray.lineTo(cx - 6 * k, cy + 28 * k);
                ray.lineTo(cx + 14 * k, cy - 6 * k);
                ray.lineTo(cx + 2 * k, cy - 6 * k);
                ray.closePath();
                g.fill(ray);
                g.setColor(crema);
                g.fill(new Rectangle2Dd(cx - 24 * k, cy - 46 * k, 48 * k, 8 * k).r());
                break;
            }
            case "cafe": {
                g.setColor(crema);
                g.fill(new RoundRectangle2D.Double(cx - 34 * k, cy - 20 * k, 60 * k, 56 * k, 16 * k, 16 * k));
                g.setColor(oscuro);
                g.draw(new RoundRectangle2D.Double(cx - 34 * k, cy - 20 * k, 60 * k, 56 * k, 16 * k, 16 * k));
                g.draw(new Arc2D.Double(cx + 16 * k, cy - 6 * k, 26 * k, 26 * k, -90, 180, Arc2D.OPEN));
                g.setColor(cobre);
                for (int i = 0; i < 3; i++) {
                    double x = cx - 18 * k + i * 14 * k;
                    g.draw(new Arc2D.Double(x, cy - 50 * k + Math.sin(t * 3 + i) * 3 * k, 8 * k, 22 * k, 90, 180, Arc2D.OPEN));
                }
                break;
            }
            case "rio": {
                g.setColor(Tema.altoContraste ? Color.CYAN : Tema.LILA_CLARO);
                for (int i = 0; i < 3; i++) {
                    Path2D w = new Path2D.Double();
                    double y = cy + 10 * k + i * 14 * k;
                    w.moveTo(r.x, y);
                    for (int x = 0; x <= r.width; x += 10) {
                        w.lineTo(r.x + x, y + Math.sin(x / 14.0 + t * 2 + i) * 3 * k);
                    }
                    g.draw(w);
                }
                g.setColor(Tema.altoContraste ? Color.GREEN : Tema.SALVIA);
                g.fill(new Ellipse2D.Double(cx - 50 * k, cy - 14 * k, 90 * k, 22 * k));
                g.fill(new Ellipse2D.Double(cx + 30 * k, cy - 20 * k, 30 * k, 16 * k));
                g.setColor(oscuro);
                g.fill(new Ellipse2D.Double(cx + 44 * k, cy - 16 * k, 5 * k, 5 * k));
                break;
            }
            case "alerta": {
                Path2D tri = new Path2D.Double();
                tri.moveTo(cx, cy - 50 * k);
                tri.lineTo(cx + 50 * k, cy + 36 * k);
                tri.lineTo(cx - 50 * k, cy + 36 * k);
                tri.closePath();
                g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.DORADO);
                g.fill(tri);
                g.setColor(oscuro);
                g.draw(tri);
                g.setFont(Tema.fuente(Font.BOLD, (float) (44 * k)));
                Dibujo.centrado(g, "!", cx, cy + 28 * k);
                break;
            }
            case "salud": {
                g.setColor(crema);
                g.fill(new Ellipse2D.Double(cx - 46 * k, cy - 46 * k, 92 * k, 92 * k));
                g.setColor(Tema.altoContraste ? Color.RED : Tema.LADRILLO);
                g.fill(new Rectangle2Dd(cx - 10 * k, cy - 32 * k, 20 * k, 64 * k).r());
                g.fill(new Rectangle2Dd(cx - 32 * k, cy - 10 * k, 64 * k, 20 * k).r());
                break;
            }
            case "documento": {
                g.setColor(crema);
                g.fill(new RoundRectangle2D.Double(cx - 36 * k, cy - 48 * k, 72 * k, 92 * k, 8 * k, 8 * k));
                g.setColor(oscuro);
                for (int i = 0; i < 5; i++) {
                    g.draw(new Line2D.Double(cx - 24 * k, cy - 32 * k + i * 12 * k, cx + 24 * k, cy - 32 * k + i * 12 * k));
                }
                g.setColor(Tema.altoContraste ? Color.RED : Tema.LADRILLO);
                g.draw(new Ellipse2D.Double(cx + 6 * k, cy + 14 * k, 26 * k, 26 * k));
                break;
            }
            case "barrio": {
                g.setColor(Tema.altoContraste ? Color.WHITE : Tema.PAPEL);
                g.fill(new Ellipse2D.Double(r.x + r.width - 50 * k, r.y + 14 * k, 26 * k, 26 * k));
                for (int i = 0; i < 4; i++) {
                    double x = r.x + 10 * k + i * r.width / 4.0, h = (40 + (i * 23) % 30) * k;
                    g.setColor(Tema.altoContraste ? Color.DARK_GRAY : new Color(74, 40, 28));
                    g.fill(new Rectangle2Dd(x, r.y + r.height - 26 * k - h, r.width / 4.0 - 8 * k, h).r());
                    g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.DORADO);
                    g.fill(new Rectangle2Dd(x + 8 * k, r.y + r.height - 16 * k - h, 8 * k, 8 * k).r());
                }
                break;
            }
            case "opinion": {
                g.setColor(crema);
                g.fill(new RoundRectangle2D.Double(cx - 60 * k, cy - 40 * k, 74 * k, 46 * k, 20 * k, 20 * k));
                g.setColor(Tema.altoContraste ? Color.CYAN : Tema.LILA_CLARO);
                g.fill(new RoundRectangle2D.Double(cx - 8 * k, cy - 6 * k, 70 * k, 44 * k, 20 * k, 20 * k));
                g.setColor(oscuro);
                for (int i = 0; i < 3; i++) {
                    g.fill(new Ellipse2D.Double(cx - 42 * k + i * 16 * k, cy - 21 * k, 7 * k, 7 * k));
                    g.fill(new Ellipse2D.Double(cx + 10 * k + i * 16 * k, cy + 13 * k, 7 * k, 7 * k));
                }
                break;
            }
            case "parque": {
                for (int i = -1; i <= 1; i++) {
                    g.setColor(oscuro);
                    g.fill(new Rectangle2Dd(cx + i * 44 * k - 4 * k, cy, 8 * k, 34 * k).r());
                    g.setColor(Tema.altoContraste ? Color.GREEN : Tema.SALVIA);
                    g.fill(new Ellipse2D.Double(cx + i * 44 * k - 24 * k, cy - 40 * k, 48 * k, 48 * k));
                }
                break;
            }
            default: {          // la ciudad con su alcaldia
                for (int i = 0; i < 6; i++) {
                    double x = r.x + i * r.width / 6.0, h = (30 + (i * 37) % 50) * k;
                    g.setColor(Tema.altoContraste ? Color.DARK_GRAY : (i % 2 == 0 ? new Color(110, 54, 33) : new Color(140, 80, 56)));
                    g.fill(new Rectangle2Dd(x, r.y + r.height - 22 * k - h, r.width / 6.0 - 4, h).r());
                }
                g.setColor(cobre);
                g.fill(new Arc2D.Double(cx - 24 * k, cy - 50 * k, 48 * k, 44 * k, 0, 180, Arc2D.CHORD));
                g.fill(new Rectangle2Dd(cx - 30 * k, cy - 28 * k, 60 * k, 40 * k).r());
                break;
            }
        }
        if (video) {
            g.setColor(new Color(20, 8, 4, 110));
            g.fill(marco);
            g.setColor(new Color(255, 245, 235, 230));
            g.fill(new Ellipse2D.Double(cx - 24 * k, cy - 24 * k, 48 * k, 48 * k));
            Path2D play = new Path2D.Double();
            play.moveTo(cx - 7 * k, cy - 12 * k);
            play.lineTo(cx + 13 * k, cy);
            play.lineTo(cx - 7 * k, cy + 12 * k);
            play.closePath();
            g.setColor(Tema.altoContraste ? Color.BLACK : Tema.COBRE);
            g.fill(play);
            g.setFont(Tema.fuente(Font.BOLD, 10));
            g.setColor(Tema.CREMA);
            g.drawString("VIDEO  0:47", r.x + 8, r.y + r.height - 8);
        }
        g.dispose();
    }

    private static void nube(Graphics2D g, double cx, double cy, double k, Color c) {
        g.setColor(c);
        g.fill(new Ellipse2D.Double(cx - 46 * k, cy - 10 * k, 50 * k, 34 * k));
        g.fill(new Ellipse2D.Double(cx - 20 * k, cy - 30 * k, 52 * k, 50 * k));
        g.fill(new Ellipse2D.Double(cx + 10 * k, cy - 12 * k, 44 * k, 36 * k));
    }

    /** Pequeno ayudante para no repetir java.awt.geom.Rectangle2D.Double. */
    private static final class Rectangle2Dd {
        private final double x, y, w, h;

        Rectangle2Dd(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        java.awt.geom.Rectangle2D r() {
            return new java.awt.geom.Rectangle2D.Double(x, y, w, h);
        }
    }
}
