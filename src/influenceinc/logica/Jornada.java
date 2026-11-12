package influenceinc.logica;

import influenceinc.estructuras.ArbolConsecuencias;
import influenceinc.estructuras.GrafoCiudad;
import influenceinc.estructuras.GrafoRed;
import influenceinc.estructuras.NodoDialogo;
import influenceinc.estructuras.Vertice;
import influenceinc.modelo.Cuenta;
import influenceinc.modelo.CuentaOficial;
import influenceinc.modelo.EfectoCiudad;
import influenceinc.modelo.Herramienta;
import influenceinc.modelo.Persona;
import influenceinc.modelo.Publicacion;
import influenceinc.modelo.RolJugador;
import influenceinc.modelo.Senal;
import influenceinc.modelo.TipoCuenta;
import influenceinc.modelo.Veredicto;
import influenceinc.modelo.Verdad;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * UN DIA DE TRABAJO de un verificador.
 *
 * La gente hace FILA (cola FIFO) frente a la ventanilla. Por cada caso el
 * jugador conversa (arbol de dialogo), usa herramientas (cada una cuesta
 * minutos del reloj), confronta al visitante con lo que encontro (se INSERTAN
 * nodos nuevos en el arbol de dialogo) y al final SELLA. El sello baja por el
 * arbol de consecuencias y el efecto se propaga por los grafos.
 *
 * El reloj va de 9:00 a 18:00. A las 18:00 la ventanilla cierra.
 */
public class Jornada {

    public static final int INICIO = 9 * 60, CIERRE = 18 * 60;
    /** Minutos de juego por segundo real (9 horas en 6 minutos). */
    public static final double VELOCIDAD = 540.0 / 360.0;

    public enum Evento {
        APAGON("¡Apagón! El computador se reinicia: sin herramientas por un rato."),
        CAFE("Llegó el café. El reloj se detiene unos segundos."),
        VIRAL("Algo se volvió viral afuera: la desinformación sube un poco."),
        FILA("Llegó un bus: la fila se ve más larga (y el reloj corre más rápido un rato)."),
        PANICO("El pánico se extiende por los barrios.");

        public final String texto;

        Evento(String texto) {
            this.texto = texto;
        }
    }

    /** Lo que pasa al sellar (para la animacion y el noticiero). */
    public static class Resultado {
        public Caso caso;
        public Veredicto veredicto;
        public ArbolConsecuencias.Nodo hoja;
        public String ruta;
        public EfectoCiudad efecto;
        public String titular;
        public int puntos, dinero, alcance, corregidos;
        public boolean advertencia, despedido, bonoRapidez, seguroUsado;
        public GrafoRed.ResultadoPropagacion propagacion;
        public List<String> logrosNuevos = new ArrayList<>();
    }

    public final int dia;
    public final JugadorVerificador jugador;
    public final boolean relajado;
    private final Deque<Caso> fila = new ArrayDeque<>();
    private final List<Caso> atendidos = new ArrayList<>();
    private final List<Resultado> resultados = new ArrayList<>();
    private final Random azar;

    private Caso actual;
    private NodoDialogo nodoActual;
    private double minuto = INICIO;
    private double minutoInicioCaso;
    private double pausaSeg;
    private double apagonHasta = -1;
    private double prisaHasta = -1;
    private double proximoPanico = INICIO + 60;
    private final List<double[]> eventos = new ArrayList<>(); // {minuto, ordinal}
    private int contradicciones;
    private final Set<Herramienta> usadasEnCaso = EnumSet.noneOf(Herramienta.class);
    private double corresponsalLlega = -1, salidaCorresponsal;
    private List<GrafoCiudad.Zona> rutaCorresponsal;
    private boolean llamo, reporteRecibido;
    private boolean terminada;
    private int puntosDia, aciertosDia, erroresDia;
    private final List<String> titulares = new ArrayList<>();
    public final List<String> cambiosDeReglas;
    /** Alerta viral pendiente de decision (la vista la muestra con cuenta regresiva). */
    private Habilidades.Viral viralPendiente;

    public Jornada(int dia, JugadorVerificador jugador, long semilla, boolean relajado) {
        this.dia = dia;
        this.jugador = jugador;
        this.relajado = relajado;
        this.azar = new Random(semilla * 7 + dia * 131);
        this.cambiosDeReglas = jugador.prepararDia(dia);
        fila.addAll(Historia.casosDelDia(dia, semilla));
        // eventos aleatorios del dia (componente de azar)
        Evento[] posibles = {Evento.APAGON, Evento.CAFE, Evento.VIRAL, Evento.FILA};
        int n = 1 + (dia >= 3 ? 1 : 0) + azar.nextInt(2);
        for (int i = 0; i < n; i++) {
            double m = INICIO + 90 + azar.nextInt(360);
            eventos.add(new double[]{m, posibles[azar.nextInt(posibles.length)].ordinal()});
        }
    }

    // ==================================================================
    // Reloj
    // ==================================================================

