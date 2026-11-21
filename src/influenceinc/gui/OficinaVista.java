package influenceinc.gui;

import influenceinc.estructuras.GrafoCiudad;
import influenceinc.estructuras.NodoDialogo;
import influenceinc.logica.Caso;
import influenceinc.logica.CiudadEstado;
import influenceinc.logica.Habilidades;
import influenceinc.logica.Historia;
import influenceinc.logica.Jornada;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Noticiero;
import influenceinc.logica.Partida;
import influenceinc.logica.PistaSellos;
import influenceinc.logica.Telefono;
import influenceinc.logica.Tienda;
import influenceinc.modelo.Cuenta;
import influenceinc.modelo.Herramienta;
import influenceinc.modelo.Persona;
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
 * Java2D, sobre un lienzo logico de 1440 x 860 que se escala a la ventana.
 *
 * Version 4: pantalla limpia. Solo se ven la VENTANILLA (el visitante detras
 * del vidrio) y el cuadro de DIALOGO con las preguntas. Lo demas aparece
 * cuando hace falta:
 *  - el CELULAR (gui/CelularVista) vive escondido a la derecha y sube con
 *    una llamada, una notificacion importante o la tecla P; las herramientas
 *    de investigacion son apps del celular;
 *  - los SELLOS solo salen al decidir (boton "Decidir el sello", D o Z X C V):
 *    la pista estilo Pump It Up de gui/JuegoSellos arranca enseguida en un
 *    panel pequeno al lado del visitante;
 *  - el NOTICIERO abre y cierra cada dia (gui/TeleVista) y lanza avisos de
 *    "ultima hora" sobre la ventanilla.
 * Arriba sigue la barra con ayuda, estructuras, mapa y pausa.
 */
public class OficinaVista extends Lienzo {

    public static final int W = 1440, H = 860;

    /** Lo que la ventana necesita saber. */
    public interface Oyente {
        void alSellar(Jornada j, Jornada.Resultado r);

        void alTerminarDia(Jornada j);

        void alSalir();

        /** Paso un evento del dia (para contarselo a los demas jugadores en red). */
        void alEvento(Jornada j, String texto);
    }

    private enum Estado { TURNO, ESPERA, NOTICIAS, LLAMADA, VACIO, ENTRANDO, ATENDIENDO, SELLANDO, RESULTADO, SALIENDO, FIN_DIA }

    /** Capas del juego (tapan el escritorio). */
    private enum Juego { NADA, LUPA, BILLETERA, MINIJUEGO }

    /** Paneles de los botones de la barra (no tapan la barra). */
    private enum Panel { NADA, AYUDA, ESTRUCTURAS, PAUSA, MAPA }

    // zonas de la pantalla
    private static final int BARRA = 64;
    /** Ancho de la escena (ventanilla + dialogo). Se corre a la izquierda cuando sube el celular. */
    private static final int ESCENA_W = 1000;
    private static final int X_CENTRADA = (W - ESCENA_W) / 2, X_CORRIDA = 36;
    /** Alto del letrero de la ventanilla (encima del vidrio). */
    private static final int CABECERA = 46;
    private Rectangle ventanaR = new Rectangle(X_CENTRADA, 76, ESCENA_W, 564);
    private Rectangle dialogoR = new Rectangle(X_CENTRADA, 652, ESCENA_W, 194);

    private final Oyente oyente;
    final Avisos avisos = new Avisos();
    private final VistaRed3D red3d = new VistaRed3D();
    final CelularVista cel = new CelularVista(this);
    private final Random azar = new Random();
    private boolean activo;

    private Partida partida;
    private Jornada j;
    private Estado estado = Estado.VACIO;
    private Juego juego = Juego.NADA;
    private Panel panel = Panel.NADA;
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

    // noticiero
    private List<Noticiero.Segmento> segmentos = new ArrayList<>();
    private int segIdx;
    private double segLetras;
    private double segInicioT;
    private boolean noticiasNoche, reviendo;
    private Estado antesDeRever;
    private final List<String> cinta = new ArrayList<>();
    private String ultimaHoraTxt;
    private double ultimaHoraT = -10;

    // sellos
    private Veredicto selloV;
    private double selloT;
    private Jornada.Resultado ultimo;
    /** Cuanto cambio cada indicador con el ultimo sello (retroalimentacion de consecuencias). */
    private final int[] cambioIndicadores = new int[6];
    private String llamadaInst;
    private List<String> marcadorRed = new ArrayList<>();
    private List<String> faltanRed = new ArrayList<>();

