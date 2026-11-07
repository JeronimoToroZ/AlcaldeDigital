package alcaldedigital.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Pantalla de "Cómo se juega".
 *
 * Se abre en dos momentos: desde el menu principal, antes de jugar, y desde el
 * boton de ayuda durante la partida. Es la pantalla que decide si alguien que
 * pasa frente al stand se sienta, asi que responde tres preguntas en ese orden:
 * de que va esto, que hago en mi turno, y que significa cada simbolo.
 *
 * Dos cosas que antes no estaban:
 *
 *  - Explica el MAPA. Hacer clic en un vecino para desmentir no se descubre
 *    solo, y es la mecanica que ensena de que trata el juego.
 *  - Los numeros son los del codigo. Antes decia "20 burbujas flotantes" y en
 *    pantalla hay como mucho 9: una instruccion que miente es peor que no
 *    tener instrucciones.
 */
public class DialogoInstrucciones extends DialogoJuego {

    private static final int ANCHO_TEXTO = 470;

    /** Datos de cada símbolo: imagen, nombre y cuándo usarlo. */
    private static final String[][] SIMBOLOS = {
        {"compartir.png", "Compartir", "Información útil y confiable: ayúdala a circular."},
        {"verificar.png", "Verificar", "Dice ser oficial, pero hay que confirmar la fuente."},
        {"ignorar.png", "Ignorar", "Opiniones o rumores sin sustento: no les des alcance."},
        {"reportar.png", "Reportar", "Acusaciones graves sin pruebas: frena el daño."},
    };

    /**
     * Un tono claro del color de cada burbuja.
     *
     * Antes eran morado, verde, azul y rosa: los colores de los botones
     * brillantes que se quitaron del juego. El nombre quedaba de un color y su
     * propio icono, al lado, de otro.
     */
    private static final Color[] COLORES = {
        new Color(0x7F, 0xB4, 0xCC),   // azul petróleo claro - compartir
        new Color(0xE3, 0xB4, 0x5E),   // ocre claro          - verificar
        new Color(0xB5, 0xAA, 0x98),   // gris cálido claro   - ignorar
        new Color(0xDD, 0x81, 0x78),   // ladrillo claro      - reportar
    };

    private static final String[] PASOS = {
        "Lee la publicación. Tienes 5 segundos antes de que aparezcan las burbujas.",
        "Revienta 8 burbujas con el símbolo de la acción correcta. Los símbolos "
                + "cambian cada 1,8 segundos, así que decide antes de disparar.",
        "Entre una situación y la siguiente, el rumor avanza una oleada por la ciudad."
    };

    private static final String TITULO_MAPA = "El mapa no es decoración";
    private static final String TEXTO_MAPA =
            "Haz clic en un vecino para hablarle y desmentir el rumor. Solo se retracta "
            + "quien todavía no lo ha pasado: a quien ya lo contó, el desmentido le llega "
            + "tarde y se queda en rojo. Por eso conviene desmentir temprano.";

