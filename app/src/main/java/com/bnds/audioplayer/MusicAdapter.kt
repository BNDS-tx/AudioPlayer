package com.bnds.audioplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MusicAdapter(
    private var musicList: List<Music>,
    private val onItemClick: (Music) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.musicTitle)
        val artist: TextView = itemView.findViewById(R.id.musicArtist)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition // 使用 bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) { // 确保位置有效
                    onItemClick(musicList[position])  // 调用点击事件
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = musicList[position]
        holder.title.text = music.title
        holder.artist.text = music.artist
    }

    override fun getItemCount(): Int = musicList.size
}