#include "llama-android.h"
#include <android/log.h>
#include <chrono>
#include <vector>
#include <sstream>
#include <iostream>

// Include llama.cpp headers
#include "llama.h"
#include "common.h"

// Utility functions implementation
std::string jstring_to_string(JNIEnv* env, jstring jstr) {
    if (jstr == nullptr) return "";

    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

jstring string_to_jstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

void log_android(const std::string& tag, const std::string& message) {
    __android_log_print(ANDROID_LOG_INFO, tag.c_str(), "%s", message.c_str());
}

// Sister-specific prompting for Dream Assistant
std::string create_sister_prompt(const std::string& user_input) {
    return "Eres el Dream Assistant, la compañera perfecta para mi hermana emprendedora. "
           "Ella tiene dificultades del habla pero sueña con crear su plataforma digital. "
           "Responde de manera cariñosa, motivacional y práctica. "
           "Entiende que ella necesita apoyo emocional y técnico para lograr sus metas.\n\n"
           "Usuario: " + user_input + "\n"
                                      "Dream Assistant: ";
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_dreamassistant_ai_LlamaEngine_initializeModel(JNIEnv *env, jobject thiz, jstring modelPath) {
    log_android(LOG_TAG, "🚀 Initializing Sister's Dream Assistant Model...");

    std::string path = jstring_to_string(env, modelPath);
    log_android(LOG_TAG, "Model path: " + path);

    // Create model wrapper
    auto* wrapper = new LlamaModelWrapper();
    wrapper->model_path = path;

    try {
        // Initialize llama backend
        llama_backend_init();
        llama_numa_init(GGML_NUMA_STRATEGY_DISABLED);

        // Model parameters optimized for sister's use case
        llama_model_params model_params = llama_model_default_params();
        model_params.n_gpu_layers = 0; // CPU only for Android
        model_params.use_mmap = true;
        model_params.use_mlock = false;

        // Load the model
        wrapper->model = llama_load_model_from_file(path.c_str(), model_params);
        if (wrapper->model == nullptr) {
            log_android(LOG_TAG, "❌ Failed to load model");
            delete wrapper;
            return 0;
        }

        // Context parameters for Dream Assistant
        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.seed = 1234;
        ctx_params.n_ctx = MAX_CONTEXT_LENGTH;
        ctx_params.n_threads = 4; // Optimize for mobile
        ctx_params.n_threads_batch = 2;

        // Create context
        wrapper->ctx = llama_new_context_with_model((llama_model*)wrapper->model, ctx_params);
        if (wrapper->ctx == nullptr) {
            log_android(LOG_TAG, "❌ Failed to create context");
            llama_free_model((llama_model*)wrapper->model);
            delete wrapper;
            return 0;
        }

        wrapper->initialized = true;
        wrapper->vocab_size = llama_n_vocab((llama_model*)wrapper->model);
        wrapper->context_size = llama_n_ctx((llama_context*)wrapper->ctx);

        log_android(LOG_TAG, "✅ Dream Assistant Model Loaded Successfully!");
        log_android(LOG_TAG, "📊 Vocab size: " + std::to_string(wrapper->vocab_size));
        log_android(LOG_TAG, "📊 Context size: " + std::to_string(wrapper->context_size));
        log_android(LOG_TAG, "💕 Sister's personalized AI companion is ready!");

        return reinterpret_cast<jlong>(wrapper);

    } catch (const std::exception& e) {
        log_android(LOG_TAG, "❌ Exception during model initialization: " + std::string(e.what()));
        delete wrapper;
        return 0;
    }
}

JNIEXPORT jstring JNICALL
Java_com_dreamassistant_ai_LlamaEngine_generateResponse(JNIEnv *env, jobject thiz, jlong modelPtr, jstring prompt) {
    if (modelPtr == 0) {
        log_android(LOG_TAG, "❌ Model not initialized");
        return string_to_jstring(env, "Lo siento, el modelo no está inicializado. 😔");
    }

    auto* wrapper = reinterpret_cast<LlamaModelWrapper*>(modelPtr);
    if (!wrapper->initialized) {
        log_android(LOG_TAG, "❌ Model wrapper not initialized");
        return string_to_jstring(env, "El Dream Assistant está despertando... inténtalo de nuevo. ✨");
    }

    std::string user_input = jstring_to_string(env, prompt);
    log_android(LOG_TAG, "👤 Sister's input: " + user_input);

    // Create sister-specific prompt
    std::string full_prompt = create_sister_prompt(user_input);

    auto start_time = std::chrono::high_resolution_clock::now();

    try {
        // Tokenize the prompt
        std::vector<llama_token> tokens;
        tokens.resize(full_prompt.length() + 1);
        int n_tokens = llama_tokenize(
                (llama_model*)wrapper->model,
                full_prompt.c_str(),
                full_prompt.length(),
                tokens.data(),
                tokens.size(),
                true, // add_special
                true  // parse_special
        );
        tokens.resize(n_tokens);

        log_android(LOG_TAG, "🔤 Tokenized " + std::to_string(n_tokens) + " tokens");

        // Clear previous context
        llama_kv_cache_clear((llama_context*)wrapper->ctx);

        // Evaluate the prompt
        if (llama_decode((llama_context*)wrapper->ctx, llama_batch_get_one(tokens.data(), n_tokens)) != 0) {
            log_android(LOG_TAG, "❌ Failed to evaluate prompt");
            return string_to_jstring(env, "Disculpa, tuve un problema procesando tu mensaje. 😅");
        }

        // Generate response
        std::string response = "";
        int max_tokens = 150; // Balanced for mobile performance

        for (int i = 0; i < max_tokens; i++) {
            // Get logits and sample next token
            float* logits = llama_get_logits((llama_context*)wrapper->ctx);

            // Sample with Dream Assistant personality parameters
            llama_token next_token = llama_sample_token_greedy(
                    (llama_context*)wrapper->ctx,
                    nullptr
            );

            // Check for end of sequence
            if (llama_token_is_eog((llama_model*)wrapper->model, next_token)) {
                break;
            }

            // Convert token to string
            char token_str[256];
            int token_len = llama_token_to_piece(
                    (llama_model*)wrapper->model,
                    next_token,
                    token_str,
                    sizeof(token_str),
                    0,
                    true
            );

            if (token_len > 0) {
                response.append(token_str, token_len);
            }

            // Evaluate the new token
            if (llama_decode((llama_context*)wrapper->ctx, llama_batch_get_one(&next_token, 1)) != 0) {
                log_android(LOG_TAG, "❌ Failed to evaluate token");
                break;
            }

            // Stop if we detect natural conversation ending
            if (response.find(".") != std::string::npos && response.length() > 50) {
                break;
            }
        }

        auto end_time = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
        wrapper->last_inference_time = duration.count() / 1000.0f;

        // Clean up response
        if (response.empty()) {
            response = "¡Hola! Soy tu Dream Assistant. ¿En qué te puedo ayudar hoy? 😊";
        }

        log_android(LOG_TAG, "🤖 Dream Assistant response: " + response);
        log_android(LOG_TAG, "⚡ Generation time: " + std::to_string(wrapper->last_inference_time) + "s");

        return string_to_jstring(env, response);

    } catch (const std::exception& e) {
        log_android(LOG_TAG, "❌ Exception during generation: " + std::string(e.what()));
        return string_to_jstring(env, "Ups, tuve un pequeño problema. ¡Pero estoy aquí para ti! 💪");
    }
}

JNIEXPORT jboolean JNICALL
Java_com_dreamassistant_ai_LlamaEngine_isModelLoaded(JNIEnv *env, jobject thiz, jlong modelPtr) {
    if (modelPtr == 0) return JNI_FALSE;

    auto* wrapper = reinterpret_cast<LlamaModelWrapper*>(modelPtr);
    return wrapper->initialized ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_dreamassistant_ai_LlamaEngine_freeModel(JNIEnv *env, jobject thiz, jlong modelPtr) {
    if (modelPtr == 0) return;

    log_android(LOG_TAG, "🧹 Cleaning up Dream Assistant model...");

    auto* wrapper = reinterpret_cast<LlamaModelWrapper*>(modelPtr);

    if (wrapper->ctx) {
        llama_free((llama_context*)wrapper->ctx);
    }

    if (wrapper->model) {
        llama_free_model((llama_model*)wrapper->model);
    }

    llama_backend_free();

    delete wrapper;
    log_android(LOG_TAG, "✅ Dream Assistant model cleaned up");
}

JNIEXPORT jstring JNICALL
Java_com_dreamassistant_ai_LlamaEngine_getModelInfo(JNIEnv *env, jobject thiz, jlong modelPtr) {
    if (modelPtr == 0) {
        return string_to_jstring(env, "Model not loaded");
    }

    auto* wrapper = reinterpret_cast<LlamaModelWrapper*>(modelPtr);
    if (!wrapper->initialized) {
        return string_to_jstring(env, "Model not initialized");
    }

    std::ostringstream info;
    info << "Dream Assistant Model Info:\n";
    info << "- Specialized for: Sister with speech impairment\n";
    info << "- Vocab size: " << wrapper->vocab_size << "\n";
    info << "- Context size: " << wrapper->context_size << "\n";
    info << "- Model path: " << wrapper->model_path << "\n";
    info << "- Status: Ready to help! 💕";

    return string_to_jstring(env, info.str());
}

JNIEXPORT jfloat JNICALL
Java_com_dreamassistant_ai_LlamaEngine_getInferenceTime(JNIEnv *env, jobject thiz, jlong modelPtr) {
    if (modelPtr == 0) return -1.0f;

    auto* wrapper = reinterpret_cast<LlamaModelWrapper*>(modelPtr);
    return wrapper->last_inference_time;
}

JNIEXPORT jint JNICALL
Java_com_dreamassistant_ai_LlamaEngine_getTokenCount(JNIEnv *env, jobject thiz, jlong modelPtr, jstring text) {
    if (modelPtr == 0) return -1;

    auto* wrapper = reinterpret_cast<LlamaModelWrapper*>(modelPtr);
    if (!wrapper->initialized) return -1;

    std::string input = jstring_to_string(env, text);

    std::vector<llama_token> tokens;
    tokens.resize(input.length() + 1);

    int n_tokens = llama_tokenize(
            (llama_model*)wrapper->model,
            input.c_str(),
            input.length(),
            tokens.data(),
            tokens.size(),
            true,
            true
    );

    return n_tokens;
}

JNIEXPORT jstring JNICALL
Java_com_dreamassistant_ai_LlamaEngine_tokenizeText(JNIEnv *env, jobject thiz, jlong modelPtr, jstring text) {
    if (modelPtr == 0) {
        return string_to_jstring(env, "Model not loaded");
    }

    auto* wrapper = reinterpret_cast<LlamaModelWrapper*>(modelPtr);
    if (!wrapper->initialized) {
        return string_to_jstring(env, "Model not initialized");
    }

    std::string input = jstring_to_string(env, text);

    std::vector<llama_token> tokens;
    tokens.resize(input.length() + 1);

    int n_tokens = llama_tokenize(
            (llama_model*)wrapper->model,
            input.c_str(),
            input.length(),
            tokens.data(),
            tokens.size(),
            true,
            true
    );

    std::ostringstream result;
    result << "Tokens (" << n_tokens << "): ";
    for (int i = 0; i < n_tokens; i++) {
        char token_str[256];
        llama_token_to_piece(
                (llama_model*)wrapper->model,
                tokens[i],
                token_str,
                sizeof(token_str),
                0,
                true
        );
        result << "[" << tokens[i] << "]" << token_str << " ";
    }

    return string_to_jstring(env, result.str());
}

} // extern "C"