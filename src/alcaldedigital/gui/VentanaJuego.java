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
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
 
/**
 * Ventana principal del juego (Tablero central). Gestiona la interfaz gráfica,
 * las pestañas de visualización del Árbol AVL en vivo, el Árbol de Decisiones,
 * la sección de ayuda, el cronómetro de turnos y la aplicación de temas visuales.
 *
 * @author Naty
 */
public class VentanaJuego extends JFrame {
 
    private final Partida partida;
 
    private final JLabel lblTitulo = new JLabel("ALCALDE DIGITAL");
    private final JLabel lblTurno = new JLabel();
    private final JLabel lblJugador = new JLabel();
    private final JLabel lblTiempo = new JLabel();
    private final JLabel lblAutor = new JLabel();
    private final JLabel lblCredibilidad = new JLabel();
    private final JTextArea areaPublicacion = new JTextArea();
    private final JTextArea areaBitacora = new JTextArea();
    private final JTextArea areaMarcador = new JTextArea();
    private final JPanel panelBotones = new JPanel();
    private JPanel barraSuperior;
    private JScrollPane scrollMarcador;
    private JScrollPane scrollBitacora;
    private final PanelIndicadores panelIndicadores;
    private final PanelArbolAVL panelArbol;
    private final JTabbedPane pestanas = new JTabbedPane();
 
    private Timer cronometro;
    private int segundosRestantes;
 
    /**
     * Inicializa la ventana del juego configurando los componentes principales,
     * la estructura de pestañas y cargando el primer turno de la partida.
     */
    public VentanaJuego(Partida partida) {
        this.partida = partida;
        this.panelIndicadores = new PanelIndicadores(partida.getCiudad());
        this.panelArbol = new PanelArbolAVL(partida.getFeed());
 
        setTitle("Alcalde Digital - Ciudad Nova");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(1000, 720));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
 
        barraSuperior = construirBarraSuperior();
        add(barraSuperior, BorderLayout.NORTH);
 
        JScrollPane scrollArbol = new JScrollPane(panelArbol);
        scrollArbol.setBorder(BorderFactory.createEmptyBorder());
        Tema.estilizarScroll(scrollArbol);
 
        pestanas.setUI(new Tema.PestanasUI());
        pestanas.setFocusable(false);
        pestanas.addTab("Partida", construirPestanaPartida());
        pestanas.addTab("Árbol AVL del feed", scrollArbol);
        pestanas.addTab("Árbol de decisión", construirPestanaDecision());
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
 
        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        izquierda.setOpaque(false);
        izquierda.add(lblTitulo);
        izquierda.add(lblTurno);
 
