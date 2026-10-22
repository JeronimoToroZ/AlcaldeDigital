package alcaldedigital.gui;

import alcaldedigital.juego.Personaje;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Dibuja al personaje con sus 10 imágenes (carpeta recursos):
 *
 *   p1 / p2   corriendo a la derecha / izquierda (pierna derecha adelante)
 *   p3 / p4   corriendo a la derecha / izquierda (pierna izquierda adelante)
 *   p5 / p6   quieto mirando a la derecha / izquierda
 *   p8 / p7   saltando a la derecha / izquierda
 *   p9 / p10  aterrizando (agachado) a la derecha / izquierda
 *
 * Cómo se elige el cuadro:
 *   - acaba de caer de un salto  -> aterrizando (unos pocos ticks)
 *   - en el aire                 -> saltando
 *   - caminando                  -> alterna las dos de correr según la distancia recorrida
 *   - quieto                     -> quieto
 *
 * Las imágenes son grandes (unos 700 px de alto) y en pantalla miden ~70 px.
 * Para que no se vean pixeladas ni borrosas, se reducen UNA vez con buena calidad
 * (reducción por pasos) al tamaño exacto de la pantalla y se guardan en caché.
 */
public class SpritePersonaje {

    private static final String RUTA = "/alcaldedigital/recursos/";

    // índices de cuadro (posición en el arreglo de imágenes)
    private static final int CORRER_A = 0, CORRER_B = 1, QUIETO = 2, SALTO = 3, ATERRIZAJE = 4;

    /** Archivo de cada cuadro mirando a la DERECHA y a la IZQUIERDA. Cambiar aquí si alguna está al revés. */
    private static final String[] DERECHA = {"p1", "p3", "p5", "p8", "p9"};
    private static final String[] IZQUIERDA = {"p2", "p4", "p6", "p7", "p10"};

    /** Alto en pantalla del personaje de pie, en píxeles "del mundo" (un poco más que su caja de choque por el gorro). */
    private static final double ALTO_VISUAL = Personaje.ALTO + 6;
    private static final double PIXELES_POR_PASO = 16;

    private final BufferedImage[] der = new BufferedImage[5];
    private final BufferedImage[] izq = new BufferedImage[5];
    private final boolean disponible;
    /** Cuántos "píxeles del mundo" vale un píxel de la imagen original. */
    private double factor;

    /** Imágenes ya reducidas para el tamaño de pantalla actual (clave = alto en píxeles). */
    private final Map<Integer, BufferedImage[]> cacheDer = new HashMap<>();
    private final Map<Integer, BufferedImage[]> cacheIzq = new HashMap<>();
    private final Map<Integer, BufferedImage[]> cacheSilDer = new HashMap<>();
    private final Map<Integer, BufferedImage[]> cacheSilIzq = new HashMap<>();

    public SpritePersonaje() {
        boolean ok = true;
        for (int i = 0; i < 5; i++) {
            der[i] = recortar(BotonImagen.cargar(RUTA + DERECHA[i] + ".png"));
            izq[i] = recortar(BotonImagen.cargar(RUTA + IZQUIERDA[i] + ".png"));
            ok &= der[i] != null && izq[i] != null;
        }
        disponible = ok;
        if (ok) {
            // la escala sale de la imagen "quieto": así todas guardan la misma proporción
            // y la de aterrizaje se ve más bajita (está agachado), como debe ser
            factor = ALTO_VISUAL / der[QUIETO].getHeight();
        }
    }

    /** true si están las 10 imágenes; si falta alguna, el juego dibuja un personaje simple. */
    public boolean isDisponible() {
        return disponible;
    }

