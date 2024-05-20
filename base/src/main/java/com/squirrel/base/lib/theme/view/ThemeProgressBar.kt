package com.squirrel.base.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import com.squirrel.base.lib.theme.accentColor
import com.squirrel.base.utils.applyTint

class ThemeProgressBar(context: Context, attrs: AttributeSet) : ProgressBar(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}