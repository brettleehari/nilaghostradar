package com.nila.ghostradar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes

/**
 * Preloads the scare track during the radar phase so [play] starts with no decode delay.
 * Plays once (loop or not is an open question for Nila). Calls [onFinished] when the track ends,
 * which the view model uses for "cooldown ends at song end or 8 s".
 */
class ScareAudio(
    private val context: Context,
    @RawRes private val trackRes: Int,
    private val onFinished: () -> Unit,
) {
    private var player: MediaPlayer? = null
    private var ready = false

    fun preload() {
        if (player != null) return
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            context.resources.openRawResourceFd(trackRes).use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mp.setOnPreparedListener { ready = true }
            mp.setOnCompletionListener { onFinished() }
            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                true
            }
            mp.prepareAsync()
            player = mp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload scare track", e)
            mp.release()
        }
    }

    fun play() {
        val mp = player ?: return
        if (!ready) {
            Log.w(TAG, "play() before prepared; track will start late")
            mp.setOnPreparedListener { ready = true; mp.start() }
            return
        }
        if (mp.isPlaying) mp.seekTo(0) else { mp.seekTo(0); mp.start() }
    }

    fun stop() {
        val mp = player ?: return
        if (ready && mp.isPlaying) {
            mp.pause()
            mp.seekTo(0)
        }
    }

    fun release() {
        player?.release()
        player = null
        ready = false
    }

    companion object {
        private const val TAG = "ScareAudio"
    }
}
