package alcaldedigital.modelo;

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

    /**
     * Constructor principal. Valida que el ID esté entre 0 y 999 
     * para garantizar que la clave compuesta del AVL funcione bien.
     */
    public Publicacion(int id, String texto, String autor, TipoContenido tipo, int credibilidad) {
        if (id < 0 || id > 999) {
            throw new IllegalArgumentException("El id debe estar entre 0 y 999 para que la clave compuesta funcione.");
        }
        this.id = id;
        this.texto = texto;
        this.autor = autor;
        this.tipo = tipo;
        this.credibilidad = Math.max(0, Math.min(999, credibilidad));
        this.verificada = false;
        this.retirada = false;
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