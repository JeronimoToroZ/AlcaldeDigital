package alcaldedigital.modelo;

/**
 * Naturaleza real de una publicación. El jugador NO la conoce: la tiene que
 * deducir leyendo el comentario. El árbol de decisión desciende primero por la
 * acción del jugador (la burbuja que recolectó) y después por este tipo.
 *
 * Hay cuatro tipos, uno por cada acción responsable:
 *   VERDADERA      -> lo ideal es COMPARTIR (información útil y confiable).
 *   FALSA          -> lo ideal es REPORTAR  (acusación o noticia falsa).
 *   OPINION        -> lo ideal es IGNORAR   (no alimentar la discusión).
 *   SIN_CONFIRMAR  -> lo ideal es VERIFICAR (dice ser oficial pero no tiene fuente).
 * Esa relación NO está escrita a mano: la calcula el árbol con accionIdeal().
 */
public enum TipoContenido {
    VERDADERA("Información verdadera"),
    FALSA("Información falsa"),
    OPINION("Opinión personal"),
    SIN_CONFIRMAR("Información sin confirmar");

    private final String etiqueta;

    TipoContenido(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
