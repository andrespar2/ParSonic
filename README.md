# ParSonic

Aplicación Android para **controlar la música con gestos**, inspirada en el gesto
musical de Infinix pero buscando la máxima compatibilidad entre fabricantes.
Escrita en Kotlin con Jetpack Compose, siguiendo el patrón MVVM y apoyándose
**solo en APIs públicas de Android**.

> Desarrollado por **andrespar2**.

## Qué hace

- **Detecta el reproductor activo** con `MediaSessionManager`, usando un
  `NotificationListenerService` únicamente como llave de acceso (nunca lee el
  contenido de tus notificaciones).
- **Controla la reproducción** (play/pausa, siguiente, anterior, avance/retroceso
  de 10 s, volumen) sobre la sesión multimedia activa.
- **Reconoce gestos** y los traduce en acciones **totalmente configurables**:
  - Táctiles (en zonas de los bordes): deslizar en las cuatro direcciones, doble
    toque, toque simple y pulsación larga.
  - De sensor: agitar el dispositivo y girarlo boca abajo.
  - Golpe con la pantalla apagada mediante el sensor de *movimiento
    significativo* (si el dispositivo lo tiene).
- **Ahorra batería de verdad**: el reconocimiento solo está activo mientras hay
  música sonando (configurable), sin sondeos periódicos, sin *wakelocks* y
  liberando los sensores en cuanto no se necesitan.
- **Interfaz "Black Modern AMOLED"**: negro puro, acentos rojos, tarjetas
  minimalistas y animaciones fluidas.

## Compatibilidad

Funciona con cualquier app que publique una sesión multimedia: **Spotify**,
**YouTube Music**, **VLC**, reproductores locales y muchas más.

Consulta [`docs/COMPATIBILITY.md`](docs/COMPATIBILITY.md) para saber con detalle
**qué permite Android y qué no**. Resumen honesto:

- Los **gestos táctiles requieren la pantalla encendida**: Android no entrega
  eventos táctiles a ninguna app con la pantalla apagada. No es una limitación de
  ParSonic, y no se simula ni se promete lo que el sistema no permite.
- Para la **pantalla apagada** existe una alternativa real: el sensor de
  *movimiento significativo* (coste de batería prácticamente nulo).

## Requisitos

- Android 8.0 (API 26) o superior.
- Acceso a notificaciones (para detectar el reproductor activo).
- Permiso para dibujar sobre otras apps (para las zonas de gesto táctiles).

## Compilar

Necesitas JDK 17 y el SDK de Android (platform 35 y build-tools 35.0.0).

```powershell
# Variables de entorno locales (JAVA_HOME / ANDROID_HOME)
. .\.build-env.ps1

# APK de depuración
.\gradlew.bat :app:assembleDebug

# APK de release (firmado con keystore.properties.local si existe)
.\gradlew.bat :app:assembleRelease

# Pruebas unitarias
.\gradlew.bat :app:testDebugUnitTest
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

### Firma del release

El build resuelve la firma en este orden:

1. `keystore.properties.local` — credenciales de **producción** (no se sube al repo).
2. `keystore.properties` — credenciales de **desarrollo** (sí se suben, para builds reproducibles).
3. Firma de depuración — último recurso para que `assembleRelease` nunca falle.

Para generar un keystore de producción:

```powershell
.\tools\generate-release-keystore.ps1
```

## Arquitectura

Resumen en [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). Capas:

```
core/     constantes, utilidades (registro, háptica, geometría de pantalla, permisos)
data/     media (sesiones/reproducción/volumen) y settings (modelo + DataStore)
service/  GestureService, ventanas de superposición, sensores, notificación, arranque
di/       AppContainer (inyección manual)
ui/       Compose + ViewModels (Inicio, Gestos, Reproductor, Ajustes, Permisos, ...)
```

## Privacidad

ParSonic no se conecta a Internet, no tiene cuentas, analítica ni publicidad, y no
guarda el contenido de tus notificaciones. Detalles en
[`docs/PRIVACY.md`](docs/PRIVACY.md).

## Pruebas

Más de 100 pruebas unitarias cubren la lógica pura (reconocedor de gestos, zonas,
evaluador de sensores, despachador de acciones, persistencia):

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Licencia

Software libre, publicado tal cual y sin garantías.
