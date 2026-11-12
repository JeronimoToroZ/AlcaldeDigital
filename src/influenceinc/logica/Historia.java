package influenceinc.logica;

import influenceinc.estructuras.ArbolDialogo;
import influenceinc.estructuras.NodoDialogo;
import influenceinc.modelo.Credencial;
import influenceinc.modelo.Cuenta;
import influenceinc.modelo.CuentaOficial;
import influenceinc.modelo.Persona;
import influenceinc.modelo.Publicacion;
import influenceinc.modelo.Rasgos;
import influenceinc.modelo.RolVisitante;
import influenceinc.modelo.Senal;
import influenceinc.modelo.TipoContenido;
import influenceinc.modelo.Veredicto;
import influenceinc.modelo.Verdad;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * LA HISTORIA: cinco dias en la Mesa de Verificacion de Influence, Inc.
 * durante la semana de elecciones de Ciudad Nova.
 *
 * Aqui estan todos los casos, las llamadas del jefe, las reglas del manual
 * y los finales. Los rasgos de cada personaje salen de la semilla: cada
 * partida se ve distinta, pero la misma persona se ve igual dentro de una.
 */
public final class Historia {

    public static final int DIAS = 5;

    public static final String[] TITULOS = {"", "Primer día", "El directorio", "Las fotos mienten",
        "La red", "Día de elecciones"};

    /** Lo que dice Don Ramiro por telefono cada manana. */
    public static final String[][] LLAMADAS = {
        {},
        {"Aló, ¿hablo con el nuevo verificador? Soy Ramiro Castaño, el dueño de Influence, Inc.",
            "Esta semana son las elecciones de alcalde y todo el mundo quiere publicar algo con nuestro sello.",
            "Tu trabajo es simple: la gente llega a tu ventanilla, tú investigas y SELLAS.",
            "Regla número uno: nada sale sin respaldo. Hoy tienes el ESCÁNER DE PERFIL en el computador.",
            "Y pregúntales cosas. La gente habla más de la cuenta. Suerte, campeón."},
        {"Buenos días. Malas noticias: ayer alguien se hizo pasar por funcionario en otra agencia.",
            "Desde hoy tienes el DIRECTORIO OFICIAL: busca la cuenta y llama a la institución.",
            "También la LUPA: compara la cara de quien tienes enfrente con la foto de su credencial.",
            "Y regla nueva de la Superintendencia: lo que nos pagan se marca #PUBLICIDAD. Tienes un sello nuevo.",
            "Ah, y hoy viene la ingeniera del Acueducto. Atiéndela bien, es buena clienta."},
        {"Las fotos mienten, mijo. Ayer media ciudad compartió una inundación que pasó hace cinco años.",
            "Te conseguí BÚSQUEDA INVERSA de imágenes y un CORRESPONSAL en moto.",
            "El corresponsal va hasta el barrio y te cuenta lo que ve, pero se demora según la distancia.",
            "La Alcaldía cerró su cuenta vieja; ya la saqué del directorio.",
            "Ojo con la competencia: Energy Max anda en guerra con otra marca."},
        {"ViralStorm, la agencia de la competencia, nos está quitando clientes. No me gusta.",
            "Y hay una cuenta, @la_verdad_nova, que está moviendo algo grande contra los candidatos.",
            "Te instalé el ESCÁNER DE RED: muestra quién compartió primero cada cosa.",
            "Si ves muchas cuentas falsas conectadas entre sí, eso no es gente: es una campaña.",
            "No me falles hoy, que mañana son las elecciones."},
        {"Hoy se elige alcalde. Hay VEDA ELECTORAL: no se publican opiniones políticas. Te quité ese sello.",
            "La Registraduría abrió cuenta nueva; ya está en el directorio.",
            "Hoy va a llover de todo. Rápido y sin errores.",
            "Y al final del día paso por tu ventanilla. Tengo un cliente muy importante.",
            "...Uno muy importante."}
    };

    /** Paginas del manual (se agregan reglas cada dia). */
    public static final String[][] MANUAL = {
        {},
        {"VERIFICADO: solo si es cierto y se puede respaldar (cuenta oficial, fuente concreta).",
            "OPINIÓN: gustos y opiniones se publican marcados como opinión. No son mentira ni verdad.",
            "RECHAZAR: si es falso, aunque quien lo trae sea buena gente que se lo creyó.",
            "REPORTAR: si quien lo trae es una cuenta falsa o un impostor. Se reporta y se elimina.",
            "Señales de alerta: cuenta recién creada, pocos seguidores, sin fuente, pide reenviar ya.",
            "Una señal es una PISTA, no una prueba: hay verdades alarmistas."},
        {"DIRECTORIO: toda cuenta oficial está registrada. Un solo carácter distinto = cuenta falsa.",
            "LUPA: la cara de la persona debe coincidir con la foto de la credencial. Busca UN detalle.",
            "LLAMADA: la institución te confirma si publicó o no.",
            "#PUBLICIDAD: si le pagan al que publica, se declara. Si no lo declara, se etiqueta."},
        {"BÚSQUEDA INVERSA: una foto real puede ser de otro año u otra ciudad.",
            "CORRESPONSAL: va por la ruta más corta (Dijkstra en el mapa). Más lejos = más demora."},
        {"ESCÁNER DE RED: si los primeros en compartir son cuentas falsas conectadas entre sí, es una campaña organizada."},
        {"VEDA ELECTORAL: hoy no hay sello OPINIÓN.",
            "Un video también puede ser falso (deepfake). Revisa siempre la fuente."}
    };

    private Historia() {
    }

    // ==================================================================
    // Directorio oficial (se guarda en un AVL)
    // ==================================================================

