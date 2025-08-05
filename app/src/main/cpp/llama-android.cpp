#include <jni.h>
#include <string>
#include <android/log.h>
#include <memory>
#include <vector>

// REAL llama.cpp includes
#include "llama.cpp/llama.h"
#include "llama.cpp/common/common.h"

// Android logging
#define TAG "SisterModelNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Sister's REAL model context with llama.cpp
struct SisterModelContext {
    std::string model_path;
    bool is_loaded = false;
    size_t model_size = 0;

    // REAL llama.cpp objects
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;

    // Sister-specific parameters
    float temperature = 0.7f;
    float top_p = 0.9f;
    int top_k = 40;
    int max_tokens = 150;

    // Performance tracking
    int total_inferences = 0;
    long total_inference_time_ms = 0;
    int successful_inferences = 0;
};

static std::unique_ptr<SisterModelContext> g_sister_model = nullptr;

extern "C" {

// Initialize Sister's personalized REAL model
JNIEXPORT jboolean JNICALL
Java_com_example_dreamassistant_ai_RealModelLoader_nativeLoadModel(
        JNIEnv *env,
        jobject /* this */,
        jstring model_path
) {
    const char *path = env->GetStringUTFChars(model_path, 0);

    LOGI("🚀 Loading Sister's REAL Gemma 3n model: %s", path);

    try {
        // Initialize llama.cpp
        llama_backend_init(false);

        // Initialize sister's model context
        g_sister_model = std::make_unique<SisterModelContext>();
        g_sister_model->model_path = std::string(path);

        // Check if model file exists
        FILE *file = fopen(path, "rb");
        if (file == nullptr) {
            LOGE("❌ Could not open sister's GGUF model file: %s", path);
            env->ReleaseStringUTFChars(model_path, path);
            return JNI_FALSE;
        }

        // Get file size
        fseek(file, 0, SEEK_END);
        g_sister_model->model_size = ftell(file);
        fclose(file);

        LOGI("📁 Sister's GGUF model size: %.1f MB", g_sister_model->model_size / (1024.0 * 1024.0));

        // REAL llama.cpp model loading
        auto model_params = llama_model_default_params();
        model_params.n_gpu_layers = 0; // CPU only for Android
        model_params.use_mmap = true;
        model_params.use_mlock = false;

        LOGI("🧠 Loading GGUF model with llama.cpp...");
        g_sister_model->model = llama_load_model_from_file(path, model_params);

        if (g_sister_model->model == nullptr) {
            LOGE("❌ Failed to load sister's GGUF model with llama.cpp");
            env->ReleaseStringUTFChars(model_path, path);
            return JNI_FALSE;
        }

        // Create context for inference
        auto ctx_params = llama_context_default_params();
        ctx_params.seed = 1234;
        ctx_params.n_ctx = 2048;
        ctx_params.n_batch = 512;
        ctx_params.n_threads = 4;
        ctx_params.f16_kv = true;

        LOGI("⚙️ Creating inference context...");
        g_sister_model->ctx = llama_new_context_with_model(g_sister_model->model, ctx_params);

        if (g_sister_model->ctx == nullptr) {
            LOGE("❌ Failed to create llama.cpp context");
            llama_free_model(g_sister_model->model);
            g_sister_model->model = nullptr;
            env->ReleaseStringUTFChars(model_path, path);
            return JNI_FALSE;
        }

        g_sister_model->is_loaded = true;

        LOGI("🎉 SUCCESS! Sister's REAL Gemma 3n model loaded with llama.cpp!");
        LOGI("🎯 Model ready for her 202+ voice sample patterns");
        LOGI("✨ Context size: %d, Vocab size: %d",
             llama_n_ctx(g_sister_model->ctx),
             llama_n_vocab(g_sister_model->model));

        env->ReleaseStringUTFChars(model_path, path);
        return JNI_TRUE;

    } catch (const std::exception& e) {
        LOGE("❌ Exception loading sister's model: %s", e.what());
        env->ReleaseStringUTFChars(model_path, path);
        return JNI_FALSE;
    }
}

// Generate response using Sister's REAL trained model
JNIEXPORT jstring JNICALL
Java_com_example_dreamassistant_ai_RealModelLoader_nativeGenerateResponse(
        JNIEnv *env,
        jobject /* this */,
        jstring prompt
) {
    if (!g_sister_model || !g_sister_model->is_loaded || !g_sister_model->ctx) {
        LOGE("❌ Sister's REAL model not loaded");
        return env->NewStringUTF("");
    }

    const char *input_prompt = env->GetStringUTFChars(prompt, 0);
    LOGI("🤖 Sister's REAL model generating for: '%.30s...'", input_prompt);

    auto start_time = std::chrono::high_resolution_clock::now();

    try {
        std::string prompt_str(input_prompt);

        // Tokenize the input prompt
        std::vector<llama_token> tokens = llama_tokenize(g_sister_model->ctx, prompt_str, true);

        if (tokens.empty()) {
            LOGE("❌ Failed to tokenize sister's input");
            env->ReleaseStringUTFChars(prompt, input_prompt);
            return env->NewStringUTF("");
        }

        LOGI("🔤 Tokenized input: %zu tokens", tokens.size());

        // Evaluate the prompt
        if (llama_eval(g_sister_model->ctx, tokens.data(), tokens.size(), 0, 4) != 0) {
            LOGE("❌ Failed to evaluate prompt with sister's model");
            env->ReleaseStringUTFChars(prompt, input_prompt);
            return env->NewStringUTF("");
        }

        // Generate response tokens
        std::string response = "";
        std::vector<llama_token> response_tokens;

        for (int i = 0; i < g_sister_model->max_tokens; i++) {
            // Sample next token
            llama_token next_token = llama_sample_token_greedy(g_sister_model->ctx, nullptr);

            // Check for end of sequence
            if (next_token == llama_token_eos(g_sister_model->model)) {
                LOGI("🏁 End of sequence reached at token %d", i);
                break;
            }

            response_tokens.push_back(next_token);

            // Convert token to text
            std::string token_str = llama_token_to_piece(g_sister_model->ctx, next_token);
            response += token_str;

            // Evaluate the new token
            if (llama_eval(g_sister_model->ctx, &next_token, 1, tokens.size() + i, 4) != 0) {
                LOGE("❌ Failed to evaluate next token");
                break;
            }

            // Check for natural stopping points
            if (token_str.find("<end_of_turn>") != std::string::npos) {
                LOGI("🛑 Natural stop detected at token %d", i);
                break;
            }
        }

        // Clean up the response
        response = cleanSisterResponse(response);

        auto end_time = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);

        // Update statistics
        g_sister_model->total_inferences++;
        g_sister_model->total_inference_time_ms += duration.count();
        if (!response.empty()) {
            g_sister_model->successful_inferences++;
        }

        LOGI("✅ Sister's REAL model generated (%ld ms): '%.50s...'",
             duration.count(), response.c_str());

        env->ReleaseStringUTFChars(prompt, input_prompt);
        return env->NewStringUTF(response.c_str());

    } catch (const std::exception& e) {
        LOGE("❌ Exception during REAL inference: %s", e.what());
        env->ReleaseStringUTFChars(prompt, input_prompt);
        return env->NewStringUTF("");
    }
}

