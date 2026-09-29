package alcaldedigital.estructuras;

import alcaldedigital.logica.Efecto;
import alcaldedigital.modelo.Accion;
import alcaldedigital.modelo.TipoContenido;

/**
 * ÁRBOL DE DECISIÓN N-ARIO
 * ========================
 * Gestiona las combinaciones de consecuencias según la acción del usuario
 * y la naturaleza real del contenido (verdadera, falsa u opinión).
 * 
 * @author Naty
 */
public class ArbolDecision {

    private final NodoDecision raiz;

    public ArbolDecision() {
        this.raiz = construir();
    }

    private NodoDecision construir() {
        NodoDecision raizLocal = new NodoDecision(null, "Publicacion recibida",
                "Que quieres hacer con esta publicacion?");

        // ---------------- RAMA 1: COMPARTIR ----------------
        NodoDecision compartir = new NodoDecision(Accion.COMPARTIR, "Compartir",
                "Y que era realmente?");
        compartir.agregarHijo(new NodoDecision(TipoContenido.VERDADERA, "Era verdadera",
                new Efecto(6, 4, 2, 2, -3, 0, 15, 5,
                        "Compartiste informacion verdadera: la ciudad se informo mejor.", false)));
        compartir.agregarHijo(new NodoDecision(TipoContenido.FALSA, "Era falsa",
                new Efecto(-6, -8, -6, -5, 14, 9, -20, -10,
                        "Compartiste una noticia falsa sin verificar. Se propago por toda Ciudad Nova.", false)));
        compartir.agregarHijo(new NodoDecision(TipoContenido.OPINION, "Era una opinion",
                new Efecto(0, -2, -3, -1, 3, 4, -5, -2,
                        "Compartiste una opinion como si fuera un hecho. Genero discusion.", false)));

        // ---------------- RAMA 2: VERIFICAR ----------------
        NodoDecision verificar = new NodoDecision(Accion.VERIFICAR, "Verificar",
                "Que arrojo la verificacion?");
        verificar.agregarHijo(new NodoDecision(TipoContenido.VERDADERA, "Se confirmo verdadera",
                new Efecto(10, 6, 3, 4, -5, -2, 25, 8,
                        "Verificaste antes de actuar y la informacion resulto cierta. Asi se construye confianza.", false)));
        verificar.agregarHijo(new NodoDecision(TipoContenido.FALSA, "Se detecto falsa",
                new Efecto(9, 7, 5, 5, -12, -4, 30, 10,
                        "Detectaste la noticia falsa antes de que se propagara. Jugada perfecta.", false)));
        verificar.agregarHijo(new NodoDecision(TipoContenido.OPINION, "Era una opinion",
                new Efecto(5, 3, 4, 3, -2, -3, 18, 6,
                        "Identificaste que era una opinion y no un hecho. Quedo marcada como tal.", false)));

        // ---------------- RAMA 3: IGNORAR ----------------
        NodoDecision ignorar = new NodoDecision(Accion.IGNORAR, "Ignorar",
                "Que era lo que dejaste pasar?");
        ignorar.agregarHijo(new NodoDecision(TipoContenido.VERDADERA, "Era verdadera",
                new Efecto(-3, -1, 0, 1, 2, 0, -5, -1,
                        "Ignoraste informacion util. No hiciste dano, pero la ciudad se quedo sin saberlo.", false)));
        ignorar.agregarHijo(new NodoDecision(TipoContenido.FALSA, "Era falsa",
                new Efecto(0, 0, 1, 2, 2, -1, 5, 1,
                        "No la compartiste, y eso evito dano. Pero el rumor sigue circulando entre otros.", false)));
        ignorar.agregarHijo(new NodoDecision(TipoContenido.OPINION, "Era una opinion",
                new Efecto(0, 0, 2, 3, 0, -2, 8, 2,
                        "Dejaste pasar una opinion sin alimentar la pelea. Buen manejo del bienestar digital.", false)));

        // ---------------- RAMA 4: REPORTAR ----------------
        NodoDecision reportar = new NodoDecision(Accion.REPORTAR, "Reportar",
                "Que era lo que reportaste?");
        reportar.agregarHijo(new NodoDecision(TipoContenido.VERDADERA, "Era verdadera",
                new Efecto(-5, -6, -4, -2, 4, 7, -18, -8,
                        "Reportaste informacion verdadera. Censurar lo cierto tambien rompe la confianza.", true)));
        reportar.agregarHijo(new NodoDecision(TipoContenido.FALSA, "Era falsa",
                new Efecto(7, 5, 6, 4, -15, -5, 28, 9,
                        "Reportaste una noticia falsa y fue retirada del feed. La ciudad te lo agradece.", true)));
        reportar.agregarHijo(new NodoDecision(TipoContenido.OPINION, "Era una opinion",
                new Efecto(0, -3, -5, -2, 0, 6, -12, -5,
                        "Reportaste una opinion legitima. Eso no es moderar, es silenciar.", true)));

        raizLocal.agregarHijo(compartir);
        raizLocal.agregarHijo(verificar);
        raizLocal.agregarHijo(ignorar);
        raizLocal.agregarHijo(reportar);
        return raizLocal;
    }

    /**
     * Desciende por el árbol según la acción del usuario y el contenido real
     * para retornar el efecto correspondiente.
     */
    public Efecto resolver(Accion accion, TipoContenido tipo) {
        NodoDecision nivel1 = raiz.hijoPor(accion);
        if (nivel1 == null) {
            throw new IllegalStateException("El arbol no tiene rama para la accion " + accion);
        }
        NodoDecision hoja = nivel1.hijoPor(tipo);
        if (hoja == null) {
            throw new IllegalStateException("El arbol no tiene hoja para el tipo " + tipo);
        }
        return hoja.getEfecto();
    }

    /** Retorna una ruta legible de la decisión tomada. */
    public String rutaDe(Accion accion, TipoContenido tipo) {
        NodoDecision n1 = raiz.hijoPor(accion);
        NodoDecision n2 = (n1 == null) ? null : n1.hijoPor(tipo);
        return "Publicacion -> " + (n1 == null ? "?" : n1.getEtiqueta())
                + " -> " + (n2 == null ? "?" : n2.getEtiqueta());
    }

    /** Recorrido en PREORDEN para mostrar el árbol completo en pantallas de ayuda. */
    public String recorridoPreorden() {
        StringBuilder sb = new StringBuilder();
        preorden(raiz, 0, sb);
        return sb.toString();
    }

    private void preorden(NodoDecision nodo, int nivel, StringBuilder sb) {
        // Reemplazo de String.repeat() compatible con JDK 8 mediante un bucle
        for (int i = 0; i < nivel; i++) {
            sb.append("   ");
        }
        
        sb.append(nivel == 0 ? "" : "|- ");
        sb.append(nodo.getEtiqueta());
        
        if (nodo.esHoja()) {
            sb.append("   =>  ").append(nodo.getEfecto().resumenDeltas());
        }
        sb.append('\n');
        
        for (NodoDecision h : nodo.getHijos()) {
            preorden(h, nivel + 1, sb);
        }
    }

    /** Retorna el conteo total de nodos del árbol. */
    public int cantidadNodos() {
        return contar(raiz);
    }

    private int contar(NodoDecision nodo) {
        int total = 1;
        for (NodoDecision h : nodo.getHijos()) {
            total += contar(h);
        }
        return total;
    }

    public NodoDecision getRaiz() {
        return raiz;
    }
}