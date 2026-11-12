package influenceinc.gui;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Carga las IMAGENES y la TIPOGRAFIA del juego (carpeta influenceinc/recursos).
 *
 * Se buscan primero dentro del classpath (asi funcionan desde NetBeans y desde
 * el .jar) y, si no, en la carpeta src/ del proyecto. Si una imagen no se
 * encuentra, el juego sigue funcionando con su dibujo de respaldo en Java2D.
 *
 * Las imagenes escaladas se guardan en cache: escalar con buena calidad en cada
 * cuadro seria muy lento.
 */
public final class Recursos {

    private static final String CARPETA = "/influenceinc/recursos/";
    private static final Map<String, BufferedImage> IMAGENES = new HashMap<>();
    private static final Map<String, BufferedImage> ESCALADAS = new HashMap<>();
    private static final Map<String, Font> FUENTES = new HashMap<>();

    private Recursos() {
    }

    /** La imagen original (null si no existe). */
    public static synchronized BufferedImage imagen(String nombre) {
        if (IMAGENES.containsKey(nombre)) {
            return IMAGENES.get(nombre);
        }
        BufferedImage img = null;
        try {
            URL u = Recursos.class.getResource(CARPETA + nombre);
            if (u != null) {
                img = ImageIO.read(u);
            } else {
                for (String base : new String[]{"src/influenceinc/recursos/", "influenceinc/recursos/", "recursos/"}) {
                    File f = new File(base + nombre);
                    if (f.exists()) {
                        img = ImageIO.read(f);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            img = null;
        }
        IMAGENES.put(nombre, img);
        return img;
    }

    /** La imagen escalada a (w, h) con buena calidad, en cache. */
    public static synchronized BufferedImage escalada(String nombre, int w, int h) {
        BufferedImage o = imagen(nombre);
        if (o == null || w <= 0 || h <= 0) {
            return null;
        }
        String k = nombre + "@" + w + "x" + h;
        BufferedImage e = ESCALADAS.get(k);
        if (e == null) {
            if (ESCALADAS.size() > 160) {
                ESCALADAS.clear();
            }
            e = escalar(o, w, h);
            ESCALADAS.put(k, e);
        }
        return e;
    }

    /** Escalado progresivo (de a la mitad) para que las reducciones grandes no se vean pixeladas. */
    private static BufferedImage escalar(BufferedImage o, int w, int h) {
        BufferedImage actual = o;
        int cw = o.getWidth(), ch = o.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            actual = paso(actual, cw, ch);
        }
        return paso(actual, w, h);
    }

    private static BufferedImage paso(Image src, int w, int h) {
        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = b.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return b;
    }

    /**
     * Dibuja la imagen cubriendo TODO el rectangulo (como background-size: cover):
     * se recorta lo que sobra pero nunca quedan franjas vacias.
     */
    public static void cubrir(Graphics2D g, String nombre, int x, int y, int w, int h) {
        BufferedImage o = imagen(nombre);
        if (o == null) {
            return;
        }
        double k = Math.max(w / (double) o.getWidth(), h / (double) o.getHeight());
        int iw = (int) Math.ceil(o.getWidth() * k), ih = (int) Math.ceil(o.getHeight() * k);
        BufferedImage e = escalada(nombre, Math.max(1, iw), Math.max(1, ih));
        Graphics2D s = (Graphics2D) g.create();
        s.clipRect(x, y, w, h);
        s.drawImage(e, x + (w - iw) / 2, y + (h - ih) / 2, null);
        s.dispose();
    }

    /** Dibuja la imagen dentro del rectangulo conservando la proporcion (centrada). */
    public static boolean contener(Graphics2D g, String nombre, double x, double y, double w, double h) {
        BufferedImage o = imagen(nombre);
        if (o == null) {
            return false;
        }
        double k = Math.min(w / o.getWidth(), h / o.getHeight());
        double iw = o.getWidth() * k, ih = o.getHeight() * k;
        double px = x + (w - iw) / 2, py = y + (h - ih) / 2;
        // se escala al tamano real en pantalla (incluye la escala del lienzo) para que se vea nitido
        double sx = Math.abs(g.getTransform().getScaleX());
        double sy = Math.abs(g.getTransform().getScaleY());
        int rw = Math.max(1, (int) Math.round(iw * sx)), rh = Math.max(1, (int) Math.round(ih * sy));
        BufferedImage e = escalada(nombre, rw, rh);
        Graphics2D s = (Graphics2D) g.create();
        s.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        s.translate(px, py);
        s.scale(iw / rw, ih / rh);
        s.drawImage(e, 0, 0, null);
        s.dispose();
        return true;
    }

    /** Fuente Poppins con el estilo pedido (si no se encuentra, SansSerif). */
    public static synchronized Font fuente(int estilo, float tam) {
        String archivo;
        boolean negrita = (estilo & Font.BOLD) != 0, cursiva = (estilo & Font.ITALIC) != 0;
        if (negrita && cursiva) {
            archivo = "Poppins-BoldItalic.ttf";
        } else if (negrita) {
            archivo = "Poppins-Bold.ttf";
        } else if (cursiva) {
            archivo = "Poppins-Italic.ttf";
        } else {
            archivo = "Poppins-Regular.ttf";
        }
        return base(archivo, estilo).deriveFont(tam);
    }

    /** Peso intermedio para textos largos (mas legible que la negrita). */
    public static synchronized Font media(float tam) {
        return base("Poppins-Medium.ttf", Font.PLAIN).deriveFont(tam);
    }

    private static Font base(String archivo, int estilo) {
        Font f = FUENTES.get(archivo);
        if (f != null) {
            return f;
        }
        try {
            InputStream in = Recursos.class.getResourceAsStream(CARPETA + archivo);
            if (in == null) {
                File file = new File("src/influenceinc/recursos/" + archivo);
                if (file.exists()) {
                    f = Font.createFont(Font.TRUETYPE_FONT, file);
                }
            } else {
                try (InputStream i = in) {
                    f = Font.createFont(Font.TRUETYPE_FONT, i);
                }
            }
        } catch (Exception e) {
            f = null;
        }
        if (f == null) {
            f = new Font("SansSerif", estilo, 12);
        }
        FUENTES.put(archivo, f);
        return f;
    }
}