    // arrastre de preguntas
    private NodoDialogo arrastraPregunta;
    private int pressX, pressY, dragX, dragY;
    private final List<Object[]> zonasPreguntas = new ArrayList<>();
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
        avisos.haciaArriba = false;
        avisos.maxVisibles = 3;
        avisos.subtituloY = 67;
        Sonido.setSubtitulos(avisos);
    }

    // ==================================================================
    // Acceso para el celular y los mini-juegos
    // ==================================================================

    Jornada jornada() {
        return j;
    }

    Caso caso() {
        return caso;
    }

    boolean atendiendo() {
        return estado == Estado.ATENDIENDO && caso != null;
    }

    boolean sellando() {
        return estado == Estado.SELLANDO;
    }

    boolean hablando() {
        return letras < texto.length();
    }

    long semilla() {
        return partida == null ? 1 : partida.semilla;
    }

    Herramienta escaneando() {
        return escaneando;
    }

    double escaneoT() {
        return escaneoT;
    }

    String llamadaInst() {
        return llamadaInst;
    }

    VistaRed3D red3d() {
        return red3d;
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
        cel.abrir(CelularVista.App.HABILIDAD);
    }

    void anotarViral(boolean exito, int puntos) {
        ultimaHora(exito ? "Una alerta viral fue frenada a tiempo en Civitas." : "Un rumor viral sigue circulando por Civitas.");
        avisos.agregar("Alerta viral", (puntos >= 0 ? "+" : "") + puntos + " puntos", exito ? Tema.bien() : Tema.mal(), 0);
    }

    /** Titular que llega de otro jugador por la red (sockets). */
    public void noticiaDeRed(String quien, String titular) {
        if (j == null) {
            return;
        }
        String txt = "En la ventanilla de " + quien + ": " + titular;
        ultimaHora(txt);
    }

    /**
     * RELACIONES sincronizadas por el servidor: otro jugador elimino una cuenta
     * falsa; se elimina tambien de MI grafo social (con sus aristas).
     */
    public void relacionDeRed(String quien, String idCuenta, String cuenta) {
        if (j == null) {
            return;
        }
        CiudadEstado c = j.jugador.ciudad;
        if (c.red.contiene(idCuenta)) {
            c.red.eliminarVertice(idCuenta);
        }
        ultimaHora(quien + " reportó " + cuenta + ": la cuenta falsa sale de Civitas para todos.");
    }

    /** TURNOS en red: termine mi dia y espero a que los demas terminen el suyo. */
    public void esperarTurno(List<String> faltan) {
        estado = Estado.ESPERA;
        faltanRed = faltan;
        cel.cerrar();
    }

    public void setFaltan(List<String> faltan) {
        faltanRed = faltan;
    }

    public boolean esperando() {
        return estado == Estado.ESPERA;
    }

    /** Aviso de "ultima hora": franja sobre la ventanilla, cinta del noticiero y app Noticias. */
    private void ultimaHora(String txt) {
        if (txt == null || txt.isEmpty()) {
            return;
        }
        ultimaHoraTxt = txt;
        ultimaHoraT = t;
        cinta.add(0, txt);
        while (cinta.size() > 8) {
            cinta.remove(cinta.size() - 1);
        }
        if (j != null) {
            j.anotarNoticia(txt);
        }
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
        cinta.clear();
        ultimaHoraTxt = null;
        cel.modoLlamada(false);
        cel.inicio();
        cel.cerrar();
        if (partida.jugadores.size() > 1) {
            estado = Estado.TURNO;
        } else {
            empezarNoticias();
        }
    }

    public void detener() {
        activo = false;
    }

    public void setMarcadorRed(List<String> m) {
        marcadorRed = m;
    }

    RolJugador rol() {
        return j == null ? RolJugador.PERIODISTA : j.jugador.rol;
    }

    // ------------------------------------------------------------ noticiero
    private void empezarNoticias() {
        segmentos = Noticiero.manana(j.dia, j.jugador);
        for (Noticiero.Segmento s : segmentos) {
            if (s.tipo.equals("giro")) {
                j.anotarNoticia(s.texto);
                cinta.add(0, s.texto);
            } else if (s.tipo.equals("vivo")) {
                cinta.add(s.texto);
            }
        }
        cinta.add("Faltan " + (Historia.DIAS - j.dia) + " días para elegir alcalde en Ciudad Nova");
        segIdx = 0;
        segLetras = 0;
        segInicioT = t;
        noticiasNoche = false;
        reviendo = false;
        estado = Estado.NOTICIAS;
        Sonido.tocar(Sonido.Tipo.TURNO);
    }

    /** Desde la app Noticias: vuelve a ver la edicion de la manana (el reloj no corre). */
    void reverNoticiero() {
        if (estado == Estado.NOTICIAS || estado == Estado.SELLANDO || juego != Juego.NADA) {
            return;
        }
        antesDeRever = estado;
        segmentos = Noticiero.manana(j.dia, j.jugador);
        segIdx = 0;
        segLetras = 0;
        segInicioT = t;
        noticiasNoche = false;
        reviendo = true;
        estado = Estado.NOTICIAS;
        cel.cerrar();
    }

    private void avanzarNoticia() {
        if (segIdx >= segmentos.size()) {
            terminarNoticias();
            return;
        }
        if (segLetras < segmentos.get(segIdx).texto.length()) {
            segLetras = segmentos.get(segIdx).texto.length();
            return;
        }
        segIdx++;
        segLetras = 0;
        segInicioT = t;
        if (segIdx >= segmentos.size()) {
            terminarNoticias();
        } else {
            Sonido.tocar(Sonido.Tipo.CLIC);
        }
    }

    private void terminarNoticias() {
        if (reviendo) {
            reviendo = false;
            estado = antesDeRever;
            return;
        }
        if (noticiasNoche) {
            estado = Estado.FIN_DIA;
        } else {
            empezarLlamada();
        }
    }

    /** Cierra la jornada: noticiero de la noche y despues el resumen del dia. */
    private void terminarDia() {
        j.cerrar();
        caso = null;
        juego = Juego.NADA;
        mini = null;
        cel.modoLlamada(false);
        cel.cerrar();
        segmentos = Noticiero.noche(j);
        segIdx = 0;
        segLetras = 0;
        segInicioT = t;
        noticiasNoche = true;
        reviendo = false;
        cinta.clear();
        for (Jornada.Resultado r : j.getResultados()) {
            cinta.add(r.titular);
        }
        estado = Estado.NOTICIAS;
        Sonido.tocar(Sonido.Tipo.ALERTA);
    }

    // ------------------------------------------------------------ llamada del jefe (en el celular)
    private void empezarLlamada() {
        estado = Estado.LLAMADA;
        llamada = Historia.LLAMADAS[j.dia];
        lineaLlamada = 0;
        cel.modoLlamada(true);
        decir("Don Ramiro · por teléfono", llamada[0], "neutral");
        Sonido.tocar(Sonido.Tipo.TELEFONO);
    }

    private void avanzarLlamada() {
        if (letras < texto.length()) {
            letras = texto.length();
            return;
        }
        lineaLlamada++;
        if (lineaLlamada < llamada.length) {
            decir("Don Ramiro · por teléfono", llamada[lineaLlamada], "neutral");
        } else {
            colgar();
        }
    }

    /** Termina la llamada de la manana (boton rojo del celular o la ultima linea). */
    void colgar() {
        if (estado != Estado.LLAMADA) {
            return;
        }
        cel.modoLlamada(false);
        cel.inicio();
        cel.cerrar();
        estado = Estado.VACIO;
        texto = "";
        for (String c : j.cambiosDeReglas) {
            avisos.agregar("Cambio de reglas", c, Tema.acento2(), 0);
        }
        if (j.dia == 1) {
            avisos.agregar("Cómo se juega", "1) Haz pasar al visitante. 2) Pregúntale (arrastra la tarjeta o 1-6). "
                    + "3) Investiga con las apps del celular (P). 4) Cuando sepas, \"Decidir el sello\" (D).", Tema.acento(), 300);
        }
        for (Herramienta h : Herramienta.values()) {
            if (h.dia == j.dia && j.delRol(h)) {
                avisos.agregar("App nueva en el celular", h.nombre, Tema.bien(), 600);
            }
        }
        Sonido.tocar(Sonido.Tipo.TURNO);
    }

    // ------------------------------------------------------------ visitantes
    private void hacerPasar() {
        if (j.isTerminada()) {
            return;
        }
        caso = j.siguiente();
        if (caso == null) {
            terminarDia();
            return;
        }
        estado = Estado.ENTRANDO;
        entradaT = 0;
        // cliente nuevo: TODAS las herramientas se reinician y quedan disponibles otra vez
        escaneando = null;
        escaneoT = 0;
        if (juego == Juego.LUPA || juego == Juego.BILLETERA) {
            juego = Juego.NADA;
        }
        arrastraPregunta = null;
        llamadaInst = null;
        ultimo = null;
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
            avisos.agregar("Pista", n.pista, Tema.acento2(), 0);
        }
    }

    // ------------------------------------------------------------ sellos (solo cuando hay que decidir)
    /**
     * Abre la pista de sellos ENSEGUIDA (sin escoger antes): el celular muestra
     * la publicacion y al lado de la ventanilla empiezan a caer los circulos.
     */
    private void decidir() {
        if (estado != Estado.ATENDIENDO || caso == null || juego != Juego.NADA) {
            return;
        }
        arrastraPregunta = null;
        cel.abrir(CelularVista.App.CIVITAS);
        mini = new JuegoSellos(this);
        juego = Juego.MINIJUEGO;
    }

    /** Donde va el panel de la pista: pegado al lado derecho de la ventanilla, sin tapar al visitante. */
    Rectangle zonaPista(int ancho, int alto) {
        Rectangle v = vidrio();
        return new Rectangle(v.x + v.width - ancho - 16, v.y + 12, ancho, alto);
    }

    /** La pista salio bien: el sello cae sobre la publicacion en el celular. */
    void selloActivado(Veredicto v) {
        if (estado != Estado.ATENDIENDO || caso == null || v == null) {
            return;
        }
        selloV = v;
        selloT = 0;
        estado = Estado.SELLANDO;
        cel.abrir(CelularVista.App.CIVITAS);
    }

    /** Pisar un icono de otro sello: se pierde una vida al instante (salvo seguro). */
    void vidaPerdidaEnPista(boolean advertencia) {
        if (advertencia) {
            avisos.agregar("ADVERTENCIA " + j.jugador.advertencias + "/" + JugadorVerificador.MAX_ADVERTENCIAS,
                    "Pisaste un círculo de otro sello: perdiste una vida.", Tema.mal(), 0);
        } else {
            avisos.agregar("Seguro de reputación", "Pisaste otro sello, pero el seguro absorbió la advertencia.", Tema.acento2(), 0);
        }
    }

    /** Con la ultima advertencia en la pista, Don Ramiro despide al verificador. */
    void despedidoEnPista() {
        ultimaHora("Influence, Inc. despide a su verificador en plena semana electoral.");
        terminarDia();
    }

    /** Se pasaron 10 iconos sin decidir: no hay castigo, se puede volver a abrir. */
    void pistaSinDecidir() {
        avisos.agregar("Sin sello todavía", "Se pasaron " + PistaSellos.MARGEN + " círculos. Piensa y vuelve con D (o Z X C V).",
                Tema.acento2(), 0);
    }

    /** Los 6 indicadores de Ciudad Nova en un arreglo (para mostrar cuanto cambio cada uno). */
    private static int[] indicadores(CiudadEstado c) {
        return new int[]{c.verificada, c.confianza, c.convivencia, c.bienestar, c.desinformacion(), c.conflictos};
    }

    private void terminarSello() {
        int[] antes = indicadores(j.jugador.ciudad);
        Jornada.Resultado r = j.sellar(selloV);
        if (r == null) {
            estado = Estado.ATENDIENDO;
            return;
        }
        ultimo = r;
        int[] despues = indicadores(j.jugador.ciudad);
        for (int i = 0; i < 6; i++) {
            cambioIndicadores[i] = despues[i] - antes[i];
        }
        Sonido.tocar(Sonido.Tipo.SELLO);
        Sonido.tocar(r.efecto.acierto ? Sonido.Tipo.BIEN : Sonido.Tipo.MAL);
        ultimaHora(r.titular);
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
        if (!r.calmados.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int z : r.calmados) {
                sb.append(sb.length() == 0 ? "" : ", ").append(CiudadEstado.ZONAS[z]);
            }
            avisos.agregar("Red de alerta (Kruskal)", "La calma llegó también a: " + sb + ".", Tema.bien(), 300);
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
        cel.inicio();
        cel.cerrar();
    }

    // ------------------------------------------------------------ apps de investigacion
    void usarHerramienta(Herramienta h) {
        cel.abrir(CelularVista.App.de(h));
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

    void abrirLupa() {
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

    void abrirTienda() {
        cel.abrir(CelularVista.App.TIENDA);
        arrastraPregunta = null;
    }

    void usarHabilidad() {
        if (!rol().tieneMiniJuego()) {
            cel.abrir(CelularVista.App.HABILIDAD);
            return;
        }
        if (caso == null || estado != Estado.ATENDIENDO) {
            cel.abrir(CelularVista.App.HABILIDAD);
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
        cel.cerrar();
        juego = Juego.MINIJUEGO;
        Sonido.tocar(Sonido.Tipo.TURNO);
    }

    private void cerrarMiniJuego() {
        MiniJuego m = mini;
        mini = null;
        juego = Juego.NADA;
        if (m != null) {
            m.cerrar();
        }
    }

    /** Tecla K o boton "Llamar a la institucion": llama a la institucion del caso (si hay). */
    void llamar() {
        if (j == null) {
            return;
        }
        String inst = j.institucionDelCaso();
        llamarContacto(inst != null ? inst : "Institución");
    }

    /**
     * Llamada a una persona de la agenda del celular. Si sirve para el caso es
     * la llamada de verificacion; si no aplica, cuesta 5 de VIDA por hacerle
     * perder el tiempo a la gente.
     */
    void llamarContacto(String nombre) {
        if (j == null || estado == Estado.LLAMADA || estado == Estado.NOTICIAS || estado == Estado.SELLANDO) {
            return;
        }
        if (j.enApagon()) {
            alertaRoja("Sin señal: hay un apagón. Espera a que vuelva la luz.");
            return;
        }
        int advAntes = j.jugador.advertencias;
        String[] r = j.llamarA(nombre);
        boolean ok = r[1].equals("1");
        Sonido.tocar(Sonido.Tipo.TELEFONO);
        cel.mostrarLlamada(nombre, r[0], ok);
        if (ok) {
            llamadaInst = r[0];
            avisos.agregar("Llamada", r[0], Tema.acento2(), 0);
            return;
        }
        Sonido.tocar(Sonido.Tipo.MAL);
        avisos.agregar("Llamada que no aplica: −" + JugadorVerificador.COSTO_LLAMADA_MALA + " de vida",
                "Le hiciste perder el tiempo a " + nombre + ". Vida: " + j.jugador.vida + "/" + JugadorVerificador.VIDA_MAX + ".",
                Tema.mal(), 0);
        if (j.jugador.advertencias > advAntes) {
            avisos.agregar("ADVERTENCIA " + j.jugador.advertencias + "/" + JugadorVerificador.MAX_ADVERTENCIAS,
                    "Te quedaste sin vida por tantas llamadas inútiles. Don Ramiro te advierte.", Tema.mal(), 200);
        }
        if (j.jugador.despedido) {
            terminarDia();
        }
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
        if (estado == Estado.NOTICIAS && panel == Panel.NADA) {
            if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_RIGHT) {
                avanzarNoticia();
            } else if (k == KeyEvent.VK_ESCAPE) {
                terminarNoticias();
            }
            return;
        }
        if (juego == Juego.MINIJUEGO && mini != null && panel == Panel.NADA) {
            mini.tecla(e);
            return;
        }
        if (k == KeyEvent.VK_ESCAPE) {
            if (panel != Panel.NADA) {
                panel = Panel.NADA;
            } else if (juego == Juego.LUPA || juego == Juego.BILLETERA) {
                juego = Juego.NADA;
            } else if (cel.abierto() && estado != Estado.LLAMADA) {
                cel.cerrar();
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
        if ((k == KeyEvent.VK_F4 || k == KeyEvent.VK_S) && estado != Estado.LLAMADA) {
            if (cel.abierto() && cel.app() == CelularVista.App.TIENDA) {
                cel.cerrar();
            } else {
                abrirTienda();
            }
            return;
        }
        if (panel == Panel.AYUDA) {
            if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_RIGHT) {
                paginaAyuda = Math.min(paginaAyuda + 1, SECCIONES.length - 1);
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
        if (k == KeyEvent.VK_P || k == KeyEvent.VK_TAB) {
            if (estado != Estado.LLAMADA) {
                cel.alternar();
            }
            return;
        }
        if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER) {
            principal();
            return;
        }
        if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_6) {
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
        // D o cualquiera de las teclas de la pista (Z X C V) abren la pista enseguida
        if (k == KeyEvent.VK_D || PistaSellos.TECLAS.indexOf(Character.toUpperCase(e.getKeyChar())) >= 0) {
            decidir();
        }
    }

    /** Lo que hace Espacio segun el momento. */
    private void principal() {
        switch (estado) {
            case TURNO:
                empezarNoticias();
                break;
            case NOTICIAS:
                avanzarNoticia();
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
    // Mouse: arrastrar preguntas; el celular usa zonas
    // ==================================================================

    private boolean escritorioLibre() {
        return juego == Juego.NADA && panel == Panel.NADA && estado != Estado.NOTICIAS;
    }

    @Override
    protected boolean presionar(int x, int y) {
        if (j == null || y < BARRA || panel != Panel.NADA || estado == Estado.NOTICIAS) {
            return false;
        }
        if (juego == Juego.MINIJUEGO && mini != null) {
            return mini.presionar(x, y);
        }
        if (!escritorioLibre() || cel.contiene(x, y)) {
            return false;
        }
        pressX = x;
        pressY = y;
        dragX = x;
        dragY = y;
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
        if (arrastraPregunta != null) {
            NodoDialogo n = arrastraPregunta;
            arrastraPregunta = null;
            if (clic || ventanaR.contains(x, y)) {
                preguntar(n);
            }
        }
    }

    @Override
    protected void rueda(int clics) {
        cel.rueda(clics);
    }

    @Override
    protected boolean cursorMano() {
        if (!escritorioLibre()) {
            return false;
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
        if (estado == Estado.NOTICIAS && segIdx < segmentos.size()) {
            segLetras = Math.min(segmentos.get(segIdx).texto.length(), segLetras + dt * 42);
        }
        parpadeoT -= dt;
        if (parpadeoT < -0.13) {
            parpadeoT = 2 + azar.nextDouble() * 3;
        }
        glitchT = Math.max(0, glitchT - dt);
        if (caso != null && caso.visitante.impostor && estado == Estado.ATENDIENDO && azar.nextDouble() < dt * 0.12) {
            glitchT = 0.18;     // los impostores "fallan" de vez en cuando...
        }
        cel.actualizar(dt);
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
                if (ev != Jornada.Evento.CAFE) {
                    ultimaHora(Noticiero.ultimaHora(ev, j.jugador.ciudad));
                    if (oyente != null) {
                        oyente.alEvento(j, Noticiero.ultimaHora(ev, j.jugador.ciudad));
                    }
                }
                if (ev == Jornada.Evento.VIRAL) {
                    viralEnEspera = j.tomarViral();
                }
            }
            String rep = j.revisarCorresponsal();
            if (rep != null) {
                j.jugador.telefono.recibir(Telefono.CORRESPONSAL, rep, j.hora(), true);
                cel.abrir(CelularVista.App.MAPA);
                Sonido.tocar(Sonido.Tipo.TELEFONO);
            }
            for (Historia.MensajeProgramado m : j.tomarMensajes()) {
                j.jugador.telefono.recibir(m.de, m.texto, j.hora(), m.de.equals(Telefono.JEFE));
            }
        }
        // la alerta viral aparece apenas el escritorio esta libre (decision con 10 segundos)
        if (viralEnEspera != null && juego == Juego.NADA && panel == Panel.NADA && estado != Estado.SELLANDO
                && estado != Estado.FIN_DIA && estado != Estado.TURNO && estado != Estado.LLAMADA && estado != Estado.NOTICIAS) {
            mini = new MiniJuegos.Viral(this, viralEnEspera);
            viralEnEspera = null;
            juego = Juego.MINIJUEGO;
            arrastraPregunta = null;
            cel.cerrar();
            Sonido.tocar(Sonido.Tipo.ALERTA);
        }
        if (j.isTerminada() && estado != Estado.FIN_DIA && estado != Estado.RESULTADO && estado != Estado.SELLANDO
                && estado != Estado.TURNO && estado != Estado.LLAMADA && estado != Estado.NOTICIAS
                && !(juego == Juego.MINIJUEGO && mini instanceof JuegoSellos)) {
            if (caso != null && estado != Estado.SALIENDO) {
                avisos.agregar("18:00", "¡Cerramos! Lo que quedó en la fila circula sin verificar.", Tema.mal(), 0);
            }
            terminarDia();
        }
        switch (estado) {
            case ENTRANDO:
                entradaT += dt * 1.6;
                if (entradaT >= 1) {
                    entradaT = 1;
                    estado = Estado.ATENDIENDO;
                    // situacion en la que se usa el celular: llego una publicacion para revisar
                    j.jugador.telefono.notificar("Civitas", caso.visitante.nombre + " quiere publicar algo", caso.peticion, true);
                    cel.abrir(CelularVista.App.CIVITAS);
                }
                break;
            case SALIENDO:
                entradaT += dt * 1.8;
                if (entradaT >= 1) {
                    caso = null;
                    texto = "";
                    if (j.isTerminada() || j.enFila() == 0) {
                        terminarDia();
                    } else {
                        estado = Estado.VACIO;
                        hacerPasar();
                    }
                }
                break;
            case SELLANDO:
                selloT += dt * 1.8;
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

    /**
     * Fondo vivo: la textura de la oficina, una luz que cambia con la hora,
     * un haz de sol que barre despacio y motas de luz que suben. Todo en los
     * colores de la paleta; con alto contraste queda negro y quieto.
     */
    @Override
    protected void fondoVentana(Graphics2D g, int w, int h) {
        g.setColor(Tema.altoContraste ? Color.BLACK : Tema.ROSA);
        g.fillRect(0, 0, w, h);
        if (Tema.altoContraste) {
            return;
        }
        Recursos.cubrir(g, "fondo2.jpg", 0, 0, w, h);
        double dia = j == null ? 0.2 : Math.max(0, Math.min(1, j.avanceDia()));
        // 1. luz de la hora: manana durazno, tarde cobre, noche ciruela
        Color luz = dia < 0.5 ? Tema.mezclar(Tema.DORADO, Tema.COBRE, dia * 2)
                : Tema.mezclar(Tema.COBRE, Tema.LILA, (dia - 0.5) * 2);
        double ola = 0.5 + 0.5 * Math.sin(t * 0.15);
        g.setPaint(new GradientPaint(0, 0, Tema.alfa(luz, (int) (70 + 30 * ola)), w, h,
                Tema.alfa(Tema.CAOBA, (int) (40 + 40 * dia))));
        g.fillRect(0, 0, w, h);
        // 2. haz de sol diagonal que barre la pared
        double bx = (t * 22) % (w + 900) - 450;
        Path2D haz = new Path2D.Double();
        haz.moveTo(bx, 0);
        haz.lineTo(bx + 260, 0);
        haz.lineTo(bx + 560, h);
        haz.lineTo(bx + 300, h);
        haz.closePath();
        g.setPaint(new GradientPaint((float) bx, 0, Tema.alfa(Tema.CREMA, 34), (float) (bx + 400), h, Tema.alfa(Tema.CREMA, 0)));
        g.fill(haz);
        // 3. motas de luz que suben despacio (siempre las mismas: semilla fija)
        Color[] cs = {Tema.DORADO, Tema.ROSA_CLARO, Tema.LILA_CLARO, Tema.COBRE};
        Random rr = new Random(12);
        for (int i = 0; i < 16; i++) {
            double r = 30 + rr.nextInt(90);
            double x = rr.nextDouble() * w + Math.sin(t * 0.3 + i) * 30;
            double y = h + r - ((t * (8 + rr.nextInt(14)) + rr.nextDouble() * (h + 2 * r)) % (h + 2 * r));
            int a = 30 + rr.nextInt(40);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) r, new float[]{0f, 1f},
                    new Color[]{Tema.alfa(cs[i % cs.length], a), Tema.alfa(cs[i % cs.length], 0)}));
            g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        }
        // 4. vineta suave en los bordes
        g.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0, h / 2.0), (float) (Math.max(w, h) * 0.75),
                new float[]{0.55f, 1f}, new Color[]{new Color(42, 23, 18, 0), new Color(42, 23, 18, 120)}));
        g.fillRect(0, 0, w, h);
    }

    /** La escena se corre a la izquierda mientras el celular esta arriba. */
    private void calcularEscena() {
        double k = cel.visible();
        int x = (int) Math.round(X_CENTRADA + (X_CORRIDA - X_CENTRADA) * k);
        ventanaR = new Rectangle(x, 76, ESCENA_W, 564);
        dialogoR = new Rectangle(x, 652, ESCENA_W, 194);
    }

    @Override
    protected void pintar(Graphics2D g, double dt) {
        actualizar(dt);
        zonasPreguntas.clear();
        if (j == null) {
            return;
        }
        calcularEscena();
        ventana(g);
        dialogo(g);
        if (estado != Estado.NOTICIAS) {
            cel.dibujar(g);
        }
        if (estado == Estado.SELLANDO) {
            animacionSello(g);
        }
        if (estado == Estado.TURNO) {
            tarjetaTurno(g);
        }
        if (estado == Estado.ESPERA) {
            tarjetaEspera(g);
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
        avisos.subtituloY = 67;
        if (estado == Estado.NOTICIAS) {
            noticiero(g);
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
            default:
                break;
        }
        barra(g);
        alertaFlotante(g);
        boolean conPista = juego == Juego.MINIJUEGO && mini instanceof JuegoSellos;
        if ((juego == Juego.NADA || conPista) && panel == Panel.NADA && estado != Estado.FIN_DIA && estado != Estado.TURNO
                && estado != Estado.NOTICIAS) {
            int ay = vidrio().y + (t - ultimaHoraT < 7 ? 58 : 16);
            avisos.dibujar(g, ventanaR.x + 16, ay, 380, H, W);
        } else {
            avisos.dibujar(g, -2000, 88, 10, H, W);   // solo los subtitulos
        }
        if (escritorioLibre()) {
            arrastrados(g);
        }
    }

    /** La pregunta que se esta arrastrando va encima de todo. */
    private void arrastrados(Graphics2D g) {
        if (arrastraPregunta != null && (Math.abs(dragX - pressX) > 6 || Math.abs(dragY - pressY) > 6)) {
            Rectangle r = new Rectangle(dragX - 180, dragY - 21, 360, 42);
            Graphics2D s = (Graphics2D) g.create();
            s.rotate(Math.toRadians(-2), dragX, dragY);
            tarjetaPregunta(s, r, 0, arrastraPregunta, true, true);
            s.dispose();
            if (ventanaR.contains(dragX, dragY)) {
                g.setFont(Tema.fuente(Font.BOLD, 13));
                int tw = g.getFontMetrics().stringWidth("¡Suelta para preguntar!") + 24;
                g.setColor(Tema.altoContraste ? Color.BLACK : Tema.TINTA);
                g.fillRoundRect(dragX - tw / 2, dragY + 30, tw, 26, 13, 13);
                g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.CREMA);
                Dibujo.centrado(g, "¡Suelta para preguntar!", dragX, dragY + 48);
            }
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
        // advertencias (las "vidas" del verificador)
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
        // vida (baja con las llamadas que no aplican)
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.texto());
        g.drawString("Vida", 750, 26);
        String vs = String.valueOf(jv.vida);
        g.drawString(vs, 836 - g.getFontMetrics().stringWidth(vs), 26);
        g.setColor(Tema.altoContraste ? Color.DARK_GRAY : new Color(255, 235, 215, 40));
        g.fillRoundRect(750, 35, 86, 12, 12, 12);
        g.setColor(jv.vida > 50 ? Tema.SALVIA : jv.vida > 25 ? Tema.DORADO : Tema.LADRILLO);
        g.fillRoundRect(750, 35, 86 * jv.vida / JugadorVerificador.VIDA_MAX, 12, 12, 12);
        // puntos y dinero
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.texto());
        g.drawString("Puntos", 860, 26);
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(Tema.acento2());
        g.drawString(String.valueOf(jv.puntos), 860, 48);
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(Tema.texto());
        g.drawString("Dinero", 940, 26);
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(Tema.acento2());
        g.drawString("$" + jv.dinero, 940, 48);
        if (!marcadorRed.isEmpty()) {
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(Tema.textoSuave());
            g.drawString(Dibujo.recortar(g.getFontMetrics(), "Red: " + String.join("  ·  ", marcadorRed), 160), 1030, 38);
        }
        // los 4 botones con imagen: siempre visibles, en cualquier rol y pantalla
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

    // ------------------------------------------------------------ la ventanilla
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

    private static double suave(double x) {
        return x * x * (3 - 2 * x);
    }

    /** Edificios de una capa del horizonte (siempre los mismos: semilla fija). */
    private void horizonte(Graphics2D b, int w, int suelo, int semilla, Color color, double ventanas, int alturaMax, double dia) {
        Random rr = new Random(semilla);
        int x = -30;
        while (x < w + 30) {
            int bw = 46 + rr.nextInt(64), bh = 60 + rr.nextInt(alturaMax);
            b.setColor(color);
            b.fillRect(x, suelo - bh, bw, bh);
            if (rr.nextInt(3) == 0) {          // remate en el techo
                b.fillRect(x + bw / 3, suelo - bh - 10, bw / 3, 10);
            }
            if (ventanas > 0) {
                for (int wy = suelo - bh + 12; wy < suelo - 16; wy += 19) {
                    for (int wx = x + 8; wx < x + bw - 10; wx += 15) {
                        if (rr.nextInt(4) == 0) {
                            b.setColor(new Color(255, 214, 150, (int) (ventanas * (dia > 0.6 ? 210 : 70))));
                            b.fillRect(wx, wy, 6, 8);
                        }
                    }
                }
            }
            x += bw + 4 + rr.nextInt(10);
        }
    }

    /** El vidrio de la ventanilla (debajo del letrero). */
    private Rectangle vidrio() {
        return new Rectangle(ventanaR.x, ventanaR.y + CABECERA, ventanaR.width, ventanaR.height - CABECERA);
    }

    /** Numero de turno que se ve en el letrero (sube con cada visitante). */
    private int turnoActual() {
        return j.getResultados().size() + (caso != null ? 1 : 0);
    }

    /**
     * Letrero de la ventanilla: placa de madera con "ATENCION AL PUBLICO" y un
     * turnero de luces que muestra el numero que se esta atendiendo.
     */
    private void cabecera(Graphics2D g) {
        Rectangle r = new Rectangle(ventanaR.x, ventanaR.y, ventanaR.width, CABECERA + 14);
        RoundRectangle2D placa = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 22, 22);
        Dibujo.sombra(g, placa, 5, 4);
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
        } else {
            g.setPaint(new GradientPaint(r.x, r.y, Tema.MADERA_MEDIA, r.x, r.y + r.height, Tema.MADERA));
        }
        g.fill(placa);
        g.setColor(Tema.altoContraste ? Color.WHITE : Tema.alfa(Tema.DORADO, 120));
        g.draw(new RoundRectangle2D.Double(r.x + 4, r.y + 4, r.width - 8, r.height - 8, 18, 18));
        // tornillos de bronce
        for (int i = 0; i < 2; i++) {
            double tx = i == 0 ? r.x + 16 : r.x + r.width - 16;
            g.setColor(Tema.DORADO);
            g.fill(new Ellipse2D.Double(tx - 4, r.y + 19, 8, 8));
            g.setColor(Tema.alfa(Tema.MADERA, 160));
            g.drawLine((int) tx - 2, r.y + 23, (int) tx + 2, r.y + 23);
        }
        g.setFont(Tema.fuente(Font.BOLD, 16));
        g.setColor(Tema.altoContraste ? Color.WHITE : Tema.CREMA);
        g.drawString("ATENCIÓN AL PÚBLICO", r.x + 36, r.y + 29);
        g.setFont(Tema.fuente(Font.PLAIN, 12));
        g.setColor(Tema.textoSuave());
        g.drawString("Influence, Inc.  ·  Mesa de verificación  ·  Horario 9:00 a 18:00", r.x + 236, r.y + 29);
        // turnero
        Rectangle led = new Rectangle(r.x + r.width - 214, r.y + 9, 184, 30);
        g.setColor(new Color(24, 12, 12));
        g.fillRoundRect(led.x, led.y, led.width, led.height, 10, 10);
        g.setColor(Tema.alfa(Tema.DORADO, 90));
        g.drawRoundRect(led.x, led.y, led.width, led.height, 10, 10);
        boolean nuevo = estado == Estado.ENTRANDO && ((int) (t * 6)) % 2 == 0;
        g.setFont(Tema.fuente(Font.BOLD, 12));
        g.setColor(Tema.alfa(Tema.DORADO, 170));
        g.drawString("TURNO", led.x + 14, led.y + 20);
        g.setFont(Tema.fuente(Font.BOLD, 19));
        g.setColor(nuevo ? Tema.CREMA : Tema.DORADO);
        g.drawString(String.format("A-%03d", turnoActual()), led.x + 70, led.y + 22);
        g.setColor(j.enFila() > 0 ? Tema.SALVIA : Tema.LADRILLO);
        g.fill(new Ellipse2D.Double(led.x + led.width - 20, led.y + 11, 8, 8));
    }

    private void ventana(Graphics2D g) {
        cabecera(g);
        Rectangle r = vidrio();
        if (bufVentana == null || bufVentana.getWidth() != r.width || bufVentana.getHeight() != r.height) {
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
        int suelo = r.height - 112;
        b.setPaint(new GradientPaint(0, 0, cs[0], 0, suelo, cs[1]));
        b.fillRect(0, 0, r.width, r.height);
        // sol que cruza el cielo; de noche, luna y estrellas
        if (!Tema.altoContraste) {
            double sx = r.width * (0.12 + 0.76 * dia), sy = 170 - Math.sin(Math.PI * dia) * 130;
            if (dia < 0.8) {
                b.setPaint(new RadialGradientPaint(new Point2D.Double(sx, sy), 90f, new float[]{0f, 1f},
                        new Color[]{new Color(255, 230, 170, 150), new Color(255, 230, 170, 0)}));
                b.fill(new Ellipse2D.Double(sx - 90, sy - 90, 180, 180));
                b.setColor(new Color(255, 228, 168));
                b.fill(new Ellipse2D.Double(sx - 24, sy - 24, 48, 48));
            }
            double noche = Math.max(0, (dia - 0.7) / 0.3);
            if (noche > 0) {
                Random st = new Random(5);
                for (int i = 0; i < 60; i++) {
                    int a = (int) (noche * (120 + 100 * Math.sin(t * 2 + i)));
                    b.setColor(new Color(255, 245, 230, Math.max(0, Math.min(255, a))));
                    b.fillRect(st.nextInt(r.width), st.nextInt(200), 2, 2);
                }
                b.setColor(new Color(252, 240, 220, (int) (230 * noche)));
                b.fill(new Ellipse2D.Double(r.width - 150, 40, 40, 40));
                b.setColor(Tema.mezclar(cs[0], cs[1], 0.1));
                b.fill(new Ellipse2D.Double(r.width - 140, 33, 40, 40));
            }
            // nubes suaves
            for (int i = 0; i < 4; i++) {
                double nx = (i * 290 + t * 6) % (r.width + 240) - 120, ny = 50 + (i % 2) * 46;
                b.setColor(new Color(255, 250, 244, (int) (70 * (1 - dia * 0.7))));
                b.fill(new Ellipse2D.Double(nx, ny, 120, 28));
                b.fill(new Ellipse2D.Double(nx + 30, ny - 14, 70, 34));
            }
        }
        // Ciudad Nova: dos capas de edificios (la de atras se pierde en el cielo)
        Color lejos = Tema.altoContraste ? new Color(50, 50, 50) : Tema.mezclar(new Color(170, 112, 104), cs[1], 0.45);
        Color cerca = Tema.altoContraste ? new Color(70, 70, 70)
                : Tema.mezclar(new Color(126, 74, 60), new Color(54, 32, 44), dia * 0.85);
        horizonte(b, r.width, suelo, 41, lejos, 0, 200, dia);
        horizonte(b, r.width, suelo, 77, cerca, 1, 150, dia);
        // andén y calle
        b.setColor(Tema.altoContraste ? new Color(40, 40, 40) : Tema.mezclar(new Color(108, 70, 58), new Color(50, 32, 36), dia * 0.7));
        b.fillRect(0, suelo, r.width, r.height - suelo);
        b.setColor(new Color(255, 255, 255, 26));
        b.fillRect(0, suelo, r.width, 3);
        // lluvia el dia 3
        if (j.dia == 3) {
            b.setColor(new Color(240, 220, 230, 110));
            for (int i = 0; i < 110; i++) {
                double x = (i * 37 + t * 220) % r.width, y = (i * 53 + t * 520) % r.height;
                b.drawLine((int) x, (int) y, (int) x - 4, (int) y + 12);
            }
        }
        // la fila de gente esperando (siluetas)
        int n = Math.min(6, j.enFila());
        for (int i = 0; i < n; i++) {
            int x = r.width - 90 - i * 40, y = suelo - 108;
            b.setColor(new Color(42, 23, 18, 120));
            b.fill(new Ellipse2D.Double(x, y + Math.sin(t * 2 + i) * 2, 28, 32));
            b.fillRoundRect(x - 7, y + 30, 42, 90, 20, 20);
        }
        // el visitante
        if (caso != null && estado != Estado.VACIO) {
            double desliz = 0;
            if (estado == Estado.ENTRANDO) {
                desliz = (1 - suave(entradaT)) * (r.width / 2.0 + 160);
            } else if (estado == Estado.SALIENDO) {
                desliz = -suave(entradaT) * (r.width / 2.0 + 200);
            }
            double habla = letras < texto.length() && estado != Estado.LLAMADA ? Math.abs(Math.sin(t * 14)) : 0;
            double bob = Math.sin(t * 1.7) * 3;
            b.setPaint(new RadialGradientPaint(new Point2D.Double(r.width / 2.0 + desliz, 230), 260f, new float[]{0f, 1f},
                    new Color[]{new Color(255, 240, 220, 60), new Color(255, 240, 220, 0)}));
            b.fill(new Ellipse2D.Double(r.width / 2.0 + desliz - 260, -30, 520, 520));
            Caras.dibujar(b, caso.visitante.cara, r.width / 2.0 + desliz, 214 + bob, 1.72, expr, parpadeoT < 0, habla);
        }
        // vidrio: reflejo, rejilla para hablar y vinilo de la agencia
        b.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 40), r.width, r.height, new Color(255, 255, 255, 0)));
        b.fillRect(0, 0, r.width, r.height);
        b.setColor(new Color(255, 255, 255, 34));
        b.setStroke(new BasicStroke(14));
        b.drawLine(60, 60, 170, -20);
        b.setStroke(new BasicStroke(6));
        b.drawLine(110, 70, 220, -10);
        b.drawLine(r.width - 140, r.height - 120, r.width - 40, r.height - 220);
        b.setStroke(new BasicStroke(1));
        double gx = r.width / 2.0, gy = r.height - 104;
        b.setColor(new Color(255, 255, 255, 50));
        b.draw(new Ellipse2D.Double(gx - 28, gy - 14, 56, 28));
        for (int i = -2; i <= 2; i++) {
            for (int k = -1; k <= 1; k++) {
                if (Math.abs(i) == 2 && k != 0) {
                    continue;
                }
                b.fill(new Ellipse2D.Double(gx + i * 9 - 2, gy + k * 7 - 2, 4, 4));
            }
        }
        // banda esmerilada del vidrio con el logo en vinilo
        int banda = r.height - 150;
        b.setColor(new Color(255, 250, 244, Tema.altoContraste ? 0 : 46));
        b.fillRect(0, banda, r.width, 18);
        b.setColor(new Color(255, 255, 255, Tema.altoContraste ? 0 : 70));
        b.setFont(Tema.fuente(Font.BOLD, 10.5f));
        for (int x = 30; x < r.width; x += 250) {
            b.drawString("INFLUENCE, INC.", x, banda + 13);
        }
        // aviso pegado en la parte baja del vidrio
        if (!Tema.altoContraste) {
            Graphics2D av = (Graphics2D) b.create();
            av.translate(4, banda - 100);
            av.rotate(0.05, 70, 40);
            av.setColor(new Color(0, 0, 0, 40));
            av.fillRect(24, 22, 104, 62);
            av.setColor(Tema.PAPEL);
            av.fillRect(22, 18, 104, 62);
            av.setColor(Tema.alfa(Tema.DORADO, 160));
            av.fillRect(56, 12, 36, 12);
            av.setFont(Tema.fuente(Font.BOLD, 10));
            av.setColor(Tema.COBRE);
            av.drawString("¿Lo viste en", 32, 40);
            av.drawString("Civitas?", 32, 53);
            av.setFont(Tema.fuente(Font.PLAIN, 9.5f));
            av.setColor(Tema.TINTA);
            av.drawString("Verifica antes", 32, 68);
            av.dispose();
        }
        // mostrador de madera
        int my0 = r.height - 74;
        if (Tema.altoContraste) {
            b.setColor(Color.WHITE);
            b.fillRect(0, my0, r.width, 74);
        } else {
            b.setPaint(new GradientPaint(0, my0, new Color(164, 96, 60), 0, r.height, new Color(96, 52, 32)));
            b.fillRect(0, my0, r.width, 74);
            b.setColor(new Color(255, 220, 180, 70));
            b.fillRect(0, my0, r.width, 4);
            b.setColor(new Color(70, 36, 22, 60));
            Random vet = new Random(3);
            for (int i = 0; i < 14; i++) {
                int y = my0 + 12 + vet.nextInt(56);
                b.drawLine(vet.nextInt(r.width / 2), y, r.width / 2 + vet.nextInt(r.width / 2), y + vet.nextInt(3) - 1);
            }
            // bandeja hundida para pasar documentos por debajo del vidrio
            int cx0 = r.width / 2;
            b.setColor(new Color(40, 20, 14, 170));
            b.fill(new java.awt.geom.Arc2D.Double(cx0 - 130, my0 - 34, 260, 68, 180, 180, java.awt.geom.Arc2D.CHORD));
            b.setPaint(new GradientPaint(0, my0, new Color(30, 14, 10, 200), 0, my0 + 30, new Color(90, 48, 30, 120)));
            b.fill(new java.awt.geom.Arc2D.Double(cx0 - 118, my0 - 26, 236, 54, 180, 180, java.awt.geom.Arc2D.CHORD));
            b.setColor(new Color(255, 220, 180, 60));
            b.draw(new java.awt.geom.Arc2D.Double(cx0 - 130, my0 - 34, 260, 68, 180, 180, java.awt.geom.Arc2D.OPEN));
            // perfil de aluminio donde el vidrio toca el mostrador
            b.setPaint(new GradientPaint(0, my0 - 6, new Color(220, 200, 190), 0, my0, new Color(150, 120, 110)));
            b.fillRect(0, my0 - 6, cx0 - 130, 6);
            b.fillRect(cx0 + 130, my0 - 6, r.width - cx0 - 130, 6);
            // timbre de atencion
            int tx = 120, ty = my0 + 30;
            b.setColor(new Color(40, 20, 14, 90));
            b.fill(new Ellipse2D.Double(tx - 26, ty + 6, 52, 12));
            b.setPaint(new GradientPaint(tx - 20, ty - 18, Tema.mezclar(Tema.DORADO, Color.WHITE, 0.4), tx + 20, ty,
                    Tema.DORADO.darker()));
            b.fill(new java.awt.geom.Arc2D.Double(tx - 20, ty - 18, 40, 36, 0, 180, java.awt.geom.Arc2D.CHORD));
            b.setColor(Tema.MADERA);
            b.fillRoundRect(tx - 24, ty, 48, 9, 6, 6);
            b.setColor(Tema.DORADO.darker());
            b.fillRect(tx - 2, ty - 24, 4, 7);
        }
        b.dispose();

        // marco de madera con sombra
        Dibujo.sombra(g, new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 22, 22), 6, 4);
        Graphics2D v = (Graphics2D) g.create();
        v.clip(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 22, 22));
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
        // letrero colgante: SIGUIENTE / CERRADO
        if (estado == Estado.VACIO || estado == Estado.TURNO || estado == Estado.NOTICIAS || estado == Estado.LLAMADA) {
            boolean sig = estado == Estado.VACIO && !j.isTerminada();
            double balanceo = Math.sin(t * 1.4) * 0.03;
            Graphics2D s = (Graphics2D) v.create();
            s.rotate(balanceo, r.getCenterX(), r.y);
            s.setColor(new Color(60, 34, 24));
            s.setStroke(new BasicStroke(2));
            s.drawLine((int) r.getCenterX() - 60, r.y, (int) r.getCenterX() - 70, r.y + 120);
            s.drawLine((int) r.getCenterX() + 60, r.y, (int) r.getCenterX() + 70, r.y + 120);
            s.setStroke(new BasicStroke(1));
            Rectangle cart = new Rectangle((int) r.getCenterX() - 110, r.y + 112, 220, 70);
            s.setPaint(new GradientPaint(cart.x, cart.y, sig ? new Color(132, 176, 104) : new Color(206, 92, 76), cart.x,
                    cart.y + cart.height, sig ? new Color(90, 130, 70) : new Color(160, 56, 44)));
            s.fillRoundRect(cart.x, cart.y, cart.width, cart.height, 18, 18);
            s.setColor(new Color(255, 255, 255, 90));
            s.drawRoundRect(cart.x + 5, cart.y + 5, cart.width - 10, cart.height - 10, 14, 14);
            s.setFont(Tema.fuente(Font.BOLD, 26));
            s.setColor(Tema.CREMA);
            Dibujo.centrado(s, sig ? "SIGUIENTE" : "CERRADO", cart.getCenterX(), cart.y + 45);
            s.dispose();
        }
        v.dispose();
        g.setColor(Tema.altoContraste ? Color.WHITE : Tema.MADERA);
        g.setStroke(new BasicStroke(12));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 22, 22);
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(206, 186, 176));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(r.x + 6, r.y + 6, r.width - 12, r.height - 12, 16, 16);
        g.setColor(Tema.altoContraste ? Color.WHITE : Tema.alfa(Tema.DORADO, 150));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(r.x + 9, r.y + 9, r.width - 18, r.height - 18, 14, 14);
        g.setStroke(new BasicStroke(1));
        // ULTIMA HORA: franja sobre el vidrio
        if (ultimaHoraTxt != null && t - ultimaHoraT < 7 && estado != Estado.NOTICIAS) {
            double edad = t - ultimaHoraT;
            double a = Math.min(1, edad / 0.3) * Math.min(1, (7 - edad) / 0.5);
            Rectangle f = new Rectangle(r.x + 14, r.y + 14, r.width - 28, 34);
            Composite antes = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, a)));
            g.setColor(new Color(24, 16, 18, 230));
            g.fillRoundRect(f.x, f.y, f.width, f.height, 10, 10);
            g.setColor(new Color(192, 52, 44));
            g.fillRoundRect(f.x, f.y, 132, f.height, 10, 10);
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.setColor(Color.WHITE);
            g.drawString("ÚLTIMA HORA", f.x + 14, f.y + 22);
            Graphics2D c = (Graphics2D) g.create();
            c.clipRect(f.x + 140, f.y, f.width - 150, f.height);
            c.setFont(Tema.fuente(Font.PLAIN, 13.5f));
            int tw = c.getFontMetrics().stringWidth(ultimaHoraTxt);
            double x = tw > f.width - 160 ? f.x + 146 - Math.max(0, (edad - 1.2) * 60) % (tw + 80) : f.x + 146;
            c.setColor(Tema.CREMA);
            c.drawString(ultimaHoraTxt, (float) x, f.y + 22);
            c.dispose();
            g.setComposite(antes);
        }
        // BILLETERA sobre el mostrador: solo si el visitante trae credencial
        if (caso != null && caso.visitante.credencial != null
                && (estado == Estado.ATENDIENDO || estado == Estado.ENTRANDO || estado == Estado.RESULTADO)) {
            Rectangle br = new Rectangle(r.x + r.width - 120, r.y + r.height - 118, 74, 74);
            br.y += (int) (Math.sin(t * 2.4) * 2);
            boolean hv = encima(br) && escritorioLibre();
            if (!Tema.altoContraste) {
                g.setColor(new Color(42, 23, 18, 110));
                g.fill(new Ellipse2D.Double(br.x - 6, br.y - 4, br.width + 12, br.height + 12));
            }
            Dibujo.botonImagen(g, "billetera.png", br, null, hv, t);
            g.setFont(Tema.fuente(Font.BOLD, 11.5f));
            g.setColor(Tema.CREMA);
            Dibujo.centrado(g, "Credencial (I)", br.getCenterX(), br.y + br.height + 15);
            if (escritorioLibre()) {
                zona(br, () -> juego = Juego.BILLETERA);
            }
        }
    }

    // ------------------------------------------------------------ cuadro de dialogo
    /**
     * Cuadro de dialogo compacto: una pestana con el nombre, dos lineas de
     * texto, las preguntas en chips de 2 columnas y una columna de acciones.
     */
    private void dialogo(Graphics2D g) {
        Rectangle r = dialogoR;
        RoundRectangle2D s = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 22, 22);
        Dibujo.sombra(g, s, 5, 4);
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
        } else {
            g.setPaint(new GradientPaint(r.x, r.y, Color.WHITE, r.x, r.y + r.height, Tema.PAPEL));
        }
        g.fill(s);
        // cinta de cobre a la izquierda
        if (!Tema.altoContraste) {
            Graphics2D c = (Graphics2D) g.create();
            c.clip(s);
            c.setColor(Tema.COBRE);
            c.fillRect(r.x, r.y, 6, r.height);
            c.dispose();
        }
        boolean destino = arrastraPregunta != null && ventanaR.contains(dragX, dragY);
        g.setColor(destino ? Tema.acento2() : Tema.altoContraste ? Color.WHITE : new Color(226, 196, 180));
        g.setStroke(new BasicStroke(destino ? 3 : 1.2f));
        g.draw(s);
        g.setStroke(new BasicStroke(1));
        int textoW = 724;
        Rectangle acc = new Rectangle(r.x + r.width - 222, r.y + 16, 200, r.height - 32);
        if (estado == Estado.TURNO || estado == Estado.NOTICIAS || estado == Estado.FIN_DIA || estado == Estado.ESPERA) {
            return;
        }
        if (estado == Estado.RESULTADO && ultimo != null) {
            resultado(g, new Rectangle(r.x + 24, r.y + 18, textoW, r.height - 28));
            separadorAcciones(g, r, acc);
            boton(g, new Rectangle(acc.x, acc.y + acc.height - 50, acc.width, 50), "Despedir  (Espacio)",
                    Dibujo.Estilo.PRINCIPAL, this::despedirVisitante);
            return;
        }
        // quien habla
        String nombre;
        if (estado == Estado.VACIO) {
            nombre = "Ventanilla 3";
        } else if (estado == Estado.LLAMADA || caso == null) {
            nombre = quienHabla;
        } else {
            nombre = quienHabla + "  ·  " + caso.visitante.rol.etiqueta;
        }
        g.setFont(Tema.fuente(Font.BOLD, 12));
        int nw = g.getFontMetrics().stringWidth(nombre) + 26;
        g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.COBRE);
        g.fillRoundRect(r.x + 24, r.y + 14, nw, 24, 12, 12);
        g.setColor(Tema.altoContraste ? Color.BLACK : Tema.CREMA);
        g.drawString(nombre, r.x + 37, r.y + 31);
        // el texto (dos lineas)
        int ty = r.y + 62;
        if (estado == Estado.VACIO) {
            g.setColor(Tema.tintaSuave());
            Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 15), j.isTerminada() ? "Ya cerramos por hoy."
                    : "La ventanilla está libre. " + j.enFila() + (j.enFila() == 1 ? " persona espera" : " personas esperan")
                    + " en la fila (cola FIFO: pasa primero quien llegó primero).", r.x + 26, ty, textoW, 21, r.y + 110);
        } else if (estado == Estado.LLAMADA) {
            if (lineaLlamada > 0) {
                g.setColor(Tema.tintaSuave());
                g.setFont(Tema.fuente(Font.ITALIC, 12.5f));
                g.drawString(Dibujo.recortar(g.getFontMetrics(), llamada[lineaLlamada - 1], textoW), r.x + 26, ty);
                ty += 24;
            }
            g.setColor(Tema.tinta());
            String vis = texto.substring(0, Math.min(texto.length(), (int) letras));
            Dibujo.parrafo(g, Tema.media(15.5f), vis, r.x + 26, ty, textoW, 21, r.y + r.height - 10);
        } else {
            g.setColor(Tema.tinta());
            String vis = texto.substring(0, Math.min(texto.length(), (int) letras));
            Dibujo.parrafo(g, Tema.media(15.5f), vis, r.x + 26, ty, textoW, 21, r.y + 90);
            if (letras >= texto.length() && ((int) (t * 2)) % 2 == 0 && !texto.isEmpty()) {
                Dibujo.triangulo(g, r.x + textoW + 18, r.y + 82, 4.5, false, Tema.acento());
            }
        }
        separadorAcciones(g, r, acc);
        switch (estado) {
            case LLAMADA:
                boton(g, new Rectangle(acc.x, acc.y, acc.width, 50), lineaLlamada < llamada.length - 1 ? "Seguir  (Espacio)"
                        : "Colgar  (Espacio)", Dibujo.Estilo.PRINCIPAL, this::avanzarLlamada);
                pista(g, acc, "Don Ramiro te llama al celular.");
                return;
            case VACIO:
                if (!j.isTerminada()) {
                    boton(g, new Rectangle(acc.x, acc.y, acc.width, 50), "Siguiente  (Espacio)", Dibujo.Estilo.PRINCIPAL,
                            this::hacerPasar);
                }
                boton(g, new Rectangle(acc.x, acc.y + 60, acc.width, 40), cel.abierto() ? "Guardar celular  (P)"
                        : "Ver celular  (P)", Dibujo.Estilo.SECUNDARIO, cel::alternar);
                return;
            case ENTRANDO:
            case ATENDIENDO:
                break;
            default:
                return;
        }
        if (caso == null) {
            return;
        }
        // preguntas: arbol de dialogo (se arrastran hacia el visitante, clic o 1-6)
        int qy = r.y + 98;
        g.setFont(Tema.fuente(Font.BOLD, 9.5f));
        g.setColor(Tema.tintaSuave());
        g.drawString("PREGUNTAS  ·  arrástralas hacia el visitante (o 1-6)", r.x + 26, qy);
        List<NodoDialogo> op = j.opciones();
        int colW = (textoW - 10) / 2;
        for (int i = 0; i < op.size() && i < 6; i++) {
            NodoDialogo n = op.get(i);
            Rectangle br = new Rectangle(r.x + 24 + (i % 2) * (colW + 10), qy + 8 + (i / 2) * 28, colW, 25);
            if (br.y + br.height > r.y + r.height - 4) {
                break;
            }
            boolean arrastrada = n == arrastraPregunta && (Math.abs(dragX - pressX) > 6 || Math.abs(dragY - pressY) > 6);
            if (arrastrada) {
                g.setColor(new Color(200, 160, 140, 50));
                g.fillRoundRect(br.x, br.y, br.width, br.height, 14, 14);
            } else {
                tarjetaPregunta(g, br, i + 1, n, escritorioLibre() && encima(br), false);
            }
            zonasPreguntas.add(new Object[]{br, n});
        }
        if (op.isEmpty()) {
            g.setColor(Tema.tintaSuave());
            g.setFont(Tema.fuente(Font.ITALIC, 13));
            g.drawString("Ya no tiene más que decir. Investiga en el celular o decide el sello.", r.x + 26, qy + 26);
        }
        // acciones
        boolean listo = estado == Estado.ATENDIENDO;
        Rectangle bd = new Rectangle(acc.x, acc.y, acc.width, 52);
        if (listo) {
            boton(g, bd, "Decidir el sello  (D)", Dibujo.Estilo.PRINCIPAL, this::decidir);
        } else {
            Dibujo.boton(g, bd, "Decidir el sello  (D)", Dibujo.Estilo.PRINCIPAL, false, false);
        }
        int bw = (acc.width - 8) / 2;
        boton(g, new Rectangle(acc.x, acc.y + 62, j.jugador.rol.tieneMiniJuego() ? bw : acc.width, 40),
                cel.abierto() ? "Guardar" : "Celular", Dibujo.Estilo.SECUNDARIO, cel::alternar);
        if (j.jugador.rol.tieneMiniJuego()) {
            if (j.puedeUsarHabilidad()) {
                boton(g, new Rectangle(acc.x + bw + 8, acc.y + 62, bw, 40), "Habilidad", Dibujo.Estilo.OSCURO,
                        this::usarHabilidad);
            } else {
                Dibujo.boton(g, new Rectangle(acc.x + bw + 8, acc.y + 62, bw, 40), "Habilidad", Dibujo.Estilo.OSCURO, false, false);
            }
        }
        g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
        g.setColor(Tema.tintaSuave());
        g.drawString("Vida " + j.jugador.vida + "  ·  llamar mal cuesta 5", acc.x + 2, acc.y + 124);
    }

    private void separadorAcciones(Graphics2D g, Rectangle r, Rectangle acc) {
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(214, 178, 158, 160));
        g.fillRect(acc.x - 18, r.y + 22, 1, r.height - 44);
    }

    private void pista(Graphics2D g, Rectangle acc, String txt) {
        g.setColor(Tema.tintaSuave());
        Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 12), txt, acc.x, acc.y + 84, acc.width, 17, acc.y + acc.height);
    }

    /** Resultado del sello, dentro del cuadro de dialogo. */
    private void resultado(Graphics2D g, Rectangle c) {
        Jornada.Resultado r = ultimo;
        boolean ok = r.efecto.acierto;
        Iconos.marca(g, ok ? 2 : 3, c.x + 14, c.y + 12, 10);
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(ok ? Tema.bien().darker() : Tema.mal());
        g.drawString((ok ? "¡CORRECTO!" : "ERROR") + "   " + (r.puntos >= 0 ? "+" : "") + r.puntos + " pts"
                + (r.dinero != 0 ? "   $" + r.dinero : ""), c.x + 36, c.y + 19);
        g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
        g.setColor(Tema.tintaSuave());
        String ruta = "Árbol de consecuencias: " + r.ruta.replace("->", "›");
        g.drawString(Dibujo.recortar(g.getFontMetrics(), ruta, c.width - 40), c.x + 36, c.y + 38);
        Font f = Tema.fuente(Font.PLAIN, 12.5f);
        g.setColor(Tema.tinta());
        int y = c.y + 60;
        List<String> lineas = new ArrayList<>();
        lineas.add("Era: " + r.caso.verdad.etiqueta + ". " + r.efecto.explicacion);
        if (r.caso.pub.getExplicacion() != null) {
            lineas.add(r.caso.pub.getExplicacion());
        }
        if (r.alcance > 0) {
            int oleadas = r.propagacion == null ? 0 : r.propagacion.oleadas.size() - 1;
            lineas.add("BFS: la publicación llegó a " + r.alcance + " cuentas del grafo social en " + oleadas
                    + (oleadas == 1 ? " oleada." : " oleadas."));
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
            if (y > c.y + c.height - 34) {
                break;
            }
            y = Dibujo.parrafo(g, f, l, c.x + 36, y, c.width - 50, 16, c.y + c.height - 30);
            y += 2;
        }
        // como cambio Ciudad Nova con este sello
        String[] nom = {"Verificada", "Confianza", "Convivencia", "Bienestar", "Desinformación", "Conflictos"};
        int x = c.x + 36, cy = c.y + c.height - 4;
        g.setFont(Tema.fuente(Font.BOLD, 11.5f));
        for (int i = 0; i < 6; i++) {
            int d = cambioIndicadores[i];
            if (d == 0) {
                continue;
            }
            boolean bueno = i >= 4 ? d < 0 : d > 0;
            String t = nom[i] + " " + (d > 0 ? "+" : "") + d;
            int w = g.getFontMetrics().stringWidth(t) + 30;
            if (x + w > c.x + c.width) {
                break;
            }
            g.setColor(Tema.alfa(bueno ? Tema.SALVIA : Tema.LADRILLO, 40));
            g.fillRoundRect(x, cy - 16, w, 22, 11, 11);
            Dibujo.triangulo(g, x + 11, cy - 5, 4.5, d > 0, bueno ? Tema.SALVIA.darker() : Tema.LADRILLO);
            g.setColor(bueno ? Tema.SALVIA.darker() : Tema.LADRILLO);
            g.drawString(t, x + 20, cy);
            x += w + 6;
        }
    }

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
        int x = br.x + 8;
        if (num > 0) {
            double d = Math.min(24, br.height - 7);
            g.setColor(conf ? Tema.CREMA : Tema.COBRE);
            g.fill(new Ellipse2D.Double(x, br.y - lift + br.height / 2.0 - d / 2, d, d));
            g.setFont(Tema.fuente(Font.BOLD, (float) Math.min(12, d * 0.6)));
            g.setColor(conf ? Tema.COBRE : Tema.CREMA);
            Dibujo.centrado(g, String.valueOf(num), x + d / 2, br.y - lift + br.height / 2.0 + d * 0.22);
            x += (int) d + 10;
        }
        Color tx = Tema.altoContraste ? Color.WHITE : conf ? Tema.CREMA : Tema.TINTA;
        String q = (conf ? "¡CONFRONTAR!  " : "") + n.pregunta;
        Font f = Dibujo.ajustar(g, q, conf ? Font.BOLD : Font.PLAIN, br.height < 32 ? 12.5f : 14, br.x + br.width - x - 34);
        g.setFont(f);
        g.setColor(tx);
        g.drawString(Dibujo.recortar(g.getFontMetrics(), q, br.x + br.width - x - 34), x, br.y - lift + br.height / 2 + 5);
        // asa de arrastre (6 puntos)
        g.setColor(Tema.alfa(tx, 110));
        for (int k = 0; k < 6; k++) {
            g.fill(new Ellipse2D.Double(br.x + br.width - 24 + (k % 2) * 6, br.y - lift + br.height / 2.0 - 8 + (k / 2) * 6, 3, 3));
        }
    }


    /** Color de cada sello, tomado de la paleta del juego. */
    static Color colorSello(Veredicto v) {
        switch (v) {
            case PUBLICAR: return Tema.SALVIA;
            case OPINION: return Tema.LILA;
            case ETIQUETA: return Tema.DORADO.darker();
            case RECHAZAR: return Tema.COBRE;
            default: return Tema.LADRILLO;
        }
    }

    /** La marca que deja el sello: imagen del sello + anillo con su nombre. */
    static void estampa(Graphics2D g, Veredicto v, double cx, double cy, double escala, double ang, float alfa) {
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

    /** El sello baja y golpea la publicacion dentro del celular. */
    private void animacionSello(Graphics2D g) {
        Rectangle p = cel.zonaPublicacion();
        Rectangle pan = cel.pantalla();
        g = (Graphics2D) g.create();
        g.clip(new RoundRectangle2D.Double(pan.x, pan.y, pan.width, pan.height, 48, 48));
        double k = suave(Math.min(1, selloT));
        double esc0 = 2.4 - 1.35 * k;
        estampa(g, selloV, p.getCenterX(), p.y + 150 - (1 - k) * 180, esc0, -0.22, (float) (0.3 + 0.7 * k));
        if (selloT > 0.85) {
            double f = (selloT - 0.85) / 0.15;
            g.setColor(Tema.alfa(Tema.DORADO, (int) (120 * (1 - f))));
            g.setStroke(new BasicStroke(5));
            g.draw(new Ellipse2D.Double(p.getCenterX() - 130 - f * 40, p.y + 150 - 60 - f * 20, 260 + f * 80, 120 + f * 40));
            g.setStroke(new BasicStroke(1));
        }
        g.dispose();
    }

    // ------------------------------------------------------------ noticiero
    /**
     * El noticiero a toda pantalla: cabecera del canal, la senal (estudio o en
     * vivo), la escaleta del programa a la derecha y la cinta de titulares.
     */
    private void noticiero(Graphics2D g) {
        if (segmentos.isEmpty()) {
            terminarNoticias();
            return;
        }
        // fondo del set: degradado de la paleta con franjas diagonales que se mueven
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
            g.fillRect(0, BARRA, W, H - BARRA);
        } else {
            g.setPaint(new GradientPaint(0, BARRA, new Color(60, 30, 26), W, H, new Color(30, 16, 22)));
            g.fillRect(0, BARRA, W, H - BARRA);
            Graphics2D f = (Graphics2D) g.create();
            f.clipRect(0, BARRA, W, H - BARRA);
            f.setColor(Tema.alfa(Tema.COBRE, 22));
            double off = (t * 30) % 120;
            for (int x = -900; x < W + 200; x += 120) {
                Path2D franja = new Path2D.Double();
                franja.moveTo(x + off, BARRA);
                franja.lineTo(x + off + 46, BARRA);
                franja.lineTo(x + off + 46 + 700, H);
                franja.lineTo(x + off + 700, H);
                franja.closePath();
                f.fill(franja);
            }
            f.dispose();
        }
        int i = Math.min(segIdx, segmentos.size() - 1);
        Noticiero.Segmento s = segmentos.get(i);
        Rectangle tv = new Rectangle((W - 870) / 2, BARRA + 94, 870, 582);
        // luces del set: dos haces suaves que caen sobre la pantalla
        if (!Tema.altoContraste) {
            for (int q = 0; q < 2; q++) {
                double lx = q == 0 ? tv.x - 120 : tv.x + tv.width + 120;
                Path2D haz = new Path2D.Double();
                haz.moveTo(lx - 18, BARRA);
                haz.lineTo(lx + 18, BARRA);
                haz.lineTo(q == 0 ? tv.x + 260 : tv.x + tv.width - 60, tv.y + tv.height);
                haz.lineTo(q == 0 ? tv.x + 60 : tv.x + tv.width - 260, tv.y + tv.height);
                haz.closePath();
                g.setPaint(new GradientPaint((float) lx, BARRA, Tema.alfa(Tema.DORADO, 34), (float) lx, tv.y + tv.height,
                        Tema.alfa(Tema.DORADO, 0)));
                g.fill(haz);
            }
            // monograma grande y difuminado a cada lado
            g.setFont(Tema.fuente(Font.BOLD, 150));
            g.setColor(Tema.alfa(Tema.CREMA, 10));
            Dibujo.centrado(g, "N", tv.x / 2.0, tv.getCenterY() + 50);
            Dibujo.centrado(g, "N", tv.x + tv.width + (W - tv.x - tv.width) / 2.0, tv.getCenterY() + 50);
            // silueta de la ciudad al fondo del set
            Graphics2D h = (Graphics2D) g.create();
            h.translate(0, H - 52 - 150);
            horizonte(h, W, 150, 31, Tema.alfa(Tema.MADERA, 150), noticiasNoche ? 0.5 : 0.15, 120, noticiasNoche ? 0 : 1);
            h.dispose();
        }
        // cabecera centrada: logo del canal entre dos filetes dorados
        double mid = W / 2.0;
        g.setFont(Tema.fuente(Font.BOLD, 24));
        String canal = Noticiero.CANAL.toUpperCase();
        int anchoCanal = g.getFontMetrics().stringWidth(canal);
        int bloque = 58 + 16 + anchoCanal;
        int lx = (int) (mid - bloque / 2.0), ly = BARRA + 14;
        g.setColor(Tema.LADRILLO);
        g.fillRoundRect(lx, ly, 58, 40, 12, 12);
        g.setColor(Tema.alfa(Tema.CREMA, 90));
        g.drawRoundRect(lx + 3, ly + 3, 52, 34, 9, 9);
        g.setFont(Tema.fuente(Font.BOLD, 20));
        g.setColor(Tema.CREMA);
        Dibujo.centrado(g, "NN", lx + 29, ly + 28);
        g.setFont(Tema.fuente(Font.BOLD, 24));
        g.drawString(canal, lx + 74, ly + 29);
        g.setColor(Tema.alfa(Tema.DORADO, 160));
        g.fillRect(tv.x, ly + 20, lx - 24 - tv.x, 1);
        g.fillRect(lx + bloque + 24, ly + 20, tv.x + tv.width - (lx + bloque + 24), 1);
        g.fill(new Ellipse2D.Double(lx - 28, ly + 17, 7, 7));
        g.fill(new Ellipse2D.Double(lx + bloque + 21, ly + 17, 7, 7));
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(Tema.textoSuave());
        Dibujo.centrado(g, (noticiasNoche ? "Edición de la noche" : "Edición de la mañana") + "   ·   Día " + j.dia + " de "
                + Historia.DIAS + "   ·   " + Historia.TITULOS[j.dia], mid, ly + 60);
        // marco de la pantalla: bisel oscuro con canto de cobre y un brillo detras
        if (!Tema.altoContraste) {
            for (int q = 5; q > 0; q--) {
                g.setColor(Tema.alfa(Tema.COBRE, 9));
                g.fillRoundRect(tv.x - 10 - q * 5, tv.y - 10 - q * 5, tv.width + 20 + q * 10, tv.height + 20 + q * 10,
                        34 + q * 6, 34 + q * 6);
            }
        }
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(18, 10, 10));
        g.fillRoundRect(tv.x - 10, tv.y - 10, tv.width + 20, tv.height + 20, 30, 30);
        g.setColor(Tema.alfa(Tema.COBRE, 170));
        g.setStroke(new BasicStroke(1.6f));
        g.drawRoundRect(tv.x - 10, tv.y - 10, tv.width + 20, tv.height + 20, 30, 30);
        g.setStroke(new BasicStroke(1));
        TeleVista.dibujar(g, tv, s, (int) segLetras, t, t - segInicioT, noticiasNoche);
        // pie: saltar, puntos de avance y siguiente
        int py = tv.y + tv.height + 22;
        boton(g, new Rectangle(tv.x - 10, py, 190, 44), "Saltar  (Esc)", Dibujo.Estilo.SECUNDARIO, this::terminarNoticias);
        boolean fin = i >= segmentos.size() - 1 && segLetras >= s.texto.length();
        String sig = fin ? (reviendo ? "Volver a la ventanilla" : noticiasNoche ? "Resumen del día" : "Ir a trabajar")
                + "  (Espacio)" : "Siguiente  (Espacio)";
        boton(g, new Rectangle(tv.x + tv.width + 10 - 270, py, 270, 44), sig, Dibujo.Estilo.PRINCIPAL, this::avanzarNoticia);
        int n = segmentos.size(), paso = 30;
        double px0 = mid - (n - 1) * paso / 2.0;
        for (int k = 0; k < n; k++) {
            double dx = px0 + k * paso, dy = py + 22;
            if (k == i) {
                double prog = s.texto.isEmpty() ? 1 : Math.min(1, segLetras / s.texto.length());
                g.setColor(Tema.alfa(Tema.CREMA, 50));
                g.fillRoundRect((int) dx - 11, (int) dy - 5, 22, 10, 10, 10);
                g.setColor(Tema.DORADO);
                g.fillRoundRect((int) dx - 11, (int) dy - 5, (int) Math.max(10, 22 * prog), 10, 10, 10);
            } else {
                g.setColor(k < i ? Tema.alfa(Tema.DORADO, 200) : Tema.alfa(Tema.CREMA, 70));
                g.fill(new Ellipse2D.Double(dx - 5, dy - 5, 10, 10));
            }
        }
        // cinta de titulares a todo lo ancho
        int ty = H - 52;
        g.setColor(Tema.altoContraste ? Color.BLACK : new Color(24, 14, 14, 240));
        g.fillRect(0, ty, W, 52);
        g.setColor(Tema.LADRILLO);
        g.fillRect(0, ty, 170, 52);
        g.setColor(Tema.DORADO);
        g.fillRect(0, ty, W, 2);
        g.setFont(Tema.fuente(Font.BOLD, 14));
        g.setColor(Tema.CREMA);
        g.drawString(noticiasNoche ? "TITULARES" : "ÚLTIMA HORA", 22, ty + 32);
        List<String> c = cinta.isEmpty() ? new ArrayList<String>() : cinta;
        if (c.isEmpty()) {
            c.add(Noticiero.CANAL + " · Día " + j.dia + " de la semana electoral");
        }
        Graphics2D k = (Graphics2D) g.create();
        k.clipRect(180, ty, W - 190, 52);
        k.setFont(Tema.fuente(Font.PLAIN, 14));
        String linea = String.join("      ●      ", c);
        int lw = k.getFontMetrics().stringWidth(linea) + 160;
        double x = W - ((t * 70) % (lw + W));
        k.setColor(Tema.CREMA);
        k.drawString(linea, (float) x, ty + 32);
        k.dispose();
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
                parrafoAyuda(g, "Apps de investigación del celular: " + hs + ".", c, yy, ancho, yMax);
                break;
            case 2:
                yy = paso(g, c, yy, 1, "Haz pasar al visitante", "Botón \"¡Siguiente!\" o Espacio. La fila es una cola: el primero en llegar pasa primero.", ancho);
                yy = paso(g, c, yy, 2, "Pregúntale", "Las preguntas están en el cuadro de diálogo: arrástralas hacia el visitante (o clic, o teclas 1 a 6). Cada una gasta 3 minutos.", ancho);
                yy = paso(g, c, yy, 3, "Investiga con el celular", "Ábrelo con P. Las herramientas son apps (Perfil, Directorio, Lupa, Imagen, Mapa, Red). En la app Teléfono puedes llamar a cualquier contacto: si no tiene que ver con el caso pierdes 5 de vida. Una contradicción abre la pregunta naranja ¡CONFRONTAR!.", ancho);
                yy = paso(g, c, yy, 4, rr.tieneMiniJuego() ? "Usa tu habilidad" : "Envía al corresponsal", rr.tieneMiniJuego()
                        ? "App \"Habilidad\" del celular o tecla U: " + rr.habilidad + "." : "App Mapa: el corresponsal va al barrio por la ruta más corta (Dijkstra) y te escribe al llegar.", ancho);
                yy = paso(g, c, yy, 5, "Revisa la credencial", "Si el visitante trae carné aparece la billetera sobre el mostrador. Con la lupa señala la diferencia en la foto.", ancho);
                paso(g, c, yy, 6, "Decide el sello", "Botón \"Decidir el sello\" (D). Caen círculos iguales (velocidad 1.0), cada uno con un sello adentro: el primero que aciertas (Z X C V) elige tu sello y con 4 iguales se activa. Pisar otro sello cuesta una vida; dejar pasar no (margen de 10).", ancho);
                break;
            case 3:
                yy = icono(g, c, yy, "ayuda.png", "Ayuda (M o F1)", "Esta guía. Cambia según tu rol.", ancho);
                yy = icono(g, c, yy, "estructura.png", "Estructuras (F2)", "Árboles, grafos, tablas y cola del juego en vivo. El reloj se detiene.", ancho);
                yy = icono(g, c, yy, "mapa.png", "Mapa y ciudad (F3 o G)", "Indicadores, barrios, pánico, corresponsal y la red de alerta.", ancho);
                yy = icono(g, c, yy, "celular.png", "Celular (P o Tab)", "Sube solo con llamadas y avisos importantes. Apps, mensajes, llamadas, noticias y la tienda (S) están dentro del celular.", ancho);
                yy = icono(g, c, yy, "pausa.png", "Pausa (Esc)", "Detiene el reloj. Accesibilidad y salir al menú.", ancho);
                yy = icono(g, c, yy, "billetera.png", "Credencial (I)", "Solo aparece si el visitante trae carné. Una X roja en una app = no aplica.", ancho);
                yy = icono(g, c, yy, "verificarr.png", "Sellos (D o Z X C V)", "Pista de 4 carriles Z X C V: todos los círculos se ven iguales; descubre tu sello por la imagen. 4 aciertos lo activan. Otro sello = −1 vida.", ancho);
                parrafoAyuda(g, "Teclas: Espacio continuar · 1-6 preguntas · Q W E R T Y apps · U habilidad · K llamar a la institución · S tienda · L lupa · X acusar en la lupa. Barra Vida: cada llamada que no aplica resta 5; en 0 recibes una advertencia.",
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
                yy = parrafoAyuda(g, "COLAS (FIFO): la fila de la ventanilla, las notificaciones del celular y las notas de la pista de sellos.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "ÁRBOLES: diálogo n-ario (se INSERTAN confrontaciones), consecuencias (rama #PUBLICIDAD insertada y OPINIÓN eliminada), AVL del directorio y ABB de los contactos (recorridos recursivos e iterativos).", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "TABLAS HASH: mensajes del celular (contacto › chat) y publicaciones de la historia (id › publicación).", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "GRAFO DE LA CIUDAD: Dijkstra (corresponsal), BFS (pánico), Floyd-Warshall (minutos a cada barrio y estudio del noticiero), Kruskal (red de alerta), Prim (cobertura del noticiero) y Ford-Fulkerson (votantes por hora el día de elecciones).", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "GRAFO SOCIAL: BFS al publicar, DFS para anillos de bots, Bellman-Ford para cámaras de eco y eliminar vértice al reportar.", c, yy, ancho, yMax);
                yy = parrafoAyuda(g, "ORDENAMIENTOS: inserción (titulares), shell (tendencias), heap (notas de la pista), quick (ranking) y burbuja (marcador en red).", c, yy, ancho, yMax);
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
                return "Cómo jugarla: tienes todas las apps del celular. El corresponsal, el escáner de red y las llamadas "
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
        Telefono tel = j.jugador.telefono;
        List<String> agenda = tel.getAgenda().inordenIterativo();
        info.add("ABB DE CONTACTOS: " + agenda.size() + " nodos, altura " + tel.getAgenda().altura() + ". Inorden: "
                + agenda.get(0) + ", " + agenda.get(1) + ", " + agenda.get(2) + "...");
        info.add("TABLA HASH DE MENSAJES: " + tel.getChats().tamano() + " chats en " + tel.getChats().cantidadCubetas()
                + " cubetas (carga " + String.format("%.2f", tel.getChats().factorCarga()) + ").");
        info.add("FLOYD-WARSHALL: estudio en " + CiudadEstado.ZONAS[ci.estudio] + " (centro del grafo). KRUSKAL: red de alerta de "
                + GrafoCiudad.pesoTotal(ci.redAlerta) + " m = PRIM " + GrafoCiudad.pesoTotal(ci.coberturaNoticiero) + " m.");
        info.add("FORD-FULKERSON: " + ci.flujoVotantes() + " votantes por hora llegarían hoy a las urnas (de "
                + ci.flujoVotantesIdeal() + " sin pánico).");
        g.setColor(Tema.texto());
        int y = d.y + 54;
        for (String s : info) {
            y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 12), s, d.x + 18, y, d.width - 36, 16, d.y + d.height - 8) + 4;
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
                            case 2: Tema.sonido = !Tema.sonido; Musica.refrescar(); break;
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
        y += 24;
        g.setColor(new Color(150, 200, 120));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{5, 5}, 0));
        g.drawLine(ley.x + 18, y - 5, ley.x + 32, y - 5);
        g.setStroke(new BasicStroke(1));
        g.setColor(Tema.texto());
        g.drawString("Red de alerta (Kruskal)", ley.x + 38, y);
        y += 36;
        g.setColor(Tema.textoSuave());
        String txt;
        if (rol().puedeUsar(Herramienta.CORRESPONSAL)) {
            txt = j.corresponsalEnCamino() ? "El corresponsal va en camino: mira la moto avanzar por la ruta más corta."
                    : "Envía el corresponsal desde la app Mapa del celular (tecla T). Más lejos = más minutos.";
        } else {
            txt = "El corresponsal es del periodista. Aquí ves cómo el pánico salta de barrio en barrio (BFS) cuando "
                    + "se publica un bulo.";
        }
        y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13), txt, ley.x + 18, y, ley.width - 36, 19, ley.y + ley.height - 10);
        y += 10;
        y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13), "Cuando aciertas sobre un barrio, la calma viaja por la red de "
                + "alerta: los barrios conectados a él por ese árbol también se calman.", ley.x + 18, y, ley.width - 36, 19,
                ley.y + ley.height - 10);
        y += 14;
        g.setColor(Tema.texto());
        Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 12.5f), "Vértices = lugares. Aristas = calles con su distancia. "
                + "El reloj sigue corriendo mientras miras el mapa.", ley.x + 18, y, ley.width - 36, 18, ley.y + ley.height - 10);
    }

    /** El grafo de la ciudad (lo usan el panel MAPA y la app Mapa del celular). */
    void mapa(Graphics2D g, Rectangle r) {
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
        // red de alerta temprana: arbol de expansion minima (Kruskal), en linea punteada
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(150, 200, 120, 200));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{7, 7}, 0));
        for (int[] calle : c.redAlerta) {
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

    /** Partida en red: el servidor todavia no da el siguiente turno. */
    private void tarjetaEspera(Graphics2D g) {
        velo(g);
        Rectangle r = new Rectangle(W / 2 - 330, 230, 660, 330);
        Dibujo.tarjetaPapel(g, r, 30);
        double a = t * 3;
        for (int i = 0; i < 8; i++) {
            double ang = a + i * Math.PI / 4;
            g.setColor(Tema.alfa(Tema.COBRE, 40 + i * 25));
            g.fill(new Ellipse2D.Double(r.getCenterX() + Math.cos(ang) * 30 - 6, r.y + 80 + Math.sin(ang) * 30 - 6, 12, 12));
        }
        g.setFont(Tema.fuente(Font.BOLD, 26));
        g.setColor(Tema.tinta());
        Dibujo.centrado(g, "Esperando a los demás verificadores", r.getCenterX(), r.y + 160);
        g.setFont(Tema.fuente(Font.PLAIN, 15));
        g.setColor(Tema.tintaSuave());
        Dibujo.centrado(g, "El servidor reparte los turnos: el día " + (j.dia + 1) + " empieza cuando todos terminen el " + j.dia + ".",
                r.getCenterX(), r.y + 194);
        if (!faltanRed.isEmpty()) {
            g.setFont(Tema.fuente(Font.BOLD, 15));
            g.setColor(Tema.acento());
            Dibujo.centrado(g, "Faltan: " + String.join(", ", faltanRed), r.getCenterX(), r.y + 236);
        }
        if (!marcadorRed.isEmpty()) {
            g.setFont(Tema.fuente(Font.PLAIN, 13));
            g.setColor(Tema.tintaSuave());
            Dibujo.centrado(g, Dibujo.recortar(g.getFontMetrics(), "Marcador: " + String.join("  ·  ", marcadorRed), r.width - 60),
                    r.getCenterX(), r.y + 280);
        }
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
        Rectangle r = new Rectangle(W / 2 - (ancho + 76) / 2, (int) (BARRA + 36 + baja), ancho + 76, 22 + l.size() * 21);
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

    /** Compra desde la app Tienda del celular. */
    void comprar(Tienda.Articulo a) {
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
            j.jugador.telefono.recibir(Telefono.INFORMANTE, Iconos.sinPrefijo(m), j.hora(), true);
        }
    }

    /** Icono dibujado de cada articulo de la tienda. */
    static void iconoArticulo(Graphics2D g, Tienda.Articulo a, double cx, double cy, double r) {
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

    /** Ultimo mensaje de la tienda (para la app Tienda del celular). */
    String mensajeTienda() {
        return tiendaMsg != null && t - tiendaT < 5 ? tiendaMsg : null;
    }



    // ------------------------------------------------------------ utilidades
    private void centrado(Graphics2D g, String s, Rectangle r, Color c, int dy) {
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(c);
        Dibujo.centrado(g, s, r.getCenterX(), r.getCenterY() + dy);
    }

    // ------------------------------------------------------------ para las pruebas
    /** Simula teclas (las pruebas automaticas y la captura de pantallas lo usan). */
    public void simularTecla(int codigo, char c) {
        tecla(new KeyEvent(this, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, codigo, c));
    }

    public Jornada getJornada() {
        return j;
    }

    /** NADA, LUPA, BILLETERA, AYUDA (o MANUAL), ESTRUCTURAS, PAUSA, MAPA, TIENDA, HABILIDAD, VIRAL, SELLOS, CELULAR. */
    public void forzarCapa(String nombre) {
        switch (nombre) {
            case "LUPA": abrirLupa(); break;
            case "BILLETERA": juego = Juego.BILLETERA; break;
            case "MANUAL":
            case "AYUDA": panel = Panel.AYUDA; break;
            case "ESTRUCTURAS": panel = Panel.ESTRUCTURAS; break;
            case "PAUSA": panel = Panel.PAUSA; break;
            case "MAPA": panel = Panel.MAPA; break;
            case "TIENDA": abrirTienda(); break;
            case "HABILIDAD": usarHabilidad(); break;
            case "SELLOS": decidir(); break;
            case "CELULAR": cel.abrir(); break;
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

    /** Para las capturas: pagina de la ayuda y app del celular (0..5 herramientas, 6 habilidad, 7 mensajes, 8 noticias). */
    public void forzarVista(int paginaAyuda, int app) {
        this.paginaAyuda = paginaAyuda;
        if (app >= 0 && app < Herramienta.values().length) {
            cel.abrir(CelularVista.App.de(Herramienta.values()[app]));
        } else if (app == 6) {
            cel.abrir(CelularVista.App.HABILIDAD);
        } else if (app == 7) {
            cel.abrir(CelularVista.App.MENSAJES);
        } else if (app == 8) {
            cel.abrir(CelularVista.App.NOTICIAS);
        } else if (app == 9) {
            cel.abrir(CelularVista.App.TELEFONO);
        }
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
