package alcaldedigital.gui;

import alcaldedigital.modelo.Cuenta;
import alcaldedigital.modelo.Publicacion;
import alcaldedigital.modelo.Senal;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La publicacion dibujada como se veria en una red social de verdad.
 *
 * POR QUE ESTA CLASE EXISTE
 * -------------------------
 * Antes el juego mostraba una frase suelta y cuatro botones. El jugador no tenia
 * NADA que mirar para decidir, asi que terminaba adivinando o aprendiendo cual
 * boton premia el juego.
 *
 * Aqui se muestra todo lo que en la vida real se puede revisar antes de creer:
 * quien publica, hace cuanto existe esa cuenta, cuantos seguidores tiene, si esta
 * verificada, a que hora se publico y que fuente cita.
 *
 * LO QUE ESTA CLASE NO HACE
 * -------------------------
 * No muestra las senales como etiquetas, salvo que se le pida expresamente
 * (mostrarSenales, que usa la pantalla de verificacion y el informe final).
 * Si la tarjeta dijera "titular alarmista" debajo del texto, el jugador no
 * aprenderia a verlo: lo leeria. Las pistas estan en los datos, no en rotulos.
 */
public class TarjetaPublicacion extends JPanel {

    private static final int ANCHO = 440;
    private static final int MARGEN = 18;
    private static final int AVATAR = 44;
    private static final int RADIO = 14;

    /** Caché de imágenes: el mismo avatar se usa en varios turnos. */
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private final Publicacion publicacion;
    private boolean mostrarSenales;

    private int altoCalculado = 320;

    public TarjetaPublicacion(Publicacion publicacion) {
        this(publicacion, false);
    }

    public TarjetaPublicacion(Publicacion publicacion, boolean mostrarSenales) {
        this.publicacion = publicacion;
        this.mostrarSenales = mostrarSenales;
        setOpaque(false);
        setPreferredSize(new Dimension(ANCHO, altoCalculado));
    }

    /** Revela las senales. Lo usa la pantalla de verificacion y el informe final. */
    public void setMostrarSenales(boolean valor) {
        this.mostrarSenales = valor;
        recalcularAlto();
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------
    // Dibujo
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);

        int ancho = Math.min(getWidth(), ANCHO);
        boolean contraste = Tema.isAltoContraste();

        Color fondoTarjeta = contraste ? Color.BLACK : Color.WHITE;
        Color tinta        = contraste ? Color.WHITE : new Color(28, 28, 32);
        Color gris         = contraste ? Color.WHITE : new Color(130, 130, 138);
        Color linea        = contraste ? Color.WHITE : new Color(224, 224, 230);

        // Sombra suave bajo la tarjeta (solo en modo normal)
        if (!contraste) {
            g2.setColor(new Color(0, 0, 0, 22));
            g2.fillRoundRect(3, 4, ancho - 6, altoCalculado - 4, RADIO, RADIO);
        }

        g2.setColor(fondoTarjeta);
        g2.fillRoundRect(0, 0, ancho - 6, altoCalculado - 6, RADIO, RADIO);
        g2.setColor(linea);
        g2.setStroke(new BasicStroke(contraste ? 2f : 1f));
        g2.drawRoundRect(0, 0, ancho - 6, altoCalculado - 6, RADIO, RADIO);

        int y = MARGEN;
        int x = MARGEN;
        int anchoUtil = ancho - 6 - MARGEN * 2;

        Cuenta c = publicacion.getCuenta();

