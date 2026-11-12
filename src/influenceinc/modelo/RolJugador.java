package influenceinc.modelo;

import java.util.EnumSet;
import java.util.Set;

/**
 * El ROL que escoge cada jugador al empezar (seccion 4 del enunciado).
 * De el dependen:
 *  - las herramientas de investigacion que puede usar en el monitor, y
 *  - su HABILIDAD UNICA: una mecanica interactiva distinta para cada rol.
 *
 *   Ciudadano  → "¿Quién te lo mandó?": rastrea la cadena de reenvios en el
 *                grafo social, nodo por nodo, hasta la cuenta que la inventó.
 *   Periodista → Investigación completa: las 6 herramientas (corresponsal,
 *                escaner de red y llamadas son solo suyas).
 *   Influencer → "Cazabots": en 12 segundos marca las cuentas falsas que
 *                empujan la publicacion (sus decisiones pesan x1,5).
 *   Candidato  → "Rueda de prensa": responde 3 preguntas con tiempo
 *                arrastrando la respuesta al microfono; suma apoyo electoral.
 */
public enum RolJugador {
    CIUDADANO("Ciudadano", "¿Quién te lo mandó?",
            "Rastrea la cadena de reenvíos en el grafo social, persona por persona, hasta llegar a quien inventó la publicación.",
            "Interactúa con responsabilidad: antes de compartir, averigua de dónde salió lo que te llegó.",
            EnumSet.of(Herramienta.PERFIL, Herramienta.LUPA, Herramienta.IMAGEN)),
    PERIODISTA("Periodista", "Investigación completa",
            "Usa las 6 herramientas del monitor. El corresponsal (Dijkstra), el escáner de red y las llamadas son solo tuyos.",
            "Investiga, detecta noticias falsas y ayuda a la ciudad a distinguir lo confiable.",
            EnumSet.allOf(Herramienta.class)),
    INFLUENCER("Influencer", "Cazabots",
            "Tienes 12 segundos: marca las cuentas falsas que están empujando la publicación entre tus seguidores.",
            "Tu alcance es enorme: cada decisión tuya pesa x1,5 en puntos y en la ciudad.",
            EnumSet.of(Herramienta.PERFIL, Herramienta.IMAGEN, Herramienta.RED)),
    CANDIDATO("Candidato a alcalde", "Rueda de prensa",
            "Responde 3 preguntas con 10 segundos cada una: arrastra la respuesta al micrófono. Lo responsable suma votos.",
            "Construye confianza, comunica tus propuestas y enfrenta los rumores sin atacar a nadie.",
            EnumSet.of(Herramienta.PERFIL, Herramienta.DIRECTORIO, Herramienta.LUPA));

    public final String nombre;
    public final String habilidad;
    public final String descripcionHabilidad;
    public final String mision;
    private final Set<Herramienta> herramientas;

    RolJugador(String nombre, String habilidad, String descripcionHabilidad, String mision, Set<Herramienta> h) {
        this.nombre = nombre;
        this.habilidad = habilidad;
        this.descripcionHabilidad = descripcionHabilidad;
        this.mision = mision;
        this.herramientas = h;
    }

    public boolean puedeUsar(Herramienta h) {
        return herramientas.contains(h);
    }

    /** true si el rol tiene un mini-juego propio (el periodista usa el monitor completo). */
    public boolean tieneMiniJuego() {
        return this != PERIODISTA;
    }

    /** Multiplicador del impacto de sus decisiones (el influencer llega a mas gente). */
    public double impacto() {
        return this == INFLUENCER ? 1.5 : 1.0;
    }

    public static RolJugador desde(String s) {
        for (RolJugador r : values()) {
            if (r.name().equalsIgnoreCase(s) || r.nombre.equalsIgnoreCase(s)) {
                return r;
            }
        }
        return PERIODISTA;
    }
}
