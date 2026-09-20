package alcaldedigital.gui;

import alcaldedigital.logica.Partida;
import alcaldedigital.modelo.Accion;
import alcaldedigital.modelo.Jugador;
import alcaldedigital.modelo.Publicacion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/** Ventana principal: tablero, arbol AVL en vivo, arbol de decision y ayuda. */
public class VentanaJuego extends JFrame {

    private final Partida partida;

    private final JLabel lblTurno = new JLabel();
    private final JLabel lblJugador = new JLabel();
    private final JLabel lblTiempo = new JLabel();
    private final JLabel lblAutor = new JLabel();
    private final JLabel lblCredibilidad = new JLabel();
    private final JTextArea areaPublicacion = new JTextArea();
    private final JTextArea areaBitacora = new JTextArea();
    private final JTextArea areaMarcador = new JTextArea();
    private final JPanel panelBotones = new JPanel();
    private JScrollPane scrollMarcador;
    private final PanelIndicadores panelIndicadores;
    private final PanelArbolAVL panelArbol;
    private final JTabbedPane pestanas = new JTabbedPane();

    private Timer cronometro;
    private int segundosRestantes;

    public VentanaJuego(Partida partida) {
        this.partida = partida;
        this.panelIndicadores = new PanelIndicadores(partida.getCiudad());
        this.panelArbol = new PanelArbolAVL(partida.getFeed());

        setTitle("Alcalde Digital - Ciudad Nova");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(construirBarraSuperior(), BorderLayout.NORTH);

        pestanas.addTab("Partida", construirPestanaPartida());
        pestanas.addTab("Arbol AVL del feed", new JScrollPane(panelArbol));
        pestanas.addTab("Arbol de decision", construirPestanaDecision());
        pestanas.addTab("Ayuda", construirPestanaAyuda());
        add(pestanas, BorderLayout.CENTER);

        aplicarTema();
        cargarTurno();
    }

    // ------------------------------------------------------------------
    // Construccion de la interfaz
    // ------------------------------------------------------------------

    private JPanel construirBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel titulo = new JLabel("ALCALDE DIGITAL");
        titulo.setFont(Tema.titulo());

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        izquierda.setOpaque(false);
        izquierda.add(titulo);
        izquierda.add(lblTurno);

        JCheckBox chkContraste = new JCheckBox("Modo alto contraste");
        chkContraste.setFocusPainted(false);
        chkContraste.addActionListener((ActionEvent e) -> {
            Tema.setAltoContraste(chkContraste.isSelected());
            aplicarTema();
            repaint();
        });

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        derecha.setOpaque(false);
        derecha.add(chkContraste);
        derecha.add(lblTiempo);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JPanel construirPestanaPartida() {
        JPanel raiz = new JPanel(new BorderLayout(12, 12));
        raiz.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // --- tarjeta de la publicacion ---
        JPanel tarjeta = new JPanel(new BorderLayout(8, 8));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.borde(), 2),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        lblJugador.setFont(Tema.subtitulo());
        lblAutor.setFont(Tema.normal());
        lblCredibilidad.setFont(Tema.normal());

