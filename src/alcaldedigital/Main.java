package alcaldedigital;

import alcaldedigital.gui.VentanaJuego;
import alcaldedigital.logica.Partida;
import alcaldedigital.modelo.Jugador;
import alcaldedigital.modelo.Rol;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada. Configura los jugadores (2 a 4) y lanza la ventana.
 *
 * NOTA PARA LA SEGUNDA ENTREGA: esta clase es la que se va a reemplazar por el
 * cliente de sockets. La logica ya esta aislada en Partida, asi que el servidor
 * podra instanciar una Partida y los clientes solo enviaran la Accion elegida.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
                // si falla, se usa el look and feel por defecto
            }

            List<Jugador> jugadores = configurarJugadores();
            if (jugadores == null) {
                return;
            }
            Partida partida = new Partida(jugadores, 4); // 4 publicaciones por jugador
            new VentanaJuego(partida).setVisible(true);
        });
    }

    private static List<Jugador> configurarJugadores() {
        String[] opciones = {"2 jugadores", "3 jugadores", "4 jugadores"};
        int seleccion = JOptionPane.showOptionDialog(null,
                "Bienvenido a ALCALDE DIGITAL.\nCuantos van a jugar?",
                "Ciudad Nova", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[1]);
        if (seleccion < 0) {
            return null;
        }
        int cantidad = seleccion + 2;

        Rol[] roles = Rol.values();
        List<Jugador> jugadores = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            String nombre = JOptionPane.showInputDialog(null,
                    "Nombre del jugador " + (i + 1) + ":", "Jugador " + (i + 1));
            if (nombre == null || nombre.isBlank()) {
                nombre = "Jugador " + (i + 1);
            }

            Object rolElegido = JOptionPane.showInputDialog(null,
                    "Rol para " + nombre + ":", "Elegir rol",
                    JOptionPane.QUESTION_MESSAGE, null, roles, roles[i % roles.length]);
            Rol rol = (rolElegido == null) ? roles[i % roles.length] : (Rol) rolElegido;

            jugadores.add(new Jugador(nombre.trim(), rol));
        }
        return jugadores;
    }
}
