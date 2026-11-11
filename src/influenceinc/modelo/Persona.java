package influenceinc.modelo;

/** El visitante de la ventanilla. */
public class Persona {

    public final String nombre;
    public final RolVisitante rol;
    public final Rasgos cara;
    public Credencial credencial;
    /** Impostor: su cara no coincide con la credencial y a veces "falla" (parpadeo raro). */
    public boolean impostor;
    public String diferencia;

    public Persona(String nombre, RolVisitante rol, Rasgos cara) {
        this.nombre = nombre;
        this.rol = rol;
        this.cara = cara;
    }
}
