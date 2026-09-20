package alcaldedigital.gui;

import alcaldedigital.modelo.EstadoCiudad;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Barras de los seis indicadores de Ciudad Nova, dibujadas a mano. */
public class PanelIndicadores extends JPanel {

    private final EstadoCiudad ciudad;

    public PanelIndicadores(EstadoCiudad ciudad) {
        this.ciudad = ciudad;
        setPreferredSize(new Dimension(300, 280));
        setBackground(Tema.panel());
    }

    @Override
    protected void paintComponent(Graphics g) {
        setBackground(Tema.panel());
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Tema.texto());
        g2.setFont(Tema.subtitulo());
        g2.drawString("Estado de Ciudad Nova", 14, 26);

        String[] nombres = {"Informacion verificada", "Confianza ciudadana", "Convivencia",
                            "Bienestar digital", "Desinformacion", "Conflictos"};
        int[] valores = {ciudad.getInformacionVerificada(), ciudad.getConfianza(),
                         ciudad.getConvivencia(), ciudad.getBienestarDigital(),
                         ciudad.getDesinformacion(), ciudad.getConflictos()};
        boolean[] esBueno = {true, true, true, true, false, false};

        int y = 46;
        int anchoBarra = getWidth() - 40;
        for (int i = 0; i < nombres.length; i++) {
            g2.setFont(Tema.indicador());
            g2.setColor(Tema.texto());
            // el valor numerico va SIEMPRE: la lectura no depende del color
            g2.drawString(nombres[i] + ": " + valores[i] + (esBueno[i] ? " (+)" : " (-)"), 16, y);

            g2.setColor(Tema.borde());
            g2.drawRect(16, y + 5, anchoBarra, 11);

            int relleno = (int) (anchoBarra * (valores[i] / 100.0));
            g2.setColor(esBueno[i] ? Tema.positivo() : Tema.negativo());
            g2.fillRect(17, y + 6, Math.max(0, relleno - 1), 10);

            // patron diferenciador para los indicadores negativos (no solo color)
            if (!esBueno[i]) {
                g2.setColor(Tema.panel());
                for (int x = 20; x < 16 + relleno; x += 8) {
                    g2.drawLine(x, y + 6, x, y + 15);
                }
            }
            y += 30;
        }

        g2.setFont(Tema.subtitulo());
        g2.setColor(Tema.acento());
        g2.drawString("Salud global: " + ciudad.saludGlobal() + "/100", 16, y + 10);
    }
}
