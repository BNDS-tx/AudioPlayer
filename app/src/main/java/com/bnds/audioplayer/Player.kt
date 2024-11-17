package com.bnds.audioplayer

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File

class Player : Service() {

    private val binder = PlayerBinder()
    var mediaPlayer = AudiobookPlayer()
    private lateinit var mediaSession: MediaSessionCompat
    private val channelId = "com.bnds.audioplayer.channel"

    private var musicUri: Uri? = null
    private var playbackSpeed: Float = 1.0f

    inner class PlayerBinder : Binder() {
        fun getService(): Player = this@Player
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("PlayerService", "Service created")
        initializeMediaSession()
        createNotificationChannel()
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "PlayerMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    if (musicUri != null) {
                        if (stateCheck(3)) {
                            musicUri?.let { play(it, playbackSpeed) }
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            updateNotification()
                        } else {
                            pauseAndResume()
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            updateNotification()
                        }
                    }
                }

                override fun onPause() {
                    super.onPause()
                    pauseAndResume()
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    updateNotification()
                }

                override fun onStop() {
                    super.onStop()
                    stop()
                    updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                }

            })
            isActive = true // 激活 MediaSession
        }
    }

    private fun updatePlaybackState(state: Int) {
        val position = mediaPlayer.getProgress().toLong() // 当前播放进度
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, position, playbackSpeed) // 播放状态、进度、播放速度
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO
            ) // 支持的操作
            .setState(state, getProgress().toLong(), playbackSpeed)
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun setPlaybackState() {
        if (stateCheck(1)) { updatePlaybackState(PlaybackStateCompat.STATE_PLAYING) }
        else if (stateCheck(2)) { updatePlaybackState(PlaybackStateCompat.STATE_PAUSED) }
        else if (stateCheck(3)) { updatePlaybackState(PlaybackStateCompat.STATE_STOPPED) }
        else { updatePlaybackState(PlaybackStateCompat.STATE_ERROR) }
    }

    private fun updateNotification() {
        val isPlaying = stateCheck(1)

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getTitle())
            .setContentText(getArtist())
            .setSmallIcon(R.drawable.ic_notificiation_foreground) // app icon for the service
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(getContentIntent())
            .setOnlyAlertOnce(true)
            .addAction(
                NotificationCompat.Action(
                    if (!isPlaying) R.drawable.ic_play_arrow_24px else R.drawable.ic_pause_circle_24px, //
                    "Pause&Play",
                    getPendingIntentForAction("PAUSE_PLAY_ACTION")
                )
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setProgress(
                getDuration(),
                getProgress(),
                false
            )

        startForeground(1, builder.build()) // 确保 startForeground() 被调用
    }

    private fun getPendingIntentForAction(action: String): PendingIntent {
        val intent = Intent(this, PlayerControlReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getContentIntent(): PendingIntent {
        val intent = Intent(this, PlayListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            this,
            0, // 请求码，唯一标识
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Playback Control",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let {
            when (it) {
                "TOGGLE_PLAY_PAUSE" -> {
                    if (mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.STOPPED) {
                        musicUri?.let { uri -> play(uri, playbackSpeed) }
                    } else {
                        pauseAndResume()
                    }
                } else -> {
                    Log.d("PlayerService", "Unknown action: $it")
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop() // Ensure mediaPlayer is stopped
        Log.d("PlayerService", "Service destroyed")
        mediaSession.release()
    }

    private fun uriToFilePath(context: Context, uri: Uri): String? {
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
            if (stateCheck(3)) {
                mediaPlayer.stop()
            }
            musicUri = uri
            playbackSpeed = speed
            mediaPlayer.apply {
                load(uriToFilePath(applicationContext, uri), speed)
            }
            setPlaybackState()
            updateNotification() // 添加这一行来更新通知
        } catch (e: Exception) {
            Log.e("PlayerService", "Error playing audio", e)
        }
    }

    fun pauseAndResume() {
        mediaPlayer.let {
            if (stateCheck(1)) {
                it.pause()
            } else if (stateCheck(2)) {
                it.play()
            }
        }
        setPlaybackState()
        updateNotification()
    }

    fun stop() {
        mediaPlayer.let {
            if (mediaPlayer.getState() != AudiobookPlayer.AudiobookPlayerState.STOPPED) {
                it.stop()
            }
        }
        setPlaybackState()
        updateNotification()
    }

    fun getDuration(): Int = mediaPlayer.mediaPlayer?.duration ?: 1

    fun getProgress(): Int = mediaPlayer.getProgress()

    fun getFilePath(): String = mediaPlayer.getFilePath()

    fun getUri(): Uri? = musicUri

    fun seekTo(progress: Int) {
        val oldState = mediaPlayer.getState()
        mediaPlayer.skipTo(progress)
        mediaPlayer.state = oldState
        setPlaybackState()
        updateNotification()
    }

    fun setSpeed(speed: Float) {
        mediaPlayer.setPlaybackSpeed(speed)
        setPlaybackState()
        updateNotification()
    }

    fun getAlbumArt(): Bitmap? {
        val filePath = getFilePath()
        val retriever = MediaMetadataRetriever()
        var art : Bitmap? = null

        try {
            retriever.setDataSource(filePath)
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
                art = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "Error retrieving album art", e)
        } finally {
            retriever.release()
        }
        return art
    }

    fun getTitle(): String {
        val filePath = getFilePath()
        val retriever = MediaMetadataRetriever()
        var Title: String? = null

        try {
            retriever.setDataSource(filePath)
            Title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        } catch (e: Exception) {
            Log.e("PlayerService", "Error retrieving title", e)
        } finally {
            retriever.release()
        }
        if (Title == null) {
            Title = File(filePath).nameWithoutExtension
        }
        return Title
    }

    fun getArtist(): String {
        val filePath = getFilePath()
        val retriever = MediaMetadataRetriever()
        var artist: String? = null

        try {
            retriever.setDataSource(filePath)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        } catch (e: Exception) {
            Log.e("PlayerService", "Error retrieving artist", e)
        } finally {
            retriever.release()
        }
        if (artist == null) {
            artist = "Unknown Artist"
        }
        return artist
    }

    fun stateCheck(type: Int) : Boolean {
        return when (type) {
            1 -> {
                mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.PLAYING
            }
            2 -> {
                mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.PAUSED
            }
            3 -> {
                mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.STOPPED
            }
            else -> {
                mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.ERROR
            }
        }
    }

    fun complete(): Boolean {
        if (getProgress() < getDuration()) {
            return getDuration() - getProgress() <= 900
        }
        return true
    }
}