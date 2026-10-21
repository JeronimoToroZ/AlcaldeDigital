package alcaldedigital.gui;

import alcaldedigital.modelo.EstadoCiudad;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * Versión compacta de los 6 indicadores de Ciudad Nova, con la paleta café y
 * dorada de las ventanas del juego. Los indicadores "malos" (desinformación y
 * conflictos) llevan una marca ▼ para no depender solo del color.
 */
public class IndicadoresMini extends JComponent {

    private static final String[] NOMBRES = {"Info verificada", "Confianza", "Convivencia",
                                             "Bienestar digital", "Desinformación", "Conflictos"};

    private final EstadoCiudad ciudad;

    public IndicadoresMini(EstadoCiudad ciudad) {
        this.ciudad = ciudad;
        setPreferredSize(new Dimension(250, 6 * 30 + 26));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean contraste = Tema.isAltoContraste();
        int[] valores = {ciudad.getInformacionVerificada(), ciudad.getConfianza(), ciudad.getConvivencia(),
                         ciudad.getBienestarDigital(), ciudad.getDesinformacion(), ciudad.getConflictos()};
        int w = getWidth();
        int y = 0;
        Font f = new Font(DialogoJuego.FUENTE, Font.PLAIN, contraste ? 13 : 12);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < 6; i++) {
            boolean malo = i >= 4;
            String nombre = (malo ? "▼ " : "") + NOMBRES[i];
            g2.setColor(contraste ? Color.WHITE : DialogoJuego.CREMA);
            g2.drawString(nombre, 0, y + fm.getAscent());
            String v = String.valueOf(valores[i]);
            g2.drawString(v, w - fm.stringWidth(v), y + fm.getAscent());
            int by = y + fm.getHeight() + 2;
            g2.setColor(contraste ? new Color(70, 70, 70) : new Color(255, 255, 255, 35));
            g2.fillRoundRect(0, by, w, 7, 7, 7);
            Color c;
            if (malo) {
                c = valores[i] > 40 ? new Color(0xF0, 0x8A, 0x80) : new Color(0xE0, 0xB0, 0x70);
            } else {
                c = valores[i] >= 60 ? new Color(0x8F, 0xD6, 0x9A) : new Color(0xE0, 0xB0, 0x70);
            }
            if (contraste) {
                c = malo ? new Color(255, 128, 0) : new Color(0, 255, 128);
            }
            g2.setColor(c);
            g2.fillRoundRect(0, by, w * valores[i] / 100, 7, 7, 7);
            y += 30;
        }
        g2.setFont(new Font(DialogoJuego.FUENTE, Font.BOLD, 13));
        g2.setColor(contraste ? new Color(255, 255, 0) : DialogoJuego.ORO);
        g2.drawString("Salud de la ciudad: " + ciudad.saludGlobal() + " / 100", 0, y + 14);
        g2.dispose();
    }
}
