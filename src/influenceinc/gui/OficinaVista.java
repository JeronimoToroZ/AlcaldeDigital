package influenceinc.gui;

import influenceinc.estructuras.GrafoCiudad;
import influenceinc.estructuras.NodoDialogo;
import influenceinc.logica.Caso;
import influenceinc.logica.CiudadEstado;
import influenceinc.logica.Historia;
import influenceinc.logica.Jornada;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Partida;
import influenceinc.modelo.Cuenta;
import influenceinc.modelo.Herramienta;
import influenceinc.modelo.Persona;
import influenceinc.modelo.Publicacion;
import influenceinc.modelo.Veredicto;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * LA OFICINA. Todo lo que ve el jugador durante un dia se dibuja aqui con
 * Java2D, sobre un lienzo logico de 1440 x 860 que se escala a la ventana.
 *
 *   ┌──────────── barra: dia, reloj, fila, advertencias, puntos ───────────┐
 *   │ VENTANILLA (visitante)  │ PUBLICACION impresa │  MONITOR (herramientas)│
 *   │ globo de dialogo        │ CREDENCIAL          │                        │
 *   │ preguntas (arbol)       │ LIBRETA de pistas   │  TV: mapa + indicadores│
 *   │                         │ SELLOS              │  noticiero             │
 *   └──────────────────────────────────────────────────────────────────────┘
 */
public class OficinaVista extends JPanel {

    public static final int W = 1440, H = 860;

    /** Lo que la ventana necesita saber. */
    public interface Oyente {
        void alSellar(Jornada j, Jornada.Resultado r);

        void alTerminarDia(Jornada j);

        void alSalir();
    }

    private enum Estado { TURNO, LLAMADA, VACIO, ENTRANDO, ATENDIENDO, SELLANDO, RESULTADO, SALIENDO, FIN_DIA }

    private enum Capa { NADA, LUPA, MANUAL, ESTRUCTURAS, PAUSA }

    // zonas de la pantalla
    private static final Rectangle VENTANA = new Rectangle(20, 52, 580, 400);
    private static final Rectangle GLOBO = new Rectangle(20, 462, 580, 108);
    private static final Rectangle OPCIONES = new Rectangle(20, 580, 580, 262);
    private static final Rectangle POST = new Rectangle(615, 52, 385, 300);
    private static final Rectangle CRED = new Rectangle(615, 362, 385, 180);
    private static final Rectangle LIBRETA = new Rectangle(615, 552, 385, 136);
    private static final Rectangle SELLOS = new Rectangle(615, 698, 385, 144);
    private static final Rectangle MONITOR = new Rectangle(1015, 52, 405, 508);
    private static final Rectangle PANTALLA = new Rectangle(1030, 112, 375, 418);
    private static final Rectangle TV = new Rectangle(1015, 570, 405, 272);

    private final Oyente oyente;
    private final Avisos avisos = new Avisos();
    private final VistaRed3D red3d = new VistaRed3D();
    private final Timer reloj;
    private final Random azar = new Random();

    private Partida partida;
    private Jornada j;
    private Estado estado = Estado.VACIO;
    private Capa capa = Capa.NADA;
    private Herramienta tab = Herramienta.PERFIL;
    private Herramienta escaneando;
    private double escaneoT;
    private int paginaManual;

    // visitante
    private Caso caso;
    private String texto = "", quienHabla = "";
    private double letras;
    private String expr = "neutral";
    private double entradaT;
    private double parpadeoT = 3;
    private double glitchT;
    private BufferedImage bufVentana;

    // llamada
    private int lineaLlamada;
    private String[] llamada = new String[0];

    // sellos
    private Veredicto selloV;
    private double selloT;
    private Jornada.Resultado ultimo;
    private final List<String> ticker = new ArrayList<>();
    private double tickerX = 0;
    private String llamadaInst;
    private List<String> marcadorRed = new ArrayList<>();

    // entrada
    private final List<Boton> botones = new ArrayList<>();
    private int mx = -1, my = -1;
    private double esc = 1, offX, offY;
    private long ultimoNs = System.nanoTime();
    private double t;

    private static class Boton {
        final Rectangle r;
        final Runnable accion;

        Boton(Rectangle r, Runnable accion) {
            this.r = r;
            this.accion = accion;
        }
    }