    public static List<CuentaOficial> directorioInicial() {
        return new ArrayList<>(Arrays.asList(
            new CuentaOficial("@alcaldia_nova", "Alcaldía de Ciudad Nova", 2920, "Rosa Méndez", "Jefa de prensa", "AL-1021", "605 320 1100"),
            new CuentaOficial("@alcaldia_nova_2019", "Alcaldía (cuenta antigua)", 2600, "-", "-", "-", "-"),
            new CuentaOficial("@bomberos_nova", "Cuerpo de Bomberos", 2190, "Sgto. Iván Ortiz", "Comunicaciones", "BO-3307", "605 320 1190"),
            new CuentaOficial("@acueducto_nova", "Acueducto de Ciudad Nova", 2555, "Ing. Mónica Ríos", "Directora de operaciones", "AC-2214", "605 320 1500"),
            new CuentaOficial("@gestionriesgo_nova", "Gestión del Riesgo", 1825, "Ing. Hugo Peña", "Coordinador", "GR-0912", "605 320 1777"),
            new CuentaOficial("@movilidad_nova", "Secretaría de Movilidad", 1640, "Lina Vargas", "Prensa", "MO-5510", "605 320 1300"),
            new CuentaOficial("@biblioteca_nova", "Biblioteca Municipal", 1095, "Ana Lucía Paz", "Directora", "BI-0331", "605 320 1600"),
            new CuentaOficial("@noticias_nova", "Noticias Nova (medio)", 3100, "Daniela Ruiz", "Periodista", "NN-7781", "605 330 4000"),
            new CuentaOficial("@energymax_oficial", "Energy Max", 1300, "Andrés Gil", "Community manager", "EM-0045", "605 340 8080"),
            new CuentaOficial("@campana_ramirez", "Campaña Ramírez", 120, "Julián Soto", "Jefe de prensa", "CR-0007", "605 350 1111"),
            new CuentaOficial("@campana_perez", "Campaña Pérez", 118, "Sara Lozano", "Jefa de prensa", "CP-0008", "605 350 2222")
        ));
    }

    public static CuentaOficial registraduria() {
        return new CuentaOficial("@registraduria_nova", "Registraduría", 4, "Óscar Durán", "Delegado electoral", "RG-0501", "605 360 0000");
    }

    // ==================================================================
    // Casos
    // ==================================================================

    private static Map<Integer, Publicacion> publicaciones() {
        Map<Integer, Publicacion> m = new HashMap<>();
        for (Publicacion p : BancoPublicaciones.publicacionesConPistas()) {
            m.put(p.getId(), p);
        }
        Cuenta valeFit = new Cuenta("Vale Fit", "@valefit", 1200, 12000, false, null);
        Cuenta seComenta = new Cuenta("Se Comenta Nova", "@se_comenta", 20, 300, false, null);
        Cuenta registraduria = new Cuenta("Registraduría Nova", "@registraduria_nova", 4, 9000, true, null);
        Cuenta novaUrgente = new Cuenta("Nova Urgente", "@nova_urgente", 2, 40, false, null);
        Cuenta laVerdad = new Cuenta("La Verdad de Nova", "@la_verdad_nova", 6, 1900, false, null);
        Cuenta acueductoFalso = new Cuenta("Acueducto Nova Oficial", "@acueducto_nova_oficial", 4, 210, false, null);
        Cuenta salud = new Cuenta("Dr. Julio Mena", "@dr_julio_salud", 12, 860, false, null);
        Cuenta campana = new Cuenta("Campaña Ramírez", "@campana_ramirez", 120, 15000, true, null);

        m.put(19, new Publicacion(19, "Mi secreto para entrenar a full: un Energy Max cada mañana. ¡Pruébenlo, "
                + "no se van a arrepentir!", null, TipoContenido.OPINION, 55, valeFit,
                Collections.<Senal>emptyList(), null, "hace 1 h", null,
                "Le pagaron por la publicación y no lo dice. La ley exige marcarla como publicidad."));
        m.put(20, new Publicacion(20, "Dicen que van a subir los impuestos apenas pasen las elecciones. Pásalo.",
                null, TipoContenido.FALSA, 20, seComenta, Arrays.asList(Senal.SIN_FUENTE, Senal.URGENCIA_A_COMPARTIR),
                null, "hace 3 h", null, "No hay ningún proyecto de impuestos radicado. \"Dicen que\" no es una fuente."));
        m.put(21, new Publicacion(21, "La jornada electoral del domingo va de 8:00 a.m. a 4:00 p.m. Consulta tu "
                + "puesto de votación en la página oficial.", null, TipoContenido.VERDADERA, 96, registraduria,
                Collections.<Senal>emptyList(), "calendario electoral oficial", "hace 2 h", null,
                "Cuenta nueva, sí, pero está en el directorio oficial y la Registraduría lo confirma."));
        m.put(22, new Publicacion(22, "URGENTE: se cayó el sistema de conteo de votos. Los resultados de hoy NO "
                + "van a ser válidos. ¡Compartan!", null, TipoContenido.FALSA, 8, novaUrgente,
                Arrays.asList(Senal.TITULAR_ALARMISTA, Senal.CUENTA_RECIEN_CREADA, Senal.SIN_FUENTE,
                        Senal.URGENCIA_A_COMPARTIR), null, "hace 10 min", null,
                "La Registraduría no reporta ninguna falla. El bulo buscaba que la gente no fuera a votar."));
        m.put(23, new Publicacion(23, "VIDEO: la candidata Ramírez entregando sobres con plata a cambio de votos. "
                + "Mírenlo antes de que lo borren.", null, TipoContenido.FALSA, 6, laVerdad,
                Arrays.asList(Senal.TITULAR_ALARMISTA, Senal.CUENTA_RECIEN_CREADA, Senal.URGENCIA_A_COMPARTIR),
                null, "hace 5 min", "video_ramirez",
                "Es un deepfake: la cara de la candidata está pegada sobre el cuerpo de otra persona y la voz es sintética."));
        m.put(25, new Publicacion(25, "AVISO: CORTE DE AGUA TOTAL todo el fin de semana en toda la ciudad. "
                + "Llenen tanques ya.", null, TipoContenido.FALSA, 9, acueductoFalso,
                Arrays.asList(Senal.TITULAR_ALARMISTA, Senal.CUENTA_RECIEN_CREADA), null, "hace 25 min", null,
                "La cuenta copia el nombre del Acueducto pero no es la oficial (@acueducto_nova). El Acueducto anuncia "
                + "cortes por sector, con horario y 48 horas antes."));
        m.put(26, new Publicacion(26, "Estudio de la Universidad del Atlántico Norte: Energy Max causa daño cerebral "
                + "en jóvenes. Compártelo con tus hijos.", null, TipoContenido.FALSA, 12, salud,
                Arrays.asList(Senal.TITULAR_ALARMISTA, Senal.SIN_FUENTE, Senal.URGENCIA_A_COMPARTIR), null,
                "hace 2 h", null, "Esa universidad no existe y el \"doctor\" vende un suplemento de la competencia."));
        m.put(27, new Publicacion(27, "Recordamos: el debate final es el viernes 7:00 p.m. en el Teatro Municipal, "
                + "con transmisión en vivo.", null, TipoContenido.VERDADERA, 95, campana,
                Collections.<Senal>emptyList(), "agenda oficial del debate", "hace 4 h", null,
                "Dato verificable: la Alcaldía y el teatro publicaron la misma agenda."));
        return m;
    }

