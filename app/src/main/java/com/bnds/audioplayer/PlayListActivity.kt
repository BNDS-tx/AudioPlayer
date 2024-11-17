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
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bnds.audioplayer.databinding.ActivityPlayListBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider

class PlayListActivity : AppCompatActivity() {

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityPlayListBinding
    private var musicList: List<Music> = emptyList()
    private var speedVal: Float = 1F
    private var colorVal: Int = 1
    private var continuePlay: Boolean = false
    private var musicPosition: Int = -1
    private var bookMarker: MutableMap<Long, Int> = mutableMapOf()
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
        enableEdgeToEdge()
        binding = ActivityPlayListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById(R.id.playListRecyclerView)
        refreshButton = findViewById(R.id.refreshButton)
        toPlayButton = findViewById(R.id.jumpToPlayButton)
        settingsButton = findViewById(R.id.settingsButton)
        playButton = findViewById(R.id.playButton)
        progressBar = findViewById(R.id.progressBar)

        setTitle(R.string.title_activity_play_list)

        val intent = Intent(this, Player::class.java)
        startService(intent)
        bindService(intent)

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val speedSetting = result.data!!.getFloatExtra("Speed Values", 1F)
                colorVal = result.data!!.getIntExtra("Color Values", 1)
                continuePlay = result.data!!.getBooleanExtra("continuePlay", false)
                musicPosition = result.data!!.getIntExtra("musicPosition", 0)
                val bookMarkerBundle = result.data!!.getBundleExtra("bookMarker")!!
                checkSpeed(speedVal, speedSetting)
                speedVal = speedSetting
                bookMarkerBundle.keySet()?.forEach { key ->
                    val id = key.toLongOrNull() // 将键转换回 Long
                    if (id != null) {
                        bookMarker[id] = bookMarkerBundle.getInt(key)
                    }
                }
                refreshMusicList()
                setButtonText(toPlayButton)
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

    private fun handleMusicPlayback() {
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
            updateBar(progressBar, mediaPlayer.getProgress(), mediaPlayer.getDuration())
            progressBar.addOnChangeListener { slider, value, fromUser ->
                if (fromUser) {
                    val newProgress = value.toInt()
                    mediaPlayer.seekTo(newProgress)
                }
            }
        }

        setButtonText(toPlayButton)
        toPlayButton.setOnClickListener {
            openPlayActivity(musicPosition)
        }

