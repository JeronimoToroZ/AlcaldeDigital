package influenceinc.logica;

import influenceinc.estructuras.ArbolBinarioBusqueda;
import influenceinc.estructuras.TablaHash;
import influenceinc.modelo.CuentaOficial;
import influenceinc.modelo.RolJugador;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * EL CELULAR DEL JUGADOR (solo datos; el dibujo esta en gui/CelularVista).
 *
 *  - AGENDA: Arbol Binario de Busqueda ordenado por nombre. La app Telefono
 *    la muestra con un recorrido INORDEN (sale en orden alfabetico).
 *  - MENSAJES: tabla hash contacto -> conversacion. Buscar el chat de alguien
 *    es O(1) en promedio, sin recorrer todos los chats.
 *  - NOTIFICACIONES: cola FIFO. Los avisos se muestran uno por uno, en el
 *    orden en que llegaron.
 *
 * Cada jugador tiene su propio celular: en el duelo local no se leen los
 * mensajes del otro.
 */
public class Telefono {

    /** Un contacto de la agenda. */
    public static class Contacto {
        public final String nombre;
        public final String detalle;
        public final String numero;

        public Contacto(String nombre, String detalle, String numero) {
            this.nombre = nombre;
            this.detalle = detalle;
            this.numero = numero;
        }
    }

    /** Un mensaje de un chat. */
    public static class Mensaje {
        public final String texto;
        public final String hora;
        /** true = lo escribio el jugador. */
        public final boolean mio;

        public Mensaje(String texto, String hora, boolean mio) {
            this.texto = texto;
            this.hora = hora;
            this.mio = mio;
        }
    }

    /** Una conversacion con un contacto. */
    public static class Conversacion {
        public final String contacto;
        public final List<Mensaje> mensajes = new ArrayList<>();
        public int noLeidos;

        Conversacion(String contacto) {
            this.contacto = contacto;
        }

        public Mensaje ultimo() {
            return mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1);
        }
    }

    /** Lo que aparece arriba de la pantalla cuando llega algo. */
    public static class Notificacion {
        /** App que la manda: "Mensajes", "Civitas", "Noticias", "Teléfono"... */
        public final String app;
        public final String titulo;
        public final String texto;
        /** Si es importante el celular se abre solo; si no, solo vibra al lado. */
        public final boolean importante;

        public Notificacion(String app, String titulo, String texto, boolean importante) {
            this.app = app;
            this.titulo = titulo;
            this.texto = texto;
            this.importante = importante;
        }
    }

    public static final String JEFE = "Don Ramiro";
    public static final String CORRESPONSAL = "Corresponsal (moto)";
    public static final String INFORMANTE = "Informante";

    private final ArbolBinarioBusqueda<String, Contacto> agenda = new ArbolBinarioBusqueda<>();
    private final TablaHash<String, Conversacion> chats = new TablaHash<>();
    /** Chats del mas reciente al mas viejo (lo que se ve en la app Mensajes). */
    private final List<String> recientes = new ArrayList<>();
    private final Queue<Notificacion> notificaciones = new ArrayDeque<>();
    public final RolJugador rol;

    public Telefono(RolJugador rol, List<CuentaOficial> instituciones) {
        this.rol = rol;
        agendar(JEFE, "Dueño de Influence, Inc.", "300 555 0101");
        agendar(INFORMANTE, "Número privado", "Privado");
        agendar(contactoDelRol(rol), detalleDelRol(rol), "301 555 0" + (120 + rol.ordinal()));
        if (rol == RolJugador.PERIODISTA) {
            agendar(CORRESPONSAL, "Unidad móvil de la agencia", "302 555 0177");
        }
        agendar("Lucía Prado", "Presentadora · Noticias Nova", "605 330 4001");
        for (CuentaOficial c : instituciones) {
            if (!c.telefono.equals("-")) {
                agendar(c.institucion, c.representante + " · " + c.cargo, c.telefono);
            }
        }
    }

    /** Con quien habla cada rol durante la semana. */
    public static String contactoDelRol(RolJugador r) {
        switch (r) {
            case CIUDADANO: return "Vecinos Barrio Norte";
            case INFLUENCER: return "Kata (mánager)";
            case CANDIDATO: return "Lorena (jefa de campaña)";
            default: return "Daniela Ruiz (editora)";
        }
    }

    private static String detalleDelRol(RolJugador r) {
        switch (r) {
            case CIUDADANO: return "Grupo de la cuadra · 38 miembros";
            case INFLUENCER: return "Te maneja las marcas";
            case CANDIDATO: return "Tu campaña a la Alcaldía";
            default: return "Editora de Noticias Nova";
        }
    }

    // ------------------------------------------------------------------ agenda (ABB)
    public void agendar(String nombre, String detalle, String numero) {
        agenda.insertar(nombre, new Contacto(nombre, detalle, numero));
    }

    /** La agenda en orden alfabetico (inorden iterativo del ABB). */
    public List<Contacto> contactos() {
        return agenda.valoresEnOrden();
    }

    public Contacto buscarContacto(String nombre) {
        return agenda.buscar(nombre);
    }

    public ArbolBinarioBusqueda<String, Contacto> getAgenda() {
        return agenda;
    }

    // ------------------------------------------------------------------ mensajes (tabla hash)
    private Conversacion chat(String contacto) {
        Conversacion c = chats.obtener(contacto);
        if (c == null) {
            c = new Conversacion(contacto);
            chats.poner(contacto, c);
        }
        return c;
    }

    /** Llega un mensaje: se guarda en su chat, el chat sube al primer lugar y se encola el aviso. */
    public void recibir(String de, String texto, String hora, boolean importante) {
        Conversacion c = chat(de);
        c.mensajes.add(new Mensaje(texto, hora, false));
        c.noLeidos++;
        subir(de);
        notificaciones.add(new Notificacion("Mensajes", de, texto, importante));
    }

    /** El jugador responde (respuestas rapidas del chat). */
    public void responder(String a, String texto, String hora) {
        chat(a).mensajes.add(new Mensaje(texto, hora, true));
        subir(a);
    }

    private void subir(String contacto) {
        recientes.remove(contacto);
        recientes.add(0, contacto);
    }

    public void leer(String contacto) {
        Conversacion c = chats.obtener(contacto);
        if (c != null) {
            c.noLeidos = 0;
        }
    }

    public Conversacion conversacion(String contacto) {
        return chats.obtener(contacto);
    }

    public List<Conversacion> conversaciones() {
        List<Conversacion> l = new ArrayList<>();
        for (String n : recientes) {
            l.add(chats.obtener(n));
        }
        return l;
    }

    public int noLeidos() {
        int t = 0;
        for (String n : chats.claves()) {
            t += chats.obtener(n).noLeidos;
        }
        return t;
    }

    public TablaHash<String, Conversacion> getChats() {
        return chats;
    }

    // ------------------------------------------------------------------ notificaciones (cola)
    public void notificar(String app, String titulo, String texto, boolean importante) {
        notificaciones.add(new Notificacion(app, titulo, texto, importante));
    }

    /** Saca la siguiente notificacion (o null). */
    public Notificacion siguienteNotificacion() {
        return notificaciones.poll();
    }

    public int notificacionesPendientes() {
        return notificaciones.size();
    }
}
