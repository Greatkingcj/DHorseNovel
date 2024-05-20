package com.squirrel.app.ui.main.bookshelf

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.stream.JsonWriter
import com.squirrel.app.R
import com.squirrel.base.base.BaseViewModel
import com.squirrel.base.constant.AppLog
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.Book
import com.squirrel.base.data.entities.BookSource
import com.squirrel.base.exception.NoStackTraceException
import com.squirrel.base.help.coroutine.Coroutine
import com.squirrel.base.help.http.newCallResponseBody
import com.squirrel.base.help.http.okHttpClient
import com.squirrel.base.help.http.text
import com.squirrel.base.model.webBook.WebBook
import com.squirrel.base.utils.FileUtils
import com.squirrel.base.utils.GSON
import com.squirrel.base.utils.NetworkUtils
import com.squirrel.base.utils.fromJsonArray
import com.squirrel.base.utils.isAbsUrl
import com.squirrel.base.utils.isJsonArray
import com.squirrel.base.utils.printOnDebug
import com.squirrel.base.utils.toastOnUi
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.forEach
import kotlin.collections.hashMapOf
import kotlin.collections.set

class BookshelfViewModel(application: Application) : BaseViewModel(application) {
    val addBookProgressLiveData = MutableLiveData(-1)
    var addBookJob: Coroutine<*>? = null

    fun addBookByUrl(bookUrls: String) {
        var successCount = 0
        addBookJob = execute {
            val hasBookUrlPattern: List<BookSource> by lazy {
                appDb.bookSourceDao.hasBookUrlPattern
            }
            val urls = bookUrls.split("\n")
            for (url in urls) {
                val bookUrl = url.trim()
                if (bookUrl.isEmpty()) continue
                if (appDb.bookDao.getBook(bookUrl) != null) {
                    successCount++
                    continue
                }
                val baseUrl = NetworkUtils.getBaseUrl(bookUrl) ?: continue
                var source = appDb.bookSourceDao.getBookSourceAddBook(baseUrl)
                if (source == null) {
                    for (bookSource in hasBookUrlPattern) {
                        try {
                            if (bookUrl.matches(bookSource.bookUrlPattern!!.toRegex())) {
                                source = bookSource
                                break
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
                val bookSource = source ?: continue
                val book = Book(
                    bookUrl = bookUrl,
                    origin = bookSource.bookSourceUrl,
                    originName = bookSource.bookSourceName
                )
                kotlin.runCatching {
                    WebBook.getBookInfoAwait(bookSource, book)
                }.onSuccess {
                    it.order = appDb.bookDao.minOrder - 1
                    it.save()
                    successCount++
                    addBookProgressLiveData.postValue(successCount)
                }
            }
        }.onSuccess {
            if (successCount > 0) {
                context.toastOnUi(R.string.success)
            } else {
                context.toastOnUi("添加网址失败")
            }
        }.onError {
            AppLog.put("添加网址出错\n${it.localizedMessage}", it, true)
        }.onFinally {
            addBookProgressLiveData.postValue(-1)
        }
    }

    fun exportBookshelf(books: List<Book>?, success: (file: File) -> Unit) {
        execute {
            books?.let {
                val path = "${context.filesDir}/books.json"
                FileUtils.delete(path)
                val file = FileUtils.createFileWithReplace(path)
                FileOutputStream(file).use { out ->
                    val writer = JsonWriter(OutputStreamWriter(out, "UTF-8"))
                    writer.setIndent("  ")
                    writer.beginArray()
                    books.forEach {
                        val bookMap = hashMapOf<String, String?>()
                        bookMap["name"] = it.name
                        bookMap["author"] = it.author
                        bookMap["intro"] = it.getDisplayIntro()
                        GSON.toJson(bookMap, bookMap::class.java, writer)
                    }
                    writer.endArray()
                    writer.close()
                }
                file
            } ?: throw NoStackTraceException("书籍不能为空")
        }.onSuccess {
            success(it)
        }.onError {
            context.toastOnUi("导出书籍出错\n${it.localizedMessage}")
        }
    }

    fun importBookshelf(str: String, groupId: Long) {
        execute {
            val text = str.trim()
            when {
                text.isAbsUrl() -> {
                    okHttpClient.newCallResponseBody {
                        url(text)
                    }.text().let {
                        importBookshelf(it, groupId)
                    }
                }

                text.isJsonArray() -> {
                    importBookshelfByJson(text, groupId)
                }

                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "ERROR")
        }
    }

    private fun importBookshelfByJson(json: String, groupId: Long) {
        execute {
            val bookSources = appDb.bookSourceDao.allEnabled
            GSON.fromJsonArray<Map<String, String?>>(json).getOrThrow().forEach { bookInfo ->
                if (!isActive) return@execute
                val name = bookInfo["name"] ?: ""
                val author = bookInfo["author"] ?: ""
                if (name.isNotEmpty() && appDb.bookDao.getBook(name, author) == null) {
                    WebBook.preciseSearch(this, bookSources, name, author)
                        .onSuccess {
                            val book = it.first
                            if (groupId > 0) {
                                book.group = groupId
                            }
                            book.save()
                        }.onError { e ->
                            context.toastOnUi(e.localizedMessage)
                        }
                }
            }
        }.onError {
            it.printOnDebug()
        }.onFinally {
            context.toastOnUi(R.string.success)
        }
    }

}
