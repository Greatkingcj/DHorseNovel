package com.squirrel.base.ui.book.read.page.api

import com.squirrel.base.model.ReadBook
import com.squirrel.base.ui.book.read.page.entities.TextChapter

interface DataSource {

    val pageIndex: Int get() = ReadBook.durPageIndex

    val currentChapter: TextChapter?

    val nextChapter: TextChapter?

    val prevChapter: TextChapter?

    val isScroll: Boolean

    fun hasNextChapter(): Boolean

    fun hasPrevChapter(): Boolean

    fun upContent(relativePosition: Int = 0, resetPageOffset: Boolean = true)

}