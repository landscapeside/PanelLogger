package com.landside.panellogger

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import com.landside.panellogger.Logger.ShowType.DRAWER_SLIDE
import io.reactivex.subjects.ReplaySubject
import timber.log.Timber
import kotlin.concurrent.thread

object Logger {

  enum class ShowType {
    DRAWER_SLIDE,
    POP_PAGE,
    FLOAT
  }

  private const val MAX_SIZE = 200
  var showType: ShowType = DRAWER_SLIDE
  private var debug: Boolean = false
  private lateinit var application: Application
  internal var logPublisher: ReplaySubject<LogItem>? = null
  internal var iLoggerInterface: ILoggerInterface? = null
  private const val MAX_SIZE_KEY = "panel_logger_max_size"

  val logTree: Timber.Tree = object : Timber.DebugTree() {
    override fun log(
      priority: Int,
      tag: String?,
      message: String,
      t: Throwable?
    ) {
      synchronized(debug) {
        if (debug) {
          super.log(priority, tag, message, t)
          logPublisher?.onNext(LogItem(LogPriority.from(priority), tag, message, t))
        } else {
          iLoggerInterface?.recordLog(LogItem(LogPriority.from(priority), tag, message, t))
        }
      }
    }
  }

  class Observer : FileProvider() {
    override fun onCreate(): Boolean {
      ApplicationUtil.getApplication(context)
          ?.apply {
            application = this
            val info = packageManager.getApplicationInfo(application.packageName,PackageManager.GET_META_DATA)
            val maxSize = info.metaData?.getInt(MAX_SIZE_KEY) ?: MAX_SIZE
            install(this, maxSize)
          }
      return super.onCreate()
    }

