package influenceinc.modelo;

/** Que clase de cuenta es cada vertice del grafo social. */
public enum TipoCuenta {
    AGENCIA("Agencia"),
    INFLUENCER("Influencer"),
    USUARIO("Usuario"),
    BOT("Cuenta falsa"),
    MEDIO("Medio verificador");

    private final String etiqueta;

    TipoCuenta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
