cmake_minimum_required(VERSION 3.22.1)

project("nexus-engine")

# The path to this CMakeLists.txt file
set(CMAKE_CXX_STANDARD 17)

# Add our native source files
add_library(
        nexus-engine
        SHARED
        native-lib.cpp)

# Search for the standard Android NDK libraries we need
find_library(
        log-lib
        log)

# Link the libraries to our engine
target_link_libraries(
        nexus-engine
        ${log-lib}
        android
        EGL
        GLESv2)
