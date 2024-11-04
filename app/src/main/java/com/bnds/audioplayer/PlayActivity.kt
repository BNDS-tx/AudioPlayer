package com.bnds.audioplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PlayActivity : AppCompatActivity() {
    private var mvar1: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var intent : Intent = getIntent()
        if (intent != null && intent.hasExtra("Music Settings")) {
            mvar1 = intent.getIntExtra("Music Settings", 0)
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.onClickListener() {
            val intent2 = Intent()
            intent2.putExtra("result", mvar1)
            setResult(Activity.RESULT_OK, intent2)
            finish()
        }
    }


}