package com.bnds.audioplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowInsetsController
import android.widget.Button
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : AppCompatActivity() {
    private var svar1: Int = 0
    private var position: Int = -1
    private var alreadyPlayed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val insetsController = window.insetsController
        insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
        WindowCompat.setDecorFitsSystemWindows(window, true)

        var intent : Intent = getIntent()
        if (intent != null && intent.hasExtra("Settings Values")) {
            svar1 = intent.getIntExtra("Settings Values", 0)
            position = intent.getIntExtra("musicPosition", -1)
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener() {
            endActivity()
        }

        onBackPressedDispatcher.addCallback(this) {
            endActivity()
        }
    }

    private fun endActivity() {
        val intent2 = Intent()
        intent2.putExtra("result", svar1)
        intent2.putExtra("musicPosition", position)
        intent2.putExtra("alreadyPlayed", alreadyPlayed)
        setResult(Activity.RESULT_OK, intent2)
        finish()
    }
}