// Helper function to clean sister's response
std::string cleanSisterResponse(const std::string& raw_response) {
    std::string cleaned = raw_response;

    // Remove special tokens
    size_t pos = cleaned.find("<end_of_turn>");
    if (pos != std::string::npos) {
        cleaned = cleaned.substr(0, pos);
    }

    pos = cleaned.find("<start_of_turn>model");
    if (pos != std::string::npos) {
        cleaned = cleaned.substr(pos + 19); // Skip "<start_of_turn>model"
    }

    // Trim whitespace
    cleaned.erase(0, cleaned.find_first_not_of(" \t\n\r"));
    cleaned.erase(cleaned.find_last_not_of(" \t\n\r") + 1);

    return cleaned;
}

// Check if REAL model is loaded
JNIEXPORT jboolean JNICALL
Java_com_example_dreamassistant_ai_RealModelLoader_nativeIsModelLoaded(
        JNIEnv *env,
        jobject /* this */
) {
    return (g_sister_model &&
            g_sister_model->is_loaded &&
            g_sister_model->model != nullptr &&
            g_sister_model->ctx != nullptr) ? JNI_TRUE : JNI_FALSE;
}

// Get REAL model info
JNIEXPORT jstring JNICALL
Java_com_example_dreamassistant_ai_RealModelLoader_nativeGetModelInfo(
        JNIEnv *env,
        jobject /* this */
) {
    if (!g_sister_model || !g_sister_model->is_loaded) {
        return env->NewStringUTF("Sister's REAL model not loaded");
    }

    char info[1024];
    double avg_time = g_sister_model->total_inferences > 0 ?
                      (double)g_sister_model->total_inference_time_ms / g_sister_model->total_inferences : 0.0;

    snprintf(info, sizeof(info),
             "Sister's REAL Gemma 3n Model (llama.cpp)\n"
             "Size: %.1f MB\n"
             "Path: %s\n"
             "Context: %d tokens\n"
             "Vocab: %d tokens\n"
             "Inferences: %d\n"
             "Success rate: %.1f%%\n"
             "Avg time: %.1f ms\n"
             "Status: Ready for her voice! 🎯",
             g_sister_model->model_size / (1024.0 * 1024.0),
             g_sister_model->model_path.c_str(),
             g_sister_model->ctx ? llama_n_ctx(g_sister_model->ctx) : 0,
             g_sister_model->model ? llama_n_vocab(g_sister_model->model) : 0,
             g_sister_model->total_inferences,
             g_sister_model->total_inferences > 0 ?
             (double)g_sister_model->successful_inferences * 100.0 / g_sister_model->total_inferences : 0.0,
             avg_time
    );

    return env->NewStringUTF(info);
}

// Cleanup REAL model
JNIEXPORT void JNICALL
Java_com_example_dreamassistant_ai_RealModelLoader_nativeCleanup(
        JNIEnv *env,
        jobject /* this */
) {
    LOGI("🧹 Cleaning up sister's REAL model");
    if (g_sister_model) {
        if (g_sister_model->ctx) {
            llama_free(g_sister_model->ctx);
            g_sister_model->ctx = nullptr;
        }
        if (g_sister_model->model) {
            llama_free_model(g_sister_model->model);
            g_sister_model->model = nullptr;
        }
        g_sister_model.reset();
    }
    llama_backend_free();
}

} // extern "C"