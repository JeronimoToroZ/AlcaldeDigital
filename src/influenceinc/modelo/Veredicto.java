package influenceinc.modelo;

/** Los sellos del escritorio: lo que decide el verificador sobre cada caso. */
public enum Veredicto {
    PUBLICAR("PUBLICAR", "Sale con el sello de la agencia"),
    OPINION("OPINIÓN", "Se publica marcada como opinión"),
    ETIQUETA("#PUBLICIDAD", "Se publica marcada como contenido pagado"),
    RECHAZAR("RECHAZAR", "No se publica"),
    DENUNCIAR("DENUNCIAR", "No se publica y se denuncia la cuenta como falsa");

    public final String sello;
    public final String descripcion;

    Veredicto(String sello, String descripcion) {
        this.sello = sello;
        this.descripcion = descripcion;
    }
}
