package influenceinc.modelo;

/** Registro del DIRECTORIO OFICIAL (se guarda en un AVL por @usuario). */
public class CuentaOficial {

    public final String arroba, institucion, representante, cargo, credencial, telefono;
    public final int diasCreada;

    public CuentaOficial(String arroba, String institucion, int diasCreada, String representante,
                         String cargo, String credencial, String telefono) {
        this.arroba = arroba;
        this.institucion = institucion;
        this.diasCreada = diasCreada;
        this.representante = representante;
        this.cargo = cargo;
        this.credencial = credencial;
        this.telefono = telefono;
    }
}
