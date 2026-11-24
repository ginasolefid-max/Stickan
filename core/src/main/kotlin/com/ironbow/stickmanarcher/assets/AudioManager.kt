package com.ironbow.stickmanarcher.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.AudioDevice
import com.badlogic.gdx.utils.Disposable
import com.ironbow.stickmanarcher.data.SaveSystem
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class SfxType {
    BOW_DRAW, ARROW_WHOOSH, HIT_BODY, HIT_HEADSHOT, ZOMBIE_GROAN,
    EXPLOSION, ICE_SHATTER, UI_CLICK, LEVEL_CLEAR, GAME_OVER, PICKUP
}

class AudioManager(private val saveSystem: SaveSystem) : Disposable {

    private class ActiveVoice(val samples: FloatArray, var cursor: Int = 0, val gain: Float = 1f)

    private val sampleRate = 22050
    private val bufferSize = 1024
    private val voices = ConcurrentLinkedQueue<ActiveVoice>()
    private val soundBank = HashMap<SfxType, FloatArray>()

    private var audioDevice: AudioDevice? = null
    private var mixerThread: Thread? = null
    @Volatile private var running = false
    @Volatile private var paused = false

    private var musicPhaseA = 0.0
    private var musicPhaseB = 0.0
    private var musicStepTimer = 0.0
    private var musicStepIndex = 0
    private val bassNotes = floatArrayOf(73.42f, 73.42f, 87.31f, 82.41f, 65.41f, 65.41f, 98.00f, 82.41f)
    private val leadNotes = floatArrayOf(293.66f, 349.23f, 440.00f, 329.63f, 261.63f, 349.23f, 392.00f, 329.63f)

    fun init() {
        synthesizeSoundBank()
        try {
            audioDevice = Gdx.audio.newAudioDevice(sampleRate, true)
            running = true
            mixerThread = thread(start = true, isDaemon = true, name = "StickmanAudioMixer") { runMixerLoop() }
        } catch (_: Throwable) {
            audioDevice = null
        }
    }

    private fun runMixerLoop() {
        val device = audioDevice ?: return
        val mixBuffer = ShortArray(bufferSize)
        val dtPerSample = 1.0 / sampleRate

        while (running) {
            if (paused) {
                try { Thread.sleep(40) } catch (_: InterruptedException) { break }
                continue
            }

            val sfxVol = saveSystem.data.sfxVolume.coerceIn(0f, 1f)
            val musicVol = saveSystem.data.musicVolume.coerceIn(0f, 1f)

            for (i in 0 until bufferSize) {
                var sample = 0f
                if (musicVol > 0.01f) {
                    musicStepTimer += dtPerSample
                    if (musicStepTimer >= 0.34) {
                        musicStepTimer -= 0.34
                        musicStepIndex = (musicStepIndex + 1) % bassNotes.size
                    }
                    val beatEnv = (1.0 - (musicStepTimer / 0.34)).toFloat().coerceIn(0f, 1f)
                    musicPhaseA += 2.0 * PI * bassNotes[musicStepIndex] * dtPerSample
                    musicPhaseB += 2.0 * PI * leadNotes[musicStepIndex] * dtPerSample
                    val bass = (sin(musicPhaseA) * 0.65 + sin(musicPhaseA * 2.0) * 0.25).toFloat()
                    val pad = sin(musicPhaseB).toFloat() * beatEnv * 0.35f
                    sample += (bass * 0.18f + pad * 0.14f) * musicVol
                }

                val iter = voices.iterator()
                while (iter.hasNext()) {
                    val v = iter.next()
                    if (v.cursor < v.samples.size) sample += v.samples[v.cursor++] * v.gain * sfxVol
                    else iter.remove()
                }
                mixBuffer[i] = (sample.coerceIn(-0.95f, 0.95f) * 32767f).toInt().toShort()
            }

            try { device.writeSamples(mixBuffer, 0, bufferSize) } catch (_: Throwable) { break }
        }
    }

    fun play(type: SfxType, volumeScale: Float = 1f) {
        if (paused || saveSystem.data.sfxVolume <= 0.01f) return
        val buf = soundBank[type] ?: return
        if (voices.size < 16) voices.add(ActiveVoice(buf, 0, volumeScale.coerceIn(0.1f, 1.5f)))
    }

    fun pause() { paused = true }
    fun resume() { paused = false }

    private fun synthesizeSoundBank() {
        soundBank[SfxType.BOW_DRAW] = genCreak(0.14f, 160f, 210f, 0.25f)
        soundBank[SfxType.ARROW_WHOOSH] = genNoiseSweep(0.16f, 0.45f)
        soundBank[SfxType.HIT_BODY] = genThud(0.11f, 150f, 55f, 0.55f)
        soundBank[SfxType.HIT_HEADSHOT] = genHeadshotCrunch(0.22f)
        soundBank[SfxType.ZOMBIE_GROAN] = genGroan(0.45f, 92f)
        soundBank[SfxType.EXPLOSION] = genExplosion(0.48f)
        soundBank[SfxType.ICE_SHATTER] = genIceChime(0.26f)
        soundBank[SfxType.UI_CLICK] = genTone(0.055f, 680f, 920f, 0.30f)
        soundBank[SfxType.LEVEL_CLEAR] = genFanfare()
        soundBank[SfxType.GAME_OVER] = genTone(0.65f, 220f, 75f, 0.45f)
        soundBank[SfxType.PICKUP] = genTone(0.16f, 520f, 1040f, 0.38f)
    }

