package influenceinc.gui;

import influenceinc.estructuras.Arista;
import influenceinc.estructuras.GrafoRed;
import influenceinc.estructuras.Vertice;
import influenceinc.logica.CiudadEstado;
import influenceinc.modelo.TipoCuenta;
import influenceinc.motor3d.Camara;
import influenceinc.motor3d.Escena;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El ESCANER DE RED: el grafo social en 3D (motor propio) dentro del monitor.
 * Las personas estan abajo, por barrios; las cuentas falsas forman un anillo
 * flotante; el origen del caso brilla y sus oleadas BFS se iluminan.
 */
public class VistaRed3D {

    private final Camara camara = new Camara();
    private final Escena escena = new Escena();
    private BufferedImage lienzo;

    public void dibujar(Graphics2D g, Rectangle r, CiudadEstado c, Vertice origen, double t) {
        if (lienzo == null || lienzo.getWidth() != r.width || lienzo.getHeight() != r.height) {
            lienzo = new BufferedImage(Math.max(1, r.width), Math.max(1, r.height), BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D gl = lienzo.createGraphics();
        gl.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gl.setPaint(new GradientPaint(0, 0, new Color(6, 10, 26), 0, r.height, new Color(20, 10, 40)));
        gl.fillRect(0, 0, r.width, r.height);

        GrafoRed red = c.red;
        Map<Vertice, double[]> pos = new HashMap<>();
        int ii = 0;
        List<Vertice> bots = c.bots;
        for (Vertice v : red.getVertices()) {
            double[] p;
            switch (v.tipo) {
                case BOT: {
                    int k = bots.indexOf(v);
                    double a = 2 * Math.PI * k / Math.max(1, bots.size()) + t * 0.4;
                    p = new double[]{Math.cos(a) * 90, 150, Math.sin(a) * 90};
                    break;
                }
                case INFLUENCER: {
                    double a = 2 * Math.PI * ii++ / 3.0 + 0.5;
                    p = new double[]{Math.cos(a) * 200, 80, Math.sin(a) * 200};
                    break;
                }
                case MEDIO:
                    p = new double[]{-40, 90, 160};
                    break;
                case AGENCIA:
                    p = new double[]{0, 230, 0};
                    break;
                default:
                    p = new double[]{v.x, 0, v.z};
                    break;
            }
            pos.put(v, p);
        }
        Set<Vertice> ola1 = new HashSet<>(), ola2 = new HashSet<>();
        if (origen != null && red.contiene(origen.id)) {
            List<List<Vertice>> o = red.bfsOleadas(origen, 2);
            if (o.size() > 1) {
                ola1.addAll(o.get(1));
            }
            if (o.size() > 2) {
                ola2.addAll(o.get(2));
            }
        }
        escena.limpiar();
        escena.agregarDisco(0, 0, 300, -2, new Color(20, 26, 60), new Color(60, 70, 130));
        for (Arista a : red.getAristas()) {
            double[] p = pos.get(a.origen), q = pos.get(a.destino);
            if (p == null || q == null) {
                continue;
            }
            boolean caliente = a.origen == origen || (ola1.contains(a.origen) && ola2.contains(a.destino));
            Color col = caliente ? new Color(255, 190, 60) : a.origen.tipo == TipoCuenta.BOT
                    ? new Color(220, 60, 70, 120) : new Color(110, 130, 200, 70);
            escena.agregar(new Escena.Linea(p[0], p[1], p[2], q[0], q[1], q[2], col, caliente ? 2.2f : 1f));
        }
        for (Map.Entry<Vertice, double[]> en : pos.entrySet()) {
            Vertice v = en.getKey();
            double[] p = en.getValue();
            Color col;
            double rad = 7;
            switch (v.tipo) {
                case BOT: col = new Color(225, 50, 60); rad = 9; break;
                case INFLUENCER: col = new Color(230, 159, 0); rad = 11; break;
                case MEDIO: col = new Color(86, 180, 233); rad = 12; break;
                case AGENCIA: col = new Color(204, 121, 167); rad = 14; break;
                default: col = v.desinformado ? new Color(150, 90, 90) : new Color(120, 200, 170); break;
            }
            Escena.Esfera e = new Escena.Esfera(p[0], p[1], p[2], rad, col);
            e.marcaX = v.desinformado || v.tipo == TipoCuenta.BOT;
            if (v == origen) {
                e.anillo = Color.WHITE;
                e.escalaPulso = 1.3 + 0.25 * Math.sin(t * 6);
                e.etiqueta = "ORIGEN";
                e.etiquetaSiempre = true;
            } else if (ola1.contains(v)) {
                e.anillo = new Color(255, 200, 60);
            } else if (ola2.contains(v)) {
                e.anillo = new Color(255, 200, 60, 120);
                e.anilloPunteado = true;
            }
            if (v.tipo == TipoCuenta.AGENCIA) {
                e.etiqueta = "Influence, Inc.";
                e.etiquetaSiempre = true;
            }
            e.brillo = v == origen ? 1 : 0.4;
            escena.agregar(e);
        }
        camara.fijar(t * 0.25, 0.5, 600, 0, 95, 0);
        camara.preparar(r.width, r.height);
        escena.render(gl, camara);
        gl.dispose();
        g.drawImage(lienzo, r.x, r.y, null);
    }
}
