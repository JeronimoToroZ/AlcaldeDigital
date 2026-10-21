package alcaldedigital.juego;

import alcaldedigital.logica.DificultadNivel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * El mapa de un nivel: suelo con huecos, plataformas flotantes y móviles, y los
 * puntos donde pueden aparecer burbujas. Se genera al azar en cada intento
 * (ninguna partida es igual), pero siempre respetando las medidas del salto
 * del personaje para que todo sea alcanzable.
 *
 * Unidades: píxeles "del mundo". El alto es fijo (600) y la pantalla se escala,
 * así el juego se siente igual en cualquier tamaño de ventana.
 */
public class Mundo {

    public static final int ALTO = 600;
    public static final int SUELO_Y = 520;
    /** Más abajo de esto el personaje cayó en un hueco. */
    public static final int LIMITE_CAIDA = ALTO + 60;

    private final int ancho;
    private final List<Plataforma> plataformas = new ArrayList<>();
    private final List<double[]> huecos = new ArrayList<>();          // {inicio, fin}
    private final List<double[]> puntosBurbuja = new ArrayList<>();   // {x, y}
    private int movilesSobrantes;   // plataformas móviles que no se usaron como puente

    public Mundo(DificultadNivel d, Random azar) {
        this.ancho = d.anchoMundo;
        generarSuelo(d, azar);
        generarPlataformas(d, azar);
        generarPuntosBurbuja(azar);
    }

    // ------------------------------------------------------------------
    // Generación
    // ------------------------------------------------------------------

    private void generarSuelo(DificultadNivel d, Random azar) {
        double inicio = 650;
        double fin = ancho - 450;
        double tramo = (fin - inicio) / d.huecos;
        int moviles = d.plataformasMoviles;

        double xSuelo = 0;
        for (int i = 0; i < d.huecos; i++) {
            double w = 70 + azar.nextInt(Math.max(1, d.anchoMaxHueco - 70 + 1));
            double x = inicio + i * tramo + azar.nextInt((int) Math.max(1, tramo - w - 80));
            plataformas.add(new Plataforma(Plataforma.Tipo.SUELO, xSuelo, SUELO_Y, x - xSuelo, ALTO));
            huecos.add(new double[]{x, x + w});
            xSuelo = x + w;

            // los huecos anchos (o los de niveles con plataformas móviles) llevan un "puente" móvil
            if (moviles > 0 && (w > 120 || azar.nextBoolean())) {
                double pw = 96;
                plataformas.add(new Plataforma(Plataforma.Tipo.MOVIL, x + w / 2 - pw / 2, SUELO_Y - 55,
                        pw, 18, Math.max(30, w / 2), 0.025 + azar.nextDouble() * 0.015, azar.nextDouble() * 6));
                moviles--;
            }
        }
        plataformas.add(new Plataforma(Plataforma.Tipo.SUELO, xSuelo, SUELO_Y, ancho - xSuelo, ALTO));
        this.movilesSobrantes = moviles;
    }


    private void generarPlataformas(DificultadNivel d, Random azar) {
        double x = 380;
        while (x < ancho - 320) {
            double w = 110 + azar.nextInt(90);
            double y = SUELO_Y - 80 - azar.nextInt(46);          // alcanzable desde el suelo
            boolean movil = movilesSobrantes > 0 && azar.nextInt(100) < 35;
            if (movil) {
                plataformas.add(new Plataforma(Plataforma.Tipo.MOVIL, x, y, w, 18,
                        70, 0.02 + azar.nextDouble() * 0.015, azar.nextDouble() * 6));
                movilesSobrantes--;
            } else {
                plataformas.add(new Plataforma(Plataforma.Tipo.FLOTANTE, x, y, w, 18));
            }

            // segundo piso: alcanzable desde la plataforma de abajo
            if (azar.nextInt(100) < 45) {
                double w2 = 100 + azar.nextInt(60);
                double x2 = x + (azar.nextBoolean() ? 1 : -1) * (60 + azar.nextInt(60));
                double y2 = y - 88 - azar.nextInt(25);
                plataformas.add(new Plataforma(Plataforma.Tipo.FLOTANTE, x2, y2, w2, 18));
            }
            x += w + 170 + azar.nextInt(170);
        }
    }

    private void generarPuntosBurbuja(Random azar) {
        // sobre el suelo, cada ~260 px (sin caer encima de un hueco); hay que saltar las equivocadas
        for (double x = 320; x < ancho - 80; x += 220 + azar.nextInt(90)) {
            if (!sobreHueco(x)) {
                puntosBurbuja.add(new double[]{x, SUELO_Y - 38});
            }
        }
        for (Plataforma p : plataformas) {
            if (p.tipo == Plataforma.Tipo.SUELO) {
                continue;
            }
            puntosBurbuja.add(new double[]{p.x + p.ancho / 2, p.y - 38});
            if (p.tipo == Plataforma.Tipo.FLOTANTE && azar.nextBoolean()) {
                // en el aire, por fuera del borde: se alcanza saltando desde la plataforma
                // (no queda encima de la otra burbuja, así no obliga a tocarla al caer)
                double lado = azar.nextBoolean() ? p.derecha() + 45 : p.x - 45;
                puntosBurbuja.add(new double[]{lado, p.y - 75});
            }
        }
        // sobre los huecos: arriesgado, se agarran en pleno salto
        for (double[] h : huecos) {
            puntosBurbuja.add(new double[]{(h[0] + h[1]) / 2, SUELO_Y - 105});
        }
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public void actualizar(long tick) {
        for (Plataforma p : plataformas) {
            p.actualizar(tick);
        }
    }

    public boolean sobreHueco(double x) {
        for (double[] h : huecos) {
            if (x > h[0] - 30 && x < h[1] + 30) {
                return true;
            }
        }
        return false;
    }

    /** Punto seguro de reaparición: suelo firme más cercano (hacia atrás) a x. */
    public double xSeguroCercaDe(double x) {
        double candidato = Math.max(60, x);
        while (candidato > 60 && sobreHueco(candidato)) {
            candidato -= 20;
        }
        return candidato;
    }

    public int getAncho() { return ancho; }
    public List<Plataforma> getPlataformas() { return plataformas; }
    public List<double[]> getHuecos() { return huecos; }
    public List<double[]> getPuntosBurbuja() { return puntosBurbuja; }
}
