#include <jni.h>
#include <fluidsynth.h>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "FluidSynthJNI", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "FluidSynthJNI", __VA_ARGS__)

static fluid_settings_t* settings = nullptr;
static fluid_synth_t* synth = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_fretboardlayouts_audio_FluidSynthEngine_nativeInit(JNIEnv*, jobject, jint sampleRate) {
    settings = new_fluid_settings();
    fluid_settings_setnum(settings, "synth.sample-rate", (double) sampleRate);
    fluid_settings_setint(settings, "synth.audio-channels", 2);
    fluid_settings_setnum(settings, "synth.gain", 0.6);

    synth = new_fluid_synth(settings);
    if (!synth) { LOGE("Failed to create synth"); return JNI_FALSE; }

    // Tune reverb to subtle room sound instead of default heavy hall
    fluid_synth_set_reverb_on(synth, 1);
    fluid_synth_set_reverb(synth, 0.2, 0.0, 0.5, 0.3);
// roomsize, damping, width, level — much drier than default

// Reduce chorus — default chorus adds a warbling effect that muddies guitar
    fluid_synth_set_chorus_on(synth, 1);
    fluid_synth_set_chorus(synth, 2, 0.3, 0.25, 3.0, FLUID_CHORUS_MOD_SINE);
// nr, level, speed, depth, type

    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_fretboardlayouts_audio_FluidSynthEngine_nativeLoadSoundFont(JNIEnv* env, jobject, jstring path) {
    if (!synth) return -1;
    const char* cpath = env->GetStringUTFChars(path, nullptr);
    int sfont_id = fluid_synth_sfload(synth, cpath, 1);
    LOGI("sfload(%s) -> %d", cpath, sfont_id);
    env->ReleaseStringUTFChars(path, cpath);
    return sfont_id;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_fretboardlayouts_audio_FluidSynthEngine_nativeProgramChange(JNIEnv*, jobject, jint channel, jint program) {
if (synth) fluid_synth_program_change(synth, channel, program);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_fretboardlayouts_audio_FluidSynthEngine_nativeNoteOn(JNIEnv*, jobject, jint channel, jint key, jint velocity) {
if (synth) fluid_synth_noteon(synth, channel, key, velocity);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_fretboardlayouts_audio_FluidSynthEngine_nativeNoteOff(JNIEnv*, jobject, jint channel, jint key) {
if (synth) fluid_synth_noteoff(synth, channel, key);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_fretboardlayouts_audio_FluidSynthEngine_nativeRender(JNIEnv* env, jobject, jshortArray buffer, jint numFrames) {
if (!synth) return;
jshort* arr = env->GetShortArrayElements(buffer, nullptr);
// interleaved stereo: left starts at 0, right starts at 1, stride 2
fluid_synth_write_s16(synth, numFrames, arr, 0, 2, arr, 1, 2);
env->ReleaseShortArrayElements(buffer, arr, 0);
}