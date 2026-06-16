cmake_minimum_required(VERSION 3.22.1)
project("fluidsynth_jni")

find_package(fluidsynth-android REQUIRED CONFIG)

add_library(fluidsynth_jni SHARED
        fluidsynth_jni.cpp)

target_link_libraries(fluidsynth_jni
        fluidsynth-android::fluidsynth
        log)