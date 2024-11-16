package com.bnds.audioplayer

import android.app.Application
import com.google.android.material.color.DynamicColors

class AudioPlayer : Application() {
    override fun onCreate() {
        super.onCreate()
        // 应用动态颜色到所有的 Activity
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
