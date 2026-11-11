# Influence, Inc.: Mesa de Verificación. Guía de sustentación

**Universidad del Norte, Estructura de Datos II, 2026.** Java puro, sin librerías externas.

> Cualquier integrante puede ser preguntado por cualquier parte. Este documento sigue los
> puntos del enunciado y dice en qué archivo está cada cosa. En el juego, **F2** abre el
> visor de estructuras: muestra los árboles y la cola en vivo (el reloj se detiene).

---

## 0. El juego en tres frases

Eres verificador en la ventanilla de una agencia durante la semana electoral de Ciudad Nova.
La gente trae publicaciones; tú conversas, investigas con herramientas y confrontas, y luego
sellas. Cada sello baja por un árbol de consecuencias y se propaga por dos grafos (la red
social y la ciudad). Al final, la ciudad vota según lo que dejaste circular.

---

## 1. Árboles (preguntas 1 a 5 del enunciado)

### 1.1 Árbol n-ario de diálogo: `estructuras/ArbolDialogo.java`, `NodoDialogo.java`
1. **Problema:** una conversación con ramas: cada pregunta abre otras.
2. **Por qué árbol:** la conversación es jerárquica: preguntar "¿de dónde lo sacó?" abre
   "¿su sobrino vio un documento?". Un nodo puede tener cualquier número de hijos.
3. **Operaciones:**
   - `pregunta()`: agrega hijos de la raíz.
   - `opciones()`: hijos no visitados del nodo actual; si no hay, vuelve a la raíz.
   - **`insertar()`:** cuando una herramienta encuentra una contradicción, `Jornada` *inserta*
     un nodo nuevo (la confrontación) como primer hijo de la raíz.
   - **`eliminar()`**.
   - `preorden()`, `altura()` y `cantidadNodos()`.
4. **En el juego:** los nodos insertados son las preguntas naranjas **¡CONFRONTAR!**. La
   segunda contradicción contra un impostor inserta un nodo con su confesión.
5. **Complejidad:** insertar es O(1) porque se tiene el padre; preorden es O(n).

### 1.2 Árbol de consecuencias: `estructuras/ArbolConsecuencias.java`
1. **Problema:** convertir (sello que pusiste × lo que el caso era en realidad) en efectos.
2. **Estructura:** raíz → 5 sellos → 5 verdades = 31 nodos, altura 2. Cada hoja guarda un
   `EfectoCiudad` (indicadores, puntos, dinero, reputación, acierto y si fue grave).
3. **`resolver(v, t)`** baja raíz → sello → verdad.
   - Los **sellos disponibles son los hijos de la raíz**: los botones del escritorio salen de ahí.
   - **Insertar:** el día 2 se inserta la rama #PUBLICIDAD (`JugadorVerificador.prepararDia`).
   - **Eliminar:** el día 5 la veda electoral elimina la rama OPINIÓN. La rama se guarda y se
     puede reinsertar en su posición original.
4. **Prueba de diseño:** hay exactamente un sello correcto por cada verdad (lo verifica
   `PruebaEstructuras`).

### 1.3 AVL del directorio oficial: `estructuras/ArbolAVLGenerico.java`
1. **Problema:** saber en O(log n) si una cuenta es oficial, con altas y bajas durante la partida.
2. **Clave:** el @arroba (String). Las 4 rotaciones mantienen |factor de equilibrio| ≤ 1.
3. **Operaciones en el juego:**
   - `buscar()`: el monitor muestra cuántas comparaciones hizo y dibuja el camino.
   - `conPrefijo()`: sugerencias del tipo "¿quisiste decir @acueducto_nova?". Así se delata
     `@acueducto_nova_oficial`.
   - `eliminar()`: el día 3 se elimina `@alcaldia_nova_2019` (borra un nodo con hijos y rebalancea).
   - `insertar()`: el día 5 se inserta `@registraduria_nova`.
4. **Por qué no una lista:** con n cuentas, la búsqueda baja de O(n) a O(log n), y el árbol
   sigue ordenado para el recorrido inorden.

### 1.4 Cola FIFO: la fila de la ventanilla (`Jornada`, `ArrayDeque`)
El primero que llega es el primero en pasar. F2 muestra la cola completa. Los que quedan en
la fila a las 18:00 dejan circular sus rumores.

---

## 2. Grafos

### 2.1 Grafo social dirigido y ponderado: `estructuras/GrafoRed.java`
- **Vértices:** 30 personas en 6 barrios, 7 cuentas falsas en anillo, 3 influencers, un medio
  y la agencia. Están en `CiudadEstado.construirRed()`.
- **Aristas:** A → B significa "lo que publica A le llega a B". Tienen probabilidad y costo.
- **Algoritmos que se usan:**
  - **BFS probabilístico** (`propagar`): al sellar PUBLICAR, OPINIÓN o #PUBLICIDAD, la
    publicación sale desde la agencia por oleadas. Si era un bulo, quien la recibe queda
    desinformado, y la desinformación (%) se **calcula del grafo**.
  - **BFS por niveles** (`bfsOleadas`): el escáner de red muestra las 2 primeras oleadas
    desde el origen y qué porcentaje son bots.
  - **DFS** (`dfsAlcanzables`): detecta que el origen pertenece a un anillo de cuentas falsas.
  - **Eliminar vértice:** al DENUNCIAR a un impostor, su cuenta falsa se elimina del grafo
    junto con sus aristas, y un desmentido (BFS con tope de 1 salto, `corregir`) corrige a
    sus seguidores.
  - **Dijkstra** (`caminoMinimo`) y comunidades (DFS) están probados en `PruebaEstructuras`.

