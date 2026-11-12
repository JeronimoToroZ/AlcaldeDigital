package influenceinc.modelo;

/** Los sellos del escritorio: lo que decide el verificador sobre cada caso. */
public enum Veredicto {
    PUBLICAR("VERIFICADO", "Se publica con el sello de verificado"),
    OPINION("OPINIÓN", "Se publica marcada como opinión"),
    ETIQUETA("#PUBLICIDAD", "Se publica marcada como contenido pagado"),
    RECHAZAR("RECHAZAR", "No se publica"),
    DENUNCIAR("REPORTAR", "No se publica y se reporta la cuenta como falsa");

    public final String sello;
    public final String descripcion;

    /** Nombre del archivo de imagen del sello (carpeta recursos). */
    public String imagen() {
        switch (this) {
            case PUBLICAR: return "verificarr.png";
            case OPINION: return "opinion.png";
            case ETIQUETA: return "publicidad.png";
            case RECHAZAR: return "rechazar.png";
            default: return "reportarr.png";
        }
    }

    Veredicto(String sello, String descripcion) {
        this.sello = sello;
        this.descripcion = descripcion;
    }
}
