package com.bnds.audioplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.slider.Slider

class PlayActivity : AppCompatActivity() {
    private var mvar1: Int = 0
    private lateinit var musicPlayer: Player
    private var musicPosition: Int = -1
    private lateinit var titleList: ArrayList<String>
    private lateinit var artistList: ArrayList<String>
    private lateinit var uriList: ArrayList<Uri>
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        var new = false
        var intent : Intent = getIntent()
        if (intent != null && intent.hasExtra("Music Settings")) {
            mvar1 = intent.getIntExtra("Music Settings", 0)
            musicPosition = intent.getIntExtra("musicPosition", -1)
            titleList = intent.getStringArrayListExtra("musicTitleList")!!
            artistList = intent.getStringArrayListExtra("musicArtistList")!!
            val uriListString = intent.getStringArrayListExtra("musicUriList")!!
            new = intent.getBooleanExtra("newSong", false)
            uriList = uriListString.map { Uri.parse(it) } as ArrayList<Uri>
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        updateTitle()

        musicPlayer = Player.getInstance(this)
        if (new) {
            musicPlayer.stop()
            musicPlayer.play(uriList[musicPosition])
        } else if (musicPosition == -1) {
            if (uriList.isNotEmpty()) {
                musicPlayer.stop()
                musicPlayer.play(uriList[0])
                musicPlayer.pauseAndResume()
            } else if (uriList.isEmpty()) {
                musicPlayer.stop()
                finish()
            }
        }

        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener() {
            pauseOrContinue()
        }

        val progressBar = findViewById<Slider>(R.id.progressBar)
        progressBar.valueTo = musicPlayer.getDuration().toFloat()
        updateBar(progressBar, musicPlayer.getProgress())

        updateArt()

        val nextButton = findViewById<Button>(R.id.playNextButton)
        nextButton.setOnClickListener() {
            jumpAnotherSong(true)
        }

        val previousButton = findViewById<Button>(R.id.playPreviousButton)
        previousButton.setOnClickListener() {
            jumpAnotherSong(false)
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener() {
            endActivity()
        }

        onBackPressedDispatcher.addCallback(this) {
            endActivity()
        }

    }

    private fun pauseOrContinue() {
        if (musicPosition != -1) { musicPlayer.pauseAndResume() }
        else {
            musicPosition = 0
            musicPlayer.play(uriList[musicPosition])
        }
        updateTitle()
        updateArt()
    }

    private fun jumpAnotherSong(next: Boolean) {
        if (next) {
            musicPlayer.stop()
            musicPosition = (musicPosition + 1) % uriList.size
            musicPlayer.play(uriList[musicPosition])
        } else {
            musicPlayer.stop()
            musicPosition = (musicPosition - 1 + uriList.size) % uriList.size
            musicPlayer.play(uriList[musicPosition])
        }
        updateTitle()
        updateArt()
    }

    private fun endActivity() {
        val intent2 = Intent()
        intent2.putExtra("result", mvar1)
        intent2.putExtra("musicPosition", musicPosition)
        setResult(RESULT_OK, intent2)
        handler.removeCallbacksAndMessages(null)
        finish()
    }

    private fun updateBar(progressBar: Slider, progress: Int) {
        progressBar.value = progress.toFloat()
        handler.postDelayed({ updateBar(progressBar, musicPlayer.getProgress()) }, 500)
    }

    private fun updateTitle() {
        val titleText = findViewById<androidx.appcompat.widget.AppCompatTextView>(R.id.titleText)
        setTitle(R.string.title_activity_player)
        if (musicPosition != -1) { setTitle(titleList[musicPosition]) }
        titleText.text = title
    }

    private fun updateArt() {
        val albumArt = findViewById<ImageView>(R.id.albumArt)
        if (musicPosition != -1) {
            val albumArtBitmap = musicPlayer.getAlbumArt(uriList[musicPosition])
            albumArt.setImageBitmap(albumArtBitmap)
        }
    }
}
