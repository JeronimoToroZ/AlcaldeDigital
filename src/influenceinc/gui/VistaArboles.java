package influenceinc.gui;

import influenceinc.estructuras.ArbolAVLGenerico;
import influenceinc.estructuras.ArbolConsecuencias;
import influenceinc.estructuras.NodoDialogo;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dibuja los arboles del juego en 2D (visor de estructuras, tecla F2), para
 * que en la sustentacion se VEA como cambian: nodos insertados, eliminados,
 * el camino recorrido y el balanceo del AVL.
 */
public final class VistaArboles {

    private VistaArboles() {
    }

    // ------------------------------------------------------------ dialogo (n-ario)
    public static void dialogo(Graphics2D g, NodoDialogo raiz, NodoDialogo actual, Rectangle r) {
        if (raiz == null) {
            return;
        }
        int hojas = hojas(raiz);
        int prof = profundidad(raiz);
        double dx = r.width / (double) Math.max(1, hojas);
        double dy = (r.height - 30) / (double) Math.max(1, prof);
        double[] cursor = {r.x + dx / 2};
        dibujarDialogo(g, raiz, actual, r, 0, dx, dy, cursor);
    }

    private static double dibujarDialogo(Graphics2D g, NodoDialogo n, NodoDialogo actual, Rectangle r, int nivel,
                                         double dx, double dy, double[] cursor) {
        double x;
        List<Double> hijosX = new ArrayList<>();
        if (n.esHoja()) {
            x = cursor[0];
            cursor[0] += dx;
        } else {
            for (NodoDialogo h : n.getHijos()) {
                hijosX.add(dibujarDialogo(g, h, actual, r, nivel + 1, dx, dy, cursor));
            }
            x = (hijosX.get(0) + hijosX.get(hijosX.size() - 1)) / 2;
        }
        double y = r.y + 16 + nivel * dy;
        g.setStroke(new BasicStroke(1.6f));
        for (int i = 0; i < hijosX.size(); i++) {
            NodoDialogo h = n.getHijos().get(i);
            g.setColor(h.visitado ? Tema.acento() : Tema.borde());
            g.drawLine((int) x, (int) y + 12, (int) (double) hijosX.get(i), (int) (y + dy) - 12);
        }
        String et = n.getPadre() == null ? "Saludo" : corto(n.pregunta, 18);
        Color fondo = n == actual ? Tema.acento() : n.insertado ? new Color(230, 140, 40)
                : n.visitado ? Tema.bien().darker() : Tema.tarjeta();
        caja(g, et, x, y, fondo, n == actual);
        return x;
    }

    private static int hojas(NodoDialogo n) {
        if (n.esHoja()) {
            return 1;
        }
        int c = 0;
        for (NodoDialogo h : n.getHijos()) {
            c += hojas(h);
        }
        return c;
    }

    private static int profundidad(NodoDialogo n) {
        int p = 0;
        for (NodoDialogo h : n.getHijos()) {
            p = Math.max(p, 1 + profundidad(h));
        }
        return p;
    }

    // ------------------------------------------------------------ consecuencias
    public static void consecuencias(Graphics2D g, ArbolConsecuencias a, ArbolConsecuencias.Nodo resaltada, Rectangle r) {
        ArbolConsecuencias.Nodo raiz = a.getRaiz();
        Set<ArbolConsecuencias.Nodo> camino = new HashSet<>();
        for (ArbolConsecuencias.Nodo n = resaltada; n != null; n = n.getPadre()) {
            camino.add(n);
        }
        List<ArbolConsecuencias.Nodo> ramas = raiz.getHijos();
        int hojas = 0;
        for (ArbolConsecuencias.Nodo rama : ramas) {
            hojas += rama.getHijos().size();
        }
        double dx = r.width / (double) Math.max(1, hojas);
        double y0 = r.y + 16, y1 = r.y + r.height * 0.42, y2 = r.y + r.height - 26;
        double rx = r.x + r.width / 2.0;
        double cursor = r.x + dx / 2;
        Font chica = Tema.fuente(Font.PLAIN, 10f);
        for (ArbolConsecuencias.Nodo rama : ramas) {
            double ini = cursor;
            for (ArbolConsecuencias.Nodo hoja : rama.getHijos()) {
                double hx = cursor;
                double bx = ini + (rama.getHijos().size() - 1) * dx / 2;
                g.setColor(camino.contains(hoja) ? Tema.acento() : Tema.borde());
                g.setStroke(new BasicStroke(camino.contains(hoja) ? 3f : 1.2f));
                g.drawLine((int) bx, (int) y1 + 12, (int) hx, (int) y2 - 10);
                Color c = hoja.efecto.acierto ? Tema.bien() : hoja.efecto.grave ? Tema.mal() : Tema.neutro();
                g.setColor(camino.contains(hoja) ? Tema.acento() : c);
                g.fillOval((int) hx - 6, (int) y2 - 6, 12, 12);
                g.setFont(chica);
                g.setColor(camino.contains(hoja) ? Color.WHITE : Tema.textoSuave());
                String et = corta((influenceinc.modelo.Verdad) hoja.clave);
                int fila = rama.getHijos().indexOf(hoja) % 2;
                g.drawString(et, (int) hx - g.getFontMetrics().stringWidth(et) / 2, (int) y2 + 18 + fila * 12);
                cursor += dx;
            }
            double bx = ini + (rama.getHijos().size() - 1) * dx / 2;
            g.setColor(camino.contains(rama) ? Tema.acento() : Tema.borde());
            g.setStroke(new BasicStroke(camino.contains(rama) ? 3f : 1.4f));
            g.drawLine((int) rx, (int) y0 + 12, (int) bx, (int) y1 - 12);
            caja(g, rama.etiqueta, bx, y1, camino.contains(rama) ? Tema.acento() : Tema.tarjeta(), camino.contains(rama));
        }
        caja(g, "Caso", rx, y0, Tema.tarjeta(), false);
        g.setStroke(new BasicStroke(1));
    }

