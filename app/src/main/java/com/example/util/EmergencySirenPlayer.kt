package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object EmergencySirenPlayer {

    private const val TAG = "EmergencySirenPlayer"
    private var audioTrack: AudioTrack? = null
    private var toneGenerator: ToneGenerator? = null
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
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set stream volume: ${e.message}")
        }

        sirenJob = scope.launch {
            var trackCreated = false

            // Try candidate sample rates supported by Android audio HALs
            val candidateSampleRates = intArrayOf(44100, 48000, 16000)

            for (sampleRate in candidateSampleRates) {
                if (!isActive || !isPlaying) break

                try {
                    val rawMin = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )

                    // Critical fix: buffer size in bytes must be positive, >= minBufferSize,
                    // and strictly divisible by 2 (the 16-bit PCM mono frame size)
                    val baseMin = if (rawMin > 0) rawMin else 8192
                    val bufferSizeInBytes = ((baseMin * 2).coerceAtLeast(8192) / 4) * 4

                    val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        AudioTrack.Builder()
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .build()
                            )
                            .setBufferSizeInBytes(bufferSizeInBytes)
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        AudioTrack(
                            AudioManager.STREAM_ALARM,
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSizeInBytes,
                            AudioTrack.MODE_STREAM
                        )
                    }

                    if (track.state != AudioTrack.STATE_INITIALIZED) {
                        track.release()
                        continue
                    }

                    audioTrack = track
                    audioTrack?.play()
                    trackCreated = true

                    // Generate loud oscillating siren PCM data (750 Hz to 1300 Hz wailing siren wave)
                    val chunkSamples = (sampleRate / 10 / 2) * 2 // 100ms chunk, even length
                    val pcmBuffer = ShortArray(chunkSamples)
                    var phase = 0.0
                    var step = 0

                    while (isActive && isPlaying) {
                        // Oscillate frequency smoothly between 700 Hz and 1300 Hz
                        val cycleProgress = (step % 30) / 30.0 // 3.0 second sweep cycle
                        val frequency = 750.0 + 550.0 * sin(cycleProgress * 2.0 * Math.PI)
                        val phaseIncrement = 2.0 * Math.PI * frequency / sampleRate

                        for (i in 0 until chunkSamples) {
                            phase += phaseIncrement
                            if (phase > 2.0 * Math.PI) {
                                phase -= 2.0 * Math.PI
                            }
                            // Maximum amplitude for loud piercing alarm sound
                            pcmBuffer[i] = (sin(phase) * 32000.0).toInt().coerceIn(-32767, 32767).toShort()
                        }

                        audioTrack?.write(pcmBuffer, 0, chunkSamples)
                        step++
                    }
                    break // Successfully played
                } catch (e: Exception) {
                    Log.w(TAG, "AudioTrack init failed with sampleRate $sampleRate: ${e.message}")
                    cleanUpTrack()
                }
            }

            // Fallback to ToneGenerator if AudioTrack could not be initialized in emulator
            if (!trackCreated && isPlaying) {
                try {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    while (isActive && isPlaying) {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
                        delay(1050)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "ToneGenerator fallback error: ${e.message}")
                }
            }

            cleanUpTrack()
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
            toneGenerator?.release()
        } catch (_: Exception) {
        } finally {
            toneGenerator = null
        }

        try {
            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED && playState == AudioTrack.PLAYSTATE_PLAYING) {
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
