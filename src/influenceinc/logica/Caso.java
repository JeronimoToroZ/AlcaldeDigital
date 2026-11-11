package influenceinc.logica;

import influenceinc.estructuras.ArbolDialogo;
import influenceinc.estructuras.Vertice;
import influenceinc.modelo.Herramienta;
import influenceinc.modelo.Persona;
import influenceinc.modelo.Publicacion;
import influenceinc.modelo.Veredicto;
import influenceinc.modelo.Verdad;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Un caso de la ventanilla: quien viene, que trae, que pide y lo que en
 * realidad es. Las pistas de cada herramienta salen de estos datos.
 */
public class Caso {

    public final int dia;
    public final Persona visitante;
    public final Publicacion pub;
    public final Verdad verdad;
    public final Veredicto correcto;
    public final ArbolDialogo dialogo;

    /** Lo que pide al llegar. */
    public String peticion = "Quiero que la agencia publique esto.";
    /** Zona de la ciudad de la que habla (para el corresponsal y el panico). */
    public int zona = 0;
    /** Resultado de la busqueda inversa (null = no trae imagen). */
    public String imagen;
    /** Lo que encuentra el corresponsal en la calle. */
    public String reporteCalle = "No encontré nada raro: la calle está tranquila.";
    /** Lo que responde la institucion si la llamas (null = no hay a quien llamar). */
    public String llamada;
    /** % de publicaciones de la cuenta que ya fueron marcadas como falsas. */
    public int marcadas;
    /** De que tipo de cuenta sale en el grafo social: BOT, USUARIO, MEDIO, INFLUENCER. */
    public String origenTipo = "USUARIO";
    /** Lo que dice el impostor cuando lo confrontas. */
    public String confesion = "...Bueno, me pagaron por traer esto. No sé más.";
    /** Lo que dice alguien honesto cuando lo confrontas. */
    public String defensa = "No sé qué decirle. Yo solo traje lo que me llegó.";
    public String titularBien = "";
    public String titularMal = "";
    /** Plata que ofrece el cliente (campanas o sobornos). */
    public int pago;
    public boolean esJefe;

    // ---------- estado durante la partida ----------
    public Vertice origen;
    public final Map<Herramienta, List<String>> resultados = new EnumMap<>(Herramienta.class);
    public boolean confrontado;
    public Veredicto decidido;

    public Caso(int dia, Persona visitante, Publicacion pub, Verdad verdad, Veredicto correcto, ArbolDialogo dialogo) {
        this.dia = dia;
        this.visitante = visitante;
        this.pub = pub;
        this.verdad = verdad;
        this.correcto = correcto;
        this.dialogo = dialogo;
    }

    /** true si hay algo raro que las herramientas pueden descubrir. */
    public boolean sospechoso() {
        return verdad == Verdad.IMPOSTOR || verdad == Verdad.FALSA;
    }
}
