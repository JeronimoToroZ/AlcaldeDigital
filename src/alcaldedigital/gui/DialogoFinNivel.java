package alcaldedigital.gui;

import alcaldedigital.logica.Partida;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Path2D;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Resultado de un nivel: estrellas (una por vida que quedó), cifras del nivel,
 * cómo quedó el rumor en la red de la ciudad y qué niveles se desbloquearon.
 */
public class DialogoFinNivel extends DialogoJuego {

    public enum Opcion { MAPA, REINTENTAR }

    private Opcion opcion = Opcion.MAPA;

    public DialogoFinNivel(Window padre, Partida partida, int nivel, PanelPlataformas.ResultadoNivel r,
                           List<Integer> desbloqueados) {
        super(padre, r.gano ? "¡Nivel " + nivel + " superado!" : "Nivel " + nivel + " no superado");

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        Estrellas estrellas = new Estrellas(r.gano ? Math.max(1, r.vidas) : 0);
        estrellas.setAlignmentX(0.5f);
        cuerpo.add(estrellas);

        String frase = r.gano
                ? (r.vidas == 3 ? "¡Perfecto! No perdiste ninguna vida." : "Bien hecho. Cada decisión cuenta.")
                : "Resolviste " + r.publicacionesResueltas + " de 4 publicaciones. ¡Inténtalo de nuevo!";
        JLabel lblFrase = etiqueta(frase, Font.ITALIC, 16, CREMA);
        lblFrase.setAlignmentX(0.5f);
        cuerpo.add(lblFrase);
        cuerpo.add(espacio(14));

        JPanel cifras = new JPanel(new GridLayout(1, 4, 10, 0));
        cifras.setOpaque(false);
        cifras.add(cifra("Puntos", (r.puntos >= 0 ? "+" : "") + r.puntos, r.puntos >= 0 ? ORO : ROJO));
        cifras.add(cifra("Vidas", r.vidas + " / 3", r.vidas > 0 ? VERDE : ROJO));
        cifras.add(cifra("Correctas", String.valueOf(r.correctas), VERDE));
        cifras.add(cifra("Errores", String.valueOf(r.errores), r.errores == 0 ? VERDE : ROJO));
        cifras.setAlignmentX(0.5f);
        cuerpo.add(cifras);
        cuerpo.add(espacio(12));

        int total = partida.getRed().nodos.size();
        Tarjeta red = new Tarjeta(new BorderLayout(0, 4), r.lugaresConRumor > total / 2 ? ROJO : null);
        red.add(etiqueta("Red de Ciudad Nova", Font.BOLD, 15, ORO), BorderLayout.NORTH);
        String textoRed = r.lugaresConRumor == 0
                ? "Contuviste el rumor por completo: ningún lugar de la ciudad quedó desinformado."
                : "El rumor quedó circulando en " + r.lugaresConRumor + " de " + total + " lugares. "
                + "Salud de la ciudad: " + partida.getCiudad().saludGlobal() + "/100.";
        red.add(parrafo(textoRed, 520, 14, CREMA), BorderLayout.CENTER);
        red.setAlignmentX(0.5f);
        cuerpo.add(red);

        if (!desbloqueados.isEmpty()) {
            cuerpo.add(espacio(12));
            Tarjeta nuevos = new Tarjeta(new BorderLayout(0, 6), VERDE);
            nuevos.add(etiqueta("¡Nuevos niveles desbloqueados!", Font.BOLD, 15, VERDE), BorderLayout.NORTH);
            JPanel iconos = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
            iconos.setOpaque(false);
            for (int n : desbloqueados) {
                ImageIcon ic = icono("n" + n + ".png", 54);
                JLabel l = new JLabel("Nivel " + n, ic, SwingConstants.CENTER);
                l.setVerticalTextPosition(SwingConstants.BOTTOM);
                l.setHorizontalTextPosition(SwingConstants.CENTER);
                l.setFont(fuente(Font.BOLD, 13));
                l.setForeground(CREMA);
                iconos.add(l);
            }
            nuevos.add(iconos, BorderLayout.CENTER);
            nuevos.setAlignmentX(0.5f);
            cuerpo.add(nuevos);
        } else if (r.gano && partida.getNiveles().esHoja(nivel)) {
            cuerpo.add(espacio(12));
            JLabel fin = parrafo("¡Llegaste al final de esta rama de la campaña! Ya puedes ver los resultados "
                    + "de las elecciones desde el mapa.", 520, 14, ORO);
            fin.setAlignmentX(0.5f);
            cuerpo.add(fin);
        }

        setContenido(cuerpo);

        if (r.gano) {
            BotonDorado mapa = new BotonDorado("Volver al mapa", true);
            mapa.addActionListener(e -> { opcion = Opcion.MAPA; dispose(); });
            BotonDorado repetir = new BotonDorado("Repetir nivel", false);
            repetir.addActionListener(e -> { opcion = Opcion.REINTENTAR; dispose(); });
            agregarBoton(mapa);
            agregarBoton(repetir);
            getRootPane().setDefaultButton(mapa);
        } else {
            BotonDorado reintentar = new BotonDorado("Reintentar", true);
            reintentar.addActionListener(e -> { opcion = Opcion.REINTENTAR; dispose(); });
            BotonDorado mapa = new BotonDorado("Volver al mapa", false);
            mapa.addActionListener(e -> { opcion = Opcion.MAPA; dispose(); });
            agregarBoton(reintentar);
            agregarBoton(mapa);
            getRootPane().setDefaultButton(reintentar);
        }
    }

