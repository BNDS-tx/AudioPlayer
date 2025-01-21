package com.bnds.audioplayer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bnds.audioplayer.databinding.ActivityPlayBinding
import com.bnds.audioplayer.uiTools.IconTools
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView

open class PlayActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayBinding
    private var speedVal: Float = 1F
    lateinit var musicPlayerService: PlayerService
    var musicSize: Int = 0
    var musicPosition: Int = -1
    private var continuePlay: Boolean = false
    private var isInOrderQueue: Boolean = true
    private var playMethodVal: Int = 0
    var bookMarker: MutableMap<Long, Long> = mutableMapOf()
    private var new: Boolean = false
    private var openFromFile: Uri? = null
    var pauseUpdate: Boolean = false
    private var needRefresh: Boolean = false
    private val handler = Handler(Looper.getMainLooper())

    private var isBound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.PlayerBinder
            musicPlayerService = binder.getService()
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
    lateinit var bookMarkBackground: CardView
    lateinit var backButton: ImageView
    lateinit var showSpeed: MaterialTextView
    lateinit var speedSlower: MaterialTextView
    lateinit var speedFaster: MaterialTextView
    lateinit var progressBar: Slider
    lateinit var playMethodIcon: ImageView
    lateinit var playMethodBackground: CardView
    lateinit var nextButton: MaterialButton
    lateinit var previousButton: MaterialButton
    lateinit var titleText: MaterialTextView
    lateinit var titleBackground: CardView
    lateinit var albumArt: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val audioUri: Uri? = intent.data
        if (audioUri != null) openFromFile = audioUri

        val intent : Intent = intent
        if (intent.hasExtra("valid")) {                                                       // receive bundle data pack from PlayListActivity
            val transferData = intent.extras
            transferData?.keySet()?.forEach { key ->
                when (key) {
                    "musicPosition" -> musicPosition = transferData.getInt(key)
                    "newSong" -> new = transferData.getBoolean(key)
                }
            }
        }

        initializeViews()
    }

    private fun bindService() {
        if (!isBound) {
            val intent = Intent(this, PlayerService::class.java)
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    private fun unbindService() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun endActivity() {
        val intent2 = Intent()
        val transferData = Bundle()
        transferData.putFloat("Speed Values", speedVal)
        transferData.putInt("musicPosition", musicPosition)
        transferData.putBoolean("continuePlay", continuePlay)
        transferData.putBoolean("isInOrderQueue", isInOrderQueue)
        intent2.putExtras(transferData)
        if (!needRefresh) setResult(RESULT_OK, intent2)
        else setResult(RESULT_FIRST_USER, intent2)
        handler.removeCallbacksAndMessages(null)
        unbindService()
        finish()
    }

    private fun handleMusicPlayback() {
        musicSize = musicPlayerService.getMusicSize()
        setUsability()
        musicPlayerService.setContext(this)

        continuePlay = musicPlayerService.getContinues()
        isInOrderQueue = musicPlayerService.getInOrderQueue()
        speedVal = musicPlayerService.getPlaybackSpeed()

        initialMethodVal(continuePlay, isInOrderQueue)

        if (openFromFile != null) {
            for (music in musicPlayerService.getMusicList()) {
                if (getRealPathFromURI(music.uri) == getRealPathFromURI(openFromFile!!)) {
                    val position = musicPlayerService.getMusicPosition(music)
                    musicPlayerService.startPlaying(true, position, speedVal)
                    openFromFile = null
                    break
                }
            }
        } else musicPlayerService.startPlaying(new, musicPosition, speedVal); new = false

        musicPosition = musicPlayerService.getThisPosition()

        playButton.setOnClickListener {
            pauseOrContinue()
        }

        bookMarkButton.setOnClickListener {
            if (musicPosition != -1) {
                musicPlayerService.setBookmark()
                bookMarker = musicPlayerService.getBookmark()
            }
            UIAdapter(this).setIcon()
            needRefresh = !needRefresh
        }

        UIAdapter(this).updateBar(
            progressBar, musicPlayerService.getProgress(), musicPlayerService.getDuration()
        )
        progressBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                pauseUpdate = true
            }
            override fun onStopTrackingTouch(slider: Slider) {
                pauseUpdate = false
                val newProgress = slider.value.toLong()
                musicPlayerService.seekTo(newProgress)
            }
        })
        progressBar.setLabelFormatter { value ->
            val valueInMillis = value.toLong()
            val timeInMinutes = valueInMillis / 60000
            val timeInSeconds = (valueInMillis % 60000) / 1000
            val valueToInMillis = progressBar.valueTo.toLong()
            val totalInMinutes = valueToInMillis / 60000
            val totalInSeconds = (valueToInMillis % 60000) / 1000
            "$timeInMinutes:$timeInSeconds - $totalInMinutes:$totalInSeconds"
        }

        IconTools().setMethodImageIcon(playMethodIcon, playMethodVal)
        playMethodIcon.setOnClickListener {
            setMethodVal()
            IconTools().setMethodImageIcon(playMethodIcon, playMethodVal)
            setMethod(playMethodVal)
        }

        speedSlower.setOnClickListener {
            changeSpeed(true)
        }
        speedFaster.setOnClickListener {
            changeSpeed(false)
        }

        nextButton.setOnClickListener {
            jumpAnotherSong(true)
        }

        previousButton.setOnClickListener {
            jumpAnotherSong(false)
        }

        backButton.setOnClickListener {
            endActivity()
        }

        titleText.setOnClickListener {
            endActivity()
        }

        checkPlayProgress()
        updateShowSpeed()
        UIAdapter(this).updateUIGroup()
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

    private fun changeSpeed(isSlower: Boolean) {
        if (isSlower) {
            speedVal -= 0.5F
            if (speedVal < 0.5F) {
                speedVal = 0.5F
            }
        } else {
            speedVal += 0.5F
            if (speedVal > 3F) {
                speedVal = 3F
            }
        }
        setPlaySpeed(speedVal)
        updateShowSpeed()
    }

    private fun setPlaySpeed(speed: Float) {
        if (musicPlayerService.stateCheck(1)) {
            musicPlayerService.setSpeed(speed)
        } else if (musicPlayerService.stateCheck(2)) {
            musicPlayerService.setSpeed(speed)
            musicPlayerService.pauseAndResume()
        }
    }

    private fun pauseOrContinue() {
        UIAdapter(this).setIcon()
        if (musicPosition != musicPlayerService.getThisPosition()) {
            musicPosition = musicPlayerService.getThisPosition()
            musicPlayerService.pauseAndResume()
            UIAdapter(this).updateUIGroup()
        } else {
            musicPlayerService.pauseAndResume()
            UIAdapter(this).updateUIIconAndBar()
        }
    }

    private fun jumpAnotherSong(next: Boolean) {
        if (next) {
            musicPlayerService.playNext()
        } else {
            musicPlayerService.playPrevious()
        }
        musicPosition = musicPlayerService.getThisPosition()
        UIAdapter(this).updateUIGroup()
    }

    private fun checkPlayProgress() {
        bookMarker = musicPlayerService.getBookmark()
        if (musicPosition != musicPlayerService.getThisPosition()) {
            musicPosition = musicPlayerService.getThisPosition()
            UIAdapter(this).updateUIGroup()
        } else UIAdapter(this).updateUIIconAndBar()
        handler.postDelayed({ checkPlayProgress() }, 100)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        unbindService()
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_play)
        } else {
            setContentView(R.layout.activity_play)
        }
        initializeViews()
        bindService()
    }

    private fun initializeViews() {
        rootView = findViewById<View>(R.id.main).rootView
        playButton = findViewById(R.id.playButton)
        bookMarkButton = findViewById(R.id.bookmarkButton)
        bookMarkBackground = findViewById(R.id.bookmarkButtonBackground)
        backButton = findViewById(R.id.backButton)
        showSpeed = findViewById(R.id.speedShow)
        speedSlower = findViewById(R.id.speedSlower)
        speedFaster = findViewById(R.id.speedFaster)
        progressBar = findViewById(R.id.progressBar)
        playMethodIcon = findViewById(R.id.playMethodIcon)
        playMethodBackground = findViewById(R.id.methodIconBackground)
        nextButton = findViewById(R.id.playNextButton)
        previousButton = findViewById(R.id.playPreviousButton)
        titleText = findViewById(R.id.titleText)
        titleBackground = findViewById(R.id.titleBackground)
        albumArt = findViewById(R.id.albumArt)

        titleText.isSelected = true

        bindService()                                                                               // bind service

        onBackPressedDispatcher.addCallback(this) {                                          // force to transfer the data back to PlayListActivity
            endActivity()
        }
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.let {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            cursor.moveToFirst()
            val filePath = cursor.getString(columnIndex)
            cursor.close()
            return filePath
        }
        return null
    }

    private fun setMethod(method: Int) {
        if (method == 0) {
            continuePlay = false
            musicPlayerService.setContinues(false)
        } else {
            continuePlay = true
            musicPlayerService.setContinues(true)
        }
        if (method == 2) {
            isInOrderQueue = false
            musicPlayerService.setInOrderQueue(false)
        } else {
            isInOrderQueue = true
            musicPlayerService.setInOrderQueue(true)
        }
    }

    private fun setMethodVal() {
        when (playMethodVal) {
            0 -> playMethodVal = 1
            1 -> playMethodVal = 2
            2 -> playMethodVal = 0
        }
    }

    private fun initialMethodVal(continuePlay: Boolean, isInOrderQueue: Boolean) {
        playMethodVal = if (continuePlay) {
            if (isInOrderQueue) 1 else 2
        } else 0
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