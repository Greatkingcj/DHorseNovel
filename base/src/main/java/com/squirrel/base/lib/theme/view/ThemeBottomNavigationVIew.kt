package com.squirrel.base.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.squirrel.base.databinding.ViewNavigationBadgeBinding
import com.squirrel.base.lib.theme.Selector
import com.squirrel.base.lib.theme.ThemeStore
import com.squirrel.base.lib.theme.bottomBackground
import com.squirrel.base.lib.theme.getSecondaryTextColor
import com.squirrel.base.ui.widget.text.BadgeView
import com.squirrel.base.utils.ColorUtils

class ThemeBottomNavigationVIew(context: Context, attrs: AttributeSet) :
    BottomNavigationView(context, attrs) {

    init {
        val bgColor = context.bottomBackground
        setBackgroundColor(bgColor)
        val textIsDark = ColorUtils.isColorLight(bgColor)
        val textColor = context.getSecondaryTextColor(textIsDark)
        val colorStateList = Selector.colorBuild()
            .setDefaultColor(textColor)
            .setSelectedColor(ThemeStore.accentColor(context)).create()
        itemIconTintList = colorStateList
        itemTextColor = colorStateList
    }

    fun addBadgeView(index: Int): BadgeView {
        //获取底部菜单view
        val menuView = getChildAt(0) as BottomNavigationMenuView
        //获取第index个itemView
        val itemView = menuView.getChildAt(index) as BottomNavigationItemView
        val badgeBinding = ViewNavigationBadgeBinding.inflate(LayoutInflater.from(context))
        itemView.addView(badgeBinding.root)
        return badgeBinding.viewBadge
    }

}