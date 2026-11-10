# Economía Inteligente — versión inicial 0.1.0

Aplicación Android nativa en Kotlin y Jetpack Compose.

## Incluye
- Inicio azul con nueve módulos y navegación.
- Laboratorio 360° en modo virtual con 40 € iniciales.
- Modo real bloqueado.
- Pantallas iniciales para los demás apartados.

## Limitaciones de esta versión
- No incluye cotizaciones en directo ni conexión bancaria o con bróker.
- No realiza compras, ventas ni transferencias.
- El diario del laboratorio todavía no conserva las notas al cerrar la aplicación.
- No incluye backend ni notificaciones push.

## Generar la APK desde GitHub
1. Sube el contenido de este ZIP a la raíz de un repositorio GitHub.
2. Abre la pestaña **Actions**.
3. Selecciona **Construir APK Android** y ejecuta el flujo con **Run workflow** (o espera a que termine el flujo automático tras subir los archivos).
4. Abre la ejecución terminada correctamente.
5. En **Artifacts**, descarga **Economia-Inteligente-debug**.
6. Descomprime el archivo descargado e instala `app-debug.apk` en tu Android.

La APK es de depuración y no está publicada en Google Play. Revisa siempre que procede de tu propio repositorio. No subas contraseñas, claves ni datos bancarios.
