# TV Home - launcher estilo tvOS para Google TV

Dock con tus apps favoritas abajo, reloj arriba a la derecha y fondo azul animado.

## IMPORTANTE
Este codigo fue escrito pero NO fue compilado ni probado en una TV real
(el entorno donde se escribio no tiene el SDK de Android). Puede haber
errores de compilacion o detalles visuales por ajustar.

## Como compilarlo (elegi UNA opcion)

### Opcion A - Android Studio (en tu PC)
1. Instala Android Studio (gratis).
2. File > Open > elegi esta carpeta (la que tiene settings.gradle).
3. Espera a que termine la sincronizacion de Gradle.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. El APK queda en app/build/outputs/apk/debug/app-debug.apk

### Opcion B - GitHub Actions (sin instalar nada)
1. Crea una cuenta en github.com y un repositorio nuevo.
2. Subi TODO el contenido de esta carpeta (incluida la carpeta oculta .github).
3. Pestana Actions > "Build APK" > Run workflow.
4. Cuando termine (unos minutos), abri la ejecucion y descarga "tvhome-apk".

## Instalar en la TV
1. Copia app-debug.apk a la carpeta donde esta adb.exe junto con instalar_launcher_propio.bat
2. Ejecuta instalar_launcher_propio.bat
3. Ejecuta launcher_paso1_hacer_principal.bat y cuando pida el componente escribi:
   com.example.tvhome/.HomeActivity
4. Usalo unos dias. Si funciona bien: launcher_paso2_ocultar_original.bat
5. Para volver atras: launcher_restaurar_original.bat

## Manejo
- OK sobre una app: la abre.
- Mantener OK sobre una app del dock: la quita. En "Todas las apps": la agrega al dock.
- Boton Home: vuelve al dock. Atras en el inicio no hace nada.
- Para cambiar las apps por defecto del dock: lista DEFAULT_FAVS en HomeActivity.java
- Para un fondo estatico (menos consumo): ANIMATE = false en AnimatedBackground.java
