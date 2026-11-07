package alcaldedigital.gui;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * Panel que pinta una imagen de fondo.
 *
 * Antes estiraba la imagen a la fuerza hasta el tamano del panel. Con el mapa
 * de la ciudad eso lo deformaba: el mapa es 16:9 y el panel casi nunca lo es,
 * asi que los edificios salian achatados o estirados.
 *
 * Ahora respeta la proporcion. Hay dos formas de hacerlo y cada una sirve para
 * algo distinto:
 *
 *   CONTENER - la imagen entra completa y sobran franjas a los lados. Es la que
 *              se usa con el mapa, porque su marco decorativo es parte del
 *              dibujo y recortarlo seria una lastima.
 *   CUBRIR   - la imagen llena todo el panel y lo que sobra se recorta. Sirve
 *              para fondos ambientales donde no importa perder los bordes.
 *
 * @author Naty (original), ajustado para no deformar el mapa
 */
public class PanelConFondo extends JPanel {

    /** Como encajar la imagen en el panel. */
    public enum Ajuste {
        /** Entra completa; pueden quedar franjas del color de fondo. */
        CONTENER,
        /** Llena el panel; se recorta lo que sobra. */
        CUBRIR,
        /** Se estira hasta el tamano del panel, deformandola. */
        ESTIRAR
    }

    private Image imagenFondo;
    private Ajuste ajuste = Ajuste.ESTIRAR;

    public PanelConFondo(LayoutManager layout, String rutaImagen) {
        this(layout, rutaImagen, Ajuste.ESTIRAR);
    }

    public PanelConFondo(LayoutManager layout, String rutaImagen, Ajuste ajuste) {
        super(layout);
        this.ajuste = ajuste;
        java.net.URL urlImagen = getClass().getResource(rutaImagen);
        if (urlImagen != null) {
            this.imagenFondo = new ImageIcon(urlImagen).getImage();
        }
    }

    /**
     * Rectangulo donde queda dibujada la imagen dentro del panel.
     * Lo necesita quien dibuje encima: sin esto, los nodos del grafo se
     * posicionan contra el panel y no contra el mapa, y quedan corridos
     * respecto a las ubicaciones.
     *
     * @return {x, y, ancho, alto}, o null si no hay imagen
     */
    public int[] areaDeLaImagen() {
        return areaDeLaImagenEn(getWidth(), getHeight());
    }

    /**
     * Igual que areaDeLaImagen, pero contra un tamano dado.
     *
     * Hace falta porque quien dibuja encima del mapa es un panel HIJO, y el
     * hijo no mide lo mismo que este: cualquier borde o inset lo encoge. Si el
     * hijo usara el area calculada contra el tamano del padre, todo lo que
     * pinte quedaria corrido unos pixeles respecto al mapa.
     */
    public int[] areaDeLaImagenEn(int w, int h) {
        if (imagenFondo == null) {
            return null;
        }
        int iw = imagenFondo.getWidth(null);
        int ih = imagenFondo.getHeight(null);
        if (iw <= 0 || ih <= 0 || w <= 0 || h <= 0) {
            return null;
        }
        if (ajuste == Ajuste.ESTIRAR) {
            return new int[]{0, 0, w, h};
        }
        double escala = ajuste == Ajuste.CONTENER
                ? Math.min(w / (double) iw, h / (double) ih)
                : Math.max(w / (double) iw, h / (double) ih);
        int dw = (int) Math.round(iw * escala);
        int dh = (int) Math.round(ih * escala);
        return new int[]{(w - dw) / 2, (h - dh) / 2, dw, dh};
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int[] a = areaDeLaImagen();
        if (a == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (ajuste == Ajuste.CUBRIR) {
            g2.setClip(0, 0, getWidth(), getHeight());
        }
        g2.drawImage(imagenFondo, a[0], a[1], a[2], a[3], this);
        g2.dispose();
    }
}
