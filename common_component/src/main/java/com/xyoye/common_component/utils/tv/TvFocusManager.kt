package com.xyoye.common_component.utils.tv

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.xyoye.common_component.extension.getCurrentFocusedPosition
import com.xyoye.common_component.extension.requestIndexChildFocusSafe
import java.util.concurrent.ConcurrentHashMap

/**
 * TV端焦点状态管理器
 * 用于保存和恢复RecyclerView的焦点状态，防止焦点意外丢失
 */
object TvFocusManager {
    
    private val focusStateMap = ConcurrentHashMap<String, FocusState>()
    
    /**
     * 焦点状态数据类
     */
    data class FocusState(
        val position: Int,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 保存RecyclerView的焦点状态
     * @param key 唯一标识符，建议使用Activity/Fragment类名+RecyclerView的id
     * @param recyclerView 要保存焦点状态的RecyclerView
     */
    fun saveFocusState(key: String, recyclerView: RecyclerView) {
        val position = recyclerView.getCurrentFocusedPosition()
        if (position != -1) {
            focusStateMap[key] = FocusState(position)
        }
    }
    
    /**
     * 恢复RecyclerView的焦点状态
     * @param key 唯一标识符
     * @param recyclerView 要恢复焦点状态的RecyclerView
     * @param maxAge 焦点状态的最大有效时间（毫秒），超过此时间的状态将被忽略
     * @return 是否成功恢复焦点
     */
    fun restoreFocusState(key: String, recyclerView: RecyclerView, maxAge: Long = 30_000): Boolean {
        val focusState = focusStateMap[key] ?: return false
        
        // 检查焦点状态是否过期
        if (System.currentTimeMillis() - focusState.timestamp > maxAge) {
            focusStateMap.remove(key)
            return false
        }
        
        val adapter = recyclerView.adapter ?: return false
        if (focusState.position >= 0 && focusState.position < adapter.itemCount) {
            return recyclerView.requestIndexChildFocusSafe(focusState.position, 0)
        }
        
        return false
    }
    
    /**
     * 清除指定key的焦点状态
     */
    fun clearFocusState(key: String) {
        focusStateMap.remove(key)
    }
    
    /**
     * 清除所有焦点状态
     */
    fun clearAllFocusStates() {
        focusStateMap.clear()
    }
    
    /**
     * 清除过期的焦点状态
     * @param maxAge 最大有效时间（毫秒）
     */
    fun clearExpiredFocusStates(maxAge: Long = 300_000) {
        val currentTime = System.currentTimeMillis()
        val iterator = focusStateMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value.timestamp > maxAge) {
                iterator.remove()
            }
        }
    }
    
    /**
     * 获取焦点状态
     */
    fun getFocusState(key: String): FocusState? {
        return focusStateMap[key]
    }
    
    /**
     * 为RecyclerView设置智能焦点恢复
     * 在数据更新后自动尝试恢复焦点到合适的位置
     */
    fun setupSmartFocusRestore(key: String, recyclerView: RecyclerView) {
        recyclerView.adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                // 数据完全改变时，尝试恢复焦点
                recyclerView.post {
                    if (!restoreFocusState(key, recyclerView)) {
                        // 如果无法恢复，则设置焦点到第一个item
                        recyclerView.requestIndexChildFocusSafe(0)
                    }
                }
            }
            
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                // 插入数据时，调整焦点位置
                val focusState = getFocusState(key)
                if (focusState != null && positionStart <= focusState.position) {
                    // 如果在当前焦点位置之前插入了数据，需要调整焦点位置
                    focusStateMap[key] = focusState.copy(position = focusState.position + itemCount)
                }
            }
            
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                // 删除数据时，调整焦点位置
                val focusState = getFocusState(key)
                if (focusState != null) {
                    when {
                        positionStart + itemCount <= focusState.position -> {
                            // 删除的数据在焦点位置之前，调整焦点位置
                            focusStateMap[key] = focusState.copy(position = focusState.position - itemCount)
                        }
                        positionStart <= focusState.position -> {
                            // 焦点位置的数据被删除，设置到删除位置的前一个
                            val newPosition = maxOf(0, positionStart - 1)
                            focusStateMap[key] = focusState.copy(position = newPosition)
                        }
                    }
                }
            }
        })
    }
}

/**
 * RecyclerView扩展函数，简化焦点状态管理
 */
fun RecyclerView.saveTvFocus(key: String) {
    TvFocusManager.saveFocusState(key, this)
}

fun RecyclerView.restoreTvFocus(key: String, maxAge: Long = 30_000): Boolean {
    return TvFocusManager.restoreFocusState(key, this, maxAge)
}

fun RecyclerView.setupSmartTvFocus(key: String) {
    TvFocusManager.setupSmartFocusRestore(key, this)
}
