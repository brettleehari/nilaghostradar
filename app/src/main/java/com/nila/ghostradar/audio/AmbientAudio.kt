package com.nila.ghostradar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes

/**
 * Background loop for the radar phase. Plays while Searching, stops dead the instant the scare
 * starts so the stinger hits silence, and resumes when the hunt is back on.
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
        if (ready && !mp.isPlaying) mp.start()
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
    }
}
