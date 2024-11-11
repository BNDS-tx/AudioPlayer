package com.bnds.audioplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PlayActivity : AppCompatActivity() {
    private var mvar1: Int = 0
    private lateinit var musicPlayer: Player
    private var musicUri: Uri? = null
    private var musicTitle: String? = null
    private var musicArtist: String? = null
    private var isPlaying: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var intent : Intent = getIntent()
        if (intent != null && intent.hasExtra("Music Settings")) {
            mvar1 = intent.getIntExtra("Music Settings", 0)
            musicTitle = intent.getStringExtra("musicTitle")
            musicArtist = intent.getStringExtra("musicArtist")
            musicUri = intent.getParcelableExtra("musicUri")
        }

        musicPlayer = Player.getInstance(this)
        musicPlayer.play(musicUri!!)
        isPlaying = true

        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener() {
            if (isPlaying) {
                musicPlayer.pause()
                isPlaying = false
            } else {
                musicPlayer.resume()
                isPlaying = true
            }
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener() {
            val intent2 = Intent()
            intent2.putExtra("result", mvar1)
            setResult(RESULT_OK, intent2)
            finish()
        }
    }


}
