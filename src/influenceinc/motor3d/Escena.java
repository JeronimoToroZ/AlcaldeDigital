package influenceinc.motor3d;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Lista de primitivas 3D de un cuadro y su dibujo con el ALGORITMO DEL PINTOR:
 * se ordenan de la mas lejana a la mas cercana y se pintan en ese orden, asi
 * lo cercano tapa a lo lejano sin necesidad de un z-buffer.
 *
 * Primitivas: esferas (cuentas de la red), lineas (aristas), caras (edificios
 * y suelo), particulas (pulsos que viajan) y textos.
 */
public class Escena {

    /** Algo que se puede proyectar y dibujar. */
    public abstract static class Primitiva {
        double profundidad;
        /** Se suma a la profundidad al ordenar: el suelo usa un sesgo grande para quedar al fondo. */
        public double sesgo;

        /** Proyecta con la camara. false si no se ve. */
        abstract boolean preparar(Camara c);

        abstract void dibujar(Graphics2D g);
    }

    private final List<Primitiva> lista = new ArrayList<>();
    private final List<Esfera> esferasVisibles = new ArrayList<>();
    private final double[] tmp = new double[4];

    public void limpiar() {
        lista.clear();
        esferasVisibles.clear();
    }

    public void agregar(Primitiva p) {
        lista.add(p);
    }

    /** Proyecta, ordena por profundidad y dibuja. */
    public void render(Graphics2D g, Camara c) {
        List<Primitiva> visibles = new ArrayList<>(lista.size());
        for (Primitiva p : lista) {
            if (p.preparar(c)) {
                visibles.add(p);
                if (p instanceof Esfera) {
                    esferasVisibles.add((Esfera) p);
                }
            }
        }
        visibles.sort((a, b) -> Double.compare(b.profundidad + b.sesgo, a.profundidad + a.sesgo));
        for (Primitiva p : visibles) {
            p.dibujar(g);
        }
    }

    /** La esfera con dato mas cercana a la camara bajo el puntero. */
    public Object elegir(int mx, int my) {
        Esfera mejor = null;
        for (Esfera e : esferasVisibles) {
            if (e.dato == null) {
                continue;
            }
            double dx = mx - e.sx, dy = my - e.sy;
            double r = Math.max(e.rPant, 7);
            if (dx * dx + dy * dy <= r * r && (mejor == null || e.profundidad < mejor.profundidad)) {
                mejor = e;
            }
        }
        return mejor == null ? null : mejor.dato;
    }

    /** Posicion en pantalla de la esfera con ese dato (o null). */
    public double[] posicionDe(Object dato) {
        for (Esfera e : esferasVisibles) {
            if (e.dato == dato) {
                return new double[]{e.sx, e.sy, e.rPant};
            }
        }
        return null;
    }