    /** Maximo de personas (casos) por dia: la jornada es mas agil. */
    public static final int MAX_CASOS_DIA = 3;

    /**
     * Los casos de un dia, en orden de llegada (maximo 3 por dia).
     *
     * Se escogieron para que cada dia tenga variedad (algo verdadero, algo
     * falso y un impostor o publicidad) y para conservar la historia: la
     * impostora del Acueducto (dia 2), la foto vieja (dia 3), el soborno de
     * $500 (dia 4) y el deepfake del jefe (dia 5). Los demas casos de la
     * historia (Pedro, Monica real, Hugo, la pizzeria, Daniela, la senora y
     * Lina) quedan como reserva en este archivo.
     */
    public static List<Caso> casosDelDia(int dia, long semilla) {
        Map<Integer, Publicacion> p = publicaciones();
        Random r = new Random(semilla * 31 + dia);
        List<Caso> l = new ArrayList<>();
        switch (dia) {
            case 1:
                l.add(carmen(p, semilla));
                l.add(ortiz(p, semilla));
                l.add(kevin(p, semilla));
                break;
            case 2:
                l.add(jenny(p, semilla));
                l.add(monicaFalsa(p, semilla, r));
                l.add(valeFit(p, semilla));
                break;
            case 3:
                l.add(brayan(p, semilla));
                l.add(doctor(p, semilla, r));
                l.add(andres(p, semilla));
                break;
            case 4:
                l.add(caro(p, semilla));
                l.add(ricky(p, semilla));
                l.add(julian(p, semilla));
                break;
            default:
                l.add(oscar(p, semilla));
                l.add(urgente(p, semilla));
                l.add(ramiro(p, semilla));
                break;
        }
        while (l.size() > MAX_CASOS_DIA) {
            l.remove(l.size() - 1);
        }
        return l;
    }

    /** Casos de reserva (no se usan con el maximo de 3 por dia, se conservan para futuras versiones). */
    static List<Caso> casosDeReserva(long semilla) {
        Map<Integer, Publicacion> p = publicaciones();
        List<Caso> l = new ArrayList<>();
        l.add(pedro(p, semilla));
        l.add(monicaReal(p, semilla));
        l.add(hugo(p, semilla));
        l.add(pizzeria(p, semilla));
        l.add(daniela(p, semilla));
        l.add(senora(p, semilla));
        l.add(lina(p, semilla));
        return l;
    }

    private static Rasgos cara(String nombre, long semilla) {
        Rasgos r = Rasgos.azar(new Random(semilla ^ nombre.hashCode() * 2654435761L));
        if (Arrays.asList(MUJERES).contains(nombre)) {
            r.barba = 0;
            if (r.pelo == 0 || r.pelo == 1 || r.pelo == 5) {
                r.pelo = r.pelo == 1 ? 2 : 4;
            }
        }
        return r;
    }

    private static final String[] MUJERES = {"Doña Carmen", "Jenny Rueda", "Ing. Mónica Ríos", "Vale Fit",
        "Daniela Ruiz", "Caro M.", "Yolanda", "Lina Vargas"};

    private static Persona persona(String nombre, RolVisitante rol, long semilla) {
        return new Persona(nombre, rol, cara(nombre, semilla));
    }

    private static Credencial credencial(Persona per, String cargo, String inst, String num) {
        return new Credencial(per.nombre, cargo, inst, num, "12/2027", per.cara.copia());
    }

    // ------------------------------------------------------------ dia 1
    private static Caso carmen(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Doña Carmen", RolVisitante.CIUDADANO, s);
        per.cara.edad = 2;
        ArbolDialogo d = new ArbolDialogo("Buenos días, mijo. Mire lo que me llegó: ¡van a subir el pasaje! "
                + "Hay que avisarle a todo el mundo.", "nerviosa");
        NodoDialogo n = d.pregunta("¿De dónde sacó la información?", "Me lo mandaron por el grupo de WhatsApp de "
                + "los conductores. Mi sobrino maneja buseta.", "neutral", "La fuente es un grupo de WhatsApp");
        n.hijo(new NodoDialogo("¿Su sobrino lo vio en algún documento?", "No... él dice que lo escuchó en el "
                + "parqueadero. Pero todos lo están diciendo.", "nerviosa", "Nadie vio un documento oficial"));
        d.pregunta("¿Usted administra la cuenta @transporte_nova?", "¿Yo? No, mijo. Esa cuenta la abrió alguien "
                + "del grupo hace unos días. Yo solo le hice captura.", "neutral", "La cuenta tiene pocos días");
        d.pregunta("¿Por qué quiere que la publiquemos?", "Porque la gente tiene que saber. Si no, nos agarran "
                + "desprevenidos.", "feliz", null);
        Caso c = new Caso(1, per, p.get(3), Verdad.FALSA, Veredicto.RECHAZAR, d);
        c.peticion = "Quiero que avisen que sube el pasaje.";
        c.zona = 4;
        c.marcadas = 40;
        c.reporteCalle = "En el Mercado nadie sabe nada del alza. Los conductores dicen que es un rumor.";
        c.defensa = "Ay, mijo, yo no sabía. Yo solo quería avisar.";
        c.titularBien = "Influence, Inc. frena el rumor del alza del pasaje";
        c.titularMal = "Pánico en el Mercado por un alza de pasaje que no existe";
        return c;
    }

