package alcaldedigital.logica;

import alcaldedigital.modelo.EstadoCiudad;
import alcaldedigital.modelo.Rol;

/**
 * Consecuencia concreta de una decision. Es lo que guardan las HOJAS del arbol
 * de decision: seis deltas sobre la ciudad, puntos, reputacion y el mensaje de
 * retroalimentacion que ve el jugador.
 */
public class Efecto {

    private final int dInfoVerificada;
    private final int dConfianza;
    private final int dConvivencia;
    private final int dBienestar;
    private final int dDesinformacion;
    private final int dConflictos;
    private final int puntos;
    private final int reputacion;
    private final String mensaje;
    private final boolean retirarPublicacion;

    public Efecto(int dInfoVerificada, int dConfianza, int dConvivencia, int dBienestar,
                  int dDesinformacion, int dConflictos, int puntos, int reputacion,
                  String mensaje, boolean retirarPublicacion) {
        this.dInfoVerificada = dInfoVerificada;
        this.dConfianza = dConfianza;
        this.dConvivencia = dConvivencia;
        this.dBienestar = dBienestar;
        this.dDesinformacion = dDesinformacion;
        this.dConflictos = dConflictos;
        this.puntos = puntos;
        this.reputacion = reputacion;
        this.mensaje = mensaje;
        this.retirarPublicacion = retirarPublicacion;
    }

    private static int escalar(int valor, double factor) {
        return (int) Math.round(valor * factor);
    }

    /**
     * Aplica el efecto sobre la ciudad escalandolo segun el rol del jugador.
     * Aqui es donde el Influencer amplifica y el Periodista saca provecho de verificar.
     */
    public void aplicarSobre(EstadoCiudad ciudad, Rol rol, boolean fueVerificacion) {
        double f = rol.getMultiplicadorCiudad();
        if (fueVerificacion) {
            f *= rol.getBonificacionVerificar();
        }
        ciudad.aplicar(
                escalar(dInfoVerificada, f),
                escalar(dConfianza, f),
                escalar(dConvivencia, f),
                escalar(dBienestar, f),
                escalar(dDesinformacion, f),
                escalar(dConflictos, f));
    }

    public int puntosPara(Rol rol, boolean fueVerificacion) {
        double f = fueVerificacion ? rol.getBonificacionVerificar() : 1.0;
        return escalar(puntos, f);
    }

    public int reputacionPara(Rol rol) {
        return escalar(reputacion, rol.getMultiplicadorReputacion());
    }

    public String getMensaje() {
        return mensaje;
    }

    public boolean debeRetirarPublicacion() {
        return retirarPublicacion;
    }

    /** Resumen legible de los deltas, para el log de la partida. */
    public String resumenDeltas() {
        StringBuilder sb = new StringBuilder();
        agregar(sb, "Info verificada", dInfoVerificada);
        agregar(sb, "Confianza", dConfianza);
        agregar(sb, "Convivencia", dConvivencia);
        agregar(sb, "Bienestar", dBienestar);
        agregar(sb, "Desinformacion", dDesinformacion);
        agregar(sb, "Conflictos", dConflictos);
        return sb.length() == 0 ? "sin cambios en la ciudad" : sb.toString().trim();
    }

    private void agregar(StringBuilder sb, String nombre, int valor) {
        if (valor != 0) {
            sb.append(nombre).append(' ').append(valor > 0 ? "+" : "").append(valor).append("   ");
        }
    }
}
