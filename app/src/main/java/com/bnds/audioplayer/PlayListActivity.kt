package com.bnds.audioplayer

import android.R.attr
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bnds.audioplayer.databinding.ActivityPlayListBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider

class PlayListActivity : AppCompatActivity() {

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityPlayListBinding
    private var musicSize = 0
    private var speedVal: Float = 1F
    private var continuePlay: Boolean = false
    private var musicPosition: Int = -1
    private val handler = Handler(Looper.getMainLooper())
    private var isNewOpen = false

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

    private val requiredReadPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshButton: MaterialButton
    private lateinit var toPlayButton: MaterialButton
    private lateinit var settingsButton: MaterialButton
    private lateinit var playButton: MaterialButton
    private lateinit var playerButtonLayout: LinearLayout
    private lateinit var playButtonImage: ImageView
    private lateinit var skipNextButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isNewOpen = true
        enableEdgeToEdge()
        binding = ActivityPlayListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViews()

        val intent = Intent(this, Player::class.java)
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                var speedSetting = 1F
                val transferData = result.data!!.extras
                if (transferData != null) {
                    transferData.keySet()?.forEach { key ->
                        when (key) {
                            "Speed Values" -> speedSetting = transferData.getFloat(key)
                            "continuePlay" -> continuePlay = transferData.getBoolean(key)
                            "musicPosition" -> musicPosition = transferData.getInt(key)
                        }
                    }
                }
                checkSpeed(speedVal, speedSetting)
                speedVal = speedSetting
                bindService(intent)
            }
        }
    }

    private fun hasPermissions(): Boolean {
        return requiredReadPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, requiredReadPermissions, 101
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startService(Intent(this, Player::class.java))
                bindService(Intent(this, Player::class.java)) // 如果权限授予，启动并绑定服务
            } else {
                Toast.makeText(this, "需要存储权限才能继续操作", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleMusicPlayback() {
        if (isNewOpen) { refreshMusicList() }
        mediaPlayer.setContext(this)
        mediaPlayer.setContinues(continuePlay)

        setSupportActionBar(findViewById(R.id.toolbar))
        refreshButton.setOnClickListener {
            refreshMusicList()
            setUsability()
        }

        setUsability()

        mediaPlayer.startPlaying(false, musicPosition, speedVal)

        settingsButton.setOnClickListener {
            openSettingsActivity()
        }

        playButton.setOnClickListener {
            playController(mediaPlayer)
        }

        skipNextButton.setOnClickListener {
            mediaPlayer.playNext()
        }

        if (musicPosition == -1 && musicSize > 0) {                                                 // load the first file as default
            mediaPlayer.play(0, speedVal)
            mediaPlayer.pauseAndResume()
        }

        setButtonText(toPlayButton)
        toPlayButton.setOnClickListener {
            openPlayActivity(musicPosition)
        }
        playerButtonLayout.setOnClickListener {
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
        mediaPlayer.setMusicList(Scanner(this).scanMusicFiles())
        musicSize = mediaPlayer.getMusicSize()
        val transferData = Bundle()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MusicAdapter(
            mediaPlayer.getMusicList(), mediaPlayer.getBookmark()) { music ->
            setTransferData(
                transferData,
                speedVal,
                null,
                mediaPlayer.getMusicPosition(music),
                true
            )
            val intent : Intent = Intent(this, PlayActivity::class.java).apply(
                fun Intent.() {
                    putExtras(transferData)
                    putExtra("valid", true)
                }
            )
            unbindService()
            handler.removeCallbacksAndMessages(null)
            activityResultLauncher.launch(intent)
        }
        isNewOpen = false
    }

    private fun checkPlayProgress() {
        if (musicPosition != -1 && mediaPlayer.getProgress() > 0 &&
            toPlayButton.text != mediaPlayer.getPositionTitle(musicPosition)) {
            toPlayButton.text = mediaPlayer.getPositionTitle(musicPosition)
        }
        setIcon()
        musicPosition = mediaPlayer.getThisPosition()
        handler.postDelayed({ checkPlayProgress() }, (100 / speedVal).toLong())
    }

    private fun checkSpeed(oldSpeed: Float, newSpeed: Float) {
        if (oldSpeed != newSpeed) {
            if (mediaPlayer.stateCheck(1)) {
                mediaPlayer.setSpeed(newSpeed)
            } else if (mediaPlayer.stateCheck(2)) {
                mediaPlayer.setSpeed(newSpeed)
                mediaPlayer.pauseAndResume()
            }
        }
    }

    private fun openPlayActivity(position: Int) {
        val intent = Intent(this, PlayActivity::class.java).apply(
            fun Intent.() {
                val bookMarkerBundle = Bundle()
                for ((id, marker) in mediaPlayer.getBookmark()) {
                    bookMarkerBundle.putInt(id.toString(), marker)
                }
                val transferData = Bundle()
                setTransferData(
                    transferData,
                    speedVal,
                    null,
                    position,
                    false
                )
                putExtras(transferData)
                putExtra("valid", true)
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
                for ((id, marker) in mediaPlayer.getBookmark()) {
                    bookMarkerBundle.putInt(id.toString(), marker)
                }
                val transferData = Bundle()
                setTransferData(
                    transferData,
                    speedVal,
                    continuePlay,
                    null, null
                )
                putExtras(transferData)
                putExtra("valid", true)
            }
        )
        unbindService()
        activityResultLauncher.launch(intent)
    }

    private fun playController(mediaPlayer: Player) {
        setIcon()
        mediaPlayer.pauseAndResume()
        musicPosition = mediaPlayer.getThisPosition()
        setIcon()
    }

    private fun setButtonText(button: Button) {
        if (musicPosition != -1 && mediaPlayer.getProgress() > 0) {
            button.text = mediaPlayer.getPositionTitle(musicPosition)
        } else {
            button.text = getString(R.string.defualt_playing)
        }
    }

    private fun setIcon() {
        if (!mediaPlayer.stateCheck(1) && !mediaPlayer.checkComplete()) {
            playButton.setIconResource(R.drawable.ic_play_arrow_24px)
        } else {
            playButton.setIconResource(R.drawable.ic_pause_circle_24px)
            FileHelper.getAlbumArt(mediaPlayer.getFilePath()!!) { bitmap ->                                        // update the album art after getting it asynchronously
                playButtonImage.post {                                                          // update the UI on the main thread
                    playButtonImage.setImageBitmap(bitmap)
                    if (bitmap != null) playButton.setIconTintResource(R.color.white)
                    else {
                        val typedValue = TypedValue()
                        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
                        val colorPrimary = typedValue.data
                        playButton.setIconTint(ColorStateList.valueOf(colorPrimary))
                    }

                }
            }
        }
    }

    private fun setUsability() {
        if (musicSize == 0) {
            playButton.isEnabled = false
        } else {
            playButton.isEnabled = true
        }
    }

    private fun setTransferData(
        transferBundle: Bundle,
        speed: Float,
        continues: Boolean?,
        position: Int?,
        newSong: Boolean?
    ) {
        transferBundle.putFloat("Speed Values", speed)
        if (continues != null) transferBundle.putBoolean("continuePlay", continues)
        if (position != null) transferBundle.putInt("musicPosition", position)
        if (newSong != null) transferBundle.putBoolean("newSong", newSong)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        unbindService()
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_play_list)
        } else {
            setContentView(R.layout.activity_play_list)
        }
        isNewOpen = true
        bindService(Intent(this, Player::class.java))
        initializeViews()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.playListRecyclerView)
        refreshButton = findViewById(R.id.refreshButton)
        toPlayButton = findViewById(R.id.jumpToPlayButton)
        settingsButton = findViewById(R.id.settingsButton)
        playButton = findViewById(R.id.playButton)
        playerButtonLayout = findViewById(R.id.playButtonLayout)
        playButtonImage = findViewById(R.id.playButtonImage)
        skipNextButton = findViewById(R.id.skipToNextButton)

        setTitle(R.string.title_activity_play_list)

        val intent = Intent(this, Player::class.java)

        if (hasPermissions()) {
            startService(intent)
            bindService(intent)
        } else {
            requestPermissions()
        }

        playerButtonLayout.setOnClickListener {
            // Do nothing
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, Player::class.java)
        bindService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(this, Player::class.java)
        unbindService()
        stopService(intent)
        handler.removeCallbacksAndMessages(null)
    }
}