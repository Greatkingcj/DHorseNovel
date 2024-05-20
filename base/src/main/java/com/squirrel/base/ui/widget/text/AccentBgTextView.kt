package com.squirrel.base.ui.widget.text

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.squirrel.base.R
import com.squirrel.base.lib.theme.Selector
import com.squirrel.base.lib.theme.ThemeStore
import com.squirrel.base.utils.ColorUtils
import com.squirrel.base.utils.dpToPx
import com.squirrel.base.utils.getCompatColor

class AccentBgTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    private var radius = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccentBgTextView)
        radius = typedArray.getDimensionPixelOffset(R.styleable.AccentBgTextView_radius, radius)
        typedArray.recycle()
        upBackground()
    }

    fun setRadius(radius: Int) {
        this.radius = radius.dpToPx()
        upBackground()
    }

    private fun upBackground() {
        val accentColor = if (isInEditMode) {
            context.getCompatColor(R.color.accent)
        } else {
            ThemeStore.accentColor(context)
        }
        background = Selector.shapeBuild()
            .setCornerRadius(radius)
            .setDefaultBgColor(accentColor)
            .setPressedBgColor(ColorUtils.darkenColor(accentColor))
            .create()
        setTextColor(
            if (ColorUtils.isColorLight(accentColor)) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        )
    }
}
