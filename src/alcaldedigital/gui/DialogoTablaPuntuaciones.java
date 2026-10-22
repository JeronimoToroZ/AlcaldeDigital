package alcaldedigital.gui;

import alcaldedigital.logica.ResultadoRonda;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Tabla de puntuaciones al final de las rondas de un nivel.
 *  - Arriba, en grande, el podio: 1.º, 2.º y 3.º lugar (según cuántos jugaron).
 *  - Abajo, la tabla de 3 columnas: Jugador, Tiempo y Puntos.
 * El orden: quien superó el nivel va primero; luego más puntos; si empatan, menos tiempo.
 */
public class DialogoTablaPuntuaciones extends DialogoJuego {

    static final Color PLATA = new Color(0xC9, 0xCF, 0xD6);
    static final Color BRONCE = new Color(0xD2, 0x8E, 0x5A);

    public DialogoTablaPuntuaciones(Window padre, int nivel, List<ResultadoRonda> resultados,
                                    List<Integer> desbloqueados) {
        super(padre, "Resultados del nivel " + nivel);

        List<ResultadoRonda> orden = new ArrayList<>(resultados);
        orden.sort(ResultadoRonda::comparar);

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        Podio podio = new Podio(orden);
        podio.setAlignmentX(0.5f);
        cuerpo.add(podio);
        cuerpo.add(espacio(12));

        // ---- tabla de 3 columnas ----
        JPanel tabla = new JPanel(new GridBagLayout());
        tabla.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 6, 0);
        c.gridy = 0;
        tabla.add(fila(-1, "Jugador", "Tiempo", "Puntos", null), c);
        for (int i = 0; i < orden.size(); i++) {
            ResultadoRonda r = orden.get(i);
            c.gridy = i + 1;
            String tiempo = PanelPlataformas.formatoTiempo(r.segundos) + (r.supero ? "" : "  (no terminó)");
            String nombre = "<html><b>" + html(r.jugador.getNombre()) + "</b><br><font size='-1' color='#D9C39E'>"
                    + html(r.jugador.getRol().getEtiqueta()) + "</font></html>";
            tabla.add(fila(i, nombre, tiempo, r.puntos + " pts", r), c);
        }
        tabla.setAlignmentX(0.5f);
        cuerpo.add(tabla);

        JLabel nota = etiqueta("Entre menos tiempo tardes, más puntos ganas. Empate en puntos: gana el más rápido.",
                Font.ITALIC, 12, CREMA_SUAVE);
        nota.setAlignmentX(0.5f);
        cuerpo.add(nota);
        cuerpo.add(espacio(10));

