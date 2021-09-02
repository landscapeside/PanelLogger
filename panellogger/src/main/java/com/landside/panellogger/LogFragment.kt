package com.landside.panellogger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.landside.panellogger.LogFragment.LogAdapter.LogHolder
import com.landside.panellogger.LogFragment.LogPriorityAdapter.PriorityHolder
import com.landside.panellogger.LogPriority.VERBOSE
import com.landside.panellogger.databinding.ItemLogPriorityBinding
import com.landside.panellogger.databinding.PopPriorityBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_log.*
import kotlinx.android.synthetic.main.item_log.view.*

class LogFragment : Fragment(R.layout.fragment_log) {

  private var logDisposable: Disposable? = null
  private lateinit var adapter: LogAdapter
  private var listScrollY: Int = 0
  private var keyword: String = ""
  private var priority: LogPriority = VERBOSE
  private val logs: List<LogItem>
    get() = logData.value?.filter {
      if (keyword.isNotEmpty()) {
        it.message.contains(keyword, true)
      } else {
        true
      }
    }
        ?.filter {
          it.priority.value >= priority.value
        } ?: mutableListOf()
  private val logData = MutableLiveData<MutableList<LogItem>>()

  override fun onDetach() {
    super.onDetach()
    logDisposable?.dispose()
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    btn_priority.setOnClickListener { popPriority(it) }
    btn_clean.setOnClickListener { cleanLog(it) }
    adapter = LogAdapter()
    log_list.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true)
    log_list.addItemDecoration(
        DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
    )
    log_list.adapter = adapter
    et_filter.addTextChangedListener {
      keyword = it?.toString() ?: ""
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
    logData.value = mutableListOf()
    logData.observe(viewLifecycleOwner) {
      updateLogs()
      val visiblePosition =
        (log_list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
      if (visiblePosition < 2) {
        log_list.scrollToPosition(0)
      }
    }
    logDisposable = Logger.logPublisher
        ?.subscribeOn(Schedulers.io())
        ?.observeOn(AndroidSchedulers.mainThread())
        ?.subscribe {
          logData.value = logData.value?.toMutableList()
              ?.apply {
                add(0, it)
              }
              ?.toMutableList()
        }
  }

  private fun cleanLog(v: View) {
    logData.value?.clear()
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
      PopPriorityBinding.bind(contentView).popList.layoutManager =
        LinearLayoutManager(requireContext())
      PopPriorityBinding.bind(contentView).popList
          .adapter = LogPriorityAdapter {
        priority = it
        updatePriority()
        updateLogs()
        dismiss()
      }
    }
        .showAsDropDown(v)
  }

  inner class LogPriorityAdapter(
    val onItemClick: (LogPriority) -> Unit
  ) : RecyclerView.Adapter<PriorityHolder>() {
    val inflater: LayoutInflater = LayoutInflater.from(context)

    inner class PriorityHolder(val itemBinding: ItemLogPriorityBinding) : RecyclerView.ViewHolder(
        itemBinding.root
    )

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int
    ): PriorityHolder =
      PriorityHolder(ItemLogPriorityBinding.inflate(inflater,parent,false))

    override fun onBindViewHolder(
      holder: PriorityHolder,
      position: Int
    ) {
      holder.itemBinding.tvPriorityItem.text = LogPriority.values()[position].name
      holder.itemBinding.tvPriorityItem.setOnClickListener {
        onItemClick(LogPriority.values()[position])
      }
    }

    override fun getItemCount(): Int = LogPriority.values()
        .toList().size

  }

  private fun updatePriority() {
    btn_priority.text = priority.name
    val textColor = ResourcesCompat.getColor(
        resources, priority.textColor, null
    )
    btn_priority.setTextColor(textColor)
  }

  private fun updateLogs() {
    adapter.notifyDataSetChanged()
  }

  inner class LogAdapter : RecyclerView.Adapter<LogHolder>() {
    val inflater: LayoutInflater = LayoutInflater.from(context)

    inner class LogHolder(val container: View) : RecyclerView.ViewHolder(
        container
    )

    override fun onCreateViewHolder(
      parent: ViewGroup,
      viewType: Int
    ): LogHolder =
      LogHolder(inflater.inflate(R.layout.item_log,parent,false))

    override fun onBindViewHolder(
      holder: LogHolder,
      position: Int
    ) {
      val textColor = ResourcesCompat.getColor(
          resources, logs[position].priority.textColor, null
      )
      holder.container.tv_tag.setTextColor(textColor)
      holder.container.tv_tag.text = logs[position].tag ?: "Unknown"
      holder.container.tv_divider.setTextColor(textColor)
      holder.container.tv_msg.setTextColor(textColor)
      holder.container.tv_msg.text = logs[position].message
      holder.container.log_box.setOnClickListener {
        val clipboard: ClipboardManager =
          requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData: ClipData =
          ClipData.newPlainText(null, JSONS.parseJson(logs[position]))
        clipboard.setPrimaryClip(clipData)
        Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT)
            .show()
      }
    }

    override fun getItemCount(): Int = logs.size

  }

}