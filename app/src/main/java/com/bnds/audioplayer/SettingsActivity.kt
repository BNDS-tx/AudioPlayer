package com.bnds.audioplayer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView

class SettingsActivity : AppCompatActivity() {
    private var speedVal: Float = 1F
    private var isConnect: Boolean = false
    private var isInOrderQueue: Boolean = true

    private lateinit var playerService: PlayerService
    private var isBound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.PlayerBinder
            playerService = binder.getService()
            isBound = true
            moveContext()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    private lateinit var backButton: ImageView
    private lateinit var titleText: MaterialTextView
    private lateinit var connectButton: MaterialSwitch
    private lateinit var queueButton: MaterialSwitch
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

        initializeViews()
    }

    private fun bindService() {
        if (!isBound) {
            val intent = Intent(this, PlayerService::class.java)
            bindService(intent, connection, BIND_AUTO_CREATE)
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

    private fun moveContext() { playerService.setContext(this) }

    private fun endActivity() {
        unbindService()
        val intent2 = Intent()
        val transferData = Bundle()
        transferData.putFloat("Speed Values", speedVal)
        transferData.putBoolean("continuePlay", isConnect)
        transferData.putBoolean("isInOrderQueue", isInOrderQueue)
        intent2.putExtras(transferData)
        setResult(RESULT_OK, intent2)
        finish()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        unbindService()
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.settings_activity)
        } else {
            setContentView(R.layout.settings_activity)
        }
        initializeViews()
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        titleText = findViewById(R.id.titleText)
        connectButton = findViewById(R.id.connectButton)
        queueButton = findViewById(R.id.queueButton)
        speedControl = findViewById(R.id.speedControl)
        speedButton1 = findViewById(R.id.speedOption1)
        speedButton2 = findViewById(R.id.speedOption2)
        speedButton3 = findViewById(R.id.speedOption3)
        colorControl = findViewById(R.id.colorControl)
        colorButton1 = findViewById(R.id.colorOption1)
        colorButton2 = findViewById(R.id.colorOption2)
        colorButton3 = findViewById(R.id.colorOption3)

        val intent : Intent = intent
        if (intent.hasExtra("valid")) {
            val transferData = intent.extras
            transferData?.keySet()?.forEach { key ->
                when (key) {
                    "Speed Values" -> speedVal = transferData.getFloat(key)
                    "continuePlay" -> isConnect = transferData.getBoolean(key)
                    "isInOrderQueue" -> isInOrderQueue = transferData.getBoolean(key)
                }
            }
        }

        bindService()

        backButton.setOnClickListener {
            endActivity()
        }

        titleText.setOnClickListener {
            endActivity()
        }

        connectButton.isChecked = isConnect
        connectButton.setOnCheckedChangeListener { _, isChecked ->
            isConnect = isChecked
        }

        queueButton.isChecked = !isInOrderQueue
        queueButton.setOnCheckedChangeListener { _, isChecked ->
            isInOrderQueue = !isChecked
        }

        playBackSpeedControl()

        onBackPressedDispatcher.addCallback(this) {
            endActivity()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        endActivity()
    }
}