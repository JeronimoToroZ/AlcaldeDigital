package influenceinc.estructuras;

import influenceinc.modelo.EfectoCiudad;
import influenceinc.modelo.Veredicto;
import influenceinc.modelo.Verdad;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ARBOL DE CONSECUENCIAS (n-ario)
 * ===============================
 *   raiz -> sello que pusiste (Publicar, Opinion, #Publicidad, Rechazar, Denunciar)
 *        -> lo que el caso era en realidad (5 posibilidades)
 *        -> hoja: el efecto sobre la ciudad y tu agencia.
 *
 * Los SELLOS del escritorio salen de los hijos de la raiz. Por eso:
 *   - el dia 2 se INSERTA la rama #PUBLICIDAD (regla nueva) y aparece el sello;
 *   - el dia 5 (veda electoral) se ELIMINA la rama OPINION y el sello desaparece.
 */
public class ArbolConsecuencias {

    /** Nodo del arbol. clave = Veredicto o Verdad. */
    public static class Nodo {
        public final Object clave;
        public final String etiqueta;
        public final EfectoCiudad efecto;
        final List<Nodo> hijos = new ArrayList<>();
        Nodo padre;

        Nodo(Object clave, String etiqueta, EfectoCiudad efecto) {
            this.clave = clave;
            this.etiqueta = etiqueta;
            this.efecto = efecto;
        }

        public List<Nodo> getHijos() {
            return hijos;
        }

        public Nodo getPadre() {
            return padre;
        }

        public boolean esHoja() {
            return efecto != null;
        }

        Nodo hijoPor(Object k) {
            for (Nodo h : hijos) {
                if (h.clave == k) {
                    return h;
                }
            }
            return null;
        }
    }

    private final Nodo raiz = new Nodo(null, "Caso en la ventanilla", null);
    private final Map<Veredicto, Nodo> guardadas = new LinkedHashMap<>();

    public ArbolConsecuencias() {
        for (Veredicto v : Veredicto.values()) {
            Nodo rama = construirRama(v);
            rama.padre = raiz;
            raiz.hijos.add(rama);
        }
    }

    private static Nodo construirRama(Veredicto v) {
        Nodo rama = new Nodo(v, v.sello, null);
        for (Verdad t : Verdad.values()) {
            Nodo h = new Nodo(t, t.etiqueta, efecto(v, t));
            h.padre = rama;
            rama.hijos.add(h);
        }
        return rama;
    }

    /** La tabla de consecuencias (verif, conf, conv, bien, desinf, confl, pts, $, rep). */
    private static EfectoCiudad efecto(Veredicto v, Verdad t) {
        switch (v) {
            case PUBLICAR:
                switch (t) {
                    case VERDADERA: return e(4, 3, 1, 2, -2, 0, 100, 40, 3, true, false, "Información cierta y útil: la ciudad se enteró.");
                    case FALSA: return e(-6, -8, -6, -4, 12, 6, -150, 0, -12, false, true, "Publicaste un bulo con el sello de la agencia.");
                    case OPINION: return e(-1, -2, -3, 0, 3, 3, -40, 0, -3, false, false, "Presentaste una opinión como si fuera un hecho.");
                    case PUBLICIDAD_OCULTA: return e(0, -4, 0, -1, 2, 0, -60, 60, -5, false, false, "Publicidad encubierta: la audiencia no sabía que era pagado.");
                    default: return e(-8, -10, -8, -5, 15, 8, -200, 0, -15, false, true, "Le diste tu sello a un impostor.");
                }
            case OPINION:
                switch (t) {
                    case VERDADERA: return e(1, 0, 0, 0, 0, 0, 20, 10, 0, false, false, "Era un hecho comprobado, no una opinión.");
                    case FALSA: return e(-3, -3, -2, 0, 5, 2, -60, 0, -4, false, false, "Un bulo con etiqueta de opinión sigue siendo un bulo.");
                    case OPINION: return e(1, 1, 2, 1, -1, -1, 100, 30, 2, true, false, "Opinión publicada como opinión: honesto.");
                    case PUBLICIDAD_OCULTA: return e(0, -2, 0, 0, 1, 0, -30, 20, -2, false, false, "Era publicidad pagada, no una opinión libre.");
                    default: return e(-4, -4, -3, -2, 6, 3, -100, 0, -6, false, true, "El impostor usó tu agencia como altavoz.");
                }
            case ETIQUETA:
                switch (t) {
                    case VERDADERA: return e(0, 0, 0, 0, 0, 0, 0, 10, -1, false, false, "Nadie te pagó por esto: la etiqueta sobraba.");
                    case FALSA: return e(-4, -5, -3, -2, 8, 3, -100, 0, -8, false, true, "Un bulo pagado sigue siendo un bulo.");
                    case OPINION: return e(0, -1, 0, 0, 0, 0, 0, 0, -1, false, false, "No era publicidad.");
                    case PUBLICIDAD_OCULTA: return e(2, 3, 1, 2, -1, 0, 100, 80, 3, true, false, "Publicidad declarada: el cliente paga y la audiencia sabe.");
                    default: return e(-5, -6, -4, -2, 10, 4, -150, 0, -10, false, true, "Le cobraste a un impostor por difundir su mentira.");
                }
            case RECHAZAR:
                switch (t) {
                    case VERDADERA: return e(-4, -2, 0, -3, 2, 0, -60, 0, -3, false, false, "Era cierta y útil: la ciudad no se enteró.");
                    case FALSA: return e(2, 2, 1, 1, -4, -1, 100, 30, 3, true, false, "Frenaste un bulo.");
                    case OPINION: return e(0, -1, -2, 0, 0, 2, -30, 0, -2, false, false, "Rechazar opiniones legítimas también es censura.");
                    case PUBLICIDAD_OCULTA: return e(0, 0, 0, 0, 0, 0, 20, 0, 0, false, false, "Perdiste un cliente: bastaba con etiquetarla.");
                    default: return e(1, 1, 0, 0, -3, 0, 50, 10, 1, false, false, "Lo frenaste, pero el impostor sigue suelto.");
                }
            default:
                switch (t) {
                    case VERDADERA: return e(-3, -5, -3, -2, 1, 2, -100, 0, -6, false, true, "Denunciaste a alguien que decía la verdad.");
                    case FALSA: return e(1, 0, -1, 0, -4, 1, 60, 10, 1, false, false, "Era una persona engañada, no una cuenta falsa.");
                    case OPINION: return e(-1, -3, -4, -1, 0, 4, -80, 0, -6, false, true, "Denunciaste una opinión: eso es silenciar.");
                    case PUBLICIDAD_OCULTA: return e(0, -1, 0, 0, 0, 1, -20, 0, -2, false, false, "Exagerado: bastaba con etiquetarla.");
                    default: return e(3, 4, 2, 2, -8, -2, 150, 60, 5, true, false, "Cayó un impostor y su red de cuentas falsas.");
                }
        }
    }

    private static EfectoCiudad e(int a, int b, int c, int d, int f, int g, int pts, int din, int rep,
                                  boolean ok, boolean grave, String txt) {
        return new EfectoCiudad(a, b, c, d, f, g, pts, din, rep, ok, grave, txt);
    }

    /** Baja raiz -> sello -> verdad. null si el sello no esta disponible. */
    public Nodo resolver(Veredicto v, Verdad t) {
        Nodo rama = raiz.hijoPor(v);
        return rama == null ? null : rama.hijoPor(t);
    }

    /** Sellos disponibles = hijos de la raiz. */
    public List<Veredicto> sellos() {
        List<Veredicto> l = new ArrayList<>();
        for (Nodo h : raiz.hijos) {
            l.add((Veredicto) h.clave);
        }
        return l;
    }

    /** ELIMINA la rama de un sello (se guarda para reinsertarla). */
    public boolean eliminar(Veredicto v) {
        Nodo rama = raiz.hijoPor(v);
        if (rama == null) {
            return false;
        }
        raiz.hijos.remove(rama);
        guardadas.put(v, rama);
        return true;
    }

    /** INSERTA (o reinserta) la rama de un sello en su lugar segun el orden del enum. */
    public boolean insertar(Veredicto v) {
        if (raiz.hijoPor(v) != null) {
            return false;
        }
        Nodo rama = guardadas.containsKey(v) ? guardadas.remove(v) : construirRama(v);
        rama.padre = raiz;
        int pos = 0;
        while (pos < raiz.hijos.size() && ((Veredicto) raiz.hijos.get(pos).clave).ordinal() < v.ordinal()) {
            pos++;
        }
        raiz.hijos.add(pos, rama);
        return true;
    }

    public Nodo getRaiz() {
        return raiz;
    }

    public String rutaHasta(Nodo hoja) {
        List<String> p = new ArrayList<>();
        for (Nodo n = hoja; n != null; n = n.padre) {
            p.add(0, n.etiqueta);
        }
        return String.join("  ->  ", p);
    }

    public int cantidadNodos() {
        return contar(raiz);
    }

    private int contar(Nodo n) {
        int c = 1;
        for (Nodo h : n.hijos) {
            c += contar(h);
        }
        return c;
    }

    public int altura() {
        return raiz.hijos.isEmpty() ? 0 : 2;
    }

    /** Preorden para la ayuda. */
    public String preorden() {
        StringBuilder sb = new StringBuilder();
        preorden(raiz, 0, sb);
        return sb.toString();
    }

    private void preorden(Nodo n, int nivel, StringBuilder sb) {
        for (int i = 0; i < nivel; i++) {
            sb.append("    ");
        }
        sb.append(nivel == 0 ? "" : "|- ").append(n.etiqueta);
        if (n.esHoja()) {
            sb.append("  =>  ").append(n.efecto.resumen());
        }
        sb.append('\n');
        for (Nodo h : n.hijos) {
            preorden(h, nivel + 1, sb);
        }
    }
}
