package com.landside.panellogger

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LogActivity:AppCompatActivity(R.layout.activity_log){

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val fm = supportFragmentManager
    val transaction = fm.beginTransaction()
    transaction.add(R.id.log_fragment_box, LogFragment())
    transaction.commit()
  }
}