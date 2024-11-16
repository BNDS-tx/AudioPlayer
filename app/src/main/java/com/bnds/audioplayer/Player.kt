package com.bnds.audioplayer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log

class Player : Service() {

    private val binder = PlayerBinder()
    var mediaPlayer = AudiobookPlayer()

    // Binder for binding with activities or other components
    inner class PlayerBinder : Binder() {
        fun getService(): com.bnds.audioplayer.Player = this@Player
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("PlayerService", "Service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop() // Ensure mediaPlayer is stopped
        Log.d("PlayerService", "Service destroyed")
    }

    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        var filePath: String? = null
        if ("content".equals(uri.scheme, true)) {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    filePath = cursor.getString(columnIndex)
                }
            }
        } else if ("file".equals(uri.scheme, true)) {
            filePath = uri.path
        }
        return filePath
    }

    fun play(uri: Uri, speed: Float) {
        try {
            if (mediaPlayer.getState() != AudiobookPlayer.AudiobookPlayerState.STOPPED) {
                mediaPlayer.stop()
            }
            mediaPlayer.apply {
                load(getFilePathFromUri(applicationContext, uri), speed)
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "Error playing audio", e)
        }
    }

    fun pauseAndResume() {
        mediaPlayer.let {
            if (mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.PLAYING) {
                it.pause()
            } else if (mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.PAUSED) {
                it.play()
            }
        }
    }

    fun stop() {
        mediaPlayer.let {
            if (mediaPlayer.getState() != AudiobookPlayer.AudiobookPlayerState.STOPPED) {
                it.stop()
            }
        }
    }

    fun getDuration(): Int {
        if (mediaPlayer.mediaPlayer == null) {
            return 1
        }
        return mediaPlayer.mediaPlayer.duration
    }

    fun getProgress(): Int {
        return mediaPlayer.getProgress()
    }

    fun getFilePath(): String {
        return mediaPlayer.getFilePath()
    }

    fun seekTo(progress: Int) {
        val oldState = mediaPlayer.getState()
        mediaPlayer.skipTo(progress)
        mediaPlayer.state = oldState
    }

    fun setSpeed(speed: Float) {
        mediaPlayer.setPlaybackSpeed(speed)
    }

    fun getAlbumArt(): Bitmap? {
        val filePath = getFilePath()
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
                BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "Error retrieving album art", e)
            null
        } finally {
            retriever.release()
        }
    }

    fun complete(): Boolean {
        if (getProgress() < getDuration()) {
            if (getDuration() - getProgress() < 750) {
                return true
            }
            return false
        }
        return true
    }
}