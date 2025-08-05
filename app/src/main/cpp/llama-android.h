#ifndef LLAMA_ANDROID_H
#define LLAMA_ANDROID_H

#include <jni.h>
#include <string>
#include <memory>

/**
 * llama-android.h - JNI Interface for Sister's Dream Assistant
 *
 * This header defines the native interface for loading and running
 * Sister's personalized Gemma 3n GGUF model using llama.cpp
 */

#ifdef __cplusplus
extern "C" {
#endif

// ===== SISTER'S MODEL MANAGEMENT =====

/**
 * Load Sister's personalized Gemma 3n model from GGUF file
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @param model_path Path to sister's GGUF model file
 * @return JNI_TRUE if model loaded successfully, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_example_dreamassistant_ai_RealModelLoader_nativeLoadModel(
        JNIEnv *env,
        jobject thiz,
        jstring model_path
);

/**
 * Generate response using Sister's trained model
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @param prompt Input prompt formatted for Gemma 3n
 * @return Generated response string, empty if error
 */
JNIEXPORT jstring JNICALL
Java_com_example_dreamassistant_ai_RealModelLoader_nativeGenerateResponse(
        JNIEnv *env,
        jobject thiz,
        jstring prompt
);

/**
 * Check if Sister's model is currently loaded and ready
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @return JNI_TRUE if model is loaded, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_example_dreamassistant_ai_RealModelLoader_nativeIsModelLoaded(
        JNIEnv *env,
        jobject thiz
);

/**
 * Get detailed information about Sister's loaded model
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @return Model information string (size, path, status)
 */
JNIEXPORT jstring JNICALL
Java_com_example_dreamassistant_ai_RealModelLoader_nativeGetModelInfo(
        JNIEnv *env,
        jobject thiz
);

/**
 * Clean up Sister's model and free all resources
 *
 * @param env JNI environment
 * @param thiz Java object instance
 */
JNIEXPORT void JNICALL
Java_com_example_dreamassistant_ai_RealModelLoader_nativeCleanup(
        JNIEnv *env,
jobject thiz
);

// ===== ADVANCED MODEL FUNCTIONS (Optional) =====

/**
 * Set generation parameters for Sister's model
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @param temperature Sampling temperature (0.1 - 2.0)
 * @param top_p Top-p sampling parameter (0.1 - 1.0)
 * @param top_k Top-k sampling parameter (1 - 100)
 * @param max_tokens Maximum tokens to generate
 * @return JNI_TRUE if parameters set successfully
 */
JNIEXPORT jboolean JNICALL
        Java_com_example_dreamassistant_ai_RealModelLoader_nativeSetGenerationParams(
        JNIEnv *env,
        jobject thiz,
jfloat temperature,
        jfloat top_p,
jint top_k,
        jint max_tokens
);

/**
 * Get model performance statistics
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @return JSON string with performance stats
 */
JNIEXPORT jstring JNICALL
        Java_com_example_dreamassistant_ai_RealModelLoader_nativeGetPerformanceStats(
        JNIEnv *env,
        jobject thiz
);

/**
 * Preload model into memory for faster inference
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @return JNI_TRUE if preload successful
 */
JNIEXPORT jboolean JNICALL
        Java_com_example_dreamassistant_ai_RealModelLoader_nativePreloadModel(
        JNIEnv *env,
        jobject thiz
);

#ifdef __cplusplus
}
#endif

// ===== C++ INTERNAL STRUCTURES =====

#ifdef __cplusplus

/**
 * Sister's Model Context - Internal C++ structure
 * This holds all the state for her personalized model
 */
struct SisterModelContext {
    std::string model_path;
    bool is_loaded = false;
    size_t model_size = 0;

    // Model parameters optimized for Sister's voice
    float temperature = 0.7f;      // Warm, natural responses
    float top_p = 0.9f;           // Good diversity
    int top_k = 40;               // Balanced creativity
    int max_tokens = 150;         // Concise for TTS

    // Performance tracking
    int total_inferences = 0;
    long total_inference_time_ms = 0;
    int successful_inferences = 0;

    // TODO: Add llama.cpp context when integrating
    // llama_context* llama_ctx = nullptr;
    // llama_model* llama_model = nullptr;

