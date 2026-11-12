package influenceinc.logica;

import influenceinc.estructuras.GrafoCiudad;
import influenceinc.estructuras.GrafoRed;
import influenceinc.estructuras.Vertice;
import influenceinc.modelo.EfectoCiudad;
import influenceinc.modelo.TipoCuenta;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * El estado de Ciudad Nova para UN verificador: los indicadores del
 * enunciado, el grafo de la ciudad (con el panico por zonas) y el grafo
 * social (quien cree que).
 */
public class CiudadEstado {

    public static final String[] ZONAS = {"Plaza Central", "Civitas Verifica", "Alcaldía", "Colegio",
        "Mercado", "Parque", "Universidad", "Barrio Norte"};
    public static final int[][] POS = {{0, 0}, {190, -185}, {-185, -185}, {-230, 30}, {-110, 200},
        {120, 210}, {235, 20}, {0, -235}};
    private static final int[][] CALLES = {{0, 2}, {0, 1}, {0, 3}, {0, 4}, {0, 5}, {0, 6}, {0, 7},
        {2, 7}, {7, 1}, {1, 6}, {6, 5}, {5, 4}, {4, 3}, {3, 2}};

    // indicadores 0..100 (el de desinformacion sale del grafo)
    public int verificada = 50, confianza = 55, convivencia = 60, bienestar = 60, conflictos = 25;

    public final GrafoCiudad mapa = new GrafoCiudad();
    /** Nivel de panico por zona: 0 nada; >0 hay panico (se propaga por BFS). */
    public final int[] panico = new int[ZONAS.length];
    public final GrafoRed red = new GrafoRed();
    public final List<Vertice> usuarios = new ArrayList<>();
    public final List<Vertice> bots = new ArrayList<>();
    public final List<Vertice> influencers = new ArrayList<>();
    public Vertice medio, agencia;
    private final Random azar;

    public CiudadEstado(long semilla) {
        azar = new Random(semilla);
        for (int i = 0; i < ZONAS.length; i++) {
            mapa.agregarZona(ZONAS[i], POS[i][0], POS[i][1]);
        }
        for (int[] c : CALLES) {
            mapa.agregarCalle(c[0], c[1]);
        }
        construirRed();
    }

    private void construirRed() {
        // 30 personas repartidas en las zonas
        for (int i = 0; i < 30; i++) {
            int zona = 2 + i % 6;
            Vertice v = new Vertice("u" + i, "Persona " + (i + 1), TipoCuenta.USUARIO, zona,
                    POS[zona][0] + azar.nextInt(60) - 30, 0, POS[zona][1] + azar.nextInt(60) - 30);
            red.agregarVertice(v);
            usuarios.add(v);
        }
        for (int z = 2; z <= 7; z++) {
            List<Vertice> g = new ArrayList<>();
            for (Vertice v : usuarios) {
                if (v.comunidad == z) {
                    g.add(v);
                }
            }
            for (int i = 0; i < g.size(); i++) {
                red.conectarMutuo(g.get(i), g.get((i + 1) % g.size()), 0.5 + azar.nextDouble() * 0.2, 2);
            }
        }
        for (int k = 0; k < 16; k++) {
            Vertice a = usuarios.get(azar.nextInt(usuarios.size())), b = usuarios.get(azar.nextInt(usuarios.size()));
            if (a != b) {
                red.conectarMutuo(a, b, 0.35 + azar.nextDouble() * 0.2, 4);
            }
        }
        // anillo de cuentas falsas: se siguen entre ellas y le llegan a mucha gente
        for (int i = 0; i < 7; i++) {
            Vertice b = new Vertice("b" + i, "@cuenta_falsa_" + (i + 1), TipoCuenta.BOT, -1,
                    -40 + i * 13, 60, 280);
            red.agregarVertice(b);
            bots.add(b);
        }
        for (int i = 0; i < bots.size(); i++) {
            red.conectarMutuo(bots.get(i), bots.get((i + 1) % bots.size()), 0.9, 1);
            for (int k = 0; k < 4; k++) {
                red.conectar(bots.get(i), usuarios.get(azar.nextInt(usuarios.size())), 0.7, 2);
            }
        }
        String[] inf = {"Vale Fit", "Alex Trend", "Luna Social"};
        for (int i = 0; i < inf.length; i++) {
            Vertice v = new Vertice("i" + i, inf[i], TipoCuenta.INFLUENCER, -1, 0, 0, 0);
            red.agregarVertice(v);
            influencers.add(v);
            for (int k = 0; k < 8; k++) {
                red.conectar(v, usuarios.get(azar.nextInt(usuarios.size())), 0.6, 3);
            }
        }
        medio = new Vertice("medio", "Noticias Nova", TipoCuenta.MEDIO, -1, 0, 0, 0);
        red.agregarVertice(medio);
        for (int k = 0; k < 9; k++) {
            red.conectar(medio, usuarios.get(azar.nextInt(usuarios.size())), 0.6, 3);
        }
        agencia = new Vertice("agencia", "Influence, Inc.", TipoCuenta.AGENCIA, -1, 0, 0, 0);
        red.agregarVertice(agencia);
        for (int k = 0; k < 10; k++) {
            red.conectar(agencia, usuarios.get(azar.nextInt(usuarios.size())), 0.7, 2);
        }
        // al empezar, algo de desinformacion ya circula
        for (int k = 0; k < 5; k++) {
            usuarios.get(azar.nextInt(usuarios.size())).desinformado = true;
        }
    }

