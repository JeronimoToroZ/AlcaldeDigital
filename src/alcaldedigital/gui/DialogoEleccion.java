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
import java.util.ArrayList;
import java.util.List;
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
        List<Jugador> jugadores = new ArrayList<>(partida.getJugadores());
        jugadores.sort((x, y) -> Integer.compare(partida.porcentajeVotos(y), partida.porcentajeVotos(x)));
        Jugador electo = partida.alcaldeElecto();
        boolean individual = jugadores.size() == 1;
        boolean gano = !individual || partida.porcentajeVotos(electo) >= 50;

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        JLabel lblRes = etiqueta(gano ? electo.getNombre() + " es el nuevo alcalde de Ciudad Nova"
                : "Esta vez no alcanzó para ganar la alcaldía", Font.BOLD, 22, gano ? VERDE : ROJO);
        lblRes.setAlignmentX(0.5f);
        cuerpo.add(lblRes);
        cuerpo.add(espacio(10));

        // una barra de votos por jugador, de más a menos votos
        for (Jugador j : jugadores) {
            BarraVotos barra = new BarraVotos(j.getNombre() + " (" + j.getRol().getEtiqueta() + ")",
                    partida.porcentajeVotos(j), j == electo && gano, individual);
            barra.setAlignmentX(0.5f);
            cuerpo.add(barra);
        }
        cuerpo.add(espacio(8));

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

        Tarjeta campanas = new Tarjeta(null);
        campanas.setLayout(new BoxLayout(campanas, BoxLayout.Y_AXIS));
        campanas.add(etiqueta("Campañas", Font.BOLD, 15, ORO));
        campanas.add(espacio(8));
        for (Jugador j : jugadores) {
            campanas.add(etiqueta(j.getNombre() + ": " + j.getPuntos() + " pts · rep " + j.getReputacion(),
                    Font.PLAIN, 14, CREMA));
            campanas.add(espacio(4));
        }
        campanas.add(espacio(6));
        campanas.add(parrafo(individual
                ? "Votos = 60% salud de la ciudad + 40% tu reputación."
                : "Los votos se reparten según los puntos y la reputación de cada uno, "
                  + "y pesan más si la ciudad quedó sana.", 230, 12, CREMA_SUAVE));
        fila.add(campanas);
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

    /** Barra horizontal con el nombre, el porcentaje de votos y (si juega uno solo) la meta del 50 %. */
    private static class BarraVotos extends JComponent {

        private final String nombre;
        private final int votos;
        private final boolean destacado;
        private final boolean conMeta;

        BarraVotos(String nombre, int votos, boolean destacado, boolean conMeta) {
            this.nombre = nombre;
            this.votos = votos;
            this.destacado = destacado;
            this.conMeta = conMeta;
            setPreferredSize(new Dimension(520, 52));
            setMaximumSize(new Dimension(520, 52));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), y = 24, alto = 18;
            g2.setFont(fuente(destacado ? Font.BOLD : Font.PLAIN, 15));
            g2.setColor(destacado ? ORO : CREMA);
            g2.drawString(nombre, 0, 17);
            String s = votos + "%";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, w - fm.stringWidth(s), 17);
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRoundRect(0, y, w, alto, alto, alto);
            Color a = destacado ? new Color(0xFF, 0xE7, 0xA8) : new Color(0xE6, 0xD6, 0xBE);
            Color b = destacado ? new Color(0xC9, 0x8E, 0x2B) : new Color(0x8A, 0x6E, 0x58);
            g2.setPaint(new GradientPaint(0, y, a, 0, y + alto, b));
            g2.fillRoundRect(0, y, Math.max(alto, w * votos / 100), alto, alto, alto);
            g2.setColor(ORO);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, y, w - 1, alto, alto, alto);
            if (conMeta) {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4, 4}, 0));
                g2.drawLine(w / 2, y - 3, w / 2, y + alto + 3);
            }
            g2.dispose();
        }
    }
}
