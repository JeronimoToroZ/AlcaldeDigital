package influenceinc.gui;

import influenceinc.estructuras.GrafoCiudad;
import influenceinc.estructuras.NodoDialogo;
import influenceinc.logica.Caso;
import influenceinc.logica.CiudadEstado;
import influenceinc.logica.Habilidades;
import influenceinc.logica.Historia;
import influenceinc.logica.Jornada;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Partida;
import influenceinc.logica.Tienda;
import influenceinc.modelo.Cuenta;
import influenceinc.modelo.Herramienta;
import influenceinc.modelo.Persona;
import influenceinc.modelo.Publicacion;
import influenceinc.modelo.RolJugador;
import influenceinc.modelo.Veredicto;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * LA OFICINA. Todo lo que ve el jugador durante un dia se dibuja aqui con
 * Java2D, sobre un lienzo logico de 1440 x 860 que se escala a la ventana
 * (el fondo "fondo2" cubre todo, asi no quedan franjas vacias).
 *
 *  ┌ barra: jugador y rol · dia · reloj · fila · advertencias · puntos · [ayuda][estructura][mapa][pausa] ┐
 *  │ VENTANA DE INTERROGATORIO │   CELULAR    │ MONITOR (herramientas + habilidad)  │
 *  │ (cielo segun la hora,     │  (la noticia │                                     │
 *  │  billetera si hay carne)  │   en Civitas)│                                     │
 *  │ globo de dialogo          │              │ LIBRETA DE PISTAS                   │
 *  │ PREGUNTAS (se arrastran)  │   SELLOS     │ INDICADORES + noticiero             │
 *  └──────────────────────────────────────────────────────────────────────────────┘
 *
 * Los 4 botones de la barra estan en TODAS las pantallas de TODOS los roles y
 * su contenido cambia segun el rol del jugador activo.
 */
public class OficinaVista extends Lienzo {

    public static final int W = 1440, H = 860;

    /** Lo que la ventana necesita saber. */
    public interface Oyente {
        void alSellar(Jornada j, Jornada.Resultado r);

        void alTerminarDia(Jornada j);

        void alSalir();
    }

    private enum Estado { TURNO, LLAMADA, VACIO, ENTRANDO, ATENDIENDO, SELLANDO, RESULTADO, SALIENDO, FIN_DIA }

    /** Capas del juego (tapan el escritorio). */
    private enum Juego { NADA, LUPA, BILLETERA, MINIJUEGO }

    /** Paneles de los 4 botones de la barra (no tapan la barra). */
    private enum Panel { NADA, AYUDA, ESTRUCTURAS, PAUSA, MAPA, TIENDA }

    // zonas de la pantalla
    private static final int BARRA = 64;
    private static final Rectangle VENTANA = new Rectangle(16, 76, 540, 330);
    private static final Rectangle GLOBO = new Rectangle(16, 416, 540, 94);
    private static final Rectangle PREG = new Rectangle(16, 520, 540, 324);
    private static final Rectangle CEL = new Rectangle(580, 70, 310, 599);
    private static final Rectangle SELLOS = new Rectangle(568, 678, 334, 166);
    private static final Rectangle MONITOR = new Rectangle(914, 76, 510, 432);
    private static final Rectangle LIBRETA = new Rectangle(914, 518, 510, 270);
    /** Noticiero (los indicadores de la ciudad ahora estan dentro del boton MAPA). */
    private static final Rectangle TV = new Rectangle(914, 798, 510, 46);

    private final Oyente oyente;
    final Avisos avisos = new Avisos();
    private final VistaRed3D red3d = new VistaRed3D();
    private final Random azar = new Random();
    private boolean activo;

    private Partida partida;
    private Jornada j;
    private Estado estado = Estado.VACIO;
    private Juego juego = Juego.NADA;
    private Panel panel = Panel.NADA;
    private int tab = 0;                 // 0..5 herramientas, 6 = habilidad del rol
    private Herramienta escaneando;
    private double escaneoT;
    private int paginaAyuda;
    private MiniJuego mini;
    private Habilidades.Viral viralEnEspera;

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

    // arrastre
    private Veredicto arrastraSello;
    private NodoDialogo arrastraPregunta;
    private int pressX, pressY, dragX, dragY;
    private final List<Object[]> zonasSellos = new ArrayList<>();
    private final List<Object[]> zonasPreguntas = new ArrayList<>();
    private int libretaScroll;
    private int lupaFallos;
    private String lupaMsg;
    // aviso flotante rojo (herramienta que no aplica, compra imposible...)
    private String alertaTxt;
    private double alertaT = -10;
    private String tiendaMsg;
    private boolean tiendaOk;
    private double tiendaT = -10;

    public OficinaVista(Oyente oyente) {
        super(W, H, false);
        this.oyente = oyente;
        avisos.haciaArriba = true;
        avisos.maxVisibles = 3;
        avisos.subtituloY = 646;
        Sonido.setSubtitulos(avisos);
    }

    // ==================================================================
    // Acceso para los mini-juegos
    // ==================================================================

    Jornada jornada() {
        return j;
    }

    Caso caso() {
        return caso;
    }

    void avisar(String titulo, String txt) {
        avisos.agregar(titulo, txt, Tema.acento2(), 0);
    }

    /** AVISO FLOTANTE ROJO: explica por que algo no se puede hacer en este momento. */
    void alertaRoja(String txt) {
        alertaTxt = txt;
        alertaT = t;
        Sonido.tocar(Sonido.Tipo.MAL);
    }

    void anotarHabilidad(List<String> pistas, boolean exito) {
        for (String p : pistas) {
            avisos.agregar(exito ? "Habilidad: ¡bien hecho!" : "Habilidad", Iconos.sinPrefijo(p),
                    exito ? Tema.bien() : Tema.mal(), 0);
        }
        Sonido.tocar(exito ? Sonido.Tipo.LOGRO : Sonido.Tipo.MAL);
        tab = Herramienta.values().length;
    }

    void anotarViral(boolean exito, int puntos) {
        ticker.add(0, exito ? "CIVITAS: una alerta viral fue frenada a tiempo" : "CIVITAS: un rumor viral sigue circulando");
        avisos.agregar("Alerta viral", (puntos >= 0 ? "+" : "") + puntos + " puntos", exito ? Tema.bien() : Tema.mal(), 0);
    }

    // ==================================================================
    // Ciclo del dia
    // ==================================================================

    public void empezar(Partida p) {
        this.partida = p;
        activo = true;
        nuevoTurno();
        requestFocusInWindow();
    }

    /** Lo llama la ventana despues de alTerminarDia si la partida sigue. */
    public void nuevoTurno() {
        j = partida.iniciarJornada();
        caso = null;
        ultimo = null;
        juego = Juego.NADA;
        panel = Panel.NADA;
        mini = null;
        viralEnEspera = null;
        texto = "";
        ticker.clear();
        ticker.add("NOTICIAS NOVA · Día " + j.dia + " de la semana electoral · " + Historia.TITULOS[j.dia]);
        if (partida.jugadores.size() > 1) {
            estado = Estado.TURNO;
        } else {
            empezarLlamada();
        }
    }

    public void detener() {
        activo = false;
    }

    public void setMarcadorRed(List<String> m) {
        marcadorRed = m;
    }

