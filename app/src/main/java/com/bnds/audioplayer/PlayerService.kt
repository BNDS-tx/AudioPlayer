package com.bnds.audioplayer

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlin.random.Random

class PlayerService : Service() {

    private val binder = PlayerBinder()
    private var mediaPlayer: ExoPlayer? = null
    var activityContext: Context? =null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var metadata: MediaMetadataCompat
    private val channelId = "com.bnds.audioplayer.channel"
    private val handler = Handler(Looper.getMainLooper())

    private var musicListPosition: Int = -1
    private var musicList: List<Music> = listOf()
    private var playbackSpeed: Float = 1.0f
    private var isContinue: Boolean = false
    private var isInOrderQueue: Boolean = true
    private var playQueue: ArrayList<Int> = ArrayList()
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
        playQueue.clear()
        playQueue = Array(musicList.size) { it }.toCollection(playQueue)
        Log.d("PlayerService", "Service created")
        initializeMediaSession()
        createNotificationChannel()

        setPlaybackState()
        updateNotification()
        updateInformation()
        sharedPreferencesLoadData()
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
                        } else {
                            pauseAndResume()
                            if (stateCheck(1)) {
                                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            } else updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        }
                    } else {
                        play(0, playbackSpeed)
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    }
                    updateNotification()
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
                    updateNotification()
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
                    if (stateCheck(1)) {
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    } else updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    updateNotification()
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    playPrevious()
                    if (stateCheck(1)) {
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    } else updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    updateNotification()
                }
            })
            isActive = true // 激活 MediaSession
        }
    }

    private fun updatePlaybackState(state: Int) {
        val progress = getProgress()
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, progress, playbackSpeed, SystemClock.elapsedRealtime())
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

        metadata = MediaMetadataCompat.Builder()
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration())
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getThisTitle())
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getThisArtist())
            .build()

        mediaSession.setMetadata(metadata)
    }

    private fun setPlaybackState() {
        if (stateCheck(1)) { updatePlaybackState(PlaybackStateCompat.STATE_PLAYING) }
        else if (stateCheck(2)) { updatePlaybackState(PlaybackStateCompat.STATE_PAUSED) }
        else if (stateCheck(3) || stateCheck(4)) { updatePlaybackState(PlaybackStateCompat.STATE_STOPPED) }
        else if (stateCheck(5)) { updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING) }
        else { updatePlaybackState(PlaybackStateCompat.STATE_ERROR) }
    }

    fun updateNotification() {
        val isPlaying = stateCheck(1)
        val duration = getDuration()
        val progress = getProgress()

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
                duration.toInt(),
                progress.toInt(),
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

    fun setMusicList(list: List<Music>) {
        this.musicList = list
        syncBookmark()
        playQueue.clear()
        playQueue = Array(musicList.size) { it }.toCollection(playQueue)
    }

    fun setContinues(continuePlay: Boolean) { this.isContinue = continuePlay }

    fun setInOrderQueue(isInOrderQueue: Boolean) {
        this.isInOrderQueue = isInOrderQueue
        playQueue.clear()
        playQueue = Array(musicList.size) { it }.toCollection(playQueue)
    }

    fun getContinues(): Boolean = isContinue

    fun getInOrderQueue(): Boolean = isInOrderQueue

    fun getPlaybackSpeed(): Float = playbackSpeed

    fun getMusicList(): List<Music> = musicList

    fun getMusicSize(): Int = musicList.size

    fun getBookmark(): MutableMap<Long, Long> = bookMarker

    fun getMusicPosition(music: Music): Int = musicList.indexOf(music)

    fun getPositionTitle(position: Int): String = musicList[position].title

    fun getPositionId(position: Int): Long = musicList[position].id

    fun getThisPosition(): Int = musicListPosition

    fun getThisTitle(): String =
        if (musicList.isEmpty()) getString(R.string.defualt_playing)
        else musicList[if (musicListPosition == -1) 0 else musicListPosition].title

    private fun getThisArtist(): String =
        if (musicList.isEmpty()) getString(R.string.unknown_artisit)
        else musicList[if (musicListPosition == -1) 0 else musicListPosition].artist

    fun getThisAlbumArt(): Bitmap? =
        if (musicList.isEmpty()) null
        else
            if (musicList[if (musicListPosition == -1) 0 else musicListPosition].albumArt != null)
                musicList[if (musicListPosition == -1) 0 else musicListPosition].albumArt
            else getAlbumArtFromUri(
                musicList[if (musicListPosition == -1) 0 else musicListPosition].uri
            )

    private fun getAlbumArtFromUri(uri: Uri): Bitmap? {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri) // 通过 Uri 设置数据源
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
                BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AlbumArt", "Error retrieving album art from Uri: $uri", e)
            null
        } finally {
            retriever?.release()
        }
    }

    fun getDuration(): Long = mediaPlayer?.duration ?: 100000

    fun getProgress(): Long = mediaPlayer?.currentPosition ?: 0

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

    fun play(position: Int, speed: Float, start: Int? = null) {
        if (!stateCheck(4) || !stateCheck(3)) { mediaPlayer?.stop() }
        musicListPosition = if (position == -1) 0
        else position
        val uri = musicList[musicListPosition].uri
        val bookMark = bookMarker[musicList[musicListPosition].id]
        playbackSpeed = speed
        try {
            mediaPlayer?.apply {
                if (start != null) setMediaItem(MediaItem.fromUri(uri), start.toLong())
                else setMediaItem(MediaItem.fromUri(uri), bookMark?.toLong() ?: 0)
                prepare()
                setPlaybackSpeed(playbackSpeed)
                play()
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "Error playing audio", e)
            PopUpWindow(this).popUpAlert(musicList.size)
        }
        setPlaybackState()
        updateNotification()
    }

    fun playNext() {
        if (isInOrderQueue) {
            musicListPosition = (musicListPosition + 1) % musicList.size
        } else {
            playQueue.remove(musicListPosition)
            if (playQueue.isEmpty()) {
                playQueue = Array(musicList.size) { it }.toCollection(playQueue)
            }
            musicListPosition = playQueue[Random.nextInt(playQueue.size)]
        }
        if (!checkBookmark(musicList[musicListPosition].id)) {
            play(musicListPosition, playbackSpeed)
        } else {
            PopUpWindow(this).popupMarker(musicListPosition, playbackSpeed)
        }
        setPlaybackState()
        updateNotification()
    }

    fun playPrevious() {
        if (isInOrderQueue) {
            musicListPosition = (musicListPosition - 1 + musicList.size) % musicList.size
        }
        if (!checkBookmark(musicList[musicListPosition].id)) {
            play(musicListPosition, playbackSpeed)
        } else {
            PopUpWindow(this).popupMarker(musicListPosition, playbackSpeed)
        }
        setPlaybackState()
        updateNotification()
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
        setPlaybackState()
        updateNotification()
    }

    fun setSpeed(speed: Float) {
        mediaPlayer?.setPlaybackSpeed(speed)
        setPlaybackState()
        updateNotification()
    }

    fun setBookmark() {
        if (!checkBookmark(musicList[musicListPosition].id)) {
            bookMarker[musicList[musicListPosition].id] = mediaPlayer?.currentPosition as Long
            musicList[musicListPosition].bookMarker = mediaPlayer?.currentPosition
        } else {
            bookMarker.remove(musicList[musicListPosition].id)
            musicList[musicListPosition].bookMarker = null
        }
    }

    fun setBookmark(id: Long, position: Long) {
        bookMarker[id] = position
        musicList.find { it.id == id }?.bookMarker = position
    }

    fun removeBookmark(id: Long) {
        bookMarker.remove(id)
        musicList.find { it.id == id }?.bookMarker = null
    }

    fun syncBookmark() {
        for (music in musicList) {
            if (bookMarker.containsKey(music.id)) {
                bookMarker[music.id]?.let { if (it > 0L) music.bookMarker = it }
            } else { music.bookMarker = null }
        }

        for (id in bookMarker.keys) {
            if (!musicList.any { it.id == id }) { bookMarker.remove(id) }
        }
    }

    private fun updateInformation() {
        setPlaybackState()
        if (checkComplete() && musicListPosition != -1 ) {
            if (!isContinue) { mediaPlayer?.stop() }
            else { playNext() }
        }
        handler.postDelayed({ updateInformation() }, 1000 / playbackSpeed.toLong())

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
            5 -> {
                mediaPlayer?.playbackState == Player.STATE_BUFFERING
            }
            else -> {
                false
            }
        }
    }

    private fun checkBookmark(id: Long) : Boolean {
        return !(bookMarker.isEmpty() ||
                !bookMarker.containsKey(id) ||
                bookMarker[id]!! <= 0L)
    }

    fun checkComplete(): Boolean {
        return mediaPlayer?.playbackState == Player.STATE_ENDED
    }

    private fun sharedPreferencesLoadData() {
        val sharedPreferences = this.getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        var bookmarkString = sharedPreferences.getString("bookmarks", "")
        bookmarkString = bookmarkString?.removeSurrounding("{", "}")
        if (bookmarkString != null && bookmarkString.isNotBlank()) {
            val entries = bookmarkString.split(",")
            for (entry in entries) {
                val (key, value) = entry.split("=")
                setBookmark(key.trim().toLong(), value.trim().toLong())
            }
        }
    }
}