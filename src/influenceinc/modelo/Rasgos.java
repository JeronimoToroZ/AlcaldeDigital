package influenceinc.modelo;

import java.util.Random;

/**
 * Como se ve una persona. La cara del visitante y la foto de su credencial
 * se dibujan con estos rasgos: si el visitante es un impostor, la foto tiene
 * UN rasgo distinto y hay que encontrarlo con la lupa.
 */
public class Rasgos {

    public int piel, forma, pelo, colorPelo, ojos, barba, cejas, ropa, accesorio, edad;
    public boolean gafas, lunar;

    public static Rasgos azar(Random r) {
        Rasgos x = new Rasgos();
        x.piel = r.nextInt(5);
        x.forma = r.nextInt(3);
        x.pelo = r.nextInt(6);
        x.colorPelo = r.nextInt(5);
        x.ojos = r.nextInt(4);
        x.barba = r.nextInt(4) == 0 ? 1 + r.nextInt(2) : 0;
        x.cejas = r.nextInt(2);
        x.ropa = r.nextInt(7);
        x.accesorio = r.nextInt(4);
        x.edad = r.nextInt(3);
        x.gafas = r.nextInt(3) == 0;
        x.lunar = r.nextInt(4) == 0;
        return x;
    }

    public Rasgos copia() {
        Rasgos x = new Rasgos();
        x.piel = piel;
        x.forma = forma;
        x.pelo = pelo;
        x.colorPelo = colorPelo;
        x.ojos = ojos;
        x.barba = barba;
        x.cejas = cejas;
        x.ropa = ropa;
        x.accesorio = accesorio;
        x.edad = edad;
        x.gafas = gafas;
        x.lunar = lunar;
        return x;
    }

    /**
     * Copia con UN rasgo cambiado (la foto falsa del impostor).
     * @return la copia; en diferencia[0] queda la descripcion de que cambio
     */
    public Rasgos mutar(Random r, String[] diferencia) {
        Rasgos x = copia();
        switch (r.nextInt(5)) {
            case 0:
                x.gafas = !gafas;
                diferencia[0] = gafas ? "En la foto no tiene gafas" : "En la foto tiene gafas";
                break;
            case 1:
                x.lunar = !lunar;
                diferencia[0] = lunar ? "En la foto no tiene el lunar" : "En la foto tiene un lunar";
                break;
            case 2:
                x.ojos = (ojos + 1 + r.nextInt(3)) % 4;
                diferencia[0] = "El color de ojos de la foto es otro";
                break;
            case 3:
                x.colorPelo = (colorPelo + 1 + r.nextInt(4)) % 5;
                diferencia[0] = "El color de pelo de la foto es otro";
                break;
            default:
                x.cejas = 1 - cejas;
                diferencia[0] = "Las cejas de la foto son distintas";
                break;
        }
        return x;
    }
}
