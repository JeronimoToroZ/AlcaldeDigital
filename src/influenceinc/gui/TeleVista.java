package influenceinc.gui;

import influenceinc.logica.CiudadEstado;
import influenceinc.logica.Noticiero;
import influenceinc.modelo.Rasgos;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/**
 * El televisor del NOTICIERO: estudio con la presentadora o nota en vivo
 * desde un barrio con el reportero, rotulo (franja inferior), sello "EN VIVO"
 * y cinta de titulares. Solo dibuja; la oficina decide que segmento va.
 */
final class TeleVista {

    private static final Rasgos PRESENTADORA = rasgos(2, 0, 2, 1, 0, 0, 1, 2, 1, false);
    private static final Rasgos REPORTERO = rasgos(1, 1, 0, 0, 3, 1, 0, 3, 0, false);

    private TeleVista() {
    }

    private static Rasgos rasgos(int piel, int forma, int pelo, int colorPelo, int ojos, int barba, int ropa,
                                 int accesorio, int edad, boolean gafas) {
        Rasgos r = new Rasgos();
        r.piel = piel;
        r.forma = forma;
        r.pelo = pelo;
        r.colorPelo = colorPelo;
        r.ojos = ojos;
        r.barba = barba;
        r.ropa = ropa;
        r.accesorio = accesorio;
        r.edad = edad;
        r.gafas = gafas;
        return r;
    }