    /**
     * Avanza el reloj. @return los eventos que ocurrieron en este paso.
     */
    public List<Evento> avanzar(double segundos) {
        List<Evento> ocurridos = new ArrayList<>();
        if (terminada) {
            return ocurridos;
        }
        if (pausaSeg > 0) {
            pausaSeg -= segundos;
            return ocurridos;
        }
        if (!relajado) {
            double factor = minuto < prisaHasta ? 1.6 : 1.0;
            minuto += segundos * VELOCIDAD * factor;
        }
        for (int i = eventos.size() - 1; i >= 0; i--) {
            if (minuto >= eventos.get(i)[0]) {
                Evento e = Evento.values()[(int) eventos.get(i)[1]];
                eventos.remove(i);
                aplicarEvento(e);
                ocurridos.add(e);
            }
        }
        if (minuto >= proximoPanico) {
            proximoPanico += 60;
            if (hayPanico() && jugador.ciudad.propagarPanico() > 0) {
                ocurridos.add(Evento.PANICO);
            }
        }
        if (minuto >= CIERRE) {
            minuto = CIERRE;
            cerrar();
        }
        return ocurridos;
    }

    private void aplicarEvento(Evento e) {
        switch (e) {
            case APAGON:
                apagonHasta = minuto + 30;
                break;
            case CAFE:
                pausaSeg = 8;
                break;
            case VIRAL:
                jugador.ciudad.ajustarDesinformacion(4);
                viralPendiente = Habilidades.viral(azar);
                break;
            default:
                prisaHasta = minuto + 60;
                break;
        }
    }

    /** Cada herramienta cuesta tiempo. En modo relajado igual se cuenta, pero el reloj no corre solo. */
    private void gastar(int minutos) {
        minuto = Math.min(CIERRE, minuto + minutos);
    }

    public boolean hayPanico() {
        for (int p : jugador.ciudad.panico) {
            if (p > 0) {
                return true;
            }
        }
        return false;
    }

    public String hora() {
        int m = (int) minuto;
        return String.format("%02d:%02d", m / 60, m % 60);
    }

    public double getMinuto() {
        return minuto;
    }

    /** 0..1 cuanto del dia ha pasado. */
    public double avanceDia() {
        return (minuto - INICIO) / (CIERRE - INICIO);
    }

    public boolean enApagon() {
        return minuto < apagonHasta;
    }

    public boolean enPausa() {
        return pausaSeg > 0;
    }

    // ==================================================================
    // Fila
    // ==================================================================

    /** Hace pasar al siguiente de la fila (desencolar). null si no hay nadie. */
    public Caso siguiente() {
        actual = fila.poll();
        if (actual == null) {
            return null;
        }
        actual.origen = jugador.ciudad.origenPara(actual.origenTipo, actual.zona);
        nodoActual = actual.dialogo.getRaiz();
        nodoActual.visitado = true;
        minutoInicioCaso = minuto;
        contradicciones = 0;
        usadasEnCaso.clear();
        reiniciarHerramientas();
        return actual;
    }

    /**
     * Cada vez que llega un cliente nuevo, TODAS las herramientas quedan
     * disponibles otra vez: lupa, directorio, llamada, corresponsal, red...
     * (los resultados se guardan en cada Caso, asi que el nuevo empieza limpio).
     */
    private void reiniciarHerramientas() {
        usadasEnCaso.clear();
        corresponsalLlega = -1;
        salidaCorresponsal = 0;
        rutaCorresponsal = null;
        llamo = false;
        reporteRecibido = false;
        if (actual != null) {
            actual.resultados.clear();
        }
    }

    /** Herramientas usadas con el cliente actual (se vacia con cada cliente nuevo). */
    public int usadasConEsteCliente() {
        return usadasEnCaso.size();
    }

    public int enFila() {
        return fila.size();
    }

    public List<Caso> verFila() {
        return new ArrayList<>(fila);
    }

    public Caso getActual() {
        return actual;
    }

    public boolean isTerminada() {
        return terminada;
    }

    // ==================================================================
    // Dialogo
    // ==================================================================

    public NodoDialogo getNodoActual() {
        return nodoActual;
    }

    public List<NodoDialogo> opciones() {
        return actual == null ? new ArrayList<NodoDialogo>() : actual.dialogo.opciones(nodoActual);
    }

    /** Hacer una pregunta: baja por el arbol de dialogo. Cuesta 3 minutos. */
    public NodoDialogo preguntar(NodoDialogo n) {
        nodoActual = n;
        n.visitado = true;
        gastar(3);
        if (n.pista != null && n.pista.startsWith("¡Confesó")) {
            actual.confrontado = true;
        }
        return n;
    }

