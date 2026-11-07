package alcaldedigital.pruebas;

import alcaldedigital.logica.BancoPublicaciones;
import alcaldedigital.modelo.Cuenta;
import alcaldedigital.modelo.Publicacion;
import alcaldedigital.modelo.Senal;
import alcaldedigital.modelo.TipoContenido;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Banco de pruebas de las publicaciones con pistas.
 *
 * Verifica que el feed este bien construido ANTES de que lo vea un jugador:
 * que el reparto de falsas y ciertas sea el pensado, que ninguna clave del AVL
 * se repita, y sobre todo que las senales no sean una regla mecanica.
 *
 * Para ejecutarlo en NetBeans: clic derecho sobre este archivo -> Run File.
 */
public class PruebaPublicaciones {

    private static int fallos = 0;

    public static void main(String[] args) {
        List<Publicacion> feed = BancoPublicaciones.publicacionesConPistas();

        imprimirTitulo("CASO 1 - El feed completo");
        for (Publicacion p : feed) {
            Cuenta c = p.getCuenta();
            System.out.println("#" + p.getId() + "  " + (p.esFalsa() ? "FALSA   " : "CIERTA  ")
                    + ajustar(c.getArroba()) + "  " + c.antiguedadTexto()
                    + ", " + c.seguidoresTexto() + (c.estaVerificada() ? ", verificada" : ""));
            System.out.println("     " + recortar(p.getTexto()));
            System.out.println("     señales: " + (p.pareceLimpia() ? "(ninguna)" : p.getSenales()));
            System.out.println();
        }

        imprimirTitulo("CASO 2 - El reparto es el pensado");
        int falsas = 0, ciertas = 0;
        for (Publicacion p : feed) {
            if (p.esFalsa()) falsas++; else ciertas++;
        }
        comprobar("Hay 12 publicaciones", feed.size() == 12);
        comprobar("Hay 6 falsas", falsas == 6);
        comprobar("Hay 6 ciertas", ciertas == 6);

        imprimirTitulo("CASO 3 - Las claves del AVL no se repiten");
        Set<Integer> claves = new HashSet<>();
        Set<Integer> ids = new HashSet<>();
        boolean clavesUnicas = true, idsUnicos = true;
        for (Publicacion p : feed) {
            if (!claves.add(p.claveAVL())) clavesUnicas = false;
            if (!ids.add(p.getId())) idsUnicos = false;
        }
        comprobar("Todos los id son unicos", idsUnicos);
        comprobar("Todas las claves del AVL son unicas", clavesUnicas);

        imprimirTitulo("CASO 4 - Las senales NO son una regla mecanica");
        List<Publicacion> ciertasConSenal = new ArrayList<>();
        List<Publicacion> falsasSinSenalesFuertes = new ArrayList<>();
        for (Publicacion p : feed) {
            if (!p.esFalsa() && !p.pareceLimpia()) {
                ciertasConSenal.add(p);
            }
            if (p.esFalsa() && p.getCuenta().getDiasDeCreada() > 365) {
                falsasSinSenalesFuertes.add(p);
            }
        }
        System.out.println("Ciertas que SI tienen alguna señal:");
        for (Publicacion p : ciertasConSenal) {
            System.out.println("  #" + p.getId() + " " + p.getSenales() + " -> " + recortar(p.getTexto()));
        }
        System.out.println();
        System.out.println("Falsas publicadas desde cuentas VIEJAS:");
        for (Publicacion p : falsasSinSenalesFuertes) {
            System.out.println("  #" + p.getId() + " " + p.getCuenta().antiguedadTexto()
                    + " -> " + recortar(p.getTexto()));
        }
        System.out.println();
        comprobar("Hay al menos una CIERTA con señal (si no, 'señal = mentira')",
                !ciertasConSenal.isEmpty());
        comprobar("Hay al menos una FALSA desde cuenta vieja (si no, 'cuenta nueva = mentira')",
                !falsasSinSenalesFuertes.isEmpty());

        imprimirTitulo("CASO 5 - Toda publicacion se puede explicar");
        boolean todasExplicadas = true;
        for (Publicacion p : feed) {
            if (p.getExplicacion() == null || p.getExplicacion().isBlank()) {
                System.out.println("  SIN EXPLICACION: #" + p.getId());
                todasExplicadas = false;
            }
        }
        comprobar("Las 12 tienen explicacion para el informe final", todasExplicadas);

        imprimirTitulo("CASO 6 - La pareja que ensena la diferencia");
        Publicacion falsa = buscar(feed, 5);
        Publicacion cierta = buscar(feed, 9);
        System.out.println("FALSA  #5: " + recortar(falsa.getTexto()));
        System.out.println("           " + falsa.getCuenta().getArroba() + ", "
                + falsa.getCuenta().antiguedadTexto() + ", fuente: "
                + (falsa.getFuenteCitada() == null ? "ninguna" : falsa.getFuenteCitada()));
        System.out.println("CIERTA #9: " + recortar(cierta.getTexto()));
        System.out.println("           " + cierta.getCuenta().getArroba() + ", "
                + cierta.getCuenta().antiguedadTexto() + ", fuente: " + cierta.getFuenteCitada());
        System.out.println();
        comprobar("Las dos hablan de un corte de agua",
                falsa.getTexto().toUpperCase().contains("AGUA") && cierta.getTexto().contains("agua"));
        comprobar("Solo la cierta cita una fuente",
                falsa.getFuenteCitada() == null && cierta.getFuenteCitada() != null);

        imprimirTitulo("CASO 7 - El feed barajado respeta el banco");
        BancoPublicaciones banco = new BancoPublicaciones(42L);
        List<Publicacion> baraja = banco.generarFeedConPistas(9);
        System.out.println("Pidiendo 9 publicaciones con semilla 42:");
        for (Publicacion p : baraja) {
            System.out.println("  #" + p.getId() + " " + (p.esFalsa() ? "FALSA " : "CIERTA"));
        }
        comprobar("Devuelve exactamente 9", baraja.size() == 9);

        imprimirTitulo("RESULTADO");
        if (fallos == 0) {
            System.out.println("Las 7 comprobaciones pasaron. El banco esta listo.");
        } else {
            System.out.println("FALLARON " + fallos + " comprobaciones. Revisar arriba.");
        }
    }

    private static Publicacion buscar(List<Publicacion> feed, int id) {
        for (Publicacion p : feed) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    private static void comprobar(String descripcion, boolean condicion) {
        System.out.println((condicion ? "  OK      " : "  FALLO   ") + descripcion);
        if (!condicion) {
            fallos++;
        }
    }

    private static String recortar(String texto) {
        String limpio = texto.replace('\n', ' ');
        return (limpio.length() <= 72) ? limpio : limpio.substring(0, 69) + "...";
    }

    private static String ajustar(String texto) {
        StringBuilder sb = new StringBuilder(texto);
        while (sb.length() < 22) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static void imprimirTitulo(String titulo) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println(" " + titulo);
        System.out.println("============================================================");
    }
}
