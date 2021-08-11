package com.landside.panellogger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.drake.brv.BindingAdapter
import com.drake.brv.utils.divider
import com.drake.brv.utils.linear
import com.drake.brv.utils.setup
import com.landside.panellogger.LogPriority.VERBOSE
import com.landside.panellogger.databinding.PopPriorityBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_log.btn_clean
import kotlinx.android.synthetic.main.fragment_log.btn_priority
import kotlinx.android.synthetic.main.fragment_log.et_filter
import kotlinx.android.synthetic.main.fragment_log.log_list
import kotlinx.android.synthetic.main.fragment_log.to_bottom

class LogFragment : Fragment(R.layout.fragment_log) {
  companion object{
    const val MAX_SIZE = 200
  }
  private lateinit var logDisposable: Disposable
  private lateinit var adapter: BindingAdapter
  private var listScrollY:Int = 0
  private var keyword: String = ""
  private var priority: LogPriority = VERBOSE
  private var logs: MutableList<LogItem> = mutableListOf()
  private val logData = MutableLiveData<LogItem>()

  override fun onDetach() {
    super.onDetach()
    logDisposable.dispose()
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    btn_priority.setOnClickListener { popPriority(it) }
    btn_clean.setOnClickListener { cleanLog(it) }
    adapter = log_list.linear(reverseLayout = true)
        .divider(R.drawable.divider_item_decoration_transparent)
        .setup {
          addType<LogItem>(R.layout.item_log)
          onBind {
            val tv_tag = findView<TextView>(R.id.tv_tag)
            val tv_divider = findView<TextView>(R.id.tv_divider)
            val tv_msg = findView<TextView>(R.id.tv_msg)
            val textColor = ResourcesCompat.getColor(
                resources, getModel<LogItem>().priority.textColor, null
            )
            tv_tag.setTextColor(textColor)
            tv_tag.text = getModel<LogItem>().tag ?: "Unknown"
            tv_divider.setTextColor(textColor)
            tv_msg.setTextColor(textColor)
            tv_msg.text = getModel<LogItem>().message
          }
          R.id.log_box.onClick {
            val clipboard: ClipboardManager =
              context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData: ClipData =
              ClipData.newPlainText(null, JSONS.parseJson(getModel<LogItem>()))
            clipboard.setPrimaryClip(clipData)
            Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT)
                .show()
          }
        }
    et_filter.addTextChangedListener {
      keyword = it?.toString()?:""
      updateLogs()
    }
    log_list.addOnScrollListener(object : OnScrollListener() {
      override fun onScrolled(
        recyclerView: RecyclerView,
        dx: Int,
        dy: Int
      ) {
        listScrollY -= dy
        if (((recyclerView.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
                ?: 0) > 2
        ) {
          to_bottom.visibility = View.VISIBLE
        } else {
          to_bottom.visibility = View.GONE
        }
      }
    })
    to_bottom.setOnClickListener {
      log_list.scrollToPosition(0)
    }
    logData.observe(viewLifecycleOwner){
      logs.add(0, it)
      logs = logs.take(MAX_SIZE).toMutableList()
      updateLogs()
      val visiblePosition = (log_list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
      if (visiblePosition < 2) {
        log_list.scrollToPosition(0)
      }
    }
    logDisposable = Logger.logPublisher
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          logData.value = it
        }
  }

  private fun cleanLog(v: View) {
    logs.clear()
    updateLogs()
  }

  private fun popPriority(v: View) {
    PopupWindow().apply {
      width = RelativeLayout.LayoutParams.WRAP_CONTENT
      height = RelativeLayout.LayoutParams.WRAP_CONTENT
      isFocusable = true
      isOutsideTouchable = true
      val dw = ColorDrawable(-0x50000000)
      setBackgroundDrawable(dw)
      contentView = LayoutInflater.from(context)
          .inflate(R.layout.pop_priority, null)
      PopPriorityBinding.bind(contentView).popList
          .linear()
          .setup {
            addType<LogPriority>(R.layout.item_log_priority)
            onBind {
              findView<TextView>(R.id.tv_priority_item).text = getModel<LogPriority>().name
            }
            R.id.tv_priority_item.onClick {
              priority = getModel()
              updatePriority()
              updateLogs()
              dismiss()
            }
          }.models = LogPriority.values().toList()
    }
        .showAsDropDown(v)
  }

  private fun updatePriority() {
    btn_priority.text = priority.name
    val textColor = ResourcesCompat.getColor(
        resources, priority.textColor, null
    )
    btn_priority.setTextColor(textColor)
  }

  private fun updateLogs() {
    adapter.models = logs.filter {
      if (keyword.isNotEmpty()) {
        it.message.contains(keyword, true)
      } else {
        true
      }
    }.filter {
      it.priority.value >= priority.value
    }
  }
}