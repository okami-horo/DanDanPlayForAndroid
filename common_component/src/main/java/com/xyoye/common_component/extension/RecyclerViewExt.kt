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

    // 如果View不存在，使用平滑滚动到目标位置
    val useSmoothScroll = true
    if (useSmoothScroll) {
        smoothScrollToPosition(index)
    } else {
        scrollToPosition(index)
    }

    // 优化的重试机制
    var retryCount = 0
    val maxRetries = 8
    val baseDelay = 50L
    val maxDelay = 200L

    fun tryRequestFocus() {
        if (retryCount >= maxRetries) {
            // 重试失败，恢复到之前的焦点位置
            if (currentFocusedPosition != -1 && currentFocusedPosition != index) {
                post { requestIndexChildFocus(currentFocusedPosition) }
            }
            return
        }

        retryCount++
        
        // 使用指数退避策略，延迟时间递增但不超过最大值
        val delay = minOf(baseDelay * retryCount, maxDelay)
        
        post {
            val view = layoutManager?.findViewByPosition(index)
            val focusableView = view?.findViewWithTag<View>(targetTag)
            
            if (focusableView != null && focusableView.isFocusable) {
                // 成功找到可聚焦的View
                focusableView.requestFocus()
            } else {
                // 继续重试，确保View已创建
                if (view == null) {
                    // View可能还未创建，强制布局计算
                    layoutManager?.let { lm ->
                        if (lm is LinearLayoutManager) {
                            lm.scrollToPositionWithOffset(index, 0)
                        } else {
                            scrollToPosition(index)
                        }
                    }
                    requestLayout()
                }
                
                // 继续重试
                postDelayed({ tryRequestFocus() }, delay)
            }
        }
    }

    // 延迟开始重试，给滚动动画留出时间
    val initialDelay = if (useSmoothScroll) 150L else 50L
    postDelayed({ tryRequestFocus() }, initialDelay)
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