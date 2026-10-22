package alcaldedigital.gui;

import alcaldedigital.logica.Partida;
import alcaldedigital.logica.ResultadoRonda;
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
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Ventana de un nivel: el juego de plataformas ocupa todo el espacio y arriba hay
 * una barra con Pausa / Mapa / Ayuda / Ajustes. El botón del mapa abre cómo va la
 * ciudad: el grafo por donde se propaga el rumor, los indicadores y la salud.
 */
public class VentanaNivel extends JFrame {

    /** Se llama al cerrar el nivel para volver al mapa. */
    public interface AlVolver {
        /** @param resultado lo que hizo el jugador de turno en esta ronda */
        void volver(ResultadoRonda resultado);
    }

    private final Partida partida;
    private final int numero;
    private final AlVolver alVolver;
    private final String textoContinuar;
    private final PanelPlataformas juego;
    private boolean cerrado;

    /**
     * @param textoContinuar texto del botón al terminar (por ejemplo "Siguiente jugador")
     */
    public VentanaNivel(Partida partida, int numero, String textoContinuar, AlVolver alVolver) {
        this.textoContinuar = textoContinuar;
        this.partida = partida;
        this.numero = numero;
        this.alVolver = alVolver;
        this.juego = new PanelPlataformas(partida, numero, this::alTerminar);

        setTitle("Alcalde Digital - Nivel " + numero + " - Turno de " + partida.getJugador().getNombre());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1260, 760);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(construirBarra(), BorderLayout.NORTH);
        raiz.add(juego, BorderLayout.CENTER);
        setContentPane(raiz);
        Configuracion.instalarBrillo(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
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

        // Botón del mapa: abre la red de Ciudad Nova, los indicadores y la salud de la ciudad
        BotonImagen mapa = new BotonImagen("/alcaldedigital/recursos/mapa.png", "Ciudad", true);
        mapa.setToolTipText("Ciudad Nova: red, indicadores y salud de la ciudad");
        mapa.setFocusable(false);
        mapa.setPreferredSize(new Dimension(42, mapa.altoParaAncho(42)));
        mapa.addActionListener(e -> conJuegoPausado(() -> new DialogoCiudad(this, partida).mostrar()));

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
        derecha.add(mapa);
        derecha.add(ayuda);
        derecha.add(ajustes);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
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
                "Si sales ahora, tu ronda cuenta como no superada y le pasas el turno al siguiente.",
                "Terminar mi turno", "Seguir jugando");
        if (salir) {
            juego.detener();
            double segundos = juego.getSegundosJugados();
            partida.registrarRonda(numero, false, 0, segundos);
            cerrar(new ResultadoRonda(partida.getJugador(), false, 0, segundos, juego.getPuntosRonda(), 0));
        }
    }

    private void alTerminar(PanelPlataformas.ResultadoNivel r) {
        int bono = partida.registrarRonda(numero, r.gano, r.vidas, r.segundos);
        int puntos = r.puntos + bono + (r.gano ? r.vidas * 25 : 0);
        new DialogoFinNivel(this, partida, numero, r, bono, puntos, textoContinuar).mostrar();
        cerrar(new ResultadoRonda(partida.getJugador(), r.gano, r.vidas, r.segundos, puntos, bono));
    }

    private void cerrar(ResultadoRonda resultado) {
        if (cerrado) {
            return;
        }
        cerrado = true;
        juego.detener();
        dispose();
        alVolver.volver(resultado);
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
