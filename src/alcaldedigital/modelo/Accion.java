package alcaldedigital.modelo;

/**
 * Las cuatro decisiones que un jugador puede tomar frente a una publicacion.
 * Cada constante es, literalmente, una rama de primer nivel del arbol de decision.
 */
public enum Accion {
    COMPARTIR("Compartir"),
    VERIFICAR("Verificar"),
    IGNORAR("Ignorar"),
    REPORTAR("Reportar");

    private final String etiqueta;

    Accion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Retorna la etiqueta descriptiva legible de la accion.
     */
    public String getEtiqueta() {
        return etiqueta;
    }
}