    static Color alfa(Color c, double a) {
        int al = (int) Math.max(0, Math.min(255, a * c.getAlpha()));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), al);
    }

    // ==================================================================
    // Esfera
    // ==================================================================

    public static class Esfera extends Primitiva {
        final double x, y, z, radio;
        final Color color;
        public double brillo = 0.6;
        public Object dato;
        public String etiqueta;
        public Color colorEtiqueta = Color.WHITE;
        public boolean etiquetaSiempre;
        public Color anillo;
        public boolean anilloPunteado;
        public int simbolo = -1;     // 0 circulo, 1 cuadrado, 2 triangulo, 3 rombo
        public Color colorSimbolo = Color.WHITE;
        public boolean marcaX;       // marca de desinformado (no depende del color)
        public double escalaPulso = 1;
        double sx, sy, rPant;

        public Esfera(double x, double y, double z, double radio, Color color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radio = radio;
            this.color = color;
        }

        @Override
        boolean preparar(Camara c) {
            double[] o = new double[4];
            if (!c.proyectar(x, y, z, o)) {
                return false;
            }
            sx = o[0];
            sy = o[1];
            profundidad = o[2];
            rPant = Math.max(1.5, radio * o[3] * escalaPulso);
            return true;
        }

        @Override
        void dibujar(Graphics2D g) {
            float r = (float) rPant;
            // halo
            if (brillo > 0) {
                float rh = r * 2.6f;
                RadialGradientPaint halo = new RadialGradientPaint(new Point2D.Double(sx, sy), rh,
                        new float[]{0f, 0.35f, 1f},
                        new Color[]{alfa(color, 0.55 * brillo), alfa(color, 0.18 * brillo), alfa(color, 0)});
                g.setPaint(halo);
                g.fill(new Ellipse2D.Double(sx - rh, sy - rh, rh * 2, rh * 2));
            }
            // cuerpo con luz arriba a la izquierda
            RadialGradientPaint cuerpo = new RadialGradientPaint(
                    new Point2D.Double(sx, sy), r, new Point2D.Double(sx - r * 0.35, sy - r * 0.4),
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{mezclar(color, Color.WHITE, 0.55), color, mezclar(color, Color.BLACK, 0.55)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE);
            g.setPaint(cuerpo);
            g.fill(new Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2));

            Stroke antes = g.getStroke();
            if (anillo != null) {
                float ra = r + Math.max(3f, r * 0.45f);
                g.setStroke(anilloPunteado
                        ? new BasicStroke(Math.max(1.5f, r * 0.18f), BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0f)
                        : new BasicStroke(Math.max(1.5f, r * 0.18f)));
                g.setColor(anillo);
                g.draw(new Ellipse2D.Double(sx - ra, sy - ra, ra * 2, ra * 2));
            }
            if (simbolo >= 0 && r > 3) {
                dibujarSimbolo(g, simbolo, sx, sy, r * 0.55, colorSimbolo);
            }
            if (marcaX && r > 2.5) {
                g.setStroke(new BasicStroke(Math.max(1.5f, r * 0.22f), BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 255, 255, 230));
                double k = r * 0.5;
                g.draw(new Line2D.Double(sx - k, sy - k, sx + k, sy + k));
                g.draw(new Line2D.Double(sx - k, sy + k, sx + k, sy - k));
            }
            g.setStroke(antes);

            if (etiqueta != null && (etiquetaSiempre || r > 6.5)) {
                Font f = g.getFont().deriveFont(Font.BOLD, (float) Math.max(10, Math.min(14, r * 1.1)));
                g.setFont(f);
                FontMetrics fm = g.getFontMetrics();
                int w = fm.stringWidth(etiqueta);
                float ty = (float) (sy - r - (anillo != null ? 9 : 5));
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRoundRect((int) (sx - w / 2.0 - 4), (int) (ty - fm.getAscent()), w + 8,
                        fm.getHeight(), 8, 8);
                g.setColor(colorEtiqueta);
                g.drawString(etiqueta, (float) (sx - w / 2.0), ty - 2);
            }
        }
    }

    /** Circulo, cuadrado, triangulo o rombo: el simbolo de cada agencia. */
    public static void dibujarSimbolo(Graphics2D g, int simbolo, double cx, double cy, double r, Color c) {
        Path2D p = new Path2D.Double();
        switch (simbolo % 4) {
            case 0:
                g.setColor(c);
                g.fill(new Ellipse2D.Double(cx - r * 0.7, cy - r * 0.7, r * 1.4, r * 1.4));
                return;
            case 1:
                p.moveTo(cx - r * 0.65, cy - r * 0.65);
                p.lineTo(cx + r * 0.65, cy - r * 0.65);
                p.lineTo(cx + r * 0.65, cy + r * 0.65);
                p.lineTo(cx - r * 0.65, cy + r * 0.65);
                break;
            case 2:
                p.moveTo(cx, cy - r * 0.85);
                p.lineTo(cx + r * 0.8, cy + r * 0.6);
                p.lineTo(cx - r * 0.8, cy + r * 0.6);
                break;
            default:
                p.moveTo(cx, cy - r * 0.9);
                p.lineTo(cx + r * 0.7, cy);
                p.lineTo(cx, cy + r * 0.9);
                p.lineTo(cx - r * 0.7, cy);
                break;
        }
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    public static Color mezclar(Color a, Color b, double t) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                a.getAlpha());
    }

    // ==================================================================
    // Linea (recortada contra el plano cercano)
    // ==================================================================

    public static class Linea extends Primitiva {
        final double x1, y1, z1, x2, y2, z2;
        final Color color;
        final float ancho;
        public boolean desvanecer = true;
        double ax, ay, bx, by;
        double escala;

        public Linea(double x1, double y1, double z1, double x2, double y2, double z2,
                     Color color, float ancho) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
            this.color = color;
            this.ancho = ancho;
        }

        @Override
        boolean preparar(Camara c) {
            double[] a = new double[4];
            double[] b = new double[4];
            c.aCamara(x1, y1, z1, a);
            c.aCamara(x2, y2, z2, b);
            double cerca = Camara.CERCA;
            if (a[2] < cerca && b[2] < cerca) {
                return false;
            }
            // recorte: se mueve el extremo que queda detras hasta el plano cercano
            if (a[2] < cerca) {
                double t = (cerca - a[2]) / (b[2] - a[2]);
                a[0] += (b[0] - a[0]) * t;
                a[1] += (b[1] - a[1]) * t;
                a[2] = cerca;
            } else if (b[2] < cerca) {
                double t = (cerca - b[2]) / (a[2] - b[2]);
                b[0] += (a[0] - b[0]) * t;
                b[1] += (a[1] - b[1]) * t;
                b[2] = cerca;
            }
            profundidad = (a[2] + b[2]) / 2 + 0.5;
            escala = 400.0 / profundidad;
            double[] o = new double[2];
            c.camaraAPantalla(a[0], a[1], a[2], o);
            ax = o[0];
            ay = o[1];
            c.camaraAPantalla(b[0], b[1], b[2], o);
            bx = o[0];
            by = o[1];
            return true;
        }

        @Override
        void dibujar(Graphics2D g) {
            double k = desvanecer ? Math.max(0.25, Math.min(1, escala)) : 1;
            g.setColor(alfa(color, k));
            g.setStroke(new BasicStroke((float) Math.max(0.6, ancho * Math.min(1.6, escala)),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(ax, ay, bx, by));
        }
    }

    // ==================================================================
    // Cara (poligono plano)
    // ==================================================================

    public static class Cara extends Primitiva {
        final double[][] pts;
        final Color relleno;
        final Color borde;
        /** Ventanas encendidas (solo para caras de 4 vertices). */
        public int filasVentanas, colsVentanas;
        public Color colorVentana = new Color(255, 214, 120, 170);
        public long semillaVentanas;
        public boolean cullTrasera = true;
        Path2D forma;
        double[][] pant;

        public Cara(double[][] pts, Color relleno, Color borde) {
            this.pts = pts;
            this.relleno = relleno;
            this.borde = borde;
        }

        @Override
        boolean preparar(Camara c) {
            // 1. a coordenadas de camara
            int n = pts.length;
            double[][] cam = new double[n][];
            boolean algunoDetras = false;
            double suma = 0;
            for (int i = 0; i < n; i++) {
                double[] o = new double[4];
                c.aCamara(pts[i][0], pts[i][1], pts[i][2], o);
                cam[i] = new double[]{o[0], o[1], o[2]};
                algunoDetras |= o[2] < Camara.CERCA;
                suma += o[2];
            }
            // 2. recorte contra el plano cercano (Sutherland-Hodgman) si hace falta
            List<double[]> poli = new ArrayList<>();
            if (algunoDetras) {
                for (int i = 0; i < n; i++) {
                    double[] a = cam[i], b = cam[(i + 1) % n];
                    boolean ain = a[2] >= Camara.CERCA, bin = b[2] >= Camara.CERCA;
                    if (ain) {
                        poli.add(a);
                    }
                    if (ain != bin) {
                        double t = (Camara.CERCA - a[2]) / (b[2] - a[2]);
                        poli.add(new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, Camara.CERCA});
                    }
                }
                if (poli.size() < 3) {
                    return false;
                }
            } else {
                for (double[] q : cam) {
                    poli.add(q);
                }
            }
            // 3. proyeccion
            pant = new double[poli.size()][];
            for (int i = 0; i < poli.size(); i++) {
                double[] o = new double[2];
                double[] q = poli.get(i);
                c.camaraAPantalla(q[0], q[1], q[2], o);
                pant[i] = o;
            }
            if (cullTrasera && pant.length >= 3) {
                double cruz = (pant[1][0] - pant[0][0]) * (pant[2][1] - pant[0][1])
                        - (pant[1][1] - pant[0][1]) * (pant[2][0] - pant[0][0]);
                if (cruz <= 0) {
                    return false;     // la cara mira hacia el otro lado
                }
            }
            profundidad = Math.max(Camara.CERCA, suma / n);
            forma = new Path2D.Double();
            forma.moveTo(pant[0][0], pant[0][1]);
            for (int i = 1; i < pant.length; i++) {
                forma.lineTo(pant[i][0], pant[i][1]);
            }
            forma.closePath();
            return true;
        }

        @Override
        void dibujar(Graphics2D g) {
            g.setColor(relleno);
            g.fill(forma);
            if (filasVentanas > 0 && pant.length == 4 && pts.length == 4) {
                dibujarVentanas(g);
            }
            if (borde != null) {
                g.setStroke(new BasicStroke(1f));
                g.setColor(borde);
                g.draw(forma);
            }
        }

        private void dibujarVentanas(Graphics2D g) {
            // interpolacion bilineal dentro del cuadrilatero proyectado
            long s = semillaVentanas;
            double tam = Math.max(1.2, Math.min(3.2,
                    Math.abs(pant[1][0] - pant[0][0]) / (colsVentanas * 2.2)));
            g.setColor(colorVentana);
            for (int f = 0; f < filasVentanas; f++) {
                for (int k = 0; k < colsVentanas; k++) {
                    s = s * 6364136223846793005L + 1442695040888963407L;
                    if (((s >>> 33) & 3) == 0) {
                        continue;          // ventana apagada
                    }
                    double u = (k + 0.5) / colsVentanas;
                    double v = (f + 0.6) / (filasVentanas + 0.4);
                    double xa = pant[0][0] + (pant[1][0] - pant[0][0]) * u;
                    double ya = pant[0][1] + (pant[1][1] - pant[0][1]) * u;
                    double xb = pant[3][0] + (pant[2][0] - pant[3][0]) * u;
                    double yb = pant[3][1] + (pant[2][1] - pant[3][1]) * u;
                    double x = xa + (xb - xa) * v;
                    double y = ya + (yb - ya) * v;
                    g.fill(new java.awt.geom.Rectangle2D.Double(x - tam / 2, y - tam / 2, tam, tam));
                }
            }
        }
    }

    /**
     * Agrega una caja (edificio) apoyada en el suelo: 4 paredes y el techo,
     * con un sombreado distinto por cara segun hacia donde mira.
     */
    public void agregarCaja(double cx, double cz, double ancho, double fondo, double alto,
                            Color base, boolean ventanas, long semilla) {
        double x0 = cx - ancho / 2, x1 = cx + ancho / 2;
        double z0 = cz - fondo / 2, z1 = cz + fondo / 2;
        double y0 = 0, y1 = alto;
        Color techo = mezclar(base, Color.WHITE, 0.18);
        Color ladoA = base;
        Color ladoB = mezclar(base, Color.BLACK, 0.35);
        Color borde = alfa(mezclar(base, Color.WHITE, 0.35), 0.5);
        int filas = (int) Math.max(1, alto / 11);
        int colsX = (int) Math.max(1, ancho / 7);
        int colsZ = (int) Math.max(1, fondo / 7);
        // caras con vertices en sentido antihorario visto desde afuera
        Cara frente = new Cara(new double[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}}, ladoA, borde);
        Cara atras = new Cara(new double[][]{{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}}, ladoA, borde);
        Cara der = new Cara(new double[][]{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}}, ladoB, borde);
        Cara izq = new Cara(new double[][]{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}}, ladoB, borde);
        Cara arriba = new Cara(new double[][]{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}}, techo, borde);
        Cara[] paredes = {frente, atras, der, izq};
        for (int i = 0; i < paredes.length; i++) {
            if (ventanas) {
                paredes[i].filasVentanas = filas;
                paredes[i].colsVentanas = (i < 2) ? colsX : colsZ;
                paredes[i].semillaVentanas = semilla * 31 + i;
            }
            agregar(paredes[i]);
        }
        agregar(arriba);
    }

    // ==================================================================
    // Particula y texto
    // ==================================================================

    public static class Particula extends Primitiva {
        final double x, y, z, radio;
        final Color color;
        double sx, sy, r;

        public Particula(double x, double y, double z, double radio, Color color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radio = radio;
            this.color = color;
        }

        @Override
        boolean preparar(Camara c) {
            double[] o = new double[4];
            if (!c.proyectar(x, y, z, o)) {
                return false;
            }
            sx = o[0];
            sy = o[1];
            profundidad = o[2] - 1;
            r = Math.max(2, radio * o[3]);
            return true;
        }

        @Override
        void dibujar(Graphics2D g) {
            double rh = r * 3;
            g.setPaint(new RadialGradientPaint(new Point2D.Double(sx, sy), (float) rh,
                    new float[]{0f, 0.3f, 1f},
                    new Color[]{alfa(Color.WHITE, color.getAlpha() / 255.0), alfa(color, 0.8), alfa(color, 0)}));
            g.fill(new Ellipse2D.Double(sx - rh, sy - rh, rh * 2, rh * 2));
        }
    }

    public static class Texto extends Primitiva {
        final double x, y, z;
        final String texto;
        final Color color;
        final float tam;
        double sx, sy, esc;

        public Texto(double x, double y, double z, String texto, Color color, float tam) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.texto = texto;
            this.color = color;
            this.tam = tam;
        }

        @Override
        boolean preparar(Camara c) {
            double[] o = new double[4];
            if (!c.proyectar(x, y, z, o)) {
                return false;
            }
            sx = o[0];
            sy = o[1];
            profundidad = o[2] - 2;
            esc = o[3];
            return true;
        }

        @Override
        void dibujar(Graphics2D g) {
            float t = (float) Math.max(9, Math.min(tam * 1.6, tam * esc * 1.4));
            g.setFont(g.getFont().deriveFont(Font.BOLD, t));
            FontMetrics fm = g.getFontMetrics();
            Composite c = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(new Color(0, 0, 0, Math.min(160, color.getAlpha())));
            g.drawString(texto, (float) (sx - fm.stringWidth(texto) / 2.0 + 1), (float) sy + 1);
            g.setColor(color);
            g.drawString(texto, (float) (sx - fm.stringWidth(texto) / 2.0), (float) sy);
            g.setComposite(c);
        }
    }

    /** Disco plano en el suelo (zonas de los barrios). */
    public void agregarDisco(double cx, double cz, double radio, double y, Color relleno, Color borde) {
        int n = 36;
        double[][] pts = new double[n][];
        for (int i = 0; i < n; i++) {
            double a = -Math.PI * 2 * i / n;
            pts[i] = new double[]{cx + Math.cos(a) * radio, y, cz + Math.sin(a) * radio};
        }
        Cara c = new Cara(pts, relleno, borde);
        c.cullTrasera = false;
        c.sesgo = 100000;
        agregar(c);
    }

    public List<Primitiva> getLista() {
        return lista;
    }

    /** Distancia de un punto a la camara (para ordenar cosas propias). */
    public double profundidadDe(Camara c, double x, double y, double z) {
        c.aCamara(x, y, z, tmp);
        return tmp[2];
    }
}
