package influenceinc.logica;

import influenceinc.estructuras.ArbolAVLGenerico;
import influenceinc.estructuras.ArbolConsecuencias;
import influenceinc.modelo.CuentaOficial;
import influenceinc.modelo.RolJugador;
import influenceinc.modelo.Veredicto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Un verificador (jugador). Cada jugador tiene SU ciudad, SU directorio (AVL)
 * y SU arbol de consecuencias: en el duelo, lo que hace uno no afecta al otro.
 */
public class JugadorVerificador {

    public static final int MAX_ADVERTENCIAS = 4;

    public final String nombre;
    /** El rol que escogio al empezar: define sus herramientas y su habilidad unica. */
    public final RolJugador rol;
    public int puntos;
    public int dinero = 100;
    public int reputacion = 50;
    public int advertencias;
    public final CiudadEstado ciudad;
    public final ArbolAVLGenerico<String, CuentaOficial> directorio = new ArbolAVLGenerico<>();
    public final ArbolConsecuencias arbol = new ArbolConsecuencias();

    public int aciertos, errores, impostoresAtrapados, casosAtendidos;
    public boolean denuncioJefe, aproboDeepfake, despedido;
    public final Set<String> logros = new LinkedHashSet<>();
    /** Habilidad unica: veces usada y veces bien resuelta. */
    public int habilidadesUsadas, habilidadesBien;
    /** Alertas virales: bien resueltas (verificar / reportar) y mal resueltas (compartir). */
    public int viralesBien, viralesMal;
    /** Apoyo que el candidato gano en sus ruedas de prensa (puntos de votacion). */
    public int apoyo;
    /** TIENDA: seguros de reputacion (cada uno anula una advertencia). */
    public int seguros;
    /** TIENDA: lupa de alta precision comprada (senala la zona sospechosa). */
    public boolean lupaPrecision;
    /** TIENDA: lo que compro (para el informe). */
    public final List<String> compras = new ArrayList<>();
    public int gastado;
    /** Bitacora de decisiones (para el informe final). */
    public final List<String> bitacora = new ArrayList<>();

    public JugadorVerificador(String nombre, long semilla) {
        this(nombre, RolJugador.PERIODISTA, semilla);
    }

    public JugadorVerificador(String nombre, RolJugador rol, long semilla) {
        this.nombre = nombre;
        this.rol = rol == null ? RolJugador.PERIODISTA : rol;
        this.ciudad = new CiudadEstado(semilla);
        for (CuentaOficial c : Historia.directorioInicial()) {
            directorio.insertar(c.arroba, c);
        }
        // el dia 1 todavia no existe el sello #PUBLICIDAD: se inserta el dia 2
        arbol.eliminar(Veredicto.ETIQUETA);
    }

    /** Los cambios de reglas de cada manana: modifican los arboles (insertar / eliminar). */
    public List<String> prepararDia(int dia) {
        List<String> cambios = new ArrayList<>();
        if (dia > 1) {
            ciudad.nocheDeRumores();
        }
        switch (dia) {
            case 2:
                arbol.insertar(Veredicto.ETIQUETA);
                cambios.add("Árbol de consecuencias: se INSERTÓ la rama #PUBLICIDAD.");
                break;
            case 3:
                directorio.eliminar("@alcaldia_nova_2019");
                cambios.add("Directorio (AVL): se ELIMINÓ @alcaldia_nova_2019.");
                break;
            case 5:
                arbol.eliminar(Veredicto.OPINION);
                cambios.add("Veda electoral: se ELIMINÓ la rama OPINIÓN del árbol de consecuencias.");
                CuentaOficial r = Historia.registraduria();
                directorio.insertar(r.arroba, r);
                cambios.add("Directorio (AVL): se INSERTÓ @registraduria_nova.");
                break;
            default:
                break;
        }
        return cambios;
    }

    public void advertir() {
        advertencias++;
        if (advertencias >= MAX_ADVERTENCIAS) {
            despedido = true;
        }
    }

    /** Puntaje final: puntos + ciudad + reputacion. */
    public int puntajeFinal() {
        return puntos + ciudad.salud() * 10 + reputacion * 5 - advertencias * 100;
    }

    /**
     * Intencion de voto (0..100) para un jugador CANDIDATO: sale de como quedo
     * SU ciudad (confianza, informacion verificada, conflictos, desinformacion),
     * de su reputacion y del apoyo que gano respondiendo con responsabilidad.
     */
    public int apoyoElectoral() {
        double v = 18 + ciudad.confianza * 0.30 + ciudad.verificada * 0.18 + reputacion * 0.12
                - ciudad.conflictos * 0.15 - ciudad.desinformacion() * 0.20 + apoyo * 1.2 - advertencias * 4;
        return Math.max(3, Math.min(97, (int) Math.round(v)));
    }

    public String fila() {
        return nombre + "  ·  " + puntos + " pts  ·  $" + dinero + "  ·  " + advertencias + "/" + MAX_ADVERTENCIAS
                + " advertencias";
    }
}
