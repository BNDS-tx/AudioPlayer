package com.bnds.audioplayer

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.bnds.audioplayer.AudiobookPlayer.AudiobookPlayerState

class Player private constructor(private val context: Context) {

    companion object {

        @Volatile
        private var INSTANCE: Player? = null

        fun getInstance(context: Context): Player {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Player(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var mediaPlayer = AudiobookPlayer()
    protected var state: buttonIconState = buttonIconState.NOT_PLAYING_NOT_READY

    enum class buttonIconState {
        PLAYING,
        NOT_PLAYING_READY,
        NOT_PLAYING_NOT_READY
    }

    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    filePath = cursor.getString(columnIndex)
                }
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            filePath = uri.path
        }
        return filePath
    }

    fun play(uri: Uri) {
        try {
            if (mediaPlayer.getState() != AudiobookPlayerState.STOPPED) {
                mediaPlayer.stop()
            }
            mediaPlayer.apply {
                load(getFilePathFromUri(context, uri), 1F)
            }
            this.state = buttonIconState.PLAYING
        } catch (e: Exception) {
            Log.e("Player", "Music Player Error: ${e.message}")
        }
    }

    fun pauseAndResume() {
        mediaPlayer.let {
            if (mediaPlayer.getState() == AudiobookPlayerState.PLAYING) {
                it.pause()
                this.state = buttonIconState.NOT_PLAYING_READY
            } else if (mediaPlayer.getState() == AudiobookPlayerState.PAUSED) {
                it.play()
                this.state = buttonIconState.PLAYING
            }
        }
    }

    fun stop() {
        mediaPlayer.let {
            if (mediaPlayer.getState() != AudiobookPlayerState.STOPPED) {
                it.stop()
                this.state = buttonIconState.NOT_PLAYING_NOT_READY
            }
        }
    }

    fun getDuration() : Int {
        return mediaPlayer.mediaPlayer.duration
    }

    fun getProgress() : Int {
        return mediaPlayer.getProgress()
    }

    fun seekTo(progress: Int) {
        mediaPlayer.skipTo(progress)
    }
}