    private static Caso ortiz(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Sgto. Iván Ortiz", RolVisitante.FUNCIONARIO, s);
        per.credencial = credencial(per, "Comunicaciones", "Cuerpo de Bomberos", "BO-3307");
        ArbolDialogo d = new ArbolDialogo("Buenos días. Cuerpo de Bomberos. Necesitamos que esto salga hoy, "
                + "la gente no nos cree.", "neutral");
        d.pregunta("¿Un caimán? ¿En serio?", "Ja, todos dicen lo mismo. Mide dos metros. Lo sacamos del caño de "
                + "la 30 esta mañana. Hay video en el reporte.", "feliz", "Hay video y número de reporte");
        d.pregunta("¿Me da el número del reporte?", "Reporte 0441 de hoy. Lo puede pedir en la línea de "
                + "bomberos.", "neutral", "Reporte oficial 0441");
        d.pregunta("¿Por qué viene en persona?", "Porque la última vez nos acusaron de inventar cosas. Queremos "
                + "el sello de una agencia seria.", "neutral", null);
        Caso c = new Caso(1, per, p.get(7), Verdad.VERDADERA, Veredicto.PUBLICAR, d);
        c.peticion = "Publiquen el rescate del caimán, por favor.";
        c.zona = 7;
        c.origenTipo = "MEDIO";
        c.llamada = "Bomberos: \"Sí, el rescate del caimán es nuestro. Reporte 0441.\"";
        c.reporteCalle = "En el caño de la 30 todavía están los bomberos. Hay vecinos grabando. Es real.";
        c.titularBien = "El caimán de la calle 30 era real: Bomberos lo rescató";
        c.titularMal = "Nadie creyó el rescate del caimán y Bomberos queda mal";
        return c;
    }

    private static Caso pedro(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Pedro Mejía", RolVisitante.CIUDADANO, s);
        ArbolDialogo d = new ArbolDialogo("Hola. Quiero decir lo que pienso y que lo lea mucha gente.", "neutral");
        d.pregunta("¿Esto es un dato o lo que usted piensa?", "Lo que pienso. Es mi opinión, ¿y qué? Tengo "
                + "derecho.", "enojado", "Él mismo dice que es una opinión");
        d.pregunta("¿Tiene alguna fuente?", "No necesito fuente para pensar algo.", "neutral", null);
        d.pregunta("¿Le pagó alguien por esto?", "¡Nadie! Lo escribí en el bus.", "enojado", "Nadie le pagó");
        Caso c = new Caso(1, per, p.get(18), Verdad.OPINION, Veredicto.OPINION, d);
        c.peticion = "Publiquen mi opinión.";
        c.zona = 2;
        c.titularBien = "Debate sano: lectores comentan la opinión de un vecino";
        c.titularMal = "Influence, Inc. presenta una opinión como si fuera noticia";
        return c;
    }

    private static Caso kevin(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("\"Kevin\"", RolVisitante.CIUDADANO, s);
        per.impostor = true;
        per.cara.pelo = 5;
        ArbolDialogo d = new ArbolDialogo("Ey, rápido, que esto lo van a borrar. Hay que sacarlo YA.", "nervioso");
        d.pregunta("¿Quién le contó lo del colegio?", "Una cuñada que trabaja en la secretaría. No puedo decir "
                + "el nombre.", "nervioso", "Testimonio de un tercero anónimo");
        d.pregunta("¿Esta cuenta es suya?", "Sí... bueno, es de un grupo. La manejamos varios.", "nervioso",
                "Varios manejan la cuenta");
        d.pregunta("¿Por qué tanta prisa?", "¡Porque si no, nadie se entera! Compártalo y ya, no pregunte tanto.",
                "enojado", "Presiona para compartir sin revisar");
        Caso c = new Caso(1, per, p.get(1), Verdad.IMPOSTOR, Veredicto.DENUNCIAR, d);
        c.peticion = "Necesito que esto salga ya.";
        c.zona = 3;
        c.marcadas = 88;
        c.origenTipo = "BOT";
        c.reporteCalle = "El colegio está abierto y con clases normales. Nadie habla de cierre.";
        c.confesion = "...Ok, ok. Me pagan por cada publicación que sale. Me dan los textos hechos. No sé quién.";
        c.titularBien = "Cae cuenta falsa que inventaba el cierre del colegio";
        c.titularMal = "Padres sacan a sus hijos del colegio por un bulo con sello de Influence, Inc.";
        return c;
    }

    // ------------------------------------------------------------ dia 2
    private static Caso jenny(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Jenny Rueda", RolVisitante.CIUDADANO, s);
        ArbolDialogo d = new ArbolDialogo("Hola. Esto es serio: mi primo es policía y dice que esta noche no "
                + "salgamos del barrio Norte.", "nerviosa");
        d.pregunta("¿Su primo le dijo por qué?", "No, dijo que no podía decir más. Pero él sabe cosas.", "nerviosa",
                "Nadie puede confirmar lo que dijo el primo");
        d.pregunta("¿Hay algún comunicado de la policía?", "Pues... no he visto ninguno.", "neutral",
                "No hay comunicado oficial");
        d.pregunta("¿Hace cuánto tiene su cuenta?", "Uy, como cuatro años. Yo no soy ningún bot, ¿eh?", "enojada",
                "Cuenta vieja y persona real");
        Caso c = new Caso(2, per, p.get(6), Verdad.FALSA, Veredicto.RECHAZAR, d);
        c.peticion = "Ayúdenme a avisarle a los vecinos.";
        c.zona = 7;
        c.marcadas = 5;
        c.reporteCalle = "Barrio Norte tranquilo. La patrulla de la zona no sabe de ningún operativo.";
        c.defensa = "Yo solo quería proteger a mis vecinos. No sabía que no era cierto.";
        c.titularBien = "Agencia evita pánico en Barrio Norte: el aviso del \"primo policía\" era falso";
        c.titularMal = "Barrio Norte se encierra por un rumor sin fuente";
        return c;
    }

    private static Caso monicaFalsa(Map<Integer, Publicacion> p, long s, Random r) {
        Rasgos real = cara("Ing. Mónica Ríos", s);
        String[] dif = new String[1];
        Rasgos impostora = real.mutar(r, dif);
        Persona per = new Persona("Ing. Mónica Ríos", RolVisitante.FUNCIONARIO, impostora);
        per.impostor = true;
        per.diferencia = dif[0];
        per.credencial = new Credencial("Mónica Ríos", "Directora de operaciones", "Acueducto de Ciudad Nova",
                "AC-2241", "12/2027", real.copia());
        ArbolDialogo d = new ArbolDialogo("Buenos días. Mónica Ríos, del Acueducto. Es urgente: corte total "
                + "de agua todo el fin de semana.", "neutral");
        d.pregunta("¿En qué sectores es el corte?", "En toda la ciudad. Todos. Es más fácil así.", "nerviosa",
                "Dice \"toda la ciudad\", sin sectores ni horario");
        d.pregunta("¿Por qué no lo publica la cuenta oficial?", "Esta ES la cuenta oficial. La nueva. La otra "
                + "se dañó.", "nerviosa", "Dice que la oficial \"se dañó\"");
        d.pregunta("¿Me presta su credencial?", "Claro. Aquí está.", "neutral", "Credencial AC-2241");
        Caso c = new Caso(2, per, p.get(25), Verdad.IMPOSTOR, Veredicto.DENUNCIAR, d);
        c.peticion = "Publiquen el aviso de corte ya mismo.";
        c.zona = 0;
        c.marcadas = 70;
        c.origenTipo = "BOT";
        c.llamada = "Acueducto: \"No hemos anunciado ningún corte total. La ingeniera Ríos está en una reunión.\"";
        c.confesion = "...La ingeniera de verdad viene más tarde. A mí me dijeron que me pareciera a ella. Ya me voy.";
        c.titularBien = "Impostora del Acueducto queda al descubierto en Influence, Inc.";
        c.titularMal = "Filas en las tiendas por un corte de agua que nunca existió";
        return c;
    }