    /** % de personas que creen un bulo. */
    public int desinformacion() {
        int d = 0;
        for (Vertice v : usuarios) {
            if (v.desinformado) {
                d++;
            }
        }
        return Math.round(100f * d / usuarios.size());
    }

    public Vertice origenPara(String tipo, int zona) {
        switch (tipo) {
            case "BOT": {
                List<Vertice> vivos = botsActivos();
                return vivos.isEmpty() ? usuarios.get(azar.nextInt(usuarios.size())) : vivos.get(azar.nextInt(vivos.size()));
            }
            case "MEDIO":
                return medio;
            case "INFLUENCER":
                return influencers.get(azar.nextInt(influencers.size()));
            default: {
                for (Vertice v : usuarios) {
                    if (v.comunidad == zona && azar.nextBoolean()) {
                        return v;
                    }
                }
                return usuarios.get(azar.nextInt(usuarios.size()));
            }
        }
    }

    public List<Vertice> botsActivos() {
        List<Vertice> l = new ArrayList<>();
        for (Vertice b : bots) {
            if (red.contiene(b.id)) {
                l.add(b);
            }
        }
        return l;
    }

    /** Aplica los numeros de una hoja del arbol de consecuencias. */
    public void aplicar(EfectoCiudad e) {
        verificada = lim(verificada + e.verificada);
        confianza = lim(confianza + e.confianza);
        convivencia = lim(convivencia + e.convivencia);
        bienestar = lim(bienestar + e.bienestar);
        conflictos = lim(conflictos + e.conflictos);
    }

    /** Aplica una fraccion (k) de una hoja: el influencer amplifica sus decisiones. */
    public void aplicarEscalado(EfectoCiudad e, double k) {
        if (k <= 0) {
            return;
        }
        verificada = lim(verificada + (int) Math.round(e.verificada * k));
        confianza = lim(confianza + (int) Math.round(e.confianza * k));
        convivencia = lim(convivencia + (int) Math.round(e.convivencia * k));
        bienestar = lim(bienestar + (int) Math.round(e.bienestar * k));
        conflictos = lim(conflictos + (int) Math.round(e.conflictos * k));
    }

    public static int lim(int v) {
        return Math.max(0, Math.min(100, v));
    }

