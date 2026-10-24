package com.nila.ghostradar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Log
import androidx.annotation.RawRes

/**
 * Background loop for the radar phase. Plays while Searching, stops dead the instant the scare
 * starts so the stinger hits silence, and resumes when the hunt is back on. [setTempo] speeds the
 * loop up (pitch unchanged) as a toy gets closer; [setVolume] follows the same signal.
 *
 * To use a track of your own on a private build, replace res/raw/radar_ambience.wav (any format
 * MediaPlayer plays). Shipped builds may only carry music the project owns or that is CC0.
 */
class AmbientAudio(
    private val context: Context,
    @RawRes private val trackRes: Int,
    private val volume: Float = 0.6f,
) {
    private var player: MediaPlayer? = null
    private var ready = false
    private var wantPlaying = false
    private var tempo = 1f
    private var currentVolume = volume

    fun preload() {
        if (player != null) return
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            context.resources.openRawResourceFd(trackRes).use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mp.isLooping = true
            mp.setVolume(volume, volume)
            mp.setOnPreparedListener {
                ready = true
                if (wantPlaying) mp.start()
            }
            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                true
            }
            mp.prepareAsync()
            player = mp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload ambience", e)
            mp.release()
        }
    }

    fun play() {
        wantPlaying = true
        val mp = player ?: return
        if (ready && !mp.isPlaying) {
            mp.start()
            applyTempo(mp)
        }
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
        val mp = player ?: return
        if (ready && mp.isPlaying) applyTempo(mp)
    }

    fun setVolume(v: Float) {
        val q = Math.round(v.coerceIn(0f, 1f) * 20) / 20f
        if (q == currentVolume) return
        currentVolume = q
        val mp = player ?: return
        if (ready) mp.setVolume(q, q)
    }

    private fun applyTempo(mp: MediaPlayer) {
        try {
            mp.playbackParams = PlaybackParams().setSpeed(tempo).setPitch(1f)
        } catch (e: Exception) {
            Log.w(TAG, "tempo $tempo not accepted", e)
        }
    }

    fun pause() {
        wantPlaying = false
        val mp = player ?: return
        if (ready && mp.isPlaying) mp.pause()
    }

    fun release() {
        player?.release()
        player = null
        ready = false
    }

    companion object {
        private const val TAG = "AmbientAudio"
        private const val TEMPO_STEP = 0.05f
    }
}
