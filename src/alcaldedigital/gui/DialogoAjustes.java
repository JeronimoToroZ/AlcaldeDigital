package alcaldedigital.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

/**
 * Pequeña ventana de ajustes: modo de juego, brillo y música.
 * Los cambios se aplican al instante. Se cierra con Aceptar o con Esc.
 */
public class DialogoAjustes extends JDialog {

    private static final Color ORO = new Color(0xE9, 0xC4, 0x7A);
    private static final Color CREMA = new Color(0xF3, 0xE3, 0xC3);
    private static final String FUENTE = "Georgia";

    public DialogoAjustes(Window padre) {
        super(padre, "Ajustes", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        JPanel fondo = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x4A, 0x2C, 0x1E),
                        0, getHeight(), new Color(0x26, 0x15, 0x0E)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.setColor(ORO);
                g2.setStroke(new java.awt.BasicStroke(3f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 28, 28);
                g2.dispose();
            }
        };
        fondo.setOpaque(false);
        fondo.setBorder(BorderFactory.createEmptyBorder(18, 26, 16, 26));

        JLabel titulo = new JLabel("Ajustes", SwingConstants.CENTER);
        titulo.setFont(new Font(FUENTE, Font.BOLD | Font.ITALIC, 28));
        titulo.setForeground(ORO);
        fondo.add(titulo, BorderLayout.NORTH);
        fondo.add(construirOpciones(), BorderLayout.CENTER);

        BotonImagen aceptar = new BotonImagen("/alcaldedigital/recursos/acep.png", "Aceptar");
        aceptar.setPreferredSize(new Dimension(170, aceptar.altoParaAncho(170)));
        aceptar.addActionListener(e -> dispose());
        JPanel pie = new JPanel();
        pie.setOpaque(false);
        pie.add(aceptar);
        fondo.add(pie, BorderLayout.SOUTH);

        setContentPane(fondo);
        setBackground(new Color(0, 0, 0, 0));   // esquinas redondeadas transparentes
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(430, 440);
        setLocationRelativeTo(padre);
    }

    private JPanel construirOpciones() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 2, 0);

        // --- Modo de juego ---
        c.gridy = 0;
        p.add(etiqueta("Modo de juego"), c);
        JRadioButton normal = radio("Normal");
        JRadioButton contraste = radio("Alto contraste");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(normal);
        grupo.add(contraste);
        (Configuracion.isAltoContraste() ? contraste : normal).setSelected(true);
        normal.addActionListener(e -> Configuracion.setAltoContraste(false));
        contraste.addActionListener(e -> Configuracion.setAltoContraste(true));
        JPanel modos = new JPanel(new java.awt.FlowLayout(FlowLayoutLeft(), 18, 0));
        modos.setOpaque(false);
        modos.add(normal);
        modos.add(contraste);
        c.gridy = 1;
        p.add(modos, c);

        // --- Brillo ---
        c.gridy = 2;
        c.insets = new Insets(14, 0, 2, 0);
        JLabel lblBrillo = etiqueta("Brillo: " + Configuracion.getBrillo() + "%");
        p.add(lblBrillo, c);
        JSlider brillo = deslizador(40, 100, Configuracion.getBrillo());
        brillo.addChangeListener(e -> {
            Configuracion.setBrillo(brillo.getValue());
            lblBrillo.setText("Brillo: " + brillo.getValue() + "%");
        });
        c.gridy = 3;
        c.insets = new Insets(0, 0, 2, 0);
        p.add(brillo, c);

        // --- Música ---
        c.gridy = 4;
        c.insets = new Insets(14, 0, 2, 0);
        JLabel lblVol = etiqueta("Volumen del sonido: " + Configuracion.getVolumenMusica() + "%");
        p.add(lblVol, c);
        JSlider volumen = deslizador(0, 100, Configuracion.getVolumenMusica());
        volumen.setEnabled(Configuracion.isMusicaActiva());
        volumen.addChangeListener(e -> {
            Configuracion.setVolumenMusica(volumen.getValue());
            lblVol.setText("Volumen del sonido: " + volumen.getValue() + "%");
        });
        c.gridy = 5;
        c.insets = new Insets(0, 0, 2, 0);
        p.add(volumen, c);

        JCheckBox silencio = new JCheckBox("Silenciar sonido", !Configuracion.isMusicaActiva());
        estilo(silencio);
        silencio.addActionListener(e -> {
            Configuracion.setMusicaActiva(!silencio.isSelected());
            volumen.setEnabled(!silencio.isSelected());
        });
        c.gridy = 6;
        c.insets = new Insets(6, 0, 0, 0);
        p.add(silencio, c);
        return p;
    }

    private static int FlowLayoutLeft() {
        return java.awt.FlowLayout.LEFT;
    }

    private static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(FUENTE, Font.BOLD, 16));
        l.setForeground(CREMA);
        return l;
    }

    private static JRadioButton radio(String texto) {
        JRadioButton r = new JRadioButton(texto);
        estilo(r);
        return r;
    }

    private static void estilo(javax.swing.AbstractButton b) {
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setForeground(CREMA);
        b.setFont(new Font(FUENTE, Font.PLAIN, 15));
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    private static JSlider deslizador(int min, int max, int valor) {
        JSlider s = new JSlider(min, max, valor);
        s.setOpaque(false);
        s.setForeground(ORO);
        s.setFocusable(false);
        return s;
    }
}