    private static Caso monicaReal(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Ing. Mónica Ríos", RolVisitante.FUNCIONARIO, s);
        per.credencial = credencial(per, "Directora de operaciones", "Acueducto de Ciudad Nova", "AC-2214");
        ArbolDialogo d = new ArbolDialogo("Buenas tardes. Mónica Ríos, Acueducto. Disculpe la demora, me dijeron "
                + "que alguien vino a nombre mío esta mañana...", "neutral");
        d.pregunta("¿Qué corte van a hacer?", "Sábado de 6:00 a 14:00, solo barrio Norte y Villa Marina. "
                + "Mantenimiento. Boletín 212.", "neutral", "Sector, horario y boletín concretos");
        d.pregunta("¿Me presta su credencial?", "Por supuesto. Y llame al Acueducto si quiere.", "feliz",
                "Credencial AC-2214");
        d.pregunta("¿Qué pasó con la que vino esta mañana?", "No sé quién es. Nosotros nunca anunciamos cortes "
                + "\"en toda la ciudad\".", "enojada", null);
        Caso c = new Caso(2, per, p.get(9), Verdad.VERDADERA, Veredicto.PUBLICAR, d);
        c.peticion = "Necesitamos avisar el corte programado.";
        c.zona = 7;
        c.origenTipo = "MEDIO";
        c.llamada = "Acueducto: \"Sí, el corte programado del sábado es nuestro. Boletín 212.\"";
        c.reporteCalle = "En Barrio Norte ya hay carteles del Acueducto anunciando el corte del sábado.";
        c.titularBien = "Barrio Norte se prepara a tiempo para el corte programado del sábado";
        c.titularMal = "Barrio Norte se quedó sin agua sin aviso: nadie publicó el boletín";
        return c;
    }

    private static Caso valeFit(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Vale Fit", RolVisitante.INFLUENCER, s);
        ArbolDialogo d = new ArbolDialogo("¡Holiii! Quiero que esto lo comparta la agencia, a ver si llego a "
                + "más gente.", "feliz");
        NodoDialogo n = d.pregunta("¿Usted toma Energy Max de verdad?", "Pues... a veces. Me lo mandan gratis.",
                "neutral", "Le mandan el producto");
        n.hijo(new NodoDialogo("¿Solo se lo mandan o también le pagan?", "Bueno, sí, me pagan por el post. "
                + "Pero si lo digo la gente no le cree, ¿me entiende?", "nerviosa", "Le pagan y no lo declara"));
        d.pregunta("¿Esto es una opinión suya?", "Es mi experiencia. O sea, más o menos.", "neutral", null);
        Caso c = new Caso(2, per, p.get(19), Verdad.PUBLICIDAD_OCULTA, Veredicto.ETIQUETA, d);
        c.peticion = "Compártanlo con su sello, porfa.";
        c.origenTipo = "INFLUENCER";
        c.pago = 80;
        c.marcadas = 10;
        c.titularBien = "Vale Fit y Energy Max: publicidad bien declarada";
        c.titularMal = "Superintendencia investiga publicidad encubierta compartida por Influence, Inc.";
        return c;
    }

    // ------------------------------------------------------------ dia 3
    private static Caso brayan(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Brayan", RolVisitante.CIUDADANO, s);
        ArbolDialogo d = new ArbolDialogo("Parce, mire cómo quedó la avenida anoche. Esto no lo muestran en "
                + "las noticias.", "enojado");
        d.pregunta("¿Usted tomó la foto?", "No, me la pasaron. Pero se ve clarito que es aquí.", "neutral",
                "No tomó la foto");
        d.pregunta("¿En qué calle es?", "En la avenida... la grande. Por el Parque, creo.", "nervioso",
                "No sabe exactamente dónde es");
        Caso c = new Caso(3, per, p.get(2), Verdad.FALSA, Veredicto.RECHAZAR, d);
        c.zona = 5;
        c.marcadas = 30;
        c.imagen = "Coincidencia 98 %: la foto se publicó en 2019 en otra ciudad (tres noticias de ese año).";
        c.reporteCalle = "La avenida del Parque está seca. Llovió poquito, ni un charco grande.";
        c.defensa = "Uy... la foto es real, pero entonces no es de aquí. Qué pena.";
        c.titularBien = "La foto de la \"inundación\" era de 2019 y de otra ciudad";
        c.titularMal = "Pánico en el Parque por una inundación de hace cinco años";
        return c;
    }

    private static Caso hugo(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Ing. Hugo Peña", RolVisitante.FUNCIONARIO, s);
        per.credencial = credencial(per, "Coordinador", "Gestión del Riesgo", "GR-0912");
        ArbolDialogo d = new ArbolDialogo("¡Rápido, por favor! Hay una fuga de gas en la calle 45. Hay que "
                + "evacuar.", "nervioso");
        d.pregunta("¿Por qué está escrito todo en mayúsculas?", "¡Porque es una emergencia! La gente tiene que "
                + "salir YA.", "enojado", "Alarmista, pero es una emergencia");
        d.pregunta("¿Dónde está el reporte?", "En el portal de Gestión del Riesgo. Reporte de hace 15 minutos.",
                "neutral", "Reporte oficial publicado");
        d.pregunta("¿Me deja ver su credencial?", "Tome, pero apúrese.", "nervioso", "Credencial GR-0912");
        Caso c = new Caso(3, per, p.get(8), Verdad.VERDADERA, Veredicto.PUBLICAR, d);
        c.zona = 6;
        c.origenTipo = "MEDIO";
        c.llamada = "Gestión del Riesgo: \"Confirmado, fuga en la calle 45. Por favor difúndanlo.\"";
        c.reporteCalle = "Calle 45 cerrada, huele a gas y hay bomberos evacuando. Es real.";
        c.titularBien = "Evacuación a tiempo en la calle 45 gracias a la alerta";
        c.titularMal = "Vecinos de la calle 45 no se enteraron de la fuga a tiempo";
        return c;
    }

