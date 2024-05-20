package com.squirrel.base.ui.book.read.page.entities.column

import android.graphics.Canvas
import androidx.annotation.Keep
import com.squirrel.base.ui.book.read.page.ContentTextView
import com.squirrel.base.ui.book.read.page.entities.TextLine
import com.squirrel.base.ui.book.read.page.entities.TextLine.Companion.emptyTextLine


/**
 * 按钮列
 */
@Keep
data class ButtonColumn(
    override var start: Float,
    override var end: Float,
) : BaseColumn {
    override var textLine: TextLine = emptyTextLine
    override fun draw(view: ContentTextView, canvas: Canvas) {

    }
}