package alcaldedigital.gui;
 
import alcaldedigital.estructuras.ArbolAVL;
import alcaldedigital.estructuras.NodoAVL;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.swing.JPanel;
 
/**
 * Panel gráfico para la visualización en tiempo real del Árbol AVL.
 * Dibuja los nodos activos del feed evitando solapamientos mediante un
 * posicionamiento basado en recorrido Inorden (columnas) y profundidad (filas).
 *
 * @author Naty
 */
public class PanelArbolAVL extends JPanel {
 
    private static final int RADIO = 22;
    private static final int SEPARACION_X = 64;
    private static final int SEPARACION_Y = 84;
    private static final int MARGEN_X = 45;
    private static final int MARGEN_Y = 150;
 
    private final ArbolAVL arbol;
    private final Map<NodoAVL, Point> posiciones = new IdentityHashMap<>();
    private int columna;
 
    /**
     * Inicializa el panel configurando el árbol a renderizar y el color de fondo.
     */
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
 
    /**
     * Pasada 1: Recorrido Inorden que asigna una columna única a cada nodo para evitar solapamientos.
     */
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
 
    /**
     * Pasada 2a: Dibuja las aristas o líneas que conectan a los nodos con sus hijos.
     */
    private void dibujarAristas(Graphics2D g2, NodoAVL nodo) {
        if (nodo == null) {
            return;
        }
        Point p = posiciones.get(nodo);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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
 
    /**
     * Pasada 2b: Dibuja los círculos de los nodos, sus identificadores,
     * los niveles de credibilidad y el factor de equilibrio (FE).
     */
    private void dibujarNodos(Graphics2D g2, NodoAVL nodo) {
        if (nodo == null) {
            return;
        }
        Point p = posiciones.get(nodo);
        int fe = arbol.factorEquilibrio(nodo);
 
        // Sombra suave (solo en modo normal)
        if (!Tema.isAltoContraste()) {
            g2.setColor(Tema.borde());
            g2.fillOval(p.x - RADIO + 3, p.y - RADIO + 3, RADIO * 2, RADIO * 2);
        }
 
        // Relleno del nodo
        g2.setColor(Tema.nodo());
        g2.fillOval(p.x - RADIO, p.y - RADIO, RADIO * 2, RADIO * 2);
 
        // Borde según su estado de equilibrio
        g2.setColor(fe == 0 ? Tema.acento() : (Math.abs(fe) == 1 ? Tema.positivo() : Tema.negativo()));
        g2.setStroke(new BasicStroke(Math.abs(fe) >= 2 ? 5f : 3f));
        g2.drawOval(p.x - RADIO, p.y - RADIO, RADIO * 2, RADIO * 2);
 
        // Credibilidad dentro del nodo
        g2.setColor(Tema.texto());
        g2.setFont(Tema.indicadorNegrita());
        FontMetrics fm = g2.getFontMetrics();
        String etiqueta = String.valueOf(nodo.getPublicacion().getCredibilidad());
        g2.drawString(etiqueta, p.x - fm.stringWidth(etiqueta) / 2, p.y + fm.getAscent() / 2 - 1);
 
        // Factor de equilibrio e id alrededor del nodo
        g2.setFont(Tema.mono());
        fm = g2.getFontMetrics();
        g2.setColor(Tema.textoSuave());
        String fx = "fe=" + (fe > 0 ? "+" : "") + fe;
        g2.drawString(fx, p.x - fm.stringWidth(fx) / 2, p.y + RADIO + 17);
 
        String id = "#" + nodo.getPublicacion().getId();
        g2.drawString(id, p.x - fm.stringWidth(id) / 2, p.y - RADIO - 7);
 
        dibujarNodos(g2, nodo.getIzquierdo());
        dibujarNodos(g2, nodo.getDerecho());
    }
 
    /** Dibuja un punto de color seguido de su descripción; devuelve la x donde termina. */
    private int leyenda(Graphics2D g2, int x, int y, Color color, String texto) {
        g2.setColor(color);
        g2.fillOval(x, y - 9, 10, 10);
        g2.setColor(Tema.textoSuave());
        g2.setFont(Tema.indicador());
        g2.drawString(texto, x + 16, y);
        return x + 16 + g2.getFontMetrics().stringWidth(texto) + 26;
    }
 
    /**
     * Renderiza el componente gráfico completo, incluyendo encabezados informativos,
     * estadísticas del árbol y las dos pasadas de dibujo estructural.
     */
    @Override
    protected void paintComponent(Graphics g) {
        setBackground(Tema.panel());
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Tema.suavizar(g2);
 
        // Encabezado
        g2.setColor(Tema.texto());
        g2.setFont(Tema.subtitulo());
        g2.drawString("Feed de Civitas ordenado por credibilidad (árbol AVL)", 20, 30);
 
        g2.setFont(Tema.normal());
        g2.setColor(Tema.acento());
        g2.drawString("Publicaciones: " + arbol.getTamano()
                + "     Altura: " + arbol.getAlturaArbol()
                + "     Rotaciones: " + arbol.getRotacionesRealizadas()
                + "     Invariante AVL: " + (arbol.estaBalanceado() ? "se cumple" : "ROTA"), 20, 56);
 
        g2.setFont(Tema.indicador());
        g2.setColor(Tema.textoSuave());
        g2.drawString("Dentro del nodo: credibilidad   ·   Arriba: id de la publicación   ·   Abajo: factor de equilibrio (fe)", 20, 82);
 
        // Leyenda del borde
        int x = 20;
        x = leyenda(g2, x, 108, Tema.acento(), "fe = 0  perfectamente balanceado");
        x = leyenda(g2, x, 108, Tema.positivo(), "fe = ±1  aceptable");
        leyenda(g2, x, 108, Tema.negativo(), "fe = ±2  requiere rotación");
 
        if (arbol.estaVacio()) {
            g2.setFont(Tema.normal());
            g2.setColor(Tema.textoSuave());
            g2.drawString("El feed está vacío.", 20, 150);
            return;
        }
 
        // Cálculo de coordenadas y renderizado por capas
        posiciones.clear();
        columna = 0;
        calcularPosiciones(arbol.getRaiz(), 0);
        dibujarAristas(g2, arbol.getRaiz());
        dibujarNodos(g2, arbol.getRaiz());
    }
}