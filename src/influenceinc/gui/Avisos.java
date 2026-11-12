package influenceinc.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Notificaciones que aparecen sobre la vista 3D y se desvanecen solas,
 * mas la linea de subtitulos de los sonidos.
 */
public class Avisos implements Sonido.Subtitulos {

    private static class Aviso {
        final String titulo;
        final String texto;
        final Color color;
        final long inicio;

        Aviso(String titulo, String texto, Color color, long inicio) {
            this.titulo = titulo;
            this.texto = texto;
            this.color = color;
            this.inicio = inicio;
        }
    }

    private static final long DURACION = 7000;
    /** Si es >= 0, la linea de subtitulos se dibuja en esta y (si no, abajo del panel). */
    public int subtituloY = -1;
    /** true = los avisos se apilan hacia ARRIBA desde y (asi no tapan la ventana de interrogatorio). */
    public boolean haciaArriba;
    /** Cuantos avisos se ven a la vez. */
    public int maxVisibles = 4;
    private final List<Aviso> lista = new ArrayList<>();
    private String subtitulo = "";
    private long subtituloDesde;

    public synchronized void agregar(String titulo, String texto, Color color, long retrasoMs) {
        lista.add(new Aviso(titulo, texto == null ? "" : texto, color, System.currentTimeMillis() + retrasoMs));
        while (lista.size() > 6) {
            lista.remove(0);
        }
    }

    @Override
    public synchronized void mostrar(String texto) {
        subtitulo = texto;
        subtituloDesde = System.currentTimeMillis();
    }

    public synchronized void limpiar() {
        lista.clear();
    }

    public synchronized void dibujar(Graphics2D g, int x, int y, int anchoMax, int altoPanel, int anchoPanel) {
        long ahora = System.currentTimeMillis();
        lista.removeIf(a -> ahora - a.inicio > DURACION);
        int yy = y;
        Font ft = Tema.fuente(Font.BOLD, 14);
        Font fx = Tema.fuente(Font.PLAIN, 12.5f);
        int visibles = 0;
        for (int i = lista.size() - 1; i >= 0 && visibles < maxVisibles; i--) {
            Aviso a = lista.get(i);
            long edad = ahora - a.inicio;
            if (edad < 0) {
                continue;
            }
            visibles++;
            double alfa = edad < 250 ? edad / 250.0 : edad > DURACION - 800 ? (DURACION - edad) / 800.0 : 1;
            List<String> lineas = partir(g, fx, a.texto, anchoMax - 24);
            int alto = 30 + lineas.size() * 16;
            int desliz = edad < 250 ? (int) ((1 - edad / 250.0) * -30) : 0;
            if (haciaArriba) {
                yy -= alto;
            }
            g.setColor(new Color(42, 23, 18, (int) (232 * alfa)));
            if (Tema.altoContraste) {
                g.setColor(new Color(0, 0, 0, (int) (255 * alfa)));
            }
            g.fillRoundRect(x + desliz, yy, anchoMax, alto, 16, 16);
            Color c = a.color;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * alfa)));
            g.fillRoundRect(x + desliz, yy, 5, alto, 4, 4);
            g.drawRoundRect(x + desliz, yy, anchoMax, alto, 14, 14);
            g.setFont(ft);
            g.drawString(a.titulo, x + desliz + 14, yy + 20);
            g.setFont(fx);
            Color tx = Tema.texto();
            g.setColor(new Color(tx.getRed(), tx.getGreen(), tx.getBlue(), (int) (235 * alfa)));
            int ly = yy + 38;
            for (String l : lineas) {
                g.drawString(l, x + desliz + 14, ly);
                ly += 16;
            }
            yy += haciaArriba ? -8 : alto + 8;
        }
        if (Tema.subtitulos && ahora - subtituloDesde < 2600 && !subtitulo.isEmpty()) {
            g.setFont(Tema.fuente(Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(subtitulo) + 20;
            int sx = (anchoPanel - w) / 2;
            int sy = subtituloY >= 0 ? subtituloY : altoPanel - 46;
            g.setColor(new Color(42, 23, 18, 220));
            g.fillRoundRect(sx, sy, w, 26, 13, 13);
            g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.DORADO);
            g.drawString(subtitulo, sx + 10, sy + 18);
        }
    }

    public static List<String> partir(Graphics2D g, Font f, String texto, int ancho) {
        List<String> lineas = new ArrayList<>();
        FontMetrics fm = g.getFontMetrics(f);
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (fm.stringWidth(prueba) > ancho && actual.length() > 0) {
                lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual = new StringBuilder(prueba);
            }
        }
        if (actual.length() > 0) {
            lineas.add(actual.toString());
        }
        return lineas;
    }
}
