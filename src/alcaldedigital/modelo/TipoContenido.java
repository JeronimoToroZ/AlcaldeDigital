package alcaldedigital.modelo;

/**
 * Naturaleza real de una publicacion. El jugador NO la conoce hasta que verifica
 * o hasta que se resuelve el turno: por eso el arbol de decision desciende primero
 * por la accion del jugador y despues por este tipo.
 */
public enum TipoContenido {
    VERDADERA("Informacion verdadera"),
    FALSA("Informacion falsa"),
    OPINION("Opinion personal");

    private final String etiqueta;

    TipoContenido(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
