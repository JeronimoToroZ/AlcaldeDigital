package alcaldedigital.gui;

import alcaldedigital.modelo.Accion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Ventana con el resultado de una situación (reemplaza al JOptionPane).
 * Muestra si el jugador acertó, la burbuja que tocó (con su imagen),
 * el tiempo, el bono, los puntos, la reputación y el efecto en la ciudad.
 */
public class DialogoResultado extends DialogoJuego {

    private static final int ANCHO = 480;

    /**
     * @param jugador     nombre del jugador del turno
     * @param situacion   número de la situación (1..4)
     * @param accion      acción de la burbuja que decidió el turno
     * @param exito       true si explotó las 8 burbujas correctas
     * @param segundos    tiempo de reacción
     * @param bono        puntos extra por velocidad
     * @param puntos      puntos totales del turno (incluye el bono)
     * @param reputacion  cambio de reputación
     * @param mensaje     mensaje del efecto sobre la ciudad
     */
    public DialogoResultado(Window padre, String jugador, int situacion, Accion accion, boolean exito,
                            double segundos, int bono, int puntos, int reputacion, String mensaje) {
        super(padre, "Situación " + situacion + " de 4");

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        // --- Encabezado: imagen de la acción + veredicto ---
        Color color = exito ? VERDE : ROJO;
        Tarjeta cabecera = new Tarjeta(new BorderLayout(16, 0), color);
        ImageIcon ic = icono(accion.name().toLowerCase() + ".png", 76);
        cabecera.add(new JLabel(ic), BorderLayout.WEST);

        JPanel textos = new JPanel(new GridLayout(0, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(etiqueta(exito ? "¡Excelente puntería!" : "¡Disparo equivocado!", Font.BOLD, 22, color));
        textos.add(etiqueta(exito
                ? "Explotaste las 8 burbujas correctas."
                : "Le diste a una burbuja que no correspondía.", Font.PLAIN, 15, CREMA));
        textos.add(etiqueta(jugador + " eligió: " + accion.getEtiqueta(), Font.ITALIC, 15, CREMA_SUAVE));
        cabecera.add(textos, BorderLayout.CENTER);
        cabecera.setAlignmentX(0.5f);
        cuerpo.add(cabecera);
        cuerpo.add(espacio(12));

        // --- Cifras del turno ---
        JPanel cifras = new JPanel(new GridLayout(1, 4, 10, 0));
        cifras.setOpaque(false);
        cifras.add(cifra("Tiempo", String.format("%.2f s", segundos), CREMA));
        cifras.add(cifra("Bono", "+" + bono, bono > 0 ? ORO : CREMA_SUAVE));
        cifras.add(cifra("Puntos", (puntos >= 0 ? "+" : "") + puntos, puntos >= 0 ? VERDE : ROJO));
        cifras.add(cifra("Reputación", (reputacion >= 0 ? "+" : "") + reputacion,
                reputacion >= 0 ? VERDE : ROJO));
        cifras.setAlignmentX(0.5f);
        cuerpo.add(cifras);
        cuerpo.add(espacio(12));

        // --- Efecto en la ciudad ---
        Tarjeta efecto = new Tarjeta(new BorderLayout(0, 6));
        efecto.add(etiqueta("Efecto en la ciudad", Font.BOLD, 15, ORO), BorderLayout.NORTH);
        efecto.add(parrafo(mensaje, ANCHO - 40, 15, CREMA), BorderLayout.CENTER);
        efecto.setAlignmentX(0.5f);
        cuerpo.add(efecto);

        setContenido(cuerpo);
        agregarBoton(botonAceptar());
    }

    /** Recuadro pequeño con un valor grande y su nombre debajo. */
    private static JPanel cifra(String nombre, String valor, Color color) {
        Tarjeta t = new Tarjeta(new GridLayout(2, 1, 0, 0));
        t.setPreferredSize(new Dimension(110, 70));
        JLabel v = etiqueta(valor, Font.BOLD, 20, color);
        v.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel n = etiqueta(nombre, Font.PLAIN, 12, CREMA_SUAVE);
        n.setHorizontalAlignment(SwingConstants.CENTER);
        t.add(v);
        t.add(n);
        return t;
    }
}
