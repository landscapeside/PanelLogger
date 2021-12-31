package com.landside.panellogger

import android.os.Parcel
import android.os.Parcelable

private fun readException(parcel: Parcel):Throwable?{
  try {
    parcel.readException()
  }catch (e:Exception){
    return e
  }
  return null
}

data class LogItem(
  val priority: LogPriority,
  val tag: String?,
  val message: String,
  val t: Throwable?
) : Parcelable {
  constructor(parcel: Parcel) : this(
    LogPriority.from(parcel.readInt()),
    parcel.readString(),
    parcel.readString()?:"",
    readException(parcel)
  ) {
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeInt(priority.value)
    parcel.writeString(tag)
    parcel.writeString(message)
    if (t != null) {
      try {
        parcel.writeException(t as Exception)
      } catch (e: Exception) {
        parcel.writeNoException()
      }
    } else {
      parcel.writeNoException()
    }
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<LogItem> {
    override fun createFromParcel(parcel: Parcel): LogItem {
      return LogItem(parcel)
    }

    override fun newArray(size: Int): Array<LogItem?> {
      return arrayOfNulls(size)
    }
  }
}