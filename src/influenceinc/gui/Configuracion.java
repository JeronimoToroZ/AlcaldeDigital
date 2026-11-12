package influenceinc.gui;

import influenceinc.modelo.Herramienta;
import influenceinc.modelo.RolJugador;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * JUGADORES, NOMBRES Y ROLES. Antes de cualquier partida (solo, local o en
 * red) el juego SIEMPRE pide el nombre y el rol de cada jugador: del rol
 * depende su habilidad unica. No se puede empezar hasta que todos tengan
 * nombre y rol, y en una misma partida local cada rol es de un solo jugador.
 */
public class Configuracion extends Lienzo {

    public enum Modo { SOLO, LOCAL, CREAR_RED, UNIRSE_RED }

    /** Lo que se escogio. */
    public interface AlTerminar {
        void listo(Modo modo, List<String> nombres, List<RolJugador> roles, String ip);

        void volver();
    }

    private final AlTerminar alTerminar;
    private Modo modo = Modo.SOLO;
    private int cantidad = 1;
    private final Campo[] nombres = new Campo[4];
    private final RolJugador[] roles = new RolJugador[4];
    private final Campo ip = new Campo(40);
    private String error;
    private double errorT;

    public Configuracion(AlTerminar alTerminar) {
        super(1240, 860, true);
        this.alTerminar = alTerminar;
        for (int i = 0; i < 4; i++) {
            nombres[i] = new Campo(16);
            nombres[i].sugerencia = "Escribe el nombre del jugador " + (i + 1);
            nombres[i].alEnter = this::confirmar;
        }
        ip.sugerencia = "Ej.: 192.168.0.12";
        ip.poner("127.0.0.1");
        ip.alEnter = this::confirmar;
    }

    /** Prepara la pantalla para un modo de juego (se limpia lo anterior). */
    public void preparar(Modo m) {
        modo = m;
        cantidad = m == Modo.LOCAL ? 2 : 1;
        for (int i = 0; i < 4; i++) {
            nombres[i].poner("");
            roles[i] = null;
        }
        error = null;
        reconstruirCampos();
        enfocado = nombres[0];
    }

