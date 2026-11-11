package influenceinc.gui;

import influenceinc.logica.Historia;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Partida;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * El final: que paso con Ciudad Nova segun las decisiones de cada verificador.
 */
public class PantallaFinal extends JPanel {

    private Partida partida;
    private List<String[]> marcador = new ArrayList<>();

    public PantallaFinal(Runnable alMenu) {
        super(new BorderLayout());
        JPanel abajo = new JPanel(new FlowLayout(FlowLayout.CENTER));
        abajo.setOpaque(false);
        BotonNeon b = new BotonNeon("Volver al menú", Tema.acento(), 18f);
        b.addActionListener(e -> alMenu.run());
        abajo.add(b);
        add(abajo, BorderLayout.SOUTH);
        setBackground(Tema.fondo());
    }

    public void mostrar(Partida p, List<String[]> marcador) {
        this.partida = p;
        setMarcador(marcador);
    }

    public void setMarcador(List<String[]> m) {
        this.marcador = new ArrayList<>(m);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, Tema.altoContraste ? Color.BLACK : new Color(12, 16, 32), 0, getHeight(),
                Tema.altoContraste ? Color.BLACK : new Color(36, 18, 46)));
        g.fillRect(0, 0, getWidth(), getHeight());
        if (partida == null) {
            return;
        }
        List<JugadorVerificador> rk = partida.ranking();
        int n = rk.size();
        int margen = 30;
        int ancho = (getWidth() - margen * (n + 1)) / n;
        for (int i = 0; i < n; i++) {
            JugadorVerificador j = rk.get(i);
            int x = margen + i * (ancho + margen), y = 30;
            String[] fin = Historia.finalPara(j);
            g.setColor(new Color(20, 24, 46, 230));
            g.fillRoundRect(x, y, ancho, getHeight() - 140, 18, 18);
            g.setColor(fin[0].equals("DESPEDIDO") || fin[0].contains("ENGAÑADA") ? Tema.mal()
                    : fin[0].contains("DIVIDIDA") ? new Color(230, 170, 40) : Tema.bien());
            g.drawRoundRect(x, y, ancho, getHeight() - 140, 18, 18);
            int ty = y + 40;
            g.setFont(Tema.fuente(Font.BOLD, 15));
            g.setColor(Tema.textoSuave());
            g.drawString((n > 1 ? "#" + (i + 1) + "  " : "") + j.nombre + "  ·  puntaje final " + j.puntajeFinal(), x + 20, ty);
            ty += 40;
            g.setFont(Tema.fuente(Font.BOLD, n > 2 ? 22 : 34));
            g.setColor(Tema.acento2());
            g.drawString(fin[0], x + 20, ty);
            ty += 34;
            Font f = Tema.fuente(Font.PLAIN, n > 2 ? 14 : 17);
            g.setFont(f);
            g.setColor(Tema.texto());
            for (String l : Avisos.partir(g, f, fin[1], ancho - 40)) {
                g.drawString(l, x + 20, ty);
                ty += f.getSize() + 6;
            }
            ty += 10;
            g.setFont(Tema.fuente(Font.BOLD, n > 2 ? 13 : 16));
            g.setColor(new Color(255, 205, 70));
            for (String l : Avisos.partir(g, g.getFont(), Historia.eleccion(j), ancho - 40)) {
                g.drawString(l, x + 20, ty);
                ty += 22;
            }
            ty += 8;
            g.setFont(Tema.fuente(Font.PLAIN, 14));
            g.setColor(Tema.textoSuave());
            String[] st = {
                "Casos: " + j.casosAtendidos + " · aciertos " + j.aciertos + " · errores " + j.errores,
                "Impostores atrapados: " + j.impostoresAtrapados,
                "Desinformación final: " + j.ciudad.desinformacion() + " % · confianza " + j.ciudad.confianza,
                "Salud de la ciudad: " + j.ciudad.salud() + " · reputación " + j.reputacion + " · $" + j.dinero,
                "Logros: " + j.logros.size()
            };
            for (String s : st) {
                g.drawString(s, x + 20, ty);
                ty += 21;
            }
            if (n == 1 && !j.logros.isEmpty()) {
                int lx = x + ancho / 2 + 40, ly = y + 230;
                g.setFont(Tema.fuente(Font.BOLD, 16));
                g.setColor(new Color(255, 205, 70));
                g.drawString("LOGROS", lx, ly);
                ly += 26;
                g.setFont(Tema.fuente(Font.PLAIN, 14));
                g.setColor(Tema.texto());
                for (String lg : j.logros) {
                    Iconos.marca(g, 4, lx + 6, ly - 5, 7);
                    g.drawString(lg, lx + 22, ly);
                    ly += 22;
                }
            }
            ty += 10;
            g.setFont(Tema.fuente(Font.PLAIN, 12.5f));
            for (String b : j.bitacora) {
                if (ty > getHeight() - 130) {
                    break;
                }
                boolean ok = b.contains("[ok]");
                Iconos.marca(g, ok ? 2 : 3, x + 26, ty - 4, 5);
                g.setColor(ok ? Tema.bien() : Tema.mal());
                g.drawString(VistaArboles.corto(b.replace(" [ok]", "").replace(" [x]", " -"), n > 2 ? 40 : 70), x + 38, ty);
                ty += 17;
            }
        }
        if (!marcador.isEmpty()) {
            g.setFont(Tema.fuente(Font.BOLD, 15));
            g.setColor(new Color(255, 210, 90));
            StringBuilder sb = new StringBuilder("MARCADOR EN RED:  ");
            List<String[]> m = new ArrayList<>(marcador);
            m.sort((a, b) -> Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1])));
            for (String[] f : m) {
                sb.append(f[0]).append(" ").append(f[1]).append(f[5].equals("1") ? " (terminó)" : " (día " + f[2] + ")")
                        .append("    ");
            }
            g.drawString(sb.toString(), 40, getHeight() - 80);
        }
    }
}
