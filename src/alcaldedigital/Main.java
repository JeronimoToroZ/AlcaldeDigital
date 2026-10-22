package alcaldedigital;

import alcaldedigital.gui.Configuracion;
import alcaldedigital.gui.PantallaInicio;
import alcaldedigital.gui.PantallaMapa;
import alcaldedigital.gui.PantallaJugadores;
import alcaldedigital.logica.Partida;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada. Flujo de pantallas:
 *   Portada (START) -> ¿Cuántos juegan? (j2, j3, j4) -> Nombre y rol de cada uno
 *   -> Mapa de niveles (grafo) -> cada nivel se juega por turnos, uno por jugador.
 *
 * @author Naty
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
                // si falla, Swing usa su aspecto por defecto
            }
            Configuracion.aplicarGuardada();   // ajustes guardados (contraste, brillo, sonido)
            mostrarPortada();
        });
    }

    /** Muestra la portada; al pulsar START se registran los jugadores. */
    public static void mostrarPortada() {
        new PantallaInicio(Main::registrarJugadores).setVisible(true);
    }

    private static void registrarJugadores() {
        new PantallaJugadores(jugadores -> {
            Partida partida = new Partida(jugadores);
            new PantallaMapa(partida, Main::mostrarPortada).setVisible(true);
        }).setVisible(true);
    }
}
