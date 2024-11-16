package com.bnds.audioplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.palette.graphics.Palette
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView
import java.util.Locale

class PlayActivity : AppCompatActivity() {
    private var speedVal: Float = 1F
    private var colorVal: Int = 1
    private var continuePlay: Boolean = false
    private lateinit var musicPlayer: Player
    private var musicPosition: Int = -1
    private var bookMarker: MutableMap<Long, Int> = mutableMapOf()
    private lateinit var titleList: ArrayList<String>
    private lateinit var artistList: ArrayList<String>
    private lateinit var uriList: ArrayList<Uri>
    private lateinit var idList: ArrayList<Long>
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

    private lateinit var rootView: View
    private lateinit var playButton: MaterialButton
    private lateinit var bookMarkButton: MaterialButton
    private lateinit var showSpeed: MaterialTextView
    private lateinit var speedSlower: MaterialTextView
    private lateinit var speedFaster: MaterialTextView
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
            titleList = intent.getStringArrayListExtra("musicTitleList")!!
            artistList = intent.getStringArrayListExtra("musicArtistList")!!
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

        if (uriList.isEmpty()) {
            popUpAlert()
            playButton.isEnabled = false
            bookMarkButton.isEnabled = false
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
        if (new) {
            if (!checkBookmark(idList[musicPosition])) {
                tryPlay(musicPosition)
            } else {
                popupMarker()
            }
        } else if (musicPosition == -1) {
            if (uriList.isNotEmpty()) {
                tryPlay(0)
                musicPlayer.pauseAndResume()
            }
        } else {
            if (checkError()) {
                popUpAlert()
            }
        }

        playButton.setOnClickListener() {
            pauseOrContinue()
        }

        bookMarkButton.setOnClickListener() {
            if (musicPosition != -1) {
                setBookMark(musicPosition)
            }
            setIcon()
        }

        if (uriList.isNotEmpty()) {
            updateBar(
                progressBar, musicPlayer.getProgress(), musicPlayer.getDuration()
            )
            progressBar.addOnChangeListener { slider, value, fromUser ->
                if (fromUser) {
                    val newProgress = value.toInt()
                    musicPlayer.seekTo(newProgress)
                }
            }
        }

        setColor()

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
            updateBar(
                progressBar, musicPlayer.getProgress(), musicPlayer.getDuration()
            )
        }

        previousButton.setOnClickListener() {
            jumpAnotherSong(false)
            updateBar(
                progressBar, musicPlayer.getProgress(), musicPlayer.getDuration()
            )
        }

        backButton.setOnClickListener() {
            endActivity()
        }

