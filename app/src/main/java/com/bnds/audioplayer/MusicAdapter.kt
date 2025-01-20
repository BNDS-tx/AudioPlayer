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
    private var bookMarker: MutableMap<Long, Long>,
    private val onItemClick: (Music) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 || position == musicList.size + 1) TYPE_HEADER else TYPE_ITEM
    }

    inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {               // define the view holder
        val title: TextView = itemView.findViewById(R.id.musicTitle)
        val artist: TextView = itemView.findViewById(R.id.musicArtist)
        val bookmark: TextView = itemView.findViewById(R.id.musicBookmark)
        val albumArt: ImageView = itemView.findViewById(R.id.albumArt)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition                                               // use bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {                                         // make sure the position is valid
                    onItemClick(musicList[position - 1])                                            // call the onItemClick function
                }
            }
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {            // create the view holder
        if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_header, parent, false
            )
            return HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_music, parent, false
            )
            return MusicViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_ITEM) {
            holder as MusicViewHolder
            val music = musicList[position - 1]
            holder.title.text = music.title
            holder.artist.text = music.artist
            if (music.bookMarker != null && music.bookMarker!! > 0L) {
                holder.bookmark.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, R.drawable.ic_bookmark_added_24px, 0
                )
                holder.bookmark.text = longToTime(music.bookMarker!!)
            } else {
                holder.bookmark.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, 0, 0
                )
                holder.bookmark.text = ""
            }

            if (music.albumArt == null) {
                FileHelper.getFilePathFromUri(
                    holder.itemView.context,
                    music.uri
                ) { filePath ->             // get file path asynchronously
                    if (filePath != null) {
                        FileHelper.getAlbumArt(filePath) { bitmap ->                                        // update the album art after getting it asynchronously
                            holder.albumArt.post {                                                          // update the UI on the main thread
                                holder.albumArt.setImageBitmap(bitmap)
                                music.albumArt = bitmap
                            }
                        }
                    }
                }
            } else holder.albumArt.setImageBitmap(music.albumArt)
        }
    }

    private fun isNeedBookmark(musicId: Long): Boolean {                                            // check if the music has been bookmarked
        if (bookMarker.isEmpty()) {
            return false
        }
        if (!bookMarker.containsKey(musicId)) {
            return false
        }
        if (bookMarker[musicId] == 0.toLong()) {
            return false
        }
        return true
    }

    private fun longToTime(time: Long): String {                                                      // convert the time to a string
        val seconds = time / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    override fun getItemCount(): Int = musicList.size + 2                                           // get the number of the musics scanned

    fun setList(newList: List<Music>) {
        musicList = newList
    }

    fun getList(): List<Music> {
        return musicList
    }
}