        areaPublicacion.setEditable(false);
        areaPublicacion.setLineWrap(true);
        areaPublicacion.setWrapStyleWord(true);
        areaPublicacion.setFont(Tema.titulo());
        areaPublicacion.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));

        JPanel cabecera = new JPanel();
        cabecera.setLayout(new BoxLayout(cabecera, BoxLayout.Y_AXIS));
        cabecera.setOpaque(false);
        lblJugador.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblAutor.setAlignmentX(Component.LEFT_ALIGNMENT);
        cabecera.add(lblJugador);
        cabecera.add(Box.createVerticalStrut(6));
        cabecera.add(lblAutor);

        tarjeta.add(cabecera, BorderLayout.NORTH);
        tarjeta.add(areaPublicacion, BorderLayout.CENTER);
        tarjeta.add(lblCredibilidad, BorderLayout.SOUTH);

        // --- botones de accion ---
        panelBotones.setLayout(new GridLayout(1, 4, 10, 10));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        for (Accion a : Accion.values()) {
            JButton b = new JButton(a.getEtiqueta().toUpperCase());
            b.setFont(Tema.subtitulo());
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(150, 56));
            b.addActionListener(e -> resolverTurno(a, false));
            panelBotones.add(b);
        }

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(tarjeta, BorderLayout.CENTER);
        centro.add(panelBotones, BorderLayout.SOUTH);

        // --- bitacora ---
        areaBitacora.setEditable(false);
        areaBitacora.setLineWrap(true);
        areaBitacora.setWrapStyleWord(true);
        areaBitacora.setFont(Tema.mono());
        JScrollPane scrollBitacora = new JScrollPane(areaBitacora);
        scrollBitacora.setPreferredSize(new Dimension(100, 190));
        scrollBitacora.setBorder(bordeTitulado("Consecuencias y eventos"));

        // --- lateral ---
        areaMarcador.setEditable(false);
        areaMarcador.setFont(Tema.mono());
        scrollMarcador = new JScrollPane(areaMarcador);
        scrollMarcador.setBorder(bordeTitulado("Jugadores"));
        scrollMarcador.setPreferredSize(new Dimension(300, 175));

        JPanel lateral = new JPanel(new BorderLayout(0, 12));
        lateral.setOpaque(false);
        lateral.setPreferredSize(new Dimension(320, 100));
        lateral.add(panelIndicadores, BorderLayout.CENTER);
        lateral.add(scrollMarcador, BorderLayout.SOUTH);

        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(lateral, BorderLayout.EAST);
        raiz.add(scrollBitacora, BorderLayout.SOUTH);
        return raiz;
    }

    private JPanel construirPestanaDecision() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Tema.mono());
        area.setText("ARBOL DE DECISION (recorrido en PREORDEN)\n"
                + "Nodos totales: " + partida.getArbolDecision().cantidadNodos() + "\n"
                + "Nivel 1 = lo que decide el jugador. Nivel 2 = lo que la publicacion era en realidad.\n\n"
                + partida.getArbolDecision().recorridoPreorden());
        area.setCaretPosition(0);

        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }

    private JPanel construirPestanaAyuda() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Tema.normal());
        area.setText(textoAyuda());
        area.setCaretPosition(0);
        area.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }

    private String textoAyuda() {
        StringBuilder sb = new StringBuilder();
        sb.append("OBJETIVO DEL JUEGO\n");
        sb.append("Ciudad Nova elige alcalde en pocos dias. Tu mision es que la ciudad llegue a la votacion ");
        sb.append("con la mayor informacion verificada, confianza, convivencia y bienestar digital posibles, ");
        sb.append("y con la menor desinformacion y conflictos.\n\n");

        sb.append("COMO SE JUEGA\n");
        sb.append("En cada turno recibes una publicacion del feed de Civitas y tienes ")
          .append(Partida.SEGUNDOS_POR_TURNO).append(" segundos para decidir. Si se acaba el tiempo, ");
        sb.append("la publicacion sigue circulando sin control y la ciudad lo paga.\n\n");

        sb.append("LOS CUATRO BOTONES\n");
        sb.append("COMPARTIR - la difundes. Si era verdadera, informas; si era falsa, propagas el dano.\n");
        sb.append("VERIFICAR - revisas antes de actuar. Es la accion mas segura y la que mas puntos da.\n");
        sb.append("IGNORAR   - la dejas pasar. No hace dano, pero tampoco ayuda.\n");
        sb.append("REPORTAR  - la sacas del feed. Excelente contra lo falso, grave si era cierto u opinion.\n\n");

        sb.append("LOS ROLES\n");
        for (alcaldedigital.modelo.Rol r : alcaldedigital.modelo.Rol.values()) {
            sb.append(r.getEtiqueta()).append(" - ").append(r.getDescripcion()).append('\n');
        }
        sb.append('\n');

        sb.append("LOS INDICADORES\n");
        sb.append("Informacion verificada, Confianza, Convivencia y Bienestar digital: mientras mas altos, mejor.\n");
        sb.append("Desinformacion y Conflictos: mientras mas bajos, mejor.\n");
        sb.append("La Salud global resume los seis y decide como termina la ciudad.\n\n");

        sb.append("COMO SE GANA\n");
        sb.append("Al cerrar la campana se elige alcalde. Gana quien combine mas puntos y mas reputacion, ");
        sb.append("es decir, quien haya tomado mas decisiones responsables. Pero si la ciudad termina ");
        sb.append("desinformada, gana un alcalde de una ciudad rota: el verdadero resultado es el de todos.\n\n");

        sb.append("ACCESIBILIDAD\n");
        sb.append("La casilla 'Modo alto contraste' sube el contraste por encima de 7:1 y refuerza los ");
        sb.append("indicadores con numeros, simbolos (+/-) y patrones, para que nada dependa solo del color.\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Ciclo de turno
    // ------------------------------------------------------------------

    private void cargarTurno() {
        if (partida.terminada()) {
            finalizar();
            return;
        }
        Publicacion p = partida.publicacionActual();
        Jugador j = partida.jugadorActual();

        lblTurno.setText("Turno " + partida.getTurno() + " de " + partida.getTotalTurnos());
        lblJugador.setText("Decide: " + j.getNombre() + "  -  " + j.getRol().getEtiqueta());
        lblAutor.setText("Publicado por " + p.getAutor());
        areaPublicacion.setText("\"" + p.getTexto() + "\"");
        lblCredibilidad.setText("Credibilidad aparente: " + p.getCredibilidad()
                + "/100     (clave en el AVL: " + p.claveAVL() + ")");

        actualizarMarcador();
        actualizarBitacora();
        panelIndicadores.repaint();
        panelArbol.revalidate();
        panelArbol.repaint();

        habilitarBotones(true);
        iniciarCronometro();
    }

    private void iniciarCronometro() {
        if (cronometro != null) {
            cronometro.stop();
        }
        segundosRestantes = Partida.SEGUNDOS_POR_TURNO;
        lblTiempo.setText("Tiempo: " + segundosRestantes + "s");
        lblTiempo.setFont(Tema.subtitulo());

        cronometro = new Timer(1000, e -> {
            segundosRestantes--;
            lblTiempo.setText("Tiempo: " + Math.max(0, segundosRestantes) + "s");
            lblTiempo.setForeground(segundosRestantes <= 3 ? Tema.negativo() : Tema.texto());
            if (segundosRestantes <= 0) {
                ((Timer) e.getSource()).stop();
                resolverTurno(Accion.IGNORAR, true);
            }
        });
        cronometro.start();
    }

    private void resolverTurno(Accion accion, boolean porTiempo) {
        if (cronometro != null) {
            cronometro.stop();
        }
        habilitarBotones(false);

        Partida.ResultadoTurno r = porTiempo ? partida.tiempoAgotado() : partida.ejecutar(accion);

        StringBuilder msg = new StringBuilder();
        if (porTiempo) {
            msg.append("SE ACABO EL TIEMPO\n\n");
        }
        msg.append("Era: ").append(r.publicacion.getTipo().getEtiqueta()).append("\n\n");
        msg.append(r.efecto.getMensaje()).append("\n\n");
        msg.append("Ruta en el arbol de decision:\n").append(r.rutaArbol).append("\n\n");
        msg.append("Efecto en la ciudad: ").append(r.efecto.resumenDeltas()).append('\n');
        msg.append("Puntos: ").append(r.puntos >= 0 ? "+" : "").append(r.puntos);
        msg.append("     Reputacion: ").append(r.reputacion >= 0 ? "+" : "").append(r.reputacion).append('\n');
        if (r.retirada) {
            msg.append("\nLa publicacion fue ELIMINADA del arbol AVL. Mira la pestana del arbol: se rebalanceo.");
        }
        if (r.evento != null) {
            msg.append("\n\n").append(r.evento);
        }

        JOptionPane.showMessageDialog(this, msg.toString(),
                "Consecuencia de tu decision", JOptionPane.INFORMATION_MESSAGE);

        cargarTurno();
    }

    private void habilitarBotones(boolean estado) {
        for (Component c : panelBotones.getComponents()) {
            c.setEnabled(estado);
        }
    }

    private void actualizarMarcador() {
        StringBuilder sb = new StringBuilder();
        for (Jugador j : partida.getJugadores()) {
            sb.append(j == partida.jugadorActual() ? "> " : "  ");
            sb.append(String.format("%s - %s%n", j.getNombre(), j.getRol().getEtiqueta()));
            sb.append(String.format("   %d pts | rep %d | %d ok %d err%n",
                    j.getPuntos(), j.getReputacion(), j.getAciertos(), j.getErrores()));
        }
        areaMarcador.setText(sb.toString());
    }

    private void actualizarBitacora() {
        StringBuilder sb = new StringBuilder();
        for (String linea : partida.getBitacora()) {
            sb.append("- ").append(linea).append('\n');
        }
        areaBitacora.setText(sb.toString());
        areaBitacora.setCaretPosition(areaBitacora.getDocument().getLength());
    }

    private void finalizar() {
        habilitarBotones(false);
        lblTiempo.setText("Campana cerrada");
        Jugador alcalde = partida.alcaldeElecto();

        StringBuilder sb = new StringBuilder();
        sb.append("ELECCION EN CIUDAD NOVA\n\n");
        sb.append("Alcalde electo: ").append(alcalde.getNombre())
          .append(" (").append(alcalde.getRol().getEtiqueta()).append(")\n\n");
        sb.append("Marcador final:\n");
        for (Jugador j : partida.getJugadores()) {
            sb.append("  ").append(j.getNombre()).append(" - ")
              .append(j.getPuntos()).append(" pts, reputacion ").append(j.getReputacion()).append('\n');
        }
        sb.append("\nSalud global de la ciudad: ").append(partida.getCiudad().saludGlobal()).append("/100\n\n");
        sb.append(partida.veredicto());

        JOptionPane.showMessageDialog(this, sb.toString(), "Resultado final", JOptionPane.INFORMATION_MESSAGE);
        actualizarMarcador();
        actualizarBitacora();
        panelIndicadores.repaint();
        panelArbol.repaint();
    }

    // ------------------------------------------------------------------
    // Tema
    // ------------------------------------------------------------------

    /** Borde con titulo legible en cualquiera de los dos temas. */
    private static javax.swing.border.Border bordeTitulado(String titulo) {
        javax.swing.border.TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Tema.borde(), 1), titulo);
        b.setTitleColor(Tema.texto());
        b.setTitleFont(Tema.normal());
        return b;
    }

    private void aplicarTema() {
        pintar(getContentPane());
        lblTiempo.setForeground(Tema.texto());

        // las pestanas tambien deben respetar el tema
        pestanas.setForeground(Tema.texto());
        for (int i = 0; i < pestanas.getTabCount(); i++) {
            pestanas.setBackgroundAt(i, Tema.panel());
            pestanas.setForegroundAt(i, Tema.texto());
        }

        // los bordes con titulo se reconstruyen para que cambien de color
        if (scrollMarcador != null) {
            scrollMarcador.setBorder(bordeTitulado("Jugadores"));
        }
        repaint();
    }

    private void pintar(Component c) {
        if (c instanceof JPanel || c instanceof JTabbedPane) {
            c.setBackground(Tema.fondo());
        }
        if (c instanceof JTextArea || c instanceof JButton || c instanceof JCheckBox
                || c instanceof javax.swing.JViewport || c instanceof JScrollPane) {
            c.setBackground(Tema.panel());
        }
        if (c instanceof JScrollPane) {
            ((JScrollPane) c).getViewport().setBackground(Tema.panel());
        }
        if (c instanceof JLabel || c instanceof JTextArea || c instanceof JButton
                || c instanceof JCheckBox) {
            c.setForeground(Tema.texto());
        }
        if (c instanceof JButton) {
            ((JButton) c).setOpaque(true);
            ((JButton) c).setBorder(BorderFactory.createLineBorder(Tema.borde(), 2));
        }
        if (c instanceof JLabel) {
            ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
        }
        if (c instanceof java.awt.Container) {
            for (Component hijo : ((java.awt.Container) c).getComponents()) {
                pintar(hijo);
            }
        }
    }

    /** Color de fondo usado por los scroll internos. */
    public static Color fondoScroll() {
        return Tema.panel();
    }
}
