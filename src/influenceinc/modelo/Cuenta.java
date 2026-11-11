package influenceinc.modelo;

/**
 * La cuenta que publica algo en el feed de Ciudad Nova.
 *
 * Existe para que el jugador pueda MIRAR quien publica antes de creerle. Todo lo
 * que guarda es lo que se ve en una red social de verdad: el nombre, la arroba,
 * hace cuanto existe la cuenta, cuantos seguidores tiene y si esta verificada.
 *
 * Ninguno de esos datos prueba nada por si solo. Juntos, dicen bastante.
 */
public class Cuenta {

    private final String nombre;
    private final String arroba;
    private final int diasDeCreada;
    private final int seguidores;
    private final boolean verificada;
    private final String avatar;   // nombre del archivo en recursos, puede ser null

    public Cuenta(String nombre, String arroba, int diasDeCreada,
                  int seguidores, boolean verificada, String avatar) {
        this.nombre = nombre;
        this.arroba = arroba;
        this.diasDeCreada = Math.max(0, diasDeCreada);
        this.seguidores = Math.max(0, seguidores);
        this.verificada = verificada;
        this.avatar = avatar;
    }

    public String getNombre() {
        return nombre;
    }

    public String getArroba() {
        return arroba;
    }

    public int getDiasDeCreada() {
        return diasDeCreada;
    }

    public int getSeguidores() {
        return seguidores;
    }

    public boolean estaVerificada() {
        return verificada;
    }

    public String getAvatar() {
        return avatar;
    }

    /** "hace 3 días", "hace 2 meses", "hace 4 años" — como lo muestra una red social. */
    public String antiguedadTexto() {
        if (diasDeCreada < 1) {
            return "creada hoy";
        }
        if (diasDeCreada == 1) {
            return "creada ayer";
        }
        if (diasDeCreada < 30) {
            return "creada hace " + diasDeCreada + " días";
        }
        if (diasDeCreada < 365) {
            int meses = diasDeCreada / 30;
            return "creada hace " + meses + (meses == 1 ? " mes" : " meses");
        }
        int anios = diasDeCreada / 365;
        return "creada hace " + anios + (anios == 1 ? " año" : " años");
    }

    /** "8.4 mil seguidores" — abreviado, como en las redes. */
    public String seguidoresTexto() {
        if (seguidores < 1000) {
            return seguidores + (seguidores == 1 ? " seguidor" : " seguidores");
        }
        double miles = seguidores / 1000.0;
        String numero = (miles == Math.floor(miles))
                ? String.valueOf((int) miles)
                : String.format("%.1f", miles).replace(',', '.');
        return numero + " mil seguidores";
    }

    /** true si la cuenta tiene menos de dos semanas. Es una SENAL, no una prueba. */
    public boolean esReciente() {
        return diasDeCreada < 15;
    }

    /** true si tiene muy pocos seguidores para el alcance de lo que publica. */
    public boolean tienePocosSeguidores() {
        return seguidores < 100;
    }

    @Override
    public String toString() {
        return arroba;
    }
}
