package influenceinc.modelo;

/**
 * Las senales que puede tener una publicacion sospechosa.
 *
 * Son las pistas que el jugador tiene que aprender a ver. Cada una existe en la
 * vida real y se puede revisar sin ser experto: mirar cuando se creo la cuenta,
 * mirar si cita una fuente, mirar de donde salio la foto.
 *
 * MUY IMPORTANTE: una senal NO es una prueba. Hay publicaciones ciertas con
 * titular alarmista y publicaciones falsas desde cuentas viejas. Por eso el juego
 * tiene casos de los dos tipos: si todas las falsas tuvieran todas las senales,
 * el jugador aprenderia una regla mecanica en vez de a leer evidencia.
 */
public enum Senal {

    CUENTA_RECIEN_CREADA(
            "Cuenta creada hace pocos días",
            "Las cuentas hechas para difundir un bulo suelen ser nuevas. Mira la fecha de creación."),

    SIN_SEGUIDORES(
            "Casi sin seguidores",
            "Una cuenta con doce seguidores que publica una alerta para toda la ciudad no cuadra."),

    TITULAR_ALARMISTA(
            "Titular alarmista",
            "Mayúsculas, signos de admiración y urgencia buscan que compartas antes de pensar."),

    SIN_FUENTE(
            "No cita ninguna fuente",
            "Si no dice de dónde salió el dato, no hay nada que verificar. Eso ya es una respuesta."),

    FUENTE_ES_OTRA_RED(
            "La fuente es otra red social",
            "Un grupo de WhatsApp o una captura de pantalla no son una fuente: son otro rumor."),

    FOTO_FUERA_DE_CONTEXTO(
            "La foto es real pero es de otro lado",
            "La foto existe, pero es de otro año u otra ciudad. Una búsqueda inversa de imagen lo delata."),

    TESTIMONIO_DE_TERCERO(
            "\"Me lo contó alguien que sabe\"",
            "Un primo, una cuñada, un amigo que trabaja ahí. Nadie que se pueda confirmar."),

    URGENCIA_A_COMPARTIR(
            "Pide que lo reenvíes ya",
            "\"Compartan antes de que lo borren\" existe para saltarse el momento en que lo verificarías.");

    private final String etiqueta;
    private final String explicacion;

    Senal(String etiqueta, String explicacion) {
        this.etiqueta = etiqueta;
        this.explicacion = explicacion;
    }

    /** Texto corto, para mostrarlo junto a la publicacion. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Por que esa senal importa. Se muestra en el informe final. */
    public String getExplicacion() {
        return explicacion;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
