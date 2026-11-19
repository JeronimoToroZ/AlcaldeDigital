package influenceinc.logica;

import influenceinc.estructuras.Ordenamientos;
import influenceinc.modelo.RolJugador;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * NOTICIAS NOVA: el noticiero de la ciudad.
 *
 * Abre cada manana (lo que paso en la noche, una nota EN VIVO desde un
 * barrio, la mirada de tu ROL y un giro de ultima hora) y cierra cada noche
 * con los titulares que TU dejaste circular. Durante el dia lanza avisos de
 * "ultima hora" con lo que va pasando.
 *
 * Estructuras que usa:
 *  - los titulares de la noche se ordenan por impacto con INSERCION (son 3 y
 *    casi siempre llegan casi ordenados);
 *  - las tendencias de la app Noticias se ordenan con SHELL;
 *  - las notas en vivo solo salen de barrios a los que llega la senal, que
 *    crece cada dia siguiendo el arbol de PRIM desde el estudio.
 */
public final class Noticiero {

    public static final String CANAL = "Noticias Nova";
    public static final String PRESENTADORA = "Lucía Prado";
    public static final String REPORTERO = "Tato Rincón";

    /** Un bloque del noticiero. */
    public static class Segmento {
        /** Texto grande del rotulo (franja inferior). */
        public final String rotulo;
        /** Lo que dice quien presenta. */
        public final String texto;
        /** Quien habla: presentadora en el estudio o reportero en vivo. */
        public final String quien;
        /** Zona desde la que se transmite (-1 = estudio). */
        public final int zona;
        /** estudio, vivo, rol, giro, titular, ciudad. */
        public final String tipo;

        public Segmento(String rotulo, String texto, String quien, int zona, String tipo) {
            this.rotulo = rotulo;
            this.texto = texto;
            this.quien = quien;
            this.zona = zona;
            this.tipo = tipo;
        }
    }

    private Noticiero() {
    }

    // ==================================================================
    // Edicion de la manana
    // ==================================================================

    private static final String[][] APERTURA = {
        {},
        {"ARRANCA LA SEMANA ELECTORAL",
            "Buenos días, Ciudad Nova. Faltan cinco días para elegir alcalde y Civitas amaneció hirviendo: anoche se compartieron más rumores que en todo el mes."},
        {"SUPLANTADORES EN LAS AGENCIAS",
            "Ayer alguien se hizo pasar por funcionario en otra agencia y logró publicar un aviso falso. Hoy todas las ventanillas piden credencial."},
        {"LAS FOTOS TAMBIÉN MIENTEN",
            "Media ciudad compartió anoche una avenida inundada. La foto es real... pero nadie sabe de cuándo es. Y la guerra de las bebidas energéticas ya tiene abogados."},
        {"¿QUIÉN ESTÁ DETRÁS DE @la_verdad_nova?",
            "Una cuenta de seis días mueve más que cualquier medio de la ciudad. Hoy la pregunta no es qué dice, sino quién la empuja."},
        {"HOY SE ELIGE ALCALDE",
            "Abrieron las urnas. Hay veda electoral: nada de opiniones políticas en las agencias. Y en Civitas, ya empezó a llover de todo."}
    };

    private static final String[] VIVO = {
        "",
        "Aquí los conductores juran que el pasaje sube el lunes. ¿La fuente? \"Lo dijo un primo\". Nadie ha visto un decreto.",
        "Los vecinos hacen fila para llenar tanques. Un aviso de \"corte total\" circula con el nombre del Acueducto... pero sin sectores ni horario.",
        "Ni un charco. La avenida de la foto viral está seca. Los vecinos se ríen, pero algunos ya compraron botas.",
        "Aquí nadie conoce a quienes administran @la_verdad_nova. Los perfiles que la comparten no tienen ni una foto.",
        "Filas largas y ambiente tranquilo. Lo único raro: un audio que dice que \"los votos de hoy no sirven\"."
    };

