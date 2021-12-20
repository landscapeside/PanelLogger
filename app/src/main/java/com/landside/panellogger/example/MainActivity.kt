package com.landside.panellogger.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.landside.panellogger.Logger
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
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
                if (count % 9 == 1){
                    Logger.json(genJsonTestStr())
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

    private fun genJsonTestStr():String{
        val obj = JSONObject()
        obj.put("count",count)
        obj.put("name","landscape!!")
        obj.put("isMan",true)
        obj.put("subObj",JSONObject().apply { put("wtf","ok") })
        obj.put("subArr",JSONArray().apply {
            put("dkdkdkdk")
            put(100)
            put(true)
        })
        return obj.toString()
    }
}