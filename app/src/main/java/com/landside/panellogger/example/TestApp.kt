package com.landside.panellogger.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.landside.panellogger.Logger
import com.landside.panellogger.Logger.ShowType.DRAWER_SLIDE
import timber.log.Timber

class TestApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Logger.debug = true
    Logger.showType = DRAWER_SLIDE
    Timber.plant(Logger.logTree)
  }
}