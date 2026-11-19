# Alcalde Digital (agencia Influence, Inc.): Mesa de Verificación. Guía de sustentación

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
- **Tiempo limitado:** el reloj va de 9:00 a 18:00, lo que equivale a 8 minutos reales por día.
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

---

## 9. Versión 2: roles, interfaz nueva y mecánicas interactivas

### 9.1 Jugadores, nombres y roles (`gui/Configuracion.java`, `modelo/RolJugador.java`)
Antes de CUALQUIER partida (solo, local o en red) el juego pide el **nombre** y el **rol** de cada
jugador. No deja empezar hasta que todos tengan nombre y rol, y en la partida local cada rol es de un
solo jugador. En red, la sala muestra a cada uno como "Ana (Periodista)".

| Rol | Herramientas del monitor | Habilidad única (mecánica interactiva) | Estructura que usa |
|---|---|---|---|
| Ciudadano | Perfil, Lupa, Búsqueda inversa | **¿Quién te lo mandó?**: sube por la cadena de reenvíos escogiendo, en cada paso, a quien tiene la misma huella y lo compartió antes, hasta el origen | Árbol BFS (padres) del grafo social |
| Periodista | Las 6 (corresponsal, escáner de red y llamadas son solo suyos) | **Investigación completa** (lo de antes) | Dijkstra, BFS/DFS, AVL |
| Influencer | Perfil, Búsqueda inversa, Escáner de red | **Cazabots**: 12 s para marcar solo las cuentas falsas que aparecen; además sus decisiones pesan x1,5 | 2 oleadas del BFS desde el origen |
| Candidato | Perfil, Directorio, Lupa | **Rueda de prensa**: 3 preguntas de 10 s; se ARRASTRA la respuesta al micrófono; suma o resta confianza, conflictos e intención de voto | Indicadores de la ciudad + AVL |

Si la habilidad sale bien y el caso es sospechoso, se **INSERTA** una confrontación en el árbol de
diálogo (igual que las herramientas). Todo está en `logica/Habilidades.java` y `Jornada.terminarHabilidad()`.

### 9.2 Otras mecánicas interactivas
- **Sellos arrastrables**: VERIFICADO (antes publicar), OPINIÓN, #PUBLICIDAD, RECHAZAR y REPORTAR (antes
  denunciar). Se toma el sello y se suelta sobre la publicación del celular (teclas Z X C V B siguen sirviendo).
- **Preguntas arrastrables**: se arrastra la tarjeta hacia el visitante (clic o teclas 1-5 también).
- **Lupa interactiva**: se hace clic en la zona de la foto donde está la diferencia (pelo, cejas, ojos/gafas, lunar).
- **Alerta viral** (sección 9 del enunciado): cuando ocurre el evento viral aparece una publicación con
  10 segundos para arrastrar VERIFICAR / REPORTAR / IGNORAR o compartirla. Puede ser verdadera o falsa.
- **Billetera**: solo aparece en la ventana si el visitante trae credencial.

### 9.3 Interfaz
- Paleta única tomada de la portada (madera, cobre, crema, rosa, lila, salvia, ladrillo) en `gui/Tema.java`.
- Tipografía única **Poppins** (licencia OFL) cargada desde `recursos/`.
- Todas las pantallas son lienzos escalables (`gui/Lienzo.java`): caben en cualquier tamaño de ventana.
- Transiciones suaves entre pantallas (desvanecido + acercamiento) en `VentanaPrincipal`.
- Barra superior con 4 botones de imagen en todas las pantallas y roles: **ayuda** (objetivo, tu rol,
  cómo se juega, botones, roles, indicadores, cómo ganar, reglas, estructuras), **estructura**, **mapa**
  (el grafo de la ciudad, que antes estaba abajo a la derecha) y **pausa**.
