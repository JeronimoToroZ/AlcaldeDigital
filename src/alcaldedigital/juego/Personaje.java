package alcaldedigital.juego;

/**
 * El personaje del jugador y su física de plataformas.
 *
 * Valores elegidos a propósito (en píxeles por tick, a ~60 ticks por segundo):
 *   GRAVEDAD 0.65 y SALTO -13.2  -> el salto sube unos 134 px.
 *   VELOCIDAD 4.6                -> en un salto avanza unos 185 px.
 * El generador del mapa usa esas medidas para que todo sea alcanzable.
 *
 * Trucos de "buen control" que usan los juegos de plataformas:
 *   - tiempo coyote: se puede saltar unos ticks después de salir del borde;
 *   - búfer de salto: si se presiona saltar justo antes de tocar el suelo, igual salta;
 *   - salto variable: soltar la tecla antes corta el salto (saltos cortos y largos).
 */
public class Personaje {

    public static final double ANCHO = 30;
    public static final double ALTO = 46;

    private static final double GRAVEDAD = 0.65;
    private static final double VEL_SALTO = -13.2;
    private static final double VEL_MAX = 4.6;
    private static final double CAIDA_MAX = 15;
    private static final int TICKS_COYOTE = 6;
    private static final int TICKS_BUFFER = 7;

    public double x;
    public double y;
    public double vx;
    public double vy;
    public boolean enSuelo;
    public boolean mirandoDerecha = true;

    private int coyote;
    private int bufferSalto;
    private double distanciaRecorrida;   // para animar las piernas

    public Personaje(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** Llamar cuando se presiona la tecla de salto (una vez por pulsación). */
    public void pedirSalto() {
        bufferSalto = TICKS_BUFFER;
    }

    /**
     * Avanza un tick de física. Devuelve true si el personaje acaba de saltar.
     *
     * @param direccion -1 izquierda, 0 quieto, 1 derecha
     * @param saltoSostenido si la tecla de salto sigue presionada
     */
    public boolean actualizar(int direccion, boolean saltoSostenido, Mundo mundo) {
        // ---- movimiento horizontal con aceleración suave ----
        double objetivo = direccion * VEL_MAX;
        vx += (objetivo - vx) * (enSuelo ? 0.35 : 0.2);
        if (Math.abs(vx) < 0.05) {
            vx = 0;
        }
        if (direccion != 0) {
            mirandoDerecha = direccion > 0;
        }
        x += vx;
        x = Math.max(0, Math.min(mundo.getAncho() - ANCHO, x));
        resolverParedes(mundo);
        if (enSuelo) {
            distanciaRecorrida += Math.abs(vx);
        }

        // ---- salto ----
        boolean salto = false;
        if (bufferSalto > 0) {
            bufferSalto--;
        }
        if (enSuelo) {
            coyote = TICKS_COYOTE;
        } else if (coyote > 0) {
            coyote--;
        }
        if (bufferSalto > 0 && coyote > 0) {
            vy = VEL_SALTO;
            bufferSalto = 0;
            coyote = 0;
            salto = true;
        }
        if (!saltoSostenido && vy < -5) {
            vy = -5;   // salto corto
        }

        // ---- gravedad y caída sobre plataformas ----
        double pieAntes = y + ALTO;
        vy = Math.min(CAIDA_MAX, vy + GRAVEDAD);
        y += vy;
        enSuelo = false;
        if (vy >= 0) {
            for (Plataforma p : mundo.getPlataformas()) {
                boolean encima = x + ANCHO > p.x + 3 && x < p.derecha() - 3;
                if (encima && pieAntes <= p.y + 1 && y + ALTO >= p.y) {
                    y = p.y - ALTO;
                    vy = 0;
                    enSuelo = true;
                    x += p.getDesplazamiento();   // la plataforma móvil lo arrastra
                    x = Math.max(0, Math.min(mundo.getAncho() - ANCHO, x));
                    break;
                }
            }
        }
        return salto;
    }

    /** El suelo es sólido: si el personaje cae en un hueco, no puede atravesar sus paredes. */
    private void resolverParedes(Mundo mundo) {
        for (Plataforma p : mundo.getPlataformas()) {
            if (p.tipo != Plataforma.Tipo.SUELO) {
                continue;
            }
            boolean dentroVertical = y + ALTO > p.y + 2;
            boolean dentroHorizontal = x + ANCHO > p.x && x < p.derecha();
            if (dentroVertical && dentroHorizontal) {
                if (x + ANCHO / 2 < p.x + p.ancho / 2) {
                    x = p.x - ANCHO;
                } else {
                    x = p.derecha();
                }
                vx = 0;
            }
        }
    }

    public boolean seCayo() {
        return y > Mundo.LIMITE_CAIDA;
    }

    public void reaparecer(double nuevaX) {
        x = nuevaX;
        y = Mundo.SUELO_Y - ALTO - 120;
        vx = 0;
        vy = 0;
    }

    public double getDistanciaRecorrida() {
        return distanciaRecorrida;
    }

    public double centroX() {
        return x + ANCHO / 2;
    }

    public double centroY() {
        return y + ALTO / 2;
    }
}