        checkPlayProgress()
        setIcon()
    }

    private fun bindService(thisIntent: Intent) {
        if (!isBound) {
            bindService(thisIntent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindService() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun refreshMusicList() {
        val musicScanner = Scanner(this)
        musicList = musicScanner.scanMusicFiles()
        val uriList = musicList.map { it.uri.toString() }
        val idList = musicList.map { it.id }
        val bookMarkerBundle = Bundle()
        for ((id, marker) in bookMarker) {
            bookMarkerBundle.putInt(id.toString(), marker)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MusicAdapter(musicList, bookMarker) { music ->
            val intent : Intent = Intent(this, PlayActivity::class.java).apply(
                fun Intent.() {
                    putExtra("Speed Values", speedVal)
                    putExtra("Color Values", colorVal)
                    putExtra("continuePlay", continuePlay)
                    putExtra("musicPosition", musicList.indexOf(music))
                    putExtra("bookMarker", bookMarkerBundle)
                    putExtra("musicUriList", ArrayList(uriList))
                    putExtra("musicId", idList.toLongArray())
                    putExtra("newSong", true)
                }
            )
            unbindService()
            handler.removeCallbacksAndMessages(null)
            activityResultLauncher.launch(intent)
        }
    }

    private fun checkBookmark(id: Long) : Boolean {
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

    private fun checkPlayProgress() {
        if ((mediaPlayer.stateCheck(3) || mediaPlayer.complete()) &&
            musicPosition != -1
        ) {
            handler.postDelayed({
                if (!continuePlay) {
                    mediaPlayer.stop()
                    setIcon()
                } else {
                    jumpToNextSong()
                }
            }, 1000 / speedVal.toLong())
        }
        else {
            setIcon()
            if (mediaPlayer.getProgress() != 0 && musicPosition == -1) {
                for (i in musicList.indices) {
                    if (musicList[i].uri == mediaPlayer.getUri()) {
                        musicPosition = i
                        break
                    }
                }
            }
            handler.postDelayed({ checkPlayProgress() }, 100)
        }
    }

    private fun checkSpeed(oldSpeed: Float, newSpeed: Float) {
        if (oldSpeed != newSpeed) {
            if (mediaPlayer.stateCheck(1)) {
                mediaPlayer.setSpeed(newSpeed)
            } else {
                mediaPlayer.setSpeed(newSpeed)
                mediaPlayer.pauseAndResume()
            }
        }
    }

    private fun jumpToNextSong() {
        musicPosition = (musicPosition + 1) % musicList.size
        if (!checkBookmark(musicList[musicPosition].id)) {
            tryPlay(musicPosition)
        } else {
            popupMarker()
        }
        handler.post({ checkPlayProgress() } )
        setButtonText(toPlayButton)
    }

    private fun openPlayActivity(position: Int) {
        val musicScanner = Scanner(this)
        musicList = musicScanner.scanMusicFiles()
        val uriList = musicList.map { it.uri.toString() }
        val idList = musicList.map { it.id }
        val bookMarkerBundle = Bundle()
        for ((id, marker) in bookMarker) {
            bookMarkerBundle.putInt(id.toString(), marker)
        }
        val intent = Intent(this, PlayActivity::class.java).apply(
            fun Intent.() {
                putExtra("Speed Values", speedVal)
                putExtra("Color Values", colorVal)
                putExtra("continuePlay", continuePlay)
                putExtra("musicPosition", position)
                putExtra("bookMarker", bookMarkerBundle)
                putExtra("musicUriList", ArrayList(uriList))
                putExtra("musicId", idList.toLongArray())
            }
        )
        unbindService()
        setUsability()
        handler.removeCallbacksAndMessages(null)
        activityResultLauncher.launch(intent)
    }

    private fun openSettingsActivity() {
        val intent: Intent = Intent(this, SettingsActivity::class.java).apply(
            fun Intent.() {
                val bookMarkerBundle = Bundle()
                for ((id, marker) in bookMarker) {
                    bookMarkerBundle.putInt(id.toString(), marker)
                }
                putExtra("Speed Values", speedVal)
                putExtra("Color Values", colorVal)
                putExtra("continuePlay", continuePlay)
                putExtra("musicPosition", musicPosition)
                putExtra("bookMarker", bookMarkerBundle)
            }
        )
        unbindService()
        activityResultLauncher.launch(intent)
    }

    private fun playController(mediaPlayer: Player) {
        setIcon()
        if (musicPosition != -1) {
            if (mediaPlayer.stateCheck(3)) {
                mediaPlayer.play(musicList[musicPosition].uri, speedVal)
                handler.post({ checkPlayProgress() } )
            } else {
                mediaPlayer.pauseAndResume()
            }
        } else if (mediaPlayer.getProgress() != 0) {
            mediaPlayer.pauseAndResume()
        } else {
            tryPlay(0)
            musicPosition = 0
            setButtonText(toPlayButton)
        }
        setIcon()
    }

    private fun popUpAlert() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.title_play_failure)
        builder.setMessage(R.string.expection_alart)
        builder.setPositiveButton(R.string.alart_button_sidmiss) { dialog, _ ->
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.jump_back) { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun popupMarker() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.title_play_bookmark)
        tryPlay(musicPosition)
        mediaPlayer.pauseAndResume()
        setIcon()
        builder.setMessage(R.string.bookmark_nottification)
        builder.setPositiveButton(R.string.bookmark_yes) { dialog, _ ->
            tryPlay(musicPosition)
            mediaPlayer.seekTo(bookMarker[musicList[musicPosition].id]!!)
            setIcon()
            dialog.dismiss()
        }
        builder.setNegativeButton(R.string.bookmark_no) { dialog, _ ->
            tryPlay(musicPosition)
            setIcon()
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun setButtonText(button: Button) {
        if (musicPosition != -1) {
            button.text = musicList[musicPosition].title
        } else {
            button.text = getString(R.string.defualt_playing)
        }
    }

    private fun setIcon() {
        if (mediaPlayer.stateCheck(1)) {
            playButton.setIconResource(R.drawable.ic_pause_circle_24px)
        } else {
            playButton.setIconResource(R.drawable.ic_play_arrow_24px)
        }
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

    private fun tryPlay(position: Int) {
        mediaPlayer.stop()
        setIcon()
        try {
            mediaPlayer.play(musicList[position].uri, speedVal)
        }catch  (e: Exception) {
            e.printStackTrace()
            popUpAlert()
        }
        if (mediaPlayer.stateCheck(0)) {
            popUpAlert()
        }
        setIcon()
        handler.post({ checkPlayProgress() } )
    }

    private fun updateBar(progressBar: Slider, progress: Int, duration: Int) {
        if (progress <= duration && !mediaPlayer.stateCheck(0)
            && duration != 0) {
            progressBar.value = progress.toFloat()
            progressBar.valueTo = duration.toFloat()
        }
        handler.postDelayed({ updateBar(
            progressBar, mediaPlayer.getProgress(), mediaPlayer.getDuration()
        ) }, 100)
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, Player::class.java)
        bindService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        val intent = Intent(this, Player::class.java)
        stopService(intent)
        handler.removeCallbacksAndMessages(null)
    }
}