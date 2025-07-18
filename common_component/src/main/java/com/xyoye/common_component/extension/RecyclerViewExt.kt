package com.xyoye.common_component.extension

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xyoye.common_component.R
import com.xyoye.common_component.adapter.BaseAdapter

/**
 * Created by xyoye on 2020/8/17.
 */

fun RecyclerView.vertical(reverse: Boolean = false): LinearLayoutManager {
    return LinearLayoutManager(context, LinearLayoutManager.VERTICAL, reverse)
}

fun RecyclerView.horizontal(reverse: Boolean = false): LinearLayoutManager {
    return LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, reverse)
}

fun RecyclerView.grid(spanCount: Int): GridLayoutManager {
    return GridLayoutManager(context, spanCount)
}

fun RecyclerView.gridEmpty(spanCount: Int): GridLayoutManager {
    return GridLayoutManager(context, spanCount).also {
        it.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (position == RecyclerView.NO_POSITION) {
                    return 1
                }
                val viewType = adapter?.getItemViewType(position)
                if (viewType != BaseAdapter.VIEW_TYPE_EMPTY) {
                    return 1
                }
                return spanCount
            }
        }
    }
}

fun RecyclerView.setData(items: List<Any>) {
    (adapter as? BaseAdapter)?.setData(items)
}

fun RecyclerView.requestIndexChildFocus(index: Int): Boolean {
    // 边界检查
    val adapter = adapter ?: return false
    if (index < 0 || index >= adapter.itemCount) {
        return false
    }

    // 保存当前焦点状态
    val currentFocusedPosition = getCurrentFocusedPosition()

    // 先尝试直接查找View
    val targetTag = R.string.focusable_item.toResString()
    var indexView = layoutManager?.findViewByPosition(index)
    if (indexView != null) {
        val focusableView = indexView.findViewWithTag<View>(targetTag)
        if (focusableView != null && focusableView.isFocusable) {
            focusableView.requestFocus()
            return true
        }
    }

    // 如果View不存在，需要滚动到目标位置
    // 使用scrollToPosition确保立即滚动，而不是平滑滚动
    scrollToPosition(index)

    // 使用重试机制确保焦点设置成功
    var retryCount = 0
    val maxRetries = 8  // 进一步增加重试次数

    fun tryRequestFocus() {
        if (retryCount >= maxRetries) {
            // 如果多次重试失败，恢复到之前的焦点位置
            if (currentFocusedPosition != -1 && currentFocusedPosition != index) {
                post { requestIndexChildFocus(currentFocusedPosition) }
            }
            return
        }

        retryCount++
        post {
            val view = layoutManager?.findViewByPosition(index)
            val focusableView = view?.findViewWithTag<View>(targetTag)
            if (focusableView != null && focusableView.isFocusable) {
                focusableView.requestFocus()
            } else {
                // 如果View仍然不存在，可能需要强制布局和滚动
                if (view == null) {
                    // 强制滚动并请求布局
                    scrollToPosition(index)
                    requestLayout()
                }
                // 继续重试，使用递增的延迟时间
                val delay = if (retryCount <= 3) 50L else 100L
                postDelayed({ tryRequestFocus() }, delay)
            }
        }
    }

    tryRequestFocus()
    return true
}

/**
 * 获取当前获得焦点的item位置
 */
fun RecyclerView.getCurrentFocusedPosition(): Int {
    val focusedChild = focusedChild ?: return -1
    return getChildAdapterPosition(focusedChild)
}

/**
 * 安全的焦点请求，带有边界检查和状态验证
 */
fun RecyclerView.requestIndexChildFocusSafe(index: Int, fallbackIndex: Int = -1): Boolean {
    val success = requestIndexChildFocus(index)
    if (!success && fallbackIndex != -1 && fallbackIndex != index) {
        return requestIndexChildFocus(fallbackIndex)
    }
    return success
}