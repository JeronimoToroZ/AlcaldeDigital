package alcaldedigital.logica;

import alcaldedigital.modelo.Publicacion;
import alcaldedigital.modelo.TipoContenido;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Genera el feed de Civitas. El orden y la seleccion son ALEATORIOS, asi que
 * ninguna partida es igual a otra (requisito 8 del enunciado).
 */
public class BancoPublicaciones {

    /*
     * Banco de comentarios de Civitas: {texto, autor, tipo real, credibilidad base}.
     * El jugador solo ve el texto y el autor; el tipo lo tiene que deducir.
     *   VERDADERA     -> datos útiles con fuente clara.
     *   FALSA         -> acusaciones o noticias falsas que hacen daño.
     *   OPINION       -> gustos o posturas personales.
     *   SIN_CONFIRMAR -> "dicen que", "comunicado oficial" sin fuente: hay que verificar.
     */
    private static final Object[][] DATOS = {
        // ---- VERDADERA (compartir) ----
        {"La alcaldia publico el presupuesto de 2026 en su portal oficial.", "@alcaldia_nova", TipoContenido.VERDADERA, 92},
        {"El debate entre candidatos sera el viernes a las 7 pm.", "@civitas_oficial", TipoContenido.VERDADERA, 95},
        {"Se habilitaron tres nuevos puestos de votacion en el sur.", "@registraduria", TipoContenido.VERDADERA, 88},
        {"El colegio Nova inauguro su nueva biblioteca esta manana.", "@colegio_nova", TipoContenido.VERDADERA, 90},
        {"Habra transporte gratuito el dia de las elecciones.", "@movilidad_nova", TipoContenido.VERDADERA, 85},
        {"La jornada electoral inicia a las 8 am y cierra a las 4 pm.", "@registraduria", TipoContenido.VERDADERA, 97},
        {"La alcaldia habilito una linea para reportar desinformacion.", "@alcaldia_nova", TipoContenido.VERDADERA, 91},
        {"El puesto de salud central estara abierto todo el dia con atencion prioritaria.", "@salud_nova", TipoContenido.VERDADERA, 89},
        {"Ya puedes consultar tu puesto de votacion en la pagina oficial de la Registraduria.", "@registraduria", TipoContenido.VERDADERA, 94},

        // ---- FALSA (reportar) ----
        {"El candidato Juan quiere cerrar el colegio del barrio.", "@nova_alerta", TipoContenido.FALSA, 15},
        {"Manana cerraran todos los parques de la ciudad.", "@rumores_nova", TipoContenido.FALSA, 12},
        {"El alcalde actual esta robando dinero de la ciudad.", "@anonimo_88", TipoContenido.FALSA, 8},
        {"La campana de la candidata Ramirez recibio dinero ilegal.", "@denuncia_ya", TipoContenido.FALSA, 18},
        {"El candidato Juan fue visto pagando por votos.", "@filtracion_nova", TipoContenido.FALSA, 10},
        {"El funcionario publico se robo los fondos destinados a la salud.", "@la_verdad_nova", TipoContenido.FALSA, 14},
        {"Los que voten en el norte no seran contados, ya esta todo arreglado.", "@nova_despierta", TipoContenido.FALSA, 9},

        // ---- OPINION (ignorar) ----
        {"Creo que el candidato Perez no tiene experiencia suficiente.", "@vecino_luis", TipoContenido.OPINION, 50},
        {"Para mi, la mejor propuesta es la de la candidata Ramirez.", "@ana_c", TipoContenido.OPINION, 45},
        {"Pienso que los debates no sirven para nada.", "@pedro_m", TipoContenido.OPINION, 40},
        {"Me parece que la ciudad esta mejor que hace cuatro anos.", "@carlos_r", TipoContenido.OPINION, 48},
        {"Creo que deberian hacer mas foros en los barrios.", "@maria_p", TipoContenido.OPINION, 52},
        {"Creo que deberia perder el candidato Perez.", "@sofi_nova", TipoContenido.OPINION, 44},
        {"Los afiches de esta campana estan horribles, la verdad.", "@diseno_critico", TipoContenido.OPINION, 47},

        // ---- SIN_CONFIRMAR (verificar) ----
        {"Dicen que van a subir los impuestos apenas pasen las elecciones.", "@se_comenta", TipoContenido.SIN_CONFIRMAR, 38},
        {"Supuestamente van a eliminar el subsidio de agua despues de las elecciones.", "@alerta_nova", TipoContenido.SIN_CONFIRMAR, 35},
        {"Comunicado oficial: las urnas cerraran a mediodia por orden de la alcaldia.", "@info_urgente", TipoContenido.SIN_CONFIRMAR, 55},
        {"Se reporto una falla en el sistema de conteo de votos.", "@nova_urgente", TipoContenido.SIN_CONFIRMAR, 42},
        {"Informacion oficial: el voto electronico sera obligatorio para mayores de 60.", "@cadena_whatsapp", TipoContenido.SIN_CONFIRMAR, 50},
        {"Me contaron que cambiaron de lugar todas las mesas del barrio sur.", "@vecina_norte", TipoContenido.SIN_CONFIRMAR, 40},
        {"Ultima hora: suspenden el debate por amenazas. Pasalo a tus grupos.", "@reenviado_muchas_veces", TipoContenido.SIN_CONFIRMAR, 37}
    };

