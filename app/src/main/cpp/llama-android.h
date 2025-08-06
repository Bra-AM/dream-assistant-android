#ifndef LLAMA_ANDROID_H
#define LLAMA_ANDROID_H

#include <jni.h>
#include <string>
#include <memory>

extern "C" {

// JNI function declarations
JNIEXPORT jlong JNICALL
Java_com_dreamassistant_ai_LlamaEngine_initializeModel(JNIEnv *env, jobject thiz, jstring modelPath);

JNIEXPORT jstring JNICALL
Java_com_dreamassistant_ai_LlamaEngine_generateResponse(JNIEnv *env, jobject thiz, jlong modelPtr, jstring prompt);

JNIEXPORT jboolean JNICALL
Java_com_dreamassistant_ai_LlamaEngine_isModelLoaded(JNIEnv *env, jobject thiz, jlong modelPtr);

JNIEXPORT void JNICALL
Java_com_dreamassistant_ai_LlamaEngine_freeModel(JNIEnv *env, jobject thiz, jlong modelPtr);

JNIEXPORT jstring JNICALL
Java_com_dreamassistant_ai_LlamaEngine_getModelInfo(JNIEnv *env, jobject thiz, jlong modelPtr);

JNIEXPORT jfloat JNICALL
Java_com_dreamassistant_ai_LlamaEngine_getInferenceTime(JNIEnv *env, jobject thiz, jlong modelPtr);

JNIEXPORT jint JNICALL
Java_com_dreamassistant_ai_LlamaEngine_getTokenCount(JNIEnv *env, jobject thiz, jlong modelPtr, jstring text);

JNIEXPORT jstring JNICALL
Java_com_dreamassistant_ai_LlamaEngine_tokenizeText(JNIEnv *env, jobject thiz, jlong modelPtr, jstring text);

} // extern "C"

// Internal structures and classes
struct LlamaModelWrapper {
    void* ctx;
    void* model;
    std::string model_path;
    bool initialized;
    float last_inference_time;
    int vocab_size;
    int context_size;

    LlamaModelWrapper() : ctx(nullptr), model(nullptr), initialized(false),
                          last_inference_time(0.0f), vocab_size(0), context_size(0) {}
};

// Utility functions
std::string jstring_to_string(JNIEnv* env, jstring jstr);
jstring string_to_jstring(JNIEnv* env, const std::string& str);
void log_android(const std::string& tag, const std::string& message);

// Constants
#define LOG_TAG "LlamaAndroid"
#define MAX_CONTEXT_LENGTH 2048
#define DEFAULT_TEMPERATURE 0.7f
#define DEFAULT_TOP_K 40
#define DEFAULT_TOP_P 0.9f

#endif // LLAMA_ANDROID_H