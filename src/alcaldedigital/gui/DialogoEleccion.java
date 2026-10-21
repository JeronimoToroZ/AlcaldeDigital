package alcaldedigital.gui;

import alcaldedigital.logica.Partida;
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
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Día de elecciones: el resultado depende de cómo quedó la ciudad (salud global)
 * y de la reputación del jugador. Se habilita al ganar un nivel final del mapa.
 */
public class DialogoEleccion extends DialogoJuego {

    public enum Opcion { SEGUIR, INICIO }

    private Opcion opcion = Opcion.SEGUIR;

    public DialogoEleccion(Window padre, Partida partida) {
        super(padre, "¡Día de elecciones!");
        Jugador j = partida.getJugador();
        int votos = partida.porcentajeVotos();
        boolean gano = votos >= 50;

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        JLabel lblRes = etiqueta(gano ? j.getNombre() + " es el nuevo alcalde de Ciudad Nova"
                : "Esta vez no alcanzó para ganar la alcaldía", Font.BOLD, 20, gano ? VERDE : ROJO);
        lblRes.setAlignmentX(0.5f);
        cuerpo.add(lblRes);
        cuerpo.add(espacio(10));

        BarraVotos barra = new BarraVotos(votos);
        barra.setAlignmentX(0.5f);
        cuerpo.add(barra);
        cuerpo.add(espacio(10));

        JLabel veredicto = parrafo(partida.veredicto(), 520, 15, CREMA);
        veredicto.setHorizontalAlignment(SwingConstants.CENTER);
        veredicto.setAlignmentX(0.5f);
        cuerpo.add(veredicto);
        cuerpo.add(espacio(12));

        JPanel fila = new JPanel(new GridLayout(1, 2, 12, 0));
        fila.setOpaque(false);
        Tarjeta ciudad = new Tarjeta(new BorderLayout(0, 8));
        ciudad.add(etiqueta("Cómo quedó la ciudad", Font.BOLD, 15, ORO), BorderLayout.NORTH);
        ciudad.add(new IndicadoresMini(partida.getCiudad()), BorderLayout.CENTER);
        fila.add(ciudad);

        Tarjeta tu = new Tarjeta(new GridLayout(0, 1, 0, 6));
        tu.add(etiqueta("Tu campaña", Font.BOLD, 15, ORO));
        tu.add(etiqueta("Puntos: " + j.getPuntos(), Font.PLAIN, 15, CREMA));
        tu.add(etiqueta("Reputación: " + j.getReputacion() + " / 100", Font.PLAIN, 15, CREMA));
        tu.add(etiqueta("Decisiones acertadas: " + j.getAciertos(), Font.PLAIN, 15, CREMA));
        tu.add(etiqueta("Decisiones erradas: " + j.getErrores(), Font.PLAIN, 15, CREMA));
        tu.add(parrafo("Votos = 60% salud de la ciudad + 40% tu reputación.", 200, 12, CREMA_SUAVE));
        fila.add(tu);
        fila.setAlignmentX(0.5f);
        cuerpo.add(fila);

        setContenido(cuerpo);

        BotonDorado seguir = new BotonDorado("Seguir jugando", true);
        seguir.addActionListener(e -> { opcion = Opcion.SEGUIR; dispose(); });
        BotonDorado inicio = new BotonDorado("Volver al inicio", false);
        inicio.addActionListener(e -> { opcion = Opcion.INICIO; dispose(); });
        agregarBoton(seguir);
        agregarBoton(inicio);
        getRootPane().setDefaultButton(seguir);
    }

    public Opcion elegir() {
        mostrar();
        return opcion;
    }

    /** Barra horizontal con el porcentaje de votos y la meta del 50%. */
    private static class BarraVotos extends JComponent {

        private final int votos;

        BarraVotos(int votos) {
            this.votos = votos;
            setPreferredSize(new Dimension(480, 64));
            setMaximumSize(new Dimension(480, 64));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), y = 26, alto = 24;
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRoundRect(0, y, w, alto, alto, alto);
            Color a = votos >= 50 ? new Color(0xB8, 0xEB, 0xBF) : new Color(0xF7, 0xC2, 0xBC);
            Color b = votos >= 50 ? new Color(0x4C, 0xAF, 0x6A) : new Color(0xD3, 0x3A, 0x2F);
            g2.setPaint(new GradientPaint(0, y, a, 0, y + alto, b));
            g2.fillRoundRect(0, y, w * votos / 100, alto, alto, alto);
            g2.setColor(ORO);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(0, y, w - 1, alto, alto, alto);
            // meta del 50 %
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4, 4}, 0));
            g2.drawLine(w / 2, y - 4, w / 2, y + alto + 4);
            g2.setFont(fuente(Font.BOLD, 20));
            String s = votos + "% de los votos";
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(CREMA);
            g2.drawString(s, (w - fm.stringWidth(s)) / 2, 19);
            g2.setFont(fuente(Font.PLAIN, 11));
            g2.setColor(CREMA_SUAVE);
            g2.drawString("meta 50%", w / 2 + 6, y + alto + 14);
            g2.dispose();
        }
    }
}
