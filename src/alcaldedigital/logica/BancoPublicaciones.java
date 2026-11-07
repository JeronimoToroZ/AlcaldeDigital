package alcaldedigital.logica;

import alcaldedigital.modelo.Cuenta;
import alcaldedigital.modelo.Publicacion;
import alcaldedigital.modelo.Senal;
import alcaldedigital.modelo.TipoContenido;
import java.util.ArrayList;
import java.util.Arrays;
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

    // ==================================================================
    // Feed con pistas: las 12 publicaciones que el jugador debe juzgar
    // ==================================================================

    /**
     * Construye las 12 publicaciones del juego, cada una con su cuenta, sus
     * senales visibles y la explicacion que se muestra al final.
     *
     * EL REPARTO ESTA PENSADO, no es casual:
     *   - 6 falsas con pistas de distinto tipo
     *   - 3 ciertas que PARECEN falsas (titular alarmista, dato increible)
     *   - 3 ciertas normales
     *
     * Las tres del medio son las importantes. Sin ellas el jugador aprende
     * "todo lo alarmante es mentira", que es otro error y tambien hace dano.
     *
     * Ademas hay dos parejas a proposito: el corte de agua falso (#5) y el corte
     * de agua cierto (#9) dicen casi lo mismo. Lo unico que los separa es la
     * evidencia: cuenta oficial, sector concreto, horario concreto y boletin citado.
     */
    public static List<Publicacion> publicacionesConPistas() {
        List<Publicacion> lista = new ArrayList<>();

        // --- Cuentas ---
        Cuenta alertaNova    = new Cuenta("Alerta Nova 24h", "@alerta_nova_24", 3, 41, false, null);
        Cuenta sinFiltro     = new Cuenta("Nova Sin Filtro", "@nova_sin_filtro", 730, 8400, false, "cuenta_sinfiltro.png");
        Cuenta transporte    = new Cuenta("Transporte Nova", "@transporte_nova", 11, 230, false, null);
        Cuenta laVerdad      = new Cuenta("La Verdad de Nova", "@la_verdad_nova", 6, 1900, false, "cuenta_laverdad.png");
        Cuenta vecinosUnidos = new Cuenta("Vecinos Unidos", "@vecinos_unidos_nova", 2, 12, false, null);
        Cuenta jenny         = new Cuenta("Jenny R.", "@jenny_nova", 1460, 600, false, "cuenta_jenny.png");

        Cuenta bomberos      = new Cuenta("Bomberos Nova", "@bomberos_nova", 2190, 22000, true, "cuenta_bomberos.png");
        Cuenta gestionRiesgo = new Cuenta("Gestión del Riesgo Nova", "@gestionriesgo_nova", 1825, 31000, true, "cuenta_gestionriesgo.png");
        Cuenta acueducto     = new Cuenta("Acueducto Nova", "@acueducto_nova", 2555, 18000, true, "cuenta_acueducto.png");
        Cuenta alcaldia      = new Cuenta("Alcaldía de Ciudad Nova", "@alcaldia_nova", 2920, 45000, true, "cuenta_alcaldia.png");
        Cuenta biblioteca    = new Cuenta("Biblioteca Municipal", "@biblioteca_nova", 1095, 3200, true, "cuenta_biblioteca.png");
        Cuenta movilidad     = new Cuenta("Movilidad Nova", "@movilidad_nova", 1640, 12000, true, "cuenta_movilidad.png");

        // ---------- 6 FALSAS ----------

        lista.add(new Publicacion(1,
                "URGENTE!! El colegio del barrio CIERRA la otra semana. Me lo mandó mi cuñada "
                + "que trabaja en la secretaría. REENVÍEN antes de que lo borren.",
                null, TipoContenido.FALSA, 12, alertaNova,
                Arrays.asList(Senal.TITULAR_ALARMISTA, Senal.CUENTA_RECIEN_CREADA,
                              Senal.TESTIMONIO_DE_TERCERO, Senal.SIN_FUENTE, Senal.URGENCIA_A_COMPARTIR),
                null, "hace 40 min", null,
                "La secretaría de educación publicó el calendario completo esa misma mañana y el "
                + "colegio no aparece en ninguna lista de cierres. Alguien que de verdad trabaje "
                + "ahí no tendría por qué ser anónimo."));

        lista.add(new Publicacion(2,
                "Miren cómo quedó la avenida después del aguacero de anoche. Esto es lo que no "
                + "muestran en las noticias.",
                null, TipoContenido.FALSA, 18, sinFiltro,
                Arrays.asList(Senal.FOTO_FUERA_DE_CONTEXTO, Senal.SIN_FUENTE),
                null, "hace 3 h", "post_inundacion.png",
                "La foto es real, pero no es de anoche ni de esta ciudad: es de una inundación de "
                + "2019 en otro país. Una búsqueda inversa de imagen la encuentra en tres notas de "
                + "prensa de ese año. Una foto verdadera también puede contar una mentira."));

        lista.add(new Publicacion(3,
                "Confirmado: suben el pasaje a $4.500 apenas pasen las elecciones. "
                + "Fuente: un grupo de WhatsApp de conductores.",
                null, TipoContenido.FALSA, 20, transporte,
                Arrays.asList(Senal.FUENTE_ES_OTRA_RED, Senal.CUENTA_RECIEN_CREADA),
                "un grupo de WhatsApp de conductores", "hace 5 h", null,
                "Un grupo de WhatsApp no es una fuente: es otro rumor con más pasos. La tarifa se "
                + "fija por decreto público y no hay ninguno radicado."));

        lista.add(new Publicacion(4,
                "El candidato Pérez tiene una casa de 2.000 millones en el exterior. "
                + "Ya se sabía, pero nadie dice nada.",
                null, TipoContenido.FALSA, 15, laVerdad,
                Arrays.asList(Senal.CUENTA_RECIEN_CREADA, Senal.SIN_FUENTE),
                null, "hace 1 h", null,
                "La declaración de bienes de los candidatos es pública y no aparece ninguna "
                + "propiedad en el exterior. \"Ya se sabía\" es la forma más vieja de no citar nada."));

        lista.add(new Publicacion(5,
                "ATENCION!! CORTE DE AGUA TODO EL FIN DE SEMANA EN TODA LA CIUDAD!! compartan por favor",
                null, TipoContenido.FALSA, 10, vecinosUnidos,
                Arrays.asList(Senal.TITULAR_ALARMISTA, Senal.CUENTA_RECIEN_CREADA,
                              Senal.SIN_SEGUIDORES, Senal.SIN_FUENTE, Senal.URGENCIA_A_COMPARTIR),
                null, "hace 20 min", null,
                "El acueducto anuncia los cortes con 48 horas de anticipación, por sector y con "
                + "horario, nunca \"toda la ciudad\". Su cuenta oficial no publicó nada. "
                + "Compárala con la publicación del acueducto: dicen casi lo mismo y una es cierta."));

        lista.add(new Publicacion(6,
                "Mi primo que es policía dice que esta noche no salgan del barrio Norte. "
                + "No puedo decir más.",
                null, TipoContenido.FALSA, 22, jenny,
                Arrays.asList(Senal.TESTIMONIO_DE_TERCERO, Senal.SIN_FUENTE),
                null, "hace 2 h", null,
                "Cuenta de hace cuatro años, persona real, y aun así es un rumor: nadie puede "
                + "confirmar lo que dijo un primo. Que una cuenta sea vieja no la hace confiable. "
                + "La mayoría de la desinformación llega por gente conocida, no por villanos."));

        // ---------- 3 CIERTAS QUE PARECEN FALSAS ----------

        lista.add(new Publicacion(7,
                "Encontraron un caimán en el caño de la calle 30. Lo sacaron nuestras unidades "
                + "esta mañana. Video en el reporte.",
                null, TipoContenido.VERDADERA, 93, bomberos,
                Collections.<Senal>emptyList(),
                "reporte oficial del cuerpo de bomberos", "hace 6 h", null,
                "Suena increíble y es cierto. Lo publicó el cuerpo de bomberos con video y número "
                + "de reporte. Que algo sea raro no lo hace falso: compruébalo, no lo descartes."));

        lista.add(new Publicacion(8,
                "ALERTA: emergencia por fuga de gas en la calle 45. Evacuar el sector. "
                + "Reporte completo en el portal oficial.",
                null, TipoContenido.VERDADERA, 90, gestionRiesgo,
                Arrays.asList(Senal.TITULAR_ALARMISTA),
                "portal oficial de gestión del riesgo", "hace 15 min", null,
                "Está en mayúsculas y da miedo, pero viene de la cuenta oficial de gestión del "
                + "riesgo, cita el reporte y da una dirección concreta. El tono alarmista es una "
                + "SEÑAL, no una prueba: aquí la alarma está justificada."));

        lista.add(new Publicacion(9,
                "Corte programado de agua el sábado de 6:00 a 14:00 en los barrios Norte y "
                + "Villa Marina por mantenimiento. Boletín 212 en nuestro portal.",
                null, TipoContenido.VERDADERA, 95, acueducto,
                Collections.<Senal>emptyList(),
                "boletín 212 del acueducto", "ayer", null,
                "Casi lo mismo que decía la publicación falsa del corte de agua. La diferencia "
                + "está en la evidencia: cuenta oficial verificada, sector específico, horario "
                + "específico y un boletín que puedes abrir y leer."));

        // ---------- 3 CIERTAS NORMALES ----------

        lista.add(new Publicacion(10,
                "El debate entre candidatos es el viernes a las 7:00 p. m. en el Teatro Municipal. "
                + "Entrada libre hasta llenar aforo.",
                null, TipoContenido.VERDADERA, 96, alcaldia,
                Collections.<Senal>emptyList(),
                "agenda oficial de la alcaldía", "hace 8 h", null,
                "Cuenta oficial, fecha, hora, lugar y condición de entrada. No hay nada que desconfiar."));

        lista.add(new Publicacion(11,
                "La biblioteca amplía horario: de lunes a sábado hasta las 8:00 p. m. desde el "
                + "próximo lunes.",
                null, TipoContenido.VERDADERA, 92, biblioteca,
                Collections.<Senal>emptyList(),
                "comunicado de la biblioteca municipal", "ayer", null,
                "Un dato aburrido de una cuenta oficial. La mayoría de la información cierta se ve así."));

        lista.add(new Publicacion(12,
                "El día de las elecciones habrá transporte gratuito en todas las rutas urbanas, "
                + "de 7:00 a 17:00.",
                null, TipoContenido.VERDADERA, 94, movilidad,
                Collections.<Senal>emptyList(),
                "resolución 0458 de movilidad", "hace 12 h", null,
                "Cuenta oficial verificada y una resolución con número. Eso es verificable en un minuto."));

        return lista;
    }

    /**
     * Baraja las 12 publicaciones con pistas para una partida.
     * A diferencia del feed antiguo, aqui NO se altera la credibilidad al azar:
     * la respuesta correcta tiene que ser siempre la misma.
     */
    public List<Publicacion> generarFeedConPistas(int cantidad) {
        List<Publicacion> todas = publicacionesConPistas();
        Collections.shuffle(todas, azar);
        return new ArrayList<>(todas.subList(0, Math.min(cantidad, todas.size())));
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