# Alcalde Digital — Documento de sustentación (Primera entrega)

**Universidad del Norte — Estructura de Datos II**
Entrega 1: árboles, operaciones, recorridos e interfaz gráfica preliminar.
Lenguaje: Java (sin librerías externas, solo Swing y AWT de la JDK).

---

## 0. Resumen en una frase

El juego usa **dos árboles con roles distintos**: un **AVL** que mantiene el feed de
Civitas ordenado por credibilidad, y un **árbol n-ario de decisión** que traduce la
acción del jugador en consecuencias concretas sobre la ciudad. Ninguno está puesto
para cumplir el requisito: si se quitan, el juego deja de funcionar.

---

## 1. ¿Qué problema resuelve cada árbol?

### 1.1 Árbol AVL — `estructuras/ArbolAVL.java`

El feed de publicaciones crece durante toda la partida. En cada turno el juego
necesita responder tres preguntas:

| Pregunta del juego | Operación | Costo |
|---|---|---|
| ¿Existe esta publicación y en qué estado está? | `buscar(clave)` | O(log n) |
| ¿Cuáles son las publicaciones menos creíbles del feed? | `menosCreiblesQue(35)` | O(log n + k) |
| Retirar del feed una publicación reportada | `eliminar(clave)` | O(log n) |

La segunda es la **habilidad del rol Periodista** dentro del juego: ver lo más
sospechoso sin recorrer todo el feed. Con una lista simple esas tres operaciones
costarían O(n) cada una.

### 1.2 Árbol de decisión n-ario — `estructuras/ArbolDecision.java`

Las consecuencias de una decisión dependen de **dos** cosas: qué hizo el jugador
(4 opciones) y qué era la publicación en realidad (verdadera, falsa u opinión: 3
opciones). Son **12 combinaciones distintas**, y el jugador no conoce la segunda
cuando decide.

Meter eso en un `switch` anidado daría código imposible de balancear. El árbol
convierte las reglas en **datos**: la lógica del juego solo desciende dos niveles y
aplica la hoja que encuentra. Para ajustar la dificultad se editan los efectos de
las hojas, no la lógica.

---

## 2. ¿Por qué se escogió esa estructura?

### 2.1 ¿Por qué AVL y no un ABB simple?

**Este es el argumento central de la entrega.** Las publicaciones no llegan en
orden aleatorio: los eventos del juego generan rachas (varias noticias falsas
seguidas, es decir, credibilidades bajas consecutivas). Con entrada ordenada un ABB
degenera en una lista enlazada y la altura pasa a ser O(n).

Demostración ejecutable, en `PruebaAVL`, caso 4 — insertando 15 elementos en orden
creciente:

```
Elementos: 15
Altura del AVL: 3   (un ABB simple habría quedado con altura 14, o sea una lista)
Cota teórica log2(15) = 3.91
Rotaciones aplicadas: 11
Invariante AVL: se cumple
```

El AVL garantiza altura O(log n) **siempre**, sin importar el orden de inserción.

### 2.2 ¿Por qué n-ario y no binario para las decisiones?

El grado de cada nodo es el número real de opciones: 4 en el primer nivel, 3 en el
segundo. Forzarlo a binario obligaría a inventar decisiones intermedias que no
existen en el juego ("¿es compartir o no es compartir?"), añadiendo profundidad sin
significado.

---

## 3. ¿Qué variante de árbol se utiliza?

| | Árbol 1 | Árbol 2 |
|---|---|---|
| Variante | **AVL** (ABB autobalanceado por altura) | **N-ario** de decisión, estático |
| Criterio de orden | Clave compuesta por credibilidad | Etiquetas: `Accion` y `TipoContenido` |
| Nodos | Dinámicos (crecen y se eliminan) | 17 fijos (1 raíz + 4 internos + 12 hojas) |
| Invariante | \|factor de equilibrio\| ≤ 1 en todo nodo | Cada hoja tiene un `Efecto` |

### 3.1 El problema de las claves duplicadas y cómo se resolvió

Se quiere ordenar por credibilidad, pero la credibilidad **se repite**: dos
publicaciones pueden valer 30, y un ABB/AVL clásico no admite claves duplicadas.

**Solución:** clave compuesta

