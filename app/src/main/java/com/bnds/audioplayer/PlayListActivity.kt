package com.bnds.audioplayer

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowInsetsController
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bnds.audioplayer.databinding.ActivityPlayListBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider

class PlayListActivity : AppCompatActivity() {

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityPlayListBinding
    private var musicList: List<Music> = emptyList()
    private var speedVal: Float = 1F
    private var continuePlay: Boolean = false
    private var musicPosition: Int = -1
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var mediaPlayer: Player
    private var isBound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as Player.PlayerBinder
            mediaPlayer = binder.getService()
            isBound = true

            handleMusicPlayback()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshButton: FloatingActionButton
    private lateinit var toPlayButton: MaterialButton
    private lateinit var settingsButton: MaterialButton
    private lateinit var playButton: MaterialButton
    private lateinit var progressBar: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById(R.id.playListRecyclerView)
        refreshButton = findViewById(R.id.refreshButton)
        toPlayButton = findViewById(R.id.jumpToPlayButton)
        settingsButton = findViewById(R.id.settingsButton)
        playButton = findViewById(R.id.playButton)
        progressBar = findViewById(R.id.progressBar)

        // 设置状态栏的文本颜色为浅色或深色
        val insetsController = window.insetsController
        insetsController?.setSystemBarsAppearance(
            0,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        ) // 根据你的设计需求调整

        setTitle(R.string.title_activity_play_list)

        val intent = Intent(this, Player::class.java)
        startService(intent)

        if (!isBound) {
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val speedSetting = result.data!!.getFloatExtra("Speed Values", 1F)
                continuePlay = result.data!!.getBooleanExtra("continuePlay", false)
                val currentPosition = result.data!!.getIntExtra("musicPosition", 0)
                checkSpeed(speedVal, speedSetting)
                speedVal = speedSetting
                musicPosition = currentPosition
                refreshMusicList()
                display(toPlayButton)
                setIcon()
            }
        }

        refreshMusicList()

        setSupportActionBar(findViewById(R.id.toolbar))
        refreshButton.setOnClickListener {
            refreshMusicList()
            setUsability()
        }

        setUsability()
    }

    private fun refreshMusicList() {
        val musicScanner = Scanner(this)
        musicList = musicScanner.scanMusicFiles()
        val titleList = musicList.map { it.title }
        val artistList = musicList.map { it.artist }
        val uriList = musicList.map { it.uri.toString() }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MusicAdapter(musicList) { music ->
            val intent : Intent = Intent(this, PlayActivity::class.java).apply(
                fun Intent.() {
                    putExtra("Speed Values", speedVal)
                    putExtra("continuePlay", continuePlay)
                    putExtra("musicPosition", musicList.indexOf(music))
                    putExtra("musicTitleList", ArrayList(titleList))
                    putExtra("musicArtistList", ArrayList(artistList))
                    putExtra("musicUriList", ArrayList(uriList))
                    putExtra("newSong", true)
                }
            )
            if (isBound) {
                unbindService(connection)
                isBound = false
            }
            activityResultLauncher.launch(intent)
        }
    }

    private fun handleMusicPlayback() {
        setIcon()
        settingsButton.setOnClickListener {
            openSettingsActivity()
        }

        playButton.setOnClickListener {
            playController(mediaPlayer)
        }

        if (musicList.isNotEmpty()) {
            if (musicPosition == -1) {
                mediaPlayer.play(musicList[0].uri, speedVal)
                mediaPlayer.pauseAndResume()
            }
            progressBar.valueTo = mediaPlayer.getDuration().toFloat()
            updateBar(progressBar, mediaPlayer.getProgress())
            progressBar.addOnChangeListener { slider, value, fromUser ->
                if (fromUser) {
                    val newProgress = value.toInt()
                    mediaPlayer.seekTo(newProgress)
                }
            }
        }

        display(toPlayButton)
        toPlayButton.setOnClickListener {
            openPlayActivity(musicPosition)
        }

        setIcon()
    }

    private fun checkSpeed(oldSpeed: Float, newSpeed: Float) {
        if (oldSpeed != newSpeed) {
            if (checkPlaying()) {
                mediaPlayer.setSpeed(newSpeed)
            } else {
                mediaPlayer.setSpeed(newSpeed)
                mediaPlayer.pauseAndResume()
            }
        }
    }

    private fun openSettingsActivity() {
        val intent: Intent = Intent(this, SettingsActivity::class.java).apply(
            fun Intent.() {
                putExtra("Speed Values", speedVal)
                putExtra("continuePlay", continuePlay)
                putExtra("musicPosition", musicPosition)
            }
        )
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        activityResultLauncher.launch(intent)
    }

    private fun openPlayActivity(position: Int) {
        val musicScanner = Scanner(this)
        musicList = musicScanner.scanMusicFiles()
        val titleList = musicList.map { it.title }
        val artistList = musicList.map { it.artist }
        val uriList = musicList.map { it.uri.toString() }
        val intent = Intent(this, PlayActivity::class.java).apply(
            fun Intent.() {
                putExtra("Speed Values", speedVal)
                putExtra("continuePlay", continuePlay)
                putExtra("musicPosition", position)
                putExtra("musicTitleList", ArrayList(titleList))
                putExtra("musicArtistList", ArrayList(artistList))
                putExtra("musicUriList", ArrayList(uriList))
            }
        )
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        setUsability()
        activityResultLauncher.launch(intent)
    }

    private fun playController(mediaPlayer: Player) {
        setIcon()
        if (musicPosition != -1) {
            mediaPlayer.pauseAndResume()
        } else {
            mediaPlayer.play(musicList[0].uri, speedVal)
            musicPosition = 0
            display(toPlayButton)
        }
        setIcon()
    }

    private fun updateBar(progressBar: Slider, progress: Int) {
        progressBar.value = progress.toFloat()
        progressBar.valueTo = mediaPlayer.getDuration().toFloat()
        if (checkError() || checkNotGreater()) {
            progressBar.valueTo = progressBar.value + 1
        }
        handler.postDelayed({ updateBar(progressBar, mediaPlayer.mediaPlayer.getProgress()) }, 500)
        if (progressBar.value >= progressBar.valueTo * 0.99) {
            if (continuePlay && !checkPlaying()) {
                mediaPlayer.stop()
                musicPosition = (musicPosition + 1) % musicList.size
                mediaPlayer.play(musicList[musicPosition].uri, speedVal)
                display(toPlayButton)
            }
        }
    }

    private fun checkError() : Boolean {
        return mediaPlayer.mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.ERROR
    }

    private fun checkNotGreater() : Boolean {
        return mediaPlayer.getProgress() > mediaPlayer.getDuration()
    }

    private fun checkPlaying() : Boolean {
        return mediaPlayer.mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.PLAYING
    }

    private fun setUsability() {
        if (musicList.isEmpty()) {
            playButton.isEnabled = false
            progressBar.isEnabled = false
        } else {
            playButton.isEnabled = true
            progressBar.isEnabled = true
        }
    }

    private fun display(button: Button) {
        if (musicPosition != -1) {
            button.text = musicList[musicPosition].title
        } else {
            button.text = getString(R.string.defualt_playing)
        }
    }

    private fun setIcon() {
        if (checkPlaying()) {
            playButton.setIconResource(R.drawable.ic_pause_circle_24px)
        } else {
            playButton.setIconResource(R.drawable.ic_play_arrow_24px)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        val intent = Intent(this, Player::class.java)
        stopService(intent)
    }
}