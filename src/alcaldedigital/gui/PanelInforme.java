package alcaldedigital.gui;

import alcaldedigital.logica.GrafoSocial;
import alcaldedigital.logica.NodoRed;
import alcaldedigital.logica.Partida;
import alcaldedigital.modelo.Publicacion;
import alcaldedigital.modelo.Senal;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * El informe que ve el jugador al terminar la partida.
 *
 * No es un marcador. Un numero no ensena nada: "72 puntos" no le dice a nadie
 * que hizo mal. Este informe responde tres preguntas concretas:
 *
 *   1. A cuantas personas desinformaste, y quienes son (con cara y nombre).
 *   2. A cuantas alcanzaste a corregir, y por que no alcanzaste a mas.
 *   3. En cada error, cual era la pista que se te paso.
 *
 * La diferencia entre el numero de la pregunta 1 y el de la 2 es la leccion
 * entera del juego, y aqui se ve de un vistazo.
 */
public class PanelInforme extends JPanel {

    private static final int ANCHO = 760;
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private final Partida partida;
    private final GrafoSocial grafo;

    public PanelInforme(Partida partida, GrafoSocial grafo) {
        this.partida = partida;
        this.grafo = grafo;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Tema.fondo());
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        construir();
    }

    private void construir() {
        int desinformados = grafo.infectados().size();
        int total = grafo.nodos.size();
        int sanos = total - desinformados;

        add(titulo("Informe de campaña"));
        add(Box.createVerticalStrut(6));

        // ---- La frase que resume la partida -----------------------------
        String frase;
        if (desinformados == 0) {
            frase = "Ciudad Nova terminó la campaña sin un solo rumor en pie. "
                  + "Las " + total + " personas de la red siguen informadas.";
        } else if (desinformados == total) {
            frase = "La desinformación llegó a las " + total + " personas de la red. "
                  + "No quedó nadie sin creerlo.";
        } else {
            frase = "La desinformación llegó a " + desinformados + " de " + total
                  + " personas. " + sanos + (sanos == 1 ? " quedó" : " quedaron") + " fuera del rumor.";
        }
        add(parrafo(frase, Tema.subtitulo()));
        add(Box.createVerticalStrut(16));

        // ---- Las caras: a quien le llego y a quien no -------------------
        add(etiqueta("LA RED, AL FINAL"));
        add(Box.createVerticalStrut(8));
        add(new FilaDeCaras(grafo.nodos));
        add(Box.createVerticalStrut(18));

        // ---- Lo que alcanzaste a corregir -------------------------------
        if (desinformados > 0) {
            add(etiqueta("POR QUÉ NO ALCANZASTE A CORREGIRLO TODO"));
            add(Box.createVerticalStrut(6));

            int yaContaron = 0;
            for (NodoRed n : grafo.infectados()) {
                if (n.yaRetransmitio) {
                    yaContaron++;
                }
            }
            add(parrafo("De las " + desinformados + " personas que quedaron desinformadas, "
                    + yaContaron + (yaContaron == 1 ? " ya se lo había contado" : " ya se lo habían contado")
                    + " a alguien más antes de que publicaras el desmentido. A esas ya no las "
                    + "alcanza una corrección: su contagio siguió camino.\n\n"
                    + "Una mentira se reenvía sola. Un desmentido solo lo comparte quien se toma "
                    + "el trabajo, y se apaga a los pocos pasos. Por eso nunca empatan.",
                    Tema.normal()));
            add(Box.createVerticalStrut(18));
        }

        // ---- Las cifras -------------------------------------------------
        add(etiqueta("TUS DECISIONES"));
        add(Box.createVerticalStrut(8));
        add(new FilaDeCifras(partida));
        add(Box.createVerticalStrut(18));

        // ---- Los errores, uno por uno -----------------------------------
        List<Partida.DecisionTomada> decisiones = partida.getDecisiones();
        boolean hayErrores = false;
        for (Partida.DecisionTomada d : decisiones) {
            if (!d.acerto) {
                hayErrores = true;
                break;
            }
        }

        if (hayErrores) {
            add(etiqueta("LO QUE SE TE PASÓ"));
            add(Box.createVerticalStrut(8));
            for (Partida.DecisionTomada d : decisiones) {
                if (!d.acerto) {
                    add(bloqueDeError(d));
                    add(Box.createVerticalStrut(12));
                }
            }
        } else {
            add(etiqueta("LO QUE SE TE PASÓ"));
            add(Box.createVerticalStrut(6));
            add(parrafo("Nada. Ninguna decisión difundió una mentira ni silenció algo cierto.",
                    Tema.normal()));
        }

        add(Box.createVerticalGlue());
    }

    /** Un error explicado: que hiciste, con que publicacion, y cual era la pista. */
    private JPanel bloqueDeError(Partida.DecisionTomada d) {
        Publicacion p = d.publicacion;

        JPanel caja = new JPanel();
        caja.setLayout(new BoxLayout(caja, BoxLayout.Y_AXIS));
        caja.setOpaque(false);
        caja.setAlignmentX(Component.LEFT_ALIGNMENT);
        caja.setMaximumSize(new Dimension(ANCHO, Integer.MAX_VALUE));
        caja.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Tema.negativo()),
                BorderFactory.createEmptyBorder(4, 12, 10, 0)));

        String queHizo = p.esFalsa()
                ? "Compartiste una publicación falsa"
                : "Reportaste una publicación que era cierta";
        JLabel t = new JLabel(queHizo);
        t.setFont(Tema.indicadorNegrita());
        t.setForeground(Tema.negativo());
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        caja.add(t);
        caja.add(Box.createVerticalStrut(4));

        caja.add(parrafo("\"" + recortar(p.getTexto()) + "\"", italica(Tema.normal())));
        caja.add(Box.createVerticalStrut(6));

        if (!p.getSenales().isEmpty()) {
            StringBuilder sb = new StringBuilder("La pista estaba ahí: ");
            List<Senal> ss = p.getSenales();
            for (int i = 0; i < ss.size(); i++) {
                sb.append(i == 0 ? "" : (i == ss.size() - 1 ? " y " : ", "));
                sb.append(ss.get(i).getEtiqueta().toLowerCase());
            }
            sb.append(".");
            caja.add(parrafo(sb.toString(), Tema.normal()));
            caja.add(Box.createVerticalStrut(4));
        }

        if (p.getExplicacion() != null) {
            caja.add(parrafo(p.getExplicacion(), Tema.normal()));
        }

        if (!d.verifico && p.esFalsa()) {
            caja.add(Box.createVerticalStrut(4));
            caja.add(parrafo("No gastaste una verificación en esta.", Tema.indicador()));
        }

        return caja;
    }

    // ------------------------------------------------------------------
    // Las caras de la red
    // ------------------------------------------------------------------

    /** Los ocho ciudadanos, con su cara, su nombre y si les llegó el rumor. */
    private static class FilaDeCaras extends JPanel {

        private static final int CARA = 72;
        private static final int PASO = 92;
        private final List<NodoRed> nodos;

        FilaDeCaras(List<NodoRed> nodos) {
            this.nodos = nodos;
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            int ancho = Math.min(ANCHO, nodos.size() * PASO);
            setPreferredSize(new Dimension(ancho, CARA + 34));
            setMaximumSize(new Dimension(ANCHO, CARA + 34));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizar(g2);

            for (int i = 0; i < nodos.size(); i++) {
                NodoRed n = nodos.get(i);
                int x = i * PASO;
                int y = 2;

                BufferedImage foto = cargar("ciudadano_" + sinTildes(primerNombre(n.persona)) + ".png");

                if (foto != null) {
                    BufferedImage circ = new BufferedImage(CARA, CARA, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D gc = circ.createGraphics();
                    Tema.suavizar(gc);
                    gc.setClip(new Ellipse2D.Float(0, 0, CARA, CARA));
                    gc.drawImage(foto, 0, 0, CARA, CARA, null);
                    gc.dispose();
                    g2.drawImage(circ, x, y, null);
                } else {
                    g2.setColor(new Color(216, 210, 196));
                    g2.fillOval(x, y, CARA, CARA);
                }

                // El aro dice el estado: rojo si le llego, verde si no.
                g2.setColor(n.estaInfectado ? Tema.negativo() : Tema.positivo());
                g2.setStroke(new BasicStroke(4f));
                g2.drawOval(x + 2, y + 2, CARA - 4, CARA - 4);

                // Y ademas un simbolo, para no depender solo del color.
                int cx = x + CARA - 14;
                int cy = y + CARA - 14;
                g2.setColor(n.estaInfectado ? Tema.negativo() : Tema.positivo());
                g2.fillOval(cx - 10, cy - 10, 22, 22);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (n.estaInfectado) {
                    g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                    g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
                } else {
                    g2.drawPolyline(new int[]{cx - 5, cx - 1, cx + 5},
                                    new int[]{cy, cy + 4, cy - 5}, 3);
                }

                g2.setFont(Tema.indicador());
                g2.setColor(Tema.texto());
                String etiqueta = primerNombre(n.persona);
                int w = g2.getFontMetrics().stringWidth(etiqueta);
                g2.drawString(etiqueta, x + (CARA - w) / 2, y + CARA + 18);
            }
            g2.dispose();
        }
    }

    /** Las cuatro cifras de la partida, en fila. */
    private static class FilaDeCifras extends JPanel {

        private final String[][] datos;

        FilaDeCifras(Partida p) {
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(ANCHO, 62));
            setMaximumSize(new Dimension(ANCHO, 62));

            int usadas = Partida.VERIFICACIONES_POR_PARTIDA - p.getVerificacionesRestantes();
            datos = new String[][]{
                {String.valueOf(p.totalAciertos()) + " de " + p.getDecisiones().size(), "decisiones acertadas"},
                {String.valueOf(p.falsasCompartidas()), "mentiras difundidas"},
                {usadas + " de " + Partida.VERIFICACIONES_POR_PARTIDA, "verificaciones usadas"},
                {String.valueOf(p.getConfianza()), "confianza final"}
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizar(g2);

            int ancho = getWidth() / datos.length;
            for (int i = 0; i < datos.length; i++) {
                int x = i * ancho;
                g2.setFont(new Font("SansSerif", Font.BOLD, 24));
                g2.setColor(Tema.texto());
                g2.drawString(datos[i][0], x, 26);

                g2.setFont(Tema.indicador());
                g2.setColor(Tema.textoSuave());
                g2.drawString(datos[i][1], x, 46);
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Piezas sueltas
    // ------------------------------------------------------------------

    private JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.titulo());
        l.setForeground(Tema.texto());
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(Tema.acento());
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Texto que se reparte solo. JTextArea y no JLabel, por lo mismo de siempre. */
    private JTextArea parrafo(String texto, Font fuente) {
        JTextArea a = new JTextArea(texto);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setFocusable(false);
        a.setOpaque(false);
        a.setBorder(null);
        a.setFont(fuente);
        a.setForeground(Tema.texto());
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        a.setMaximumSize(new Dimension(ANCHO, Integer.MAX_VALUE));
        return a;
    }

    private static Font italica(Font f) {
        return f.deriveFont(Font.ITALIC);
    }

    private static String recortar(String t) {
        String limpio = t.replace('\n', ' ');
        return limpio.length() <= 120 ? limpio : limpio.substring(0, 117) + "...";
    }

    /** "don Humberto" -> "Humberto"; "doña Carmen" -> "Carmen". */
    private static String primerNombre(String persona) {
        if (persona == null) {
            return "";
        }
        String[] partes = persona.trim().split("\\s+");
        for (String parte : partes) {
            String p = parte.toLowerCase();
            if (!p.equals("don") && !p.equals("doña") && !p.equals("dona")) {
                return parte;
            }
        }
        return partes[partes.length - 1];
    }

    /** Para armar el nombre del archivo: "Nicolás" -> "nicolas". */
    private static String sinTildes(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase();
    }

    private static BufferedImage cargar(String nombre) {
        if (CACHE.containsKey(nombre)) {
            return CACHE.get(nombre);
        }
        BufferedImage img = null;
        try {
            URL url = PanelInforme.class.getResource("/alcaldedigital/recursos/" + nombre);
            if (url != null) {
                img = ImageIO.read(url);
            }
        } catch (IOException | RuntimeException ignorada) {
            img = null;
        }
        CACHE.put(nombre, img);
        return img;
    }
}