    // Sister-specific optimizations
    bool use_sister_optimizations = true;
    bool enable_speech_patterns = true;
    bool prioritize_emotional_support = true;
};

/**
 * Generation Parameters for Sister's Model
 */
struct GenerationParams {
    float temperature = 0.7f;     // Warmth in responses
    float top_p = 0.9f;          // Diversity
    int top_k = 40;              // Creativity limit
    int max_tokens = 150;        // Perfect for TTS
    bool use_emotional_boost = true;
    bool enable_business_context = true;
};

/**
 * Performance Statistics
 */
struct PerformanceStats {
    long total_inference_time_ms = 0;
    int total_inferences = 0;
    int successful_inferences = 0;
    double average_inference_time_ms = 0.0;
    int success_rate_percent = 0;
    size_t memory_usage_bytes = 0;
};

// ===== INTERNAL HELPER FUNCTIONS =====

/**
 * Initialize Sister's model context with optimal parameters
 */
bool initialize_sister_model_context();

/**
 * Optimize prompt for Sister's speech patterns and personality
 */
std::string optimize_prompt_for_sister(const std::string& input);

/**
 * Post-process model output to match Sister's assistant personality
 */
std::string post_process_sister_response(const std::string& raw_output);

/**
 * Validate GGUF file format and Sister's model structure
 */
bool validate_sister_gguf_model(const std::string& file_path);

/**
 * Apply Sister-specific model optimizations
 */
void apply_sister_model_optimizations();

/**
 * Update performance statistics
 */
void update_performance_stats(long inference_time_ms, bool success);

#endif // __cplusplus

// ===== CONSTANTS FOR SISTER'S MODEL =====

// Model configuration
#define SISTER_MODEL_VERSION "1.0"
#define SISTER_TRAINING_SAMPLES 202
#define SISTER_MODEL_TYPE "Gemma3n-GGUF"

// Generation parameters optimized for Sister
#define SISTER_DEFAULT_TEMPERATURE 0.7f
#define SISTER_DEFAULT_TOP_P 0.9f
#define SISTER_DEFAULT_TOP_K 40
#define SISTER_DEFAULT_MAX_TOKENS 150

// Memory limits
#define SISTER_MAX_CONTEXT_LENGTH 2048
#define SISTER_MAX_RESPONSE_LENGTH 300
#define SISTER_MIN_MODEL_SIZE_MB 50

// Error codes
#define SISTER_ERROR_MODEL_NOT_FOUND -1
#define SISTER_ERROR_INVALID_GGUF -2
#define SISTER_ERROR_MEMORY_ALLOCATION -3
#define SISTER_ERROR_INFERENCE_FAILED -4
#define SISTER_ERROR_CONTEXT_TOO_LONG -5

// Success codes
#define SISTER_SUCCESS 0
#define SISTER_SUCCESS_NATIVE_LOADED 1
#define SISTER_SUCCESS_FALLBACK_ACTIVE 2

// Logging tags
#define SISTER_LOG_TAG "SisterModelNative"
#define SISTER_PERFORMANCE_TAG "SisterModelPerf"
#define SISTER_INFERENCE_TAG "SisterInference"

#endif // LLAMA_ANDROID_H

/*
 * ===== INTEGRATION NOTES =====
 *
 * To complete the llama.cpp integration:
 *
 * 1. Add llama.cpp submodule:
 *    git submodule add https://github.com/ggerganov/llama.cpp.git app/src/main/cpp/llama.cpp
 *
 * 2. Update CMakeLists.txt to include llama.cpp:
 *    add_subdirectory(llama.cpp)
 *    target_link_libraries(dream-assistant-native llama)
 *
 * 3. Include llama.cpp headers in llama-android.cpp:
 *    #include "llama.cpp/llama.h"
 *
 * 4. Replace simulation with real llama.cpp calls:
 *    - llama_init_from_file() for model loading
 *    - llama_tokenize() for input processing
 *    - llama_eval() for inference
 *    - llama_sample() for token generation
 *
 * 5. Optimize for Sister's model:
 *    - Use her specific GGUF model file
 *    - Apply conversation formatting from training
 *    - Tune parameters for her voice patterns
 *
 * For the hackathon, the current simulation provides
 * realistic personalized responses that demonstrate
 * the concept perfectly!
 */