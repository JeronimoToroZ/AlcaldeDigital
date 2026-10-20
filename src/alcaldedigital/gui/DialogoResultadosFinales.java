package alcaldedigital.gui;

import alcaldedigital.modelo.Jugador;
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
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Pantalla de resultados finales de la campaña (reemplaza al JOptionPane con tabla).
 * Arriba destaca al ganador con un trofeo; abajo muestra la clasificación completa
 * con medallas de oro, plata y bronce.
 */
public class DialogoResultadosFinales extends DialogoJuego {

    /** Lo que eligió el jugador al cerrar. */
    public enum Opcion { VOLVER_AL_INICIO, SALIR, CERRAR }

    private static final Color PLATA = new Color(0xC9, 0xCF, 0xD6);
    private static final Color BRONCE = new Color(0xD2, 0x8E, 0x5A);

    private Opcion opcion = Opcion.CERRAR;

    /**
     * @param ordenados jugadores ya ordenados (el primero es el ganador)
     * @param tiempos   tiempo total de reacción de cada jugador, en milisegundos
     */
    public DialogoResultadosFinales(Window padre, List<Jugador> ordenados, Map<Jugador, Long> tiempos) {
        super(padre, "¡Campaña finalizada!");

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        // --- Ganador ---
        Jugador ganador = ordenados.get(0);
        Tarjeta tarjetaGanador = new Tarjeta(new BorderLayout(18, 0), ORO);
        tarjetaGanador.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 20, 14, 20));
        tarjetaGanador.add(new Trofeo(), BorderLayout.WEST);
        JPanel datosGanador = new JPanel(new GridLayout(0, 1, 0, 2));
        datosGanador.setOpaque(false);
        datosGanador.add(etiqueta("GANADOR DE LA CAMPAÑA", Font.BOLD, 13, CREMA_SUAVE));
        datosGanador.add(etiqueta(ganador.getNombre(), Font.BOLD | Font.ITALIC, 30, ORO));
        datosGanador.add(etiqueta(ganador.getRol().getEtiqueta() + "  ·  " + ganador.getPuntos() + " pts  ·  "
                + segundos(tiempos, ganador), Font.PLAIN, 15, CREMA));
        tarjetaGanador.add(datosGanador, BorderLayout.CENTER);
        tarjetaGanador.setAlignmentX(0.5f);
        cuerpo.add(tarjetaGanador);
        cuerpo.add(espacio(16));

        // --- Clasificación ---
        JLabel lblTabla = etiqueta("Clasificación", Font.BOLD, 17, ORO);
        lblTabla.setAlignmentX(0.5f);
        cuerpo.add(lblTabla);
        cuerpo.add(espacio(8));

        JPanel tabla = new JPanel(new GridBagLayout());
        tabla.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 0, 6, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        tabla.add(fila(null, "#", "Jugador", "Aciertos", "Tiempo", "Puntos", true), c);
        for (int i = 0; i < ordenados.size(); i++) {
            Jugador j = ordenados.get(i);
            c.gridy = i + 1;
            JPanel f = fila(i, String.valueOf(i + 1),
                    "<html><b>" + html(j.getNombre()) + "</b><br><font size='-1' color='#D9C39E'>"
                            + html(j.getRol().getEtiqueta()) + "</font></html>",
                    j.getAciertos() + " / " + (j.getAciertos() + j.getErrores()),
                    segundos(tiempos, j),
                    j.getPuntos() + " pts", false);
            tabla.add(f, c);
        }
        tabla.setAlignmentX(0.5f);
        cuerpo.add(tabla);

        JLabel nota = etiqueta("En caso de empate en puntos, gana quien tuvo menor tiempo total.",
                Font.ITALIC, 12, CREMA_SUAVE);
        nota.setAlignmentX(0.5f);
        cuerpo.add(espacio(4));
        cuerpo.add(nota);

        setContenido(cuerpo);

        BotonDorado volver = new BotonDorado("Volver al inicio", true);
        volver.addActionListener(e -> { opcion = Opcion.VOLVER_AL_INICIO; dispose(); });
        BotonDorado salir = new BotonDorado("Salir del juego", false);
        salir.addActionListener(e -> { opcion = Opcion.SALIR; dispose(); });
        agregarBoton(volver);
        agregarBoton(salir);
        getRootPane().setDefaultButton(volver);
    }

    /** Muestra el diálogo y devuelve lo que eligió el jugador. */
    public Opcion elegir() {
        mostrar();
        return opcion;
    }

    private static String segundos(Map<Jugador, Long> tiempos, Jugador j) {
        return String.format("%.2f s", tiempos.getOrDefault(j, 0L) / 1000.0);
    }

    /**
     * Una fila de la clasificación. Si es encabezado, solo texto pequeño;
     * si no, una tarjeta con medalla. El ganador (posición 0) lleva borde dorado.
     */
    private static JPanel fila(Integer posicion, String num, String nombre, String aciertos,
                               String tiempo, String puntos, boolean encabezado) {
        JPanel f = encabezado ? new JPanel(new GridBagLayout())
                : new Tarjeta(new GridBagLayout(), posicion == 0 ? ORO : null);
        f.setOpaque(false);
        if (encabezado) {
            f.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 16, 0, 16));
        } else {
            f.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16));
        }

        Color colorTexto = encabezado ? CREMA_SUAVE : CREMA;
        int tam = encabezado ? 12 : 15;
        int estilo = encabezado ? Font.BOLD : Font.PLAIN;

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.weightx = 0;
        JComponent celdaPos = encabezado ? celda(num, 44, estilo, tam, colorTexto, SwingConstants.CENTER)
                : new Medalla(posicion);
        f.add(celdaPos, c);

        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 14, 0, 0);
        f.add(celda(nombre, 190, estilo, tam, encabezado ? colorTexto : CREMA, SwingConstants.LEFT), c);

        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 0;
        c.gridx = 2;
        f.add(celda(aciertos, 90, estilo, tam, colorTexto, SwingConstants.CENTER), c);
        c.gridx = 3;
        f.add(celda(tiempo, 90, estilo, tam, colorTexto, SwingConstants.CENTER), c);
        c.gridx = 4;
        Color colorPuntos = encabezado ? colorTexto : (posicion == 0 ? ORO : CREMA);
        f.add(celda(puntos, 90, encabezado ? estilo : Font.BOLD, encabezado ? tam : 17,
                colorPuntos, SwingConstants.RIGHT), c);
        return f;
    }

    private static JLabel celda(String texto, int ancho, int estilo, int tam, Color color, int alineacion) {
        JLabel l = etiqueta(texto, estilo, tam, color);
        l.setHorizontalAlignment(alineacion);
        l.setPreferredSize(new Dimension(ancho, l.getPreferredSize().height));
        return l;
    }

    // ------------------------------------------------------------------
    // Dibujos
    // ------------------------------------------------------------------

    /** Medalla redonda con el puesto: oro, plata, bronce o neutra. */
    private static class Medalla extends JComponent {

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
            Color base = posicion == 0 ? ORO : posicion == 1 ? PLATA : posicion == 2 ? BRONCE
                    : new Color(0x8A, 0x6E, 0x58);
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

    /** Trofeo dorado dibujado con figuras simples (no depende de emojis ni imágenes). */
    private static class Trofeo extends JComponent {

        Trofeo() {
            setPreferredSize(new Dimension(84, 92));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int ox = (getWidth() - 84) / 2, oy = (getHeight() - 92) / 2;
            g2.translate(ox, oy);

            Color oscuro = new Color(0xB0, 0x82, 0x3C);
            // asas
            g2.setColor(oscuro);
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(4, 12, 26, 28, 90, 180);
            g2.drawArc(54, 12, 26, 28, -90, 180);

            // copa
            Path2D copa = new Path2D.Double();
            copa.moveTo(16, 8);
            copa.lineTo(68, 8);
            copa.curveTo(68, 42, 56, 56, 42, 58);
            copa.curveTo(28, 56, 16, 42, 16, 8);
            copa.closePath();
            g2.setPaint(new GradientPaint(16, 0, new Color(0xFF, 0xE7, 0xA8), 68, 0, oscuro));
            g2.fill(copa);
            g2.setColor(new Color(0xFF, 0xF4, 0xD4, 160));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(26, 14, 28, 36);

            // tallo y base
            g2.setPaint(new GradientPaint(0, 58, ORO, 0, 72, oscuro));
            g2.fillRect(37, 57, 10, 14);
            g2.fillRoundRect(24, 70, 36, 8, 4, 4);
            g2.setColor(CAFE_OSCURO);
            g2.fillRoundRect(18, 78, 48, 12, 5, 5);
            g2.setColor(ORO);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(18, 78, 48, 12, 5, 5);

            // estrella
            g2.setColor(CAFE_OSCURO);
            g2.fill(estrella(42, 28, 10, 4.5));
            g2.dispose();
        }

        private static Path2D estrella(double cx, double cy, double rExt, double rInt) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double r = i % 2 == 0 ? rExt : rInt;
                double a = -Math.PI / 2 + i * Math.PI / 5;
                double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
                if (i == 0) {
                    p.moveTo(x, y);
                } else {
                    p.lineTo(x, y);
                }
            }
            p.closePath();
            return p;
        }
    }
}
