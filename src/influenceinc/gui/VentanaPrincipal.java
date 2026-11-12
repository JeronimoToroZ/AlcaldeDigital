package influenceinc.gui;

import influenceinc.logica.Historia;
import influenceinc.logica.Jornada;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Partida;
import influenceinc.modelo.RolJugador;
import influenceinc.red.ClienteOficina;
import influenceinc.red.ServidorOficina;
import java.awt.AlphaComposite;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * La ventana: portada, configuracion de jugadores, oficina, sala de red y
 * final. Los cambios de pantalla se hacen con una TRANSICION suave (la
 * pantalla anterior se desvanece y se aleja mientras aparece la nueva).
 */
public class VentanaPrincipal extends JFrame implements OficinaVista.Oyente {

    private final CardLayout cartas = new CardLayout();
    private final JPanel raiz = new JPanel(cartas);
    private final OficinaVista oficina = new OficinaVista(this);
    private final PantallaFinal pantallaFinal = new PantallaFinal(this::alMenu);
    private final Portada portada;
    private final Configuracion configuracion;
    private final SalaEspera sala;
    private final Cortina cortina = new Cortina();
    private String cartaActual = "menu";
    private Partida partida;
    private ServidorOficina servidor;
    private ClienteOficina cliente;
    private final List<String[]> marcador = new ArrayList<>();

