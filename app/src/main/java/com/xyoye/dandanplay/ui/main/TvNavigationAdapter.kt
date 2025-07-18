package com.xyoye.dandanplay.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xyoye.dandanplay.R

/**
 * TV端导航菜单适配器
 */
class TvNavigationAdapter(
    private val items: List<TvNavigationItem>,
    private val onItemClick: (TvNavigationItem) -> Unit
) : RecyclerView.Adapter<TvNavigationAdapter.ViewHolder>() {

    private var selectedPosition = 0

    inner class ViewHolder(itemView: android.view.View) :
        RecyclerView.ViewHolder(itemView) {

        private val navIconIv: ImageView = itemView.findViewById(R.id.nav_icon_iv)
        private val navTitleTv: TextView = itemView.findViewById(R.id.nav_title_tv)

        fun bind(item: TvNavigationItem, position: Int) {
            navIconIv.setImageResource(item.iconRes)
            navTitleTv.text = item.title

            // 设置选中状态
            itemView.isSelected = item.isSelected

            // 设置点击事件
            itemView.setOnClickListener {
                selectItem(position)
                onItemClick(item)
            }

            // 设置焦点事件
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    selectItem(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_tv_navigation,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    /**
     * 选择指定位置的菜单项
     */
    fun selectItem(position: Int) {
        if (position == selectedPosition) return
        
        // 取消之前选中的项
        items[selectedPosition].isSelected = false
        notifyItemChanged(selectedPosition)
        
        // 选中新的项
        selectedPosition = position
        items[position].isSelected = true
        notifyItemChanged(position)
    }

    /**
     * 获取当前选中的位置
     */
    fun getSelectedPosition(): Int = selectedPosition

    /**
     * 设置选中位置
     */
    fun setSelectedPosition(position: Int) {
        if (position in 0 until items.size) {
            selectItem(position)
        }
    }
}
