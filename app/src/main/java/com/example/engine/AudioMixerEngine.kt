package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val TAG = "AudioMixerEngine"

class AudioMixerEngine(private val context: Context) {

    // Synthesize real audible sound effects programmatically so SFX buttons ALWAYS work
    suspend fun playSynthesizedSfx(type: String, volume: Float = 1.0f) = withContext(Dispatchers.Default) {
        try {
            val sampleRate = 44100
            val durationMs = when (type.uppercase()) {
                "WHOOSH" -> 400
                "IMPACT", "BASS_DROP" -> 700
                "RISER" -> 1000
                "GLITCH" -> 300
                "CHIME" -> 600
                else -> 400
            }
            val numSamples = (sampleRate * durationMs) / 1000
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / numSamples
                val sample: Double = when (type.uppercase()) {
                    "WHOOSH" -> {
                        // Filtered sweep from 250Hz to 1200Hz then down
                        val freq = 200.0 + 800.0 * sin(progress * PI)
                        val env = sin(progress * PI)
                        sin(2 * PI * freq * t) * env
                    }
                    "IMPACT", "BASS_DROP" -> {
                        // Deep bass thump 90Hz exponential decaying
                        val freq = 120.0 * exp(-progress * 4.0) + 40.0
                        val env = exp(-progress * 5.0)
                        sin(2 * PI * freq * t) * env
                    }
                    "RISER" -> {
                        // Pitch riser 200Hz to 1600Hz
                        val freq = 200.0 + 1400.0 * (progress * progress)
                        val env = progress
                        sin(2 * PI * freq * t) * env
                    }
                    "GLITCH" -> {
                        // High noise square wave
                        val freq = if ((i / 400) % 2 == 0) 1800.0 else 440.0
                        if (sin(2 * PI * freq * t) > 0) 0.6 else -0.6
                    }
                    "CHIME" -> {
                        // Bell chime 880Hz + 1760Hz
                        val env = exp(-progress * 4.0)
                        (sin(2 * PI * 880.0 * t) + 0.5 * sin(2 * PI * 1760.0 * t)) * env
                    }
                    else -> sin(2 * PI * 440.0 * t) * exp(-progress * 3.0)
                }
                buffer[i] = (sample * 32767.0 * volume.coerceIn(0f, 1f)).toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play synthesized sound effect", e)
        }
    }
}
