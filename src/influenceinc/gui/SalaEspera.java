package influenceinc.gui;

import influenceinc.modelo.RolJugador;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/**
 * SALA DE ESPERA de la partida en red (cliente-servidor con sockets).
 * Muestra la IP del anfitrion y quienes estan conectados, con su rol.
 */
public class SalaEspera extends Lienzo {

    private final Runnable alEmpezar, alCancelar;
    private boolean anfitrion;
    private String ip = "";
    private int puerto;
    private String estado = "Conectando...";
    private List<String> conectados = new ArrayList<>();

    public SalaEspera(Runnable alEmpezar, Runnable alCancelar) {
        super(1100, 860, true);
        this.alEmpezar = alEmpezar;
        this.alCancelar = alCancelar;
    }

    public void preparar(boolean anfitrion, String ip, int puerto) {
        this.anfitrion = anfitrion;
        this.ip = ip;
        this.puerto = puerto;
        this.estado = "Conectando...";
        this.conectados = new ArrayList<>();
    }

    public void setConectados(List<String> l) {
        conectados = new ArrayList<>(l);
        estado = anfitrion ? "Cuando estén todos, empieza la partida." : "Esperando a que el anfitrión empiece...";
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
        int w = lw, h = lh;
        Rectangle r = new Rectangle(w / 2 - 330, h / 2 - 300, 660, 600);
        Dibujo.tarjetaPapel(g, r, 28);
        Dibujo.tituloTarjeta(g, "Sala de espera", r.x + 36, r.y + 58, Tema.lila(), Tema.tinta(), 30);
        g.setFont(Tema.fuente(Font.PLAIN, 15));
        g.setColor(Tema.tintaSuave());
        g.drawString(estado, r.x + 50, r.y + 90);
        if (anfitrion) {
            Rectangle ipr = new Rectangle(r.x + 36, r.y + 112, r.width - 72, 76);
            g.setColor(Tema.altoContraste ? Color.BLACK : Tema.alfa(Tema.LILA, 40));
            g.fillRoundRect(ipr.x, ipr.y, ipr.width, ipr.height, 18, 18);
            g.setFont(Tema.fuente(Font.PLAIN, 13));
            g.setColor(Tema.tintaSuave());
            g.drawString("Los demás se unen escribiendo esta IP:", ipr.x + 20, ipr.y + 28);
            g.setFont(Tema.fuente(Font.BOLD, 24));
            g.setColor(Tema.tinta());
            g.drawString(ip + "   (puerto " + puerto + ")", ipr.x + 20, ipr.y + 60);
        }
        int y = r.y + (anfitrion ? 222 : 130);
        g.setFont(Tema.fuente(Font.BOLD, 15));
        g.setColor(Tema.tinta());
        g.drawString("Conectados (" + conectados.size() + ")", r.x + 40, y);
        y += 16;
        for (String n : conectados) {
            Rectangle f = new Rectangle(r.x + 36, y, r.width - 72, 52);
            g.setColor(Tema.altoContraste ? Color.BLACK : new Color(255, 255, 255, 170));
            g.fillRoundRect(f.x, f.y, f.width, f.height, 16, 16);
            RolJugador rol = rolDe(n);
            if (rol != null) {
                Dibujo.emblemaRol(g, rol, f.x + 30, f.getCenterY(), 18, true);
            } else {
                g.setColor(Tema.ROSA);
                g.fill(new Ellipse2D.Double(f.x + 12, f.y + 8, 36, 36));
            }
            g.setFont(Tema.fuente(Font.BOLD, 16));
            g.setColor(Tema.tinta());
            g.drawString(n, f.x + 60, f.y + 32);
            y += 60;
            if (y > r.y + r.height - 140) {
                break;
            }
        }
        // puntos animados mientras se espera
        for (int k = 0; k < 3; k++) {
            double a = 0.5 + 0.5 * Math.sin(t * 4 - k * 0.7);
            g.setColor(Tema.alfa(Tema.acento(), (int) (80 + 170 * a)));
            g.fill(new Ellipse2D.Double(r.x + r.width / 2.0 - 30 + k * 24, r.y + r.height - 108, 12, 12));
        }
        int by = r.y + r.height - 74;
        if (anfitrion) {
            boton(g, new Rectangle(r.x + 60, by, 300, 50), "Empezar la partida", Dibujo.Estilo.EXITO, alEmpezar);
            boton(g, new Rectangle(r.x + 380, by, 220, 50), "Cancelar", Dibujo.Estilo.SECUNDARIO, alCancelar);
        } else {
            boton(g, new Rectangle(r.x + r.width / 2 - 110, by, 220, 50), "Cancelar", Dibujo.Estilo.SECUNDARIO, alCancelar);
        }
    }

    /** Los nombres llegan como "Ana (Periodista)". */
    private static RolJugador rolDe(String n) {
        int a = n.lastIndexOf('('), b = n.lastIndexOf(')');
        if (a >= 0 && b > a) {
            String r = n.substring(a + 1, b).trim();
            for (RolJugador x : RolJugador.values()) {
                if (x.nombre.equalsIgnoreCase(r) || x.name().equalsIgnoreCase(r)) {
                    return x;
                }
            }
        }
        return null;
    }
}
