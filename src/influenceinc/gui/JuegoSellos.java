package influenceinc.gui;

import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.PistaSellos;
import influenceinc.modelo.Veredicto;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * PONER UN SELLO (estilo Pump It Up), en un panel pequeno sobre la ventanilla.
 *
 * Arranca enseguida, sin escoger antes. Caen circulos con los sellos del dia
 * por 4 carriles (Z X C V). El primer acierto decide el sello; con 4 aciertos
 * de ese sello se activa. Pisar un icono de otro sello cuesta una vida al
 * instante. Dejar pasar iconos no castiga (margen de 10).
 * La logica esta en logica/PistaSellos; aqui solo se dibuja y se leen teclas.
 */
final class JuegoSellos extends MiniJuego {

    /** Un color de la paleta para cada carril (anillo del receptor). */
    /** Un solo color para todos los circulos: el jugador descubre su sello por la imagen, no por el color. */
    private static final Color COLOR_CIRCULO = Tema.COBRE;
    private static final int ANCHO_CARRIL = 62;
    static final int ANCHO = ANCHO_CARRIL * PistaSellos.CARRILES + 36, ALTO = 438;

    private final PistaSellos pista;
    private final double[] pisado = {-10, -10, -10, -10};
    private double vidaT = -10;
    private boolean ultimaFueAdvertencia;
    private boolean despedido;
    private double finT = -1;

    JuegoSellos(OficinaVista v) {
        super(v);
        pista = j.prepararPista(lineaY() - (cuerpo().y + 6));
        Sonido.tocar(Sonido.Tipo.TURNO);
    }

    @Override
    String titulo() {
        return "Sello";
    }

    @Override
    String subtitulo() {
        return "";
    }

    // ------------------------------------------------------------------ geometria
    private Rectangle panel() {
        return v.zonaPista(ANCHO, ALTO);
    }

    /** Zona donde caen los circulos. */
    private Rectangle cuerpo() {
        Rectangle p = panel();
        return new Rectangle(p.x + 18, p.y + 82, ANCHO_CARRIL * PistaSellos.CARRILES, p.height - 82 - 18);
    }

    private int lineaY() {
        Rectangle c = cuerpo();
        return c.y + c.height - 52;
    }

    private double centroCarril(int i) {
        return cuerpo().x + i * ANCHO_CARRIL + ANCHO_CARRIL / 2.0;
    }

    // ------------------------------------------------------------------ logica
    private void pisar(int c) {
        if (pista.terminado() || despedido || c < 0 || c >= PistaSellos.CARRILES) {
            return;
        }
        pisado[c] = tiempo;
        PistaSellos.Juicio ju = pista.pisar(c);
        if (ju == PistaSellos.Juicio.PERFECTO || ju == PistaSellos.Juicio.BIEN) {
            Sonido.tocar(Sonido.Tipo.TIC);
        } else if (ju == PistaSellos.Juicio.EQUIVOCADO) {
            // la unica forma de perder una vida: pisar un icono de otro sello
            ultimaFueAdvertencia = j.fallarSello();
            vidaT = tiempo;
            Sonido.tocar(Sonido.Tipo.ALERTA);
            v.vidaPerdidaEnPista(ultimaFueAdvertencia);
            if (j.jugador.despedido) {
                despedido = true;
                finT = tiempo;
            }
        }
        revisarFin();
    }

    private void revisarFin() {
        if (finT < 0 && pista.terminado()) {
            finT = tiempo;
            exito = pista.exito();
            if (exito) {
                Sonido.tocar(Sonido.Tipo.SELLO);
            }
        }
    }

    @Override
    void actualizar(double dt) {
        tiempo += dt;
        if (!despedido) {
            pista.actualizar(dt);
            revisarFin();
        }
        if (finT >= 0 && tiempo - finT > (exito ? 0.7 : 1.6)) {
            terminado = true;
        }
    }

    @Override
    boolean presionar(int x, int y) {
        Rectangle c = cuerpo();
        if (x >= c.x && x < c.x + c.width && y > lineaY() - 60 && y < c.y + c.height) {
            pisar((x - c.x) / ANCHO_CARRIL);
            return true;
        }
        return false;      // la X del panel y el celular siguen funcionando
    }

