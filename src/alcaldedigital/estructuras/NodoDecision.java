package alcaldedigital.estructuras;

import alcaldedigital.logica.Efecto;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un nodo dentro del Árbol N-ario de Decisión.
 *
 * Estructura: - Nodos internos: almacenan una pregunta y una lista de opciones
 * hijas. - Hojas: almacenan un objeto Efecto con las consecuencias concretas
 * sobre la ciudad. - Clave: etiqueta de transición (Acción del jugador o Tipo
 * de Contenido).
 *
 * @author Naty
 */
public class NodoDecision {

    private final Object clave;      // Accion o TipoContenido; null en la raiz
    private final String etiqueta;
    private final String pregunta;   // null si es hoja
    private final Efecto efecto;     // null si es nodo interno
    private final List<NodoDecision> hijos;

    /**
     * Constructor para un nodo interno (contiene preguntas y sub-ramas).
     */
    public NodoDecision(Object clave, String etiqueta, String pregunta) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.pregunta = pregunta;
        this.efecto = null;
        this.hijos = new ArrayList<>();
    }

    /**
     * Constructor para una hoja (contiene el efecto final de la decisión).
     */
    public NodoDecision(Object clave, String etiqueta, Efecto efecto) {
        this.clave = clave;
        this.etiqueta = etiqueta;
        this.pregunta = null;
        this.efecto = efecto;
        this.hijos = new ArrayList<>();
    }

    /**
     * Agrega una nueva rama hija al nodo actual y retorna la instancia para
     * encadenamiento.
     */
    public NodoDecision agregarHijo(NodoDecision hijo) {
        hijos.add(hijo);
        return this;
    }

    /**
     * Desciende al siguiente nivel buscando la rama que coincida con la clave
     * indicada.
     */
    public NodoDecision hijoPor(Object claveBuscada) {
        for (NodoDecision h : hijos) {
            if (claveBuscada.equals(h.clave)) {
                return h;
            }
        }
        return null;
    }

    /**
     * Verifica si el nodo actual es una hoja evaluando si posee un efecto
     * asignado.
     */
    public boolean esHoja() {
        return efecto != null;
    }

    // ------------------------------------------------------------------
    // Métodos Getters de los atributos del nodo
    // ------------------------------------------------------------------
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
