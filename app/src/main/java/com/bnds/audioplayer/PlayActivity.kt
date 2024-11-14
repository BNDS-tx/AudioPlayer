package com.bnds.audioplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider

class PlayActivity : AppCompatActivity() {
    private var speedVal: Float = 1F
    private var continuePlay: Boolean = false
    private lateinit var musicPlayer: Player
    private var musicPosition: Int = -1
    private lateinit var titleList: ArrayList<String>
    private lateinit var artistList: ArrayList<String>
    private lateinit var uriList: ArrayList<Uri>
    private var new: Boolean = false
    private val handler = Handler(Looper.getMainLooper())

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

    private lateinit var playButton: MaterialButton
    private lateinit var showSpeed: TextView
    private lateinit var speedSlower: TextView
    private lateinit var speedFaster: TextView
    private lateinit var progressBar: Slider
    private lateinit var nextButton: MaterialButton
    private lateinit var previousButton: MaterialButton
    private lateinit var backButton: MaterialButton
    private lateinit var titleText: androidx.appcompat.widget.AppCompatTextView
    private lateinit var albumArt: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)
        playButton = findViewById(R.id.playButton)
        showSpeed = findViewById(R.id.speedShow)
        speedSlower = findViewById(R.id.speedSlower)
        speedFaster = findViewById(R.id.speedFaster)
        progressBar = findViewById(R.id.progressBar)
        nextButton = findViewById(R.id.playNextButton)
        previousButton = findViewById(R.id.playPreviousButton)
        backButton = findViewById(R.id.backButton)
        titleText = findViewById(R.id.titleText)
        albumArt = findViewById(R.id.albumArt)

        var intent : Intent = getIntent()
        if (intent != null && intent.hasExtra("Speed Values")) {
            speedVal = intent.getFloatExtra("Speed Values", 1F)
            continuePlay = intent.getBooleanExtra("continuePlay", false)
            musicPosition = intent.getIntExtra("musicPosition", -1)
            titleList = intent.getStringArrayListExtra("musicTitleList")!!
            artistList = intent.getStringArrayListExtra("musicArtistList")!!
            val uriListString = intent.getStringArrayListExtra("musicUriList")!!
            new = intent.getBooleanExtra("newSong", false)
            uriList = uriListString.map { Uri.parse(it) } as ArrayList<Uri>
        }

        if (uriList.isEmpty()) {
            popUpScreen()
            playButton.isEnabled = false
            speedSlower.isEnabled = false
            speedFaster.isEnabled = false
            progressBar.isEnabled = false
            nextButton.isEnabled = false
            previousButton.isEnabled = false
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        updateTitle()

        bindService()

        onBackPressedDispatcher.addCallback(this) {
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

        setIcon()
        updateTitle()
        updateSpeed()

        if (new) {
            musicPlayer.stop()
            tryPlay(musicPosition)
        } else if (musicPosition == -1) {
            if (uriList.isNotEmpty()) {
                musicPlayer.stop()
                tryPlay(0)
                musicPlayer.pauseAndResume()
            }
        } else {
            if (checkError()) {
                popUpScreen()
            }
        }

        playButton.setOnClickListener() {
            pauseOrContinue()
        }

        if (uriList.isNotEmpty()) {
            progressBar.valueTo = musicPlayer.getDuration().toFloat()
            updateBar(progressBar, musicPlayer.getProgress())
            progressBar.addOnChangeListener { slider, value, fromUser ->
                if (fromUser) {
                    val newProgress = value.toInt()
                    musicPlayer.seekTo(newProgress)
                }
            }
        }


        speedSlower.setOnClickListener() {
            speedVal -= 0.5F
            if (speedVal < 0.5F) {
                speedVal = 0.5F
            }
            setPlaySpeed(speedVal)
            updateSpeed()
        }
        speedFaster.setOnClickListener() {
            speedVal += 0.5F
            if (speedVal > 3F) {
                speedVal = 3F
            }
            setPlaySpeed(speedVal)
            updateSpeed()
        }

        nextButton.setOnClickListener() {
            jumpAnotherSong(true)
            updateBar(progressBar, musicPlayer.getProgress())
        }

        previousButton.setOnClickListener() {
            jumpAnotherSong(false)
            updateBar(progressBar, musicPlayer.getProgress())
        }

        backButton.setOnClickListener() {
            endActivity()
        }

        updateSpeed()
        updateTitle()
        updateArt()
        setIcon()
    }

    private fun tryPlay(position: Int) {
        setIcon()
        try {
            musicPlayer.play(uriList[position], speedVal)
        }catch  (e: Exception) {
            e.printStackTrace()
            popUpScreen()
            endActivity()
        }
        if (checkError()) {
            popUpScreen()
        }
        setIcon()
    }

    private fun setPlaySpeed(speed: Float) {
        if (checkPlaying()) {
            musicPlayer.setSpeed(speed)
        } else if (checkPaused()) {
            musicPlayer.setSpeed(speed)
            musicPlayer.pauseAndResume()
        }
    }

    private fun checkError() : Boolean {
        bindService()
        return musicPlayer.mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.ERROR
    }

    private fun checkPlaying() : Boolean {
        return musicPlayer.mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.PLAYING
    }

    private fun checkPaused() : Boolean {
        return musicPlayer.mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.PAUSED
    }

    private fun checkNotGreater() : Boolean {
        return musicPlayer.getProgress() > musicPlayer.getDuration()
    }

    private fun pauseOrContinue() {
        if (musicPosition != -1) {
            musicPlayer.pauseAndResume()
            setIcon()
        }
        else {
            musicPosition = 0
            tryPlay(musicPosition)
            setIcon()
        }
        updateTitle()
        updateArt()
    }

    private fun jumpAnotherSong(next: Boolean) {
        if (next) {
            musicPlayer.stop()
            musicPosition = (musicPosition + 1) % uriList.size
            tryPlay(musicPosition)
        } else {
            musicPlayer.stop()
            musicPosition = (musicPosition - 1 + uriList.size) % uriList.size
            tryPlay(musicPosition)
        }
        updateTitle()
        updateArt()
        setIcon()
    }

    private fun endActivity() {
        val intent2 = Intent()
        intent2.putExtra("Speed Values", speedVal)
        intent2.putExtra("continuePlay", continuePlay)
        intent2.putExtra("musicPosition", musicPosition)
        setResult(RESULT_OK, intent2)
        handler.removeCallbacksAndMessages(null)
        unbindService()
        finish()
    }

    private fun updateBar(progressBar: Slider, progress: Int) {
        progressBar.value = progress.toFloat()
        progressBar.valueTo = musicPlayer.getDuration().toFloat()
        if (checkError() || checkNotGreater()) {
            progressBar.valueTo = progressBar.value + 1
        }
        handler.postDelayed({ updateBar(progressBar, musicPlayer.getProgress()) }, 500)
    }

    private fun updateTitle() {
        setTitle(R.string.title_activity_player)
        if (musicPosition != -1) { setTitle(titleList[musicPosition]) }
        titleText.text = title
    }

    private fun updateArt() {
        if (musicPosition != -1) {
            val albumArtBitmap = musicPlayer.getAlbumArt()
            albumArt.setImageBitmap(albumArtBitmap)
        }
    }

    private fun setIcon() {
        if (checkPlaying()) {
            playButton.setIconResource(R.drawable.ic_pause_circle_24px)
        } else {
            playButton.setIconResource(R.drawable.ic_play_arrow_24px)
        }
    }

    private fun updateSpeed() {
        when (speedVal) {
            0.5F -> showSpeed.text = "0.5X"
            1F -> showSpeed.text = "1X"
            1.5F -> showSpeed.text = "1.5X"
            2F -> showSpeed.text = "2X"
            2.5F -> showSpeed.text = "2.5X"
            3F -> showSpeed.text = "3X"
        }
    }

    private fun popUpScreen() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.title_play_failure)
        if (uriList.isEmpty()) {
            builder.setMessage(R.string.null_alart)
            builder.setNegativeButton(R.string.jump_back) { dialog, _ ->
                dialog.dismiss()
                endActivity()
            }
        } else {
            builder.setMessage(R.string.expection_alart)
            builder.setPositiveButton(R.string.alart_button_sidmiss) { dialog, _ ->
                dialog.dismiss()
            }
            builder.setNegativeButton(R.string.jump_back) { dialog, _ ->
                dialog.dismiss()
                endActivity()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }
}
