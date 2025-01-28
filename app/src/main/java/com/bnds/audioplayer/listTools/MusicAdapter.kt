package com.bnds.audioplayer.listTools

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bnds.audioplayer.fileTools.*
import com.bnds.audioplayer.*
import com.bnds.audioplayer.uiTools.*

class MusicAdapter(
    private var musicList: List<Music>,
    private val onItemClick: (Music) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {               // define the view holder
        val title: TextView = itemView.findViewById(R.id.musicTitle)
        val artist: TextView = itemView.findViewById(R.id.musicArtist)
        val bookmark: TextView = itemView.findViewById(R.id.musicBookmark)
        val albumArt: ImageView = itemView.findViewById(R.id.albumArt)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition                                               // use bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {                                         // make sure the position is valid
                    onItemClick(musicList[position])                                            // call the onItemClick function
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {            // create the view holder
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_music, parent, false
        )
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as MusicViewHolder
        val music = musicList[position]
        holder.title.text = music.title
        holder.artist.text = music.artist
        if (music.bookMarker != null && music.bookMarker!! > 0L) {
            holder.bookmark.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, R.drawable.ic_bookmark_added_24px, 0
            )
            holder.bookmark.text = IconTools.longToTime(music.bookMarker!!)
        } else {
            holder.bookmark.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, 0, 0
            )
            holder.bookmark.text = ""
        }
        if (music.albumArt == null) {
            FileScanner.getAlbumArtAsynchronously(
                holder.itemView.context,
                music.uri
            ) { bitmap ->
                holder.albumArt.post {                                                          // update the UI on the main thread
                    holder.albumArt.setImageBitmap(bitmap)
                    music.albumArt = bitmap
                }
            }
        } else holder.albumArt.setImageBitmap(music.albumArt)
    }

    override fun getItemCount(): Int = musicList.size

    fun setList(musics: List<Music>) {
        val oldList = musicList
        musicList = musics
        val newList = musics
        val diffResult = DiffUtil.calculateDiff(MusicDiffCallback(oldList, newList))
        diffResult.dispatchUpdatesTo(this)
    }
}