    public void dibujar(Graphics2D g2, Personaje p, boolean contraste) {
        int cuadro;
        if (p.ticksAterrizaje > 0 && p.enSuelo) {
            cuadro = ATERRIZAJE;
        } else if (!p.enSuelo) {
            cuadro = SALTO;
        } else if (Math.abs(p.vx) > 0.4) {
            cuadro = ((int) (p.getDistanciaRecorrida() / PIXELES_POR_PASO)) % 2 == 0 ? CORRER_A : CORRER_B;
        } else {
            cuadro = QUIETO;
        }
        BufferedImage original = (p.mirandoDerecha ? der : izq)[cuadro];

        // tamaño en el mundo, anclado abajo y al centro de la caja de choque
        double w = original.getWidth() * factor;
        double h = original.getHeight() * factor;
        double x = p.x + Personaje.ANCHO / 2.0 - w / 2.0;
        double y = p.y + Personaje.ALTO - h + 1;

        // tamaño real en pantalla (el Graphics ya viene escalado por la cámara)
        double escalaPantalla = g2.getTransform().getScaleY();
        int altoPx = Math.max(8, (int) Math.round(ALTO_VISUAL * escalaPantalla));
        BufferedImage img = reducida(p.mirandoDerecha, cuadro, altoPx, false);

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (contraste) {
            // borde blanco para que resalte sobre el fondo negro del alto contraste
            BufferedImage s = reducida(p.mirandoDerecha, cuadro, altoPx, true);
            double d = 2 / escalaPantalla;
            double[][] desp = {{-d, 0}, {d, 0}, {0, -d}, {0, d}};
            for (double[] o : desp) {
                dibujarEn(g2, s, x + o[0], y + o[1], w, h);
            }
        }
        dibujarEn(g2, img, x, y, w, h);
    }

    private static void dibujarEn(Graphics2D g2, BufferedImage img, double x, double y, double w, double h) {
        java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
        at.translate(x, y);
        at.scale(w / img.getWidth(), h / img.getHeight());
        g2.drawImage(img, at, null);
    }

    // ------------------------------------------------------------------
    // Caché de imágenes reducidas
    // ------------------------------------------------------------------

    private BufferedImage reducida(boolean derecha, int cuadro, int altoPx, boolean silueta) {
        Map<Integer, BufferedImage[]> cache = silueta ? (derecha ? cacheSilDer : cacheSilIzq)
                                                      : (derecha ? cacheDer : cacheIzq);
        if (cache.size() > 6) {
            cache.clear();   // la ventana cambió mucho de tamaño: se rehace
        }
        BufferedImage[] fila = cache.get(altoPx);
        if (fila == null) {
            fila = new BufferedImage[5];
            cache.put(altoPx, fila);
        }
        if (fila[cuadro] == null) {
            BufferedImage base = (derecha ? der : izq)[cuadro];
            double k = altoPx / ALTO_VISUAL * factor;   // píxeles de pantalla por píxel original
            int w = Math.max(1, (int) Math.round(base.getWidth() * k));
            int h = Math.max(1, (int) Math.round(base.getHeight() * k));
            BufferedImage r = reducirPorPasos(base, w, h);
            fila[cuadro] = silueta ? blanca(r) : r;
        }
        return fila[cuadro];
    }

    /**
     * Reducir de 700 px a 70 px de un solo golpe se ve "con serrucho".
     * Reducir a la mitad varias veces con interpolación bilineal da un resultado suave.
     */
    private static BufferedImage reducirPorPasos(BufferedImage img, int w, int h) {
        BufferedImage actual = img;
        int cw = img.getWidth(), ch = img.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            actual = escalar(actual, cw, ch);
        }
        return escalar(actual, w, h);
    }

    private static BufferedImage escalar(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** Recorta los bordes transparentes de la imagen (null si no existe). */
    private static BufferedImage recortar(BufferedImage img) {
        if (img == null) {
            return null;
        }
        Rectangle r = cajaVisible(img);
        BufferedImage copia = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copia.createGraphics();
        g.drawImage(img, -r.x, -r.y, null);
        g.dispose();
        return copia;
    }

    private static Rectangle cajaVisible(BufferedImage img) {
        int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 20) {
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

    /** Copia toda blanca (misma forma) para el borde del modo alto contraste. */
    private static BufferedImage blanca(BufferedImage img) {
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
