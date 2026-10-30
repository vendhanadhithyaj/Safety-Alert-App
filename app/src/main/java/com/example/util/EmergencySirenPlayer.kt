package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object EmergencySirenPlayer {

    private const val TAG = "EmergencySirenPlayer"
    private const val SAMPLE_RATE = 44100
    private var audioTrack: AudioTrack? = null
    private var sirenJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var isPlaying = false

    fun isSirenRunning(): Boolean = isPlaying

    fun start(context: Context) {
        if (isPlaying) return
        isPlaying = true

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            // Request high volume for emergency alarm
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set stream volume: ${e.message}")
        }

        sirenJob = scope.launch {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(SAMPLE_RATE / 4)

                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val format = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                audioTrack = AudioTrack(
                    attributes,
                    format,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )

                audioTrack?.play()

                // Generate loud oscillating siren PCM data (750 Hz to 1250 Hz wailing siren wave)
                val chunkSamples = SAMPLE_RATE / 10 // 100ms chunk
                val pcmBuffer = ShortArray(chunkSamples)
                var phase = 0.0
                var step = 0

                while (isActive && isPlaying) {
                    // Oscillate frequency smoothly between 700 Hz and 1300 Hz
                    val cycleProgress = (step % 40) / 40.0 // 4.0 second sweep cycle
                    val frequency = 750.0 + 550.0 * sin(cycleProgress * 2.0 * Math.PI)

                    val phaseIncrement = 2.0 * Math.PI * frequency / SAMPLE_RATE

                    for (i in 0 until chunkSamples) {
                        phase += phaseIncrement
                        if (phase > 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI
                        }
                        // Maximum amplitude (Short.MAX_VALUE = 32767) for maximum loud piercing alarm sound
                        pcmBuffer[i] = (sin(phase) * 32000.0).toInt().toShort()
                    }

                    audioTrack?.write(pcmBuffer, 0, chunkSamples)
                    step++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing siren audio: ${e.message}")
            } finally {
                cleanUpTrack()
            }
        }
    }

    fun stop() {
        isPlaying = false
        sirenJob?.cancel()
        sirenJob = null
        cleanUpTrack()
    }

    private fun cleanUpTrack() {
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {
        } finally {
            audioTrack = null
        }
    }
}
