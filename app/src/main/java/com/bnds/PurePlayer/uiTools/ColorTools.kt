package com.bnds.PurePlayer.uiTools

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.bnds.PurePlayer.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider

class ColorTools {

    companion object {
        private var colorSurface: Int = 0
        private var colorSurfaceInverse: Int = 0
        private var colorPrimary: Int = 0
        private var colorPrimaryContainer: Int = 0
        private var colorOnPrimary: Int = 0
        private var colorOnSurface: Int = 0
        private var colorOnSurfaceInverse: Int = 0

        fun initColors(activity: Activity) {
            val typedValue = TypedValue()
            val theme = activity.theme
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurface,
                typedValue,
                true
            )
            colorSurface = typedValue.data
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurfaceInverse,
                typedValue,
                true
            )
            colorSurfaceInverse = typedValue.data
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorPrimary,
                typedValue,
                true
            )
            colorPrimary = typedValue.data
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorPrimaryContainer,
                typedValue,
                true
            )
            colorPrimaryContainer = typedValue.data
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnPrimary,
                typedValue,
                true
            )
            colorOnPrimary = typedValue.data
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface,
                typedValue,
                true
            )
            colorOnSurface = typedValue.data
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurfaceInverse,
                typedValue,
                true
            )
            colorOnSurfaceInverse = typedValue.data
        }

        fun extractDominantColor(albumArt: Bitmap?): Int {
            if (albumArt != null) {
                val palette = Palette.from(albumArt).generate()
                return palette.getDominantColor(0)
            } else return 0
        }

        fun updatePageColor(
            setColor: Int, background: View,
            albumCard: CardView, cardIcon: ImageView,
            titleBackground: CardView, titleText: TextView, backButton: ImageView,
            bookmarkBackground: CardView, bookMarkButton: MaterialButton,
            playButton: MaterialButton, nextButton: MaterialButton, previousButton: MaterialButton,
            playMethodBackground: CardView, playMethodIcon: ImageView,
            progressBar: Slider,
            activity: Activity
        ) {
            initColors(activity)
            val color = if (setColor == 0) colorPrimaryContainer
            else setColor
            val iconColor = if (color == colorPrimaryContainer) colorPrimary
            else if (checkIsLightColor(color) == 1) activity.getColor(R.color.black)
            else activity.getColor(R.color.white)

            setBackgroundColor(color, background)
            setCardColor(colorPrimary, albumCard)
            if (setColor == 0) {
                setCardColor(colorPrimary, titleBackground)
                setImageViewColor(colorPrimaryContainer, backButton)
                setTextColor(colorPrimaryContainer, titleText)
            } else {
                setCardColor(color, titleBackground)
                setImageViewColor(iconColor, backButton)
                setTextColor(iconColor, titleText)
            }
            setCardColor(color, bookmarkBackground)
            setButtonTextColor(iconColor, bookMarkButton)
            setButtonIconColor(iconColor, bookMarkButton)
            setButtonColor(iconColor, playButton)
            setButtonIconColor(color, playButton)
            setButtonIconColor(iconColor, nextButton)
            setButtonIconColor(iconColor, previousButton)
            setCardColor(color, playMethodBackground)
            setImageViewColor(iconColor, playMethodIcon)
            setBarColor(iconColor, color, progressBar)
        }

        private fun setBackgroundColor(color: Int, background: View) {
            background.setBackgroundColor(color)
        }

        private fun setCardColor(color: Int, card: CardView) {
            card.backgroundTintList = ColorStateList.valueOf(color)
        }

        private fun setImageViewColor(color: Int, image: ImageView) {
            image.imageTintList = ColorStateList.valueOf(color)
        }

        private fun setButtonColor(color: Int, button: MaterialButton) {
            button.backgroundTintList = ColorStateList.valueOf(color)
        }

        private fun setButtonTextColor(color: Int, button: MaterialButton) {
            button.setTextColor(color)
        }

        private fun setButtonIconColor(color: Int, button: MaterialButton) {
            button.iconTint = ColorStateList.valueOf(color)
        }

        private fun setBarColor(vibrantColor: Int, mutedColor: Int, sysBar: Slider) {
            sysBar.trackActiveTintList = ColorStateList.valueOf(vibrantColor)
            sysBar.trackInactiveTintList = ColorStateList.valueOf(
                if (mutedColor == colorPrimaryContainer) colorSurface
                else {
                    if (checkIsLightColor(mutedColor) == 1) ColorUtils.blendARGB(mutedColor, Color.BLACK, 0.2f)
                    else ColorUtils.blendARGB(mutedColor, Color.WHITE, 0.2f)
                }
            )
            sysBar.thumbTintList = ColorStateList.valueOf(vibrantColor)
        }

        fun updateTextsColor(
            setColor: Int, isDarkMode: Boolean,
            speedSlower: TextView, speedFaster: TextView, showSpeed: TextView,
            activity: Activity
        ) {
            initColors(activity)
            val color = if (setColor == 0) colorPrimaryContainer else setColor
            val darkColor = if (isDarkMode) colorOnSurfaceInverse
            else if (setColor == 0) colorPrimary
            else colorOnSurface
            val lightColor = if (isDarkMode) colorOnSurface
            else colorOnSurfaceInverse
            val type = checkIsLightColor(color)
            if (type == 1) {
                setTextColor(darkColor, showSpeed)
                setTextIconColor(darkColor, speedSlower, 2)
                setTextIconColor(darkColor, speedFaster, 0)

                val insetsController = activity.window.insetsController
                insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                setTextColor(lightColor, showSpeed)
                setTextIconColor(lightColor, speedSlower, 2)
                setTextIconColor(lightColor, speedFaster, 0)

                val insetsController = activity.window.insetsController
                insetsController?.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }

        private fun setTextColor(color: Int, text: TextView) {
            text.setTextColor(color)
        }

        private fun setTextIconColor(color: Int, text: TextView, location: Int) {
            val drawables = text.compoundDrawablesRelative
            val drawable = drawables[location]
            drawable.setTint(color)
            drawables[location] = drawable
            text.setCompoundDrawablesRelative(
                drawables[0],
                drawables[1],
                drawables[2],
                drawables[3]
            )
        }

        private fun checkIsLightColor(color: Int): Int {
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF

            val brightness = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
            if (brightness > 0.5) {
                return 1
            }
            return 0
        }
    }
}