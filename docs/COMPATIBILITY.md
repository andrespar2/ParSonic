# Compatibilidad de ParSonic

Este documento explica, sin promesas de marketing, **qué puede hacer ParSonic en
cada situación y qué bloquea Android o el fabricante**. La meta es la máxima
compatibilidad posible dentro de las APIs públicas del sistema; cuando algo no
es posible, ParSonic lo dice en vez de fingirlo.

## Requisitos mínimos

| Elemento | Requisito |
|---|---|
| Android | 8.0 Oreo (API 26) o superior |
| Permiso clave | Acceso a notificaciones (para leer sesiones multimedia) |
| Permiso de gestos táctiles | Dibujar sobre otras apps (`SYSTEM_ALERT_WINDOW`) |
| Permiso de servicio | Servicio en primer plano (`specialUse`) + notificaciones (Android 13+) |
| Botones de volumen | Servicio de accesibilidad (**opcional**, solo filtra las teclas de volumen) |
| Reinicio automático | `RECEIVE_BOOT_COMPLETED` (opcional) |

## Qué controla ParSonic

ParSonic no reproduce audio: **controla la sesión multimedia que ya está
publicando otra app** a través de `MediaSessionManager` + `MediaController`. Por
eso funciona con cualquier aplicación que publique una sesión multimedia
compatible: Spotify, YouTube Music, VLC, reproductores locales, podcasts, etc.

Las acciones disponibles sobre la sesión activa son las que el reproductor
declare soportar: reproducir/pausar, siguiente, anterior, subir/bajar volumen,
silenciar, etc. Si el reproductor no soporta una acción, ParSonic no puede
inventarla.

## Gestos

### Gestos táctiles (requieren pantalla encendida)

Son franjas transparentes en los bordes de la pantalla (`TYPE_APPLICATION_OVERLAY`
con `FLAG_NOT_TOUCH_MODAL`). Los toques **fuera** de las zonas siguen llegando
con normalidad a la app que hay debajo.

- Deslizamientos, toques, doble toque y pulsación larga.
- **Solo con la pantalla encendida.** Android no entrega eventos táctiles a
  ninguna aplicación con la pantalla apagada; es una restricción del sistema, no
  de ParSonic.
- Conviven con el gesto "atrás" del sistema (Android 10+): ParSonic activa las
  zonas solo mientras suena música y su ancho/posición es configurable.

### Gestos de sensor

| Gesto | Sensor | Pantalla |
|---|---|---|
| Agitar | Acelerómetro | Encendida |
| Girar boca abajo | Acelerómetro | Encendida |
| Golpe en pantalla apagada | Movimiento significativo (`TYPE_SIGNIFICANT_MOTION`) | Apagada |

El sensor de **movimiento significativo** es el único que Android mantiene
activo con el dispositivo suspendido a coste casi nulo, porque lo gestiona el
*hardware*. Es la alternativa real y honesta a los gestos táctiles con la
pantalla apagada. Es de un solo disparo, así que ParSonic lo vuelve a armar tras
cada aviso. No todos los dispositivos lo exponen; la pantalla de Compatibilidad
de la app indica si el tuyo lo tiene.

## Botones de volumen como mando (opcional)

Además de los gestos, ParSonic puede dar otro uso a los botones de volumen:

- Una pulsación → **siguiente** (subir) o **anterior** (bajar).
- Dos pulsaciones rápidas → **ajustar el volumen** y entrar en «modo volumen».
- Tras **7 segundos** sin pulsar, los botones vuelven a cambiar de pista.

Para poder interceptar y **consumir** las teclas de volumen hace falta un
`AccessibilityService` con `flagRequestFilterKeyEvents`: es la única vía pública;
`MediaSession`, `AudioManager` y los *broadcasts* no permiten consumir una tecla
física. El servicio de ParSonic es mínimo y solo filtra las teclas de volumen (no
lee la pantalla). La función es **opcional y está apagada por defecto**: se activa
desde Inicio o Ajustes.

## Limitaciones que no se pueden sortear

1. **Pantalla apagada y toques:** imposible para cualquier app sin root. Android
   no enruta `MotionEvent` a aplicaciones con la pantalla apagada.
2. **Superposición sobre la pantalla de bloqueo:** depende del fabricante. En
   algunos dispositivos el sistema oculta las ventanas de superposición por
   seguridad.
3. **Acciones no soportadas por el reproductor:** si la app no declara la acción,
   no existe.
4. **Fabricantes agresivos con el segundo plano:** ver sección siguiente.
5. **Botones de volumen con la pantalla apagada:** depende del sistema y del
   fabricante; algunos no entregan las pulsaciones al servicio con la pantalla
   apagada. Con la pantalla encendida funciona en cualquier dispositivo.

## Fabricantes y segundo plano

Muchos fabricantes (Xiaomi/MIUI/HyperOS, Huawei/EMUI, Samsung, OPPO/ColorOS,
vivo, realme...) limitan el trabajo en segundo plano y los servicios en primer
plano. Para que ParSonic sea fiable en esos dispositivos conviene:

- Permitir que ParSonic se ejecute en segundo plano.
- Desactivar la optimización de batería para ParSonic.
- Activar el inicio automático (autostart) en los ajustes del fabricante.
- Bloquear la app en la lista de recientes para que el sistema no la mate.

ParSonic declara un servicio en primer plano de tipo `specialUse` justificándolo
en el manifiesto. No reproduce audio, por lo que no puede usar los tipos
`mediaPlayback`/`mediaProjection`.

## Resumen de estado por función

| Función | Estado | Depende de |
|---|---|---|
| Detectar reproductores activos | Disponible | Permiso de notificaciones |
| Play/pausa/siguiente/anterior | Disponible | Soporte del reproductor |
| Volumen/silenciar | Disponible | Sesión activa |
| Gestos táctiles | Disponible con pantalla encendida | Overlay + pantalla encendida |
| Agitar / boca abajo | Disponible con acelerómetro | Pantalla encendida |
| Botones de volumen como mando | Disponible con pantalla encendida | Servicio de accesibilidad + sesión activa |
| Golpe en pantalla apagada | Disponible si existe el sensor | `TYPE_SIGNIFICANT_MOTION` |
| Inicio automático tras reinicio | Disponible | `RECEIVE_BOOT_COMPLETED` + autostart del fabricante |
