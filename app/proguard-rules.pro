# Reglas de R8/ProGuard para ParSonic.
# La app usa APIs de plataforma (media.session, NotificationListenerService),
# por lo que solo se anotan los puntos de entrada reflejados por el sistema.

# Componentes declarados en el manifiesto (los instancia el sistema).
-keep class com.andrespar2.parsonic.service.MediaNotificationListenerService { *; }
-keep class com.andrespar2.parsonic.service.GestureService { *; }
-keep class com.andrespar2.parsonic.service.BootReceiver { *; }
-keep class com.andrespar2.parsonic.service.volume.VolumeKeyAccessibilityService { *; }

# Callbacks de MediaSessionManager / MediaController (invocados desde el framework).
-keepclassmembers class * extends android.media.session.MediaController$Callback {
    public <methods>;
}

# Conserva informacion util para reportes de fallos.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
