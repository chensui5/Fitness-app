package com.chensui.lianji.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 轻量音效。
 *
 * 用 [AudioTrack] 实时合成柔和的正弦提示音，不依赖任何音频素材文件，
 * 既不增加安装包体积，也避开素材版权问题。
 *
 * 音色设计上刻意做了三件事来避免"刺耳"：
 * 1. 基频控制在 520–880Hz，不往高频走；
 * 2. 起音加约 8ms 的平滑包络，消除波形突变的爆音；
 * 3. 叠一点二次泛音让音色偏暖，整体音量压低。
 */
object SoundFx {

    private const val RATE = 44100
    private val cache = HashMap<String, ShortArray>()

    @Volatile
    private var lastAt = 0L

    /** 勾选动作：短促轻快 */
    fun tick(enabled: Boolean) {
        if (!enabled) return
        play("tick", durationSec = 0.20, volume = 0.22) { t ->
            bell(t, 784.0, decay = 38.0)
        }
    }

    /** 取消勾选：略低的短音 */
    fun undo(enabled: Boolean) {
        if (!enabled) return
        play("undo", durationSec = 0.20, volume = 0.16) { t ->
            bell(t, 587.3, decay = 32.0)
        }
    }

    /** 单项其他训练完成 / 获得金币 */
    fun reward(enabled: Boolean) {
        if (!enabled) return
        play("reward", durationSec = 0.36, volume = 0.26) { t ->
            bell(t, 587.3, decay = 13.0) + bell(t, 880.0, decay = 16.0) * 0.65
        }
    }

    /** 当日训练全部完成、自动打卡：C5–E5–G5 上行琶音 */
    fun celebrate(enabled: Boolean) {
        if (!enabled) return
        play("celebrate", durationSec = 0.60, volume = 0.30) { t ->
            bell(t, 523.3, decay = 9.0) +
                bell(t - 0.058, 659.3, decay = 9.0) * 0.9 +
                bell(t - 0.116, 784.0, decay = 7.5) * 0.8
        }
    }

    /** 单个带衰减的正弦音；t 为相对时间（秒），负值表示尚未起振 */
    private fun bell(t: Double, freq: Double, decay: Double): Double {
        if (t < 0.0) return 0.0
        val attack = 1.0 - exp(-260.0 * t)
        val env = attack * exp(-decay * t)
        val fundamental = sin(2.0 * PI * freq * t)
        val overtone = sin(2.0 * PI * freq * 2.0 * t) * 0.18
        return (fundamental + overtone) * env
    }

    private fun play(key: String, durationSec: Double, volume: Double, wave: (Double) -> Double) {
        // 连续快速点击时节流，避免同时创建大量 AudioTrack
        val now = System.currentTimeMillis()
        if (now - lastAt < 55L) return
        lastAt = now

        Thread {
            runCatching {
                val samples = synchronized(cache) {
                    cache[key] ?: build(durationSec, wave).also { cache[key] = it }
                }
                emit(samples, volume)
            }
        }.start()
    }

    private fun build(durationSec: Double, wave: (Double) -> Double): ShortArray {
        val count = (RATE * durationSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val v = (wave(i.toDouble() / RATE) * 0.55).coerceIn(-1.0, 1.0)
            out[i] = (v * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun emit(samples: ShortArray, volume: Double) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.setVolume(volume.toFloat())
        track.write(samples, 0, samples.size)
        track.play()
        Thread.sleep((samples.size * 1000L) / RATE + 90L)
        runCatching { track.stop() }
        runCatching { track.release() }
    }
}
