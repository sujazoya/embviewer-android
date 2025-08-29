cmake_minimum_required(VERSION 3.22.1)
project(embbridge)

set(LIBEMBROIDERY_DIR ${CMAKE_CURRENT_SOURCE_DIR}/libembroidery/src)

set(LIBEMB_SRC
        ${LIBEMBROIDERY_DIR}/compress.c
        ${LIBEMBROIDERY_DIR}/data.c
        ${LIBEMBROIDERY_DIR}/emb_convert.c
        ${LIBEMBROIDERY_DIR}/formats.c
        ${LIBEMBROIDERY_DIR}/geometry.c
        ${LIBEMBROIDERY_DIR}/pattern.c
        ${LIBEMBROIDERY_DIR}/script.c
)

add_library(
        embbridge
        SHARED
        embbridge.cpp
        ${LIBEMB_SRC}
)

target_include_directories(
        embbridge PRIVATE
        ${LIBEMBROIDERY_DIR}
)

find_library(log-lib log)

target_link_libraries(
        embbridge
        ${log-lib}
)
