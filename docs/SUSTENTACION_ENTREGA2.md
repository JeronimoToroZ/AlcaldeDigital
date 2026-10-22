# Alcalde Digital — Sustentación (Segunda entrega: grafos)

**Universidad del Norte — Estructura de Datos II**
Se evalúa: implementación de grafos, operaciones, integración con los árboles,
integración con la mecánica del juego y visualización del grafo.

---

## 0. Cómo es el juego ahora

Portada → ¿cuántos juegan? (2, 3 o 4) → nombre y rol de cada uno (roles sin repetir)
→ **mapa de niveles (grafo)** → cada nivel se juega **por turnos**: una ronda por jugador,
con un aviso antes de cada ronda, cronómetro y, al final, tabla de puntuaciones con podio.

En cada nivel aparece arriba una publicación de Civitas. El jugador corre y salta
recolectando **solo** las burbujas de la acción correcta (Compartir, Verificar,
Ignorar o Reportar): 3 por publicación, 4 publicaciones, 3 vidas. Las burbujas se
generan en otro lugar cada 2,0 s en el nivel 1 y 0,1 s menos por nivel (1,4 s en el 7).
Entre menos tarde el jugador, más puntos (bono por velocidad).

---

## 1. Los dos grafos del juego

### 1.1 Grafo de niveles — `estructuras/GrafoNiveles.java`

| | |
|---|---|
| Tipo | Dirigido y acíclico (hoy con forma de árbol binario) |
| Vértices | 7 niveles (`NodoNivel`, imágenes n1…n7) |
| Aristas | `A -> B` = "ganar A desbloquea B" |
| Representación | Listas de adyacencia: `sucesores` y `predecesores` |

**Problema que resuelve:** decidir qué niveles se pueden jugar.
Regla: un nivel está desbloqueado si no tiene predecesores (N1) o si **alguno** de
sus predecesores ya fue ganado. Un nivel se marca como ganado cuando al menos un
jugador lo supera en su ronda. Ganar N2 abre N4 y N5, pero no N6 ni N7.

**¿Por qué grafo y no árbol?** Hoy el mapa tiene forma de árbol, pero un árbol no
permite que un nivel tenga dos padres. Con el grafo se puede agregar, por ejemplo,
un nivel final al que se llegue desde N4 **o** desde N7 sin cambiar el código: solo
se agregan dos aristas.

**Operaciones:**

| Operación | Método | Uso en el juego |
|---|---|---|
| Agregar vértice / arista | `agregarVertice`, `agregarArista` | Construir el mapa |
| Sucesores / predecesores | `sucesores(n)`, `predecesores(n)` | Desbloqueo y mensajes de "bloqueado" |
| Desbloqueo | `estaDesbloqueado(n)`, `completar(n, …)` | Qué nodos se pueden pulsar; animación de los recién abiertos |
| BFS | `recorridoBFS(1)` | Orden por profundidad (panel de estructuras) |
| DFS | `recorridoDFS(1)` | Recorrido rama por rama (panel de estructuras) |
| Camino más corto | `camino(a, b)` (BFS con padres) | "Camino en el mapa" antes de jugar un nivel |
| Profundidad / hojas | `profundidad(n)`, `esHoja(n)` | Al ganar una hoja se habilitan las elecciones |

**Visualización:** `gui/PantallaMapa.java` dibuja los vértices (n1…n7) y las aristas
sobre el fondo f1. Bloqueado = gris con candado; disponible = brilla; ganado =
anillo dorado y estrellas. Las aristas se vuelven doradas cuando su origen se gana.

### 1.2 Red de la ciudad — `logica/GrafoSocial.java`

| | |
|---|---|
| Tipo | No dirigido |
| Vértices | 8 lugares de Ciudad Nova (`NodoRed`) |
| Aristas | Conexiones entre lugares (por ahí viaja la información) |
| Representación | Listas de adyacencia (cada `NodoRed` guarda sus vecinos) |

