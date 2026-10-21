package alcaldedigital.gui;

import alcaldedigital.logica.Partida;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Ventana de un nivel: el juego de plataformas en el centro, una barra superior
 * con Pausa / Ayuda / Ajustes y, a la derecha, cómo va la ciudad: el grafo por
 * donde se propaga el rumor, los indicadores y el tamaño del feed (AVL).
 */
public class VentanaNivel extends JFrame {

    /** Se llama al cerrar el nivel para volver al mapa. */
    public interface AlVolver {
        /**
         * @param desbloqueados niveles que se abrieron con esta victoria (vacío si perdió)
         * @param reintentar    true si el jugador pidió repetir el nivel
         */
        void volver(List<Integer> desbloqueados, boolean reintentar);
    }

    private final Partida partida;
    private final int numero;
    private final AlVolver alVolver;
    private final PanelPlataformas juego;
    private final JLabel lblRumor = new JLabel();
    private final JLabel lblFeed = new JLabel();
    private final IndicadoresMini indicadores;
    private final Timer refresco;
    private boolean cerrado;

    public VentanaNivel(Partida partida, int numero, AlVolver alVolver) {
        this.partida = partida;
        this.numero = numero;
        this.alVolver = alVolver;
        this.juego = new PanelPlataformas(partida, numero, this::alTerminar);
        this.indicadores = new IndicadoresMini(partida.getCiudad());

        setTitle("Alcalde Digital - Nivel " + numero);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1260, 760);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(construirBarra(), BorderLayout.NORTH);
        raiz.add(juego, BorderLayout.CENTER);
        raiz.add(construirLateral(), BorderLayout.EAST);
        setContentPane(raiz);
        Configuracion.instalarBrillo(this);

