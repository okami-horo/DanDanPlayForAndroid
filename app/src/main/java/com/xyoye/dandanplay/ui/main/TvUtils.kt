package com.xyoye.dandanplay.ui.main

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

/**
 * TV端检测工具类
 */
object TvUtils {
    
    /**
     * 检测当前是否运行在TV设备上
     */
    fun isTvDevice(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
    
    /**
     * 检测是否支持TV界面（包括TV设备和大屏设备）
     */
    fun supportTvInterface(context: Context): Boolean {
        return isTvDevice(context) || isLargeScreen(context)
    }
    
    /**
     * 检测是否为大屏设备
     */
    private fun isLargeScreen(context: Context): Boolean {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}
