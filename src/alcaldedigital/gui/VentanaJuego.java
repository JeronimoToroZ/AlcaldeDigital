/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package alcaldedigital.gui;

import alcaldedigital.logica.Partida;
import alcaldedigital.logica.GrafoSocial;
import alcaldedigital.logica.NodoRed;
import alcaldedigital.modelo.Accion;
import alcaldedigital.modelo.Jugador;
import alcaldedigital.modelo.Publicacion;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.SwingUtilities;

/**
 * Ventana principal del juego con minijuego de burbujas flotantes (20 burbujas),
 * control de tiempo real por jugador, Grafo Social sincronizado y diálogo final personalizado.
 *
 * @author Naty
 */
public class VentanaJuego extends JFrame {

    private final Partida partida;

    private final GrafoSocial grafoSocial = new GrafoSocial();
    private JPanel panelLienzoGrafo;
    private Timer timerPropagacionGrafo;

    private final Map<String, List<String>> grafoCiudadZonas = new HashMap<>();

    private final JLabel lblTitulo = new JLabel("ALCALDE DIGITAL");
    private final JLabel lblTurno = new JLabel();
    private final JLabel lblTiempo = new JLabel();
    private final JTextArea areaBitacora = new JTextArea();
    private final JTextArea areaMarcador = new JTextArea();
    private JPanel barraSuperior;
    private JScrollPane scrollMarcador;
    private JScrollPane scrollBitacora;
    private final PanelIndicadores panelIndicadores;
    private final PanelArbolAVL panelArbol;
    private final JTabbedPane pestanas = new JTabbedPane();

    private final Map<Jugador, Long> tiemposTotalesJugadores = new HashMap<>();
    private boolean juegoIniciado = false;
    private boolean contrasteAplicado = Tema.isAltoContraste();
    
    private final List<Accion> ordenTurnosPartida = new ArrayList<>();

    private final List<String> textosIgnorar = new ArrayList<>();
    private final List<String> textosReportar = new ArrayList<>();
    private final List<String> textosVerificar = new ArrayList<>();
    private final List<String> textosCompartir = new ArrayList<>();
    private final Random randomGlobal = new Random();

    public VentanaJuego(Partida partida) {
        this.partida = partida;
        this.panelIndicadores = new PanelIndicadores(partida.getCiudad());
        this.panelArbol = new PanelArbolAVL(partida.getFeed());

        inicializarBancosTextos();
        inicializarGrafoCiudadZonas();

        ordenTurnosPartida.add(Accion.IGNORAR);
        ordenTurnosPartida.add(Accion.REPORTAR);
        ordenTurnosPartida.add(Accion.VERIFICAR);
        ordenTurnosPartida.add(Accion.COMPARTIR);
        Collections.shuffle(ordenTurnosPartida);

        grafoSocial.inicializarMapaPrueba();
        
        if (!grafoSocial.nodos.isEmpty()) {
            for (int i = 0; i < grafoSocial.nodos.size(); i++) {
                grafoSocial.nodos.get(i).estaInfectado = (i == 0);
            }
        }

        timerPropagacionGrafo = new Timer(4000, e -> {
            propagarInfeccionGrafoGradual();
        });
        timerPropagacionGrafo.start();

        for (Jugador j : partida.getJugadores()) {
            tiemposTotalesJugadores.put(j, 0L);
        }

        setTitle("Alcalde Digital - Ciudad Nova");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(1000, 720));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        barraSuperior = construirBarraSuperior();
        add(barraSuperior, BorderLayout.NORTH);

        JScrollPane scrollArbol = new JScrollPane(panelArbol);
        scrollArbol.setBorder(BorderFactory.createEmptyBorder());
        Tema.estilizarScroll(scrollArbol);

        pestanas.setUI(new Tema.PestanasUI());
        pestanas.setFocusable(false);
        pestanas.addTab("Partida", construirPestanaPartida());
        pestanas.addTab("Ciudad y jugadores", construirPestanaCiudad());
        pestanas.addTab("Árbol AVL del feed", scrollArbol);
        pestanas.addTab("Árbol de decisión", construirPestanaDecision());
        pestanas.addTab("Red Social (Grafo)", construirPestanaRedSocial());
        add(pestanas, BorderLayout.CENTER);

        aplicarTema();

