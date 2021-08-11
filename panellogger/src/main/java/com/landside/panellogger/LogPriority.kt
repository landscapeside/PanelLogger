package com.landside.panellogger

import android.util.Log
import androidx.annotation.ColorRes

enum class LogPriority(
  val value: Int,
  @ColorRes val textColor: Int
) {

  VERBOSE(Log.VERBOSE, R.color.color_verbose),
  DEBUG(Log.DEBUG, R.color.color_debug),
  INFO(Log.INFO, R.color.color_info),
  WARN(Log.WARN, R.color.color_warn),
  ERROR(Log.ERROR, R.color.color_error),
  ASSERT(Log.ASSERT, R.color.color_assert);

  companion object {
    @JvmStatic
    fun from(value: Int): LogPriority {
      for (type in values()) {
        if (value == type.value) {
          return type
        }
      }
      return VERBOSE
    }
  }

}