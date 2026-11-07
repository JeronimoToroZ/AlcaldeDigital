package alcaldedigital.gui;

import alcaldedigital.logica.NodoRed;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Dibuja un ciudadano del grafo: su cara, el aro de estado y su nombre.
 *
 * Esta clase existe para que la cara se dibuje igual en todos lados — en el mapa
 * del grafo y en el informe final — y para que el nombre del archivo se arme en
 * un solo sitio ("don Humberto" -> ciudadano_humberto.png).
 *
 * El estado NO se indica solo con color: cada nodo lleva ademas un simbolo
 * (equis o chulo). Un jugador que no distinga rojo de verde tiene que poder
 * jugar igual, y eso es parte de lo que evalua el enunciado.
 */
public final class RetratoCiudadano {

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private RetratoCiudadano() {
    }

    /**
     * @param cx     centro en x
     * @param cy     centro en y
     * @param radio  radio del circulo de la cara
     */
    public static void dibujar(Graphics2D g2, NodoRed nodo, int cx, int cy, int radio) {
        int lado = radio * 2;
        BufferedImage foto = cargar(archivoDe(nodo.persona));

        if (foto != null) {
            BufferedImage circular = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gc = circular.createGraphics();
            Tema.suavizar(gc);
            gc.setClip(new Ellipse2D.Float(0, 0, lado, lado));
            gc.drawImage(foto, 0, 0, lado, lado, null);
            gc.dispose();
            g2.drawImage(circular, cx - radio, cy - radio, null);
        } else {
            g2.setColor(nodo.estaInfectado ? new Color(220, 50, 50) : new Color(40, 120, 220));
            g2.fillOval(cx - radio, cy - radio, lado, lado);
        }

        Color estado = nodo.estaInfectado ? new Color(206, 42, 44) : new Color(26, 132, 78);
        g2.setColor(estado);
        g2.setStroke(new BasicStroke(3.5f));
        g2.drawOval(cx - radio, cy - radio, lado, lado);

        // Simbolo de estado, para no depender del color
        int sx = cx + radio - 5;
        int sy = cy + radio - 5;
        int r = Math.max(7, radio / 3);
        g2.setColor(estado);
        g2.fillOval(sx - r, sy - r, r * 2, r * 2);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int d = r - 3;
        if (nodo.estaInfectado) {
            g2.drawLine(sx - d, sy - d, sx + d, sy + d);
            g2.drawLine(sx + d, sy - d, sx - d, sy + d);
        } else {
            g2.drawPolyline(new int[]{sx - d, sx - 1, sx + d},
                            new int[]{sy, sy + d - 1, sy - d}, 3);
        }

        // Nombre debajo, con halo para que se lea sobre el mapa
        String etiqueta = primerNombre(nodo.persona);
        g2.setFont(Tema.indicadorNegrita());
        int ancho = g2.getFontMetrics().stringWidth(etiqueta);
        int tx = cx - ancho / 2;
        int ty = cy + radio + 16;

        g2.setColor(new Color(255, 255, 255, 220));
        for (int ox = -2; ox <= 2; ox++) {
            for (int oy = -2; oy <= 2; oy++) {
                if (ox != 0 || oy != 0) {
                    g2.drawString(etiqueta, tx + ox, ty + oy);
                }
            }
        }
        g2.setColor(new Color(30, 25, 18));
        g2.drawString(etiqueta, tx, ty);
    }

    /** "don Humberto" -> "Humberto"; "doña Carmen" -> "Carmen". */
    public static String primerNombre(String persona) {
        if (persona == null) {
            return "";
        }
        String[] partes = persona.trim().split("\\s+");
        for (String parte : partes) {
            String p = quitarTildes(parte);
            if (!p.equals("don") && !p.equals("dona")) {
                return parte;
            }
        }
        return partes[partes.length - 1];
    }

    /** "don Humberto" -> "ciudadano_humberto.png" */
    public static String archivoDe(String persona) {
        return "ciudadano_" + quitarTildes(primerNombre(persona)) + ".png";
    }

    private static String quitarTildes(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase();
    }

    /** Carga desde recursos. Nunca lanza: una imagen que falta no tumba el juego. */
    public static BufferedImage cargar(String nombre) {
        if (CACHE.containsKey(nombre)) {
            return CACHE.get(nombre);
        }
        BufferedImage img = null;
        try {
            URL url = RetratoCiudadano.class.getResource("/alcaldedigital/recursos/" + nombre);
            if (url != null) {
                img = ImageIO.read(url);
            }
        } catch (IOException | RuntimeException ignorada) {
            img = null;
        }
        CACHE.put(nombre, img);
        return img;
    }
}
