# PDF Suite

App Android nativa (Kotlin + Jetpack Compose) para trabajar con PDFs **en el dispositivo**, sin subir archivos a ningún servidor.

## Funciones (v1)

- **Ver PDF**: abre cualquier PDF del dispositivo y lo muestra página por página, con zoom (pellizcar para acercar).
- **Unir PDFs**: elegí varios archivos, reordenalos y combinalos en un solo PDF.
- **Separar / extraer páginas**: elegí un PDF, mirá miniaturas de todas sus páginas, seleccioná algunas y generá un PDF nuevo solo con esas páginas (o con el resto).
- **Reconocer texto (OCR)**: extrae el texto de un PDF escaneado página por página, 100% en el dispositivo (sin conexión). El texto se puede copiar o guardar como `.txt`.
- **Comprimir PDF**: recomprime cada página como JPEG (alta calidad / equilibrado / máxima compresión) para reducir el tamaño de PDFs escaneados o con muchas imágenes. Muestra el tamaño original y el final.
- **Convertir imagen ↔ PDF**: armá un PDF a partir de una o más fotos, o exportá las páginas de un PDF como imágenes JPEG a una carpeta elegida.
- **Marca de agua**: agrega un texto diagonal semitransparente a todas las páginas, preservando el contenido original del PDF (no lo rasteriza).
- **Escanear documento**: sacá una foto con la cámara; se detectan los bordes de la hoja, se corrige la perspectiva y se puede elegir un filtro de color (automático, blanco y negro, foto) antes de guardar como PDF.

Todo el acceso a archivos usa el selector del sistema (Storage Access Framework), así que la app no pide permisos de almacenamiento (salvo la cámara, solo para la función de escanear).

## Stack técnico

- Kotlin + Jetpack Compose (Material 3)
- Navigation Compose
- [`PdfBox-Android`](https://github.com/TomRoush/PdfBox-Android) para unir/separar páginas, comprimir, convertir imagen↔PDF y marca de agua
- `android.graphics.pdf.PdfRenderer` (nativo de Android) para el visor y las miniaturas
- [`ML Kit Text Recognition`](https://developers.google.com/ml-kit/vision/text-recognition) (modelo incluido en la app) para el OCR
- [`ML Kit Document Scanner`](https://developers.google.com/ml-kit/vision/doc-scanner) (Google Play Services) para escanear con la cámara

## Cómo compilarlo

Este proyecto se generó y revisó en un entorno sin SDK de Android instalado (no se pudo ejecutar `./gradlew assembleDebug` acá). Para compilarlo y probarlo:

1. Abrí la carpeta del repo con **Android Studio** (Koala o más nuevo).
2. Dejá que Android Studio sincronice Gradle y descargue el SDK/plataforma 34 si hace falta.
3. Ejecutá la app en un emulador o dispositivo (`Run ▶`), o generá un APK con `./gradlew assembleDebug`.

Requisitos: minSdk 24 (Android 7.0), compileSdk/targetSdk 34.

La función "Escanear documento" necesita Google Play Services (viene instalado en casi todos los equipos Android reales) y puede descargar un módulo pequeño la primera vez que se usa; el resto de las funciones son 100% sin conexión.

## Próximos pasos sugeridos

- Anotaciones a mano alzada (resaltar, dibujar, texto libre posicionable) y firma.