- Ventana de interrogatorio con cielo que cambia con la hora (mañana, tarde, noche con luna y estrellas).
- La noticia se ve dentro del **celular** con el usuario y una ilustración alusiva.
- Elección del alcalde al final: si hay jugadores candidatos, compiten con dos rivales de la ciudad; su
  intención de voto sale de la confianza, la información verificada, los conflictos y sus ruedas de prensa.

### 9.4 Pruebas
`pruebas/PruebaRoles.java` comprueba herramientas por rol, la cadena del ciudadano (una sola opción
correcta por paso), el x1,5 del influencer, la rueda de prensa y la alerta viral.

---

## 10. Versión 3: ajustes de ritmo, validación y tienda

- **Máximo 3 personas por día** (`Historia.MAX_CASOS_DIA`): 15 casos en la semana. Se conservaron los
  casos clave de la historia (impostora del Acueducto, foto vieja, soborno de $500, deepfake del jefe) y
  los demás quedan como reserva en `Historia.casosDeReserva()`.
- **Herramientas que se reinician con cada cliente** (`Jornada.reiniciarHerramientas()`): lupa,
  directorio, llamada, corresponsal y red quedan disponibles otra vez cuando pasa la siguiente persona.
- **Validación de herramientas** (`Jornada.motivoNoAplica()`): si el caso no permite una herramienta
  (lupa sin credencial, directorio sin cuenta, búsqueda inversa sin foto, corresponsal sin barrio, red sin
  origen), la pestaña sale deshabilitada con una X roja y, si se intenta usar, aparece un **aviso flotante
  rojo** que explica por qué. No gasta minutos.
- **Indicadores dentro del botón MAPA**: verificada, confianza, convivencia, bienestar, desinformación y
  conflictos se consultan ahí (con la salud de la ciudad en la insignia del botón). La pantalla principal
  queda más limpia y la libreta de pistas es más grande.
- **Credenciales** con el diseño unificado: papel crema, franja de cobre, emblema de Ciudad Nova y Poppins.
- **Tienda** (`logica/Tienda.java`, botón con la bolsa o tecla S): café (pausa el reloj), horas extra,
  pista del informante, recarga de habilidad, lupa de alta precisión, seguro de reputación (anula una
  advertencia) y la campaña "Piensa antes de compartir" (baja la desinformación). Convierte el dinero
  ganado en ventaja: es un elemento motivador (sección 17 del enunciado).
- `PruebaRoles` ahora también prueba el reinicio de herramientas, la validación y la tienda.

---

## 11. Versión 4: pantalla limpia, celular, pista de sellos, noticiero y temario completo

> Esta sección reemplaza lo que las secciones 9.2, 9.3 y 10 dicen sobre la interfaz (bandeja de sellos,
> monitor, libreta y noticiero de abajo). La lógica de los casos, los roles y los finales no cambió.
> Capturas en `docs/capturas_v4/`.

### 11.1 Qué se ve en pantalla (`gui/OficinaVista.java`)
- Solo quedan fijas la **ventanilla** (el visitante detrás del vidrio) y el **cuadro de diálogo** con las
  preguntas. Arriba sigue la barra (ayuda, estructuras, mapa y pausa).
- **Se quitó la libreta de pistas.** Las pistas salen como avisos y quedan guardadas en cada app.
- **El celular vive escondido** a la derecha (lengüeta "Celular · tecla P"). Sube solo cuando hay una
  situación que lo pide: la llamada de Don Ramiro, un visitante que trae una publicación, el reporte del
  corresponsal o un mensaje importante. Los mensajes menores solo hacen vibrar la lengüeta. Cuando el
  celular sube, la escena se corre a la izquierda.
- **Los sellos solo aparecen al decidir**: botón "Decidir el sello" (D) o directamente Z X C V B.
- La credencial solo aparece sobre el mostrador si el visitante trae una.