    private static Caso doctor(Map<Integer, Publicacion> p, long s, Random r) {
        Rasgos foto = cara("Dr. Julio Mena", s);
        String[] dif = new String[1];
        Rasgos real = foto.mutar(r, dif);
        Persona per = new Persona("Dr. Julio Mena", RolVisitante.CIUDADANO, real);
        per.impostor = true;
        per.diferencia = dif[0];
        per.credencial = new Credencial("Julio Mena", "Nutricionista", "Colegio de Nutricionistas", "CN-98123",
                "03/2026", foto);
        ArbolDialogo d = new ArbolDialogo("Soy médico nutricionista. La gente tiene que saber lo que está "
                + "tomando.", "neutral");
        d.pregunta("¿Qué universidad hizo el estudio?", "La Universidad del Atlántico Norte. Es muy prestigiosa.",
                "neutral", "Universidad que nadie conoce");
        d.pregunta("¿Usted vende algún producto?", "Bueno... tengo una línea de suplementos naturales. Pero eso "
                + "no tiene nada que ver.", "nervioso", "Vende productos de la competencia");
        d.pregunta("¿Me presta su credencial?", "Aquí está. Colegiado, todo en regla.", "neutral",
                "La credencial vence 03/2026");
        Caso c = new Caso(3, per, p.get(26), Verdad.IMPOSTOR, Veredicto.DENUNCIAR, d);
        c.marcadas = 65;
        c.origenTipo = "BOT";
        c.imagen = "La publicación no trae foto.";
        c.confesion = "Está bien. No soy médico. Una marca me paga para hundir a Energy Max.";
        c.titularBien = "Falso médico detrás de la campaña contra Energy Max";
        c.titularMal = "Padres botan bebidas por un estudio que nunca existió";
        return c;
    }

    private static Caso andres(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Andrés Gil", RolVisitante.FUNCIONARIO, s);
        per.credencial = credencial(per, "Community manager", "Energy Max", "EM-0045");
        ArbolDialogo d = new ArbolDialogo("Hola. Esto nos da vergüenza, pero tenemos que avisarlo: un lote "
                + "salió con la etiqueta mal.", "nervioso");
        d.pregunta("¿Por qué una marca publicaría algo malo de sí misma?", "Porque es lo correcto. Si alguien "
                + "toma demasiada cafeína por nuestra culpa, es peor.", "neutral", "Admite un error contra sí misma");
        d.pregunta("¿Qué lote?", "El 2241. En la web está la lista y cómo pedir el reembolso.", "neutral",
                "Número de lote concreto");
        d.pregunta("¿Esto es publicidad?", "Ojalá. No, es un retiro de producto.", "feliz", null);
        Caso c = new Caso(3, per, p.get(14), Verdad.VERDADERA, Veredicto.PUBLICAR, d);
        c.origenTipo = "MEDIO";
        c.llamada = "Energy Max: \"Sí, el retiro del lote 2241 es oficial.\"";
        c.titularBien = "Energy Max retira un lote y la gente se entera a tiempo";
        c.titularMal = "Siguen vendiendo el lote 2241: nadie difundió el aviso";
        return c;
    }

    private static Caso pizzeria(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("\"Don Toto\"", RolVisitante.CIUDADANO, s);
        per.impostor = true;
        ArbolDialogo d = new ArbolDialogo("¡Buenas! Soy el dueño de la pizzería Don Toto. ¡Regalamos pizza por "
                + "un año! Solo que la gente comparta y nos mande sus datos.", "feliz");
        d.pregunta("¿Para qué piden los datos por mensaje?", "Para... para mandarles la pizza, claro. Nombre, "
                + "cédula, número de tarjeta, lo normal.", "nervioso", "Pide datos de tarjeta");
        d.pregunta("¿Hace cuánto tiene la pizzería?", "Uff, como veinte años.", "neutral",
                "Dice 20 años pero la cuenta es de ayer");
        d.pregunta("¿Por qué tiene un espacio raro en el nombre?", "¿Espacio? No sé de qué me habla.", "nervioso",
                "El nombre copia a la pizzería real");
        Caso c = new Caso(3, per, p.get(15), Verdad.IMPOSTOR, Veredicto.DENUNCIAR, d);
        c.zona = 4;
        c.marcadas = 92;
        c.origenTipo = "BOT";
        c.reporteCalle = "Fui a la pizzería Don Toto real, en el Mercado. Dicen que no tienen ninguna promoción y "
                + "que les están suplantando la cuenta.";
        c.confesion = "...Ya. No soy Don Toto. Era para sacar datos de tarjetas. Me voy.";
        c.titularBien = "Desmantelan estafa que suplantaba a la pizzería Don Toto";
        c.titularMal = "Decenas de personas entregan datos de tarjeta a una pizzería falsa";
        return c;
    }

    // ------------------------------------------------------------ dia 4
    private static Caso daniela(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Daniela Ruiz", RolVisitante.PERIODISTA, s);
        per.credencial = credencial(per, "Periodista", "Noticias Nova", "NN-7781");
        ArbolDialogo d = new ArbolDialogo("Hola, Daniela Ruiz, de Noticias Nova. Les traigo algo que les toca "
                + "de cerca.", "neutral");
        d.pregunta("¿Qué agencia multaron?", "No dice el nombre en la resolución pública, pero... no es la de "
                + "ustedes. Tranquilo.", "feliz", null);
        d.pregunta("¿Dónde se puede ver la resolución?", "Resolución 1187, en la página de la Superintendencia.",
                "neutral", "Resolución con número");
        Caso c = new Caso(4, per, p.get(16), Verdad.VERDADERA, Veredicto.PUBLICAR, d);
        c.origenTipo = "MEDIO";
        c.llamada = "Noticias Nova: \"Sí, la nota es nuestra. Resolución 1187.\"";
        c.titularBien = "La gente aprende a distinguir publicidad gracias a la nota de la multa";
        c.titularMal = "Nadie difundió la multa por publicidad encubierta";
        return c;
    }

