package alcaldedigital.modelo;

/**
 * Naturaleza real de una publicación. El jugador NO la conoce hasta que verifica
 * o hasta que se resuelve el turno: por eso el árbol de decisión desciende primero
 * por la acción del jugador y después por este tipo.
 */
public enum TipoContenido {
    VERDADERA("Información verdadera"),
    FALSA("Información falsa"),
    OPINION("Opinión personal");

    private final String etiqueta;

    TipoContenido(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}