        // ---- qué pasa con el mapa ----
        boolean alguienSupero = false;
        for (ResultadoRonda r : resultados) {
            alguienSupero |= r.supero;
        }
        String texto;
        Color color;
        if (!alguienSupero) {
            texto = "Nadie superó el nivel " + nivel + ". Los niveles que salen de él siguen bloqueados: inténtenlo de nuevo.";
            color = ROJO;
        } else if (desbloqueados.isEmpty()) {
            texto = "Nivel " + nivel + " superado por el equipo.";
            color = VERDE;
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < desbloqueados.size(); i++) {
                sb.append(i == 0 ? "" : " y ").append(desbloqueados.get(i));
            }
            texto = "¡Nivel superado! Se desbloquea" + (desbloqueados.size() > 1 ? "n los niveles " : " el nivel ") + sb + ".";
            color = VERDE;
        }
        Tarjeta aviso = new Tarjeta(new BorderLayout(), color);
        aviso.add(parrafo(texto, 520, 15, color), BorderLayout.CENTER);
        aviso.setAlignmentX(0.5f);
        cuerpo.add(aviso);

        setContenido(cuerpo);
        BotonDorado mapa = new BotonDorado("Volver al mapa", true);
        mapa.addActionListener(e -> dispose());
        agregarBoton(mapa);
        getRootPane().setDefaultButton(mapa);
    }

    private static JPanel fila(int posicion, String nombre, String tiempo, String puntos, ResultadoRonda r) {
        boolean encabezado = posicion < 0;
        JPanel f = encabezado ? new JPanel(new GridBagLayout()) : new Tarjeta(new GridBagLayout(), posicion == 0 ? ORO : null);
        f.setOpaque(false);
        f.setBorder(BorderFactory.createEmptyBorder(encabezado ? 0 : 8, 16, encabezado ? 0 : 8, 16));
        Color texto = encabezado ? CREMA_SUAVE : CREMA;
        int tam = encabezado ? 13 : 15;
        int estilo = encabezado ? Font.BOLD : Font.PLAIN;

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        f.add(encabezado ? celda("#", 44, estilo, tam, texto, SwingConstants.CENTER) : new Medalla(posicion), c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 14, 0, 0);
        f.add(celda(nombre, 220, estilo, tam, texto, SwingConstants.LEFT), c);
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 2;
        f.add(celda(tiempo, 170, estilo, tam, r != null && !r.supero ? ROJO : texto, SwingConstants.CENTER), c);
        c.gridx = 3;
        f.add(celda(puntos, 100, encabezado ? estilo : Font.BOLD, encabezado ? tam : 17,
                encabezado ? texto : (posicion == 0 ? ORO : CREMA), SwingConstants.RIGHT), c);
        return f;
    }

    private static JLabel celda(String texto, int ancho, int estilo, int tam, Color color, int alineacion) {
        JLabel l = etiqueta(texto, estilo, tam, color);
        l.setHorizontalAlignment(alineacion);
        l.setPreferredSize(new Dimension(ancho, l.getPreferredSize().height));
        return l;
    }

    static Color colorPuesto(int posicion) {
        return posicion == 0 ? ORO : posicion == 1 ? PLATA : posicion == 2 ? BRONCE : new Color(0x8A, 0x6E, 0x58);
    }

    // ------------------------------------------------------------------

    /** Medalla redonda con el puesto. */
    static class Medalla extends JComponent {

        private final int posicion;

        Medalla(int posicion) {
            this.posicion = posicion;
            setPreferredSize(new Dimension(44, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color base = colorPuesto(posicion);
            int d = 34, x = (getWidth() - d) / 2, y = (getHeight() - d) / 2;
            g2.setPaint(new GradientPaint(0, y, base.brighter(), 0, y + d, base.darker()));
            g2.fillOval(x, y, d, d);
            g2.setColor(conAlfa(Color.WHITE, 120));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x + 3, y + 3, d - 6, d - 6);
            g2.setColor(CAFE_OSCURO);
            g2.setFont(fuente(Font.BOLD, 16));
            String s = String.valueOf(posicion + 1);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, x + (d - fm.stringWidth(s)) / 2, y + (d - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Podio: el 1.º al centro y más alto, el 2.º a la izquierda y el 3.º a la derecha. */
    static class Podio extends JComponent {

        private final List<ResultadoRonda> orden;

        Podio(List<ResultadoRonda> orden) {
            this.orden = orden;
            setPreferredSize(new Dimension(560, 230));
            setMaximumSize(new Dimension(560, 230));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int anchoBloque = 160;
            int[] centros = {w / 2, w / 2 - anchoBloque - 10, w / 2 + anchoBloque + 10};   // 1.º, 2.º, 3.º
            int[] alturas = {100, 70, 48};
            String[] textoPuesto = {"1.er lugar", "2.º lugar", "3.er lugar"};
            int base = h - 6;
            int puestos = Math.min(3, orden.size());
            for (int i = 0; i < puestos; i++) {
                ResultadoRonda r = orden.get(i);
                int cx = centros[i];
                int top = base - alturas[i];
                Color color = colorPuesto(i);
                // bloque del podio
                g2.setPaint(new GradientPaint(0, top, color.brighter(), 0, base, color.darker()));
                g2.fillRoundRect(cx - anchoBloque / 2, top, anchoBloque, alturas[i], 12, 12);
                g2.setColor(CAFE_OSCURO);
                g2.setFont(fuente(Font.BOLD, i == 0 ? 34 : 26));
                FontMetrics fm = g2.getFontMetrics();
                String num = String.valueOf(i + 1);
                g2.drawString(num, cx - fm.stringWidth(num) / 2, top + alturas[i] / 2 + fm.getAscent() / 2 - 4);
                // nombre en grande encima del bloque
                g2.setColor(i == 0 ? ORO : CREMA);
                g2.setFont(fuente(Font.BOLD | Font.ITALIC, i == 0 ? 28 : 20));
                fm = g2.getFontMetrics();
                String nombre = recortar(r.jugador.getNombre(), fm, anchoBloque + 20);
                g2.drawString(nombre, cx - fm.stringWidth(nombre) / 2, top - 30);
                g2.setFont(fuente(Font.PLAIN, 13));
                g2.setColor(CREMA_SUAVE);
                fm = g2.getFontMetrics();
                String detalle = textoPuesto[i] + " · " + r.puntos + " pts";
                g2.drawString(detalle, cx - fm.stringWidth(detalle) / 2, top - 10);
                if (i == 0) {
                    dibujarCorona(g2, cx, top - 98);
                }
            }
            g2.dispose();
        }

        private static String recortar(String s, FontMetrics fm, int max) {
            if (fm.stringWidth(s) <= max) {
                return s;
            }
            while (s.length() > 1 && fm.stringWidth(s + "…") > max) {
                s = s.substring(0, s.length() - 1);
            }
            return s + "…";
        }

        private static void dibujarCorona(Graphics2D g2, int cx, int y) {
            int[] xs = {cx - 20, cx - 20, cx - 10, cx, cx + 10, cx + 20, cx + 20};
            int[] ys = {y + 22, y + 4, y + 14, y, y + 14, y + 4, y + 22};
            g2.setColor(ORO);
            g2.fillPolygon(xs, ys, xs.length);
            g2.setColor(CAFE_OSCURO);
            g2.fillOval(cx - 3, y + 12, 6, 6);
        }
    }
}
