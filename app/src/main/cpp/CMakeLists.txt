cmake_minimum_required(VERSION 3.10)
project(embbridge)

set(LIBEMBROIDERY_DIR ${CMAKE_SOURCE_DIR}/../../../external/libembroidery)
if(EXISTS ${LIBEMBROIDERY_DIR}/CMakeLists.txt)
    add_subdirectory(${LIBEMBROIDERY_DIR} libembroidery_build)
endif()

add_library(embbridge SHARED embbridge.cpp)

if(EXISTS ${LIBEMBROIDERY_DIR})
    target_include_directories(embbridge PRIVATE
        ${LIBEMBROIDERY_DIR}/src
        ${LIBEMBROIDERY_DIR}/include
    )
endif()

find_library(log-lib log)
target_link_libraries(embbridge PRIVATE ${log-lib})
