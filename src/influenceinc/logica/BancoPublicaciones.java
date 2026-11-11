package influenceinc.logica;

import influenceinc.modelo.Cuenta;
import influenceinc.modelo.Publicacion;
import influenceinc.modelo.Senal;
import influenceinc.modelo.TipoContenido;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Las publicaciones que circulan por Civitas, administradas como una BARAJA.
 *
 * Cada dia entran publicaciones nuevas al feed (el arbol AVL). Cuando una
 * agencia decide sobre una, sale del feed y va a la pila de descarte. Si la
 * baraja se acaba, el descarte se baraja de nuevo: asi una partida larga con
 * cuatro agencias nunca se queda sin publicaciones.
 *
 * El reparto de las 18 publicaciones esta pensado:
 *   - 8 falsas con pistas de distinto tipo
 *   - 3 ciertas que PARECEN falsas (titular alarmista, dato increible)
 *   - 5 ciertas normales
 *   - 2 opiniones
 * Sin las ciertas que parecen falsas el jugador aprenderia "todo lo alarmante
 * es mentira", que es otro error.
 */
public class BancoPublicaciones {

    private final Random azar;
    private final List<Publicacion> baraja = new ArrayList<>();
    private final List<Publicacion> descarte = new ArrayList<>();

    public BancoPublicaciones(Random azar) {
        this.azar = azar;
        baraja.addAll(publicacionesConPistas());
        Collections.shuffle(baraja, azar);
    }

    /** Todas las publicaciones de la partida (para buscarlas por id). */
    public List<Publicacion> todas() {
        List<Publicacion> l = new ArrayList<>(baraja);
        l.addAll(descarte);
        return l;
    }

    /** Saca la siguiente publicacion de la baraja (rebaraja el descarte si hace falta). */
    public Publicacion siguiente() {
        if (baraja.isEmpty()) {
            baraja.addAll(descarte);
            descarte.clear();
            Collections.shuffle(baraja, azar);
        }
        if (baraja.isEmpty()) {
            return null;
        }
        Publicacion p = baraja.remove(baraja.size() - 1);
        p.reiniciar();
        return p;
    }

    /** Devuelve una publicacion ya usada para que pueda volver a salir mas adelante. */
    public void descartar(Publicacion p) {
        if (p != null && !descarte.contains(p) && !baraja.contains(p)) {
            descarte.add(p);
        }
    }

