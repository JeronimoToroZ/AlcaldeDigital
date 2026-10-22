package alcaldedigital.logica;

import alcaldedigital.modelo.Jugador;

/**
 * Lo que hizo UN jugador en UNA ronda (su turno en un nivel).
 * Con esto se arma la tabla de puntuaciones al final de las rondas.
 */
public class ResultadoRonda {

    public final Jugador jugador;
    public final boolean supero;    // resolvió las 4 publicaciones
    public final int vidas;
    public final double segundos;   // lo que tardó (sin pausas)
    public final int puntos;        // puntos ganados en la ronda (incluye bono por tiempo y vidas)
    public final int bonoTiempo;

    public ResultadoRonda(Jugador jugador, boolean supero, int vidas, double segundos, int puntos, int bonoTiempo) {
        this.jugador = jugador;
        this.supero = supero;
        this.vidas = vidas;
        this.segundos = segundos;
        this.puntos = puntos;
        this.bonoTiempo = bonoTiempo;
    }

    /**
     * Orden de la tabla: más puntos primero; si empatan, gana quien tardó menos.
     * Quien superó el nivel siempre queda por encima de quien no.
     */
    public static int comparar(ResultadoRonda a, ResultadoRonda b) {
        if (a.supero != b.supero) {
            return a.supero ? -1 : 1;
        }
        if (a.puntos != b.puntos) {
            return Integer.compare(b.puntos, a.puntos);
        }
        return Double.compare(a.segundos, b.segundos);
    }
}
