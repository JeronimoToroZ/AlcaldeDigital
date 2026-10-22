package alcaldedigital.gui;

import alcaldedigital.juego.Burbuja;
import alcaldedigital.juego.Mundo;
import alcaldedigital.juego.Personaje;
import alcaldedigital.juego.Plataforma;
import alcaldedigital.logica.DificultadNivel;
import alcaldedigital.logica.NodoRed;
import alcaldedigital.logica.Partida;
import alcaldedigital.modelo.Accion;
import alcaldedigital.modelo.Publicacion;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * El minijuego de plataformas de cada nivel.
 *
 * Reglas:
 *  - Arriba aparece una publicación de Civitas. El jugador debe deducir qué hacer
 *    con ella y recolectar SOLO las burbujas de esa acción.
 *  - Cada burbuja correcta suma; al juntar las requeridas, la publicación se resuelve
 *    (el árbol de decisión aplica su consecuencia) y sale la siguiente.
 *  - Una burbuja equivocada, caer en un hueco o quedarse sin tiempo quita una vida.
 *  - 3 vidas. 4 publicaciones resueltas = nivel ganado.
 *
 * Esta clase solo dibuja y aplica reglas de juego; las consecuencias en la ciudad,
 * el árbol y los grafos las calcula {@link Partida}.
 */
public class PanelPlataformas extends JPanel {

    /** Quien abrió el nivel recibe el resultado por aquí. */
    public interface AlTerminar {
        void nivelTerminado(ResultadoNivel resultado);
    }

    /** Resumen del nivel jugado. */
    public static class ResultadoNivel {
        public final boolean gano;
        public final int vidas;
        public final int puntos;
        public final int correctas;
        public final int errores;
        public final int publicacionesResueltas;
        public final int lugaresConRumor;
        /** Tiempo que tardó el jugador en la ronda (sin contar pausas ni la cuenta regresiva). */
        public final double segundos;

        ResultadoNivel(boolean gano, int vidas, int puntos, int correctas, int errores,
                       int publicacionesResueltas, int lugaresConRumor, double segundos) {
            this.segundos = segundos;
            this.gano = gano;
            this.vidas = vidas;
            this.puntos = puntos;
            this.correctas = correctas;
            this.errores = errores;
            this.publicacionesResueltas = publicacionesResueltas;
            this.lugaresConRumor = lugaresConRumor;
        }
    }

    private enum Fase { PREPARANDO, JUGANDO, ENTRE_PUBLICACIONES, TERMINADO }

    private static final int FPS_MS = 16;
    private static final int PUNTOS_BURBUJA = 10;
    private static final String FUENTE = "Georgia";

    // ---- referencias ----
    private final Partida partida;
    private final DificultadNivel dificultad;
    private final AlTerminar alTerminar;
    private final Random azar = new Random();
    private final Timer bucle;

    // ---- estado del nivel ----
    private final Mundo mundo;
    private final Personaje jugador;
    private final List<Burbuja> burbujas = new ArrayList<>();
    private final List<Publicacion> publicaciones;
    private int indicePublicacion;
    private Accion accionCorrecta;
    private int correctasPublicacion;
    private int vidas = DificultadNivel.VIDAS;
    private int ticksRestantes;
    private int correctasTotales;
    private int errores;
    private final int puntosAlEmpezar;
    private Fase fase = Fase.PREPARANDO;
    private int ticksFase;
    private long tick;
    private boolean pausado;
    private boolean avisoEnviado;

    // ---- efectos ----
    private int invulnerable;
    private int temblor;
    private int destelloRojo;
    private int ticksReubicacion;
    private int ticksJugados;   // cronómetro de la ronda (no cuenta la cuenta regresiva ni la pausa)
    private final List<double[]> particulas = new ArrayList<>();   // x, y, vx, vy, vida, rgb
    private final List<TextoFlotante> textos = new ArrayList<>();
    private final List<Aviso> avisos = new ArrayList<>();

    // ---- entrada ----
    private boolean izquierda;
    private boolean derecha;
    private boolean saltoSostenido;

    // ---- imágenes ----
    private final BufferedImage fondo;
    private final Map<Accion, BufferedImage> iconos = new EnumMap<>(Accion.class);
    private final SpritePersonaje sprite = new SpritePersonaje();

    public PanelPlataformas(Partida partida, int numeroNivel, AlTerminar alTerminar) {
        this.partida = partida;
        this.dificultad = new DificultadNivel(numeroNivel);
        this.alTerminar = alTerminar;
        this.mundo = new Mundo(dificultad, azar);
        this.jugador = new Personaje(80, Mundo.SUELO_Y - Personaje.ALTO);
        this.publicaciones = partida.iniciarNivel(numeroNivel);
        this.puntosAlEmpezar = partida.getJugador().getPuntos();

        BufferedImage f = BotonImagen.cargar("/alcaldedigital/recursos/ftransparente.png");
        fondo = f != null ? f : BotonImagen.cargar("/alcaldedigital/recursos/fondo_transparente.png");
        for (Accion a : Accion.values()) {
            BufferedImage img = BotonImagen.cargar("/alcaldedigital/recursos/" + a.name().toLowerCase() + ".png");
            if (img != null) {
                iconos.put(a, escalar(BotonImagen.recortarTransparencia(img), 48));
            }
        }

        for (int i = 0; i < dificultad.cantidadBurbujas; i++) {
            burbujas.add(new Burbuja(0, 0, Accion.IGNORAR, azar.nextDouble() * 6));
        }

        setFocusable(true);
        setDoubleBuffered(true);
        instalarTeclas();
        prepararPublicacion();

        bucle = new Timer(FPS_MS, this::alTick);
        bucle.setCoalesce(true);
    }

    public void iniciar() {
        bucle.start();
        requestFocusInWindow();
    }

    public void detener() {
        bucle.stop();
    }

    public void setPausado(boolean p) {
        pausado = p;
        izquierda = derecha = saltoSostenido = false;
        repaint();
    }

    public boolean isPausado() {
        return pausado;
    }

    public boolean estaTerminado() {
        return fase == Fase.TERMINADO;
    }

