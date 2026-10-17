# Arquitectura de ParSonic

## Principios

1. **Solo APIs públicas de Android.** Sin root, sin APIs privadas ni reflexión
   sobre clases internas. La única concesión es un `AccessibilityService` mínimo
   y **opcional** que solo filtra las teclas de volumen (nunca lee el contenido de
   la pantalla). Está apagado por defecto y el usuario lo activa a voluntad.
2. **Lógica pura y comprobable.** Todo lo que se puede decidir sin el framework
   (reconocimiento de gestos, geometría de zonas, evaluación de sensores,
   despacho de acciones, persistencia) vive en clases sin dependencias de Android
   y está cubierto por pruebas unitarias JVM.
3. **Sin sondeos.** El estado llega siempre por *callbacks* del sistema
   (`MediaSessionManager.OnActiveSessionsChangedListener`, `MediaController.Callback`,
   `SensorEventListener`, `TriggerEventListener`) o por flujos (`Flow`).
4. **Batería primero.** Ningún trabajo se mantiene encendido sin motivo.

## Capas y paquetes

```
com.andrespar2.parsonic
├── core/                 Constantes y utilidades transversales
│   ├── ParSonicConstants.kt
│   └── util/             Logg, Haptics, ScreenGeometry, ParSonicPermissions
├── data/
│   ├── media/            PlaybackStatus, NowPlaying, MediaSessionRepository (+ impl),
│   │                     VolumeController (+ impl), PlaybackStatusMapper
│   └── settings/         AppSettings y modelo de gestos, SettingsRepository (+ impl
│                         DataStore), ParSonicDataStore
├── di/AppContainer.kt    Contenedor de dependencias manual (sin Hilt/KSP)
├── service/              Servicio de gestos y sus piezas
│   ├── GestureService.kt
│   ├── GestureRuntimeState.kt
│   ├── NotificationHelper.kt
│   ├── BootReceiver.kt
│   ├── MediaListenerBridge.kt
│   ├── MediaNotificationListenerService.kt
│   ├── dispatcher/       GestureActionDispatcher
│   ├── gesture/          GestureInterpreter, GestureZone(+Calculator), TouchPhase
│   ├── overlay/          EdgeZoneView, GestureOverlayController
│   ├── sensors/          MotionGestureEvaluator, SensorGestureController
│   └── volume/           VolumeKeyAccessibilityService, VolumeKeyInterpreter,
│                         VolumeKeyAction
└── ui/                   Jetpack Compose + ViewModels
    ├── ParSonicApp.kt, ParSonicViewModelFactory.kt
    ├── home/ gestures/ player/ settings/ permissions/ compatibility/ credits/
    ├── components/ navigation/ util/
    └── theme/            Color, Type, Theme (Black Modern AMOLED)
```

## Flujo de datos

```
                 (preferencias)                       (estado de reproduccion)
   DataStore ──► SettingsRepository.settings ─┐  ┌─ MediaSessionRepository.nowPlaying
                                              ▼  ▼
                                     GestureService (primer plano)
                                              │
                    ┌─────────────────────────┼─────────────────────────┐
                    ▼                         ▼                         ▼
        GestureOverlayController     SensorGestureController   GestureRuntimeState
        (zonas táctiles)             (acelerómetro +           (estado observable
                    │                 movimiento significativo) por la interfaz)
                    ▼                         ▼
                 EdgeZoneView ──► GestureInterpreter   MotionGestureEvaluator
                    └──────────────┬──────────────────────────┘
                                   ▼
                        GestureActionDispatcher ──► MediaSessionRepository / VolumeController
```

La interfaz observa `settings`, `nowPlaying` y `GestureRuntimeState.status` a
través de ViewModels, y modifica las preferencias escribiendo en el repositorio:
el servicio reacciona solo, sin necesidad de mensajes imperativos.

## El servicio de gestos

`GestureService` es un servicio en primer plano de tipo `specialUse` (ParSonic no
reproduce audio, controla sesiones). En cada emisión de
`combine(settings, nowPlaying)` decide:

- `serviceEnabled` — interruptor maestro. Si se apaga, el servicio se detiene solo.
- `onlyWhilePlaying` — si está activo y no hay reproducción, no se mantiene ningún
  reconocimiento encendido (ahorro de batería).
- `touchGesturesEnabled` + permiso de superposición → activa las ventanas.
- `sensorGesturesEnabled` → registra los sensores.

### Zonas táctiles

