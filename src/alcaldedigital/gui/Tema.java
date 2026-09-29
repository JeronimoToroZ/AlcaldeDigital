package alcaldedigital.gui;

import java.awt.Color;
import java.awt.Font;

/**
 * Componente inclusivo que centraliza la paleta de colores y tipografías del sistema,
 * permitiendo alternar en caliente a un modo de alto contraste para atender
 * requerimientos de accesibilidad (baja visión y daltonismo).
 * 
 * Garantiza que la información nunca dependa exclusivamente del color, 
 * combinando altos índices de contraste, patrones visuales y símbolos numéricos.
 * 
 * @author Naty
 */
public final class Tema {

    private static boolean altoContraste = false;

    private Tema() {
    }

    /**
     * Activa o desactiva globalmente el modo de alto contraste en la interfaz gráfica.
     */
    public static void setAltoContraste(boolean valor) {
        altoContraste = valor;
    }

    /**
     * Consulta si el modo de alto contraste se encuentra actualmente habilitado.
     */
    public static boolean isAltoContraste() {
        return altoContraste;
    }

    // ------------------------------------------------------------------
    // Configuración de colores para fondos, paneles y estructuras de texto
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Colores funcionales y de estado (acento, positivos y negativos)
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Definición de tipografías dinámicas adaptadas al modo activo
    // ------------------------------------------------------------------

    public static Font titulo() {
        return new Font("SansSerif", Font.BOLD, altoContraste ? 22 : 20);
    }

    public static Font subtitulo() {
        return new Font("SansSerif", Font.BOLD, altoContraste ? 16 : 15);
    }

    public static Font normal() {
        return new Font("SansSerif", Font.PLAIN, altoContraste ? 16 : 14);
    }

    /**
     * Fuente para las etiquetas de indicadores (en negrita y tamaño controlado),
     * asegurando que el modo de alto contraste no provoque desbordamientos en las barras.
     */
    public static Font indicador() {
        return new Font("SansSerif", Font.BOLD, altoContraste ? 14 : 13);
    }

    public static Font mono() {
        return new Font("Monospaced", Font.PLAIN, altoContraste ? 15 : 13);
    }
}