    @Override
    void tecla(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            terminado = true;              // cerrar sin castigo
            return;
        }
        pisar(PistaSellos.TECLAS.indexOf(Character.toUpperCase(e.getKeyChar())));
    }

    @Override
    void cerrar() {
        if (exito) {
            v.selloActivado(pista.getElegido());
        } else if (despedido) {
            v.despedidoEnPista();
        } else {
            v.pistaSinDecidir();
        }
    }

    // ------------------------------------------------------------------ dibujo
    @Override
    void dibujar(Graphics2D g) {
        Rectangle p = panel();
        RoundRectangle2D forma = new RoundRectangle2D.Double(p.x, p.y, p.width, p.height, 26, 26);
        Dibujo.sombra(g, forma, 6, 5);
        g.setPaint(new GradientPaint(p.x, p.y, Tema.altoContraste ? Color.BLACK : Tema.MADERA_MEDIA, p.x, p.y + p.height,
                Tema.altoContraste ? Color.BLACK : Tema.MADERA));
        g.fill(forma);
        g.setColor(Tema.altoContraste ? Color.WHITE : Tema.alfa(Tema.DORADO, 110));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(forma);
        g.setStroke(new BasicStroke(1));
        encabezado(g, p);
        carriles(g);
        if (finT >= 0) {
            cierre(g, p);
        }
        botonCerrar(p);
    }

    /** La X de arriba cierra la pista sin castigo. */
    private void botonCerrar(Rectangle p) {
        v.zona(new Rectangle(p.x + p.width - 34, p.y + 10, 24, 24), () -> terminado = true);
    }

    private void encabezado(Graphics2D g, Rectangle p) {
        Veredicto el = pista.getElegido();
        g.setFont(Tema.fuente(Font.BOLD, 11));
        g.setColor(Tema.textoSuave());
        g.drawString(el == null ? "TU SELLO" : "TU SELLO: " + el.sello, p.x + 18, p.y + 26);
        // cerrar (sin castigo)
        g.setColor(Tema.alfa(Tema.CREMA, 120));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(p.x + p.width - 28, p.y + 16, p.x + p.width - 18, p.y + 26);
        g.drawLine(p.x + p.width - 18, p.y + 16, p.x + p.width - 28, p.y + 26);
        g.setStroke(new BasicStroke(1));
        // 4 casillas de aciertos
        for (int i = 0; i < PistaSellos.ACIERTOS; i++) {
            double cx = p.x + 34 + i * 40, cy = p.y + 54;
            if (el != null && i < pista.getAciertos()) {
                circulo(g, el, cx, cy, 30, 1f);
            } else {
                g.setColor(Tema.alfa(Tema.CREMA, 60));
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{4, 4}, 0));
                g.draw(new Ellipse2D.Double(cx - 14, cy - 14, 28, 28));
                g.setStroke(new BasicStroke(1));
            }
        }
        // margen: iconos que todavia pueden pasar
        int quedan = Math.max(0, PistaSellos.MARGEN - pista.getPasadas());
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(quedan <= 3 ? Tema.ROSA : Tema.DORADO);
        String q = String.valueOf(quedan);
        g.drawString(q, p.x + p.width - 22 - g.getFontMetrics().stringWidth(q), p.y + 60);
        g.setFont(Tema.fuente(Font.PLAIN, 9.5f));
        g.setColor(Tema.textoSuave());
        String m = "margen";
        g.drawString(m, p.x + p.width - 22 - g.getFontMetrics().stringWidth(m), p.y + 72);
    }

    private void carriles(Graphics2D g) {
        Rectangle c = cuerpo();
        int ly = lineaY();
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(20, 10, 8, 150));
        g.fill(new RoundRectangle2D.Double(c.x, c.y, c.width, c.height, 18, 18));
        for (int i = 0; i < PistaSellos.CARRILES; i++) {
            double cx = centroCarril(i);
            double luz = Math.max(0, 1 - (tiempo - pisado[i]) / 0.22);
            if (luz > 0) {
                g.setPaint(new GradientPaint((float) cx, c.y, Tema.alfa(COLOR_CIRCULO, 0), (float) cx, ly,
                        Tema.alfa(COLOR_CIRCULO, (int) (90 * luz))));
                g.fillRect(c.x + i * ANCHO_CARRIL + 4, c.y, ANCHO_CARRIL - 8, ly - c.y);
            }
            if (i > 0) {
                g.setColor(Tema.alfa(Tema.CREMA, 16));
                g.drawLine(c.x + i * ANCHO_CARRIL, c.y + 10, c.x + i * ANCHO_CARRIL, c.y + c.height - 10);
            }
            // receptor: anillo de la paleta
            double r = 22 + 3 * luz;
            g.setColor(Tema.alfa(Tema.mezclar(COLOR_CIRCULO, Tema.CREMA, 0.5), (int) (26 + 150 * luz)));
            g.fill(new Ellipse2D.Double(cx - r, ly - r, 2 * r, 2 * r));
            g.setColor(Tema.altoContraste ? Color.WHITE : Tema.mezclar(COLOR_CIRCULO, Tema.CREMA, 0.35));
            g.setStroke(new BasicStroke(2.6f));
            g.draw(new Ellipse2D.Double(cx - r, ly - r, 2 * r, 2 * r));
            g.setStroke(new BasicStroke(1));
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.CREMA);
            Dibujo.centrado(g, String.valueOf(PistaSellos.TECLAS.charAt(i)), cx, ly + 42);
        }
        Graphics2D s = (Graphics2D) g.create();
        s.clip(new RoundRectangle2D.Double(c.x, c.y, c.width, c.height, 18, 18));
        for (PistaSellos.Nota n : pista.getEnPista()) {
            double cx = centroCarril(n.carril);
            double y = ly - pista.alturaSobreLinea(n);
            double edad = pista.getT() - n.cuando;
            if (n.estado == 0) {
                circulo(s, n.sello, cx, y, 40, 1f);
            } else if (n.estado == 1 && edad < 0.3) {
                double f = edad / 0.3;
                circulo(s, n.sello, cx, ly, 40 * (1 + f * 0.5), (float) (1 - f));
            } else if (n.estado == 2 && edad < 0.45) {
                double f = edad / 0.45;
                circulo(s, n.sello, cx, ly, 40, (float) (1 - f));
                s.setColor(Tema.alfa(Tema.LADRILLO, (int) (230 * (1 - f))));
                s.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                s.drawLine((int) cx - 14, ly - 14, (int) cx + 14, ly + 14);
                s.drawLine((int) cx + 14, ly - 14, (int) cx - 14, ly + 14);
                s.setStroke(new BasicStroke(1));
            }
        }
        s.dispose();
        // juicio
        double edad = pista.getT() - pista.getUltimoJuicioT();
        PistaSellos.Juicio ju = pista.getUltimoJuicio();
        if (ju != null && ju != PistaSellos.Juicio.FUERA && edad < 0.55) {
            String txt = ju == PistaSellos.Juicio.PERFECTO ? "¡Perfecto!" : ju == PistaSellos.Juicio.BIEN ? "¡Bien!"
                    : ultimaFueAdvertencia ? "¡Otro sello! −1 vida" : "¡Otro sello! (seguro)";
            Color col = ju == PistaSellos.Juicio.EQUIVOCADO ? Tema.ROSA : Tema.DORADO;
            float a = (float) Math.max(0, 1 - edad / 0.55);
            g.setFont(Tema.fuente(Font.BOLD, 17));
            g.setColor(Tema.alfa(col, (int) (255 * a)));
            Dibujo.centrado(g, txt, c.getCenterX(), c.y + 40 - edad * 20);
        }
        // destello rojo al perder una vida
        if (tiempo - vidaT < 0.4) {
            g.setColor(Tema.alfa(Tema.LADRILLO, (int) (110 * (1 - (tiempo - vidaT) / 0.4))));
            g.fill(new RoundRectangle2D.Double(c.x, c.y, c.width, c.height, 18, 18));
        }
        if (pista.getElegido() == null && pista.getT() < 4) {
            g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
            g.setColor(Tema.texto());
            Dibujo.parrafo(g, g.getFont(), "El primer círculo que aciertes elige tu sello. Pisar otro sello cuesta una vida.",
                    c.x + 12, c.y + 70, c.width - 24, 15, c.y + 120);
        }
    }

    /** Circulo estilizado con el sello: todos con el mismo anillo y la imagen del sello adentro. */
    static void circulo(Graphics2D g, Veredicto v, double cx, double cy, double d, float alfa) {
        Graphics2D s = (Graphics2D) g.create();
        s.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alfa))));
        Color c = Tema.altoContraste ? Color.WHITE : COLOR_CIRCULO;
        s.setColor(new Color(0, 0, 0, 80));
        s.fill(new Ellipse2D.Double(cx - d / 2 + 1, cy - d / 2 + 3, d, d));
        s.setPaint(new RadialGradientPaint(new Point2D.Double(cx - d * 0.15, cy - d * 0.2), (float) (d * 0.7),
                new float[]{0f, 1f}, new Color[]{Tema.PAPEL, Tema.ROSA_CLARO}));
        s.fill(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
        if (!Recursos.contener(s, v.imagen(), cx - d * 0.36, cy - d * 0.36, d * 0.72, d * 0.72)) {
            Dibujo.selloRespaldo(s, cx, cy, d * 0.32, c, "");
        }
        s.setColor(c);
        s.setStroke(new BasicStroke((float) Math.max(2, d * 0.08)));
        s.draw(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
        s.dispose();
    }

    private void cierre(Graphics2D g, Rectangle p) {
        double f = Math.min(1, (tiempo - finT) / 0.3);
        g.setColor(new Color(20, 8, 4, (int) (205 * f)));
        g.fill(new RoundRectangle2D.Double(p.x, p.y, p.width, p.height, 26, 26));
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(Tema.CREMA);
        if (exito) {
            circulo(g, pista.getElegido(), p.getCenterX(), p.getCenterY() - 20, 90 * (1.4 - 0.4 * f), (float) f);
            Dibujo.centrado(g, "¡" + pista.getElegido().sello + "!", p.getCenterX(), p.getCenterY() + 60);
        } else if (despedido) {
            Dibujo.centrado(g, "Despedido", p.getCenterX(), p.getCenterY());
        } else {
            Dibujo.centrado(g, "Se pasaron " + PistaSellos.MARGEN, p.getCenterX(), p.getCenterY() - 10);
            g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
            Dibujo.centrado(g, "Sin castigo. Pulsa D para seguir.", p.getCenterX(), p.getCenterY() + 16);
        }
        JugadorVerificador jv = j.jugador;
        g.setFont(Tema.fuente(Font.PLAIN, 11));
        g.setColor(Tema.textoSuave());
        Dibujo.centrado(g, "Advertencias: " + jv.advertencias + " de " + JugadorVerificador.MAX_ADVERTENCIAS, p.getCenterX(),
                p.getCenterY() + 100);
    }
}
