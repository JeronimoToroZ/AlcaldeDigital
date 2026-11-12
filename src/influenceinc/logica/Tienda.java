package influenceinc.logica;

import influenceinc.modelo.Herramienta;

/**
 * TIENDA: el dinero que el jugador gana (sueldo, casos, campanas) se puede
 * gastar en mejoras, suministros y objetos utiles. Cada compra tiene un
 * efecto REAL en la partida (reloj, pistas, reputacion o la ciudad).
 *
 * La recompensa por trabajar bien (dinero) se convierte asi en ventaja para
 * seguir trabajando bien: es uno de los elementos motivadores del juego.
 */
public final class Tienda {

    public enum Articulo {
        CAFE("Café cargado", "Suministro", "Detiene el reloj 20 segundos para pensar con calma.", 60),
        HORAS_EXTRA("Horas extra", "Suministro", "Le devuelve 30 minutos al reloj del día.", 120),
        INFORMANTE("Pista del informante", "Suministro", "Un informante te da una pista sobre el caso actual.", 150),
        RECARGA("Recarga de habilidad", "Mejora", "Vuelve a usar tu habilidad única con el cliente actual.", 120),
        LUPA_PRO("Lupa de alta precisión", "Mejora permanente", "En la lupa se marca la zona sospechosa de la foto.", 180),
        SEGURO("Seguro de reputación", "Protección", "Anula tu próxima advertencia por un error grave.", 220),
        CAMPANA("Campaña \"Piensa antes de compartir\"", "Para la ciudad", "Baja la desinformación y mejora la convivencia digital.", 250);

        public final String nombre, tipo, descripcion;
        public final int precio;

        Articulo(String nombre, String tipo, String descripcion, int precio) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.precio = precio;
        }
    }

    private Tienda() {
    }

    /** null si se puede comprar; si no, el motivo. */
    public static String motivoNoCompra(Articulo a, Jornada j) {
        JugadorVerificador jv = j.jugador;
        if (jv.dinero < a.precio) {
            return "Te faltan $" + (a.precio - jv.dinero) + " para comprar " + a.nombre + ".";
        }
        switch (a) {
            case INFORMANTE:
                return j.getActual() == null ? "Primero haz pasar a alguien a la ventanilla." : null;
            case RECARGA:
                if (!jv.rol.tieneMiniJuego()) {
                    return "Como periodista no tienes mini-juego: tu habilidad es el monitor completo.";
                }
                if (j.getActual() == null) {
                    return "Primero haz pasar a alguien a la ventanilla.";
                }
                return j.getActual().habilidadUsada ? null : "Todavía no has usado tu habilidad con este cliente.";
            case LUPA_PRO:
                if (jv.lupaPrecision) {
                    return "Ya tienes la lupa de alta precisión.";
                }
                return jv.rol.puedeUsar(Herramienta.LUPA) ? null : "Tu rol no usa la lupa.";
            case HORAS_EXTRA:
                return j.relajado ? "En modo relajado el reloj no corre." : null;
            case CAFE:
                return j.relajado ? "En modo relajado el reloj no corre." : null;
            default:
                return null;
        }
    }

    /**
     * Compra el articulo y aplica su efecto.
     * @return el mensaje de lo que paso (empieza con "[x] " si no se pudo)
     */
    public static String comprar(Articulo a, Jornada j) {
        String no = motivoNoCompra(a, j);
        if (no != null) {
            return "[x] " + no;
        }
        JugadorVerificador jv = j.jugador;
        jv.dinero -= a.precio;
        jv.gastado += a.precio;
        jv.compras.add(a.nombre);
        String msg;
        switch (a) {
            case CAFE:
                j.pausar(20);
                msg = "El reloj se detiene 20 segundos. ¡A pensar con calma!";
                break;
            case HORAS_EXTRA:
                j.devolverMinutos(30);
                msg = "Ganaste 30 minutos más de jornada.";
                break;
            case INFORMANTE:
                msg = j.pistaInformante().replaceFirst("^\\[[^\\]]*\\]\\s*", "") + " (anotado en la libreta)";
                break;
            case RECARGA:
                j.recargarHabilidad();
                msg = "Tu habilidad está lista otra vez para este cliente.";
                break;
            case LUPA_PRO:
                jv.lupaPrecision = true;
                msg = "Desde ahora la lupa marca la zona sospechosa de la foto.";
                break;
            case SEGURO:
                jv.seguros++;
                msg = "Tu próxima advertencia quedará anulada (seguros: " + jv.seguros + ").";
                break;
            default:
                CiudadEstado c = jv.ciudad;
                c.ajustarDesinformacion(-6);
                c.convivencia = CiudadEstado.lim(c.convivencia + 3);
                c.bienestar = CiudadEstado.lim(c.bienestar + 2);
                jv.reputacion = CiudadEstado.lim(jv.reputacion + 2);
                msg = "La campaña llegó a los colegios y barrios: baja la desinformación y sube la convivencia.";
                break;
        }
        if (jv.compras.size() == 3) {
            jv.logros.add("Buen inversionista: 3 compras en la tienda");
        }
        return msg;
    }
}
