package influenceinc.gui;

import influenceinc.logica.Caso;
import influenceinc.logica.CiudadEstado;
import influenceinc.logica.Historia;
import influenceinc.logica.Jornada;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Noticiero;
import influenceinc.logica.Telefono;
import influenceinc.logica.Tienda;
import influenceinc.modelo.Cuenta;
import influenceinc.modelo.Herramienta;
import influenceinc.modelo.Publicacion;
import influenceinc.modelo.RolJugador;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * EL CELULAR del verificador, dibujado como un telefono de verdad: marco,
 * isla de la camara, barra de estado, pantalla de inicio con apps, dock y
 * barra de gestos. Vive escondido al lado derecho y solo sube cuando hay
 * una llamada, una notificacion importante o el jugador lo abre (tecla P).
 *
 * Las herramientas de investigacion son APPS: Perfil, Directorio, Lupa,
 * Imagen, Mapa (corresponsal) y Red. Ademas estan Civitas (la publicacion
 * que se revisa), Mensajes, Telefono, Noticias, Habilidad y Tienda.
 */
final class CelularVista {

    /** Las apps del celular. Las que son herramientas apuntan a su Herramienta. */
    enum App {
        CIVITAS("Civitas", null), MENSAJES("Mensajes", null), TELEFONO("Teléfono", null), NOTICIAS("Noticias", null),
        PERFIL("Perfil", Herramienta.PERFIL), DIRECTORIO("Directorio", Herramienta.DIRECTORIO),
        LUPA("Lupa", Herramienta.LUPA), IMAGEN("Imagen", Herramienta.IMAGEN), MAPA("Mapa", Herramienta.CORRESPONSAL),
        RED("Red", Herramienta.RED), HABILIDAD("Habilidad", null), TIENDA("Tienda", null);

        final String nombre;
        final Herramienta herramienta;

        App(String nombre, Herramienta h) {
            this.nombre = nombre;
            this.herramienta = h;
        }

        static App de(Herramienta h) {
            for (App a : values()) {
                if (a.herramienta == h) {
                    return a;
                }
            }
            return CIVITAS;
        }
    }

    // geometria (en el lienzo de 1440 x 860)
    static final int FW = 372, FH = 748, FX = 1052, FY = 96;
    private static final App[] DOCK = {App.CIVITAS, App.MENSAJES, App.TELEFONO, App.NOTICIAS};
    private static final App[] GRILLA = {App.PERFIL, App.DIRECTORIO, App.LUPA, App.IMAGEN, App.MAPA, App.RED,
        App.HABILIDAD, App.TIENDA};
    private static final Color FONDO_APP = new Color(250, 246, 241);

    private final OficinaVista v;
    private double k;                    // 0 escondido .. 1 arriba
    private boolean abierto;
    private App app;                     // null = pantalla de inicio
    private String chat;                 // chat abierto en Mensajes
    private int scroll, altoContenido;
    private Telefono.Notificacion banner;
    private double bannerT = -10;
    private Telefono.Notificacion avisoLado;
    private double avisoLadoT = -10;
    private boolean llamada;
    private double llamadaT;
    // llamada a un contacto de la agenda (pantalla propia, no la de Don Ramiro)
    private String contacto, respuesta;
    private boolean contactoOk;
    private double contactoT;

    CelularVista(OficinaVista v) {
        this.v = v;
    }

    // ==================================================================
    // Estado
    // ==================================================================

    boolean abierto() {
        return abierto;
    }

    /** 0..1 suavizado: cuanto se ve el celular (la ventanilla se corre segun esto). */
    double visible() {
        return k * k * (3 - 2 * k);
    }

    void abrir() {
        abierto = true;
    }

    void abrir(App a) {
        abierto = true;
        if (a != app) {
            app = a;
            scroll = 0;
            chat = null;
        }
    }

    void cerrar() {
        abierto = false;
    }

    void alternar() {
        abierto = !abierto;
        Sonido.tocar(Sonido.Tipo.CLIC);
    }

    void inicio() {
        app = null;
        scroll = 0;
        chat = null;
    }

    App app() {
        return app;
    }

    /** Muestra la pantalla de llamada con un contacto y lo que contesto. */
    void mostrarLlamada(String nombre, String texto, boolean ok) {
        contacto = nombre;
        respuesta = texto;
        contactoOk = ok;
        contactoT = 0;
        abierto = true;
    }

    private void colgarContacto() {
        contacto = null;
        Sonido.tocar(Sonido.Tipo.CLIC);
        abrir(App.TELEFONO);
    }

    void modoLlamada(boolean si) {
        if (si && !llamada) {
            llamadaT = 0;
        }
        llamada = si;
        if (si) {
            abierto = true;
        }
    }

    void actualizar(double dt) {
        double obj = abierto ? 1 : 0;
        k += (obj - k) * Math.min(1, dt * 9);
        if (Math.abs(obj - k) < 0.002) {
            k = obj;
        }
        if (llamada) {
            llamadaT += dt;
        }
        if (contacto != null) {
            contactoT += dt;
        }
        // las notificaciones salen de la cola del telefono una por una
        if (v.jornada() != null && v.t - bannerT > 3.6 && v.t - avisoLadoT > 3.6) {
            Telefono.Notificacion n = v.jornada().jugador.telefono.siguienteNotificacion();
            if (n != null) {
                if (n.importante || abierto) {
                    banner = n;
                    bannerT = v.t;
                    abierto = true;
                } else {
                    avisoLado = n;
                    avisoLadoT = v.t;
                }
                Sonido.tocar(Sonido.Tipo.TURNO);
            }
        }
    }

    Rectangle marco() {
        int y = (int) Math.round(FY + (1 - visible()) * (OficinaVista.H - FY + 30));
        return new Rectangle(FX, y, FW, FH);
    }

    Rectangle pantalla() {
        Rectangle m = marco();
        return new Rectangle(m.x + 11, m.y + 11, m.width - 22, m.height - 22);
    }

    boolean contiene(int x, int y) {
        return visible() > 0.05 && marco().contains(x, y);
    }

    /** Donde va la publicacion de Civitas (para la animacion del sello). */
    Rectangle zonaPublicacion() {
        Rectangle p = pantalla();
        return new Rectangle(p.x + 12, p.y + 106, p.width - 24, 360);
    }

    void rueda(int clics) {
        if (contiene(v.mx, v.my)) {
            int max = Math.max(0, altoContenido - (pantalla().height - 130));
            scroll = Math.max(0, Math.min(max, scroll + clics * 36));
        }
    }

    // ==================================================================
    // Dibujo
    // ==================================================================

