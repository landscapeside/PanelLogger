package com.landside.panellogger.example

import android.app.Application
import com.landside.panellogger.Logger
import com.landside.panellogger.Logger.ShowType.FLOAT

class TestApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Logger.showType = FLOAT
  }
}