    private final Random azar;

    public BancoPublicaciones(long semilla) {
        this.azar = new Random(semilla);
    }

    public BancoPublicaciones() {
        this.azar = new Random();
    }

    /** Devuelve una baraja aleatoria de publicaciones para la partida. */
    public List<Publicacion> generarFeed(int cantidad) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < DATOS.length; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, azar);

        List<Publicacion> feed = new ArrayList<>();
        int total = Math.min(cantidad, DATOS.length);
        for (int i = 0; i < total; i++) {
            Object[] fila = DATOS[indices.get(i)];
            int credibilidadBase = (Integer) fila[3];
            // pequena variacion aleatoria: la misma publicacion no siempre entra igual
            int credibilidad = Math.max(1, Math.min(99, credibilidadBase + azar.nextInt(11) - 5));
            feed.add(new Publicacion(i + 1, (String) fila[0], (String) fila[1],
                    (TipoContenido) fila[2], credibilidad));
        }
        return feed;
    }

    /**
     * Elige una publicación al azar del tipo indicado. Si el tipo es null, de cualquier tipo.
     * El id lo pone quien llama (Partida), para que sea único dentro del AVL.
     */
    public Publicacion publicacionAleatoria(int id, TipoContenido tipo, List<String> yaUsados) {
        List<Object[]> candidatas = new ArrayList<>();
        for (Object[] fila : DATOS) {
            if ((tipo == null || fila[2] == tipo) && !yaUsados.contains((String) fila[0])) {
                candidatas.add(fila);
            }
        }
        if (candidatas.isEmpty()) {           // ya se usaron todas: se permiten repetidas
            for (Object[] fila : DATOS) {
                if (tipo == null || fila[2] == tipo) {
                    candidatas.add(fila);
                }
            }
        }
        Object[] fila = candidatas.get(azar.nextInt(candidatas.size()));
        int credibilidadBase = (Integer) fila[3];
        int credibilidad = Math.max(1, Math.min(99, credibilidadBase + azar.nextInt(11) - 5));
        return new Publicacion(id, (String) fila[0], (String) fila[1], (TipoContenido) fila[2], credibilidad);
    }

    /** Evento aleatorio que altera el ambiente de la ciudad entre turnos. */
    public String eventoAleatorio(alcaldedigital.modelo.EstadoCiudad ciudad) {
        int tirada = azar.nextInt(6);
        switch (tirada) {
            case 0:
                ciudad.aplicar(0, 0, -2, -1, 5, 3);
                return "EVENTO: aparecio un rumor sobre un candidato. La desinformacion subio.";
            case 1:
                ciudad.aplicar(3, 2, 1, 1, -2, 0);
                return "EVENTO: un medio local publico una noticia verificada.";
            case 2:
                ciudad.aplicar(0, -3, -4, -2, 3, 6);
                return "EVENTO: se desato una discusion entre ciudadanos en Civitas.";
            case 3:
                ciudad.aplicar(2, 3, 5, 4, -3, -4);
                return "EVENTO: arranco una campana de convivencia digital en la ciudad.";
            case 4:
                ciudad.aplicar(0, 0, 0, -3, 6, 2);
                return "EVENTO: una publicacion se volvio viral antes de poder verificarla.";
            default:
                ciudad.aplicar(1, 1, 1, 1, -1, -1);
                return "EVENTO: un ciudadano reporto contenido danino por su cuenta.";
        }
    }

    public Random getAzar() {
        return azar;
    }
}