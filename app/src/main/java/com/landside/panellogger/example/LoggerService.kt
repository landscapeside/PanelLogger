package com.landside.panellogger.example

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.landside.panellogger.ILoggerInterface
import com.landside.panellogger.LogItem
import com.landside.panellogger.Logger

class LoggerService : Service() {

  override fun onBind(intent: Intent): IBinder = LoggerBinder()

  inner class LoggerBinder : ILoggerInterface.Stub() {

    override fun recordLog(item: LogItem?) {
      item?.let {
        Logger.record(item)
      }
    }
  }
}