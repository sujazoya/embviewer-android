cmake_minimum_required(VERSION 3.22.1)

project("embroidery-converter")

file(GLOB LIBEMB_SOURCES
        ${CMAKE_CURRENT_SOURCE_DIR}/libembroidery/src/*.c
)

add_library(embroidery-converter SHARED
        native-lib.cpp
        ${LIBEMB_SOURCES}
)

target_include_directories(embroidery-converter PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/libembroidery/src
)

find_library(log-lib log)

target_link_libraries(embroidery-converter
        ${log-lib}
        atomic
        m
)
