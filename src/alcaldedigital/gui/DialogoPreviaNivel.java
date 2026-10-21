package alcaldedigital.gui;

import alcaldedigital.estructuras.NodoNivel;
import alcaldedigital.logica.DificultadNivel;
import alcaldedigital.logica.Partida;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Ventanita antes de entrar a un nivel: muestra su imagen, el camino en el
 * grafo desde el nivel 1 y qué tan difícil es.
 */
public class DialogoPreviaNivel extends DialogoJuego {

    private boolean jugar;

    public DialogoPreviaNivel(Window padre, Partida partida, NodoNivel n) {
        super(padre, "Nivel " + n.getNumero());
        DificultadNivel d = new DificultadNivel(n.getNumero());

        Tarjeta t = new Tarjeta(new BorderLayout(18, 0));
        t.add(new JLabel(icono(n.getImagen(), 96)), BorderLayout.WEST);

        JPanel datos = new JPanel(new GridLayout(0, 1, 0, 4));
        datos.setOpaque(false);
        List<Integer> camino = partida.getNiveles().camino(1, n.getNumero());
        StringBuilder ruta = new StringBuilder();
        for (int i = 0; i < camino.size(); i++) {
            ruta.append(i == 0 ? "" : "  →  ").append("N").append(camino.get(i));
        }
        datos.add(etiqueta("Camino en el mapa: " + ruta, Font.ITALIC, 14, CREMA_SUAVE));
        datos.add(etiqueta(d.burbujasRequeridas + " burbujas correctas por publicación", Font.PLAIN, 15, CREMA));
        datos.add(etiqueta(d.segundosPorPublicacion + " segundos por publicación · 4 publicaciones",
                Font.PLAIN, 15, CREMA));
        datos.add(etiqueta(d.huecos + " huecos en el camino · 3 vidas", Font.PLAIN, 15, CREMA));
        if (d.amplitudMovimiento > 0) {
            datos.add(etiqueta("Las burbujas se mueven de lado a lado", Font.PLAIN, 15, ORO));
        }
        if (d.plataformasMoviles > 0) {
            datos.add(etiqueta("Hay plataformas móviles", Font.PLAIN, 15, ORO));
        }
        if (d.segundosCambioSimbolos > 0) {
            datos.add(etiqueta("¡Los símbolos cambian cada " + d.segundosCambioSimbolos + " s!", Font.BOLD, 15, ROJO));
        }
        if (n.isCompletado()) {
            datos.add(etiqueta("Ya lo ganaste con " + n.getEstrellas() + " estrella(s). Puedes mejorar.",
                    Font.ITALIC, 14, VERDE));
        }
        t.add(datos, BorderLayout.CENTER);
        setContenido(t);

        BotonDorado si = new BotonDorado("¡Jugar!", true);
        si.addActionListener(e -> { jugar = true; dispose(); });
        BotonDorado no = new BotonDorado("Cancelar", false);
        no.addActionListener(e -> dispose());
        agregarBoton(si);
        agregarBoton(no);
        getRootPane().setDefaultButton(si);
    }

    /** Muestra el diálogo; devuelve true si el jugador quiere entrar al nivel. */
    public boolean elegir() {
        mostrar();
        return jugar;
    }
}