        // ---------------- Cabecera: avatar + identidad de la cuenta ----------
        if (c != null) {
            dibujarAvatar(g2, x, y, c, gris, fondoTarjeta);

            int xTexto = x + AVATAR + 12;
            int yTexto = y + 15;

            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.setColor(tinta);
            g2.drawString(c.getNombre(), xTexto, yTexto);

            int anchoNombre = g2.getFontMetrics().stringWidth(c.getNombre());
            if (c.estaVerificada()) {
                dibujarVerificado(g2, xTexto + anchoNombre + 7, yTexto - 9);
            }

            yTexto += 16;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(gris);
            String linea1 = c.getArroba();
            if (publicacion.getHora() != null) {
                linea1 += "  ·  " + publicacion.getHora();
            }
            g2.drawString(linea1, xTexto, yTexto);

            // Esta es la fila que lleva las pistas: antiguedad y seguidores.
            yTexto += 15;
            g2.drawString(c.antiguedadTexto() + "  ·  " + c.seguidoresTexto(), xTexto, yTexto);

            y += AVATAR + 14;
        }

        // ---------------- Texto de la publicacion ----------------------------
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(tinta);
        FontMetrics fm = g2.getFontMetrics();
        for (String renglon : partir(publicacion.getTexto(), fm, anchoUtil)) {
            y += fm.getHeight();
            g2.drawString(renglon, x, y);
        }
        y += 10;

        // ---------------- Imagen de la publicacion ---------------------------
        BufferedImage foto = cargar(publicacion.getImagen());
        if (foto != null) {
            int altoFoto = (int) (anchoUtil * foto.getHeight() / (double) foto.getWidth());
            altoFoto = Math.min(altoFoto, 190);
            g2.drawImage(foto, x, y, anchoUtil, altoFoto, null);
            g2.setColor(linea);
            g2.drawRect(x, y, anchoUtil, altoFoto);
            y += altoFoto + 12;
        }

        // ---------------- Fuente: siempre se muestra, aunque no haya ---------
        g2.setColor(linea);
        g2.drawLine(x, y, x + anchoUtil, y);
        y += 18;

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(gris);
        g2.drawString("Fuente:", x, y);
        int xFuente = x + g2.getFontMetrics().stringWidth("Fuente: ") + 4;

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        if (publicacion.getFuenteCitada() != null) {
            g2.setColor(tinta);
            FontMetrics fmF = g2.getFontMetrics();
            List<String> renglones = partir(publicacion.getFuenteCitada(), fmF, anchoUtil - (xFuente - x));
            g2.drawString(renglones.get(0), xFuente, y);
            for (int i = 1; i < renglones.size(); i++) {
                y += fmF.getHeight();
                g2.drawString(renglones.get(i), x, y);
            }
        } else {
            g2.setColor(gris);
            g2.drawString("—", xFuente, y);
        }
        y += 8;

        // ---------------- Senales, solo si se piden --------------------------
        if (mostrarSenales && !publicacion.getSenales().isEmpty()) {
            y += 12;
            for (Senal s : publicacion.getSenales()) {
                y = dibujarEtiqueta(g2, x, y, anchoUtil, s.getEtiqueta(), contraste);
            }
        }

