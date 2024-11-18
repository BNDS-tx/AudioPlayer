package com.bnds.audioplayer

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class SettingsActivity : AppCompatActivity() {
    private var speedVal: Float = 1F
    private var colorVal: Int = 1
    private var isConnect: Boolean = false

    private lateinit var player: Player
    private var isBound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as Player.PlayerBinder
            player = binder.getService()
            isBound = true
            moveContext()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

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
        enableEdgeToEdge()
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

        val intent : Intent = intent
        if (intent.hasExtra("valid")) {
            val transferData = intent.extras
            if (transferData != null) {
                transferData.keySet()?.forEach { key ->
                    when (key) {
                        "Speed Values" -> speedVal = transferData.getFloat(key)
                        "Color Values" -> colorVal = transferData.getInt(key)
                        "continuePlay" -> isConnect = transferData.getBoolean(key)
                    }
                }
            }
        }

        bindService()

        backButton.setOnClickListener {
            endActivity()
        }

        connectButton.isChecked = isConnect
        connectButton.setOnCheckedChangeListener { _, isChecked ->
            isConnect = isChecked
        }

        playBackSpeedControl()
        backgroundColorControl()

        onBackPressedDispatcher.addCallback(this) {
            endActivity()
        }
    }

    private fun bindService() {
        if (!isBound) {
            val intent = Intent(this, Player::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindService() {
        if (isBound) {
            unbindService(connection)
            isBound = false
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
            if (group.checkedButtonId == View.NO_ID) {
                group.check(checkedId)
            }
        }
    }

    private fun backgroundColorControl() {
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        when (colorVal) {
            1 -> {
                if (isDarkMode) { colorControl.check(colorButton3.id) }
                else { colorControl.check(colorButton1.id) }
            }
            2 -> colorControl.check(colorButton2.id)
            3 -> {
                if (isDarkMode) { colorControl.check(colorButton1.id) }
                else { colorControl.check(colorButton3.id) }
            }
        }

        colorControl.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    colorButton1.id -> {
                        colorVal = if (!isDarkMode) {
                            1
                        } else {
                            3
                        }
                    }
                    colorButton2.id -> colorVal = 2
                    colorButton3.id -> {
                        colorVal = if (!isDarkMode) {
                            3
                        } else {
                            1
                        }
                    }
                }
            }
            if (group.checkedButtonId == View.NO_ID) {
                group.check(checkedId)
            }
        }
    }

    private fun moveContext() { player.setContext(this) }

    private fun endActivity() {
        unbindService()
        val intent2 = Intent()
        val transferData = Bundle()
        transferData.putFloat("Speed Values", speedVal)
        transferData.putInt("Color Values", colorVal)
        transferData.putBoolean("continuePlay", isConnect)
        intent2.putExtras(transferData)
        setResult(Activity.RESULT_OK, intent2)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        endActivity()
    }
}