    private void reconstruirCampos() {
        campos.clear();
        for (int i = 0; i < cantidad; i++) {
            campos.add(nombres[i]);
        }
        if (modo == Modo.UNIRSE_RED) {
            campos.add(ip);
        }
        if (enfocado != null && !campos.contains(enfocado)) {
            enfocado = campos.get(0);
        }
    }

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
        int w = lw;
        // encabezado
        Rectangle volver = new Rectangle(28, 26, 130, 42);
        boton(g, volver, "‹  Volver", Dibujo.Estilo.SECUNDARIO, alTerminar::volver);
        g.setFont(Tema.fuente(Font.BOLD, 34));
        g.setColor(Tema.tinta());
        Dibujo.centrado(g, cantidad == 1 ? "¿Quién va a jugar?" : "¿Quiénes van a jugar?", w / 2.0, 62);
        g.setFont(Tema.fuente(Font.PLAIN, 15));
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(96, 58, 44));
        String sub;
        switch (modo) {
            case LOCAL: sub = "Partida local por turnos: cada jugador escribe su nombre y escoge un rol distinto."; break;
            case CREAR_RED: sub = "Partida en red: escribe tu nombre y tu rol. Luego los demás se unirán con tu IP."; break;
            case UNIRSE_RED: sub = "Unirse a una partida en red: tu nombre, tu rol y la IP del anfitrión."; break;
            default: sub = "Escribe tu nombre y escoge tu rol: de él depende tu habilidad única en la partida."; break;
        }
        Dibujo.centrado(g, sub, w / 2.0, 90);

        int top = 112;
        if (modo == Modo.LOCAL) {
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.setColor(Tema.tinta());
            int bx = w / 2 - 170;
            g.drawString("Número de jugadores:", bx, top + 27);
            for (int k = 2; k <= 4; k++) {
                Rectangle r = new Rectangle(bx + 170 + (k - 2) * 58, top + 4, 48, 36);
                final int kk = k;
                boton(g, r, String.valueOf(k), k == cantidad ? Dibujo.Estilo.PRINCIPAL : Dibujo.Estilo.SECUNDARIO, () -> {
                    cantidad = kk;
                    for (int i = cantidad; i < 4; i++) {
                        roles[i] = null;
                    }
                    reconstruirCampos();
                });
            }
            top += 52;
        }

        // tarjetas de jugadores
        int gap = 20;
        int alto = modo == Modo.UNIRSE_RED ? 556 : 590;
        int ancho = Math.min(cantidad == 1 ? 560 : 340, (w - 60 - gap * (cantidad - 1)) / cantidad);
        int total = ancho * cantidad + gap * (cantidad - 1);
        int x0 = (w - total) / 2;
        if (modo == Modo.LOCAL) {
            alto -= 30;
        }
        for (int i = 0; i < cantidad; i++) {
            tarjetaJugador(g, i, new Rectangle(x0 + i * (ancho + gap), top + 10, ancho, alto));
        }
        int y = top + 10 + alto + 18;
        if (modo == Modo.UNIRSE_RED) {
            campo(g, ip, new Rectangle(w / 2 - 300, y + 18, 290, 44), "IP del anfitrión");
            y += 0;
            boton(g, new Rectangle(w / 2 + 20, y + 18, 280, 46), "Unirme a la partida", Dibujo.Estilo.PRINCIPAL,
                    this::confirmar);
        } else {
            String txt = modo == Modo.CREAR_RED ? "Crear la sala" : "¡A jugar!";
            boton(g, new Rectangle(w / 2 - 150, y, 300, 52), txt, Dibujo.Estilo.PRINCIPAL, this::confirmar);
        }
        // mensaje de validacion
        if (error != null && t - errorT < 4) {
            g.setFont(Tema.fuente(Font.BOLD, 14));
            int ew = g.getFontMetrics().stringWidth(error) + 40;
            Rectangle er = new Rectangle(w / 2 - ew / 2, lh - 50, ew, 34);
            g.setColor(Tema.altoContraste ? Color.BLACK : Tema.mal());
            g.fillRoundRect(er.x, er.y, er.width, er.height, 17, 17);
            Iconos.marca(g, 1, er.x + 18, er.y + 17, 7);
            g.setColor(Tema.altoContraste ? Color.YELLOW : Tema.CREMA);
            g.drawString(error, er.x + 30, er.y + 23);
        }
    }

    private void tarjetaJugador(Graphics2D g, int i, Rectangle r) {
        Dibujo.tarjetaPapel(g, r, 24);
        RolJugador rol = roles[i];
        // cabecera con el emblema del rol escogido
        Color barra = rol == null ? Tema.ROSA : Dibujo.colorRol(rol);
        g.setColor(Tema.altoContraste ? Color.DARK_GRAY : Tema.alfa(barra, 60));
        g.fill(new RoundRectangle2D.Double(r.x + 8, r.y + 8, r.width - 16, 64, 18, 18));
        if (rol != null) {
            Dibujo.emblemaRol(g, rol, r.x + 44, r.y + 40, 24, true);
        } else {
            g.setColor(Tema.altoContraste ? Color.WHITE : Tema.alfa(Tema.TINTA_SUAVE, 120));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{5, 5}, 0));
            g.drawOval(r.x + 20, r.y + 16, 48, 48);
            g.setStroke(new BasicStroke(1));
            g.setFont(Tema.fuente(Font.BOLD, 22));
            Dibujo.centrado(g, "?", r.x + 44, r.y + 48);
        }
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(Tema.tinta());
        String nom = nombres[i].valor().isEmpty() ? "Jugador " + (i + 1) : nombres[i].valor();
        g.drawString(Dibujo.recortar(g.getFontMetrics(), nom, r.width - 100), r.x + 80, r.y + 36);
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(Tema.tintaSuave());
        g.drawString(rol == null ? "Sin rol todavía" : rol.nombre, r.x + 80, r.y + 57);

        campo(g, nombres[i], new Rectangle(r.x + 18, r.y + 104, r.width - 36, 44), "Nombre");

        g.setFont(Tema.fuente(Font.BOLD, 12.5f));
        g.setColor(Tema.tintaSuave());
        g.drawString("Rol (define tu habilidad)", r.x + 22, r.y + 172);
        RolJugador[] todos = RolJugador.values();
        int cw = (r.width - 36 - 10) / 2, ch = 70;
        for (int k = 0; k < todos.length; k++) {
            RolJugador rr = todos[k];
            Rectangle c = new Rectangle(r.x + 18 + (k % 2) * (cw + 10), r.y + 180 + (k / 2) * (ch + 10), cw, ch);
            int duenio = duenio(rr);
            boolean mio = rol == rr, ocupado = duenio >= 0 && duenio != i;
            boolean hv = encima(c) && !ocupado;
            Color base = Dibujo.colorRol(rr);
            RoundRectangle2D s = new RoundRectangle2D.Double(c.x, c.y, c.width, c.height, 16, 16);
            if (Tema.altoContraste) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(mio ? Tema.alfa(base, 70) : hv ? Tema.ROSA_CLARO : new Color(255, 255, 255, ocupado ? 60 : 160));
            }
            g.fill(s);
            g.setColor(mio ? base : Tema.altoContraste ? Color.WHITE : new Color(200, 160, 140));
            g.setStroke(new BasicStroke(mio ? 3f : 1.2f));
            g.draw(s);
            g.setStroke(new BasicStroke(1));
            java.awt.Composite antes = g.getComposite();
            if (ocupado) {
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f));
            }
            Dibujo.emblemaRol(g, rr, c.x + 28, c.y + c.height / 2.0, 18, true);
            String corto = rr == RolJugador.CANDIDATO ? "Candidato" : rr.nombre;
            Font f = Dibujo.ajustar(g, corto, Font.BOLD, 13.5f, c.width - 62);
            g.setFont(f);
            g.setColor(Tema.tinta());
            g.drawString(corto, c.x + 54, c.y + 30);
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(Tema.tintaSuave());
            String det = ocupado ? "Lo tiene " + nombreDe(duenio) : rr.habilidad;
            g.drawString(Dibujo.recortar(g.getFontMetrics(), det, c.width - 60), c.x + 54, c.y + 49);
            g.setComposite(antes);
            if (mio) {
                Iconos.marca(g, 2, c.x + c.width - 14, c.y + 14, 7);
            }
            if (!ocupado) {
                final int ii = i;
                zona(c, () -> roles[ii] = rr);
            }
        }
        // descripcion de la habilidad
        int y = r.y + 180 + 2 * (ch + 10) + 14;
        g.setColor(Tema.altoContraste ? Color.DARK_GRAY : new Color(255, 255, 255, 140));
        Rectangle d = new Rectangle(r.x + 18, y, r.width - 36, r.y + r.height - 18 - y);
        g.fillRoundRect(d.x, d.y, d.width, d.height, 16, 16);
        if (rol == null) {
            g.setFont(Tema.fuente(Font.ITALIC, 13));
            g.setColor(Tema.tintaSuave());
            Dibujo.parrafo(g, g.getFont(), "Escoge un rol para ver su habilidad única y sus herramientas.", d.x + 14,
                    d.y + 26, d.width - 28, 18, d.y + d.height - 8);
            return;
        }
        g.setFont(Tema.fuente(Font.BOLD, 14));
        g.setColor(Dibujo.colorRol(rol).darker());
        g.drawString("Habilidad: " + rol.habilidad, d.x + 14, d.y + 24);
        g.setColor(Tema.tinta());
        int yy = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 12.5f), rol.descripcionHabilidad, d.x + 14, d.y + 44,
                d.width - 28, 17, d.y + d.height - 40);
        g.setColor(Tema.tintaSuave());
        yy = Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 12), rol.mision, d.x + 14, yy + 2, d.width - 28, 16,
                d.y + d.height - 26);
        StringBuilder hs = new StringBuilder();
        for (Herramienta h : Herramienta.values()) {
            if (rol.puedeUsar(h)) {
                hs.append(hs.length() == 0 ? "" : " · ").append(h.nombre);
            }
        }
        g.setColor(Tema.tinta());
        Dibujo.parrafo(g, Tema.fuente(Font.BOLD, 11), "Herramientas: " + hs, d.x + 14, Math.max(yy + 4, d.y + d.height - 30),
                d.width - 28, 14, d.y + d.height - 4);
    }

    private int duenio(RolJugador r) {
        if (modo != Modo.LOCAL) {
            return -1;
        }
        for (int i = 0; i < cantidad; i++) {
            if (roles[i] == r) {
                return i;
            }
        }
        return -1;
    }

    private String nombreDe(int i) {
        String n = nombres[i].valor();
        return n.isEmpty() ? "el jugador " + (i + 1) : n;
    }

    private void confirmar() {
        List<String> ns = new ArrayList<>();
        List<RolJugador> rs = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            String n = nombres[i].valor();
            if (n.isEmpty()) {
                avisar("Falta el nombre del jugador " + (i + 1) + ".");
                enfocado = nombres[i];
                return;
            }
            if (ns.contains(n)) {
                avisar("Dos jugadores no pueden llamarse igual (" + n + ").");
                enfocado = nombres[i];
                return;
            }
            if (roles[i] == null) {
                avisar(n + " todavía no ha escogido su rol.");
                return;
            }
            ns.add(n);
            rs.add(roles[i]);
        }
        if (modo == Modo.UNIRSE_RED && ip.valor().isEmpty()) {
            avisar("Escribe la IP del anfitrión.");
            enfocado = ip;
            return;
        }
        Sonido.tocar(Sonido.Tipo.TURNO);
        alTerminar.listo(modo, ns, rs, ip.valor());
    }

    private void avisar(String s) {
        error = s;
        errorT = t;
        Sonido.tocar(Sonido.Tipo.ALERTA);
    }

    /** Para las pruebas y capturas: llena los datos sin teclado. */
    public void llenar(List<String> ns, List<RolJugador> rs) {
        cantidad = ns.size();
        reconstruirCampos();
        for (int i = 0; i < ns.size(); i++) {
            nombres[i].poner(ns.get(i));
            roles[i] = rs.get(i);
        }
    }

    @Override
    protected void tecla(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            alTerminar.volver();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            confirmar();
        }
    }
}
