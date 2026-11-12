package influenceinc.logica;

import influenceinc.estructuras.Arista;
import influenceinc.estructuras.GrafoRed;
import influenceinc.estructuras.Vertice;
import influenceinc.modelo.Rasgos;
import influenceinc.modelo.TipoCuenta;
import influenceinc.modelo.Verdad;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

/**
 * DATOS de las habilidades unicas de cada rol y de la alerta viral.
 *
 * Todo sale de las estructuras del juego, no es decorado:
 *  - CIUDADANO: la cadena de reenvios es un camino real del GRAFO SOCIAL,
 *    hallado con BFS desde la cuenta de origen (arbol BFS con padres).
 *  - INFLUENCER: los perfiles del cazabots son las cuentas de las 2 primeras
 *    oleadas del BFS desde el origen (las mismas que ve el escaner de red).
 *  - CANDIDATO: las preguntas de la rueda de prensa y su efecto en la ciudad.
 */
public final class Habilidades {

    private Habilidades() {
    }

    // ==================================================================
    // CIUDADANO: "¿Quien te lo mando?"
    // ==================================================================

    /** Una persona que aparece como opcion en la cadena. */
    public static class Eslabon {
        public final String nombre;
        public final String hora;
        public final String huella;
        public final Vertice v;
        /** true = es quien de verdad se lo reenvio al de abajo. */
        public final boolean correcto;
        /** true = la tarjeta "Nadie: esta cuenta lo publico primero". */
        public final boolean nadie;

        Eslabon(String nombre, String hora, String huella, Vertice v, boolean correcto, boolean nadie) {
            this.nombre = nombre;
            this.hora = hora;
            this.huella = huella;
            this.v = v;
            this.correcto = correcto;
            this.nadie = nadie;
        }
    }

    /** La cadena completa: camino[0] = origen ... camino[n-1] = quien te lo mando a ti. */
    public static class Cadena {
        public final List<Vertice> camino = new ArrayList<>();
        public final List<String> horas = new ArrayList<>();
        public String huella;
        public String horaTu;
        /** Opciones de cada paso: paso 0 = quien te lo mando a ti, el ultimo = "nadie" (llegaste al origen). */
        public final List<List<Eslabon>> pasos = new ArrayList<>();
    }

    private static final String[] NOMBRES = {"Ana", "Luis", "Pedro", "Carlos", "María", "Sofía", "Andrés", "Valentina",
        "Camila", "Jorge", "Daniela", "Mateo", "Laura", "Felipe", "Juliana", "Diego", "Paula", "Sebastián",
        "Natalia", "Tomás", "Isabela", "Samuel", "Mariana", "David", "Gabriela", "Nicolás", "Lucía", "Martín",
        "Sara", "Emilio"};
    private static final String[] APELLIDOS = {"R.", "G.", "M.", "P.", "C.", "L.", "T.", "V.", "B.", "S."};

    /** Nombre "humano" estable para un vertice del grafo (las cuentas falsas conservan su arroba). */
    public static String nombreVisible(Vertice v) {
        if (v == null) {
            return "?";
        }
        if (v.tipo != TipoCuenta.USUARIO) {
            return v.nombre;
        }
        int h = Math.abs(v.id.hashCode());
        return NOMBRES[h % NOMBRES.length] + " " + APELLIDOS[(h / 7) % APELLIDOS.length];
    }

    private static String hora(int minutos) {
        return String.format("%02d:%02d", minutos / 60, minutos % 60);
    }

    private static String huella(Random r) {
        String hex = "0123456789ABCDEF";
        StringBuilder sb = new StringBuilder("#");
        for (int i = 0; i < 4; i++) {
            sb.append(hex.charAt(r.nextInt(16)));
        }
        return sb.toString();
    }

