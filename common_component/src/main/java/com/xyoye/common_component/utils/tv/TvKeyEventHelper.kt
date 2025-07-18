package com.xyoye.common_component.utils.tv

import android.view.KeyEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xyoye.common_component.extension.getCurrentFocusedPosition
import com.xyoye.common_component.extension.nextItemIndexSafe
import com.xyoye.common_component.extension.previousItemIndexSafe
import com.xyoye.common_component.extension.requestIndexChildFocusSafe

/**
 * TV端按键事件处理增强工具类
 * 提供更智能的焦点导航和按键处理
 */
object TvKeyEventHelper {
    
    /**
     * 处理RecyclerView的方向键事件
     * @param recyclerView 目标RecyclerView
     * @param keyCode 按键代码
     * @param items 数据列表
     * @return 是否处理了该事件
     */
    fun handleRecyclerViewKeyEvent(
        recyclerView: RecyclerView,
        keyCode: Int,
        items: List<*>
    ): Boolean {
        val focusedChild = recyclerView.focusedChild ?: return false
        val focusedIndex = recyclerView.getChildAdapterPosition(focusedChild)
        if (focusedIndex == -1) return false

        val targetIndex = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                handleLeftKey(recyclerView, focusedIndex, items)
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                handleRightKey(recyclerView, focusedIndex, items)
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                handleUpKey(recyclerView, focusedIndex, items)
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                handleDownKey(recyclerView, focusedIndex, items)
            }
            else -> return false
        }

        // 如果targetIndex为-1，表示无法处理该方向的导航，不消费事件
        if (targetIndex == -1) {
            return false
        }

        // 如果targetIndex与当前索引相同，表示已经在边界位置，消费事件但不移动焦点
        if (targetIndex == focusedIndex) {
            return true
        }

        // 尝试移动焦点到目标位置
        return recyclerView.requestIndexChildFocusSafe(targetIndex, focusedIndex)
    }
    
    /**
     * 处理左键事件
     */
    fun handleLeftKey(
        recyclerView: RecyclerView,
        currentIndex: Int,
        items: List<*>
    ): Int {
        return when (val layoutManager = recyclerView.layoutManager) {
            is GridLayoutManager -> {
                // 网格布局：向左移动一列
                val spanCount = layoutManager.spanCount
                if (currentIndex % spanCount > 0) {
                    maxOf(0, currentIndex - 1)
                } else {
                    // 已经在最左列，保持当前位置
                    currentIndex
                }
            }
            is LinearLayoutManager -> {
                if (layoutManager.orientation == LinearLayoutManager.HORIZONTAL) {
                    // 水平线性布局：向前移动
                    maxOf(0, currentIndex - 1)
                } else {
                    // 垂直线性布局：左键通常不处理
                    -1
                }
            }
            else -> maxOf(0, currentIndex - 1)
        }
    }
    
    /**
     * 处理右键事件
     */
    fun handleRightKey(
        recyclerView: RecyclerView,
        currentIndex: Int,
        items: List<*>
    ): Int {
        return when (val layoutManager = recyclerView.layoutManager) {
            is GridLayoutManager -> {
                // 网格布局：向右移动一列
                val spanCount = layoutManager.spanCount
                if (currentIndex % spanCount < spanCount - 1 && currentIndex < items.size - 1) {
                    minOf(items.size - 1, currentIndex + 1)
                } else {
                    // 已经在最右列，保持当前位置
                    currentIndex
                }
            }
            is LinearLayoutManager -> {
                if (layoutManager.orientation == LinearLayoutManager.HORIZONTAL) {
                    // 水平线性布局：向后移动
                    minOf(items.size - 1, currentIndex + 1)
                } else {
                    // 垂直线性布局：右键通常不处理
                    -1
                }
            }
            else -> minOf(items.size - 1, currentIndex + 1)
        }
    }
    
    /**
     * 处理上键事件
     */
    fun handleUpKey(
        recyclerView: RecyclerView,
        currentIndex: Int,
        items: List<*>
    ): Int {
        return when (val layoutManager = recyclerView.layoutManager) {
            is GridLayoutManager -> {
                // 网格布局：向上移动一行
                val spanCount = layoutManager.spanCount
                val targetIndex = currentIndex - spanCount
                if (targetIndex >= 0) {
                    // 确保目标位置有效
                    if (targetIndex < items.size) {
                        targetIndex
                    } else {
                        // 寻找最近的有效位置
                        maxOf(0, currentIndex - 1)
                    }
                } else {
                    // 已经在第一行，保持当前位置
                    currentIndex
                }
            }
            is LinearLayoutManager -> {
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    // 垂直线性布局：向前移动
                    maxOf(0, currentIndex - 1)
                } else {
                    // 水平线性布局：上键通常不处理
                    -1
                }
            }
            else -> maxOf(0, currentIndex - 1)
        }
    }
    
    /**
     * 处理下键事件
     */
    fun handleDownKey(
        recyclerView: RecyclerView,
        currentIndex: Int,
        items: List<*>
    ): Int {
        return when (val layoutManager = recyclerView.layoutManager) {
            is GridLayoutManager -> {
                // 网格布局：向下移动一行
                val spanCount = layoutManager.spanCount
                val targetIndex = currentIndex + spanCount
                if (targetIndex < items.size) {
                    // 确保目标位置有效
                    targetIndex
                } else {
                    // 已经在最后一行，保持当前位置
                    currentIndex
                }
            }
            is LinearLayoutManager -> {
                if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                    // 垂直线性布局：检查是否已经在最后一个项目
                    if (currentIndex >= items.size - 1) {
                        // 已经在最后一个项目，保持当前位置
                        currentIndex
                    } else {
                        // 移动到下一个项目
                        currentIndex + 1
                    }
                } else {
                    // 水平线性布局：下键通常不处理
                    -1
                }
            }
            else -> {
                // 其他布局管理器：检查是否已经在最后一个项目
                if (currentIndex >= items.size - 1) {
                    currentIndex
                } else {
                    currentIndex + 1
                }
            }
        }
    }
    
    /**
     * 检查是否为TV端方向键
     */
    fun isTvDirectionKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> true
            else -> false
        }
    }
    
    /**
     * 检查是否为TV端确认键
     */
    fun isTvConfirmKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> true
            else -> false
        }
    }
    
    /**
     * 获取相反方向的按键代码
     */
    fun getOppositeKeyCode(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> KeyEvent.KEYCODE_DPAD_RIGHT
            KeyEvent.KEYCODE_DPAD_RIGHT -> KeyEvent.KEYCODE_DPAD_LEFT
            KeyEvent.KEYCODE_DPAD_UP -> KeyEvent.KEYCODE_DPAD_DOWN
            KeyEvent.KEYCODE_DPAD_DOWN -> KeyEvent.KEYCODE_DPAD_UP
            else -> keyCode
        }
    }
}
