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

    inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {               // define the view holder
        val title: TextView = itemView.findViewById(R.id.musicTitle)
        val artist: TextView = itemView.findViewById(R.id.musicArtist)
        val bookmark: TextView = itemView.findViewById(R.id.musicBookmark)
        val albumArt: ImageView = itemView.findViewById(R.id.albumArt)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition                                               // use bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {                                         // make sure the position is valid
                    onItemClick(musicList[position])                                                // call the onItemClick function
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {            // create the view holder
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_music, parent, false
        )
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {                         // bind the view holder
        val music = musicList[position]
        holder.title.text = music.title
        holder.artist.text = music.artist
        if (isNeedBookmark(music.id)) {
            holder.bookmark.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, R.drawable.ic_bookmark_added_24px, 0
            )
            holder.bookmark.text = intToTime(bookMarker[music.id]!!)

        }

        FileHelper.getFilePathFromUri(holder.itemView.context, music.uri) { filePath ->             // get file path asynchronously
            if (filePath != null) {
                FileHelper.getAlbumArt(filePath) { bitmap ->                                        // update the album art after getting it asynchronously
                    holder.albumArt.post {                                                          // update the UI on the main thread
                        holder.albumArt.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    private fun isNeedBookmark(musicId: Long): Boolean {                                            // check if the music has been bookmarked
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

    private fun intToTime(time: Int): String {                                                      // convert the time to a string
        val seconds = time / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    override fun getItemCount(): Int = musicList.size                                               // get the number of the musics scanned
}