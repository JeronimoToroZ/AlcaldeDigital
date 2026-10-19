/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package alcaldedigital.logica;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nplopez
 */
public class NodoRed {
    public String nombre;
    public int x, y; // Coordenadas en píxeles dentro de la pantalla para pintar el círculo
    public boolean estaInfectado = false; // true si el rumor falso llegó aquí
    public List<NodoRed> vecinos; // Las aristas que lo conectan con otros nodos

    public NodoRed(String nombre, int x, int y) {
        this.nombre = nombre;
        this.x = x;
        this.y = y;
        this.vecinos = new ArrayList<>();
    }

    // Método para conectar este nodo con otro (crear la arista del grafo)
    public void agregarConexion(NodoRed vecino) {
        this.vecinos.add(vecino);
        vecino.vecinos.add(this); // Conexión bidireccional (grafo no dirigido)
    }
}