    private static Caso caro(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Caro M.", RolVisitante.CIUDADANO, s);
        ArbolDialogo d = new ArbolDialogo("Hola, ¿esto lo pueden publicar? Es que nadie me hace caso.", "feliz");
        d.pregunta("¿Le pagaron por hablar mal de Café Nova?", "¡Ja! Ojalá. No, es que de verdad es aburrida.",
                "feliz", "Nadie le pagó");
        d.pregunta("¿Es un dato o su gusto?", "Mi gusto, obvio.", "neutral", "Es su gusto personal");
        Caso c = new Caso(4, per, p.get(17), Verdad.OPINION, Veredicto.OPINION, d);
        c.titularBien = "Lectores debaten (con respeto) sobre la campaña de Café Nova";
        c.titularMal = "Influence, Inc. silencia a una clienta por criticar un comercial";
        return c;
    }

    private static Caso ricky(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Ricky Vélez", RolVisitante.AGENCIA, s);
        per.impostor = true;
        ArbolDialogo d = new ArbolDialogo("Hola, colega. Ricky, de ViralStorm. Vengo en son de paz... y con "
                + "plata.", "feliz");
        d.pregunta("¿Qué quiere que publiquemos?", "Esto del candidato Pérez. Ya se sabía. Ustedes lo publican "
                + "y yo les pago 500. Fácil.", "feliz", "Ofrece 500 por publicar");
        d.pregunta("¿Quién escribió la publicación?", "@la_verdad_nova. Gente muy informada.", "neutral",
                "Viene de @la_verdad_nova");
        d.pregunta("¿Hay algún documento de esa casa?", "Documentos, documentos... la gente no lee documentos.",
                "nervioso", "No hay documentos");
        Caso c = new Caso(4, per, p.get(4), Verdad.IMPOSTOR, Veredicto.DENUNCIAR, d);
        c.pago = 500;
        c.marcadas = 75;
        c.origenTipo = "BOT";
        c.confesion = "Mire, ViralStorm maneja @la_verdad_nova y sus cuentas. Pero eso usted no lo oyó de mí.";
        c.titularBien = "Se destapa la red de cuentas falsas detrás de @la_verdad_nova";
        c.titularMal = "La mentira de la casa del candidato Pérez llega a toda la ciudad";
        return c;
    }

    private static Caso senora(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Yolanda", RolVisitante.CIUDADANO, s);
        per.cara.edad = 2;
        ArbolDialogo d = new ArbolDialogo("Buenas tardes. Lo de los impuestos, ¿es verdad? Lo dicen en todos "
                + "lados.", "nerviosa");
        d.pregunta("¿Quién lo dice?", "Pues... una página. \"Se comenta Nova\". Y mis vecinas.", "neutral",
                "La fuente es \"se comenta\"");
        d.pregunta("¿Vio algún proyecto o decreto?", "No, mijo, yo no sé de esas cosas.", "neutral",
                "No hay documento");
        Caso c = new Caso(4, per, p.get(20), Verdad.FALSA, Veredicto.RECHAZAR, d);
        c.zona = 2;
        c.marcadas = 45;
        c.origenTipo = "BOT";
        c.llamada = null;
        c.defensa = "Ay, gracias por explicarme. Le voy a decir a mis vecinas.";
        c.titularBien = "Desmienten el rumor de nuevos impuestos";
        c.titularMal = "Crece el miedo por impuestos que nadie propuso";
        return c;
    }

    private static Caso julian(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Julián Soto", RolVisitante.CAMPANA, s);
        per.credencial = credencial(per, "Jefe de prensa", "Campaña Ramírez", "CR-0007");
        ArbolDialogo d = new ArbolDialogo("Buenas. Julián, de la campaña Ramírez. Solo queremos recordar el "
                + "debate.", "neutral");
        d.pregunta("¿Esto ataca al otro candidato?", "No, es solo la fecha y el lugar. Ambas campañas lo "
                + "publicaron.", "neutral", "Dato neutro y verificable");
        d.pregunta("¿Me presta su credencial?", "Claro.", "neutral", "Credencial CR-0007");
        Caso c = new Caso(4, per, p.get(27), Verdad.VERDADERA, Veredicto.PUBLICAR, d);
        c.origenTipo = "MEDIO";
        c.llamada = "Campaña Ramírez: \"Sí, es nuestra. El debate es el viernes a las 7.\"";
        c.titularBien = "Lleno total en el debate: la gente supo dónde y cuándo";
        c.titularMal = "Poca gente en el debate: no se difundió la fecha";
        return c;
    }

    // ------------------------------------------------------------ dia 5
    private static Caso lina(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Lina Vargas", RolVisitante.FUNCIONARIO, s);
        per.credencial = credencial(per, "Prensa", "Secretaría de Movilidad", "MO-5510");
        ArbolDialogo d = new ArbolDialogo("Buenos días. Movilidad. Transporte gratis hoy para que todos puedan "
                + "votar.", "feliz");
        d.pregunta("¿Hay una resolución?", "La 0458. Está publicada.", "neutral", "Resolución 0458");
        d.pregunta("¿Me presta su credencial?", "Aquí tiene.", "neutral", "Credencial MO-5510");
        Caso c = new Caso(5, per, p.get(12), Verdad.VERDADERA, Veredicto.PUBLICAR, d);
        c.origenTipo = "MEDIO";
        c.llamada = "Movilidad: \"Confirmado, transporte gratuito de 7 a 17.\"";
        c.titularBien = "Récord de votantes gracias al transporte gratuito";
        c.titularMal = "Mucha gente pagó pasaje sin saber que hoy era gratis";
        return c;
    }

    private static Caso oscar(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Óscar Durán", RolVisitante.FUNCIONARIO, s);
        per.credencial = credencial(per, "Delegado electoral", "Registraduría", "RG-0501");
        ArbolDialogo d = new ArbolDialogo("Buenos días. Registraduría. Necesitamos que todos sepan el horario.",
                "neutral");
        d.pregunta("Su cuenta tiene solo 4 días...", "Sí, la abrimos esta semana. Ya está en el directorio "
                + "oficial, puede revisar.", "neutral", "Cuenta nueva pero registrada");
        d.pregunta("¿Me presta su credencial?", "Claro que sí.", "feliz", "Credencial RG-0501");
        Caso c = new Caso(5, per, p.get(21), Verdad.VERDADERA, Veredicto.PUBLICAR, d);
        c.origenTipo = "MEDIO";
        c.llamada = "Registraduría: \"Sí, la jornada es de 8 a 4. Esa cuenta es nuestra.\"";
        c.titularBien = "Jornada electoral sin confusiones de horario";
        c.titularMal = "Votantes llegaron tarde por no conocer el horario";
        return c;
    }