### 11.2 El celular (`gui/CelularVista.java`, `logica/Telefono.java`)
Dibujado como un teléfono real: marco, isla de la cámara, barra de estado (hora del juego, señal que se
cae con el apagón, batería que baja con el día), pantalla de inicio con widget de la ciudad, dock y barra
de gestos. Apps:

| App | Qué hace | Estructura |
|---|---|---|
| Civitas | La publicación del caso; allí cae el sello | — |
| Mensajes | Chats con respuestas rápidas ("¿Y la fuente?") | **Tabla hash** contacto → conversación |
| Teléfono | Agenda en orden alfabético y "Llamar a la institución" (K) | **ABB** + recorrido inorden iterativo |
| Noticias | Últimas horas del día, tendencias y "Repetir" el noticiero | **Shell sort** (tendencias) |
| Perfil, Directorio, Lupa, Imagen, Mapa, Red | Las herramientas de antes, ahora como apps (Q W E R T Y) | AVL, Dijkstra, BFS/DFS, Bellman-Ford, Floyd-Warshall |
| Habilidad | El minijuego del rol (U) | — |
| Tienda | Abre la tienda (S) | — |

Las notificaciones llegan a una **cola FIFO** y se muestran una por una.

### 11.3 Pista de sellos estilo Pump It Up (`logica/PistaSellos.java`, `gui/JuegoSellos.java`)
1. El jugador escoge el sello (o lo pide directo con Z X C V B).
2. Cuenta regresiva 3, 2, 1.
3. Por **5 carriles** (flechas ↙ ↖ ● ↗ ↘, teclas Z Q S E C o 1-5, o clic) caen iconos de sellos a
   **velocidad 1.5** (`PistaSellos.VELOCIDAD`; 1.0 = 200 píxeles del lienzo por segundo).
4. Con **4 aciertos** del sello escogido, el sello se activa y cae sobre la publicación del celular.
5. Pisar un icono de otro sello o dejar pasar el bueno es un fallo. Con **3 fallos** el sello no sale y
   llega una **advertencia** (`Jornada.fallarSello()`): se pierde una "vida". El seguro de la tienda la
   puede absorber. Pisar un carril vacío no castiga.

Estructuras: las notas se generan por carril, se mezclan ordenándolas por tiempo con **heap sort** y
esperan en una **cola** hasta que les toca caer.

### 11.4 Noticiero y narrativa por rol (`logica/Noticiero.java`, `gui/TeleVista.java`, `logica/Historia.java`)
- **Edición de la mañana:** apertura en el estudio con la presentadora (Lucía Prado), nota **en vivo** desde un
  barrio con el reportero (Tato Rincón), un bloque de **tu rol** y un **giro de última hora** que anticipa
  el caso del día.
- **Edición de la noche:** tus titulares del día, ordenados por impacto (**inserción**), el termómetro de la
  ciudad y un **avance** de mañana. El día 5 informa el flujo de votantes hacia las urnas.
- **Última hora durante el día:** franja roja sobre el vidrio con cada sello, evento o pánico.
- **Según el rol**, a cada jugador le escribe alguien distinto (`Historia.mensajesDelDia`): el ciudadano
  recibe el grupo de vecinos, el periodista a su editora, el influencer a su mánager y el candidato a su jefa
  de campaña. Don Ramiro les escribe a todos y en el día 5 avisa que "ya va para allá".
- **En red**, cada titular viaja por **sockets** (mensaje `NOT`/`NOTICIA`) y aparece en la última hora de
  los otros jugadores.

### 11.5 Personajes (`gui/Caras.java`)
Se redibujaron: cabeza con cráneo y mentón (no elipse), sombreado suave, ojos con párpado, iris y brillo,
cejas rellenas, nariz y labios, rubor, pelo recortado contra la cabeza con `Area`, ropa con cuello, saco y
corbata. Las zonas de la lupa (pelo, cejas, ojos/gafas, lunar) quedaron en el mismo lugar.

### 11.6 Dónde está cada tema del semestre