`GestureZoneCalculator` (puro) calcula franjas rectangulares a partir de la
geometría de pantalla y de los *insets* del sistema. Cada franja es una ventana
independiente `TYPE_APPLICATION_OVERLAY` con `FLAG_NOT_TOUCH_MODAL`, de modo que
los toques **fuera** de las zonas siguen llegando con normalidad a la app de
debajo. `EdgeZoneView` traduce `MotionEvent` a `TouchPhase` y alimenta al
`GestureInterpreter`, manteniendo **un único** temporizador activo para resolver el
doble toque y la pulsación larga.

### Sensores

`SensorGestureController` usa dos sensores:

- **Acelerómetro**: agitado y giro boca abajo. **Solo con la pantalla encendida**
  (se retira al apagarse, observable con `ACTION_SCREEN_OFF`).
- **Movimiento significativo** (`TYPE_SIGNIFICANT_MOTION`): único sensor que
  Android mantiene con el dispositivo suspendido a coste casi nulo; gestionado por
  el *hardware*. Es de un solo disparo, así que se vuelve a armar tras cada aviso.

`MotionGestureEvaluator` (puro) recibe muestras `(x, y, z, t)` filtradas y devuelve
`SHAKE` o `FLIP_DOWN` con ventanas y periodos de *debounce*.

## Botones de volumen (servicio de accesibilidad opcional)

Interceptar las teclas de volumen de forma global y **consumirlas** no es posible
con las APIs públicas de `MediaSession`, `AudioManager` ni con *broadcasts*:
Android solo entrega (y permite consumir) teclas físicas a un
`AccessibilityService` con `flagRequestFilterKeyEvents`. ParSonic incluye uno
mínimo (`VolumeKeyAccessibilityService`):

- Declara solo `TYPE_WINDOW_STATE_CHANGED` y `onAccessibilityEvent` está vacío:
  **no** lee el contenido de la pantalla (`canRetrieveWindowContent=false`) ni
  ejecuta gestos (`canPerformGestures=false`).
- `onKeyEvent` filtra únicamente `KEYCODE_VOLUME_UP`/`KEYCODE_VOLUME_DOWN` y
  consume el evento solo si `volumeKeysEnabled` está activo y hay una sesión
  multimedia con contenido (`nowPlaying != null`).
- `VolumeKeyInterpreter` (puro) decide en una máquina de estados:
  - Una pulsación sola → siguiente (subir) o anterior (bajar).
  - Dos pulsaciones dentro de la ventana de 400 ms → en lugar de eso, ajusta el
    volumen y entra en «modo volumen».
  - El modo volumen caduca por **pereza** (sin temporizador propio): 7 s después
    de la última pulsación, la siguiente vuelve a cambiar de pista.
- `onServiceConnected` llama a `AppContainer.acquireMedia()` y `onDestroy` lo
  libera, respetando el recuento de referencias.

La app expone el interruptor (Inicio y Ajustes) y un acceso directo a los ajustes
de accesibilidad del sistema. La función está **desactivada por defecto** y, con
la pantalla apagada, depende del sistema y del fabricante.

## Inyección de dependencias

`AppContainer` es un contenedor manual y perezoso, sin procesadores de
anotaciones. Es un *singleton* por proceso: la actividad y el servicio comparten
las mismas instancias, con lo que el estado multimedia se observa una sola vez.

El estado multimedia se gobierna por **recuento de referencias**
(`acquireMedia()` / `releaseMedia()`): el callback del sistema solo está registrado
mientras la interfaz está visible o el servicio captura gestos.

## Reconocedor de gestos

`GestureInterpreter` no depende de `MotionEvent`. Recibe `TouchPhase`
(DOWN/MOVE/UP/CANCEL) con posición y tiempo y devuelve un `GestureType`. Reglas:

- Un **deslizamiento** siempre gana a un doble toque.
- El **doble toque** se resuelve en el UP del segundo toque.
- La **pulsación larga** se puede resolver por temporizador o, como respaldo, por
  duración en el UP.
- `nextTimeoutAtMs()` indica cuándo hay que reevaluar, evitando sondeos.

## Persistencia

`DataStoreSettingsRepository` guarda cada gesto con claves independientes
(`gesture_<id>_enabled`, `gesture_<id>_action`) y una sensibilidad global
(`sensitivity`). Cambiar el conjunto de gestos en el futuro no invalida las
preferencias ya guardadas y cualquier valor desconocido cae al valor por defecto.
