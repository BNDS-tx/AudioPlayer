package com.bnds.audioplayer

import android.content.res.Configuration
import android.util.TypedValue
import android.widget.ImageView
import androidx.cardview.widget.CardView
import com.bnds.audioplayer.uiTools.ColorTools
import com.bnds.audioplayer.uiTools.IconTools
import com.google.android.material.slider.Slider

class UIAdapter(private val activity: PlayActivity) {
    private var colorSurface: Int = 0
    private var colorPrimary: Int = 0
    private var colorPrimaryContainer: Int = 0
    private var colorSurfaceInverse: Int = 0
    private var colorOnPrimary: Int = 0
    private var colorOnSurface: Int = 0
    private var colorOnSurfaceInverse: Int = 0

    private fun initColors(activity: PlayActivity) {
        val typedValue = TypedValue()
        val theme = activity.theme
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        colorSurface = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        colorPrimary = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)
        colorPrimaryContainer = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceInverse, typedValue, true)
        colorSurfaceInverse = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, typedValue, true)
        colorOnPrimary = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        colorOnSurface = typedValue.data
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceInverse, typedValue, true)
        colorOnSurfaceInverse = typedValue.data
    }

    fun updateUIGroup () {
        initColors(activity)
        setColor()
        updateTitle()
        updateArt()
        setIcon()
        updateBar(
            activity.progressBar,
            activity.musicPlayerService.getProgress(),
            activity.musicPlayerService.getDuration()
        )
    }

    fun updateUIIconAndBar () {
        setIcon()
        updateBar(
            activity.progressBar,
            activity.musicPlayerService.getProgress(),
            activity.musicPlayerService.getDuration()
        )
    }

    private fun setColor() {
        val isDarkMode = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        var albumDominantColor =
            if (activity.musicPosition == -1) 0
            else if (activity.musicSize == 0) 0
            else ColorTools().extractDominantColor(activity.musicPlayerService.getThisAlbumArt())
        ColorTools().updatePageColor(
            albumDominantColor,
            activity.rootView,
            activity.findViewById<CardView>(R.id.albumCard),
            activity.findViewById<ImageView>(R.id.cardIcon),
            activity.titleBackground,
            activity.titleText,
            activity.backButton,
            activity.bookMarkBackground,
            activity.bookMarkButton,
            activity.playButton,
            activity.nextButton,
            activity.previousButton,
            activity.playMethodBackground,
            activity.playMethodIcon,
            activity
        )
        ColorTools().setBarColor(activity.progressBar, albumDominantColor)
        if (isDarkMode) {
            ColorTools().updateTextsColor(
                albumDominantColor,
                colorOnSurfaceInverse,
                colorOnSurface,
                activity.speedSlower,
                activity.speedFaster,
                activity.showSpeed,
                activity
            )
        } else {
            ColorTools().updateTextsColor(
                albumDominantColor,
                colorOnSurface,
                colorOnSurfaceInverse,
                activity.speedSlower,
                activity.speedFaster,
                activity.showSpeed,
                activity
            )
        }
    }

    fun updateBar(progressBar: Slider, progress: Long, duration: Long) {
        if (activity.pauseUpdate) return
        if (progress <= duration && !activity.musicPlayerService.stateCheck(0)
            && duration != 0.toLong()) {
            progressBar.value = progress.toFloat()
            progressBar.valueTo = duration.toFloat()
        }
    }

    private fun updateTitle() {
        activity.setTitle(R.string.title_activity_player)
        if (activity.musicPosition != -1) {
            activity.setTitle(activity.musicPlayerService.getThisTitle()) }
        if (activity.titleText.text != activity.title) activity.titleText.text = activity.title
    }

    private fun updateArt() {
        if (activity.musicPosition != -1) {
            val albumArtBitmap = activity.musicPlayerService.getThisAlbumArt()
            activity.albumArt.setImageBitmap(albumArtBitmap)
        }
    }

    fun setIcon() {
        IconTools().setPlayIcon(activity.playButton, activity.musicPlayerService.stateCheck(1))
        if (activity.musicPosition >= 0) {
            IconTools().setBookmarkIcon(activity.bookMarkButton,
                activity.bookMarker[
                    activity.musicPlayerService.getPositionId(activity.musicPosition)
            ])
        } else {
            IconTools().setBookmarkIcon(activity.bookMarkButton, null)
        }
    }
}