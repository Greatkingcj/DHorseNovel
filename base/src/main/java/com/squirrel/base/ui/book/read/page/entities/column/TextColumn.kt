package com.squirrel.base.ui.book.read.page.entities.column

import android.graphics.Canvas
import androidx.annotation.Keep
import com.squirrel.base.help.config.ReadBookConfig
import com.squirrel.base.lib.theme.ThemeStore
import com.squirrel.base.ui.book.read.page.ContentTextView
import com.squirrel.base.ui.book.read.page.entities.TextLine
import com.squirrel.base.ui.book.read.page.entities.TextLine.Companion.emptyTextLine
import com.squirrel.base.ui.book.read.page.provider.ChapterProvider

/**
 * 文字列
 */
@Keep
data class TextColumn(
    override var start: Float,
    override var end: Float,
    val charData: String,
) : BaseColumn {

    override var textLine: TextLine = emptyTextLine

    var selected: Boolean = false
        set(value) {
            if (field != value) {
                textLine.invalidate()
            }
            field = value
        }
    var isSearchResult: Boolean = false
        set(value) {
            if (field != value) {
                textLine.invalidate()
                if (value) {
                    textLine.searchResultColumnCount++
                } else {
                    textLine.searchResultColumnCount--
                }
            }
            field = value
        }

    override fun draw(view: ContentTextView, canvas: Canvas) {
        val textPaint = if (textLine.isTitle) {
            ChapterProvider.titlePaint
        } else {
            ChapterProvider.contentPaint
        }
        val textColor = if (textLine.isReadAloud || isSearchResult) {
            ThemeStore.accentColor
        } else {
            ReadBookConfig.textColor
        }
        if (textPaint.color != textColor) {
            textPaint.color = textColor
        }
        canvas.drawText(charData, start, textLine.lineBase - textLine.lineTop, textPaint)
        if (selected) {
            canvas.drawRect(start, 0f, end, textLine.height, view.selectedPaint)
        }
    }

}
