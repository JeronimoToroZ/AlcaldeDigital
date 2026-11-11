package influenceinc;

import influenceinc.gui.VentanaPrincipal;
import java.awt.Color;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * INFLUENCE, INC. - MESA DE VERIFICACION
 * Videojuego educativo sobre el uso responsable de las redes sociales:
 * eres verificador en una agencia durante la semana de elecciones de Ciudad
 * Nova. La gente llega a tu ventanilla; tu investigas y sellas.
 * Universidad del Norte - Estructura de Datos II - 2026.
 *
 * Java puro (Swing + Java2D), sin librerias externas. El escaner de red
 * usa un motor 3D propio (paquete motor3d).
 */
public class Main {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("ToolTip.background", new Color(20, 22, 50));
            UIManager.put("ToolTip.foreground", Color.WHITE);
        } catch (Exception e) {
            // aspecto por defecto
        }
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
