# CMakeLists.txt for Sister's Dream Assistant with REAL llama.cpp
cmake_minimum_required(VERSION 3.22.1)

project("dream-assistant-llama")

# C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_library(log-lib log)
find_library(android-lib android)

# llama.cpp configuration for Android
set(LLAMA_STATIC ON)
set(LLAMA_BUILD_TESTS OFF)
set(LLAMA_BUILD_EXAMPLES OFF)
set(LLAMA_BUILD_SERVER OFF)
set(LLAMA_NATIVE OFF)
set(LLAMA_LTO OFF)

# Android-specific optimizations
if(ANDROID)
    set(LLAMA_ANDROID ON)
    set(LLAMA_BLAS OFF)
    set(LLAMA_CUBLAS OFF)
    set(LLAMA_METAL OFF)

    # Enable optimizations for Sister's model
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        set(LLAMA_NATIVE ON)
        set(LLAMA_F16C ON)
    endif()
endif()

# Add llama.cpp subdirectory
add_subdirectory(llama.cpp)

# Include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR})
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# Create the native library for Sister's model
add_library(
        dream-assistant-native
        SHARED
        llama-android.cpp
)

# Link libraries - INCLUDING REAL LLAMA.CPP
target_link_libraries(
        dream-assistant-native
        llama              # REAL llama.cpp library
        ${log-lib}
        ${android-lib}
)

# Compiler flags for Android optimization
target_compile_options(dream-assistant-native PRIVATE
        -O3
        -ffast-math
        -funroll-loops
        -DANDROID
        -DLLAMA_ANDROID
        -DGGUF_SUPPORT
)

# Sister's model specific optimizations
target_compile_definitions(dream-assistant-native PRIVATE
        SISTER_MODEL_OPTIMIZATION=1
        GGUF_SUPPORT=1
        GEMMA_3N_SUPPORT=1
        LLAMA_ANDROID=1
)