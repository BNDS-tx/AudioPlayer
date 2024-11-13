package com.bnds.audioplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bnds.audioplayer.databinding.ActivityPlayListBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PlayListActivity : AppCompatActivity() {

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityPlayListBinding
    private var musicList: List<Music> = emptyList()
    private var var1: Int = 0
    private var musicPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        // 设置状态栏的文本颜色为浅色或深色
        val insetsController = window.insetsController
        insetsController?.setSystemBarsAppearance(
            0,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        ) // 根据你的设计需求调整

        setTitle(R.string.title_activity_play_list)

        val mediaPlayer = Player.getInstance(this)

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val tvar1 = result.data!!.getIntExtra("result", 0)
                val currentPosition = result.data!!.getIntExtra("musicPosition", 0)
                var1 = tvar1
                Log.d("MyTag", "currentPosition = $currentPosition")
                musicPosition = currentPosition
                refreshMusicList()
                display(findViewById(R.id.jumpToPlayButton))
            }
            val isNull = (result.data == null)
            Log.d("MyTag", "is null = $isNull")
        }

        refreshMusicList()

        setSupportActionBar(findViewById(R.id.toolbar))
        val refreshButton = findViewById<FloatingActionButton>(R.id.refreshButton)
        refreshButton.setOnClickListener {
            refreshMusicList()
        }

        val settingsButton = findViewById<Button>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            openSettingsActivity()
        }

        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener {
            playController(mediaPlayer)
        }

        val toPlayButton = findViewById<Button>(R.id.jumpToPlayButton)
        display(toPlayButton)
        toPlayButton.setOnClickListener {
            openPlayActivity(musicPosition)
        }

    }

    private fun refreshMusicList() {
        val musicScanner = Scanner(this)
        musicList = musicScanner.scanMusicFiles()
        val titleList = musicList.map { it.title }
        val artistList = musicList.map { it.artist }
        val uriList = musicList.map { it.uri.toString() }
        val recyclerView = findViewById<RecyclerView>(R.id.playListRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MusicAdapter(musicList) { music ->
            val tvar1 = var1
            val intent : Intent = Intent(this, PlayActivity::class.java).apply(
                fun Intent.() {
                    putExtra("Music Settings", tvar1)
                    putExtra("musicPosition", musicList.indexOf(music))
                    putExtra("musicTitleList", ArrayList(titleList))
                    putExtra("musicArtistList", ArrayList(artistList))
                    putExtra("musicUriList", ArrayList(uriList))
                    putExtra("newSong", true)
                }
            )
            activityResultLauncher.launch(intent)
        }
    }

    private fun openSettingsActivity() {
        val tvar1 = var1
        val intent: Intent = Intent(this, SettingsActivity::class.java).apply(
            fun Intent.() {
                putExtra("Settings Values", tvar1)
                putExtra("musicPosition", musicPosition)
            }
        )
        activityResultLauncher.launch(intent)
    }

    private fun openPlayActivity(position: Int) {
        val tvar1 = var1
        val musicScanner = Scanner(this)
        musicList = musicScanner.scanMusicFiles()
        val titleList = musicList.map { it.title }
        val artistList = musicList.map { it.artist }
        val uriList = musicList.map { it.uri.toString() }
        val intent = Intent(this, PlayActivity::class.java).apply(
            fun Intent.() {
                putExtra("Music Settings", tvar1)
                putExtra("musicPosition", position)
                putExtra("musicTitleList", ArrayList(titleList))
                putExtra("musicArtistList", ArrayList(artistList))
                putExtra("musicUriList", ArrayList(uriList))
            }
        )
        activityResultLauncher.launch(intent)
    }

    private fun playController(mediaPlayer: Player) {
        if (musicPosition != -1) {
            mediaPlayer.pauseAndResume()
        } else {
            mediaPlayer.play(musicList[0].uri)
            musicPosition = 0
            display(findViewById(R.id.jumpToPlayButton))
        }
    }

    private fun display(button: Button) {
        if (musicPosition != -1) {
            button.text = musicList[musicPosition].title
        } else {
            button.text = getString(R.string.defualt_playing)
        }
    }

    private fun checkPosition() {

    }
}