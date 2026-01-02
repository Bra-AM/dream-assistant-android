# Dream Assistant – Gemma 3n Impact Challenge

A private, offline-first, voice-driven chat assistant designed to empower people with speech impairments. Fine-tuned on “sister\_dream\_assistant.gguf” using Gemma 3n and Unsloth, the app runs entirely on device, no cloud needed, preserving user privacy and accessibility.

Visit site: https://bra-am.github.io/dream-assistant-android/


**License:** This repository, including code and write-up, is released under
the **Creative Commons Attribution 4.0 International (CC BY 4.0)** license.

If you use this work, please cite:

Brady Ali Medina (2025). Dream Assistant: Fine-tuning Gemma 3n with Unsloth for speech accessibility.
GitHub: https://github.com/Bra-AM/dream-assistant-android/

License: CC BY 4.0

## The Problem

Imagine wanting to share your thoughts but feeling misunderstood every time you speak. Many people with speech impairments face this daily—voice assistants that ignore their words or garble their messages. It’s frustrating, isolating, and undermines confidence—especially when technology, meant to help, simply doesn’t hear them.

## Vision

Dream Assistant is built to help individuals with speech impairments communicate effortlessly. It enables users to:

* **Read messages aloud** (WhatsApp, SMS, or any text).
* **Send messages** by voice, reducing the need for typing.
* **Record and upload YouTube videos**, with spoken titles and descriptions, hands-free.
* **Navigate apps** and perform tasks without touching the screen.

Each user has unique goals—my sister uses it to grow her business by uploading promotional videos and sending client messages, while others might focus on everyday communication, journaling, or learning tools. Through personalization, each user gets a model trained on their own voice patterns, tailored to their unique needs—whether it’s articulation differences, accent variations, communication objectives, or entrepreneurial tasks—making technology truly accessible.

##  Why Gemma 3n for Speech Impairment?

* **On-Device Performance**: Gemma 3n’s efficient architecture lets a 4B-parameter model run with a 2B footprint, enabling real-time inference on most smartphones.
* **Privacy & Independence**: All audio and text data stay on the user’s device—critical for sensitive health or personal data. No internet means reliable use in remote or low-connectivity areas.
* **Personalization**: Fine-tuning with Unsloth allows small-data adaptation. With just \~200 audio samples, we teach the model to understand a unique speech pattern, making it ideal for various speech impairments.
* **Multimodal Potential**: Gemma 3n natively supports audio, text, and images, opening doors for future features like lip-reading, gesture recognition, or visual prompts.

##  Mission & Impact

This project demonstrates how private, personalized AI can transform accessibility:

1. **Data Collection**: We asked my sister to record 200 common phrases.
2. **Unsloth Fine-Tuning**: Using the Jupyter notebook `notebooks/Sister_Dream_Assistant_Gemma_3n_Training.ipynb`, we trained the base model (`gemma3nlu_base.gguf`) to her voice, producing `sister_dream_assistant.gguf`.
3. **On-Device Inference**: The personalized model runs locally to read WhatsApp, send messages, and speak back in her own voice.

In the long term, this pattern can be repeated easily: collect a few minutes of speech from any individual with an impairment, fine-tune offline, and deploy a custom assistant that truly understands their unique patterns.

##  How It Works

1. **Initialize**: On first run, the app copies both `sister_dream_assistant.gguf` and `gemma3nlu_base.gguf` from `assets/models/` into internal storage.
2. **Load & Fallback**: Attempts to load the personalized model; on failure, transparently falls back to the base Gemma 3n model.
3. **Voice Loop**:

   * **Listen**: `SpeechRecognitionService` captures the user’s speech.
   * **Respond**: `ChatViewModel` sends text to `LlamaEngine` and streams polled tokens.
   * **Speak**: `TextToSpeechService` vocalizes responses, then automatically restarts listening.
4. **UI**: Minimal Jetpack Compose interface with a single mic button and message bubbles—no typing required for core tasks.

##  Install & Run

1. **Clone**:

   ```bash
   git clone https://github.com/YOUR_USERNAME/dream-assistant-android.git
   cd dream-assistant-android
   ```
2. **Place Models**:

   ```bash
   mkdir -p app/src/main/assets/models
   cp path/to/sister_dream_assistant.gguf app/src/main/assets/models/
   cp path/to/gemma3nlu_base.gguf        app/src/main/assets/models/
   ```
3. **Build & Install**:

   ```bash
   cd app
   ./gradlew assembleDebug
   adb install -r build/outputs/apk/debug/app-debug.apk
   ```
4. **Grant Permissions** on first launch: Microphone & Storage.

##  Releases & Models

To keep the repo lightweight, the `.gguf` files are not tracked in Git. You can download them from our **GitHub Release v1.0** under **Assets**. See the [Releases page](https://github.com/YOUR_USERNAME/dream-assistant-android/releases).

##  Key Features

* **Privacy-First**: All inference and audio synthesis run offline—no user data leaves the device.
* **Personalization Template**: Easily swap in any `.gguf` model trained via Unsloth to adapt for different voices, accents, or impairment needs.
* **Seamless Fallback**: Automatically reverts to the base Gemma 3n model if the personalized file fails to load.
* **Hands-Free UI**: Minimal Jetpack Compose interface with a single mic button and automatic listen–speak loop.
* **Extendable Architecture**: Natively supports audio, text, and images for future multimodal enhancements.
