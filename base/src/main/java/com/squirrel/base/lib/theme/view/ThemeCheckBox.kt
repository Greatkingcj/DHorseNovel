package com.squirrel.base.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import com.squirrel.base.lib.theme.accentColor
import com.squirrel.base.utils.applyTint

class ThemeCheckBox(context: Context, attrs: AttributeSet) : AppCompatCheckBox(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}
