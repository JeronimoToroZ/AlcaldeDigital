/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package alcaldedigital.logica;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Naty
 */
public class GrafoSocial {
    public List<NodoRed> nodos;

    public GrafoSocial() {
        this.nodos = new ArrayList<>();
    }

    public void agregarNodo(NodoRed nodo) {
        nodos.add(nodo);
    }
    
    // Coordenadas ajustadas exactamente sobre los nombres escritos en el mapa de la ciudad
    public void inicializarMapaPrueba() {
        NodoRed parque = new NodoRed("Parque", 230, 215);
        NodoRed hospital = new NodoRed("Hospital", 460, 160);
        NodoRed biblioteca = new NodoRed("Biblioteca", 655, 220);
        NodoRed estacion = new NodoRed("Estación", 250, 495);
        NodoRed alcaldia = new NodoRed("Alcaldía", 515, 550);
        NodoRed teatro = new NodoRed("Teatro", 770, 490);
        NodoRed mercado = new NodoRed("Mercado", 360, 750);
        NodoRed cafe = new NodoRed("Café", 705, 745);

        agregarNodo(parque);
        agregarNodo(hospital);
        agregarNodo(biblioteca);
        agregarNodo(estacion);
        agregarNodo(alcaldia);
        agregarNodo(teatro);
        agregarNodo(mercado);
        agregarNodo(cafe);

        // Conexiones del grafo
        alcaldia.agregarConexion(hospital);
        alcaldia.agregarConexion(estacion);
        alcaldia.agregarConexion(teatro);
        parque.agregarConexion(estacion);
        biblioteca.agregarConexion(teatro);
        hospital.agregarConexion(biblioteca);
        estacion.agregarConexion(mercado);
        teatro.agregarConexion(cafe);
        
        // Nodo infectado por defecto para probar el BFS
        parque.estaInfectado = true; 
    }

    public void propagarRumorBFS() {
        List<NodoRed> nuevosInfectados = new ArrayList<>();
        for (NodoRed nodo : nodos) {
            if (nodo.estaInfectado) {
                for (NodoRed vecino : nodo.vecinos) {
                    if (!vecino.estaInfectado) {
                        nuevosInfectados.add(vecino);
                        break; 
                    }
                }
            }
        }
        for (NodoRed n : nuevosInfectados) {
            n.estaInfectado = true;
        }
    }
}