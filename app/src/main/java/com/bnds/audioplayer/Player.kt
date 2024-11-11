package com.bnds.audioplayer

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

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

    private var mediaPlayer: MediaPlayer? = null

    /**
     * 播放音乐
     * @param uri 音乐文件的 URI
     */
    fun play(uri: Uri) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, uri)
                    prepare()
                    start()
                    setOnCompletionListener {
                        stop()
                    }
                }
            } else {
                mediaPlayer?.apply {
                    reset()
                    setDataSource(context, uri)
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e("Player", "播放音乐失败: ${e.message}")
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    /**
     * 继续播放
     */
    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.reset()
                it.release()
                mediaPlayer = null
            }
        }
    }

    /**
     * 释放 MediaPlayer 资源
     */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}