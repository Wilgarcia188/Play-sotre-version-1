# PDF Suite

App Android nativa (Kotlin + Jetpack Compose) para trabajar con PDFs **en el dispositivo**, sin subir archivos a ningún servidor.

## Funciones (v1)

- **Ver PDF**: abre cualquier PDF del dispositivo y lo muestra página por página, con zoom (pellizcar para acercar).
- **Unir PDFs**: elegí varios archivos, reordenalos y combinalos en un solo PDF.
- **Separar / extraer páginas**: elegí un PDF, mirá miniaturas de todas sus páginas, seleccioná algunas y generá un PDF nuevo solo con esas páginas (o con el resto).
- **Reconocer texto (OCR)**: extrae el texto de un PDF escaneado página por página, 100% en el dispositivo (sin conexión). El texto se puede copiar o guardar como `.txt`.

Todo el acceso a archivos usa el selector del sistema (Storage Access Framework), así que la app no pide permisos de almacenamiento.

## Stack técnico

- Kotlin + Jetpack Compose (Material 3)
- Navigation Compose
- [`PdfBox-Android`](https://github.com/TomRoush/PdfBox-Android) para unir/separar páginas
- `android.graphics.pdf.PdfRenderer` (nativo de Android) para el visor y las miniaturas
- [`ML Kit Text Recognition`](https://developers.google.com/ml-kit/vision/text-recognition) (modelo incluido en la app) para el OCR

## Cómo compilarlo

Este proyecto se generó y revisó en un entorno sin SDK de Android instalado (no se pudo ejecutar `./gradlew assembleDebug` acá). Para compilarlo y probarlo:

1. Abrí la carpeta del repo con **Android Studio** (Koala o más nuevo).
2. Dejá que Android Studio sincronice Gradle y descargue el SDK/plataforma 34 si hace falta.
3. Ejecutá la app en un emulador o dispositivo (`Run ▶`), o generá un APK con `./gradlew assembleDebug`.

Requisitos: minSdk 24 (Android 7.0), compileSdk/targetSdk 34.

## Próximos pasos sugeridos

- Anotaciones (resaltar, texto, firma) y marca de agua.
- Comprimir PDF y convertir imagen ↔ PDF.