    void dibujar(Graphics2D g) {
        Jornada j = v.jornada();
        if (j == null) {
            return;
        }
        if (visible() < 0.02) {
            asa(g, j);
            return;
        }
        Rectangle m = marco();
        // sombra del telefono sobre el escritorio
        if (!Tema.altoContraste) {
            for (int i = 6; i > 0; i--) {
                g.setColor(new Color(20, 8, 4, 10));
                g.fill(new RoundRectangle2D.Double(m.x - i + 4, m.y - i + 10, m.width + 2 * i, m.height + 2 * i, 64 + i, 64 + i));
            }
        }
        // botones laterales
        g.setColor(new Color(58, 52, 58));
        g.fill(new RoundRectangle2D.Double(m.x - 3, m.y + 150, 5, 44, 3, 3));
        g.fill(new RoundRectangle2D.Double(m.x - 3, m.y + 206, 5, 44, 3, 3));
        g.fill(new RoundRectangle2D.Double(m.x + m.width - 2, m.y + 176, 5, 70, 3, 3));
        // cuerpo de grafito con borde metalico
        RoundRectangle2D cuerpo = new RoundRectangle2D.Double(m.x, m.y, m.width, m.height, 62, 62);
        g.setPaint(new GradientPaint(m.x, m.y, new Color(70, 64, 70), m.x + m.width, m.y + m.height, new Color(24, 22, 26)));
        g.fill(cuerpo);
        g.setColor(new Color(150, 140, 146, 120));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(m.x + 1.5, m.y + 1.5, m.width - 3, m.height - 3, 60, 60));
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(10, 9, 11));
        g.fill(new RoundRectangle2D.Double(m.x + 6, m.y + 6, m.width - 12, m.height - 12, 56, 56));
        // pantalla
        Rectangle p = pantalla();
        RoundRectangle2D forma = new RoundRectangle2D.Double(p.x, p.y, p.width, p.height, 48, 48);
        Graphics2D s = (Graphics2D) g.create();
        s.clip(forma);
        boolean oscuro;
        if (llamada) {
            pantallaLlamada(s, p, j);
            oscuro = true;
        } else if (contacto != null) {
            pantallaContacto(s, p);
            oscuro = true;
        } else if (app == null) {
            inicio(s, p, j);
            oscuro = true;
        } else {
            s.setColor(FONDO_APP);
            s.fill(p);
            contenidoApp(s, p, j);
            oscuro = false;
        }
        barraEstado(s, p, j, oscuro);
        bannerNotificacion(s, p);
        // barra de gestos (toca para volver al inicio)
        s.setColor(oscuro ? new Color(255, 255, 255, 200) : new Color(30, 24, 26, 200));
        s.fill(new RoundRectangle2D.Double(p.getCenterX() - 62, p.y + p.height - 13, 124, 5, 5, 5));
        s.dispose();
        if (!llamada && contacto == null) {
            v.zona(new Rectangle(p.x + 60, p.y + p.height - 26, p.width - 120, 26), this::inicio);
        }
        // isla de la camara
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Double(p.getCenterX() - 54, p.y + 11, 108, 30, 30, 30));
        g.setColor(new Color(30, 34, 48));
        g.fill(new Ellipse2D.Double(p.getCenterX() + 28, p.y + 19, 13, 13));
        g.setColor(new Color(70, 90, 140, 150));
        g.fill(new Ellipse2D.Double(p.getCenterX() + 31, p.y + 22, 4, 4));
        // reflejo del vidrio
        if (!Tema.altoContraste) {
            Graphics2D r = (Graphics2D) g.create();
            r.clip(forma);
            r.setPaint(new GradientPaint(p.x, p.y, new Color(255, 255, 255, 22), p.x + p.width * 0.6f, p.y + p.height * 0.35f,
                    new Color(255, 255, 255, 0)));
            r.fill(p);
            r.dispose();
        }
        // boton para esconderlo (lengueta arriba del celular); en llamada no se puede guardar
        if (llamada) {
            return;
        }
        Rectangle x = new Rectangle(m.x + m.width / 2 - 46, m.y - 30, 92, 26);
        boolean hv = v.encima(x);
        g.setColor(hv ? Tema.COBRE : new Color(42, 23, 18, 220));
        g.fillRoundRect(x.x, x.y, x.width, x.height, 13, 13);
        g.setFont(Tema.fuente(Font.BOLD, 11.5f));
        g.setColor(Tema.CREMA);
        Dibujo.centrado(g, "Guardar  (P)", x.getCenterX(), x.y + 17);
        v.zona(x, this::cerrar);
    }

    /** Lengueta del celular escondido: muestra los mensajes sin leer y vibra si llega algo. */
    private void asa(Graphics2D g, Jornada j) {
        Rectangle r = new Rectangle(OficinaVista.W - 182, OficinaVista.H - 60, 166, 46);
        double vibra = v.t - avisoLadoT < 1.2 ? Math.sin(v.t * 60) * 3 * (1.2 - (v.t - avisoLadoT)) : 0;
        r.x += (int) vibra;
        boolean hv = v.encima(r);
        g.setColor(new Color(20, 8, 4, 70));
        g.fillRoundRect(r.x + 2, r.y + 4, r.width, r.height, 23, 23);
        g.setPaint(new GradientPaint(r.x, r.y, hv ? new Color(96, 56, 40) : new Color(74, 40, 28), r.x, r.y + r.height,
                new Color(42, 23, 18)));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 23, 23);
        g.setColor(new Color(242, 179, 94, hv ? 200 : 110));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 23, 23);
        // telefonito
        g.setColor(new Color(24, 22, 26));
        g.fillRoundRect(r.x + 16, r.y + 9, 17, 28, 6, 6);
        g.setColor(new Color(150, 120, 182));
        g.fillRoundRect(r.x + 18, r.y + 12, 13, 22, 4, 4);
        g.setFont(Tema.fuente(Font.BOLD, 13.5f));
        g.setColor(Tema.CREMA);
        g.drawString("Celular", r.x + 44, r.y + 21);
        g.setFont(Tema.fuente(Font.PLAIN, 11));
        g.setColor(Tema.textoSuave());
        g.drawString("tecla P", r.x + 44, r.y + 36);
        int nl = j.jugador.telefono.noLeidos();
        if (nl > 0) {
            g.setColor(Tema.LADRILLO);
            g.fill(new Ellipse2D.Double(r.x + r.width - 34, r.y + 11, 24, 24));
            g.setColor(Tema.CREMA);
            g.setFont(Tema.fuente(Font.BOLD, 12));
            Dibujo.centrado(g, String.valueOf(nl), r.x + r.width - 22, r.y + 28);
        }
        v.zona(r, this::alternar);
        // burbuja con la vista previa de lo que llego
        if (avisoLado != null && v.t - avisoLadoT < 4.2) {
            double a = Math.min(1, (v.t - avisoLadoT) / 0.2) * Math.min(1, (4.2 - (v.t - avisoLadoT)) / 0.4);
            int bw = 300;
            Rectangle b = new Rectangle(OficinaVista.W - bw - 16, r.y - 78, bw, 66);
            g.setColor(new Color(42, 23, 18, (int) (235 * a)));
            g.fillRoundRect(b.x, b.y, b.width, b.height, 18, 18);
            Path2D cola = new Path2D.Double();
            cola.moveTo(b.x + b.width - 70, b.y + b.height);
            cola.lineTo(b.x + b.width - 56, b.y + b.height + 10);
            cola.lineTo(b.x + b.width - 50, b.y + b.height);
            g.fill(cola);
            g.setFont(Tema.fuente(Font.BOLD, 12.5f));
            g.setColor(new Color(242, 179, 94, (int) (255 * a)));
            g.drawString(Dibujo.recortar(g.getFontMetrics(), avisoLado.app + " · " + avisoLado.titulo, b.width - 28), b.x + 14, b.y + 22);
            g.setFont(Tema.fuente(Font.PLAIN, 12));
            g.setColor(new Color(247, 235, 221, (int) (240 * a)));
            List<String> l = Avisos.partir(g, g.getFont(), avisoLado.texto, b.width - 28);
            for (int i = 0; i < l.size() && i < 2; i++) {
                String t = i == 1 && l.size() > 2 ? Dibujo.recortar(g.getFontMetrics(), l.get(i) + "…", b.width - 28) : l.get(i);
                g.drawString(t, b.x + 14, b.y + 40 + i * 16);
            }
            final Telefono.Notificacion n = avisoLado;
            v.zona(b, () -> {
                abrir(n.app.equals("Mensajes") ? App.MENSAJES : n.app.equals("Noticias") ? App.NOTICIAS : App.CIVITAS);
                avisoLado = null;
            });
        }
    }

    private void barraEstado(Graphics2D g, Rectangle p, Jornada j, boolean oscuro) {
        Color c = oscuro ? Color.WHITE : new Color(30, 24, 26);
        g.setColor(c);
        g.setFont(Tema.fuente(Font.BOLD, 14));
        g.drawString(j.relajado ? "9:41" : j.hora().replaceFirst("^0", ""), p.x + 30, p.y + 32);
        double x = p.x + p.width - 92, y = p.y + 31;
        // senal
        for (int i = 0; i < 4; i++) {
            boolean llena = !j.enApagon() || i < 1;
            g.setColor(llena ? c : new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
            g.fillRoundRect((int) x + i * 5, (int) y - 3 - i * 3, 3, 4 + i * 3, 1, 1);
        }
        // wifi
        g.setColor(c);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 3; i++) {
            double rr = 3 + i * 3.5;
            g.draw(new Arc2D.Double(x + 30 - rr, y - 4 - rr, rr * 2, rr * 2, 50, 80, Arc2D.OPEN));
        }
        g.fill(new Ellipse2D.Double(x + 28.5, y - 4.5, 3, 3));
        // bateria (baja con la hora)
        double carga = Math.max(0.12, 1 - Math.max(0, j.avanceDia()) * 0.7);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(x + 48, y - 11, 24, 12, 4, 4));
        g.fillRect((int) x + 73, (int) y - 7, 2, 4);
        g.setColor(carga < 0.25 ? Tema.LADRILLO : c);
        g.fill(new RoundRectangle2D.Double(x + 50, y - 9, 20 * carga, 8, 2, 2));
        g.setStroke(new BasicStroke(1));
    }

    private void bannerNotificacion(Graphics2D g, Rectangle p) {
        if (banner == null) {
            return;
        }
        double edad = v.t - bannerT;
        if (edad > 3.6) {
            banner = null;
            return;
        }
        double baja = edad < 0.25 ? (1 - edad / 0.25) * -90 : edad > 3.2 ? (edad - 3.2) / 0.4 * -90 : 0;
        Rectangle b = new Rectangle(p.x + 10, (int) (p.y + 50 + baja), p.width - 20, 78);
        g.setColor(new Color(36, 30, 34, 238));
        g.fillRoundRect(b.x, b.y, b.width, b.height, 22, 22);
        App a = banner.app.equals("Mensajes") ? App.MENSAJES : banner.app.equals("Noticias") ? App.NOTICIAS
                : banner.app.equals("Teléfono") ? App.TELEFONO : banner.app.equals("Mapa") ? App.MAPA : App.CIVITAS;
        icono(g, a, b.x + 14, b.y + 14, 34);
        g.setFont(Tema.fuente(Font.BOLD, 12.5f));
        g.setColor(Color.WHITE);
        g.drawString(Dibujo.recortar(g.getFontMetrics(), banner.titulo, b.width - 110), b.x + 58, b.y + 27);
        g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
        g.setColor(new Color(255, 255, 255, 150));
        g.drawString("ahora", b.x + b.width - 44, b.y + 27);
        g.setFont(Tema.fuente(Font.PLAIN, 12));
        g.setColor(new Color(255, 255, 255, 220));
        List<String> l = Avisos.partir(g, g.getFont(), banner.texto, b.width - 72);
        for (int i = 0; i < l.size() && i < 2; i++) {
            g.drawString(i == 1 && l.size() > 2 ? Dibujo.recortar(g.getFontMetrics(), l.get(i) + "…", b.width - 72) : l.get(i),
                    b.x + 58, b.y + 46 + i * 16);
        }
        final App destino = a;
        v.zona(b, () -> {
            abrir(destino);
            banner = null;
        });
    }

    // ------------------------------------------------------------------ inicio
    private void fondoInicio(Graphics2D g, Rectangle p) {
        g.setPaint(new GradientPaint(p.x, p.y, new Color(96, 70, 128), p.x, p.y + p.height, new Color(206, 116, 72)));
        g.fill(p);
        if (!Tema.altoContraste) {
            double[][] b = {{0.2, 0.18, 120}, {0.85, 0.36, 160}, {0.3, 0.72, 140}, {0.75, 0.9, 110}};
            for (double[] o : b) {
                double cx = p.x + p.width * o[0], cy = p.y + p.height * o[1], r = o[2];
                g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float) r, new float[]{0f, 1f},
                        new Color[]{new Color(255, 214, 160, 70), new Color(255, 214, 160, 0)}));
                g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            }
        }
    }

    private void inicio(Graphics2D g, Rectangle p, Jornada j) {
        fondoInicio(g, p);
        // reloj grande
        g.setColor(Color.WHITE);
        g.setFont(Tema.fuente(Font.BOLD, 54));
        Dibujo.centrado(g, j.relajado ? "9:41" : j.hora().replaceFirst("^0", ""), p.getCenterX(), p.y + 128);
        g.setFont(Tema.fuente(Font.PLAIN, 14));
        g.setColor(new Color(255, 255, 255, 220));
        Dibujo.centrado(g, "Día " + j.dia + " de " + Historia.DIAS + "  ·  " + Historia.TITULOS[j.dia], p.getCenterX(), p.y + 70);
        // grilla de apps
        int cols = 4, tam = 58;
        int gx = (p.width - cols * tam) / (cols + 1);
        for (int i = 0; i < GRILLA.length; i++) {
            App a = GRILLA[i];
            int x = p.x + gx + (i % cols) * (tam + gx), y = p.y + 176 + (i / cols) * 100;
            botonApp(g, a, x, y, tam, j, true);
        }
        // dock
        Rectangle d = new Rectangle(p.x + 12, p.y + p.height - 112, p.width - 24, 88);
        g.setColor(new Color(255, 255, 255, 52));
        g.fillRoundRect(d.x, d.y, d.width, d.height, 30, 30);
        for (int i = 0; i < DOCK.length; i++) {
            int x = p.x + gx + i * (tam + gx), y = d.y + 15;
            botonApp(g, DOCK[i], x, y, tam, j, false);
        }
    }

    private void botonApp(Graphics2D g, App a, int x, int y, int tam, Jornada j, boolean conNombre) {
        Rectangle r = new Rectangle(x, y, tam, tam);
        boolean hv = v.encima(r);
        Graphics2D s = (Graphics2D) g.create();
        if (hv) {
            s.translate(0, -2);
        }
        icono(s, a, x, y, tam);
        if (a == App.HABILIDAD) {
            Dibujo.emblemaRol(s, j.jugador.rol, x + tam / 2.0, y + tam / 2.0, tam * 0.34, true);
        }
        // estado de la app
        String no = motivoBloqueo(a, j);
        if (no != null && a.herramienta != null) {
            s.setColor(new Color(20, 14, 16, 120));
            s.fill(new RoundRectangle2D.Double(x, y, tam, tam, tam * 0.28, tam * 0.28));
            boolean candado = !j.delRol(a.herramienta) || a.herramienta.dia > j.dia;
            if (candado) {
                Iconos.candado(s, x + tam - 10, y + 10, 6, Color.WHITE);
            } else {
                s.setColor(Tema.LADRILLO);
                s.fill(new Ellipse2D.Double(x + tam - 18, y - 4, 22, 22));
                s.setColor(Color.WHITE);
                s.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                s.draw(new Line2D.Double(x + tam - 11.5, y + 2.5, x + tam - 2.5, y + 11.5));
                s.draw(new Line2D.Double(x + tam - 2.5, y + 2.5, x + tam - 11.5, y + 11.5));
            }
        } else {
            int badge = 0;
            boolean punto = false;
            if (a == App.MENSAJES) {
                badge = j.jugador.telefono.noLeidos();
            } else if (a == App.CIVITAS) {
                punto = v.caso() != null && v.caso().decidido == null;
            } else if (a.herramienta != null && v.caso() != null && j.resultados(a.herramienta) != null) {
                s.setColor(Tema.SALVIA);
                s.fill(new Ellipse2D.Double(x + tam - 16, y - 4, 20, 20));
                s.setColor(Color.WHITE);
                s.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D ch = new Path2D.Double();
                ch.moveTo(x + tam - 11, y + 6);
                ch.lineTo(x + tam - 7, y + 10);
                ch.lineTo(x + tam - 1, y + 2);
                s.draw(ch);
            } else if (a == App.HABILIDAD && j.puedeUsarHabilidad() && v.atendiendo()) {
                punto = true;
            }
            if (badge > 0 || punto) {
                s.setColor(new Color(214, 72, 58));
                double bw = badge > 0 ? 22 : 14;
                s.fill(new Ellipse2D.Double(x + tam - bw + 6, y - 6, bw, bw));
                if (badge > 0) {
                    s.setColor(Color.WHITE);
                    s.setFont(Tema.fuente(Font.BOLD, 11.5f));
                    Dibujo.centrado(s, String.valueOf(badge), x + tam - 5, y + 9);
                }
            }
        }
        if (conNombre) {
            s.setFont(Tema.fuente(Font.PLAIN, 11.5f));
            s.setColor(new Color(0, 0, 0, 70));
            Dibujo.centrado(s, a.nombre, x + tam / 2.0 + 1, y + tam + 18);
            s.setColor(Color.WHITE);
            Dibujo.centrado(s, a.nombre, x + tam / 2.0, y + tam + 17);
        }
        s.dispose();
        v.zona(r, () -> tocarApp(a));
    }

    /** null si la app se puede abrir con el caso actual; si no, el motivo. */
    private String motivoBloqueo(App a, Jornada j) {
        if (a.herramienta == null) {
            return a == App.HABILIDAD && !j.jugador.rol.tieneMiniJuego() ? "periodista" : null;
        }
        if (!j.delRol(a.herramienta) || a.herramienta.dia > j.dia) {
            return j.motivoNoAplica(a.herramienta);
        }
        if (v.caso() != null && j.getActual() != null && j.resultados(a.herramienta) == null) {
            return j.motivoNoAplica(a.herramienta);
        }
        return null;
    }

    private void tocarApp(App a) {
        Sonido.tocar(Sonido.Tipo.CLIC);
        Jornada j = v.jornada();
        if (a.herramienta != null && (!j.delRol(a.herramienta) || a.herramienta.dia > j.dia)) {
            v.alertaRoja(j.motivoNoAplica(a.herramienta));
            return;
        }
        abrir(a);
    }

    /** Icono de cada app: cuadro redondeado con degradado y un dibujo blanco. */
    static void icono(Graphics2D g, App a, double x, double y, double tam) {
        Color c;
        switch (a) {
            case CIVITAS: c = new Color(122, 92, 153); break;
            case MENSAJES: c = new Color(104, 160, 84); break;
            case TELEFONO: c = new Color(84, 140, 70); break;
            case NOTICIAS: c = new Color(192, 71, 58); break;
            case PERFIL: case DIRECTORIO: c = Tema.COBRE; break;
            case LUPA: c = new Color(140, 108, 172); break;
            case IMAGEN: c = new Color(214, 150, 70); break;
            case MAPA: c = new Color(92, 140, 120); break;
            case RED: c = new Color(110, 54, 33); break;
            case TIENDA: c = new Color(196, 140, 60); break;
            default: c = new Color(160, 110, 70); break;
        }
        RoundRectangle2D f = new RoundRectangle2D.Double(x, y, tam, tam, tam * 0.28, tam * 0.28);
        g.setPaint(new GradientPaint((float) x, (float) y, Tema.mezclar(c, Color.WHITE, 0.22), (float) x, (float) (y + tam),
                Tema.mezclar(c, Color.BLACK, 0.12)));
        g.fill(f);
        g.setColor(new Color(255, 255, 255, 40));
        g.draw(f);
        double cx = x + tam / 2, cy = y + tam / 2, r = tam * 0.3;
        Graphics2D s = (Graphics2D) g.create();
        s.setColor(Color.WHITE);
        s.setStroke(new BasicStroke((float) (tam * 0.06), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (a) {
            case CIVITAS: {
                // burbuja con un visto
                s.fill(new RoundRectangle2D.Double(cx - r, cy - r * 0.85, r * 2, r * 1.5, r * 0.8, r * 0.8));
                Path2D cola = new Path2D.Double();
                cola.moveTo(cx - r * 0.5, cy + r * 0.5);
                cola.lineTo(cx - r * 0.75, cy + r * 1.05);
                cola.lineTo(cx, cy + r * 0.6);
                s.fill(cola);
                s.setColor(new Color(122, 92, 153));
                Path2D ch = new Path2D.Double();
                ch.moveTo(cx - r * 0.45, cy - r * 0.1);
                ch.lineTo(cx - r * 0.1, cy + r * 0.22);
                ch.lineTo(cx + r * 0.5, cy - r * 0.42);
                s.draw(ch);
                break;
            }
            case MENSAJES: {
                s.fill(new Ellipse2D.Double(cx - r * 1.05, cy - r * 0.9, r * 2.1, r * 1.65));
                Path2D cola = new Path2D.Double();
                cola.moveTo(cx - r * 0.6, cy + r * 0.45);
                cola.lineTo(cx - r * 0.95, cy + r * 1.0);
                cola.lineTo(cx - r * 0.1, cy + r * 0.7);
                s.fill(cola);
                break;
            }
            case TELEFONO:
                auricular(s, cx, cy, r);
                break;
            case NOTICIAS: {
                s.draw(new RoundRectangle2D.Double(cx - r, cy - r * 0.75, r * 2, r * 1.5, r * 0.3, r * 0.3));
                s.draw(new Line2D.Double(cx - r * 0.4, cy - r * 1.15, cx, cy - r * 0.75));
                s.draw(new Line2D.Double(cx + r * 0.4, cy - r * 1.15, cx, cy - r * 0.75));
                s.fill(new Ellipse2D.Double(cx - r * 0.18, cy - r * 0.18, r * 0.36, r * 0.36));
                break;
            }
            case HABILIDAD:
                s.dispose();
                return;     // el emblema del rol lo pone botonApp
            case TIENDA: {
                s.draw(new RoundRectangle2D.Double(cx - r * 0.8, cy - r * 0.45, r * 1.6, r * 1.35, r * 0.3, r * 0.3));
                s.draw(new Arc2D.Double(cx - r * 0.45, cy - r * 1.0, r * 0.9, r * 1.0, 0, 180, Arc2D.OPEN));
                break;
            }
            default:
                Iconos.herramienta(s, a.herramienta.ordinal(), cx, cy, r * 1.05, Color.WHITE);
                break;
        }
        s.dispose();
    }

    private static void auricular(Graphics2D s, double cx, double cy, double r) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx - r * 0.85, cy - r * 0.75);
        p.curveTo(cx - r * 1.1, cy + r * 0.2, cx - r * 0.2, cy + r * 1.1, cx + r * 0.75, cy + r * 0.85);
        p.lineTo(cx + r * 0.95, cy + r * 0.45);
        p.lineTo(cx + r * 0.45, cy + r * 0.15);
        p.lineTo(cx + r * 0.2, cy + r * 0.4);
        p.curveTo(cx - r * 0.15, cy + r * 0.25, cx - r * 0.3, cy + r * 0.05, cx - r * 0.4, cy - r * 0.25);
        p.lineTo(cx - r * 0.15, cy - r * 0.5);
        p.lineTo(cx - r * 0.45, cy - r * 0.95);
        p.closePath();
        s.fill(p);
    }

    // ------------------------------------------------------------------ llamada
    private void pantallaLlamada(Graphics2D g, Rectangle p, Jornada j) {
        g.setPaint(new GradientPaint(p.x, p.y, new Color(74, 48, 60), p.x, p.y + p.height, new Color(30, 20, 24)));
        g.fill(p);
        double cx = p.getCenterX();
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(new Color(255, 255, 255, 170));
        int seg = (int) llamadaT;
        Dibujo.centrado(g, String.format("En llamada  %02d:%02d", seg / 60, seg % 60), cx, p.y + 88);
        // foto del contacto
        double r = 66, cy = p.y + 190;
        double pulso = 1 + 0.04 * Math.sin(v.t * 3);
        g.setColor(new Color(255, 255, 255, 30));
        g.fill(new Ellipse2D.Double(cx - r * 1.25 * pulso, cy - r * 1.25 * pulso, r * 2.5 * pulso, r * 2.5 * pulso));
        Ellipse2D foto = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        g.setColor(new Color(238, 214, 205));
        g.fill(foto);
        Graphics2D f = (Graphics2D) g.create();
        f.clip(foto);
        Caras.dibujar(f, Historia.caraDelJefe(v.semilla()), cx, cy + 4, 0.9, "feliz", false,
                Math.abs(Math.sin(v.t * 9)) * (v.hablando() ? 1 : 0));
        f.dispose();
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(2));
        g.draw(foto);
        g.setStroke(new BasicStroke(1));
        g.setFont(Tema.fuente(Font.BOLD, 26));
        g.setColor(Color.WHITE);
        Dibujo.centrado(g, Telefono.JEFE, cx, cy + r + 50);
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(new Color(255, 255, 255, 170));
        Dibujo.centrado(g, "Influence, Inc.  ·  móvil", cx, cy + r + 74);
        // botones
        String[] et = {"silencio", "teclado", "altavoz"};
        for (int i = 0; i < 3; i++) {
            double bx = cx + (i - 1) * 92, by = p.y + p.height - 236;
            g.setColor(new Color(255, 255, 255, 40));
            g.fill(new Ellipse2D.Double(bx - 31, by - 31, 62, 62));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (i == 0) {
                g.draw(new RoundRectangle2D.Double(bx - 6, by - 14, 12, 20, 12, 12));
                g.draw(new Arc2D.Double(bx - 11, by - 6, 22, 18, 180, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(bx - 12, by - 14, bx + 12, by + 12));
            } else if (i == 1) {
                for (int q = 0; q < 9; q++) {
                    g.fill(new Ellipse2D.Double(bx - 12 + (q % 3) * 10, by - 12 + (q / 3) * 10, 4, 4));
                }
            } else {
                Path2D al = new Path2D.Double();
                al.moveTo(bx - 12, by - 5);
                al.lineTo(bx - 6, by - 5);
                al.lineTo(bx + 2, by - 12);
                al.lineTo(bx + 2, by + 12);
                al.lineTo(bx - 6, by + 5);
                al.lineTo(bx - 12, by + 5);
                al.closePath();
                g.fill(al);
                g.draw(new Arc2D.Double(bx, by - 8, 12, 16, -60, 120, Arc2D.OPEN));
            }
            g.setStroke(new BasicStroke(1));
            g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
            g.setColor(new Color(255, 255, 255, 180));
            Dibujo.centrado(g, et[i], bx, by + 50);
        }
        // colgar
        Rectangle col = new Rectangle((int) cx - 36, p.y + p.height - 120, 72, 72);
        boolean hv = v.encima(col);
        g.setColor(hv ? new Color(232, 88, 74) : new Color(214, 66, 54));
        g.fill(new Ellipse2D.Double(col.x, col.y, col.width, col.height));
        Graphics2D a = (Graphics2D) g.create();
        a.rotate(Math.toRadians(135), col.getCenterX(), col.getCenterY());
        a.setColor(Color.WHITE);
        auricular(a, col.getCenterX(), col.getCenterY(), 15);
        a.dispose();
        v.zona(col, v::colgar);
    }

    /** Pantalla de llamada con un contacto de la agenda. */
    private void pantallaContacto(Graphics2D g, Rectangle p) {
        Color arriba = contactoOk ? new Color(58, 84, 60) : new Color(92, 46, 42);
        g.setPaint(new GradientPaint(p.x, p.y, arriba, p.x, p.y + p.height, new Color(30, 20, 24)));
        g.fill(p);
        double cx = p.getCenterX();
        boolean marcando = contactoT < 1.2;
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(new Color(255, 255, 255, 170));
        int seg = (int) Math.max(0, contactoT - 1.2);
        Dibujo.centrado(g, marcando ? "Llamando..." : String.format("En llamada  %02d:%02d", seg / 60, seg % 60), cx, p.y + 88);
        double r = 56, cy = p.y + 172;
        double pulso = 1 + 0.06 * Math.sin(v.t * (marcando ? 8 : 3));
        g.setColor(new Color(255, 255, 255, 30));
        g.fill(new Ellipse2D.Double(cx - r * 1.25 * pulso, cy - r * 1.25 * pulso, r * 2.5 * pulso, r * 2.5 * pulso));
        avatar(g, contacto, cx, cy, r);
        g.setFont(Dibujo.ajustar(g, contacto, Font.BOLD, 22, p.width - 40));
        g.setColor(Color.WHITE);
        Dibujo.centrado(g, contacto, cx, cy + r + 42);
        if (!marcando) {
            // lo que contesto, en una burbuja
            Font f = Tema.fuente(Font.PLAIN, 13);
            List<String> l = Avisos.partir(g, f, respuesta, p.width - 76);
            int alto = Math.min(9, l.size()) * 18 + 44;
            Rectangle b = new Rectangle(p.x + 22, (int) (cy + r + 66), p.width - 44, alto);
            g.setColor(new Color(255, 255, 255, 235));
            g.fillRoundRect(b.x, b.y, b.width, b.height, 20, 20);
            g.setFont(Tema.fuente(Font.BOLD, 11));
            g.setColor(contactoOk ? new Color(66, 124, 58) : new Color(176, 58, 50));
            g.drawString(contactoOk ? "LLAMADA ÚTIL PARA EL CASO" : "NO APLICA  ·  −" + JugadorVerificador.COSTO_LLAMADA_MALA
                    + " DE VIDA", b.x + 16, b.y + 22);
            g.setColor(new Color(40, 30, 28));
            Dibujo.parrafo(g, f, respuesta, b.x + 16, b.y + 42, b.width - 32, 18, b.y + b.height - 4);
        }
        Rectangle col = new Rectangle((int) cx - 34, p.y + p.height - 116, 68, 68);
        boolean hv = v.encima(col);
        g.setColor(hv ? new Color(232, 88, 74) : new Color(214, 66, 54));
        g.fill(new Ellipse2D.Double(col.x, col.y, col.width, col.height));
        Graphics2D a = (Graphics2D) g.create();
        a.rotate(Math.toRadians(135), col.getCenterX(), col.getCenterY());
        a.setColor(Color.WHITE);
        auricular(a, col.getCenterX(), col.getCenterY(), 14);
        a.dispose();
        v.zona(col, this::colgarContacto);
    }

    // ==================================================================
    // Apps
    // ==================================================================

    // ------------------------------------------------------------------ Tienda
    /** Tienda dentro del celular: saldo arriba y una tarjeta por articulo. */
    private int tienda(Graphics2D g, Rectangle c, int y, Jornada j) {
        JugadorVerificador jv = j.jugador;
        Rectangle sal = new Rectangle(c.x, y, c.width, 84);
        g.setPaint(new GradientPaint(sal.x, sal.y, new Color(196, 140, 60), sal.x + sal.width, sal.y + sal.height,
                new Color(150, 84, 46)));
        g.fillRoundRect(sal.x, sal.y, sal.width, sal.height, 20, 20);
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(new Ellipse2D.Double(sal.x + sal.width - 90, sal.y - 30, 130, 130));
        g.setFont(Tema.fuente(Font.PLAIN, 12));
        g.setColor(new Color(255, 255, 255, 220));
        g.drawString("Tu saldo", sal.x + 18, sal.y + 26);
        g.setFont(Tema.fuente(Font.BOLD, 30));
        g.setColor(Color.WHITE);
        g.drawString("$" + jv.dinero, sal.x + 18, sal.y + 62);
        g.setFont(Tema.fuente(Font.PLAIN, 11));
        g.setColor(new Color(255, 255, 255, 210));
        String vida = "Vida " + jv.vida + "/" + JugadorVerificador.VIDA_MAX;
        g.drawString(vida, sal.x + sal.width - 18 - g.getFontMetrics().stringWidth(vida), sal.y + 62);
        y += 98;
        String msg = v.mensajeTienda();
        if (msg != null) {
            Font f = Tema.fuente(Font.PLAIN, 12);
            List<String> l = Avisos.partir(g, f, msg, c.width - 40);
            int alto = l.size() * 16 + 16;
            g.setColor(new Color(104, 160, 84, 40));
            g.fillRoundRect(c.x, y, c.width, alto, 14, 14);
            Iconos.marca(g, 2, c.x + 14, y + 14, 6);
            g.setColor(new Color(50, 96, 44));
            Dibujo.parrafo(g, f, msg, c.x + 28, y + 19, c.width - 40, 16, Integer.MAX_VALUE);
            y += alto + 10;
        }
        String tipo = null;
        for (Tienda.Articulo a : Tienda.Articulo.values()) {
            if (!a.tipo.equals(tipo)) {
                tipo = a.tipo;
                g.setFont(Tema.fuente(Font.BOLD, 11));
                g.setColor(new Color(150, 84, 46));
                g.drawString(tipo.toUpperCase(), c.x + 2, y + 12);
                y += 20;
            }
            String no = Tienda.motivoNoCompra(a, j);
            Font fd = Tema.fuente(Font.PLAIN, 11);
            List<String> l = Avisos.partir(g, fd, a.descripcion, c.width - 76);
            int alto = Math.max(70, 40 + l.size() * 15);
            Rectangle t = new Rectangle(c.x, y, c.width, alto);
            g.setColor(Color.WHITE);
            g.fillRoundRect(t.x, t.y, t.width, t.height, 18, 18);
            g.setColor(new Color(0, 0, 0, 18));
            g.drawRoundRect(t.x, t.y, t.width, t.height, 18, 18);
            // icono
            g.setColor(no == null ? new Color(196, 140, 60) : new Color(190, 176, 170));
            g.fillRoundRect(t.x + 10, t.y + 12, 44, 44, 14, 14);
            OficinaVista.iconoArticulo(g, a, t.x + 32, t.y + 34, 18);
            g.setFont(Dibujo.ajustar(g, a.nombre, Font.BOLD, 13, c.width - 150));
            g.setColor(new Color(30, 24, 26));
            g.drawString(a.nombre, t.x + 64, t.y + 24);
            g.setColor(new Color(120, 100, 94));
            Dibujo.parrafo(g, fd, a.descripcion, t.x + 64, t.y + 42, c.width - 76, 15, t.y + t.height - 4);
            // pastilla de precio / comprar
            Rectangle pr = new Rectangle(t.x + t.width - 78, t.y + 9, 68, 24);
            boolean hv = no == null && v.encima(pr);
            g.setColor(no == null ? (hv ? new Color(84, 140, 70) : new Color(104, 160, 84)) : new Color(226, 216, 212));
            g.fillRoundRect(pr.x, pr.y, pr.width, pr.height, 24, 24);
            g.setFont(Tema.fuente(Font.BOLD, 12));
            g.setColor(no == null ? Color.WHITE : new Color(140, 120, 114));
            Dibujo.centrado(g, "$" + a.precio, pr.getCenterX(), pr.y + 17);
            final Tienda.Articulo art = a;
            final String motivo = no;
            zonaVisible(t, c, () -> {
                if (motivo == null) {
                    v.comprar(art);
                } else {
                    v.alertaRoja(motivo);
                }
            });
            y += alto + 8;
        }
        g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
        g.setColor(new Color(150, 130, 124));
        g.drawString("Toca un artículo para comprarlo (tecla S abre la tienda).", c.x, y + 10);
        return y + 20;
    }

    private void contenidoApp(Graphics2D g, Rectangle p, Jornada j) {
        // encabezado: volver + nombre de la app
        int top = p.y + 48;
        Rectangle back = new Rectangle(p.x + 10, top, 110, 40);
        g.setColor(new Color(122, 92, 153));
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(p.x + 28, top + 12, p.x + 20, top + 20));
        g.draw(new Line2D.Double(p.x + 20, top + 20, p.x + 28, top + 28));
        g.setStroke(new BasicStroke(1));
        g.setFont(Tema.fuente(Font.PLAIN, 14));
        String volver = app == App.MENSAJES && chat != null ? "Chats" : "Inicio";
        g.drawString(volver, p.x + 34, top + 25);
        v.zona(back, () -> {
            if (app == App.MENSAJES && chat != null) {
                chat = null;
                scroll = 0;
            } else {
                inicio();
            }
        });
        g.setFont(Tema.fuente(Font.BOLD, 16));
        g.setColor(new Color(30, 24, 26));
        String titulo = app == App.MENSAJES && chat != null ? chat : app.nombre;
        Dibujo.centrado(g, Dibujo.recortar(g.getFontMetrics(), titulo, p.width - 190), p.getCenterX() + 14, top + 25);
        g.setColor(new Color(0, 0, 0, 22));
        g.fillRect(p.x, top + 42, p.width, 1);
        Rectangle c = new Rectangle(p.x + 14, top + 58, p.width - 28, p.height - (top + 58 - p.y) - 30);
        Graphics2D s = (Graphics2D) g.create();
        s.clip(new Rectangle(p.x, c.y - 6, p.width, c.height + 6));
        int y0 = c.y - scroll;
        int yFin;
        switch (app) {
            case CIVITAS: yFin = civitas(s, c, y0, j); break;
            case MENSAJES: yFin = chat == null ? mensajes(s, c, y0, j) : conversacion(s, c, y0, j); break;
            case TELEFONO: yFin = telefono(s, c, y0, j); break;
            case NOTICIAS: yFin = noticias(s, c, y0, j); break;
            case HABILIDAD: yFin = habilidad(s, c, y0, j); break;
            case TIENDA: yFin = tienda(s, c, y0, j); break;
            default: yFin = herramienta(s, c, y0, j, app.herramienta); break;
        }
        s.dispose();
        altoContenido = yFin - y0;
        // indicador de desplazamiento
        if (altoContenido > c.height) {
            double frac = (double) c.height / altoContenido;
            double pos = (double) scroll / Math.max(1, altoContenido - c.height);
            int bh = (int) Math.max(30, c.height * frac);
            g.setColor(new Color(0, 0, 0, 60));
            g.fillRoundRect(p.x + p.width - 7, (int) (c.y + (c.height - bh) * pos), 4, bh, 4, 4);
        }
    }

    /** Registra una zona solo si se ve dentro del area de la app. */
    private void zonaVisible(Rectangle r, Rectangle c, Runnable a) {
        if (r.intersects(c)) {
            v.zona(r.intersection(c), a);
        }
    }

    private static Color colorMarca(int tipo) {
        return tipo == 1 ? new Color(170, 104, 24) : tipo == 2 ? new Color(66, 124, 58) : tipo == 3 ? new Color(176, 58, 50)
                : new Color(48, 36, 34);
    }

    private int lineas(Graphics2D g, List<String> res, int x, int y, int ancho) {
        Font f = Tema.fuente(Font.PLAIN, 12.5f);
        for (String l : res) {
            int tp = Iconos.tipo(l);
            if (tp != 0) {
                Iconos.marca(g, tp, x + 7, y - 4, 6);
            }
            g.setColor(colorMarca(tp));
            y = Dibujo.parrafo(g, f, Iconos.sinPrefijo(l), x + (tp != 0 ? 20 : 0), y, ancho - (tp != 0 ? 20 : 0), 17,
                    Integer.MAX_VALUE) + 6;
        }
        return y;
    }

    // ------------------------------------------------------------------ Civitas
    private int civitas(Graphics2D g, Rectangle c, int y, Jornada j) {
        Caso caso = v.caso();
        if (caso == null) {
            g.setColor(new Color(232, 222, 216));
            for (int i = 0; i < 3; i++) {
                g.fill(new Ellipse2D.Double(c.x, y, 34, 34));
                g.fillRoundRect(c.x + 44, y + 4, 140, 10, 10, 10);
                g.fillRoundRect(c.x + 44, y + 20, 90, 9, 9, 9);
                g.fillRoundRect(c.x, y + 44, c.width, 90, 16, 16);
                y += 150;
            }
            g.setFont(Tema.fuente(Font.BOLD, 13.5f));
            g.setColor(new Color(120, 96, 90));
            Dibujo.centrado(g, "Sin publicaciones por revisar", c.getCenterX(), c.y + 220);
            return y;
        }
        int ini = y;
        y = publicacion(g, c, y, caso, true);
        // la marca del sello cae sobre la foto
        if (caso.decidido != null && !v.sellando()) {
            OficinaVista.estampa(g, caso.decidido, c.getCenterX(), ini + 150, 1.05, -0.22, 1f);
        }
        // tendencias del dia
        y += 18;
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(new Color(48, 36, 34));
        g.drawString("Tendencias", c.x, y);
        y += 10;
        List<String[]> ten = Noticiero.tendencias(j.dia, v.semilla(), Math.max(0, j.avanceDia()));
        for (int i = 0; i < ten.size() && i < 3; i++) {
            y += 26;
            g.setFont(Tema.fuente(Font.BOLD, 12.5f));
            g.setColor(new Color(122, 92, 153));
            g.drawString((i + 1) + ".  " + ten.get(i)[0], c.x, y);
            g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
            g.setColor(new Color(120, 96, 90));
            String n = ten.get(i)[1] + " publicaciones";
            g.drawString(n, c.x + c.width - g.getFontMetrics().stringWidth(n), y);
        }
        return archivo(g, c, y + 20, j);
    }

    /** Archivo de la semana: publicaciones selladas, clasificadas en un ABB por credibilidad (inorden). */
    private int archivo(Graphics2D g, Rectangle c, int y, Jornada j) {
        List<String> l = j.jugador.archivo.valoresEnOrden();
        if (l.isEmpty()) {
            return y;
        }
        y += 10;
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(new Color(48, 36, 34));
        g.drawString("Tu archivo (ABB por credibilidad)", c.x, y);
        for (String a : l) {
            String[] p = a.split("\\|", 4);
            y += 8;
            g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
            List<String> lineas = Avisos.partir(g, g.getFont(), p[3], c.width - 54);
            int h = 22 + Math.min(2, lineas.size()) * 15;
            g.setColor(new Color(122, 92, 153, 22));
            g.fillRoundRect(c.x, y, c.width, h, 14, 14);
            boolean ok = p[2].equals("1");
            g.setColor(ok ? Tema.SALVIA : Tema.LADRILLO);
            g.fill(new Ellipse2D.Double(c.x + 8, y + 8, 30, 30));
            g.setFont(Tema.fuente(Font.BOLD, 11));
            g.setColor(Color.WHITE);
            Dibujo.centrado(g, p[0], c.x + 23, y + 27);
            g.setColor(new Color(40, 30, 28));
            g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
            for (int i = 0; i < lineas.size() && i < 2; i++) {
                g.drawString(lineas.get(i), c.x + 46, y + 18 + i * 15);
            }
            y += h;
        }
        return y + 14;
    }

    /** La tarjeta de la publicacion (tambien la usa el minijuego de sellos). */
    static int publicacion(Graphics2D g, Rectangle c, int y, Caso caso, boolean conPeticion) {
        Publicacion pub = caso.pub;
        Cuenta cu = pub.getCuenta();
        g.setColor(new Color(122, 92, 153));
        g.fill(new Ellipse2D.Double(c.x, y, 36, 36));
        String ini = cu == null ? "?" : cu.getNombre().substring(0, 1).toUpperCase();
        g.setFont(Tema.fuente(Font.BOLD, 16));
        g.setColor(Color.WHITE);
        Dibujo.centrado(g, ini, c.x + 18, y + 24);
        String nombre = cu == null ? pub.getAutor() : cu.getNombre();
        g.setFont(Tema.fuente(Font.BOLD, 13.5f));
        g.setColor(new Color(30, 24, 26));
        FontMetrics fm = g.getFontMetrics();
        String nm = Dibujo.recortar(fm, nombre == null ? "Usuario" : nombre, c.width - 70);
        g.drawString(nm, c.x + 46, y + 15);
        if (cu != null && cu.estaVerificada()) {
            int vx = c.x + 50 + fm.stringWidth(nm);
            g.setColor(new Color(122, 92, 153));
            g.fill(new Ellipse2D.Double(vx, y + 4, 13, 13));
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D ch = new Path2D.Double();
            ch.moveTo(vx + 3.5, y + 10.5);
            ch.lineTo(vx + 6, y + 13);
            ch.lineTo(vx + 10, y + 7.5);
            g.draw(ch);
            g.setStroke(new BasicStroke(1));
        }
        g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
        g.setColor(new Color(130, 110, 104));
        g.drawString(Dibujo.recortar(g.getFontMetrics(), (cu == null ? "" : cu.getArroba() + " · ")
                + (pub.getHora() == null ? "" : pub.getHora()), c.width - 50), c.x + 46, y + 31);
        y += 46;
        int ih = (int) (c.width * 0.62);
        boolean video = pub.getImagen() != null && pub.getImagen().startsWith("video");
        Graphics2D il = (Graphics2D) g.create();
        il.clip(new RoundRectangle2D.Double(c.x, y, c.width, ih, 18, 18));
        Ilustracion.dibujar(il, new Rectangle(c.x, y, c.width, ih), pub.getTexto(), video, System.nanoTime() / 1e9);
        il.dispose();
        y += ih + 10;
        // acciones
        g.setColor(new Color(30, 24, 26));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D cor = new Path2D.Double();
        double hx = c.x + 9, hy = y + 9;
        cor.moveTo(hx, hy + 7);
        cor.curveTo(hx - 11, hy - 1, hx - 5, hy - 10, hx, hy - 3);
        cor.curveTo(hx + 5, hy - 10, hx + 11, hy - 1, hx, hy + 7);
        g.draw(cor);
        g.draw(new RoundRectangle2D.Double(c.x + 28, y, 18, 15, 8, 8));
        g.draw(new Line2D.Double(c.x + 64, y + 1, c.x + 64, y + 15));
        g.draw(new Line2D.Double(c.x + 58, y + 7, c.x + 64, y + 1));
        g.draw(new Line2D.Double(c.x + 70, y + 7, c.x + 64, y + 1));
        g.setStroke(new BasicStroke(1));
        int likes = 40 + Math.abs(pub.getTexto().hashCode()) % 900;
        g.setFont(Tema.fuente(Font.BOLD, 12));
        g.drawString(likes + " me gusta", c.x + 86, y + 13);
        y += 34;
        g.setColor(new Color(30, 24, 26));
        y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 14), pub.getTexto(), c.x, y, c.width, 20, Integer.MAX_VALUE);
        if (conPeticion) {
            y += 6;
            g.setFont(Tema.fuente(Font.ITALIC, 12));
            List<String> l = Avisos.partir(g, g.getFont(), "Te pide: " + caso.peticion, c.width - 24);
            int h = 14 + l.size() * 16;
            g.setColor(new Color(200, 105, 58, 34));
            g.fillRoundRect(c.x, y, c.width, h, 14, 14);
            g.setColor(new Color(170, 80, 42));
            int ly = y + 19;
            for (String s : l) {
                g.drawString(s, c.x + 12, ly);
                ly += 16;
            }
            y += h;
        }
        return y;
    }

    // ------------------------------------------------------------------ Mensajes
    private int mensajes(Graphics2D g, Rectangle c, int y, Jornada j) {
        List<Telefono.Conversacion> l = j.jugador.telefono.conversaciones();
        if (l.isEmpty()) {
            g.setFont(Tema.fuente(Font.ITALIC, 13));
            g.setColor(new Color(130, 110, 104));
            Dibujo.centrado(g, "Todavía no tienes mensajes.", c.getCenterX(), y + 60);
            return y + 80;
        }
        for (Telefono.Conversacion conv : l) {
            Rectangle fila = new Rectangle(c.x - 4, y, c.width + 8, 66);
            if (v.encima(fila) && fila.intersects(c)) {
                g.setColor(new Color(122, 92, 153, 22));
                g.fillRoundRect(fila.x, fila.y, fila.width, fila.height, 14, 14);
            }
            avatar(g, conv.contacto, c.x + 22, y + 33, 22);
            Telefono.Mensaje u = conv.ultimo();
            g.setFont(Tema.fuente(Font.BOLD, 13.5f));
            g.setColor(new Color(30, 24, 26));
            g.drawString(Dibujo.recortar(g.getFontMetrics(), conv.contacto, c.width - 120), c.x + 54, y + 26);
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(new Color(130, 110, 104));
            String h = u == null ? "" : u.hora;
            g.drawString(h, c.x + c.width - g.getFontMetrics().stringWidth(h), y + 25);
            g.setFont(Tema.fuente(conv.noLeidos > 0 ? Font.BOLD : Font.PLAIN, 12));
            g.setColor(conv.noLeidos > 0 ? new Color(48, 36, 34) : new Color(120, 100, 96));
            g.drawString(Dibujo.recortar(g.getFontMetrics(), u == null ? "" : (u.mio ? "Tú: " : "") + u.texto,
                    c.width - 90), c.x + 54, y + 46);
            if (conv.noLeidos > 0) {
                g.setColor(new Color(104, 160, 84));
                g.fill(new Ellipse2D.Double(c.x + c.width - 20, y + 33, 20, 20));
                g.setColor(Color.WHITE);
                g.setFont(Tema.fuente(Font.BOLD, 11));
                Dibujo.centrado(g, String.valueOf(conv.noLeidos), c.x + c.width - 10, y + 47);
            }
            g.setColor(new Color(0, 0, 0, 18));
            g.fillRect(c.x + 54, y + 65, c.width - 54, 1);
            final String n = conv.contacto;
            zonaVisible(fila, c, () -> {
                chat = n;
                scroll = 0;
                j.jugador.telefono.leer(n);
            });
            y += 66;
        }
        g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
        g.setColor(new Color(150, 130, 124));
        y += 18;
        g.drawString("Chats en una tabla hash: " + j.jugador.telefono.getChats().tamano() + " entradas, "
                + j.jugador.telefono.getChats().cantidadCubetas() + " cubetas.", c.x, y);
        return y + 10;
    }

    private void avatar(Graphics2D g, String nombre, double cx, double cy, double r) {
        Color[] cs = {new Color(122, 92, 153), Tema.COBRE, new Color(104, 160, 84), new Color(192, 71, 58),
            new Color(214, 150, 70), new Color(92, 140, 120)};
        Color c = cs[Math.abs(nombre.hashCode()) % cs.length];
        g.setColor(c);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        if (nombre.equals(Telefono.JEFE)) {
            Graphics2D f = (Graphics2D) g.create();
            f.clip(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            Caras.dibujar(f, Historia.caraDelJefe(v.semilla()), cx, cy + r * 0.2, r / 70, "neutral", false, 0);
            f.dispose();
            return;
        }
        String ini = nombre.replace("(", "").trim();
        StringBuilder sb = new StringBuilder();
        for (String p : ini.split(" ")) {
            if (!p.isEmpty() && sb.length() < 2 && Character.isLetter(p.charAt(0))) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
        }
        g.setColor(Color.WHITE);
        g.setFont(Tema.fuente(Font.BOLD, (float) (r * 0.75)));
        Dibujo.centrado(g, sb.toString(), cx, cy + r * 0.28);
    }

    private static final String[] RESPUESTAS = {"¿Y la fuente?", "Lo reviso y te cuento.", "Gracias por avisar."};

    private int conversacion(Graphics2D g, Rectangle c, int y, Jornada j) {
        Telefono.Conversacion conv = j.jugador.telefono.conversacion(chat);
        if (conv == null) {
            return y;
        }
        j.jugador.telefono.leer(chat);
        y += 8;
        for (Telefono.Mensaje m : conv.mensajes) {
            Font f = Tema.fuente(Font.PLAIN, 13);
            g.setFont(f);
            List<String> l = Avisos.partir(g, f, m.texto, c.width - 90);
            int ancho = 0;
            for (String s : l) {
                ancho = Math.max(ancho, g.getFontMetrics().stringWidth(s));
            }
            int bw = ancho + 26, bh = 18 + l.size() * 17;
            int bx = m.mio ? c.x + c.width - bw : c.x;
            g.setColor(m.mio ? new Color(200, 105, 58) : new Color(236, 228, 222));
            g.fillRoundRect(bx, y, bw, bh, 18, 18);
            g.setColor(m.mio ? Color.WHITE : new Color(40, 30, 28));
            int ly = y + 21;
            for (String s : l) {
                g.drawString(s, bx + 13, ly);
                ly += 17;
            }
            g.setFont(Tema.fuente(Font.PLAIN, 10));
            g.setColor(new Color(150, 130, 124));
            g.drawString(m.hora, m.mio ? bx - 34 : bx + bw + 6, y + bh - 4);
            y += bh + 10;
        }
        // respuestas rapidas
        y += 10;
        int x = c.x;
        for (String r : RESPUESTAS) {
            g.setFont(Tema.fuente(Font.BOLD, 12));
            int w = g.getFontMetrics().stringWidth(r) + 24;
            if (x + w > c.x + c.width) {
                x = c.x;
                y += 38;
            }
            Rectangle b = new Rectangle(x, y, w, 30);
            boolean hv = v.encima(b);
            g.setColor(hv ? new Color(122, 92, 153) : new Color(122, 92, 153, 30));
            g.fillRoundRect(b.x, b.y, b.width, b.height, 15, 15);
            g.setColor(hv ? Color.WHITE : new Color(122, 92, 153));
            g.drawString(r, b.x + 12, b.y + 20);
            final String txt = r;
            zonaVisible(b, c, () -> {
                j.jugador.telefono.responder(chat, txt, j.hora());
                Sonido.tocar(Sonido.Tipo.CLIC);
            });
            x += w + 8;
        }
        return y + 44;
    }

    // ------------------------------------------------------------------ Telefono
    private int telefono(Graphics2D g, Rectangle c, int y, Jornada j) {
        if (v.atendiendo() && j.puedeLlamar()) {
            Rectangle b = new Rectangle(c.x, y, c.width, 58);
            boolean hv = v.encima(b);
            g.setColor(hv ? new Color(84, 140, 70) : new Color(104, 160, 84));
            g.fillRoundRect(b.x, b.y, b.width, b.height, 18, 18);
            g.setColor(Color.WHITE);
            auricular(g, b.x + 28, b.y + 29, 11);
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.drawString("Llamar a la institución  (K)", b.x + 52, b.y + 26);
            g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
            g.drawString("Confirma si de verdad publicaron eso", b.x + 52, b.y + 44);
            zonaVisible(b, c, v::llamar);
            y += 72;
        }
        String ll = v.llamadaInst();
        if (ll != null) {
            g.setColor(new Color(104, 160, 84, 30));
            g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
            List<String> l = Avisos.partir(g, g.getFont(), ll, c.width - 24);
            g.fillRoundRect(c.x, y, c.width, 30 + l.size() * 17, 16, 16);
            g.setFont(Tema.fuente(Font.BOLD, 11.5f));
            g.setColor(new Color(66, 124, 58));
            g.drawString("ÚLTIMA LLAMADA", c.x + 12, y + 19);
            g.setColor(new Color(40, 30, 28));
            Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 12.5f), ll, c.x + 12, y + 37, c.width - 24, 17, Integer.MAX_VALUE);
            y += 40 + l.size() * 17;
        }
        Telefono tel = j.jugador.telefono;
        char letra = 0;
        for (Telefono.Contacto k : tel.contactos()) {
            char inicial = Character.toUpperCase(k.nombre.charAt(0));
            if (inicial != letra) {
                letra = inicial;
                y += 8;
                g.setFont(Tema.fuente(Font.BOLD, 12));
                g.setColor(new Color(122, 92, 153));
                g.drawString(String.valueOf(letra), c.x, y + 12);
                y += 18;
            }
            Rectangle fila = new Rectangle(c.x - 4, y + 2, c.width + 8, 42);
            if (v.encima(fila) && fila.intersects(c)) {
                g.setColor(new Color(122, 92, 153, 22));
                g.fillRoundRect(fila.x, fila.y, fila.width, fila.height, 14, 14);
            }
            avatar(g, k.nombre, c.x + 18, y + 22, 16);
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.setColor(new Color(30, 24, 26));
            g.drawString(Dibujo.recortar(g.getFontMetrics(), k.nombre, c.width - 84), c.x + 44, y + 19);
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(new Color(130, 110, 104));
            g.drawString(Dibujo.recortar(g.getFontMetrics(), k.detalle + "  ·  " + k.numero, c.width - 84), c.x + 44, y + 35);
            // boton verde de llamar
            double bx = c.x + c.width - 18, by = y + 22;
            g.setColor(new Color(104, 160, 84));
            g.fill(new Ellipse2D.Double(bx - 15, by - 15, 30, 30));
            g.setColor(Color.WHITE);
            auricular(g, bx, by, 7);
            final String quien = k.nombre;
            zonaVisible(fila, c, () -> v.llamarContacto(quien));
            y += 46;
        }
        y += 14;
        g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
        g.setColor(new Color(150, 130, 124));
        g.drawString("Agenda en un ABB: " + tel.getAgenda().getTamano() + " contactos, altura " + tel.getAgenda().altura()
                + ", inorden = orden alfabético.", c.x, y);
        g.drawString("Llamar a quien no tiene que ver con el caso: −5 de vida.", c.x, y + 15);
        return y + 27;
    }

    // ------------------------------------------------------------------ Noticias
    private int noticias(Graphics2D g, Rectangle c, int y, Jornada j) {
        Rectangle cab = new Rectangle(c.x, y, c.width, 72);
        g.setPaint(new GradientPaint(cab.x, cab.y, new Color(192, 71, 58), cab.x + cab.width, cab.y, new Color(110, 54, 33)));
        g.fillRoundRect(cab.x, cab.y, cab.width, cab.height, 18, 18);
        g.setFont(Tema.fuente(Font.BOLD, 19));
        g.setColor(Color.WHITE);
        g.drawString(Noticiero.CANAL, cab.x + 16, cab.y + 30);
        g.setFont(Tema.fuente(Font.PLAIN, 12));
        g.drawString("Con " + Noticiero.PRESENTADORA + " y " + Noticiero.REPORTERO, cab.x + 16, cab.y + 52);
        Rectangle rep = new Rectangle(cab.x + cab.width - 92, cab.y + 20, 78, 32);
        boolean hv = v.encima(rep);
        g.setColor(hv ? Color.WHITE : new Color(255, 255, 255, 50));
        g.fillRoundRect(rep.x, rep.y, rep.width, rep.height, 16, 16);
        g.setFont(Tema.fuente(Font.BOLD, 11.5f));
        g.setColor(hv ? new Color(192, 71, 58) : Color.WHITE);
        Path2D tri = new Path2D.Double();
        tri.moveTo(rep.x + 12, rep.y + 10);
        tri.lineTo(rep.x + 21, rep.y + 16);
        tri.lineTo(rep.x + 12, rep.y + 22);
        tri.closePath();
        g.fill(tri);
        g.drawString("Repetir", rep.x + 26, rep.y + 21);
        zonaVisible(rep, c, v::reverNoticiero);
        y += 92;
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(new Color(48, 36, 34));
        g.drawString("Hoy en la ciudad", c.x, y);
        y += 10;
        List<String[]> l = j.getNoticias();
        if (l.isEmpty()) {
            y += 24;
            g.setFont(Tema.fuente(Font.ITALIC, 12.5f));
            g.setColor(new Color(130, 110, 104));
            g.drawString("Aún no hay noticias de última hora.", c.x, y);
        }
        for (String[] n : l) {
            y += 14;
            g.setFont(Tema.fuente(Font.BOLD, 11));
            g.setColor(new Color(192, 71, 58));
            g.drawString(n[0], c.x, y + 12);
            g.setColor(new Color(40, 30, 28));
            y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 12.5f), n[1], c.x + 48, y + 12, c.width - 48, 17, Integer.MAX_VALUE);
            g.setColor(new Color(0, 0, 0, 16));
            g.fillRect(c.x, y, c.width, 1);
        }
        y += 26;
        g.setFont(Tema.fuente(Font.BOLD, 13));
        g.setColor(new Color(48, 36, 34));
        g.drawString("Tendencias (ordenadas con shell sort)", c.x, y);
        y += 12;
        for (String[] t : Noticiero.tendencias(j.dia, v.semilla(), Math.max(0, j.avanceDia()))) {
            g.setFont(Tema.fuente(Font.BOLD, 12));
            y += 8;
            g.setColor(new Color(122, 92, 153, 26));
            g.fillRoundRect(c.x, y, c.width, 30, 15, 15);
            g.setColor(new Color(122, 92, 153));
            g.drawString(t[0], c.x + 12, y + 20);
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(new Color(130, 110, 104));
            g.drawString(t[1], c.x + c.width - 12 - g.getFontMetrics().stringWidth(t[1]), y + 20);
            y += 30;
        }
        return y + 16;
    }

    // ------------------------------------------------------------------ Habilidad
    private int habilidad(Graphics2D g, Rectangle c, int y, Jornada j) {
        RolJugador rr = j.jugador.rol;
        Dibujo.emblemaRol(g, rr, c.x + 30, y + 30, 28, true);
        g.setFont(Tema.fuente(Font.BOLD, 17));
        g.setColor(new Color(30, 24, 26));
        g.drawString(rr.habilidad, c.x + 70, y + 26);
        g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
        g.setColor(new Color(130, 110, 104));
        g.drawString("Rol " + rr.nombre + " · una vez por cliente", c.x + 70, y + 46);
        y += 82;
        g.setColor(new Color(48, 36, 34));
        y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 13), rr.descripcionHabilidad, c.x, y, c.width, 19, Integer.MAX_VALUE) + 10;
        if (!rr.tieneMiniJuego()) {
            g.setColor(new Color(130, 110, 104));
            return Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 12.5f), "Como periodista no tienes mini-juego: tienes TODAS las apps "
                    + "de investigación, incluidos el corresponsal, el escáner de red y las llamadas.", c.x, y, c.width, 18, Integer.MAX_VALUE);
        }
        Caso caso = v.caso();
        if (caso == null) {
            g.setColor(new Color(130, 110, 104));
            return Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 12.5f), "Espera a que llegue alguien a la ventanilla.", c.x, y,
                    c.width, 18, Integer.MAX_VALUE);
        }
        if (caso.habilidadUsada) {
            g.setFont(Tema.fuente(Font.BOLD, 12.5f));
            g.setColor(new Color(170, 80, 42));
            g.drawString("RESULTADO", c.x, y + 8);
            return lineas(g, caso.pistasHabilidad, c.x, y + 32, c.width);
        }
        if (v.atendiendo()) {
            Rectangle b = new Rectangle(c.x, y + 6, c.width, 54);
            Dibujo.boton(g, b, "¡Usar mi habilidad!  (U)", Dibujo.Estilo.PRINCIPAL, v.encima(b), true);
            zonaVisible(b, c, v::usarHabilidad);
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(new Color(130, 110, 104));
            y = Dibujo.parrafo(g, g.getFont(), "El reloj se detiene mientras juegas; al terminar se cobran unos minutos.",
                    c.x, y + 84, c.width, 16, Integer.MAX_VALUE);
        }
        return y;
    }

    // ------------------------------------------------------------------ herramientas
    private static String descripcion(Herramienta h) {
        switch (h) {
            case PERFIL: return "Antigüedad de la cuenta, seguidores, fuente citada y señales de alerta.";
            case DIRECTORIO: return "¿La cuenta es oficial? Búsqueda O(log n) en el árbol AVL de cuentas oficiales.";
            case LUPA: return "Compara la cara del visitante con la foto de su credencial y señala la diferencia.";
            case IMAGEN: return "¿La foto ya existía en otro lugar o en otra fecha?";
            case CORRESPONSAL: return "El corresponsal va al barrio por la ruta más corta (Dijkstra) y te escribe lo que ve.";
            default: return "¿Quién lo compartió primero? BFS por oleadas, DFS y Bellman-Ford en el grafo social.";
        }
    }

    private static String accion(Herramienta h) {
        switch (h) {
            case PERFIL: return "Escanear perfil";
            case DIRECTORIO: return "Buscar en el directorio";
            case LUPA: return "Abrir la lupa";
            case IMAGEN: return "Búsqueda inversa";
            case CORRESPONSAL: return "Enviar corresponsal";
            default: return "Escanear la red";
        }
    }

    private int herramienta(Graphics2D g, Rectangle c, int y, Jornada j, Herramienta h) {
        g.setColor(new Color(110, 90, 84));
        y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 12.5f), descripcion(h), c.x, y + 6, c.width, 18, Integer.MAX_VALUE) + 8;
        if (j.enApagon()) {
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.setColor(new Color(176, 58, 50));
            g.drawString("Sin señal: hay un apagón.", c.x, y + 20);
            return y + 40;
        }
        Caso caso = v.caso();
        if (h == Herramienta.CORRESPONSAL) {
            // el mapa siempre se ve (aunque no haya caso)
            Rectangle m = new Rectangle(c.x, y, c.width, 300);
            Graphics2D s = (Graphics2D) g.create();
            s.clip(new RoundRectangle2D.Double(m.x, m.y, m.width, m.height, 16, 16));
            v.mapa(s, m);
            s.dispose();
            y += 312;
        }
        if (caso == null) {
            g.setFont(Tema.fuente(Font.ITALIC, 13));
            g.setColor(new Color(130, 110, 104));
            g.drawString("Esperando a que llegue alguien...", c.x, y + 16);
            return h == Herramienta.CORRESPONSAL ? distancias(g, c, y + 36, j) : y + 30;
        }
        if (v.escaneando() == h) {
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.setColor(new Color(48, 36, 34));
            g.drawString(h == Herramienta.DIRECTORIO ? "Buscando en el AVL..." : h == Herramienta.RED
                    ? "Recorriendo el grafo..." : "Analizando...", c.x, y + 20);
            g.setColor(new Color(200, 105, 58, 50));
            g.fillRoundRect(c.x, y + 32, c.width, 14, 14, 14);
            g.setColor(new Color(200, 105, 58));
            g.fillRoundRect(c.x, y + 32, (int) (c.width * Math.min(1, v.escaneoT())), 14, 14, 14);
            return y + 60;
        }
        List<String> res = j.resultados(h);
        String no = res == null && j.getActual() != null ? j.motivoNoAplica(h) : null;
        if (no != null) {
            g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
            List<String> l = Avisos.partir(g, g.getFont(), no, c.width - 50);
            int hh = 40 + l.size() * 17;
            g.setColor(new Color(192, 71, 58, 30));
            g.fillRoundRect(c.x, y, c.width, hh, 16, 16);
            Iconos.marca(g, 3, c.x + 20, y + 22, 8);
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.setColor(new Color(176, 58, 50));
            g.drawString("No aplica con este cliente", c.x + 38, y + 27);
            g.setColor(new Color(48, 36, 34));
            Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 12.5f), no, c.x + 38, y + 47, c.width - 50, 17, Integer.MAX_VALUE);
            return y + hh + 10;
        }
        if (res == null || h == Herramienta.CORRESPONSAL && !j.corresponsalEnCamino() && !j.corresponsalListo()) {
            if (v.atendiendo() && (res == null || h != Herramienta.CORRESPONSAL)) {
                Rectangle b = new Rectangle(c.x, y + 4, c.width, 52);
                Dibujo.boton(g, b, accion(h) + "  (" + "QWERTY".charAt(h.ordinal()) + ")", Dibujo.Estilo.PRINCIPAL,
                        v.encima(b), true);
                zonaVisible(b, c, () -> v.usarHerramienta(h));
                g.setFont(Tema.fuente(Font.PLAIN, 11));
                g.setColor(new Color(130, 110, 104));
                Dibujo.centrado(g, "Cuesta minutos del reloj", c.getCenterX(), y + 76);
                y += 90;
            }
            return h == Herramienta.CORRESPONSAL ? distancias(g, c, y, j) : y;
        }
        if (h == Herramienta.RED) {
            Rectangle r3 = new Rectangle(c.x, y, c.width, 170);
            Graphics2D s = (Graphics2D) g.create();
            s.clip(new RoundRectangle2D.Double(r3.x, r3.y, r3.width, r3.height, 16, 16));
            v.red3d().dibujar(s, r3, j.jugador.ciudad, caso.origen, v.t);
            s.dispose();
            y += 182;
        }
        y = lineas(g, res, c.x, y + 14, c.width);
        if (h == Herramienta.DIRECTORIO) {
            Rectangle ar = new Rectangle(c.x, y + 4, c.width, 120);
            g.setColor(new Color(34, 18, 14));
            g.fillRoundRect(ar.x, ar.y, ar.width, ar.height, 16, 16);
            Cuenta cu = caso.pub.getCuenta();
            VistaArboles.avl(g, j.jugador.directorio, cu == null ? null : cu.getArroba(),
                    new Rectangle(ar.x + 4, ar.y + 8, ar.width - 8, ar.height - 14), true);
            y += 132;
            if (v.atendiendo() && j.puedeLlamar()) {
                Rectangle b = new Rectangle(c.x, y + 4, c.width, 46);
                Dibujo.boton(g, b, "Llamar a la institución  (K)", Dibujo.Estilo.EXITO, v.encima(b), true);
                zonaVisible(b, c, v::llamar);
                y += 58;
            }
        }
        if (h == Herramienta.LUPA && v.atendiendo() && caso.visitante.credencial != null) {
            Rectangle b = new Rectangle(c.x, y + 6, c.width, 48);
            Dibujo.boton(g, b, "Comparar la foto  (L)", Dibujo.Estilo.LILA, v.encima(b), true);
            zonaVisible(b, c, v::abrirLupa);
            y += 62;
        }
        if (h == Herramienta.CORRESPONSAL) {
            if (j.corresponsalEnCamino()) {
                g.setColor(new Color(200, 105, 58, 50));
                g.fillRoundRect(c.x, y + 4, c.width, 12, 12, 12);
                g.setColor(new Color(200, 105, 58));
                g.fillRoundRect(c.x, y + 4, (int) (c.width * j.avanceCorresponsal()), 12, 12, 12);
                g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
                g.setColor(new Color(110, 90, 84));
                g.drawString("La moto va en camino: te escribirá al llegar.", c.x, y + 34);
                y += 44;
            }
            y = distancias(g, c, y, j);
        }
        return y;
    }

    /** Minutos desde la oficina a cada barrio (fila de la matriz de Floyd-Warshall). */
    private int distancias(Graphics2D g, Rectangle c, int y, Jornada j) {
        CiudadEstado ci = j.jugador.ciudad;
        y += 16;
        g.setFont(Tema.fuente(Font.BOLD, 12.5f));
        g.setColor(new Color(48, 36, 34));
        g.drawString("Minutos en moto desde tu oficina", c.x, y);
        g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
        g.setColor(new Color(150, 130, 124));
        y += 15;
        g.drawString("(fila de la matriz de Floyd-Warshall)", c.x, y);
        y += 6;
        for (int z = 0; z < CiudadEstado.ZONAS.length; z++) {
            if (z == 1) {
                continue;
            }
            y += 20;
            g.setFont(Tema.fuente(Font.PLAIN, 12));
            g.setColor(new Color(48, 36, 34));
            g.drawString(CiudadEstado.ZONAS[z], c.x, y);
            String m = Math.max(15, ci.distancias[1][z] / 7) + " min";
            g.drawString(m, c.x + c.width - g.getFontMetrics().stringWidth(m), y);
        }
        return y + 14;
    }
}
