package alcaldedigital.gui;

import alcaldedigital.estructuras.ArbolAVL;
import alcaldedigital.estructuras.NodoAVL;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.swing.JPanel;

/**
 * VISUALIZACION DEL ARBOL AVL
 * ===========================
 * Dibuja el arbol real, no una maqueta: cada circulo es un NodoAVL vivo del feed.
 *
 * Posicionamiento: la coordenada X de cada nodo se asigna con un recorrido
 * INORDEN (el k-esimo nodo visitado ocupa la columna k) y la Y depende de su
 * profundidad. Ese truco garantiza que dos nodos nunca se solapen.
 *
 * El dibujo se hace en dos pasadas: primero las aristas y despues los nodos,
 * para que las lineas queden por debajo de los circulos.
 *
 * Dentro de cada nodo se muestra la credibilidad y, debajo, el factor de
 * equilibrio. Asi, en la sustentacion, se ve en vivo como el arbol se rebalancea
 * cuando se elimina una publicacion reportada.
 */
public class PanelArbolAVL extends JPanel {

    private static final int RADIO = 22;
    private static final int SEPARACION_X = 64;
    private static final int SEPARACION_Y = 84;
    private static final int MARGEN_X = 45;
    private static final int MARGEN_Y = 138;

    private final ArbolAVL arbol;
    private final Map<NodoAVL, Point> posiciones = new IdentityHashMap<>();
    private int columna;

    public PanelArbolAVL(ArbolAVL arbol) {
        this.arbol = arbol;
        setBackground(Tema.panel());
    }

    @Override
    public Dimension getPreferredSize() {
        int ancho = Math.max(720, MARGEN_X * 2 + Math.max(1, arbol.getTamano()) * SEPARACION_X);
        int alto = Math.max(420, MARGEN_Y + (arbol.getAlturaArbol() + 2) * SEPARACION_Y);
        return new Dimension(ancho, alto);
    }

    /** Pasada 1: recorrido inorden que asigna una columna a cada nodo. */
    private void calcularPosiciones(NodoAVL nodo, int profundidad) {
        if (nodo == null) {
            return;
        }
        calcularPosiciones(nodo.getIzquierdo(), profundidad + 1);
        posiciones.put(nodo, new Point(MARGEN_X + columna * SEPARACION_X,
                                       MARGEN_Y + profundidad * SEPARACION_Y));
        columna++;
        calcularPosiciones(nodo.getDerecho(), profundidad + 1);
    }

    /** Pasada 2a: aristas. */
    private void dibujarAristas(Graphics2D g2, NodoAVL nodo) {
        if (nodo == null) {
            return;
        }
        Point p = posiciones.get(nodo);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(Tema.borde());
        for (NodoAVL hijo : new NodoAVL[]{nodo.getIzquierdo(), nodo.getDerecho()}) {
            if (hijo != null) {
                Point q = posiciones.get(hijo);
                g2.drawLine(p.x, p.y, q.x, q.y);
            }
        }
        dibujarAristas(g2, nodo.getIzquierdo());
        dibujarAristas(g2, nodo.getDerecho());
    }

    /** Pasada 2b: nodos. */
    private void dibujarNodos(Graphics2D g2, NodoAVL nodo) {
        if (nodo == null) {
            return;
        }
        Point p = posiciones.get(nodo);
        int fe = arbol.factorEquilibrio(nodo);

        g2.setColor(Tema.nodo());
        g2.fillOval(p.x - RADIO, p.y - RADIO, RADIO * 2, RADIO * 2);

        g2.setColor(fe == 0 ? Tema.acento() : (Math.abs(fe) == 1 ? Tema.positivo() : Tema.negativo()));
        g2.setStroke(new BasicStroke(Math.abs(fe) >= 2 ? 4f : 2f));
        g2.drawOval(p.x - RADIO, p.y - RADIO, RADIO * 2, RADIO * 2);

        g2.setColor(Tema.texto());
        g2.setFont(Tema.normal());
        String etiqueta = String.valueOf(nodo.getPublicacion().getCredibilidad());
        g2.drawString(etiqueta, p.x - g2.getFontMetrics().stringWidth(etiqueta) / 2, p.y + 5);

        g2.setFont(Tema.mono());
        g2.setColor(Tema.textoSuave());
        String fx = "fe=" + (fe > 0 ? "+" : "") + fe;
        g2.drawString(fx, p.x - g2.getFontMetrics().stringWidth(fx) / 2, p.y + RADIO + 15);

        String id = "#" + nodo.getPublicacion().getId();
        g2.drawString(id, p.x - g2.getFontMetrics().stringWidth(id) / 2, p.y - RADIO - 6);

        dibujarNodos(g2, nodo.getIzquierdo());
        dibujarNodos(g2, nodo.getDerecho());
    }

    @Override
    protected void paintComponent(Graphics g) {
        setBackground(Tema.panel());
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Tema.texto());
        g2.setFont(Tema.subtitulo());
        g2.drawString("Feed de Civitas ordenado por credibilidad (arbol AVL)", 20, 28);

        g2.setFont(Tema.normal());
        g2.setColor(Tema.textoSuave());
        g2.drawString("Publicaciones: " + arbol.getTamano()
                + "     Altura: " + arbol.getAlturaArbol()
                + "     Rotaciones aplicadas: " + arbol.getRotacionesRealizadas()
                + "     Invariante AVL: " + (arbol.estaBalanceado() ? "se cumple" : "ROTA"), 20, 50);
        g2.drawString("Dentro del nodo: credibilidad.  Arriba: id de la publicacion.  Abajo: factor de equilibrio.", 20, 70);
        g2.drawString("Borde: fe = 0 perfectamente balanceado | fe = +-1 aceptable | fe = +-2 requiere rotacion.", 20, 88);

        if (arbol.estaVacio()) {
            g2.drawString("El feed esta vacio.", 20, 100);
            return;
        }

        posiciones.clear();
        columna = 0;
        calcularPosiciones(arbol.getRaiz(), 0);
        dibujarAristas(g2, arbol.getRaiz());
        dibujarNodos(g2, arbol.getRaiz());
    }
}
