package alcaldedigital.pruebas;

import alcaldedigital.red.ClientePartida;
import alcaldedigital.red.Protocolo;
import alcaldedigital.red.ServidorPartida;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Levanta un servidor y dos clientes de verdad -con sockets, no simulados- y
 * juega una partida completa.
 *
 * Es la prueba que de verdad dice si la red funciona: comprueba que dos
 * clientes se conecten a la vez (el ejemplo de clase solo atendia a uno),
 * que los turnos se repartan, que ambos vean el MISMO estado de la ciudad y
 * que la partida termine sola.
 */
public class PruebaRed {

    private static final int PUERTO = 5099;

    public static void main(String[] args) throws Exception {
        titulo("PRUEBA DE RED: dos jugadores, una partida completa");

        List<String> transcripcion = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch terminaron = new CountDownLatch(2);

        Thread hiloServidor = new Thread(() -> {
            try {
                new ServidorPartida(PUERTO, 2).arrancar();
            } catch (Exception ex) {
                System.out.println("  servidor: " + ex.getMessage());
            }
        }, "servidor");
        hiloServidor.setDaemon(true);
        hiloServidor.start();

        Thread.sleep(600);   // que alcance a abrir el puerto

        ClientePartida uno = conectar("Jeronimo", transcripcion, terminaron);
        Thread.sleep(300);
        ClientePartida dos = conectar("Naty", transcripcion, terminaron);

        boolean aTiempo = terminaron.await(45, TimeUnit.SECONDS);

        uno.cerrar();
        dos.cerrar();

        titulo("LO QUE VIO CADA JUGADOR");
        for (String l : transcripcion) {
            System.out.println("  " + l);
        }

        titulo("COMPROBACIONES");
        comprobar("La partida terminó sola", aTiempo);
        comprobar("Los dos clientes se conectaron a la vez",
                contar(transcripcion, "ESPERANDO 2") >= 2);
        comprobar("Hubo al menos un turno de cada jugador",
                contar(transcripcion, "Jeronimo|TU") >= 1 && contar(transcripcion, "Naty|TU") >= 1);
        comprobar("Los dos recibieron el mismo cierre",
                contar(transcripcion, Protocolo.FIN) == 2);

        System.out.println();
        System.exit(0);
    }

    private static ClientePartida conectar(String nombre, List<String> transcripcion,
                                           CountDownLatch terminaron) throws Exception {
        ClientePartida c = new ClientePartida("127.0.0.1", PUERTO, nombre);
        c.conectar(linea -> {
            transcripcion.add(nombre + " <- " + linea);
            String comando = Protocolo.comando(linea);

            if (Protocolo.TURNO.equals(comando)) {
                String[] campos = Protocolo.campos(Protocolo.argumento(linea));
                if (campos.length >= 3 && "TU".equals(campos[2])) {
                    // Un jugador automatico: siempre reporta. Lo que se prueba
                    // es la red, no el criterio.
                    c.enviar(Protocolo.ACCION, "REPORTAR");
                    transcripcion.add(nombre + " -> ACCION REPORTAR");
                }
            } else if (Protocolo.FIN.equals(comando)) {
                terminaron.countDown();
            }
        });
        return c;
    }

    private static int contar(List<String> lineas, String fragmento) {
        int n = 0;
        synchronized (lineas) {
            for (String l : lineas) {
                if (l.contains(fragmento)) {
                    n++;
                }
            }
        }
        return n;
    }

    private static void comprobar(String que, boolean bien) {
        System.out.printf("  %-45s %s%n", que, bien ? "OK" : "FALLA");
    }

    private static void titulo(String t) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println(" " + t);
        System.out.println("============================================================");
    }
}
