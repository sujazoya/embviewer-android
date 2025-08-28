cmake_minimum_required(VERSION 3.10)
project(embbridge)

# Path to libembroidery submodule
set(LIBEMBROIDERY_DIR ${CMAKE_SOURCE_DIR}/../../../external/libembroidery)

# Build libembroidery as a subdirectory if its CMakeLists exists
if(EXISTS ${LIBEMBROIDERY_DIR}/CMakeLists.txt)
    add_subdirectory(${LIBEMBROIDERY_DIR} libembroidery_build)
endif()

# Add your JNI library
add_library(embbridge SHARED embbridge.cpp)

# Include directories for headers (latest libembroidery structure)
if(EXISTS ${LIBEMBROIDERY_DIR})
    target_include_directories(embbridge PRIVATE
        ${LIBEMBROIDERY_DIR}/src/embroidery   # contains emb-pattern.h, emb-file-read.h, emb-file-write.h
    )
endif()

# Link Android log library for debug output
find_library(log-lib log)
target_link_libraries(embbridge PRIVATE ${log-lib})