    /**
     * Construye la cadena de reenvios. Se hace un BFS DIRIGIDO desde el origen
     * guardando el PADRE de cada vertice (arbol BFS) y se escoge una persona a
     * 2 o 3 saltos: subiendo por los padres sale el camino exacto por el que la
     * publicacion le llego.
     */
    public static Cadena cadena(Vertice origen, GrafoRed red, List<Vertice> usuarios, Random r) {
        Cadena c = new Cadena();
        c.huella = huella(r);
        Map<Vertice, Vertice> padre = new HashMap<>();
        Map<Vertice, Integer> nivel = new HashMap<>();
        List<Vertice> candidatos = new ArrayList<>();
        if (origen != null && red.contiene(origen.id)) {
            Queue<Vertice> cola = new ArrayDeque<>();
            cola.add(origen);
            nivel.put(origen, 0);
            while (!cola.isEmpty()) {
                Vertice a = cola.poll();
                int n = nivel.get(a);
                if (n >= 3) {
                    continue;
                }
                for (Arista ar : a.getSalientes()) {
                    Vertice b = ar.destino;
                    if (!nivel.containsKey(b) && red.contiene(b.id)) {
                        nivel.put(b, n + 1);
                        padre.put(b, a);
                        cola.add(b);
                        if (b.tipo == TipoCuenta.USUARIO && n + 1 >= 2) {
                            candidatos.add(b);
                        }
                    }
                }
            }
        }
        Vertice destino = null;
        if (!candidatos.isEmpty()) {
            // preferimos los mas lejanos (cadena mas larga = mas interesante)
            int max = 0;
            for (Vertice v : candidatos) {
                max = Math.max(max, nivel.get(v));
            }
            List<Vertice> lejos = new ArrayList<>();
            for (Vertice v : candidatos) {
                if (nivel.get(v) == max) {
                    lejos.add(v);
                }
            }
            destino = lejos.get(r.nextInt(lejos.size()));
        }
        if (destino != null) {
            for (Vertice p = destino; p != null; p = padre.get(p)) {
                c.camino.add(0, p);
            }
        } else {
            // sin camino en el grafo: el origen y dos personas cualesquiera
            if (origen != null) {
                c.camino.add(origen);
            }
            List<Vertice> u = new ArrayList<>(usuarios);
            Collections.shuffle(u, r);
            for (int i = 0; i < 2 && i < u.size(); i++) {
                c.camino.add(u.get(i));
            }
        }
        // horas: el origen publica temprano y cada reenvio pasa 15-40 minutos despues
        int m = 7 * 60 + r.nextInt(50);
        for (int i = 0; i < c.camino.size(); i++) {
            c.horas.add(hora(m));
            m += 15 + r.nextInt(26);
        }
        c.horaTu = hora(m);
        // pasos: del final al origen
        List<Vertice> todos = new ArrayList<>(usuarios);
        for (int paso = c.camino.size() - 1; paso >= -1; paso--) {
            List<Eslabon> op = new ArrayList<>();
            int horaActual = paso + 1 < c.camino.size() ? minutos(c.horas.get(paso + 1)) : minutos(c.horaTu);
            if (paso >= 0) {
                Vertice v = c.camino.get(paso);
                op.add(new Eslabon(nombreVisible(v), c.horas.get(paso), c.huella, v, true, false));
            }
            // distractores: misma huella pero DESPUES (lo recibieron mas tarde) o ANTES pero otra version
            Collections.shuffle(todos, r);
            int k = 0;
            for (Vertice d : todos) {
                if (op.size() >= (paso >= 0 ? 3 : 3) || k > 40) {
                    break;
                }
                k++;
                if (c.camino.contains(d)) {
                    continue;
                }
                boolean despues = op.size() % 2 == 1;
                String h = despues ? hora(horaActual + 5 + r.nextInt(30)) : hora(Math.max(6 * 60, horaActual - 10 - r.nextInt(40)));
                String hu = despues ? c.huella : huella(r);
                op.add(new Eslabon(nombreVisible(d), h, hu, d, false, false));
            }
            op.add(new Eslabon("Nadie: esta cuenta lo publicó primero", "", "", null, paso < 0, true));
            Collections.shuffle(op.subList(0, op.size() - 1), r);
            c.pasos.add(op);
        }
        return c;
    }