    private fun genTone(dur: Float, fStart: Float, fEnd: Float, amp: Float): FloatArray {
        val count = (sampleRate * dur).toInt()
        val out = FloatArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            phase += 2.0 * PI * (fStart + (fEnd - fStart) * t) / sampleRate
            out[i] = sin(phase).toFloat() * (1f - t) * (1f - t) * amp
        }
        return out
    }

    private fun genCreak(dur: Float, fStart: Float, fEnd: Float, amp: Float): FloatArray {
        val count = (sampleRate * dur).toInt()
        val out = FloatArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            phase += 2.0 * PI * (fStart + (fEnd - fStart) * t + sin(t * 28.0).toFloat() * 18f) / sampleRate
            out[i] = (sin(phase).toFloat() * 0.7f + (Random.nextFloat() * 2f - 1f) * 0.18f) * sin(t * PI).toFloat() * amp
        }
        return out
    }

    private fun genNoiseSweep(dur: Float, amp: Float): FloatArray {
        val count = (sampleRate * dur).toInt()
        val out = FloatArray(count)
        var last = 0f
        for (i in 0 until count) {
            val t = i.toFloat() / count
            last += (0.25f + 0.55f * (1f - t)) * ((Random.nextFloat() * 2f - 1f) - last)
            out[i] = last * sin(t * PI).toFloat() * amp
        }
        return out
    }

    private fun genThud(dur: Float, fStart: Float, fEnd: Float, amp: Float): FloatArray {
        val count = (sampleRate * dur).toInt()
        val out = FloatArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            phase += 2.0 * PI * (fStart + (fEnd - fStart) * t) / sampleRate
            val snap = if (i < count / 6) (Random.nextFloat() * 2f - 1f) * 0.4f else 0f
            out[i] = (sin(phase).toFloat() * 0.8f + snap) * (1f - t) * (1f - t) * amp
        }
        return out
    }

    private fun genHeadshotCrunch(dur: Float): FloatArray {
        val count = (sampleRate * dur).toInt()
        val out = FloatArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            phase += 2.0 * PI * (920f - 480f * t) / sampleRate
            val chime = sin(phase).toFloat() * 0.45f * (1f - t)
            val crunch = if (t < 0.35f) (Random.nextFloat() * 2f - 1f) * (1f - t / 0.35f) * 0.55f else 0f
            out[i] = chime + crunch
        }
        return out
    }

    private fun genGroan(dur: Float, baseFreq: Float): FloatArray {
        val count = (sampleRate * dur).toInt()
        val out = FloatArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            phase += 2.0 * PI * (baseFreq * (1f - 0.18f * t) + sin(t * 22.0).toFloat() * 7f) / sampleRate
            val saw = ((phase / (2.0 * PI)) % 1.0).toFloat() * 2f - 1f
            out[i] = (sin(phase).toFloat() * 0.5f + saw * 0.35f) * sin(t * PI).toFloat() * 0.32f
        }
        return out
    }

    private fun genExplosion(dur: Float): FloatArray {
        val count = (sampleRate * dur).toInt()
        val out = FloatArray(count)
        var low = 0f
        for (i in 0 until count) {
            val t = i.toFloat() / count
            low = low * 0.82f + (Random.nextFloat() * 2f - 1f) * 0.18f
            out[i] = low * (1f - t) * (1f - t) * 0.95f
        }
        return out
    }

    private fun genIceChime(dur: Float): FloatArray {
        val count = (sampleRate * dur).toInt()
        val out = FloatArray(count)
        var p1 = 0.0
        var p2 = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            p1 += 2.0 * PI * 1420.0 / sampleRate
            p2 += 2.0 * PI * 2180.0 / sampleRate
            out[i] = (sin(p1).toFloat() * 0.5f + sin(p2).toFloat() * 0.4f) * (1f - t) * (1f - t) * 0.42f
        }
        return out
    }

    private fun genFanfare(): FloatArray {
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
        val perNote = (sampleRate * 0.12f).toInt()
        val out = FloatArray(perNote * notes.size)
        for ((idx, freq) in notes.withIndex()) {
            var phase = 0.0
            for (i in 0 until perNote) {
                val t = i.toFloat() / perNote
                phase += 2.0 * PI * freq / sampleRate
                out[idx * perNote + i] = sin(phase).toFloat() * (1f - t * 0.7f) * 0.36f
            }
        }
        return out
    }

    override fun dispose() {
        running = false
        mixerThread?.interrupt()
        try { mixerThread?.join(250) } catch (_: InterruptedException) {}
        audioDevice?.dispose()
        audioDevice = null
    }
}
