package influenceinc.gui;

import influenceinc.logica.Historia;
import influenceinc.logica.Jornada;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Partida;
import influenceinc.modelo.Rasgos;
import influenceinc.red.ClienteOficina;
import influenceinc.red.ServidorOficina;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * La ventana: menu, oficina, sala de red y final.
 */
public class VentanaPrincipal extends JFrame implements OficinaVista.Oyente {

    private final CardLayout cartas = new CardLayout();
    private final JPanel raiz = new JPanel(cartas);
    private final OficinaVista oficina = new OficinaVista(this);
    private final PantallaFinal pantallaFinal = new PantallaFinal(this::alMenu);
    private final JPanel sala = new JPanel(new BorderLayout());
    private final JLabel salaTexto = new JLabel();
    private Partida partida;
    private ServidorOficina servidor;
    private ClienteOficina cliente;
    private final List<String[]> marcador = new ArrayList<>();
    private boolean relajado;

    public VentanaPrincipal() {
        super("Influence, Inc. — Mesa de Verificación");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setSize(1440, 900);
        setLocationRelativeTo(null);
        raiz.add(new Menu(), "menu");
        raiz.add(oficina, "oficina");
        raiz.add(pantallaFinal, "final");
        raiz.add(construirSala(), "sala");
        setContentPane(raiz);
        cartas.show(raiz, "menu");
    }

    // ==================================================================
    // Menu
    // ==================================================================

    private class Menu extends JPanel {
        private final Timer anim = new Timer(40, e -> repaint());
        private final Rasgos[] caras = new Rasgos[7];
        private double t;

        Menu() {
            super(new BorderLayout());
            setBackground(Tema.fondo());
            Random r = new Random();
            for (int i = 0; i < caras.length; i++) {
                caras[i] = Rasgos.azar(r);
            }
            JPanel col = new JPanel();
            col.setOpaque(false);
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.add(Box.createVerticalStrut(300));
            agregar(col, "Jugar solo", "La historia completa: 5 días, 22 casos", Tema.acento(), e -> jugarSolo());
            agregar(col, "Duelo en este computador", "2 a 4 verificadores, mismos casos, por turnos", Tema.acento2(),
                    e -> jugarDuelo());
            agregar(col, "Crear partida en red", "Eres el anfitrión: los demás se unen con tu IP", Tema.bien(),
                    e -> crearRed());
            agregar(col, "Unirse a partida en red", "Escribe la IP del anfitrión", Tema.bien(), e -> unirseRed());
            agregar(col, "Opciones y accesibilidad", "Alto contraste, texto grande, subtítulos, modo relajado",
                    Tema.neutro(), e -> opciones());
            agregar(col, "Salir", null, Tema.mal(), e -> System.exit(0));
            JPanel centro = new JPanel(new FlowLayout(FlowLayout.CENTER));
            centro.setOpaque(false);
            centro.add(col);
            add(centro, BorderLayout.CENTER);
            anim.start();
        }

