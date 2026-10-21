package alcaldedigital;

import alcaldedigital.gui.Configuracion;
import alcaldedigital.gui.PantallaInicio;
import alcaldedigital.gui.PantallaMapa;
import alcaldedigital.gui.PantallaNombre;
import alcaldedigital.logica.Partida;
import alcaldedigital.modelo.Jugador;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada. Flujo de pantallas:
 *   Portada (START) -> Nombre del jugador -> Mapa de niveles (grafo) -> Nivel de plataformas.
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

    /** Muestra la portada; al pulsar START se pide el nombre del jugador. */
    public static void mostrarPortada() {
        new PantallaInicio(Main::pedirNombre).setVisible(true);
    }

    private static void pedirNombre() {
        new PantallaNombre(nombre -> {
            Partida partida = new Partida(new Jugador(nombre));
            new PantallaMapa(partida, Main::mostrarPortada).setVisible(true);
        }).setVisible(true);
    }
}
