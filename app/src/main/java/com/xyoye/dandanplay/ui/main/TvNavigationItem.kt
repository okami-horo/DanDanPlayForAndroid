package com.xyoye.dandanplay.ui.main

import androidx.annotation.DrawableRes

/**
 * TV端导航菜单项数据类
 */
data class TvNavigationItem(
    val id: Int,
    val title: String,
    @DrawableRes val iconRes: Int,
    var isSelected: Boolean = false
)