    private static final String[] GIRO = {
        "",
        "Una cuenta nueva, @transporte_nova, ya tiene tres mil reenvíos. Fue creada hace cuatro días.",
        "Una \"funcionaria del Acueducto\" anda visitando agencias con un aviso urgente. El Acueducto dice que no envió a nadie.",
        "El \"doctor\" que asegura que Energy Max daña el cerebro no aparece en ningún colegio médico.",
        "ViralStorm, la agencia rival, estaría ofreciendo dinero a otras agencias por publicar contenido político.",
        "Don Ramiro Castaño cerró su oficina con llave y no contesta. Dicen que espera \"un cliente muy importante\"."
    };

    /** La edicion de la manana del dia (cambia segun el rol del jugador). */
    public static List<Segmento> manana(int dia, JugadorVerificador j) {
        List<Segmento> l = new ArrayList<>();
        if (dia < 1 || dia > Historia.DIAS) {
            return l;
        }
        l.add(new Segmento(APERTURA[dia][0], APERTURA[dia][1], PRESENTADORA, -1, "estudio"));
        int z = zonaEnVivo(dia, j.ciudad);
        l.add(new Segmento("EN VIVO DESDE " + CiudadEstado.ZONAS[z].toUpperCase(), VIVO[dia], REPORTERO, z, "vivo"));
        l.add(segmentoRol(dia, j, true));
        l.add(new Segmento("ÚLTIMA HORA", GIRO[dia], PRESENTADORA, -1, "giro"));
        return l;
    }

    /**
     * El barrio desde donde transmite el reportero: el ultimo al que llego la
     * senal (la cobertura crece por el arbol de Prim desde el estudio).
     */
    static int zonaEnVivo(int dia, CiudadEstado c) {
        List<Integer> cubiertos = c.barriosEnVivo(dia);
        int z = cubiertos.get(cubiertos.size() - 1);
        return z == 1 ? cubiertos.get(0) : z;      // la agencia no es noticia
    }

    private static Segmento segmentoRol(int dia, JugadorVerificador j, boolean manana) {
        RolJugador r = j.rol;
        String rotulo;
        String texto;
        switch (r) {
            case CIUDADANO:
                rotulo = "LA VOZ DE LOS BARRIOS";
                texto = manana ? CIUDADANO[dia] : (j.ciudad.desinformacion() < 25
                        ? "En tu cuadra hoy se habló más de datos que de rumores. La profe Luisa ya está pidiendo fuentes en el grupo."
                        : "En tu cuadra siguen circulando audios sin fuente. Mañana el grupo de vecinos te va a necesitar.");
                break;
            case INFLUENCER:
                rotulo = "TENDENCIAS";
                texto = manana ? INFLUENCER[dia] : (j.reputacion >= 55
                        ? "Tus seguidores aplauden que marques lo pagado y frenes lo falso. Ganaste seguidores... y credibilidad."
                        : "En tus comentarios te piden explicaciones. Con tu alcance, cada error se multiplica por mil.");
                break;
            case CANDIDATO:
                rotulo = "CAMINO A LA ALCALDÍA";
                texto = (manana ? CANDIDATO[dia] + " " : "") + "Tu intención de voto en la encuesta de Noticias Nova: "
                        + j.apoyoElectoral() + " %. Ramírez y Pérez siguen cerca.";
                break;
            default:
                rotulo = "SALA DE REDACCIÓN";
                texto = manana ? PERIODISTA[dia] : (j.aciertos >= j.errores
                        ? "Daniela Ruiz cierra la edición: \"Buen trabajo. Hoy la ciudad supo qué creer gracias a tu ventanilla.\""
                        : "Daniela Ruiz no está contenta: \"Mañana toca corregir. Una nota mala nos cuesta diez buenas\".");
                break;
        }
        return new Segmento(rotulo, texto, PRESENTADORA, -1, "rol");
    }

    private static final String[] CIUDADANO = {"",
        "En el grupo de tu cuadra ya van 214 mensajes antes de las ocho. Hoy tú decides qué se queda y qué se reenvía.",
        "Vecinos de Barrio Norte no salieron anoche por un audio anónimo. Tu barrio te está mirando.",
        "Una foto de la avenida inundada tiene a media ciudad comprando botas. ¿Alguien preguntó de cuándo es?",
        "Tu tía reenvió \"lo de la casa de Pérez\" a nueve grupos. Hoy te toca preguntar: ¿y la fuente?",
        "Hoy se vota. Si en tu cuadra alguien dice que los votos no sirven, ya sabes qué preguntar."};

