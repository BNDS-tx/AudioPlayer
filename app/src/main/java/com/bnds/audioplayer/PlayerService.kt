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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class PlayerService : Service() {

    private val binder = PlayerBinder()
    private var mediaPlayer: ExoPlayer? = null
    var activityContext: Context? =null
    private lateinit var mediaSession: MediaSessionCompat
    private val channelId = "com.bnds.audioplayer.channel"
    private val handler = Handler(Looper.getMainLooper())
    private var isHandled = false
    private var oldDuration = 0

    private var musicListPosition: Int = -1
    private lateinit var musicList: List<Music>
    private var playbackSpeed: Float = 1.0f
    private var isContinue: Boolean = false
    private var bookMarker: MutableMap<Long, Long> = mutableMapOf()

    inner class PlayerBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = ExoPlayer.Builder(this).build()
        initializeListener()
        musicList = Scanner(this).scanMusicFiles()
        Log.d("PlayerService", "Service created")
        initializeMediaSession()
        createNotificationChannel()

        handler.postDelayed({
            updateNotification()
        }, 1000)
    }

    private fun initializeListener() {
        mediaPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        // 播放器为空闲状态
                        println("Player is idle")
                    }
                    Player.STATE_BUFFERING -> {
                        // 播放器正在缓冲
                        println("Player is buffering")
                    }
                    Player.STATE_READY -> {
                        // 播放器准备好，可以播放
                        println("Player is ready")
                    }
                    Player.STATE_ENDED -> {
                        // 播放完成
                        println("Playback ended")
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // 捕获播放错误
                PopUpWindow(this@PlayerService).popUpAlert(1)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // 播放状态切换
                if (isPlaying) {
                    updateNotification()
                } else {
                    updateNotification()
                }
            }
        })
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "PlayerMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    if (musicListPosition != -1) {
                        if (stateCheck(3)) {
                            play(musicListPosition, playbackSpeed)
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            updateNotification()
                        } else {
                            pauseAndResume()
                            if (stateCheck(1)) {
                                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            } else updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                            updateNotification()
                        }
                    } else {
                        play(0, playbackSpeed)
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        updateNotification()
                    }
                }

                override fun onPause() {
                    super.onPause()
                    pauseAndResume()
                    if (stateCheck(1)) updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    else updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    updateNotification()
                }

                override fun onStop() {
                    super.onStop()
                    stop()
                    updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                }

                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    seekTo(pos)
                    if (stateCheck(1)) {
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    } else updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    updateNotification()
                }

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    playNext()
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    playPrevious()
                }
            })
            isActive = true // 激活 MediaSession
        }
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, getProgress().toLong(), playbackSpeed)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
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
            .setContentTitle(getThisTitle())
            .setContentText(getThisArtist())
            .setSmallIcon(R.drawable.ic_notificiation_foreground)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(getContentIntent())
            .setOnlyAlertOnce(true)
            .setLargeIcon(getThisAlbumArt())
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_previous_24px,
                    "Previous",
                    getPendingIntentForAction("PLAY_PREVIOUS")
                )
            )
            .addAction(
                NotificationCompat.Action(
                    if (!isPlaying) R.drawable.ic_play_arrow_24px
                    else R.drawable.ic_pause_circle_24px,
                    "Pause&Play",
                    getPendingIntentForAction("PAUSE_PLAY_ACTION")
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_next_24px,
                    "Next",
                    getPendingIntentForAction("PLAY_NEXT")
                )
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setProgress(
                getDuration(),
                getProgress(),
                false
            )

        startForeground(1, builder.build())
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
        val intent = Intent(this, PlayActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            this,
            0,
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
                    if (mediaPlayer?.isPlaying == false) {
                        play(musicListPosition, playbackSpeed)
                    } else {
                        pauseAndResume()
                    }
                } "PLAY_NEXT" -> {
                    playNext()
                } "PLAY_PREVIOUS" -> {
                    playPrevious()
                } else -> {
                    Log.d("PlayerService", "Unknown action: $it")
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        handler.removeCallbacksAndMessages(null)
        Log.d("PlayerService", "Service destroyed")
        mediaSession.release()
    }

    fun setContext(context: Context) { activityContext = context }

    private fun uriToFilePath(context: Context, uri: Uri): String? {
        var filePath: String? = null
        if ("content".equals(uri.scheme, true)) {
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            context.contentResolver.query(uri, projection,
                null, null, null)?.use { cursor ->
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

    fun setMusicList(list: List<Music>) { this.musicList = list }

    fun setContinues(continuePlay: Boolean) { this.isContinue = continuePlay }

    fun getMusicList(): List<Music> = musicList

    fun getMusicSize(): Int = musicList.size

    fun getBookmark(): MutableMap<Long, Long> = bookMarker

    fun getMusicPosition(music: Music): Int = musicList.indexOf(music)

    fun getPositionTitle(position: Int): String = musicList[position].title

    fun getPositionId(position: Int): Long = musicList[position].id

    fun getThisPosition(): Int = musicListPosition

    fun getThisTitle(): String = musicList[if (musicListPosition == -1) 0 else musicListPosition].title

    private fun getThisArtist(): String = musicList[if (musicListPosition == -1) 0 else musicListPosition].artist

    fun getThisAlbumArt(): Bitmap? {
        val filePath = getFilePath()?: return null
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

    fun getDuration(): Int = (mediaPlayer?.duration ?: 1).toInt()

    fun getProgress(): Int = mediaPlayer?.currentPosition?.toInt() ?: 0

    fun getFilePath(): String = uriToFilePath(this, musicList[if (musicListPosition == -1) 0 else musicListPosition].uri).toString()

    fun startPlaying(new: Boolean, position: Int, speed: Float) {
        playbackSpeed = speed
        if (new) {
            if (!checkBookmark(musicList[position].id)) {
                play(position, speed)
            } else {
                PopUpWindow(this).popupMarker(position, speed)
            }
        } else if (musicListPosition == -1) {
            if (musicList.isEmpty()) {
                PopUpWindow(this).popUpAlert(musicList.size)
            }
        }
    }

    fun play(position: Int, speed: Float) {
        try {
            if (mediaPlayer?.isPlaying == true) { mediaPlayer?.stop() }
            musicListPosition = if (position == -1) 0
                else position
            val uri = musicList[musicListPosition].uri
            val bookMark = bookMarker[musicList[musicListPosition].id]
            playbackSpeed = speed
            mediaPlayer?.apply {
                setMediaItem(MediaItem.fromUri(uri), bookMark?.toLong() ?: 0)
                prepare()
                setPlaybackSpeed(playbackSpeed)
                play()
            }
            setPlaybackState()
            updateNotification()
        } catch (e: Exception) {
            Log.e("PlayerService", "Error playing audio", e)
            PopUpWindow(this).popUpAlert(musicList.size)
        }
        if (!isHandled) effectContinues(); isHandled = true
    }

    fun playNext() {
        musicListPosition = (musicListPosition + 1) % musicList.size
        if (!checkBookmark(musicList[musicListPosition].id)) {
            play(musicListPosition, playbackSpeed)
        } else {
            PopUpWindow(this).popupMarker(musicListPosition, playbackSpeed)
        }
    }

    fun playPrevious() {
        musicListPosition = (musicListPosition - 1 + musicList.size) % musicList.size
        if (!checkBookmark(musicList[musicListPosition].id)) {
            play(musicListPosition, playbackSpeed)
        } else {
            PopUpWindow(this).popupMarker(musicListPosition, playbackSpeed)
        }
    }

    fun pauseAndResume() {
        mediaPlayer?.let {
            if (stateCheck(1)) { it.pause() }
            else if (stateCheck(2)) { it.play() }
            else { play(musicListPosition, playbackSpeed) }
        }
        setPlaybackState()
        updateNotification()
    }

    fun stop() {
        mediaPlayer?.let {
            if (!stateCheck(3)) { it.stop() }
        }
        setPlaybackState()
        updateNotification()
    }

    fun seekTo(progress: Long) {
        mediaPlayer?.seekTo(progress)
    }

    fun setSpeed(speed: Float) {
        mediaPlayer?.setPlaybackSpeed(speed)
        setPlaybackState()
        updateNotification()
    }

    fun setBookmark() {
        if (!checkBookmark(musicList[musicListPosition].id)) {
            bookMarker[musicList[musicListPosition].id] = mediaPlayer?.currentPosition as Long
        } else {
            bookMarker[musicList[musicListPosition].id] = 0
        }
    }

    private fun effectContinues() {
        if (checkComplete() && musicListPosition != -1 ) {
            handler.removeCallbacks { effectContinues() }
            isHandled = false
            oldDuration = getDuration()
            if (!isContinue) { mediaPlayer?.stop() }
            else { playNext() }

        }
        else {
            handler.postDelayed({ effectContinues() }, 1100 / playbackSpeed.toLong())
        }
    }

    fun stateCheck(type: Int) : Boolean {
        return when (type) {
            1 -> {
                mediaPlayer?.isPlaying == true
            }
            2 -> {
                mediaPlayer?.playbackState == Player.STATE_READY
            }
            3 -> {
                mediaPlayer?.playbackState == Player.STATE_IDLE
            }
            4 -> {
                mediaPlayer?.playbackState == Player.STATE_ENDED
            }
            else -> {
                mediaPlayer?.playbackState == Player.STATE_BUFFERING
            }
        }
    }

    private fun checkBookmark(id: Long) : Boolean {
        if (bookMarker.isEmpty()) {
            return false
        }
        if (!bookMarker.containsKey(id)) {
            return false
        }
        if (bookMarker[id] == 0.toLong()) {
            return false
        }
        return true
    }

    fun checkComplete(): Boolean {
        return mediaPlayer?.playbackState == Player.STATE_ENDED
    }
}