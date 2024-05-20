package com.squirrel.app.ui.book.info.edit

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.squirrel.base.base.BaseViewModel
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.Book
import com.squirrel.base.model.ReadBook

class BookInfoEditViewModel(application: Application) : BaseViewModel(application) {
    var book: Book? = null
    val bookData = MutableLiveData<Book>()

    fun loadBook(bookUrl: String) {
        execute {
            book = appDb.bookDao.getBook(bookUrl)
            book?.let {
                bookData.postValue(it)
            }
        }
    }

    fun saveBook(book: Book, success: (() -> Unit)?) {
        execute {
            if (ReadBook.book?.bookUrl == book.bookUrl) {
                ReadBook.book = book
            }
            appDb.bookDao.update(book)
        }.onSuccess {
            success?.invoke()
        }
    }
}