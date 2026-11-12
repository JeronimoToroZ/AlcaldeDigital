package influenceinc.gui;

import influenceinc.logica.Historia;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Partida;
import influenceinc.modelo.RolJugador;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * EL FINAL: la eleccion del alcalde y lo que paso con Ciudad Nova segun las
 * decisiones de cada jugador (puntaje individual + resultado de la ciudad).
 */
public class PantallaFinal extends Lienzo {

    private final Runnable alMenu;
    private Partida partida;
    private List<String[]> marcador = new ArrayList<>();
    private final List<double[]> confeti = new ArrayList<>();

    public PantallaFinal(Runnable alMenu) {
        super(1280, 860, true);
        this.alMenu = alMenu;
    }

    public void mostrar(Partida p, List<String[]> marcador) {
        this.partida = p;
        setMarcador(marcador);
        confeti.clear();
        Random r = new Random();
        for (int i = 0; i < 90; i++) {
            confeti.add(new double[]{r.nextDouble(), -r.nextDouble() * 1.2, 0.08 + r.nextDouble() * 0.12,
                r.nextInt(4), r.nextDouble() * 6});
        }
    }

    public void setMarcador(List<String[]> m) {
        this.marcador = new ArrayList<>(m);
        repaint();
    }

    @Override
    protected void fondoVentana(Graphics2D g, int w, int h) {
        g.setColor(Tema.altoContraste ? Color.BLACK : Tema.ROSA);
        g.fillRect(0, 0, w, h);
        if (!Tema.altoContraste) {
            Recursos.cubrir(g, "fondo2.jpg", 0, 0, w, h);
        }
    }

    // ------------------------------------------------------------------ eleccion
    /** {nombre, porcentaje, esJugador(1/0)} de cada candidato. */
    private List<Object[]> eleccion() {
        List<Object[]> l = new ArrayList<>();
        int desinf = 0, salud = 0;
        for (JugadorVerificador j : partida.jugadores) {
            desinf += j.ciudad.desinformacion();
            salud += j.ciudad.salud();
        }
        desinf /= partida.jugadores.size();
        salud /= partida.jugadores.size();
        double[] votos = new double[6];
        String[] nombres = new String[6];
        int n = 0;
        for (JugadorVerificador j : partida.jugadores) {
            if (j.rol == RolJugador.CANDIDATO) {
                nombres[n] = j.nombre;
                votos[n++] = j.apoyoElectoral();
            }
        }
        // los rivales de la ciudad: uno gana con propuestas, el otro con rumores
        nombres[n] = "Marta Gómez (propuestas)";
        votos[n++] = 15 + salud * 0.35;
        nombres[n] = "Juan Pérez (rumores)";
        votos[n++] = 10 + desinf * 0.9;
        double total = 0;
        for (int i = 0; i < n; i++) {
            total += Math.max(1, votos[i]);
        }
        for (int i = 0; i < n; i++) {
            l.add(new Object[]{nombres[i], (int) Math.round(100 * Math.max(1, votos[i]) / total), i < n - 2 ? 1 : 0});
        }
        l.sort((a, b) -> Integer.compare((Integer) b[1], (Integer) a[1]));
        return l;
    }

