package influenceinc.gui;

import influenceinc.modelo.RolJugador;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Utilidades de dibujo con la paleta y la tipografia del juego: botones,
 * tarjetas con sombra, parrafos, iconos de los roles... Todas las pantallas
 * las usan, asi el juego se ve igual en todas partes.
 */
public final class Dibujo {

    /** Estilos de boton. */
    public enum Estilo { PRINCIPAL, SECUNDARIO, PELIGRO, EXITO, LILA, OSCURO }

    private Dibujo() {
    }

    // ------------------------------------------------------------------ superficies
    public static void sombra(Graphics2D g, Shape s, int capas, int dy) {
        if (Tema.altoContraste) {
            return;
        }
        Graphics2D c = (Graphics2D) g.create();
        for (int i = capas; i >= 1; i--) {
            c.setColor(new Color(30, 12, 6, 18));
            c.translate(0, 0);
            Rectangle b = s.getBounds();
            c.fill(new RoundRectangle2D.Double(b.x - i, b.y - i + dy, b.width + 2 * i, b.height + 2 * i, 24 + i * 2,
                    24 + i * 2));
        }
        c.dispose();
    }

    /** Tarjeta oscura (madera) con sombra y borde suave. */
    public static void tarjetaOscura(Graphics2D g, Rectangle r, int radio) {
        RoundRectangle2D s = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, radio, radio);
        sombra(g, s, 6, 4);
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
            g.fill(s);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.draw(s);
            g.setStroke(new BasicStroke(1));
            return;
        }
        g.setPaint(new GradientPaint(r.x, r.y, new Color(78, 44, 31, 245), r.x, r.y + r.height, new Color(50, 27, 20, 245)));
        g.fill(s);
        g.setColor(new Color(255, 225, 200, 40));
        g.draw(new RoundRectangle2D.Double(r.x + 1, r.y + 1, r.width - 2, r.height - 2, radio, radio));
        g.setColor(new Color(20, 8, 4, 120));
        g.draw(s);
    }

    /** Tarjeta clara (papel crema) con sombra. */
    public static void tarjetaPapel(Graphics2D g, Rectangle r, int radio) {
        RoundRectangle2D s = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, radio, radio);
        sombra(g, s, 6, 4);
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
            g.fill(s);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.draw(s);
            g.setStroke(new BasicStroke(1));
            return;
        }
        g.setPaint(new GradientPaint(r.x, r.y, Tema.PAPEL, r.x, r.y + r.height, Tema.CREMA));
        g.fill(s);
        g.setColor(new Color(170, 120, 96, 150));
        g.draw(s);
    }

    /** Titulo de tarjeta con una pequena barra de color a la izquierda. */
    public static void tituloTarjeta(Graphics2D g, String titulo, int x, int y, Color barra, Color texto, float tam) {
        g.setColor(barra);
        g.fillRoundRect(x, y - (int) (tam * 0.85), 5, (int) (tam * 1.05), 4, 4);
        g.setFont(Tema.fuente(Font.BOLD, tam));
        g.setColor(texto);
        g.drawString(titulo, x + 13, y);
    }

    // ------------------------------------------------------------------ botones
    public static Color[] colores(Estilo e) {
        if (Tema.altoContraste) {
            return new Color[]{Color.BLACK, Color.BLACK, Color.WHITE, Color.WHITE};
        }
        switch (e) {
            case SECUNDARIO:
                return new Color[]{Tema.PAPEL, Tema.ROSA_CLARO, Tema.TINTA, new Color(176, 120, 96)};
            case PELIGRO:
                return new Color[]{new Color(212, 96, 80), new Color(160, 52, 42), Tema.CREMA, new Color(120, 36, 28)};
            case EXITO:
                return new Color[]{new Color(132, 180, 106), new Color(84, 128, 66), Tema.CREMA, new Color(60, 96, 46)};
            case LILA:
                return new Color[]{new Color(150, 120, 182), Tema.LILA, Tema.CREMA, new Color(84, 60, 110)};
            case OSCURO:
                return new Color[]{new Color(96, 56, 40), new Color(64, 35, 25), Tema.CREMA, new Color(150, 96, 70)};
            default:
                return new Color[]{new Color(222, 132, 80), new Color(170, 80, 42), Tema.CREMA, new Color(122, 56, 28)};
        }
    }

    /** Boton tipo "pildora": degradado, borde, sombra y elevacion al pasar el mouse. */
    public static void boton(Graphics2D g, Rectangle r, String txt, Estilo estilo, boolean hover, boolean activo) {
        Color[] c = colores(estilo);
        int lift = hover ? 2 : 0;
        double rad = Math.min(r.height, 26);
        RoundRectangle2D s = new RoundRectangle2D.Double(r.x, r.y - lift, r.width, r.height, rad, rad);
        if (!Tema.altoContraste) {
            g.setColor(new Color(30, 12, 6, hover ? 70 : 45));
            g.fill(new RoundRectangle2D.Double(r.x + 1, r.y + 3, r.width, r.height, rad, rad));
        }
        Composite antes = g.getComposite();
        if (!activo) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        }
        Color a = hover ? Tema.mezclar(c[0], Color.WHITE, 0.12) : c[0];
        g.setPaint(new GradientPaint(r.x, r.y - lift, a, r.x, r.y + r.height - lift, c[1]));
        g.fill(s);
        if (!Tema.altoContraste) {
            g.setColor(new Color(255, 255, 255, estilo == Estilo.SECUNDARIO ? 70 : 28));
            g.fill(new RoundRectangle2D.Double(r.x + 3, r.y - lift + 2, r.width - 6, r.height * 0.38, rad - 4, rad - 4));
        }
        g.setColor(hover && Tema.altoContraste ? Color.YELLOW : c[3]);
        g.setStroke(new BasicStroke(hover ? 2f : 1.4f));
        g.draw(s);
        g.setStroke(new BasicStroke(1));
        float tam = Math.max(11f, Math.min(17f, r.height * 0.36f));
        Font f = Tema.fuente(Font.BOLD, tam);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        String sTxt = recortar(fm, txt, r.width - 22);
        g.setColor(Tema.altoContraste ? (hover ? Color.YELLOW : Color.WHITE) : c[2]);
        g.drawString(sTxt, r.x + (r.width - fm.stringWidth(sTxt)) / 2f,
                r.y - lift + (r.height + fm.getAscent() - fm.getDescent()) / 2f);
        g.setComposite(antes);
    }

    /** Boton redondo con imagen: crece y brilla al pasar el mouse; muestra su nombre. */
    public static void botonImagen(Graphics2D g, String imagen, Rectangle r, String etiqueta, boolean hover, double t) {
        double k = hover ? 1.08 : 1.0;
        double w = r.width * k, h = r.height * k;
        double x = r.getCenterX() - w / 2, y = r.getCenterY() - h / 2;
        if (hover && !Tema.altoContraste) {
            g.setColor(Tema.alfa(Tema.DORADO, 90));
            g.fill(new Ellipse2D.Double(x - 4, y - 4, w + 8, h + 8));
        }
        if (!Recursos.contener(g, imagen, x, y, w, h)) {
            g.setColor(Tema.ROSA_CLARO);
            g.fill(new Ellipse2D.Double(x, y, w, h));
        }
        if (Tema.altoContraste) {
            g.setColor(hover ? Color.YELLOW : Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.draw(new Ellipse2D.Double(x, y, w, h));
            g.setStroke(new BasicStroke(1));
        }
        if (etiqueta != null && hover) {
            g.setFont(Tema.fuente(Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(etiqueta) + 16;
            int tx = (int) (r.getCenterX() - tw / 2.0), ty = r.y + r.height + 6;
            g.setColor(Tema.altoContraste ? Color.BLACK : new Color(42, 23, 18, 235));
            g.fillRoundRect(tx, ty, tw, 22, 11, 11);
            g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.CREMA);
            g.drawString(etiqueta, tx + 8, ty + 16);
        }
    }

    /** Campo de texto redondeado. */
    public static void campo(Graphics2D g, Lienzo.Campo c, Rectangle r, String titulo, boolean foco, double t) {
        if (titulo != null) {
            g.setFont(Tema.fuente(Font.BOLD, 12.5f));
            g.setColor(Tema.tintaSuave());
            g.drawString(titulo, r.x + 4, r.y - 7);
        }
        RoundRectangle2D s = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 14, 14);
        g.setColor(Tema.altoContraste ? Color.BLACK : Color.WHITE);
        g.fill(s);
        g.setColor(foco ? Tema.acento() : Tema.altoContraste ? Color.WHITE : new Color(200, 160, 140));
        g.setStroke(new BasicStroke(foco ? 2.4f : 1.4f));
        g.draw(s);
        g.setStroke(new BasicStroke(1));
        g.setFont(Tema.fuente(Font.PLAIN, 16));
        FontMetrics fm = g.getFontMetrics();
        int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
        String v = c.texto.toString();
        if (v.isEmpty() && !foco) {
            g.setColor(Tema.alfa(Tema.tintaSuave(), 170));
            g.drawString(c.sugerencia, r.x + 14, ty);
        } else {
            g.setColor(Tema.tinta());
            g.drawString(v, r.x + 14, ty);
            if (foco && ((int) (t * 2.2)) % 2 == 0) {
                int cx = r.x + 15 + fm.stringWidth(v);
                g.fillRect(cx, r.y + 10, 2, r.height - 20);
            }
        }
    }

    // ------------------------------------------------------------------ texto
    public static String recortar(FontMetrics fm, String s, int ancho) {
        if (fm.stringWidth(s) <= ancho) {
            return s;
        }
        String r = s;
        while (r.length() > 1 && fm.stringWidth(r + "…") > ancho) {
            r = r.substring(0, r.length() - 1);
        }
        return r + "…";
    }

    public static void centrado(Graphics2D g, String s, double cx, double y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (float) (cx - fm.stringWidth(s) / 2.0), (float) y);
    }

    /** Escribe un parrafo con ajuste de linea. @return la y siguiente */
    public static int parrafo(Graphics2D g, Font f, String s, int x, int y, int ancho, int alto, int yMax) {
        g.setFont(f);
        List<String> l = Avisos.partir(g, f, s, ancho);
        for (String linea : l) {
            if (y > yMax) {
                break;
            }
            g.drawString(linea, x, y);
            y += alto;
        }
        return y;
    }

    /** Ajusta el tamano de letra para que el texto quepa en el ancho dado. */
    public static Font ajustar(Graphics2D g, String s, int estilo, float tam, int ancho) {
        Font f = Tema.fuente(estilo, tam);
        while (g.getFontMetrics(f).stringWidth(s) > ancho && tam > 8) {
            tam -= 0.5f;
            f = Tema.fuente(estilo, tam);
        }
        return f;
    }

    // ------------------------------------------------------------------ iconos
    /** Triangulo arriba / abajo (los indicadores no dependen solo del color). */
    public static void triangulo(Graphics2D g, double cx, double cy, double r, boolean arriba, Color c) {
        Path2D p = new Path2D.Double();
        if (arriba) {
            p.moveTo(cx, cy - r);
            p.lineTo(cx + r, cy + r * 0.7);
            p.lineTo(cx - r, cy + r * 0.7);
        } else {
            p.moveTo(cx, cy + r);
            p.lineTo(cx + r, cy - r * 0.7);
            p.lineTo(cx - r, cy - r * 0.7);
        }
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    /** Flecha horizontal (la fuente no trae el caracter). */
    public static void flecha(Graphics2D g, double x1, double y, double x2, Color c, float grosor) {
        g.setColor(c);
        g.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Double(x1, y, x2, y));
        double d = x2 > x1 ? -1 : 1;
        Path2D p = new Path2D.Double();
        p.moveTo(x2 + d * grosor * 3, y - grosor * 2.6);
        p.lineTo(x2, y);
        p.lineTo(x2 + d * grosor * 3, y + grosor * 2.6);
        g.draw(p);
        g.setStroke(new BasicStroke(1));
    }

    /** Color de cada rol (siempre dentro de la paleta). */
    public static Color colorRol(RolJugador r) {
        if (Tema.altoContraste) {
            return Color.YELLOW;
        }
        switch (r) {
            case CIUDADANO: return Tema.SALVIA;
            case INFLUENCER: return Tema.LILA;
            case CANDIDATO: return Tema.COBRE;
            default: return new Color(160, 110, 70);
        }
    }

    /** Emblema dibujado de cada rol, dentro de un circulo. */
    public static void emblemaRol(Graphics2D g, RolJugador rol, double cx, double cy, double r, boolean conFondo) {
        Graphics2D s = (Graphics2D) g.create();
        Color c = colorRol(rol);
        if (conFondo) {
            s.setPaint(new GradientPaint((float) cx, (float) (cy - r), Tema.mezclar(c, Color.WHITE, 0.25f), (float) cx,
                    (float) (cy + r), c.darker()));
            s.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            s.setColor(Tema.altoContraste ? Color.WHITE : new Color(60, 30, 20));
            s.setStroke(new BasicStroke((float) Math.max(1.5, r * 0.08)));
            s.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        }
        Color tz = conFondo ? (Tema.altoContraste ? Color.BLACK : Tema.CREMA) : c;
        s.setColor(tz);
        double k = r / 30.0;
        s.setStroke(new BasicStroke((float) (3.2 * k), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (rol) {
            case CIUDADANO: {          // casita con una persona
                Path2D p = new Path2D.Double();
                p.moveTo(cx - 16 * k, cy - 1 * k);
                p.lineTo(cx, cy - 15 * k);
                p.lineTo(cx + 16 * k, cy - 1 * k);
                s.draw(p);
                s.draw(new RoundRectangle2D.Double(cx - 12 * k, cy - 3 * k, 24 * k, 18 * k, 4 * k, 4 * k));
                s.fill(new Ellipse2D.Double(cx - 3.5 * k, cy + 1 * k, 7 * k, 7 * k));
                s.fill(new Arc2D.Double(cx - 6 * k, cy + 7 * k, 12 * k, 12 * k, 0, 180, Arc2D.CHORD));
                break;
            }
            case PERIODISTA: {         // microfono
                s.draw(new RoundRectangle2D.Double(cx - 7 * k, cy - 17 * k, 14 * k, 22 * k, 14 * k, 14 * k));
                s.draw(new Arc2D.Double(cx - 12 * k, cy - 9 * k, 24 * k, 20 * k, 180, 180, Arc2D.OPEN));
                s.draw(new java.awt.geom.Line2D.Double(cx, cy + 11 * k, cx, cy + 17 * k));
                s.draw(new java.awt.geom.Line2D.Double(cx - 7 * k, cy + 17 * k, cx + 7 * k, cy + 17 * k));
                break;
            }
            case INFLUENCER: {         // estrella con ondas
                Path2D p = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double a = -Math.PI / 2 + i * Math.PI / 5, rr = (i % 2 == 0 ? 13 : 5.5) * k;
                    double x = cx - 3 * k + Math.cos(a) * rr, y = cy + 1 * k + Math.sin(a) * rr;
                    if (i == 0) {
                        p.moveTo(x, y);
                    } else {
                        p.lineTo(x, y);
                    }
                }
                p.closePath();
                s.fill(p);
                s.draw(new Arc2D.Double(cx + 2 * k, cy - 12 * k, 16 * k, 16 * k, -50, 100, Arc2D.OPEN));
                s.draw(new Arc2D.Double(cx + 2 * k, cy - 17 * k, 24 * k, 26 * k, -45, 90, Arc2D.OPEN));
                break;
            }
            default: {                 // atril / podio con urna
                Path2D p = new Path2D.Double();
                p.moveTo(cx - 14 * k, cy - 2 * k);
                p.lineTo(cx + 14 * k, cy - 2 * k);
                p.lineTo(cx + 10 * k, cy + 16 * k);
                p.lineTo(cx - 10 * k, cy + 16 * k);
                p.closePath();
                s.draw(p);
                s.draw(new java.awt.geom.Line2D.Double(cx - 5 * k, cy - 2 * k, cx - 5 * k, cy - 12 * k));
                s.fill(new RoundRectangle2D.Double(cx - 3 * k, cy - 17 * k, 14 * k, 9 * k, 3 * k, 3 * k));
                s.fill(new Ellipse2D.Double(cx - 2.5 * k, cy + 4 * k, 5 * k, 5 * k));
                break;
            }
        }
        s.dispose();
    }

    /** Sello circular dibujado (respaldo si falta la imagen). */
    public static void selloRespaldo(Graphics2D g, double cx, double cy, double r, Color c, String txt) {
        g.setColor(Tema.mezclar(c, Color.WHITE, 0.6));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setColor(c);
        g.setStroke(new BasicStroke((float) (r * 0.12)));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setStroke(new BasicStroke(1));
        g.setFont(Tema.fuente(Font.BOLD, (float) (r * 0.32)));
        centrado(g, txt, cx, cy + r * 0.12);
    }
}
