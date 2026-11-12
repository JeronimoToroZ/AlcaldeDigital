package influenceinc.pruebas;

import influenceinc.estructuras.NodoDialogo;
import influenceinc.logica.Caso;
import influenceinc.logica.Historia;
import influenceinc.logica.Jornada;
import influenceinc.logica.JugadorVerificador;
import influenceinc.logica.Partida;
import influenceinc.modelo.Herramienta;
import influenceinc.modelo.Veredicto;
import influenceinc.modelo.Verdad;
import influenceinc.red.ClienteOficina;
import influenceinc.red.ServidorOficina;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PRUEBA DE LA PARTIDA COMPLETA Y DE LA RED (consola).
 *  - Juega los 5 dias tres veces: perfecto, publicando todo y rechazando todo.
 *  - Comprueba confrontaciones, herramientas, finales y el duelo.
 *  - Levanta un servidor en un puerto local y conecta dos clientes.
 */
public class PruebaJuego {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== HISTORIA ===");
        int total = 0;
        for (int d = 1; d <= Historia.DIAS; d++) {
            total += Historia.casosDelDia(d, 1).size();
        }
        comprobar(total == 15, "15 casos en 5 días (máximo 3 por día)");
        for (int d = 1; d <= Historia.DIAS; d++) {
            comprobar(Historia.casosDelDia(d, 1).size() <= Historia.MAX_CASOS_DIA, "día " + d + ": máximo 3 personas");
        }

        System.out.println("\n=== UN CASO POR DENTRO (día 2, impostora del Acueducto) ===");
        Partida p = new Partida(Partida.Modo.SOLO, Arrays.asList("Prueba"), 5, false);
        jugarDia(p, 0);
        p.siguienteTurno();
        Jornada j = p.iniciarJornada();
        comprobar(j.sellos().contains(Veredicto.ETIQUETA), "el día 2 aparece el sello #PUBLICIDAD (rama insertada)");
        j.siguiente();
        Caso c = j.siguiente();
        comprobar(c.verdad == Verdad.IMPOSTOR && c.visitante.diferencia != null, "la impostora tiene un rasgo cambiado");
        int nodos = c.dialogo.cantidadNodos();
        j.usar(Herramienta.DIRECTORIO);
        comprobar(c.dialogo.cantidadNodos() == nodos + 1, "el directorio la contradice: se INSERTA una confrontación");
        comprobar(j.acusarFoto(), "la lupa la delata");
        NodoDialogo confesion = null;
        for (NodoDialogo n : j.opciones()) {
            if (n.insertado && n.respuesta.equals(c.confesion)) {
                confesion = n;
            }
        }
        comprobar(confesion != null, "con 2 contradicciones aparece la confesión");
        j.preguntar(confesion);
        Jornada.Resultado r = j.sellar(Veredicto.DENUNCIAR);
        comprobar(r.efecto.acierto && r.puntos >= 150 + 40, "denunciar tras la confesión da bono");
        comprobar(j.usar(Herramienta.RED).isEmpty(), "sin caso abierto no hay herramienta");

        System.out.println("\n=== PARTIDAS COMPLETAS ===");
        JugadorVerificador perfecto = jugarTodo(0);
        comprobar(Historia.finalPara(perfecto)[0].equals("EL SELLO VALIENTE"), "perfecto: " + Historia.finalPara(perfecto)[0]);
        JugadorVerificador ingenuo = jugarTodo(1);
        comprobar(ingenuo.despedido, "publicar todo: " + Historia.finalPara(ingenuo)[0]);
        JugadorVerificador miedoso = jugarTodo(2);
        comprobar(!miedoso.despedido && Historia.finalPara(miedoso)[0].equals("CIUDAD DIVIDIDA"),
                "rechazar todo: " + Historia.finalPara(miedoso)[0]);

        System.out.println("\n=== DUELO LOCAL ===");
        Partida duelo = new Partida(Partida.Modo.DUELO, Arrays.asList("Ana", "Luis"), 9, false);
        int turnos = 0;
        do {
            jugarDia(duelo, duelo.getTurno() == 0 ? 0 : 2);
            turnos++;
        } while (duelo.siguienteTurno());
        comprobar(turnos == 10 && duelo.terminada(), "2 jugadores x 5 días = 10 turnos");
        comprobar(duelo.ranking().get(0).nombre.equals("Ana"), "gana quien verificó mejor");
        comprobar(duelo.jugadores.get(0).ciudad != duelo.jugadores.get(1).ciudad, "cada uno con su propia ciudad");