    private static int minutos(String hhmm) {
        String[] p = hhmm.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    // ==================================================================
    // INFLUENCER: cazabots
    // ==================================================================

    public static class Perfil {
        public final String nombre;
        public final String detalle;
        public final boolean bot;
        public final Rasgos cara;

        Perfil(String nombre, String detalle, boolean bot, Rasgos cara) {
            this.nombre = nombre;
            this.detalle = detalle;
            this.bot = bot;
            this.cara = cara;
        }
    }

    /** Las cuentas de las 2 primeras oleadas del BFS desde el origen, como perfiles. */
    public static List<Perfil> perfiles(Vertice origen, GrafoRed red, List<Vertice> usuarios, Random r) {
        List<Vertice> vs = new ArrayList<>();
        if (origen != null && red.contiene(origen.id)) {
            List<List<Vertice>> ol = red.bfsOleadas(origen, 2);
            for (int i = 0; i < ol.size(); i++) {
                for (Vertice v : ol.get(i)) {
                    if (v.tipo != TipoCuenta.AGENCIA) {
                        vs.add(v);
                    }
                }
            }
        }
        if (vs.size() < 6) {
            List<Vertice> u = new ArrayList<>(usuarios);
            Collections.shuffle(u, r);
            for (Vertice v : u) {
                if (vs.size() >= 8) {
                    break;
                }
                if (!vs.contains(v)) {
                    vs.add(v);
                }
            }
        }
        Collections.shuffle(vs, r);
        List<Perfil> l = new ArrayList<>();
        String[] palabras = {"nova", "verdad", "ciudad", "info", "pueblo", "alerta", "noticia", "real"};
        for (Vertice v : vs) {
            if (l.size() >= 14) {
                break;
            }
            if (v.tipo == TipoCuenta.BOT) {
                String n = "@" + palabras[r.nextInt(palabras.length)] + "_" + (1000 + r.nextInt(90000));
                int dias = 1 + r.nextInt(4);
                l.add(new Perfil(n, "Creada hace " + dias + (dias == 1 ? " día · " : " días · ") + (180 + r.nextInt(600))
                        + " publicaciones hoy · sin foto", true, null));
            } else {
                Rasgos cara = Rasgos.azar(new Random(v.id.hashCode() * 31L + 7));
                String det = v.tipo == TipoCuenta.MEDIO ? "Medio de la ciudad · desde 2009"
                        : v.tipo == TipoCuenta.INFLUENCER ? "Creadora de contenido · desde 2018"
                        : "En Civitas desde " + (2014 + r.nextInt(9)) + " · " + (1 + r.nextInt(6)) + " publicaciones hoy";
                l.add(new Perfil(nombreVisible(v), det, false, cara));
            }
        }
        return l;
    }

    // ==================================================================
    // CANDIDATO: rueda de prensa
    // ==================================================================

    /** R = responsable, A = ataque, M = repite un rumor, E = evasiva. */
    public static class Respuesta {
        public final String texto;
        public final char tipo;

        Respuesta(String texto, char tipo) {
            this.texto = texto;
            this.tipo = tipo;
        }
    }

    public static class Pregunta {
        public final String texto;
        public final List<Respuesta> respuestas = new ArrayList<>();

        Pregunta(String texto, String... rs) {
            this.texto = texto;
            for (String s : rs) {
                respuestas.add(new Respuesta(s.substring(2), s.charAt(0)));
            }
        }
    }

    private static List<Pregunta> banco() {
        List<Pregunta> b = new ArrayList<>();
        b.add(new Pregunta("Están circulando rumores sobre usted en Civitas. ¿Qué responde?",
                "R:Invito a revisar la fuente antes de compartir. Aquí están mis datos.",
                "A:¡Eso lo inventó mi rival, que es un mentiroso!",
                "E:Prefiero no hablar de eso. Siguiente pregunta."));
        b.add(new Pregunta("¿Qué hará con el colegio del barrio?",
                "R:Mi propuesta está publicada: ampliar cupos con un presupuesto aprobado.",
                "M:Dicen que lo cierran mañana. No sé si es cierto, pero compártanlo.",
                "A:Lo que importa es que el otro candidato lo quiere cerrar."));
        b.add(new Pregunta("Una cuenta dice que usted robó dinero. ¿Es cierto?",
                "R:No. Publiqué mis cuentas y cualquiera puede revisarlas.",
                "A:¡Los que dicen eso deberían ir presos!",
                "E:No tengo tiempo para esas preguntas."));
        b.add(new Pregunta("¿Cómo frenaría las noticias falsas en Ciudad Nova?",
                "R:Con una mesa de verificación y enseñando a revisar fuentes en los colegios.",
                "A:Cerrando las cuentas de los que me critican.",
                "M:Las noticias falsas son todas de mis rivales, todo el mundo lo sabe."));
        b.add(new Pregunta("Un video suyo se volvió viral. ¿Es real?",
                "R:Pido revisar el video original. Si es falso, lo reportaremos.",
                "E:Los videos virales no me interesan.",
                "M:Seguro lo hicieron mis rivales con hackers extranjeros."));
        b.add(new Pregunta("¿Qué le dice a quienes comparten sin leer?",
                "R:Que lean completo y busquen la fuente antes de compartir.",
                "A:Que son unos ignorantes.",
                "E:Que cada quien haga lo que quiera."));
        b.add(new Pregunta("¿Va a cerrar los parques, como dicen en redes?",
                "R:No. Mi plan de parques está publicado y se puede revisar.",
                "M:Eso lo dijo otro candidato. Compártanlo para que se sepa.",
                "A:Quien diga eso es enemigo de la ciudad."));
        b.add(new Pregunta("¿Acepta debatir con los demás candidatos?",
                "R:Sí, con respeto y con datos que se puedan verificar.",
                "A:Yo no debato con mentirosos.",
                "E:Ya veremos. No sé."));
        return b;
    }

    /** 3 preguntas al azar, con las respuestas en desorden. */
    public static List<Pregunta> ruedaDePrensa(Random r) {
        List<Pregunta> b = banco();
        Collections.shuffle(b, r);
        List<Pregunta> l = new ArrayList<>(b.subList(0, 3));
        for (Pregunta p : l) {
            Collections.shuffle(p.respuestas, r);
        }
        return l;
    }

    /** Lo que averigua el candidato en la sala si responde bien (depende de lo que el caso ES). */
    public static String pistaPrensa(Verdad v) {
        switch (v) {
            case FALSA:
            case IMPOSTOR:
                return "[!] Una periodista te cuenta que eso ya fue desmentido en otra ciudad.";
            case PUBLICIDAD_OCULTA:
                return "[!] En la sala comentan que a esa cuenta le pagan por publicar.";
            case OPINION:
                return "Nadie en la sala presentó datos: suena más a una opinión.";
            default:
                return "[ok] Un periodista confirma el dato con documentos oficiales.";
        }
    }

    // ==================================================================
    // Alerta viral (decision con tiempo limitado, seccion 9 del enunciado)
    // ==================================================================

    public static class Viral {
        public final String texto;
        public final String fuente;
        public final boolean verdadera;

        Viral(String texto, String fuente, boolean verdadera) {
            this.texto = texto;
            this.fuente = fuente;
            this.verdadera = verdadera;
        }
    }

    public static Viral viral(Random r) {
        Viral[] v = {
            new Viral("El colegio será cerrado mañana.", "un audio reenviado muchas veces", false),
            new Viral("El alcalde actual está robando dinero.", "una cuenta creada ayer", false),
            new Viral("Mañana cerrarán todos los parques.", "una captura de pantalla sin fecha", false),
            new Viral("El candidato Juan quiere cerrar el colegio.", "un meme sin autor", false),
            new Viral("La Registraduría amplía el horario de votación hasta las 5 p. m.",
                    "@registraduria_nova (cuenta oficial)", true),
            new Viral("Campaña de convivencia digital: \"Piensa antes de compartir\" llega a los colegios.",
                    "Alcaldía de Ciudad Nova (cuenta oficial)", true),
            new Viral("Corte de agua el jueves en el Barrio Norte por mantenimiento.",
                    "@acueducto_nova (cuenta oficial)", true)
        };
        return v[r.nextInt(v.length)];
    }
}