    /**
     * Publicar con el sello de la agencia: BFS probabilistico desde la agencia
     * y desde la cuenta que lo origino. Si es mentira, quien lo recibe queda
     * desinformado.
     * @return cuantas personas lo recibieron
     */
    public GrafoRed.ResultadoPropagacion publicar(Vertice origen, boolean bulo) {
        if (origen != null && red.contiene(origen.id) && agencia.aristaHacia(origen) == null && origen != agencia) {
            red.conectar(agencia, origen, 1.0, 1);
        }
        GrafoRed.ResultadoPropagacion r = red.propagar(agencia, 3,
                a -> a.probabilidad * (bulo ? 0.7 : 0.6),
                v -> v.tipo == TipoCuenta.USUARIO || v.tipo == TipoCuenta.BOT || v.tipo == TipoCuenta.INFLUENCER
                        || v.tipo == TipoCuenta.MEDIO,
                azar);
        for (Vertice v : r.alcanzados()) {
            if (v.tipo == TipoCuenta.USUARIO) {
                if (bulo && !v.escuchoDesmentido) {
                    v.desinformado = true;
                } else if (!bulo && v.desinformado && azar.nextDouble() < 0.25) {
                    v.desinformado = false;      // la informacion buena tambien corrige un poco
                }
            }
        }
        if (origen != null) {
            red.desconectar(agencia, origen);
        }
        return r;
    }

    /**
     * Denunciar: si la cuenta de origen es falsa se ELIMINA del grafo, y un
     * desmentido (BFS con tope de 2 saltos) corrige a quien no lo habia reenviado.
     * @return cuantas personas corrigio
     */
    public int denunciar(Vertice origen) {
        if (origen == null) {
            return 0;
        }
        List<Vertice> vecinos = red.contiene(origen.id) ? red.vecinosSalida(origen) : new ArrayList<Vertice>();
        if (origen.tipo == TipoCuenta.BOT && red.contiene(origen.id)) {
            red.eliminarVertice(origen.id);
        }
        int c = 0;
        for (Vertice v : vecinos) {
            if (red.contiene(v.id)) {
                c += red.corregir(v, 1).corregidos.size();
            }
        }
        return c;
    }

    /**
     * Si el numero de la hoja cambia la desinformacion, se ajusta el grafo:
     * delta > 0 = mas personas creen el bulo; delta < 0 = algunas se corrigen.
     */
    public void ajustarDesinformacion(int delta) {
        if (delta == 0) {
            return;
        }
        int n = Math.max(1, Math.round(Math.abs(delta) * usuarios.size() / 100f));
        List<Vertice> candidatos = new ArrayList<>();
        for (Vertice v : usuarios) {
            if (delta > 0 ? !v.desinformado && !v.escuchoDesmentido : v.desinformado) {
                candidatos.add(v);
            }
        }
        for (int i = 0; i < n && !candidatos.isEmpty(); i++) {
            candidatos.remove(azar.nextInt(candidatos.size())).desinformado = delta > 0;
        }
    }

    /** Cada noche los rumores circulan solos (presion de fondo). */
    public void nocheDeRumores() {
        for (Vertice v : usuarios) {
            v.escuchoDesmentido = false;
        }
        ajustarDesinformacion(7);
    }

    /** Panico: empieza en una zona y cada cierto tiempo salta a las vecinas (BFS por niveles). */
    public void iniciarPanico(int zona) {
        if (zona > 1 && panico[zona] == 0) {
            panico[zona] = 1;
        }
    }

    /** Un paso del BFS del panico: las zonas vecinas de las que tienen panico se contagian. */
    public int propagarPanico() {
        List<Integer> nuevas = new ArrayList<>();
        for (GrafoCiudad.Zona z : mapa.getZonas()) {
            if (panico[z.id] > 0) {
                for (GrafoCiudad.Zona v : mapa.vecinos(z)) {
                    if (panico[v.id] == 0 && v.id > 1 && azar.nextDouble() < 0.5) {
                        nuevas.add(v.id);
                    }
                }
            }
        }
        for (int z : nuevas) {
            panico[z] = 1;
        }
        int activas = 0;
        for (int p : panico) {
            if (p > 0) {
                activas++;
            }
        }
        convivencia = lim(convivencia - activas / 2);
        bienestar = lim(bienestar - (activas + 1) / 2);
        return nuevas.size();
    }

    public void calmarPanico() {
        for (int i = 0; i < panico.length; i++) {
            panico[i] = 0;
        }
    }

    /** Puntaje de la ciudad (resultado general que pide el enunciado). */
    public int salud() {
        return Math.round((verificada + confianza + convivencia + bienestar + (100 - desinformacion())
                + (100 - conflictos)) / 6f);
    }
}