    // ------------------------------------------------------------------ dibujo
    @Override
    protected void pintar(Graphics2D g, double dt) {
        if (partida == null) {
            return;
        }
        int w = lw, h = lh;
        g.setFont(Tema.fuente(Font.BOLD, 34));
        g.setColor(Tema.tinta());
        Dibujo.centrado(g, "Resultados de Ciudad Nova", w / 2.0, 58);
        g.setFont(Tema.fuente(Font.PLAIN, 15));
        g.setColor(Tema.altoContraste ? Color.WHITE : new Color(96, 58, 44));
        Dibujo.centrado(g, "Cada decisión en Civitas cambió la ciudad. Así terminó la semana electoral.", w / 2.0, 86);

        // eleccion del alcalde
        List<Object[]> el = eleccion();
        Rectangle er = new Rectangle(40, 104, w - 80, 128);
        Dibujo.tarjetaOscura(g, er, 22);
        g.setFont(Tema.fuente(Font.BOLD, 17));
        g.setColor(Tema.acento2());
        g.drawString("ELECCIÓN DEL ALCALDE", er.x + 24, er.y + 32);
        g.setFont(Tema.fuente(Font.PLAIN, 13));
        g.setColor(Tema.texto());
        String gan = (String) el.get(0)[0];
        g.drawString(Dibujo.recortar(g.getFontMetrics(), "Ganó " + gan + " con el " + el.get(0)[1] + " % de los votos.  "
                + Historia.eleccion(partida.ranking().get(0)), er.width - 290), er.x + 260, er.y + 31);
        int bx = er.x + 24, by = er.y + 50, bw = er.width - 48;
        int col = Math.min(el.size(), 4);
        int cw = (bw - (col - 1) * 14) / col;
        for (int i = 0; i < col; i++) {
            Object[] c = el.get(i);
            Rectangle cr = new Rectangle(bx + i * (cw + 14), by, cw, 62);
            g.setColor(Tema.altoContraste ? Color.BLACK : new Color(255, 235, 215, 25));
            g.fillRoundRect(cr.x, cr.y, cr.width, cr.height, 14, 14);
            boolean jugador = (Integer) c[2] == 1;
            g.setFont(Tema.fuente(Font.BOLD, 13.5f));
            g.setColor(Tema.texto());
            g.drawString(Dibujo.recortar(g.getFontMetrics(), (i == 0 ? "1.º  " : "") + c[0], cr.width - 70), cr.x + 12, cr.y + 22);
            g.setFont(Tema.fuente(Font.BOLD, 18));
            g.setColor(i == 0 ? Tema.acento2() : Tema.textoSuave());
            String pc = c[1] + " %";
            g.drawString(pc, cr.x + cr.width - 12 - g.getFontMetrics().stringWidth(pc), cr.y + 24);
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRoundRect(cr.x + 12, cr.y + 36, cr.width - 24, 12, 12, 12);
            int fw = (int) ((cr.width - 24) * Math.min(1, ((Integer) c[1]) / 100.0 * Math.min(1, t / 1.5)));
            g.setColor(jugador ? Tema.acento() : i == 0 ? Tema.acento2() : Tema.neutro());
            g.fillRoundRect(cr.x + 12, cr.y + 36, fw, 12, 12, 12);
            if (jugador) {
                g.setFont(Tema.fuente(Font.PLAIN, 10.5f));
                g.setColor(Tema.textoSuave());
                g.drawString("jugador", cr.x + 12, cr.y + 60);
            }
        }

        // tarjetas de cada jugador
        List<JugadorVerificador> rk = partida.ranking();
        int n = rk.size();
        int gap = 18;
        int top = 248, alto = h - top - 96;
        int ancho = (w - 80 - gap * (n - 1)) / n;
        for (int i = 0; i < n; i++) {
            tarjeta(g, rk.get(i), i, n, new Rectangle(40 + i * (ancho + gap), top, ancho, alto));
        }
        if (!marcador.isEmpty()) {
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.setColor(Tema.tinta());
            StringBuilder sb = new StringBuilder("Marcador en red:   ");
            List<String[]> m = new ArrayList<>(marcador);
            m.sort((a, b) -> Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1])));
            for (String[] f : m) {
                sb.append(f[0]).append(" ").append(f[1]).append(f[5].equals("1") ? " (terminó)" : " (día " + f[2] + ")")
                        .append("     ");
            }
            g.drawString(Dibujo.recortar(g.getFontMetrics(), sb.toString(), w - 400), 44, h - 62);
        }
        boton(g, new Rectangle(w - 300, h - 84, 260, 52), "Volver al menú", Dibujo.Estilo.PRINCIPAL, alMenu);
        confeti(g, w, h, dt);
    }

    private void tarjeta(Graphics2D g, JugadorVerificador j, int i, int n, Rectangle r) {
        String[] fin = Historia.finalPara(j);
        boolean malo = fin[0].equals("DESPEDIDO") || fin[0].contains("ENGAÑADA");
        boolean medio = fin[0].contains("DIVIDIDA");
        Dibujo.tarjetaPapel(g, r, 24);
        Color c = malo ? Tema.mal() : medio ? Tema.acento2() : Tema.bien();
        g.setColor(c);
        g.fill(new RoundRectangle2D.Double(r.x, r.y, r.width, 8, 8, 8));
        Dibujo.emblemaRol(g, j.rol, r.x + 40, r.y + 46, 24, true);
        g.setFont(Tema.fuente(Font.BOLD, 18));
        g.setColor(Tema.tinta());
        g.drawString(Dibujo.recortar(g.getFontMetrics(), (n > 1 ? "#" + (i + 1) + "  " : "") + j.nombre, r.width - 90),
                r.x + 76, r.y + 42);
        g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
        g.setColor(Tema.tintaSuave());
        g.drawString(j.rol.nombre + "  ·  puntaje final " + j.puntajeFinal(), r.x + 76, r.y + 62);
        int y = r.y + 100;
        Font ft = Dibujo.ajustar(g, fin[0], Font.BOLD, n > 2 ? 21 : 28, r.width - 40);
        g.setFont(ft);
        g.setColor(malo ? Tema.mal() : Tema.acento());
        g.drawString(fin[0], r.x + 20, y);
        y += 14;
        g.setColor(Tema.tinta());
        int yMax = r.y + r.height - 150;
        y = Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, n > 2 ? 12.5f : 14), fin[1], r.x + 20, y + 14, r.width - 40,
                n > 2 ? 17 : 20, yMax);
        y += 8;
        // logros (recompensas) en el espacio libre
        int limite = r.y + r.height - 210;
        if (!j.logros.isEmpty() && y < limite - 30) {
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.setColor(Tema.acento());
            g.drawString("Logros", r.x + 20, y + 6);
            y += 26;
            g.setFont(Tema.fuente(Font.PLAIN, 12));
            for (String lg : j.logros) {
                if (y > limite) {
                    break;
                }
                Iconos.marca(g, 4, r.x + 28, y - 4, 6);
                g.setColor(Tema.tinta());
                g.drawString(Dibujo.recortar(g.getFontMetrics(), lg, r.width - 70), r.x + 42, y);
                y += 19;
            }
        }
        // estadisticas en dos columnas
        String[][] st = {
            {"Casos", j.casosAtendidos + " (" + j.aciertos + " bien)"},
            {"Impostores", String.valueOf(j.impostoresAtrapados)},
            {"Habilidad", j.habilidadesBien + "/" + j.habilidadesUsadas + " bien"},
            {"Alertas virales", j.viralesBien + " frenadas"},
            {"Tienda", j.compras.isEmpty() ? "sin compras" : j.compras.size() + " compras ($" + j.gastado + ")"},
            {"Desinformación", j.ciudad.desinformacion() + " %"},
            {"Confianza", String.valueOf(j.ciudad.confianza)},
            {"Salud ciudad", String.valueOf(j.ciudad.salud())},
            {"Dinero final", "$" + j.dinero},
            {j.rol == RolJugador.CANDIDATO ? "Intención de voto" : "Logros",
                j.rol == RolJugador.CANDIDATO ? j.apoyoElectoral() + " %" : String.valueOf(j.logros.size())}
        };
        int sy = Math.max(y, r.y + r.height - 194);
        int colw = (r.width - 40) / 2;
        for (int k = 0; k < st.length; k++) {
            int x = r.x + 20 + (k % 2) * colw, yy = sy + (k / 2) * 34;
            g.setFont(Tema.fuente(Font.PLAIN, 11));
            g.setColor(Tema.tintaSuave());
            g.drawString(st[k][0], x, yy);
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.setColor(Tema.tinta());
            g.drawString(st[k][1], x, yy + 17);
        }
    }

    /** Efecto al ganar: confeti con los colores de la paleta (solo si la ciudad termino bien). */
    private void confeti(Graphics2D g, int w, int h, double dt) {
        if (Tema.altoContraste || partida == null) {
            return;
        }
        String fin = Historia.finalPara(partida.ranking().get(0))[0];
        if (fin.equals("DESPEDIDO") || fin.contains("ENGAÑADA") || fin.contains("DIVIDIDA")) {
            return;
        }
        Color[] cs = {Tema.COBRE, Tema.DORADO, Tema.LILA, Tema.SALVIA};
        for (double[] p : confeti) {
            p[1] += p[2] * dt;
            p[4] += dt * 3;
            if (p[1] > 1.1) {
                continue;
            }
            double x = p[0] * w + Math.sin(p[4]) * 14, y = p[1] * h;
            g.setColor(cs[(int) p[3]]);
            Graphics2D s = (Graphics2D) g.create();
            s.translate(x, y);
            s.rotate(p[4]);
            s.fill(p[3] % 2 == 0 ? new Rectangle(-5, -3, 10, 6) : new Ellipse2D.Double(-4, -4, 8, 8));
            s.dispose();
        }
        g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 0), 0, 1, new Color(0, 0, 0, 0)));
    }
}
