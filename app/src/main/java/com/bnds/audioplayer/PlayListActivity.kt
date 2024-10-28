package com.bnds.audioplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.bnds.audioplayer.databinding.ActivityPlayListBinding

class PlayListActivity : AppCompatActivity() {

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityPlayListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var var1: Int = 1

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val tvar1 = result.data!!.getIntExtra("Settings Values", 0)
                var1 = tvar1
            }
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = title
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