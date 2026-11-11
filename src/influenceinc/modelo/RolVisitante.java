package influenceinc.modelo;

/** Quien llega a la ventanilla (los roles del enunciado y algunos mas). */
public enum RolVisitante {
    CIUDADANO("Ciudadano"),
    FUNCIONARIO("Funcionario"),
    INFLUENCER("Influencer"),
    PERIODISTA("Periodista"),
    CAMPANA("Equipo de campaña"),
    AGENCIA("Agencia rival"),
    JEFE("Tu jefe");

    public final String etiqueta;

    RolVisitante(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
