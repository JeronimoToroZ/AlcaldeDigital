package alcaldedigital.modelo;

/**
 * Roles jugables. Cada rol aplica multiplicadores distintos sobre el efecto que
 * devuelve el arbol de decision, de modo que la misma accion no vale lo mismo
 * para todos los jugadores.
 *
 * multiplicadorCiudad  -> cuanto pesa su accion sobre los indicadores de la ciudad.
 * multiplicadorReputacion -> cuanto gana o pierde el jugador en reputacion.
 * bonificacionVerificar -> refuerzo extra cuando la accion elegida es VERIFICAR.
 */
public enum Rol {
    CIUDADANO("Ciudadano", 1.0, 1.0, 1.0,
            "Interactua de forma responsable con lo que recibe."),
    PERIODISTA("Periodista", 1.0, 1.2, 1.8,
            "Verificar le cuesta menos y le rinde mucho mas."),
    INFLUENCER("Influencer", 2.0, 1.0, 1.0,
            "Todo lo que toca se amplifica: para bien y para mal."),
    CANDIDATO("Candidato a alcalde", 1.2, 1.8, 1.0,
            "Su reputacion es su campana. Gana o pierde el doble.");

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

    /** Se muestra asi en los combos de la interfaz. */
    @Override
    public String toString() {
        return etiqueta;
    }
}