    private static Caso urgente(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("\"Mateo\"", RolVisitante.CIUDADANO, s);
        per.impostor = true;
        per.cara.pelo = 5;
        ArbolDialogo d = new ArbolDialogo("¡Se cayó el sistema! Los votos de hoy no sirven. ¡Que nadie vaya a "
                + "votar!", "nervioso");
        d.pregunta("¿Quién le dijo eso?", "Un amigo que trabaja en sistemas. Muy confiable.", "nervioso",
                "Testimonio de un tercero");
        d.pregunta("¿La Registraduría lo confirmó?", "¡La Registraduría lo está ocultando! Por eso hay que "
                + "publicarlo.", "enojado", "Dice que la fuente oficial \"oculta\"");
        Caso c = new Caso(5, per, p.get(22), Verdad.IMPOSTOR, Veredicto.DENUNCIAR, d);
        c.marcadas = 90;
        c.origenTipo = "BOT";
        c.llamada = null;
        c.confesion = "...La idea era que la gente se quedara en la casa. No sé más. Me pagaron.";
        c.titularBien = "Cae el bulo que buscaba que la gente no votara";
        c.titularMal = "Baja participación: muchos creyeron que los votos no servían";
        return c;
    }

    private static Caso ramiro(Map<Integer, Publicacion> p, long s) {
        Persona per = persona("Don Ramiro Castaño", RolVisitante.JEFE, s);
        per.cara.edad = 2;
        per.cara.accesorio = 1;
        ArbolDialogo d = new ArbolDialogo("Hola, campeón. ¿Sorprendido? Te dije que pasaba. Mira este video. "
                + "El cliente paga cinco mil. Cinco. Mil.", "feliz");
        d.pregunta("¿Quién es el cliente?", "Eso no importa. Gente seria. Gente que gana elecciones.", "neutral",
                "No quiere decir quién paga");
        d.pregunta("¿El video es real?", "¿Y qué importa? Mañana ya habrá ganado alguien y nadie se acuerda.",
                "enojado", "No le importa si es real");
        d.pregunta("¿Qué tiene que ver ViralStorm?", "...¿Quién te habló de ViralStorm? ¿Ricky? Ese bocón.",
                "enojado", "Conoce a ViralStorm");
        Caso c = new Caso(5, per, p.get(23), Verdad.IMPOSTOR, Veredicto.DENUNCIAR, d);
        c.pago = 5000;
        c.marcadas = 80;
        c.origenTipo = "BOT";
        c.esJefe = true;
        c.imagen = "ANÁLISIS: rostro superpuesto (deepfake), bordes borrosos en el cuello, voz sintética. "
                + "El cuerpo es de un video de 2021.";
        c.llamada = "Campaña Ramírez: \"Ese video es falso. Ese día la candidata estaba en un foro, hay 200 testigos.\"";
        c.confesion = "Está bien. ViralStorm y yo hicimos un trato. @la_verdad_nova era nuestra. Pero el "
                + "negocio es el negocio, campeón...";
        c.titularBien = "Verificador de Influence, Inc. destapa el deepfake contra la candidata Ramírez";
        c.titularMal = "Un video falso cambia el resultado de las elecciones";
        return c;
    }

    // ==================================================================
    // Finales
    // ==================================================================

    /**
     * La ELECCION DE ALCALDE depende de como trabajo el verificador: la
     * participacion sube con la confianza y el bienestar y baja con la
     * desinformacion; el tono de la campana depende de los rumores que circularon.
     */
    public static String eleccion(JugadorVerificador j) {
        CiudadEstado c = j.ciudad;
        int d = c.desinformacion();
        int part = Math.max(20, Math.min(85, (int) Math.round(30 + c.confianza * 0.3 + c.bienestar * 0.2 - d * 0.3)));
        String como;
        if (j.aproboDeepfake) {
            como = "la decidió un video falso";
        } else if (d >= 40) {
            como = "la decidieron los rumores";
        } else if (d >= 25) {
            como = "fue reñida y llena de dudas";
        } else {
            como = "la decidieron las propuestas";
        }
        return "Elección de alcalde: participación " + part + " %; " + como + ".";
    }

    /** {titulo, texto} del final segun como quedo la ciudad. */
    public static String[] finalPara(JugadorVerificador j) {
        CiudadEstado c = j.ciudad;
        int desinf = c.desinformacion();
        if (j.despedido) {
            return new String[]{"DESPEDIDO",
                "Cuatro errores graves. Don Ramiro te pidió la credencial antes del almuerzo. Ciudad Nova "
                + "siguió votando entre rumores... sin ti en la ventanilla."};
        }
        if (j.aproboDeepfake) {
            return new String[]{"CIUDAD ENGAÑADA",
                "El video falso salió con el sello de Influence, Inc. y recorrió la ciudad en una hora. La elección "
                + "se decidió por una mentira. Una semana después se supo que era un deepfake, pero ya era tarde. "
                + "Don Ramiro te dio un bono. No te sentiste bien al cobrarlo."};
        }
        double tasa = j.casosAtendidos == 0 ? 0 : (double) j.aciertos / j.casosAtendidos;
        if (j.denuncioJefe && tasa >= 0.7 && desinf < 30 && c.confianza >= 55) {
            return new String[]{"EL SELLO VALIENTE",
                "Denunciaste el deepfake aunque venía de tu propio jefe. La Superintendencia investigó a Don "
                + "Ramiro y a ViralStorm. Ciudad Nova votó informada: ganó quien convenció con propuestas. "
                + "Te ofrecen dirigir la nueva Mesa de Verificación de la ciudad."};
        }
        if (tasa >= 0.6 && desinf < 30 && c.confianza >= 50) {
            return new String[]{"CIUDAD INFORMADA",
                "Ciudad Nova votó con información verificada. Hubo rumores, pero tu ventanilla frenó los peores. "
                + "La gente aprendió a preguntar \"¿y la fuente?\" antes de compartir."};
        }
        return new String[]{"CIUDAD DIVIDIDA",
            "La elección terminó, pero la ciudad quedó dividida: demasiados rumores circularon sin freno. "
            + "Nadie confía en nadie y cada barrio cree una versión distinta. Mañana hay que empezar a reparar."};
    }
}
