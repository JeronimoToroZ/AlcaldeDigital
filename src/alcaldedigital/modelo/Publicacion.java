package alcaldedigital.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Una publicación del feed de Ciudad Nova.
 *
 * CLAVE DEL AVL
 * -------------
 * El árbol AVL ordena las publicaciones por credibilidad, porque la consulta que
 * más usa el juego es "dame las publicaciones menos creíbles del feed".
 * El problema es que la credibilidad se repite (dos publicaciones pueden tener 30),
 * y un ABB/AVL clásico no admite claves duplicadas.
 *
 * Solución: clave compuesta = credibilidad * 1000 + id.
 * Como el id es único y siempre menor que 1000, la clave nunca se repite y, al
 * dividir por 1000, el orden sigue siendo exactamente el orden por credibilidad.
 * Un recorrido inorden devuelve el feed ordenado de menos a más creíble.
 */
public class Publicacion {

    private final int id;
    private final String texto;
    private final String autor;
    private final TipoContenido tipo;
    private final int credibilidad;   // 0..999
    private boolean verificada;
    private boolean retirada;

    // --- Lo que el jugador tiene que poder MIRAR para decidir por si mismo ---

    /** Quien publico. Puede ser null en las publicaciones del banco antiguo. */
    private final Cuenta cuenta;
    /** Las pistas que tiene esta publicacion. Vacia si no tiene ninguna. */
    private final List<Senal> senales;
    /** Que fuente cita, tal como la escribio. null si no cita ninguna. */
    private final String fuenteCitada;
    /** Hace cuanto se publico, como lo muestra una red social: "hace 2 h". */
    private final String hora;
    /** Imagen que acompana la publicacion, en recursos. Puede ser null. */
    private final String imagen;
    /** Por que es falsa, o por que es cierta a pesar de parecer falsa. */
    private final String explicacion;

    /**
     * Constructor principal. Valida que el ID esté entre 0 y 999
     * para garantizar que la clave compuesta del AVL funcione bien.
     */
    public Publicacion(int id, String texto, String autor, TipoContenido tipo, int credibilidad) {
        this(id, texto, autor, tipo, credibilidad, null, null, null, null, null, null);
    }

    /**
     * Constructor completo: la publicacion con todo lo que el jugador puede revisar.
     *
     * @param cuenta       quien publica
     * @param senales      las pistas sospechosas (null o vacia si no tiene)
     * @param fuenteCitada que fuente cita, o null
     * @param hora         "hace 2 h", "ayer"
     * @param imagen       nombre del recurso de imagen, o null
     * @param explicacion  por que es falsa o por que es cierta; se muestra al final
     */
    public Publicacion(int id, String texto, String autor, TipoContenido tipo, int credibilidad,
                       Cuenta cuenta, List<Senal> senales, String fuenteCitada,
                       String hora, String imagen, String explicacion) {
        if (id < 0 || id > 999) {
            throw new IllegalArgumentException("El id debe estar entre 0 y 999 para que la clave compuesta funcione.");
        }
        this.id = id;
        this.texto = texto;
        this.autor = (autor != null) ? autor : (cuenta != null ? cuenta.getArroba() : "");
        this.tipo = tipo;
        this.credibilidad = Math.max(0, Math.min(999, credibilidad));
        this.verificada = false;
        this.retirada = false;
        this.cuenta = cuenta;
        this.senales = (senales == null) ? new ArrayList<>() : new ArrayList<>(senales);
        this.fuenteCitada = fuenteCitada;
        this.hora = hora;
        this.imagen = imagen;
        this.explicacion = explicacion;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    /** Las senales sospechosas de esta publicacion. Nunca null; puede venir vacia. */
    public List<Senal> getSenales() {
        return Collections.unmodifiableList(senales);
    }

    public String getFuenteCitada() {
        return fuenteCitada;
    }

    public String getHora() {
        return hora;
    }

    public String getImagen() {
        return imagen;
    }

    public String getExplicacion() {
        return explicacion;
    }

    /** true si de verdad es falsa. Es la respuesta correcta, no lo que parece. */
    public boolean esFalsa() {
        return tipo == TipoContenido.FALSA;
    }

    /**
     * true si no tiene ninguna senal visible.
     *
     * Ojo: que no tenga senales NO significa que sea cierta, y tener senales NO
     * significa que sea falsa. En el banco hay publicaciones ciertas con titular
     * alarmista, justamente para que el jugador no aprenda una regla mecanica.
     */
    public boolean pareceLimpia() {
        return senales.isEmpty();
    }

    /** Retorna la clave única y ordenada por credibilidad que usa el AVL. */
    public int claveAVL() {
        return credibilidad * 1000 + id;
    }

    public int getId() {
        return id;
    }

    public String getTexto() {
        return texto;
    }

    public String getAutor() {
        return autor;
    }

    public TipoContenido getTipo() {
        return tipo;
    }

    public int getCredibilidad() {
        return credibilidad;
    }

    public boolean estaVerificada() {
        return verificada;
    }

    public void marcarVerificada() {
        this.verificada = true;
    }

    public boolean estaRetirada() {
        return retirada;
    }

    public void marcarRetirada() {
        this.retirada = true;
    }

    @Override
    public String toString() {
        return "[#" + id + " cred=" + credibilidad + "] " + texto + " - " + autor;
    }
}