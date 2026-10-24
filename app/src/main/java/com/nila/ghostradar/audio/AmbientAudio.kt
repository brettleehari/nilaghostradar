package com.nila.ghostradar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Log
import androidx.annotation.RawRes
import kotlin.random.Random

/**
 * Background loops for the radar phase. One of [trackRes] is picked at random each time the hunt
 * (re)starts, never the same one twice running. Plays while Searching, stops dead the instant the
 * scare starts so the stinger hits silence, and resumes on a fresh pick when the hunt is back on.
 * [setTempo] speeds the loop up (pitch unchanged) as a toy gets closer; [setVolume] follows the
 * same signal.
 *
 * To use tracks of your own on a private build, replace res/raw/radar_ambience*.wav (any format
 * MediaPlayer plays). Shipped builds may only carry music the project owns or that is CC0.
 */
class AmbientAudio(
    private val context: Context,
    @RawRes private val trackRes: List<Int>,
    private val volume: Float = 0.6f,
    private val random: Random = Random.Default,
) {
    private class Slot(val res: Int) {
        var player: MediaPlayer? = null
        var ready = false
    }

    private val slots = trackRes.map { Slot(it) }
    private var current: Slot? = null
    private var lastIndex = -1
    private var wantPlaying = false
    private var tempo = 1f
    private var currentVolume = volume

    fun preload() {
        for (slot in slots) {
            if (slot.player != null) continue
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                context.resources.openRawResourceFd(slot.res).use { afd ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                mp.isLooping = true
                mp.setVolume(currentVolume, currentVolume)
                mp.setOnPreparedListener {
                    slot.ready = true
                    // First loop: start as soon as any track is ready and the hunt wants music.
                    if (wantPlaying && current == null) play()
                }
                mp.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    true
                }
                mp.prepareAsync()
                slot.player = mp
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload ambience", e)
                mp.release()
            }
        }
    }

    /** Start (or resume after a pause) on a freshly picked loop. */
    fun play() {
        wantPlaying = true
        if (current?.player?.isPlaying == true) return
        val slot = pick() ?: return
        current = slot
        val mp = slot.player ?: return
        Log.i("GhostRadar", "ambience loop #${slots.indexOf(slot) + 1} of ${slots.size}")
        mp.seekTo(0)
        mp.start()
        applyTempo(mp)
    }

    private fun pick(): Slot? {
        val ready = slots.withIndex().filter { it.value.ready && it.value.player != null }
        if (ready.isEmpty()) return null
        val choices = ready.filter { it.index != lastIndex }.ifEmpty { ready }
        val chosen = choices[random.nextInt(choices.size)]
        lastIndex = chosen.index
        return chosen.value
    }

    fun pause() {
        wantPlaying = false
        val slot = current ?: return
        val mp = slot.player ?: return
        if (slot.ready && mp.isPlaying) mp.pause()
        current = null
    }

    /**
     * Playback speed, 1.0 = as recorded. Quantised to [TEMPO_STEP] so the stretcher is not poked
     * every frame. Only touches the player while it is playing: setting PlaybackParams on a paused
     * MediaPlayer starts it, which would put music under the stinger.
     */
    fun setTempo(speed: Float) {
        val q = (Math.round(speed / TEMPO_STEP) * TEMPO_STEP).coerceIn(0.5f, 2.5f)
        if (q == tempo) return
        tempo = q
        val mp = current?.player ?: return
        if (mp.isPlaying) applyTempo(mp)
    }

    fun setVolume(v: Float) {
        val q = Math.round(v.coerceIn(0f, 1f) * 20) / 20f
        if (q == currentVolume) return
        currentVolume = q
        for (slot in slots) if (slot.ready) slot.player?.setVolume(q, q)
    }

    private fun applyTempo(mp: MediaPlayer) {
        try {
            mp.playbackParams = PlaybackParams().setSpeed(tempo).setPitch(1f)
        } catch (e: Exception) {
            Log.w(TAG, "tempo $tempo not accepted", e)
        }
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
        private const val TAG = "AmbientAudio"
        private const val TEMPO_STEP = 0.05f
    }
}
