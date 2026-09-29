package alcaldedigital.modelo;

/** Un jugador de la partida: su rol, su puntaje y su reputacion. */
public class Jugador {

    private final String nombre;
    private final Rol rol;
    private int puntos;
    private int reputacion;
    private int aciertos;
    private int errores;

    /**
     * Constructor para inicializar al jugador con su nombre y rol asignado.
     * La reputacion inicial se establece en 50.
     */
    public Jugador(String nombre, Rol rol) {
        this.nombre = nombre;
        this.rol = rol;
        this.puntos = 0;
        this.reputacion = 50;
        this.aciertos = 0;
        this.errores = 0;
    }

    /**
     * Suma o resta puntos al jugador y registra automaticamente un acierto o error
     * dependiendo del signo de los puntos recibidos.
     */
    public void sumarPuntos(int p) {
        this.puntos += p;
        if (p > 0) {
            aciertos++;
        } else if (p < 0) {
            errores++;
        }
    }

    /**
     * Modifica la reputacion del jugador asegurando que se mantenga dentro del rango de 0 a 100.
     */
    public void sumarReputacion(int r) {
        this.reputacion = Math.max(0, Math.min(100, this.reputacion + r));
    }

    /**
     * Retorna el nombre del jugador.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Retorna el rol asignado al jugador.
     */
    public Rol getRol() {
        return rol;
    }

    /**
     * Retorna los puntos acumulados del jugador.
     */
    public int getPuntos() {
        return puntos;
    }

    /**
     * Retorna el nivel actual de reputación del jugador.
     */
    public int getReputacion() {
        return reputacion;
    }

    /**
     * Retorna la cantidad de aciertos del jugador.
     */
    public int getAciertos() {
        return aciertos;
    }

    /**
     * Retorna la cantidad de errores del jugador.
     */
    public int getErrores() {
        return errores;
    }

    @Override
    public String toString() {
        return nombre + " (" + rol.getEtiqueta() + ")";
    }
}