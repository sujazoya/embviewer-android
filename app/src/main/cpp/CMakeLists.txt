cmake_minimum_required(VERSION 3.22.1)

project("embroidery-converter")

# Collect all C sources from libembroidery
file(GLOB LIBEMB_SOURCES
        ${CMAKE_CURRENT_SOURCE_DIR}/libembroidery/src/*.c
)

# Debug: print to verify sources found
message(STATUS "LibEmbroidery sources: ${LIBEMB_SOURCES}")

# Build shared library
add_library(embroidery-converter SHARED
        native-lib.cpp
        emb_jni.cpp
        embbridge.cpp
        ${LIBEMB_SOURCES}
)

# Include dirs
target_include_directories(embroidery-converter PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/libembroidery/src
)

# Link Android log
find_library(log-lib log)

target_link_libraries(embroidery-converter
        ${log-lib}
        atomic
        m
)