    /**
     * CONFRONTAR: el jugador le muestra al visitante una contradiccion. Se
     * INSERTA un nodo nuevo en el arbol de dialogo (como hijo de la raiz) para
     * que aparezca como pregunta disponible.
     */
    private void insertarConfrontacion(String pregunta, boolean esContradiccion) {
        Persona p = actual.visitante;
        String resp;
        String expr;
        String pista = null;
        if (!esContradiccion) {
            resp = actual.verdad == Verdad.VERDADERA ? "Todo en orden, ¿ve? Puede revisar lo que quiera."
                    : "No entiendo qué tiene de raro eso.";
            expr = "neutral";
        } else if (p.impostor || actual.verdad == Verdad.IMPOSTOR) {
            contradicciones++;
            if (contradicciones >= 2) {
                resp = actual.confesion;
                expr = "roto";
                pista = "¡Confesó! Es un impostor.";
            } else {
                resp = "Eh... eso... seguro es un error del sistema. Siguiente pregunta.";
                expr = "nervioso";
                pista = "Se puso nervioso";
            }
        } else if (actual.verdad == Verdad.PUBLICIDAD_OCULTA) {
            contradicciones++;
            resp = "Bueno, sí... me pagan. Pero no quería que se notara.";
            expr = "nervioso";
            pista = "Admite que le pagan";
        } else {
            contradicciones++;
            resp = actual.defensa;
            expr = "triste";
            pista = "Lo creía de buena fe";
        }
        // no repetir la misma confrontacion
        for (NodoDialogo h : actual.dialogo.getRaiz().getHijos()) {
            if (h.pregunta.equals(pregunta)) {
                return;
            }
        }
        actual.dialogo.insertar(actual.dialogo.getRaiz(), new NodoDialogo(pregunta, resp, expr, pista));
    }

    public int getContradicciones() {
        return contradicciones;
    }

    // ==================================================================
    // Herramientas
    // ==================================================================

    public boolean disponible(Herramienta h) {
        return h.dia <= dia && !enApagon() && jugador.rol.puedeUsar(h);
    }

    /** true si el ROL del jugador incluye esta herramienta (aunque todavia no sea su dia). */
    public boolean delRol(Herramienta h) {
        return jugador.rol.puedeUsar(h);
    }

    /**
     * VALIDACION: por que NO se puede usar una herramienta ahora mismo (null = si se puede).
     * Mira el rol, el dia, el apagon y si el caso ACTUAL la necesita
     * (la lupa sin credencial, el directorio sin cuenta, la busqueda inversa sin foto...).
     */
    public String motivoNoAplica(Herramienta h) {
        if (!jugador.rol.puedeUsar(h)) {
            return "Tu rol (" + jugador.rol.nombre + ") no usa " + h.nombre.toLowerCase() + ".";
        }
        if (h.dia > dia) {
            return h.nombre + " llega el día " + h.dia + ".";
        }
        if (enApagon()) {
            return "¡Apagón! Sin energía no funciona " + h.nombre.toLowerCase() + ".";
        }
        if (actual == null) {
            return "No hay nadie en la ventanilla.";
        }
        switch (h) {
            case LUPA:
                return actual.visitante.credencial == null
                        ? "Este visitante no trae credencial: no hay foto que comparar con la lupa." : null;
            case DIRECTORIO:
                return actual.pub.getCuenta() == null
                        ? "La publicación no sale de ninguna cuenta: no hay nada que buscar en el directorio." : null;
            case IMAGEN:
                return actual.pub.getImagen() == null && (actual.imagen == null || actual.imagen.startsWith("La publicación no"))
                        ? "La publicación no trae foto ni video: la búsqueda inversa no aplica." : null;
            case CORRESPONSAL:
                return actual.zona == 1 ? "La noticia no habla de ningún barrio: no hay a dónde enviar al corresponsal." : null;
            case RED:
                return actual.origen == null || !jugador.ciudad.red.contiene(actual.origen.id)
                        ? "El origen de esta publicación ya no está en la red social." : null;
            default:
                return null;
        }
    }

    public List<String> usar(Herramienta h) {
        List<String> r = new ArrayList<>();
        if (actual == null) {
            return r;
        }
        String no = motivoNoAplica(h);
        if (no != null && disponible(h)) {
            // no aplica a ESTE caso: no gasta tiempo y no se bloquea para el siguiente
            r.add("[x] " + no);
            return r;
        }
        if (!disponible(h)) {
            r.add(!jugador.rol.puedeUsar(h) ? "Tu rol (" + jugador.rol.nombre + ") no usa esta herramienta."
                    : enApagon() ? "Sin energía. Espera a que vuelva la luz." : "Esta herramienta llega el día " + h.dia + ".");
            return r;
        }
        usadasEnCaso.add(h);
        switch (h) {
            case PERFIL:
                gastar(6);
                perfil(r);
                break;
            case DIRECTORIO:
                gastar(4);
                directorio(r);
                break;
            case LUPA:
                gastar(3);
                lupa(r);
                break;
            case IMAGEN:
                gastar(10);
                imagen(r);
                break;
            case CORRESPONSAL:
                corresponsal(r);
                break;
            default:
                gastar(8);
                red(r);
                break;
        }
        actual.resultados.put(h, r);
        if (usadasEnCaso.size() >= 5) {
            jugador.logros.add("Detective: 5 herramientas en un solo caso");
        }
        return r;
    }

