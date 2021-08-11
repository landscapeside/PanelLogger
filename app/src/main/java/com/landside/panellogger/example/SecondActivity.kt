package com.landside.panellogger.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.landside.panellogger.Logger
import com.landside.panellogger.R
import kotlinx.android.synthetic.main.activity_main.open_logger

class SecondActivity:AppCompatActivity(R.layout.activity_main) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    open_logger.setOnClickListener {
      Logger.popLogger()
    }
  }
}