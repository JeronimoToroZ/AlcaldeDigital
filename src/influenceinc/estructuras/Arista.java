package influenceinc.estructuras;

/**
 * Arista DIRIGIDA y con DOS pesos.
 *
 *  - probabilidad (0..1): que tan probable es que lo que publica el origen le
 *    llegue al destino. La usa la propagacion (BFS probabilistico).
 *  - costo (entero positivo): lo que cuesta llevar una campana de verificacion
 *    por este tramo. La usa Dijkstra.
 *
 * Se separan porque responden preguntas distintas: "que tan facil se difunde
 * algo" no es lo mismo que "que tan caro es corregirlo".
 */
public class Arista {

    public final Vertice origen;
    public final Vertice destino;
    public double probabilidad;
    public int costo;

    public Arista(Vertice origen, Vertice destino, double probabilidad, int costo) {
        if (costo <= 0) {
            throw new IllegalArgumentException("Dijkstra necesita costos positivos");
        }
        this.origen = origen;
        this.destino = destino;
        this.probabilidad = Math.max(0, Math.min(1, probabilidad));
        this.costo = costo;
    }

    @Override
    public String toString() {
        return origen.nombre + " -> " + destino.nombre
                + String.format(" (p=%.2f, c=%d)", probabilidad, costo);
    }
}
