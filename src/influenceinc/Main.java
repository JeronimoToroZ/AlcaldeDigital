package influenceinc;

import influenceinc.gui.VentanaPrincipal;
import java.awt.Color;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * ALCALDE DIGITAL - MESA DE VERIFICACION (agencia Influence, Inc.)
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
            UIManager.put("ToolTip.background", new Color(42, 23, 18));
            UIManager.put("ToolTip.foreground", new Color(247, 235, 221));
            UIManager.put("OptionPane.messageFont", influenceinc.gui.Tema.fuente(java.awt.Font.PLAIN, 14));
            UIManager.put("OptionPane.buttonFont", influenceinc.gui.Tema.fuente(java.awt.Font.BOLD, 13));
        } catch (Exception e) {
            // aspecto por defecto
        }
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
