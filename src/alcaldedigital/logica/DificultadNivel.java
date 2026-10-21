package alcaldedigital.logica;

/**
 * Parámetros de dificultad de un nivel de plataformas. Todo se calcula a partir
 * del número del nivel (1..7), así que subir de nivel siempre es más difícil:
 *
 *   - más burbujas correctas por publicación,
 *   - menos tiempo por publicación,
 *   - menos burbujas correctas en el mapa (más trampas),
 *   - más huecos en el suelo y huecos más anchos,
 *   - burbujas que se mueven (desde el nivel 3),
 *   - plataformas móviles (desde el nivel 4),
 *   - símbolos que cambian solos cada pocos segundos (desde el nivel 5).
 */
public class DificultadNivel {

    public static final int PUBLICACIONES_POR_NIVEL = 4;
    public static final int VIDAS = 3;

    public final int nivel;
    public final int burbujasRequeridas;     // correctas por publicación
    public final int segundosPorPublicacion;
    public final double proporcionCorrectas; // de las burbujas visibles, cuántas son la correcta
    public final int huecos;
    public final int anchoMaxHueco;          // en píxeles
    public final int anchoMundo;             // largo del mapa en píxeles
    public final int amplitudMovimiento;     // cuánto se mueven las burbujas de lado a lado
    public final int plataformasMoviles;
    public final int segundosCambioSimbolos; // 0 = los símbolos no cambian solos

    public DificultadNivel(int nivel) {
        this.nivel = nivel;
        this.burbujasRequeridas = 2 + nivel;                       // 3, 4, 5 ... 9
        this.segundosPorPublicacion = 50 - 3 * nivel;              // 47 ... 29
        this.proporcionCorrectas = Math.max(0.25, 0.50 - 0.035 * nivel);
        this.huecos = 1 + nivel;                                   // 2 ... 8
        this.anchoMaxHueco = Math.min(170, 90 + 12 * nivel);
        this.anchoMundo = 2800 + 380 * nivel;
        this.amplitudMovimiento = nivel >= 3 ? 14 * (nivel - 2) : 0;
        this.plataformasMoviles = nivel >= 4 ? nivel - 2 : 0;
        this.segundosCambioSimbolos = nivel >= 5 ? 13 - nivel : 0; // 8, 7, 6 s
    }

    /** Texto corto para mostrar en el mapa antes de entrar. */
    public String resumen() {
        StringBuilder sb = new StringBuilder();
        sb.append(burbujasRequeridas).append(" burbujas correctas por publicación · ")
          .append(segundosPorPublicacion).append(" s por publicación");
        if (amplitudMovimiento > 0) {
            sb.append(" · burbujas en movimiento");
        }
        if (plataformasMoviles > 0) {
            sb.append(" · plataformas móviles");
        }
        if (segundosCambioSimbolos > 0) {
            sb.append(" · símbolos que cambian cada ").append(segundosCambioSimbolos).append(" s");
        }
        return sb.toString();
    }
}
