package com.landside.panellogger.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.landside.panellogger.Logger
import kotlinx.android.synthetic.main.activity_main.*
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
                if (count % 7 == 0) {
                    Logger.e(IllegalStateException("count is 7 times"))
                }
                Logger.w("$count")
                count++
                Thread.sleep(2000)
            }
        }
        Logger.d("MainActivity#onCreate")
    }

    override fun onStart() {
        super.onStart()
        Logger.d("MainActivity#onStart")
    }

    override fun onRestart() {
        super.onRestart()
        Logger.d("MainActivity#onRestart")
    }
}