package com.squirrel.base.utils

import android.text.TextPaint

val TextPaint.textHeight: Float
    get() = fontMetrics.run { descent - ascent + leading }
