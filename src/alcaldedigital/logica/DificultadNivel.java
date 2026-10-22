package alcaldedigital.logica;

/**
 * Parámetros de dificultad de un nivel de plataformas. Todo se calcula a partir
 * del número del nivel (1..7).
 *
 * La dificultad principal es la VELOCIDAD con la que las burbujas se generan en
 * otro lugar del mapa: cada 2.0 s en el nivel 1 y 0.1 s menos por nivel
 * (1.9, 1.8, 1.7, 1.6, 1.5 y 1.4 s en el nivel 7). Además el mapa es más largo,
 * con más huecos y, desde el nivel 4, con plataformas móviles.
 */
public class DificultadNivel {

    public static final int PUBLICACIONES_POR_NIVEL = 4;
    public static final int VIDAS = 3;
    /** Burbujas correctas que hay que recolectar en cada publicación (igual en todos los niveles). */
    public static final int BURBUJAS_REQUERIDAS = 3;
    /** Distancia mínima entre dos burbujas, para que nunca queden pegadas. */
    public static final int DISTANCIA_MINIMA_BURBUJAS = 170;

    public final int nivel;
    public final int burbujasRequeridas;     // correctas por publicación
    public final int segundosPorPublicacion;
    public final double proporcionCorrectas; // de las burbujas visibles, cuántas son la correcta
    public final int huecos;
    public final int anchoMaxHueco;          // en píxeles
    public final int anchoMundo;             // largo del mapa en píxeles
    public final int amplitudMovimiento;     // las burbujas ya no se mueven de lado (se reubican)
    public final int plataformasMoviles;
    public final double segundosReubicacion; // cada cuánto se generan en otro lugar
    public final int cantidadBurbujas;       // burbujas en el mapa al mismo tiempo

    public DificultadNivel(int nivel) {
        this.nivel = nivel;
        this.burbujasRequeridas = BURBUJAS_REQUERIDAS;
        this.segundosPorPublicacion = 50 - 2 * nivel;              // 48 ... 36
        this.proporcionCorrectas = 0.40;
        this.huecos = 1 + nivel;                                   // 2 ... 8
        this.anchoMaxHueco = Math.min(170, 90 + 12 * nivel);
        this.anchoMundo = 2800 + 380 * nivel;
        this.amplitudMovimiento = 0;
        this.plataformasMoviles = nivel >= 4 ? nivel - 2 : 0;
        this.segundosReubicacion = Math.round((2.1 - 0.1 * nivel) * 10) / 10.0;   // 2.0 ... 1.4
        this.cantidadBurbujas = Math.max(16, Math.min(28, anchoMundo / 170));
    }

    /** Texto corto para mostrar en el mapa antes de entrar. */
    public String resumen() {
        StringBuilder sb = new StringBuilder();
        sb.append(burbujasRequeridas).append(" burbujas correctas por publicación · ")
          .append("cambian de lugar cada ").append(segundosReubicacion).append(" s");
        if (plataformasMoviles > 0) {
            sb.append(" · plataformas móviles");
        }
        return sb.toString();
    }
}
