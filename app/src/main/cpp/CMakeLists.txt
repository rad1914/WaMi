cmake_minimum_required(VERSION 3.10)
project(native_mel)

# KissFFT static library
add_library(kissfft STATIC
    kiss_fft/kiss_fft.c
    kiss_fft/kiss_fftr.c
)

# Your native library
add_library(native_mel SHARED
    native_mel.cpp
    mel_filterbank.cpp
)

target_include_directories(kissfft PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/kiss_fft
)

target_include_directories(native_mel PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/kiss_fft
)

find_library(log-lib log)

target_link_libraries(native_mel
    kissfft
    ${log-lib}
    m
)

set_target_properties(native_mel PROPERTIES CXX_STANDARD 17)