package alcaldedigital.gui;

import alcaldedigital.juego.Personaje;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Dibuja al personaje con las imágenes de pixel art:
 *   p1.png = quieto, p2.png = pie derecho delante, p3.png = pie izquierdo delante.
 *
 * Al caminar alterna p2 y p3 según la distancia recorrida (así el paso va al ritmo
 * del movimiento), en el aire usa p2 y quieto usa p1. Las imágenes miran a la
 * derecha; para ir a la izquierda se usan copias reflejadas. Se escalan con
 * "vecino más cercano" para que el pixel art se vea nítido y no borroso.
 */
public class SpritePersonaje {

    private static final String RUTA = "/alcaldedigital/recursos/";
    private static final double PIXELES_POR_PASO = 14;

    private final BufferedImage[] derecha = new BufferedImage[3];
    private final BufferedImage[] izquierda = new BufferedImage[3];
    private final BufferedImage[] siluetaDer = new BufferedImage[3];
    private final BufferedImage[] siluetaIzq = new BufferedImage[3];
    private final boolean disponible;

    public SpritePersonaje() {
        BufferedImage[] originales = new BufferedImage[3];
        boolean ok = true;
        for (int i = 0; i < 3; i++) {
            originales[i] = BotonImagen.cargar(RUTA + "p" + (i + 1) + ".png");
            ok &= originales[i] != null;
        }
        disponible = ok;
        if (!ok) {
            return;
        }
        // recorte común a las 3 imágenes (para que no "salten" al cambiar de cuadro)
        Rectangle caja = null;
        for (BufferedImage img : originales) {
            Rectangle r = cajaVisible(img);
            caja = caja == null ? r : caja.union(r);
        }
        for (int i = 0; i < 3; i++) {
            derecha[i] = originales[i].getSubimage(caja.x, caja.y, caja.width, caja.height);
            izquierda[i] = reflejar(derecha[i]);
            siluetaDer[i] = silueta(derecha[i]);
            siluetaIzq[i] = silueta(izquierda[i]);
        }
    }

    /** true si las tres imágenes se cargaron; si no, el juego usa el personaje dibujado a mano. */
    public boolean isDisponible() {
        return disponible;
    }

    /** Dibuja el cuadro que toca, ajustado a la caja de choque del personaje. */
    public void dibujar(Graphics2D g2, Personaje p, boolean contraste) {
        int cuadro;
        if (!p.enSuelo) {
            cuadro = 1;                                   // en el aire: una pierna adelante
        } else if (Math.abs(p.vx) > 0.3) {
            cuadro = ((int) (p.getDistanciaRecorrida() / PIXELES_POR_PASO)) % 2 == 0 ? 1 : 2;
        } else {
            cuadro = 0;                                   // quieto
        }
        BufferedImage img = (p.mirandoDerecha ? derecha : izquierda)[cuadro];

        double escala = Personaje.ALTO / img.getHeight();
        int w = (int) Math.round(img.getWidth() * escala);
        int h = (int) Math.round(img.getHeight() * escala);
        int x = (int) Math.round(p.x + (Personaje.ANCHO - w) / 2.0);
        int y = (int) Math.round(p.y + Personaje.ALTO - h);

        Object antes = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (contraste) {
            // borde blanco alrededor del personaje para que resalte sobre el fondo negro
            BufferedImage s = (p.mirandoDerecha ? siluetaDer : siluetaIzq)[cuadro];
            int[][] desplazamientos = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
            for (int[] d : desplazamientos) {
                g2.drawImage(s, x + d[0], y + d[1], w, h, null);
            }
        }
        g2.drawImage(img, x, y, w, h, null);
        if (antes != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, antes);
        }
    }

    // ------------------------------------------------------------------

    private static Rectangle cajaVisible(BufferedImage img) {
        int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 10) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) {
            return new Rectangle(0, 0, img.getWidth(), img.getHeight());
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static BufferedImage reflejar(BufferedImage img) {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                out.setRGB(img.getWidth() - 1 - x, y, img.getRGB(x, y));
            }
        }
        return out;
    }

    /** Copia toda blanca (misma forma) para el borde del modo alto contraste. */
    private static BufferedImage silueta(BufferedImage img) {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int a = img.getRGB(x, y) >>> 24;
                out.setRGB(x, y, (a << 24) | 0xFFFFFF);
            }
        }
        return out;
    }
}