        System.out.println("\n=== RED (servidor + 2 clientes en este computador) ===");
        int puerto = 5600 + (int) (Math.random() * 300);
        ServidorOficina s = new ServidorOficina(puerto);
        final List<String> lobby = new ArrayList<>();
        final long[] semillas = {-1, -1};
        final List<String[]> marc = new ArrayList<>();
        ClienteOficina a = new ClienteOficina("127.0.0.1", puerto, "Ana", oyente(lobby, semillas, 0, marc));
        ClienteOficina b = new ClienteOficina("127.0.0.1", puerto, "Ana", oyente(lobby, semillas, 1, marc));
        esperar(() -> s.jugadores() == 2);
        comprobar(s.jugadores() == 2, "dos clientes en la sala");
        esperar(() -> lobby.contains("Ana 2"));
        comprobar(lobby.contains("Ana 2"), "nombres repetidos se renombran (Ana 2)");
        s.iniciar(777, false);
        esperar(() -> semillas[0] == 777 && semillas[1] == 777);
        comprobar(semillas[0] == 777 && semillas[1] == 777, "los dos reciben la MISMA semilla");
        a.enviarResultado(450, 2, 6, 0, false);
        esperar(() -> !marc.isEmpty() && marc.get(0)[1].equals("450"));
        comprobar(!marc.isEmpty() && marc.get(0)[1].equals("450"), "el marcador en vivo llega a todos");
        a.cerrar();
        b.cerrar();
        s.cerrar();

        System.out.println(fallos == 0 ? "\nTODAS LAS PRUEBAS PASARON" : "\nFALLOS: " + fallos);
        System.exit(fallos == 0 ? 0 : 1);
    }

    private static ClienteOficina.Oyente oyente(List<String> lobby, long[] semillas, int i, List<String[]> marc) {
        return new ClienteOficina.Oyente() {
            @Override
            public void alLobby(List<String> nombres) {
                synchronized (lobby) {
                    lobby.clear();
                    lobby.addAll(nombres);
                }
            }

            @Override
            public void alIniciar(long semilla, boolean relajado) {
                semillas[i] = semilla;
            }

            @Override
            public void alMarcador(List<String[]> filas) {
                synchronized (marc) {
                    marc.clear();
                    marc.addAll(filas);
                }
            }

            @Override
            public void alError(String texto) {
            }
        };
    }

    private interface Condicion {
        boolean ok();
    }

    private static void esperar(Condicion c) throws InterruptedException {
        for (int i = 0; i < 60 && !c.ok(); i++) {
            Thread.sleep(50);
        }
    }

    /** estilo 0 = correcto, 1 = publicar todo, 2 = rechazar todo. */
    private static void jugarDia(Partida p, int estilo) {
        Jornada j = p.iniciarJornada();
        Caso c;
        while (!j.isTerminada() && (c = j.siguiente()) != null) {
            for (Herramienta h : Herramienta.values()) {
                j.usar(h);
            }
            Veredicto v = estilo == 0 ? c.correcto : estilo == 1 ? Veredicto.PUBLICAR : Veredicto.RECHAZAR;
            if (!j.sellos().contains(v)) {
                v = Veredicto.RECHAZAR;
            }
            j.sellar(v);
        }
        j.cerrar();
    }

    private static JugadorVerificador jugarTodo(int estilo) {
        Partida p = new Partida(Partida.Modo.SOLO, Arrays.asList("P" + estilo), 42, false);
        do {
            jugarDia(p, estilo);
        } while (p.siguienteTurno());
        JugadorVerificador j = p.jugadores.get(0);
        System.out.println("   estilo " + estilo + ": " + j.fila() + " · desinformación " + j.ciudad.desinformacion() + " %");
        return j;
    }

    private static void comprobar(boolean ok, String que) {
        System.out.println((ok ? "  [OK]    " : "  [FALLA] ") + que);
        if (!ok) {
            fallos++;
        }
    }
}
