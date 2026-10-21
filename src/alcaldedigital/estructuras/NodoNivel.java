package alcaldedigital.estructuras;

/**
 * Vértice del grafo de niveles: un nivel de la campaña.
 * Guarda su número, la imagen que lo representa (n1.png ... n7.png),
 * su posición en el mapa (como fracción del ancho/alto de la pantalla)
 * y el progreso del jugador en él.
 */
public class NodoNivel {

    private final int numero;
    private final String imagen;
    private final double x;   // 0.0 .. 1.0 del ancho del mapa
    private final double y;   // 0.0 .. 1.0 del alto del mapa

    private boolean completado;
    private int estrellas;      // 0..3 (3 = sin perder vidas)
    private int mejorPuntaje;

    public NodoNivel(int numero, String imagen, double x, double y) {
        this.numero = numero;
        this.imagen = imagen;
        this.x = x;
        this.y = y;
    }

    /** Registra una victoria; conserva el mejor resultado si el nivel se repite. */
    public void registrarVictoria(int estrellasObtenidas, int puntaje) {
        completado = true;
        estrellas = Math.max(estrellas, estrellasObtenidas);
        mejorPuntaje = Math.max(mejorPuntaje, puntaje);
    }

    public int getNumero() { return numero; }
    public String getImagen() { return imagen; }
    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isCompletado() { return completado; }
    public int getEstrellas() { return estrellas; }
    public int getMejorPuntaje() { return mejorPuntaje; }

    @Override
    public String toString() {
        return "N" + numero;
    }
}
