package alcaldedigital.juego;

/**
 * Un bloque sobre el que se puede pararse.
 *  - SUELO: bloque sólido que llega hasta abajo (entre huecos). No se atraviesa por los lados.
 *  - FLOTANTE: plataforma "de un solo sentido": se atraviesa saltando desde abajo
 *    y solo frena al personaje cuando cae desde arriba (como en Mario).
 *  - MOVIL: flotante que se desplaza de lado a lado y arrastra al personaje.
 */
public class Plataforma {

    public enum Tipo { SUELO, FLOTANTE, MOVIL }

    public final Tipo tipo;
    public double x;
    public final double y;      // borde superior
    public final double ancho;
    public final double alto;

    // solo para MOVIL
    private final double xBase;
    private final double amplitud;
    private final double velocidad;
    private final double fase;
    private double desplazamiento;   // cuánto se movió en el último tick

    public Plataforma(Tipo tipo, double x, double y, double ancho, double alto) {
        this(tipo, x, y, ancho, alto, 0, 0, 0);
    }

    public Plataforma(Tipo tipo, double x, double y, double ancho, double alto,
                      double amplitud, double velocidad, double fase) {
        this.tipo = tipo;
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
        this.xBase = x;
        this.amplitud = amplitud;
        this.velocidad = velocidad;
        this.fase = fase;
    }

    /** Mueve la plataforma (solo si es móvil) y recuerda cuánto se desplazó. */
    public void actualizar(long tick) {
        if (tipo != Tipo.MOVIL) {
            desplazamiento = 0;
            return;
        }
        double nuevaX = xBase + Math.sin(tick * velocidad + fase) * amplitud;
        desplazamiento = nuevaX - x;
        x = nuevaX;
    }

    public double getDesplazamiento() {
        return desplazamiento;
    }

    public double derecha() {
        return x + ancho;
    }
}
