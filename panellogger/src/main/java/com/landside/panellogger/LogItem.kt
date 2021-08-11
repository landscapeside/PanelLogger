package com.landside.panellogger

data class LogItem(
  val priority: LogPriority,
  val tag: String?,
  val message: String,
  val t: Throwable?
)