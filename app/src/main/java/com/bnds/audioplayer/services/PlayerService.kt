package com.bnds.audioplayer.services

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
import com.bnds.audioplayer.fileTools.*
import com.bnds.audioplayer.*
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
                        if (checkState(3)) {
                            if (checkBookmark(musicList[musicListPosition].id))
                                PopUpWindow(this@PlayerService).popupMarker(musicListPosition, playbackSpeed)
                            else play(musicListPosition, playbackSpeed, 0)
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        } else {
                            pauseAndResume()
                            if (checkState(1)) {
                                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            } else updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        }
                    } else {
                        if (musicList.isEmpty()) PopUpWindow(this@PlayerService).popUpAlert(0)
                        else if (checkBookmark(musicList[0].id))
                            PopUpWindow(this@PlayerService).popupMarker(0, playbackSpeed)
                        else play(0, playbackSpeed, 0)
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    }
                    updateNotification()
                }

                override fun onPause() {
                    super.onPause()
                    pauseAndResume()
                    if (checkState(1)) updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
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
                    if (checkState(1)) {
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    } else updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    updateNotification()
                }

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    playNext()
                    if (checkState(1)) {
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    } else updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    updateNotification()
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    playPrevious()
                    if (checkState(1)) {
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
        if (checkState(1)) { updatePlaybackState(PlaybackStateCompat.STATE_PLAYING) }
        else if (checkState(2)) { updatePlaybackState(PlaybackStateCompat.STATE_PAUSED) }
        else if (checkState(3) || checkState(4)) { updatePlaybackState(PlaybackStateCompat.STATE_STOPPED) }
        else if (checkState(5)) { updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING) }
        else { updatePlaybackState(PlaybackStateCompat.STATE_ERROR) }
    }

    fun updateNotification() {
        val isPlaying = checkState(1)
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
                    if (checkState(3) || checkState(4)) {
                        if (checkBookmark(musicList[musicListPosition].id))
                            PopUpWindow(this).popupMarker(musicListPosition, playbackSpeed)
                        else play(musicListPosition, playbackSpeed, 0)
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

    fun getPositionTitle(position: Int): String =
        if (position in 0 until musicList.size) musicList[position].title
        else getString(R.string.defualt_playing)

    fun getPositionId(position: Int): Long =
        if (position in 0 until musicList.size) musicList[position].id
        else 0

    fun getThisPosition(): Int = musicListPosition

    fun getThisTitle(): String =
        if (musicList.isEmpty() || musicListPosition == -1) getString(R.string.defualt_playing)
        else musicList[musicListPosition].title

    private fun getThisArtist(): String =
        if (musicList.isEmpty() || musicListPosition == -1) getString(R.string.unknown_artisit)
        else musicList[musicListPosition].artist

    fun getThisAlbumArt(): Bitmap? =
        if (musicList.isEmpty() || musicListPosition == -1) null
        else
            if (musicList[musicListPosition].albumArt != null)
                musicList[musicListPosition].albumArt
            else getAlbumArtFromUri(
                musicList[musicListPosition].uri
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
            if (position in 0 until musicList.size && checkBookmark(musicList[position].id))
                PopUpWindow(this).popupMarker(position, speed)
            else play(position, speed, 0)
        } else if (musicListPosition == -1) {
            if (musicList.isEmpty()) {
                PopUpWindow(this).popUpAlert(musicList.size)
            }
        }
    }

    fun play(position: Int, speed: Float, start: Long) {
        if (!checkState(4) || !checkState(3)) { mediaPlayer?.stop() }
        musicListPosition = if (position == -1) 0
        else position
        val uri = musicList[musicListPosition].uri
        playbackSpeed = speed
        try {
            mediaPlayer?.apply {
                setMediaItem(MediaItem.fromUri(uri), start)
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
        if (checkBookmark(musicList[musicListPosition].id))
            PopUpWindow(this).popupMarker(musicListPosition, playbackSpeed)
        else play(musicListPosition, playbackSpeed, 0)

        setPlaybackState()
        updateNotification()
    }

    fun playPrevious() {
        if (isInOrderQueue) {
            musicListPosition = (musicListPosition - 1 + musicList.size) % musicList.size
        }
        if (checkBookmark(musicList[musicListPosition].id))
            PopUpWindow(this).popupMarker(musicListPosition, playbackSpeed)
        else play(musicListPosition, playbackSpeed, 0)

        setPlaybackState()
        updateNotification()
    }

    fun pauseAndResume() {
        mediaPlayer?.let {
            if (checkState(1)) { it.pause() }
            else if (checkState(2)) { it.play() }
            else {
                if (checkBookmark(musicList[if (musicListPosition == -1) 0 else musicListPosition].id))
                    PopUpWindow(this).popupMarker(musicListPosition, playbackSpeed)
                else play(musicListPosition, playbackSpeed, 0) }
        }
        setPlaybackState()
        updateNotification()
    }

    fun stop() {
        mediaPlayer?.let {
            if (!checkState(3)) { it.stop() }
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
        playbackSpeed = speed
        setPlaybackState()
        updateNotification()
    }

    fun setCurrentBookmark() {
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
        val currentMusicUri = mediaPlayer?.currentMediaItem?.localConfiguration?.uri
        if (currentMusicUri != null) {
            musicListPosition = musicList.indexOfFirst { it.uri == currentMusicUri }
        }
        if (checkState(4) && musicListPosition != -1 ) {
            if (!isContinue) { mediaPlayer?.stop() }
            else { playNext() }
        }
        handler.postDelayed({ updateInformation() }, 1000)

    }

    fun checkAvailability() {
        if (musicList.isEmpty()) {
            musicListPosition = -1
            stop()
            PopUpWindow(this).popUpAlert(musicList.size)
            return
        }
        if (musicListPosition >= musicList.size) {
            musicListPosition = -1
            stop()
            return
        }
        val currentUri = mediaPlayer?.currentMediaItem?.localConfiguration?.uri
        if (currentUri != null) {
            musicListPosition = musicList.indexOfFirst { it.uri == currentUri }
        }
        if (musicListPosition == -1) stop()
        return
    }

    fun checkState(type: Int) : Boolean {
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