package alcaldedigital;

import alcaldedigital.gui.Configuracion;
import alcaldedigital.gui.PantallaJugadores;
import alcaldedigital.gui.VentanaJuego;
import alcaldedigital.logica.Partida;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Clase principal que inicializa el sistema, configura la interfaz gráfica
 * y gestiona el registro inicial de los participantes de la partida.
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
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

            Configuracion.aplicarGuardada();   // ajustes guardados (contraste, brillo...)

            mostrarPortada();
        });
    }

    /**
     * Muestra la portada; al pulsar START continúa el flujo.
     * También se usa al terminar una campaña con "Volver al inicio".
     */
    public static void mostrarPortada() {
        alcaldedigital.gui.Transicion.entrar(
                new alcaldedigital.gui.PantallaInicio(Main::pantallaDeJugadores));
    }

    /**
     * Construye el registro de jugadores. No lo muestra: lo devuelve, y la
     * transicion lo abre cuando la pantalla ya esta en negro.
     */
    private static PantallaJugadores pantallaDeJugadores() {
        return new PantallaJugadores(jugadores -> new VentanaJuego(new Partida(jugadores, 4)));
    }
}
