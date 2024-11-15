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
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class SettingsActivity : AppCompatActivity() {
    private var speedVal: Float = 1F
    private var colorVal: Int = 1
    private var position: Int = -1
    private var isConnect: Boolean = false

    private lateinit var backButton: MaterialButton
    private lateinit var connectButton: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var speedControl: MaterialButtonToggleGroup
    private lateinit var speedButton1: MaterialButton
    private lateinit var speedButton2: MaterialButton
    private lateinit var speedButton3: MaterialButton
    private lateinit var colorControl: MaterialButtonToggleGroup
    private lateinit var colorButton1: MaterialButton
    private lateinit var colorButton2: MaterialButton
    private lateinit var colorButton3: MaterialButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        backButton = findViewById(R.id.backButton)
        connectButton = findViewById(R.id.connectButton)
        speedControl = findViewById(R.id.speedControl)
        speedButton1 = findViewById(R.id.speedOption1)
        speedButton2 = findViewById(R.id.speedOption2)
        speedButton3 = findViewById(R.id.speedOption3)
        colorControl = findViewById(R.id.colorControl)
        colorButton1 = findViewById(R.id.colorOption1)
        colorButton2 = findViewById(R.id.colorOption2)
        colorButton3 = findViewById(R.id.colorOption3)

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
            colorVal = intent.getIntExtra("Color Values", 1)
            isConnect = intent.getBooleanExtra("continuePlay", false)
            position = intent.getIntExtra("musicPosition", -1)
        }

        backButton.setOnClickListener() {
            endActivity()
        }

        connectButton.isChecked = isConnect
        connectButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                isConnect = true
            } else {
                isConnect = false
            }
        }

        playBackSpeedControl()
        backgroundColorControl()

        onBackPressedDispatcher.addCallback(this) {
            endActivity()
        }
    }

    private fun playBackSpeedControl() {
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
        when (colorVal) {
            1 -> colorControl.check(colorButton1.id)
            2 -> colorControl.check(colorButton2.id)
            3 -> colorControl.check(colorButton3.id)
        }

        colorControl.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    colorButton1.id -> colorVal = 1
                    colorButton2.id -> colorVal = 2
                    colorButton3.id -> colorVal = 3
                }
            }
        }
    }

    private fun endActivity() {
        val intent2 = Intent()
        intent2.putExtra("Speed Values", speedVal)
        intent2.putExtra("Color Values", colorVal)
        intent2.putExtra("continuePlay", isConnect)
        intent2.putExtra("musicPosition", position)
        setResult(Activity.RESULT_OK, intent2)
        finish()
    }
}