    /**
     * La pantalla del noticiero (sin marco de televisor: es la "senal" a
     * pantalla completa dentro de la tarjeta del noticiero).
     * @param visibles letras del texto que ya se "dijeron" (maquina de escribir)
     * @param edad     segundos desde que empezo este bloque (para las animaciones)
     * @param noche    edicion de la noche (cambia el cielo de las notas en vivo)
     */
    static void dibujar(Graphics2D g, Rectangle r, Noticiero.Segmento seg, int visibles, double t, double edad,
                        boolean noche) {
        RoundRectangle2D forma = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 22, 22);
        Dibujo.sombra(g, forma, 8, 6);
        Graphics2D s = (Graphics2D) g.create();
        s.clip(forma);
        boolean vivo = seg.zona >= 0;
        boolean habla = visibles < seg.texto.length();
        double boca = habla ? Math.abs(Math.sin(t * 13)) : 0;
        // camara que se acerca despacio durante el bloque
        double zoom = 1 + Math.min(0.06, edad * 0.006);
        Graphics2D cam = (Graphics2D) s.create();
        cam.translate(r.getCenterX(), r.getCenterY());
        cam.scale(zoom, zoom);
        cam.translate(-r.getCenterX(), -r.getCenterY());
        if (vivo) {
            calle(cam, r, seg.zona, noche, t);
            Caras.dibujar(cam, REPORTERO, r.x + r.width * 0.30, r.y + r.height * 0.42, 1.6, "neutral",
                    ((int) (t * 10)) % 37 == 0, boca);
            microfono(cam, r.x + r.width * 0.30 + 70, r.y + r.height * 0.42 + 90);
        } else {
            estudio(cam, r, t, seg.tipo.equals("giro"));
            Caras.dibujar(cam, PRESENTADORA, r.x + r.width * 0.5, r.y + r.height * 0.38, 1.5,
                    seg.tipo.equals("giro") ? "nervioso" : seg.tipo.equals("estudio") ? "feliz" : "neutral",
                    ((int) (t * 10)) % 41 == 0, boca);
            escritorio(cam, r);
        }
        cam.dispose();
        // sello EN VIVO, hora y logo
        double pul = 0.5 + 0.5 * Math.sin(t * 5);
        s.setColor(Tema.LADRILLO);
        s.fillRoundRect(r.x + 22, r.y + 20, 104, 30, 15, 15);
        s.setColor(Tema.alfa(Tema.CREMA, (int) (140 + 115 * pul)));
        s.fill(new Ellipse2D.Double(r.x + 34, r.y + 30, 10, 10));
        s.setFont(Tema.fuente(Font.BOLD, 14));
        s.setColor(Tema.CREMA);
        s.drawString("EN VIVO", r.x + 51, r.y + 41);
        s.setColor(Tema.alfa(Tema.MADERA, 170));
        s.fillRoundRect(r.x + 132, r.y + 20, 66, 30, 15, 15);
        s.setColor(Tema.CREMA);
        s.drawString(noche ? "21:00" : "06:30", r.x + 144, r.y + 41);
        s.setColor(Tema.alfa(Tema.PAPEL, 230));
        s.fill(new Ellipse2D.Double(r.x + r.width - 66, r.y + 18, 44, 44));
        s.setColor(Tema.COBRE);
        s.setFont(Tema.fuente(Font.BOLD, 17));
        Dibujo.centrado(s, "NN", r.x + r.width - 44, r.y + 46);
        // rotulo (franja inferior) que entra deslizandose desde la izquierda
        double entra = Math.min(1, edad / 0.35);
        entra = entra * entra * (3 - 2 * entra);
        int dx = (int) ((1 - entra) * -(r.width));
        int ry = r.y + r.height - 132;
        int rw = r.width - 56;
        s.setPaint(new GradientPaint(r.x, ry, Tema.COBRE, r.x + r.width, ry, Tema.CAOBA));
        s.fillRect(r.x + 28 + dx, ry, rw, 40);
        s.setColor(Tema.DORADO);
        s.fillRect(r.x + 28 + dx, ry, 8, 40);
        s.setFont(Dibujo.ajustar(s, seg.rotulo, Font.BOLD, 20, rw - 60));
        s.setColor(Tema.CREMA);
        s.drawString(seg.rotulo, r.x + 50 + dx, ry + 28);
        double entra2 = Math.min(1, Math.max(0, (edad - 0.15) / 0.35));
        int dx2 = (int) ((1 - entra2 * entra2 * (3 - 2 * entra2)) * -(r.width));
        Rectangle caja = new Rectangle(r.x + 28 + dx2, ry + 40, rw, 78);
        s.setColor(Tema.alfa(Tema.PAPEL, 245));
        s.fillRect(caja.x, caja.y, caja.width, caja.height);
        s.setFont(Tema.fuente(Font.BOLD, 11.5f));
        s.setColor(Tema.COBRE);
        s.drawString((vivo ? "EN VIVO · " : "") + seg.quien.toUpperCase(), caja.x + 22, caja.y + 20);
        s.setColor(Tema.TINTA);
        String txt = seg.texto.substring(0, Math.max(0, Math.min(seg.texto.length(), visibles)));
        Dibujo.parrafo(s, Tema.media(15.5f), txt, caja.x + 22, caja.y + 42, caja.width - 44, 20, caja.y + caja.height - 2);
        // cortinilla de cambio de bloque (cobre que barre la pantalla)
        if (edad < 0.3) {
            double f = edad / 0.3;
            int bx = (int) (r.x + (r.width + 200) * f) - 200;
            s.setPaint(new GradientPaint(bx, 0, Tema.alfa(Tema.COBRE, 0), bx + 200, 0, Tema.alfa(Tema.DORADO, 200)));
            s.fillRect(bx, r.y, 200, r.height);
        }
        // lineas de barrido muy suaves
        s.setColor(new Color(0, 0, 0, 10));
        for (int y = r.y; y < r.y + r.height; y += 3) {
            s.drawLine(r.x, y, r.x + r.width, y);
        }
        s.dispose();
        g.setColor(Tema.alfa(Tema.DORADO, 140));
        g.setStroke(new BasicStroke(2));
        g.draw(forma);
        g.setStroke(new BasicStroke(1));
    }

    private static void estudio(Graphics2D g, Rectangle r, double t, boolean alerta) {
        g.setPaint(new GradientPaint(r.x, r.y, new Color(66, 36, 48), r.x, r.y + r.height, new Color(28, 16, 22)));
        g.fill(r);
        // pantalla gigante detras con la ciudad
        Rectangle p = new Rectangle(r.x + 60, r.y + 58, r.width - 120, (int) (r.height * 0.52));
        g.setPaint(new GradientPaint(p.x, p.y, alerta ? new Color(150, 52, 44) : new Color(110, 70, 120), p.x, p.y + p.height,
                alerta ? new Color(90, 30, 30) : new Color(200, 110, 70)));
        g.fillRoundRect(p.x, p.y, p.width, p.height, 18, 18);
        Random rr = new Random(9);
        for (int i = 0; i < 18; i++) {
            int bw = 30 + rr.nextInt(40), bh = 50 + rr.nextInt(120);
            int bx = p.x + i * (p.width / 16) - 20;
            g.setColor(new Color(40, 20, 30, 150));
            g.fillRect(bx, p.y + p.height - bh, bw, bh);
            g.setColor(new Color(255, 214, 150, 90));
            for (int wy = p.y + p.height - bh + 8; wy < p.y + p.height - 10; wy += 14) {
                if (rr.nextInt(3) > 0) {
                    g.fillRect(bx + 6, wy, 5, 6);
                }
            }
        }
        g.setPaint(new RadialGradientPaint(new Point2D.Double(p.getCenterX(), p.y + p.height * 0.4), (float) (p.width * 0.5),
                new float[]{0f, 1f}, new Color[]{new Color(255, 220, 170, 60), new Color(255, 220, 170, 0)}));
        g.fill(p);
        g.setFont(Tema.fuente(Font.BOLD, 30));
        g.setColor(new Color(255, 255, 255, 40));
        g.drawString(alerta ? "ÚLTIMA HORA" : "NOTICIAS NOVA", p.x + 26, p.y + 50);
        // focos del estudio
        for (int i = 0; i < 5; i++) {
            double fx = r.x + 80 + i * (r.width - 160) / 4.0;
            double a = 0.5 + 0.5 * Math.sin(t * 0.8 + i);
            g.setColor(new Color(255, 230, 190, (int) (30 + 30 * a)));
            g.fill(new Ellipse2D.Double(fx - 8, r.y + 22, 16, 16));
        }
    }

    private static void escritorio(Graphics2D g, Rectangle r) {
        int y = r.y + (int) (r.height * 0.60);
        Path2D d = new Path2D.Double();
        d.moveTo(r.x + r.width * 0.12, y + 20);
        d.quadTo(r.getCenterX(), y - 14, r.x + r.width * 0.88, y + 20);
        d.lineTo(r.x + r.width * 0.84, r.y + r.height);
        d.lineTo(r.x + r.width * 0.16, r.y + r.height);
        d.closePath();
        g.setPaint(new GradientPaint(r.x, y, new Color(150, 84, 52), r.x, r.y + r.height, new Color(74, 40, 28)));
        g.fill(d);
        g.setColor(new Color(242, 179, 94, 160));
        g.setStroke(new BasicStroke(3));
        Path2D borde = new Path2D.Double();
        borde.moveTo(r.x + r.width * 0.12, y + 20);
        borde.quadTo(r.getCenterX(), y - 14, r.x + r.width * 0.88, y + 20);
        g.draw(borde);
        g.setStroke(new BasicStroke(1));
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(new Color(247, 235, 221, 200));
        Dibujo.centrado(g, "N O T I C I A S   N O V A", r.getCenterX(), y + 40);
    }

    private static void calle(Graphics2D g, Rectangle r, int zona, boolean noche, double t) {
        g.setPaint(new GradientPaint(r.x, r.y, noche ? new Color(40, 28, 60) : new Color(240, 186, 140), r.x, r.y + r.height,
                noche ? new Color(100, 60, 90) : new Color(252, 228, 200)));
        g.fill(r);
        Random rr = new Random(31 + zona * 7);
        for (int capa = 0; capa < 2; capa++) {
            for (int i = 0; i < 12; i++) {
                int bw = 60 + rr.nextInt(80), bh = 120 + rr.nextInt(200) - capa * 50;
                int bx = r.x + i * (r.width / 10) - 40 + capa * 30;
                Color cb = capa == 0 ? (noche ? new Color(70, 50, 80) : new Color(214, 160, 140))
                        : (noche ? new Color(50, 30, 44) : new Color(160, 96, 74));
                g.setColor(cb);
                g.fillRect(bx, r.y + (int) (r.height * 0.62) - bh, bw, bh);
                if (capa == 1) {
                    g.setColor(noche ? new Color(255, 214, 140, 180) : new Color(255, 236, 210, 120));
                    for (int wy = r.y + (int) (r.height * 0.62) - bh + 10; wy < r.y + r.height * 0.62 - 14; wy += 20) {
                        for (int wx = bx + 8; wx < bx + bw - 10; wx += 16) {
                            if (rr.nextInt(3) > 0) {
                                g.fillRect(wx, wy, 7, 9);
                            }
                        }
                    }
                }
            }
        }
        int suelo = r.y + (int) (r.height * 0.62);
        g.setColor(noche ? new Color(46, 34, 40) : new Color(120, 82, 66));
        g.fillRect(r.x, suelo, r.width, r.height);
        // letrero del barrio
        String nombre = CiudadEstado.ZONAS[zona];
        int lx = r.x + (int) (r.width * 0.66), ly = suelo - 150;
        g.setColor(new Color(60, 60, 64));
        g.fillRect(lx + 8, ly + 30, 6, 160);
        g.setFont(Tema.fuente(Font.BOLD, 16));
        int lw = g.getFontMetrics().stringWidth(nombre) + 30;
        g.setColor(new Color(84, 140, 70));
        g.fillRoundRect(lx - lw / 2 + 11, ly, lw, 34, 8, 8);
        g.setColor(Color.WHITE);
        g.drawRoundRect(lx - lw / 2 + 13, ly + 2, lw - 4, 30, 6, 6);
        Dibujo.centrado(g, nombre, lx + 11, ly + 23);
        // vecinos curiosos al fondo
        for (int i = 0; i < 5; i++) {
            double px = r.x + r.width * 0.55 + i * 46, py = suelo - 40 + Math.sin(t * 2 + i) * 2;
            g.setColor(new Color(42, 23, 18, 120));
            g.fill(new Ellipse2D.Double(px, py - 30, 22, 26));
            g.fillRoundRect((int) px - 6, (int) py - 6, 34, 70, 16, 16);
        }
    }

    private static void microfono(Graphics2D g, double x, double y) {
        g.setColor(new Color(30, 28, 30));
        g.fill(new RoundRectangle2D.Double(x - 5, y, 10, 70, 6, 6));
        g.setColor(new Color(192, 71, 58));
        g.fill(new RoundRectangle2D.Double(x - 13, y + 8, 26, 20, 4, 4));
        g.setColor(Color.WHITE);
        g.setFont(Tema.fuente(Font.BOLD, 11));
        Dibujo.centrado(g, "NN", x, y + 23);
        g.setColor(new Color(60, 56, 60));
        g.fill(new Ellipse2D.Double(x - 14, y - 22, 28, 30));
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(new Ellipse2D.Double(x - 8, y - 18, 10, 10));
    }
}