        checkPlayProgress()
        updateSpeed()
        updateTitle()
        updateArt()
        setIcon()
    }

    private fun setColor() {
        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface,
            typedValue, true)
        val colorSurface = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary,
            typedValue, true)
        val colorPrimary = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceInverse,
            typedValue, true)
        val colorSurfaceInverse = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface,
            typedValue, true)
        val colorOnSurface = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary,
            typedValue, true)
        val colorOnPrimary = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceInverse,
            typedValue, true)
        val colorOnSurfaceInverse = typedValue.data
        when (colorVal) {
            1 -> {
                rootView.setBackgroundColor(colorSurface)
                updateTextColor(colorSurface, colorOnSurface, colorOnSurface)
            }
            2 -> {
                val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
                val albumDominantColor = extractDominantColor()
                if (albumDominantColor != 0) {
                    rootView.setBackgroundColor(albumDominantColor)
                    updateButtonColor(albumDominantColor, false)
                    updateBarColor(
                        lightenColor(albumDominantColor, 0.6F),
                        darkenColor(albumDominantColor, 0.4F)
                    )
                    if (isDarkMode) {
                        updateTextColor(albumDominantColor, colorOnSurfaceInverse, colorOnSurface)
                    } else {
                        updateTextColor(albumDominantColor, colorOnSurface, colorOnSurfaceInverse)
                    }
                } else {
                    rootView.setBackgroundColor(colorSurface)
                    updateButtonColor(colorPrimary, true)
                    updateBarColor(colorPrimary, colorOnSurfaceInverse)
                    updateTextColor(colorPrimary, colorOnSurface, colorOnSurface)
                }

            }
            3 -> {
                rootView.setBackgroundColor(colorSurfaceInverse)
                updateTextColor(colorSurfaceInverse, colorOnSurfaceInverse, colorOnSurfaceInverse)
            }
        }
    }

    private fun extractDominantColor(): Int {
        val defaultColor = 0
        val albumArt = musicPlayer.getAlbumArt()
        return if (albumArt != null) {
            val palette = Palette.from(albumArt).generate()
            palette.getDominantColor(defaultColor)
        } else {
            defaultColor
        }
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        val newRed = (red + (255 - red) * factor).toInt()
        val newGreen = (green + (255 - green) * factor).toInt()
        val newBlue = (blue + (255 - blue) * factor).toInt()

        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        val newRed = (red * (1 - factor)).toInt()
        val newGreen = (green * (1 - factor)).toInt()
        val newBlue = (blue * (1 - factor)).toInt()

        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun tryPlay(position: Int) {
        musicPlayer.stop()
        setIcon()
        try {
            musicPlayer.play(uriList[position], speedVal)
        }catch  (e: Exception) {
            e.printStackTrace()
            popUpAlert()
            endActivity()
        }
        if (checkError()) {
            popUpAlert()
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

    private fun setBookMark(position: Int) {
        if (!checkBookmark(idList[position])) {
            bookMarker[idList[position]] = musicPlayer.getProgress()
        } else {
            bookMarker[idList[position]] = 0
        }
    }

    private fun checkError() : Boolean {
        return musicPlayer.mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.ERROR
    }

    private fun checkPlaying() : Boolean {
        return musicPlayer.mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.PLAYING
    }

    private fun checkPaused() : Boolean {
        return musicPlayer.mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.PAUSED
    }

    private fun checkStopped() : Boolean {
        return musicPlayer.mediaPlayer.getState() == AudiobookPlayer.AudiobookPlayerState.STOPPED
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

    private fun checkLight(color: Int): Int {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF

        val brightness = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        if (brightness > 0.5) {
            return 1
        }
        return 0
    }

    private fun pauseOrContinue() {
        if (!checkStopped()) {
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
            setColor()
        }
        setIcon()
        updateTitle()
        updateArt()
    }

    private fun jumpAnotherSong(next: Boolean) {
        if (next) {
            musicPosition = (musicPosition + 1) % uriList.size
            if (!checkBookmark(idList[musicPosition])) {
                tryPlay(musicPosition)
            } else {
                popupMarker()
            }
        } else {
            musicPosition = (musicPosition - 1 + uriList.size) % uriList.size
            if (!checkBookmark(idList[musicPosition])) {
                tryPlay(musicPosition)
            } else {
                popupMarker()
            }
        }
        handler.post({ checkPlayProgress() })
        updateTitle()
        updateArt()
        setIcon()
        setColor()
    }

    private fun endActivity() {
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
        if ((checkStopped() || musicPlayer.complete()) &&
            musicPosition != -1
            ) {
            handler.postDelayed({
                if (!continuePlay) {
                    musicPlayer.stop()
                    setIcon()
                } else {
                    jumpAnotherSong(true)
                }
            }, 750 / speedVal.toLong())
        }
        else handler.postDelayed({ checkPlayProgress() }, 100)
    }

    private fun updateBar(progressBar: Slider, progress: Int, duration: Int) {
        if (progress <= duration && !checkError()) {
            progressBar.value = progress.toFloat()
            progressBar.valueTo = duration.toFloat()
        }
        handler.postDelayed({ updateBar(
            progressBar, musicPlayer.getProgress(), musicPlayer.getDuration()
        ) }, 100)
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

    private fun updateTextColor(color: Int, darkColor: Int, lightColor: Int) {
        val type = checkLight(color)
        if (type == 1) {
            titleText.setTextColor(darkColor)
            showSpeed.setTextColor(darkColor)
            val drawablesS = speedSlower.compoundDrawablesRelative
            val drawablesF = speedFaster.compoundDrawablesRelative
            val drawableEnd = drawablesS[2]
            val drawableStart = drawablesF[0]
            if (drawableEnd != null) {
                drawableEnd.setTint(darkColor)
                drawableStart.setTint(darkColor)
                speedSlower.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawablesS[0], drawablesS[1], drawableEnd, drawablesS[3]
                )
                speedFaster.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawableStart, drawablesF[1], drawablesF[2], drawablesF[3]
                )
            }

            val insetsController = window.insetsController
            insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            titleText.setTextColor(lightColor)
            showSpeed.setTextColor(lightColor)
            val drawablesS = speedSlower.compoundDrawablesRelative
            val drawablesF = speedFaster.compoundDrawablesRelative
            val drawableEnd = drawablesS[2]
            val drawableStart = drawablesF[0]
            if (drawableEnd != null) {
                drawableEnd.setTint(lightColor)
                drawableStart.setTint(lightColor)
                speedSlower.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawablesS[0], drawablesS[1], drawableEnd, drawablesS[3]
                )
                speedFaster.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawableStart, drawablesF[1], drawablesF[2], drawablesF[3]
                )
            }

            val insetsController = window.insetsController
            insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    private fun updateButtonColor(color: Int, isDefault: Boolean) {
        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary,
            typedValue, true)
        val colorOnPrimary = typedValue.data
        val iconColor: ColorStateList
        if (isDefault) {
            iconColor = ColorStateList.valueOf(colorOnPrimary)
        } else {
            if (checkLight(color) == 1) {
                iconColor = ColorStateList.valueOf(getColor(R.color.black))
            } else {
                iconColor = ColorStateList.valueOf(getColor(R.color.white))
            }
        }
        backButton.backgroundTintList = ColorStateList.valueOf(color)
        backButton.iconTint = iconColor
        backButton.setTextColor(iconColor)
        bookMarkButton.backgroundTintList = ColorStateList.valueOf(color)
        bookMarkButton.iconTint = iconColor
        bookMarkButton.setTextColor(iconColor)
        playButton.backgroundTintList = ColorStateList.valueOf(color)
        playButton.iconTint = iconColor
        nextButton.backgroundTintList = ColorStateList.valueOf(color)
        nextButton.iconTint = iconColor
        previousButton.backgroundTintList = ColorStateList.valueOf(color)
        previousButton.iconTint = iconColor
    }

    private fun updateBarColor(vibrantColor: Int, mutedColor: Int) {
        progressBar.trackActiveTintList = ColorStateList.valueOf(vibrantColor)
        progressBar.trackInactiveTintList = ColorStateList.valueOf(mutedColor)
        progressBar.thumbTintList = ColorStateList.valueOf(vibrantColor)
    }

    private fun setIcon() {
        if (checkPlaying()) {
            playButton.setIconResource(R.drawable.ic_pause_circle_24px)
        } else {
            playButton.setIconResource(R.drawable.ic_play_arrow_24px)
        }
        if (musicPosition >= 0) {
            if (checkBookmark(idList[musicPosition])) {
                bookMarkButton.setIconResource(R.drawable.ic_bookmark_check_24px)
                bookMarkButton.text = intToTime(bookMarker[idList[musicPosition]]!!)
            } else {
                bookMarkButton.setIconResource(R.drawable.ic_bookmark_add_24px)
                bookMarkButton.text = "--:--"
            }
        } else {
            bookMarkButton.setIconResource(R.drawable.ic_bookmark_add_24px)
            bookMarkButton.text = "--:--"
        }
    }

    private fun intToTime(time: Int): String {
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

    private fun popupMarker() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.title_play_bookmark)
        tryPlay(musicPosition)
        musicPlayer.pauseAndResume()
        setIcon()
        builder.setMessage(R.string.bookmark_nottification)
        builder.setPositiveButton(R.string.bookmark_yes) { dialog, _ ->
            tryPlay(musicPosition)
            musicPlayer.seekTo(bookMarker[idList[musicPosition]]!!)
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

    private fun popUpAlert() {
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

    override fun onResume() {
        super.onResume()
        bindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        endActivity()
    }
}
