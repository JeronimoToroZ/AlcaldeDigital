package alcaldedigital.gui;

import alcaldedigital.modelo.Cuenta;
import alcaldedigital.modelo.Publicacion;
import alcaldedigital.modelo.Senal;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que el jugador ve cuando gasta una de sus verificaciones.
 *
 * LA REGLA DE DISENO MAS IMPORTANTE DE TODO EL JUEGO:
 * esta pantalla entrega EVIDENCIA, nunca el veredicto. En ningun lugar dice
 * "esta publicacion es falsa". Dice cuando se creo la cuenta, cuantos seguidores
 * tiene, que fuente cita y que encontro la busqueda inversa de la imagen.
 *
 * Quien decide sigue siendo el jugador. Si la pantalla diera la respuesta,
 * verificar seria apretar un boton que dice la verdad, y el jugador no aprenderia
 * a leer nada: aprenderia a gastar verificaciones.
 */
public class DialogoVerificacion extends JDialog {

    /** Ancho util de la columna de hallazgos. */
    private static final int ANCHO_TEXTO = 400;

    public DialogoVerificacion(Window padre, Publicacion publicacion, int verificacionesRestantes) {
        super(padre, "Verificación", ModalityType.APPLICATION_MODAL);

        JPanel raiz = new JPanel(new BorderLayout(18, 12));
        raiz.setBackground(Tema.fondo());
        raiz.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        raiz.add(cabecera(verificacionesRestantes), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(20, 0));
        centro.setOpaque(false);

        TarjetaPublicacion tarjeta = new TarjetaPublicacion(publicacion, true);
        JPanel contenedorTarjeta = new JPanel(new BorderLayout());
        contenedorTarjeta.setOpaque(false);
        contenedorTarjeta.add(tarjeta, BorderLayout.NORTH);
        centro.add(contenedorTarjeta, BorderLayout.WEST);

        JScrollPane scroll = new JScrollPane(hallazgos(publicacion));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(ANCHO_TEXTO + 24, 100));
        Tema.estilizarScroll(scroll);
        centro.add(scroll, BorderLayout.CENTER);

        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(pie(), BorderLayout.SOUTH);

        setContentPane(raiz);
        pack();
        setSize(new Dimension(Math.min(960, getWidth()), Math.min(620, getHeight() + 20)));
        setLocationRelativeTo(padre);
    }

    private JPanel cabecera(int restantes) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel titulo = new JLabel("Lo que encontramos");
        titulo.setFont(Tema.titulo());
        titulo.setForeground(Tema.texto());
        p.add(titulo, BorderLayout.WEST);

        JLabel contador = new JLabel(restantes + " verificaciones restantes");
        contador.setFont(Tema.indicadorNegrita());
        contador.setForeground(restantes == 0 ? Tema.negativo() : Tema.textoSuave());
        contador.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(contador, BorderLayout.EAST);

        return p;
    }

    /** La lista de hallazgos. Cada linea es un hecho comprobable, no una opinion. */
    private JPanel hallazgos(Publicacion publicacion) {
        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        lista.setOpaque(false);

        Cuenta c = publicacion.getCuenta();

        if (c != null) {
            lista.add(bloque("La cuenta",
                    c.getNombre() + " (" + c.getArroba() + ")\n"
                    + "· " + mayuscula(c.antiguedadTexto()) + "\n"
                    + "· " + c.seguidoresTexto() + "\n"
                    + "· " + (c.estaVerificada()
                             ? "Cuenta verificada por la plataforma"
                             : "Sin verificar")));
        }

        lista.add(bloque("La fuente",
                publicacion.getFuenteCitada() != null
                        ? "Cita: " + publicacion.getFuenteCitada()
                        : "No cita ninguna fuente que se pueda abrir y leer."));

        if (publicacion.getImagen() != null) {
            boolean fueraDeContexto = publicacion.getSenales().contains(Senal.FOTO_FUERA_DE_CONTEXTO);
            lista.add(bloque("La imagen",
                    fueraDeContexto
                            ? "La búsqueda inversa encuentra esta misma foto publicada en 2019, en "
                              + "notas de prensa de otro país. La foto es real; lo que no coincide "
                              + "es cuándo y dónde fue tomada."
                            : "La búsqueda inversa no encuentra esta imagen publicada antes."));
        }

        List<Senal> senales = new ArrayList<>(publicacion.getSenales());
        senales.remove(Senal.FOTO_FUERA_DE_CONTEXTO);   // ya se explicó arriba

        if (!senales.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Senal s : senales) {
                sb.append("· ").append(s.getEtiqueta()).append("\n   ")
                  .append(s.getExplicacion()).append("\n");
            }
            lista.add(bloque("Detalles del texto", sb.toString().trim()));
        }

        if (publicacion.getSenales().isEmpty()) {
            lista.add(bloque("Resultado",
                    "No encontramos nada que no cuadre: cuenta antigua, verificada y con "
                    + "una fuente que se puede consultar."));
        }

        lista.add(Box.createVerticalStrut(8));
        lista.add(bloque("Y ahora",
                "Con esto decides tú. La verificación no dice si es verdad o mentira: "
                + "muestra lo que se puede comprobar. El resto es tu criterio."));

        return lista;
    }

    private JPanel bloque(String titulo, String cuerpo) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        p.setMaximumSize(new Dimension(ANCHO_TEXTO, Integer.MAX_VALUE));

        JLabel t = new JLabel(titulo.toUpperCase());
        t.setFont(new Font("SansSerif", Font.BOLD, 11));
        t.setForeground(Tema.acento());
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(4));

        // JTextArea y no JLabel: el ancho en CSS de Swing no es fiable y deja
        // frases cortadas. Un area con ajuste de linea reparte el texto sola,
        // con el ancho real que tenga el panel.
        javax.swing.JTextArea c = new javax.swing.JTextArea(cuerpo);
        c.setLineWrap(true);
        c.setWrapStyleWord(true);
        c.setEditable(false);
        c.setFocusable(false);
        c.setOpaque(false);
        c.setBorder(null);
        c.setFont(Tema.normal());
        c.setForeground(Tema.texto());
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(ANCHO_TEXTO, Integer.MAX_VALUE));
        p.add(c);

        return p;
    }

    private JPanel pie() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        Tema.Boton cerrar = new Tema.Boton("Volver y decidir", Tema::acento);
        cerrar.setPreferredSize(new Dimension(200, 42));   // sin esto el texto se corta
        cerrar.addActionListener(e -> dispose());
        p.add(cerrar, BorderLayout.EAST);

        return p;
    }

    private static String mayuscula(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