| Tema | Archivo y método | Dónde se ve en el juego |
|---|---|---|
| Recorridos recursivos de árbol binario | `ArbolBinarioBusqueda.preordenRecursivo / inordenRecursivo / postordenRecursivo` | Prueba y visor (F2) |
| Recorridos iterativos | `preordenIterativo` (pila), `inordenIterativo` (pila), `postordenIterativo` (dos pilas), `porNiveles` (cola) | App Teléfono (inorden) |
| ABB: insertar, buscar, eliminar (hoja, 1 hijo, 2 hijos con sucesor) | `ArbolBinarioBusqueda` | Agenda del celular |
| AVL | `ArbolAVLGenerico` (sin cambios) | App Directorio |
| BFS / DFS | `GrafoRed.bfsOleadas`, `dfsAlcanzables`; `GrafoCiudad.saltosDesde` | App Red, pánico |
| Dijkstra | `GrafoCiudad.caminoMasCorto`, `GrafoRed.caminoMinimo` | Corresponsal (app Mapa) |
| Bellman-Ford | `GrafoRed.bellmanFord` (aristas bot→bot pesan −1) | App Red: **cámara de eco** = ciclo negativo |
| Floyd-Warshall | `GrafoCiudad.floydWarshall`, `centro` | App Mapa (minutos a cada barrio) y estudio del noticiero |
| Kruskal (con conjuntos disjuntos) | `GrafoCiudad.kruskal` | **Red de alerta**: al acertar, la calma viaja por ese árbol |
| Prim | `GrafoCiudad.prim` | Cobertura en vivo del noticiero (crece cada día) |
| Ford-Fulkerson | `GrafoCiudad.flujoMaximo` (DFS para caminos aumentantes) | Votantes por hora el día de elecciones; el pánico tapa calles |
| Burbuja | `Ordenamientos.burbuja` | Marcador de la partida en red |
| Inserción | `Ordenamientos.insercion` | Titulares del noticiero; calles en Kruskal |
| Shell | `Ordenamientos.shell` | Tendencias de la app Noticias |
| Quick sort | `Ordenamientos.quick` | Ranking final de jugadores |
| Heap sort | `Ordenamientos.heap` | Notas de la pista de sellos |
| Tabla hash / diccionario | `TablaHash` (encadenamiento, rehash al 0,75) | Mensajes del celular; publicaciones de la historia |
| Colas | `ArrayDeque` | Fila de la ventanilla, notificaciones, notas de la pista |
| Hilos | `Sonido` (productor-consumidor con `LinkedBlockingQueue`), un hilo por conexión en `ServidorOficina` | Sonido sin trabar la pantalla |
| Sockets | `red/ServidorOficina`, `red/ClienteOficina` | Lobby, semilla, marcador y titulares en vivo |

También se simplificó código que no era del nivel del curso: la propagación del grafo ya no recibe
`Predicate`/`ToDoubleFunction` sino un `double factor`, y los comparadores de Dijkstra son clases anónimas.

### 11.7 Pruebas
- `pruebas/PruebaTemario.java` (nueva): recorridos recursivos = iterativos, ABB, tabla hash (colisión
  `"Aa"`/`"BB"` y rehash), los cinco ordenamientos, Floyd-Warshall = Dijkstra en los 64 pares,
  Kruskal = Prim, Ford-Fulkerson (red de libro = 23) y Bellman-Ford (cámara de eco).
- `PruebaRoles` ahora también prueba la pista de sellos (velocidad 1.5, 4 aciertos, 3 fallos = advertencia,
  seguro), el celular (cola, ABB, chats) y el noticiero por rol.
- `PruebaJuego` prueba además el titular que viaja por sockets.

---

## 12. Versión 5: pista de sellos directa, noticiero, fondo vivo y red sincronizada

> Reemplaza lo que la sección 11.3 dice de la pista de sellos. Capturas en `docs/capturas_v5/`.