    private static final String[] PERIODISTA = {"",
        "Daniela Ruiz, editora de Noticias Nova, te pide una sola cosa: ni una nota sin dos fuentes. Esta semana nos juzgan a todos.",
        "Exclusiva en camino: alguien suplanta funcionarios. Tu corresponsal en moto está listo para salir.",
        "La guerra de Energy Max ya tiene abogados. Cualquier \"estudio\" que llegue, revísalo dos veces.",
        "Fuentes anónimas señalan a ViralStorm detrás de @la_verdad_nova. Falta la prueba, y está en el grafo.",
        "Hoy cualquier video puede ser falso. Tu firma es lo único que no se puede falsificar."};

    private static final String[] INFLUENCER = {"",
        "Tu último video pasó las 80 mil vistas y tienes tres marcas esperando respuesta. Cada sello tuyo pesa por mil.",
        "La Superintendencia multará la publicidad que no se declare. Tu mánager dice \"tranqui\". No es tranqui.",
        "#AvenidaInundada es la tendencia número uno. La mitad de tus seguidores ya la compartió.",
        "ViralStorm ofrece plata por \"apoyar\" contenidos políticos. Tu bandeja está llena de propuestas.",
        "Veda electoral: hoy nada de opiniones políticas. Tus seguidores esperan que des el ejemplo."};

    private static final String[] CANDIDATO = {"",
        "La confianza en la información va a decidir esta elección.",
        "Tu jefa de campaña quiere responder duro a los rumores. La ciudad mira cómo respondes.",
        "Un audio falso con tu voz circula en dos barrios. Los rumores no tienen partido.",
        "La casa de Pérez, el soborno de Ramírez... las campañas sucias aprietan.",
        "Día de elecciones. Lo que circule hoy en Civitas puede pesar más que tus propuestas."};

    // ==================================================================
    // Edicion de la noche
    // ==================================================================

    private static final String[] AVANCE = {"",
        "Mañana: alguien con credencial del Acueducto quiere publicar algo \"urgente\".",
        "Mañana: una foto vale más que mil palabras... aunque sea de hace cinco años.",
        "Mañana: ViralStorm mueve sus fichas. Y trae billetera.",
        "Mañana se elige alcalde. Y Don Ramiro promete pasar por tu ventanilla.",
        ""};

    /** Impacto de una decision: lo que gano o perdio la ciudad (puntos) y a cuanta gente le llego. */
    public static int impacto(Jornada.Resultado r) {
        return Math.abs(r.puntos) + r.alcance * 2 + (r.advertencia ? 80 : 0);
    }

