# whisper.cpp dentro de CoinDen

- **Origen:** https://github.com/ggml-org/whisper.cpp, etiqueta `v1.9.4` (11 de septiembre de 2026).
- **Archivo descargado:** `whisper.cpp-1.9.4.tar.gz`, SHA-256 `57e280cee375ab02425b806ad5146b99f6eb9357e3c2b31357c8a6af2e2e44ae`.
- **Licencia:** MIT (ver `LICENSE`).

Se copiaron solo las partes que compila la app Android, sin cambios en el código:

- `include/`, `src/whisper.cpp` y sus encabezados. Se quitaron los aceleradores `coreml/`, `openvino/` y `vitisai/`.
- `ggml/CMakeLists.txt`, `ggml/cmake/`, `ggml/include/` y, de `ggml/src/`, los archivos base y `ggml-cpu/`. No se copiaron los backends de GPU (CUDA, Vulkan, Metal, etc.), que el teléfono no usa.

Para actualizar: descargar la nueva etiqueta, repetir la misma selección, anotar aquí la versión y la huella, y correr las pruebas instrumentadas de voz.
