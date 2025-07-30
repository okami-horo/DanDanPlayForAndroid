package com.xyoye.user_component.ui.fragment.personal

import android.view.KeyEvent
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.core.widget.NestedScrollView

/**
 * TV端遥控器焦点处理工具类
 * 专门处理TV遥控器焦点导航和滚动问题
 */
object TvFocusHandler {

    /**
     * 处理TV遥控器按键事件
     * @param view 当前聚焦的视图
     * @param keyCode 按键码
     * @param event 按键事件
     * @param focusableViews 可聚焦视图列表
     * @return 是否处理了事件
     */
    fun handleKeyEvent(
        view: View,
        keyCode: Int,
        event: KeyEvent,
        focusableViews: List<View>
    ): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val currentIndex = focusableViews.indexOf(view)
        if (currentIndex == -1) return false

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> handleUpNavigation(currentIndex, focusableViews)
            KeyEvent.KEYCODE_DPAD_DOWN -> handleDownNavigation(currentIndex, focusableViews)
            KeyEvent.KEYCODE_DPAD_LEFT -> handleLeftNavigation(view)
            KeyEvent.KEYCODE_DPAD_RIGHT -> handleRightNavigation(view)
            else -> false
        }
    }

    /**
     * 处理向上导航
     */
    private fun handleUpNavigation(
        currentIndex: Int,
        focusableViews: List<View>
    ): Boolean {
        if (currentIndex > 0) {
            val targetView = focusableViews[currentIndex - 1]
            if (targetView.isVisible && targetView.isFocusable) {
                targetView.requestFocus()
                ensureViewVisible(targetView)
                return true
            }
        }
        return false
    }

    /**
     * 判断目标视图是否是当前视图的左侧邻居
     */
    private fun isLeftNeighbor(currentView: View, targetView: View): Boolean {
        val currentRect = Rect().apply { currentView.getGlobalVisibleRect(this) }
        val targetRect = Rect().apply { targetView.getGlobalVisibleRect(this) }
        
        // 目标视图在当前视图的左侧，并且在同一行
        return targetRect.right <= currentRect.left && 
               targetRect.top < currentRect.bottom && 
               targetRect.bottom > currentRect.top &&
               targetView.isVisible && 
               targetView.isFocusable
    }

    /**
     * 判断目标视图是否是当前视图的右侧邻居
     */
    private fun isRightNeighbor(currentView: View, targetView: View): Boolean {
        val currentRect = Rect().apply { currentView.getGlobalVisibleRect(this) }
        val targetRect = Rect().apply { targetView.getGlobalVisibleRect(this) }
        
        // 目标视图在当前视图的右侧，并且在同一行
        return targetRect.left >= currentRect.right && 
               targetRect.top < currentRect.bottom && 
               targetRect.bottom > currentRect.top &&
               targetView.isVisible && 
               targetView.isFocusable
    }

    /**
     * 处理向下导航
     */
    private fun handleDownNavigation(
        currentIndex: Int,
        focusableViews: List<View>
    ): Boolean {
        if (currentIndex < focusableViews.size - 1) {
            val targetView = focusableViews[currentIndex + 1]
            if (targetView.isVisible && targetView.isFocusable) {
                targetView.requestFocus()
                ensureViewVisible(targetView)
                return true
            }
        }
        return false
    }

    /**
     * 处理向左导航
     */
    private fun handleLeftNavigation(currentView: View): Boolean {
        val parent = currentView.parent as? ViewGroup ?: return false
        val siblings = getFocusableViews(parent)
        val currentIndex = siblings.indexOf(currentView)
        
        // 查找左侧邻居视图
        for (i in currentIndex - 1 downTo 0) {
            val targetView = siblings[i]
            if (isLeftNeighbor(currentView, targetView)) {
                targetView.requestFocus()
                ensureViewVisible(targetView)
                return true
            }
        }
        
        return false
    }

    /**
     * 处理向右导航
     */
    private fun handleRightNavigation(currentView: View): Boolean {
        val parent = currentView.parent as? ViewGroup ?: return false
        val siblings = getFocusableViews(parent)
        val currentIndex = siblings.indexOf(currentView)
        
        // 查找右侧邻居视图
        for (i in currentIndex + 1 until siblings.size) {
            val targetView = siblings[i]
            if (isRightNeighbor(currentView, targetView)) {
                targetView.requestFocus()
                ensureViewVisible(targetView)
                return true
            }
        }
        
        return false
    }

    /**
     * 确保视图在可见区域
     */
    private fun ensureViewVisible(targetView: View) {
        val scrollView = findParentScrollView(targetView)
        scrollView?.let {
            val scrollBounds = android.graphics.Rect()
            it.getDrawingRect(scrollBounds)
            
            val viewBounds = android.graphics.Rect()
            targetView.getDrawingRect(viewBounds)
            it.offsetDescendantRectToMyCoords(targetView, viewBounds)
            
            if (!scrollBounds.contains(viewBounds)) {
                it.smoothScrollTo(0, viewBounds.top - scrollBounds.height() / 2)
            }
        }
    }

    /**
     * 查找父级滚动视图
     */
    private fun findParentScrollView(view: View): NestedScrollView? {
        var parent = view.parent
        while (parent != null && parent !is NestedScrollView) {
            parent = parent.parent
        }
        return parent as? NestedScrollView
    }

    /**
     * 设置TV端焦点高亮效果
     */
    fun setupFocusHighlight(view: View, focused: Boolean) {
        view.isSelected = focused
        view.alpha = if (focused) 1.0f else 0.8f
        
        // 添加缩放效果
        val scale = if (focused) 1.05f else 1.0f
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(150)
            .start()
    }

    /**
     * 获取所有可聚焦的子视图
     */
    fun getFocusableViews(parent: ViewGroup): List<View> {
        return parent.children
            .filter { it.isVisible && it.isFocusable }
            .toList()
    }

    /**
     * 设置焦点监听器
     */
    fun setupFocusListener(view: View) {
        view.setOnFocusChangeListener { v, hasFocus ->
            setupFocusHighlight(v, hasFocus)
        }
    }
}