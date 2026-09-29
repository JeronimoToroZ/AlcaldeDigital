package alcaldedigital.modelo;

/**
 * Los seis indicadores de Ciudad Nova. Todos viven en el rango 0..100.
 * Cuatro son "positivos" (hay que subirlos) y dos son "negativos" (hay que bajarlos).
 */
public class EstadoCiudad {

    private int informacionVerificada = 82;
    private int confianza = 74;
    private int convivencia = 88;
    private int bienestarDigital = 79;
    private int desinformacion = 21;
    private int conflictos = 14;

    private static int limitar(int valor) {
        return Math.max(0, Math.min(100, valor));
    }

    /**
     * Aplica los deltas correspondientes sobre cada uno de los indicadores de la ciudad,
     * asegurando que se mantengan dentro del rango válido de 0 a 100.
     */
    public void aplicar(int dInfo, int dConf, int dConv, int dBien, int dDesinfo, int dConflictos) {
        informacionVerificada = limitar(informacionVerificada + dInfo);
        confianza = limitar(confianza + dConf);
        convivencia = limitar(convivencia + dConv);
        bienestarDigital = limitar(bienestarDigital + dBien);
        desinformacion = limitar(desinformacion + dDesinfo);
        conflictos = limitar(conflictos + dConflictos);
    }

    /**
     * Salud global de la ciudad: promedio de los positivos menos promedio de los
     * negativos, normalizado a 0..100. Es el numero que decide la eleccion final.
     */
    public int saludGlobal() {
        double positivos = (informacionVerificada + confianza + convivencia + bienestarDigital) / 4.0;
        double negativos = (desinformacion + conflictos) / 2.0;
        return limitar((int) Math.round(positivos - negativos));
    }

    /**
     * Retorna el nivel actual de información verificada.
     */
    public int getInformacionVerificada() {
        return informacionVerificada;
    }

    /**
     * Retorna el nivel actual de confianza.
     */
    public int getConfianza() {
        return confianza;
    }

    /**
     * Retorna el nivel actual de convivencia.
     */
    public int getConvivencia() {
        return convivencia;
    }

    /**
     * Retorna el nivel actual de bienestar digital.
     */
    public int getBienestarDigital() {
        return bienestarDigital;
    }

    /**
     * Retorna el nivel actual de desinformación.
     */
    public int getDesinformacion() {
        return desinformacion;
    }

    /**
     * Retorna el nivel actual de conflictos.
     */
    public int getConflictos() {
        return conflictos;
    }
}