    private void perfil(List<String> r) {
        Publicacion p = actual.pub;
        Cuenta c = p.getCuenta();
        boolean raro = false;
        if (c != null) {
            r.add("Cuenta: " + c.getNombre() + "  " + c.getArroba());
            r.add(c.antiguedadTexto() + "  ·  " + c.seguidoresTexto()
                    + (c.estaVerificada() ? "  ·  cuenta verificada" : ""));
            if (c.esReciente()) {
                r.add("[!] Cuenta recién creada.");
                raro = true;
            }
        }
        r.add("Fuente citada: " + (p.getFuenteCitada() == null ? "ninguna" : p.getFuenteCitada()));
        r.add("Publicaciones de la cuenta marcadas como falsas: " + actual.marcadas + " %");
        if (actual.marcadas >= 50) {
            raro = true;
        }
        for (Senal s : p.getSenales()) {
            r.add("[!] " + s.getEtiqueta());
        }
        if (raro && actual.sospechoso()) {
            insertarConfrontacion("Su cuenta tiene un historial muy raro, ¿no?", true);
        } else if (raro) {
            insertarConfrontacion("Su cuenta tiene un historial muy raro, ¿no?", false);
        }
    }

    private void directorio(List<String> r) {
        Cuenta c = actual.pub.getCuenta();
        String arroba = c == null ? "" : c.getArroba();
        CuentaOficial o = jugador.directorio.buscar(arroba);
        r.add("Búsqueda en el AVL: " + arroba + "  (" + jugador.directorio.getUltimaBusquedaPasos()
                + " comparaciones, altura " + jugador.directorio.getAltura() + ")");
        if (o != null) {
            r.add("[ok] Cuenta OFICIAL de " + o.institucion + ".");
            r.add("Representante: " + o.representante + " (" + o.cargo + ")  ·  credencial " + o.credencial);
            Persona v = actual.visitante;
            if (v.credencial != null && !v.credencial.numero.equals(o.credencial)) {
                r.add("[!] La credencial del visitante dice " + v.credencial.numero + " y el directorio " + o.credencial + ".");
                insertarConfrontacion("Su número de credencial no coincide con el directorio.", true);
            }
        } else {
            r.add("[x] No está en el directorio oficial.");
            String pref = arroba.length() > 5 ? arroba.substring(0, 5) : arroba;
            List<CuentaOficial> parecidas = jugador.directorio.conPrefijo(pref);
            for (CuentaOficial x : parecidas) {
                r.add("   ¿Quisiste decir " + x.arroba + "? (" + x.institucion + ")");
            }
            if (!parecidas.isEmpty()) {
                r.add("[!] El nombre se parece a una cuenta oficial, pero no es igual.");
                insertarConfrontacion("Su cuenta no es la oficial. La oficial es " + parecidas.get(0).arroba + ".",
                        actual.sospechoso());
            }
        }
    }

    /** true si el directorio permite llamar a la institucion de este caso. */
    public boolean puedeLlamar() {
        return actual != null && dia >= 2 && !llamo && !enApagon() && jugador.rol.puedeUsar(Herramienta.DIRECTORIO);
    }

    public String llamar() {
        if (!puedeLlamar()) {
            return "";
        }
        llamo = true;
        gastar(8);
        String t = actual.llamada;
        if (t == null) {
            CuentaOficial o = actual.pub.getCuenta() == null ? null
                    : jugador.directorio.buscar(actual.pub.getCuenta().getArroba());
            t = o == null ? "No hay a quién llamar: esa cuenta no pertenece a ninguna institución."
                    : o.institucion + ": \"Sí, esa publicación es nuestra.\"";
        }
        if (actual.sospechoso() && actual.llamada != null) {
            insertarConfrontacion("Llamé a la institución y dicen otra cosa.", true);
        }
        return t;
    }

    private void lupa(List<String> r) {
        Persona v = actual.visitante;
        if (v.credencial == null) {
            r.add("No trae credencial: es un particular.");
            return;
        }
        r.add("Credencial: " + v.credencial.nombre + " · " + v.credencial.cargo);
        r.add(v.credencial.institucion + " · N.º " + v.credencial.numero + " · vence " + v.credencial.vence);
        if (vencida(v.credencial.vence)) {
            r.add("[!] La credencial está VENCIDA.");
            insertarConfrontacion("Su credencial está vencida.", actual.sospechoso());
        }
        r.add("Compara la foto con la cara. Si algo no cuadra, acúsalo.");
    }

