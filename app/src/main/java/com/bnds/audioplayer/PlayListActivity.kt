package com.bnds.audioplayer

import android.content.ComponentName
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
import android.os.Parcelable
import android.util.TypedValue
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bnds.audioplayer.databinding.ActivityPlayListBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class PlayListActivity : AppCompatActivity() {

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityPlayListBinding
    private var musicSize = 0
    private var speedVal: Float = 1F
    private var continuePlay: Boolean = false
    private var isInOrderQueue: Boolean = true
    private var playMethodVal: Int = 0
    private var musicPosition: Int = -1
    private val handler = Handler(Looper.getMainLooper())
    private var isNewOpen = false
    private var isDirectionChanged = false

    private lateinit var mediaPlayerService: PlayerService
    private var isBound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.PlayerBinder
            mediaPlayerService = binder.getService()
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
    private lateinit var musicAdapter: MusicAdapter
    private var layoutManagerState: Parcelable? = null
    private lateinit var refreshButton: MaterialButton
    private lateinit var musicTitle: MaterialTextView
    private lateinit var titleText: MaterialTextView
    private lateinit var settingsButton: MaterialButton
    private lateinit var playButton: MaterialButton
    private lateinit var playerButtonLayout: LinearLayout
    private lateinit var playButtonImage: ImageView
    private lateinit var skipNextButton: MaterialButton
    private lateinit var playMethodButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isNewOpen = true
        enableEdgeToEdge()
        binding = ActivityPlayListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViews()

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.data != null) {
                var speedSetting = 1F
                val transferData = result.data!!.extras
                transferData?.keySet()?.forEach { key ->
                    when (key) {
                        "Speed Values" -> speedSetting = transferData.getFloat(key)
                        "continuePlay" -> continuePlay = transferData.getBoolean(key)
                        "isInOrderQueue" -> isInOrderQueue = transferData.getBoolean(key)
                        "musicPosition" -> musicPosition = transferData.getInt(key)
                    }
                }
                checkSpeed(speedVal, speedSetting)
                speedVal = speedSetting
                if (hasPermissions()) {
                    bindService(Intent(this, PlayerService::class.java))
                } else {
                    requestPermissions()
                }
            }
            if (result.resultCode == RESULT_FIRST_USER) {
                updateMusicMarker()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startService(Intent(this, PlayerService::class.java))
                bindService(Intent(this, PlayerService::class.java)) // 如果权限授予，启动并绑定服务
            } else {
                Toast.makeText(this, "需要存储权限才能继续操作", Toast.LENGTH_SHORT).show()
            }
        }
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
        isDirectionChanged = true
        bindService(Intent(this, PlayerService::class.java))
        initializeViews()
    }

    override fun onPause() {
        super.onPause()
        layoutManagerState = recyclerView.layoutManager?.onSaveInstanceState()
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, PlayerService::class.java)
        if (isBound) { setIcon(); setImage(); setPlayingText() } else { bindService(intent) }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferencesSaveData()
        val intent = Intent(this, PlayerService::class.java)
        unbindService()
        stopService(intent)
        handler.removeCallbacksAndMessages(null)
    }

    private fun bindService(thisIntent: Intent) {
        if (!isBound) {
            bindService(thisIntent, connection, BIND_AUTO_CREATE)
        }
    }

    private fun unbindService() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun adapterBuilder() {
        val transferData = Bundle()
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        recyclerView.layoutManager = GridLayoutManager(this, if (isLandscape) 2 else 1)
        musicAdapter = MusicAdapter(
            mediaPlayerService.getMusicList(), mediaPlayerService.getBookmark()) { music ->
            setTransferData(
                transferData,
                speedVal,
                continuePlay,
                isInOrderQueue,
                mediaPlayerService.getMusicPosition(music),
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
    }

    private fun checkPlayProgress() {
        if ((musicPosition != -1 || mediaPlayerService.stateCheck(1)) &&
            musicPosition != mediaPlayerService.getThisPosition()) {
            musicPosition = mediaPlayerService.getThisPosition()
            musicTitle.text = mediaPlayerService.getPositionTitle(musicPosition)
            titleText.text = mediaPlayerService.getPositionTitle(musicPosition)
            setImage()
        }
        if (isDirectionChanged) setImage(); isDirectionChanged = false
        setIcon()
        setPlayingText()
        handler.postDelayed({ checkPlayProgress() }, (100 / speedVal).toLong())
    }

    private fun checkSpeed(oldSpeed: Float, newSpeed: Float) {
        if (oldSpeed != newSpeed) {
            mediaPlayerService.setSpeed(newSpeed)
        }
    }

    private fun handleMusicPlayback() {
        adapterBuilder()
        if (isNewOpen) {
            sharedPreferencesLoadData()
            updateMusicList()
            isNewOpen = false
        }
        if (layoutManagerState != null) {
            recyclerView.layoutManager?.onRestoreInstanceState(layoutManagerState)
        }
        mediaPlayerService.setContext(this)
        mediaPlayerService.setContinues(continuePlay)
        mediaPlayerService.setInOrderQueue(isInOrderQueue)
        setMethodVal(continuePlay, isInOrderQueue)

        titleText.setOnClickListener {
            if (musicPosition != -1)
                CenterSmoothScroller(this)
                    .smoothScrollToPositionCentered(recyclerView, musicPosition + 1)
        }

        refreshButton.setOnClickListener {
            updateMusicList()
            setUsability(musicSize != 0)
        }

        setUsability(musicSize != 0)

        mediaPlayerService.startPlaying(false, musicPosition, speedVal)

        settingsButton.setOnClickListener {
            openSettingsActivity()
        }

        playButton.setOnClickListener {
            playController(mediaPlayerService)
        }

        skipNextButton.setOnClickListener {
            playNextController(mediaPlayerService)
        }

        setMethodIcon(playMethodButton)
        playMethodButton.setOnClickListener {
            when (playMethodVal) {
                0 -> playMethodVal = 1
                1 -> playMethodVal = 2
                2 -> playMethodVal = 0
            }
            setMethodIcon(playMethodButton)
            setMethod(playMethodVal)
        }

        playerButtonLayout.setOnClickListener {
            openPlayActivity(musicPosition)
        }

        setPlayingText()
        checkPlayProgress()
        setIcon()
        setImage()
    }

    private fun hasPermissions(): Boolean {
        return requiredReadPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.playListRecyclerView)
        refreshButton = findViewById(R.id.refreshButton)
        musicTitle = findViewById(R.id.musicTitle)
        titleText = findViewById(R.id.page_title)
        settingsButton = findViewById(R.id.settingsButton)
        playButton = findViewById(R.id.playButton)
        playerButtonLayout = findViewById(R.id.playButtonLayout)
        playButtonImage = findViewById(R.id.playButtonImage)
        skipNextButton = findViewById(R.id.skipToNextButton)
        playMethodButton = findViewById(R.id.playMethodButton)

        setTitle(R.string.title_activity_play_list)

        val intent = Intent(this, PlayerService::class.java)

        if (hasPermissions()) {
            startService(intent)
            bindService(intent)
        } else {
            requestPermissions()
        }
    }

    private fun loadData() {
        if (!hasPermissions()) { requestPermissions(); return }
        mediaPlayerService.setMusicList(Scanner(this)
            .scanMusicFiles(mediaPlayerService.getMusicList()))
        musicSize = mediaPlayerService.getMusicSize()
        mediaPlayerService.updateNotification()
    }

    private fun openPlayActivity(position: Int) {
        val intent = Intent(this, PlayActivity::class.java).apply(
            fun Intent.() {
                val bookMarkerBundle = Bundle()
                for ((id, marker) in mediaPlayerService.getBookmark()) {
                    bookMarkerBundle.putLong(id.toString(), marker)
                }
                val transferData = Bundle()
                setTransferData(
                    transferData,
                    speedVal,
                    continuePlay,
                    isInOrderQueue,
                    position,
                    false
                )
                putExtras(transferData)
                putExtra("valid", true)
            }
        )
        unbindService()
        setUsability(musicSize != 0)
        handler.removeCallbacksAndMessages(null)
        activityResultLauncher.launch(intent)
    }

    private fun openSettingsActivity() {
        val intent: Intent = Intent(this, SettingsActivity::class.java).apply(
            fun Intent.() {
                val bookMarkerBundle = Bundle()
                for ((id, marker) in mediaPlayerService.getBookmark()) {
                    bookMarkerBundle.putLong(id.toString(), marker)
                }
                val transferData = Bundle()
                setTransferData(
                    transferData,
                    speedVal,
                    continuePlay,
                    isInOrderQueue,
                    null, null
                )
                putExtras(transferData)
                putExtra("valid", true)
            }
        )
        unbindService()
        handler.removeCallbacksAndMessages(null)
        activityResultLauncher.launch(intent)
    }

    private fun playController(mediaPlayerService: PlayerService) {
        mediaPlayerService.pauseAndResume()
        musicPosition = mediaPlayerService.getThisPosition()
        setIcon()
        handler.postDelayed({ setImage() }, 100)
    }

    private fun playNextController(mediaPlayerService: PlayerService) {
        mediaPlayerService.playNext()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, requiredReadPermissions, 101
        )
    }

    private fun setIcon() {
        if (!mediaPlayerService.stateCheck(1)) {
            playButton.setIconResource(R.drawable.ic_play_arrow_24px)
        } else if (mediaPlayerService.checkComplete()) {
            playButton.setIconResource(R.drawable.ic_play_arrow_24px)
        } else {
            playButton.setIconResource(R.drawable.ic_pause_circle_24px)
        }
    }

    private fun setImage() {
        if (musicPosition == -1) return
        val bitmap = mediaPlayerService.getThisAlbumArt()
        playButtonImage.setImageBitmap(bitmap)
        if (bitmap != null) playButton.setIconTintResource(R.color.white)
        else {
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
            val colorPrimary = typedValue.data
            playButton.setIconTint(ColorStateList.valueOf(colorPrimary))
        }
    }

    private fun setMethod(method: Int) {
        if (method == 0) {
            continuePlay = false
            mediaPlayerService.setContinues(false)
        } else {
            continuePlay = true
            mediaPlayerService.setContinues(true)
        }
        if (method == 2) {
            isInOrderQueue = false
            mediaPlayerService.setInOrderQueue(false)
        } else {
            isInOrderQueue = true
            mediaPlayerService.setInOrderQueue(true)
        }
    }

    private fun setMethodIcon(button: MaterialButton) {
        when (playMethodVal) {
            0 -> button.setIconResource(R.drawable.ic_play_once)
            1 -> button.setIconResource(R.drawable.ic_play_continuously)
            2 -> button.setIconResource(R.drawable.ic_play_randomly)
        }
    }

    private fun setMethodVal(continuePlay: Boolean, isInOrderQueue: Boolean) {
        playMethodVal = if (continuePlay) {
            if (isInOrderQueue) 1 else 2
        } else 0
    }

    private fun setPlayingText() {
        if (musicPosition != -1 && mediaPlayerService.getProgress() > 0) {
            musicTitle.text = mediaPlayerService.getPositionTitle(musicPosition)
            titleText.text = mediaPlayerService.getPositionTitle(musicPosition)
        } else if (mediaPlayerService.stateCheck(1)) {
            musicPosition = mediaPlayerService.getThisPosition()
            musicTitle.text = mediaPlayerService.getPositionTitle(musicPosition)
            titleText.text = mediaPlayerService.getPositionTitle(musicPosition)
        } else {
            musicTitle.text = if (musicPosition == -1) getString(R.string.defualt_playing)
            else "..."
            titleText.text = getString(R.string.title_activity_play_list)
        }
    }

    private fun setTransferData(
        transferBundle: Bundle,
        speed: Float,
        continues: Boolean?,
        isInOrderQueue: Boolean?,
        position: Int?,
        newSong: Boolean?
    ) {
        transferBundle.putFloat("Speed Values", speed)
        if (continues != null) transferBundle.putBoolean("continuePlay", continues)
        if (isInOrderQueue != null) transferBundle.putBoolean("isInOrderQueue", isInOrderQueue)
        if (position != null) transferBundle.putInt("musicPosition", position)
        if (newSong != null) transferBundle.putBoolean("newSong", newSong)
    }

    private fun setUsability(switch: Boolean) {
        playButton.isEnabled = switch
        skipNextButton.isEnabled = switch
    }

    private fun sharedPreferencesSaveData() {
        val sharedPreferences = this.getSharedPreferences("bookmarks", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val bookmarkString = mediaPlayerService.getBookmark().toString()
        editor.putString("bookmarks", bookmarkString)
        editor.apply()
    }

    private fun sharedPreferencesLoadData() {
        val sharedPreferences = this.getSharedPreferences("bookmarks", MODE_PRIVATE)
        var bookmarkString = sharedPreferences.getString("bookmarks", "")
        bookmarkString = bookmarkString?.removeSurrounding("{", "}")
        if (bookmarkString != null && bookmarkString.isNotBlank()) {
            val entries = bookmarkString.split(",")
            for (entry in entries) {
                val (key, value) = entry.split("=")
                mediaPlayerService.setBookmark(key.trim().toLong(), value.trim().toLong())
            }
        }
    }

    private fun updateMusicList() {
        if (isNewOpen) recyclerView.adapter = musicAdapter
        val oldMusicList = musicAdapter.getList()
        loadData()
        val newMusicList = mediaPlayerService.getMusicList()
        val diffResult = DiffUtil.calculateDiff(MusicDiffCallback(oldMusicList, newMusicList))
        musicAdapter.setList(newMusicList)
        diffResult.dispatchUpdatesTo(musicAdapter)

        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                mediaPlayerService.setMusicList(musicAdapter.getList())
            }
        })
    }

    private fun updateMusicMarker() {
        val bookMarker = mediaPlayerService.getBookmark()
        for (id in bookMarker.keys) {
            musicAdapter.notifyItemChanged(
                mediaPlayerService.getMusicList().indexOfFirst { it.id == id }.takeIf { it != -1 }!!
            )
        }
        var removeList =  mutableListOf<Long>()
        for (id in bookMarker.keys) {
            if (bookMarker[id] == 0L) removeList.add(id)
        }
        for (id in removeList) {
            mediaPlayerService.removeBookmark(id)
        }
    }
}