    private static String corta(influenceinc.modelo.Verdad v) {
        switch (v) {
            case VERDADERA: return "Verdad";
            case FALSA: return "Falsa";
            case OPINION: return "Opinión";
            case PUBLICIDAD_OCULTA: return "Publi.";
            default: return "Impostor";
        }
    }

    // ------------------------------------------------------------ AVL
    public static <V> void avl(Graphics2D g, ArbolAVLGenerico<String, V> a, String buscada, Rectangle r) {
        avl(g, a, buscada, r, false);
    }

    /** compacto = solo circulos; se rotulan el camino de busqueda (para el monitor). */
    public static <V> void avl(Graphics2D g, ArbolAVLGenerico<String, V> a, String buscada, Rectangle r, boolean compacto) {
        ArbolAVLGenerico.Nodo<String, V> raiz = a.getRaiz();
        if (raiz == null) {
            return;
        }
        Set<String> camino = new HashSet<>();
        ArbolAVLGenerico.Nodo<String, V> n = raiz;
        while (buscada != null && n != null) {
            camino.add(n.clave);
            int c = buscada.compareTo(n.clave);
            n = c == 0 ? null : c < 0 ? n.getIzq() : n.getDer();
        }
        int alt = Math.max(1, raiz.getAltura());
        double dy = (r.height - 30) / (double) alt;
        avl(g, a, raiz, r.x, r.x + r.width, r.y + 14, dy, camino, buscada, compacto);
    }

    private static <V> void avl(Graphics2D g, ArbolAVLGenerico<String, V> a, ArbolAVLGenerico.Nodo<String, V> n,
                                double x0, double x1, double y, double dy, Set<String> camino, String buscada,
                                boolean compacto) {
        double x = (x0 + x1) / 2;
        if (n.getIzq() != null) {
            g.setColor(camino.contains(n.getIzq().clave) ? Tema.acento() : Tema.borde());
            g.setStroke(new BasicStroke(camino.contains(n.getIzq().clave) ? 3f : 1.4f));
            g.drawLine((int) x, (int) y, (int) ((x0 + x) / 2), (int) (y + dy));
            avl(g, a, n.getIzq(), x0, x, y + dy, dy, camino, buscada, compacto);
        }
        if (n.getDer() != null) {
            g.setColor(camino.contains(n.getDer().clave) ? Tema.acento() : Tema.borde());
            g.setStroke(new BasicStroke(camino.contains(n.getDer().clave) ? 3f : 1.4f));
            g.drawLine((int) x, (int) y, (int) ((x + x1) / 2), (int) (y + dy));
            avl(g, a, n.getDer(), x, x1, y + dy, dy, camino, buscada, compacto);
        }
        boolean enc = n.clave.equals(buscada);
        Color fondo = enc ? Tema.bien() : camino.contains(n.clave) ? Tema.acento() : Tema.tarjeta();
        String et = corto(n.clave.replace("_nova", "").replace("@", ""), 11);
        if (compacto) {
            g.setColor(fondo);
            g.fillOval((int) x - 6, (int) y - 6, 12, 12);
            if (camino.contains(n.clave)) {
                g.setFont(Tema.fuente(Font.PLAIN, 10));
                g.setColor(Color.WHITE);
                g.drawString(et, (int) x + 8, (int) y + 4);
            }
        } else {
            caja(g, et + " " + a.factorEquilibrio(n), x, y, fondo, camino.contains(n.clave));
        }
        g.setStroke(new BasicStroke(1));
    }

    // ------------------------------------------------------------ utilidades
    static void caja(Graphics2D g, String texto, double cx, double cy, Color fondo, boolean fuerte) {
        g.setFont(Tema.fuente(fuerte ? Font.BOLD : Font.PLAIN, 11f));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(texto) + 14, h = 22;
        RoundRectangle2D rr = new RoundRectangle2D.Double(cx - w / 2.0, cy - h / 2.0, w, h, 10, 10);
        g.setColor(fondo);
        g.fill(rr);
        g.setColor(fuerte ? Color.WHITE : Tema.borde());
        g.setStroke(new BasicStroke(fuerte ? 2f : 1f));
        g.draw(rr);
        g.setColor(esClaro(fondo) ? Color.BLACK : Tema.texto());
        g.drawString(texto, (int) (cx - w / 2.0 + 7), (int) (cy + fm.getAscent() / 2.0 - 2));
    }

    static boolean esClaro(Color c) {
        return c.getRed() * 0.3 + c.getGreen() * 0.59 + c.getBlue() * 0.11 > 150;
    }

    public static String corto(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}
