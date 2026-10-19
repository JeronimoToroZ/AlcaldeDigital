/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package alcaldedigital.gui;
import javax.swing.*;
import java.awt.*;
/**
 *
 * @author Naty
 */

public class PanelConFondo extends JPanel {
    private Image imagenFondo;

    public PanelConFondo(LayoutManager layout, String rutaImagen) {
        super(layout);
        java.net.URL urlImagen = getClass().getResource(rutaImagen);
        if (urlImagen != null) {
            this.imagenFondo = new ImageIcon(urlImagen).getImage();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (imagenFondo != null) {
            g.drawImage(imagenFondo, 0, 0, getWidth(), getHeight(), this);
        }
    }
}

