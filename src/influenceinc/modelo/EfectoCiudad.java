package influenceinc.modelo;

/** Hoja del arbol de consecuencias: como cambia la ciudad y tu agencia. */
public class EfectoCiudad {

    public final int verificada, confianza, convivencia, bienestar, desinformacion, conflictos;
    public final int puntos, dinero, reputacion;
    public final boolean acierto;
    /** true si es un error grave (cuenta como advertencia del jefe). */
    public final boolean grave;
    public final String explicacion;

    public EfectoCiudad(int verificada, int confianza, int convivencia, int bienestar, int desinformacion,
                        int conflictos, int puntos, int dinero, int reputacion, boolean acierto, boolean grave,
                        String explicacion) {
        this.verificada = verificada;
        this.confianza = confianza;
        this.convivencia = convivencia;
        this.bienestar = bienestar;
        this.desinformacion = desinformacion;
        this.conflictos = conflictos;
        this.puntos = puntos;
        this.dinero = dinero;
        this.reputacion = reputacion;
        this.acierto = acierto;
        this.grave = grave;
        this.explicacion = explicacion;
    }

    public String resumen() {
        StringBuilder sb = new StringBuilder();
        add(sb, "Verif", verificada);
        add(sb, "Conf", confianza);
        add(sb, "Conv", convivencia);
        add(sb, "Bien", bienestar);
        add(sb, "Desinf", desinformacion);
        add(sb, "Confl", conflictos);
        sb.append(" | ").append(puntos >= 0 ? "+" : "").append(puntos).append(" pts");
        return sb.toString().trim();
    }

    private static void add(StringBuilder sb, String n, int v) {
        if (v != 0) {
            sb.append(n).append(v > 0 ? " +" : " ").append(v).append("  ");
        }
    }
}
