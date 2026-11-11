package influenceinc.modelo;

/** Carne que entrega el visitante. */
public class Credencial {

    public final String nombre, cargo, institucion, numero, vence;
    public final Rasgos foto;

    public Credencial(String nombre, String cargo, String institucion, String numero, String vence, Rasgos foto) {
        this.nombre = nombre;
        this.cargo = cargo;
        this.institucion = institucion;
        this.numero = numero;
        this.vence = vence;
        this.foto = foto;
    }
}