### 2.2 Grafo de la ciudad (no dirigido, con pesos = distancia): `estructuras/GrafoCiudad.java`
- **Dijkstra:** el corresponsal sale de tu oficina y va al barrio del caso por la ruta más
  corta. El tiempo de llegada depende del largo de la ruta, y la moto se ve avanzar por el mapa.
- **BFS del pánico:** si publicas un bulo sobre un barrio, el pánico empieza ahí y cada hora
  de juego salta a los barrios vecinos (`propagarPanico`). Eso baja la convivencia y el bienestar.
  Un acierto sobre ese barrio lo calma.

---

## 3. Azar y habilidad (decisiones con tiempo limitado)
- **Azar:**
  - Eventos del día: apagón (sin herramientas), café (pausa el reloj), viral (+desinformación)
    y bus (el reloj corre más rápido).
  - Las caras de cada partida.
  - **Qué rasgo** cambia en la foto del impostor (gafas, lunar, ojos, pelo o cejas).
  - La propagación probabilística.
  - Las noches de rumores entre días.
- **Habilidad:**
  - Elegir qué preguntar y qué herramienta usar, porque **todo gasta minutos**.
  - Encontrar la diferencia en la lupa.
  - Confrontar a tiempo y sellar bien.
- **Tiempo limitado:** el reloj va de 9:00 a 18:00, lo que equivale a 6 minutos reales por día.
  El bono de rapidez es +30 si aciertas en menos de 25 minutos de juego. El **modo relajado**
  quita el reloj, por inclusión.

## 4. Indicadores, puntuación y elección de alcalde
- **Indicadores:** verificación, confianza, convivencia, bienestar, desinformación (del grafo)
  y conflictos. Están en el televisor.
- **Puntos:**
  - Cada hoja del árbol de consecuencias da puntos.
  - Bonos: +30 por rapidez y +40 si sellas después de que el impostor confiesa.
  - −40 por cada caso sin atender.
- **Puntaje final:** `puntos + 10·salud de la ciudad + 5·reputación − 100·advertencias`.
- **Elección de alcalde** (`Historia.eleccion`): la participación sale de la confianza, el
  bienestar y la desinformación, y la elección "la deciden las propuestas", "los rumores" o
  "un video falso" según cómo jugaste.
- **Final** (`Historia.finalPara`): depende del % de aciertos, la desinformación, la
  confianza y lo que hiciste con el deepfake del jefe.
- **Balance comprobado** con `PruebaJuego`:

  | Estilo de juego | Final |
  |---|---|
  | Jugar bien | El sello valiente |
  | Publicar todo | Despedido |
  | Rechazar todo, por miedo | Ciudad dividida (rechazar lo cierto también hace daño) |

## 5. Comunicación entre jugadores
- **Duelo local:** mismo computador, por turnos, con los mismos casos. Cada jugador tiene su
  propia ciudad, su propio AVL y su propio árbol de consecuencias (`Partida`).
- **En red: cliente-servidor con sockets TCP** (`red/`), puerto 5077, una línea de texto por mensaje:

```
cliente                                  servidor
HOLA Ana                    --->
                            <---         LOBBY Ana,Luis
                            <---         INICIO <semilla>|<relajado>      (todos la misma semilla)
RES Ana|450|2|6|0|0         --->         (puntos|día|aciertos|advertencias|terminó)
                            <---         MARCADOR Ana:450:2:6:0:0;Luis:...
```

Como todos tienen la misma semilla, todos ven los mismos casos y los mismos eventos. El
servidor solo recibe resultados y reparte el marcador, que aparece en vivo en la barra
superior. Los nombres repetidos se renombran ("Ana 2") y no se puede entrar a una partida ya
empezada. Lo prueba `PruebaJuego` con un servidor y dos clientes reales.

## 6. Gráficos sin librerías
- `gui/Caras.java` dibuja personas con Java2D a partir de `Rasgos`: piel, forma, pelo, ojos,
  cejas, barba, gafas, lunar, ropa y accesorio.
  - Tiene 6 expresiones, parpadeo y boca que habla.
  - Los impostores "fallan" con un glitch.
- La foto de la credencial usa el mismo código. Por eso, si cambia un rasgo, se nota.
- `motor3d/`: cámara en perspectiva y algoritmo del pintor. `gui/VistaRed3D` dibuja ahí el
  grafo social girando.

## 7. Inclusión, ayuda y motivación
- **Inclusión:**
  - Alto contraste y texto grande.
  - **Subtítulos de cada sonido**.
  - Modo relajado sin reloj.
  - **Todo se juega con teclado o con mouse**.
  - Nada depende solo del color: las pistas llevan icono (triángulo, ✓, X) y los indicadores
    llevan ▲▼.
- **Ayuda:**
  - Tutorial del día 1.
  - **Manual** (M) que suma reglas cada día.
  - Descripción de cada herramienta en el monitor.
  - **Visor de estructuras** (F2).
  - Explicación de cada error al sellar.
- **Motivación:**
  - Historia con giro final.
  - Logros: Ojo de halcón, Incorruptible, Sin precio, Detective, Rayo, Día perfecto.
  - Titulares de mañana.
  - Cinco finales.
  - Duelo y marcador en red.

## 8. ¿Qué comportamiento responsable fomenta?
Preguntar "¿y la fuente?" antes de compartir, desconfiar de lo urgente y de las cuentas
recién creadas, distinguir opinión de hecho y publicidad de opinión, y entender que una
foto real puede ser vieja. También que **rechazar lo cierto también hace daño** y que a veces
hay que decir que no aunque la presión venga de arriba (el caso del jefe).
