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
    private var continuePlay: Boolean = false
    lateinit var musicPlayer: Player
    var musicPosition: Int = -1
    var bookMarker: MutableMap<Long, Int> = mutableMapOf()
    lateinit var uriList: ArrayList<Uri>
    lateinit var idList: ArrayList<Long>
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

        val intent : Intent = getIntent()
        if (intent != null && intent.hasExtra("Speed Values")) {
            speedVal = intent.getFloatExtra("Speed Values", 1F)
            colorVal = intent.getIntExtra("Color Values", 1)
            continuePlay = intent.getBooleanExtra("continuePlay", false)
            musicPosition = intent.getIntExtra("musicPosition", -1)
            val bookMarkerBundle = intent.getBundleExtra("bookMarker")!!
            val uriListString = intent.getStringArrayListExtra("musicUriList")!!
            val idArray = intent.getLongArrayExtra("musicId")!!
            new = intent.getBooleanExtra("newSong", false)
            bookMarkerBundle.keySet()?.forEach { key ->
                val id = key.toLongOrNull() // 将键转换回 Long
                if (id != null) {
                    bookMarker[id] = bookMarkerBundle.getInt(key)
                }
            }
            uriList = uriListString.map { Uri.parse(it) } as ArrayList<Uri>
            idList = idArray.toCollection(ArrayList())
        }

        setUsability()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindService()

        onBackPressedDispatcher.addCallback(this) {
            endActivity()
        }

    }

    protected fun bindService() {
        if (!isBound) {
            val intent = Intent(this, Player::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    protected fun unbindService() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    private fun handleMusicPlayback() {
        if (new) {
            if (!checkBookmark(idList[musicPosition])) {
                tryPlay(musicPosition)
            } else {
                PopUpWindow(this).popupMarker(musicPosition)
            }
        } else if (musicPosition == -1 &&
            !musicPlayer.stateCheck(1)) {
            if (uriList.isNotEmpty()) {
                tryPlay(0)
                musicPlayer.pauseAndResume()
            }
        } else if (musicPlayer.stateCheck(1) ||
            musicPlayer.getProgress() != 0)
        {
            val tempUri = musicPlayer.getUri()
            for (uri in uriList) {
                if (tempUri == uri) {
                    musicPosition = uriList.indexOf(uri)
                    break
                }
            }
        } else {
            if (musicPlayer.stateCheck(0)) {
                PopUpWindow(this).popUpAlert()
            }
        }

        playButton.setOnClickListener() {
            pauseOrContinue()
        }

        bookMarkButton.setOnClickListener() {
            if (musicPosition != -1) {
                setBookMark(musicPosition)
            }
            UIAdapter(this).setIcon()
        }

        UIAdapter(this).updateBar(
            progressBar, musicPlayer.getProgress(), musicPlayer.getDuration()
        )
        progressBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val newProgress = value.toInt()
                musicPlayer.seekTo(newProgress)
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
            UIAdapter(this).updateBar(
                progressBar, musicPlayer.getProgress(), musicPlayer.getDuration()
            )
        }

        previousButton.setOnClickListener() {
            jumpAnotherSong(false)
            UIAdapter(this).updateBar(
                progressBar, musicPlayer.getProgress(), musicPlayer.getDuration()
            )
        }

        backButton.setOnClickListener() {
            endActivity()
        }

        checkPlayProgress()
        updateSpeed()
        UIAdapter(this).updateUIGroup(colorVal)
    }

    private fun setUsability() {
        if (uriList.isEmpty()) {
            PopUpWindow(this).popUpAlert()
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

    fun tryPlay(position: Int) {
        musicPlayer.stop()
        UIAdapter(this).setIcon()
        try {
            musicPlayer.play(uriList[position], speedVal)
        }catch  (e: Exception) {
            e.printStackTrace()
            PopUpWindow(this).popUpAlert()
            endActivity()
        }
        if (musicPlayer.stateCheck(0)) {
            PopUpWindow(this).popUpAlert()
        }
        UIAdapter(this).setIcon()
    }

    private fun setPlaySpeed(speed: Float) {
        if (musicPlayer.stateCheck(1)) {
            musicPlayer.setSpeed(speed)
        } else if (musicPlayer.stateCheck(2)) {
            musicPlayer.setSpeed(speed)
            musicPlayer.pauseAndResume()
        }
    }

    private fun setBookMark(position: Int) {
        if (!checkBookmark(idList[position])) {
            bookMarker[idList[position]] = musicPlayer.getProgress()
        } else {
            bookMarker[idList[position]] = 0
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
        if (!musicPlayer.stateCheck(3)) {
            if (musicPosition != -1) {
                musicPlayer.pauseAndResume()
            } else {
                musicPosition = 0
                tryPlay(musicPosition)
                handler.post({ checkPlayProgress() })
            }
        } else {
            if (musicPosition == -1) {
                musicPosition = 0
                tryPlay(musicPosition)
                handler.post({ checkPlayProgress() })
            } else {
                tryPlay(musicPosition)
                handler.post({ checkPlayProgress() })
            }
        }
        UIAdapter(this).updateUIGroup(colorVal)
    }

    private fun jumpAnotherSong(next: Boolean) {
        if (next) {
            musicPosition = (musicPosition + 1) % uriList.size
            if (!checkBookmark(idList[musicPosition])) {
                tryPlay(musicPosition)
            } else {
                PopUpWindow(this).popupMarker(musicPosition)
            }
        } else {
            musicPosition = (musicPosition - 1 + uriList.size) % uriList.size
            if (!checkBookmark(idList[musicPosition])) {
                tryPlay(musicPosition)
            } else {
                PopUpWindow(this).popupMarker(musicPosition)
            }
        }
        handler.post({ checkPlayProgress() })
        UIAdapter(this).updateUIGroup(colorVal)
    }

    fun endActivity() {
        val intent2 = Intent()
        val bookMarkerBundle = Bundle()
        for ((id, marker) in bookMarker) {
            bookMarkerBundle.putInt(id.toString(), marker)
        }
        intent2.putExtra("Speed Values", speedVal)
        intent2.putExtra("Color Values", colorVal)
        intent2.putExtra("continuePlay", continuePlay)
        intent2.putExtra("bookMarker", bookMarkerBundle)
        intent2.putExtra("musicPosition", musicPosition)
        setResult(RESULT_OK, intent2)
        handler.removeCallbacksAndMessages(null)
        unbindService()
        finish()
    }

    private fun checkPlayProgress() {
        if ((musicPlayer.stateCheck(3) || musicPlayer.complete()) &&
            musicPosition != -1
            ) {
            handler.postDelayed({
                if (!continuePlay) {
                    musicPlayer.stop()
                    UIAdapter(this).setIcon()
                } else {
                    jumpAnotherSong(true)
                }
            }, 1000 / speedVal.toLong())
        }
        else {
            UIAdapter(this).setIcon()
            handler.postDelayed({ checkPlayProgress() }, 100)
        }
    }

    fun intToTime(time: Int): String {
        val seconds = time / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
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

    override fun onResume() {
        super.onResume()
        bindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        endActivity()
    }
}