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
 * Clase principal que inicializa el sistema, configura la interfaz gráfica
 * y gestiona el registro inicial de los participantes de la partida.
 * 
 * @author Naty
 */
public class Main {

    /**
     * Punto de entrada de la aplicación. Configura el aspecto visual nativo
     * y lanza el flujo inicial del juego mediante Swing.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
            }

            // Configuración inicial de jugadores y lanzamiento de la ventana principal
            List<Jugador> jugadores = configurarJugadores();
            if (jugadores == null) {
                return;
            }
            
            Partida partida = new Partida(jugadores, 4);
            new VentanaJuego(partida).setVisible(true);
        });
    }

    /**
     * Gestiona los diálogos interactivos para definir la cantidad de participantes,
     * capturar sus nombres y asignarles sus respectivos roles de juego.
     */
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

        // Ciclo para registrar los datos individuales de cada jugador
        for (int i = 0; i < cantidad; i++) {
            String nombre = JOptionPane.showInputDialog(null,
                    "Nombre del jugador " + (i + 1) + ":", "Jugador " + (i + 1));
            
            if (nombre == null || nombre.trim().isEmpty()) {
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