cmake_minimum_required(VERSION 3.22.1)

project("monster_audio" C)

add_library(monster_audio SHARED
    biquad_filter.c
    fft.c
    effects_dsp.c
    biquad_jni.c
)

target_compile_options(monster_audio PRIVATE -O3 -ffast-math -fno-finite-math-only)

find_library(log-lib log)

target_link_libraries(monster_audio
    ${log-lib}
    m
)

target_link_options(
        monster_audio
        PRIVATE
        "-Wl,--build-id=none"
)