    public VentanaPrincipal() {
        super("Alcalde Digital — Mesa de Verificación");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 460));
        // tamano inicial: 90 % de la pantalla (cabe en portatiles y monitores grandes)
        Rectangle pantalla;
        try {
            pantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        } catch (Exception e) {
            pantalla = new Rectangle(0, 0, 1440, 900);
        }
        setSize(Math.max(760, (int) (pantalla.width * 0.9)), Math.max(480, (int) (pantalla.height * 0.9)));
        setLocationRelativeTo(null);
        try {
            BufferedImage ic = Recursos.imagen("verificarr.png");
            if (ic != null) {
                setIconImage(ic);
            }
        } catch (Exception e) {
            // sin icono
        }

        portada = new Portada(new Portada.Acciones() {
            @Override
            public void jugarSolo() {
                configurar(Configuracion.Modo.SOLO);
            }

            @Override
            public void jugarLocal() {
                configurar(Configuracion.Modo.LOCAL);
            }

            @Override
            public void crearRed() {
                configurar(Configuracion.Modo.CREAR_RED);
            }

            @Override
            public void unirseRed() {
                configurar(Configuracion.Modo.UNIRSE_RED);
            }

            @Override
            public void salir() {
                System.exit(0);
            }
        });
        configuracion = new Configuracion(new Configuracion.AlTerminar() {
            @Override
            public void listo(Configuracion.Modo modo, List<String> nombres, List<RolJugador> roles, String ip) {
                arrancar(modo, nombres, roles, ip);
            }

            @Override
            public void volver() {
                ir("menu");
            }
        });
        sala = new SalaEspera(() -> {
            if (servidor != null) {
                servidor.iniciar(System.nanoTime(), portada.relajado);
            }
        }, () -> {
            cerrarRed();
            ir("menu");
        });

        raiz.add(portada, "menu");
        raiz.add(configuracion, "config");
        raiz.add(oficina, "oficina");
        raiz.add(pantallaFinal, "final");
        raiz.add(sala, "sala");
        setContentPane(raiz);
        setGlassPane(cortina);
        cartas.show(raiz, "menu");
    }

    // ==================================================================
    // Transiciones
    // ==================================================================

    /** Velo que dibuja la pantalla anterior desvaneciendose sobre la nueva. */
    private static class Cortina extends JComponent {
        private BufferedImage antes;
        private long inicio;
        private static final double DURACION = 0.48;
        private final Timer timer = new Timer(15, e -> repaint());

        Cortina() {
            setOpaque(false);
        }

        void empezar(BufferedImage img) {
            antes = img;
            inicio = System.nanoTime();
            setVisible(true);
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (antes == null) {
                return;
            }
            double k = (System.nanoTime() - inicio) / 1e9 / DURACION;
            if (k >= 1) {
                antes = null;
                timer.stop();
                setVisible(false);
                return;
            }
            double e = k * k * (3 - 2 * k);           // suavizado
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1 - e)));
            double z = 1 + 0.05 * e;                  // la pantalla vieja se acerca un poco al irse
            int w = getWidth(), h = getHeight();
            g.translate(w / 2.0, h / 2.0);
            g.scale(z, z);
            g.drawImage(antes, -w / 2, -h / 2, w, h, null);
            g.dispose();
        }
    }

    /** Cambia de pantalla con transicion suave. */
    private void ir(String carta) {
        if (carta.equals(cartaActual)) {
            return;
        }
        Component actual = visible();
        BufferedImage img = null;
        if (actual != null && actual.getWidth() > 0 && actual.getHeight() > 0 && isShowing()) {
            img = new BufferedImage(actual.getWidth(), actual.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            actual.paint(g);
            g.dispose();
        }
        cartaActual = carta;
        cartas.show(raiz, carta);
        Component nuevo = visible();
        if (nuevo != null) {
            nuevo.requestFocusInWindow();
        }
        if (img != null) {
            cortina.empezar(img);
        }
    }

    private Component visible() {
        for (Component c : raiz.getComponents()) {
            if (c.isVisible()) {
                return c;
            }
        }
        return null;
    }

    // ==================================================================
    // Modos
    // ==================================================================

    private void configurar(Configuracion.Modo m) {
        configuracion.preparar(m);
        ir("config");
    }

    private void arrancar(Configuracion.Modo modo, List<String> nombres, List<RolJugador> roles, String ip) {
        switch (modo) {
            case SOLO:
                empezar(new Partida(Partida.Modo.SOLO, nombres, roles, System.nanoTime(), portada.relajado));
                break;
            case LOCAL:
                empezar(new Partida(Partida.Modo.DUELO, nombres, roles, System.nanoTime(), portada.relajado));
                break;
            case CREAR_RED:
                try {
                    servidor = new ServidorOficina(ServidorOficina.PUERTO);
                    conectar("127.0.0.1", nombres.get(0), roles.get(0), true);
                } catch (IOException e) {
                    configuracionError("No se pudo abrir el puerto " + ServidorOficina.PUERTO + ": " + e.getMessage());
                    cerrarRed();
                }
                break;
            default:
                try {
                    conectar(ip, nombres.get(0), roles.get(0), false);
                } catch (IOException e) {
                    configuracionError("No se pudo conectar con " + ip + ": " + e.getMessage());
                }
                break;
        }
    }

    private void configuracionError(String s) {
        javax.swing.JOptionPane.showMessageDialog(this, s, "Alcalde Digital", javax.swing.JOptionPane.WARNING_MESSAGE);
    }

    private void empezar(Partida p) {
        partida = p;
        marcador.clear();
        oficina.setMarcadorRed(new ArrayList<String>());
        oficina.empezar(p);
        ir("oficina");
        oficina.requestFocusInWindow();
    }

    private String miIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            return "127.0.0.1";
        }
    }

    private void conectar(String host, String nombre, RolJugador rol, boolean anfitrion) throws IOException {
        final String miNombre = nombre.isEmpty() ? "Verificador" : nombre;
        final RolJugador miRol = rol;
        sala.preparar(anfitrion, miIP(), ServidorOficina.PUERTO);
        // en la sala todos ven el nombre CON el rol: "Ana (Periodista)"
        cliente = new ClienteOficina(host, ServidorOficina.PUERTO, miNombre + " (" + rol.nombre + ")",
                new ClienteOficina.Oyente() {
                    @Override
                    public void alLobby(List<String> nombres) {
                        SwingUtilities.invokeLater(() -> sala.setConectados(nombres));
                    }

                    @Override
                    public void alIniciar(long semilla, boolean rel) {
                        SwingUtilities.invokeLater(() -> empezarRed(miNombre, miRol, semilla, rel));
                    }

                    @Override
                    public void alMarcador(List<String[]> filas) {
                        SwingUtilities.invokeLater(() -> {
                            marcador.clear();
                            marcador.addAll(filas);
                            List<String> l = new ArrayList<>();
                            for (String[] f : filas) {
                                l.add(f[0] + " " + f[1] + (f[5].equals("1") ? " (terminó)" : " (día " + f[2] + ")"));
                            }
                            oficina.setMarcadorRed(l);
                            pantallaFinal.setMarcador(marcador);
                        });
                    }

                    @Override
                    public void alError(String texto) {
                        SwingUtilities.invokeLater(() -> {
                            if (partida == null) {
                                configuracionError(texto);
                                ir("menu");
                            }
                        });
                    }
                });
        partida = null;
        ir("sala");
    }

    private void empezarRed(String nombre, RolJugador rol, long semilla, boolean rel) {
        empezar(new Partida(Partida.Modo.RED, Arrays.asList(nombre), Arrays.asList(rol), semilla, rel));
    }

    private void cerrarRed() {
        if (cliente != null) {
            cliente.cerrar();
            cliente = null;
        }
        if (servidor != null) {
            servidor.cerrar();
            servidor = null;
        }
    }

    // ==================================================================
    // Oyente de la oficina
    // ==================================================================

    @Override
    public void alSellar(Jornada j, Jornada.Resultado r) {
        enviar(j, false);
    }

    @Override
    public void alTerminarDia(Jornada j) {
        boolean sigue = partida.siguienteTurno();
        enviar(j, !sigue);
        if (!sigue || partida.terminada()) {
            oficina.detener();
            pantallaFinal.mostrar(partida, marcador);
            ir("final");
        } else {
            oficina.nuevoTurno();
        }
    }

    private void enviar(Jornada j, boolean terminado) {
        if (cliente != null) {
            JugadorVerificador jv = j.jugador;
            cliente.enviarResultado(terminado ? jv.puntajeFinal() : jv.puntos, j.dia, jv.aciertos, jv.advertencias, terminado);
        }
    }

    @Override
    public void alSalir() {
        alMenu();
    }

    private void alMenu() {
        oficina.detener();
        cerrarRed();
        partida = null;
        portada.cerrarOpciones();
        ir("menu");
    }

    // ==================================================================
    // Para las pruebas y capturas
    // ==================================================================

    public OficinaVista getOficina() {
        return oficina;
    }

    public PantallaFinal getPantallaFinal() {
        return pantallaFinal;
    }

    public Configuracion getConfiguracion() {
        return configuracion;
    }

    public SalaEspera getSala() {
        return sala;
    }

    public void empezarPrueba(Partida p) {
        empezar(p);
    }

    public void mostrarFinal(Partida p) {
        pantallaFinal.mostrar(p, marcador);
        ir("final");
    }

    public void mostrarCarta(String c) {
        ir(c);
    }

    static String nombreDia(int d) {
        return Historia.TITULOS[d];
    }
}