**Problema que resuelve:** cómo se propaga un rumor (enunciado, sección 7):
quiénes lo reciben, cuántos, qué tan rápido y qué consecuencias genera.

**Integración con la mecánica:**
- Al empezar un nivel aparece un rumor en un lugar **aleatorio** (`sembrarRumor`).
- Cada **error** (burbuja equivocada o tiempo agotado) ejecuta un paso de **BFS**
  (`propagarPaso`): todos los vecinos sanos de los lugares contagiados se contagian.
  Además, cada lugar nuevo sube la desinformación de la ciudad.
- Cada publicación **bien resuelta** ejecuta `contener()`: calcula distancias con BFS
  desde el origen y limpia los lugares más lejanos (el rumor retrocede).
- El botón del mapa (barra superior del nivel) abre el grafo en vivo, con los indicadores y la salud de la ciudad; el mensaje del juego dice a
  qué lugares llegó o de cuáles se limpió.

**Operaciones:** `distanciasDesde` (BFS con niveles = "qué tan rápido", en saltos),
`ordenDePropagacion` (BFS), `propagarPaso`, `contener`, `cantidadInfectados`,
`cantidadAristas`.

---

## 2. Integración grafos ↔ árboles

1. **El árbol de decisión decide la burbuja correcta.** `ArbolDecision.accionIdeal(tipo)`
   recorre las 4 ramas de la raíz, baja a la hoja de ese tipo de publicación y se queda
   con la que da más puntos. El juego no tiene la respuesta escrita en otro lado.
   Se agregó un 4.º tipo, `SIN_CONFIRMAR` ("dicen que…", "comunicado oficial" sin
   fuente), para que cada acción tenga su caso ideal: 4 acciones × 4 tipos = 16 hojas.
2. **El árbol decide la consecuencia y el grafo la propaga.** Cuando el jugador falla,
   `Partida.resolver` aplica la hoja del árbol (indicadores de la ciudad) y luego mueve
   el rumor un paso en la red. Cuando acierta, aplica la hoja y contiene el rumor.
3. **El AVL guarda el feed.** Cada publicación que sale en un nivel entra al AVL;
   las reportadas se **eliminan** del AVL. El panel del nivel muestra su tamaño y altura.

Todo pasa por `logica/Partida.java`: la interfaz no toca las estructuras directamente.

---

## 3. Dónde mostrarlo en la sustentación

- **Mapa:** ganar N1 y ver cómo se abren N2 y N3 (animación + aristas doradas).
- **Nivel:** equivocarse a propósito y abrir el botón del mapa para ver el rumor avanzar.
- **Botón "Ciudad y estructuras"** (en el mapa): pestañas con la lista de adyacencia,
  BFS, DFS, caminos, la red con los saltos de cada lugar, el AVL y el árbol de decisión
  con la acción ideal por tipo.

---

## 4. Otros requisitos cubiertos

| Requisito | Dónde |
|---|---|
| Componente aleatorio | Origen del rumor, mapa de cada nivel, publicaciones, eventos entre publicaciones |
| Habilidad del jugador | Plataformas, tiempo límite por publicación, 3 vidas |
| Inclusividad | Alto contraste (letras en las burbujas, "!" en la red, ▼ en indicadores malos); cada sonido tiene aviso visual |
| Ayuda | Botón "?" en el mapa y en cada nivel |
| Elementos motivadores | Estrellas, sonidos, partículas, mensajes, desbloqueo animado, elecciones finales |
| Elección del alcalde | Votos = 60 % salud de la ciudad + 40 % reputación |

> Multijugador: es **local por turnos** (2 a 4 jugadores en el mismo computador, roles
> distintos y obligatorios). El rol de cada jugador cambia cuánto pesan sus decisiones
> en el árbol (multiplicadores de `Rol`). La versión cliente-servidor con sockets queda
> para la entrega final.
