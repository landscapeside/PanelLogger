package com.landside.panellogger.example

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.landside.panellogger.Logger
import com.landside.panellogger.R
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

  private var count = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    open_logger.setOnClickListener {
      Logger.popLogger()
    }
    to_second.setOnClickListener {
      startActivity(Intent(this,SecondActivity::class.java))
    }

    thread {
      while (true) {
        Timber.w("$count")
        count++
        Thread.sleep(2000)
      }
    }
    Timber.d("MainActivity#onCreate")
  }

  override fun onStart() {
    super.onStart()
    Timber.d("MainActivity#onStart")
  }

  override fun onRestart() {
    super.onRestart()
    Timber.d("MainActivity#onRestart")
  }
}