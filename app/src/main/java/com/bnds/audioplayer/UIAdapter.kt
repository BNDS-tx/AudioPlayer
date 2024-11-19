package com.bnds.audioplayer

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue
import android.view.WindowInsetsController
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.palette.graphics.Palette
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

    fun updateUIGroup (colorVal: Int) {
        initColors(activity)
        setColor(colorVal)
        updateTitle()
        updateArt()
        setIcon()
    }

    private fun setColor(colorVal: Int) {
        when (colorVal) {
            1 -> {
                activity.rootView.setBackgroundColor(colorSurface)
                updateTextColor(colorSurface, colorOnSurface, colorOnSurface)
            }
            2 -> {
                val isDarkMode = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
                var albumDominantColor = extractDominantColor()
                if (activity.musicSize == 0 || albumDominantColor == 0) {
                    albumDominantColor = colorPrimaryContainer
                }
                activity.rootView.setBackgroundColor(albumDominantColor)
                if (albumDominantColor != colorPrimaryContainer) {
                    updateButtonColor(albumDominantColor)
                } else {
                    updateButtonColor(colorPrimary)
                }
                updateBarColor(
                    lightenColor(albumDominantColor),
                    darkenColor(albumDominantColor)
                )
                if (isDarkMode) {
                    updateTextColor(albumDominantColor, colorOnSurfaceInverse, colorOnSurface)
                } else {
                    updateTextColor(albumDominantColor, colorOnSurface, colorOnSurfaceInverse)
                }
            }
            3 -> {
                activity.rootView.setBackgroundColor(colorSurfaceInverse)
                updateTextColor(colorSurfaceInverse, colorOnSurfaceInverse, colorOnSurfaceInverse)
            }
        }
    }

    fun updateBar(progressBar: Slider, progress: Int, duration: Int) {
        if (progress <= duration && !activity.musicPlayer.stateCheck(0)
            && duration != 0) {
            progressBar.value = progress.toFloat()
            progressBar.valueTo = duration.toFloat()
        }
        activity.handler.postDelayed({ updateBar(
            progressBar, activity.musicPlayer.getProgress(), activity.musicPlayer.getDuration()
        ) }, 100)
    }

    private fun updateTitle() {
        activity.setTitle(R.string.title_activity_player)
        if (activity.musicPosition != -1) {
            activity.setTitle(activity.musicPlayer.getThisTitle()) }
        if (activity.titleText.text != activity.title) activity.titleText.text = activity.title
    }

    private fun updateArt() {
        if (activity.musicPosition != -1) {
            val albumArtBitmap = activity.musicPlayer.getThisAlbumArt()
            activity.albumArt.setImageBitmap(albumArtBitmap)
        }
    }

    fun setIcon() {
        if (activity.musicPlayer.stateCheck(1)) {
            activity.playButton.setIconResource(R.drawable.ic_pause_circle_24px)
        } else {
            activity.playButton.setIconResource(R.drawable.ic_play_arrow_24px)
        }
        if (activity.musicPosition >= 0) {
            if (activity.checkBookmark(
                    activity.musicPlayer.getPositionId(activity.musicPosition)
            )) {
                activity.bookMarkButton.setIconResource(R.drawable.ic_bookmark_check_24px)
                activity.bookMarkButton.text =
                    activity.bookMarker[
                        activity.musicPlayer.getPositionId(activity.musicPosition)
                    ]?.let { activity.intToTime(it) }
            } else {
                activity.bookMarkButton.setIconResource(R.drawable.ic_bookmark_add_24px)
                activity.bookMarkButton.text = "--:--"
            }
        } else {
            activity.bookMarkButton.setIconResource(R.drawable.ic_bookmark_add_24px)
            activity.bookMarkButton.text = "--:--"
        }
    }

    private fun updateTextColor(color: Int, darkColor: Int, lightColor: Int) {
        val type = checkLight(color)
        if (type == 1) {
            activity.titleText.setTextColor(darkColor)
            activity.showSpeed.setTextColor(darkColor)
            val drawablesS = activity.speedSlower.compoundDrawablesRelative
            val drawablesF = activity.speedFaster.compoundDrawablesRelative
            val drawableEnd = drawablesS[2]
            val drawableStart = drawablesF[0]
            if (drawableEnd != null) {
                drawableEnd.setTint(darkColor)
                drawableStart.setTint(darkColor)
                activity.speedSlower.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawablesS[0], drawablesS[1], drawableEnd, drawablesS[3]
                )
                activity.speedFaster.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawableStart, drawablesF[1], drawablesF[2], drawablesF[3]
                )
            }

            val insetsController = activity.window.insetsController
            insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            activity.titleText.setTextColor(lightColor)
            activity.showSpeed.setTextColor(lightColor)
            val drawablesS = activity.speedSlower.compoundDrawablesRelative
            val drawablesF = activity.speedFaster.compoundDrawablesRelative
            val drawableEnd = drawablesS[2]
            val drawableStart = drawablesF[0]
            if (drawableEnd != null) {
                drawableEnd.setTint(lightColor)
                drawableStart.setTint(lightColor)
                activity.speedSlower.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawablesS[0], drawablesS[1], drawableEnd, drawablesS[3]
                )
                activity.speedFaster.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawableStart, drawablesF[1], drawablesF[2], drawablesF[3]
                )
            }

            val insetsController = activity.window.insetsController
            insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    private fun updateButtonColor(setColor: Int) {
        val color =ColorStateList.valueOf(setColor)
        val iconColor = if (setColor == colorPrimary) {
            ColorStateList.valueOf(colorOnPrimary)
        } else if (checkLight(setColor) == 1) {
            ColorStateList.valueOf(activity.getColor(R.color.black))
        } else {
            ColorStateList.valueOf(activity.getColor(R.color.white))
        }
        activity.findViewById<CardView>(R.id.albumCard)
            .setCardBackgroundColor(colorPrimary)
        activity.findViewById<ImageView>(R.id.cardIcon)
            .setColorFilter(colorOnPrimary)
        activity.backButton.backgroundTintList = color
        activity.backButton.iconTint = iconColor
        activity.backButton.setTextColor(iconColor)
        activity.bookMarkButton.backgroundTintList = color
        activity.bookMarkButton.iconTint = iconColor
        activity.bookMarkButton.setTextColor(iconColor)
        activity.playButton.backgroundTintList = color
        activity.playButton.iconTint = iconColor
        activity.nextButton.backgroundTintList = color
        activity.nextButton.iconTint = iconColor
        activity.previousButton.backgroundTintList = color
        activity.previousButton.iconTint = iconColor
    }

    private fun updateBarColor(vibrantColor: Int, mutedColor: Int) {
        activity.progressBar.trackActiveTintList = ColorStateList.valueOf(vibrantColor)
        activity.progressBar.trackInactiveTintList = ColorStateList.valueOf(mutedColor)
        activity.progressBar.thumbTintList = ColorStateList.valueOf(vibrantColor)
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

    private fun extractDominantColor(): Int {
        val defaultColor = 0
        val albumArt = activity.musicPlayer.getThisAlbumArt()
        return if (albumArt != null) {
            val palette = Palette.from(albumArt).generate()
            palette.getDominantColor(defaultColor)
        } else {
            defaultColor
        }
    }

    private fun lightenColor(color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        val newRed = (red + (255 - red) * 0.6).toInt()
        val newGreen = (green + (255 - green) * 0.6).toInt()
        val newBlue = (blue + (255 - blue) * 0.6).toInt()

        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun darkenColor(color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        val newRed = (red * 0.6).toInt()
        val newGreen = (green * 0.6).toInt()
        val newBlue = (blue * 0.6).toInt()

        return Color.rgb(newRed, newGreen, newBlue)
    }
}