```java
public int claveAVL() {
    return credibilidad * 1000 + id;   // id único, 0..999
}
```

Como el `id` siempre es menor que 1000, la clave nunca se repite y al dividir por
1000 el orden sigue siendo exactamente el orden por credibilidad. El recorrido
inorden devuelve el feed ordenado de menos a más creíble. **Esta es la decisión de
diseño más probable de que les pregunten.**

### 3.2 Convención de altura y factor de equilibrio

```
altura(subárbol vacío) = -1      →  una hoja mide 0
factorEquilibrio(nodo) = altura(hijo izquierdo) - altura(hijo derecho)
```

Con esta convención un nodo cargado a la izquierda da **+2** y uno cargado a la
derecha **-2**. Se eligió así porque permite calcular el factor restando
directamente las alturas de los hijos, sin casos especiales para `null`.

---

## 4. ¿Cómo se insertan y eliminan elementos?

### 4.1 Inserción

1. Descenso recursivo comparando `claveAVL()` hasta encontrar un hueco.
2. Se crea el nodo.
3. **Al regresar de la recursión** se llama `rebalancear(nodo)` en cada nivel, de
   abajo hacia arriba: se actualiza la altura y, si el factor de equilibrio es ±2,
   se rota.

Los cuatro casos, todos en `rebalancear()`:

| Caso | Condición | Rotación |
|---|---|---|
| Izquierda-Izquierda | fe = +2 y fe(hijo izq) ≥ 0 | simple derecha |
| Izquierda-Derecha | fe = +2 y fe(hijo izq) < 0 | izquierda sobre el hijo, luego derecha |
| Derecha-Derecha | fe = -2 y fe(hijo der) ≤ 0 | simple izquierda |
| Derecha-Izquierda | fe = -2 y fe(hijo der) > 0 | derecha sobre el hijo, luego izquierda |

Al rotar, **el orden importa**: primero se actualiza la altura del nodo que quedó
abajo y después la del que subió. Al revés da alturas incorrectas.

### 4.2 Eliminación — los tres casos

| Caso | Situación | Qué se hace |
|---|---|---|
| 1 | El nodo es hoja | desaparece (se devuelve `null`) |
| 2 | Tiene un solo hijo | el hijo ocupa su lugar |
| 3 | Tiene dos hijos | se reemplaza por su **sucesor inorden** (el mínimo del subárbol derecho) y se elimina recursivamente ese sucesor |

Después de cualquiera de los tres, `rebalancear()` corrige el camino de regreso.
Verificado en `PruebaAVL`, caso 6.

**Dónde lo usa el juego:** cuando un jugador reporta una publicación y el efecto
tiene `retirarPublicacion = true`, se llama `feed.eliminar(p.claveAVL())`. En la
pestaña del árbol se ve el rebalanceo en vivo.

---

## 5. ¿Cómo se realiza su recorrido?

### 5.1 En el AVL

| Recorrido | Orden | Para qué lo usa el juego |
|---|---|---|
| **Inorden** | izq → raíz → der | Feed ordenado por credibilidad ascendente |
| **Preorden** | raíz → izq → der | Serializar el árbol (se reutilizará para el servidor de sockets en la entrega 3) |
| **Postorden** | izq → der → raíz | Liberar el feed al cerrar la partida |
| **Por rango** | inorden **con poda** | Habilidad del Periodista: solo las de credibilidad < 35 |

El recorrido por rango es el interesante: no visita todo el árbol. Si la clave del
nodo ya supera el tope, no baja por su subárbol derecho.

También se usa inorden en `PanelArbolAVL` para **dibujar** el árbol: al k-ésimo nodo
visitado se le asigna la columna k, y la fila depende de su profundidad. Eso
garantiza que dos nodos nunca se solapen en pantalla.

### 5.2 En el árbol de decisión

- **Durante la partida:** descenso dirigido en dos pasos, `raíz → acción → tipo`.
  Costo constante.
- **En la pestaña de ayuda:** recorrido en **preorden** que imprime el árbol
  completo con las consecuencias de cada hoja, para que el jugador vea el mapa de
  decisiones antes de jugar.

---

## 6. ¿Cómo afecta el árbol las decisiones del jugador?

