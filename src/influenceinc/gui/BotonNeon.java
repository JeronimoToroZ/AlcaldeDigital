package influenceinc.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

/** Boton con estilo propio que se redibuja con los colores del tema actual. */
public class BotonNeon extends JButton {

    private Color color;
    private String subtitulo;
    private boolean encima;
    private final float tam;

    public BotonNeon(String texto, Color color) {
        this(texto, color, 14f);
    }

    public BotonNeon(String texto, Color color, float tam) {
        super(texto);
        this.color = color;
        this.tam = tam;
        setFont(Tema.fuente(java.awt.Font.BOLD, tam));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                encima = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                encima = false;
                repaint();
            }
        });
    }

    public void setColor(Color c) {
        this.color = c;
        repaint();
    }

    public void setSubtitulo(String s) {
        this.subtitulo = s;
        setToolTipText(s);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(getText()) + 32;
        int h = (int) (tam * 2.6);
        if (subtitulo != null) {
            FontMetrics fs = getFontMetrics(getFont().deriveFont(java.awt.Font.PLAIN, tam - 3));
            w = Math.max(w, fs.stringWidth(subtitulo) + 28);
            h += (int) tam;
        }
        return new Dimension(w, h);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        boolean activo = isEnabled();
        Color c = activo ? color : new Color(90, 90, 110);
        if (Tema.altoContraste) {
            g.setColor(Color.BLACK);
            g.fillRoundRect(1, 1, w - 2, h - 2, 14, 14);
            g.setColor(activo ? (encima ? Color.YELLOW : Color.WHITE) : Color.GRAY);
            g.setStroke(new BasicStroke(encima ? 3f : 2f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);
        } else {
            Color a = new Color(c.getRed(), c.getGreen(), c.getBlue(), encima && activo ? 120 : 70);
            Color b = new Color(c.getRed() / 3, c.getGreen() / 3, c.getBlue() / 3, 200);
            g.setPaint(new GradientPaint(0, 0, a, 0, h, b));
            g.fillRoundRect(1, 1, w - 2, h - 2, 14, 14);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), encima && activo ? 255 : 170));
            g.setStroke(new BasicStroke(encima && activo ? 2f : 1.3f));
            g.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);
        }
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        int yTexto = subtitulo == null ? (h + fm.getAscent() - fm.getDescent()) / 2 : h / 2 - 2;
        g.setColor(activo ? Tema.texto() : new Color(140, 140, 150));
        g.drawString(getText(), (w - fm.stringWidth(getText())) / 2, yTexto);
        if (subtitulo != null) {
            g.setFont(getFont().deriveFont(java.awt.Font.PLAIN, tam - 3));
            FontMetrics fs = g.getFontMetrics();
            g.setColor(activo ? Tema.textoSuave() : new Color(120, 120, 130));
            g.drawString(subtitulo, (w - fs.stringWidth(subtitulo)) / 2, yTexto + fs.getHeight());
        }
        g.dispose();
    }
}