        // El modo alto contraste ahora se cambia desde el botón de Ajustes:
        // cuando cambie, se vuelve a pintar esta ventana con el tema nuevo.
        Configuracion.agregarOyente(() -> {
            if (contrasteAplicado != Tema.isAltoContraste()) {
                aplicarTema();
            }
        });
    }

    private void inicializarGrafoCiudadZonas() {
        grafoCiudadZonas.put("Alcaldía", java.util.Arrays.asList("Plaza Central", "Barrio Norte"));
        grafoCiudadZonas.put("Plaza Central", java.util.Arrays.asList("Alcaldía", "Parque Municipal", "Barrio Sur"));
        grafoCiudadZonas.put("Parque Municipal", java.util.Arrays.asList("Plaza Central", "Colegio"));
        grafoCiudadZonas.put("Colegio", java.util.Arrays.asList("Parque Municipal", "Barrio Norte"));
        grafoCiudadZonas.put("Barrio Norte", java.util.Arrays.asList("Alcaldía", "Colegio"));
        grafoCiudadZonas.put("Barrio Sur", java.util.Arrays.asList("Plaza Central"));
    }

    private void inicializarBancosTextos() {
        textosIgnorar.add("Creo que debería perder el candidato Perez.");
        textosIgnorar.add("Para mí, la mejor propuesta es la de la candidata Ramirez.");
        textosIgnorar.add("Dicen que van a subir los impuestos apenas pasen las elecciones.");
        textosIgnorar.add("Supuestamente van a eliminar el subsidio de agua después de las elecciones.");
        textosIgnorar.add("Creo que deberían hacer menos debates entre candidatos.");

        textosReportar.add("El alcalde actual está robando dinero de la ciudad.");
        textosReportar.add("El funcionario público está robando los fondos destinados a la salud.");
        textosReportar.add("Se descubrió un esquema de sobornos ilegales en la alcaldía.");
        textosReportar.add("Demandan a contratista por desviar recursos públicos de las obras.");
        textosReportar.add("Confirman desvío ilegal de fondos del presupuesto municipal.");

        textosVerificar.add("Comunicado Oficial: Las urnas de votación cerrarán a mediodía por orden de la alcaldía.");
        textosVerificar.add("Información Oficial: Se confirma por la alcaldía que el voto electrónico será obligatorio únicamente para los mayores de 60 años.");
        textosVerificar.add("Fuente Oficial: Las mesas de votación abrirán a las 8:00 a.m. en todos los puestos autorizados.");
        textosVerificar.add("Información Oficial: Inicia la distribución oficial de tarjetones electorales en las zonas norte y sur.");
        textosVerificar.add("Fuente Oficial: El boletín de la Registraduría confirma el cierre de mesas a las 4:00 p.m.");

        textosCompartir.add("Atención comunidad: El puesto de salud central estará abierto todo el día brindando atención prioritaria. ¡Pasen la voz!");
        textosCompartir.add("Noticia de Última hora: ya pueden revisar la publicación oficial de la ciudad con los resultados consolidados y validados.");
        textosCompartir.add("¡Buenos días vecinos! Ya habilitaron la nueva ruta de transporte público gratuito hacia los centros de votación. ¡Compártanlo con su familia!");
        textosCompartir.add("¡Entérate! La alcaldía habilitó una plataforma digital gratuita para consultar tu puesto de votación en segundos. ¡Difunde esta información!");
        textosCompartir.add("Reporte de última hora: Las vías principales del centro estarán despejadas y con paso libre para facilitar la movilidad electoral. ¡Compártanlo en sus grupos!");
    }

    private void propagarInfeccionGrafoGradual() {
        if (grafoSocial == null || grafoSocial.nodos.isEmpty()) return;

        List<NodoRed> vecinosSanosDisponibles = new ArrayList<>();

        for (NodoRed nodo : grafoSocial.nodos) {
            if (nodo.estaInfectado) {
                for (NodoRed vecino : nodo.vecinos) {
                    if (!vecino.estaInfectado && !vecinosSanosDisponibles.contains(vecino)) {
                        vecinosSanosDisponibles.add(vecino);
                    }
                }
            }
        }

        if (!vecinosSanosDisponibles.isEmpty()) {
            Random rand = new Random();
            NodoRed elegidoParaContagio = vecinosSanosDisponibles.get(rand.nextInt(vecinosSanosDisponibles.size()));
            elegidoParaContagio.estaInfectado = true;
        }

        if (panelLienzoGrafo != null) {
            panelLienzoGrafo.repaint();
        }
    }

    private void impactarDecisionEnGrafoSocial(boolean exito) {
        if (exito && grafoSocial != null && !grafoSocial.nodos.isEmpty()) {
            for (NodoRed nodo : grafoSocial.nodos) {
                if (nodo.estaInfectado) {
                    nodo.estaInfectado = false;
                    break;
                }
            }
            if (panelLienzoGrafo != null) {
                panelLienzoGrafo.repaint();
            }

            if (timerPropagacionGrafo != null) {
                timerPropagacionGrafo.stop();
                javax.swing.Timer pausaInfeccion = new javax.swing.Timer(10000, evt -> {
                    if (timerPropagacionGrafo != null && !timerPropagacionGrafo.isRunning()) {
                        timerPropagacionGrafo.start();
                    }
                });
                pausaInfeccion.setRepeats(false);
                pausaInfeccion.start();
            }
        }
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b && !juegoIniciado) {
            juegoIniciado = true;
            mostrarInstruccionesIniciales();
        }
    }

    private void mostrarInstruccionesIniciales() {
        Tema.instalarUI();
        DialogoInstrucciones.instruccionesIniciales(this);

        cargarTurno();
        Configuracion.instalarBrillo(this);
    }

    private JPanel construirBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        izquierda.setOpaque(false);
        izquierda.add(lblTitulo);
        izquierda.add(lblTurno);

        // Botón Ayuda: abre las instrucciones del minijuego (antes era una pestaña)
        BotonImagen btnAyuda = new BotonImagen("/alcaldedigital/recursos/ayuda.png", "Ayuda", true);
        btnAyuda.setToolTipText("Ayuda");
        btnAyuda.setPreferredSize(new Dimension(46, btnAyuda.altoParaAncho(46)));
        btnAyuda.addActionListener(e -> DialogoInstrucciones.ayuda(this));

        // Botón Ajustes: modo de juego (normal / alto contraste), brillo y música
        BotonImagen btnAjustes = new BotonImagen("/alcaldedigital/recursos/ajuste.png", "Ajustes", true);
        btnAjustes.setToolTipText("Ajustes");
        btnAjustes.setPreferredSize(new Dimension(46, btnAjustes.altoParaAncho(46)));
        btnAjustes.addActionListener(e -> new DialogoAjustes(this).setVisible(true));

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        derecha.setOpaque(false);
        derecha.add(lblTiempo);
        derecha.add(javax.swing.Box.createHorizontalStrut(6));
        derecha.add(btnAyuda);
        derecha.add(btnAjustes);

        barra.add(izquierda, BorderLayout.WEST);
        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private JPanel construirPestanaPartida() {
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        PanelMinijuegoBurbujas panelJuego = new PanelMinijuegoBurbujas(this);

        areaBitacora.setEditable(false);
        areaBitacora.setLineWrap(true);
        areaBitacora.setWrapStyleWord(true);
        areaBitacora.setFont(Tema.mono());
        scrollBitacora = new JScrollPane(areaBitacora);
        scrollBitacora.setPreferredSize(new Dimension(400, 140));
        scrollBitacora.setBorder(bordeTitulado("Bitácora de Disparos"));
        Tema.estilizarScroll(scrollBitacora);
        scrollBitacora.setVisible(false);

        JPanel panelSurOculto = new JPanel(new BorderLayout());
        panelSurOculto.setOpaque(false);

        JButton btnToggleBitacora = new JButton("📜 Ver Bitácora de Eventos");
        btnToggleBitacora.setFocusPainted(false);
        btnToggleBitacora.setFont(Tema.indicadorNegrita());
        btnToggleBitacora.addActionListener(e -> {
            boolean visible = scrollBitacora.isVisible();
            scrollBitacora.setVisible(!visible);
            btnToggleBitacora.setText(visible ? "📜 Ver Bitácora de Eventos" : "📜 Ocultar Bitácora");
            raiz.revalidate();
            raiz.repaint();
        });

        JPanel panelBotonEsquina = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotonEsquina.setOpaque(false);
        panelBotonEsquina.add(btnToggleBitacora);

        panelSurOculto.add(panelBotonEsquina, BorderLayout.NORTH);
        panelSurOculto.add(scrollBitacora, BorderLayout.SOUTH);

        raiz.add(panelJuego, BorderLayout.CENTER);
        raiz.add(panelSurOculto, BorderLayout.SOUTH);
        return raiz;
    }

    class PanelMinijuegoBurbujas extends JPanel {
        private Timer gameTimer;
        private Timer timerCambioIconos;
        private int faseJuego = 0; 
        private int contadorLectura = 5; 
        private Timer timerLectura;
        
        private List<Burbuja> burbujasActivas = new ArrayList<>();
        private int burbujasExplotadasCorrectas = 0;
        private final int OBJETIVO_BURBUJAS = 8;
        private Accion accionCorrectaRonda;
        private VentanaJuego ventanaPadre;
        private boolean turnoResuelto = false;
        private long tiempoInicioAccion;

        private java.awt.Image imgFondo;
        private final Map<Accion, java.awt.Image> imgAcciones = new HashMap<>();
        private final List<java.awt.Image> imgPosts = new ArrayList<>();

        public PanelMinijuegoBurbujas(VentanaJuego padre) {
            this.ventanaPadre = padre;
            setOpaque(true);
            setFocusable(true);

            try {
                imgFondo = new javax.swing.ImageIcon(getClass().getResource("/alcaldedigital/recursos/fondo_transparente.png")).getImage();
                
                imgAcciones.put(Accion.VERIFICAR, new javax.swing.ImageIcon(getClass().getResource("/alcaldedigital/recursos/verificar.png")).getImage());
                imgAcciones.put(Accion.REPORTAR, new javax.swing.ImageIcon(getClass().getResource("/alcaldedigital/recursos/reportar.png")).getImage());
                imgAcciones.put(Accion.COMPARTIR, new javax.swing.ImageIcon(getClass().getResource("/alcaldedigital/recursos/compartir.png")).getImage());
                imgAcciones.put(Accion.IGNORAR, new javax.swing.ImageIcon(getClass().getResource("/alcaldedigital/recursos/ignorar.png")).getImage());

                imgPosts.add(new javax.swing.ImageIcon(getClass().getResource("/alcaldedigital/recursos/instagram_post1.png")).getImage());
                imgPosts.add(new javax.swing.ImageIcon(getClass().getResource("/alcaldedigital/recursos/instagram_post2.png")).getImage());
                imgPosts.add(new javax.swing.ImageIcon(getClass().getResource("/alcaldedigital/recursos/instagram_post3.png")).getImage());
                imgPosts.add(new javax.swing.ImageIcon(getClass().getResource("/alcaldedigital/recursos/instagram_post4.png")).getImage());
            } catch (Exception e) {}

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (faseJuego != 1 || turnoResuelto) return;

                    int mouseX = e.getX();
                    int mouseY = e.getY();

                    for (int i = burbujasActivas.size() - 1; i >= 0; i--) {
                        Burbuja b = burbujasActivas.get(i);
                        double distancia = Math.sqrt(Math.pow(mouseX - b.x, 2) + Math.pow(mouseY - b.y, 2));
                        if (distancia <= 52) { 
                            long milisegundosTardados = System.currentTimeMillis() - tiempoInicioAccion;
                            if (b.accion == accionCorrectaRonda) {
                                burbujasExplotadasCorrectas++;
                                burbujasActivas.remove(i);

                                if (burbujasExplotadasCorrectas >= OBJETIVO_BURBUJAS) {
                                    turnoResuelto = true;
                                    detenerTimers();
                                    ventanaPadre.resolverTurnoBurbujas(accionCorrectaRonda, true, milisegundosTardados);
                                }
                            } else {
                                turnoResuelto = true;
                                detengorTimersYPenalizar(b.accion, milisegundosTardados);
                            }
                            repaint();
                            break;
                        }
                    }
                }
            });
        }

        public void iniciarRonda() {
            faseJuego = 0;
            contadorLectura = 5; 
            burbujasActivas.clear();
            burbujasExplotadasCorrectas = 0;
            turnoResuelto = false;

            Publicacion p = partida.publicacionActual();
            
            int turnoActualIndice = partida.getTurno() - 1;
            if (turnoActualIndice >= 0 && turnoActualIndice < ordenTurnosPartida.size()) {
                accionCorrectaRonda = ordenTurnosPartida.get(turnoActualIndice);
                adaptarPublicacionConFraseExacta(p, accionCorrectaRonda);
            } else {
                accionCorrectaRonda = determinarAccionIdeal(p);
            }

            if (timerLectura != null) timerLectura.stop();
            detenerTimers();

            timerLectura = new Timer(1000, e -> {
                contadorLectura--;
                if (contadorLectura <= 0) {
                    timerLectura.stop();
                    faseJuego = 1; 
                    tiempoInicioAccion = System.currentTimeMillis();
                    iniciarMotorBurbujas();
                }
                repaint();
            });
            timerLectura.start();
            
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        }

        private void adaptarPublicacionConFraseExacta(Publicacion p, Accion accionEsperada) {
            try {
                java.lang.reflect.Field campoTexto = Publicacion.class.getDeclaredField("texto");
                campoTexto.setAccessible(true);
                java.lang.reflect.Field campoCred = Publicacion.class.getDeclaredField("credibilidad");
                campoCred.setAccessible(true);

                String fraseSeleccionada = "";
                int credibilidadAsignada = 50;

                switch (accionEsperada) {
                    case IGNORAR:
                        fraseSeleccionada = textosIgnorar.get(randomGlobal.nextInt(textosIgnorar.size()));
                        credibilidadAsignada = 40;
                        break;
                    case REPORTAR:
                        fraseSeleccionada = textosReportar.get(randomGlobal.nextInt(textosReportar.size()));
                        credibilidadAsignada = 15;
                        break;
                    case VERIFICAR:
                        fraseSeleccionada = textosVerificar.get(randomGlobal.nextInt(textosVerificar.size()));
                        credibilidadAsignada = 55;
                        break;
                    case COMPARTIR:
                        fraseSeleccionada = textosCompartir.get(randomGlobal.nextInt(textosCompartir.size()));
                        credibilidadAsignada = 92;
                        break;
                }

                campoTexto.set(p, fraseSeleccionada);
                campoCred.setInt(p, credibilidadAsignada);
            } catch (Exception e) {}
        }

        private Accion determinarAccionIdeal(Publicacion p) {
            String texto = p.getTexto().toLowerCase();
            for (String f : textosReportar) if (texto.equals(f.toLowerCase())) return Accion.REPORTAR;
            for (String f : textosIgnorar) if (texto.equals(f.toLowerCase())) return Accion.IGNORAR;
            for (String f : textosCompartir) if (texto.equals(f.toLowerCase())) return Accion.COMPARTIR;
            for (String f : textosVerificar) if (texto.equals(f.toLowerCase())) return Accion.VERIFICAR;

            int cred = p.getCredibilidad();
            if (cred < 30) return Accion.REPORTAR;
            if (cred >= 80) return Accion.COMPARTIR;
            if (cred >= 50) return Accion.VERIFICAR;
            return Accion.IGNORAR;
        }

        private void iniciarMotorBurbujas() {
            Random rand = new Random();

            gameTimer = new Timer(25, e -> {
                if (faseJuego == 1 && !turnoResuelto) {
                    int w = getWidth();
                    int h = getHeight();
                    if (w < 450 || h < 450) return;

                    int postAncho = 450;
                    int postAlto = 450;
                    int postX = (w / 2) - (postAncho / 2);
                    int postY = (h / 2) - (postAlto / 2);

                    if (burbujasActivas.size() < 20 && rand.nextInt(100) < 55) {
                        int x, y;
                        int intentos = 0;
                        do {
                            x = rand.nextInt(w - 100) + 50;
                            y = rand.nextInt(h - 100) + 50;
                            intentos++;
                        } while (x >= postX - 55 && x <= postX + postAncho + 55 && 
                                 y >= postY - 55 && y <= postY + postAlto + 55 && intentos < 35);

                        Accion[] otrasAcciones = {Accion.REPORTAR, Accion.VERIFICAR, Accion.COMPARTIR, Accion.IGNORAR};
                        Accion colorAsignado = (rand.nextInt(100) < 35) ? accionCorrectaRonda : otrasAcciones[rand.nextInt(otrasAcciones.length)];

                        burbujasActivas.add(new Burbuja(x, y, colorAsignado));
                    }

                    for (Burbuja b : burbujasActivas) {
                        b.x += b.vx;
                        b.y += b.vy;

                        if (b.x < 50) { b.x = 50; b.vx *= -1; }
                        if (b.x > w - 50) { b.x = w - 50; b.vx *= -1; }
                        if (b.y < 50) { b.y = 50; b.vy *= -1; }
                        if (b.y > h - 50) { b.y = h - 50; b.vy *= -1; }

                        if (b.x >= postX - 55 && b.x <= postX + postAncho + 55 &&
                            b.y >= postY - 55 && b.y <= postY + postAlto + 55) {
                            b.vx *= -1;
                            b.vy *= -1;
                            b.x += b.vx * 4;
                            b.y += b.vy * 4;
                        }
                    }

                    checkBurbujaCollisions();
                    repaint();
                }
            });
            gameTimer.start();

            timerCambioIconos = new Timer(1800, e -> {
                if (faseJuego == 1 && !turnoResuelto) {
                    Accion[] otrasAcciones = {Accion.REPORTAR, Accion.VERIFICAR, Accion.COMPARTIR, Accion.IGNORAR};
                    for (Burbuja b : burbujasActivas) {
                        b.accion = (rand.nextInt(100) < 40) ? accionCorrectaRonda : otrasAcciones[rand.nextInt(otrasAcciones.length)];
                    }
                    repaint();
                }
            });
            timerCambioIconos.start();
        }

        private void checkBurbujaCollisions() {
            int diametro = 100;
            for (int i = 0; i < burbujasActivas.size(); i++) {
                for (int j = i + 1; j < burbujasActivas.size(); j++) {
                    Burbuja b1 = burbujasActivas.get(i);
                    Burbuja b2 = burbujasActivas.get(j);
                    double dist = Math.sqrt(Math.pow(b1.x - b2.x, 2) + Math.pow(b1.y - b2.y, 2));
                    if (dist < diametro) {
                        double tempVx = b1.vx;
                        double tempVy = b1.vy;
                        b1.vx = b2.vx;
                        b1.vy = b2.vy;
                        b2.vx = tempVx;
                        b2.vy = tempVy;
                        b1.x += b1.vx;
                        b1.y += b1.vy;
                        b2.x += b2.vx;
                        b2.y += b2.vy;
                    }
                }
            }
        }

        private void detenerTimers() {
            if (gameTimer != null) gameTimer.stop();
            if (timerCambioIconos != null) timerCambioIconos.stop();
        }

        private void detengorTimersYPenalizar(Accion accionEquivocada, long milisegundosTardados) {
            detenerTimers();
            ventanaPadre.resolverTurnoBurbujas(accionEquivocada, false, milisegundosTardados);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();

            if (imgFondo != null) {
                g2d.drawImage(imgFondo, 0, 0, w, h, this);
            } else {
                g2d.setColor(new Color(230, 235, 245));
                g2d.fillRect(0, 0, w, h);
            }

            int centroX = w / 2;
            int centroY = h / 2;

            int postAncho = 450;
            int postAlto = 450;
            int postX = centroX - (postAncho / 2);
            int postY = centroY - (postAlto / 2);

            if (!imgPosts.isEmpty()) {
                int indicePost = Math.abs(partida.getTurno() - 1) % imgPosts.size();
                java.awt.Image imgPostActual = imgPosts.get(indicePost);
                if (imgPostActual != null) {
                    g2d.drawImage(imgPostActual, postX, postY, postAncho, postAlto, this);
                }
            }

            Publicacion p = partida.publicacionActual();

            g2d.setColor(new Color(15, 23, 42));
            g2d.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 14)); 
            
            int textoX = postX + 115;           
            int textoY = postY + 190;          
            int textoAnchoMax = postAncho - 220; 
            drawMultilineText(g2d, p.getTexto(), textoX, textoY, textoAnchoMax);

            if (faseJuego == 0) {
                g2d.setColor(new Color(185, 28, 28));
                g2d.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20));
                String textoLectura = "🔍 ¡Analiza el Post! Las burbujas aparecen en: " + contadorLectura + "s";
                int strWidth = g2d.getFontMetrics().stringWidth(textoLectura);
                g2d.drawString(textoLectura, centroX - (strWidth / 2), postY - 20);
            } else {
                g2d.setColor(new Color(20, 83, 45));
                g2d.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
                String textoJuego = "🎯 ¡Dispara a 8 burbujas correctas! Restantes: " + (OBJETIVO_BURBUJAS - burbujasExplotadasCorrectas);
                int strWidth = g2d.getFontMetrics().stringWidth(textoJuego);
                g2d.drawString(textoJuego, centroX - (strWidth / 2), postY - 20);
                
                g2d.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
                g2d.setColor(new Color(51, 65, 85));
                g2d.drawString("🟣 Compartir   |   🟢 Verificar   |   🔵 Ignorar   |   🔴 Reportar", centroX - 180, h - 15);
            }

            for (Burbuja b : burbujasActivas) {
                java.awt.Image imgAccion = imgAcciones.get(b.accion);
                if (imgAccion != null) {
                    int diametro = 100;
                    int radio = diametro / 2;

                    g2d.setClip(new java.awt.geom.Ellipse2D.Float(b.x - radio, b.y - radio, diametro, diametro));
                    g2d.drawImage(imgAccion, b.x - radio, b.y - radio, diametro, diametro, this);
                    g2d.setClip(null);

                    g2d.setColor(new Color(255, 255, 255, 200));
                    g2d.setStroke(new BasicStroke(2.5f));
                    g2d.drawOval(b.x - radio, b.y - radio, diametro, diametro);
                }
            }
        }

        private void drawMultilineText(Graphics2D g2d, String text, int x, int y, int maxWidth) {
            java.awt.FontMetrics fm = g2d.getFontMetrics();
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int currentY = y;

            for (String word : words) {
                if (fm.stringWidth(line + word + " ") < maxWidth) {
                    line.append(word).append(" ");
                } else {
                    g2d.drawString(line.toString(), x, currentY);
                    line = new StringBuilder(word + " ");
                    currentY += fm.getHeight() + 2;
                }
            }
            g2d.drawString(line.toString(), x, currentY);
        }
    }

    static class Burbuja {
        int x, y;
        double vx, vy;
        Accion accion;

        public Burbuja(int x, int y, Accion accion) {
            this.x = x;
            this.y = y;
            this.accion = accion;
            Random r = new Random();
            this.vx = (r.nextBoolean() ? 1 : -1) * (2.0 + r.nextDouble() * 1.5);
            this.vy = (r.nextBoolean() ? 1 : -1) * (2.0 + r.nextDouble() * 1.5);
        }
    }

    private void cargarTurno() {
        if (partida.terminada() || partida.getTurno() > 4) {
            finalizar();
            return;
        }

        lblTurno.setText("Situación " + partida.getTurno() + " de 4");

        propagarInfeccionGrafoGradual();

        actualizarMarcador();
        actualizarBitacora();
        panelIndicadores.repaint();
        panelArbol.revalidate();
        panelArbol.repaint();

        Component comp = pestanas.getComponentAt(0);
        if (comp instanceof JPanel) {
            for (Component hijo : ((JPanel)comp).getComponents()) {
                if (hijo instanceof PanelMinijuegoBurbujas) {
                    ((PanelMinijuegoBurbujas) hijo).iniciarRonda();
                    break;
                } else if (hijo instanceof JPanel) {
                    for (Component nieto : ((JPanel)hijo).getComponents()) {
                        if (nieto instanceof PanelMinijuegoBurbujas) {
                            ((PanelMinijuegoBurbujas) nieto).iniciarRonda();
                            break;
                        }
                    }
                }
            }
        }

        lblTiempo.setText("Modo Puntuación por Velocidad");
    }

    private void resolverTurnoBurbujas(Accion accionElegida, boolean exitoDisparos, long milisegundosTardados) {
        Jugador jugadorActual = partida.jugadorActual();
        int situacionActual = partida.getTurno();
        
        long tiempoActual = tiemposTotalesJugadores.getOrDefault(jugadorActual, 0L);
        tiemposTotalesJugadores.put(jugadorActual, tiempoActual + milisegundosTardados);

        Partida.ResultadoTurno r = partida.ejecutar(accionElegida);

        // Limpia/desinfecta una zona del grafo si el jugador acierta
        impactarDecisionEnGrafoSocial(exitoDisparos);

        int puntosExtraVelocidad = 0;
        if (exitoDisparos) {
            puntosExtraVelocidad = Math.max(20, (int)(8000 - milisegundosTardados) / 100);
        }

        int puntosTotalesTurno = r.puntos + puntosExtraVelocidad;

        Tema.instalarUI();
        new DialogoResultado(this, jugadorActual.getNombre(), situacionActual, accionElegida, exitoDisparos,
                milisegundosTardados / 1000.0, puntosExtraVelocidad, puntosTotalesTurno,
                r.reputacion, r.efecto.getMensaje()).mostrar();

        cargarTurno();
    }

    private void actualizarMarcador() {
        StringBuilder sb = new StringBuilder();
        for (Jugador j : partida.getJugadores()) {
            sb.append(j == partida.jugadorActual() ? "> " : "  ");
            sb.append(String.format("%s - %s%n", j.getNombre(), j.getRol().getEtiqueta()));
            sb.append(String.format("   %d pts | rep %d | %d ok %d err%n",
                    j.getPuntos(), j.getReputacion(), j.getAciertos(), j.getErrores()));
        }
        areaMarcador.setText(sb.toString());
    }

    private void actualizarBitacora() {
        StringBuilder sb = new StringBuilder();
        for (String linea : partida.getBitacora()) {
            sb.append("- ").append(linea).append('\n');
        }
        areaBitacora.setText(sb.toString());
        areaBitacora.setCaretPosition(areaBitacora.getDocument().getLength());
    }

    private void finalizar() {
        if (timerPropagacionGrafo != null) {
            timerPropagacionGrafo.stop();
        }
        lblTiempo.setText("Campaña finalizada");
        
        List<Jugador> jugadoresOrdenados = new ArrayList<>(partida.getJugadores());
        jugadoresOrdenados.sort((j1, j2) -> {
            int compPuntos = Integer.compare(j2.getPuntos(), j1.getPuntos());
            if (compPuntos != 0) {
                return compPuntos;
            } else {
                long tiempo1 = tiemposTotalesJugadores.getOrDefault(j1, 0L);
                long tiempo2 = tiemposTotalesJugadores.getOrDefault(j2, 0L);
                return Long.compare(tiempo1, tiempo2);
            }
        });

        actualizarMarcador();
        actualizarBitacora();
        panelIndicadores.repaint();
        panelArbol.repaint();

        Tema.instalarUI();
        DialogoResultadosFinales.Opcion opcion =
                new DialogoResultadosFinales(this, jugadoresOrdenados, tiemposTotalesJugadores).elegir();

        if (opcion == DialogoResultadosFinales.Opcion.VOLVER_AL_INICIO) {
            dispose();
            alcaldedigital.Main.mostrarPortada();
        } else if (opcion == DialogoResultadosFinales.Opcion.SALIR) {
            System.exit(0);
        }
    }

    private JPanel construirPestanaCiudad() {
        areaMarcador.setEditable(false);
        areaMarcador.setFont(Tema.mono());
        areaMarcador.setMargin(new java.awt.Insets(10, 14, 10, 14));
        scrollMarcador = new JScrollPane(areaMarcador);
        scrollMarcador.setBorder(bordeTitulado("Jugadores"));
        Tema.estilizarScroll(scrollMarcador);

        panelIndicadores.setPreferredSize(new Dimension(400, 300));
        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.setOpaque(false);
        izquierda.add(panelIndicadores, BorderLayout.NORTH);

        JPanel raiz = new JPanel(new BorderLayout(16, 16));
        raiz.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        raiz.add(izquierda, BorderLayout.WEST);
        raiz.add(scrollMarcador, BorderLayout.CENTER);
        return raiz;
    }

    private JPanel construirPestanaDecision() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Tema.mono());
        area.setMargin(new java.awt.Insets(14, 16, 14, 16));
        area.setText("ÁRBOL DE DECISIÓN (recorrido en PREORDEN)\n"
                + "Nodos totales: " + partida.getArbolDecision().cantidadNodos() + "\n\n"
                + partida.getArbolDecision().recorridoPreorden());
        area.setCaretPosition(0);

        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createEmptyBorder());
        Tema.estilizarScroll(sp);

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel construirPestanaRedSocial() {
        PanelConFondo panelRed = new PanelConFondo(new BorderLayout(), "/alcaldedigital/recursos/mapa_ciudad.jpg");
        panelRed.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        
        panelLienzoGrafo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (w <= 0 || h <= 0) return;

                double scaleX = (double) w / 1000.0;
                double scaleY = (double) h / 800.0;

                g2d.setColor(new Color(90, 50, 20, 200));
                g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                
                for (NodoRed nodo : grafoSocial.nodos) {
                    int x1 = (int) (nodo.x * scaleX);
                    int y1 = (int) (nodo.y * scaleY);
                    for (NodoRed vecino : nodo.vecinos) {
                        int x2 = (int) (vecino.x * scaleX);
                        int y2 = (int) (vecino.y * scaleY);
                        g2d.drawLine(x1, y1, x2, y2);
                    }
                }

                for (NodoRed nodo : grafoSocial.nodos) {
                    int x = (int) (nodo.x * scaleX);
                    int y = (int) (nodo.y * scaleY);
                    
                    if (nodo.estaInfectado) {
                        g2d.setColor(new Color(220, 50, 50, 230)); 
                    } else {
                        g2d.setColor(new Color(40, 120, 220, 220)); 
                    }
                    
                    g2d.fillOval(x - 16, y - 16, 32, 32);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2.5f));
                    g2d.drawOval(x - 16, y - 16, 32, 32);
                }
            }
        };
        panelLienzoGrafo.setOpaque(false);

        panelLienzoGrafo.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int w = panelLienzoGrafo.getWidth();
                int h = panelLienzoGrafo.getHeight();
                if (w <= 0 || h <= 0) return;

                double scaleX = (double) w / 1000.0;
                double scaleY = (double) h / 800.0;

                int mouseX = e.getX();
                int mouseY = e.getY();

                for (NodoRed nodo : grafoSocial.nodos) {
                    int nodoX = (int) (nodo.x * scaleX);
                    int nodoY = (int) (nodo.y * scaleY);

                    double distancia = Math.sqrt(Math.pow(mouseX - nodoX, 2) + Math.pow(mouseY - nodoY, 2));
                    if (distancia <= 22) {
                        nodo.estaInfectado = false; 
                        panelLienzoGrafo.repaint();
                        break;
                    }
                }
            }
        });

        panelRed.add(panelLienzoGrafo, BorderLayout.CENTER);
        return panelRed;
    }

    private static javax.swing.border.Border bordeTitulado(String titulo) {
        javax.swing.border.TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Tema.borde(), 2), "  " + titulo.toUpperCase() + "  ");
        b.setTitleColor(Tema.texto());
        b.setTitleFont(Tema.indicadorNegrita());
        return b;
    }

    private void aplicarTema() {
        contrasteAplicado = Tema.isAltoContraste();
        Tema.instalarUI();
        pintar(getContentPane());
        lblTitulo.setFont(Tema.titulo());
        lblTurno.setFont(Tema.normal());
        lblTiempo.setFont(Tema.titulo());
        lblTitulo.setForeground(Tema.texto());
        lblTurno.setForeground(Tema.textoSuave());
        lblTiempo.setForeground(Tema.texto());

        barraSuperior.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, Tema.borde()),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        lblTiempo.setOpaque(true);
        lblTiempo.setBackground(Tema.resaltado());
        lblTiempo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.borde(), 3),
                BorderFactory.createEmptyBorder(3, 12, 3, 12)));
        pestanas.revalidate();
        repaint();
    }

    private void pintar(Component c) {
        if (c instanceof JPanel || c instanceof JTabbedPane) {
            c.setBackground(Tema.fondo());
        }
        if (c instanceof JTextArea || c instanceof javax.swing.JViewport || c instanceof JScrollPane) {
            c.setBackground(Tema.panel());
        }
        if (c instanceof java.awt.Container) {
            for (Component hijo : ((java.awt.Container) c).getComponents()) {
                pintar(hijo);
            }
        }
    }
}