package com.landside.panellogger

import android.app.Application
import android.content.Context

internal object ApplicationUtil {

  fun getApplication(context: Context?): Application? {
    return context as Application?
        ?: try {
          val actThreadClass =
              Reflect28Util.forName("android.app.ActivityThread")
          val method =
              Reflect28Util.getDeclaredMethod(actThreadClass, "currentApplication")
          method.invoke(null) as Context
        } catch (e: Exception) {
          e.printStackTrace()
          null
        } as Application?
  }
}