    public Opcion elegir() {
        mostrar();
        return opcion;
    }

    private static JPanel cifra(String nombre, String valor, Color color) {
        Tarjeta t = new Tarjeta(new GridLayout(2, 1, 0, 0));
        t.setPreferredSize(new Dimension(110, 70));
        JLabel v = etiqueta(valor, Font.BOLD, 20, color);
        v.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel n = etiqueta(nombre, Font.PLAIN, 12, CREMA_SUAVE);
        n.setHorizontalAlignment(SwingConstants.CENTER);
        t.add(v);
        t.add(n);
        return t;
    }

    /** Tres estrellas dibujadas; se rellenan las ganadas. */
    static class Estrellas extends JComponent {

        private final int ganadas;

        Estrellas(int ganadas) {
            this.ganadas = ganadas;
            setPreferredSize(new Dimension(220, 74));
            setMaximumSize(new Dimension(220, 74));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2;
            double[][] pos = {{cx - 66, 42, 24}, {cx, 34, 32}, {cx + 66, 42, 24}};
            for (int i = 0; i < 3; i++) {
                Path2D e = estrella(pos[i][0], pos[i][1], pos[i][2], pos[i][2] * 0.45);
                if (i < ganadas) {
                    g2.setPaint(new GradientPaint(0, (float) (pos[i][1] - pos[i][2]), new Color(0xFF, 0xE7, 0xA8),
                            0, (float) (pos[i][1] + pos[i][2]), new Color(0xC9, 0x8E, 0x2B)));
                    g2.fill(e);
                } else {
                    g2.setColor(new Color(0, 0, 0, 70));
                    g2.fill(e);
                }
                g2.setColor(ORO);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(e);
            }
            g2.dispose();
        }

        static Path2D estrella(double cx, double cy, double rExt, double rInt) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double r = i % 2 == 0 ? rExt : rInt;
                double a = -Math.PI / 2 + i * Math.PI / 5;
                if (i == 0) {
                    p.moveTo(cx + r * Math.cos(a), cy + r * Math.sin(a));
                } else {
                    p.lineTo(cx + r * Math.cos(a), cy + r * Math.sin(a));
                }
            }
            p.closePath();
            return p;
        }
    }
}