    public int getVidas() { return vidas; }
    public double getSegundosJugados() { return ticksJugados / 60.0; }
    public int getPuntosRonda() { return partida.getJugador().getPuntos() - puntosAlEmpezar; }
    public int getIndicePublicacion() { return indicePublicacion; }

    // ------------------------------------------------------------------
    // Teclado
    // ------------------------------------------------------------------

    private void instalarTeclas() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        String[][] mov = {{"LEFT", "izq"}, {"A", "izq"}, {"RIGHT", "der"}, {"D", "der"},
                          {"SPACE", "salto"}, {"UP", "salto"}, {"W", "salto"}};
        for (String[] m : mov) {
            im.put(KeyStroke.getKeyStroke("pressed " + m[0]), m[1] + "+");
            im.put(KeyStroke.getKeyStroke("released " + m[0]), m[1] + "-");
        }
        im.put(KeyStroke.getKeyStroke("pressed P"), "pausa");
        im.put(KeyStroke.getKeyStroke("pressed ESCAPE"), "pausa");

        accion("izq+", () -> izquierda = true);
        accion("izq-", () -> izquierda = false);
        accion("der+", () -> derecha = true);
        accion("der-", () -> derecha = false);
        accion("salto+", () -> {
            if (!saltoSostenido && !pausado) {
                jugador.pedirSalto();
            }
            saltoSostenido = true;
        });
        accion("salto-", () -> saltoSostenido = false);
        accion("pausa", () -> {
            if (fase != Fase.TERMINADO) {
                setPausado(!pausado);
            }
        });
    }

    private void accion(String nombre, Runnable r) {
        getActionMap().put(nombre, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }

    // ------------------------------------------------------------------
    // Bucle del juego
    // ------------------------------------------------------------------

    private void alTick(ActionEvent e) {
        if (!pausado) {
            actualizar();
        }
        repaint();
    }

    private void actualizar() {
        tick++;
        ticksFase++;
        if (fase == Fase.JUGANDO || fase == Fase.ENTRE_PUBLICACIONES) {
            ticksJugados++;
        }
        mundo.actualizar(tick);
        actualizarEfectos();

        switch (fase) {
            case PREPARANDO:
                // 3 segundos para leer la publicación antes de empezar
                for (Burbuja b : burbujas) {
                    b.edad++;
                }
                moverJugador();   // sin control: queda quieto en el suelo (pose p5)
                if (ticksFase >= 180) {
                    cambiarFase(Fase.JUGANDO);
                }
                return;
            case ENTRE_PUBLICACIONES:
                moverJugador();
                if (ticksFase == 50 && azar.nextInt(100) < 45) {
                    avisar(partida.eventoAleatorio(), new Color(0x86, 0xC5, 0xF0));
                }
                if (ticksFase >= 120) {
                    indicePublicacion++;
                    prepararPublicacion();
                    cambiarFase(Fase.JUGANDO);
                }
                return;
            case TERMINADO:
                moverJugador();
                if (ticksFase == 100 && !avisoEnviado) {
                    avisoEnviado = true;
                    bucle.stop();
                    boolean gano = vidas > 0;
                    alTerminar.nivelTerminado(new ResultadoNivel(gano, vidas,
                            partida.getJugador().getPuntos() - puntosAlEmpezar, correctasTotales, errores,
                            indicePublicacion + (gano ? 1 : 0), partida.getRed().cantidadInfectados(),
                            ticksJugados / 60.0));
                }
                return;
            case JUGANDO:
            default:
                break;
        }

        moverJugador();
        actualizarBurbujas();

        // tiempo de la publicación
        ticksRestantes--;
        if (ticksRestantes <= 0) {
            Partida.ResultadoDecision r = partida.tiempoAgotado(publicaciones.get(indicePublicacion));
            avisar(("¡Se acabó el tiempo! La publicación siguió circulando. " + textoRumor(r)).trim(), ROJO_AVISO);
            perderVida();
            ticksRestantes = dificultad.segundosPorPublicacion * 60;
        }

        // las burbujas se generan en otro lugar cada X segundos (menos tiempo en niveles altos)
        if (--ticksReubicacion <= 0) {
            reubicarBurbujas();
        }
    }

    private void moverJugador() {
        int dir = (derecha ? 1 : 0) - (izquierda ? 1 : 0);
        boolean controlActivo = fase == Fase.JUGANDO || fase == Fase.ENTRE_PUBLICACIONES;
        if (jugador.actualizar(controlActivo ? dir : 0, saltoSostenido, mundo)) {
            Sonido.salto();
        }
        if (jugador.seCayo()) {
            Sonido.caida();
            jugador.reaparecer(mundo.xSeguroCercaDe(jugador.x - 40));
            if (fase == Fase.JUGANDO) {
                avisar("¡Caíste en un hueco! Pierdes una vida.", ROJO_AVISO);
                perderVida();
            }
        }
    }

    private void cambiarFase(Fase nueva) {
        fase = nueva;
        ticksFase = 0;
    }

    // ------------------------------------------------------------------
    // Publicaciones y burbujas
    // ------------------------------------------------------------------

    private void prepararPublicacion() {
        Publicacion p = publicaciones.get(indicePublicacion);
        accionCorrecta = partida.accionCorrecta(p);   // la decide el árbol de decisión
        correctasPublicacion = 0;
        ticksRestantes = dificultad.segundosPorPublicacion * 60;
        reubicarBurbujas();
    }

    /**
     * Genera de nuevo todas las burbujas en lugares al azar, separadas entre sí
     * (DISTANCIA_MINIMA_BURBUJAS) y lejos del jugador, con símbolos nuevos.
     * Las que ya se recolectaron también vuelven a aparecer.
     */
    private void reubicarBurbujas() {
        ticksReubicacion = (int) Math.round(dificultad.segundosReubicacion * 60);
        List<double[]> lugares = mundo.elegirPosiciones(burbujas.size(), DificultadNivel.DISTANCIA_MINIMA_BURBUJAS,
                jugador.centroX(), jugador.centroY(), azar);
        for (int i = 0; i < burbujas.size(); i++) {
            Burbuja b = burbujas.get(i);
            if (i < lugares.size()) {
                b.moverA(lugares.get(i)[0], lugares.get(i)[1]);
                b.viva = true;
                b.edad = 0;
                b.gracia = 0;
            } else {
                b.viva = false;   // no cupo con la separación mínima
            }
        }
        asignarSimbolos();
    }

    private int faltan() {
        return dificultad.burbujasRequeridas - correctasPublicacion;
    }

    /**
     * Reparte los símbolos entre las burbujas vivas: una parte con la acción
     * correcta (según la dificultad) y el resto con las otras tres. Siempre
     * deja al menos las que faltan + 2 correctas, para que el nivel sea posible.
     */
    private void asignarSimbolos() {
        List<Burbuja> vivas = new ArrayList<>();
        for (Burbuja b : burbujas) {
            if (b.viva) {
                vivas.add(b);
            }
        }
        Collections.shuffle(vivas, azar);
        int correctas = Math.max(faltan() + 2, (int) Math.round(vivas.size() * dificultad.proporcionCorrectas));
        for (int i = 0; i < vivas.size(); i++) {
            vivas.get(i).accion = i < correctas ? accionCorrecta : accionIncorrecta();
        }
    }

    private Accion accionIncorrecta() {
        Accion a;
        do {
            a = Accion.values()[azar.nextInt(Accion.values().length)];
        } while (a == accionCorrecta);
        return a;
    }

    private void actualizarBurbujas() {
        for (Burbuja b : burbujas) {
            if (!b.viva) {
                continue;   // vuelve a aparecer en la siguiente reubicación
            }
            b.edad++;
            if (b.gracia > 0) {
                b.gracia--;
                continue;
            }
            if (b.edad < 15 || !b.tocaA(jugador, tick, dificultad.amplitudMovimiento)) {
                continue;
            }
            if (b.accion == accionCorrecta) {
                recolectarCorrecta(b);
                if (fase != Fase.JUGANDO) {
                    return;
                }
            } else if (invulnerable == 0) {
                recolectarIncorrecta(b);
                if (fase != Fase.JUGANDO) {
                    return;
                }
            }
        }
    }

    private void recolectarCorrecta(Burbuja b) {
        b.viva = false;
        correctasPublicacion++;
        correctasTotales++;
        partida.sumarBurbuja(PUNTOS_BURBUJA);
        Sonido.burbujaCorrecta();
        double bx = b.x(tick, dificultad.amplitudMovimiento);
        double by = b.y(tick);
        explosion(bx, by, 0x8FD69A, 14);
        textos.add(new TextoFlotante(bx, by - 20, "+" + PUNTOS_BURBUJA, new Color(0x2E, 0x9E, 0x4F)));

        if (correctasPublicacion >= dificultad.burbujasRequeridas) {
            Partida.ResultadoDecision r = partida.resolver(publicaciones.get(indicePublicacion), accionCorrecta);
            Sonido.publicacionResuelta();
            explosion(jugador.centroX(), jugador.y, 0xE9C47A, 40);
            textos.add(new TextoFlotante(jugador.centroX(), jugador.y - 30,
                    "¡" + accionCorrecta.getEtiqueta() + "! +" + r.puntos, new Color(0xC9, 0x8E, 0x2B)));
            String rumor = r.limpiados.isEmpty() ? "" : " El rumor retrocede en " + nombres(r.limpiados) + ".";
            avisar(r.efecto.getMensaje() + rumor, VERDE_AVISO);
            for (Burbuja otra : burbujas) {
                if (otra.viva) {
                    otra.viva = false;
                    explosion(otra.x(tick, dificultad.amplitudMovimiento), otra.y(tick), 0xFFFFFF, 3);
                }
            }
            if (indicePublicacion + 1 >= DificultadNivel.PUBLICACIONES_POR_NIVEL) {
                Sonido.nivelGanado();
                avisar("¡Nivel superado! Resolviste las " + DificultadNivel.PUBLICACIONES_POR_NIVEL
                        + " publicaciones.", ORO_AVISO);
                cambiarFase(Fase.TERMINADO);
            } else {
                cambiarFase(Fase.ENTRE_PUBLICACIONES);
            }
        }
    }

    private void recolectarIncorrecta(Burbuja b) {
        b.viva = false;
        errores++;
        Sonido.burbujaIncorrecta();
        double bx = b.x(tick, dificultad.amplitudMovimiento);
        explosion(bx, b.y(tick), 0xF08A80, 18);
        textos.add(new TextoFlotante(bx, b.y(tick) - 20, "-1 vida", new Color(0xD3, 0x3A, 0x2F)));

        Partida.ResultadoDecision r = partida.resolver(publicaciones.get(indicePublicacion), b.accion);
        avisar((b.accion.getEtiqueta() + " no era la decisión. " + r.efecto.getMensaje() + " " + textoRumor(r)).trim(),
                ROJO_AVISO);
        perderVida();
    }

    private void perderVida() {
        vidas--;
        invulnerable = 80;
        temblor = 18;
        destelloRojo = 20;
        if (vidas <= 0) {
            vidas = 0;
            Sonido.nivelPerdido();
            avisar("Te quedaste sin vidas. El rumor ganó esta vez.", ROJO_AVISO);
            cambiarFase(Fase.TERMINADO);
        }
    }

    private static String textoRumor(Partida.ResultadoDecision r) {
        return r.contagiados.isEmpty() ? "" : "El rumor llegó a " + nombres(r.contagiados) + ".";
    }

    private static String nombres(List<NodoRed> lista) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            sb.append(i == 0 ? "" : (i == lista.size() - 1 ? " y " : ", ")).append(lista.get(i).nombre);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Efectos visuales
    // ------------------------------------------------------------------

    private void explosion(double x, double y, int rgb, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            double ang = azar.nextDouble() * Math.PI * 2;
            double vel = 1.5 + azar.nextDouble() * 3.5;
            particulas.add(new double[]{x, y, Math.cos(ang) * vel, Math.sin(ang) * vel - 1.5,
                                        30 + azar.nextInt(20), rgb});
        }
    }

    private void avisar(String texto, Color color) {
        avisos.add(new Aviso(texto, color));
        while (avisos.size() > 2) {
            avisos.remove(0);
        }
    }

    private void actualizarEfectos() {
        if (invulnerable > 0) invulnerable--;
        if (temblor > 0) temblor--;
        if (destelloRojo > 0) destelloRojo--;
        for (Iterator<double[]> it = particulas.iterator(); it.hasNext(); ) {
            double[] p = it.next();
            p[0] += p[2];
            p[1] += p[3];
            p[3] += 0.18;
            if (--p[4] <= 0) {
                it.remove();
            }
        }
        for (Iterator<TextoFlotante> it = textos.iterator(); it.hasNext(); ) {
            TextoFlotante t = it.next();
            t.y -= 0.8;
            if (--t.vida <= 0) {
                it.remove();
            }
        }
        for (Iterator<Aviso> it = avisos.iterator(); it.hasNext(); ) {
            if (--it.next().vida <= 0) {
                it.remove();
            }
        }
    }

    private static final Color VERDE_AVISO = new Color(0x8F, 0xD6, 0x9A);
    private static final Color ROJO_AVISO = new Color(0xF0, 0x8A, 0x80);
    private static final Color ORO_AVISO = new Color(0xE9, 0xC4, 0x7A);

    private static class TextoFlotante {
        double x, y;
        final String texto;
        final Color color;
        int vida = 55;

        TextoFlotante(double x, double y, String texto, Color color) {
            this.x = x;
            this.y = y;
            this.texto = texto;
            this.color = color;
        }
    }

    private static class Aviso {
        final String texto;
        final Color color;
        int vida = 330;

        Aviso(String texto, Color color) {
            this.texto = texto;
            this.color = color;
        }
    }

    // ------------------------------------------------------------------
    // Dibujo
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        boolean contraste = Tema.isAltoContraste();

        int w = getWidth(), h = getHeight();
        double escala = h / (double) Mundo.ALTO;
        double vista = w / escala;
        double camX = Math.max(0, Math.min(mundo.getAncho() - vista, jugador.centroX() - vista * 0.4));
        if (vista >= mundo.getAncho()) {
            camX = 0;
        }

        dibujarFondo(g2, w, h, camX, vista, contraste);

        // ---- mundo ----
        AffineTransform original = g2.getTransform();
        double tx = temblor > 0 ? (azar.nextDouble() - 0.5) * 8 : 0;
        double ty = temblor > 0 ? (azar.nextDouble() - 0.5) * 6 : 0;
        g2.scale(escala, escala);
        g2.translate(-camX + tx, ty);
        for (Plataforma p : mundo.getPlataformas()) {
            if (p.derecha() >= camX - 20 && p.x <= camX + vista + 20) {
                dibujarPlataforma(g2, p, contraste);
            }
        }
        dibujarBurbujas(g2, camX, vista, contraste);
        dibujarPersonaje(g2, contraste);
        dibujarParticulas(g2);
        g2.setTransform(original);

        // ---- interfaz encima ----
        if (destelloRojo > 0) {
            g2.setColor(new Color(220, 40, 40, destelloRojo * 4));
            g2.fillRect(0, 0, w, h);
        }
        dibujarPublicacion(g2, w, contraste);
        dibujarVidas(g2, contraste);
        dibujarCronometro(g2, contraste);
        dibujarProgreso(g2, w, contraste);
        dibujarAvisos(g2, w, h);
        dibujarMensajesCentrales(g2, w, h);
        g2.dispose();
    }

    // ---- Paleta del escenario: sale del fondo (atardecer lila/rosa) y del personaje
    //      (gorro y botas rojas, chaleco azul, mangas amarillas, estrella dorada) ----
    private static final Color CIELO_ARRIBA = new Color(0xF3, 0xEC, 0xF8);
    private static final Color CIELO_ABAJO = new Color(0xFF, 0xF1, 0xE6);
    private static final Color PIEDRA_CLARA = new Color(0xF6, 0xE4, 0xEE);   // borde superior del suelo
    private static final Color PIEDRA_BRILLO = new Color(0xF4, 0xA8, 0x72);  // reflejo naranja del atardecer
    private static final Color PIEDRA_ARRIBA = new Color(0x8E, 0x6C, 0xA8);  // lila de los edificios
    private static final Color PIEDRA_ABAJO = new Color(0x4A, 0x32, 0x66);
    private static final Color MADERA_ARRIBA = new Color(0xC2, 0x55, 0x3E);  // rojo del gorro y las botas
    private static final Color MADERA_ABAJO = new Color(0x7E, 0x2E, 0x2A);
    private static final Color MOVIL_ARRIBA = new Color(0x58, 0x92, 0xC8);   // azul del chaleco
    private static final Color MOVIL_ABAJO = new Color(0x2F, 0x5A, 0x8C);
    private static final Color DORADO = new Color(0xF4, 0xC4, 0x4E);         // estrella del gorro / mangas
    private static final Color SOMBRA_LILA = new Color(0x3A, 0x22, 0x55, 70);

    private void dibujarFondo(Graphics2D g2, int w, int h, double camX, double vista, boolean contraste) {
        g2.setPaint(new GradientPaint(0, 0, CIELO_ARRIBA, 0, h, CIELO_ABAJO));
        g2.fillRect(0, 0, w, h);
        if (fondo != null) {
            // el fondo se recorre más lento que el mundo (efecto de profundidad / parallax)
            int fh = h;
            int fw = Math.max(w, fondo.getWidth() * fh / fondo.getHeight());
            double recorrido = mundo.getAncho() - vista;
            double avance = recorrido <= 0 ? 0 : camX / recorrido;
            int fx = (int) (-(fw - w) * avance);
            g2.drawImage(fondo, fx, 0, fw, fh, null);
        }
        if (contraste) {
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRect(0, 0, w, h);
        }
    }

    private void dibujarPlataforma(Graphics2D g2, Plataforma p, boolean contraste) {
        int x = (int) p.x, y = (int) p.y, ancho = (int) Math.ceil(p.ancho);
        if (p.tipo == Plataforma.Tipo.SUELO) {
            int alto = Mundo.ALTO - y + 5;
            if (contraste) {
                g2.setColor(Color.BLACK);
                g2.fillRect(x, y, ancho, alto);
                g2.setColor(Color.WHITE);
                g2.fillRect(x, y, ancho, 5);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRect(x, y, ancho, alto);
                return;
            }
            // cuerpo de piedra lila (como los edificios del fondo)
            g2.setPaint(new GradientPaint(0, y, PIEDRA_ARRIBA, 0, Mundo.ALTO, PIEDRA_ABAJO));
            g2.fillRect(x, y, ancho, alto);
            // bloques de piedra
            g2.setColor(new Color(0x2E, 0x1B, 0x44, 60));
            g2.setStroke(new BasicStroke(1.5f));
            for (int fila = 0; y + 20 + fila * 26 < Mundo.ALTO; fila++) {
                int fy = y + 20 + fila * 26;
                g2.drawLine(x, fy, x + ancho, fy);
                int desfase = (fila % 2) * 32;
                for (int bx = x + desfase; bx < x + ancho; bx += 64) {
                    g2.drawLine(bx, fy, bx, fy + 26);
                }
            }
            // brillo cálido del atardecer en las juntas de arriba
            g2.setColor(new Color(0xF4, 0xA8, 0x72, 45));
            g2.fillRect(x, y + 16, ancho, 10);
            // borde superior claro con filo naranja
            g2.setColor(PIEDRA_CLARA);
            g2.fillRect(x, y, ancho, 11);
            g2.setColor(PIEDRA_BRILLO);
            g2.fillRect(x, y + 11, ancho, 4);
            g2.setColor(new Color(0x3A, 0x22, 0x55));
            g2.fillRect(x, y, 3, alto);
            g2.fillRect(x + ancho - 3, y, 3, alto);
            return;
        }
        int alto = (int) p.alto;
        boolean movil = p.tipo == Plataforma.Tipo.MOVIL;
        if (contraste) {
            g2.setColor(Color.BLACK);
            g2.fillRoundRect(x, y, ancho, alto, 8, 8);
            g2.setColor(movil ? new Color(255, 255, 0) : Color.WHITE);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(x, y, ancho, alto, 8, 8);
            return;
        }
        // sombra lila suave
        g2.setColor(SOMBRA_LILA);
        g2.fillRoundRect(x + 4, y + 7, ancho, alto, 10, 10);
        // tablón rojo (fijas) o azul (móviles), con borde dorado
        g2.setPaint(new GradientPaint(0, y, movil ? MOVIL_ARRIBA : MADERA_ARRIBA,
                0, y + alto, movil ? MOVIL_ABAJO : MADERA_ABAJO));
        g2.fillRoundRect(x, y, ancho, alto, 10, 10);
        g2.setColor(DORADO);
        g2.fillRoundRect(x, y, ancho, 4, 4, 4);
        g2.setColor(new Color(0x3A, 0x1A, 0x22, 70));
        for (int tx = x + 28; tx < x + ancho - 10; tx += 34) {
            g2.drawLine(tx, y + 5, tx, y + alto - 2);
        }
        // remaches dorados en las puntas
        g2.setColor(DORADO);
        g2.fillOval(x + 5, y + alto / 2, 4, 4);
        g2.fillOval(x + ancho - 9, y + alto / 2, 4, 4);
        if (movil) {
            // flechas que indican que se mueve
            g2.setColor(new Color(0xFF, 0xF1, 0xC8, 220));
            int cy = y + alto / 2 + 2;
            g2.fillPolygon(new int[]{x + 14, x + 21, x + 21}, new int[]{cy, cy - 4, cy + 4}, 3);
            g2.fillPolygon(new int[]{x + ancho - 14, x + ancho - 21, x + ancho - 21}, new int[]{cy, cy - 4, cy + 4}, 3);
        }
    }

    private void dibujarBurbujas(Graphics2D g2, double camX, double vista, boolean contraste) {
        // justo antes de cambiar de lugar, las burbujas se desvanecen (aviso visual)
        boolean avisoCambio = fase == Fase.JUGANDO && ticksReubicacion < 18;
        float alfaAviso = Math.max(0.15f, ticksReubicacion / 18f);
        int amp = dificultad.amplitudMovimiento;
        Font letra = new Font(FUENTE, Font.BOLD, 13);
        for (Burbuja b : burbujas) {
            if (!b.viva) {
                continue;
            }
            double bx = b.x(tick, amp), by = b.y(tick);
            if (bx < camX - 60 || bx > camX + vista + 60) {
                continue;
            }
            double aparicion = Math.min(1.0, b.edad / 15.0);
            double r = Burbuja.RADIO * (0.4 + 0.6 * aparicion);
            float alfa = avisoCambio ? alfaAviso : b.gracia > 0 ? 0.7f : (float) aparicion;
            java.awt.Composite c = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
            // halo
            g2.setColor(new Color(255, 255, 255, 70));
            g2.fill(new java.awt.geom.Ellipse2D.Double(bx - r - 5, by - r - 5, (r + 5) * 2, (r + 5) * 2));
            BufferedImage img = iconos.get(b.accion);
            if (img != null) {
                g2.drawImage(img, (int) (bx - r), (int) (by - r), (int) (r * 2), (int) (r * 2), null);
            }
            if (contraste) {
                // sin depender del color: la inicial de la acción en una insignia
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.5f));
                g2.draw(new java.awt.geom.Ellipse2D.Double(bx - r, by - r, r * 2, r * 2));
                double ix = bx + r * 0.55, iy = by - r * 0.55;
                g2.setColor(Color.BLACK);
                g2.fill(new java.awt.geom.Ellipse2D.Double(ix - 10, iy - 10, 20, 20));
                g2.setColor(new Color(255, 255, 0));
                g2.draw(new java.awt.geom.Ellipse2D.Double(ix - 10, iy - 10, 20, 20));
                g2.setFont(letra);
                String s = b.accion.getEtiqueta().substring(0, 1);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s, (float) (ix - fm.stringWidth(s) / 2.0), (float) (iy + fm.getAscent() / 2.0 - 1));
            }
            g2.setComposite(c);
        }
    }

    private void dibujarPersonaje(Graphics2D g2, boolean contraste) {
        if (invulnerable > 0 && (invulnerable / 5) % 2 == 0) {
            return;   // parpadeo después de perder una vida
        }
        if (sprite.isDisponible()) {
            if (jugador.enSuelo) {
                g2.setColor(SOMBRA_LILA);   // sombra en el suelo
                g2.fill(new java.awt.geom.Ellipse2D.Double(jugador.x - 2, jugador.y + Personaje.ALTO - 4,
                        Personaje.ANCHO + 4, 8));
            }
            sprite.dibujar(g2, jugador, contraste);
            return;
        }
        // si faltan p1/p2/p3, se dibuja un personaje con figuras simples
        double x = jugador.x, y = jugador.y;
        int dir = jugador.mirandoDerecha ? 1 : -1;
        double paso = jugador.enSuelo ? Math.sin(jugador.getDistanciaRecorrida() * 0.22) : 0.8;

        // sombra en el suelo
        if (jugador.enSuelo) {
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fill(new java.awt.geom.Ellipse2D.Double(x - 2, y + Personaje.ALTO - 4, Personaje.ANCHO + 4, 8));
        }
        Color contorno = contraste ? Color.WHITE : new Color(0x2A, 0x1A, 0x12);
        g2.setStroke(new BasicStroke(2f));

        // piernas
        g2.setColor(new Color(0x33, 0x2B, 0x3D));
        double piernaA = paso * 5, piernaB = -paso * 5;
        g2.fill(new java.awt.geom.RoundRectangle2D.Double(x + 7 + piernaA, y + 32, 7, 14, 4, 4));
        g2.fill(new java.awt.geom.RoundRectangle2D.Double(x + 16 + piernaB, y + 32, 7, 14, 4, 4));
        // zapatos
        g2.setColor(new Color(0x5A, 0x34, 0x1C));
        g2.fill(new java.awt.geom.RoundRectangle2D.Double(x + 5 + piernaA + (dir > 0 ? 2 : -2), y + 42, 11, 5, 4, 4));
        g2.fill(new java.awt.geom.RoundRectangle2D.Double(x + 14 + piernaB + (dir > 0 ? 2 : -2), y + 42, 11, 5, 4, 4));

        // cuerpo (chaqueta)
        java.awt.geom.RoundRectangle2D cuerpo = new java.awt.geom.RoundRectangle2D.Double(x + 4, y + 17, 22, 19, 9, 9);
        g2.setPaint(new GradientPaint((float) x, (float) y + 17, new Color(0x3C, 0x7E, 0xC8),
                (float) x, (float) y + 36, new Color(0x24, 0x56, 0x94)));
        g2.fill(cuerpo);
        g2.setColor(contorno);
        g2.draw(cuerpo);
        // insignia dorada
        g2.setColor(DialogoJuego.ORO);
        g2.fill(new java.awt.geom.Ellipse2D.Double(x + 15 + dir * 3 - 3, y + 22, 6, 6));
        // brazo
        g2.setColor(new Color(0x2E, 0x66, 0xA8));
        double brazo = jugador.enSuelo ? -paso * 4 : -6;
        g2.fill(new java.awt.geom.RoundRectangle2D.Double(x + 12 - dir * 2 + brazo * dir, y + 20, 6, 14, 5, 5));

        // cabeza
        java.awt.geom.Ellipse2D cabeza = new java.awt.geom.Ellipse2D.Double(x + 4, y - 2, 22, 22);
        g2.setColor(new Color(0xF2, 0xC9, 0xA0));
        g2.fill(cabeza);
        g2.setColor(contorno);
        g2.draw(cabeza);
        // pelo
        g2.setColor(new Color(0x4A, 0x2C, 0x1E));
        g2.fill(new java.awt.geom.Arc2D.Double(x + 3, y - 4, 24, 18, 0, 180, java.awt.geom.Arc2D.CHORD));
        g2.fill(new java.awt.geom.Ellipse2D.Double(x + 15 - dir * 9 - 3, y + 1, 7, 9));
        // ojo y sonrisa
        g2.setColor(new Color(0x22, 0x18, 0x14));
        g2.fill(new java.awt.geom.Ellipse2D.Double(x + 15 + dir * 5 - 2, y + 8, 4, 5));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new java.awt.geom.Arc2D.Double(x + 13 + dir * 3, y + 10, 8, 6, 200, 140, java.awt.geom.Arc2D.OPEN));
    }

    private void dibujarParticulas(Graphics2D g2) {
        for (double[] p : particulas) {
            int alfa = (int) Math.min(255, p[4] * 8);
            Color c = new Color((int) p[5]);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alfa));
            g2.fill(new java.awt.geom.Ellipse2D.Double(p[0] - 3, p[1] - 3, 6, 6));
        }
        Font f = new Font(FUENTE, Font.BOLD, 18);
        g2.setFont(f);
        for (TextoFlotante t : textos) {
            int alfa = Math.min(255, t.vida * 6);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (int) (t.x - fm.stringWidth(t.texto) / 2.0);
            g2.setColor(new Color(255, 255, 255, alfa));
            g2.drawString(t.texto, tx + 1, (int) t.y + 1);
            g2.setColor(new Color(t.color.getRed(), t.color.getGreen(), t.color.getBlue(), alfa));
            g2.drawString(t.texto, tx, (int) t.y);
        }
    }

    // ---- interfaz (coordenadas de pantalla) ----

    private void dibujarPublicacion(Graphics2D g2, int w, boolean contraste) {
        if (indicePublicacion >= publicaciones.size()) {
            return;
        }
        Publicacion p = publicaciones.get(indicePublicacion);
        int ancho = Math.min(620, w - 400);
        ancho = Math.max(ancho, Math.min(w - 40, 420));
        int x = (w - ancho) / 2, y = 12, alto = 118;

        Color papel = contraste ? Color.BLACK : new Color(255, 253, 248);
        Color tinta = contraste ? Color.WHITE : new Color(0x1E, 0x1A, 0x18);
        Color suave = contraste ? new Color(255, 255, 0) : new Color(0x7A, 0x6E, 0x62);

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(x + 3, y + 5, ancho, alto, 18, 18);
        g2.setColor(papel);
        g2.fillRoundRect(x, y, ancho, alto, 18, 18);
        g2.setColor(contraste ? Color.WHITE : DialogoJuego.ORO);
        g2.setStroke(new BasicStroke(contraste ? 3f : 2f));
        g2.drawRoundRect(x, y, ancho, alto, 18, 18);

        // avatar con la inicial del autor
        String autor = p.getAutor();
        int hash = Math.abs(autor.hashCode());
        Color[] tonos = {new Color(0x7B, 0x5E, 0xA7), new Color(0x2E, 0x86, 0x7A), new Color(0xC2, 0x6B, 0x3A),
                         new Color(0x3C, 0x7E, 0xC8), new Color(0xA8, 0x3E, 0x5A)};
        g2.setColor(tonos[hash % tonos.length]);
        g2.fillOval(x + 14, y + 12, 34, 34);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(FUENTE, Font.BOLD, 17));
        String ini = autor.length() > 1 ? autor.substring(1, 2).toUpperCase() : "?";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(ini, x + 31 - fm.stringWidth(ini) / 2, y + 35);

        g2.setColor(tinta);
        g2.setFont(new Font(FUENTE, Font.BOLD, 14));
        g2.drawString(autor, x + 56, y + 26);
        g2.setColor(suave);
        g2.setFont(new Font(FUENTE, Font.PLAIN, 12));
        g2.drawString("Civitas · hace un momento", x + 56, y + 42);
        String cont = "Publicación " + (indicePublicacion + 1) + " de " + DificultadNivel.PUBLICACIONES_POR_NIVEL;
        g2.setFont(new Font(FUENTE, Font.BOLD, 12));
        fm = g2.getFontMetrics();
        g2.drawString(cont, x + ancho - 16 - fm.stringWidth(cont), y + 26);

        // texto de la publicación (con salto de línea)
        g2.setColor(tinta);
        Font fTexto = new Font(FUENTE, Font.PLAIN, contraste ? 17 : 16);
        List<String> lineas = partirTexto(p.getTexto(), g2.getFontMetrics(fTexto), ancho - 40);
        if (lineas.size() > 2) {
            fTexto = fTexto.deriveFont(13f);
            lineas = partirTexto(p.getTexto(), g2.getFontMetrics(fTexto), ancho - 40);
        }
        g2.setFont(fTexto);
        int ly = y + 66;
        for (String l : lineas) {
            g2.drawString(l, x + 18, ly);
            ly += g2.getFontMetrics().getHeight() - 1;
        }

        // barra de tiempo
        double frac = fase == Fase.PREPARANDO ? 1.0
                : Math.max(0, ticksRestantes / (double) (dificultad.segundosPorPublicacion * 60));
        int bx = x + 18, by = y + alto - 14, bw = ancho - 90;
        g2.setColor(contraste ? new Color(80, 80, 80) : new Color(0xEA, 0xE2, 0xD6));
        g2.fillRoundRect(bx, by, bw, 7, 7, 7);
        Color barra = frac > 0.5 ? new Color(0x4C, 0xAF, 0x6A) : frac > 0.25 ? new Color(0xE0, 0xA2, 0x30)
                : new Color(0xD3, 0x3A, 0x2F);
        if (contraste) {
            barra = frac > 0.25 ? new Color(0, 255, 128) : new Color(255, 128, 0);
        }
        g2.setColor(barra);
        g2.fillRoundRect(bx, by, (int) (bw * frac), 7, 7, 7);
        g2.setColor(frac > 0.25 ? suave : barra);
        g2.setFont(new Font(FUENTE, Font.BOLD, 13));
        int seg = (int) Math.ceil(fase == Fase.PREPARANDO ? dificultad.segundosPorPublicacion : ticksRestantes / 60.0);
        g2.drawString(seg + " s", bx + bw + 12, by + 8);
    }

    private void dibujarVidas(Graphics2D g2, boolean contraste) {
        int x = 18, y = 18;
        panelHud(g2, x - 8, y - 8, 140, 64, contraste);
        for (int i = 0; i < DificultadNivel.VIDAS; i++) {
            boolean llena = i < vidas;
            Path2D c = corazon(x + i * 40, y, 30);
            if (llena) {
                g2.setColor(contraste ? new Color(255, 80, 80) : new Color(0xE0, 0x4B, 0x4B));
                g2.fill(c);
            }
            g2.setColor(contraste ? Color.WHITE : new Color(0x8A, 0x2B, 0x2B));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(c);
        }
        g2.setColor(contraste ? Color.WHITE : DialogoJuego.CREMA);
        g2.setFont(new Font(FUENTE, Font.BOLD, 13));
        g2.drawString("Vidas  ·  Nivel " + dificultad.nivel, x, y + 46);
    }

    /** Cronómetro de la ronda: entre menos tiempo, más puntos al final. */
    private void dibujarCronometro(Graphics2D g2, boolean contraste) {
        int x = 10, y = 82, ancho = 140, alto = 52;
        panelHud(g2, x, y, ancho, alto, contraste);
        // reloj dibujado (sin depender de emojis)
        int cx = x + 22, cy = y + 26;
        g2.setColor(contraste ? Color.WHITE : DialogoJuego.ORO);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(cx - 10, cy - 10, 20, 20);
        g2.drawLine(cx, cy, cx, cy - 6);
        g2.drawLine(cx, cy, cx + 5, cy + 2);
        g2.setFont(new Font(FUENTE, Font.PLAIN, 11));
        g2.setColor(contraste ? Color.WHITE : DialogoJuego.CREMA_SUAVE);
        g2.drawString(partida.getJugador().getNombre(), x + 40, y + 19);
        g2.setFont(new Font(FUENTE, Font.BOLD, 18));
        g2.setColor(contraste ? new Color(255, 255, 0) : DialogoJuego.CREMA);
        g2.drawString(formatoTiempo(ticksJugados / 60.0), x + 40, y + 41);
    }

    /** 83.4 segundos -> "1:23.4" */
    public static String formatoTiempo(double segundos) {
        int min = (int) (segundos / 60);
        double resto = segundos - min * 60;
        return String.format("%d:%04.1f", min, resto);
    }

    private static Path2D corazon(double x, double y, double t) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + t / 2, y + t * 0.95);
        p.curveTo(x - t * 0.1, y + t * 0.5, x, y - t * 0.05, x + t / 2, y + t * 0.28);
        p.curveTo(x + t, y - t * 0.05, x + t * 1.1, y + t * 0.5, x + t / 2, y + t * 0.95);
        p.closePath();
        return p;
    }

    private void dibujarProgreso(Graphics2D g2, int w, boolean contraste) {
        int ancho = 170, x = w - ancho - 10, y = 10;
        panelHud(g2, x, y, ancho, 64, contraste);
        g2.setColor(contraste ? Color.WHITE : DialogoJuego.CREMA);
        g2.setFont(new Font(FUENTE, Font.BOLD, 12));
        g2.drawString("Burbujas correctas", x + 12, y + 19);
        g2.setFont(new Font(FUENTE, Font.BOLD, 20));
        g2.setColor(contraste ? new Color(255, 255, 0) : DialogoJuego.ORO);
        int hechas = Math.min(correctasPublicacion, dificultad.burbujasRequeridas);
        g2.drawString(hechas + " / " + dificultad.burbujasRequeridas, x + 12, y + 43);
        g2.setFont(new Font(FUENTE, Font.PLAIN, 12));
        g2.setColor(contraste ? Color.WHITE : DialogoJuego.CREMA_SUAVE);
        String pts = (partida.getJugador().getPuntos() - puntosAlEmpezar) + " pts";
        g2.drawString(pts, x + ancho - 12 - g2.getFontMetrics().stringWidth(pts), y + 43);
        // puntitos de progreso
        int n = dificultad.burbujasRequeridas;
        double paso = (ancho - 24) / (double) n;
        for (int i = 0; i < n; i++) {
            int cx = (int) (x + 12 + paso * i + paso / 2);
            g2.setColor(i < hechas ? (contraste ? new Color(0, 255, 128) : VERDE_AVISO)
                    : (contraste ? new Color(90, 90, 90) : new Color(255, 255, 255, 60)));
            g2.fillOval(cx - 4, y + 51, 8, 8);
        }
    }

    private static void panelHud(Graphics2D g2, int x, int y, int ancho, int alto, boolean contraste) {
        g2.setColor(contraste ? Color.BLACK : new Color(0x26, 0x15, 0x0E, 200));
        g2.fillRoundRect(x, y, ancho, alto, 14, 14);
        g2.setColor(contraste ? Color.WHITE : new Color(0xE9, 0xC4, 0x7A, 160));
        g2.setStroke(new BasicStroke(contraste ? 2.5f : 1.5f));
        g2.drawRoundRect(x, y, ancho, alto, 14, 14);
    }

    private void dibujarAvisos(Graphics2D g2, int w, int h) {
        int y = h - 26;
        Font f = new Font(FUENTE, Font.PLAIN, Tema.isAltoContraste() ? 16 : 15);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        int maxAncho = Math.min(760, w - 60);
        for (int i = avisos.size() - 1; i >= 0; i--) {
            Aviso a = avisos.get(i);
            List<String> lineas = partirTexto(a.texto, fm, maxAncho - 40);
            int alto = lineas.size() * fm.getHeight() + 18;
            int ancho = 0;
            for (String l : lineas) {
                ancho = Math.max(ancho, fm.stringWidth(l));
            }
            ancho += 40;
            y -= alto;
            int x = (w - ancho) / 2;
            float alfa = Math.min(1f, a.vida / 40f);
            java.awt.Composite c = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
            g2.setColor(new Color(0x26, 0x15, 0x0E, 225));
            g2.fillRoundRect(x, y, ancho, alto, 14, 14);
            g2.setColor(a.color);
            g2.fillRoundRect(x, y, 6, alto, 6, 6);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, ancho, alto, 14, 14);
            g2.setColor(DialogoJuego.CREMA);
            int ly = y + 9 + fm.getAscent();
            for (String l : lineas) {
                g2.drawString(l, x + 22, ly);
                ly += fm.getHeight();
            }
            g2.setComposite(c);
            y -= 8;
        }
    }

    private void dibujarMensajesCentrales(Graphics2D g2, int w, int h) {
        String grande = null, chico = null;
        if (fase == Fase.PREPARANDO) {
            int seg = 3 - ticksFase / 60;
            grande = seg > 0 ? String.valueOf(seg) : "¡Ya!";
            chico = "Lee la publicación: ¿qué burbujas debes recolectar?";
        } else if (fase == Fase.ENTRE_PUBLICACIONES) {
            grande = "¡Publicación resuelta!";
            chico = "Prepárate para la siguiente...";
        } else if (fase == Fase.TERMINADO) {
            grande = vidas > 0 ? "¡Nivel superado!" : "Sin vidas";
        }
        if (pausado) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, w, h);
            grande = "Pausa";
            chico = "Presiona P para continuar";
        }
        if (grande == null) {
            return;
        }
        Font fg = new Font(FUENTE, Font.BOLD | Font.ITALIC, grande.length() <= 4 ? 72 : 46);
        g2.setFont(fg);
        FontMetrics fm = g2.getFontMetrics();
        int gx = (w - fm.stringWidth(grande)) / 2, gy = h / 2 + 10;
        g2.setColor(new Color(0x26, 0x15, 0x0E, 200));
        g2.drawString(grande, gx + 3, gy + 3);
        g2.setColor(DialogoJuego.ORO);
        g2.drawString(grande, gx, gy);
        if (chico != null) {
            g2.setFont(new Font(FUENTE, Font.BOLD, 18));
            fm = g2.getFontMetrics();
            int cx = (w - fm.stringWidth(chico)) / 2;
            g2.setColor(new Color(0x26, 0x15, 0x0E, 220));
            g2.fillRoundRect(cx - 14, gy + 22, fm.stringWidth(chico) + 28, 32, 16, 16);
            g2.setColor(DialogoJuego.CREMA);
            g2.drawString(chico, cx, gy + 44);
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static List<String> partirTexto(String texto, FontMetrics fm, int ancho) {
        List<String> lineas = new ArrayList<>();
        StringBuilder linea = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String prueba = linea.length() == 0 ? palabra : linea + " " + palabra;
            if (fm.stringWidth(prueba) > ancho && linea.length() > 0) {
                lineas.add(linea.toString());
                linea = new StringBuilder(palabra);
            } else {
                linea = new StringBuilder(prueba);
            }
        }
        if (linea.length() > 0) {
            lineas.add(linea.toString());
        }
        return lineas;
    }

    private static BufferedImage escalar(BufferedImage img, int lado) {
        BufferedImage out = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(img.getScaledInstance(lado, lado, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return out;
    }
}
