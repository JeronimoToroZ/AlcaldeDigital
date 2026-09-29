package alcaldedigital.estructuras;

import alcaldedigital.modelo.Publicacion;

/**
 * Representa un nodo individual dentro del Árbol AVL de publicaciones.
 * Almacena los apuntadores a sus subárboles, su clave de ordenamiento, 
 * los datos de la publicación y su respectiva altura para el control de balanceo.
 * 
 * Convención de altura: un nodo hoja mide 0 y un subárbol vacío mide -1, 
 * lo que permite calcular el factor de equilibrio directamente restando alturas.
 * 
 * @author Naty
 */
public class NodoAVL {

    private final int clave;
    private Publicacion publicacion;
    private NodoAVL izquierdo;
    private NodoAVL derecho;
    private int altura;

    /**
     * Constructor del nodo. Asigna la publicación correspondiente,
     * extrae su clave numérica para el árbol y lo inicializa como hoja (altura 0).
     */
    public NodoAVL(Publicacion publicacion) {
        this.publicacion = publicacion;
        this.clave = publicacion.claveAVL();
        this.izquierdo = null;
        this.derecho = null;
        this.altura = 0;
    }

    // ------------------------------------------------------------------
    // Métodos Getters y Setters de los datos y enlaces del nodo
    // ------------------------------------------------------------------

    public int getClave() {
        return clave;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public void setPublicacion(Publicacion p) {
        this.publicacion = p;
    }

    public NodoAVL getIzquierdo() {
        return izquierdo;
    }

    public void setIzquierdo(NodoAVL n) {
        this.izquierdo = n;
    }

    public NodoAVL getDerecho() {
        return derecho;
    }

    public void setDerecho(NodoAVL n) {
        this.derecho = n;
    }

    public int getAltura() {
        return altura;
    }

    public void setAltura(int altura) {
        this.altura = altura;
    }

    /**
     * Verifica si el nodo actual es una hoja (no posee hijos en ninguna dirección).
     */
    public boolean esHoja() {
        return izquierdo == null && derecho == null;
    }
}