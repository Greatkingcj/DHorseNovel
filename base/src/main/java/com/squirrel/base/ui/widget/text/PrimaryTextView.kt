package com.squirrel.base.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.squirrel.base.lib.theme.ThemeStore

/**
 * @author Aidan Follestad (afollestad)
 */
@Suppress("unused")
class PrimaryTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    init {
        setTextColor(ThemeStore.textColorPrimary(context))
    }
}