    private RolJugador rol() {
        return j == null ? RolJugador.PERIODISTA : j.jugador.rol;
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
                avisos.agregar("Cómo se juega", "1) Haz pasar al visitante. 2) Arrastra preguntas hacia él. "
                        + "3) Investiga en el monitor" + (rol().tieneMiniJuego() ? " y usa tu habilidad (pestaña \"Habilidad\")" : "")
                        + ". 4) Arrastra un sello sobre el celular. Botón de ayuda arriba.", Tema.acento(), 300);
            }
            for (Herramienta h : Herramienta.values()) {
                if (h.dia == j.dia && j.delRol(h)) {
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
        // cliente nuevo: TODAS las herramientas se reinician y quedan disponibles otra vez
        tab = 0;
        escaneando = null;
        escaneoT = 0;
        if (juego == Juego.LUPA || juego == Juego.BILLETERA) {
            juego = Juego.NADA;
        }
        arrastraSello = null;
        arrastraPregunta = null;
        llamadaInst = null;
        ultimo = null;
        libretaScroll = 0;
        lupaFallos = 0;
        lupaMsg = null;
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
            avisos.agregar("¡Logro!", l, Tema.DORADO, 400);
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
        tab = h.ordinal();
        if (caso == null || estado != Estado.ATENDIENDO || escaneando != null) {
            return;
        }
        String no = j.motivoNoAplica(h);
        if (no != null) {
            alertaRoja(no);
            return;
        }
        if (h == Herramienta.LUPA) {
            abrirLupa();
        }
        if (j.resultados(h) == null || h == Herramienta.CORRESPONSAL) {
            escaneando = h;
            escaneoT = 0;
            Sonido.tocar(Sonido.Tipo.ESCANER);
        }
    }

    private void abrirLupa() {
        String no = j == null ? "..." : j.motivoNoAplica(Herramienta.LUPA);
        if (no != null) {
            if (j != null && caso != null) {
                alertaRoja(no);
            }
            return;
        }
        juego = Juego.LUPA;
        lupaMsg = null;
    }

    private void usarHabilidad() {
        if (!rol().tieneMiniJuego()) {
            avisos.agregar("Tu habilidad", "Como periodista tienes TODO el monitor: corresponsal, escáner de red y llamadas.",
                    Tema.acento2(), 0);
            return;
        }
        tab = Herramienta.values().length;
        if (caso == null || estado != Estado.ATENDIENDO) {
            return;
        }
        if (!j.puedeUsarHabilidad()) {
            alertaRoja("Ya usaste tu habilidad con este cliente. Vuelve a tenerla con el siguiente (o compra una recarga en la tienda).");
            return;
        }
        switch (rol()) {
            case CIUDADANO: mini = new MiniJuegos.Cadena(this); break;
            case INFLUENCER: mini = new MiniJuegos.Cazabots(this); break;
            case CANDIDATO: mini = new MiniJuegos.Prensa(this); break;
            default: return;
        }
        juego = Juego.MINIJUEGO;
        Sonido.tocar(Sonido.Tipo.TURNO);
    }

    private void cerrarMiniJuego() {
        if (mini != null) {
            mini.cerrar();
        }
        mini = null;
        juego = Juego.NADA;
    }

    private void llamar() {
        if (caso == null || estado != Estado.ATENDIENDO) {
            return;
        }
        if (!j.puedeLlamar()) {
            String no = j.motivoNoAplica(Herramienta.DIRECTORIO);
            alertaRoja(no != null ? no : "Ya llamaste a la institución por este caso.");
            return;
        }
        Sonido.tocar(Sonido.Tipo.TELEFONO);
        llamadaInst = j.llamar();
        avisos.agregar("Llamada", llamadaInst, Tema.acento2(), 0);
    }

    private void acusar() {
        boolean ok = j.acusarFoto();
        juego = Juego.NADA;
        if (ok) {
            avisos.agregar("¡Bien visto!", "La foto no coincide. Confróntalo en las preguntas.", Tema.bien(), 0);
            Sonido.tocar(Sonido.Tipo.ALERTA);
        } else {
            avisos.agregar("Hmm...", "La foto sí coincide. El visitante se ofendió un poco.", Tema.mal(), 0);
        }
    }

    /** Lupa interactiva: el jugador senala la zona de la foto donde ve la diferencia. */
    private void senalarZona(String zona) {
        Persona p = caso.visitante;
        if (p.diferencia == null) {
            acusar();                       // no habia diferencia: se equivoco al acusar
            return;
        }
        String d = p.diferencia.toLowerCase();
        boolean acierta = (zona.equals("ojos") && (d.contains("ojos") || d.contains("gafas")))
                || (zona.equals("cejas") && d.contains("cejas")) || (zona.equals("pelo") && d.contains("pelo"))
                || (zona.equals("lunar") && d.contains("lunar"));
        if (acierta) {
            acusar();
        } else {
            lupaFallos++;
            Sonido.tocar(Sonido.Tipo.MAL);
            lupaMsg = lupaFallos >= 2 ? "Pista: la diferencia es otra. Compara " + pistaZona(d) + "."
                    : "Ahí no hay diferencia. Mira con más cuidado.";
        }
    }

    private static String pistaZona(String d) {
        return d.contains("gafas") || d.contains("ojos") ? "los ojos" : d.contains("cejas") ? "las cejas"
                : d.contains("pelo") ? "el pelo" : "las mejillas";
    }

    // ==================================================================
    // Teclado (todo el juego se puede jugar sin mouse)
    // ==================================================================

    @Override
    protected void tecla(KeyEvent e) {
        if (j == null) {
            return;
        }
        int k = e.getKeyCode();
        if (juego == Juego.MINIJUEGO && mini != null && panel == Panel.NADA) {
            mini.tecla(e);
            return;
        }
        if (k == KeyEvent.VK_ESCAPE) {
            if (panel != Panel.NADA) {
                panel = Panel.NADA;
            } else if (juego == Juego.LUPA || juego == Juego.BILLETERA) {
                juego = Juego.NADA;
            } else {
                panel = Panel.PAUSA;
            }
            return;
        }
        if (k == KeyEvent.VK_F1 || k == KeyEvent.VK_M || k == KeyEvent.VK_H) {
            panel = panel == Panel.AYUDA ? Panel.NADA : Panel.AYUDA;
            return;
        }
        if (k == KeyEvent.VK_F2) {
            panel = panel == Panel.ESTRUCTURAS ? Panel.NADA : Panel.ESTRUCTURAS;
            return;
        }
        if (k == KeyEvent.VK_F3 || k == KeyEvent.VK_G) {
            panel = panel == Panel.MAPA ? Panel.NADA : Panel.MAPA;
            return;
        }
        if (k == KeyEvent.VK_F4 || k == KeyEvent.VK_S) {
            panel = panel == Panel.TIENDA ? Panel.NADA : Panel.TIENDA;
            return;
        }
        if (panel == Panel.TIENDA) {
            int i = k - KeyEvent.VK_1;
            if (i >= 0 && i < Tienda.Articulo.values().length) {
                comprar(Tienda.Articulo.values()[i]);
            }
            return;
        }
        if (panel == Panel.AYUDA) {
            if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_RIGHT) {
                paginaAyuda = Math.min(paginaAyuda + 1, 8);
            } else if (k == KeyEvent.VK_UP || k == KeyEvent.VK_LEFT) {
                paginaAyuda = Math.max(0, paginaAyuda - 1);
            }
            return;
        }
        if (panel != Panel.NADA && panel != Panel.MAPA) {
            return;
        }
        if (juego == Juego.LUPA) {
            if (k == KeyEvent.VK_X) {
                acusar();
            } else if (k == KeyEvent.VK_L || k == KeyEvent.VK_ENTER) {
                juego = Juego.NADA;
            }
            return;
        }
        if (juego == Juego.BILLETERA) {
            if (k == KeyEvent.VK_L) {
                abrirLupa();
            } else if (k == KeyEvent.VK_I || k == KeyEvent.VK_ENTER) {
                juego = Juego.NADA;
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
        if (k == KeyEvent.VK_U) {
            usarHabilidad();
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
        if (k == KeyEvent.VK_I && caso != null && caso.visitante.credencial != null) {
            juego = Juego.BILLETERA;
            return;
        }
        String sel = "ZXCVB";
        int si = sel.indexOf(Character.toUpperCase(e.getKeyChar()));
        if (si >= 0) {
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

    // ==================================================================
    // Mouse: arrastrar sellos y preguntas
    // ==================================================================

    private boolean escritorioLibre() {
        return juego == Juego.NADA && (panel == Panel.NADA);
    }

    @Override
    protected boolean presionar(int x, int y) {
        if (j == null) {
            return false;
        }
        if (y < BARRA) {
            return false;               // la barra siempre responde con sus zonas
        }
        if (panel != Panel.NADA) {
            return false;
        }
        if (juego == Juego.MINIJUEGO && mini != null) {
            return mini.presionar(x, y);
        }
        if (!escritorioLibre()) {
            return false;
        }
        pressX = x;
        pressY = y;
        dragX = x;
        dragY = y;
        if (estado == Estado.ATENDIENDO) {
            for (Object[] z : zonasSellos) {
                if (((Rectangle) z[0]).contains(x, y)) {
                    arrastraSello = (Veredicto) z[1];
                    Sonido.tocar(Sonido.Tipo.CLIC);
                    return true;
                }
            }
        }
        if (estado == Estado.ATENDIENDO || estado == Estado.ENTRANDO) {
            for (Object[] z : zonasPreguntas) {
                if (((Rectangle) z[0]).contains(x, y)) {
                    arrastraPregunta = (NodoDialogo) z[1];
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void arrastrar(int x, int y) {
        dragX = x;
        dragY = y;
        if (mini != null && juego == Juego.MINIJUEGO) {
            mini.arrastrar(x, y);
        }
    }

    @Override
    protected void soltar(int x, int y) {
        if (mini != null && juego == Juego.MINIJUEGO) {
            mini.soltar(x, y);
            return;
        }
        boolean clic = Math.abs(x - pressX) < 7 && Math.abs(y - pressY) < 7;
        if (arrastraSello != null) {
            Veredicto v = arrastraSello;
            arrastraSello = null;
            if (CEL.contains(x, y)) {
                sellar(v);
            } else if (clic) {
                avisos.agregar("Arrastra el sello", "Toma el sello " + v.sello + " y suéltalo sobre la publicación del celular.",
                        Tema.acento2(), 0);
            }
            return;
        }
        if (arrastraPregunta != null) {
            NodoDialogo n = arrastraPregunta;
            arrastraPregunta = null;
            if (clic || VENTANA.contains(x, y) || GLOBO.contains(x, y)) {
                preguntar(n);
            }
        }
    }

    @Override
    protected void rueda(int clics) {
        if (LIBRETA.contains(mx, my)) {
            libretaScroll = Math.max(0, libretaScroll + clics);
        }
    }

    @Override
    protected boolean cursorMano() {
        if (!escritorioLibre()) {
            return false;
        }
        for (Object[] z : zonasSellos) {
            if (((Rectangle) z[0]).contains(mx, my)) {
                return true;
            }
        }
        for (Object[] z : zonasPreguntas) {
            if (((Rectangle) z[0]).contains(mx, my)) {
                return true;
            }
        }
        return false;
    }

    // ==================================================================
    // Actualizacion
    // ==================================================================

    private void actualizar(double dt) {
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
        if (j == null || !activo) {
            return;
        }
        if (mini != null && panel == Panel.NADA) {
            mini.actualizar(dt);
            if (mini.terminado) {
                cerrarMiniJuego();
            }
        }
        boolean corre = juego == Juego.NADA && (panel == Panel.NADA || panel == Panel.MAPA)
                && (estado == Estado.ATENDIENDO || estado == Estado.VACIO || estado == Estado.ENTRANDO);
        if (corre) {
            for (Jornada.Evento ev : j.avanzar(dt)) {
                avisos.agregar("Evento", ev.texto, ev == Jornada.Evento.PANICO ? Tema.mal() : Tema.acento2(), 0);
                Sonido.tocar(Sonido.Tipo.ALERTA);
                if (ev == Jornada.Evento.VIRAL) {
                    viralEnEspera = j.tomarViral();
                }
            }
            String rep = j.revisarCorresponsal();
            if (rep != null) {
                avisos.agregar("Corresponsal", rep, Tema.acento2(), 0);
                Sonido.tocar(Sonido.Tipo.TELEFONO);
            }
        }
        // la alerta viral aparece apenas el escritorio esta libre (decision con 10 segundos)
        if (viralEnEspera != null && juego == Juego.NADA && panel == Panel.NADA && estado != Estado.SELLANDO
                && estado != Estado.FIN_DIA && estado != Estado.TURNO && estado != Estado.LLAMADA) {
            mini = new MiniJuegos.Viral(this, viralEnEspera);
            viralEnEspera = null;
            juego = Juego.MINIJUEGO;
            arrastraSello = null;
            arrastraPregunta = null;
            Sonido.tocar(Sonido.Tipo.ALERTA);
        }
        if (j.isTerminada() && estado != Estado.FIN_DIA && estado != Estado.RESULTADO && estado != Estado.SELLANDO
                && estado != Estado.TURNO && estado != Estado.LLAMADA) {
            if (caso != null && estado != Estado.SALIENDO) {
                avisos.agregar("18:00", "¡Cerramos! Lo que quedó en la fila circula sin verificar.", Tema.mal(), 0);
            }
            estado = Estado.FIN_DIA;
            caso = null;
            juego = Juego.NADA;
            mini = null;
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
    protected void fondoVentana(Graphics2D g, int w, int h) {
        g.setColor(Tema.altoContraste ? Color.BLACK : Tema.ROSA);
        g.fillRect(0, 0, w, h);
        if (!Tema.altoContraste) {
            Recursos.cubrir(g, "fondo2.jpg", 0, 0, w, h);
        }
    }

    @Override
    protected void pintar(Graphics2D g, double dt) {
        actualizar(dt);
        zonasSellos.clear();
        zonasPreguntas.clear();
        if (j == null) {
            return;
        }
        ventana(g);
        globo(g);
        preguntas(g);
        celular(g);
        sellos(g);
        monitor(g);
        libreta(g);
        tv(g);
        if (estado == Estado.SELLANDO) {
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
        switch (juego) {
            case LUPA:
                lupa(g);
                break;
            case BILLETERA:
                billetera(g);
                break;
            case MINIJUEGO:
                if (mini != null) {
                    mini.dibujar(g);
                }
                break;
            default:
                break;
        }
        switch (panel) {
            case AYUDA:
                ayuda(g);
                break;
            case ESTRUCTURAS:
                estructuras(g);
                break;
            case PAUSA:
                pausa(g);
                break;
            case MAPA:
                panelMapa(g);
                break;
            case TIENDA:
                tienda(g);
                break;
            default:
                break;
        }
        barra(g);
        alertaFlotante(g);
        if (juego == Juego.NADA && panel == Panel.NADA && estado != Estado.FIN_DIA && estado != Estado.TURNO) {
            avisos.dibujar(g, VENTANA.x + 12, VENTANA.y + VENTANA.height - 46, 430, H, W);
        } else {
            avisos.dibujar(g, -2000, 88, 10, H, W);   // solo los subtitulos
        }
        if (escritorioLibre()) {
            arrastrados(g);
        }
    }

    /** Lo que se esta arrastrando (sello o pregunta) va encima de todo. */
    private void arrastrados(Graphics2D g) {
        if (arrastraSello != null) {
            boolean sobre = CEL.contains(dragX, dragY);
            if (!Recursos.contener(g, arrastraSello.imagen(), dragX - 50, dragY - 58, 100, 100)) {
                Dibujo.selloRespaldo(g, dragX, dragY - 8, 46, colorSello(arrastraSello), arrastraSello.sello);
            }
            if (sobre) {
                g.setFont(Tema.fuente(Font.BOLD, 13));
                int tw = g.getFontMetrics().stringWidth("¡Suelta para sellar!") + 24;
                g.setColor(Tema.altoContraste ? Color.BLACK : Tema.TINTA);
                g.fillRoundRect(dragX - tw / 2, dragY + 44, tw, 26, 13, 13);
                g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.CREMA);
                Dibujo.centrado(g, "¡Suelta para sellar!", dragX, dragY + 62);
            }
        }
        if (arrastraPregunta != null && (Math.abs(dragX - pressX) > 6 || Math.abs(dragY - pressY) > 6)) {
            Rectangle r = new Rectangle(dragX - 200, dragY - 22, 400, 44);
            Graphics2D s = (Graphics2D) g.create();
            s.rotate(Math.toRadians(-2), dragX, dragY);
            tarjetaPregunta(s, r, 0, arrastraPregunta, true, true);
            s.dispose();
        }
    }

    // ------------------------------------------------------------ barra superior
    private void barra(Graphics2D g) {
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
        } else {
            g.setPaint(new GradientPaint(0, 0, new Color(74, 40, 28), 0, BARRA, new Color(42, 23, 18)));
        }
        g.fillRect(0, 0, W, BARRA);
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(242, 179, 94, 120));
        g.fillRect(0, BARRA - 2, W, 2);
        JugadorVerificador jv = j.jugador;
        // el jugador y su rol (su "personaje")
        Dibujo.emblemaRol(g, jv.rol, 40, 32, 22, true);
        g.setFont(Tema.fuente(Font.BOLD, 16));
        g.setColor(Tema.texto());
        g.drawString(Dibujo.recortar(g.getFontMetrics(), jv.nombre, 150), 72, 28);
        g.setFont(Tema.fuente(Font.PLAIN, 12));
        g.setColor(Tema.textoSuave());
        g.drawString(jv.rol.nombre, 72, 47);
        // dia
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.acento2());
        g.drawString("DÍA " + j.dia + " DE " + Historia.DIAS, 236, 26);
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(Tema.texto());
        g.drawString(Historia.TITULOS[j.dia], 236, 46);
        // reloj
        boolean tarde = j.getMinuto() > Jornada.CIERRE - 60;
        Rectangle rl = new Rectangle(392, 12, 150, 40);
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(255, 235, 215, 25));
        g.fillRoundRect(rl.x, rl.y, rl.width, rl.height, 20, 20);
        g.setFont(Tema.fuente(Font.BOLD, 22));
        g.setColor(tarde && ((int) (t * 2)) % 2 == 0 ? Tema.mal() : Tema.acento2());
        g.drawString(j.relajado ? "--:--" : j.hora(), rl.x + 14, rl.y + 29);
        g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
        g.setColor(Tema.textoSuave());
        String est = j.relajado ? "relajado" : j.enPausa() ? "café" : j.enApagon() ? "¡apagón!" : "cierra 18:00";
        g.drawString(est, rl.x + 88, rl.y + 25);
        // fila
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.texto());
        g.drawString("En fila", 562, 26);
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(Tema.acento2());
        g.drawString(String.valueOf(j.enFila()), 562, 48);
        // advertencias
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.texto());
        g.drawString("Advertencias", 630, 26);
        for (int i = 0; i < JugadorVerificador.MAX_ADVERTENCIAS; i++) {
            boolean llena = i < jv.advertencias;
            int x = 630 + i * 24;
            g.setColor(llena ? Tema.mal() : Tema.altoContraste ? Color.WHITE : new Color(140, 88, 64));
            if (llena) {
                g.fillRoundRect(x, 33, 18, 20, 6, 6);
                g.setColor(Tema.CREMA);
                g.setFont(Tema.fuente(Font.BOLD, 13));
                g.drawString("!", x + 7, 48);
            } else {
                g.drawRoundRect(x, 33, 18, 20, 6, 6);
            }
        }
        // puntos
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.texto());
        g.drawString("Puntos", 760, 26);
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(Tema.acento2());
        g.drawString(String.valueOf(jv.puntos), 760, 48);
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.texto());
        g.drawString("Dinero", 850, 26);
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(Tema.acento2());
        g.drawString("$" + jv.dinero, 850, 48);
        if (!marcadorRed.isEmpty()) {
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(Tema.textoSuave());
            g.drawString(Dibujo.recortar(g.getFontMetrics(), "Red: " + String.join("  ·  ", marcadorRed), 175), 950, 38);
        }
        // los 4 botones con imagen: siempre visibles, en cualquier rol y pantalla
        // TIENDA: boton redondo dibujado con el mismo estilo de los botones con imagen
        Rectangle rt = new Rectangle(W - 5 * 58 - 6, 7, 50, 50);
        botonTienda(g, rt);
        String[][] b = {{"ayuda.png", "Ayuda (M)"}, {"estructura.png", "Estructuras (F2)"}, {"mapa.png", "Mapa y ciudad (F3)"},
            {"pausa.png", "Pausa (Esc)"}};
        Panel[] p = {Panel.AYUDA, Panel.ESTRUCTURAS, Panel.MAPA, Panel.PAUSA};
        for (int i = 0; i < 4; i++) {
            Rectangle r = new Rectangle(W - 4 * 58 - 6 + i * 58, 7, 50, 50);
            boolean activoP = panel == p[i];
            if (activoP && !Tema.altoContraste) {
                g.setColor(Tema.alfa(Tema.DORADO, 120));
                g.fill(new Ellipse2D.Double(r.x - 4, r.y - 4, r.width + 8, r.height + 8));
            }
            // el mapa late cuando el corresponsal va en camino o hay panico
            if (p[i] == Panel.MAPA && !activoP && (j.corresponsalEnCamino() || j.hayPanico())) {
                double pul = 0.5 + 0.5 * Math.sin(t * 5);
                g.setColor(Tema.alfa(j.hayPanico() ? Tema.LADRILLO : Tema.DORADO, (int) (60 + 100 * pul)));
                g.fill(new Ellipse2D.Double(r.x - 5, r.y - 5, r.width + 10, r.height + 10));
            }
            final Panel pp = p[i];
            botonImagen(g, b[i][0], r, b[i][1], () -> {
                panel = panel == pp ? Panel.NADA : pp;
                arrastraSello = null;
                arrastraPregunta = null;
            });
            // el mapa lleva la salud de la ciudad en una insignia
            if (p[i] == Panel.MAPA) {
                int salud = j.jugador.ciudad.salud();
                String sv = String.valueOf(salud);
                g.setFont(Tema.fuente(Font.BOLD, 10.5f));
                int bw = g.getFontMetrics().stringWidth(sv) + 10;
                g.setColor(salud >= 60 ? Tema.bien() : salud >= 45 ? Tema.acento() : Tema.mal());
                g.fillRoundRect(r.x + r.width - bw + 4, r.y + r.height - 14, bw, 16, 8, 8);
                g.setColor(Tema.altoContraste ? Color.BLACK : Tema.CREMA);
                g.drawString(sv, r.x + r.width - bw + 9, r.y + r.height - 2);
            }
        }
    }

    // ------------------------------------------------------------ ventana de interrogatorio
    /** Cielo segun la hora: manana (durazno), tarde (dorado), atardecer (malva), noche (ciruela). */
    private Color[] cielo(double dia) {
        double[] k = {0, 0.4, 0.72, 1};
        Color[][] c = {
            {new Color(246, 206, 184), new Color(252, 236, 216)},
            {new Color(238, 168, 108), new Color(250, 214, 160)},
            {new Color(150, 96, 140), new Color(232, 150, 120)},
            {new Color(46, 30, 66), new Color(104, 60, 94)}
        };
        int i = 0;
        while (i < k.length - 2 && dia > k[i + 1]) {
            i++;
        }
        double f = (dia - k[i]) / (k[i + 1] - k[i]);
        return new Color[]{Tema.mezclar(c[i][0], c[i + 1][0], f), Tema.mezclar(c[i][1], c[i + 1][1], f)};
    }

    private String momento(double dia) {
        return dia < 0.33 ? "Mañana" : dia < 0.72 ? "Tarde" : "Noche";
    }

    private void ventana(Graphics2D g) {
        Rectangle r = VENTANA;
        if (bufVentana == null) {
            bufVentana = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D b = bufVentana.createGraphics();
        b.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        b.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double dia = Math.max(0, Math.min(1, j.avanceDia()));
        Color[] cs = cielo(dia);
        if (Tema.altoContraste) {
            cs = new Color[]{Color.BLACK, new Color(30, 30, 30)};
        }
        b.setPaint(new GradientPaint(0, 0, cs[0], 0, r.height, cs[1]));
        b.fillRect(0, 0, r.width, r.height);
        // sol que cruza el cielo; de noche, luna y estrellas
        if (!Tema.altoContraste) {
            double sx = r.width * (0.1 + 0.8 * dia), sy = 150 - Math.sin(Math.PI * dia) * 120;
            if (dia < 0.8) {
                b.setPaint(new RadialGradientPaint(new Point2D.Double(sx, sy), 70f, new float[]{0f, 1f},
                        new Color[]{new Color(255, 230, 170, 160), new Color(255, 230, 170, 0)}));
                b.fill(new Ellipse2D.Double(sx - 70, sy - 70, 140, 140));
                b.setColor(new Color(255, 226, 160));
                b.fill(new Ellipse2D.Double(sx - 22, sy - 22, 44, 44));
            }
            double noche = Math.max(0, (dia - 0.7) / 0.3);
            if (noche > 0) {
                Random st = new Random(5);
                for (int i = 0; i < 40; i++) {
                    int a = (int) (noche * (120 + 100 * Math.sin(t * 2 + i)));
                    b.setColor(new Color(255, 245, 230, Math.max(0, Math.min(255, a))));
                    b.fillRect(st.nextInt(r.width), st.nextInt(160), 2, 2);
                }
                b.setColor(new Color(252, 240, 220, (int) (230 * noche)));
                b.fill(new Ellipse2D.Double(r.width - 110, 30, 38, 38));
                b.setColor(Tema.mezclar(cs[0], cs[1], 0.1));
                b.fill(new Ellipse2D.Double(r.width - 100, 24, 38, 38));
            }
        }
        // edificios de Ciudad Nova (en tonos de la paleta)
        Random rr = new Random(77);
        for (int i = 0; i < 12; i++) {
            int bw = 40 + rr.nextInt(50), bh = 70 + rr.nextInt(150), bx = i * 50 - 20;
            Color ed = Tema.altoContraste ? new Color(60, 60, 60)
                    : Tema.mezclar(new Color(120 + rr.nextInt(30), 70 + rr.nextInt(20), 56), new Color(56, 32, 40), dia * 0.8);
            b.setColor(ed);
            b.fillRect(bx, r.height - 96 - bh, bw, bh);
            b.setColor(new Color(255, 214, 140, dia > 0.6 ? 200 : 70));
            for (int wy = r.height - 86 - bh; wy < r.height - 106; wy += 18) {
                for (int wx = bx + 6; wx < bx + bw - 8; wx += 14) {
                    if (rr.nextInt(3) > 0) {
                        b.fillRect(wx, wy, 6, 8);
                    }
                }
            }
        }
        b.setColor(Tema.altoContraste ? new Color(40, 40, 40) : new Color(90, 58, 48));
        b.fillRect(0, r.height - 96, r.width, 96);
        // lluvia el dia 3
        if (j.dia == 3) {
            b.setColor(new Color(240, 220, 230, 120));
            for (int i = 0; i < 70; i++) {
                double x = (i * 37 + t * 220) % r.width, y = (i * 53 + t * 520) % r.height;
                b.drawLine((int) x, (int) y, (int) x - 4, (int) y + 12);
            }
        }
        // la fila de gente esperando (siluetas)
        int n = Math.min(6, j.enFila());
        for (int i = 0; i < n; i++) {
            int x = r.width - 60 - i * 34, y = r.height - 160;
            b.setColor(new Color(42, 23, 18, 170));
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
            Caras.dibujar(b, caso.visitante.cara, r.width / 2.0 + desliz, 150 + bob, 1.35, expr, parpadeoT < 0, habla);
        }
        // reflejo del vidrio y mostrador de madera
        b.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 46), r.width, r.height, new Color(255, 255, 255, 0)));
        b.fillRect(0, 0, r.width, r.height);
        b.setColor(new Color(255, 255, 255, 40));
        b.setStroke(new BasicStroke(10));
        b.drawLine(60, 30, 140, -10);
        b.drawLine(90, 60, 200, -10);
        b.setStroke(new BasicStroke(1));
        if (Tema.altoContraste) {
            b.setColor(Color.WHITE);
        } else {
            b.setPaint(new GradientPaint(0, r.height - 40, new Color(150, 84, 52), 0, r.height, new Color(96, 52, 32)));
        }
        b.fillRect(0, r.height - 40, r.width, 40);
        b.setColor(new Color(42, 23, 18));
        b.fillRoundRect(r.width / 2 - 62, r.height - 33, 124, 24, 12, 12);
        b.setFont(Tema.fuente(Font.BOLD, 11.5f));
        b.setColor(Tema.DORADO);
        Dibujo.centrado(b, "VENTANILLA 3", r.width / 2.0, r.height - 16);
        // chip de la hora del dia
        String mo = momento(dia);
        b.setFont(Tema.fuente(Font.BOLD, 11.5f));
        int cw = b.getFontMetrics().stringWidth("Interrogatorio · " + mo) + 24;
        b.setColor(new Color(42, 23, 18, 170));
        b.fillRoundRect(12, 12, cw, 24, 12, 12);
        b.setColor(Tema.CREMA);
        b.drawString("Interrogatorio · " + mo, 24, 29);
        if (estado == Estado.VACIO || estado == Estado.TURNO) {
            boolean sig = estado == Estado.VACIO;
            b.setColor(sig ? Tema.SALVIA : Tema.LADRILLO);
            b.fillRoundRect(r.width / 2 - 90, 110, 180, 54, 16, 16);
            b.setColor(Tema.CREMA);
            b.setFont(Tema.fuente(Font.BOLD, 22));
            Dibujo.centrado(b, sig ? "SIGUIENTE" : "CERRADO", r.width / 2.0, 146);
        }
        b.dispose();

        // marco de madera con sombra
        Dibujo.sombra(g, new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 18, 18), 6, 4);
        Graphics2D v = (Graphics2D) g.create();
        v.clip(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 18, 18));
        if (glitchT > 0) {
            for (int y = 0; y < r.height; y += 12) {
                int dx = azar.nextInt(5) == 0 ? azar.nextInt(30) - 15 : 0;
                v.drawImage(bufVentana, r.x + dx, r.y + y, r.x + dx + r.width, r.y + y + 12, 0, y, r.width, y + 12, null);
            }
            v.setColor(new Color(192, 71, 58, 50));
            v.fillRect(r.x + 4, r.y, r.width, r.height);
        } else {
            v.drawImage(bufVentana, r.x, r.y, null);
        }
        v.dispose();
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(110, 54, 33));
        g.setStroke(new BasicStroke(8));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 18, 18);
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(200, 130, 90, 150));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 14, 14);
        g.setStroke(new BasicStroke(1));
        // llamada del jefe: el telefono timbra dentro de la ventana
        if (estado == Estado.LLAMADA) {
            double sh = Math.sin(t * 30) * (((int) (t * 1.5)) % 2 == 0 ? 3 : 0);
            Rectangle tr = new Rectangle((int) (r.getCenterX() - 46 + sh), r.y + 92, 92, 92);
            g.setColor(Tema.alfa(Tema.DORADO, (int) (70 + 60 * Math.sin(t * 6))));
            g.fill(new Ellipse2D.Double(tr.x - 14, tr.y - 14, tr.width + 28, tr.height + 28));
            if (!Recursos.contener(g, "celular2.png", tr.x, tr.y, tr.width, tr.height)) {
                Dibujo.selloRespaldo(g, tr.getCenterX(), tr.getCenterY(), 44, Tema.COBRE, "TEL");
            }
            g.setFont(Tema.fuente(Font.BOLD, 15));
            int tw = g.getFontMetrics().stringWidth("Don Ramiro está llamando...") + 32;
            g.setColor(new Color(42, 23, 18, 200));
            g.fillRoundRect((int) r.getCenterX() - tw / 2, tr.y + tr.height + 14, tw, 30, 15, 15);
            g.setColor(Tema.CREMA);
            Dibujo.centrado(g, "Don Ramiro está llamando...", r.getCenterX(), tr.y + tr.height + 34);
        }
        // BILLETERA: solo si el visitante trae credencial
        if (caso != null && caso.visitante.credencial != null
                && (estado == Estado.ATENDIENDO || estado == Estado.ENTRANDO || estado == Estado.RESULTADO)) {
            Rectangle br = new Rectangle(r.x + r.width - 84, r.y + r.height - 116, 66, 66);
            br.y += (int) (Math.sin(t * 2.4) * 3);
            boolean hv = encima(br) && escritorioLibre();
            if (!Tema.altoContraste) {
                g.setColor(new Color(42, 23, 18, 120));
                g.fill(new Ellipse2D.Double(br.x - 6, br.y - 6, br.width + 12, br.height + 12));
            }
            Dibujo.botonImagen(g, "billetera.png", br, null, hv, t);
            g.setFont(Tema.fuente(Font.BOLD, 11));
            g.setColor(Tema.CREMA);
            Dibujo.centrado(g, "Credencial (I)", br.getCenterX(), br.y + br.height + 16);
            if (escritorioLibre()) {
                zona(br, () -> juego = Juego.BILLETERA);
            }
        }
    }

    private static double suave(double x) {
        return x * x * (3 - 2 * x);
    }

    // ------------------------------------------------------------ globo de dialogo
    private void globo(Graphics2D g) {
        Rectangle r = GLOBO;
        RoundRectangle2D s = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 22, 22);
        Dibujo.sombra(g, s, 4, 3);
        g.setColor(Tema.altoContraste ? Color.BLACK : Tema.PAPEL);
        g.fill(s);
        Path2D cola = new Path2D.Double();
        cola.moveTo(r.x + 80, r.y + 1);
        cola.lineTo(r.x + 96, r.y - 12);
        cola.lineTo(r.x + 112, r.y + 1);
        g.fill(cola);
        boolean destino = arrastraPregunta != null && (GLOBO.contains(dragX, dragY) || VENTANA.contains(dragX, dragY));
        g.setColor(destino ? Tema.acento2() : Tema.altoContraste ? Color.WHITE : new Color(200, 160, 140));
        g.setStroke(new BasicStroke(destino ? 3 : 1.4f));
        g.draw(s);
        g.setStroke(new BasicStroke(1));
        if (texto.isEmpty()) {
            g.setFont(Tema.fuente(Font.ITALIC, 14));
            g.setColor(Tema.tintaSuave());
            g.drawString(estado == Estado.VACIO ? "La ventanilla está libre. Haz pasar al siguiente." : "...", r.x + 18, r.y + 52);
            return;
        }
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.COBRE);
        g.drawString(quienHabla + (caso != null && estado != Estado.LLAMADA ? "  ·  " + caso.visitante.rol.etiqueta : ""),
                r.x + 18, r.y + 24);
        Font f = Tema.media(15);
        g.setColor(Tema.tinta());
        String vis = texto.substring(0, (int) letras);
        Dibujo.parrafo(g, f, vis, r.x + 18, r.y + 47, r.width - 40, 20, r.y + r.height - 6);
        if (letras >= texto.length() && ((int) (t * 2)) % 2 == 0) {
            Dibujo.triangulo(g, r.x + r.width - 20, r.y + r.height - 14, 5, false, Tema.acento());
        }
    }

    // ------------------------------------------------------------ preguntas
    private void preguntas(Graphics2D g) {
        Rectangle r = PREG;
        Dibujo.tarjetaOscura(g, r, 22);
        switch (estado) {
            case TURNO:
                return;
            case LLAMADA:
                encabezado(g, r, "LLAMADA DE TU JEFE", "Escucha las instrucciones del día");
                boton(g, new Rectangle(r.x + 40, r.y + 120, r.width - 80, 64),
                        lineaLlamada < llamada.length - 1 ? "Seguir escuchando   (Espacio)" : "Colgar y abrir la ventanilla   (Espacio)",
                        Dibujo.Estilo.PRINCIPAL, this::avanzarLlamada);
                return;
            case VACIO:
                encabezado(g, r, "VENTANILLA LIBRE", j.enFila() + " personas esperando en la fila (cola FIFO)");
                if (!j.isTerminada()) {
                    boton(g, new Rectangle(r.x + 40, r.y + 120, r.width - 80, 64), "¡Siguiente!  Hacer pasar   (Espacio)",
                            Dibujo.Estilo.PRINCIPAL, this::hacerPasar);
                }
                return;
            case RESULTADO:
                boton(g, new Rectangle(r.x + 30, r.y + r.height - 76, r.width - 60, 54), "Despedir y continuar   (Espacio)",
                        Dibujo.Estilo.PRINCIPAL, this::despedirVisitante);
                return;
            default:
                break;
        }
        if (caso == null || estado == Estado.SALIENDO) {
            return;
        }
        encabezado(g, r, "PREGUNTAS", "Árbol de diálogo: " + caso.dialogo.visitados() + "/" + caso.dialogo.cantidadNodos()
                + " nodos · arrástralas hacia el visitante");
        List<NodoDialogo> op = j.opciones();
        int y = r.y + 60;
        int i = 0;
        for (NodoDialogo n : op) {
            if (i >= 5) {
                break;
            }
            Rectangle br = new Rectangle(r.x + 14, y, r.width - 28, 44);
            boolean arrastrada = n == arrastraPregunta && (Math.abs(dragX - pressX) > 6 || Math.abs(dragY - pressY) > 6);
            if (arrastrada) {
                g.setColor(new Color(255, 235, 215, 30));
                g.fillRoundRect(br.x, br.y, br.width, br.height, 16, 16);
            } else {
                tarjetaPregunta(g, br, i + 1, n, escritorioLibre() && encima(br), false);
            }
            zonasPreguntas.add(new Object[]{br, n});
            y += 51;
            i++;
        }
        if (op.isEmpty()) {
            g.setColor(Tema.textoSuave());
            g.setFont(Tema.fuente(Font.ITALIC, 14));
            g.drawString("Ya no tiene más que decir. Investiga o sella.", r.x + 22, y + 26);
        }
    }

    private void encabezado(Graphics2D g, Rectangle r, String titulo, String sub) {
        Dibujo.tituloTarjeta(g, titulo, r.x + 18, r.y + 28, Tema.acento(), Tema.texto(), 14);
        g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
        g.setColor(Tema.textoSuave());
        g.drawString(Dibujo.recortar(g.getFontMetrics(), sub, r.width - 40), r.x + 32, r.y + 46);
    }

    /** Tarjeta de una pregunta: crema; las CONFRONTACIONES (nodos insertados) en cobre. */
    private void tarjetaPregunta(Graphics2D g, Rectangle br, int num, NodoDialogo n, boolean hover, boolean flotando) {
        boolean conf = n.insertado;
        int lift = hover ? 2 : 0;
        RoundRectangle2D s = new RoundRectangle2D.Double(br.x, br.y - lift, br.width, br.height, 16, 16);
        if (!Tema.altoContraste) {
            g.setColor(new Color(20, 8, 4, flotando ? 90 : 50));
            g.fill(new RoundRectangle2D.Double(br.x + 2, br.y + (flotando ? 6 : 3), br.width, br.height, 16, 16));
        }
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
        } else if (conf) {
            g.setPaint(new GradientPaint(br.x, br.y, new Color(226, 140, 86), br.x, br.y + br.height, new Color(176, 84, 44)));
        } else {
            g.setColor(hover || flotando ? Color.WHITE : Tema.PAPEL);
        }
        g.fill(s);
        g.setColor(hover || flotando ? Tema.acento2() : Tema.altoContraste ? Color.WHITE : conf ? new Color(122, 56, 28)
                : new Color(200, 160, 140));
        g.setStroke(new BasicStroke(hover ? 2.2f : 1.2f));
        g.draw(s);
        g.setStroke(new BasicStroke(1));
        int x = br.x + 12;
        if (num > 0) {
            g.setColor(conf ? Tema.CREMA : Tema.COBRE);
            g.fill(new Ellipse2D.Double(x, br.y - lift + br.height / 2.0 - 12, 24, 24));
            g.setFont(Tema.fuente(Font.BOLD, 12));
            g.setColor(conf ? Tema.COBRE : Tema.CREMA);
            Dibujo.centrado(g, String.valueOf(num), x + 12, br.y - lift + br.height / 2.0 + 5);
            x += 34;
        }
        Color tx = Tema.altoContraste ? Color.WHITE : conf ? Tema.CREMA : Tema.TINTA;
        String q = (conf ? "¡CONFRONTAR!  " : "") + n.pregunta;
        Font f = Dibujo.ajustar(g, q, conf ? Font.BOLD : Font.PLAIN, 14, br.x + br.width - x - 34);
        g.setFont(f);
        g.setColor(tx);
        g.drawString(Dibujo.recortar(g.getFontMetrics(), q, br.x + br.width - x - 34), x, br.y - lift + br.height / 2 + 5);
        // asa de arrastre (6 puntos)
        g.setColor(Tema.alfa(tx, 110));
        for (int k = 0; k < 6; k++) {
            g.fill(new Ellipse2D.Double(br.x + br.width - 24 + (k % 2) * 6, br.y - lift + br.height / 2.0 - 8 + (k / 2) * 6, 3, 3));
        }
    }

    // ------------------------------------------------------------ celular con la noticia
    /** Rectangulo de la pantalla util del celular (dentro del marco de la imagen). */
    private static Rectangle pantallaCel() {
        double k = CEL.height / 613.0;
        return new Rectangle((int) (CEL.x + 33 * k), (int) (CEL.y + 92 * k), (int) (251 * k), (int) (436 * k));
    }

    private void celular(Graphics2D g) {
        Rectangle c = CEL;
        boolean destino = arrastraSello != null && c.contains(dragX, dragY);
        if (destino) {
            g.setColor(Tema.alfa(Tema.DORADO, (int) (110 + 60 * Math.sin(t * 8))));
            g.fill(new RoundRectangle2D.Double(c.x + 4, c.y - 2, c.width - 8, c.height + 4, 60, 60));
        }
        if (!Tema.altoContraste) {
            g.setColor(new Color(30, 12, 6, 70));
            g.fill(new RoundRectangle2D.Double(c.x + 14, c.y + 10, c.width - 16, c.height - 4, 56, 56));
        }
        if (!Recursos.contener(g, "celular.png", c.x, c.y, c.width, c.height)) {
            g.setColor(Color.BLACK);
            g.fill(new RoundRectangle2D.Double(c.x + 20, c.y, c.width - 40, c.height, 50, 50));
        }
        Rectangle p = pantallaCel();
        Graphics2D s = (Graphics2D) g.create();
        s.clip(p);
        s.setColor(Tema.altoContraste ? Color.BLACK : Color.WHITE);
        s.fill(p);
        if (caso == null) {
            s.setColor(Tema.altoContraste ? Color.WHITE : new Color(238, 226, 220));
            for (int i = 0; i < 4; i++) {
                int y = p.y + 20 + i * 100;
                s.fill(new Ellipse2D.Double(p.x + 12, y, 30, 30));
                s.fillRoundRect(p.x + 50, y + 4, 120, 9, 9, 9);
                s.fillRoundRect(p.x + 50, y + 20, 80, 8, 8, 8);
                s.fillRoundRect(p.x + 12, y + 40, p.width - 24, 44, 10, 10);
            }
            s.setFont(Tema.fuente(Font.BOLD, 13));
            s.setColor(Tema.tintaSuave());
            Dibujo.centrado(s, "Sin publicaciones por revisar", p.getCenterX(), p.getCenterY());
            s.dispose();
            return;
        }
        Publicacion pub = caso.pub;
        Cuenta cu = pub.getCuenta();
        int y = p.y + 10;
        // usuario
        s.setColor(Tema.lila());
        s.fill(new Ellipse2D.Double(p.x + 10, y, 34, 34));
        String ini = cu == null ? "?" : cu.getNombre().substring(0, 1).toUpperCase();
        s.setFont(Tema.fuente(Font.BOLD, 15));
        s.setColor(Tema.CREMA);
        Dibujo.centrado(s, ini, p.x + 27, y + 23);
        String nombre = cu == null ? pub.getAutor() : cu.getNombre();
        s.setFont(Tema.fuente(Font.BOLD, 12.5f));
        s.setColor(Tema.tinta());
        FontMetrics fm = s.getFontMetrics();
        String nm = Dibujo.recortar(fm, nombre == null ? "Usuario" : nombre, p.width - 80);
        s.drawString(nm, p.x + 52, y + 14);
        if (cu != null && cu.estaVerificada()) {
            int vx = p.x + 56 + fm.stringWidth(nm);
            s.setColor(Tema.lila());
            s.fill(new Ellipse2D.Double(vx, y + 3, 13, 13));
            s.setColor(Color.WHITE);
            s.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D ch = new Path2D.Double();
            ch.moveTo(vx + 3.5, y + 9.5);
            ch.lineTo(vx + 6, y + 12);
            ch.lineTo(vx + 10, y + 6.5);
            s.draw(ch);
            s.setStroke(new BasicStroke(1));
        }
        s.setFont(Tema.fuente(Font.PLAIN, 10.5f));
        s.setColor(Tema.tintaSuave());
        s.drawString(Dibujo.recortar(s.getFontMetrics(), (cu == null ? "" : cu.getArroba() + " · ")
                + (pub.getHora() == null ? "" : pub.getHora()), p.width - 60), p.x + 52, y + 30);
        y += 44;
        // ilustracion alusiva a la noticia
        int ih = (int) (p.width * 0.66);
        boolean video = pub.getImagen() != null && pub.getImagen().startsWith("video");
        Ilustracion.dibujar(s, new Rectangle(p.x + 8, y, p.width - 16, ih), pub.getTexto(), video, t);
        y += ih + 8;
        // acciones (corazon, comentario, compartir)
        s.setColor(Tema.tinta());
        s.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D cor = new Path2D.Double();
        double hx = p.x + 20, hy = y + 10;
        cor.moveTo(hx, hy + 7);
        cor.curveTo(hx - 11, hy - 1, hx - 5, hy - 10, hx, hy - 3);
        cor.curveTo(hx + 5, hy - 10, hx + 11, hy - 1, hx, hy + 7);
        s.draw(cor);
        s.draw(new RoundRectangle2D.Double(p.x + 38, y + 1, 18, 15, 8, 8));
        s.draw(new java.awt.geom.Line2D.Double(p.x + 74, y + 2, p.x + 74, y + 16));
        s.draw(new java.awt.geom.Line2D.Double(p.x + 68, y + 8, p.x + 74, y + 2));
        s.draw(new java.awt.geom.Line2D.Double(p.x + 80, y + 8, p.x + 74, y + 2));
        s.setStroke(new BasicStroke(1));
        int likes = 40 + Math.abs(pub.getTexto().hashCode()) % 900;
        s.setFont(Tema.fuente(Font.BOLD, 11));
        s.drawString(likes + " me gusta", p.x + 96, y + 14);
        y += 28;
        // texto (se ajusta para caber siempre dentro del celular)
        int yMax = p.y + p.height - 26;
        float tam = 13f;
        List<String> lineas;
        Font f;
        do {
            f = Tema.fuente(Font.PLAIN, tam);
            lineas = Avisos.partir(s, f, pub.getTexto(), p.width - 22);
            tam -= 0.5f;
        } while (y + lineas.size() * (f.getSize2D() + 3) > yMax && tam > 9);
        s.setFont(f);
        s.setColor(Tema.tinta());
        for (String l : lineas) {
            if (y > yMax) {
                break;
            }
            y += (int) (f.getSize2D() + 3);
            s.drawString(l, p.x + 11, y);
        }
        // la peticion del visitante
        s.setColor(Tema.alfa(Tema.COBRE, 30));
        s.fillRect(p.x, p.y + p.height - 22, p.width, 22);
        s.setFont(Tema.fuente(Font.ITALIC, 10.5f));
        s.setColor(Tema.COBRE);
        s.drawString(Dibujo.recortar(s.getFontMetrics(), "Te pide: " + caso.peticion, p.width - 16), p.x + 8, p.y + p.height - 7);
        // sello estampado
        if (caso.decidido != null && estado != Estado.SELLANDO) {
            estampa(s, caso.decidido, p.getCenterX(), p.y + p.height * 0.42, 1, -0.22, 1f);
        }
        s.dispose();
    }

    static Color colorSello(Veredicto v) {
        switch (v) {
            case PUBLICAR: return new Color(70, 130, 64);
            case OPINION: return new Color(84, 120, 172);
            case ETIQUETA: return new Color(196, 150, 56);
            case RECHAZAR: return new Color(214, 120, 60);
            default: return new Color(176, 60, 58);
        }
    }

    /** La marca que deja el sello: imagen del sello + anillo con su nombre. */
    private void estampa(Graphics2D g, Veredicto v, double cx, double cy, double escala, double ang, float alfa) {
        Graphics2D s = (Graphics2D) g.create();
        s.translate(cx, cy);
        s.rotate(ang);
        s.scale(escala, escala);
        s.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alfa)) * 0.92f));
        Color c = colorSello(v);
        s.setColor(Tema.alfa(c, 225));
        s.setStroke(new BasicStroke(4));
        s.drawRoundRect(-104, -38, 208, 76, 18, 18);
        s.setStroke(new BasicStroke(1.5f));
        s.drawRoundRect(-98, -32, 196, 64, 14, 14);
        if (!Recursos.contener(s, v.imagen(), -94, -28, 56, 56)) {
            Dibujo.selloRespaldo(s, -66, 0, 26, c, "");
        }
        Font f = Tema.fuente(Font.BOLD, 20);
        s.setFont(f);
        FontMetrics fm = s.getFontMetrics();
        while (fm.stringWidth(v.sello) > 124 && f.getSize2D() > 11) {
            f = f.deriveFont(f.getSize2D() - 1);
            s.setFont(f);
            fm = s.getFontMetrics();
        }
        s.drawString(v.sello, -30, 8);
        s.dispose();
    }

    private void animacionSello(Graphics2D g) {
        Rectangle p = pantallaCel();
        double k = suave(Math.min(1, selloT));
        double esc0 = 2.4 - 1.4 * k;
        Graphics2D s = (Graphics2D) g.create();
        s.clip(new Rectangle(p.x - 120, p.y - 200, p.width + 240, p.height + 260));
        estampa(s, selloV, p.getCenterX(), p.y + p.height * 0.42 - (1 - k) * 160, esc0, -0.22, (float) (0.3 + 0.7 * k));
        s.dispose();
    }

    // ------------------------------------------------------------ bandeja de sellos
    private void sellos(Graphics2D g) {
        Rectangle r = SELLOS;
        Dibujo.tarjetaOscura(g, r, 22);
        g.setFont(Tema.fuente(Font.BOLD, 11.5f));
        g.setColor(Tema.textoSuave());
        Dibujo.centrado(g, "SELLOS · arrástralos sobre la publicación", r.getCenterX(), r.y + 20);
        List<Veredicto> lista = j.sellos();
        int n = lista.size();
        int w = (r.width - 16) / Math.max(1, n);
        String teclas = "ZXCVB";
        boolean activoS = estado == Estado.ATENDIENDO && caso != null && escritorioLibre();
        for (int i = 0; i < n; i++) {
            Veredicto v = lista.get(i);
            Rectangle br = new Rectangle(r.x + 8 + i * w, r.y + 28, w, r.height - 34);
            boolean hover = activoS && br.contains(mx, my) && arrastraSello == null;
            boolean enMano = v == arrastraSello;
            double d = Math.min(w - 10, 76);
            double cx = br.getCenterX(), cy = br.y + 8 + d / 2 - (hover ? 5 : 0);
            if (!enMano) {
                if (!Tema.altoContraste) {
                    g.setColor(new Color(20, 8, 4, 90));
                    g.fill(new Ellipse2D.Double(cx - d / 2 + 2, br.y + 12 + d / 2 - d / 2 + 4, d, d));
                }
                Composite antes = g.getComposite();
                if (!activoS) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
                }
                if (!Recursos.contener(g, v.imagen(), cx - d / 2, cy - d / 2, d, d)) {
                    Dibujo.selloRespaldo(g, cx, cy, d / 2, colorSello(v), "");
                }
                g.setComposite(antes);
            } else {
                g.setColor(new Color(255, 235, 215, 40));
                g.draw(new Ellipse2D.Double(cx - d / 2, cy - d / 2, d, d));
            }
            Font f = Dibujo.ajustar(g, v.sello, Font.BOLD, 11.5f, w - 6);
            g.setFont(f);
            g.setColor(hover ? Tema.acento2() : Tema.texto());
            Dibujo.centrado(g, v.sello, cx, br.y + d + 30);
            g.setFont(Tema.fuente(Font.PLAIN, 10));
            g.setColor(Tema.textoSuave());
            Dibujo.centrado(g, "(" + teclas.charAt(i) + ")", cx, br.y + d + 45);
            if (activoS) {
                zonasSellos.add(new Object[]{new Rectangle((int) (cx - d / 2), (int) (cy - d / 2), (int) d, (int) d + 30), v});
            }
        }
    }

    // ------------------------------------------------------------ monitor
    private void monitor(Graphics2D g) {
        Rectangle r = MONITOR;
        Dibujo.tarjetaOscura(g, r, 24);
        Herramienta[] hs = Herramienta.values();
        boolean conHab = rol().tieneMiniJuego();
        int ntabs = hs.length + (conHab ? 1 : 0);
        int tw = (r.width - 20) / ntabs;
        String[] cortos = {"Perfil", "Directorio", "Lupa", "Imagen", "Corresp.", "Red"};
        for (int i = 0; i < ntabs; i++) {
            Rectangle tr = new Rectangle(r.x + 10 + i * tw, r.y + 10, tw - 5, 52);
            boolean sel = i == tab;
            boolean hv = encima(tr) && escritorioLibre();
            if (i < hs.length) {
                Herramienta h = hs[i];
                boolean porDia = h.dia > j.dia, porRol = !j.delRol(h);
                boolean bloqueada = porDia || porRol;
                // no aplica a ESTE cliente (sin credencial, sin foto...): pestana deshabilitada con una X roja
                boolean noAplica = !bloqueada && caso != null && j.getActual() != null && j.resultados(h) == null
                        && j.motivoNoAplica(h) != null;
                g.setColor(Tema.altoContraste ? (sel ? Color.YELLOW : Color.BLACK) : sel ? (noAplica ? new Color(120, 52, 40) : Tema.COBRE)
                        : bloqueada || noAplica ? new Color(52, 30, 22) : hv ? new Color(110, 64, 44) : new Color(86, 50, 35));
                g.fillRoundRect(tr.x, tr.y, tr.width, tr.height, 14, 14);
                Color ci = bloqueada || noAplica ? new Color(140, 100, 84) : Tema.altoContraste && sel ? Color.BLACK : Tema.CREMA;
                if (noAplica) {
                    g.setColor(Tema.mal());
                    g.fill(new Ellipse2D.Double(tr.x + tr.width - 17, tr.y + 3, 14, 14));
                    g.setColor(Tema.CREMA);
                    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(tr.x + tr.width - 13, tr.y + 7, tr.x + tr.width - 7, tr.y + 13);
                    g.drawLine(tr.x + tr.width - 7, tr.y + 7, tr.x + tr.width - 13, tr.y + 13);
                    g.setStroke(new BasicStroke(1));
                }
                if (bloqueada) {
                    Iconos.candado(g, tr.getCenterX(), tr.y + 21, 8, ci);
                } else {
                    Iconos.herramienta(g, i, tr.getCenterX(), tr.y + 20, 10, ci);
                }
                g.setColor(ci);
                g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
                String et = "QWERTY".charAt(i) + " · " + cortos[i];
                Dibujo.centrado(g, et, tr.getCenterX(), tr.y + 45);
            } else {
                // la pestana de la HABILIDAD del rol
                Color rc = Dibujo.colorRol(rol());
                double pul = j.puedeUsarHabilidad() && estado == Estado.ATENDIENDO ? 0.5 + 0.5 * Math.sin(t * 4) : 0;
                g.setColor(sel ? rc : Tema.mezclar(new Color(86, 50, 35), rc, 0.35 + 0.3 * pul));
                g.fillRoundRect(tr.x, tr.y, tr.width, tr.height, 14, 14);
                Dibujo.emblemaRol(g, rol(), tr.getCenterX(), tr.y + 20, 12, false);
                g.setColor(Tema.altoContraste && sel ? Color.BLACK : Tema.CREMA);
                g.setFont(Tema.fuente(Font.BOLD, 10));
                Dibujo.centrado(g, "U · Habilidad", tr.getCenterX(), tr.y + 45);
            }
            final int ii = i;
            if (escritorioLibre()) {
                zona(tr, () -> tab = ii);
            }
        }
        Rectangle p = new Rectangle(r.x + 12, r.y + 72, r.width - 24, r.height - 84);
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(34, 18, 14));
        g.fillRoundRect(p.x, p.y, p.width, p.height, 16, 16);
        Graphics2D s = (Graphics2D) g.create();
        s.clip(new RoundRectangle2D.Double(p.x, p.y, p.width, p.height, 16, 16));
        if (tab >= hs.length) {
            pantallaHabilidad(s, p);
        } else {
            contenidoPantalla(s, p, hs[tab]);
        }
        s.dispose();
    }

    private void contenidoPantalla(Graphics2D g, Rectangle p, Herramienta h) {
        Color crema = Tema.texto();
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(Tema.acento2());
        g.drawString(h.nombre.toUpperCase(), p.x + 16, p.y + 28);
        g.setColor(Tema.alfa(Tema.DORADO, 60));
        g.fillRect(p.x + 16, p.y + 36, 60, 2);
        if (!j.delRol(h)) {
            Iconos.candado(g, p.getCenterX(), p.getCenterY() - 60, 22, Tema.textoSuave());
            centrado(g, "Tu rol (" + rol().nombre + ") no usa esta herramienta", p, Tema.textoSuave(), 0);
            g.setFont(Tema.fuente(Font.PLAIN, 13));
            g.setColor(Tema.textoSuave());
            String alt = rol().tieneMiniJuego() ? "Usa tu habilidad única: pestaña \"Habilidad\" (tecla U)."
                    : "Esta herramienta es de otro rol.";
            Dibujo.centrado(g, alt, p.getCenterX(), p.getCenterY() + 28);
            return;
        }
        if (h.dia > j.dia) {
            Iconos.candado(g, p.getCenterX(), p.getCenterY() - 50, 22, Tema.textoSuave());
            centrado(g, "Disponible desde el día " + h.dia, p, Tema.textoSuave(), 0);
            return;
        }
        if (j.enApagon()) {
            for (int i = 0; i < 400; i++) {
                int c = 60 + azar.nextInt(160);
                g.setColor(new Color(c, (int) (c * 0.8), (int) (c * 0.7)));
                g.fillRect(p.x + azar.nextInt(p.width), p.y + azar.nextInt(p.height), 3, 2);
            }
            centrado(g, "SIN SEÑAL: APAGÓN", p, Tema.CREMA, 0);
            return;
        }
        if (caso == null) {
            centrado(g, "Esperando al siguiente caso...", p, Tema.textoSuave(), 0);
            return;
        }
        if (escaneando == h) {
            g.setColor(Tema.alfa(Tema.DORADO, 70));
            g.fillRoundRect(p.x + 30, p.y + p.height / 2, p.width - 60, 18, 18, 18);
            g.setColor(Tema.acento2());
            g.fillRoundRect(p.x + 30, p.y + p.height / 2, (int) ((p.width - 60) * Math.min(1, escaneoT)), 18, 18, 18);
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.setColor(crema);
            g.drawString(h == Herramienta.DIRECTORIO ? "Buscando en el AVL..." : h == Herramienta.RED
                    ? "Recorriendo el grafo (BFS / DFS)..." : "Analizando...", p.x + 30, p.y + p.height / 2 - 12);
            int ly = p.y + 50 + (int) ((t * 160) % (p.height - 60));
            g.setColor(Tema.alfa(Tema.DORADO, 50));
            g.fillRect(p.x, ly, p.width, 4);
            return;
        }
        List<String> res = j.resultados(h);
        String noAplica = res == null && j.getActual() != null ? j.motivoNoAplica(h) : null;
        if (noAplica != null) {
            // herramienta deshabilitada para este cliente: se explica en rojo por que
            g.setColor(Tema.textoSuave());
            Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13.5f), descripcion(h), p.x + 16, p.y + 64, p.width - 32, 19, p.y + 120);
            Rectangle av = new Rectangle(p.x + 30, p.y + p.height / 2 - 40, p.width - 60, 100);
            g.setColor(Tema.altoContraste ? Color.BLACK : Tema.alfa(Tema.LADRILLO, 230));
            g.fillRoundRect(av.x, av.y, av.width, av.height, 18, 18);
            g.setColor(Tema.altoContraste ? Color.RED : new Color(240, 140, 120));
            g.drawRoundRect(av.x, av.y, av.width, av.height, 18, 18);
            Iconos.marca(g, 3, av.x + 26, av.y + 30, 9);
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.setColor(Tema.CREMA);
            g.drawString("No se puede usar con este cliente", av.x + 46, av.y + 35);
            Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13), noAplica, av.x + 20, av.y + 60, av.width - 40, 18, av.y + av.height - 4);
            g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
            g.setColor(Tema.textoSuave());
            Dibujo.centrado(g, "Con el siguiente cliente las herramientas se reinician.", p.getCenterX(), av.y + av.height + 28);
            return;
        }
        int y = p.y + 60;
        if (h == Herramienta.RED && res != null) {
            red3d.dibujar(g, new Rectangle(p.x + 4, p.y + 44, p.width - 8, 170), j.jugador.ciudad, caso.origen, t);
            y = p.y + 232;
        }
        if (h == Herramienta.DIRECTORIO && res != null) {
            Rectangle ar = new Rectangle(p.x + 6, p.y + p.height - 128, p.width - 12, 92);
            Cuenta c = caso.pub.getCuenta();
            VistaArboles.avl(g, j.jugador.directorio, c == null ? null : c.getArroba(), ar, true);
            if (j.puedeLlamar()) {
                boton(g, new Rectangle(p.x + p.width - 150, p.y + p.height - 38, 140, 30), "Llamar  (K)",
                        Dibujo.Estilo.EXITO, this::llamar);
            }
        }
        if (res == null) {
            String acc;
            switch (h) {
                case PERFIL: acc = "Escanear perfil"; break;
                case DIRECTORIO: acc = "Buscar en el directorio"; break;
                case LUPA: acc = "Abrir la lupa"; break;
                case IMAGEN: acc = "Búsqueda inversa"; break;
                case CORRESPONSAL: acc = "Enviar corresponsal"; break;
                default: acc = "Escanear la red"; break;
            }
            g.setColor(Tema.textoSuave());
            Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13.5f), descripcion(h), p.x + 16, p.y + 64, p.width - 32, 19, p.y + 120);
            if (estado == Estado.ATENDIENDO) {
                boton(g, new Rectangle(p.x + 50, p.y + p.height / 2 - 10, p.width - 100, 56), acc, Dibujo.Estilo.PRINCIPAL,
                        () -> usarHerramienta(h));
                g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
                g.setColor(Tema.textoSuave());
                Dibujo.centrado(g, "Cuesta minutos del reloj · tecla " + "QWERTY".charAt(h.ordinal()), p.getCenterX(),
                        p.y + p.height / 2 + 70);
            }
            return;
        }
        Font f = Tema.fuente(Font.PLAIN, 13);
        g.setFont(f);
        for (String l : res) {
            int tp = Iconos.tipo(l);
            String limpio = Iconos.sinPrefijo(l);
            boolean primera = true;
            for (String parte : Avisos.partir(g, f, limpio, p.width - 52)) {
                if (y > p.y + p.height - 8) {
                    break;
                }
                if (primera && tp != 0) {
                    Iconos.marca(g, tp, p.x + 22, y - 4, 6);
                }
                g.setColor(tp == 0 ? crema : Iconos.color(tp, crema));
                g.drawString(parte, p.x + (tp != 0 ? 36 : 16), y);
                y += 18;
                primera = false;
            }
            y += 4;
        }
        if (h == Herramienta.CORRESPONSAL && j.corresponsalEnCamino()) {
            g.setColor(Tema.alfa(Tema.DORADO, 70));
            g.fillRoundRect(p.x + 20, y + 10, p.width - 40, 14, 14, 14);
            g.setColor(Tema.acento2());
            g.fillRoundRect(p.x + 20, y + 10, (int) ((p.width - 40) * j.avanceCorresponsal()), 14, 14, 14);
            g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
            g.setColor(crema);
            g.drawString("La moto va en camino... ábrela en el MAPA (botón de arriba)", p.x + 20, y + 44);
        }
        if (h == Herramienta.LUPA && estado == Estado.ATENDIENDO && caso.visitante.credencial != null) {
            boton(g, new Rectangle(p.x + 60, p.y + p.height - 70, p.width - 120, 48), "Abrir la lupa  (L)",
                    Dibujo.Estilo.LILA, this::abrirLupa);
        }
    }

    private void pantallaHabilidad(Graphics2D g, Rectangle p) {
        RolJugador rr = rol();
        Dibujo.emblemaRol(g, rr, p.x + 50, p.y + 52, 30, true);
        g.setFont(Tema.fuente(Font.BOLD, 19));
        g.setColor(Tema.acento2());
        g.drawString("Habilidad: " + rr.habilidad, p.x + 94, p.y + 46);
        g.setFont(Tema.fuente(Font.PLAIN, 12));
        g.setColor(Tema.textoSuave());
        g.drawString("Solo la tiene el rol " + rr.nombre + " · una vez por caso", p.x + 94, p.y + 66);
        g.setColor(Tema.texto());
        int y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13.5f), rr.descripcionHabilidad, p.x + 20, p.y + 106,
                p.width - 40, 20, p.y + 190);
        if (caso == null) {
            centrado(g, "Espera a que llegue alguien a la ventanilla.", p, Tema.textoSuave(), 60);
            return;
        }
        if (caso.habilidadUsada) {
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.setColor(Tema.acento2());
            g.drawString("RESULTADO (anotado en la libreta)", p.x + 20, y + 14);
            y += 38;
            g.setFont(Tema.fuente(Font.PLAIN, 13));
            for (String l : caso.pistasHabilidad) {
                int tp = Iconos.tipo(l);
                if (tp != 0) {
                    Iconos.marca(g, tp, p.x + 26, y - 4, 6);
                }
                g.setColor(tp == 0 ? Tema.texto() : Iconos.color(tp, Tema.texto()));
                y = Dibujo.parrafo(g, g.getFont(), Iconos.sinPrefijo(l), p.x + 40, y, p.width - 60, 18, p.y + p.height - 10);
                y += 6;
            }
            return;
        }
        if (estado == Estado.ATENDIENDO) {
            boton(g, new Rectangle(p.x + 50, p.y + p.height - 120, p.width - 100, 60), "¡Usar mi habilidad!  (U)",
                    Dibujo.Estilo.PRINCIPAL, this::usarHabilidad);
            g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
            g.setColor(Tema.textoSuave());
            Dibujo.centrado(g, "El reloj se detiene mientras juegas; al terminar se cobran unos minutos.", p.getCenterX(),
                    p.y + p.height - 36);
        }
    }

    private static String descripcion(Herramienta h) {
        switch (h) {
            case PERFIL: return "Antigüedad de la cuenta, seguidores, fuente citada y señales de alerta.";
            case DIRECTORIO: return "¿La cuenta es oficial? Búsqueda O(log n) en un árbol AVL de cuentas oficiales.";
            case LUPA: return "Compara la cara del visitante con la foto de su credencial. Señala la diferencia.";
            case IMAGEN: return "¿La foto ya existía en otro lugar o en otra fecha?";
            case CORRESPONSAL: return "Va al barrio por la ruta más corta (Dijkstra en el grafo de la ciudad).";
            default: return "¿Quién lo compartió primero? BFS por oleadas y DFS en el grafo social.";
        }
    }

    // ------------------------------------------------------------ libreta de pistas
    private void libreta(Graphics2D g) {
        Rectangle r = LIBRETA;
        RoundRectangle2D s = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 16, 16);
        Dibujo.sombra(g, s, 5, 3);
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(253, 246, 232));
        g.fill(s);
        Graphics2D c = (Graphics2D) g.create();
        c.clip(s);
        if (!Tema.altoContraste) {
            c.setColor(new Color(217, 178, 166, 110));
            for (int y = r.y + 46; y < r.y + r.height; y += 20) {
                c.drawLine(r.x + 30, y, r.x + r.width - 8, y);
            }
            c.setColor(new Color(192, 71, 58, 110));
            c.drawLine(r.x + 52, r.y + 6, r.x + 52, r.y + r.height - 6);
            c.setColor(new Color(242, 179, 94, 60));
            c.fillRect(r.x, r.y, r.width, 34);
        }
        c.dispose();
        // espiral
        for (int y = r.y + 14; y < r.y + r.height - 6; y += 18) {
            g.setColor(Tema.altoContraste ? Color.WHITE : new Color(110, 54, 33));
            g.fill(new Ellipse2D.Double(r.x + 10, y, 9, 9));
            g.setColor(Tema.altoContraste ? Color.GRAY : new Color(200, 160, 140));
            g.setStroke(new BasicStroke(2));
            g.drawArc(r.x + 2, y - 3, 16, 14, 90, 180);
            g.setStroke(new BasicStroke(1));
        }
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(170, 120, 96, 150));
        g.draw(s);
        List<String> pistas = pistas();
        g.setFont(Tema.fuente(Font.BOLD, 14));
        g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.COBRE);
        g.drawString("Libreta de pistas", r.x + 62, r.y + 24);
        g.setFont(Tema.fuente(Font.PLAIN, 11));
        g.setColor(Tema.tintaSuave());
        String cnt = pistas.size() + (pistas.size() == 1 ? " pista" : " pistas") + (pistas.size() > (r.height - 70) / 20 ? " · rueda para ver más" : "");
        g.drawString(cnt, r.x + r.width - 14 - g.getFontMetrics().stringWidth(cnt), r.y + 23);
        Font f = Tema.media(12.5f);
        g.setFont(f);
        int y = r.y + 58;
        int visibles = (r.height - 70) / 20;
        int maxScroll = Math.max(0, pistas.size() - visibles);
        libretaScroll = Math.min(libretaScroll, maxScroll);
        int desde = Math.max(0, pistas.size() - visibles - libretaScroll);
        for (int i = desde; i < Math.min(pistas.size(), desde + visibles); i++) {
            String pi = pistas.get(i);
            int tp = Iconos.tipo(pi);
            if (tp == 0) {
                g.setColor(Tema.altoContraste ? Color.WHITE : Tema.COBRE);
                g.fill(new Ellipse2D.Double(r.x + 64, y - 8, 6, 6));
            } else {
                Iconos.marca(g, tp, r.x + 67, y - 5, 6);
            }
            g.setColor(Tema.tinta());
            g.drawString(Dibujo.recortar(g.getFontMetrics(), Iconos.sinPrefijo(pi), r.width - 96), r.x + 80, y);
            y += 20;
        }
        if (pistas.isEmpty()) {
            g.setFont(Tema.fuente(Font.ITALIC, 12.5f));
            g.setColor(Tema.tintaSuave());
            g.drawString("Pregunta, investiga y usa tu habilidad: las pistas se anotan solas.", r.x + 64, y);
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
        l.addAll(caso.pistasHabilidad);
        if (llamadaInst != null) {
            l.add("Llamada: " + llamadaInst);
        }
        return l;
    }

    // ------------------------------------------------------------ indicadores y noticiero
    /**
     * Noticiero. Los indicadores de Ciudad Nova ya NO se muestran aqui: estan
     * dentro del boton MAPA (con la salud de la ciudad en su insignia).
     */
    private void tv(Graphics2D g) {
        Rectangle tk = TV;
        Dibujo.tarjetaOscura(g, tk, 18);
        Graphics2D s = (Graphics2D) g.create();
        s.clipRect(tk.x + 104, tk.y, tk.width - 112, tk.height);
        s.setFont(Tema.fuente(Font.PLAIN, 13));
        String linea = String.join("     •     ", ticker);
        int lw2 = s.getFontMetrics().stringWidth(linea) + 80;
        double x = tk.x + tk.width + (tickerX % (lw2 + tk.width));
        s.setColor(Tema.texto());
        s.drawString(linea, (float) x, tk.y + 29);
        s.dispose();
        g.setColor(Tema.altoContraste ? Color.RED : Tema.COBRE);
        g.fillRoundRect(tk.x + 8, tk.y + 8, 90, tk.height - 16, 15, 15);
        g.setFont(Tema.fuente(Font.BOLD, 11.5f));
        g.setColor(Tema.CREMA);
        Dibujo.centrado(g, "NOTICIAS", tk.x + 53, tk.y + 28);
    }

    /** Los 6 indicadores de Ciudad Nova con su barra (se usan en el panel del MAPA). */
    private void indicadores(Graphics2D g, Rectangle r) {
        CiudadEstado c = j.jugador.ciudad;
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(34, 18, 14));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        g.setFont(Tema.fuente(Font.BOLD, 14));
        g.setColor(Tema.acento2());
        g.drawString("INDICADORES DE CIUDAD NOVA", r.x + 18, r.y + 28);
        int salud = c.salud();
        g.setFont(Tema.fuente(Font.BOLD, 30));
        g.setColor(salud >= 60 ? Tema.bien() : salud >= 45 ? Tema.acento2() : Tema.mal());
        g.drawString(String.valueOf(salud), r.x + 18, r.y + 68);
        int sw = g.getFontMetrics().stringWidth(String.valueOf(salud));
        g.setFont(Tema.fuente(Font.PLAIN, 12));
        g.setColor(Tema.textoSuave());
        g.drawString("/ 100  salud de la ciudad", r.x + 24 + sw, r.y + 66);
        String[] nom = {"Información verificada", "Confianza ciudadana", "Convivencia", "Bienestar digital",
            "Desinformación", "Conflictos"};
        String[] que = {"Lo comprobado que circula", "Cuánto confía la gente", "Cómo se llevan los barrios",
            "Qué tan sana está Civitas", "% del grafo que cree un bulo", "Peleas y discusiones"};
        int[] val = {c.verificada, c.confianza, c.convivencia, c.bienestar, c.desinformacion(), c.conflictos};
        boolean[] malo = {false, false, false, false, true, true};
        int y = r.y + 104;
        int fila = Math.max(44, (r.y + r.height - 14 - y) / nom.length);
        for (int i = 0; i < nom.length; i++) {
            boolean bueno = malo[i] ? val[i] < 35 : val[i] >= 50;
            Dibujo.triangulo(g, r.x + 24, y - 5, 5.5, !malo[i], malo[i] ? Tema.ROSA : Tema.LILA_CLARO);
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.setColor(Tema.texto());
            g.drawString(nom[i], r.x + 36, y);
            String vs = String.valueOf(val[i]) + (i == 4 ? " %" : "");
            g.drawString(vs, r.x + r.width - 18 - g.getFontMetrics().stringWidth(vs), y);
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(Tema.textoSuave());
            g.drawString(que[i] + (malo[i] ? " · mejor si BAJA" : " · mejor si SUBE"), r.x + 36, y + 15);
            int bw = r.width - 54;
            g.setColor(new Color(0, 0, 0, 110));
            g.fillRoundRect(r.x + 36, y + 21, bw, 8, 8, 8);
            g.setColor(bueno ? Tema.bien() : Tema.mal());
            g.fillRoundRect(r.x + 36, y + 21, bw * val[i] / 100, 8, 8, 8);
            Iconos.marca(g, bueno ? 2 : 1, r.x + 24, y + 25, 5);
            y += fila;
        }
    }

    // ------------------------------------------------------------ resultado de un sello
    private void tarjetaResultado(Graphics2D g) {
        Jornada.Resultado r = ultimo;
        Rectangle c = new Rectangle(PREG.x + 14, PREG.y + 14, PREG.width - 28, PREG.height - 106);
        boolean ok = r.efecto.acierto;
        Dibujo.tarjetaPapel(g, c, 18);
        g.setColor(ok ? Tema.bien() : Tema.mal());
        g.fillRoundRect(c.x, c.y, 8, c.height, 8, 8);
        Iconos.marca(g, ok ? 2 : 3, c.x + 32, c.y + 28, 10);
        g.setFont(Tema.fuente(Font.BOLD, 19));
        g.drawString((ok ? "¡CORRECTO!" : "ERROR") + "   " + (r.puntos >= 0 ? "+" : "") + r.puntos + " pts"
                + (r.dinero != 0 ? "   $" + r.dinero : ""), c.x + 52, c.y + 35);
        Font f = Tema.fuente(Font.PLAIN, 12.5f);
        g.setColor(Tema.tinta());
        int y = c.y + 60;
        List<String> lineas = new ArrayList<>();
        lineas.add("Era: " + r.caso.verdad.etiqueta + ". " + r.efecto.explicacion);
        if (r.caso.pub.getExplicacion() != null) {
            lineas.add(r.caso.pub.getExplicacion());
        }
        if (r.alcance > 0) {
            lineas.add("BFS: la publicación llegó a " + r.alcance + " cuentas del grafo social.");
        }
        if (r.corregidos > 0) {
            lineas.add("Reporte: cuenta eliminada del grafo; " + r.corregidos + " personas corregidas.");
        }
        if (r.bonoRapidez) {
            lineas.add("Bono por rapidez +30.");
        }
        if (r.seguroUsado) {
            lineas.add("Tu seguro de reputación (tienda) anuló la advertencia de este error.");
        }
        if (j.jugador.rol == RolJugador.INFLUENCER) {
            lineas.add("Influencer: tu decisión pesó x1,5 en puntos y en la ciudad.");
        }
        for (String l : lineas) {
            y = Dibujo.parrafo(g, f, l, c.x + 22, y, c.width - 40, 17, c.y + c.height - 6);
            y += 3;
        }
    }

    // ==================================================================
    // Capas del juego
    // ==================================================================

    private void velo(Graphics2D g) {
        g.setColor(new Color(20, 8, 4, 185));
        g.fillRect(0, BARRA, W, H - BARRA);
    }

    private void billetera(Graphics2D g) {
        velo(g);
        Persona p = caso == null ? null : caso.visitante;
        if (p == null || p.credencial == null) {
            juego = Juego.NADA;
            return;
        }
        Rectangle r = new Rectangle(W / 2 - 330, 150, 660, 520);
        // la billetera abierta (cuero)
        RoundRectangle2D cuero = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 34, 34);
        Dibujo.sombra(g, cuero, 8, 6);
        g.setPaint(new GradientPaint(r.x, r.y, new Color(130, 70, 44), r.x, r.y + r.height, new Color(86, 44, 28)));
        g.fill(cuero);
        g.setColor(new Color(242, 179, 94, 90));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{6, 6}, 0));
        g.draw(new RoundRectangle2D.Double(r.x + 12, r.y + 12, r.width - 24, r.height - 24, 26, 26));
        g.setStroke(new BasicStroke(1));
        Recursos.contener(g, "billetera.png", r.x + 26, r.y + 22, 56, 56);
        g.setFont(Tema.fuente(Font.BOLD, 20));
        g.setColor(Tema.CREMA);
        g.drawString("Billetera de " + p.nombre, r.x + 92, r.y + 50);
        g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
        g.setColor(new Color(240, 210, 190));
        g.drawString("Revisa su credencial: institución, número y vencimiento.", r.x + 92, r.y + 70);
        // credencial (con la misma paleta y tipografia del juego)
        credencial(g, p.credencial, new Rectangle(r.x + 60, r.y + 96, r.width - 120, 300));
        int by = r.y + r.height - 92;
        if (j.disponible(Herramienta.LUPA)) {
            boton(g, new Rectangle(r.x + 60, by, 300, 54), "Comparar con la lupa  (L)", Dibujo.Estilo.PRINCIPAL, this::abrirLupa);
        } else {
            g.setFont(Tema.fuente(Font.ITALIC, 13));
            g.setColor(new Color(240, 210, 190));
            Dibujo.parrafo(g, g.getFont(), !j.delRol(Herramienta.LUPA) ? "Tu rol no usa la lupa: confía en las preguntas y tu habilidad."
                    : "La lupa llega el día " + Herramienta.LUPA.dia + ".", r.x + 60, by + 22, 300, 18, by + 54);
        }
        boton(g, new Rectangle(r.x + r.width - 260, by, 200, 54), "Cerrar  (I)", Dibujo.Estilo.SECUNDARIO,
                () -> juego = Juego.NADA);
    }

    /**
     * CREDENCIAL con el diseno unificado del juego: papel crema, franja de cobre,
     * emblema de Ciudad Nova, foto con marco, datos en Poppins y codigo de barras.
     */
    static void credencial(Graphics2D g, influenceinc.modelo.Credencial c, Rectangle cr) {
        RoundRectangle2D cs = new RoundRectangle2D.Double(cr.x, cr.y, cr.width, cr.height, 22, 22);
        Dibujo.sombra(g, cs, 5, 4);
        g.setPaint(new GradientPaint(cr.x, cr.y, Tema.PAPEL, cr.x + cr.width, cr.y + cr.height, Tema.CREMA));
        g.fill(cs);
        Graphics2D s = (Graphics2D) g.create();
        s.clip(cs);
        // marca de agua: el emblema de la ciudad
        s.setColor(Tema.alfa(Tema.ROSA, 70));
        s.setStroke(new BasicStroke(10));
        s.draw(new Ellipse2D.Double(cr.x + cr.width - 170, cr.y + 90, 190, 190));
        s.setStroke(new BasicStroke(1));
        // franja superior
        s.setPaint(new GradientPaint(cr.x, cr.y, Tema.COBRE, cr.x + cr.width, cr.y, Tema.CAOBA));
        s.fillRect(cr.x, cr.y, cr.width, 58);
        s.setColor(Tema.alfa(Tema.DORADO, 160));
        s.fillRect(cr.x, cr.y + 58, cr.width, 3);
        // franja inferior con codigo de barras
        s.setColor(Tema.alfa(Tema.ROSA_CLARO, 255));
        s.fillRect(cr.x, cr.y + cr.height - 34, cr.width, 34);
        s.setColor(Tema.TINTA);
        java.util.Random rb = new java.util.Random(c.numero.hashCode());
        int bx = cr.x + cr.width - 190;
        while (bx < cr.x + cr.width - 24) {
            int w = 1 + rb.nextInt(3);
            s.fillRect(bx, cr.y + cr.height - 27, w, 20);
            bx += w + 1 + rb.nextInt(3);
        }
        s.dispose();
        // emblema
        g.setColor(Tema.CREMA);
        g.fill(new Ellipse2D.Double(cr.x + 16, cr.y + 11, 36, 36));
        g.setColor(Tema.COBRE);
        g.setFont(Tema.fuente(Font.BOLD, 13));
        Dibujo.centrado(g, "CN", cr.x + 34, cr.y + 34);
        g.setFont(Dibujo.ajustar(g, c.institucion.toUpperCase(), Font.BOLD, 16, cr.width - 90));
        g.setColor(Tema.CREMA);
        g.drawString(c.institucion.toUpperCase(), cr.x + 64, cr.y + 28);
        g.setFont(Tema.fuente(Font.BOLD, 10.5f));
        g.setColor(Tema.DORADO);
        g.drawString("CREDENCIAL OFICIAL  ·  CIUDAD NOVA", cr.x + 64, cr.y + 46);
        // foto con marco
        int fw = 150, fh = 184, fx = cr.x + 22, fy = cr.y + 76;
        BufferedImage foto = Caras.foto(c.foto, fw, fh);
        Graphics2D f = (Graphics2D) g.create();
        f.clip(new RoundRectangle2D.Double(fx, fy, fw, fh, 14, 14));
        f.drawImage(foto, fx, fy, null);
        f.dispose();
        g.setColor(Tema.COBRE);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new RoundRectangle2D.Double(fx, fy, fw, fh, 14, 14));
        g.setStroke(new BasicStroke(1));
        // datos
        int tx = fx + fw + 26;
        String[][] campos = {{"Nombre", c.nombre}, {"Cargo", c.cargo}, {"Número", c.numero}, {"Vence", c.vence}};
        int y = cr.y + 92;
        for (String[] d : campos) {
            g.setFont(Tema.fuente(Font.BOLD, 10.5f));
            g.setColor(Tema.TINTA_SUAVE);
            g.drawString(d[0].toUpperCase(), tx, y);
            g.setFont(Tema.fuente(Font.BOLD, 16));
            g.setColor(Tema.TINTA);
            g.drawString(Dibujo.recortar(g.getFontMetrics(), d[1], cr.x + cr.width - tx - 20), tx, y + 21);
            y += 44;
        }
        g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
        g.setColor(Tema.TINTA_SUAVE);
        g.drawString("Documento personal e intransferible", cr.x + 22, cr.y + cr.height - 13);
    }

    private void lupa(Graphics2D g) {
        velo(g);
        Persona p = caso == null ? null : caso.visitante;
        if (p == null || p.credencial == null) {
            juego = Juego.NADA;
            return;
        }
        Rectangle izq = new Rectangle(190, 120, 480, 500), der = new Rectangle(770, 120, 480, 500);
        Dibujo.tarjetaPapel(g, new Rectangle(izq.x - 10, izq.y - 44, izq.width + 20, izq.height + 54), 24);
        Dibujo.tarjetaPapel(g, new Rectangle(der.x - 10, der.y - 44, der.width + 20, der.height + 54), 24);
        g.setColor(Tema.ROSA_CLARO);
        g.fillRoundRect(izq.x, izq.y, izq.width, izq.height, 18, 18);
        g.setColor(Tema.ROSA_CLARO);
        g.fillRoundRect(der.x, der.y, der.width, der.height, 18, 18);
        double s = 2.2, cyI = izq.y + 220, cyD = der.y + 220;
        Graphics2D a = (Graphics2D) g.create();
        a.clip(new RoundRectangle2D.Double(izq.x, izq.y, izq.width, izq.height, 18, 18));
        Caras.dibujar(a, p.cara, izq.getCenterX(), cyI, s, expr, parpadeoT < 0, 0);
        a.dispose();
        a = (Graphics2D) g.create();
        a.clip(new RoundRectangle2D.Double(der.x, der.y, der.width, der.height, 18, 18));
        Caras.dibujar(a, p.credencial.foto, der.getCenterX(), cyD, s, "neutral", false, 0);
        a.dispose();
        g.setFont(Tema.fuente(Font.BOLD, 17));
        g.setColor(Tema.tinta());
        Graphics2D hi = (Graphics2D) g.create();
        hi.clip(new RoundRectangle2D.Double(izq.x - 10, izq.y - 44, izq.width + 20, izq.height + 54, 24, 24));
        hi.setPaint(new GradientPaint(izq.x, izq.y, Tema.MADERA_MEDIA, izq.x + izq.width, izq.y, Tema.CAOBA));
        hi.fillRect(izq.x - 10, izq.y - 44, izq.width + 20, 40);
        hi.dispose();
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(Tema.CREMA);
        g.drawString("En la ventanilla  ·  " + p.nombre, izq.x + 8, izq.y - 18);
        // cabecera de la credencial con el mismo diseno de la billetera
        Graphics2D hb = (Graphics2D) g.create();
        hb.clip(new RoundRectangle2D.Double(der.x - 10, der.y - 44, der.width + 20, der.height + 54, 24, 24));
        hb.setPaint(new GradientPaint(der.x, der.y, Tema.COBRE, der.x + der.width, der.y, Tema.CAOBA));
        hb.fillRect(der.x - 10, der.y - 44, der.width + 20, 40);
        hb.dispose();
        g.setColor(Tema.CREMA);
        g.fill(new Ellipse2D.Double(der.x, der.y - 38, 28, 28));
        g.setFont(Tema.fuente(Font.BOLD, 10.5f));
        g.setColor(Tema.COBRE);
        Dibujo.centrado(g, "CN", der.x + 14, der.y - 20);
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(Tema.CREMA);
        g.drawString("Foto de la credencial  ·  " + p.credencial.numero, der.x + 38, der.y - 18);
        // zonas para senalar la diferencia (sobre la FOTO de la credencial)
        double cx = der.getCenterX();
        Object[][] zonasL = {
            {"pelo", "Pelo", new Rectangle((int) (cx - 54 * s), (int) (cyD - 74 * s), (int) (108 * s), (int) (34 * s))},
            {"cejas", "Cejas", new Rectangle((int) (cx - 34 * s), (int) (cyD - 24 * s), (int) (68 * s), (int) (11 * s))},
            {"ojos", "Ojos / gafas", new Rectangle((int) (cx - 34 * s), (int) (cyD - 12 * s), (int) (68 * s), (int) (20 * s))},
            {"lunar", "Lunar", new Rectangle((int) (cx + 12 * s), (int) (cyD + 8 * s), (int) (22 * s), (int) (20 * s))}
        };
        // TIENDA: la lupa de alta precision marca la zona sospechosa
        String sospechosa = null;
        if (j.jugador.lupaPrecision && p.diferencia != null) {
            String d = p.diferencia.toLowerCase();
            sospechosa = d.contains("gafas") || d.contains("ojos") ? "ojos" : d.contains("cejas") ? "cejas"
                    : d.contains("pelo") ? "pelo" : "lunar";
        }
        for (Object[] z : zonasL) {
            Rectangle zr = (Rectangle) z[2];
            boolean hv = encima(zr);
            if (z[0].equals(sospechosa)) {
                double pul = 0.5 + 0.5 * Math.sin(t * 5);
                g.setColor(Tema.alfa(Tema.LADRILLO, (int) (60 + 90 * pul)));
                g.setStroke(new BasicStroke(4));
                g.drawRoundRect(zr.x - 4, zr.y - 4, zr.width + 8, zr.height + 8, 18, 18);
                g.setStroke(new BasicStroke(1));
            }
            g.setColor(hv ? Tema.alfa(Tema.DORADO, 80) : new Color(255, 255, 255, 0));
            g.fillRoundRect(zr.x, zr.y, zr.width, zr.height, 14, 14);
            g.setColor(hv ? Tema.COBRE : Tema.alfa(Tema.COBRE, 120));
            g.setStroke(new BasicStroke(hv ? 2.5f : 1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{5, 5}, 0));
            g.drawRoundRect(zr.x, zr.y, zr.width, zr.height, 14, 14);
            g.setStroke(new BasicStroke(1));
            if (hv) {
                g.setFont(Tema.fuente(Font.BOLD, 12));
                int tw = g.getFontMetrics().stringWidth((String) z[1]) + 16;
                g.setColor(Tema.TINTA);
                g.fillRoundRect(zr.x + zr.width + 6, zr.y + zr.height / 2 - 11, tw, 22, 11, 11);
                g.setColor(Tema.CREMA);
                g.drawString((String) z[1], zr.x + zr.width + 14, zr.y + zr.height / 2 + 5);
            }
            final String zn = (String) z[0];
            zona(zr, () -> senalarZona(zn));
        }
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(Tema.CREMA);
        Dibujo.centrado(g, "¿Ves UN detalle distinto? Haz clic en esa zona de la FOTO (pelo, cejas, ojos/gafas o lunar).",
                W / 2.0, 664);
        if (j.jugador.lupaPrecision) {
            g.setFont(Tema.fuente(Font.ITALIC, 12.5f));
            g.setColor(Tema.acento2());
            Dibujo.centrado(g, p.diferencia != null ? "Lupa de alta precisión: la zona en rojo es sospechosa."
                    : "Lupa de alta precisión: no detecta ninguna zona sospechosa.", W / 2.0, 106);
        }
        if (lupaMsg != null) {
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.setColor(Tema.acento2());
            Dibujo.centrado(g, lupaMsg, W / 2.0, 690);
        }
        boton(g, new Rectangle(W / 2 - 330, 712, 300, 54), "Coinciden: es la misma  (L)", Dibujo.Estilo.SECUNDARIO,
                () -> juego = Juego.NADA);
        boton(g, new Rectangle(W / 2 + 30, 712, 300, 54), "No es la misma persona  (X)", Dibujo.Estilo.PELIGRO, this::acusar);
    }

    // ==================================================================
    // Paneles de los 4 botones (siempre disponibles)
    // ==================================================================

    private Rectangle panelBase(Graphics2D g, int ancho, int alto, String titulo, String imagen, boolean oscuro) {
        velo(g);
        Rectangle r = new Rectangle(W / 2 - ancho / 2, BARRA + 14, ancho, Math.min(alto, H - BARRA - 28));
        if (oscuro) {
            Dibujo.tarjetaOscura(g, r, 26);
        } else {
            Dibujo.tarjetaPapel(g, r, 26);
        }
        Recursos.contener(g, imagen, r.x + 22, r.y + 16, 48, 48);
        g.setFont(Tema.fuente(Font.BOLD, 23));
        g.setColor(oscuro ? Tema.texto() : Tema.tinta());
        g.drawString(titulo, r.x + 82, r.y + 48);
        Rectangle x = new Rectangle(r.x + r.width - 58, r.y + 18, 40, 40);
        boolean hv = encima(x);
        g.setColor(hv ? Tema.acento() : oscuro ? new Color(255, 235, 215, 30) : Tema.ROSA_CLARO);
        g.fill(new Ellipse2D.Double(x.x, x.y, x.width, x.height));
        g.setColor(hv ? Tema.CREMA : oscuro ? Tema.texto() : Tema.tinta());
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x.x + 13, x.y + 13, x.x + 27, x.y + 27);
        g.drawLine(x.x + 27, x.y + 13, x.x + 13, x.y + 27);
        g.setStroke(new BasicStroke(1));
        zona(x, () -> panel = Panel.NADA);
        return r;
    }

    // ------------------------------------------------------------ AYUDA
    private static final String[] SECCIONES = {"Objetivo", "Tu rol", "Cómo se juega", "Botones y controles",
        "Los cuatro roles", "Indicadores", "Cómo ganar", "Reglas del día", "Estructuras de datos"};

    private void ayuda(Graphics2D g) {
        Rectangle r = panelBase(g, 1200, 772, "Ayuda · Alcalde Digital", "ayuda.png", false);
        // indice
        int y = r.y + 92;
        for (int i = 0; i < SECCIONES.length; i++) {
            Rectangle b = new Rectangle(r.x + 22, y, 250, 50);
            boolean sel = i == paginaAyuda, hv = encima(b);
            g.setColor(Tema.altoContraste ? (sel ? Color.YELLOW : Color.BLACK) : sel ? Tema.COBRE
                    : hv ? Tema.ROSA_CLARO : new Color(255, 255, 255, 120));
            g.fillRoundRect(b.x, b.y, b.width, b.height, 16, 16);
            if (i == 1) {
                Dibujo.emblemaRol(g, rol(), b.x + 26, b.getCenterY(), 14, true);
            } else {
                g.setColor(sel ? Tema.CREMA : Tema.COBRE);
                g.fill(new Ellipse2D.Double(b.x + 14, b.getCenterY() - 12, 24, 24));
                g.setFont(Tema.fuente(Font.BOLD, 12));
                g.setColor(sel ? Tema.COBRE : Tema.CREMA);
                Dibujo.centrado(g, String.valueOf(i + 1), b.x + 26, b.getCenterY() + 5);
            }
            g.setFont(Tema.fuente(Font.BOLD, 14.5f));
            g.setColor(sel ? (Tema.altoContraste ? Color.BLACK : Tema.CREMA) : Tema.tinta());
            g.drawString(SECCIONES[i], b.x + 50, b.y + 31);
            final int ii = i;
            zona(b, () -> paginaAyuda = ii);
            y += 58;
        }
        // contenido
        Rectangle c = new Rectangle(r.x + 300, r.y + 92, r.width - 330, r.height - 116);
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(255, 255, 255, 150));
        g.fillRoundRect(c.x, c.y, c.width, c.height, 20, 20);
        g.setFont(Tema.fuente(Font.BOLD, 22));
        g.setColor(Tema.acento());
        g.drawString(SECCIONES[paginaAyuda], c.x + 28, c.y + 44);
        int yy = c.y + 82;
        int ancho = c.width - 56;
        int yMax = c.y + c.height - 16;
        RolJugador rr = rol();
        switch (paginaAyuda) {
            case 0:
                yy = parrafoAyuda(g, "Ciudad Nova está a pocos días de elegir alcalde y todo pasa por Civitas, la red social de la "
                        + "ciudad. Circulan rumores, noticias falsas, opiniones y publicidad disfrazada.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "Tu misión es que la ciudad termine la elección con MÁS información verificada, convivencia, "
                        + "confianza y bienestar digital, y con MENOS desinformación y conflictos.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "Cada día llega gente a tu ventanilla con algo que quiere publicar. Conversas, investigas y "
                        + "decides con un sello. Cada decisión cambia la ciudad: se propaga por el grafo social y por los barrios.",
                        c, yy, ancho, yMax);
                parrafoAyuda(g, "No hay preguntas de examen: se aprende decidiendo y viendo las consecuencias.", c, yy, ancho, yMax);
                break;
            case 1:
                Dibujo.emblemaRol(g, rr, c.x + 60, yy + 20, 34, true);
                g.setFont(Tema.fuente(Font.BOLD, 20));
                g.setColor(Tema.tinta());
                g.drawString(j.jugador.nombre + " · " + rr.nombre, c.x + 108, yy + 14);
                g.setFont(Tema.fuente(Font.ITALIC, 14));
                g.setColor(Tema.tintaSuave());
                g.drawString(rr.mision, c.x + 108, yy + 38);
                yy += 82;
                g.setFont(Tema.fuente(Font.BOLD, 16));
                g.setColor(Dibujo.colorRol(rr).darker());
                g.drawString("Tu habilidad única: " + rr.habilidad, c.x + 28, yy);
                yy += 28;
                yy = parrafoAyuda(g, rr.descripcionHabilidad, c, yy, ancho, yMax);
                yy = parrafoAyuda(g, ayudaRol(rr), c, yy, ancho, yMax);
                StringBuilder hs = new StringBuilder();
                for (Herramienta h : Herramienta.values()) {
                    hs.append(hs.length() == 0 ? "" : ", ").append(h.nombre).append(rr.puedeUsar(h) ? "" : " (no)");
                }
                parrafoAyuda(g, "Herramientas del monitor: " + hs + ".", c, yy, ancho, yMax);
                break;
            case 2:
                yy = paso(g, c, yy, 1, "Haz pasar al visitante", "Botón \"¡Siguiente!\" o Espacio. La fila es una cola: el primero en llegar pasa primero.", ancho);
                yy = paso(g, c, yy, 2, "Pregúntale", "Arrastra una tarjeta de pregunta hacia el visitante (o haz clic, o teclas 1 a 5). Cada pregunta gasta 3 minutos.", ancho);
                yy = paso(g, c, yy, 3, "Investiga", "Usa las herramientas del monitor que tenga tu rol. Si encuentras una contradicción aparece una pregunta naranja ¡CONFRONTAR!.", ancho);
                yy = paso(g, c, yy, 4, rr.tieneMiniJuego() ? "Usa tu habilidad" : "Envía al corresponsal", rr.tieneMiniJuego()
                        ? "Pestaña \"Habilidad\" del monitor (tecla U): " + rr.habilidad + "." : "El corresponsal va al barrio por la ruta más corta (Dijkstra). Míralo en el MAPA.", ancho);
                yy = paso(g, c, yy, 5, "Revisa la credencial", "Si el visitante trae carné aparece la BILLETERA en la ventana. Con la lupa señala la diferencia en la foto.", ancho);
                paso(g, c, yy, 6, "Sella", "Toma un sello de la bandeja y ARRÁSTRALO sobre la publicación del celular. A las 18:00 se cierra la ventanilla.", ancho);
                break;
            case 3:
                yy = icono(g, c, yy, "ayuda.png", "Ayuda (M o F1)", "Esta guía. Cambia según tu rol.", ancho);
                yy = icono(g, c, yy, "estructura.png", "Estructuras (F2)", "Árboles, grafos y cola del juego en vivo. El reloj se detiene.", ancho);
                yy = icono(g, c, yy, "mapa.png", "Mapa y ciudad (F3 o G)", "Los indicadores de Ciudad Nova, el grafo de barrios, el pánico y el corresponsal.", ancho);
                yy = icono(g, c, yy, "billetera.png", "Tienda (S o F4)", "Gasta tu dinero en café, horas extra, pistas, seguros, mejoras y campañas.", ancho);
                yy = icono(g, c, yy, "pausa.png", "Pausa (Esc)", "Detiene el reloj. Accesibilidad y salir al menú.", ancho);
                yy = icono(g, c, yy, "billetera.png", "Billetera (I)", "Solo aparece si el visitante trae credencial. La X roja en una herramienta = no aplica.", ancho);
                yy = icono(g, c, yy, "verificarr.png", "Sellos (Z X C V B)", "Arrástralos sobre el celular: verificado, opinión, #publicidad, rechazar, reportar.", ancho);
                parrafoAyuda(g, "Teclas: Espacio continuar · 1-5 preguntas · Q W E R T Y herramientas · U habilidad · K llamar · L lupa · X acusar en la lupa.",
                        c, yy + 4, ancho, yMax);
                break;
            case 4:
                for (RolJugador x : RolJugador.values()) {
                    Dibujo.emblemaRol(g, x, c.x + 50, yy + 18, 22, true);
                    g.setFont(Tema.fuente(Font.BOLD, 15.5f));
                    g.setColor(Tema.tinta());
                    g.drawString(x.nombre + "  ·  " + x.habilidad + (x == rr ? "   (tú)" : ""), c.x + 86, yy + 12);
                    g.setColor(Tema.tintaSuave());
                    yy = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13), x.mision + " " + x.descripcionHabilidad, c.x + 86,
                            yy + 32, ancho - 70, 18, yMax) + 18;
                }
                break;
            case 5:
                String[][] ind = {
                    {"Verificada", "Cuánta información comprobada circula. Sube al sellar bien."},
                    {"Confianza", "Cuánto confía la gente en la información y en los demás."},
                    {"Convivencia", "Qué tan bien se llevan los barrios. El pánico la baja."},
                    {"Bienestar digital", "Qué tan sana es la vida en Civitas."},
                    {"Desinformación", "% de personas del grafo social que creen un bulo. Debe BAJAR."},
                    {"Conflictos", "Peleas y discusiones. Debe BAJAR."}
                };
                for (int i = 0; i < ind.length; i++) {
                    Dibujo.triangulo(g, c.x + 40, yy - 5, 7, i < 4, i < 4 ? Tema.SALVIA : Tema.LADRILLO);
                    g.setFont(Tema.fuente(Font.BOLD, 15));
                    g.setColor(Tema.tinta());
                    g.drawString(ind[i][0], c.x + 58, yy);
                    g.setFont(Tema.fuente(Font.PLAIN, 13.5f));
                    g.setColor(Tema.tintaSuave());
                    g.drawString(ind[i][1], c.x + 58, yy + 20);
                    yy += 54;
                }
                parrafoAyuda(g, "Se consultan en el botón MAPA (su insignia muestra la salud de la ciudad). El triángulo hacia arriba = mientras más alto, mejor. Hacia abajo = mientras más bajo, mejor. "
                        + "Las barras no dependen solo del color.", c, yy, ancho, yMax);
                break;
            case 6:
                yy = parrafoAyuda(g, "Puntaje final = puntos + 10 × salud de la ciudad + 5 × reputación − 100 × advertencias.",
                        c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "Acertar da puntos, reputación y logros; equivocarte baja la confianza y sube la "
                        + "desinformación. Los errores graves son advertencias: con 4 te despiden.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "Bonos: +30 si resuelves rápido, +40 si el impostor confiesa antes de que lo selles. "
                        + "Las alertas virales bien resueltas dan puntos extra.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, rr == RolJugador.CANDIDATO ? "Como candidato, tu intención de voto sale de la confianza, la "
                        + "información verificada, los conflictos y tus ruedas de prensa. ¡Puedes ganar la alcaldía!"
                        : "Al final se elige alcalde: gana quien convenció con propuestas si la ciudad quedó informada; "
                        + "si circularon muchos rumores, gana el candidato de los rumores.", c, yy, ancho, yMax);
                parrafoAyuda(g, "Finales posibles: El sello valiente, Ciudad informada, Ciudad dividida, Ciudad engañada o Despedido.",
                        c, yy, ancho, yMax);
                break;
            case 7:
                for (int d = 1; d <= j.dia; d++) {
                    for (String s : Historia.MANUAL[d]) {
                        g.setColor(Tema.COBRE);
                        g.setFont(Tema.fuente(Font.BOLD, 12));
                        g.drawString("Día " + d, c.x + 28, yy);
                        g.setColor(Tema.tinta());
                        yy = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 14), s, c.x + 90, yy, ancho - 70, 19, yMax) + 8;
                    }
                }
                break;
            default:
                yy = parrafoAyuda(g, "COLA (FIFO): la fila de la ventanilla.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "ÁRBOL N-ARIO de diálogo: cada pregunta es un nodo; al confrontar se INSERTAN nodos.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "ÁRBOL de consecuencias: raíz › sello › verdad. El día 2 se INSERTA la rama #PUBLICIDAD y el 5 se ELIMINA la rama OPINIÓN.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "AVL del directorio: búsqueda O(log n); se eliminan e insertan cuentas durante la semana.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "GRAFO de la ciudad: Dijkstra para el corresponsal y BFS para el pánico entre barrios.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "GRAFO social: BFS probabilístico al publicar, DFS para anillos de bots, eliminar vértice al reportar.", c, yy, ancho, yMax);
                parrafoAyuda(g, "Tu rol: " + estructuraRol(rr), c, yy, ancho, yMax);
                break;
        }
    }

    private int parrafoAyuda(Graphics2D g, String s, Rectangle c, int y, int ancho, int yMax) {
        g.setColor(Tema.tinta());
        return Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 15), s, c.x + 28, y, ancho, 22, yMax) + 12;
    }

    private int paso(Graphics2D g, Rectangle c, int y, int n, String t1, String t2, int ancho) {
        g.setColor(Tema.COBRE);
        g.fill(new Ellipse2D.Double(c.x + 28, y - 18, 30, 30));
        g.setFont(Tema.fuente(Font.BOLD, 14));
        g.setColor(Tema.CREMA);
        Dibujo.centrado(g, String.valueOf(n), c.x + 43, y + 2);
        g.setFont(Tema.fuente(Font.BOLD, 15.5f));
        g.setColor(Tema.tinta());
        g.drawString(t1, c.x + 72, y);
        g.setColor(Tema.tintaSuave());
        return Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13.5f), t2, c.x + 72, y + 21, ancho - 60, 18, c.y + c.height) + 16;
    }

    private int icono(Graphics2D g, Rectangle c, int y, String img, String t1, String t2, int ancho) {
        Recursos.contener(g, img, c.x + 26, y - 22, 46, 46);
        g.setFont(Tema.fuente(Font.BOLD, 15.5f));
        g.setColor(Tema.tinta());
        g.drawString(t1, c.x + 86, y - 2);
        g.setFont(Tema.fuente(Font.PLAIN, 13.5f));
        g.setColor(Tema.tintaSuave());
        g.drawString(Dibujo.recortar(g.getFontMetrics(), t2, ancho - 70), c.x + 86, y + 18);
        return y + 62;
    }

    private static String ayudaRol(RolJugador r) {
        switch (r) {
            case CIUDADANO:
                return "Cómo jugarla: en cada paso verás quién recibió el mensaje y a qué hora. Escoge a la persona que tiene "
                        + "la MISMA huella del mensaje y lo compartió ANTES. Cuando nadie lo tenía antes, llegaste al origen. "
                        + "Si empieza en una cuenta falsa, se agrega una pregunta para confrontar.";
            case INFLUENCER:
                return "Cómo jugarla: aparecen perfiles de quienes comparten la publicación. Haz clic solo en las cuentas falsas "
                        + "(recién creadas, sin foto, cientos de publicaciones). Marcar a una persona real cuenta en contra. "
                        + "Además, cada sello tuyo pesa x1,5: tu alcance es enorme.";
            case CANDIDATO:
                return "Cómo jugarla: escucha la pregunta y arrastra al micrófono la respuesta más responsable antes de que se "
                        + "acaben los 10 segundos. Atacar o repetir rumores sube los conflictos y la desinformación. "
                        + "Lo responsable suma confianza y votos para la elección.";
            default:
                return "Cómo jugarla: eres quien tiene todo el monitor. El corresponsal, el escáner de red y las llamadas "
                        + "a las instituciones son solo tuyos. Confronta al visitante con lo que encuentres.";
        }
    }

    private static String estructuraRol(RolJugador r) {
        switch (r) {
            case CIUDADANO: return "tu habilidad recorre el árbol BFS del grafo social subiendo por los padres hasta el origen.";
            case INFLUENCER: return "tu habilidad usa las 2 primeras oleadas del BFS desde el origen en el grafo social.";
            case CANDIDATO: return "tus respuestas modifican los indicadores de la ciudad y tu intención de voto; el directorio es un AVL.";
            default: return "usas Dijkstra (corresponsal), BFS/DFS (escáner de red) y el AVL (directorio y llamadas).";
        }
    }

    // ------------------------------------------------------------ ESTRUCTURAS
    private void estructuras(Graphics2D g) {
        Rectangle r = panelBase(g, 1400, 778, "Visor de estructuras  ·  el reloj se detiene mientras miras", "estructura.png", true);
        // chip del rol
        RolJugador rr = rol();
        String chip = "Tu rol: " + rr.nombre + "  ·  " + estructuraRol(rr);
        g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
        Rectangle ch = new Rectangle(r.x + 84, r.y + 58, Math.min(r.width - 160, g.getFontMetrics().stringWidth(chip) + 44), 24);
        g.setColor(Tema.alfa(Dibujo.colorRol(rr), 110));
        g.fillRoundRect(ch.x, ch.y, ch.width, ch.height, 12, 12);
        Dibujo.emblemaRol(g, rr, ch.x + 13, ch.getCenterY(), 9, false);
        g.setColor(Tema.texto());
        g.drawString(Dibujo.recortar(g.getFontMetrics(), chip, ch.width - 34), ch.x + 28, ch.y + 17);
        int gx = r.x + 16, gy = r.y + 92, gw = (r.width - 48) / 2, gh = (r.height - 116) / 2;
        Rectangle a = new Rectangle(gx, gy, gw, gh), b = new Rectangle(gx + gw + 16, gy, gw, gh),
                c = new Rectangle(gx, gy + gh + 12, gw, gh), d = new Rectangle(gx + gw + 16, gy + gh + 12, gw, gh);
        cuadro(g, a, caso == null ? "Árbol de diálogo (no hay visitante)" : "Árbol de diálogo de " + caso.visitante.nombre
                + " · " + caso.dialogo.cantidadNodos() + " nodos, altura " + caso.dialogo.altura());
        cuadro(g, b, "Árbol de consecuencias · " + j.jugador.arbol.cantidadNodos() + " nodos · sellos: "
                + j.jugador.arbol.sellos().size());
        cuadro(g, c, "AVL del directorio · " + j.jugador.directorio.getTamano() + " cuentas, altura "
                + j.jugador.directorio.getAltura() + ", rotaciones " + j.jugador.directorio.getRotaciones());
        cuadro(g, d, "Grafos, cola y tu rol");
        if (caso != null) {
            VistaArboles.dialogo(g, caso.dialogo.getRaiz(), j.getNodoActual(), new Rectangle(a.x + 10, a.y + 40, a.width - 20, a.height - 50));
        }
        VistaArboles.consecuencias(g, j.jugador.arbol, ultimo == null ? null : ultimo.hoja,
                new Rectangle(b.x + 10, b.y + 40, b.width - 20, b.height - 50));
        Cuenta cu = caso == null ? null : caso.pub.getCuenta();
        VistaArboles.avl(g, j.jugador.directorio, cu == null ? null : cu.getArroba(),
                new Rectangle(c.x + 10, c.y + 40, c.width - 20, c.height - 50));
        CiudadEstado ci = j.jugador.ciudad;
        List<String> info = new ArrayList<>();
        StringBuilder sb = new StringBuilder("COLA de la ventanilla (" + j.enFila() + "): frente › ");
        for (Caso k : j.verFila()) {
            sb.append(k.visitante.nombre).append(" › ");
        }
        info.add(sb.append("final").toString());
        info.add("GRAFO SOCIAL: " + ci.red.cantidadVertices() + " vértices, " + ci.red.cantidadAristas() + " aristas · cuentas falsas activas "
                + ci.botsActivos().size() + " de " + ci.bots.size() + " · desinformación " + ci.desinformacion() + " %");
        int pan = 0;
        for (int p : ci.panico) {
            pan += p > 0 ? 1 : 0;
        }
        info.add("GRAFO DE LA CIUDAD: " + ci.mapa.getZonas().size() + " zonas, " + ci.mapa.calles().size() + " calles · pánico (BFS) en "
                + pan + " zonas");
        if (ultimo != null) {
            info.add("ÚLTIMO SELLO: " + ultimo.ruta.replace("->", "›"));
        }
        info.add("HABILIDAD (" + rr.habilidad + "): usada " + j.jugador.habilidadesUsadas + " veces, bien "
                + j.jugador.habilidadesBien + ". " + (rr.tieneMiniJuego() ? estructuraRol(rr) : "Tienes todas las herramientas."));
        g.setColor(Tema.texto());
        int y = d.y + 62;
        for (String s : info) {
            y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13), s, d.x + 18, y, d.width - 36, 19, d.y + d.height - 8) + 6;
        }
        g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
        g.setColor(Tema.textoSuave());
        g.drawString("Cobre = nodo INSERTADO al confrontar · color de acento = actual / camino · verde = visitado / encontrado",
                r.x + 20, r.y + r.height - 8);
    }

    private void cuadro(Graphics2D g, Rectangle r, String titulo) {
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(34, 18, 14));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(140, 88, 64, 120));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        g.setFont(Tema.fuente(Font.BOLD, 13.5f));
        g.setColor(Tema.acento2());
        g.drawString(Dibujo.recortar(g.getFontMetrics(), titulo, r.width - 30), r.x + 16, r.y + 26);
    }

    // ------------------------------------------------------------ PAUSA
    private void pausa(Graphics2D g) {
        Rectangle r = panelBase(g, 520, 640, "Pausa", "pausa.png", false);
        JugadorVerificador jv = j.jugador;
        Rectangle info = new Rectangle(r.x + 30, r.y + 84, r.width - 60, 74);
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(255, 255, 255, 150));
        g.fillRoundRect(info.x, info.y, info.width, info.height, 18, 18);
        Dibujo.emblemaRol(g, jv.rol, info.x + 40, info.getCenterY(), 24, true);
        g.setFont(Tema.fuente(Font.BOLD, 17));
        g.setColor(Tema.tinta());
        g.drawString(jv.nombre + " · " + jv.rol.nombre, info.x + 78, info.y + 32);
        g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
        g.setColor(Tema.tintaSuave());
        g.drawString("Día " + j.dia + " · " + jv.puntos + " pts · habilidad: " + jv.rol.habilidad, info.x + 78, info.y + 54);
        int y = r.y + 178;
        boton(g, new Rectangle(r.x + 40, y, r.width - 80, 50), "Continuar  (Esc)", Dibujo.Estilo.PRINCIPAL, () -> panel = Panel.NADA);
        y += 64;
        String[] op = {"Alto contraste", "Texto grande", "Sonido", "Subtítulos de sonido"};
        boolean[] v = {Tema.altoContraste, Tema.textoGrande, Tema.sonido, Tema.subtitulos};
        for (int i = 0; i < op.length; i++) {
            final int k = i;
            boton(g, new Rectangle(r.x + 40, y, r.width - 80, 44), op[i] + ":  " + (v[i] ? "SÍ" : "NO"),
                    Dibujo.Estilo.SECUNDARIO, () -> {
                        switch (k) {
                            case 0: Tema.altoContraste = !Tema.altoContraste; break;
                            case 1: Tema.textoGrande = !Tema.textoGrande; break;
                            case 2: Tema.sonido = !Tema.sonido; break;
                            default: Tema.subtitulos = !Tema.subtitulos; break;
                        }
                    });
            y += 52;
        }
        y += 10;
        boton(g, new Rectangle(r.x + 40, y, (r.width - 92) / 2, 46), "Ayuda", Dibujo.Estilo.LILA, () -> panel = Panel.AYUDA);
        boton(g, new Rectangle(r.x + 52 + (r.width - 92) / 2, y, (r.width - 92) / 2, 46), "Salir al menú",
                Dibujo.Estilo.PELIGRO, () -> {
                    panel = Panel.NADA;
                    activo = false;
                    if (oyente != null) {
                        oyente.alSalir();
                    }
                });
    }

    // ------------------------------------------------------------ MAPA (grafo de la ciudad)
    private void panelMapa(Graphics2D g) {
        Rectangle r = panelBase(g, 1400, 768, "Mapa y estado de Ciudad Nova", "mapa.png", true);
        Rectangle m = new Rectangle(r.x + 22, r.y + 82, 600, r.height - 104);
        mapa(g, m);
        // los indicadores de la ciudad viven aqui (antes estaban en la pantalla principal)
        Rectangle ind = new Rectangle(r.x + r.width - 22 - 420, m.y, 420, m.height);
        indicadores(g, ind);
        Rectangle ley = new Rectangle(m.x + m.width + 18, m.y, ind.x - 18 - (m.x + m.width + 18), m.height);
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(34, 18, 14));
        g.fillRoundRect(ley.x, ley.y, ley.width, ley.height, 16, 16);
        int y = ley.y + 30;
        g.setFont(Tema.fuente(Font.BOLD, 14));
        g.setColor(Tema.acento2());
        g.drawString("LEYENDA", ley.x + 18, y);
        y += 26;
        Object[][] l = {{Tema.COBRE, "Tu oficina (Civitas Verifica)"}, {Tema.DORADO, "Barrio del caso actual"},
            {Tema.LADRILLO, "Barrio con pánico (BFS)"}, {Tema.ROSA, "Barrio tranquilo"}};
        for (Object[] o : l) {
            g.setColor((Color) o[0]);
            g.fill(new Ellipse2D.Double(ley.x + 18, y - 10, 12, 12));
            g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
            g.setColor(Tema.texto());
            g.drawString((String) o[1], ley.x + 38, y);
            y += 24;
        }
        y += 6;
        g.setColor(Tema.DORADO);
        g.setStroke(new BasicStroke(3));
        g.drawLine(ley.x + 18, y - 5, ley.x + 32, y - 5);
        g.setStroke(new BasicStroke(1));
        g.setColor(Tema.texto());
        g.drawString("Ruta del corresponsal (Dijkstra)", ley.x + 38, y);
        y += 36;
        g.setColor(Tema.textoSuave());
        String txt;
        if (rol().puedeUsar(Herramienta.CORRESPONSAL)) {
            txt = j.corresponsalEnCamino() ? "El corresponsal va en camino: mira la moto avanzar por la ruta más corta."
                    : "Envía el corresponsal desde el monitor (tecla T). Más lejos = más minutos.";
        } else {
            txt = "El corresponsal es del periodista. Aquí ves cómo el pánico salta de barrio en barrio (BFS) cuando "
                    + "se publica un bulo.";
        }
        y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13), txt, ley.x + 18, y, ley.width - 36, 19, ley.y + ley.height - 10);
        y += 14;
        g.setColor(Tema.texto());
        Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 12.5f), "Vértices = lugares. Aristas = calles con su distancia. "
                + "El reloj sigue corriendo mientras miras el mapa.", ley.x + 18, y, ley.width - 36, 18, ley.y + ley.height - 10);
    }

    private void mapa(Graphics2D g, Rectangle r) {
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(34, 18, 14));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        if (!Tema.altoContraste) {
            Recursos.contener(g, "mapa.png", r.getCenterX() - 160, r.getCenterY() - 160, 320, 320);
            g.setColor(new Color(34, 18, 14, 215));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        }
        CiudadEstado c = j.jugador.ciudad;
        double esc = Math.min(r.width / 560.0, r.height / 540.0) * 0.95;
        double cx = r.x + r.width / 2.0, cy = r.y + r.height / 2.0 + 6;
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Tema.altoContraste ? Color.GRAY : new Color(140, 88, 64, 160));
        for (int[] calle : c.mapa.calles()) {
            GrafoCiudad.Zona a = c.mapa.zona(calle[0]), b = c.mapa.zona(calle[1]);
            g.drawLine((int) (cx + a.x * esc), (int) (cy + a.z * esc), (int) (cx + b.x * esc), (int) (cy + b.z * esc));
        }
        g.setStroke(new BasicStroke(1));
        List<GrafoCiudad.Zona> ruta = j.getRutaCorresponsal();
        if (ruta != null && caso != null) {
            g.setColor(Tema.acento2());
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i + 1 < ruta.size(); i++) {
                GrafoCiudad.Zona a = ruta.get(i), b = ruta.get(i + 1);
                g.drawLine((int) (cx + a.x * esc), (int) (cy + a.z * esc), (int) (cx + b.x * esc), (int) (cy + b.z * esc));
            }
            g.setStroke(new BasicStroke(1));
            double av = j.corresponsalListo() ? 1 : j.avanceCorresponsal();
            double[] pt = puntoEnRuta(ruta, av);
            int mxp = (int) (cx + pt[0] * esc), myp = (int) (cy + pt[1] * esc);
            g.setColor(Tema.TINTA);
            g.fill(new Ellipse2D.Double(mxp - 16, myp - 16, 32, 32));
            Iconos.herramienta(g, 4, mxp, myp - 3, 11, Tema.DORADO);
        }
        for (GrafoCiudad.Zona z : c.mapa.getZonas()) {
            int x = (int) (cx + z.x * esc), y = (int) (cy + z.z * esc);
            boolean pan = c.panico[z.id] > 0;
            if (pan) {
                double pulso = 18 + 8 * Math.sin(t * 6);
                g.setColor(Tema.alfa(Tema.LADRILLO, 90));
                g.fill(new Ellipse2D.Double(x - pulso, y - pulso, pulso * 2, pulso * 2));
            }
            boolean destino = caso != null && caso.zona == z.id && z.id > 1;
            g.setColor(pan ? Tema.mal() : z.id == 1 ? Tema.COBRE : destino ? Tema.DORADO : Tema.ROSA);
            g.fill(new Ellipse2D.Double(x - 11, y - 11, 22, 22));
            g.setColor(Tema.CREMA);
            g.draw(new Ellipse2D.Double(x - 11, y - 11, 22, 22));
            g.setFont(Tema.fuente(Font.BOLD, 12.5f));
            String n = (pan ? "! " : "") + (z.id == 1 ? "Tu oficina" : z.nombre);
            int tw = g.getFontMetrics().stringWidth(n) + 14;
            g.setColor(new Color(42, 23, 18, 200));
            g.fillRoundRect(x - tw / 2, y - 40, tw, 22, 11, 11);
            g.setColor(Tema.texto());
            Dibujo.centrado(g, n, x, y - 24);
        }
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

    // ------------------------------------------------------------ turno y fin del dia
    private void tarjetaTurno(Graphics2D g) {
        velo(g);
        JugadorVerificador jv = j.jugador;
        Rectangle r = new Rectangle(W / 2 - 360, 200, 720, 420);
        Dibujo.tarjetaPapel(g, r, 30);
        Dibujo.emblemaRol(g, jv.rol, r.getCenterX(), r.y + 82, 46, true);
        g.setFont(Tema.fuente(Font.BOLD, 34));
        g.setColor(Tema.tinta());
        Dibujo.centrado(g, "Turno de " + jv.nombre, r.getCenterX(), r.y + 176);
        g.setFont(Tema.fuente(Font.BOLD, 16));
        g.setColor(Dibujo.colorRol(jv.rol).darker());
        Dibujo.centrado(g, jv.rol.nombre + "  ·  habilidad: " + jv.rol.habilidad, r.getCenterX(), r.y + 210);
        g.setFont(Tema.fuente(Font.PLAIN, 15));
        g.setColor(Tema.tintaSuave());
        Dibujo.centrado(g, "Día " + j.dia + " · " + Historia.TITULOS[j.dia] + " · los demás, ¡no miren!", r.getCenterX(), r.y + 244);
        g.setColor(Tema.tinta());
        Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 14), jv.rol.mision, r.x + 90, r.y + 280, r.width - 180, 20, r.y + 320);
        boton(g, new Rectangle(r.x + r.width / 2 - 160, r.y + r.height - 86, 320, 58), "Empezar  (Espacio)",
                Dibujo.Estilo.PRINCIPAL, this::empezarLlamada);
    }

    private void finDia(Graphics2D g) {
        velo(g);
        Rectangle r = new Rectangle(W / 2 - 470, 84, 940, 752);
        Dibujo.tarjetaPapel(g, r, 30);
        JugadorVerificador jv = j.jugador;
        Dibujo.emblemaRol(g, jv.rol, r.x + 60, r.y + 58, 30, true);
        g.setFont(Tema.fuente(Font.BOLD, 30));
        g.setColor(jv.despedido ? Tema.mal() : Tema.tinta());
        g.drawString(jv.despedido ? "¡Despedido!" : "Fin del día " + j.dia, r.x + 106, r.y + 58);
        g.setFont(Tema.fuente(Font.PLAIN, 14));
        g.setColor(Tema.tintaSuave());
        g.drawString(jv.nombre + " · " + jv.rol.nombre + " · " + Historia.TITULOS[j.dia], r.x + 108, r.y + 82);
        // tarjetas de numeros
        String[][] nums = {
            {"Aciertos", String.valueOf(j.getAciertosDia())}, {"Errores", String.valueOf(j.getErroresDia())},
            {"Sin atender", String.valueOf(j.sinAtender())}, {"Puntos del día", String.valueOf(j.getPuntosDia())},
            {"Total", String.valueOf(jv.puntos)}, {"Dinero (tienda: S)", "$" + jv.dinero},
            {"Advertencias", jv.advertencias + "/" + JugadorVerificador.MAX_ADVERTENCIAS}, {"Salud ciudad", String.valueOf(jv.ciudad.salud())}
        };
        int cw = (r.width - 60 - 3 * 14) / 4;
        for (int i = 0; i < nums.length; i++) {
            Rectangle c = new Rectangle(r.x + 30 + (i % 4) * (cw + 14), r.y + 108 + (i / 4) * 80, cw, 68);
            g.setColor(Tema.altoContraste ? Color.BLACK : new Color(255, 255, 255, 160));
            g.fillRoundRect(c.x, c.y, c.width, c.height, 16, 16);
            g.setFont(Tema.fuente(Font.PLAIN, 12));
            g.setColor(Tema.tintaSuave());
            g.drawString(nums[i][0], c.x + 16, c.y + 24);
            g.setFont(Tema.fuente(Font.BOLD, 22));
            g.setColor(Tema.tinta());
            g.drawString(nums[i][1], c.x + 16, c.y + 54);
        }
        int y = r.y + 290;
        g.setFont(Tema.fuente(Font.BOLD, 17));
        g.setColor(Tema.acento());
        g.drawString("Titulares de mañana", r.x + 32, y);
        y += 28;
        for (Jornada.Resultado res : j.getResultados()) {
            if (y > r.y + r.height - 220) {
                break;
            }
            Iconos.marca(g, res.efecto.acierto ? 2 : 3, r.x + 42, y - 5, 7);
            g.setFont(Tema.fuente(Font.PLAIN, 14));
            g.setColor(Tema.tinta());
            g.drawString(Dibujo.recortar(g.getFontMetrics(), res.titular, r.width - 100), r.x + 60, y);
            y += 24;
        }
        if (!jv.logros.isEmpty()) {
            y += 10;
            g.setFont(Tema.fuente(Font.BOLD, 16));
            g.setColor(Tema.acento());
            g.drawString("Logros (" + jv.logros.size() + ")", r.x + 32, y);
            y += 26;
            g.setFont(Tema.fuente(Font.PLAIN, 13));
            for (String lg : jv.logros) {
                if (y > r.y + r.height - 100) {
                    break;
                }
                Iconos.marca(g, 4, r.x + 44, y - 5, 7);
                g.setColor(Tema.tinta());
                g.drawString(lg, r.x + 60, y);
                y += 21;
            }
        }
        String txt = partida.terminada() || (j.dia == Historia.DIAS && partida.esUltimoTurnoDelDia()) || jv.despedido
                && partida.jugadores.size() == 1 ? "Ver el final  (Espacio)" : "Continuar  (Espacio)";
        boton(g, new Rectangle(r.x + r.width - 360, r.y + r.height - 82, 330, 56), txt, Dibujo.Estilo.PRINCIPAL, () -> {
            if (oyente != null) {
                oyente.alTerminarDia(j);
            }
        });
    }

    // ------------------------------------------------------------ aviso flotante rojo
    private void alertaFlotante(Graphics2D g) {
        double edad = t - alertaT;
        if (alertaTxt == null || edad > 4.2) {
            return;
        }
        double alfa = edad < 0.2 ? edad / 0.2 : edad > 3.6 ? (4.2 - edad) / 0.6 : 1;
        double baja = edad < 0.25 ? (1 - edad / 0.25) * -16 : 0;
        Font f = Tema.fuente(Font.BOLD, 14.5f);
        g.setFont(f);
        List<String> l = Avisos.partir(g, f, alertaTxt, 560);
        int ancho = 0;
        for (String x : l) {
            ancho = Math.max(ancho, g.getFontMetrics().stringWidth(x));
        }
        Rectangle r = new Rectangle(W / 2 - (ancho + 76) / 2, (int) (BARRA + 12 + baja), ancho + 76, 22 + l.size() * 21);
        Composite antes = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, Math.min(1, alfa))));
        Dibujo.sombra(g, new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 20, 20), 5, 4);
        g.setColor(Tema.altoContraste ? Color.RED : Tema.LADRILLO);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 20, 20);
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(240, 150, 130));
        g.setStroke(new BasicStroke(1.6f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 20, 20);
        g.setStroke(new BasicStroke(1));
        g.setColor(Tema.CREMA);
        g.fill(new Ellipse2D.Double(r.x + 14, r.y + r.height / 2.0 - 12, 24, 24));
        g.setColor(Tema.LADRILLO);
        g.setFont(Tema.fuente(Font.BOLD, 16));
        Dibujo.centrado(g, "!", r.x + 26, r.y + r.height / 2.0 + 6);
        g.setFont(f);
        g.setColor(Tema.CREMA);
        int y = r.y + 27;
        for (String x : l) {
            g.drawString(x, r.x + 52, y);
            y += 21;
        }
        g.setComposite(antes);
    }

    // ------------------------------------------------------------ TIENDA
    /** Boton redondo de la tienda (mismo estilo que los botones con imagen: circulo rosa con borde cafe). */
    private void botonTienda(Graphics2D g, Rectangle r) {
        boolean hv = encima(r), activoP = panel == Panel.TIENDA;
        double k = hv ? 1.08 : 1.0;
        double w = r.width * k, x = r.getCenterX() - w / 2, y = r.getCenterY() - w / 2;
        if ((hv || activoP) && !Tema.altoContraste) {
            g.setColor(Tema.alfa(Tema.DORADO, activoP ? 120 : 90));
            g.fill(new Ellipse2D.Double(x - 4, y - 4, w + 8, w + 8));
        }
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(96, 46, 36));
        g.fill(new Ellipse2D.Double(x, y, w, w));
        g.setPaint(new GradientPaint((float) x, (float) y, Tema.altoContraste ? Color.BLACK : new Color(246, 228, 222),
                (float) x, (float) (y + w), Tema.altoContraste ? Color.BLACK : new Color(214, 176, 166)));
        g.fill(new Ellipse2D.Double(x + w * 0.09, y + w * 0.09, w * 0.82, w * 0.82));
        // bolsa de compras
        double cx = r.getCenterX(), cy = r.getCenterY() + 2, s = w / 50.0;
        Color cafe = Tema.altoContraste ? Color.YELLOW : new Color(96, 46, 36);
        g.setColor(Tema.altoContraste ? Color.BLACK : Tema.COBRE);
        g.fill(new RoundRectangle2D.Double(cx - 11 * s, cy - 7 * s, 22 * s, 18 * s, 5 * s, 5 * s));
        g.setColor(cafe);
        g.setStroke(new BasicStroke((float) (2.4 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new RoundRectangle2D.Double(cx - 11 * s, cy - 7 * s, 22 * s, 18 * s, 5 * s, 5 * s));
        g.draw(new java.awt.geom.Arc2D.Double(cx - 6 * s, cy - 14 * s, 12 * s, 14 * s, 0, 180, java.awt.geom.Arc2D.OPEN));
        g.setStroke(new BasicStroke(1));
        g.setFont(Tema.fuente(Font.BOLD, (float) (9 * s)));
        g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.CREMA);
        Dibujo.centrado(g, "$", cx, cy + 6 * s);
        if (hv) {
            String et = "Tienda (S)";
            g.setFont(Tema.fuente(Font.BOLD, 12));
            int tw = g.getFontMetrics().stringWidth(et) + 16;
            int tx = (int) (r.getCenterX() - tw / 2.0), ty = r.y + r.height + 6;
            g.setColor(Tema.altoContraste ? Color.BLACK : new Color(42, 23, 18, 235));
            g.fillRoundRect(tx, ty, tw, 22, 11, 11);
            g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.CREMA);
            g.drawString(et, tx + 8, ty + 16);
        }
        zona(r, () -> {
            panel = panel == Panel.TIENDA ? Panel.NADA : Panel.TIENDA;
            arrastraSello = null;
            arrastraPregunta = null;
        });
    }

    private void comprar(Tienda.Articulo a) {
        String m = Tienda.comprar(a, j);
        if (m.startsWith("[x] ")) {
            alertaRoja(m.substring(4));
            tiendaMsg = null;
            return;
        }
        tiendaMsg = a.nombre + ": " + m;
        tiendaOk = true;
        tiendaT = t;
        Sonido.tocar(Sonido.Tipo.LOGRO);
        if (a == Tienda.Articulo.INFORMANTE) {
            avisos.agregar("Pista del informante", m, Tema.acento2(), 0);
        }
    }

    /** Icono dibujado de cada articulo de la tienda. */
    private void iconoArticulo(Graphics2D g, Tienda.Articulo a, double cx, double cy, double r) {
        Graphics2D s = (Graphics2D) g.create();
        s.setStroke(new BasicStroke((float) (r * 0.13), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        s.setColor(Tema.CREMA);
        switch (a) {
            case CAFE:
                s.draw(new RoundRectangle2D.Double(cx - r * 0.45, cy - r * 0.25, r * 0.7, r * 0.7, r * 0.2, r * 0.2));
                s.draw(new java.awt.geom.Arc2D.Double(cx + r * 0.1, cy - r * 0.1, r * 0.4, r * 0.35, -90, 180, java.awt.geom.Arc2D.OPEN));
                s.draw(new java.awt.geom.Line2D.Double(cx - r * 0.2, cy - r * 0.6, cx - r * 0.2, cy - r * 0.4));
                s.draw(new java.awt.geom.Line2D.Double(cx + r * 0.05, cy - r * 0.6, cx + r * 0.05, cy - r * 0.4));
                break;
            case HORAS_EXTRA:
                s.draw(new Ellipse2D.Double(cx - r * 0.5, cy - r * 0.5, r, r));
                s.draw(new java.awt.geom.Line2D.Double(cx, cy, cx, cy - r * 0.32));
                s.draw(new java.awt.geom.Line2D.Double(cx, cy, cx + r * 0.25, cy + r * 0.1));
                break;
            case INFORMANTE:
                s.fill(new Ellipse2D.Double(cx - r * 0.2, cy - r * 0.55, r * 0.4, r * 0.4));
                s.fill(new java.awt.geom.Arc2D.Double(cx - r * 0.42, cy - r * 0.1, r * 0.84, r * 0.8, 0, 180, java.awt.geom.Arc2D.CHORD));
                s.draw(new java.awt.geom.Line2D.Double(cx - r * 0.5, cy - r * 0.42, cx + r * 0.5, cy - r * 0.42));
                break;
            case RECARGA:
                s.draw(new java.awt.geom.Arc2D.Double(cx - r * 0.45, cy - r * 0.45, r * 0.9, r * 0.9, 60, 280, java.awt.geom.Arc2D.OPEN));
                s.fill(new java.awt.geom.Ellipse2D.Double(cx + r * 0.12, cy - r * 0.5, r * 0.22, r * 0.22));
                break;
            case LUPA_PRO:
                Iconos.herramienta(s, 2, cx, cy, r * 0.62, Tema.CREMA);
                break;
            case SEGURO: {
                Path2D e = new Path2D.Double();
                e.moveTo(cx, cy - r * 0.55);
                e.lineTo(cx + r * 0.45, cy - r * 0.35);
                e.curveTo(cx + r * 0.45, cy + r * 0.2, cx + r * 0.2, cy + r * 0.45, cx, cy + r * 0.58);
                e.curveTo(cx - r * 0.2, cy + r * 0.45, cx - r * 0.45, cy + r * 0.2, cx - r * 0.45, cy - r * 0.35);
                e.closePath();
                s.draw(e);
                break;
            }
            default:
                Path2D m = new Path2D.Double();
                m.moveTo(cx - r * 0.45, cy - r * 0.1);
                m.lineTo(cx + r * 0.25, cy - r * 0.45);
                m.lineTo(cx + r * 0.25, cy + r * 0.35);
                m.lineTo(cx - r * 0.45, cy + r * 0.05);
                m.closePath();
                s.draw(m);
                s.draw(new java.awt.geom.Line2D.Double(cx - r * 0.3, cy + r * 0.05, cx - r * 0.2, cy + r * 0.45));
                break;
        }
        s.dispose();
    }

    private void tienda(Graphics2D g) {
        Rectangle r = panelBase(g, 1160, 720, "Tienda de la agencia", "billetera.png", false);
        JugadorVerificador jv = j.jugador;
        // saldo
        Rectangle sal = new Rectangle(r.x + r.width - 330, r.y + 22, 250, 40);
        g.setColor(Tema.altoContraste ? Color.BLACK : Tema.TINTA);
        g.fillRoundRect(sal.x, sal.y, sal.width, sal.height, 20, 20);
        g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
        g.setColor(Tema.textoSuave());
        g.drawString("Tu dinero", sal.x + 18, sal.y + 25);
        g.setFont(Tema.fuente(Font.BOLD, 20));
        g.setColor(Tema.acento2());
        String dn = "$" + jv.dinero;
        g.drawString(dn, sal.x + sal.width - 18 - g.getFontMetrics().stringWidth(dn), sal.y + 28);
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(Tema.tintaSuave());
        g.drawString("Gasta lo que ganas (sueldo, casos y campañas) en mejoras y suministros. El reloj se detiene mientras compras.",
                r.x + 84, r.y + 74);
        Tienda.Articulo[] arts = Tienda.Articulo.values();
        int cols = 4, gap = 16;
        int cw = (r.width - 48 - gap * (cols - 1)) / cols, ch = 250;
        for (int i = 0; i < arts.length; i++) {
            Tienda.Articulo a = arts[i];
            Rectangle c = new Rectangle(r.x + 24 + (i % cols) * (cw + gap), r.y + 96 + (i / cols) * (ch + gap), cw, ch);
            String no = Tienda.motivoNoCompra(a, j);
            boolean hv = encima(c);
            g.setColor(Tema.altoContraste ? Color.BLACK : hv ? Color.WHITE : new Color(255, 255, 255, 170));
            g.fillRoundRect(c.x, c.y, c.width, c.height, 20, 20);
            g.setColor(hv ? Tema.acento() : Tema.altoContraste ? Color.WHITE : new Color(200, 160, 140));
            g.setStroke(new BasicStroke(hv ? 2.2f : 1.2f));
            g.drawRoundRect(c.x, c.y, c.width, c.height, 20, 20);
            g.setStroke(new BasicStroke(1));
            Color ic = i < 3 ? Tema.COBRE : i < 5 ? Tema.LILA : i == 5 ? Tema.SALVIA : Tema.DORADO.darker();
            g.setColor(Tema.altoContraste ? Color.DARK_GRAY : ic);
            g.fill(new Ellipse2D.Double(c.x + 18, c.y + 18, 54, 54));
            iconoArticulo(g, a, c.x + 45, c.y + 45, 27);
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(Tema.tintaSuave());
            g.drawString(a.tipo.toUpperCase(), c.x + 84, c.y + 36);
            g.setFont(Tema.fuente(Font.BOLD, 17));
            g.setColor(Tema.acento());
            g.drawString("$" + a.precio, c.x + 84, c.y + 60);
            g.setColor(Tema.tinta());
            int yy = Dibujo.parrafo(g, Tema.fuente(Font.BOLD, 14.5f), a.nombre, c.x + 18, c.y + 98, c.width - 36, 19, c.y + 140);
            g.setColor(Tema.tintaSuave());
            Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 12.5f), a.descripcion, c.x + 18, yy + 4, c.width - 36, 17, c.y + ch - 62);
            if (a == Tienda.Articulo.SEGURO && jv.seguros > 0) {
                g.setFont(Tema.fuente(Font.BOLD, 11.5f));
                g.setColor(Tema.bien());
                g.drawString("Tienes " + jv.seguros, c.x + c.width - 70, c.y + 36);
            }
            if (a == Tienda.Articulo.LUPA_PRO && jv.lupaPrecision) {
                g.setFont(Tema.fuente(Font.BOLD, 11.5f));
                g.setColor(Tema.bien());
                g.drawString("Comprada", c.x + c.width - 76, c.y + 36);
            }
            Rectangle b = new Rectangle(c.x + 16, c.y + ch - 54, c.width - 32, 40);
            final Tienda.Articulo aa = a;
            if (no == null) {
                boton(g, b, "Comprar  (" + (i + 1) + ")", Dibujo.Estilo.PRINCIPAL, () -> comprar(aa));
            } else {
                // deshabilitado: el clic explica en rojo por que no se puede
                Dibujo.boton(g, b, "No disponible", Dibujo.Estilo.SECUNDARIO, encima(b), false);
                zona(b, () -> alertaRoja(no));
            }
        }
        if (tiendaMsg != null && t - tiendaT < 5) {
            Rectangle m = new Rectangle(r.x + 24, r.y + r.height - 64, r.width - 48, 44);
            g.setColor(Tema.altoContraste ? Color.BLACK : Tema.alfa(Tema.SALVIA, 230));
            g.fillRoundRect(m.x, m.y, m.width, m.height, 18, 18);
            Iconos.marca(g, 2, m.x + 24, m.getCenterY(), 9);
            g.setFont(Tema.fuente(Font.BOLD, 13.5f));
            g.setColor(Tema.CREMA);
            g.drawString(Dibujo.recortar(g.getFontMetrics(), tiendaMsg, m.width - 70), m.x + 46, m.y + 28);
        } else {
            g.setFont(Tema.fuente(Font.ITALIC, 12.5f));
            g.setColor(Tema.tintaSuave());
            g.drawString("Compras de la partida: " + (jv.compras.isEmpty() ? "ninguna todavía" : jv.compras.size() + " · gastado $" + jv.gastado)
                    + "   ·   teclas 1 a " + arts.length + " para comprar · S o Esc para cerrar", r.x + 28, r.y + r.height - 34);
        }
    }

    // ------------------------------------------------------------ utilidades
    private void centrado(Graphics2D g, String s, Rectangle r, Color c, int dy) {
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(c);
        Dibujo.centrado(g, s, r.getCenterX(), r.getCenterY() + dy);
    }

    /** Utilidad de color (se conserva por compatibilidad). */
    static final class Escala {
        private Escala() {
        }

        static Color mezclar(Color a, Color b, double k) {
            return Tema.mezclar(a, b, k);
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

    /** NADA, LUPA, BILLETERA, AYUDA (o MANUAL), ESTRUCTURAS, PAUSA, MAPA, HABILIDAD, VIRAL. */
    public void forzarCapa(String nombre) {
        switch (nombre) {
            case "LUPA": abrirLupa(); break;
            case "BILLETERA": juego = Juego.BILLETERA; break;
            case "MANUAL":
            case "AYUDA": panel = Panel.AYUDA; break;
            case "ESTRUCTURAS": panel = Panel.ESTRUCTURAS; break;
            case "PAUSA": panel = Panel.PAUSA; break;
            case "MAPA": panel = Panel.MAPA; break;
            case "TIENDA": panel = Panel.TIENDA; break;
            case "HABILIDAD": usarHabilidad(); break;
            case "VIRAL":
                mini = new MiniJuegos.Viral(this, Habilidades.viral(new Random()));
                juego = Juego.MINIJUEGO;
                break;
            default:
                panel = Panel.NADA;
                juego = Juego.NADA;
                break;
        }
    }

    /** Para las capturas: pagina de la ayuda y pestana del monitor. */
    public void forzarVista(int paginaAyuda, int pestana) {
        this.paginaAyuda = paginaAyuda;
        this.tab = pestana;
    }

    /** Para las capturas: el mini-juego activo (o null). */
    Object miniActivo() {
        return mini;
    }

    /** Para las capturas: simula un clic en coordenadas del lienzo. */
    public void simularClic(int x, int y) {
        mx = x;
        my = y;
        if (presionar(x, y)) {
            soltar(x, y);
            return;
        }
        for (int i = zonas.size() - 1; i >= 0; i--) {
            if (zonas.get(i).r.contains(x, y)) {
                zonas.get(i).accion.run();
                return;
            }
        }
    }

    /** Para las capturas: simula arrastrar de un punto a otro. */
    public void simularArrastre(int x1, int y1, int x2, int y2) {
        mx = x1;
        my = y1;
        if (presionar(x1, y1)) {
            arrastrar(x2, y2);
            soltar(x2, y2);
        }
    }

    public void moverMouse(int x, int y) {
        mx = x;
        my = y;
        dragX = x;
        dragY = y;
    }
}