Flujo de un turno (`logica/Partida.java`, método `ejecutar`):

```
1. El jugador pulsa un botón          →  Accion
2. arbolDecision.resolver(accion, tipoReal)  →  Efecto  (hoja del árbol)
3. efecto.aplicarSobre(ciudad, rol)   →  cambian los 6 indicadores
4. si el efecto lo pide               →  feed.eliminar(clave)  (el AVL se rebalancea)
5. pasa el turno                      →  evento aleatorio
```

El rol escala el efecto: el **Influencer** multiplica por 2 el impacto sobre la
ciudad (amplifica para bien y para mal), el **Periodista** obtiene 1.8× al
verificar, y el **Candidato** gana o pierde 1.8× en reputación. La misma acción no
vale lo mismo para todos.

---

## 7. ¿Qué comportamiento responsable busca fomentar el juego?

**Verificar antes de compartir.** Está en los números, no en un mensaje de texto:
verificar es la única acción que **nunca** produce un resultado negativo en las tres
ramas del árbol, y es la que más puntos da. Compartir sin verificar castiga fuerte
cuando la publicación era falsa.

El juego también castiga el extremo opuesto: **reportar** informaciones verdaderas u
opiniones legítimas resta confianza y convivencia. No se premia moderar por
reflejo, se premia verificar.

---

## 8. Cómo demostrarlo en la sustentación (guion de 5 minutos)

1. **Consola primero.** Correr `PruebaAVL`: muestra las rotaciones, la comparación
   de altura contra el ABB degenerado, los tres recorridos y las tres eliminaciones.
   Prueba de que el árbol funciona, sin depender de la interfaz.
2. **Abrir el juego** y jugar dos turnos.
3. **Pestaña "Árbol AVL del feed":** señalar los factores de equilibrio bajo cada
   nodo y la altura contra el número de publicaciones.
4. **Reportar una publicación falsa** y volver a la pestaña del árbol: se ve el nodo
   eliminado y el árbol rebalanceado, con el contador de rotaciones subiendo.
5. **Pestaña "Árbol de decisión":** mostrar el preorden con las 12 hojas.
6. **Activar "Modo alto contraste"** para el componente inclusivo.

---

## 9. Qué falta (honestidad para el equipo)

Esta entrega cubre **árboles + GUI preliminar**, que es lo que se califica ahora.
Queda pendiente:

- **Entrega 2 (19–23 oct):** grafo de la ciudad, grafo social y propagación de
  publicaciones por el grafo. `Partida` ya está aislada de la interfaz, así que el
  grafo se integra ahí sin tocar la GUI.
- **Entrega 3 (16–20 nov):** cliente-servidor por sockets, sonidos y animaciones.
  `Main` es la única clase que hay que reemplazar por el cliente: la lógica ya no
  depende de Swing.

---

## 10. Archivos y responsabilidades

| Archivo | Qué contiene |
|---|---|
| `estructuras/ArbolAVL.java` | AVL completo: inserción, búsqueda, eliminación, rotaciones, 3 recorridos, rango |
| `estructuras/NodoAVL.java` | Nodo con altura |
| `estructuras/ArbolDecision.java` | Árbol n-ario, las 12 hojas y el descenso |
| `estructuras/NodoDecision.java` | Nodo n-ario |
| `modelo/` | `Publicacion` (clave compuesta), `Jugador`, `EstadoCiudad`, enums |
| `logica/Partida.java` | Motor del juego: conecta los dos árboles con las reglas |
| `logica/Efecto.java` | Consecuencia de una hoja, escalada por rol |
| `logica/BancoPublicaciones.java` | 20 publicaciones y los eventos aleatorios |
| `gui/VentanaJuego.java` | Ventana con 4 pestañas |
| `gui/PanelArbolAVL.java` | Dibujo del árbol real con `paintComponent` |
| `gui/PanelIndicadores.java` | Barras de los 6 indicadores |
| `gui/Tema.java` | Paleta y modo de alto contraste |
| `pruebas/PruebaAVL.java` | Banco de pruebas por consola |

> Recordatorio: en la sustentación cualquier integrante puede ser interrogado sobre
> cualquier parte. Repartan el estudio, no solo el código.
