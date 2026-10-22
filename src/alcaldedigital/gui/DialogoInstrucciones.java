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
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Instrucciones y Ayuda del juego, con el mismo estilo que los Ajustes.
 *   - {@link #instruccionesIniciales(Window)}: versión corta al llegar al mapa.
 *   - {@link #ayuda(Window)}: sección AYUDA completa (objetivo, reglas, controles,
 *     botones, símbolos, mapa de niveles, cómo ganar e indicadores).
 * Cada símbolo se muestra con la imagen de su burbuja al lado del nombre.
 */
public class DialogoInstrucciones extends DialogoJuego {

    private static final int ANCHO_TEXTO = 560;

    /** imagen, nombre, cuándo usarlo */
    private static final String[][] SIMBOLOS = {
        {"compartir.png", "Compartir", "Información útil y confiable para la comunidad: ayúdala a circular."},
        {"verificar.png", "Verificar", "Dice ser oficial o \"dicen que...\", pero no trae fuente: hay que confirmarla."},
        {"ignorar.png", "Ignorar", "Opiniones y gustos personales: no les des más alcance ni pelees."},
        {"reportar.png", "Reportar", "Acusaciones graves o noticias falsas que hacen daño: denúncialas."},
    };

    private static final Color[] COLORES = {
        new Color(0xB9, 0x8C, 0xE6),   // morado - compartir
        new Color(0x8F, 0xD6, 0x9A),   // verde  - verificar
        new Color(0x86, 0xC5, 0xF0),   // azul   - ignorar
        new Color(0xF0, 0x8A, 0x80),   // rojo   - reportar
    };

    private final JPanel cuerpo = new JPanel();

    private DialogoInstrucciones(Window padre, String titulo, String subtitulo) {
        super(padre, titulo);
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        JLabel lblSub = etiqueta(subtitulo, Font.ITALIC, 17, CREMA);
        lblSub.setAlignmentX(0.5f);
        cuerpo.add(lblSub);
        cuerpo.add(espacio(8));
    }

    // ------------------------------------------------------------------
    // Las dos versiones
    // ------------------------------------------------------------------

    /** Instrucciones cortas que aparecen la primera vez que se abre el mapa. */
    public static void instruccionesIniciales(Window padre) {
        DialogoInstrucciones d = new DialogoInstrucciones(padre, "Instrucciones", "¡Bienvenidos a Alcalde Digital!");
        d.pasos(new String[]{
            "Elijan un nivel en el mapa (empiecen por el 1). Ese nivel lo juegan TODOS, uno por uno: "
                    + "antes de cada ronda aparece un aviso con el jugador al que le toca.",
            "Arriba sale una publicación de Civitas. Piensa qué harías con ella: compartir, verificar, "
                    + "ignorar o reportar.",
            "Muévete con ← → y salta con Espacio. Recolecta " + alcaldedigital.logica.DificultadNivel.BURBUJAS_REQUERIDAS
                    + " burbujas de esa decisión por publicación. "
                    + "Ojo: las burbujas cambian de lugar cada 2 segundos (y más rápido en cada nivel).",
            "Burbuja equivocada, caer en un hueco o quedarte sin tiempo te quita una vida. Tienes 3 por ronda.",
            "Resuelve las 4 publicaciones. Un cronómetro mide tu ronda: entre menos tardes, más puntos.",
            "Cuando todos jueguen, sale la tabla de puntuaciones con el podio. Si al menos uno superó el "
                    + "nivel, se desbloquean los dos niveles que salen de él."
        });
        d.simbolos();
        d.cierre("Todos juegan sobre la misma ciudad: los errores de uno esparcen rumores para todos.");
        d.finalizar(true);
    }

    /** Sección AYUDA completa, disponible desde el mapa y desde cada nivel. */
    public static void ayuda(Window padre) {
        DialogoInstrucciones d = new DialogoInstrucciones(padre, "Ayuda", "Todo lo que necesitas para jugar");
        d.seccion("Objetivo del juego",
                "Ciudad Nova está en elecciones y en Civitas circulan rumores, opiniones y noticias falsas. "
                + "Cada jugador debe tomar decisiones responsables para que la ciudad llegue al día de "
                + "elecciones informada, con confianza y sin conflictos, y así ganar más votos que los demás.");
        d.seccion("Turnos",
                "• Cada nivel se juega por rondas: una por jugador, en el orden en que se registraron.\n"
                + "• Antes de cada ronda aparece un aviso con el nombre y el rol del jugador de turno.\n"
                + "• Mientras uno juega, los demás miran: todos comparten la misma ciudad y la misma red.");
        d.seccion("Controles",
                "← / A: izquierda    → / D: derecha    Espacio / ↑ / W: saltar (mantén para saltar más alto)    "
                + "P o Esc: pausa (el cronómetro se detiene).");
        d.seccion("Reglas de cada ronda",
                "• Arriba aparece una publicación. Recolecta solo las burbujas de la acción correcta.\n"
                + "• Necesitas " + alcaldedigital.logica.DificultadNivel.BURBUJAS_REQUERIDAS
                + " burbujas correctas por publicación y son 4 publicaciones.\n"
                + "• Las burbujas se generan en otro lugar cada 2 s en el nivel 1, y 0,1 s más rápido "
                + "en cada nivel (1,4 s en el nivel 7). Justo antes de moverse se desvanecen.\n"
                + "• Burbuja equivocada, caer en un hueco o tiempo agotado = pierdes una vida. Tienes 3.");
        d.seccion("Puntuación",
                "• Cada burbuja correcta: +10. Cada publicación resuelta: los puntos del árbol de decisión.\n"
                + "• Si superas el nivel: +25 por cada vida que te quede y un bono por velocidad "
                + "(3 puntos por cada segundo por debajo de " + alcaldedigital.logica.Partida.SEGUNDOS_REFERENCIA
                + " s). Entre menos tardes, más puntos.\n"
                + "• Al final de las rondas, la tabla muestra a cada jugador con su tiempo y sus puntos, "
                + "y el podio con el 1.º, 2.º y 3.º lugar.");
        d.simbolos();
        StringBuilder roles = new StringBuilder();
        for (alcaldedigital.modelo.Rol r : alcaldedigital.modelo.Rol.values()) {
            roles.append("• ").append(r.getEtiqueta()).append(": ").append(r.getDescripcion()).append("\n");
        }
        roles.append("Los roles no se repiten: cada jugador tiene uno distinto.");
        d.seccion("Roles", roles.toString());
        d.seccion("Mapa de niveles",
                "Los niveles forman un grafo: el 1 abre el 2 y el 3; el 2 abre el 4 y el 5; el 3 abre el 6 y el 7. "
                + "Un nivel se desbloquea cuando al menos un jugador supera el nivel anterior. "
                + "Gris con candado = bloqueado; brilla = disponible; anillo dorado y estrellas = ganado.");
        d.seccion("Cómo ganar",
                "Al superar un nivel final (4, 5, 6 o 7) se abren las elecciones. Los votos se reparten según "
                + "los puntos y la reputación de cada jugador, y pesan más si la ciudad quedó sana. "
                + "Quien tenga más votos es el nuevo alcalde.");
        d.seccion("Indicadores de la ciudad",
                "Información verificada, Confianza, Convivencia y Bienestar digital: conviene que suban.\n"
                + "▼ Desinformación y ▼ Conflictos: conviene que bajen.\n"
                + "La red de la ciudad (botón del mapa, dentro del nivel) muestra en rojo los lugares a los que llegó el rumor.");
        d.seccion("Botones",
                "• Botón con signo de interrogación (Ayuda): esta ventana.\n"
                + "• Botón del engranaje (Ajustes): modo normal o alto contraste, brillo y sonido.\n"
                + "• Botón del mapa (dentro del nivel): red de Ciudad Nova, indicadores y salud de la ciudad. "
                + "El juego se pausa mientras está abierto.\n"
                + "• Pausa: detiene el nivel y el cronómetro.\n"
                + "• Ciudad y estructuras (en el mapa de niveles): bitácora, grafos y árboles del juego.");
        d.finalizar(true);
    }

    // ------------------------------------------------------------------
    // Bloques de contenido
    // ------------------------------------------------------------------

    private void pasos(String[] pasos) {
        JPanel lista = new JPanel(new GridBagLayout());
        lista.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(3, 0, 3, 10);
        for (int i = 0; i < pasos.length; i++) {
            c.gridy = i;
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            lista.add(new Numero(i + 1), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            lista.add(parrafo(pasos[i], ANCHO_TEXTO - 34, 15, CREMA), c);
        }
        lista.setAlignmentX(0.5f);
        cuerpo.add(lista);
        cuerpo.add(espacio(10));
    }

    private void seccion(String titulo, String texto) {
        Tarjeta t = new Tarjeta(new BorderLayout(0, 4));
        t.add(etiqueta(titulo, Font.BOLD, 16, ORO), BorderLayout.NORTH);
        t.add(parrafo(texto, ANCHO_TEXTO + 8, 14, CREMA), BorderLayout.CENTER);
        t.setAlignmentX(0.5f);
        t.setMaximumSize(new Dimension(ANCHO_TEXTO + 40, Integer.MAX_VALUE));
        cuerpo.add(t);
        cuerpo.add(espacio(10));
    }

    private void simbolos() {
        JLabel lbl = etiqueta("Los símbolos de las burbujas", Font.BOLD, 17, ORO);
        lbl.setAlignmentX(0.5f);
        cuerpo.add(lbl);
        cuerpo.add(espacio(8));
        JPanel grilla = new JPanel(new GridLayout(2, 2, 12, 12));
        grilla.setOpaque(false);
        for (int i = 0; i < SIMBOLOS.length; i++) {
            grilla.add(tarjetaSimbolo(SIMBOLOS[i][0], SIMBOLOS[i][1], SIMBOLOS[i][2], COLORES[i]));
        }
        grilla.setAlignmentX(0.5f);
        grilla.setMaximumSize(new Dimension(ANCHO_TEXTO + 40, Integer.MAX_VALUE));
        cuerpo.add(grilla);
        cuerpo.add(espacio(10));
    }

    private void cierre(String texto) {
        JLabel l = parrafo(texto, ANCHO_TEXTO, Font.BOLD | Font.ITALIC, 15, ORO);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setAlignmentX(0.5f);
        cuerpo.add(l);
    }

    /** Coloca el contenido (con desplazamiento si es largo) y muestra el diálogo. */
    private void finalizar(boolean conScroll) {
        if (conScroll) {
            JScrollPane sp = new JScrollPane(cuerpo);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.getVerticalScrollBar().setUI(new BarraDorada());
            sp.getVerticalScrollBar().setOpaque(false);
            sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
            sp.getVerticalScrollBar().setUnitIncrement(18);
            sp.setPreferredSize(new Dimension(cuerpo.getPreferredSize().width + 24, 470));
            setContenido(sp);
        } else {
            setContenido(cuerpo);
        }
        agregarBoton(botonAceptar());
        mostrar();
    }

    private static JPanel tarjetaSimbolo(String imagen, String nombre, String descripcion, Color color) {
        Tarjeta t = new Tarjeta(new BorderLayout(12, 0), color);
        ImageIcon ic = icono(imagen, 56);
        JLabel lblImg = new JLabel(ic);
        if (ic == null) {
            lblImg.setText("?");
        }
        t.add(lblImg, BorderLayout.WEST);
        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        textos.add(etiqueta(nombre, Font.BOLD, 18, color), BorderLayout.NORTH);
        textos.add(parrafo(descripcion, 180, 13, CREMA), BorderLayout.CENTER);
        t.add(textos, BorderLayout.CENTER);
        return t;
    }

    /** Barra de desplazamiento delgada y dorada. */
    private static class BarraDorada extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = ORO;
            trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected javax.swing.JButton createDecreaseButton(int o) {
            return sinBoton();
        }

        @Override
        protected javax.swing.JButton createIncreaseButton(int o) {
            return sinBoton();
        }

        private static javax.swing.JButton sinBoton() {
            javax.swing.JButton b = new javax.swing.JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle r) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(conAlfa(ORO, 170));
            g2.fillRoundRect(r.x + 2, r.y, r.width - 4, r.height, 6, 6);
            g2.dispose();
        }
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
