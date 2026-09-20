package alcaldedigital.estructuras;

import alcaldedigital.logica.Efecto;
import alcaldedigital.modelo.Accion;
import alcaldedigital.modelo.TipoContenido;

/**
 * ARBOL DE DECISION N-ARIO
 * ========================
 *
 * 1. QUE PROBLEMA RESUELVE
 *    Las consecuencias de una decision no dependen solo de lo que hizo el jugador,
 *    sino tambien de lo que la publicacion era REALMENTE (verdadera, falsa u opinion),
 *    dato que el jugador no conoce al decidir. Eso son 4 x 3 = 12 combinaciones.
 *    Meterlas en un if/switch gigante haria el codigo imposible de mantener y de
 *    balancear. El arbol convierte esas reglas en datos: la logica del juego solo
 *    "desciende" por el arbol y aplica la hoja que encuentra.
 *
 * 2. POR QUE N-ARIO Y NO BINARIO
 *    El primer nivel tiene 4 ramas (las 4 acciones) y el segundo 3 (los 3 tipos de
 *    contenido). Un arbol binario obligaria a encadenar decisiones artificiales.
 *    El grado del nodo aqui es el numero real de opciones del jugador.
 *
 * 3. RECORRIDO
 *    - Durante la partida: descenso dirigido, raiz -> accion -> tipo. Cuesta O(grado)
 *      por nivel, es decir constante, contra O(1) de un switch pero con reglas
 *      declarativas y auditables.
 *    - En la pantalla de AYUDA: recorrido en PREORDEN que imprime el arbol completo,
 *      para que el jugador vea todas las consecuencias posibles antes de jugar.
 *
 *              PUBLICACION  (raiz: que quieres hacer?)
 *              /      |       |        \
 *      Compartir  Verificar  Ignorar  Reportar
 *        /|\        /|\       /|\       /|\
 *       V F O      V F O     V F O     V F O      <- que era realmente
 *      (12 hojas = 12 efectos distintos)
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
     * DESCENSO: raiz -> accion del jugador -> naturaleza real de la publicacion.
     * Devuelve el efecto de la hoja alcanzada.
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

    /** Ruta legible de la decision, para mostrar en pantalla. */
    public String rutaDe(Accion accion, TipoContenido tipo) {
        NodoDecision n1 = raiz.hijoPor(accion);
        NodoDecision n2 = (n1 == null) ? null : n1.hijoPor(tipo);
        return "Publicacion -> " + (n1 == null ? "?" : n1.getEtiqueta())
                + " -> " + (n2 == null ? "?" : n2.getEtiqueta());
    }

    /** Recorrido en PREORDEN: raiz, luego cada subarbol de izquierda a derecha. */
    public String recorridoPreorden() {
        StringBuilder sb = new StringBuilder();
        preorden(raiz, 0, sb);
        return sb.toString();
    }

    private void preorden(NodoDecision nodo, int nivel, StringBuilder sb) {
        sb.append("   ".repeat(nivel));
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

    /** Cuenta de nodos, util para la sustentacion. */
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
