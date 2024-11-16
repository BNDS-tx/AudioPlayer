package com.bnds.audioplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class MusicAdapter(
    private var musicList: List<Music>,
    private var bookMarker: MutableMap<Long, Int>,
    private val onItemClick: (Music) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.musicTitle)
        val artist: TextView = itemView.findViewById(R.id.musicArtist)
        val bookmark: TextView = itemView.findViewById(R.id.musicBookmark)
        val albumArt: ImageView = itemView.findViewById(R.id.albumArt)

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
        if (isNeedBookmark(music.id)) {
            holder.bookmark.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, R.drawable.ic_bookmark_added_24px, 0
            )
            holder.bookmark.text = intToTime(bookMarker[music.id]!!)

        }
        // 异步获取文件路径
        FileHelper.getFilePathFromUri(holder.itemView.context, music.uri) { filePath ->
            if (filePath != null) {
                // 在获取文件路径后，异步获取专辑封面
                FileHelper.getAlbumArt(filePath) { bitmap ->
                    // 在主线程上更新 UI
                    holder.albumArt.post {
                        holder.albumArt.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    private fun isNeedBookmark(musicId: Long): Boolean {
        if (bookMarker.isEmpty()) {
            return false
        }
        if (!bookMarker.containsKey(musicId)) {
            return false
        }
        if (bookMarker[musicId] == 0) {
            return false
        }
        return true
    }

    private fun intToTime(time: Int): String {
        val seconds = time / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    override fun getItemCount(): Int = musicList.size
}