    private DialogoInstrucciones(Window padre, String titulo, String subtitulo, String cierre) {
        super(padre, titulo);

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        JLabel lblSub = parrafo(subtitulo, ANCHO_TEXTO, 15, CREMA);
        lblSub.setHorizontalAlignment(SwingConstants.CENTER);
        lblSub.setAlignmentX(0.5f);
        cuerpo.add(lblSub);
        cuerpo.add(espacio(16));

        cuerpo.add(rotulo("TU TURNO"));
        cuerpo.add(espacio(8));

        JPanel listaPasos = new JPanel(new GridBagLayout());
        listaPasos.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(4, 0, 4, 10);
        for (int i = 0; i < PASOS.length; i++) {
            c.gridy = i;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            listaPasos.add(new Numero(i + 1), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            listaPasos.add(parrafo(PASOS[i], ANCHO_TEXTO - 40, 15, CREMA), c);
        }
        listaPasos.setAlignmentX(0.5f);
        cuerpo.add(listaPasos);
        cuerpo.add(espacio(16));

        // El mapa va en su propio bloque destacado: es lo que nadie descubre
        // solo y lo que de verdad explica el juego.
        Tarjeta mapa = new Tarjeta(new BorderLayout(0, 4), ORO);
        JLabel lblMapa = etiqueta(TITULO_MAPA, Font.BOLD, 16, ORO);
        mapa.add(lblMapa, BorderLayout.NORTH);
        // El ancho del parrafo tiene que descontar el relleno de la Tarjeta:
        // con el ancho completo, el texto se salia por la derecha y se cortaba.
        mapa.add(parrafo(TEXTO_MAPA, ANCHO_TEXTO - 80, 14, CREMA), BorderLayout.CENTER);
        mapa.setAlignmentX(0.5f);
        mapa.setMaximumSize(new Dimension(ANCHO_TEXTO + 30, 220));
        cuerpo.add(mapa);
        cuerpo.add(espacio(16));

        cuerpo.add(rotulo("LOS CUATRO SÍMBOLOS"));
        cuerpo.add(espacio(8));

        JPanel grilla = new JPanel(new GridLayout(2, 2, 12, 12));
        grilla.setOpaque(false);
        for (int i = 0; i < SIMBOLOS.length; i++) {
            grilla.add(tarjetaSimbolo(SIMBOLOS[i][0], SIMBOLOS[i][1], SIMBOLOS[i][2], COLORES[i]));
        }
        grilla.setAlignmentX(0.5f);
        cuerpo.add(grilla);

        if (cierre != null) {
            cuerpo.add(espacio(16));
            JLabel lblCierre = parrafo(cierre, ANCHO_TEXTO, 15, ORO);
            lblCierre.setFont(fuenteEspaciada(Font.BOLD, 15, 0.04));
            lblCierre.setHorizontalAlignment(SwingConstants.CENTER);
            lblCierre.setAlignmentX(0.5f);
            cuerpo.add(lblCierre);
        }

        setContenido(cuerpo);
        agregarBoton(botonCerrar(padre == null ? "ENTENDIDO" : "ENTENDIDO"));
    }

    /** Rótulo de sección: mayúsculas espaciadas, como en la portada. */
    private static JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto, SwingConstants.CENTER);
        l.setFont(fuenteEspaciada(Font.BOLD, 14, 0.20));
        l.setForeground(ORO);
        l.setAlignmentX(0.5f);
        return l;
    }

    /** Instrucciones al empezar la partida. */
    public static void instruccionesIniciales(Window padre) {
        new DialogoInstrucciones(padre, "CÓMO SE JUEGA",
                "Ciudad Nova está en campaña y un rumor falso ya salió del Parque. "
                + "Tú decides qué hacer con cada publicación que llega.",
                "Cuatro situaciones. Gana quien más acierte y más rápido.").mostrar();
    }

    /** La misma pantalla, desde el botón de ayuda. */
    public static void ayuda(Window padre) {
        instruccionesIniciales(padre);
    }

    // ------------------------------------------------------------------

    private static JPanel tarjetaSimbolo(String imagen, String nombre, String descripcion, Color color) {
        Tarjeta t = new Tarjeta(new BorderLayout(12, 0), color);
        ImageIcon ic = icono(imagen, 54);
        JLabel lblImg = new JLabel(ic);
        if (ic == null) {
            lblImg.setText("?");
        }
        t.add(lblImg, BorderLayout.WEST);

        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        JLabel lblNombre = etiqueta(nombre, Font.BOLD, 17, color);
        lblNombre.setFont(fuenteEspaciada(Font.BOLD, 17, 0.05));
        textos.add(lblNombre, BorderLayout.NORTH);
        textos.add(parrafo(descripcion, 150, 13, CREMA), BorderLayout.CENTER);
        t.add(textos, BorderLayout.CENTER);
        return t;
    }

    /** Círculo dorado con el número del paso. */
    private static class Numero extends JComponent {

        private final int n;

        Numero(int n) {
            this.n = n;
            setPreferredSize(new Dimension(24, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(ORO);
            g2.fillOval(1, 1, 22, 22);
            g2.setColor(CAFE_OSCURO);
            g2.setFont(fuente(Font.BOLD, 13));
            String s = String.valueOf(n);
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, 12 - fm.stringWidth(s) / 2, 12 + fm.getAscent() / 2 - 1);
            g2.dispose();
        }
    }
}
