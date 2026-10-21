package alcaldedigital.gui;

import alcaldedigital.logica.GrafoSocial;
import alcaldedigital.logica.NodoRed;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Dibuja el grafo de Ciudad Nova sobre el mapa: aristas, lugares sanos (azul)
 * y lugares a los que llegó el rumor (rojo, con "!"). El origen del rumor
 * lleva un anillo dorado. En la versión grande también muestra a cuántos
 * saltos (BFS) está cada lugar del origen.
 *
 * No depende solo del color: los lugares contagiados llevan un "!" y los
 * sanos un punto, para que se distingan también en alto contraste o con daltonismo.
 */
public class PanelRedCiudad extends JPanel {

    private static final double ANCHO_LOGICO = 1000.0;
    private static final double ALTO_LOGICO = 800.0;

    private final GrafoSocial red;
    private final boolean compacto;
    private final BufferedImage mapa;
    private long pulso;
    private final Timer animacion;

    public PanelRedCiudad(GrafoSocial red, boolean compacto) {
        this.red = red;
        this.compacto = compacto;
        this.mapa = BotonImagen.cargar("/alcaldedigital/recursos/mapa_ciudad.jpg");
        setOpaque(false);
        setPreferredSize(compacto ? new Dimension(260, 190) : new Dimension(700, 520));
        // animación suave del pulso de los nodos contagiados
        animacion = new Timer(80, e -> {
            pulso++;
            repaint();
        });
    }

    /** La animación solo corre mientras el panel está en pantalla (no deja timers huérfanos). */
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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        boolean contraste = Tema.isAltoContraste();
        int w = getWidth(), h = getHeight();

        // el área de dibujo conserva la proporción 1000x800 del mapa
        double escala = Math.min(w / ANCHO_LOGICO, h / ALTO_LOGICO);
        int aw = (int) (ANCHO_LOGICO * escala), ah = (int) (ALTO_LOGICO * escala);
        int ox = (w - aw) / 2, oy = (h - ah) / 2;

        java.awt.geom.RoundRectangle2D marco = new java.awt.geom.RoundRectangle2D.Double(ox, oy, aw, ah, 16, 16);
        g2.setClip(marco);
        if (mapa != null) {
            g2.drawImage(mapa, ox, oy, aw, ah, null);
        }
        g2.setColor(contraste ? new Color(0, 0, 0, 190) : new Color(255, 248, 235, 70));
        g2.fill(marco);
        g2.setClip(null);
        g2.setColor(contraste ? Color.WHITE : DialogoJuego.ORO);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(marco);

        double r = compacto ? 9 : 17;
        // aristas
        for (NodoRed n : red.nodos) {
            for (NodoRed v : n.vecinos) {
                if (red.nodos.indexOf(v) < red.nodos.indexOf(n)) {
                    continue;   // cada arista una sola vez
                }
                boolean caliente = n.estaInfectado && v.estaInfectado;
                g2.setColor(caliente ? new Color(0xD3, 0x3A, 0x2F, 220)
                        : contraste ? Color.WHITE : new Color(0x5A, 0x34, 0x1C, 200));
                g2.setStroke(new BasicStroke(caliente ? (compacto ? 3f : 5f) : (compacto ? 2f : 3f),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(px(n.x, escala, ox), py(n.y, escala, oy), px(v.x, escala, ox), py(v.y, escala, oy));
            }
        }

        Map<NodoRed, Integer> dist = red.getOrigen() == null ? null : red.distanciasDesde(red.getOrigen());
        Font fNombre = new Font(DialogoJuego.FUENTE, Font.BOLD, compacto ? 9 : 13);
        for (NodoRed n : red.nodos) {
            int cx = px(n.x, escala, ox), cy = py(n.y, escala, oy);
            if (n.estaInfectado) {
                double halo = r + 4 + Math.abs(Math.sin(pulso * 0.25)) * (compacto ? 4 : 7);
                g2.setColor(new Color(0xD3, 0x3A, 0x2F, 80));
                g2.fill(new java.awt.geom.Ellipse2D.Double(cx - halo, cy - halo, halo * 2, halo * 2));
            }
            Color relleno = n.estaInfectado ? new Color(0xD3, 0x3A, 0x2F) : new Color(0x2E, 0x78, 0xD2);
            if (contraste) {
                relleno = n.estaInfectado ? new Color(255, 128, 0) : new Color(0, 160, 255);
            }
            g2.setColor(relleno);
            g2.fill(new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(compacto ? 1.5f : 2.5f));
            g2.draw(new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            if (n == red.getOrigen()) {
                g2.setColor(contraste ? new Color(255, 255, 0) : DialogoJuego.ORO);
                g2.setStroke(new BasicStroke(compacto ? 2f : 3f));
                g2.draw(new java.awt.geom.Ellipse2D.Double(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2));
            }
            // símbolo dentro del nodo: "!" contagiado, punto sano
            g2.setColor(Color.WHITE);
            if (n.estaInfectado) {
                g2.setFont(new Font(DialogoJuego.FUENTE, Font.BOLD, compacto ? 12 : 20));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("!", cx - fm.stringWidth("!") / 2, cy + fm.getAscent() / 2 - 2);
            } else {
                g2.fillOval(cx - 2, cy - 2, 4, 4);
            }
            // nombre (y saltos desde el origen en la versión grande)
            String etiqueta = n.nombre;
            if (!compacto && dist != null && dist.containsKey(n)) {
                etiqueta += "  (" + dist.get(n) + (dist.get(n) == 1 ? " salto)" : " saltos)");
            }
            g2.setFont(fNombre);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(etiqueta);
            int ty = (int) (cy + r + fm.getAscent() + 2);
            g2.setColor(contraste ? Color.BLACK : new Color(0x26, 0x15, 0x0E, 190));
            g2.fillRoundRect(cx - tw / 2 - 4, ty - fm.getAscent(), tw + 8, fm.getHeight(), 8, 8);
            g2.setColor(contraste ? Color.WHITE : DialogoJuego.CREMA);
            g2.drawString(etiqueta, cx - tw / 2, ty - 1);
        }
        g2.dispose();
    }

    private static int px(int x, double escala, int ox) {
        return ox + (int) (x * escala);
    }

    private static int py(int y, double escala, int oy) {
        return oy + (int) (y * escala);
    }
}
