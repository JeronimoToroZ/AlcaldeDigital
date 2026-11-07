package alcaldedigital.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Timer;

/**
 * Fundido a negro entre pantallas.
 *
 * El juego son tres ventanas distintas que se cierran y se abren: portada,
 * registro de jugadores y partida. Al saltar de una a otra se veia el
 * escritorio por un instante, y ademas cada ventana tiene otro tamano, asi que
 * el salto se notaba el doble.
 *
 * El orden importa y no es el obvio:
 *
 *   1. Se oscurece la ventana actual.
 *   2. Se CREA la siguiente, ya tapada de negro, y se muestra ENCIMA.
 *   3. Recien ahi se cierra la anterior.
 *   4. Se revela la nueva.
 *
 * Cerrar la vieja antes de tener lista la nueva es lo que produce el parpadeo:
 * durante ese hueco no hay ninguna ventana y se ve el escritorio. Haciendolo
 * en este orden, la pantalla nunca queda sin nada encima.
 *
 * De paso, construir VentanaJuego toma su tiempo -carga el mapa, los ocho
 * retratos y arma el grafo- y ese trabajo ahora ocurre con la pantalla en
 * negro, en vez de a la vista.
 *
 * El velo es un panel encima del contenido y NO Window.setOpacity(): la
 * opacidad de ventana depende de que el sistema soporte translucidez y puede
 * lanzar excepcion. En una feria, con un equipo que uno no conoce, eso es un
 * crash en el primer clic.
 */
public final class Transicion {

    /** Duracion de cada mitad del fundido. */
    private static final int DURACION = 260;

    /** Milisegundos entre cuadros (unos 60 por segundo). */
    private static final int PASO = 16;

    private Transicion() {
    }

    /** Velo negro que se dibuja encima de todo el contenido de una ventana. */
    private static class Velo extends JComponent {

        private float alfa;

        Velo(float alfa) {
            this.alfa = alfa;
            setOpaque(false);
        }

        void setAlfa(float a) {
            alfa = Math.max(0f, Math.min(1f, a));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (alfa <= 0f) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0, 0, 0, (int) (alfa * 255)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /**
     * Suaviza el avance para que el fundido no sea lineal.
     * Un fundido lineal se siente mecanico; este arranca y termina despacio.
     */
    private static float suave(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
    }

    private static Velo ponerVelo(JFrame ventana, float alfaInicial) {
        Velo velo = new Velo(alfaInicial);
        ventana.setGlassPane(velo);
        velo.setVisible(true);
        return velo;
    }

    /**
     * Anima el velo de una ventana y avisa al terminar.
     *
     * @param hacia 1 para oscurecer, 0 para revelar
     */
    private static void animar(Velo velo, float desde, float hacia, Runnable alTerminar) {
        final long inicio = System.currentTimeMillis();
        Timer t = new Timer(PASO, null);
        t.addActionListener(e -> {
            float avance = (System.currentTimeMillis() - inicio) / (float) DURACION;
            if (avance >= 1f) {
                velo.setAlfa(hacia);
                t.stop();
                if (alTerminar != null) {
                    alTerminar.run();
                }
            } else {
                velo.setAlfa(desde + (hacia - desde) * suave(avance));
            }
        });
        t.start();
    }

    /**
     * Cambia de una ventana a otra con fundido.
     *
     * @param actual     la que se va; se cierra sola
     * @param siguiente  fabrica de la que entra. Se invoca con la pantalla ya
     *                   en negro, asi que puede tardar sin que se note.
     */
    public static void cambiar(JFrame actual, Supplier<JFrame> siguiente) {
        Velo velo = ponerVelo(actual, 0f);
        animar(velo, 0f, 1f, () -> {
            JFrame nueva = siguiente.get();
            Velo veloNueva = ponerVelo(nueva, 1f);

            // Pintar el fondo de negro ademas del velo. Al mapear una ventana,
            // el sistema la rellena con su color de fondo ANTES de que Swing
            // pinte nada encima: sin esto se ve un destello blanco de un cuadro
            // justo antes de que aparezca el velo.
            nueva.getContentPane().setBackground(java.awt.Color.BLACK);
            nueva.setBackground(java.awt.Color.BLACK);

            nueva.setVisible(true);          // ya tapada: no se ve aparecer
            actual.dispose();                // recien ahora se cierra la vieja

            // Un respiro antes de revelar: le da a Swing el tiempo de pintar
            // el contenido real debajo del velo. Si se revela de inmediato, se
            // alcanza a ver la ventana a medio dibujar.
            Timer espera = new Timer(90, null);
            espera.setRepeats(false);
            espera.addActionListener(ev ->
                    animar(veloNueva, 1f, 0f, () -> veloNueva.setVisible(false)));
            espera.start();
        });
    }

    /** Revela una ventana que se acaba de abrir. Para la primera del juego. */
    public static void entrar(JFrame ventana) {
        Velo velo = ponerVelo(ventana, 1f);
        ventana.setVisible(true);
        animar(velo, 1f, 0f, () -> velo.setVisible(false));
    }

    /**
     * Parpadeo corto sobre un panel, para marcar que empezo algo nuevo.
     *
     * Se usa entre una situacion y la siguiente: sin esto la publicacion se
     * reemplaza sin aviso y el jugador puede seguir disparando sin notar que
     * ya es otra. No es decoracion, es decirle "vuelve a leer".
     */
    public static void destello(JFrame ventana) {
        Velo velo = ponerVelo(ventana, 0f);
        animar(velo, 0f, 0.72f, () -> animar(velo, 0.72f, 0f, () -> velo.setVisible(false)));
    }
}
