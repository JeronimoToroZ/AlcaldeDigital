package alcaldedigital.gui;
 
import alcaldedigital.modelo.EstadoCiudad;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
 
/**
 * Panel gráfico encargado de renderizar manualmente las barras de estado
 * de los seis indicadores clave de Ciudad Nova (métricas positivas y negativas),
 * incluyendo sus valores numéricos y el cálculo de la salud global.
 *
 * @author Naty
 */
public class PanelIndicadores extends JPanel {
 
    private final EstadoCiudad ciudad;
 
    /**
     * Inicializa el panel de indicadores configurando las dimensiones predeterminadas
     * y el color de fondo correspondiente al tema visual.
     */
    public PanelIndicadores(EstadoCiudad ciudad) {
        this.ciudad = ciudad;
        setPreferredSize(new Dimension(300, 280));
        setMinimumSize(new Dimension(300, 230));
        setBackground(Tema.panel());
    }
 
    /**
     * Dibuja de manera personalizada los títulos, las etiquetas con sus valores numéricos,
     * las barras de progreso proporcionales y los patrones de advertencia para indicadores negativos.
     */
    @Override
    protected void paintComponent(Graphics g) {
        setBackground(Tema.panel());
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Tema.suavizar(g2);
 
        // Título principal del panel de estado
        g2.setColor(Tema.texto());
        g2.setFont(Tema.subtitulo());
        g2.drawString("ESTADO DE CIUDAD NOVA", 14, 26);
        g2.setColor(Tema.borde());
        g2.setStroke(new java.awt.BasicStroke(2f));
        g2.drawLine(14, 34, getWidth() - 14, 34);
        g2.setStroke(new java.awt.BasicStroke(1f));
 
        String[] nombres = {"Información verificada", "Confianza ciudadana", "Convivencia",
                            "Bienestar digital", "Desinformación", "Conflictos"};
        int[] valores = {ciudad.getInformacionVerificada(), ciudad.getConfianza(),
                         ciudad.getConvivencia(), ciudad.getBienestarDigital(),
                         ciudad.getDesinformacion(), ciudad.getConflictos()};
        boolean[] esBueno = {true, true, true, true, false, false};
 
        int y = 50;
        int anchoBarra = getWidth() - 40;
 
        // El espaciado se ajusta a la altura disponible para que nada quede cortado
        // (deja espacio abajo para la línea de "Salud global").
        int paso = Math.max(24, Math.min(32, (getHeight() - y - 44) / nombres.length));
 
        // Iteración para dibujar cada una de las barras de los indicadores
        for (int i = 0; i < nombres.length; i++) {
            g2.setFont(Tema.indicador());
            g2.setColor(Tema.texto());
 
            // El valor numérico y etiqueta se muestran siempre para garantizar legibilidad independiente del color
            g2.drawString(nombres[i] + ": " + valores[i] + (esBueno[i] ? " (+)" : " (-)"), 16, y);
 
            g2.setColor(Tema.borde());
            g2.drawRect(16, y + 5, anchoBarra, 11);
 
            int relleno = (int) (anchoBarra * (valores[i] / 100.0));
            g2.setColor(esBueno[i] ? Tema.positivo() : Tema.negativo());
            g2.fillRect(17, y + 6, Math.max(0, relleno - 1), 10);
 
            // Patrón visual diferenciador para los indicadores negativos (no depende únicamente del color)
            if (!esBueno[i]) {
                g2.setColor(Tema.panel());
                for (int x = 20; x < 16 + relleno; x += 8) {
                    g2.drawLine(x, y + 6, x, y + 15);
                }
            }
            y += paso;
        }
 
        // Renderizado final de la salud global de la ciudad
        g2.setFont(Tema.subtitulo());
        g2.setColor(Tema.acento());
        g2.drawString("Salud global: " + ciudad.saludGlobal() + "/100", 16, y + 10);
 
        // Marco de tinta alrededor del panel
        g2.setColor(Tema.borde());
        g2.setStroke(new java.awt.BasicStroke(2f));
        g2.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
        g2.setStroke(new java.awt.BasicStroke(1f));
    }
}
 