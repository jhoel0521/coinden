# Puente JNI de CoinDen con whisper.cpp (MIT), compilado solo para CPU.
cmake_minimum_required(VERSION 3.22.1)
project(coinden_whisper C CXX)

set(CMAKE_CXX_STANDARD 17)
set(WHISPER_DIR ${CMAKE_SOURCE_DIR}/../../../../third_party/whisper.cpp)

# ggml es muy lento sin optimizar: también en Debug se compila con -O3.
set(CMAKE_C_FLAGS_DEBUG "-O3")
set(CMAKE_CXX_FLAGS_DEBUG "-O3")

# ggml solo con el backend de CPU, sin OpenMP ni detección de la CPU del equipo que compila.
set(GGML_CPU ON CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
add_subdirectory(${WHISPER_DIR}/ggml ggml)

add_library(coinden_whisper SHARED
    ${WHISPER_DIR}/src/whisper.cpp
    whisper_jni.c)
target_include_directories(coinden_whisper PRIVATE ${WHISPER_DIR}/include ${WHISPER_DIR}/src)
target_compile_definitions(coinden_whisper PRIVATE GGML_USE_CPU WHISPER_VERSION="1.9.4")
target_compile_options(coinden_whisper PRIVATE -O3 -fvisibility=hidden -ffunction-sections -fdata-sections)
target_link_options(coinden_whisper PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
target_link_libraries(coinden_whisper ggml log android)