        refresco = new Timer(300, e -> actualizarLateral());
        actualizarLateral();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                refresco.start();
                juego.iniciar();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // si el jugador cambia de ventana, el juego se pausa solo
                if (!juego.estaTerminado()) {
                    juego.setPausado(true);
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                abandonar();
            }
        });
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz
    // ------------------------------------------------------------------

    private JPanel construirBarra() {
        JPanel barra = new FondoCafe(new BorderLayout());
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, DialogoJuego.ORO),
                BorderFactory.createEmptyBorder(8, 18, 8, 14)));

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        izquierda.setOpaque(false);
        izquierda.add(DialogoJuego.etiqueta("Nivel " + numero, Font.BOLD | Font.ITALIC, 26, DialogoJuego.ORO));
        izquierda.add(DialogoJuego.etiqueta(partida.getJugador().getNombre(), Font.PLAIN, 16, DialogoJuego.CREMA));
        izquierda.add(DialogoJuego.etiqueta("←  →  moverse   ·   Espacio  saltar   ·   P  pausa",
                Font.PLAIN, 13, DialogoJuego.CREMA_SUAVE));

        DialogoJuego.BotonDorado pausa = new DialogoJuego.BotonDorado("Pausa", false);
        pausa.setPreferredSize(new Dimension(110, 40));
        pausa.setFocusable(false);
        pausa.addActionListener(e -> juego.setPausado(!juego.isPausado()));

        BotonImagen ayuda = new BotonImagen("/alcaldedigital/recursos/ayuda.png", "Ayuda", true);
        ayuda.setToolTipText("Ayuda");
        ayuda.setFocusable(false);
        ayuda.setPreferredSize(new Dimension(42, ayuda.altoParaAncho(42)));
        ayuda.addActionListener(e -> conJuegoPausado(() -> DialogoInstrucciones.ayuda(this)));

        BotonImagen ajustes = new BotonImagen("/alcaldedigital/recursos/ajuste.png", "Ajustes", true);
        ajustes.setToolTipText("Ajustes");
        ajustes.setFocusable(false);
        ajustes.setPreferredSize(new Dimension(42, ajustes.altoParaAncho(42)));
        ajustes.addActionListener(e -> conJuegoPausado(() -> new DialogoAjustes(this).setVisible(true)));

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        derecha.setOpaque(false);
        derecha.add(pausa);
        derecha.add(ayuda);
        derecha.add(ajustes);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JPanel construirLateral() {
        JPanel lateral = new FondoCafe(null);
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, DialogoJuego.ORO),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        lateral.setPreferredSize(new Dimension(290, 10));

        lateral.add(titulo("Red de Ciudad Nova"));
        lateral.add(DialogoJuego.espacio(4));
        JLabel explica = DialogoJuego.parrafo("Cada error hace avanzar el rumor un salto por la red (BFS). "
                + "Cada publicación bien resuelta lo hace retroceder.", 255, 12, DialogoJuego.CREMA_SUAVE);
        explica.setAlignmentX(0f);
        lateral.add(explica);
        lateral.add(DialogoJuego.espacio(6));
        PanelRedCiudad red = new PanelRedCiudad(partida.getRed(), true);
        red.setAlignmentX(0f);
        red.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        lateral.add(red);
        lateral.add(DialogoJuego.espacio(6));
        lblRumor.setFont(DialogoJuego.fuente(Font.BOLD, 14));
        lblRumor.setAlignmentX(0f);
        lateral.add(lblRumor);

        lateral.add(DialogoJuego.espacio(16));
        lateral.add(titulo("Indicadores"));
        lateral.add(DialogoJuego.espacio(8));
        indicadores.setAlignmentX(0f);
        indicadores.setMaximumSize(new Dimension(Integer.MAX_VALUE, indicadores.getPreferredSize().height));
        lateral.add(indicadores);

        lateral.add(DialogoJuego.espacio(10));
        lblFeed.setFont(DialogoJuego.fuente(Font.PLAIN, 12));
        lblFeed.setForeground(DialogoJuego.CREMA_SUAVE);
        lblFeed.setAlignmentX(0f);
        lateral.add(lblFeed);
        return lateral;
    }

    private static JLabel titulo(String texto) {
        JLabel l = DialogoJuego.etiqueta(texto, Font.BOLD, 16, DialogoJuego.ORO);
        l.setAlignmentX(0f);
        return l;
    }

    private void actualizarLateral() {
        int infectados = partida.getRed().cantidadInfectados();
        int total = partida.getRed().nodos.size();
        lblRumor.setText("Rumor en " + infectados + " de " + total + " lugares");
        lblRumor.setForeground(infectados > total / 2 ? DialogoJuego.ROJO : DialogoJuego.CREMA);
        lblFeed.setText("Feed (árbol AVL): " + partida.getFeed().getTamano() + " publicaciones · altura "
                + partida.getFeed().getAlturaArbol());
        indicadores.repaint();
    }

    // ------------------------------------------------------------------
    // Flujo
    // ------------------------------------------------------------------

    /** Abre un diálogo con el juego en pausa y lo reanuda al cerrarlo (si no estaba pausado antes). */
    private void conJuegoPausado(Runnable dialogo) {
        boolean estabaPausado = juego.isPausado();
        juego.setPausado(true);
        dialogo.run();
        if (!estabaPausado && !juego.estaTerminado()) {
            juego.setPausado(false);
        }
        juego.requestFocusInWindow();
    }

    private void abandonar() {
        if (juego.estaTerminado()) {
            return;
        }
        juego.setPausado(true);
        boolean salir = DialogoJuego.confirmar(this, "¿Abandonar el nivel?",
                "Si sales ahora, el nivel cuenta como no superado y tendrás que repetirlo.",
                "Volver al mapa", "Seguir jugando");
        if (salir) {
            juego.detener();
            partida.terminarNivel(numero, false, 0, 0);
            cerrar(new ArrayList<>(), false);
        }
    }

    private void alTerminar(PanelPlataformas.ResultadoNivel r) {
        List<Integer> desbloqueados = partida.terminarNivel(numero, r.gano, r.vidas, r.puntos);
        actualizarLateral();
        DialogoFinNivel.Opcion opcion = new DialogoFinNivel(this, partida, numero, r, desbloqueados).elegir();
        cerrar(desbloqueados, opcion == DialogoFinNivel.Opcion.REINTENTAR);
    }

    private void cerrar(List<Integer> desbloqueados, boolean reintentar) {
        if (cerrado) {
            return;
        }
        cerrado = true;
        refresco.stop();
        juego.detener();
        dispose();
        alVolver.volver(desbloqueados, reintentar);
    }

    /** Panel con el degradado café de las ventanas del juego. */
    static class FondoCafe extends JPanel {

        FondoCafe(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            if (Tema.isAltoContraste()) {
                g2.setColor(Color.BLACK);
            } else {
                g2.setPaint(new GradientPaint(0, 0, DialogoJuego.CAFE_CLARO, 0, getHeight(), DialogoJuego.CAFE_OSCURO));
            }
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }
}
