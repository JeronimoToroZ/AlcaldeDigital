package alcaldedigital.gui;

import alcaldedigital.logica.Partida;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Lo que se abre con el botón del mapa durante un nivel (el juego queda en pausa):
 *   - Red de Ciudad Nova: el grafo con los lugares a los que llegó el rumor (BFS).
 *   - Indicadores de la ciudad y su salud.
 *   - Tamaño y altura del feed (árbol AVL).
 */
public class DialogoCiudad extends DialogoJuego {

    public DialogoCiudad(Window padre, Partida partida) {
        super(padre, "Ciudad Nova");

        // ---- Red de Ciudad Nova ----
        Tarjeta red = new Tarjeta(null);
        red.setLayout(new BoxLayout(red, BoxLayout.Y_AXIS));
        JLabel tRed = etiqueta("Red de Ciudad Nova", Font.BOLD, 17, ORO);
        tRed.setAlignmentX(0f);
        red.add(tRed);
        red.add(espacio(4));
        JLabel explica = parrafo("Cada error hace avanzar el rumor un salto por la red (BFS). "
                + "Cada publicación bien resuelta lo hace retroceder.", 440, 13, CREMA_SUAVE);
        explica.setAlignmentX(0f);
        red.add(explica);
        red.add(espacio(8));
        PanelRedCiudad grafo = new PanelRedCiudad(partida.getRed(), false);
        grafo.setPreferredSize(new Dimension(440, 350));
        grafo.setMaximumSize(new Dimension(440, 350));
        grafo.setAlignmentX(0f);
        red.add(grafo);
        red.add(espacio(8));
        int infectados = partida.getRed().cantidadInfectados();
        int total = partida.getRed().nodos.size();
        JLabel rumor = etiqueta("Rumor en " + infectados + " de " + total + " lugares", Font.BOLD, 15,
                infectados > total / 2 ? ROJO : CREMA);
        rumor.setAlignmentX(0f);
        red.add(rumor);

        // ---- Indicadores + salud ----
        Tarjeta ind = new Tarjeta(null);
        ind.setLayout(new BoxLayout(ind, BoxLayout.Y_AXIS));
        JLabel tInd = etiqueta("Indicadores", Font.BOLD, 17, ORO);
        tInd.setAlignmentX(0f);
        ind.add(tInd);
        ind.add(espacio(10));
        IndicadoresMini mini = new IndicadoresMini(partida.getCiudad());
        mini.setPreferredSize(new Dimension(260, mini.getPreferredSize().height));
        mini.setMaximumSize(mini.getPreferredSize());
        mini.setAlignmentX(0f);
        ind.add(mini);   // incluye "Salud de la ciudad"
        ind.add(espacio(14));
        JLabel feed = parrafo("Feed (árbol AVL): " + partida.getFeed().getTamano() + " publicaciones · altura "
                + partida.getFeed().getAlturaArbol(), 260, 12, CREMA_SUAVE);
        feed.setAlignmentX(0f);
        ind.add(feed);

        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.VERTICAL;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 12);
        fila.add(red, c);
        c.insets = new Insets(0, 0, 0, 0);
        fila.add(ind, c);

        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.setOpaque(false);
        envoltura.add(fila, BorderLayout.CENTER);
        setContenido(envoltura);

        BotonDorado volver = new BotonDorado("Volver al juego", true);
        volver.addActionListener(e -> dispose());
        agregarBoton(volver);
        getRootPane().setDefaultButton(volver);
    }
}
