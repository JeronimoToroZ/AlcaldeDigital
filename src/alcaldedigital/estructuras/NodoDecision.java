package alcaldedigital.estructuras;

import alcaldedigital.logica.Efecto;
import java.util.ArrayList;
import java.util.List;

/**
 * Nodo de un arbol N-ARIO de decision.
 *
 * - Los nodos internos guardan una pregunta y una lista de hijos.
 * - Las hojas guardan un Efecto (la consecuencia concreta sobre la ciudad).
 * - "clave" es la etiqueta por la que se llega a este nodo desde su padre:
 *   en el primer nivel es una Accion del jugador y en el segundo un TipoContenido.
 */
public class NodoDecision {

    private final Object clave;      // Accion o TipoContenido; null en la raiz
    private final String etiqueta;
    private final String pregunta;   // null si es hoja
    private final Efecto efecto;     // null si es nodo interno
    private final List<NodoDecision> hijos;

    /** Constructor de nodo interno. */
    public NodoDecision(Object clave, String etiqueta, String pregunta) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.pregunta = pregunta;
        this.efecto = null;
        this.hijos = new ArrayList<>();
    }

    /** Constructor de hoja. */
    public NodoDecision(Object clave, String etiqueta, Efecto efecto) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.pregunta = null;
        this.efecto = efecto;
        this.hijos = new ArrayList<>();
    }

    public NodoDecision agregarHijo(NodoDecision hijo) {
        hijos.add(hijo);
        return this;
    }

    /** Baja un nivel siguiendo la etiqueta indicada. Null si no existe esa rama. */
    public NodoDecision hijoPor(Object claveBuscada) {
        for (NodoDecision h : hijos) {
            if (claveBuscada.equals(h.clave)) {
                return h;
            }
        }
        return null;
    }

    public boolean esHoja() {
        return efecto != null;
    }

    public Object getClave() {
        return clave;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getPregunta() {
        return pregunta;
    }

    public Efecto getEfecto() {
        return efecto;
    }

    public List<NodoDecision> getHijos() {
        return hijos;
    }
}