        g2.dispose();
    }

    /** Una etiqueta de senal: punto naranja + texto. Devuelve la nueva y. */
    private int dibujarEtiqueta(Graphics2D g2, int x, int y, int anchoUtil,
                                String texto, boolean contraste) {
        Color marca = contraste ? new Color(255, 160, 0) : new Color(214, 118, 20);
        g2.setColor(marca);
        g2.fillOval(x, y - 7, 7, 7);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(contraste ? Color.WHITE : new Color(120, 70, 10));
        g2.drawString(texto, x + 13, y);
        return y + 18;
    }

    /** Avatar circular. Sin imagen, el muñequito gris por defecto. */
    private void dibujarAvatar(Graphics2D g2, int x, int y, Cuenta c,
                               Color gris, Color fondoTarjeta) {
        BufferedImage img = cargar(c.getAvatar());

        if (img != null) {
            BufferedImage circular = new BufferedImage(AVATAR, AVATAR, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gc = circular.createGraphics();
            Tema.suavizar(gc);
            gc.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, AVATAR, AVATAR));
            gc.drawImage(img, 0, 0, AVATAR, AVATAR, null);
            gc.dispose();
            g2.drawImage(circular, x, y, null);
        } else {
            // Cuenta sin foto: lo que de verdad pasa con las cuentas nuevas.
            g2.setColor(new Color(224, 224, 230));
            g2.fillOval(x, y, AVATAR, AVATAR);
            g2.setColor(new Color(168, 168, 176));
            int cx = x + AVATAR / 2;
            g2.fillOval(cx - 7, y + 10, 14, 14);                       // cabeza
            g2.fillOval(cx - 13, y + AVATAR - 16, 26, 20);             // hombros
            g2.setColor(new Color(224, 224, 230));
            g2.fillRect(x, y + AVATAR - 3, AVATAR, 4);
        }
    }

    /** El chulo azul de cuenta verificada. */
    private void dibujarVerificado(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(29, 155, 240));
        g2.fillOval(x, y, 13, 13);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolyline(new int[]{x + 3, x + 6, x + 10},
                        new int[]{y + 7, y + 10, y + 4}, 3);
    }

    // ------------------------------------------------------------------
    // Medidas
    // ------------------------------------------------------------------

    /** Calcula cuanto mide la tarjeta con este contenido, para que nada se corte. */
    private void recalcularAlto() {
        int anchoUtil = ANCHO - 6 - MARGEN * 2;
        int alto = MARGEN;

        if (publicacion.getCuenta() != null) {
            alto += AVATAR + 14;
        }

        FontMetrics fm = getFontMetrics(new Font("SansSerif", Font.PLAIN, 14));
        alto += partir(publicacion.getTexto(), fm, anchoUtil).size() * fm.getHeight() + 10;

        BufferedImage foto = cargar(publicacion.getImagen());
        if (foto != null) {
            int altoFoto = (int) (anchoUtil * foto.getHeight() / (double) foto.getWidth());
            alto += Math.min(altoFoto, 190) + 12;
        }

        alto += 26;   // linea divisoria + fila de fuente
        FontMetrics fmF = getFontMetrics(new Font("SansSerif", Font.PLAIN, 12));
        if (publicacion.getFuenteCitada() != null) {
            int extra = partir(publicacion.getFuenteCitada(), fmF, anchoUtil - 60).size() - 1;
            alto += Math.max(0, extra) * fmF.getHeight();
        }

        if (mostrarSenales) {
            alto += 12 + publicacion.getSenales().size() * 18;
        }

        altoCalculado = alto + MARGEN + 6;
        setPreferredSize(new Dimension(ANCHO, altoCalculado));
    }

    @Override
    public void addNotify() {
        super.addNotify();
        recalcularAlto();   // ya hay fuentes disponibles para medir
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ANCHO, altoCalculado);
    }

    /** Parte un texto en renglones que quepan en el ancho dado. */
    private static List<String> partir(String texto, FontMetrics fm, int ancho) {
        List<String> renglones = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            return renglones;
        }
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (fm.stringWidth(prueba) > ancho && actual.length() > 0) {
                renglones.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual = new StringBuilder(prueba);
            }
        }
        if (actual.length() > 0) {
            renglones.add(actual.toString());
        }
        return renglones;
    }

    /** Carga una imagen de recursos. Devuelve null si no existe; nunca lanza. */
    private static BufferedImage cargar(String nombre) {
        if (nombre == null) {
            return null;
        }
        if (CACHE.containsKey(nombre)) {
            return CACHE.get(nombre);
        }
        BufferedImage img = null;
        try {
            URL url = TarjetaPublicacion.class.getResource("/alcaldedigital/recursos/" + nombre);
            if (url != null) {
                img = ImageIO.read(url);
            }
        } catch (IOException | RuntimeException ignorada) {
            img = null;   // una imagen que falta no puede tumbar el juego
        }
        CACHE.put(nombre, img);
        return img;
    }

    /** Escala una imagen manteniendo la proporcion. */
    public static Image escalar(BufferedImage img, int ancho) {
        if (img == null) {
            return null;
        }
        int alto = (int) (ancho * img.getHeight() / (double) img.getWidth());
        return img.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
    }
}