    private static boolean vencida(String mmYyyy) {
        try {
            String[] p = mmYyyy.split("/");
            int mes = Integer.parseInt(p[0]), anio = Integer.parseInt(p[1]);
            return anio < 2026 || (anio == 2026 && mes < 11);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * El jugador dice "la foto no es la misma persona".
     * @return true si tenia razon
     */
    public boolean acusarFoto() {
        if (actual == null) {
            return false;
        }
        boolean razon = actual.visitante.diferencia != null;
        insertarConfrontacion("La persona de la foto no es usted.", razon);
        if (razon) {
            jugador.logros.add("Ojo de halcón: detectaste una foto que no coincide");
        } else {
            jugador.reputacion = Math.max(0, jugador.reputacion - 1);
        }
        return razon;
    }

    private void imagen(List<String> r) {
        if (actual.imagen == null && actual.pub.getImagen() == null) {
            r.add("La publicación no trae imagen.");
            return;
        }
        String t = actual.imagen != null ? actual.imagen : "Sin coincidencias anteriores: la imagen parece original.";
        r.add(t);
        if (actual.imagen != null && actual.sospechoso() && !t.startsWith("La publicación no")) {
            insertarConfrontacion("Esta foto ya existía. No es de aquí ni de ahora.", true);
        }
    }

    /** El corresponsal sale de Civitas Verifica (zona 1) por la ruta mas corta (Dijkstra). */
    private void corresponsal(List<String> r) {
        gastar(2);
        if (corresponsalLlega >= 0) {
            if (corresponsalListo()) {
                r.add(recibirReporte());
            } else {
                r.add("El corresponsal sigue en camino...");
            }
            return;
        }
        rutaCorresponsal = jugador.ciudad.mapa.caminoMasCorto(1, actual.zona);
        int largo = jugador.ciudad.mapa.largo(rutaCorresponsal);
        int minutos = Math.max(15, largo / 7);
        salidaCorresponsal = minuto;
        corresponsalLlega = minuto + minutos;
        StringBuilder sb = new StringBuilder();
        for (GrafoCiudad.Zona z : rutaCorresponsal) {
            if (sb.length() > 0) {
                sb.append(" › ");
            }
            sb.append(z.nombre);
        }
        r.add("Ruta (Dijkstra): " + sb);
        r.add("Distancia " + largo + " m · llega en " + minutos + " min de juego.");
    }

    public boolean corresponsalEnCamino() {
        return corresponsalLlega >= 0 && minuto < corresponsalLlega;
    }

    public boolean corresponsalListo() {
        return corresponsalLlega >= 0 && minuto >= corresponsalLlega;
    }

    /** 0..1 avance del corresponsal por su ruta. */
    public double avanceCorresponsal() {
        if (corresponsalLlega < 0) {
            return 0;
        }
        double total = corresponsalLlega - salidaCorresponsal;
        return Math.max(0, Math.min(1, 1 - (corresponsalLlega - minuto) / Math.max(1, total)));
    }


    public List<GrafoCiudad.Zona> getRutaCorresponsal() {
        return rutaCorresponsal;
    }

    /** La vista lo llama en cada cuadro: devuelve el reporte UNA vez, cuando el corresponsal llega. */
    public String revisarCorresponsal() {
        if (actual != null && corresponsalListo() && !reporteRecibido) {
            return recibirReporte();
        }
        return null;
    }

    private String recibirReporte() {
        reporteRecibido = true;
        List<String> r = new ArrayList<>();
        r.add("Reporte desde " + CiudadEstado.ZONAS[actual.zona] + ": " + actual.reporteCalle);
        actual.resultados.put(Herramienta.CORRESPONSAL, r);
        if (actual.sospechoso() && actual.zona > 1) {
            insertarConfrontacion("Mi corresponsal está allá y dice que no es cierto.", true);
        }
        return r.get(0);
    }

    private void red(List<String> r) {
        Vertice o = actual.origen;
        GrafoRed g = jugador.ciudad.red;
        if (o == null || !g.contiene(o.id)) {
            r.add("No se encontró el origen en la red.");
            return;
        }
        r.add("Primero en compartir: " + o.nombre + " (" + o.tipo.getEtiqueta() + ")");
        List<List<Vertice>> oleadas = g.bfsOleadas(o, 2);
        int bots = 0, total = 0;
        for (int k = 1; k < oleadas.size(); k++) {
            for (Vertice v : oleadas.get(k)) {
                total++;
                if (v.tipo == TipoCuenta.BOT) {
                    bots++;
                }
            }
        }
        int pct = total == 0 ? 0 : 100 * bots / total;
        r.add("BFS 2 niveles: " + total + " cuentas alcanzadas, " + pct + " % son cuentas falsas.");
        if (o.tipo == TipoCuenta.BOT) {
            int anillo = 0;
            for (Vertice v : g.dfsAlcanzables(o)) {
                if (v.tipo == TipoCuenta.BOT) {
                    anillo++;
                }
            }
            r.add("[!] DFS: el origen pertenece a un ANILLO de " + anillo + " cuentas falsas que se comparten entre sí.");
            insertarConfrontacion("Lo que trae lo empujan cuentas falsas conectadas entre sí.", true);
        } else {
            r.add("El origen es una cuenta real.");
        }
    }

    public List<String> resultados(Herramienta h) {
        return actual == null ? null : actual.resultados.get(h);
    }

    // ==================================================================
    // Sellar
    // ==================================================================

    public List<Veredicto> sellos() {
        return jugador.arbol.sellos();
    }

    public Resultado sellar(Veredicto v) {
        Caso c = actual;
        Resultado res = new Resultado();
        res.caso = c;
        res.veredicto = v;
        res.hoja = jugador.arbol.resolver(v, c.verdad);
        if (res.hoja == null) {
            return null;
        }
        c.decidido = v;
        res.ruta = jugador.arbol.rutaHasta(res.hoja);
        EfectoCiudad e = res.hoja.efecto;
        res.efecto = e;
        CiudadEstado ciudad = jugador.ciudad;
        ciudad.aplicar(e);
        boolean bulo = c.verdad == Verdad.FALSA || c.verdad == Verdad.IMPOSTOR;

        // efectos en los grafos
        if (v == Veredicto.PUBLICAR || v == Veredicto.OPINION || v == Veredicto.ETIQUETA) {
            res.propagacion = ciudad.publicar(c.origen, bulo);
            res.alcance = res.propagacion.alcance();
            if (bulo && c.zona > 1) {
                ciudad.iniciarPanico(c.zona);
            }
        } else if (v == Veredicto.DENUNCIAR) {
            res.corregidos = c.verdad == Verdad.IMPOSTOR ? ciudad.denunciar(c.origen) : 0;
            if (c.verdad != Verdad.IMPOSTOR) {
                ciudad.ajustarDesinformacion(e.desinformacion / 2);
            }
        } else {
            ciudad.ajustarDesinformacion(e.desinformacion / 2);
        }
        if (e.acierto && c.zona > 1) {
            ciudad.panico[c.zona] = 0;
        }

        // puntos, dinero, reputacion
        res.puntos = e.puntos;
        res.dinero = e.dinero;
        if (e.acierto && minuto - minutoInicioCaso <= 25) {
            res.puntos += 30;
            res.bonoRapidez = true;
        }
        if (e.acierto && c.confrontado) {
            res.puntos += 40;
        }
        if (c.pago > 0 && (v == Veredicto.PUBLICAR || v == Veredicto.OPINION)) {
            res.dinero += c.pago;                       // aceptaste la plata
        }
        // INFLUENCER: su alcance es enorme, asi que sus decisiones pesan x1,5 (en puntos y en la ciudad)
        if (jugador.rol == RolJugador.INFLUENCER) {
            res.puntos = (int) Math.round(res.puntos * jugador.rol.impacto());
            ciudad.aplicarEscalado(e, jugador.rol.impacto() - 1);
            if (v == Veredicto.PUBLICAR || v == Veredicto.OPINION || v == Veredicto.ETIQUETA) {
                ciudad.ajustarDesinformacion(bulo ? 3 : -1);
            }
        }
        jugador.puntos += res.puntos;
        jugador.dinero += res.dinero;
        jugador.reputacion = CiudadEstado.lim(jugador.reputacion + e.reputacion);
        jugador.casosAtendidos++;
        if (e.acierto) {
            jugador.aciertos++;
            aciertosDia++;
            if (c.verdad == Verdad.IMPOSTOR) {
                jugador.impostoresAtrapados++;
            }
        } else {
            jugador.errores++;
            erroresDia++;
        }
        if (e.grave) {
            if (jugador.seguros > 0) {
                jugador.seguros--;            // TIENDA: el seguro de reputacion absorbe la advertencia
                res.seguroUsado = true;
            } else {
                jugador.advertir();
                res.advertencia = true;
                res.despedido = jugador.despedido;
            }
        }
        puntosDia += res.puntos;

        // historia
        if (c.esJefe) {
            if (v == Veredicto.DENUNCIAR) {
                jugador.denuncioJefe = true;
                res.logrosNuevos.add("Incorruptible: denunciaste a tu propio jefe");
            } else if (v != Veredicto.RECHAZAR) {
                jugador.aproboDeepfake = true;
            }
        }
        if (c.pago >= 500 && (v == Veredicto.DENUNCIAR || v == Veredicto.RECHAZAR)) {
            res.logrosNuevos.add("Sin precio: rechazaste un soborno de $" + c.pago);
        }
        if (res.bonoRapidez) {
            res.logrosNuevos.add("Rayo: caso resuelto en menos de 25 minutos");
        }
        for (String l : res.logrosNuevos) {
            jugador.logros.add(l);
        }
        res.titular = e.acierto ? c.titularBien : c.titularMal;
        if (res.titular == null || res.titular.isEmpty()) {
            res.titular = e.explicacion;
        }
        titulares.add(res.titular);
        jugador.bitacora.add("Día " + dia + " · " + c.visitante.nombre + " › " + v.sello
                + (e.acierto ? " [ok]" : " [x] (era: " + c.correcto.sello + ")"));
        atendidos.add(c);
        resultados.add(res);
        actual = null;
        if (jugador.despedido) {
            cerrar();
        }
        return res;
    }

    // ==================================================================
    // Habilidad unica del rol
    // ==================================================================

    /** true si el jugador puede usar su habilidad con el caso actual (una vez por caso). */
    public boolean puedeUsarHabilidad() {
        return actual != null && jugador.rol.tieneMiniJuego() && !actual.habilidadUsada;
    }

    /** CIUDADANO: la cadena de reenvios del caso actual (camino del arbol BFS en el grafo social). */
    public Habilidades.Cadena prepararCadena() {
        return Habilidades.cadena(actual == null ? null : actual.origen, jugador.ciudad.red, jugador.ciudad.usuarios, azar);
    }

    /** INFLUENCER: los perfiles de las primeras oleadas (BFS) desde el origen. */
    public List<Habilidades.Perfil> prepararPerfiles() {
        return Habilidades.perfiles(actual == null ? null : actual.origen, jugador.ciudad.red, jugador.ciudad.usuarios, azar);
    }

    /** CANDIDATO: las 3 preguntas de la rueda de prensa. */
    public List<Habilidades.Pregunta> prepararRueda() {
        return Habilidades.ruedaDePrensa(azar);
    }

    /**
     * Cierra el mini-juego de la habilidad: gasta minutos de juego, anota las
     * pistas en la libreta y, si salio bien y el caso es sospechoso, INSERTA
     * una confrontacion en el arbol de dialogo (igual que las herramientas).
     */
    public List<String> terminarHabilidad(boolean exito, int minutos) {
        List<String> pistas = new ArrayList<>();
        if (actual == null) {
            return pistas;
        }
        actual.habilidadUsada = true;
        jugador.habilidadesUsadas++;
        gastar(minutos);
        Caso c = actual;
        switch (jugador.rol) {
            case CIUDADANO: {
                Vertice o = c.origen;
                String tipo = o == null ? "desconocido" : o.tipo.getEtiqueta();
                if (exito) {
                    if (o != null && o.tipo == TipoCuenta.BOT) {
                        pistas.add("[!] La cadena empieza en una cuenta falsa: " + o.nombre + ".");
                        insertarConfrontacion("Rastreé la cadena y empieza en una cuenta falsa.", true);
                    } else if (o != null && o.tipo == TipoCuenta.MEDIO) {
                        pistas.add("[ok] La cadena empieza en un medio reconocido (" + o.nombre + ").");
                    } else if (o != null && o.tipo == TipoCuenta.INFLUENCER) {
                        pistas.add("[!] La cadena empieza en un influencer: " + o.nombre + ". ¿Le pagaron?");
                        if (c.verdad == Verdad.PUBLICIDAD_OCULTA) {
                            insertarConfrontacion("Rastreé la cadena: empieza en un influencer pagado.", true);
                        }
                    } else {
                        pistas.add("Cadena rastreada: empezó en una persona común (" + tipo + "), no en una fuente oficial.");
                    }
                } else {
                    pistas.add("Te perdiste en la cadena de reenvíos: no encontraste el origen.");
                }
                break;
            }
            case INFLUENCER: {
                Vertice o = c.origen;
                int bots = 0, total = 0;
                if (o != null && jugador.ciudad.red.contiene(o.id)) {
                    List<List<Vertice>> ol = jugador.ciudad.red.bfsOleadas(o, 2);
                    for (int k = 1; k < ol.size(); k++) {
                        for (Vertice v : ol.get(k)) {
                            total++;
                            bots += v.tipo == TipoCuenta.BOT ? 1 : 0;
                        }
                    }
                }
                int pct = total == 0 ? 0 : 100 * bots / total;
                if (exito) {
                    jugador.ciudad.ajustarDesinformacion(-2);     // bloqueaste las cuentas falsas que viste
                    if (o != null && o.tipo == TipoCuenta.BOT) {
                        pistas.add("[!] Cazabots: el " + pct + " % de quienes la empujan son cuentas falsas.");
                        insertarConfrontacion("Lo que trae lo están empujando cuentas falsas.", true);
                    } else {
                        pistas.add("[ok] Cazabots: la difusión la mueve gente real (" + pct + " % de cuentas falsas).");
                    }
                } else {
                    pistas.add("Cazabots: marcaste cuentas reales o se te escaparon los bots.");
                }
                break;
            }
            case CANDIDATO: {
                if (exito) {
                    String p = Habilidades.pistaPrensa(c.verdad);
                    pistas.add(p);
                    if (c.sospechoso() || c.verdad == Verdad.PUBLICIDAD_OCULTA) {
                        insertarConfrontacion(c.verdad == Verdad.PUBLICIDAD_OCULTA
                                ? "En la rueda de prensa dicen que a usted le pagan." : "En la rueda de prensa me dijeron que eso ya fue desmentido.", true);
                    }
                } else {
                    pistas.add("La rueda de prensa se desordenó: no sacaste nada en claro.");
                }
                break;
            }
            default:
                break;
        }
        if (exito) {
            jugador.habilidadesBien++;
            jugador.reputacion = CiudadEstado.lim(jugador.reputacion + 1);
            if (jugador.habilidadesBien == 3) {
                jugador.logros.add("Maestría de " + jugador.rol.nombre + ": 3 habilidades bien resueltas");
            }
        }
        c.pistasHabilidad.addAll(pistas);
        return pistas;
    }

    /**
     * Efecto de UNA respuesta en la rueda de prensa del candidato.
     * R responsable, A ataque, M repite un rumor, E evasiva (o se acabo el tiempo).
     */
    public void respuestaPrensa(char tipo) {
        CiudadEstado ci = jugador.ciudad;
        switch (tipo) {
            case 'R':
                ci.confianza = CiudadEstado.lim(ci.confianza + 2);
                jugador.apoyo += 3;
                break;
            case 'A':
                ci.conflictos = CiudadEstado.lim(ci.conflictos + 3);
                ci.convivencia = CiudadEstado.lim(ci.convivencia - 2);
                jugador.apoyo -= 1;
                break;
            case 'M':
                ci.ajustarDesinformacion(2);
                jugador.apoyo -= 2;
                break;
            default:
                ci.confianza = CiudadEstado.lim(ci.confianza - 1);
                break;
        }
    }

    // ==================================================================
    // Alerta viral (decision con 10 segundos)
    // ==================================================================

    /** La alerta viral que acaba de aparecer (una sola vez). */
    public Habilidades.Viral tomarViral() {
        Habilidades.Viral v = viralPendiente;
        viralPendiente = null;
        return v;
    }

    /**
     * Resuelve la alerta viral. opcion: 0 verificar, 1 reportar, 2 ignorar
     * (o se acabo el tiempo), 3 compartir.
     * @return {puntos, acierto(1/0)} y el texto de la consecuencia en msg[0]
     */
    public int[] resolverViral(Habilidades.Viral v, int opcion, String[] msg) {
        CiudadEstado ci = jugador.ciudad;
        int pts;
        boolean ok;
        if (v.verdadera) {
            switch (opcion) {
                case 0: ci.ajustarDesinformacion(-4); ci.verificada = CiudadEstado.lim(ci.verificada + 2);
                    pts = 40; ok = true; msg[0] = "Verificaste: era cierto y de una cuenta oficial. La información correcta circula."; break;
                case 3: ci.ajustarDesinformacion(-3); pts = 20; ok = true;
                    msg[0] = "Compartiste un dato cierto de una cuenta oficial. Bien, aunque verificar primero es mejor."; break;
                case 1: ci.confianza = CiudadEstado.lim(ci.confianza - 3); pts = -30; ok = false;
                    msg[0] = "Reportaste una información verdadera y oficial: eso también hace daño."; break;
                default: pts = 0; ok = false; msg[0] = "Lo dejaste pasar. Era cierto, pero el rumor viral siguió su curso."; break;
            }
        } else {
            switch (opcion) {
                case 0: ci.ajustarDesinformacion(-5); ci.verificada = CiudadEstado.lim(ci.verificada + 2);
                    pts = 40; ok = true; msg[0] = "Verificaste antes de compartir: era falso y frenaste su avance."; break;
                case 1: ci.ajustarDesinformacion(-6); ci.convivencia = CiudadEstado.lim(ci.convivencia + 1);
                    pts = 50; ok = true; msg[0] = "Reportaste un bulo: Civitas lo marcó como falso."; break;
                case 3: ci.ajustarDesinformacion(3); ci.conflictos = CiudadEstado.lim(ci.conflictos + 3);
                    pts = -50; ok = false; msg[0] = "Compartiste un bulo: llegó a más gente y causó discusiones."; break;
                default: pts = 0; ok = false; msg[0] = "No hiciste nada y el bulo siguió volviéndose viral."; break;
            }
        }
        if (jugador.rol == RolJugador.INFLUENCER) {
            pts = (int) Math.round(pts * jugador.rol.impacto());
        }
        jugador.puntos += pts;
        puntosDia += pts;
        if (ok) {
            jugador.viralesBien++;
            if (jugador.viralesBien == 2) {
                jugador.logros.add("Antiviral: frenaste 2 alertas virales a tiempo");
            }
        } else {
            jugador.viralesMal++;
        }
        return new int[]{pts, ok ? 1 : 0};
    }

    // ==================================================================
    // Para la tienda
    // ==================================================================

    /** Detiene el reloj unos segundos (cafe). */
    public void pausar(double segundos) {
        pausaSeg += segundos;
    }

    /** Devuelve minutos al reloj (horas extra). */
    public void devolverMinutos(int minutos) {
        minuto = Math.max(INICIO, minuto - minutos);
    }

    /** Permite volver a usar la habilidad con el cliente actual. */
    public boolean recargarHabilidad() {
        if (actual == null || !actual.habilidadUsada || !jugador.rol.tieneMiniJuego()) {
            return false;
        }
        actual.habilidadUsada = false;
        return true;
    }

    /** Pista del informante: lo que el caso ES, dicho de forma indirecta. */
    public String pistaInformante() {
        if (actual == null) {
            return null;
        }
        String p = "Informante: " + Habilidades.pistaPrensa(actual.verdad).replaceFirst("^\\[[^\\]]*\\]\\s*", "");
        String marca = actual.sospechoso() || actual.verdad == Verdad.PUBLICIDAD_OCULTA ? "[!] " : actual.verdad == Verdad.VERDADERA ? "[ok] " : "";
        actual.pistasHabilidad.add(marca + p);
        return marca + p;
    }

    /** Fin del dia: los que quedaron en la fila sin atender dejan circular sus rumores. */
    public void cerrar() {
        if (terminada) {
            return;
        }
        terminada = true;
        int sinAtender = fila.size() + (actual != null ? 1 : 0);
        for (Caso c : fila) {
            if (c.sospechoso()) {
                jugador.ciudad.ajustarDesinformacion(3);
            }
        }
        if (actual != null && actual.sospechoso()) {
            jugador.ciudad.ajustarDesinformacion(3);
        }
        jugador.puntos -= 40 * sinAtender;
        puntosDia -= 40 * sinAtender;
        if (aciertosDia > 0 && erroresDia == 0 && sinAtender == 0) {
            jugador.logros.add("Día perfecto (día " + dia + ")");
        }
        jugador.dinero += 100; // sueldo
    }

    public int sinAtender() {
        return fila.size() + (actual != null ? 1 : 0);
    }

    public List<Resultado> getResultados() {
        return resultados;
    }

    public List<String> getTitulares() {
        return titulares;
    }

    public int getPuntosDia() {
        return puntosDia;
    }

    public int getAciertosDia() {
        return aciertosDia;
    }

    public int getErroresDia() {
        return erroresDia;
    }
}
