package com.nila.ghostradar.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Says a short line in a low, slow voice using the phone's own text-to-speech. No network, no
 * permission. If TTS is missing or not ready, [speak] calls [onDone] immediately so the scare is
 * never delayed by a voice that is not coming.
 */
class VoiceLine(context: Context, private val pitch: Float = 0.5f, private val rate: Float = 0.7f) {
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var pendingDone: (() -> Unit)? = null
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (!ready) {
            Log.w(TAG, "TTS unavailable (status=$status)")
            return@TextToSpeech
        }
        tts.language = Locale.US
        tts.setPitch(pitch)
        tts.setSpeechRate(rate)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finish()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish()
            override fun onError(utteranceId: String?, errorCode: Int) = finish()
        })
    }

    private fun finish() {
        main.post {
            val cb = pendingDone
            pendingDone = null
            cb?.invoke()
        }
    }

    fun speak(text: String, onDone: () -> Unit) {
        if (!ready) {
            onDone()
            return
        }
        pendingDone = onDone
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "line")
        if (result != TextToSpeech.SUCCESS) finish()
    }

    fun stop() {
        pendingDone = null
        if (ready) tts.stop()
    }

    fun release() {
        stop()
        tts.shutdown()
    }

    companion object {
        private const val TAG = "VoiceLine"
    }
}