    /** La edicion de la noche: tus titulares del dia, ordenados por impacto (INSERCION). */
    public static List<Segmento> noche(Jornada j) {
        List<Segmento> l = new ArrayList<>();
        JugadorVerificador jv = j.jugador;
        l.add(new Segmento("EDICIÓN DE LA NOCHE · DÍA " + j.dia,
                j.dia == Historia.DIAS ? "Cierran las urnas en Ciudad Nova. Así se vivió la jornada electoral."
                        : "Buenas noches. Así terminó el día " + j.dia + " de la semana electoral en Ciudad Nova.",
                PRESENTADORA, -1, "estudio"));
        List<Jornada.Resultado> res = new ArrayList<>(j.getResultados());
        Ordenamientos.insercion(res, new Comparator<Jornada.Resultado>() {
            @Override
            public int compare(Jornada.Resultado a, Jornada.Resultado b) {
                return Integer.compare(impacto(b), impacto(a));     // de mayor a menor
            }
        });
        for (int i = 0; i < res.size() && i < 3; i++) {
            Jornada.Resultado r = res.get(i);
            String cola = r.efecto.acierto
                    ? (r.alcance > 0 ? " La información llegó a " + r.alcance + " cuentas de Civitas." : " La agencia frenó el daño a tiempo.")
                    : " Influence, Inc. queda bajo la lupa por su sello de " + r.veredicto.sello + ".";
            l.add(new Segmento(i == 0 ? "LO MÁS VISTO DEL DÍA" : "EN DESARROLLO", r.titular + "." + cola,
                    PRESENTADORA, -1, "titular"));
        }
        if (res.isEmpty()) {
            l.add(new Segmento("VENTANILLA CERRADA", "La Mesa de Verificación no selló nada hoy. Los rumores circularon sin freno.",
                    PRESENTADORA, -1, "titular"));
        }
        CiudadEstado c = jv.ciudad;
        StringBuilder sb = new StringBuilder("Termómetro de Civitas: la desinformación quedó en " + c.desinformacion()
                + " % y la confianza en " + c.confianza + ".");
        int conPanico = 0;
        for (int z = 2; z < c.panico.length; z++) {
            conPanico += c.panico[z] > 0 ? 1 : 0;
        }
        sb.append(conPanico == 0 ? " Ningún barrio reporta pánico." : " Hay pánico en " + conPanico
                + (conPanico == 1 ? " barrio." : " barrios."));
        if (j.dia == Historia.DIAS) {
            int flujo = c.flujoVotantes(), ideal = c.flujoVotantesIdeal();
            sb.append(" Por las calles llegaron ").append(flujo).append(" votantes por hora a las urnas, de ")
                    .append(ideal).append(" posibles (flujo máximo).");
        }
        int z = zonaEnVivo(j.dia, c);
        l.add(new Segmento("EN VIVO DESDE " + CiudadEstado.ZONAS[z].toUpperCase(), sb.toString(), REPORTERO, z, "ciudad"));
        l.add(segmentoRol(j.dia, jv, false));
        if (j.dia < Historia.DIAS && !jv.despedido) {
            l.add(new Segmento("MAÑANA EN NOTICIAS NOVA", AVANCE[j.dia], PRESENTADORA, -1, "giro"));
        }
        return l;
    }

    // ==================================================================
    // Ultima hora (durante el dia)
    // ==================================================================

    public static String ultimaHora(Jornada.Evento e, CiudadEstado c) {
        switch (e) {
            case APAGON: return "Apagón en el sector de la agencia: los computadores se reinician.";
            case VIRAL: return "Una publicación sin fuente se vuelve tendencia en Civitas en minutos.";
            case FILA: return "Llega un bus lleno a Influence, Inc.: la fila le da la vuelta a la cuadra.";
            case PANICO: return "El pánico salta de barrio en barrio. Vecinos piden calma en Civitas.";
            default: return "La agencia se toma un café. El reloj se detiene unos segundos.";
        }
    }

    // ==================================================================
    // Tendencias (app Noticias)
    // ==================================================================

    private static final String[][] TEMAS = {
        {},
        {"#AlzaDelPasaje", "#CaimánEnLa30", "#DebateNova", "#ColegioCerrado", "#EleccionesNova"},
        {"#CorteDeAgua", "#BarrioNorte", "#ValeFit", "#Acueducto", "#EleccionesNova"},
        {"#AvenidaInundada", "#EnergyMax", "#Lote2241", "#FotoVieja", "#EleccionesNova"},
        {"#LaVerdadNova", "#CasaDePérez", "#ViralStorm", "#DebateNova", "#EleccionesNova"},
        {"#YoVoto", "#VedaElectoral", "#VideoRamírez", "#Registraduría", "#EleccionesNova"}
    };

    /** Tendencias del dia: {tema, veces compartido}, de la mas compartida a la menos (SHELL SORT). */
    public static List<String[]> tendencias(int dia, long semilla, double avance) {
        List<String[]> l = new ArrayList<>();
        if (dia < 1 || dia > Historia.DIAS) {
            return l;
        }
        Random r = new Random(semilla * 13 + dia);
        for (String t : TEMAS[dia]) {
            int base = 800 + r.nextInt(9000);
            l.add(new String[]{t, String.valueOf((int) (base * (0.4 + avance * 1.6)))});
        }
        Ordenamientos.shell(l, new Comparator<String[]>() {
            @Override
            public int compare(String[] a, String[] b) {
                return Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1]));
            }
        });
        return l;
    }
}
