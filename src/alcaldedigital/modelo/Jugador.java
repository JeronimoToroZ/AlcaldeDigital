package alcaldedigital.modelo;

/** Un jugador de la partida: su rol, su puntaje y su reputacion. */
public class Jugador {

    private final String nombre;
    private final Rol rol;
    private int puntos;
    private int reputacion;
    private int aciertos;
    private int errores;

    public Jugador(String nombre, Rol rol) {
        this.nombre = nombre;
        this.rol = rol;
        this.puntos = 0;
        this.reputacion = 50;
        this.aciertos = 0;
        this.errores = 0;
    }

    public void sumarPuntos(int p) {
        this.puntos += p;
        if (p > 0) {
            aciertos++;
        } else if (p < 0) {
            errores++;
        }
    }

    public void sumarReputacion(int r) {
        this.reputacion = Math.max(0, Math.min(100, this.reputacion + r));
    }

    public String getNombre() {
        return nombre;
    }

    public Rol getRol() {
        return rol;
    }

    public int getPuntos() {
        return puntos;
    }

    public int getReputacion() {
        return reputacion;
    }

    public int getAciertos() {
        return aciertos;
    }

    public int getErrores() {
        return errores;
    }

    @Override
    public String toString() {
        return nombre + " (" + rol.getEtiqueta() + ")";
    }
}
