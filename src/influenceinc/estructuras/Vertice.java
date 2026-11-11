package influenceinc.estructuras;

import influenceinc.modelo.TipoCuenta;
import java.util.ArrayList;
import java.util.List;

/**
 * Un vertice del grafo social de Civitas: una cuenta de la red.
 *
 * Guarda tres grupos de datos que conviene no mezclar:
 *   1. Identidad y presentacion: id, nombre, tipo y posicion (x, y, z) en el
 *      espacio 3D donde se dibuja.
 *   2. Estructura del grafo: aristas que SALEN (a quien le llega lo que esta
 *      cuenta publica) y aristas que ENTRAN (de quien recibe informacion).
 *      Guardar las dos listas permite recorrer el grafo como dirigido (para
 *      propagar) y como no dirigido (para DFS de comunidades y Dijkstra).
 *   3. Estado del juego: que agencia tiene a esta cuenta como seguidora y si
 *      esta desinformada.
 */
public class Vertice {

    // ---------- identidad ----------
    public final String id;
    public final String nombre;
    public final TipoCuenta tipo;
    /** Comunidad o barrio al que pertenece (0..3). -1 para agencias e influencers. */
    public final int comunidad;

    // ---------- posicion en el mundo 3D ----------
    public double x, y, z;

    // ---------- datos de influencer ----------
    /** Seguidores que se muestran en pantalla (solo influencers). */
    public int seguidores;
    /** Lo que cuesta contratarlo por una campana. */
    public int precio;
    /** 0..1: que tanto le cree su audiencia. */
    public double credibilidad = 0.6;
    /** Texto corto con su caracteristica. */
    public String rasgo = "";

    // ---------- estructura (listas de adyacencia) ----------
    final List<Arista> salientes = new ArrayList<>();
    final List<Arista> entrantes = new ArrayList<>();

    // ---------- estado del juego ----------
    /** Indice de la agencia que tiene esta cuenta en su audiencia. -1 = nadie. */
    public int duenio = -1;
    /** true si la cuenta se creyo un bulo. */
    public boolean desinformado = false;
    /** true si ya le paso el bulo a alguien: a esa persona el desmentido le llega tarde. */
    public boolean yaRetransmitio = false;
    /** true si ya escucho un desmentido: no se la vuelve a convencer del mismo bulo. */
    public boolean escuchoDesmentido = false;
    /** Influencer contratado hoy por alguna agencia (no se puede contratar dos veces). */
    public int contratadoHoyPor = -1;
    /** Influencer que cancelo hoy por un evento. */
    public boolean noDisponibleHoy = false;

    public Vertice(String id, String nombre, TipoCuenta tipo, int comunidad,
                   double x, double y, double z) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.comunidad = comunidad;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Aristas que salen de esta cuenta (copia de solo lectura). */
    public List<Arista> getSalientes() {
        return new ArrayList<>(salientes);
    }

    /** Aristas que llegan a esta cuenta (copia de solo lectura). */
    public List<Arista> getEntrantes() {
        return new ArrayList<>(entrantes);
    }

    public int gradoSalida() {
        return salientes.size();
    }

    public int gradoEntrada() {
        return entrantes.size();
    }

    /** Busca la arista que va hacia otro vertice. null si no existe. */
    public Arista aristaHacia(Vertice destino) {
        for (Arista a : salientes) {
            if (a.destino == destino) {
                return a;
            }
        }
        return null;
    }

    public boolean esUsuario() {
        return tipo == TipoCuenta.USUARIO;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
