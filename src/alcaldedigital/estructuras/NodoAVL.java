package alcaldedigital.estructuras;

import alcaldedigital.modelo.Publicacion;

/**
 * Nodo del arbol AVL de publicaciones.
 *
 * CONVENCION DE ALTURA: la altura de un nodo hoja es 0 y la de un subarbol vacio
 * es -1. Con esa convencion el factor de equilibrio se calcula restando
 * directamente las alturas de los hijos.
 */
public class NodoAVL {

    private final int clave;
    private Publicacion publicacion;
    private NodoAVL izquierdo;
    private NodoAVL derecho;
    private int altura;

    public NodoAVL(Publicacion publicacion) {
        this.publicacion = publicacion;
        this.clave = publicacion.claveAVL();
        this.izquierdo = null;
        this.derecho = null;
        this.altura = 0; // una hoja mide 0
    }

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

    public boolean esHoja() {
        return izquierdo == null && derecho == null;
    }
}
