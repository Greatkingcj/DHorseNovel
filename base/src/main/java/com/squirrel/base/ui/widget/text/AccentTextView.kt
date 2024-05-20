package com.squirrel.base.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.squirrel.base.R
import com.squirrel.base.lib.theme.accentColor
import com.squirrel.base.utils.getCompatColor

class AccentTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    init {
        if (!isInEditMode) {
            setTextColor(context.accentColor)
        } else {
            setTextColor(context.getCompatColor(R.color.accent))
        }
    }

}
