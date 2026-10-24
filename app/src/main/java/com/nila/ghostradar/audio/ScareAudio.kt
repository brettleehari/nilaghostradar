package com.nila.ghostradar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import kotlin.random.Random

/**
 * Preloads every scare track during the radar phase so [play] starts with no decode delay, and
 * picks one at random each time (never the same one twice in a row). Each plays once (loop or
 * not is an open question for Nila). Calls [onFinished] when the playing track ends, which the
 * view model uses for "cooldown ends at song end or 8 s".
 */
class ScareAudio(
    private val context: Context,
    @RawRes private val trackRes: List<Int>,
    private val onFinished: () -> Unit,
    private val random: Random = Random.Default,
) {
    private class Slot(val res: Int) {
        var player: MediaPlayer? = null
        var ready = false
    }

    private val slots = trackRes.map { Slot(it) }
    private var current: Slot? = null
    private var lastIndex = -1

    fun preload() {
        for (slot in slots) {
            if (slot.player != null) continue
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                context.resources.openRawResourceFd(slot.res).use { afd ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                mp.setVolume(1f, 1f)
                mp.setOnPreparedListener { slot.ready = true }
                mp.setOnCompletionListener { if (current === slot) onFinished() }
                mp.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    true
                }
                mp.prepareAsync()
                slot.player = mp
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload scare track", e)
                mp.release()
            }
        }
    }

    /** Starts a random ready track from the top. */
    fun play() {
        stop()
        val slot = pick() ?: run { Log.w(TAG, "no scare track ready"); return }
        current = slot
        val mp = slot.player ?: return
        Log.i("GhostRadar", "scare track #${slots.indexOf(slot) + 1} of ${slots.size}")
        mp.seekTo(0)
        mp.start()
    }

    private fun pick(): Slot? {
        val ready = slots.withIndex().filter { it.value.ready && it.value.player != null }
        if (ready.isEmpty()) return null
        val choices = ready.filter { it.index != lastIndex }.ifEmpty { ready }
        val chosen = choices[random.nextInt(choices.size)]
        lastIndex = chosen.index
        return chosen.value
    }

    fun stop() {
        val slot = current ?: return
        val mp = slot.player ?: return
        if (slot.ready && mp.isPlaying) {
            mp.pause()
            mp.seekTo(0)
        }
        current = null
    }

    fun release() {
        for (slot in slots) {
            slot.player?.release()
            slot.player = null
            slot.ready = false
        }
        current = null
    }

    companion object {
        private const val TAG = "ScareAudio"
    }
}
