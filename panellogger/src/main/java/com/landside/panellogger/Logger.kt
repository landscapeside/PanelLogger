package com.landside.panellogger

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import com.landside.panellogger.Logger.ShowType.DRAWER_SLIDE
import io.reactivex.subjects.ReplaySubject
import timber.log.Timber

object Logger {

  enum class ShowType {
    DRAWER_SLIDE,
    POP_PAGE
  }

  var MAX_SIZE = 200
  var showType: ShowType = DRAWER_SLIDE
  var debug: Boolean = false
  private lateinit var application: Application
  internal var logPublisher: ReplaySubject<LogItem>? = null

  val logTree: Timber.Tree = object : Timber.DebugTree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?
    ) {
      if (debug) {
        super.log(priority, tag, message, t)
        logPublisher?.onNext(LogItem(LogPriority.from(priority), tag, message, t))
      } else {
        // TODO: 2021/8/10 正式环境上报日志
      }
    }
  }

  class Observer : FileProvider() {
    override fun onCreate(): Boolean {
      ApplicationUtil.getApplication(context)
          ?.apply {
            application = this
            install(this)
          }
      return super.onCreate()
    }

    private fun install(app: Application) {
      logPublisher = ReplaySubject.createWithSize(MAX_SIZE)
      Timber.plant(logTree)
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
              if (debug && showType == DRAWER_SLIDE && activity is FragmentActivity && activity !is LogActivity) {
                  val globalLayoutListener = object : OnGlobalLayoutListener {
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
                                  val box = drawerLayout.findViewById<FrameLayout>(R.id.log_fragment_box)
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
          }

      })
    }
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
}