    private fun install(
      app: Application,
      maxSize: Int
    ) {
      debug = (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
      logPublisher = ReplaySubject.createWithSize(maxSize)
      logPublisher?.onNext(
          LogItem(
              LogPriority.INFO,
              "PannelLogger!",
              "Hello!Welcome to PannelLogger!",
              null
          )
      )
      Timber.plant(logTree)
      bindRemote()
      app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {

        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(
          activity: Activity,
          outState: Bundle
        ) {
        }

        override fun onActivityDestroyed(activity: Activity) {
        }

        override fun onActivityCreated(
          activity: Activity,
          savedInstanceState: Bundle?
        ) {
          if (debug && activity is FragmentActivity && activity !is LogActivity) {
            when (showType) {
              DRAWER_SLIDE->{
                val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
                  override fun onGlobalLayout() {
                    activity.window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(
                      this
                    )
                    val decorView = (activity.window.decorView as FrameLayout)
                    val originView = decorView.getChildAt(0) as ViewGroup
                    val rootPaddingTop = originView.paddingTop
                    val rootPaddingLeft = originView.paddingLeft
                    val rootPaddingRight = originView.paddingRight
                    val rootPaddingBottom = originView.paddingBottom
                    (originView.parent as ViewGroup).removeView(originView)
                    val drawerLayout =
                      activity.layoutInflater.inflate(
                        R.layout.view_drawer, originView, false
                      ) as DrawerLayout
                    drawerLayout.addView(originView, 0, originView.layoutParams)
                    decorView.addView(drawerLayout, 0)
                    decorView.post {
                      val fm = (activity as? FragmentActivity)?.supportFragmentManager
                      fm?.let {
                        val transaction = it.beginTransaction()
                        transaction.add(R.id.log_fragment_box, LogFragment())
                        transaction.commit()
                        val box =
                          drawerLayout.findViewById<FrameLayout>(R.id.log_fragment_box)
                        val lp = (box.layoutParams as DrawerLayout.LayoutParams)
                        lp.bottomMargin = getNavigationBarHeight(activity)
                        box.layoutParams = lp
                        drawerLayout.findViewById<FrameLayout>(R.id.log_fragment_box)
                          .setPadding(
                            rootPaddingLeft,
                            rootPaddingTop,
                            rootPaddingRight,
                            rootPaddingBottom
                          )
                      }
                    }
                  }
                }
                activity.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(
                  globalLayoutListener
                )
              }
              ShowType.FLOAT ->{
                val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
                  override fun onGlobalLayout() {
                    activity.window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(
                      this
                    )
                    renderDragButton(activity,120){
                      popLogger()
                    }
                  }
                }
                activity.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(
                  globalLayoutListener
                )
              }
            }
          }
        }
      })
    }
  }

  private fun renderDragButton(
    activity: Activity,
    alignBottom: Int,
    loadImg: (ImageView) -> Unit = { it.setImageResource(R.drawable.ic_log) },
    onClick: () -> Unit
  ): ImageView {
    val imageView = ImageView(activity)
    imageView.scaleType = ImageView.ScaleType.FIT_END
    SonnyJackDragView.Builder()
      .setActivity(activity)
      .setRootView((activity.findViewById(android.R.id.content) as ViewGroup).getChildAt(0))
      .setDefaultRight(0)
      .setDefaultBottom(
        alignBottom.byDip()
          .toInt()
      )
      .setNeedNearEdge(true)
      .setSize(
        40.byDip()
          .toInt()
      )
      .setView(imageView)
      .build()
    loadImg(imageView)
    imageView.setOnClickListener {
      onClick()
    }
    return imageView
  }

  private val metric = Resources.getSystem()
    .displayMetrics

  private fun Int.byDip(): Float = toFloat() * metric.density

  private val conn = object : ServiceConnection {
    override fun onServiceConnected(
      name: ComponentName?,
      service: IBinder?
    ) {
      iLoggerInterface = ILoggerInterface.Stub.asInterface(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      iLoggerInterface = null
      application.unbindService(this)
      thread {
        Thread.sleep(2000)
        bindRemote()
      }
    }

  }

  private fun bindRemote() {
    application.bindService(
        Intent("com.landside.panellogger.LOG_SERVER").apply {
          setPackage("com.landside.panellogger.example.display")
        },
        conn,
        Context.BIND_AUTO_CREATE
    )
  }

  fun popLogger() {
    val intent = Intent(application, LogActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    application.startActivity(intent)
  }

  private fun getNavigationBarHeight(context: Activity): Int {
    val view: View? = context.findViewById(android.R.id.navigationBarBackground)
    return view?.height ?: 0
  }

  fun record(log: LogItem) {
    synchronized(log) {
      logPublisher?.onNext(log)
    }
  }

  /*
   * functions for users to record log in PanelLogger
   * a: record assert log
   * v: record verbose log
   * d: record debug log
   * w: record warn log
   * i: record info log
   * e: record error log
   *
   */

  fun a(
    message: String,
    vararg args: Any?
  ) = Timber.wtf(message, args)

  fun a(
    t: Throwable,
    message: String,
    vararg args: Any?
  ) = Timber.wtf(t, message, args)

  fun a(t: Throwable) = Timber.wtf(t)

  fun v(
    msg: String,
    vararg args: Any?
  ) = Timber.v(msg, args)

  fun v(
    t: Throwable,
    message: String,
    vararg args: Any?
  ) = Timber.v(t, message, args)

  fun v(t: Throwable) = Timber.v(t)

  fun d(
    message: String,
    vararg args: Any?
  ) = Timber.d(message, args)

  fun d(
    t: Throwable,
    message: String,
    vararg args: Any?
  ) = Timber.d(t, message, args)

  fun d(t: Throwable) = Timber.d(t)

  fun i(
    message: String,
    vararg args: Any?
  ) = Timber.i(message, args)

  fun i(
    t: Throwable,
    message: String,
    vararg args: Any?
  ) = Timber.i(t, message, args)

  fun i(t: Throwable) = Timber.i(t)

  fun w(
    message: String,
    vararg args: Any?
  ) = Timber.w(message, args)

  fun w(
    t: Throwable,
    message: String,
    vararg args: Any?
  ) = Timber.w(t, message, args)

  fun w(t: Throwable) = Timber.w(t)

  fun e(
    message: String,
    vararg args: Any?
  ) = Timber.e(message, args)

  fun e(
    t: Throwable,
    message: String,
    vararg args: Any?
  ) = Timber.e(t, message, args)

  fun e(t: Throwable) = Timber.e(t)

}