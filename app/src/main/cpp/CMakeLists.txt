cmake_minimum_required(VERSION 3.22.1)

# Declare the project
project("llama-android")

# Set C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable position independent code
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Find required packages
find_package(Threads REQUIRED)

# Set llama.cpp directory
set(LLAMA_CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# Include directories - minimal set
include_directories(${CMAKE_CURRENT_SOURCE_DIR})
include_directories(${LLAMA_CPP_DIR})
include_directories(${LLAMA_CPP_DIR}/include)

# Try to find the right ggml include directory
if(EXISTS "${LLAMA_CPP_DIR}/ggml/include")
 include_directories(${LLAMA_CPP_DIR}/ggml/include)
endif()

if(EXISTS "${LLAMA_CPP_DIR}/ggml/src")
 include_directories(${LLAMA_CPP_DIR}/ggml/src)
endif()

if(EXISTS "${LLAMA_CPP_DIR}/ggml")
 include_directories(${LLAMA_CPP_DIR}/ggml)
endif()

if(EXISTS "${LLAMA_CPP_DIR}/common")
 include_directories(${LLAMA_CPP_DIR}/common)
endif()

# Start with just our JNI wrapper - minimal approach
set(SOURCES
        ${CMAKE_CURRENT_SOURCE_DIR}/llama-android.cpp
)

# Try to add core llama.cpp file if it exists
if(EXISTS "${LLAMA_CPP_DIR}/src/llama.cpp")
 list(APPEND SOURCES ${LLAMA_CPP_DIR}/src/llama.cpp)
 message(STATUS "✅ Found: llama.cpp/src/llama.cpp")
elseif(EXISTS "${LLAMA_CPP_DIR}/llama.cpp")
 list(APPEND SOURCES ${LLAMA_CPP_DIR}/llama.cpp)
 message(STATUS "✅ Found: llama.cpp/llama.cpp")
endif()

# Try to add basic ggml file
if(EXISTS "${LLAMA_CPP_DIR}/ggml/src/ggml.c")
 list(APPEND SOURCES ${LLAMA_CPP_DIR}/ggml/src/ggml.c)
 message(STATUS "✅ Found: ggml/src/ggml.c")
elseif(EXISTS "${LLAMA_CPP_DIR}/ggml.c")
 list(APPEND SOURCES ${LLAMA_CPP_DIR}/ggml.c)
 message(STATUS "✅ Found: ggml.c")
endif()

# Try to add common utilities if they exist
if(EXISTS "${LLAMA_CPP_DIR}/common/common.cpp")
 list(APPEND SOURCES ${LLAMA_CPP_DIR}/common/common.cpp)
 message(STATUS "✅ Found: common/common.cpp")
endif()

# Essential: Check our JNI wrapper exists
if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/llama-android.cpp")
 message(FATAL_ERROR "❌ Missing: llama-android.cpp")
endif()

message(STATUS "🎯 Building with ${list(LENGTH SOURCES)} source files")

# Create the shared library
add_library(llama-android SHARED ${SOURCES})

# Link libraries
target_link_libraries(llama-android
        ${CMAKE_THREAD_LIBS_INIT}
        log
        android
)

# Minimal compiler flags
target_compile_options(llama-android PRIVATE
        -O2
        -Wall
        -Wno-unused-function
        -Wno-unused-variable
        -Wno-multichar
        -Wno-unknown-pragmas
        -Wno-missing-field-initializers
)

# Essential definitions
target_compile_definitions(llama-android PRIVATE
        GGML_USE_CPU=1
        ANDROID=1
)

# Set target properties
set_target_properties(llama-android PROPERTIES
        CXX_VISIBILITY_PRESET hidden
        C_VISIBILITY_PRESET hidden
)

message(STATUS "🌟 Minimal llama-android library configured!")
message(STATUS "💕 Ready for Sister's Dream Assistant!")