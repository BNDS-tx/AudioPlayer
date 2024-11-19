package com.bnds.audioplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import java.util.Locale

open class PlayActivity : AppCompatActivity() {
    private var speedVal: Float = 1F
    private var colorVal: Int = 1
    lateinit var musicPlayer: Player
    var musicSize: Int = 0
    var musicPosition: Int = -1
    var bookMarker: MutableMap<Long, Int> = mutableMapOf()
    private var new: Boolean = false
    val handler = Handler(Looper.getMainLooper())

    private var isBound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as Player.PlayerBinder
            musicPlayer = binder.getService()
            isBound = true
            handleMusicPlayback()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    lateinit var rootView: View
    lateinit var playButton: MaterialButton
    lateinit var bookMarkButton: MaterialButton
    lateinit var showSpeed: MaterialTextView
    lateinit var speedSlower: MaterialTextView
    lateinit var speedFaster: MaterialTextView
    lateinit var progressBar: Slider
    lateinit var nextButton: MaterialButton
    lateinit var previousButton: MaterialButton
    lateinit var backButton: MaterialButton
    lateinit var titleText: androidx.appcompat.widget.AppCompatTextView
    lateinit var albumArt: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)
        rootView = findViewById<View>(R.id.main).rootView
        playButton = findViewById(R.id.playButton)
        bookMarkButton = findViewById(R.id.bookmarkButton)
        showSpeed = findViewById(R.id.speedShow)
        speedSlower = findViewById(R.id.speedSlower)
        speedFaster = findViewById(R.id.speedFaster)
        progressBar = findViewById(R.id.progressBar)
        nextButton = findViewById(R.id.playNextButton)
        previousButton = findViewById(R.id.playPreviousButton)
        backButton = findViewById(R.id.backButton)
        titleText = findViewById(R.id.titleText)
        albumArt = findViewById(R.id.albumArt)

        titleText.isSelected = true

        val intent : Intent = intent
        if (intent.hasExtra("valid")) {                                                       // receive bundle data pack from PlayListActivity
            val transferData = intent.extras
            if (transferData != null) {                                                             // unpack the bundle data pack if it's valid
                transferData.keySet()?.forEach { key ->
                    when (key) {
                        "Speed Values" -> speedVal = transferData.getFloat(key)
                        "Color Values" -> colorVal = transferData.getInt(key)
                        "musicPosition" -> musicPosition = transferData.getInt(key)
                        "newSong" -> new = transferData.getBoolean(key)
                    }
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->           // make the display view fitting the window
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindService()                                                                               // bind service

        onBackPressedDispatcher.addCallback(this) {                                          // force to transfer the data back to PlayListActivity
            endActivity()
        }

    }

    private fun bindService() {
        if (!isBound) {
            val intent = Intent(this, Player::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindService() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun handleMusicPlayback() {
        musicSize = musicPlayer.getMusicSize()
        setUsability()
        musicPlayer.setContext(this)

        musicPlayer.startPlaying(new, musicPosition, speedVal)
        musicPosition = musicPlayer.getThisPosition()

        playButton.setOnClickListener {
            pauseOrContinue()
        }

        bookMarkButton.setOnClickListener {
            if (musicPosition != -1) {
                musicPlayer.setBookmark()
                bookMarker = musicPlayer.getBookmark()
            }
            UIAdapter(this).setIcon()
        }

        UIAdapter(this).updateBar(                                                           // update the slider with progress
            progressBar, musicPlayer.getProgress(), musicPlayer.getDuration()
        )
        progressBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {                                     // stand by when the tracker is being dragged till it is released
            }
            override fun onStopTrackingTouch(slider: Slider) {                                      // update progress when the tracker is released
                val newProgress = slider.value.toInt()
                musicPlayer.seekTo(newProgress)
            }
        })

        speedSlower.setOnClickListener {
            speedVal -= 0.5F
            if (speedVal < 0.5F) {
                speedVal = 0.5F
            }
            setPlaySpeed(speedVal)
            updateShowSpeed()
        }
        speedFaster.setOnClickListener {
            speedVal += 0.5F
            if (speedVal > 3F) {
                speedVal = 3F
            }
            setPlaySpeed(speedVal)
            updateShowSpeed()
        }

        nextButton.setOnClickListener {
            jumpAnotherSong(true)
            UIAdapter(this).updateBar(
                progressBar, musicPlayer.getProgress(), musicPlayer.getDuration()
            )
        }

        previousButton.setOnClickListener {
            jumpAnotherSong(false)
            UIAdapter(this).updateBar(
                progressBar, musicPlayer.getProgress(), musicPlayer.getDuration()
            )
        }

        backButton.setOnClickListener {
            endActivity()
        }

        checkPlayProgress()
        updateShowSpeed()
        UIAdapter(this).updateUIGroup(colorVal)
    }

    private fun setUsability() {
        if (musicSize == 0) {
            playButton.isEnabled = false
            bookMarkButton.isEnabled = false
            speedSlower.isEnabled = false
            speedFaster.isEnabled = false
            progressBar.isEnabled = false
            nextButton.isEnabled = false
            previousButton.isEnabled = false
        } else {
            playButton.isEnabled = true
            bookMarkButton.isEnabled = true
            speedSlower.isEnabled = true
            speedFaster.isEnabled = true
            progressBar.isEnabled = true
            nextButton.isEnabled = true
            previousButton.isEnabled = true
        }
    }

    private fun setPlaySpeed(speed: Float) {
        if (musicPlayer.stateCheck(1)) {
            musicPlayer.setSpeed(speed)
        } else if (musicPlayer.stateCheck(2)) {
            musicPlayer.setSpeed(speed)
            musicPlayer.pauseAndResume()
        }
    }

    fun checkBookmark(id: Long) : Boolean {
        if (bookMarker.isEmpty()) {
            return false
        }
        if (!bookMarker.containsKey(id)) {
            return false
        }
        if (bookMarker[id] == 0) {
            return false
        }
        return true
    }

    private fun pauseOrContinue() {
        UIAdapter(this).setIcon()
        musicPosition = musicPlayer.getThisPosition()
        musicPlayer.pauseAndResume()
        UIAdapter(this).updateUIGroup(colorVal)
    }

    private fun jumpAnotherSong(next: Boolean) {
        if (next) {
            musicPlayer.playNext()
        } else {
            musicPlayer.playPrevious()
        }
        musicPosition = musicPlayer.getThisPosition()
        UIAdapter(this).updateUIGroup(colorVal)
    }

    private fun endActivity() {
        val intent2 = Intent()
        val transferData = Bundle()
        transferData.putFloat("Speed Values", speedVal)
        transferData.putInt("Color Values", colorVal)
        transferData.putInt("musicPosition", musicPosition)
        intent2.putExtras(transferData)
        setResult(RESULT_OK, intent2)
        handler.removeCallbacksAndMessages(null)
        unbindService()
        finish()
    }

    private fun checkPlayProgress() {
        if (musicPosition != musicPlayer.getThisPosition()) {
            musicPosition = musicPlayer.getThisPosition()
            bookMarker = musicPlayer.getBookmark()
            UIAdapter(this).updateUIGroup(colorVal)
        }
        handler.postDelayed({ checkPlayProgress() }, 100)
    }

    fun intToTime(time: Int): String {
        val seconds = time / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    private fun updateShowSpeed() {
        when (speedVal) {
            0.5F -> showSpeed.setText(R.string.speed_0_5)
            1F -> showSpeed.setText(R.string.speed_1)
            1.5F -> showSpeed.setText(R.string.speed_1_5)
            2F -> showSpeed.setText(R.string.speed_2)
            2.5F -> showSpeed.setText(R.string.speed_2_5)
            3F -> showSpeed.setText(R.string.speed_3)
        }
    }

    override fun onResume() {
        super.onResume()
        bindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        endActivity()
    }
}