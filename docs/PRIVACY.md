# Privacidad de ParSonic

ParSonic es una aplicación **local**. No tiene servidor, no envía datos a
Internet y no incorpora analítica, publicidad ni rastreadores de ningún tipo.

## Qué hace con cada permiso

| Permiso | Para qué se usa | Qué NO hace |
|---|---|---|
| **Acceso a notificaciones** (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Es la única vía pública para que `MediaSessionManager` entregue las **sesiones multimedia activas** de otras apps. Se usa como "llave" de acceso y para saber cuándo cambian. | No lee el contenido de tus notificaciones, ni las guarda, ni las envía a ningún sitio. |
| **Dibujar sobre otras apps** (`SYSTEM_ALERT_WINDOW`) | Crear las franjas transparentes de gesto en los bordes de la pantalla. | No captura la pantalla ni lee lo que muestran otras apps. |
| **Servicio en primer plano** (`specialUse`) | Mantener activos los listener de gestos mientras el usuario lo autoriza. | No reproduce audio ni accede a tu ubicación. |
| **Servicio de accesibilidad** (`BIND_ACCESSIBILITY_SERVICE`) | Detectar las pulsaciones de los botones de volumen para usarlos como mando. Es **opcional** (apagado por defecto). | No lee el contenido de la pantalla (`canRetrieveWindowContent=false`), no ejecuta gestos ajenos y no observa ninguna otra tecla. |
| **Notificaciones** (`POST_NOTIFICATIONS`) | Mostrar la notificación obligatoria del servicio (Android 13+). | — |
| **Reinicio** (`RECEIVE_BOOT_COMPLETED`) | Reiniciar el servicio tras un reinicio, **solo si el usuario lo activa**. | — |

ParSonic **no** solicita permisos de ubicación, cámara, micrófono, contactos,
almacenamiento ni SMS.

## Qué datos se guardan

Solo preferencias en el propio dispositivo, mediante **Jetpack DataStore**
(almacenamiento privado de la app):

- Interruptor maestro y arranque automático.
- Si el uso de los **botones de volumen** como mando está activo.
- Qué gestos están activos y qué acción hace cada uno.
- Sensibilidad global.
- Ajuste de "solo mientras suena música".

No se guarda historial de reproducción, ni nombres de canciones, ni qué apps
usas. Toda esta información queda en tu teléfono y se borra al desinstalar la
app.

## Datos en tránsito

Ninguno. ParSonic no abre conexiones de red. No hay `INTERNET` en el manifiesto.

## Menores y terceros

ParSonic no comparte datos con terceros porque no recoge ni transmite datos.
El control de reproducción se ejerce directamente sobre las sesiones multimedia
del sistema, sin intermediarios.

## Código abierto

El código es público. Puedes auditar exactamente qué hace cada permiso en el
repositorio: <https://github.com/andrespar2/ParSonic>.

## Contacto

Desarrollado por **andrespar2**. Para dudas de privacidad, abre un issue en el
repositorio.
