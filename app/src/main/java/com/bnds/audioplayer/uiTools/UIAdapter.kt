package com.bnds.audioplayer.uiTools

import android.content.res.Configuration
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bnds.audioplayer.*
import com.bnds.audioplayer.fileTools.LyricLine
import com.bnds.audioplayer.listTools.LyricsAdapter
import com.google.android.material.slider.Slider

class UIAdapter(private val activity: PlayActivity) {

    fun refreshPage() {
        setColor()
        updateTitle()
        updateArt()
        setIcon()
        activity.updateLyricList()
        setLyricList(
            activity.lyricView,
            activity.lyricLine
        )

        updateBarProgressNLyricList(
            activity.progressBar,
            activity.musicPlayerService.getProgress(),
            activity.musicPlayerService.getDuration(),
            activity.lyricLine
        )
    }

    fun refreshIconAndBar() {
        setIcon()
        updateBarProgressNLyricList(
            activity.progressBar,
            activity.musicPlayerService.getProgress(),
            activity.musicPlayerService.getDuration(),
            activity.lyricLine
        )
    }

    private fun setColor() {
        val isDarkMode = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        var albumDominantColor =
            if (activity.musicPosition == -1) 0
            else if (activity.musicPlayerService.getMusicList().isEmpty()) 0
            else ColorTools.extractDominantColor(activity.musicPlayerService.getThisAlbumArt())
        ColorTools.updatePageColor(
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
            activity.progressBar,
            activity
        )
        ColorTools.updateTextsColor(
            albumDominantColor,
            isDarkMode,
            activity.speedSlower,
            activity.speedFaster,
            activity.showSpeed,
            activity
        )
    }

    fun setLyricList(lyricView: RecyclerView, lyricLine: List<LyricLine>) {
        activity.lyricsAdapter = LyricsAdapter(
            lyricLine,
            activity.progressBar.trackActiveTintList,
            activity.progressBar.trackInactiveTintList)
        lyricView.adapter = activity.lyricsAdapter
    }

    fun findCurrentLyricLine(currentTime: Long, lyricLine: List<LyricLine>): Int {
        var pos = -1
        for (i in lyricLine.indices) {
            if (currentTime >= lyricLine[i].time &&
                (i == lyricLine.size - 1 || currentTime < lyricLine[i + 1].time)) {
                pos = i
                break
            }
        }
        return pos
    }

    fun updateBarProgressNLyricList(progressBar: Slider, progress: Long, duration: Long, lyricLine: List<LyricLine>) {
        if (activity.pauseUpdate) return
        if (progress <= duration && !activity.musicPlayerService.checkState(0)
            && duration != 0.toLong()) {
            progressBar.value = progress.toFloat()
            progressBar.valueTo = duration.toFloat()
        }
        val newPos = findCurrentLyricLine(progress, lyricLine)
        if (newPos >= 0) {
            activity.lyricsAdapter.updateCurrentLine(newPos)
            activity.lyricView.smoothScrollToPosition(newPos)
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
        IconTools.setPlayIcon(activity.playButton, activity.musicPlayerService.checkState(1))
        if (activity.musicPosition >= 0) {
            IconTools.setBookmarkIcon(activity.bookMarkButton,
                activity.bookMarker[
                    activity.musicPlayerService.getPositionId(activity.musicPosition)
            ])
        } else {
            IconTools.setBookmarkIcon(activity.bookMarkButton, null)
        }
    }
}