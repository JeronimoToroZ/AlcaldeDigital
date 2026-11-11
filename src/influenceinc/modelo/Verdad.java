package influenceinc.modelo;

/** Lo que el caso ES en realidad (el jugador lo tiene que descubrir). */
public enum Verdad {
    VERDADERA("Información verdadera"),
    FALSA("Información falsa"),
    OPINION("Opinión personal"),
    PUBLICIDAD_OCULTA("Publicidad sin declarar"),
    IMPOSTOR("Impostor / campaña de desinformación");

    public final String etiqueta;

    Verdad(String etiqueta) {
        this.etiqueta = etiqueta;
    }
}
