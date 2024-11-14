package com.bnds.audioplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.Switch
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButtonToggleGroup

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : AppCompatActivity() {
    private var speedVal: Float = 1F
    private var position: Int = -1
    private var isConnect: Boolean = false

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

        var intent : Intent = getIntent()
        if (intent != null && intent.hasExtra("Speed Values")) {
            speedVal = intent.getFloatExtra("Speed Values", 1F)
            isConnect = intent.getBooleanExtra("continuePlay", false)
            position = intent.getIntExtra("musicPosition", -1)
        }

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener() {
            endActivity()
        }

        val connectButton = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.connectButton)
        connectButton.isChecked = isConnect
        connectButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                isConnect = true
            } else {
                isConnect = false
            }
        }

        playBackSpeedControl()

        onBackPressedDispatcher.addCallback(this) {
            endActivity()
        }
    }

    private fun playBackSpeedControl() {
        val speedControl = findViewById<MaterialButtonToggleGroup>(R.id.speedControl)
        val speedButton1 = findViewById<Button>(R.id.speedOption1)
        val speedButton2 = findViewById<Button>(R.id.speedOption2)
        val speedButton3 = findViewById<Button>(R.id.speedOption3)

        when (speedVal) {
            1F -> speedControl.check(speedButton1.id)
            2F -> speedControl.check(speedButton2.id)
            3F -> speedControl.check(speedButton3.id)
        }

        speedControl.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    speedButton1.id -> speedVal = 1F
                    speedButton2.id -> speedVal = 2F
                    speedButton3.id -> speedVal = 3F
                }
            }
        }
    }

    private fun backgroundColorControl() {

    }

    private fun endActivity() {
        val intent2 = Intent()
        intent2.putExtra("Speed Values", speedVal)
        intent2.putExtra("continuePlay", isConnect)
        intent2.putExtra("musicPosition", position)
        setResult(Activity.RESULT_OK, intent2)
        finish()
    }
}