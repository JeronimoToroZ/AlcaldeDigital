package alcaldedigital.gui;

import alcaldedigital.estructuras.GrafoNiveles;
import alcaldedigital.estructuras.NodoNivel;
import alcaldedigital.logica.GrafoSocial;
import alcaldedigital.logica.NodoRed;
import alcaldedigital.logica.Partida;
import alcaldedigital.modelo.TipoContenido;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

/**
 * "Ciudad y estructuras": muestra en vivo las estructuras de datos del juego y
 * sus operaciones, para poder explicarlas en la sustentación:
 *   - Grafo de niveles (lista de adyacencia, BFS, DFS, caminos, estado).
 *   - Red de la ciudad (grafo no dirigido con la propagación del rumor).
 *   - Indicadores de la ciudad y bitácora de decisiones.
 *   - Árbol AVL del feed.
 *   - Árbol de decisión (con la acción ideal que calcula para cada tipo).
 */
public class DialogoEstructuras extends JDialog {

    private final Partida partida;

    public DialogoEstructuras(Window padre, Partida partida) {
        super(padre, "Ciudad Nova y estructuras de datos", ModalityType.APPLICATION_MODAL);
        this.partida = partida;

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setUI(new Tema.PestanasUI());
        pestanas.setFocusable(false);
        pestanas.addTab("Grafo de niveles", envolver(texto(textoGrafoNiveles())));
        pestanas.addTab("Red de la ciudad", construirRed());
        pestanas.addTab("Indicadores", construirIndicadores());

        JScrollPane scrollArbol = new JScrollPane(new PanelArbolAVL(partida.getFeed()));
        scrollArbol.setBorder(BorderFactory.createEmptyBorder());
        Tema.estilizarScroll(scrollArbol);
        pestanas.addTab("Árbol AVL del feed", scrollArbol);
        pestanas.addTab("Árbol de decisión", envolver(texto(textoArbolDecision())));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.fondo());
        raiz.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        raiz.add(pestanas, BorderLayout.CENTER);
        setContentPane(raiz);
        pintar(raiz);

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(1080, 720);
        setLocationRelativeTo(padre);
    }

    // ------------------------------------------------------------------
    // Pestañas
    // ------------------------------------------------------------------

    private String textoGrafoNiveles() {
        GrafoNiveles g = partida.getNiveles();
        StringBuilder sb = new StringBuilder();
        sb.append("GRAFO DIRIGIDO DE NIVELES\n");
        sb.append("Vértices: ").append(g.cantidadVertices()).append("    Aristas: ").append(g.cantidadAristas())
          .append("    Representación: listas de adyacencia\n");
        sb.append("Regla: un nivel se desbloquea cuando se gana alguno de sus predecesores.\n\n");
        sb.append("LISTA DE ADYACENCIA (A -> B: ganar A desbloquea B)\n").append(g.listaAdyacencia()).append('\n');
        sb.append("Recorrido BFS desde N1 (por profundidad): ").append(nombres(g.recorridoBFS(1))).append('\n');
        sb.append("Recorrido DFS desde N1 (rama por rama):   ").append(nombres(g.recorridoDFS(1))).append("\n\n");
        sb.append("ESTADO DE CADA NIVEL\n");
        for (NodoNivel n : g.getNodos()) {
            String estado = n.isCompletado() ? "GANADO (" + n.getEstrellas() + " estrellas, mejor " + n.getMejorPuntaje() + " pts)"
                    : g.estaDesbloqueado(n.getNumero()) ? "DISPONIBLE" : "BLOQUEADO";
            sb.append(String.format("N%d  profundidad %d  camino %-18s  %s%n", n.getNumero(),
                    g.profundidad(n.getNumero()), nombres(g.camino(1, n.getNumero())), estado));
        }
        return sb.toString();
    }

    private JPanel construirRed() {
        GrafoSocial red = partida.getRed();
        StringBuilder sb = new StringBuilder();
        sb.append("GRAFO NO DIRIGIDO DE LA CIUDAD\n");
        sb.append("Lugares: ").append(red.nodos.size()).append("   Conexiones: ").append(red.cantidadAristas()).append("\n\n");
        if (red.getOrigen() == null) {
            sb.append("Todavía no ha aparecido ningún rumor.\nJuega un nivel para verlo propagarse.");
        } else {
            sb.append("Origen del rumor: ").append(red.getOrigen().nombre).append('\n');
            sb.append("Lugares con el rumor: ").append(red.cantidadInfectados()).append(" de ")
              .append(red.nodos.size()).append("\n\n");
            sb.append("Si nadie lo detiene, llegaría en este orden (BFS):\n");
            Map<NodoRed, Integer> dist = red.distanciasDesde(red.getOrigen());
            for (NodoRed n : red.ordenDePropagacion()) {
                sb.append(String.format("  %-11s salto %d  %s%n", n.nombre, dist.get(n),
                        n.estaInfectado ? "[RUMOR]" : ""));
            }
            sb.append("\nCada error en un nivel = un paso de BFS\n(todos los vecinos sanos se contagian).\n");
            sb.append("Cada publicación bien resuelta limpia\nlos lugares más lejanos del origen.");
        }
        JTextArea area = texto(sb.toString());
        area.setColumns(34);

        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        p.add(new PanelRedCiudad(red, false), BorderLayout.CENTER);
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(340, 10));
        Tema.estilizarScroll(sp);
        p.add(sp, BorderLayout.EAST);
        return p;
    }

    private JPanel construirIndicadores() {
        PanelIndicadores ind = new PanelIndicadores(partida.getCiudad());
        ind.setPreferredSize(new Dimension(380, 320));
        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.add(ind, BorderLayout.NORTH);

        StringBuilder sb = new StringBuilder();
        sb.append("BITÁCORA DE LA CAMPAÑA\n\n");
        List<String> bit = partida.getBitacora();
        for (int i = bit.size() - 1; i >= 0; i--) {   // lo más reciente arriba
            sb.append("- ").append(bit.get(i)).append("\n");
        }
        JTextArea area = texto(sb.toString());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        p.add(izquierda, BorderLayout.WEST);
        JScrollPane sp = new JScrollPane(area);
        Tema.estilizarScroll(sp);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private String textoArbolDecision() {
        StringBuilder sb = new StringBuilder();
        sb.append("ÁRBOL DE DECISIÓN N-ARIO (4 acciones x 4 tipos = 16 hojas)\n");
        sb.append("Nodos totales: ").append(partida.getArbolDecision().cantidadNodos()).append("\n\n");
        sb.append("BURBUJA CORRECTA SEGÚN EL ÁRBOL (la hoja con más puntos de cada tipo):\n");
        for (TipoContenido t : TipoContenido.values()) {
            sb.append(String.format("  %-26s -> %s%n", t.getEtiqueta(),
                    partida.getArbolDecision().accionIdeal(t).getEtiqueta()));
        }
        sb.append("\nRECORRIDO EN PREORDEN\n");
        sb.append(partida.getArbolDecision().recorridoPreorden());
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static String nombres(List<Integer> lista) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            sb.append(i == 0 ? "" : " -> ").append("N").append(lista.get(i));
        }
        return sb.toString();
    }

    private static JTextArea texto(String contenido) {
        JTextArea a = new JTextArea(contenido);
        a.setEditable(false);
        a.setFont(Tema.mono());
        a.setForeground(Tema.texto());
        a.setMargin(new java.awt.Insets(14, 16, 14, 16));
        a.setCaretPosition(0);
        return a;
    }

    private static JPanel envolver(JTextArea area) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createEmptyBorder());
        Tema.estilizarScroll(sp);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    /** Aplica los colores del tema (normal o alto contraste) a todo el árbol de componentes. */
    private static void pintar(java.awt.Component c) {
        if (c instanceof JPanel || c instanceof JTabbedPane) {
            c.setBackground(Tema.fondo());
        }
        if (c instanceof JTextArea || c instanceof javax.swing.JViewport || c instanceof JScrollPane) {
            c.setBackground(Tema.panel());
        }
        if (c instanceof java.awt.Container) {
            for (java.awt.Component hijo : ((java.awt.Container) c).getComponents()) {
                pintar(hijo);
            }
        }
    }
}
