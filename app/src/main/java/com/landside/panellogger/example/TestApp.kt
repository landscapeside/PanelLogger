package com.landside.panellogger.example

import android.app.Application
import com.landside.panellogger.Logger
import com.landside.panellogger.Logger.ShowType.DRAWER_SLIDE

class TestApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Logger.debug = true
    Logger.showType = DRAWER_SLIDE
  }
}