        private void agregar(JPanel col, String texto, String sub, Color c, java.awt.event.ActionListener a) {
            BotonNeon b = new BotonNeon(texto, c, 17f);
            if (sub != null) {
                b.setSubtitulo(sub);
            }
            b.setAlignmentX(0.5f);
            b.setMaximumSize(new Dimension(420, 80));
            b.setPreferredSize(new Dimension(420, sub == null ? 46 : 62));
            b.addActionListener(a);
            col.add(b);
            col.add(Box.createVerticalStrut(10));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            t += 0.04;
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, Tema.altoContraste ? Color.BLACK : new Color(14, 22, 34), 0, getHeight(),
                    Tema.altoContraste ? Color.BLACK : new Color(40, 20, 50)));
            g.fillRect(0, 0, getWidth(), getHeight());
            // la fila de visitantes desfilando al fondo
            int w = getWidth();
            for (int i = 0; i < caras.length; i++) {
                double x = ((i * 230 + t * 40) % (w + 300)) - 150;
                Caras.dibujar(g, caras[i], x, getHeight() - 170, 0.9, i % 3 == 0 ? "nervioso" : "neutral",
                        ((int) (t * 3 + i)) % 17 == 0, 0);
            }
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect(0, getHeight() - 90, w, 90);
            g.setFont(Tema.fuente(Font.BOLD, 54));
            String titulo = "INFLUENCE, INC.";
            g.setColor(Tema.acento2());
            g.drawString(titulo, (w - g.getFontMetrics().stringWidth(titulo)) / 2, 120);
            g.setFont(Tema.fuente(Font.BOLD, 22));
            g.setColor(Tema.texto());
            String sub = "MESA DE VERIFICACIÓN";
            g.drawString(sub, (w - g.getFontMetrics().stringWidth(sub)) / 2, 160);
            g.setFont(Tema.fuente(Font.ITALIC, 16));
            g.setColor(Tema.textoSuave());
            String s2 = "Semana de elecciones en Ciudad Nova. Todos quieren publicar algo. No todos dicen la verdad.";
            g.drawString(s2, (w - g.getFontMetrics().stringWidth(s2)) / 2, 200);
            String s3 = "Universidad del Norte · Estructura de Datos II · 2026";
            g.setFont(Tema.fuente(Font.PLAIN, 13));
            g.drawString(s3, (w - g.getFontMetrics().stringWidth(s3)) / 2, 240);
        }
    }

    // ==================================================================
    // Modos
    // ==================================================================

    private void jugarSolo() {
        String n = pedirTexto("¿Cómo te llamas, verificador?", "Verificador");
        if (n == null) {
            return;
        }
        empezar(new Partida(Partida.Modo.SOLO, Arrays.asList(n), System.nanoTime(), relajado));
    }

    private void jugarDuelo() {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(2, 2, 4, 1));
        if (JOptionPane.showConfirmDialog(this, sp, "¿Cuántos verificadores?", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        int k = (Integer) sp.getValue();
        JPanel p = new JPanel(new GridLayout(k, 2, 6, 6));
        JTextField[] campos = new JTextField[k];
        for (int i = 0; i < k; i++) {
            p.add(new JLabel("Jugador " + (i + 1) + ":"));
            campos[i] = new JTextField("Jugador " + (i + 1), 14);
            p.add(campos[i]);
        }
        if (JOptionPane.showConfirmDialog(this, p, "Nombres", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        List<String> nombres = new ArrayList<>();
        for (JTextField c : campos) {
            nombres.add(c.getText().trim().isEmpty() ? "Jugador" : c.getText().trim());
        }
        empezar(new Partida(Partida.Modo.DUELO, nombres, System.nanoTime(), relajado));
    }

    private void empezar(Partida p) {
        partida = p;
        marcador.clear();
        oficina.setMarcadorRed(new ArrayList<String>());
        cartas.show(raiz, "oficina");
        oficina.empezar(p);
        oficina.requestFocusInWindow();
    }

    private JComponent construirSala() {
        sala.setBackground(Tema.fondo());
        salaTexto.setFont(Tema.fuente(Font.PLAIN, 18));
        salaTexto.setForeground(Tema.texto());
        salaTexto.setHorizontalAlignment(JLabel.CENTER);
        salaTexto.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        sala.add(salaTexto, BorderLayout.CENTER);
        JPanel abajo = new JPanel();
        abajo.setOpaque(false);
        BotonNeon empezar = new BotonNeon("Empezar la partida", Tema.bien(), 18f);
        empezar.addActionListener(e -> {
            if (servidor != null) {
                servidor.iniciar(System.nanoTime(), relajado);
            }
        });
        BotonNeon salir = new BotonNeon("Cancelar", Tema.mal(), 16f);
        salir.addActionListener(e -> {
            cerrarRed();
            cartas.show(raiz, "menu");
        });
        abajo.add(empezar);
        abajo.add(salir);
        sala.add(abajo, BorderLayout.SOUTH);
        empezar.putClientProperty("anfitrion", true);
        return sala;
    }

    private String miIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            return "127.0.0.1";
        }
    }

    private void crearRed() {
        String n = pedirTexto("Tu nombre:", "Anfitrión");
        if (n == null) {
            return;
        }
        try {
            servidor = new ServidorOficina(ServidorOficina.PUERTO);
            conectar("127.0.0.1", n, true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir el puerto " + ServidorOficina.PUERTO + ": " + e.getMessage());
            cerrarRed();
        }
    }

    private void unirseRed() {
        JTextField ip = new JTextField("127.0.0.1", 14), nom = new JTextField("Verificador", 14);
        JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
        p.add(new JLabel("IP del anfitrión:"));
        p.add(ip);
        p.add(new JLabel("Tu nombre:"));
        p.add(nom);
        if (JOptionPane.showConfirmDialog(this, p, "Unirse", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            conectar(ip.getText().trim(), nom.getText().trim(), false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar: " + e.getMessage());
        }
    }

    private void conectar(String host, String nombre, boolean anfitrion) throws IOException {
        final String miNombre = nombre.isEmpty() ? "Verificador" : nombre;
        cliente = new ClienteOficina(host, ServidorOficina.PUERTO, miNombre, new ClienteOficina.Oyente() {
            @Override
            public void alLobby(List<String> nombres) {
                SwingUtilities.invokeLater(() -> salaTexto.setText("<html><center><h1>Sala de espera</h1>"
                        + (anfitrion ? "Que los demás se unan con la IP <b>" + miIP() + "</b> (puerto "
                        + ServidorOficina.PUERTO + ")<br><br>" : "Esperando a que el anfitrión empiece...<br><br>")
                        + "Conectados: <b>" + String.join(", ", nombres) + "</b></center></html>"));
            }

            @Override
            public void alIniciar(long semilla, boolean rel) {
                SwingUtilities.invokeLater(() -> empezarRed(miNombre, semilla, rel));
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
                        JOptionPane.showMessageDialog(VentanaPrincipal.this, texto);
                        cartas.show(raiz, "menu");
                    }
                });
            }
        });
        for (java.awt.Component c : ((JPanel) sala.getComponent(1)).getComponents()) {
            if (Boolean.TRUE.equals(((JComponent) c).getClientProperty("anfitrion"))) {
                c.setVisible(anfitrion);
            }
        }
        salaTexto.setText("<html><center><h1>Conectando...</h1></center></html>");
        partida = null;
        cartas.show(raiz, "sala");
    }

    private void empezarRed(String nombre, long semilla, boolean rel) {
        empezar(new Partida(Partida.Modo.RED, Arrays.asList(nombre), semilla, rel));
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

    private void opciones() {
        JCheckBox ac = new JCheckBox("Alto contraste", Tema.altoContraste);
        JCheckBox tg = new JCheckBox("Texto grande", Tema.textoGrande);
        JCheckBox so = new JCheckBox("Sonido", Tema.sonido);
        JCheckBox su = new JCheckBox("Subtítulos de sonido", Tema.subtitulos);
        JCheckBox re = new JCheckBox("Modo relajado (sin reloj: para aprender o para quien lee más despacio)", relajado);
        JPanel p = new JPanel(new GridLayout(5, 1));
        p.add(ac);
        p.add(tg);
        p.add(so);
        p.add(su);
        p.add(re);
        if (JOptionPane.showConfirmDialog(this, p, "Opciones y accesibilidad", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION) {
            Tema.altoContraste = ac.isSelected();
            Tema.textoGrande = tg.isSelected();
            Tema.sonido = so.isSelected();
            Tema.subtitulos = su.isSelected();
            relajado = re.isSelected();
            repaint();
        }
    }

    private String pedirTexto(String pregunta, String porDefecto) {
        String n = (String) JOptionPane.showInputDialog(this, pregunta, "Influence, Inc.", JOptionPane.QUESTION_MESSAGE,
                null, null, porDefecto);
        if (n == null) {
            return null;
        }
        n = n.trim();
        return n.isEmpty() ? porDefecto : n;
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
            cartas.show(raiz, "final");
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
        cartas.show(raiz, "menu");
    }

    /** Para las pruebas y capturas. */
    public OficinaVista getOficina() {
        return oficina;
    }

    public PantallaFinal getPantallaFinal() {
        return pantallaFinal;
    }

    public void empezarPrueba(Partida p) {
        empezar(p);
    }

    public void mostrarFinal(Partida p) {
        pantallaFinal.mostrar(p, marcador);
        cartas.show(raiz, "final");
    }

    static String nombreDia(int d) {
        return Historia.TITULOS[d];
    }
}
