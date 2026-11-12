package influenceinc.gui;

import influenceinc.logica.Habilidades;
import influenceinc.logica.Jornada;
import influenceinc.modelo.RolJugador;
import influenceinc.modelo.Veredicto;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Las MECANICAS INTERACTIVAS de cada rol y la ALERTA VIRAL. Cada una es un
 * mini-juego distinto (nada de "presionar un boton y ya"):
 *
 *  - Cadena (Ciudadano): sube por la cadena de reenvios del grafo social
 *    escogiendo, en cada paso, quien te lo mando (misma huella, hora anterior).
 *  - Cazabots (Influencer): perfiles aparecen y desaparecen; atrapa solo las
 *    cuentas falsas en 12 segundos.
 *  - Rueda de prensa (Candidato): 3 preguntas con 10 segundos cada una;
 *    ARRASTRA la respuesta al microfono.
 *  - Alerta viral (todos): 10 segundos para arrastrar un sello sobre la
 *    publicacion viral (verificar, reportar, ignorar) o compartirla.
 */
abstract class MiniJuego {

    /** El area de la tarjeta del mini-juego (en el lienzo de la oficina). */
    static final Rectangle AREA = new Rectangle(190, 84, 1060, 762);

    final OficinaVista v;
    final Jornada j;
    boolean terminado;
    boolean exito;
    double tiempo;

    MiniJuego(OficinaVista v) {
        this.v = v;
        this.j = v.jornada();
    }

    abstract String titulo();

    abstract String subtitulo();

    abstract void actualizar(double dt);

    abstract void dibujar(Graphics2D g);

    /** @return true si consumio el clic */
    boolean presionar(int x, int y) {
        return false;
    }

    void arrastrar(int x, int y) {
    }

    void soltar(int x, int y) {
    }

    void tecla(KeyEvent e) {
    }

    /** Lo que pasa al cerrar (minutos, pistas...). */
    abstract void cerrar();

    /** Marco comun: velo, tarjeta de papel, emblema y titulo. */
    void marco(Graphics2D g, RolJugador rol, double restante, double total) {
        g.setColor(new Color(20, 8, 4, 175));
        g.fillRect(0, 64, OficinaVista.W, OficinaVista.H - 64);
        Rectangle a = AREA;
        Dibujo.tarjetaPapel(g, a, 30);
        g.setColor(Tema.altoContraste ? Color.DARK_GRAY : Tema.alfa(rol == null ? Tema.LADRILLO : Dibujo.colorRol(rol), 45));
        g.fill(new RoundRectangle2D.Double(a.x + 10, a.y + 10, a.width - 20, 76, 22, 22));
        if (rol != null) {
            Dibujo.emblemaRol(g, rol, a.x + 52, a.y + 48, 28, true);
        } else {
            Recursos.contener(g, "reportarr.png", a.x + 22, a.y + 18, 60, 60);
        }
        g.setFont(Tema.fuente(Font.BOLD, 24));
        g.setColor(Tema.tinta());
        g.drawString(titulo(), a.x + 96, a.y + 44);
        g.setFont(Tema.fuente(Font.PLAIN, 13.5f));
        g.setColor(Tema.tintaSuave());
        g.drawString(Dibujo.recortar(g.getFontMetrics(), subtitulo(), a.width - 330), a.x + 96, a.y + 68);
        if (total > 0) {
            reloj(g, a.x + a.width - 120, a.y + 48, restante, total);
        }
    }

    /** Reloj circular con la cuenta regresiva. */
    static void reloj(Graphics2D g, double cx, double cy, double restante, double total) {
        double k = Math.max(0, Math.min(1, restante / total));
        boolean urge = restante < 3.5;
        g.setColor(Tema.altoContraste ? Color.BLACK : Tema.PAPEL);
        g.fill(new Ellipse2D.Double(cx - 30, cy - 30, 60, 60));
        g.setColor(Tema.altoContraste ? Color.DARK_GRAY : Tema.ROSA_CLARO);
        g.setStroke(new BasicStroke(7));
        g.draw(new Ellipse2D.Double(cx - 26, cy - 26, 52, 52));
        g.setColor(urge ? Tema.mal() : Tema.acento());
        g.draw(new java.awt.geom.Arc2D.Double(cx - 26, cy - 26, 52, 52, 90, 360 * k, java.awt.geom.Arc2D.OPEN));
        g.setStroke(new BasicStroke(1));
        g.setFont(Tema.fuente(Font.BOLD, 19));
        g.setColor(urge ? Tema.mal() : Tema.tinta());
        Dibujo.centrado(g, String.valueOf((int) Math.ceil(Math.max(0, restante))), cx, cy + 7);
    }

    /** Tarjeta clara con borde (opcion clicable). */
    static void opcion(Graphics2D g, Rectangle r, boolean hover, Color borde, boolean tachada) {
        RoundRectangle2D s = new RoundRectangle2D.Double(r.x, r.y - (hover ? 2 : 0), r.width, r.height, 18, 18);
        if (!Tema.altoContraste) {
            g.setColor(new Color(60, 30, 20, hover ? 50 : 28));
            g.fill(new RoundRectangle2D.Double(r.x + 1, r.y + 4, r.width, r.height, 18, 18));
        }
        g.setColor(Tema.altoContraste ? Color.BLACK : tachada ? new Color(236, 218, 210) : hover ? Color.WHITE
                : new Color(255, 251, 246));
        g.fill(s);
        g.setColor(Tema.altoContraste ? (hover ? Color.YELLOW : Color.WHITE) : borde);
        g.setStroke(new BasicStroke(hover ? 2.6f : 1.5f));
        g.draw(s);
        g.setStroke(new BasicStroke(1));
    }
}

/** Contenedor de los mini-juegos concretos. */
final class MiniJuegos {

    private MiniJuegos() {
    }

    // ==================================================================
    // CIUDADANO: ¿Quien te lo mando?
    // ==================================================================
    static final class Cadena extends MiniJuego {
        private final Habilidades.Cadena c;
        private int paso;
        private int errores;
        private final List<Integer> tachadas = new ArrayList<>();
        private double sacudir;
        private boolean resumen;

