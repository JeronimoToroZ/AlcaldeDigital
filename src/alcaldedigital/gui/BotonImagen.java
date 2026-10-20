package alcaldedigital.gui;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JButton;

/**
 * Botón que se ve como una imagen PNG (respeta la transparencia). Se aclara
 * un poco al pasar el mouse. El tamaño se fija con {@link #ajustarAncho(int)}
 * y la altura se calcula conservando la proporción de la imagen.
 */
public class BotonImagen extends JButton {

    private final BufferedImage original;
    private BufferedImage normal;
    private BufferedImage resaltado;

    public BotonImagen(String ruta, String texto) {
        this(ruta, texto, false);
    }

    /**
     * @param recortar true para quitar los márgenes transparentes de la imagen
     *                 (útil si el PNG trae mucho espacio vacío alrededor, como ayuda.png).
     */
    public BotonImagen(String ruta, String texto, boolean recortar) {
        BufferedImage img = cargar(ruta);
        original = (recortar && img != null) ? recortarTransparencia(img) : img;
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (original == null) {
            setText(texto);   // si falta la imagen, el botón sigue funcionando
        }
    }

    /** Carga una imagen del classpath; devuelve null si no existe. */
    public static BufferedImage cargar(String ruta) {
        try {
            return ImageIO.read(BotonImagen.class.getResource(ruta));
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("No se pudo cargar la imagen: " + ruta);
            return null;
        }
    }

    /** Devuelve la imagen recortada al rectángulo que contiene píxeles visibles. */
    public static BufferedImage recortarTransparencia(BufferedImage img) {
        int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 10) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return img;   // imagen totalmente transparente: se deja igual
        }
        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /** Alto que tendría el botón con ese ancho, conservando la proporción. */
    public int altoParaAncho(int ancho) {
        return original == null ? 44 : ancho * original.getHeight() / original.getWidth();
    }

    /** Coloca el botón con su centro en (cx, cy) y el ancho indicado. */
    public void centrarEn(int cx, int cy, int ancho) {
        int alto = altoParaAncho(ancho);
        setBounds(cx - ancho / 2, cy - alto / 2, ancho, alto);
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        if (original != null && w > 0 && h > 0
                && (normal == null || normal.getWidth() != w || normal.getHeight() != h)) {
            normal = escalar(original, w, h);
            resaltado = new RescaleOp(new float[]{1.15f, 1.15f, 1.15f, 1f},
                    new float[4], null).filter(normal, null);
        }
    }

    private static BufferedImage escalar(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (normal == null) {
            super.paintComponent(g);
            return;
        }
        g.drawImage(getModel().isRollover() ? resaltado : normal, 0, 0, null);
    }
}
