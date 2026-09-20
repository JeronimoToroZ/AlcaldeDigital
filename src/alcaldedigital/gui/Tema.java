package alcaldedigital.gui;

import java.awt.Color;
import java.awt.Font;

/**
 * COMPONENTE INCLUSIVO
 * ====================
 * Paleta centralizada con un modo de ALTO CONTRASTE conmutable en caliente.
 *
 * Dificultad que atiende: baja vision y daltonismo. En el modo normal la
 * informacion se apoya en color (verde = bien, rojo = mal); en alto contraste
 * se sube la relacion de contraste por encima de 7:1 sobre fondo negro y, ademas,
 * NINGUN dato depende solo del color: los indicadores llevan siempre su valor
 * numerico y un simbolo (+ / -), y las barras cambian de patron ademas de tono.
 */
public final class Tema {

    private static boolean altoContraste = false;

    private Tema() {
    }

    public static void setAltoContraste(boolean valor) {
        altoContraste = valor;
    }

    public static boolean isAltoContraste() {
        return altoContraste;
    }

    public static Color fondo() {
        return altoContraste ? Color.BLACK : new Color(24, 28, 38);
    }

    public static Color panel() {
        return altoContraste ? Color.BLACK : new Color(34, 40, 54);
    }

    public static Color texto() {
        return altoContraste ? Color.WHITE : new Color(232, 236, 245);
    }

    public static Color textoSuave() {
        return altoContraste ? Color.WHITE : new Color(158, 168, 190);
    }

    public static Color borde() {
        return altoContraste ? Color.WHITE : new Color(62, 72, 94);
    }

    public static Color acento() {
        return altoContraste ? new Color(255, 255, 0) : new Color(90, 160, 255);
    }

    public static Color positivo() {
        return altoContraste ? new Color(0, 255, 128) : new Color(74, 200, 130);
    }

    public static Color negativo() {
        return altoContraste ? new Color(255, 128, 0) : new Color(228, 92, 92);
    }

    public static Color nodo() {
        return altoContraste ? Color.BLACK : new Color(48, 58, 78);
    }

    public static Font titulo() {
        return new Font("SansSerif", Font.BOLD, altoContraste ? 22 : 20);
    }

    public static Font subtitulo() {
        return new Font("SansSerif", Font.BOLD, altoContraste ? 16 : 15);
    }

    public static Font normal() {
        return new Font("SansSerif", Font.PLAIN, altoContraste ? 16 : 14);
    }

    /** Fuente de las etiquetas de indicadores: en negrita y de tamano estable,
     *  para que el modo de alto contraste no desborde las barras. */
    public static Font indicador() {
        return new Font("SansSerif", Font.BOLD, altoContraste ? 14 : 13);
    }

    public static Font mono() {
        return new Font("Monospaced", Font.PLAIN, altoContraste ? 15 : 13);
    }
}