        Cadena(OficinaVista v) {
            super(v);
            c = j.prepararCadena();
        }

        @Override
        String titulo() {
            return "¿Quién te lo mandó?";
        }

        @Override
        String subtitulo() {
            return "Rastrea la cadena de reenvíos en el grafo social hasta la cuenta que la inventó.";
        }

        private int n() {
            return c.camino.size();
        }

        /** Nombre de quien recibio el mensaje en este paso. */
        private String actual() {
            return paso == 0 ? "Tú" : Habilidades.nombreVisible(c.camino.get(n() - paso));
        }

        private String horaActual() {
            return paso == 0 ? c.horaTu : c.horas.get(n() - paso);
        }

        @Override
        void actualizar(double dt) {
            tiempo += dt;
            sacudir = Math.max(0, sacudir - dt);
        }

        @Override
        void dibujar(Graphics2D g) {
            marco(g, RolJugador.CIUDADANO, 0, 0);
            Rectangle a = AREA;
            // ---- la cadena (izquierda): de abajo (Tu) hacia arriba (origen)
            Rectangle izq = new Rectangle(a.x + 30, a.y + 104, 300, a.height - 134);
            g.setColor(Tema.altoContraste ? Color.BLACK : new Color(255, 255, 255, 150));
            g.fillRoundRect(izq.x, izq.y, izq.width, izq.height, 22, 22);
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.setColor(Tema.tintaSuave());
            g.drawString("LA CADENA (grafo social)", izq.x + 18, izq.y + 28);
            int encontrados = Math.min(paso, n());
            int total = encontrados + 2;
            double gapY = Math.min(96, (izq.height - 90) / (double) Math.max(1, total - 1));
            double nx = izq.x + 62, ny = izq.y + izq.height - 44;
            List<double[]> pts = new ArrayList<>();
            List<String> etiquetas = new ArrayList<>();
            pts.add(new double[]{nx, ny});
            etiquetas.add("Tú · " + c.horaTu);
            for (int k = 0; k < encontrados; k++) {
                pts.add(new double[]{nx + (k % 2 == 0 ? 22 : 0), ny - (k + 1) * gapY});
                int idx = n() - 1 - k;
                etiquetas.add(Habilidades.nombreVisible(c.camino.get(idx)) + " · " + c.horas.get(idx));
            }
            boolean listo = resumen;
            if (!listo) {
                pts.add(new double[]{nx, ny - (encontrados + 1) * gapY});
                etiquetas.add("?");
            }
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{2, 7}, (float) ((tiempo * 20) % 9)));
            g.setColor(Tema.altoContraste ? Color.WHITE : Tema.COBRE);
            for (int k = 0; k + 1 < pts.size(); k++) {
                g.draw(new java.awt.geom.Line2D.Double(pts.get(k)[0], pts.get(k)[1], pts.get(k + 1)[0], pts.get(k + 1)[1]));
            }
            g.setStroke(new BasicStroke(1));
            for (int k = 0; k < pts.size(); k++) {
                double[] p = pts.get(k);
                boolean incog = etiquetas.get(k).equals("?");
                boolean origen = listo && k == pts.size() - 1;
                Color col = k == 0 ? Tema.SALVIA : incog ? Tema.ROSA : origen ? Tema.LADRILLO : Tema.LILA;
                if (Tema.altoContraste) {
                    col = Color.YELLOW;
                }
                g.setColor(col);
                double rr = incog ? 20 + 3 * Math.sin(tiempo * 4) : 20;
                g.fill(new Ellipse2D.Double(p[0] - rr, p[1] - rr, 2 * rr, 2 * rr));
                g.setColor(Tema.altoContraste ? Color.BLACK : Tema.CREMA);
                g.setFont(Tema.fuente(Font.BOLD, 15));
                String ini = incog ? "?" : k == 0 ? "Tú" : etiquetas.get(k).substring(0, 1);
                Dibujo.centrado(g, ini, p[0], p[1] + 5);
                if (!incog) {
                    g.setFont(Tema.fuente(Font.PLAIN, 11.5f));
                    g.setColor(Tema.tinta());
                    String et = Dibujo.recortar(g.getFontMetrics(), etiquetas.get(k), (int) (izq.x + izq.width - p[0] - 36));
                    double tx = p[0] + 28;
                    g.drawString(et, (float) tx, (float) p[1] + 4);
                }
            }
            // ---- la pregunta (derecha)
            int rx = a.x + 360, ry = a.y + 104, rw = a.width - 390;
            if (resumen) {
                dibujarResumen(g, new Rectangle(rx, ry, rw, a.height - 134));
                return;
            }
            double dx = sacudir > 0 ? Math.sin(sacudir * 60) * 8 * sacudir : 0;
            g.setFont(Tema.fuente(Font.BOLD, 20));
            g.setColor(Tema.tinta());
            g.drawString("¿Quién le envió esto a " + actual() + "?", (float) (rx + dx), ry + 26);
            // el mensaje recibido
            Rectangle msg = new Rectangle(rx, ry + 42, rw, 92);
            g.setColor(Tema.altoContraste ? Color.BLACK : Tema.alfa(Tema.LILA, 30));
            g.fillRoundRect(msg.x, msg.y, msg.width, msg.height, 18, 18);
            g.setFont(Tema.fuente(Font.BOLD, 12.5f));
            g.setColor(Tema.lila());
            g.drawString(actual() + " lo recibió a las " + horaActual() + "  ·  huella del mensaje " + c.huella,
                    msg.x + 16, msg.y + 24);
            g.setColor(Tema.tinta());
            Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 13), "\"" + v.caso().pub.getTexto() + "\"", msg.x + 16, msg.y + 46,
                    msg.width - 32, 18, msg.y + msg.height - 8);
            // opciones
            List<Habilidades.Eslabon> op = c.pasos.get(paso);
            int oy = msg.y + msg.height + 16;
            int oh = Math.min(78, (a.y + a.height - 120 - oy) / op.size() - 10);
            for (int i = 0; i < op.size(); i++) {
                Habilidades.Eslabon e = op.get(i);
                Rectangle r = new Rectangle(rx, oy + i * (oh + 10), rw, oh);
                boolean tach = tachadas.contains(i);
                boolean hv = !tach && v.encima(r);
                opcion(g, r, hv, e.nadie ? Tema.acento() : new Color(200, 160, 140), tach);
                if (e.nadie) {
                    g.setFont(Tema.fuente(Font.BOLD, 15));
                    g.setColor(Tema.acento());
                    g.drawString(e.nombre, r.x + 22, r.y + oh / 2 + 6);
                } else {
                    g.setColor(tach ? Tema.ROSA : Tema.LILA_CLARO);
                    g.fill(new Ellipse2D.Double(r.x + 14, r.y + oh / 2.0 - 20, 40, 40));
                    g.setFont(Tema.fuente(Font.BOLD, 16));
                    g.setColor(Tema.TINTA);
                    Dibujo.centrado(g, e.nombre.substring(0, 1), r.x + 34, r.y + oh / 2.0 + 6);
                    g.setFont(Tema.fuente(Font.BOLD, 15));
                    g.setColor(Tema.tinta());
                    g.drawString(e.nombre, r.x + 66, r.y + oh / 2 - 4);
                    g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
                    g.setColor(Tema.tintaSuave());
                    g.drawString("Lo compartió a las " + e.hora + "   ·   huella " + e.huella, r.x + 66, r.y + oh / 2 + 16);
                }
                if (tach) {
                    Iconos.marca(g, 3, r.x + r.width - 24, r.getCenterY(), 9);
                }
                if (!tach) {
                    final int ii = i;
                    v.zona(r, () -> elegir(ii));
                }
            }
            g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
            g.setColor(Tema.tintaSuave());
            Dibujo.parrafo(g, g.getFont(), "Pista: quien te lo mandó tiene la MISMA huella y lo compartió ANTES. "
                    + "Si nadie lo tenía antes, llegaste al origen.  Errores: " + errores + " (cada uno cuesta 2 minutos)",
                    rx, a.y + a.height - 40, rw, 16, a.y + a.height - 8);
        }

        private void dibujarResumen(Graphics2D g, Rectangle r) {
            boolean ok = errores <= 2;
            g.setFont(Tema.fuente(Font.BOLD, 26));
            g.setColor(ok ? Tema.bien() : Tema.mal());
            g.drawString(ok ? "¡Encontraste el origen!" : "Llegaste, pero con muchos tropiezos", r.x, r.y + 34);
            influenceinc.estructuras.Vertice o = c.camino.isEmpty() ? null : c.camino.get(0);
            g.setFont(Tema.fuente(Font.PLAIN, 15));
            g.setColor(Tema.tinta());
            int y = r.y + 76;
            y = Dibujo.parrafo(g, g.getFont(), "La publicación empezó en " + Habilidades.nombreVisible(o) + " ("
                    + (o == null ? "?" : o.tipo.getEtiqueta()) + ") a las " + c.horas.get(0) + " y dio " + n()
                    + " saltos hasta llegarte.", r.x, y, r.width, 22, r.y + r.height);
            y += 10;
            g.setColor(Tema.tintaSuave());
            y = Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 13.5f), "Cómo lo hiciste: es un recorrido por el GRAFO SOCIAL. "
                    + "El juego guardó, con BFS, el PADRE de cada persona; subir por los padres desde ti lleva al origen.",
                    r.x, y, r.width, 20, r.y + r.height);
            y += 10;
            g.setColor(Tema.tinta());
            Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 14), "Errores: " + errores + "  ·  tiempo de juego usado: "
                    + (6 + 2 * errores) + " min", r.x, y, r.width, 20, r.y + r.height);
            v.boton(g, new Rectangle(r.x, r.y + r.height - 70, 320, 52), "Anotar en la libreta", Dibujo.Estilo.PRINCIPAL,
                    () -> terminado = true);
        }

        private void elegir(int i) {
            Habilidades.Eslabon e = c.pasos.get(paso).get(i);
            if (e.correcto) {
                Sonido.tocar(Sonido.Tipo.BIEN);
                tachadas.clear();
                paso++;
                if (paso >= c.pasos.size()) {
                    resumen = true;
                    exito = errores <= 2;
                }
            } else {
                Sonido.tocar(Sonido.Tipo.MAL);
                tachadas.add(i);
                errores++;
                sacudir = 0.5;
            }
        }

        @Override
        void tecla(KeyEvent e) {
            int k = e.getKeyCode() - KeyEvent.VK_1;
            if (!resumen && k >= 0 && k < c.pasos.get(paso).size() && !tachadas.contains(k)) {
                elegir(k);
            } else if (resumen && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE)) {
                terminado = true;
            }
        }

        @Override
        void cerrar() {
            List<String> p = j.terminarHabilidad(exito, 6 + 2 * errores);
            v.anotarHabilidad(p, exito);
        }
    }

    // ==================================================================
    // INFLUENCER: Cazabots
    // ==================================================================
    static final class Cazabots extends MiniJuego {
        private static final double DURACION = 12;
        private final List<Habilidades.Perfil> perfiles;
        private final Habilidades.Perfil[] slot = new Habilidades.Perfil[12];
        private final double[] vida = new double[12];
        private final int[] marca = new int[12];       // 0 nada, 1 atrapado, 2 falso
        private final double[] marcaT = new double[12];
        private int siguiente, atrapados, falsos, escapados, botsVistos;
        private double spawn;
        private boolean empezado, fin;
        private final Random r = new Random();

        Cazabots(OficinaVista v) {
            super(v);
            perfiles = j.prepararPerfiles();
        }

        @Override
        String titulo() {
            return "Cazabots";
        }

        @Override
        String subtitulo() {
            return "Atrapa SOLO las cuentas falsas que empujan esta publicación entre tus seguidores.";
        }

        @Override
        void actualizar(double dt) {
            if (!empezado || fin) {
                return;
            }
            tiempo += dt;
            spawn -= dt;
            for (int i = 0; i < slot.length; i++) {
                if (marca[i] != 0) {
                    marcaT[i] -= dt;
                    if (marcaT[i] <= 0) {
                        marca[i] = 0;
                        slot[i] = null;
                    }
                } else if (slot[i] != null) {
                    vida[i] -= dt;
                    if (vida[i] <= 0) {
                        if (slot[i].bot) {
                            escapados++;
                        }
                        slot[i] = null;
                    }
                }
            }
            if (spawn <= 0 && tiempo < DURACION - 1 && !perfiles.isEmpty()) {
                spawn = 0.55 + r.nextDouble() * 0.35;
                List<Integer> libres = new ArrayList<>();
                for (int i = 0; i < slot.length; i++) {
                    if (slot[i] == null && marca[i] == 0) {
                        libres.add(i);
                    }
                }
                if (!libres.isEmpty()) {
                    int s = libres.get(r.nextInt(libres.size()));
                    slot[s] = perfiles.get(siguiente++ % perfiles.size());
                    vida[s] = 1.6 + r.nextDouble() * 0.6;
                    if (slot[s].bot) {
                        botsVistos++;
                    }
                }
            }
            if (tiempo >= DURACION) {
                fin = true;
                for (int i = 0; i < slot.length; i++) {
                    if (slot[i] != null && marca[i] == 0 && slot[i].bot) {
                        escapados++;
                    }
                }
                exito = botsVistos == 0 ? falsos <= 1 : atrapados >= Math.ceil(botsVistos * 0.6) && falsos <= 1;
                Sonido.tocar(exito ? Sonido.Tipo.BIEN : Sonido.Tipo.MAL);
            }
        }

        private Rectangle celda(int i) {
            Rectangle a = AREA;
            int cw = (a.width - 80 - 3 * 16) / 4, ch = 150;
            return new Rectangle(a.x + 40 + (i % 4) * (cw + 16), a.y + 168 + (i / 4) * (ch + 16), cw, ch);
        }

        @Override
        void dibujar(Graphics2D g) {
            marco(g, RolJugador.INFLUENCER, empezado ? Math.max(0, DURACION - tiempo) : DURACION, DURACION);
            Rectangle a = AREA;
            g.setFont(Tema.fuente(Font.BOLD, 15));
            g.setColor(Tema.tinta());
            g.drawString("Atrapadas: " + atrapados + "     Marcaste gente real: " + falsos + "     Se escaparon: " + escapados,
                    a.x + 40, a.y + 130);
            g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
            g.setColor(Tema.tintaSuave());
            g.drawString("Señales de cuenta falsa: creada hace pocos días, sin foto, cientos de publicaciones al día.",
                    a.x + 40, a.y + 152);
            for (int i = 0; i < slot.length; i++) {
                Rectangle c = celda(i);
                g.setColor(Tema.altoContraste ? Color.DARK_GRAY : new Color(238, 214, 205, 120));
                g.fillRoundRect(c.x, c.y, c.width, c.height, 18, 18);
                Habilidades.Perfil p = slot[i];
                if (p == null) {
                    continue;
                }
                double pop = marca[i] == 0 ? Math.min(1, (2.2 - vida[i]) * 6) : 1;
                Rectangle cr = new Rectangle((int) (c.getCenterX() - c.width * pop / 2), (int) (c.getCenterY() - c.height * pop / 2),
                        (int) (c.width * pop), (int) (c.height * pop));
                boolean hv = marca[i] == 0 && v.encima(c);
                opcion(g, cr, hv, marca[i] == 1 ? Tema.bien() : marca[i] == 2 ? Tema.mal() : new Color(200, 160, 140), false);
                if (pop < 0.6) {
                    continue;
                }
                // foto
                double fx = cr.x + 44, fy = cr.y + 46;
                g.setColor(Tema.altoContraste ? Color.DARK_GRAY : Tema.ROSA_CLARO);
                g.fill(new Ellipse2D.Double(fx - 28, fy - 28, 56, 56));
                if (p.cara != null) {
                    Graphics2D s = (Graphics2D) g.create();
                    s.clip(new Ellipse2D.Double(fx - 28, fy - 28, 56, 56));
                    Caras.dibujar(s, p.cara, fx, fy + 4, 0.42, "neutral", false, 0);
                    s.dispose();
                } else {            // huevo gris de cuenta sin foto
                    g.setColor(new Color(176, 150, 140));
                    g.fill(new Ellipse2D.Double(fx - 13, fy - 18, 26, 34));
                }
                g.setFont(Tema.fuente(Font.BOLD, 13));
                g.setColor(Tema.tinta());
                g.drawString(Dibujo.recortar(g.getFontMetrics(), p.nombre, cr.width - 90), cr.x + 82, cr.y + 40);
                g.setColor(Tema.tintaSuave());
                Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 11), p.detalle, cr.x + 14, cr.y + 92, cr.width - 28, 15, cr.y + cr.height - 6);
                // barra de vida
                if (marca[i] == 0) {
                    g.setColor(Tema.alfa(Tema.acento(), 160));
                    g.fillRoundRect(cr.x + 82, cr.y + 52, (int) ((cr.width - 100) * Math.max(0, vida[i] / 2.2)), 6, 6, 6);
                } else {
                    Iconos.marca(g, marca[i] == 1 ? 2 : 3, cr.x + cr.width - 22, cr.y + 22, 10);
                    g.setFont(Tema.fuente(Font.BOLD, 12));
                    g.setColor(marca[i] == 1 ? Tema.bien() : Tema.mal());
                    g.drawString(marca[i] == 1 ? "¡Bot atrapado!" : "¡Era una persona!", cr.x + 82, cr.y + 64);
                }
                if (marca[i] == 0 && !fin) {
                    final int ii = i;
                    v.zona(c, () -> golpear(ii));
                }
            }
            if (!empezado) {
                velo(g, "¿Listo? Tienes 12 segundos.", "Las tarjetas aparecen y desaparecen rápido. Haz clic SOLO en las cuentas "
                        + "falsas. Marcar a una persona real te quita puntos.", "¡Empezar!", () -> empezado = true);
            } else if (fin) {
                String t = exito ? "¡Buena cacería!" : "Se te complicó";
                String d = "Atrapaste " + atrapados + " de " + botsVistos + " cuentas falsas y marcaste " + falsos
                        + " personas reales. " + (botsVistos == 0 ? "Esta vez casi todo era gente real: bien por no marcarla. " : "")
                        + "Los perfiles salieron del BFS de 2 oleadas desde el origen en el grafo social.";
                velo(g, t, d, "Anotar en la libreta", () -> terminado = true);
            }
        }

        private void velo(Graphics2D g, String t, String d, String boton, Runnable a) {
            Rectangle area = AREA;
            Rectangle c = new Rectangle(area.x + area.width / 2 - 300, area.y + 250, 600, 250);
            g.setColor(new Color(42, 23, 18, 120));
            g.fillRoundRect(area.x + 10, area.y + 100, area.width - 20, area.height - 110, 22, 22);
            Dibujo.tarjetaPapel(g, c, 24);
            g.setFont(Tema.fuente(Font.BOLD, 24));
            g.setColor(Tema.tinta());
            Dibujo.centrado(g, t, c.getCenterX(), c.y + 50);
            g.setColor(Tema.tintaSuave());
            Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 14), d, c.x + 36, c.y + 84, c.width - 72, 20, c.y + c.height - 70);
            v.boton(g, new Rectangle(c.x + c.width / 2 - 140, c.y + c.height - 66, 280, 48), boton, Dibujo.Estilo.PRINCIPAL, a);
        }

        private void golpear(int i) {
            if (slot[i] == null || marca[i] != 0) {
                return;
            }
            if (slot[i].bot) {
                atrapados++;
                marca[i] = 1;
                Sonido.tocar(Sonido.Tipo.BIEN);
            } else {
                falsos++;
                marca[i] = 2;
                Sonido.tocar(Sonido.Tipo.MAL);
            }
            marcaT[i] = 0.7;
        }

        @Override
        void tecla(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (!empezado) {
                    empezado = true;
                } else if (fin) {
                    terminado = true;
                }
            }
        }

        @Override
        void cerrar() {
            List<String> p = j.terminarHabilidad(exito, 6);
            v.anotarHabilidad(p, exito);
        }
    }

    // ==================================================================
    // CANDIDATO: Rueda de prensa
    // ==================================================================
    static final class Prensa extends MiniJuego {
        private static final double POR_PREGUNTA = 10;
        private final List<Habilidades.Pregunta> preguntas;
        private int actual;
        private double restante = POR_PREGUNTA;
        private int responsables;
        private final List<Character> dadas = new ArrayList<>();
        private int arrastrando = -1;
        private int ax, ay, ox, oy;
        private double feedback;
        private char ultimo;
        private boolean fin;

        Prensa(OficinaVista v) {
            super(v);
            preguntas = j.prepararRueda();
        }

        @Override
        String titulo() {
            return "Rueda de prensa";
        }

        @Override
        String subtitulo() {
            return "Arrastra tu respuesta al micrófono. Lo responsable suma confianza y votos; atacar o repetir rumores, resta.";
        }

        private Rectangle microfono() {
            Rectangle a = AREA;
            return new Rectangle(a.x + a.width / 2 - 120, a.y + 300, 240, 170);
        }

        private Rectangle tarjeta(int i) {
            Rectangle a = AREA;
            int w = (a.width - 80 - 2 * 18) / 3;
            return new Rectangle(a.x + 40 + i * (w + 18), a.y + a.height - 200, w, 150);
        }

        @Override
        void actualizar(double dt) {
            tiempo += dt;
            if (fin) {
                return;
            }
            if (feedback > 0) {
                feedback -= dt;
                if (feedback <= 0) {
                    actual++;
                    restante = POR_PREGUNTA;
                    if (actual >= preguntas.size()) {
                        fin = true;
                        exito = responsables >= 2;
                        Sonido.tocar(exito ? Sonido.Tipo.LOGRO : Sonido.Tipo.MAL);
                    }
                }
                return;
            }
            restante -= dt;
            if (restante <= 0) {
                responder('E');
            }
        }

        private void responder(char tipo) {
            j.respuestaPrensa(tipo);
            dadas.add(tipo);
            ultimo = tipo;
            if (tipo == 'R') {
                responsables++;
                Sonido.tocar(Sonido.Tipo.BIEN);
            } else {
                Sonido.tocar(Sonido.Tipo.MAL);
            }
            feedback = 1.6;
            arrastrando = -1;
        }

        @Override
        void dibujar(Graphics2D g) {
            marco(g, RolJugador.CANDIDATO, fin ? 0 : restante, POR_PREGUNTA);
            Rectangle a = AREA;
            if (fin) {
                resumen(g);
                return;
            }
            Habilidades.Pregunta p = preguntas.get(Math.min(actual, preguntas.size() - 1));
            // periodistas
            for (int k = 0; k < 3; k++) {
                double px = a.x + 130 + k * 70, py = a.y + 170;
                influenceinc.modelo.Rasgos rs = influenceinc.modelo.Rasgos.azar(new Random(k * 97L + actual));
                Graphics2D s = (Graphics2D) g.create();
                s.clip(new Rectangle(a.x + 60, a.y + 100, 280, 150));
                Caras.dibujar(s, rs, px, py, 0.5, k == 1 ? "nervioso" : "neutral", false, k == 1 ? Math.abs(Math.sin(tiempo * 12)) : 0);
                s.dispose();
            }
            // globo de la pregunta
            Rectangle gl = new Rectangle(a.x + 360, a.y + 108, a.width - 400, 112);
            g.setColor(Tema.altoContraste ? Color.BLACK : Color.WHITE);
            g.fillRoundRect(gl.x, gl.y, gl.width, gl.height, 22, 22);
            Path2D cola = new Path2D.Double();
            cola.moveTo(gl.x, gl.y + 40);
            cola.lineTo(gl.x - 22, gl.y + 56);
            cola.lineTo(gl.x, gl.y + 64);
            g.fill(cola);
            g.setColor(Tema.lila());
            g.setFont(Tema.fuente(Font.BOLD, 12.5f));
            g.drawString("PREGUNTA " + (actual + 1) + " DE " + preguntas.size(), gl.x + 20, gl.y + 26);
            g.setColor(Tema.tinta());
            Dibujo.parrafo(g, Tema.media(18), p.texto, gl.x + 20, gl.y + 54, gl.width - 40, 24, gl.y + gl.height - 6);
            // microfono (zona de soltar)
            Rectangle m = microfono();
            boolean sobre = arrastrando >= 0 && m.contains(ax, ay);
            g.setColor(Tema.altoContraste ? Color.DARK_GRAY : Tema.alfa(sobre ? Tema.DORADO : Tema.ROSA, sobre ? 150 : 80));
            g.fillRoundRect(m.x, m.y, m.width, m.height, 26, 26);
            g.setColor(Tema.altoContraste ? Color.WHITE : Tema.COBRE);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{8, 8}, (float) ((tiempo * 20) % 16)));
            g.drawRoundRect(m.x, m.y, m.width, m.height, 26, 26);
            g.setStroke(new BasicStroke(1));
            Dibujo.emblemaRol(g, RolJugador.PERIODISTA, m.getCenterX(), m.y + 70, 40, true);
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.setColor(Tema.tinta());
            Dibujo.centrado(g, "Suelta aquí tu respuesta", m.getCenterX(), m.y + m.height - 22);
            if (feedback > 0) {
                boolean ok = ultimo == 'R';
                String t = ok ? "¡Respuesta responsable! +confianza" : ultimo == 'A' ? "Atacaste: más conflictos"
                        : ultimo == 'M' ? "Repetiste un rumor: más desinformación" : "Evadiste la pregunta";
                g.setFont(Tema.fuente(Font.BOLD, 18));
                int tw = g.getFontMetrics().stringWidth(t) + 56;
                Rectangle fr = new Rectangle((int) m.getCenterX() - tw / 2, m.y - 58, tw, 42);
                g.setColor(ok ? Tema.bien() : Tema.mal());
                g.fillRoundRect(fr.x, fr.y, fr.width, fr.height, 21, 21);
                Iconos.marca(g, ok ? 2 : 3, fr.x + 22, fr.getCenterY(), 9);
                g.setColor(Tema.CREMA);
                g.drawString(t, fr.x + 40, fr.y + 28);
            }
            // tarjetas de respuesta
            for (int i = 0; i < p.respuestas.size(); i++) {
                Rectangle r = tarjeta(i);
                if (i == arrastrando || feedback > 0) {
                    g.setColor(Tema.altoContraste ? Color.DARK_GRAY : new Color(238, 214, 205, 110));
                    g.fillRoundRect(r.x, r.y, r.width, r.height, 18, 18);
                    if (i == arrastrando) {
                        continue;
                    }
                    if (feedback > 0) {
                        continue;
                    }
                }
                respuesta(g, r, p.respuestas.get(i).texto, i, v.encima(r));
            }
            if (arrastrando >= 0 && feedback <= 0) {
                Rectangle r0 = tarjeta(arrastrando);
                Graphics2D s = (Graphics2D) g.create();
                s.rotate(Math.toRadians(-3), ax, ay);
                respuesta(s, new Rectangle(ax - ox, ay - oy, r0.width, r0.height), p.respuestas.get(arrastrando).texto,
                        arrastrando, true);
                s.dispose();
            }
            g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
            g.setColor(Tema.tintaSuave());
            Dibujo.centrado(g, "Arrastra una tarjeta hasta el micrófono (o pulsa 1, 2 o 3).", a.getCenterX(), a.y + a.height - 22);
        }

        private void respuesta(Graphics2D g, Rectangle r, String txt, int i, boolean hv) {
            opcion(g, r, hv, new Color(200, 160, 140), false);
            g.setColor(Tema.COBRE);
            g.fill(new Ellipse2D.Double(r.x + 14, r.y + 14, 26, 26));
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.setColor(Tema.CREMA);
            Dibujo.centrado(g, String.valueOf(i + 1), r.x + 27, r.y + 32);
            g.setColor(Tema.tinta());
            Dibujo.parrafo(g, Tema.media(14.5f), "\"" + txt + "\"", r.x + 18, r.y + 64, r.width - 36, 20, r.y + r.height - 8);
        }

        private void resumen(Graphics2D g) {
            Rectangle a = AREA;
            g.setFont(Tema.fuente(Font.BOLD, 28));
            g.setColor(exito ? Tema.bien() : Tema.mal());
            Dibujo.centrado(g, exito ? "¡La rueda de prensa salió bien!" : "La rueda de prensa se te salió de las manos",
                    a.getCenterX(), a.y + 170);
            g.setFont(Tema.fuente(Font.PLAIN, 16));
            g.setColor(Tema.tinta());
            Dibujo.centrado(g, "Respuestas responsables: " + responsables + " de " + preguntas.size()
                    + "   ·   apoyo electoral acumulado: " + v.jornada().jugador.apoyo, a.getCenterX(), a.y + 214);
            int x = a.x + a.width / 2 - 270;
            for (int i = 0; i < dadas.size(); i++) {
                char c = dadas.get(i);
                Rectangle r = new Rectangle(x + i * 190, a.y + 260, 170, 120);
                opcion(g, r, false, c == 'R' ? Tema.bien() : Tema.mal(), false);
                Iconos.marca(g, c == 'R' ? 2 : 3, r.getCenterX(), r.y + 36, 14);
                g.setFont(Tema.fuente(Font.BOLD, 14));
                g.setColor(Tema.tinta());
                Dibujo.centrado(g, c == 'R' ? "Responsable" : c == 'A' ? "Ataque" : c == 'M' ? "Rumor" : "Evasiva",
                        r.getCenterX(), r.y + 82);
            }
            g.setColor(Tema.tintaSuave());
            Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 14), "Un candidato que responde con datos y sin atacar construye "
                    + "confianza. Cada respuesta cambió los indicadores de la ciudad (confianza, conflictos, convivencia o "
                    + "desinformación) y tu intención de voto para la elección final.", a.x + 120, a.y + 430, a.width - 240, 21,
                    a.y + a.height - 120);
            v.boton(g, new Rectangle((int) a.getCenterX() - 160, a.y + a.height - 100, 320, 52), "Anotar en la libreta",
                    Dibujo.Estilo.PRINCIPAL, () -> terminado = true);
        }

        @Override
        boolean presionar(int x, int y) {
            if (fin || feedback > 0) {
                return false;
            }
            for (int i = 0; i < 3; i++) {
                Rectangle r = tarjeta(i);
                if (r.contains(x, y)) {
                    arrastrando = i;
                    ox = x - r.x;
                    oy = y - r.y;
                    ax = x;
                    ay = y;
                    return true;
                }
            }
            return false;
        }

        @Override
        void arrastrar(int x, int y) {
            ax = x;
            ay = y;
        }

        @Override
        void soltar(int x, int y) {
            if (arrastrando < 0) {
                return;
            }
            Rectangle r = tarjeta(arrastrando);
            boolean clic = Math.abs(x - (r.x + ox)) < 6 && Math.abs(y - (r.y + oy)) < 6;
            if (microfono().contains(x, y)) {
                responder(preguntas.get(actual).respuestas.get(arrastrando).tipo);
            } else if (clic) {
                // un clic tambien sirve (accesibilidad): la tarjeta "vuela" al microfono
                responder(preguntas.get(actual).respuestas.get(arrastrando).tipo);
            }
            arrastrando = -1;
        }

        @Override
        void tecla(KeyEvent e) {
            int k = e.getKeyCode() - KeyEvent.VK_1;
            if (!fin && feedback <= 0 && k >= 0 && k < 3) {
                responder(preguntas.get(actual).respuestas.get(k).tipo);
            } else if (fin && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE)) {
                terminado = true;
            }
        }

        @Override
        void cerrar() {
            List<String> p = j.terminarHabilidad(exito, 8);
            v.anotarHabilidad(p, exito);
        }
    }

    // ==================================================================
    // ALERTA VIRAL: decision con 10 segundos
    // ==================================================================
    static final class Viral extends MiniJuego {
        private static final double DURACION = 10;
        private final Habilidades.Viral viral;
        private double restante = DURACION;
        private boolean fin;
        private String mensaje = "";
        private int puntos;
        private int arrastrando = -1;
        private int ax, ay;
        private static final Veredicto[] SELLOS = {Veredicto.PUBLICAR, Veredicto.DENUNCIAR, Veredicto.RECHAZAR};
        private static final String[] NOMBRES = {"VERIFICAR", "REPORTAR", "IGNORAR"};

        Viral(OficinaVista v, Habilidades.Viral viral) {
            super(v);
            this.viral = viral;
        }

        @Override
        String titulo() {
            return "¡INFORMACIÓN VIRAL!";
        }

        @Override
        String subtitulo() {
            return "Se está compartiendo rápidamente. Tienes 10 segundos: arrastra un sello sobre ella o compártela.";
        }

        private Rectangle post() {
            Rectangle a = AREA;
            return new Rectangle(a.x + 60, a.y + 120, 540, 400);
        }

        private Rectangle sello(int i) {
            Rectangle a = AREA;
            return new Rectangle(a.x + 660 + (i % 2) * 180, a.y + 130 + (i / 2) * 190, 150, 170);
        }

        @Override
        void actualizar(double dt) {
            tiempo += dt;
            if (!fin) {
                restante -= dt;
                if (restante <= 0) {
                    decidir(2);
                }
            }
        }

        private void decidir(int opcion) {
            String[] m = new String[1];
            int[] r = j.resolverViral(viral, opcion, m);
            puntos = r[0];
            exito = r[1] == 1;
            mensaje = m[0];
            fin = true;
            arrastrando = -1;
            Sonido.tocar(exito ? Sonido.Tipo.BIEN : Sonido.Tipo.MAL);
        }

        @Override
        void dibujar(Graphics2D g) {
            marco(g, null, fin ? 0 : restante, DURACION);
            Rectangle a = AREA;
            Rectangle p = post();
            boolean sobre = arrastrando >= 0 && p.contains(ax, ay);
            Dibujo.tarjetaOscura(g, new Rectangle(p.x - 6, p.y - 6, p.width + 12, p.height + 12), 30);
            g.setColor(Tema.altoContraste ? Color.BLACK : Color.WHITE);
            g.fillRoundRect(p.x, p.y, p.width, p.height, 24, 24);
            if (sobre) {
                g.setColor(Tema.alfa(Tema.DORADO, 200));
                g.setStroke(new BasicStroke(5));
                g.drawRoundRect(p.x, p.y, p.width, p.height, 24, 24);
                g.setStroke(new BasicStroke(1));
            }
            g.setColor(Tema.lila());
            g.fillRoundRect(p.x, p.y, p.width, 46, 24, 24);
            g.fillRect(p.x, p.y + 24, p.width, 22);
            g.setFont(Tema.fuente(Font.BOLD, 18));
            g.setColor(Tema.CREMA);
            g.drawString("Civitas  ·  Tendencia", p.x + 20, p.y + 30);
            int veces = (int) (1200 + tiempo * 780 + Math.pow(tiempo, 2.4) * 40);
            String vc = String.format("%,d", veces).replace(',', '.') + " veces";
            g.setFont(Tema.fuente(Font.BOLD, 13));
            g.drawString(vc, p.x + p.width - 20 - g.getFontMetrics().stringWidth(vc), p.y + 29);
            Ilustracion.dibujar(g, new Rectangle(p.x + 20, p.y + 62, p.width - 40, 160), viral.texto, false, tiempo);
            g.setColor(Tema.tinta());
            Dibujo.parrafo(g, Tema.fuente(Font.BOLD, 21), "\"" + viral.texto + "\"", p.x + 24, p.y + 260, p.width - 48, 28,
                    p.y + p.height - 60);
            g.setFont(Tema.fuente(Font.PLAIN, 13.5f));
            g.setColor(Tema.tintaSuave());
            g.drawString("Fuente: " + viral.fuente, p.x + 24, p.y + p.height - 26);
            if (fin) {
                resultado(g);
                return;
            }
            // sellos arrastrables
            for (int i = 0; i < 3; i++) {
                Rectangle s = sello(i);
                boolean hv = v.encima(s) && arrastrando < 0;
                if (i != arrastrando) {
                    selloDibujo(g, s, i, hv);
                } else {
                    g.setColor(new Color(238, 214, 205, 120));
                    g.fillRoundRect(s.x, s.y, s.width, s.height, 20, 20);
                }
            }
            // compartir
            Rectangle comp = sello(3);
            boolean hv = v.encima(comp);
            opcion(g, comp, hv, Tema.lila(), false);
            g.setColor(Tema.lila());
            double cx = comp.getCenterX(), cy = comp.y + 66;
            g.fill(new Ellipse2D.Double(cx - 36, cy - 36, 72, 72));
            g.setColor(Tema.CREMA);
            g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new java.awt.geom.Line2D.Double(cx, cy + 14, cx, cy - 16));
            Path2D fl = new Path2D.Double();
            fl.moveTo(cx - 12, cy - 4);
            fl.lineTo(cx, cy - 17);
            fl.lineTo(cx + 12, cy - 4);
            g.draw(fl);
            g.setStroke(new BasicStroke(1));
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.setColor(Tema.tinta());
            Dibujo.centrado(g, "COMPARTIR (4)", cx, comp.y + comp.height - 22);
            v.zona(comp, () -> decidir(3));
            if (arrastrando >= 0) {
                Recursos.contener(g, SELLOS[arrastrando].imagen(), ax - 55, ay - 55, 110, 110);
            }
            g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
            g.setColor(Tema.tintaSuave());
            Dibujo.centrado(g, "Arrastra VERIFICAR, REPORTAR o IGNORAR sobre la publicación (teclas 1 a 3) o tócala para compartir (4).",
                    a.getCenterX(), a.y + a.height - 24);
        }

        private void selloDibujo(Graphics2D g, Rectangle s, int i, boolean hv) {
            opcion(g, s, hv, OficinaVista.colorSello(SELLOS[i]), false);
            double k = hv ? 1.06 : 1;
            if (!Recursos.contener(g, SELLOS[i].imagen(), s.getCenterX() - 48 * k, s.y + 14 - (hv ? 3 : 0), 96 * k, 96 * k)) {
                Dibujo.selloRespaldo(g, s.getCenterX(), s.y + 62, 44, OficinaVista.colorSello(SELLOS[i]), NOMBRES[i]);
            }
            g.setFont(Tema.fuente(Font.BOLD, 14));
            g.setColor(Tema.tinta());
            Dibujo.centrado(g, NOMBRES[i] + " (" + (i + 1) + ")", s.getCenterX(), s.y + s.height - 22);
        }

        private void resultado(Graphics2D g) {
            Rectangle a = AREA;
            Rectangle r = new Rectangle(a.x + 640, a.y + 140, a.width - 700, 360);
            Dibujo.tarjetaOscura(g, r, 24);
            Iconos.marca(g, exito ? 2 : 3, r.x + 40, r.y + 46, 14);
            g.setFont(Tema.fuente(Font.BOLD, 24));
            g.setColor(exito ? Tema.bien() : Tema.mal());
            g.drawString(exito ? "¡Buena decisión!" : "Decisión arriesgada", r.x + 66, r.y + 55);
            g.setFont(Tema.fuente(Font.BOLD, 20));
            g.setColor(Tema.acento2());
            g.drawString((puntos >= 0 ? "+" : "") + puntos + " puntos", r.x + 30, r.y + 100);
            g.setColor(Tema.texto());
            Dibujo.parrafo(g, Tema.fuente(Font.PLAIN, 15), mensaje, r.x + 30, r.y + 138, r.width - 60, 22, r.y + r.height - 70);
            g.setColor(Tema.textoSuave());
            Dibujo.parrafo(g, Tema.fuente(Font.ITALIC, 13), viral.verdadera ? "Era VERDADERA (cuenta oficial)."
                    : "Era FALSA: " + viral.fuente + " no es una fuente confiable.", r.x + 30, r.y + r.height - 96,
                    r.width - 60, 18, r.y + r.height - 60);
            v.boton(g, new Rectangle(r.x + 30, r.y + r.height + 24, r.width - 60, 52), "Volver a la ventanilla",
                    Dibujo.Estilo.PRINCIPAL, () -> terminado = true);
        }

        @Override
        boolean presionar(int x, int y) {
            if (fin) {
                return false;
            }
            for (int i = 0; i < 3; i++) {
                if (sello(i).contains(x, y)) {
                    arrastrando = i;
                    ax = x;
                    ay = y;
                    return true;
                }
            }
            return false;
        }

        @Override
        void arrastrar(int x, int y) {
            ax = x;
            ay = y;
        }

        @Override
        void soltar(int x, int y) {
            if (arrastrando < 0) {
                return;
            }
            if (post().contains(x, y)) {
                int op = arrastrando == 0 ? 0 : arrastrando == 1 ? 1 : 2;
                Sonido.tocar(Sonido.Tipo.SELLO);
                decidir(op);
            } else if (sello(arrastrando).contains(x, y)) {
                v.avisar("Arrastra el sello", "Llévalo hasta la publicación para marcarla.");
            }
            arrastrando = -1;
        }

        @Override
        void tecla(KeyEvent e) {
            int k = e.getKeyCode() - KeyEvent.VK_1;
            if (!fin && k >= 0 && k <= 3) {
                decidir(k == 0 ? 0 : k == 1 ? 1 : k == 2 ? 2 : 3);
            } else if (fin && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE)) {
                terminado = true;
            }
        }

        @Override
        void cerrar() {
            v.anotarViral(exito, puntos);
        }
    }
}
