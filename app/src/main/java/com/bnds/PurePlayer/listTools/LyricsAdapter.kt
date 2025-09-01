package com.bnds.PurePlayer.listTools

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bnds.PurePlayer.R
import com.bnds.PurePlayer.fileTools.*

class LyricsAdapter(
    private val lyrics: List<LyricLine>,
    private val HighlightColor: ColorStateList,
    private val BackgroundColor: ColorStateList
) :
    RecyclerView.Adapter<LyricsAdapter.LyricViewHolder>() {

    private var currentPosition = -1

    inner class LyricViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lyricText: TextView = itemView.findViewById(R.id.lyricText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric_line, parent, false)
        return LyricViewHolder(view)
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        holder.lyricText.text = lyrics[position].text

        // 高亮当前行
        if (position == currentPosition) {
            holder.lyricText.setTextColor(HighlightColor)
            holder.lyricText.setTypeface(null, Typeface.BOLD)
        } else {
            holder.lyricText.setTextColor(BackgroundColor)
            holder.lyricText.setTypeface(null, Typeface.NORMAL)
        }
    }

    override fun getItemCount() = lyrics.size

    fun updateCurrentLine(newPosition: Int) {
        if (newPosition != currentPosition) {
            val old = currentPosition
            currentPosition = newPosition
            if (old >= 0) notifyItemChanged(old)
            notifyItemChanged(newPosition)
        }
    }
}
