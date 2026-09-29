package alcaldedigital.modelo;

/**
 * Roles jugables de la partida. Cada rol aplica multiplicadores distintos sobre el efecto que
 * devuelve el árbol de decisión, de modo que la misma acción no vale lo mismo
 * para todos los jugadores.
 *
 * - multiplicadorCiudad: cuánto pesa su acción sobre los indicadores de la ciudad.
 * - multiplicadorReputacion: cuánto gana o pierde el jugador en reputación.
 * - bonificacionVerificar: refuerzo extra cuando la acción elegida es VERIFICAR.
 */
public enum Rol {
    CIUDADANO("Ciudadano", 1.0, 1.0, 1.0,
            "Interactúa de forma responsable con lo que recibe."),
    PERIODISTA("Periodista", 1.0, 1.2, 1.8,
            "Verificar le cuesta menos y le rinde mucho más."),
    INFLUENCER("Influencer", 2.0, 1.0, 1.0,
            "Todo lo que toca se amplifica: para bien y para mal."),
    CANDIDATO("Candidato a alcalde", 1.2, 1.8, 1.0,
            "Su reputación es su campaña. Gana o pierde el doble.");

    private final String etiqueta;
    private final double multiplicadorCiudad;
    private final double multiplicadorReputacion;
    private final double bonificacionVerificar;
    private final String descripcion;

    Rol(String etiqueta, double mc, double mr, double bv, String descripcion) {
        this.etiqueta = etiqueta;
        this.multiplicadorCiudad = mc;
        this.multiplicadorReputacion = mr;
        this.bonificacionVerificar = bv;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public double getMultiplicadorCiudad() {
        return multiplicadorCiudad;
    }

    public double getMultiplicadorReputacion() {
        return multiplicadorReputacion;
    }

    public double getBonificacionVerificar() {
        return bonificacionVerificar;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Se muestra así en los combos de la interfaz. */
    @Override
    public String toString() {
        return etiqueta;
    }
}