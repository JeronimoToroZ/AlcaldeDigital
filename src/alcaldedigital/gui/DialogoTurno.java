package alcaldedigital.gui;

import alcaldedigital.modelo.Jugador;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panel que aparece ANTES de cada ronda para que todos sepan a quién le toca
 * jugar. Muestra el jugador, su rol (y qué hace ese rol) y recuerda que el
 * tiempo cuenta para los puntos.
 */
public class DialogoTurno extends DialogoJuego {

    public DialogoTurno(Window padre, Jugador jugador, int nivel, int ronda, int totalRondas) {
        super(padre, "Turno de " + jugador.getNombre());

        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));

        JLabel sub = etiqueta("Nivel " + nivel + "  ·  Ronda " + ronda + " de " + totalRondas, Font.ITALIC, 17, CREMA);
        sub.setAlignmentX(0.5f);
        cuerpo.add(sub);
        cuerpo.add(espacio(12));

        Tarjeta rol = new Tarjeta(new BorderLayout(0, 4), ORO);
        rol.add(etiqueta("Rol: " + jugador.getRol().getEtiqueta(), Font.BOLD, 18, ORO), BorderLayout.NORTH);
        rol.add(parrafo(jugador.getRol().getDescripcion(), 420, 15, CREMA), BorderLayout.CENTER);
        rol.setAlignmentX(0.5f);
        cuerpo.add(rol);
        cuerpo.add(espacio(12));

        JPanel datos = new JPanel(new GridLayout(0, 1, 0, 4));
        datos.setOpaque(false);
        datos.add(etiqueta("Puntos acumulados: " + jugador.getPuntos() + "   ·   Reputación: "
                + jugador.getReputacion(), Font.PLAIN, 15, CREMA));
        datos.add(parrafo("El cronómetro arranca después de la cuenta regresiva. "
                + "Entre menos tardes en resolver las 4 publicaciones, más puntos ganas.", 420, 14, CREMA_SUAVE));
        datos.setAlignmentX(0.5f);
        cuerpo.add(datos);

        setContenido(cuerpo);
        BotonDorado listo = new BotonDorado("¡Estoy listo!", true);
        listo.addActionListener(e -> dispose());
        agregarBoton(listo);
        getRootPane().setDefaultButton(listo);
    }
}