    public OficinaVista(Oyente oyente) {
        this.oyente = oyente;
        setBackground(Color.BLACK);
        setFocusable(true);
        setDoubleBuffered(true);
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mx = (int) ((e.getX() - offX) / esc);
                my = (int) ((e.getY() - offY) / esc);
                boolean mano = false;
                for (Boton b : botones) {
                    if (b.r.contains(mx, my)) {
                        mano = true;
                        break;
                    }
                }
                setCursor(Cursor.getPredefinedCursor(mano ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int x = (int) ((e.getX() - offX) / esc), y = (int) ((e.getY() - offY) / esc);
                for (int i = botones.size() - 1; i >= 0; i--) {
                    Boton b = botones.get(i);
                    if (b.r.contains(x, y)) {
                        b.accion.run();
                        repaint();
                        return;
                    }
                }
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                tecla(e);
            }
        });
        Sonido.setSubtitulos(avisos);
        reloj = new Timer(33, e -> repaint());
    }

    // ==================================================================
    // Ciclo del dia
    // ==================================================================

    public void empezar(Partida p) {
        this.partida = p;
        nuevoTurno();
        reloj.start();
        requestFocusInWindow();
    }

    /** Lo llama la ventana despues de alTerminarDia si la partida sigue. */
    public void nuevoTurno() {
        j = partida.iniciarJornada();
        caso = null;
        ultimo = null;
        capa = Capa.NADA;
        ticker.clear();
        ticker.add("NOTICIAS NOVA · Día " + j.dia + " de la semana electoral · " + Historia.TITULOS[j.dia]);
        if (partida.jugadores.size() > 1) {
            estado = Estado.TURNO;
        } else {
            empezarLlamada();
        }
    }

    public void detener() {
        reloj.stop();
    }

    public void setMarcadorRed(List<String> m) {
        marcadorRed = m;
    }

    private void empezarLlamada() {
        estado = Estado.LLAMADA;
        llamada = Historia.LLAMADAS[j.dia];
        lineaLlamada = 0;
        decir("Don Ramiro (teléfono)", llamada[0], "neutral");
        Sonido.tocar(Sonido.Tipo.TELEFONO);
    }

    private void avanzarLlamada() {
        if (letras < texto.length()) {
            letras = texto.length();
            return;
        }
        lineaLlamada++;
        if (lineaLlamada < llamada.length) {
            decir("Don Ramiro (teléfono)", llamada[lineaLlamada], "neutral");
        } else {
            estado = Estado.VACIO;
            texto = "";
            for (String c : j.cambiosDeReglas) {
                avisos.agregar("Cambio de reglas", c, Tema.acento2(), 0);
            }
            if (j.dia == 1) {
                avisos.agregar("Cómo se juega", "1) Haz pasar al visitante. 2) Pregúntale (teclas 1-5). "
                        + "3) Investiga en el monitor. 4) Sella abajo. M = manual.", Tema.acento(), 300);
            }
            for (Herramienta h : Herramienta.values()) {
                if (h.dia == j.dia) {
                    avisos.agregar("Herramienta nueva", h.nombre, Tema.bien(), 600);
                }
            }
            Sonido.tocar(Sonido.Tipo.TURNO);
        }
    }

    private void hacerPasar() {
        if (j.isTerminada()) {
            return;
        }
        caso = j.siguiente();
        if (caso == null) {
            j.cerrar();
            estado = Estado.FIN_DIA;
            return;
        }
        estado = Estado.ENTRANDO;
        entradaT = 0;
        tab = Herramienta.PERFIL;
        llamadaInst = null;
        ultimo = null;
        decir(caso.visitante.nombre, caso.dialogo.getRaiz().respuesta, caso.dialogo.getRaiz().expresion);
        Sonido.tocar(Sonido.Tipo.PUERTA);
    }

    private void decir(String quien, String txt, String expresion) {
        quienHabla = quien;
        texto = txt == null ? "" : txt;
        letras = 0;
        expr = Caras.normalizar(expresion);
        if ("roto".equals(expr)) {
            glitchT = 1.2;
            Sonido.tocar(Sonido.Tipo.GLITCH);
        }
    }

    private void preguntar(NodoDialogo n) {
        if (estado != Estado.ATENDIENDO && estado != Estado.ENTRANDO) {
            return;
        }
        Sonido.tocar(Sonido.Tipo.CLIC);
        j.preguntar(n);
        decir(caso.visitante.nombre, n.respuesta, n.expresion);
        if (n.pista != null) {
            avisos.agregar("Anotado en la libreta", n.pista, Tema.acento2(), 0);
        }
    }

    private void sellar(Veredicto v) {
        if (estado != Estado.ATENDIENDO || caso == null) {
            return;
        }
        selloV = v;
        selloT = 0;
        estado = Estado.SELLANDO;
    }

    private void terminarSello() {
        Jornada.Resultado r = j.sellar(selloV);
        if (r == null) {
            estado = Estado.ATENDIENDO;
            return;
        }
        ultimo = r;
        Sonido.tocar(Sonido.Tipo.SELLO);
        Sonido.tocar(r.efecto.acierto ? Sonido.Tipo.BIEN : Sonido.Tipo.MAL);
        ticker.add(0, "ÚLTIMA HORA: " + r.titular);
        // reaccion del visitante
        Persona p = caso.visitante;
        if (r.veredicto == Veredicto.DENUNCIAR && p.impostor) {
            decir(p.nombre, caso.confrontado ? "..." : caso.confesion, "roto");
        } else if (r.veredicto == Veredicto.DENUNCIAR || r.veredicto == Veredicto.RECHAZAR) {
            decir(p.nombre, p.impostor ? "Tch. Ya encontraré otra agencia." : caso.defensa,
                    p.impostor ? "enojado" : "triste");
        } else {
            decir(p.nombre, p.impostor ? "Je. Gracias, muy amable... muy amable." : "¡Gracias! Que tenga buen día.",
                    "feliz");
        }
        for (String l : r.logrosNuevos) {
            avisos.agregar("¡Logro!", l, new Color(255, 200, 60), 400);
            Sonido.tocar(Sonido.Tipo.LOGRO);
        }
        if (r.advertencia) {
            avisos.agregar("ADVERTENCIA " + j.jugador.advertencias + "/" + JugadorVerificador.MAX_ADVERTENCIAS,
                    "Error grave. Con " + JugadorVerificador.MAX_ADVERTENCIAS + " te despiden.", Tema.mal(), 200);
            Sonido.tocar(Sonido.Tipo.ALERTA);
        }
        if (r.alcance > 0) {
            Sonido.tocar(Sonido.Tipo.DIFUSION);
        }
        estado = Estado.RESULTADO;
        if (oyente != null) {
            oyente.alSellar(j, r);
        }
    }

    private void despedirVisitante() {
        estado = Estado.SALIENDO;
        entradaT = 0;
    }

    private void usarHerramienta(Herramienta h) {
        tab = h;
        if (caso == null || estado != Estado.ATENDIENDO || escaneando != null) {
            return;
        }
        if (h == Herramienta.LUPA && j.disponible(h)) {
            capa = Capa.LUPA;
        }
        if (!j.disponible(h)) {
            return;
        }
        if (j.resultados(h) == null || h == Herramienta.CORRESPONSAL) {
            escaneando = h;
            escaneoT = 0;
            Sonido.tocar(Sonido.Tipo.ESCANER);
        }
    }

    // ==================================================================
    // Teclado (todo el juego se puede jugar sin mouse)
    // ==================================================================

    private void tecla(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_ESCAPE) {
            capa = capa == Capa.NADA ? Capa.PAUSA : Capa.NADA;
            return;
        }
        if (k == KeyEvent.VK_F1 || k == KeyEvent.VK_M) {
            capa = capa == Capa.MANUAL ? Capa.NADA : Capa.MANUAL;
            return;
        }
        if (k == KeyEvent.VK_F2) {
            capa = capa == Capa.ESTRUCTURAS ? Capa.NADA : Capa.ESTRUCTURAS;
            return;
        }
        if (capa == Capa.LUPA) {
            if (k == KeyEvent.VK_X) {
                acusar();
            } else if (k == KeyEvent.VK_L || k == KeyEvent.VK_ENTER) {
                capa = Capa.NADA;
            }
            return;
        }
        if (capa != Capa.NADA) {
            if (capa == Capa.MANUAL && k == KeyEvent.VK_RIGHT) {
                paginaManual++;
            } else if (capa == Capa.MANUAL && k == KeyEvent.VK_LEFT) {
                paginaManual = Math.max(0, paginaManual - 1);
            }
            return;
        }
        if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER) {
            principal();
            return;
        }
        if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_5) {
            List<NodoDialogo> op = j.opciones();
            int i = k - KeyEvent.VK_1;
            if (caso != null && i < op.size()) {
                preguntar(op.get(i));
            }
            return;
        }
        String herr = "QWERTY";
        int hi = herr.indexOf(Character.toUpperCase(e.getKeyChar()));
        if (hi >= 0 && hi < Herramienta.values().length) {
            usarHerramienta(Herramienta.values()[hi]);
            return;
        }
        if (k == KeyEvent.VK_K) {
            llamar();
            return;
        }
        if (k == KeyEvent.VK_L) {
            usarHerramienta(Herramienta.LUPA);
            return;
        }
        String sel = "ZXCVB";
        int si = sel.indexOf(Character.toUpperCase(e.getKeyChar()));
        if (si >= 0 && j != null) {
            List<Veredicto> s = j.sellos();
            if (si < s.size()) {
                sellar(s.get(si));
            }
        }
    }

    /** Lo que hace Espacio segun el momento. */
    private void principal() {
        switch (estado) {
            case TURNO:
                empezarLlamada();
                break;
            case LLAMADA:
                avanzarLlamada();
                break;
            case VACIO:
                hacerPasar();
                break;
            case ENTRANDO:
            case ATENDIENDO:
                letras = texto.length();
                break;
            case RESULTADO:
                despedirVisitante();
                break;
            case FIN_DIA:
                if (oyente != null) {
                    oyente.alTerminarDia(j);
                }
                break;
            default:
                break;
        }
    }

    private void llamar() {
        if (caso == null || estado != Estado.ATENDIENDO || !j.puedeLlamar()) {
            return;
        }
        Sonido.tocar(Sonido.Tipo.TELEFONO);
        llamadaInst = j.llamar();
        avisos.agregar("Llamada", llamadaInst, Tema.acento2(), 0);
    }

    private void acusar() {
        boolean ok = j.acusarFoto();
        capa = Capa.NADA;
        if (ok) {
            avisos.agregar("¡Bien visto!", "La foto no coincide. Confróntalo en las preguntas.", Tema.bien(), 0);
            Sonido.tocar(Sonido.Tipo.ALERTA);
        } else {
            avisos.agregar("Hmm...", "La foto sí coincide. El visitante se ofendió un poco.", Tema.mal(), 0);
        }
    }

    // ==================================================================
    // Actualizacion
    // ==================================================================

    private void actualizar(double dt) {
        t += dt;
        double antes = letras;
        letras = Math.min(texto.length(), letras + dt * 48);
        if ((int) letras / 4 != (int) antes / 4 && letras < texto.length()) {
            Sonido.tocar(Sonido.Tipo.TECLEO);
        }
        parpadeoT -= dt;
        if (parpadeoT < -0.13) {
            parpadeoT = 2 + azar.nextDouble() * 3;
        }
        glitchT = Math.max(0, glitchT - dt);
        if (caso != null && caso.visitante.impostor && estado == Estado.ATENDIENDO && azar.nextDouble() < dt * 0.12) {
            glitchT = 0.18;     // los impostores "fallan" de vez en cuando...
        }
        tickerX -= dt * 70;

        if (j == null) {
            return;
        }
        boolean corre = capa == Capa.NADA && (estado == Estado.ATENDIENDO || estado == Estado.VACIO
                || estado == Estado.ENTRANDO);
        if (corre) {
            for (Jornada.Evento ev : j.avanzar(dt)) {
                avisos.agregar("Evento", ev.texto, ev == Jornada.Evento.PANICO ? Tema.mal() : Tema.acento2(), 0);
                Sonido.tocar(Sonido.Tipo.ALERTA);
            }
            String rep = j.revisarCorresponsal();
            if (rep != null) {
                avisos.agregar("Corresponsal", rep, Tema.acento2(), 0);
                Sonido.tocar(Sonido.Tipo.TELEFONO);
            }
        }
        if (j.isTerminada() && estado != Estado.FIN_DIA && estado != Estado.RESULTADO && estado != Estado.SELLANDO
                && estado != Estado.TURNO && estado != Estado.LLAMADA) {
            if (caso != null && estado != Estado.SALIENDO) {
                avisos.agregar("18:00", "¡Cerramos! Lo que quedó en la fila circula sin verificar.", Tema.mal(), 0);
            }
            estado = Estado.FIN_DIA;
            caso = null;
            Sonido.tocar(Sonido.Tipo.ALERTA);
        }
        switch (estado) {
            case ENTRANDO:
                entradaT += dt * 1.6;
                if (entradaT >= 1) {
                    entradaT = 1;
                    estado = Estado.ATENDIENDO;
                }
                break;
            case SALIENDO:
                entradaT += dt * 1.8;
                if (entradaT >= 1) {
                    caso = null;
                    texto = "";
                    if (j.isTerminada()) {
                        estado = Estado.FIN_DIA;
                    } else if (j.enFila() == 0) {
                        j.cerrar();
                        estado = Estado.FIN_DIA;
                    } else {
                        estado = Estado.VACIO;
                        hacerPasar();
                    }
                }
                break;
            case SELLANDO:
                selloT += dt * 2.2;
                if (selloT >= 1) {
                    terminarSello();
                }
                break;
            default:
                break;
        }
        if (escaneando != null) {
            escaneoT += dt * 1.6;
            if (escaneoT >= 1) {
                List<String> r = j.usar(escaneando);
                if (escaneando == Herramienta.CORRESPONSAL && j.corresponsalEnCamino()) {
                    avisos.agregar("Corresponsal en camino", r.isEmpty() ? "" : r.get(r.size() - 1), Tema.acento2(), 0);
                }
                escaneando = null;
            }
        }
    }

    // ==================================================================
    // Dibujo
    // ==================================================================

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        long ahora = System.nanoTime();
        double dt = Math.min(0.1, (ahora - ultimoNs) / 1e9);
        ultimoNs = ahora;
        actualizar(dt);

        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        esc = Math.min(getWidth() / (double) W, getHeight() / (double) H);
        offX = (getWidth() - W * esc) / 2;
        offY = (getHeight() - H * esc) / 2;
        g.translate(offX, offY);
        g.scale(esc, esc);
        g.setClip(0, 0, W, H);
        botones.clear();
        if (j == null) {
            g.dispose();
            return;
        }
        fondo(g);
        barra(g);
        ventana(g);
        globo(g);
        opciones(g);
        publicacion(g);
        credencial(g);
        libreta(g);
        sellos(g);
        monitor(g);
        tv(g);
        if (estado == Estado.SELLANDO || estado == Estado.RESULTADO) {
            animacionSello(g);
        }
        if (estado == Estado.RESULTADO && ultimo != null) {
            tarjetaResultado(g);
        }
        if (estado == Estado.TURNO) {
            tarjetaTurno(g);
        }
        if (estado == Estado.FIN_DIA) {
            finDia(g);
        }
        switch (capa) {
            case LUPA:
                lupa(g);
                break;
            case MANUAL:
                manual(g);
                break;
            case ESTRUCTURAS:
                estructuras(g);
                break;
            case PAUSA:
                pausa(g);
                break;
            default:
                break;
        }
        if (capa == Capa.NADA && estado != Estado.FIN_DIA && estado != Estado.TURNO) {
            avisos.dibujar(g, 30, 62, 560, H, W);
        } else {
            avisos.dibujar(g, -2000, 62, 10, H, W);   // solo los subtitulos
        }
        g.dispose();
    }

    private void fondo(Graphics2D g) {
        Color pared = Tema.altoContraste ? Color.BLACK : new Color(30, 44, 52);
        g.setPaint(new GradientPaint(0, 0, pared, 0, H, Tema.altoContraste ? Color.BLACK : new Color(16, 24, 30)));
        g.fillRect(0, 0, W, H);
        // escritorio de madera
        if (!Tema.altoContraste) {
            g.setPaint(new GradientPaint(0, 540, new Color(92, 62, 40), 0, H, new Color(60, 38, 24)));
            g.fillRect(605, 540, 405, H - 540);
            g.setColor(new Color(0, 0, 0, 40));
            for (int y = 548; y < H; y += 14) {
                g.drawLine(605, y, 1010, y + 3);
            }
        }
    }

    private void barra(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, W, 42);
        JugadorVerificador jv = j.jugador;
        g.setFont(Tema.fuente(Font.BOLD, 17));
        g.setColor(Tema.texto());
        g.drawString("DÍA " + j.dia + " · " + Historia.TITULOS[j.dia].toUpperCase(), 20, 28);
        // reloj
        g.setFont(Tema.fuente(Font.BOLD, 24));
        boolean tarde = j.getMinuto() > Jornada.CIERRE - 60;
        g.setColor(tarde && ((int) (t * 2)) % 2 == 0 ? Tema.mal() : Tema.acento2());
        String hora = j.relajado ? "--:--" : j.hora();
        g.drawString(hora, 340, 31);
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(Tema.textoSuave());
        g.drawString(j.relajado ? "modo relajado" : j.enPausa() ? "pausa (café)" : j.enApagon() ? "¡apagón!" : "cierra 18:00",
                425, 28);
        // fila
        g.setColor(Tema.texto());
        g.drawString("En fila: " + j.enFila(), 540, 28);
        // advertencias
        int ax = 660;
        g.drawString("Advertencias:", ax, 28);
        for (int i = 0; i < JugadorVerificador.MAX_ADVERTENCIAS; i++) {
            boolean llena = i < jv.advertencias;
            g.setColor(llena ? Tema.mal() : Tema.borde());
            int x = ax + 92 + i * 22;
            if (llena) {
                g.fillRoundRect(x, 14, 16, 18, 4, 4);
                g.setColor(Color.WHITE);
                g.drawString("!", x + 6, 28);
            } else {
                g.drawRoundRect(x, 14, 16, 18, 4, 4);
            }
        }
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(Tema.texto());
        g.drawString(jv.nombre + " · " + jv.puntos + " pts · $" + jv.dinero, 870, 28);
        if (!marcadorRed.isEmpty()) {
            g.setFont(Tema.fuente(Font.PLAIN, 12));
            g.setColor(new Color(255, 210, 90));
            g.drawString("Marcador: " + String.join("  ·  ", marcadorRed), 870, 40);
        }
        boton(g, new Rectangle(1170, 8, 74, 28), "Manual", Tema.tarjeta(), () -> capa = Capa.MANUAL);
        boton(g, new Rectangle(1250, 8, 96, 28), "Estructuras", Tema.tarjeta(), () -> capa = Capa.ESTRUCTURAS);
        boton(g, new Rectangle(1352, 8, 72, 28), "Pausa", Tema.tarjeta(), () -> capa = Capa.PAUSA);
    }

    // ------------------------------------------------------------ ventanilla
    private void ventana(Graphics2D g) {
        Rectangle r = VENTANA;
        if (bufVentana == null) {
            bufVentana = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D b = bufVentana.createGraphics();
        b.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // cielo segun la hora
        double dia = Math.max(0, Math.min(1, j.avanceDia()));
        Color c1 = Escala.mezclar(new Color(120, 180, 235), new Color(240, 130, 80), Math.max(0, dia * 1.3 - 0.3));
        Color c2 = Escala.mezclar(new Color(200, 230, 250), new Color(80, 50, 110), dia);
        if (Tema.altoContraste) {
            c1 = Color.BLACK;
            c2 = new Color(30, 30, 30);
        }
        b.setPaint(new GradientPaint(0, 0, c1, 0, r.height, c2));
        b.fillRect(0, 0, r.width, r.height);
        // edificios de Ciudad Nova
        Random rr = new Random(77);
        for (int i = 0; i < 12; i++) {
            int bw = 40 + rr.nextInt(50), bh = 80 + rr.nextInt(170), bx = i * 52 - 20;
            b.setColor(new Color(40 + rr.nextInt(20), 50 + rr.nextInt(20), 70 + rr.nextInt(30)));
            b.fillRect(bx, r.height - 110 - bh, bw, bh);
            b.setColor(new Color(255, 220, 130, dia > 0.6 ? 180 : 60));
            for (int wy = r.height - 100 - bh; wy < r.height - 120; wy += 18) {
                for (int wx = bx + 6; wx < bx + bw - 8; wx += 14) {
                    if (rr.nextInt(3) > 0) {
                        b.fillRect(wx, wy, 6, 8);
                    }
                }
            }
        }
        b.setColor(new Color(60, 60, 66));
        b.fillRect(0, r.height - 110, r.width, 110);
        // lluvia el dia 3
        if (j.dia == 3) {
            b.setColor(new Color(200, 220, 255, 120));
            for (int i = 0; i < 70; i++) {
                double x = (i * 37 + t * 220) % r.width, y = (i * 53 + t * 520) % r.height;
                b.drawLine((int) x, (int) y, (int) x - 4, (int) y + 12);
            }
        }
        // la fila de gente esperando (siluetas)
        int n = Math.min(6, j.enFila());
        for (int i = 0; i < n; i++) {
            int x = r.width - 60 - i * 34, y = r.height - 170;
            b.setColor(new Color(20, 20, 30, 170));
            b.fill(new Ellipse2D.Double(x, y + Math.sin(t * 2 + i) * 2, 26, 30));
            b.fillRoundRect(x - 6, y + 28, 38, 80, 18, 18);
        }
        // el visitante
        if (caso != null && estado != Estado.VACIO) {
            double desliz = 0;
            if (estado == Estado.ENTRANDO) {
                desliz = (1 - suave(entradaT)) * (r.width / 2.0 + 120);
            } else if (estado == Estado.SALIENDO) {
                desliz = -suave(entradaT) * (r.width / 2.0 + 160);
            }
            double habla = letras < texto.length() ? Math.abs(Math.sin(t * 14)) : 0;
            double bob = Math.sin(t * 1.7) * 3;
            Caras.dibujar(b, caso.visitante.cara, r.width / 2.0 + desliz, 170 + bob, 1.45, expr,
                    parpadeoT < 0, habla);
        }
        // vidrio y mostrador
        b.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 40), r.width, r.height, new Color(255, 255, 255, 0)));
        b.fillRect(0, 0, r.width, r.height);
        b.setColor(new Color(255, 255, 255, 50));
        b.setStroke(new BasicStroke(10));
        b.drawLine(60, 30, 140, -10);
        b.drawLine(90, 60, 200, -10);
        b.setColor(Tema.altoContraste ? Color.WHITE : new Color(110, 116, 128));
        b.fillRect(0, r.height - 38, r.width, 38);
        b.setColor(new Color(30, 30, 36));
        b.fillRoundRect(r.width / 2 - 50, r.height - 34, 100, 26, 8, 8);
        b.setFont(Tema.fuente(Font.BOLD, 12));
        b.setColor(new Color(255, 210, 90));
        b.drawString("VENTANILLA 3", r.width / 2 - 40, r.height - 16);
        if (estado == Estado.VACIO || estado == Estado.LLAMADA || estado == Estado.TURNO) {
            b.setColor(new Color(170, 40, 40));
            b.fillRoundRect(r.width / 2 - 80, 120, 160, 50, 10, 10);
            b.setColor(Color.WHITE);
            b.setFont(Tema.fuente(Font.BOLD, 22));
            String cart = estado == Estado.VACIO ? "SIGUIENTE" : "CERRADO";
            b.drawString(cart, r.width / 2 - b.getFontMetrics().stringWidth(cart) / 2, 154);
        }
        b.dispose();

        // glitch: la imagen se parte en franjas desplazadas
        if (glitchT > 0) {
            for (int y = 0; y < r.height; y += 12) {
                int dx = azar.nextInt(5) == 0 ? azar.nextInt(30) - 15 : 0;
                g.drawImage(bufVentana, r.x + dx, r.y + y, r.x + dx + r.width, r.y + y + 12, 0, y, r.width, y + 12, null);
            }
            g.setColor(new Color(255, 0, 80, 40));
            g.fillRect(r.x + 4, r.y, r.width, r.height);
        } else {
            g.drawImage(bufVentana, r.x, r.y, null);
        }
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(160, 166, 180));
        g.setStroke(new BasicStroke(6));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g.setStroke(new BasicStroke(1));
        // rejilla del intercomunicador
        g.setColor(new Color(40, 40, 46));
        for (int i = 0; i < 5; i++) {
            g.fillOval(r.x + r.width / 2 - 24 + i * 11, r.y + r.height - 52, 6, 6);
        }
    }

    private static double suave(double x) {
        return x * x * (3 - 2 * x);
    }

    private void globo(Graphics2D g) {
        Rectangle r = GLOBO;
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(245, 240, 228));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(80, 70, 60));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        if (texto.isEmpty()) {
            return;
        }
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.altoContraste ? Color.YELLOW : new Color(150, 60, 40));
        g.drawString(quienHabla + (caso != null && estado != Estado.LLAMADA ? "  ·  " + caso.visitante.rol.etiqueta : ""),
                r.x + 14, r.y + 20);
        Font f = Tema.fuente(Font.PLAIN, 16);
        g.setFont(f);
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(30, 26, 22));
        String vis = texto.substring(0, (int) letras);
        int y = r.y + 42;
        for (String l : Avisos.partir(g, f, vis, r.width - 28)) {
            g.drawString(l, r.x + 14, y);
            y += 20;
        }
        if (letras >= texto.length() && ((int) (t * 2)) % 2 == 0) {
            g.fillRect(r.x + r.width - 22, r.y + r.height - 18, 8, 8);
        }
    }

    private void opciones(Graphics2D g) {
        Rectangle r = OPCIONES;
        panel(g, r, null);
        int y = r.y + 14;
        switch (estado) {
            case TURNO:
                return;
            case LLAMADA:
                botonGrande(g, new Rectangle(r.x + 20, r.y + 80, r.width - 40, 70),
                        lineaLlamada < llamada.length - 1 ? "Seguir escuchando  (Espacio)" : "Colgar y abrir la ventanilla  (Espacio)",
                        Tema.acento(), this::avanzarLlamada);
                return;
            case VACIO:
                if (!j.isTerminada()) {
                    botonGrande(g, new Rectangle(r.x + 20, r.y + 80, r.width - 40, 70),
                            "¡Siguiente!  Hacer pasar  (Espacio)", Tema.acento(), this::hacerPasar);
                }
                return;
            case RESULTADO:
                botonGrande(g, new Rectangle(r.x + 20, r.y + 180, r.width - 40, 60),
                        "Despedir y continuar  (Espacio)", Tema.acento(), this::despedirVisitante);
                break;
            default:
                break;
        }
        if (caso == null || estado == Estado.RESULTADO || estado == Estado.SALIENDO) {
            return;
        }
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.textoSuave());
        g.drawString("PREGUNTAR  (teclas 1-5)  ·  árbol de diálogo: " + caso.dialogo.visitados() + "/"
                + caso.dialogo.cantidadNodos() + " nodos", r.x + 14, y + 4);
        y += 16;
        List<NodoDialogo> op = j.opciones();
        int i = 0;
        for (NodoDialogo n : op) {
            if (i >= 5) {
                break;
            }
            Rectangle br = new Rectangle(r.x + 12, y, r.width - 24, 40);
            boolean conf = n.insertado;
            Color c = conf ? new Color(200, 110, 30) : Tema.tarjeta();
            final NodoDialogo nn = n;
            boton(g, br, (i + 1) + ".  " + (conf ? "¡CONFRONTAR! " : "") + n.pregunta, c, () -> preguntar(nn));
            y += 46;
            i++;
        }
        if (op.isEmpty()) {
            g.setColor(Tema.textoSuave());
            g.setFont(Tema.fuente(Font.ITALIC, 14));
            g.drawString("Ya no tiene más que decir. Investiga o sella.", r.x + 14, y + 24);
        }
    }

    // ------------------------------------------------------------ escritorio
    private void publicacion(Graphics2D g) {
        Rectangle r = POST;
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(r.x + 5, r.y + 6, r.width, r.height, 8, 8);
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(250, 250, 246));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(180, 180, 170));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        Color tinta = Tema.altoContraste ? Color.WHITE : new Color(30, 30, 36);
        if (caso == null) {
            g.setColor(Tema.altoContraste ? Color.WHITE : new Color(160, 160, 150));
            g.setFont(Tema.fuente(Font.ITALIC, 15));
            g.drawString("Aquí aparece lo que te piden publicar.", r.x + 40, r.y + r.height / 2);
            return;
        }
        Publicacion p = caso.pub;
        Cuenta c = p.getCuenta();
        g.setColor(new Color(90, 110, 200));
        g.fillOval(r.x + 14, r.y + 14, 38, 38);
        g.setColor(Color.WHITE);
        g.setFont(Tema.fuente(Font.BOLD, 18));
        String ini = c == null ? "?" : c.getNombre().substring(0, 1);
        g.drawString(ini, r.x + 33 - g.getFontMetrics().stringWidth(ini) / 2, r.y + 40);
        g.setColor(tinta);
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.drawString(c == null ? p.getAutor() : c.getNombre() + (c.estaVerificada() ? "  (verificada)" : ""), r.x + 62, r.y + 30);
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(new Color(110, 110, 120));
        g.drawString((c == null ? "" : c.getArroba()) + "  ·  " + (p.getHora() == null ? "" : p.getHora()),
                r.x + 62, r.y + 48);
        Font f = Tema.fuente(Font.PLAIN, 16);
        g.setFont(f);
        g.setColor(tinta);
        int y = r.y + 80;
        for (String l : Avisos.partir(g, f, p.getTexto(), r.width - 30)) {
            g.drawString(l, r.x + 15, y);
            y += 21;
        }
        if (caso.imagen != null || p.getImagen() != null) {
            int ih = Math.max(40, r.y + r.height - 50 - y);
            Rectangle ir = new Rectangle(r.x + 15, y, r.width - 30, Math.min(110, ih));
            g.setPaint(new GradientPaint(ir.x, ir.y, new Color(90, 110, 130), ir.x, ir.y + ir.height, new Color(50, 60, 70)));
            g.fill(ir);
            g.setColor(new Color(255, 255, 255, 150));
            g.setFont(Tema.fuente(Font.BOLD, 12));
            g.drawString(p.getImagen() != null && p.getImagen().startsWith("video") ? "► VIDEO" : "[ FOTO ]",
                    ir.x + 8, ir.y + 18);
        }
        g.setFont(Tema.fuente(Font.ITALIC, 13));
        g.setColor(new Color(120, 90, 60));
        g.drawString("Pide: " + caso.peticion, r.x + 15, r.y + r.height - 14);
        // sello estampado
        if (caso.decidido != null && estado != Estado.SELLANDO) {
            estampa(g, caso.decidido, r.x + r.width - 120, r.y + r.height - 90, 1, -0.2);
        }
    }

    private void credencial(Graphics2D g) {
        Rectangle r = CRED;
        if (caso == null) {
            return;
        }
        Persona p = caso.visitante;
        if (p.credencial == null) {
            g.setColor(new Color(0, 0, 0, 60));
            g.fillRoundRect(r.x + 60, r.y + 40, r.width - 120, 70, 10, 10);
            g.setColor(Tema.textoSuave());
            g.setFont(Tema.fuente(Font.ITALIC, 14));
            g.drawString("Sin credencial: es un particular.", r.x + 85, r.y + 80);
            return;
        }
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(r.x + 5, r.y + 5, r.width, r.height, 14, 14);
        g.setPaint(new GradientPaint(r.x, r.y, new Color(236, 242, 250), r.x + r.width, r.y + r.height, new Color(206, 220, 236)));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g.setColor(new Color(40, 80, 160));
        g.fillRoundRect(r.x, r.y, r.width, 32, 14, 14);
        g.fillRect(r.x, r.y + 18, r.width, 14);
        g.setColor(Color.WHITE);
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.drawString(p.credencial.institucion.toUpperCase(), r.x + 12, r.y + 21);
        BufferedImage foto = Caras.foto(p.credencial.foto, 104, 128);
        g.drawImage(foto, r.x + 12, r.y + 40, null);
        g.setColor(new Color(40, 40, 50));
        g.drawRect(r.x + 12, r.y + 40, 104, 128);
        g.setColor(new Color(20, 24, 40));
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.drawString(p.credencial.nombre, r.x + 130, r.y + 62);
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.drawString(p.credencial.cargo, r.x + 130, r.y + 84);
        g.drawString("N.º " + p.credencial.numero, r.x + 130, r.y + 106);
        g.drawString("Vence " + p.credencial.vence, r.x + 130, r.y + 128);
        if (j.disponible(Herramienta.LUPA) && estado == Estado.ATENDIENDO) {
            boton(g, new Rectangle(r.x + 130, r.y + 140, 150, 30), "Lupa (L)", new Color(40, 80, 160),
                    () -> usarHerramienta(Herramienta.LUPA));
        }
    }

    private void libreta(Graphics2D g) {
        Rectangle r = LIBRETA;
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(255, 236, 150));
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(200, 170, 90));
        g.drawRect(r.x, r.y, r.width, r.height);
        if (!Tema.altoContraste) {
            g.setColor(new Color(150, 180, 220, 120));
            for (int y = r.y + 24; y < r.y + r.height; y += 18) {
                g.drawLine(r.x + 4, y, r.x + r.width - 4, y);
            }
        }
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.altoContraste ? Color.YELLOW : new Color(120, 60, 30));
        g.drawString("LIBRETA DE PISTAS", r.x + 10, r.y + 17);
        List<String> pistas = pistas();
        g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(40, 40, 70));
        int y = r.y + 37;
        int desde = Math.max(0, pistas.size() - 6);
        for (int i = desde; i < pistas.size(); i++) {
            String pi = pistas.get(i);
            int tp = Iconos.tipo(pi);
            if (tp == 0) {
                g.drawString("• " + VistaArboles.corto(pi, 58), r.x + 10, y);
            } else {
                Iconos.marca(g, tp, r.x + 15, y - 4, 6);
                g.drawString(VistaArboles.corto(Iconos.sinPrefijo(pi), 56), r.x + 26, y);
            }
            y += 17;
        }
        if (pistas.isEmpty()) {
            g.drawString("(pregunta e investiga: las pistas se anotan solas)", r.x + 10, y);
        }
    }

    private List<String> pistas() {
        List<String> l = new ArrayList<>();
        if (caso == null) {
            return l;
        }
        for (NodoDialogo n : caso.dialogo.preorden()) {
            if (n.visitado && n.pista != null) {
                l.add(n.pista);
            }
        }
        for (List<String> rs : caso.resultados.values()) {
            for (String s : rs) {
                if (Iconos.tipo(s) != 0) {
                    l.add(s);
                }
            }
        }
        if (llamadaInst != null) {
            l.add("Llamada: " + llamadaInst);
        }
        return l;
    }

    private void sellos(Graphics2D g) {
        Rectangle r = SELLOS;
        List<Veredicto> s = j.sellos();
        int n = s.size();
        int w = (r.width - 10) / Math.max(1, n);
        String teclas = "ZXCVB";
        for (int i = 0; i < n; i++) {
            Veredicto v = s.get(i);
            Rectangle br = new Rectangle(r.x + 5 + i * w, r.y + 10, w - 8, r.height - 20);
            boolean activo = estado == Estado.ATENDIENDO && caso != null;
            boolean hover = activo && br.contains(mx, my);
            Color c = colorSello(v);
            // mango
            g.setColor(new Color(50, 30, 20));
            g.fillRoundRect(br.x + br.width / 2 - 14, br.y + (hover ? 0 : 6), 28, 52, 12, 12);
            // base
            g.setColor(activo ? c : c.darker().darker());
            g.fillRoundRect(br.x, br.y + 56 + (hover ? -4 : 0), br.width, 50, 8, 8);
            g.setColor(Color.WHITE);
            float tam = 13.5f;
            g.setFont(Tema.fuente(Font.BOLD, tam));
            while (g.getFontMetrics().stringWidth(v.sello) > br.width - 8 && tam > 8) {
                tam -= 0.5f;
                g.setFont(new Font(Tema.FUENTE, Font.BOLD, Math.round(tam)));
            }
            FontMetrics fm = g.getFontMetrics();
            g.drawString(v.sello, br.x + (br.width - fm.stringWidth(v.sello)) / 2, br.y + 85 + (hover ? -4 : 0));
            g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
            g.setColor(Tema.textoSuave());
            String tk = "(" + teclas.charAt(i) + ")";
            g.drawString(tk, br.x + (br.width - g.getFontMetrics().stringWidth(tk)) / 2, br.y + 120);
            if (activo) {
                final Veredicto vv = v;
                botones.add(new Boton(br, () -> sellar(vv)));
            }
        }
    }

    static Color colorSello(Veredicto v) {
        switch (v) {
            case PUBLICAR: return new Color(30, 150, 90);
            case OPINION: return new Color(50, 110, 200);
            case ETIQUETA: return new Color(210, 150, 20);
            case RECHAZAR: return new Color(110, 110, 120);
            default: return new Color(200, 40, 50);
        }
    }

    private void estampa(Graphics2D g, Veredicto v, double cx, double cy, double escala, double ang) {
        Graphics2D s = (Graphics2D) g.create();
        s.translate(cx, cy);
        s.rotate(ang);
        s.scale(escala, escala);
        Color c = colorSello(v);
        s.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
        s.setStroke(new BasicStroke(4));
        s.drawRoundRect(-90, -28, 180, 56, 10, 10);
        s.setFont(Tema.fuente(Font.BOLD, 24));
        FontMetrics fm = s.getFontMetrics();
        s.drawString(v.sello, -fm.stringWidth(v.sello) / 2, 9);
        s.dispose();
    }

    private void animacionSello(Graphics2D g) {
        if (estado == Estado.SELLANDO) {
            double k = suave(Math.min(1, selloT));
            double esc0 = 2.6 - 1.6 * k;
            Composite antes = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, 0.3 + k)));
            estampa(g, selloV, POST.x + POST.width - 120, POST.y + POST.height - 90, esc0, -0.2);
            g.setComposite(antes);
        }
    }

    private void tarjetaResultado(Graphics2D g) {
        Jornada.Resultado r = ultimo;
        Rectangle c = new Rectangle(OPCIONES.x + 12, OPCIONES.y + 12, OPCIONES.width - 24, 160);
        boolean ok = r.efecto.acierto;
        g.setColor(ok ? new Color(20, 70, 50) : new Color(90, 30, 20));
        g.fillRoundRect(c.x, c.y, c.width, c.height, 12, 12);
        g.setColor(ok ? Tema.bien() : Tema.mal());
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(c.x, c.y, c.width, c.height, 12, 12);
        g.setStroke(new BasicStroke(1));
        g.setFont(Tema.fuente(Font.BOLD, 20));
        g.drawString((ok ? "CORRECTO" : "ERROR") + "   " + (r.puntos >= 0 ? "+" : "") + r.puntos + " pts"
                + (r.dinero != 0 ? "   $" + r.dinero : ""), c.x + 14, c.y + 28);
        Font f = Tema.fuente(Font.PLAIN, 13.5f);
        g.setFont(f);
        g.setColor(Color.WHITE);
        int y = c.y + 50;
        List<String> lineas = new ArrayList<>();
        lineas.add("Era: " + r.caso.verdad.etiqueta + ". " + r.efecto.explicacion);
        if (r.caso.pub.getExplicacion() != null) {
            lineas.add(r.caso.pub.getExplicacion());
        }
        if (r.alcance > 0) {
            lineas.add("BFS: la publicación llegó a " + r.alcance + " cuentas.");
        }
        if (r.corregidos > 0) {
            lineas.add("Denuncia: cuenta eliminada del grafo; " + r.corregidos + " personas corregidas.");
        }
        if (r.bonoRapidez) {
            lineas.add("Bono por rapidez +30.");
        }
        for (String l : lineas) {
            for (String p : Avisos.partir(g, f, l, c.width - 28)) {
                if (y < c.y + c.height - 6) {
                    g.drawString(p, c.x + 14, y);
                }
                y += 17;
            }
        }
    }

    // ------------------------------------------------------------ monitor
    private void monitor(Graphics2D g) {
        Rectangle r = MONITOR;
        g.setColor(new Color(30, 32, 38));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 18, 18);
        g.setColor(new Color(70, 74, 84));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 18, 18);
        // pestanas
        Herramienta[] hs = Herramienta.values();
        int tw = (r.width - 20) / hs.length;
        String[] cortos = {"Perfil", "Directorio", "Lupa", "Imagen", "Corresp.", "Red"};
        for (int i = 0; i < hs.length; i++) {
            Herramienta h = hs[i];
            Rectangle tr = new Rectangle(r.x + 10 + i * tw, r.y + 10, tw - 4, 46);
            boolean bloqueada = h.dia > j.dia;
            boolean sel = h == tab;
            g.setColor(sel ? Tema.acento() : bloqueada ? new Color(40, 40, 46) : new Color(52, 56, 70));
            g.fillRoundRect(tr.x, tr.y, tr.width, tr.height, 8, 8);
            Color ci = bloqueada ? new Color(90, 90, 96) : Color.WHITE;
            if (bloqueada) {
                Iconos.candado(g, tr.getCenterX(), tr.y + 17, 8, ci);
            } else {
                Iconos.herramienta(g, i, tr.getCenterX(), tr.y + 17, 10, ci);
            }
            g.setColor(ci);
            g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
            String et = "QWERTY".charAt(i) + "·" + cortos[i];
            g.drawString(et, tr.x + (tr.width - g.getFontMetrics().stringWidth(et)) / 2, tr.y + 40);
            final Herramienta hh = h;
            botones.add(new Boton(tr, () -> {
                tab = hh;
                Sonido.tocar(Sonido.Tipo.CLIC);
            }));
        }
        // pantalla
        Rectangle p = PANTALLA;
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(10, 20, 30));
        g.fillRect(p.x, p.y, p.width, p.height);
        Graphics2D s = (Graphics2D) g.create();
        s.clip(p);
        contenidoPantalla(s, p);
        // lineas de barrido CRT
        if (!Tema.altoContraste) {
            s.setColor(new Color(0, 0, 0, 40));
            for (int y = p.y; y < p.y + p.height; y += 3) {
                s.drawLine(p.x, y, p.x + p.width, y);
            }
        }
        s.dispose();
        g.setColor(new Color(90, 200, 120));
        g.fillOval(r.x + r.width / 2 - 4, r.y + r.height - 20, 8, 8);
    }

    private void contenidoPantalla(Graphics2D g, Rectangle p) {
        Color verde = Tema.altoContraste ? Color.WHITE : new Color(120, 255, 170);
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(verde);
        g.drawString(tab.nombre.toUpperCase(), p.x + 12, p.y + 22);
        if (tab.dia > j.dia) {
            Iconos.candado(g, p.getCenterX(), p.getCenterY() - 50, 22, Tema.textoSuave());
            centrado(g, "Disponible desde el día " + tab.dia, p, Tema.textoSuave());
            return;
        }
        if (j.enApagon()) {
            for (int i = 0; i < 400; i++) {
                int c = azar.nextInt(200);
                g.setColor(new Color(c, c, c));
                g.fillRect(p.x + azar.nextInt(p.width), p.y + azar.nextInt(p.height), 3, 2);
            }
            centrado(g, "SIN SEÑAL — APAGÓN", p, Color.WHITE);
            return;
        }
        if (caso == null) {
            centrado(g, "Esperando al siguiente caso...", p, Tema.textoSuave());
            return;
        }
        if (escaneando == tab) {
            g.setColor(verde);
            int bw = (int) ((p.width - 60) * Math.min(1, escaneoT));
            g.drawRect(p.x + 30, p.y + p.height / 2, p.width - 60, 18);
            g.fillRect(p.x + 30, p.y + p.height / 2, bw, 18);
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.drawString(tab == Herramienta.DIRECTORIO ? "Buscando en el AVL..." : tab == Herramienta.RED
                    ? "Recorriendo el grafo (BFS / DFS)..." : "Analizando...", p.x + 30, p.y + p.height / 2 - 10);
            int ly = p.y + 50 + (int) ((t * 160) % (p.height - 60));
            g.setColor(new Color(120, 255, 170, 90));
            g.fillRect(p.x, ly, p.width, 4);
            return;
        }
        List<String> res = j.resultados(tab);
        int y = p.y + 40;
        if (tab == Herramienta.RED && res != null) {
            red3d.dibujar(g, new Rectangle(p.x + 4, p.y + 30, p.width - 8, 210), j.jugador.ciudad, caso.origen, t);
            y = p.y + 256;
        }
        if (tab == Herramienta.DIRECTORIO && res != null) {
            Rectangle ar = new Rectangle(p.x + 6, p.y + p.height - 150, p.width - 12, 110);
            Cuenta c = caso.pub.getCuenta();
            VistaArboles.avl(g, j.jugador.directorio, c == null ? null : c.getArroba(), ar, true);
            if (j.puedeLlamar()) {
                boton(g, new Rectangle(p.x + p.width - 130, p.y + p.height - 36, 120, 28), "Llamar (K)",
                        new Color(40, 120, 80), this::llamar);
            }
        }
        if (res == null) {
            String acc;
            switch (tab) {
                case PERFIL: acc = "ESCANEAR PERFIL"; break;
                case DIRECTORIO: acc = "BUSCAR EN EL DIRECTORIO"; break;
                case LUPA: acc = "ABRIR LA LUPA"; break;
                case IMAGEN: acc = "BÚSQUEDA INVERSA"; break;
                case CORRESPONSAL: acc = "ENVIAR CORRESPONSAL"; break;
                default: acc = "ESCANEAR LA RED"; break;
            }
            g.setFont(Tema.fuente(Font.PLAIN, 13));
            g.setColor(Tema.textoSuave());
            g.drawString(descripcion(tab), p.x + 14, p.y + 60);
            if (estado == Estado.ATENDIENDO) {
                final Herramienta h = tab;
                botonGrande(g, new Rectangle(p.x + 40, p.y + p.height / 2 - 30, p.width - 80, 60), acc,
                        new Color(30, 110, 80), () -> usarHerramienta(h));
            }
            return;
        }
        Font f = Tema.fuente(Font.PLAIN, 13);
        g.setFont(f);
        Color normal = Tema.altoContraste ? Color.WHITE : new Color(200, 230, 220);
        for (String l : res) {
            int tp = Iconos.tipo(l);
            String limpio = Iconos.sinPrefijo(l);
            boolean primera = true;
            for (String parte : Avisos.partir(g, f, limpio, p.width - 40)) {
                if (primera && tp != 0) {
                    Iconos.marca(g, tp, p.x + 18, y - 4, 6);
                }
                g.setColor(Iconos.color(tp, normal));
                g.drawString(parte, p.x + (tp != 0 ? 30 : 12), y);
                y += 17;
                primera = false;
            }
            y += 3;
        }
        if (tab == Herramienta.CORRESPONSAL && j.corresponsalEnCamino()) {
            g.setColor(verde);
            int bw = (int) ((p.width - 40) * j.avanceCorresponsal());
            g.drawRect(p.x + 20, y + 10, p.width - 40, 14);
            g.fillRect(p.x + 20, y + 10, bw, 14);
            g.drawString("La moto va en camino... mira el mapa", p.x + 20, y + 44);
        }
        if (tab == Herramienta.LUPA && estado == Estado.ATENDIENDO && caso.visitante.credencial != null) {
            botonGrande(g, new Rectangle(p.x + 40, p.y + p.height - 80, p.width - 80, 50), "ABRIR LA LUPA",
                    new Color(40, 80, 160), () -> capa = Capa.LUPA);
        }
    }

    private static String descripcion(Herramienta h) {
        switch (h) {
            case PERFIL: return "Antigüedad, seguidores, fuente y señales de alerta.";
            case DIRECTORIO: return "¿La cuenta es oficial? Búsqueda O(log n) en un AVL.";
            case LUPA: return "Compara la cara del visitante con su credencial.";
            case IMAGEN: return "¿La foto ya existía en otro lugar u otra fecha?";
            case CORRESPONSAL: return "Va al barrio por la ruta más corta (Dijkstra).";
            default: return "¿Quién lo compartió primero? BFS y DFS en el grafo.";
        }
    }

    // ------------------------------------------------------------ TV
    private void tv(Graphics2D g) {
        Rectangle r = TV;
        g.setColor(new Color(20, 20, 24));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g.setColor(new Color(60, 60, 70));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        mapa(g, new Rectangle(r.x + 8, r.y + 8, 196, 210));
        indicadores(g, new Rectangle(r.x + 212, r.y + 8, 186, 210));
        // noticiero
        Rectangle tk = new Rectangle(r.x + 8, r.y + r.height - 44, r.width - 16, 36);
        g.setColor(new Color(170, 20, 30));
        g.fillRect(tk.x, tk.y, 92, tk.height);
        g.setColor(Color.WHITE);
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.drawString("NOTICIAS", tk.x + 12, tk.y + 23);
        g.setColor(new Color(15, 15, 20));
        g.fillRect(tk.x + 92, tk.y, tk.width - 92, tk.height);
        Graphics2D s = (Graphics2D) g.create();
        s.clipRect(tk.x + 94, tk.y, tk.width - 96, tk.height);
        s.setFont(Tema.fuente(Font.PLAIN, 14));
        String linea = String.join("     •     ", ticker);
        int lw = s.getFontMetrics().stringWidth(linea) + 80;
        double x = tk.x + tk.width + (tickerX % (lw + tk.width));
        s.setColor(Color.WHITE);
        s.drawString(linea, (float) x, tk.y + 23);
        s.dispose();
    }

    private void mapa(Graphics2D g, Rectangle r) {
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(16, 30, 26));
        g.fillRect(r.x, r.y, r.width, r.height);
        CiudadEstado c = j.jugador.ciudad;
        double sx = r.width / 560.0, sy = r.height / 540.0;
        double cx = r.x + r.width / 2.0, cy = r.y + r.height / 2.0 + 6;
        g.setColor(new Color(80, 110, 100));
        for (int[] calle : c.mapa.calles()) {
            GrafoCiudad.Zona a = c.mapa.zona(calle[0]), b = c.mapa.zona(calle[1]);
            g.drawLine((int) (cx + a.x * sx), (int) (cy + a.z * sy), (int) (cx + b.x * sx), (int) (cy + b.z * sy));
        }
        List<GrafoCiudad.Zona> ruta = j.getRutaCorresponsal();
        if (ruta != null && caso != null) {
            g.setColor(new Color(255, 200, 60));
            g.setStroke(new BasicStroke(3));
            for (int i = 0; i + 1 < ruta.size(); i++) {
                GrafoCiudad.Zona a = ruta.get(i), b = ruta.get(i + 1);
                g.drawLine((int) (cx + a.x * sx), (int) (cy + a.z * sy), (int) (cx + b.x * sx), (int) (cy + b.z * sy));
            }
            g.setStroke(new BasicStroke(1));
            // la moto avanza por la ruta
            double av = j.corresponsalListo() ? 1 : j.avanceCorresponsal();
            double[] pt = puntoEnRuta(ruta, av);
            int mxp = (int) (cx + pt[0] * sx), myp = (int) (cy + pt[1] * sy);
            g.setColor(Color.BLACK);
            g.fillOval(mxp - 9, myp - 9, 18, 18);
            Iconos.herramienta(g, 4, mxp, myp - 2, 7, new Color(255, 200, 60));
        }
        for (GrafoCiudad.Zona z : c.mapa.getZonas()) {
            int x = (int) (cx + z.x * sx), y = (int) (cy + z.z * sy);
            boolean pan = c.panico[z.id] > 0;
            if (pan) {
                double pulso = 10 + 5 * Math.sin(t * 6);
                g.setColor(new Color(255, 60, 60, 90));
                g.fillOval((int) (x - pulso), (int) (y - pulso), (int) (pulso * 2), (int) (pulso * 2));
            }
            boolean destino = caso != null && caso.zona == z.id && z.id > 1;
            g.setColor(pan ? Tema.mal() : z.id == 1 ? Tema.acento() : destino ? new Color(255, 200, 60) : new Color(140, 200, 180));
            g.fillOval(x - 5, y - 5, 10, 10);
            g.setFont(Tema.fuente(Font.PLAIN, 9.5f));
            g.setColor(Tema.texto());
            String n = z.id == 1 ? "Tu oficina" : z.nombre;
            g.drawString((pan ? "! " : "") + n, x - g.getFontMetrics().stringWidth(n) / 2, y - 8);
        }
        g.setFont(Tema.fuente(Font.BOLD, 10));
        g.setColor(Tema.textoSuave());
        g.drawString("MAPA · grafo de la ciudad", r.x + 4, r.y + r.height - 4);
    }

    private static double[] puntoEnRuta(List<GrafoCiudad.Zona> ruta, double av) {
        if (ruta.size() == 1) {
            return new double[]{ruta.get(0).x, ruta.get(0).z};
        }
        double total = 0;
        for (int i = 0; i + 1 < ruta.size(); i++) {
            total += Math.hypot(ruta.get(i + 1).x - ruta.get(i).x, ruta.get(i + 1).z - ruta.get(i).z);
        }
        double obj = total * av;
        for (int i = 0; i + 1 < ruta.size(); i++) {
            GrafoCiudad.Zona a = ruta.get(i), b = ruta.get(i + 1);
            double l = Math.hypot(b.x - a.x, b.z - a.z);
            if (obj <= l) {
                double k = l == 0 ? 0 : obj / l;
                return new double[]{a.x + (b.x - a.x) * k, a.z + (b.z - a.z) * k};
            }
            obj -= l;
        }
        GrafoCiudad.Zona u = ruta.get(ruta.size() - 1);
        return new double[]{u.x, u.z};
    }

    private void indicadores(Graphics2D g, Rectangle r) {
        CiudadEstado c = j.jugador.ciudad;
        String[] nom = {"Verificada", "Confianza", "Convivencia", "Bienestar", "Desinformación", "Conflictos"};
        int[] val = {c.verificada, c.confianza, c.convivencia, c.bienestar, c.desinformacion(), c.conflictos};
        boolean[] malo = {false, false, false, false, true, true};
        g.setFont(Tema.fuente(Font.BOLD, 11));
        g.setColor(Tema.textoSuave());
        g.drawString("CIUDAD NOVA · salud " + c.salud(), r.x, r.y + 12);
        int y = r.y + 30;
        for (int i = 0; i < nom.length; i++) {
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(Tema.texto());
            g.drawString(nom[i] + (malo[i] ? " ▼" : " ▲"), r.x, y);
            g.drawString(val[i] + "", r.x + r.width - 24, y);
            int bw = r.width - 4;
            g.setColor(new Color(50, 50, 60));
            g.fillRect(r.x, y + 4, bw, 8);
            boolean bueno = malo[i] ? val[i] < 35 : val[i] >= 50;
            g.setColor(bueno ? Tema.bien() : Tema.mal());
            g.fillRect(r.x, y + 4, bw * val[i] / 100, 8);
            y += 30;
        }
    }

    // ------------------------------------------------------------ capas
    private void oscurecer(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, W, H);
    }

    private void lupa(Graphics2D g) {
        oscurecer(g);
        Persona p = caso == null ? null : caso.visitante;
        if (p == null || p.credencial == null) {
            capa = Capa.NADA;
            return;
        }
        Rectangle izq = new Rectangle(220, 120, 440, 520), der = new Rectangle(780, 120, 440, 520);
        g.setColor(new Color(240, 236, 226));
        g.fillRoundRect(izq.x, izq.y, izq.width, izq.height, 16, 16);
        g.setColor(new Color(190, 215, 235));
        g.fillRoundRect(der.x, der.y, der.width, der.height, 16, 16);
        Graphics2D a = (Graphics2D) g.create();
        a.clip(new RoundRectangle2D.Double(izq.x, izq.y, izq.width, izq.height, 16, 16));
        Caras.dibujar(a, p.cara, izq.getCenterX(), izq.y + 210, 2.2, expr, parpadeoT < 0, 0);
        a.dispose();
        a = (Graphics2D) g.create();
        a.clip(new RoundRectangle2D.Double(der.x, der.y, der.width, der.height, 16, 16));
        Caras.dibujar(a, p.credencial.foto, der.getCenterX(), der.y + 210, 2.2, "neutral", false, 0);
        a.dispose();
        g.setFont(Tema.fuente(Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("EN LA VENTANILLA", izq.x, izq.y - 14);
        g.drawString("FOTO DE LA CREDENCIAL  ·  " + p.credencial.numero, der.x, der.y - 14);
        g.setFont(Tema.fuente(Font.PLAIN, 15));
        g.setColor(Tema.textoSuave());
        g.drawString("Busca UN detalle distinto: gafas, lunar, color de ojos, color de pelo o cejas.", 360, 680);
        botonGrande(g, new Rectangle(380, 710, 300, 56), "Coinciden  (L)", Tema.tarjeta(), () -> capa = Capa.NADA);
        botonGrande(g, new Rectangle(760, 710, 320, 56), "¡No es la misma persona!  (X)", Tema.mal(), this::acusar);
    }

    private void manual(Graphics2D g) {
        oscurecer(g);
        Rectangle r = new Rectangle(260, 70, 920, 720);
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(246, 240, 222));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(120, 100, 70));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        Color tinta = Tema.altoContraste ? Color.WHITE : new Color(40, 34, 28);
        List<String[]> paginas = new ArrayList<>();
        List<String> reglas = new ArrayList<>();
        for (int d = 1; d <= j.dia; d++) {
            for (String s : Historia.MANUAL[d]) {
                reglas.add("(día " + d + ")  " + s);
            }
        }
        paginas.add(new String[]{"REGLAS DE LA MESA DE VERIFICACIÓN"});
        paginas.add(new String[]{"CONTROLES"});
        paginas.add(new String[]{"LAS ESTRUCTURAS DE DATOS DEL JUEGO"});
        paginaManual = Math.min(paginaManual, paginas.size() - 1);
        g.setFont(Tema.fuente(Font.BOLD, 24));
        g.setColor(tinta);
        g.drawString("MANUAL DEL VERIFICADOR · " + paginas.get(paginaManual)[0], r.x + 30, r.y + 46);
        List<String> lineas;
        if (paginaManual == 0) {
            lineas = reglas;
        } else if (paginaManual == 1) {
            lineas = new ArrayList<>();
            lineas.add("Espacio / Enter: siguiente visitante, seguir, continuar.");
            lineas.add("1 a 5: hacer una pregunta. Las preguntas naranjas (¡CONFRONTAR!) son nodos nuevos del árbol.");
            lineas.add("Q W E R T Y: herramientas del monitor (perfil, directorio, lupa, imagen, corresponsal, red).");
            lineas.add("L: lupa · X (en la lupa): \"no es la misma persona\" · K: llamar a la institución.");
            lineas.add("Z X C V B: los sellos, de izquierda a derecha.");
            lineas.add("M o F1: este manual · F2: visor de estructuras · Esc: pausa y opciones.");
            lineas.add("Cada herramienta y cada pregunta gasta minutos del reloj. A las 18:00 se cierra.");
            lineas.add("Confronta 2 veces a un impostor con pruebas y confesará (+40 pts).");
            lineas.add("Errores graves = advertencias. Con 4 te despiden.");
            lineas.add("Accesibilidad: alto contraste, texto grande, subtítulos de sonido y modo relajado (Esc).");
        } else {
            lineas = new ArrayList<>();
            lineas.add("COLA (FIFO): la fila de la ventanilla. El primero en llegar es el primero en pasar.");
            lineas.add("ÁRBOL N-ARIO de diálogo: cada pregunta es un nodo; al confrontar se INSERTAN nodos.");
            lineas.add("ÁRBOL de consecuencias: raíz → sello → verdad. El sello baja por el árbol hasta una hoja.");
            lineas.add("   El día 2 se INSERTA la rama #PUBLICIDAD; el día 5 se ELIMINA la rama OPINIÓN.");
            lineas.add("AVL del directorio: búsqueda O(log n); el día 3 se elimina una cuenta y el 5 se inserta otra.");
            lineas.add("GRAFO de la ciudad: Dijkstra para el corresponsal y BFS para el pánico entre barrios.");
            lineas.add("GRAFO social dirigido: BFS probabilístico al publicar, DFS para encontrar anillos de bots,");
            lineas.add("   y al denunciar se ELIMINA el vértice de la cuenta falsa.");
            lineas.add("Azar: eventos del día, caras, el rasgo cambiado del impostor y la propagación.");
        }
        Font f = Tema.fuente(Font.PLAIN, 16);
        g.setFont(f);
        int y = r.y + 90;
        for (String l : lineas) {
            for (String p : Avisos.partir(g, f, l, r.width - 60)) {
                g.drawString(p, r.x + 30, y);
                y += 23;
            }
            y += 5;
        }
        g.setFont(Tema.fuente(Font.PLAIN, 14));
        g.drawString("Página " + (paginaManual + 1) + "/" + paginas.size() + "   (← →)", r.x + 30, r.y + r.height - 24);
        if (paginaManual > 0) {
            boton(g, new Rectangle(r.x + r.width - 330, r.y + r.height - 50, 90, 32), "◄ Anterior", Tema.tarjeta(),
                    () -> paginaManual--);
        }
        if (paginaManual < paginas.size() - 1) {
            boton(g, new Rectangle(r.x + r.width - 230, r.y + r.height - 50, 100, 32), "Siguiente ►", Tema.tarjeta(),
                    () -> paginaManual++);
        }
        boton(g, new Rectangle(r.x + r.width - 120, r.y + r.height - 50, 90, 32), "Cerrar", Tema.acento(),
                () -> capa = Capa.NADA);
    }

    private void estructuras(Graphics2D g) {
        oscurecer(g);
        g.setFont(Tema.fuente(Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString("VISOR DE ESTRUCTURAS (F2)  ·  el reloj se detiene mientras miras", 40, 50);
        Rectangle a = new Rectangle(30, 70, 690, 360), b = new Rectangle(730, 70, 680, 360),
                c = new Rectangle(30, 440, 690, 380), d = new Rectangle(730, 440, 680, 380);
        panel(g, a, caso == null ? "Árbol de diálogo (no hay visitante)" : "Árbol de diálogo de " + caso.visitante.nombre
                + "  ·  " + caso.dialogo.cantidadNodos() + " nodos, altura " + caso.dialogo.altura());
        panel(g, b, "Árbol de consecuencias  ·  " + j.jugador.arbol.cantidadNodos() + " nodos · sellos: "
                + j.jugador.arbol.sellos().size());
        panel(g, c, "AVL del directorio (número = factor de equilibrio)  ·  " + j.jugador.directorio.getTamano() + " cuentas, altura "
                + j.jugador.directorio.getAltura() + ", rotaciones " + j.jugador.directorio.getRotaciones());
        panel(g, d, "Grafos y cola");
        if (caso != null) {
            VistaArboles.dialogo(g, caso.dialogo.getRaiz(), j.getNodoActual(), new Rectangle(a.x + 10, a.y + 40, a.width - 20, a.height - 60));
        }
        VistaArboles.consecuencias(g, j.jugador.arbol, ultimo == null ? null : ultimo.hoja,
                new Rectangle(b.x + 10, b.y + 40, b.width - 20, b.height - 60));
        Cuenta cu = caso == null ? null : caso.pub.getCuenta();
        VistaArboles.avl(g, j.jugador.directorio, cu == null ? null : cu.getArroba(),
                new Rectangle(c.x + 10, c.y + 40, c.width - 20, c.height - 60));
        CiudadEstado ci = j.jugador.ciudad;
        List<String> info = new ArrayList<>();
        info.add("COLA de la ventanilla: " + j.enFila() + " esperando");
        StringBuilder sb = new StringBuilder("   frente → ");
        for (Caso k : j.verFila()) {
            sb.append(k.visitante.nombre).append(" → ");
        }
        info.add(sb.append("final").toString());
        info.add("GRAFO SOCIAL: " + ci.red.cantidadVertices() + " vértices, " + ci.red.cantidadAristas() + " aristas");
        info.add("   cuentas falsas activas: " + ci.botsActivos().size() + " de " + ci.bots.size());
        info.add("   desinformación: " + ci.desinformacion() + " % de las personas");
        info.add("GRAFO DE LA CIUDAD: " + ci.mapa.getZonas().size() + " zonas, " + ci.mapa.calles().size() + " calles");
        int pan = 0;
        for (int p : ci.panico) {
            pan += p > 0 ? 1 : 0;
        }
        info.add("   zonas con pánico (BFS): " + pan);
        if (ultimo != null) {
            info.add("ÚLTIMO SELLO: " + ultimo.ruta);
        }
        g.setFont(Tema.fuente(Font.PLAIN, 14));
        g.setColor(Tema.texto());
        int y = d.y + 60;
        for (String s : info) {
            for (String p : Avisos.partir(g, g.getFont(), s, d.width - 30)) {
                g.drawString(p, d.x + 16, y);
                y += 21;
            }
        }
        g.setFont(Tema.fuente(Font.PLAIN, 12));
        g.setColor(Tema.textoSuave());
        g.drawString("Naranja = nodo INSERTADO al confrontar · morado = actual / camino · verde = visitado / encontrado",
                40, 842);
        boton(g, new Rectangle(1300, 24, 100, 32), "Cerrar", Tema.acento(), () -> capa = Capa.NADA);
    }

    private void pausa(Graphics2D g) {
        oscurecer(g);
        Rectangle r = new Rectangle(520, 180, 400, 470);
        panel(g, r, "PAUSA");
        int y = r.y + 60;
        boton(g, new Rectangle(r.x + 40, y, 320, 44), "Continuar", Tema.acento(), () -> capa = Capa.NADA);
        y += 56;
        boton(g, new Rectangle(r.x + 40, y, 320, 40), "Alto contraste: " + (Tema.altoContraste ? "SÍ" : "no"),
                Tema.tarjeta(), () -> Tema.altoContraste = !Tema.altoContraste);
        y += 50;
        boton(g, new Rectangle(r.x + 40, y, 320, 40), "Texto grande: " + (Tema.textoGrande ? "SÍ" : "no"),
                Tema.tarjeta(), () -> Tema.textoGrande = !Tema.textoGrande);
        y += 50;
        boton(g, new Rectangle(r.x + 40, y, 320, 40), "Sonido: " + (Tema.sonido ? "SÍ" : "no"), Tema.tarjeta(),
                () -> Tema.sonido = !Tema.sonido);
        y += 50;
        boton(g, new Rectangle(r.x + 40, y, 320, 40), "Subtítulos de sonido: " + (Tema.subtitulos ? "SÍ" : "no"),
                Tema.tarjeta(), () -> Tema.subtitulos = !Tema.subtitulos);
        y += 50;
        boton(g, new Rectangle(r.x + 40, y, 320, 40), "Manual", Tema.tarjeta(), () -> capa = Capa.MANUAL);
        y += 60;
        boton(g, new Rectangle(r.x + 40, y, 320, 40), "Salir al menú", Tema.mal(), () -> {
            capa = Capa.NADA;
            reloj.stop();
            if (oyente != null) {
                oyente.alSalir();
            }
        });
    }

    private void tarjetaTurno(Graphics2D g) {
        oscurecer(g);
        Rectangle r = new Rectangle(420, 250, 600, 300);
        panel(g, r, null);
        g.setFont(Tema.fuente(Font.BOLD, 34));
        g.setColor(Tema.acento2());
        String s = "Turno de " + j.jugador.nombre;
        g.drawString(s, r.x + (r.width - g.getFontMetrics().stringWidth(s)) / 2, r.y + 90);
        g.setFont(Tema.fuente(Font.PLAIN, 18));
        g.setColor(Tema.texto());
        s = "Día " + j.dia + " · " + Historia.TITULOS[j.dia] + " · los demás, ¡no miren!";
        g.drawString(s, r.x + (r.width - g.getFontMetrics().stringWidth(s)) / 2, r.y + 140);
        botonGrande(g, new Rectangle(r.x + 150, r.y + 190, 300, 60), "Empezar  (Espacio)", Tema.acento(),
                this::empezarLlamada);
    }

    private void finDia(Graphics2D g) {
        oscurecer(g);
        Rectangle r = new Rectangle(270, 60, 900, 740);
        panel(g, r, null);
        JugadorVerificador jv = j.jugador;
        g.setFont(Tema.fuente(Font.BOLD, 30));
        g.setColor(jv.despedido ? Tema.mal() : Tema.acento2());
        g.drawString(jv.despedido ? "¡DESPEDIDO!" : "FIN DEL DÍA " + j.dia, r.x + 30, r.y + 50);
        g.setFont(Tema.fuente(Font.PLAIN, 17));
        g.setColor(Tema.texto());
        int y = r.y + 90;
        String[] l = {
            "Aciertos: " + j.getAciertosDia() + "   ·   Errores: " + j.getErroresDia() + "   ·   Sin atender: " + j.sinAtender(),
            "Puntos del día: " + j.getPuntosDia() + "   ·   Total: " + jv.puntos + "   ·   Dinero: $" + jv.dinero + " (sueldo +$100)",
            "Advertencias: " + jv.advertencias + "/" + JugadorVerificador.MAX_ADVERTENCIAS + "   ·   Reputación: " + jv.reputacion,
            "Desinformación en la ciudad: " + jv.ciudad.desinformacion() + " %   ·   Confianza: " + jv.ciudad.confianza
                + "   ·   Salud de la ciudad: " + jv.ciudad.salud()
        };
        for (String s : l) {
            g.drawString(s, r.x + 30, y);
            y += 28;
        }
        y += 10;
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(new Color(255, 200, 80));
        g.drawString("TITULARES DE MAÑANA", r.x + 30, y);
        y += 28;
        g.setFont(Tema.fuente(Font.PLAIN, 15));
        g.setColor(Tema.texto());
        for (Jornada.Resultado res : j.getResultados()) {
            Iconos.marca(g, res.efecto.acierto ? 2 : 3, r.x + 38, y - 5, 7);
            g.setColor(Tema.texto());
            g.drawString(VistaArboles.corto(res.titular, 95), r.x + 54, y);
            y += 24;
        }
        if (!jv.logros.isEmpty()) {
            y += 10;
            g.setFont(Tema.fuente(Font.BOLD, 16));
            g.setColor(new Color(255, 200, 80));
            g.drawString("LOGROS: " + jv.logros.size(), r.x + 30, y);
            y += 24;
            g.setFont(Tema.fuente(Font.PLAIN, 14));
            g.setColor(Tema.textoSuave());
            for (String lg : jv.logros) {
                if (y > r.y + r.height - 100) {
                    break;
                }
                Iconos.marca(g, 4, r.x + 46, y - 5, 7);
                g.drawString(lg, r.x + 60, y);
                y += 20;
            }
        }
        String txt = partida.terminada() || (j.dia == Historia.DIAS && partida.esUltimoTurnoDelDia()) || jv.despedido
                && partida.jugadores.size() == 1 ? "Ver el final  (Espacio)" : "Continuar  (Espacio)";
        botonGrande(g, new Rectangle(r.x + r.width - 360, r.y + r.height - 80, 330, 56), txt, Tema.acento(), () -> {
            if (oyente != null) {
                oyente.alTerminarDia(j);
            }
        });
    }

    // ------------------------------------------------------------ utilidades de dibujo
    private void panel(Graphics2D g, Rectangle r, String titulo) {
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(18, 22, 40));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g.setColor(Tema.borde());
        g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        if (titulo != null) {
            g.setFont(Tema.fuente(Font.BOLD, 15));
            g.setColor(Tema.acento2());
            g.drawString(titulo, r.x + 14, r.y + 26);
        }
    }

    private void boton(Graphics2D g, Rectangle r, String txt, Color fondo, Runnable accion) {
        boolean hover = r.contains(mx, my);
        Color f = hover ? Escala.mezclar(fondo, Color.WHITE, 0.18) : fondo;
        g.setColor(f);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g.setColor(hover ? Color.WHITE : Tema.borde());
        g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        Font fnt = Tema.fuente(Font.BOLD, 13.5f);
        g.setFont(fnt);
        g.setColor(VistaArboles.esClaro(f) ? Color.BLACK : Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        String s = txt;
        while (fm.stringWidth(s) > r.width - 20 && s.length() > 4) {
            s = s.substring(0, s.length() - 2);
        }
        if (!s.equals(txt)) {
            s = s + "…";
        }
        int x = r.width > 250 && txt.matches("^\\d\\..*") ? r.x + 12 : r.x + (r.width - fm.stringWidth(s)) / 2;
        g.drawString(s, x, r.y + (r.height + fm.getAscent()) / 2 - 3);
        botones.add(new Boton(r, accion));
    }

    private void botonGrande(Graphics2D g, Rectangle r, String txt, Color fondo, Runnable accion) {
        boolean hover = r.contains(mx, my);
        double pulso = 0.5 + 0.5 * Math.sin(t * 3);
        g.setColor(new Color(fondo.getRed(), fondo.getGreen(), fondo.getBlue(), (int) (60 + 60 * pulso)));
        g.fillRoundRect(r.x - 4, r.y - 4, r.width + 8, r.height + 8, 16, 16);
        boton(g, r, txt, hover ? fondo.brighter() : fondo, accion);
    }

    private void centrado(Graphics2D g, String s, Rectangle r, Color c) {
        g.setFont(Tema.fuente(Font.BOLD, 16));
        g.setColor(c);
        g.drawString(s, r.x + (r.width - g.getFontMetrics().stringWidth(s)) / 2, r.y + r.height / 2);
    }

    /** Utilidad de color. */
    static final class Escala {
        private Escala() {
        }

        static Color mezclar(Color a, Color b, double k) {
            k = Math.max(0, Math.min(1, k));
            return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * k),
                    (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                    (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * k));
        }
    }

    // ------------------------------------------------------------ para las pruebas
    /** Simula teclas (las pruebas automaticas y la captura de pantallas lo usan). */
    public void simularTecla(int codigo, char c) {
        tecla(new KeyEvent(this, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, codigo, c));
    }

    public Jornada getJornada() {
        return j;
    }

    public void forzarCapa(String nombre) {
        capa = Capa.valueOf(nombre);
    }
}
