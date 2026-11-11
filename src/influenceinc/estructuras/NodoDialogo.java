package influenceinc.estructuras;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo del ARBOL DE DIALOGO: lo que pregunta el verificador y lo que responde
 * el visitante. Los hijos son las preguntas que se abren despues.
 */
public class NodoDialogo {

    public final String pregunta;
    public final String respuesta;
    /** Expresion del visitante al responder: neutral, feliz, nervioso, enojado, roto. */
    public final String expresion;
    /** Pista que se gana al hacer esta pregunta (o null). */
    public final String pista;
    final List<NodoDialogo> hijos = new ArrayList<>();
    NodoDialogo padre;
    public boolean visitado;
    /** Nodo insertado durante la partida (confrontacion con una evidencia). */
    public boolean insertado;

    public NodoDialogo(String pregunta, String respuesta, String expresion, String pista) {
        this.pregunta = pregunta;
        this.respuesta = respuesta;
        this.expresion = expresion == null ? "neutral" : expresion;
        this.pista = pista;
    }

    public NodoDialogo hijo(NodoDialogo h) {
        h.padre = this;
        hijos.add(h);
        return this;
    }

    public List<NodoDialogo> getHijos() {
        return hijos;
    }

    public NodoDialogo getPadre() {
        return padre;
    }

    public boolean esHoja() {
        return hijos.isEmpty();
    }
}
