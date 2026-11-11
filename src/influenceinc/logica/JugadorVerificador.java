package influenceinc.logica;

import influenceinc.estructuras.ArbolAVLGenerico;
import influenceinc.estructuras.ArbolConsecuencias;
import influenceinc.modelo.CuentaOficial;
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
    /** Bitacora de decisiones (para el informe final). */
    public final List<String> bitacora = new ArrayList<>();

    public JugadorVerificador(String nombre, long semilla) {
        this.nombre = nombre;
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

    public String fila() {
        return nombre + "  ·  " + puntos + " pts  ·  $" + dinero + "  ·  " + advertencias + "/" + MAX_ADVERTENCIAS
                + " advertencias";
    }
}
