package com.bnds.PurePlayer

import android.app.Application
import com.google.android.material.color.DynamicColors

class PurePlayer : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
