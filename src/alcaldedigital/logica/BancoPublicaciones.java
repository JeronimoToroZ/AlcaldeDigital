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

    private static final Object[][] DATOS = {
        {"El candidato Juan quiere cerrar el colegio del barrio.", "@nova_alerta", TipoContenido.FALSA, 15},
        {"La alcaldia publico el presupuesto de 2026 en su portal oficial.", "@alcaldia_nova", TipoContenido.VERDADERA, 92},
        {"Creo que el candidato Perez no tiene experiencia suficiente.", "@vecino_luis", TipoContenido.OPINION, 50},
        {"Manana cerraran todos los parques de la ciudad.", "@rumores_nova", TipoContenido.FALSA, 12},
        {"El alcalde actual esta robando dinero de la ciudad.", "@anonimo_88", TipoContenido.FALSA, 8},
        {"El debate entre candidatos sera el viernes a las 7 pm.", "@civitas_oficial", TipoContenido.VERDADERA, 95},
        {"Para mi, la mejor propuesta es la de la candidata Ramirez.", "@ana_c", TipoContenido.OPINION, 45},
        {"Se habilitaron tres nuevos puestos de votacion en el sur.", "@registraduria", TipoContenido.VERDADERA, 88},
        {"Dicen que van a subir los impuestos apenas pasen las elecciones.", "@se_comenta", TipoContenido.FALSA, 20},
        {"La campana de la candidata Ramirez recibio dinero ilegal.", "@denuncia_ya", TipoContenido.FALSA, 18},
        {"El colegio Nova inauguro su nueva biblioteca esta manana.", "@colegio_nova", TipoContenido.VERDADERA, 90},
        {"Pienso que los debates no sirven para nada.", "@pedro_m", TipoContenido.OPINION, 40},
        {"Habra transporte gratuito el dia de las elecciones.", "@movilidad_nova", TipoContenido.VERDADERA, 85},
        {"El candidato Juan fue visto pagando por votos.", "@filtracion_nova", TipoContenido.FALSA, 10},
        {"Me parece que la ciudad esta mejor que hace cuatro anos.", "@carlos_r", TipoContenido.OPINION, 48},
        {"La jornada electoral inicia a las 8 am y cierra a las 4 pm.", "@registraduria", TipoContenido.VERDADERA, 97},
        {"Van a eliminar el subsidio de agua despues de las elecciones.", "@alerta_nova", TipoContenido.FALSA, 22},
        {"Creo que deberian hacer mas foros en los barrios.", "@maria_p", TipoContenido.OPINION, 52},
        {"Se reporto una falla en el sistema de conteo de votos.", "@nova_urgente", TipoContenido.FALSA, 25},
        {"La alcaldia habilito una linea para reportar desinformacion.", "@alcaldia_nova", TipoContenido.VERDADERA, 91}
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