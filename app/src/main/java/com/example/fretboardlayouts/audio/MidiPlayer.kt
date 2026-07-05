package com.example.fretboardlayouts.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CopyOnWriteArrayList

class MidiPlayer(private val context: Context) {
    private val TAG = "MidiPlayer"
    
    // Engine states shown in UI
    var currentEngineName = "Initializing..."
        private set
        
    private var isFluidSynthAvailable = false
    private var isHardwareMidiAvailable = false


    // Fallback Engine (High Quality Polyphonic)
    private var fallbackAudioTrack: AudioTrack? = null
    private val SAMPLE_RATE = 44100
    private val audioScope = CoroutineScope(Dispatchers.Default + Job())
    private val activeVoices = CopyOnWriteArrayList<ActiveVoice>()
    private var audioWorkerJob: Job? = null

    class ActiveVoice(val samples: ShortArray, var position: Int = 0)

    // Hardware MIDI Fallback (The "90s" sound)
    private val midiManager: MidiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    private var inputPort: MidiInputPort? = null
    private var midiDevice: android.media.midi.MidiDevice? = null

    init {
        setupAudio()
    }

    private fun setupAudio() {
        Log.i(TAG, ">>> STARTING AUDIO ENGINE BOOTSTRAP <<<")

        // 1. ATTEMPT FLUIDSYNTH (The only one that sounds "Real")
        val sfPath = copySoundFontToInternalStorage()
        if (sfPath != null) {
            try {
                Log.i(TAG, "Attempting to load high-quality FluidSynth engine...")

                val success = FluidSynthEngine.start(sfPath)

                if (success) {
                    isFluidSynthAvailable = true
                    currentEngineName = "FluidSynth Studio (Active)"
                    Log.i(TAG, "✅ SUCCESS: Studio Engine Active.")
                    return 
                } else {
                    Log.e(TAG, "❌ FAILURE: SoundFont rejected (sfId -1).")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "❌ FAILURE: FluidSynth could not start: ${e.javaClass.simpleName} - ${e.message}")
            }
        }

        // 2. FALLBACK TO PLUCKY ENGINE (Priority 2 - Sounds better than beeps)
        Log.w(TAG, "⚠️ FluidSynth failed. Using Custom Plucky Synthesis.")
        currentEngineName = "Plucky Synth (Fallback)"
        startAudioWorker()
        
        // 3. TRY HARDWARE MIDI (Background only)
        val hardwareSynth = midiManager.devices.find { info ->
            info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.contains("Synth", ignoreCase = true) == true ||
            info.type == MidiDeviceInfo.TYPE_VIRTUAL
        }
        if (hardwareSynth != null) {
            midiManager.openDevice(hardwareSynth, { device ->
                midiDevice = device
                inputPort = device.openInputPort(0)
                isHardwareMidiAvailable = true
                Log.i(TAG, "MIDI Hardware detected (90s sound).")
            }, Handler(Looper.getMainLooper()))
        }
    }

    private fun copySoundFontToInternalStorage(): String? {
        val fileName = "Timbres of Heaven (XGM) 4.00(G).sf2"
        val destFile = File(context.filesDir, fileName)
        try {
            Log.i(TAG, "Checking SoundFont...")
            context.assets.open(fileName).use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            return destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Font error: ${e.message}")
            return null
        }
    }

    private fun startAudioWorker() {
        audioWorkerJob?.cancel()
        audioWorkerJob = audioScope.launch {
            ensureFallbackTrack()
            val track = fallbackAudioTrack ?: return@launch
            val bufferSize = 1024
            val mixBuffer = FloatArray(bufferSize)
            val outBuffer = ShortArray(bufferSize)

            while (true) {
                mixBuffer.fill(0f)
                if (activeVoices.isNotEmpty()) {
                    val iterator = activeVoices.iterator()
                    while (iterator.hasNext()) {
                        val voice = iterator.next()
                        for (i in 0 until bufferSize) {
                            if (voice.position < voice.samples.size) {
                                mixBuffer[i] += (voice.samples[voice.position] / 32768f) * 0.4f 
                                voice.position++
                            }
                        }
                        if (voice.position >= voice.samples.size) activeVoices.remove(voice)
                    }
                }
                for (i in 0 until bufferSize) {
                    outBuffer[i] = (mixBuffer[i].coerceIn(-1f, 1f) * 32767).toInt().toShort()
                }
                track.write(outBuffer, 0, bufferSize)
            }
        }
    }

    private fun ensureFallbackTrack() {
        if (fallbackAudioTrack == null) {
            val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            fallbackAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes((minBufferSize * 4).coerceAtLeast(16384))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            fallbackAudioTrack?.play()
        }
    }

    fun isMidiAvailable(): Boolean = isFluidSynthAvailable || isHardwareMidiAvailable

    fun noteOn(channel: Int, pitch: Int, velocity: Int) {
        if (isFluidSynthAvailable) {
            FluidSynthEngine.nativeNoteOn(channel, pitch, velocity)
        } else {
            val vol = velocity / 127f
            val samples = when (channel) {
                9 -> InstrumentSynthesis.generateDrum(pitch, vol)
                1 -> InstrumentSynthesis.generateBass(pitch, vol)
                else -> InstrumentSynthesis.generateGuitar(pitch, vol)
            }
            activeVoices.add(ActiveVoice(samples))
            
            if (isHardwareMidiAvailable) {
                val buffer = byteArrayOf((0x90 or (channel and 0x0F)).toByte(), (pitch and 0x7F).toByte(), (velocity and 0x7F).toByte())
                inputPort?.send(buffer, 0, 3)
            }
        }
    }

    fun noteOff(channel: Int, pitch: Int) {
        if (isFluidSynthAvailable) {
            FluidSynthEngine.nativeNoteOff(channel, pitch)
        } else if (isHardwareMidiAvailable) {
            val buffer = byteArrayOf((0x80 or (channel and 0x0F)).toByte(), (pitch and 0x7F).toByte(), 0.toByte())
            inputPort?.send(buffer, 0, 3)
        }
    }

    fun stopAllNotes() {
        if (isFluidSynthAvailable) {
            for (ch in 0..15) {
                for (note in 0..127) { // NEW — silence every possible note on every channel
                    FluidSynthEngine.nativeNoteOff(ch, note) // NEW
                }
            }
        }
    }

    fun setProgram(channel: Int, program: Int) {
        if (isFluidSynthAvailable) {
            FluidSynthEngine.nativeProgramChange(channel, program)
        } else if (isHardwareMidiAvailable) {
            val buffer = byteArrayOf((0xC0 or (channel and 0x0F)).toByte(), (program and 0x7F).toByte())
            inputPort?.send(buffer, 0, 2)
        }
    }

    fun setupInstruments(instrumentation: GenreInstrumentation) {
        setProgram(0, instrumentation.guitarProgram)
        setProgram(1, instrumentation.bassProgram)
        setProgram(9, instrumentation.drumKitProgram)
    }

    fun release() {
        try {
            audioWorkerJob?.cancel()
            inputPort?.close()
            midiDevice?.close()
            fallbackAudioTrack?.stop()
            fallbackAudioTrack?.release()
            FluidSynthEngine.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Release Error: ${e.message}")
        }
    }
}
