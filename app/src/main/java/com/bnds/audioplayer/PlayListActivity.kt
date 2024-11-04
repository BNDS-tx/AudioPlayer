package com.bnds.audioplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bnds.audioplayer.databinding.ActivityPlayListBinding

class PlayListActivity : AppCompatActivity() {

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityPlayListBinding

    data class Music(val title: String, val artist: String)

    class MusicAdapter(
        private val musicList: List<Music>,
        private val onItemClick: (Music) -> Unit  //
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var var1: Int = 1

        val recyclerView = findViewById<RecyclerView>(R.id.playListRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val musicList = listOf(
            Music("Song 1", "Artist 1"),
            Music("Song 2", "Artist 2"),
            Music("Song 3", "Artist 3"),
            Music("Song 4", "Artist 4"),
            Music("Song 5", "Artist 5"),
            Music("Song 6", "Artist 6"),
            Music("Song 7", "Artist 7"),
            Music("Song 8", "Artist 8"),
            // Add more songs
        )

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val tvar1 = result.data!!.getIntExtra("Settings Values", 0)
                var1 = tvar1
            }
        }

        recyclerView.adapter = MusicAdapter(musicList) { music ->
            val tvar1 = var1
            val intent : Intent = Intent(this, PlayActivity::class.java).apply(
//                putExtra("musicTitle", music.title)
//                putExtra("musicArtist", music.artist)
                fun Intent.() {
                    putExtra("Music Settings", tvar1)
                }
            )
            activityResultLauncher.launch(intent)
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = "Play List"
        binding.fab.setOnClickListener {
            val tvar1 = var1
            val intent : Intent = Intent(this, SettingsActivity::class.java).apply(
                fun Intent.() {
                    putExtra("Settings Values", tvar1)
                }
            )
            activityResultLauncher.launch(intent)
        }
    }
}