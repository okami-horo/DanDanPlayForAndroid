package com.xyoye.user_component.ui.fragment.personal

import android.view.KeyEvent
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
            KeyEvent.KEYCODE_DPAD_UP -> handleUpNavigation(view, currentIndex, focusableViews)
            KeyEvent.KEYCODE_DPAD_DOWN -> handleDownNavigation(view, currentIndex, focusableViews)
            KeyEvent.KEYCODE_DPAD_LEFT -> handleLeftNavigation(view, focusableViews)
            KeyEvent.KEYCODE_DPAD_RIGHT -> handleRightNavigation(view, focusableViews)
            else -> false
        }
    }

    /**
     * 处理向上导航
     */
    private fun handleUpNavigation(
        currentView: View,
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
     * 处理向下导航
     */
    private fun handleDownNavigation(
        currentView: View,
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
    private fun handleLeftNavigation(currentView: View, focusableViews: List<View>): Boolean {
        // 在TV端，向左通常不应该离开当前视图
        // 防止焦点跳到左侧菜单栏
        return true
    }

    /**
     * 处理向右导航
     */
    private fun handleRightNavigation(currentView: View, focusableViews: List<View>): Boolean {
        // 在TV端，向右通常不应该离开当前视图
        // 防止焦点跳到其他区域
        return true
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