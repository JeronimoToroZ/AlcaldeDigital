# Alcalde Digital

Videojuego educativo sobre uso responsable de redes sociales.
Universidad del Norte — Estructura de Datos II — Primera entrega (árboles + GUI).

Java puro: solo Swing y AWT de la JDK. **Cero dependencias externas.**

---

## Cómo ejecutarlo

### Opción A — NetBeans (la que van a usar)

1. `File → New Project → Java with Ant → Java Application`. Nombre: `AlcaldeDigital`.
   Desmarcar *Create Main Class*.
2. Cerrar NetBeans y copiar la carpeta `src/alcaldedigital` dentro del `src` del
   proyecto recién creado (debe quedar `.../AlcaldeDigital/src/alcaldedigital/...`).
3. Abrir NetBeans otra vez. Clic derecho en el proyecto → `Properties → Run` →
   *Main Class*: `alcaldedigital.Main`.
4. `F6` para correr.

> Si NetBeans marca error de codificación por los acentos: `Properties → Sources →
> Encoding: UTF-8`.

### Opción B — Terminal

```bash
# compilar
javac -d build -encoding UTF-8 $(find src -name "*.java")

# jugar
java -cp build alcaldedigital.Main

# correr el banco de pruebas del AVL (esto es lo que se muestra en la sustentación)
java -cp build alcaldedigital.pruebas.PruebaAVL
```

### Opción C — JAR ya compilado

```bash
java -jar AlcaldeDigital.jar
```

---

## Qué se ve al abrirlo

| Pestaña | Contenido |
|---|---|
| **Partida** | Publicación del turno, los 4 botones, temporizador de 10 s, indicadores de la ciudad, marcador y bitácora |
| **Árbol AVL del feed** | El árbol real dibujado, con la credibilidad dentro de cada nodo y el factor de equilibrio debajo |
| **Árbol de decisión** | Recorrido en preorden con las 12 consecuencias posibles |
| **Ayuda** | Objetivo, reglas, función de cada botón, roles, indicadores y cómo se gana |

La casilla **Modo alto contraste** (arriba a la derecha) es el componente inclusivo.

---

## Estructura del código

```
src/alcaldedigital/
├── Main.java                      punto de entrada, configura 2 a 4 jugadores
├── modelo/                        datos del juego
│   ├── Accion.java                enum: COMPARTIR, VERIFICAR, IGNORAR, REPORTAR
│   ├── TipoContenido.java         enum: VERDADERA, FALSA, OPINION
│   ├── Rol.java                   enum con los multiplicadores de cada rol
│   ├── Publicacion.java           clave compuesta para el AVL
│   ├── Jugador.java               puntos y reputación
│   └── EstadoCiudad.java          los 6 indicadores
├── estructuras/                   AQUÍ ESTÁ LA ENTREGA
│   ├── NodoAVL.java
│   ├── ArbolAVL.java              AVL completo con las 4 rotaciones
│   ├── NodoDecision.java
│   └── ArbolDecision.java         árbol n-ario, 17 nodos
├── logica/
│   ├── Efecto.java                consecuencia de una hoja
│   ├── BancoPublicaciones.java    feed aleatorio y eventos
│   └── Partida.java               motor: conecta los árboles con las reglas
├── gui/
│   ├── Tema.java                  paleta + alto contraste
│   ├── PanelIndicadores.java
│   ├── PanelArbolAVL.java         dibuja el árbol con paintComponent
│   └── VentanaJuego.java
└── pruebas/
    └── PruebaAVL.java             7 pruebas por consola
```

La GUI **no conoce ninguna estructura de datos**: solo habla con `Partida`. Por eso
la entrega 3 (sockets) solo tiene que reemplazar `Main`.

---

## Las dos estructuras en una línea cada una

- **AVL** (`ArbolAVL`): mantiene el feed ordenado por credibilidad para buscar,
  consultar por rango y eliminar en O(log n) garantizado, sin importar el orden de
  llegada de las publicaciones.
- **Árbol n-ario** (`ArbolDecision`): traduce las 12 combinaciones de
  (acción del jugador × naturaleza real de la publicación) en consecuencias
  concretas, como datos en vez de `if` anidados.

El razonamiento completo, con las 5 preguntas del enunciado respondidas, está en
[`docs/SUSTENTACION.md`](docs/SUSTENTACION.md).

---

## Estado

- [x] Árboles (AVL + n-ario) con inserción, eliminación, búsqueda y recorridos
- [x] Interfaz gráfica con visualización del árbol
- [x] Componente aleatorio (feed barajado + eventos)
- [x] Componente inclusivo (alto contraste, datos nunca solo por color)
- [x] Sistema de ayuda
- [x] Habilidad y tiempo limitado (10 s por turno)
- [x] Indicadores, puntuación y elección del alcalde
- [ ] Grafos — entrega 2
- [ ] Cliente-servidor por sockets — entrega 3
- [ ] Sonidos y animaciones — entrega 3
