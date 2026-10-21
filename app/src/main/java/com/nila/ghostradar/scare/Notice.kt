package com.nila.ghostradar.scare

/**
 * A small, short-lived line on the radar. Never blocks anything; the status line stays as it is.
 * [id] makes two identical notices in a row distinct so the UI animates each one.
 */
sealed interface Notice {
    val id: Long

    /** A toy was found but this one is not haunted. */
    data class FaintTrace(override val id: Long) : Notice

    /** An everyday object the camera recognised: "CHAIR · NO SOUL". */
    data class NoSoul(val label: String, override val id: Long) : Notice
}
