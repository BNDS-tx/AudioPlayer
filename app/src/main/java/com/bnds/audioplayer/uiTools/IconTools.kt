package com.bnds.audioplayer.uiTools

import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.bnds.audioplayer.R
import java.util.Locale

class IconTools {
    fun longToTime(time: Long): String {
        val seconds = time / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    fun setMethodButtonIcon(button: MaterialButton, playMethodVal: Int) {
        when (playMethodVal) {
            0 -> button.setIconResource(R.drawable.ic_play_once)
            1 -> button.setIconResource(R.drawable.ic_play_continuously)
            2 -> button.setIconResource(R.drawable.ic_play_randomly)
        }
    }

    fun setMethodImageIcon(image: ImageView, playMethodVal: Int) {
        when (playMethodVal) {
            0 -> image.setImageResource(R.drawable.ic_play_once)
            1 -> image.setImageResource(R.drawable.ic_play_continuously)
            2 -> image.setImageResource(R.drawable.ic_play_randomly)
        }
    }

    fun setPlayIcon(button: MaterialButton, isPlaying: Boolean) {
        if (isPlaying) button.setIconResource(R.drawable.ic_pause_circle_24px)
        else button.setIconResource(R.drawable.ic_play_arrow_24px)
    }

    fun setBookmarkIcon(button: MaterialButton, bookmark: Long?) {
        if (bookmark != null && bookmark > 0L) {
            button.setIconResource(R.drawable.ic_bookmark_check_24px)
            button.text = longToTime(bookmark)
        } else {
            button.setIconResource(R.drawable.ic_bookmark_add_24px)
            button.text = "--:--"
        }
    }
}