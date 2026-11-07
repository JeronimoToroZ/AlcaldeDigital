package alcaldedigital.red;

/**
 * Vocabulario de mensajes entre el cliente y el servidor.
 *
 * El formato es el mismo del ejemplo visto en clase: texto plano, una linea
 * por mensaje, enviado con println y leido con readLine. No se usa
 * serializacion de objetos ni JSON a proposito:
 *
 *  - Se puede depurar leyendo la consola, o incluso con telnet.
 *  - No obliga a que las dos partes tengan las mismas clases compiladas.
 *  - Un error se ve a simple vista, en vez de aparecer como una excepcion
 *    de deserializacion sin contexto.
 *
 * Cada linea es COMANDO seguido, opcionalmente, de un argumento que ocupa
 * TODO el resto de la linea. Se parte por el primer espacio y nada mas, para
 * que un texto con espacios y tildes -que es lo normal en las publicaciones-
 * viaje sin romperse.
 *
 * Orden tipico de una partida:
 *
 *   cliente                      servidor
 *   -------                      --------
 *   NOMBRE Jeronimo       --->
 *                         <---   BIENVENIDA 0
 *                         <---   ESPERANDO 1 2
 *                         <---   ESPERANDO 2 2
 *                         <---   INICIO 4
 *                         <---   TURNO 1 Jeronimo TU
 *                         <---   PUBLICACION Dicen que van a cortar el agua
 *                         <---   CUENTA agua_ya|3 dias|8400|sin fuente
 *                         <---   CIUDAD 50|2|8
 *   ACCION REPORTAR       --->
 *                         <---   RESULTADO SI 10
 *                         <---   TURNO 2 Naty OTRO
 *                           ...
 *                         <---   FIN Jeronimo 70
 */
public final class Protocolo {

    /** Puerto por defecto. El mismo del ejemplo de clase. */
    public static final int PUERTO = 5000;

    /** Separador entre campos de un mismo argumento. */
    public static final String SEP = "|";

    // ---------------- del cliente hacia el servidor ----------------

    /** NOMBRE <nombre del jugador>. Primer mensaje al conectarse. */
    public static final String NOMBRE = "NOMBRE";

    /** ACCION <COMPARTIR|VERIFICAR|REPORTAR|IGNORAR>. */
    public static final String ACCION = "ACCION";

    /** DESMENTIR <nombre del lugar>. Habla con un vecino del mapa. */
    public static final String DESMENTIR = "DESMENTIR";

    /** SALIR. El jugador se va. */
    public static final String SALIR = "SALIR";

    // ---------------- del servidor hacia el cliente ----------------

    /** BIENVENIDA <numero de jugador>. */
    public static final String BIENVENIDA = "BIENVENIDA";

    /** ESPERANDO <conectados>|<esperados>. */
    public static final String ESPERANDO = "ESPERANDO";

    /** INICIO <total de turnos>. */
    public static final String INICIO = "INICIO";

    /** TURNO <numero>|<jugador>|<TU u OTRO>. */
    public static final String TURNO = "TURNO";

    /** PUBLICACION <texto>. */
    public static final String PUBLICACION = "PUBLICACION";

    /** CUENTA <usuario>|<antiguedad>|<seguidores>|<fuente>. */
    public static final String CUENTA = "CUENTA";

    /** CIUDAD <confianza>|<desinformados>|<total de vecinos>. */
    public static final String CIUDAD = "CIUDAD";

    /** MAPA <persona>=<R o V>|<persona>=<R o V>|... */
    public static final String MAPA = "MAPA";

    /** RESULTADO <SI o NO>|<cambio de confianza>|<explicacion>. */
    public static final String RESULTADO = "RESULTADO";

    /** AVISO <texto>. Algo que paso y el jugador deberia ver. */
    public static final String AVISO = "AVISO";

    /** FIN <ganador>|<confianza final>. */
    public static final String FIN = "FIN";

    /** ERROR <motivo>. */
    public static final String ERROR = "ERROR";

    private Protocolo() {
    }

    /** Arma una linea: comando, espacio y el argumento. */
    public static String linea(String comando, String argumento) {
        return argumento == null || argumento.isEmpty()
                ? comando
                : comando + " " + argumento;
    }

    /** Arma una linea uniendo varios campos con el separador. */
    public static String linea(String comando, Object... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(SEP);
            }
            sb.append(campos[i]);
        }
        return linea(comando, sb.toString());
    }

    /**
     * El comando de una linea recibida.
     * @return el comando en mayusculas, o cadena vacia si la linea es nula
     */
    public static String comando(String linea) {
        if (linea == null) {
            return "";
        }
        int corte = linea.indexOf(' ');
        return (corte < 0 ? linea : linea.substring(0, corte)).trim().toUpperCase();
    }

    /**
     * El argumento de una linea recibida: TODO lo que sigue al primer espacio.
     *
     * Se parte solo por el primer espacio para que el texto de una publicacion
     * -que trae espacios y tildes- llegue entero.
     *
     * @return el argumento, o cadena vacia si no hay
     */
    public static String argumento(String linea) {
        if (linea == null) {
            return "";
        }
        int corte = linea.indexOf(' ');
        return corte < 0 ? "" : linea.substring(corte + 1).trim();
    }

    /** Parte un argumento en sus campos. */
    public static String[] campos(String argumento) {
        return argumento.isEmpty()
                ? new String[0]
                : argumento.split("\\" + SEP, -1);
    }
}