        JCheckBox chkContraste = new JCheckBox("Modo alto contraste");
        chkContraste.setFocusPainted(false);
        chkContraste.addActionListener((ActionEvent e) -> {
            Tema.setAltoContraste(chkContraste.isSelected());
            aplicarTema();
            repaint();
        });
 
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 22, 0));
        derecha.setOpaque(false);
        derecha.add(chkContraste);
        derecha.add(lblTiempo);
 
        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }
 
    private static Supplier<Color> colorAccion(Accion a) {
        switch (a.name()) {
            case "COMPARTIR":
                return Tema::acento;
            case "VERIFICAR":
                return Tema::positivo;
            case "REPORTAR":
                return Tema::negativo;
            default:
                return Tema::textoSuave;
        }
    }
 
    private JPanel construirPestanaPartida() {
        JPanel raiz = new JPanel(new BorderLayout(14, 14));
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
 
        // --- tarjeta de la publicacion ---
        Tema.Tarjeta tarjeta = new Tema.Tarjeta(new BorderLayout(8, 8), 20);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
 
        lblJugador.setFont(Tema.subtitulo());
        lblAutor.setFont(Tema.normal());
        lblCredibilidad.setFont(Tema.normal());
 
        areaPublicacion.setEditable(false);
        areaPublicacion.setLineWrap(true);
        areaPublicacion.setWrapStyleWord(true);
        areaPublicacion.setOpaque(false);
        areaPublicacion.setFont(Tema.destacado());
        areaPublicacion.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 0));
 
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
        panelBotones.setLayout(new GridLayout(1, 4, 12, 12));
        panelBotones.setOpaque(false);
        panelBotones.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        for (Accion a : Accion.values()) {
            Tema.Boton b = new Tema.Boton(a.getEtiqueta().toUpperCase(), colorAccion(a));
            b.setPreferredSize(new Dimension(150, 58));
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
        areaBitacora.setMargin(new java.awt.Insets(4, 8, 4, 8));
        scrollBitacora = new JScrollPane(areaBitacora);
        scrollBitacora.setPreferredSize(new Dimension(100, 150));
        scrollBitacora.setBorder(bordeTitulado("Consecuencias y eventos"));
        Tema.estilizarScroll(scrollBitacora);
 
        // --- lateral ---
        areaMarcador.setEditable(false);
        areaMarcador.setFont(Tema.mono());
        areaMarcador.setMargin(new java.awt.Insets(4, 8, 4, 8));
        scrollMarcador = new JScrollPane(areaMarcador);
        scrollMarcador.setBorder(bordeTitulado("Jugadores"));
        scrollMarcador.setPreferredSize(new Dimension(300, 130));
        Tema.estilizarScroll(scrollMarcador);
 
        JPanel lateral = new JPanel(new BorderLayout(0, 14));
        lateral.setOpaque(false);
        lateral.setPreferredSize(new Dimension(330, 100));
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
        area.setMargin(new java.awt.Insets(14, 16, 14, 16));
        area.setText("ÁRBOL DE DECISIÓN (recorrido en PREORDEN)\n"
                + "Nodos totales: " + partida.getArbolDecision().cantidadNodos() + "\n"
                + "Nivel 1 = lo que decide el jugador. Nivel 2 = lo que la publicación era en realidad.\n\n"
                + partida.getArbolDecision().recorridoPreorden());
        area.setCaretPosition(0);
 
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createEmptyBorder());
        Tema.estilizarScroll(sp);
 
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        p.add(sp, BorderLayout.CENTER);
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
        area.setMargin(new java.awt.Insets(18, 22, 18, 22));
 
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createEmptyBorder());
        Tema.estilizarScroll(sp);
 
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }
 
    private String textoAyuda() {
        StringBuilder sb = new StringBuilder();
        sb.append("OBJETIVO DEL JUEGO\n");
        sb.append("Ciudad Nova elige alcalde en pocos días. Tu misión es que la ciudad llegue a la votación ");
        sb.append("con la mayor información verificada, confianza, convivencia y bienestar digital posibles, ");
        sb.append("y con la menor desinformación y conflictos.\n\n");
 
        sb.append("CÓMO SE JUEGA\n");
        sb.append("En cada turno recibes una publicación del feed de Civitas y tienes ")
          .append(Partida.SEGUNDOS_POR_TURNO).append(" segundos para decidir. Si se acaba el tiempo, ");
        sb.append("la publicación sigue circulando sin control y la ciudad lo paga.\n\n");
 
        sb.append("LOS CUATRO BOTONES\n");
        sb.append("COMPARTIR - la difundes. Si era verdadera, informas; si era falsa, propagas el daño.\n");
        sb.append("VERIFICAR - revisas antes de actuar. Es la acción más segura y la que más puntos da.\n");
        sb.append("IGNORAR  - la dejas pasar. No hace daño, pero tampoco ayuda.\n");
        sb.append("REPORTAR  - la sacas del feed. Excelente contra lo falso, grave si era cierto u opinión.\n\n");
 
        sb.append("LOS ROLES\n");
        for (alcaldedigital.modelo.Rol r : alcaldedigital.modelo.Rol.values()) {
            sb.append(r.getEtiqueta()).append(" - ").append(r.getDescripcion()).append('\n');
        }
        sb.append('\n');
 
        sb.append("LOS INDICADORES\n");
        sb.append("Información verificada, Confianza, Convivencia y Bienestar digital: mientras más altos, mejor.\n");
        sb.append("Desinformación y Conflictos: mientras más bajos, mejor.\n");
        sb.append("La Salud global resume los seis y decide cómo termina la ciudad.\n\n");
 
        sb.append("CÓMO SE GANA\n");
        sb.append("Al cerrar la campaña se elige alcalde. Gana quien combine más puntos y más reputación, ");
        sb.append("es decir, quien haya tomado más decisiones responsables. Pero si la ciudad termina ");
        sb.append("desinformada, gana un alcalde de una ciudad rota: el verdadero resultado es el de todos.\n\n");
 
        sb.append("ACCESIBILIDAD\n");
        sb.append("La casilla 'Modo alto contraste' sube el contraste por encima de 7:1 y refuerza los ");
        sb.append("indicadores con números, símbolos (+/-) y patrones, para que nada dependa solo del color.\n");
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
        lblJugador.setText("Decide: " + j.getNombre() + "  ·  " + j.getRol().getEtiqueta());
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
        lblTiempo.setFont(Tema.titulo());
        lblTiempo.setForeground(Tema.texto());
 
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
            msg.append("SE ACABÓ EL TIEMPO\n\n");
        }
        msg.append("Era: ").append(r.publicacion.getTipo().getEtiqueta()).append("\n\n");
        msg.append(r.efecto.getMensaje()).append("\n\n");
        msg.append("Ruta en el árbol de decisión:\n").append(r.rutaArbol).append("\n\n");
        msg.append("Efecto en la ciudad: ").append(r.efecto.resumenDeltas()).append('\n');
        msg.append("Puntos: ").append(r.puntos >= 0 ? "+" : "").append(r.puntos);
        msg.append("     Reputación: ").append(r.reputacion >= 0 ? "+" : "").append(r.reputacion).append('\n');
        if (r.retirada) {
            msg.append("\nLa publicación fue ELIMINADA del árbol AVL. Mira la pestaña del árbol: se rebalanceó.");
        }
        if (r.evento != null) {
            msg.append("\n\n").append(r.evento);
        }
 
        Tema.instalarUI();
        Tema.mostrarDialogo(this, "Consecuencia de tu decisión", msg.toString());
 
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
 
    /**
     * Finaliza la partida al agotarse los turnos, mostrando el resumen final de la campaña,
     * el alcalde electo, las estadísticas de los jugadores y la salud global de Ciudad Nova.
     */
    private void finalizar() {
        habilitarBotones(false);
        lblTiempo.setText("Campaña cerrada");
        Jugador alcalde = partida.alcaldeElecto();
 
        StringBuilder sb = new StringBuilder();
        sb.append("ELECCIÓN EN CIUDAD NOVA\n\n");
        sb.append("Alcalde electo: ").append(alcalde.getNombre())
          .append(" (").append(alcalde.getRol().getEtiqueta()).append(")\n\n");
        sb.append("Marcador final:\n");
        for (Jugador j : partida.getJugadores()) {
            sb.append("  ").append(j.getNombre()).append(" - ")
              .append(j.getPuntos()).append(" pts, reputación ").append(j.getReputacion()).append('\n');
        }
        sb.append("\nSalud global de la ciudad: ").append(partida.getCiudad().saludGlobal()).append("/100\n\n");
        sb.append(partida.veredicto());
 
        Tema.instalarUI();
        Tema.mostrarDialogo(this, "Resultado final", sb.toString());
        actualizarMarcador();
        actualizarBitacora();
        panelIndicadores.repaint();
        panelArbol.repaint();
    }
 
    // ------------------------------------------------------------------
    // Tema
    // ------------------------------------------------------------------
 
    /** Genera un borde con título legible adaptado al tema visual activo. */
    private static javax.swing.border.Border bordeTitulado(String titulo) {
        javax.swing.border.TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Tema.borde(), 1), "  " + titulo + "  ");
        b.setTitleColor(Tema.textoSuave());
        b.setTitleFont(Tema.indicadorNegrita());
        return b;
    }
 
    private void aplicarTema() {
        Tema.instalarUI();
        pintar(getContentPane());
 
        // Tipografías que dependen del modo (normal / alto contraste)
        lblTitulo.setFont(Tema.titulo());
        lblTurno.setFont(Tema.normal());
        lblTiempo.setFont(Tema.titulo());
        lblJugador.setFont(Tema.subtitulo());
        lblAutor.setFont(Tema.normal());
        lblCredibilidad.setFont(Tema.normal());
        areaPublicacion.setFont(Tema.destacado());
        areaBitacora.setFont(Tema.mono());
        areaMarcador.setFont(Tema.mono());
        pestanas.setFont(Tema.subtitulo());
 
        // Colores específicos
        lblTitulo.setForeground(Tema.acento());
        lblTurno.setForeground(Tema.textoSuave());
        lblAutor.setForeground(Tema.textoSuave());
        lblCredibilidad.setForeground(Tema.textoSuave());
        lblTiempo.setForeground(Tema.texto());
 
        barraSuperior.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.borde()),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));
 
        if (scrollMarcador != null) {
            scrollMarcador.setBorder(bordeTitulado("Jugadores"));
        }
        if (scrollBitacora != null) {
            scrollBitacora.setBorder(bordeTitulado("Consecuencias y eventos"));
        }
        pestanas.revalidate();
        repaint();
    }
 
    private void pintar(Component c) {
        if (c instanceof JPanel || c instanceof JTabbedPane) {
            c.setBackground(Tema.fondo());
        }
        if (c instanceof JTextArea || c instanceof javax.swing.JViewport || c instanceof JScrollPane) {
            c.setBackground(Tema.panel());
        }
        if (c instanceof JCheckBox) {
            c.setBackground(Tema.fondo());
            c.setFont(Tema.normal());
            ((JCheckBox) c).setOpaque(false);
        }
        if (c instanceof JScrollPane) {
            ((JScrollPane) c).getViewport().setBackground(Tema.panel());
        }
        if (c instanceof JLabel || c instanceof JTextArea || c instanceof JCheckBox) {
            c.setForeground(Tema.texto());
        }
        if (c instanceof JButton && !Boolean.TRUE.equals(((JButton) c).getClientProperty("moderno"))) {
            JButton btn = (JButton) c;
            btn.setOpaque(true);
            btn.setBackground(Tema.panel());
            btn.setForeground(Tema.texto());
            btn.setBorder(BorderFactory.createLineBorder(Tema.borde(), 2));
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
 
    /** Retorna el color de fondo utilizado por los paneles de desplazamiento internos. */
    public static Color fondoScroll() {
        return Tema.panel();
    }
}