package com.bnds.audioplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bnds.audioplayer.databinding.ActivityPlayListBinding

class PlayListActivity : AppCompatActivity() {

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityPlayListBinding
    private lateinit var musicAdapter: MusicAdapter

    class MusicAdapter(
        private var musicList: List<Music>,
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

        WindowCompat.setDecorFitsSystemWindows(window, true)

        // 设置状态栏的文本颜色为浅色或深色
        val insetsController = window.insetsController
        insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        ) // 根据你的设计需求调整

        var var1: Int = 1

        val musicScanner = Scanner(this)
        var musicList = musicScanner.scanMusicFiles()

        var recyclerView = findViewById<RecyclerView>(R.id.playListRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

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
                fun Intent.() {
                    putExtra("Music Settings", tvar1)
                    putExtra("musicTitle", music.title)
                    putExtra("musicArtist", music.artist)
                    putExtra("musicUri", music.uri)
                }
            )
            activityResultLauncher.launch(intent)
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = "Play List"
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        refreshButton.setOnClickListener {
            musicList = musicScanner.scanMusicFiles()
            recyclerView = findViewById<RecyclerView>(R.id.playListRecyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this)
        }

        val settingsButton = findViewById<Button>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val tvar1 = var1
            val intent: Intent = Intent(this, SettingsActivity::class.java).apply(
                fun Intent.() {
                    putExtra("Settings Values", tvar1)
                }
            )
            activityResultLauncher.launch(intent)
        }
    }
}