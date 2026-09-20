package alcaldedigital.modelo;

/**
 * Una publicacion del feed de Civitas.
 *
 * CLAVE DEL AVL
 * -------------
 * El arbol AVL ordena las publicaciones por credibilidad, porque la consulta que
 * mas usa el juego es "dame las publicaciones menos creibles del feed".
 * El problema es que la credibilidad se repite (dos publicaciones pueden tener 30),
 * y un ABB/AVL clasico no admite claves duplicadas.
 *
 * Solucion: clave compuesta = credibilidad * 1000 + id.
 * Como el id es unico y siempre menor que 1000, la clave nunca se repite y, al
 * dividir por 1000, el orden sigue siendo exactamente el orden por credibilidad.
 * Un recorrido inorden devuelve el feed ordenado de menos a mas creible.
 */
public class Publicacion {

    private final int id;
    private final String texto;
    private final String autor;
    private final TipoContenido tipo;
    private final int credibilidad;   // 0..999
    private boolean verificada;
    private boolean retirada;

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

    /** Clave unica y ordenada por credibilidad que usa el AVL. */
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
