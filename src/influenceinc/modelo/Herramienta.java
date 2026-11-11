package influenceinc.modelo;

/** Herramientas de investigacion. Se desbloquean dia a dia. */
public enum Herramienta {
    PERFIL("Escáner de perfil", 1),
    DIRECTORIO("Directorio oficial", 2),
    LUPA("Lupa de credenciales", 2),
    IMAGEN("Búsqueda inversa", 3),
    CORRESPONSAL("Corresponsal", 3),
    RED("Escáner de red", 4);

    public final String nombre;
    public final int dia;

    Herramienta(String nombre, int dia) {
        this.nombre = nombre;
        this.dia = dia;
    }
}
