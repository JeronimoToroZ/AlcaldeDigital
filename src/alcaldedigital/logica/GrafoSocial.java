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
    
    // Método para inicializar el mapa de prueba con nodos y posiciones (x, y)
    public void inicializarMapaPrueba() {
        NodoRed n1 = new NodoRed("Alcaldía", 450, 300);
        NodoRed n2 = new NodoRed("Plaza Central", 250, 180);
        NodoRed n3 = new NodoRed("Universidad", 650, 180);
        NodoRed n4 = new NodoRed("Estación", 450, 480);

        agregarNodo(n1);
        agregarNodo(n2);
        agregarNodo(n3);
        agregarNodo(n4);

        // Creamos las conexiones (Aristas del Grafo)
        n1.agregarConexion(n2);
        n1.agregarConexion(n3);
        n1.agregarConexion(n4);
        
        // Infectamos uno por defecto para iniciar la prueba visual
        n2.estaInfectado = true; 
    }

    // --- ALGORITMO DE PROPAGACIÓN (BFS / Búsqueda en Anchura) ---
    public void propagarRumorBFS() {
        List<NodoRed> nuevosInfectados = new ArrayList<>();
        
        // Recorremos los nodos para ver cuáles están infectados y pasar el rumor a sus vecinos
        for (NodoRed nodo : nodos) {
            if (nodo.estaInfectado) {
                for (NodoRed vecino : nodo.vecinos) {
                    if (!vecino.estaInfectado) {
                        nuevosInfectados.add(vecino);
                        break; // Propaga de a uno por turno para que sea visualmente claro
                    }
                }
            }
        }
        
        // Aplicamos la infección a los nuevos nodos encontrados por el grafo
        for (NodoRed n : nuevosInfectados) {
            n.estaInfectado = true;
        }
    }
}