package alcaldedigital.juego;

import alcaldedigital.modelo.Accion;

/**
 * Una burbuja recolectable con uno de los cuatro símbolos (Compartir,
 * Verificar, Ignorar o Reportar). Flota en su sitio y, desde el nivel 3,
 * también se mueve de lado a lado.
 */
public class Burbuja {

    public static final double RADIO = 22;

    public final double xBase;
    public final double yBase;
    public Accion accion;
    public boolean viva = true;
    public int ticksParaReaparecer;
    /** Ticks desde que apareció (para animar su entrada). */
    public int edad;
    /** Ticks en los que no se puede recolectar (justo después de cambiar de símbolo). */
    public int gracia;
    private final double fase;

    public Burbuja(double xBase, double yBase, Accion accion, double fase) {
        this.xBase = xBase;
        this.yBase = yBase;
        this.accion = accion;
        this.fase = fase;
    }

    /** Posición actual: flota arriba-abajo y se mueve de lado según la amplitud del nivel. */
    public double x(long tick, int amplitud) {
        return xBase + (amplitud == 0 ? 0 : Math.sin(tick * 0.025 + fase) * amplitud);
    }

    public double y(long tick) {
        return yBase + Math.sin(tick * 0.06 + fase * 2) * 5;
    }

    /** Choque círculo (burbuja) contra rectángulo (personaje). */
    public boolean tocaA(Personaje p, long tick, int amplitud) {
        double cx = x(tick, amplitud);
        double cy = y(tick);
        double masCercanoX = Math.max(p.x, Math.min(cx, p.x + Personaje.ANCHO));
        double masCercanoY = Math.max(p.y, Math.min(cy, p.y + Personaje.ALTO));
        double dx = cx - masCercanoX;
        double dy = cy - masCercanoY;
        return dx * dx + dy * dy <= RADIO * RADIO;
    }
}
