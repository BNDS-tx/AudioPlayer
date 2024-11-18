package com.bnds.audioplayer

import android.app.Application
import com.google.android.material.color.DynamicColors

class AudioPlayer : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)                                  // apply dynamic colors to all activities
    }
}
