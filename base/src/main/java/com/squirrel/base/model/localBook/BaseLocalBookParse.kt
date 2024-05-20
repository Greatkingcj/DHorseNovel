package com.squirrel.base.model.localBook

import com.squirrel.base.data.entities.Book
import com.squirrel.base.data.entities.BookChapter
import java.io.InputStream

/**
 *companion object interface
 *see EpubFile.kt
 */
interface BaseLocalBookParse {

    fun upBookInfo(book: Book)

    fun getChapterList(book: Book): ArrayList<BookChapter>

    fun getContent(book: Book, chapter: BookChapter): String?

    fun getImage(book: Book, href: String): InputStream?

}
