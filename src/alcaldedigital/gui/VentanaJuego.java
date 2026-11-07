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
    private PanelMinijuegoBurbujas panelMinijuego;
    private final JTextArea areaAviso = new JTextArea();
    private JScrollPane panelAviso;
    private Timer timerAviso;
    private Timer timerPropagacionGrafo;

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
        setSize(1360, 880);
        setMinimumSize(new Dimension(1180, 760));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        barraSuperior = construirBarraSuperior();
        add(barraSuperior, BorderLayout.NORTH);

        // Pantalla unica. Antes esto eran cinco pestanas, y como el jugador solo
        // puede ver una a la vez, el 92% de la pantalla terminaba siendo papel
        // tapiz: la ciudad, los indicadores y la bitacora estaban escondidos
        // justo cuando mas falta hacen, que es mientras se decide.
        //
        // Ahora las tres cosas conviven: la ciudad a la izquierda (se ve el
        // rumor avanzando), la partida al centro, los indicadores a la derecha.
        // El AVL y el arbol de decision pasan a un boton, porque sirven en la
        // sustentacion y no durante el turno.
        add(construirColumnaIzquierda(), BorderLayout.WEST);
        add(construirPestanaPartida(), BorderLayout.CENTER);
        add(construirColumnaIndicadores(), BorderLayout.EAST);

        aplicarTema();

        // El modo alto contraste ahora se cambia desde el botón de Ajustes:
        // cuando cambie, se vuelve a pintar esta ventana con el tema nuevo.
        Configuracion.agregarOyente(() -> {
            if (contrasteAplicado != Tema.isAltoContraste()) {
                aplicarTema();
            }
        });
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

    /**
     * Avanza la desinformacion UNA oleada de BFS: contagia a todos los vecinos
     * sanos de los nodos ya infectados. El trabajo de grafo lo hace GrafoSocial;
     * aqui solo se dispara y se repinta.
     */
    private void propagarInfeccionGrafoGradual() {
        if (grafoSocial == null || grafoSocial.nodos.isEmpty()) return;

        List<NodoRed> nuevosInfectados = grafoSocial.expandirUnaOleada();

        if (!nuevosInfectados.isEmpty()) {
            StringBuilder sb = new StringBuilder("La desinformación llegó a: ");
            for (int i = 0; i < nuevosInfectados.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(nuevosInfectados.get(i).nombre);
            }
            partida.registrarEnBitacora(sb.toString());
            actualizarBitacora();
        }

        if (panelLienzoGrafo != null) {
            panelLienzoGrafo.repaint();
        }
    }

    /**
     * Cuando el jugador acierta, sale una campana de verificacion desde la
     * Alcaldia hacia el foco infectado mas cercano. La ruta la calcula Dijkstra
     * sobre el grafo ponderado, y se limpia el nodo del final.
     */
    private void impactarDecisionEnGrafoSocial(boolean exito) {
        if (exito && grafoSocial != null && !grafoSocial.nodos.isEmpty()) {

            NodoRed sedeCampana = grafoSocial.buscar("Alcaldía");
            if (sedeCampana == null) {
                sedeCampana = grafoSocial.nodos.get(0);
            }

            GrafoSocial.Ruta ruta = grafoSocial.rutaAlInfectadoMasCercano(sedeCampana);

            if (ruta.existe()) {
                NodoRed objetivo = ruta.camino.get(ruta.camino.size() - 1);
                objetivo.estaInfectado = false;
                partida.registrarEnBitacora("Campaña de verificación: " + ruta.toString());
            } else if (sedeCampana.estaInfectado) {
                sedeCampana.estaInfectado = false;
                partida.registrarEnBitacora("Campaña de verificación en la propia Alcaldía.");
            }
            actualizarBitacora();

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
            // invokeLater y no una llamada directa: el dialogo de
            // instrucciones es modal y bloquea el hilo de Swing hasta que lo
            // cierran. Llamandolo aqui mismo, setVisible no retornaba, y
            // cualquier animacion de entrada se quedaba congelada a medias.
            javax.swing.SwingUtilities.invokeLater(this::mostrarInstruccionesIniciales);
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

        // Las estructuras de datos ya no son pestanas: viven aqui, a un clic,
        // porque se miran en la sustentacion y no en mitad de un turno.
        Tema.Boton btnEstructuras = new Tema.Boton("VER ESTRUCTURAS", Tema::acento);
        btnEstructuras.setPreferredSize(new Dimension(190, 42));
        btnEstructuras.addActionListener(e -> abrirEstructuras());

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        derecha.setOpaque(false);
        derecha.add(btnEstructuras);
        derecha.add(javax.swing.Box.createHorizontalStrut(6));
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
        this.panelMinijuego = panelJuego;

        raiz.add(panelJuego, BorderLayout.CENTER);
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
                // fondo_transparente.png ya no se dibuja (el minijuego usa el
                // color liso del tema), pero se seguia cargando: 2,2 MB y unos
                // 136 ms de espera por una imagen que nadie ve.
                
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
                        if (distancia <= RADIO_CLIC_BURBUJA) {
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

                    // Menos burbujas a la vez: la columna del centro es mas
                    // angosta que la pantalla completa de antes, y 20 burbujas
                    // ahi dentro se ven como una sopa.
                    if (burbujasActivas.size() < 9 && rand.nextInt(100) < 40) {
                        int margen = DIAMETRO_BURBUJA / 2 + 6;
                        int x = 0;
                        int y = 0;
                        boolean hayCupo = false;

                        int yMin = FRANJA_TEXTO + margen;
                        int yMax = h - FRANJA_TEXTO - margen;
                        for (int intentos = 0; intentos < 60 && !hayCupo; intentos++) {
                            x = rand.nextInt(Math.max(1, w - margen * 2)) + margen;
                            y = rand.nextInt(Math.max(1, yMax - yMin)) + yMin;
                            if (sobreElPost(x, y)) {
                                continue;
                            }
                            hayCupo = true;
                            for (Burbuja otra : burbujasActivas) {
                                if (Math.hypot(x - otra.x, y - otra.y) < SEPARACION_BURBUJAS) {
                                    hayCupo = false;
                                    break;
                                }
                            }
                        }

                        // Si no hay sitio, NO aparece. Antes se soltaba igual
                        // encima del post o encima de otra, y se quedaba ahi
                        // vibrando: ese era el amontonamiento.
                        if (hayCupo) {
                            Accion[] otrasAcciones = {Accion.REPORTAR, Accion.VERIFICAR, Accion.COMPARTIR, Accion.IGNORAR};
                            Accion colorAsignado = (rand.nextInt(100) < 35) ? accionCorrectaRonda : otrasAcciones[rand.nextInt(otrasAcciones.length)];
                            burbujasActivas.add(new Burbuja(x, y, colorAsignado));
                        }
                    }

                    int borde = DIAMETRO_BURBUJA / 2 + 4;
                    int[] post = areaPost();

                    for (Burbuja b : burbujasActivas) {
                        b.x += b.vx;
                        b.y += b.vy;

                        if (b.x < borde) { b.x = borde; b.vx = Math.abs(b.vx); }
                        if (b.x > w - borde) { b.x = w - borde; b.vx = -Math.abs(b.vx); }
                        int topeArriba = FRANJA_TEXTO + borde;
                        int topeAbajo = h - FRANJA_TEXTO - borde;
                        if (b.y < topeArriba) { b.y = topeArriba; b.vy = Math.abs(b.vy); }
                        if (b.y > topeAbajo) { b.y = topeAbajo; b.vy = -Math.abs(b.vy); }

                        // Repelido del post: se saca por el lado MAS CERCANO,
                        // una sola vez. Antes se le invertian las dos
                        // velocidades en cada cuadro mientras estuviera dentro,
                        // asi que una burbuja atrapada vibraba en el sitio en
                        // vez de salir.
                        if (sobreElPost(b.x, b.y)) {
                            int izq = b.x - (post[0] - MARGEN_POST);
                            int der = (post[0] + post[2] + MARGEN_POST) - b.x;
                            int arr = b.y - (post[1] - MARGEN_POST);
                            int aba = (post[1] + post[2] + MARGEN_POST) - b.y;
                            int menor = Math.min(Math.min(izq, der), Math.min(arr, aba));

                            if (menor == izq) {
                                b.x = post[0] - MARGEN_POST - 1;
                                b.vx = -Math.abs(b.vx);
                            } else if (menor == der) {
                                b.x = post[0] + post[2] + MARGEN_POST + 1;
                                b.vx = Math.abs(b.vx);
                            } else if (menor == arr) {
                                b.y = post[1] - MARGEN_POST - 1;
                                b.vy = -Math.abs(b.vy);
                            } else {
                                b.y = post[1] + post[2] + MARGEN_POST + 1;
                                b.vy = Math.abs(b.vy);
                            }
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

        /** Tamano con el que se dibuja cada burbuja. */
        private static final int DIAMETRO_BURBUJA = 72;

        /** Margen libre que se deja alrededor del post para que vuelen. */
        private static final int MARGEN_POST = 14;

        /**
         * Franja de arriba y de abajo donde NO entran burbujas.
         * Ahi van el rotulo ("Revienta 8...") y la leyenda de acciones; sin
         * reservarlas, las burbujas les pasan por encima y no se leen.
         */
        private static final int FRANJA_TEXTO = 54;

        /**
         * Lado del post, adaptado al panel.
         *
         * Antes era 450 fijo. Cuando la pantalla paso a tres columnas, el
         * centro quedo en ~560 px y un post de 450 mas su zona de respeto no
         * dejaba un solo pixel libre donde pudiera aparecer una burbuja.
         *
         * Se calcula aqui y lo usan tanto el dibujo como el motor, para que no
         * puedan volver a desincronizarse.
         */
        private int ladoPost() {
            int libre = Math.min(getWidth() - DIAMETRO_BURBUJA * 2 - MARGEN_POST * 2,
                                 getHeight() - DIAMETRO_BURBUJA - MARGEN_POST * 2);
            return Math.max(240, Math.min(450, libre));
        }

        /** Rectangulo del post: {x, y, lado}. */
        private int[] areaPost() {
            int lado = ladoPost();
            return new int[]{(getWidth() - lado) / 2, (getHeight() - lado) / 2, lado};
        }

        /** Si un punto cae sobre el post (con su margen). */
        private boolean sobreElPost(int x, int y) {
            int[] p = areaPost();
            return x >= p[0] - MARGEN_POST && x <= p[0] + p[2] + MARGEN_POST
                && y >= p[1] - MARGEN_POST && y <= p[1] + p[2] + MARGEN_POST;
        }

        /**
         * Distancia a la que dos burbujas rebotan. Va aparte del diametro a
         * proposito: si rebotaran justo al tocarse, achicarlas las dejaria
         * amontonarse mas y la pantalla se veria igual de saturada.
         */
        private static final int SEPARACION_BURBUJAS = 96;

        /**
         * Radio del area clicable. Mas grande que la burbuja, para que
         * reducirle el tamano no la vuelva dificil de pegar.
         */
        private static final int RADIO_CLIC_BURBUJA = 44;

        private void checkBurbujaCollisions() {
            int diametro = SEPARACION_BURBUJAS;
            for (int i = 0; i < burbujasActivas.size(); i++) {
                for (int j = i + 1; j < burbujasActivas.size(); j++) {
                    Burbuja b1 = burbujasActivas.get(i);
                    Burbuja b2 = burbujasActivas.get(j);
                    double dist = Math.sqrt(Math.pow(b1.x - b2.x, 2) + Math.pow(b1.y - b2.y, 2));

                    // Separarlas por posicion ANTES de tocar las velocidades.
                    // Intercambiar velocidades no despega a dos burbujas que ya
                    // estan encimadas: se las intercambian una y otra vez y
                    // siguen juntas. Hay que empujarlas.
                    if (dist < diametro && dist > 0.01) {
                        double empuje = (diametro - dist) / 2.0;
                        double ux = (b1.x - b2.x) / dist;
                        double uy = (b1.y - b2.y) / dist;
                        b1.x += (int) Math.round(ux * empuje);
                        b1.y += (int) Math.round(uy * empuje);
                        b2.x -= (int) Math.round(ux * empuje);
                        b2.y -= (int) Math.round(uy * empuje);
                    } else if (dist <= 0.01) {
                        // Exactamente encima: separarlas en cualquier direccion.
                        b1.x += diametro / 2;
                        b2.x -= diametro / 2;
                    }

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

            // Fondo liso del tema, no papel tapiz. El skyline pastel que habia
            // aqui era una tercera estetica peleando con el mapa sepia y con los
            // botones, y ademas le robaba contraste al post.
            g2d.setColor(Tema.fondo());
            g2d.fillRect(0, 0, w, h);

            int centroX = w / 2;
            int centroY = h / 2;

            int[] zonaPost = areaPost();
            int postAncho = zonaPost[2];
            int postAlto = zonaPost[2];
            int postX = zonaPost[0];
            int postY = zonaPost[1];

            if (!imgPosts.isEmpty()) {
                int indicePost = Math.abs(partida.getTurno() - 1) % imgPosts.size();
                java.awt.Image imgPostActual = imgPosts.get(indicePost);
                if (imgPostActual != null) {
                    g2d.drawImage(imgPostActual, postX, postY, postAncho, postAlto, this);
                }
            }

            Publicacion p = partida.publicacionActual();

            g2d.setColor(Tema.texto());
            g2d.setFont(Tema.indicadorNegrita());
            
            int textoX = postX + 115;           
            int textoY = postY + 190;          
            int textoAnchoMax = postAncho - 220; 
            drawMultilineText(g2d, p.getTexto(), textoX, textoY, textoAnchoMax);

            // Fuentes y colores salen de Tema: antes estaban escritos a mano aqui,
            // asi que ni seguian la paleta ni respondian al modo alto contraste.
            // Los emojis tambien se fueron: Swing los pinta como cuadritos rotos.
            if (faseJuego == 0) {
                g2d.setColor(Tema.texto());
                g2d.setFont(Tema.subtitulo());
                String textoLectura = "Lee el post. Las burbujas aparecen en " + contadorLectura + "s";
                int strWidth = g2d.getFontMetrics().stringWidth(textoLectura);
                g2d.drawString(textoLectura, centroX - (strWidth / 2), 30);
            } else {
                g2d.setColor(Tema.texto());
                g2d.setFont(Tema.subtitulo());
                String textoJuego = "Revienta " + OBJETIVO_BURBUJAS + " burbujas correctas. Faltan "
                        + (OBJETIVO_BURBUJAS - burbujasExplotadasCorrectas);
                int strWidth = g2d.getFontMetrics().stringWidth(textoJuego);
                // Arriba del todo, no pegado al post: ahi lo tapaban las burbujas.
                g2d.drawString(textoJuego, centroX - (strWidth / 2), 30);
                dibujarLeyenda(g2d, centroX, h - 14);
            }

            for (Burbuja b : burbujasActivas) {
                java.awt.Image imgAccion = imgAcciones.get(b.accion);
                if (imgAccion != null) {
                    int diametro = DIAMETRO_BURBUJA;
                    int radio = diametro / 2;

                    g2d.setClip(new java.awt.geom.Ellipse2D.Float(b.x - radio, b.y - radio, diametro, diametro));
                    g2d.drawImage(imgAccion, b.x - radio, b.y - radio, diametro, diametro, this);
                    g2d.setClip(null);

                    // El aro ahora es de tinta, no blanco translucido: el blanco
                    // le ponia un brillo de caramelo que no va con el resto.
                    g2d.setColor(Tema.borde());
                    g2d.setStroke(new BasicStroke(2.5f));
                    g2d.drawOval(b.x - radio, b.y - radio, diametro, diametro);
                }
            }
        }

        /**
         * Leyenda de las cuatro acciones, dibujada con los mismos iconos que
         * llevan las burbujas. Antes era una linea de texto con circulos de
         * colores en emoji, que ademas nombraba colores equivocados.
         * Mostrar el icono de verdad evita que la leyenda y el boton se
         * desincronicen cuando alguien cambie un dibujo.
         */
        private void dibujarLeyenda(Graphics2D g2d, int centroX, int y) {
            Accion[] orden = {Accion.COMPARTIR, Accion.VERIFICAR, Accion.REPORTAR, Accion.IGNORAR};
            String[] nombres = {"Compartir", "Verificar", "Reportar", "Ignorar"};
            int icono = 22;
            int hueco = 10;

            g2d.setFont(Tema.indicador());
            java.awt.FontMetrics fm = g2d.getFontMetrics();
            int total = 0;
            for (String n : nombres) {
                total += icono + 5 + fm.stringWidth(n) + hueco * 2;
            }

            int x = centroX - total / 2;
            for (int i = 0; i < orden.length; i++) {
                java.awt.Image img = imgAcciones.get(orden[i]);
                if (img != null) {
                    g2d.drawImage(img, x, y - icono + 4, icono, icono, this);
                }
                x += icono + 5;
                g2d.setColor(Tema.textoSuave());
                g2d.drawString(nombres[i], x, y);
                x += fm.stringWidth(nombres[i]) + hueco * 2;
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

        // Un parpadeo corto al cambiar de situacion. Sin el, la publicacion se
        // reemplaza sin aviso y se puede seguir disparando sin notar que ya es
        // otra. En la primera no va: ahi acaba de entrar la ventana.
        if (partida.getTurno() > 1) {
            Transicion.destello(this);
        }

        propagarInfeccionGrafoGradual();

        actualizarMarcador();
        actualizarBitacora();
        panelIndicadores.repaint();
        panelArbol.revalidate();
        panelArbol.repaint();

        // Antes esto buscaba el minijuego caminando el arbol de componentes
        // dos niveles hacia abajo. Cualquier cambio de layout lo rompia, y de
        // hecho lo rompio. Ahora se guarda la referencia al construirlo.
        if (panelMinijuego != null) {
            panelMinijuego.iniciarRonda();
        }

        lblTiempo.setText("Puntúa por velocidad");
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

    /**
     * Columna derecha: lo que el jugador necesita mirar mientras decide.
     * Indicadores de la ciudad arriba, marcador en medio, bitacora abajo.
     * Nada de esto estaba visible durante el turno; vivia en otra pestana.
     */
    private JPanel construirColumnaIndicadores() {
        areaBitacora.setEditable(false);
        areaBitacora.setLineWrap(true);
        areaBitacora.setWrapStyleWord(true);
        areaBitacora.setFont(Tema.mono());
        scrollBitacora = new JScrollPane(areaBitacora);
        scrollBitacora.setBorder(bordeTitulado("Bitácora"));
        Tema.estilizarScroll(scrollBitacora);

        // La bitacora se queda sola en esta columna y usa todo el alto: es el
        // unico panel que de verdad crece con la partida, asi que es el que
        // aprovecha el espacio en vez de dejarlo en blanco.
        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setPreferredSize(new Dimension(300, 0));
        raiz.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, Tema.borde()),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        raiz.add(scrollBitacora, BorderLayout.CENTER);
        return raiz;
    }

    /** Abre el AVL y el arbol de decision juntos, en un dialogo con pestanas. */
    private void abrirEstructuras() {
        JTabbedPane tp = new JTabbedPane();
        tp.setUI(new Tema.PestanasUI());
        tp.setFocusable(false);

        JScrollPane scrollArbol = new JScrollPane(panelArbol);
        scrollArbol.setBorder(BorderFactory.createEmptyBorder());
        Tema.estilizarScroll(scrollArbol);

        tp.addTab("Árbol AVL del feed", scrollArbol);
        tp.addTab("Árbol de decisión", construirPestanaDecision());

        javax.swing.JDialog d = new javax.swing.JDialog(this, "Estructuras de datos", false);
        tp.setPreferredSize(new Dimension(880, 620));
        d.setContentPane(tp);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
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

    /**
     * Columna izquierda: la ciudad, siempre a la vista.
     *
     * El mapa se dibuja CONTENIDO y no estirado, porque es 16:9 y la columna no:
     * estirarlo achataba los edificios y le hacia un flaco favor a la unica
     * ilustracion buena que tiene el juego.
     *
     * Los nodos se posicionan contra el rectangulo del mapa, no contra el panel.
     * Si se posicionaran contra el panel, las caras quedarian corridas respecto
     * a los rotulos (Parque, Hospital, Alcaldia...) apenas aparece una franja.
     */
    private PanelConFondo construirColumnaCiudad() {
        final PanelConFondo panelRed = new PanelConFondo(
                new BorderLayout(), "/alcaldedigital/recursos/mapa_ciudad.jpg",
                PanelConFondo.Ajuste.CONTENER);
        // 410 y no 470: el minijuego necesita ancho para que las burbujas
        // tengan por donde volar alrededor del post.
        panelRed.setPreferredSize(new Dimension(410, 0));
        // Sin borde propio: el filete que separa la columna lo pone el panel
        // que la contiene. Si los dos lo ponen, el lienzo donde se dibujan los
        // nodos queda 4 px corrido respecto a donde se detectan los clics.

        panelLienzoGrafo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Contra el tamano de ESTE panel, no el del padre.
                int[] a = panelRed.areaDeLaImagenEn(getWidth(), getHeight());
                if (a == null) {
                    return;
                }
                Graphics2D g2d = (Graphics2D) g;
                Tema.suavizar(g2d);

                g2d.setColor(new Color(90, 50, 20, 200));
                g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                for (NodoRed nodo : grafoSocial.nodos) {
                    int x1 = aX(a, nodo);
                    int y1 = aY(a, nodo);
                    for (NodoRed vecino : nodo.vecinos) {
                        g2d.drawLine(x1, y1, aX(a, vecino), aY(a, vecino));
                    }
                }

                // Los nodos son personas: cara, nombre y estado.
                // El radio se ata al ancho del mapa para que la cara no se
                // vuelva una pelotica cuando la columna es angosta.
                int radio = radioDeLaCara(a);
                for (NodoRed nodo : grafoSocial.nodos) {
                    RetratoCiudadano.dibujar(g2d, nodo, aX(a, nodo), aY(a, nodo), radio);
                }


            }
        };
        panelLienzoGrafo.setOpaque(false);

        panelLienzoGrafo.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int[] a = panelRed.areaDeLaImagenEn(
                        panelLienzoGrafo.getWidth(), panelLienzoGrafo.getHeight());
                if (a == null) {
                    return;
                }
                // El mas cercano, no el primero que caiga dentro del radio.
                // Con dos caras juntas, "el primero" es el que este antes en la
                // lista, que no tiene nada que ver con donde se hizo clic: por
                // eso parecia que desmentia al vecino.
                // Un poco mas generoso que la cara dibujada: acertarle a un
                // circulo de 24 px con el mouse es incomodo, y como ya se elige
                // el MAS cercano, ampliar no genera ambiguedad.
                int radio = radioDeLaCara(a) + 10;
                NodoRed elegido = null;
                double mejor = Double.MAX_VALUE;
                for (NodoRed nodo : grafoSocial.nodos) {
                    double d = Math.hypot(e.getX() - aX(a, nodo), e.getY() - aY(a, nodo));
                    if (d <= radio && d < mejor) {
                        mejor = d;
                        elegido = nodo;
                    }
                }
                if (elegido != null) {
                    desmentirDesde(elegido);
                }
            }
        });

        panelRed.add(panelLienzoGrafo, BorderLayout.CENTER);
        return panelRed;
    }

    /**
     * La columna izquierda entera: el mapa arriba y el marcador debajo.
     *
     * El mapa es 16:9 y la columna es alta y angosta, asi que contenido deja
     * mas de la mitad de la columna vacia. En vez de dejar ese hueco, el mapa
     * se ancla arriba con su proporcion exacta y debajo van los jugadores.
     */
    private JPanel construirColumnaIzquierda() {
        PanelConFondo mapa = construirColumnaCiudad();
        int ancho = 410;
        mapa.setPreferredSize(new Dimension(ancho, (int) Math.round(ancho / 1.777) + 8));

        areaMarcador.setEditable(false);
        areaMarcador.setFont(Tema.mono());
        areaMarcador.setMargin(new java.awt.Insets(10, 14, 10, 14));
        scrollMarcador = new JScrollPane(areaMarcador);
        scrollMarcador.setBorder(bordeTitulado("Jugadores"));
        Tema.estilizarScroll(scrollMarcador);

        // El alto lo decide el panel, no yo: apretarlo a ojo fue lo que hizo
        // que las barras se montaran sobre las etiquetas.
        panelIndicadores.setPreferredSize(
                new Dimension(ancho - 14, PanelIndicadores.altoNecesario()));

        // El aviso del desmentido va DEBAJO del mapa, no encima.
        // Pintado sobre el mapa tapaba a doña Carmen y a Tatiana, que son
        // justamente las dos que estan mas abajo.
        areaAviso.setEditable(false);
        areaAviso.setLineWrap(true);
        areaAviso.setWrapStyleWord(true);
        areaAviso.setFont(Tema.indicador());
        areaAviso.setMargin(new java.awt.Insets(8, 10, 8, 10));
        panelAviso = new JScrollPane(areaAviso);
        panelAviso.setBorder(bordeTitulado("Último desmentido"));
        panelAviso.setPreferredSize(new Dimension(ancho - 14, 96));
        panelAviso.setVisible(false);
        Tema.estilizarScroll(panelAviso);

        JPanel datos = new JPanel(new BorderLayout(0, 12));
        datos.setOpaque(false);
        JPanel arriba = new JPanel(new BorderLayout(0, 12));
        arriba.setOpaque(false);
        arriba.add(panelAviso, BorderLayout.NORTH);
        arriba.add(panelIndicadores, BorderLayout.CENTER);

        datos.add(arriba, BorderLayout.NORTH);
        datos.add(scrollMarcador, BorderLayout.CENTER);

        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setPreferredSize(new Dimension(ancho, 0));
        raiz.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 4, Tema.borde()),
                BorderFactory.createEmptyBorder(0, 0, 14, 14)));
        raiz.add(mapa, BorderLayout.NORTH);
        raiz.add(datos, BorderLayout.CENTER);
        return raiz;
    }

    /**
     * Radio con el que se dibuja cada cara, atado al ancho del mapa.
     *
     * Existe como metodo para que el dibujo y la deteccion del clic usen
     * literalmente el mismo numero. Cuando estaban escritos aparte (26 al
     * dibujar, 24 al hacer clic) el area clicable no calzaba con la cara.
     */
    private static int radioDeLaCara(int[] area) {
        return Math.max(16, Math.min(26, area[2] / 19));
    }

    // Los nodos no se reparten sobre el mapa entero, sino sobre un area algo
    // mas chica. Si un nodo cae pegado al borde de abajo, su NOMBRE -que va
    // debajo de la cara- queda por fuera y se corta. Pasaba con Carmen y
    // Tatiana. Estos margenes reservan ese espacio.
    private static final double MARGEN_X = 0.045;
    private static final double MARGEN_ARRIBA = 0.05;
    private static final double MARGEN_ABAJO = 0.14;

    /**
     * Muestra u oculta el panel del aviso.
     *
     * El revalidate() no es opcional: en un BorderLayout, volver visible un
     * componente que estaba oculto no le reserva espacio hasta que se recalcula
     * el layout del contenedor. Sin esto el panel existe, tiene texto, dice que
     * es visible... y no se ve nada.
     */
    private void mostrarAviso(boolean visible) {
        panelAviso.setVisible(visible);
        java.awt.Container padre = panelAviso.getParent();
        if (padre != null) {
            padre.revalidate();
            padre.repaint();
        }
    }

    /** Pasa la x de un nodo (espacio 1000x800) al rectangulo real del mapa. */
    private static int aX(int[] area, NodoRed n) {
        double util = 1.0 - MARGEN_X * 2;
        return area[0] + (int) Math.round((MARGEN_X + n.x / 1000.0 * util) * area[2]);
    }

    /** Pasa la y de un nodo (espacio 1000x800) al rectangulo real del mapa. */
    private static int aY(int[] area, NodoRed n) {
        double util = 1.0 - MARGEN_ARRIBA - MARGEN_ABAJO;
        return area[1] + (int) Math.round((MARGEN_ARRIBA + n.y / 800.0 * util) * area[3]);
    }

    /**
     * Lanzar un desmentido desde una persona.
     *
     * Antes, hacer clic en un nodo simplemente ponia estaInfectado = false:
     * curaba gratis, a cualquiera, de a uno. Eso no ensena nada y ademas
     * contradice lo que el juego quiere mostrar.
     *
     * Ahora usa GrafoSocial.corregir(), que propaga el desmentido por BFS con
     * tope de alcance y solo limpia a quien TODAVIA no retransmitio el rumor.
     * A quien ya lo conto, el desmentido le llega tarde: sigue en rojo. Esa es
     * toda la leccion, y queda dicha por la mecanica y no por un cartel.
     */
    private void desmentirDesde(NodoRed origen) {
        // Alcance 1, no 2. Con alcance 2 el desmentido saltaba hasta gente que
        // el jugador no toco, y desde afuera parecia que el juego curaba al
        // azar. Con 1 la regla se lee sola: le hablas a alguien y a sus
        // contactos directos, y hasta ahi llega.
        GrafoSocial.ResultadoCorreccion r = grafoSocial.corregir(origen, 1);
        panelLienzoGrafo.repaint();

        // "Hablaste con X" y no "Desmentido desde X": el jugador hace clic
        // sobre una persona para hablarle, y antes el mensaje sonaba a que esa
        // persona era la que desmentia ("Desmentido desde Tatiana... se
        // retractaron: Tatiana").
        StringBuilder sb = new StringBuilder();
        sb.append("Hablaste con ").append(origen.descripcion()).append(". ");
        boolean nadie = r.corregidos.isEmpty();
        if (nadie) {
            sb.append("Nadie se retractó: ya habían pasado el rumor.");
        } else {
            sb.append("Se retractaron ").append(r.corregidos.size()).append(": ");
            for (int i = 0; i < r.corregidos.size(); i++) {
                sb.append(i > 0 ? ", " : "").append(r.corregidos.get(i).persona);
            }
            sb.append('.');
        }
        // Si no se retracto nadie, la razon ya se dijo: no repetirla.
        if (!nadie && !r.siguenDesinformados.isEmpty()) {
            sb.append(" Siguen desinformados ").append(r.siguenDesinformados.size())
              .append(" porque ya lo habían contado.");
        }
        partida.registrarEnBitacora(sb.toString());
        actualizarBitacora();

        // El mismo texto, debajo del mapa, por seis segundos.
        areaAviso.setText(sb.toString());
        areaAviso.setCaretPosition(0);
        mostrarAviso(true);
        if (timerAviso != null) {
            timerAviso.stop();
        }
        timerAviso = new Timer(6000, ev -> mostrarAviso(false));
        timerAviso.setRepeats(false);
        timerAviso.start();
        panelLienzoGrafo.repaint();
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
        lblTiempo.setFont(Tema.indicadorNegrita());
        lblTitulo.setForeground(Tema.texto());
        lblTurno.setForeground(Tema.textoSuave());
        lblTiempo.setForeground(Tema.texto());

        barraSuperior.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, Tema.borde()),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        // Este rotulo llevaba la fuente de titulo (26 px) sobre un bloque de
        // color: terminaba siendo lo mas llamativo de toda la pantalla, por
        // encima del juego. Pasa a ser una etiqueta discreta.
        lblTiempo.setFont(Tema.indicadorNegrita());
        lblTiempo.setForeground(Tema.textoSuave());
        lblTiempo.setOpaque(false);
        lblTiempo.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        revalidate();
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
