package com.nila.ghostradar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes

/**
 * One short recorded line (Nila's own voice), preloaded during the radar phase so it starts the
 * instant it is asked for. [play] calls [onDone] when the clip ends, is stopped, or could not play,
 * so a scare that waits on the line is never delayed by a voice that is not coming.
 */
class VoiceClip(
    private val context: Context,
    @RawRes private val res: Int,
    /** What the clip says; only for the log. */
    private val says: String,
) {
    private var player: MediaPlayer? = null
    private var ready = false
    private var pendingDone: (() -> Unit)? = null
    private var startedAtMs = 0L

    fun preload() {
        if (player != null) return
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            context.resources.openRawResourceFd(res).use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mp.setVolume(1f, 1f)
            mp.setOnPreparedListener { ready = true }
            mp.setOnCompletionListener { finish() }
            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                finish()
                true
            }
            mp.prepareAsync()
            player = mp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload voice clip", e)
            mp.release()
        }
    }

    val isPlaying: Boolean get() = ready && player?.isPlaying == true

    /** Starts the clip from the top. If it is not ready, [onDone] is called at once. */
    fun play(onDone: () -> Unit = {}) {
        val mp = player
        if (!ready || mp == null) {
            onDone()
            return
        }
        pendingDone?.invoke()
        pendingDone = onDone
        Log.i("GhostRadar", "voice: \"$says\"")
        mp.seekTo(0)
        mp.start()
    }

    /** Runs [cb] when the clip that is playing ends; at once if nothing is playing. */
    fun onFinished(cb: () -> Unit) {
        if (isPlaying) pendingDone = cb else cb()
    }

    /** Stops the clip and forgets the pending callback (a stop is not a finish). */
    fun stop() {
        pendingDone = null
        val mp = player ?: return
        if (ready && mp.isPlaying) {
            mp.pause()
            mp.seekTo(0)
        }
    }

    private fun finish() {
        val cb = pendingDone
        pendingDone = null
        cb?.invoke()
    }

    fun release() {
        stop()
        player?.release()
        player = null
        ready = false
    }

    companion object {
        private const val TAG = "VoiceClip"
    }
}
