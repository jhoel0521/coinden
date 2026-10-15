// Puente mínimo entre Kotlin (WhisperNative) y whisper.cpp: cargar un modelo, transcribir PCM
// de 16 kHz en un idioma fijo y liberar. Sin red: todo corre en el teléfono.
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include "whisper.h"

JNIEXPORT jlong JNICALL
Java_com_coinden_app_perception_WhisperNative_load(JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = (*env)->GetStringUTFChars(env, model_path, NULL);
    struct whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, params);
    (*env)->ReleaseStringUTFChars(env, model_path, path);
    return (jlong) ctx;
}

JNIEXPORT jstring JNICALL
Java_com_coinden_app_perception_WhisperNative_transcribe(
        JNIEnv *env, jobject thiz, jlong handle, jfloatArray samples, jstring language, jint threads) {
    struct whisper_context *ctx = (struct whisper_context *) handle;
    if (ctx == NULL) return NULL;

    const char *lang = (*env)->GetStringUTFChars(env, language, NULL);
    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = lang;
    params.translate = false;
    params.no_timestamps = true;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_special = false;
    params.print_timestamps = false;
    params.n_threads = threads;

    jsize count = (*env)->GetArrayLength(env, samples);
    jfloat *data = (*env)->GetFloatArrayElements(env, samples, NULL);
    int status = whisper_full(ctx, params, data, count);
    (*env)->ReleaseFloatArrayElements(env, samples, data, JNI_ABORT);
    (*env)->ReleaseStringUTFChars(env, language, lang);
    if (status != 0) return NULL;

    size_t size = 1;
    int segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < segments; i++) size += strlen(whisper_full_get_segment_text(ctx, i));
    char *text = (char *) calloc(size, 1);
    if (text == NULL) return NULL;
    for (int i = 0; i < segments; i++) strcat(text, whisper_full_get_segment_text(ctx, i));
    jstring result = (*env)->NewStringUTF(env, text);
    free(text);
    return result;
}

JNIEXPORT void JNICALL
Java_com_coinden_app_perception_WhisperNative_free(JNIEnv *env, jobject thiz, jlong handle) {
    if (handle != 0) whisper_free((struct whisper_context *) handle);
}
