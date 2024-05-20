package com.squirrel.base.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import com.squirrel.base.lib.theme.accentColor
import com.squirrel.base.utils.applyTint

/**
 * @author Aidan Follestad (afollestad)
 */
class ThemeSeekBar(context: Context, attrs: AttributeSet) : AppCompatSeekBar(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}