    public static List<Publicacion> publicacionesConPistas() {
        List<Publicacion> lista = new ArrayList<>();

        // --- Cuentas ---
        Cuenta alertaNova    = new Cuenta("Alerta Nova 24h", "@alerta_nova_24", 3, 41, false, null);
        Cuenta sinFiltro     = new Cuenta("Nova Sin Filtro", "@nova_sin_filtro", 730, 8400, false, null);
        Cuenta transporte    = new Cuenta("Transporte Nova", "@transporte_nova", 11, 230, false, null);
        Cuenta laVerdad      = new Cuenta("La Verdad de Nova", "@la_verdad_nova", 6, 1900, false, null);
        Cuenta vecinosUnidos = new Cuenta("Vecinos Unidos", "@vecinos_unidos_nova", 2, 12, false, null);
        Cuenta jenny         = new Cuenta("Jenny R.", "@jenny_nova", 1460, 600, false, null);

        Cuenta bomberos      = new Cuenta("Bomberos Nova", "@bomberos_nova", 2190, 22000, true, null);
        Cuenta gestionRiesgo = new Cuenta("Gestión del Riesgo Nova", "@gestionriesgo_nova", 1825, 31000, true, null);
        Cuenta acueducto     = new Cuenta("Acueducto Nova", "@acueducto_nova", 2555, 18000, true, null);
        Cuenta alcaldia      = new Cuenta("Alcaldía de Ciudad Nova", "@alcaldia_nova", 2920, 45000, true, null);
        Cuenta biblioteca    = new Cuenta("Biblioteca Municipal", "@biblioteca_nova", 1095, 3200, true, null);
        Cuenta movilidad     = new Cuenta("Movilidad Nova", "@movilidad_nova", 1640, 12000, true, null);

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
                null, "hace 3 h", null,
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
                "ATENCIÓN!! CORTE DE AGUA TODO EL FIN DE SEMANA EN TODA LA CIUDAD!! compartan por favor",
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


        // ---------- 6 DEL MUNDO DE LAS MARCAS (Influence, Inc.) ----------

        Cuenta saludSinCensura = new Cuenta("Salud Sin Censura", "@salud_sin_censura", 9, 310, false, null);
        Cuenta energyMax       = new Cuenta("Energy Max", "@energymax_oficial", 1300, 52000, true, null);
        Cuenta donTotoFalso    = new Cuenta("Pizzería Don Toto ", "@pizzeria_dontoto_regalos", 1, 27, false, null);
        Cuenta noticiasNova    = new Cuenta("Noticias Nova", "@noticias_nova", 3100, 88000, true, null);
        Cuenta caro            = new Cuenta("Caro M.", "@caro_m", 900, 410, false, null);
        Cuenta pedro           = new Cuenta("Pedro M.", "@pedro_m", 1200, 280, false, null);

        lista.add(new Publicacion(13,
                "Energy Max tiene un ingrediente PROHIBIDO en Europa. Lo explico un nutricionista "
                + "en un video de TikTok. Compartan para que nadie más se lo tome!!",
                null, TipoContenido.FALSA, 14, saludSinCensura,
                Arrays.asList(Senal.FUENTE_ES_OTRA_RED, Senal.CUENTA_RECIEN_CREADA,
                              Senal.TITULAR_ALARMISTA, Senal.URGENCIA_A_COMPARTIR),
                "un video de TikTok", "hace 50 min", null,
                "Ningún organismo europeo prohibio ese ingrediente; el \"nutricionista\" del video "
                + "vende un suplemento de la competencia. Un video no es una fuente: es alguien "
                + "diciendo algo. Además la cuenta tiene nueve días."));

        lista.add(new Publicacion(14,
                "Retiramos el lote 2241 de Energy Max por un error en la etiqueta: dice 80 mg de "
                + "cafeína y tiene 120 mg. Lista de lotes y reembolso en nuestra web.",
                null, TipoContenido.VERDADERA, 91, energyMax,
                Collections.<Senal>emptyList(),
                "comunicado oficial con número de lote", "hace 4 h", null,
                "Una marca admitiendo un error contra si misma, con número de lote y forma de "
                + "reembolso. Es verificable y no le conviene inventarlo. Fijate que el bulo del "
                + "ingrediente prohibido y este aviso hablan del mismo producto."));

        lista.add(new Publicacion(15,
                "SOLO HOY: Pizzería Don Toto regala UN AÑO de pizza gratis a quien comparta este "
                + "post antes de las 12. Escríbenos tus datos por interno!!",
                null, TipoContenido.FALSA, 9, donTotoFalso,
                Arrays.asList(Senal.CUENTA_RECIEN_CREADA, Senal.SIN_SEGUIDORES,
                              Senal.URGENCIA_A_COMPARTIR, Senal.SIN_FUENTE),
                null, "hace 15 min", null,
                "Es una cuenta que copia el nombre de la pizzeria (mira el espacio extra y el "
                + "arroba raro). Fue creada ayer y pide datos por mensaje interno: es una estafa "
                + "para robar información personal, no una promocion."));

        lista.add(new Publicacion(16,
                "La Superintendencia multó a una agencia de la ciudad por publicar contenido "
                + "pagado sin marcarlo como publicidad. Resolución 1187 en el sitio oficial.",
                null, TipoContenido.VERDADERA, 93, noticiasNova,
                Collections.<Senal>emptyList(),
                "resolucion 1187 de la Superintendencia", "hace 7 h", null,
                "Medio verificado y una resolucion con número que cualquiera puede consultar. "
                + "Y es justo la regla de este juego: si te pagan por publicarlo, tu audiencia "
                + "tiene derecho a saberlo."));

        lista.add(new Publicacion(17,
                "Para mí la nueva campaña de Café Nova es la más aburrida del año. Ni la terminé de ver.",
                null, TipoContenido.OPINION, 50, caro,
                Collections.<Senal>emptyList(),
                null, "hace 2 h", null,
                "Es un gusto personal, no un hecho. Se puede compartir o no, pero no tiene nada "
                + "que verificar ni nada que reportar."));

        lista.add(new Publicacion(18,
                "Creo que los influencers no deberían hablar de política. Que se queden en lo suyo.",
                null, TipoContenido.OPINION, 45, pedro,
                Collections.<Senal>emptyList(),
                null, "ayer", null,
                "Una opinión que mucha gente no comparte. No estar de acuerdo con ella no la "
                + "convierte en desinformación."));

        return lista;
    }
}