### 12.1 Pista de sellos (`logica/PistaSellos.java`, `gui/JuegoSellos.java`)
- **Empieza enseguida**: "Decidir el sello" (D) o cualquiera de **Z X C V** abre la pista sin escoger antes.
  El celular muestra la publicación y la pista sale en un **panel pequeño** al lado del visitante.
- **4 carriles, teclas Z X C V** (o clic sobre el círculo). Velocidad de caída **1.1**.
- Caen **círculos** con todos los sellos del día, mezclados en "bolsas" (en cada ronda sale cada sello una vez).
  Los receptores son círculos con los colores de la paleta (cobre, lila, salvia, dorado) y cada sello tiene
  ahora un color de la paleta (`OficinaVista.colorSello`).
- El **primer círculo que se acierta decide el sello**; con **4 iguales** se activa y cae sobre la publicación.
- **La única forma de perder una vida al instante** es pisar un círculo de **otro** sello
  (`Jornada.fallarSello()`; el seguro de la tienda la puede absorber).
- **Margen de 10**: los círculos que pasan sin pisarse no castigan. Si pasan 10, la pista se cierra sin
  castigo y se puede volver a abrir. Pisar un carril vacío tampoco castiga.
- Estructuras: heap sort (notas por tiempo) y cola FIFO (salen en orden).

### 12.2 Noticiero (`gui/TeleVista.java`, `OficinaVista.noticiero`)
Cabecera del canal, señal a pantalla grande con cámara que se acerca, rótulo que entra deslizándose,
cortinilla de cobre entre bloques, **escaleta** del programa con el bloque actual y su progreso,
**termómetro con los 6 indicadores** de la ciudad y cinta de titulares a todo lo ancho.

### 12.3 Fondo y ritmo
- Fondo vivo (`OficinaVista.fondoVentana`): luz que cambia con la hora del juego, un haz de sol que barre la
  pared y motas de luz que suben, todo en la paleta. Con alto contraste queda negro y quieto.
- El reloj va más lento: 9 horas de juego en **8 minutos** reales (`Jornada.VELOCIDAD`).

### 12.4 Indicadores y consecuencias
- El widget del celular muestra los **6 indicadores** con barra, siempre actualizados.
- Después de cada sello, el resultado muestra cuánto subió o bajó cada indicador
  (por ejemplo "Confianza +2", "Desinformación −3").
- El resultado dice además **a cuántas cuentas** llegó la publicación y **en cuántas oleadas** del BFS
  (quiénes, cuántos y qué tan rápido).

### 12.5 Cliente-servidor: qué sincroniza el servidor (`red/ServidorOficina.java`)

| Estado | Cómo | Mensajes |
|---|---|---|
| Jugadores | Tabla hash propia nombre → datos | `HOLA`, `LOBBY` |
| Puntuaciones | Marcador ordenado por inserción | `RES`, `MARCADOR` |
| Publicaciones | Lista de sellos de cada jugador; se reenvían como noticia | `PUB`, `NOTICIA` |
| Relaciones | Cuentas falsas reportadas: se **eliminan del grafo social de todos** | `REL`, `RELACION` |
| Eventos | Apagón, viral, pánico... de cada jugador | `EVT`, `NOTICIA` |
| Turnos | Nadie empieza el día d+1 hasta que todos terminen el d (barrera) | `FIN`, `ESPERA`, `TURNO` |

Cada conexión tiene su propio **hilo**; los métodos que tocan el estado son `synchronized`. Si alguien se
desconecta, deja de frenar a los demás. `PruebaJuego` lo prueba con un servidor y dos clientes reales.

### 12.6 Árbol aplicado a la clasificación de publicaciones
`JugadorVerificador.archivo` es un **ABB** donde cada publicación sellada se inserta con clave
`credibilidad × 1000 + id`. La app Civitas muestra "Tu archivo" con el recorrido **inorden** (de la menos
creíble a la más creíble). Lo prueba `PruebaRoles`.
