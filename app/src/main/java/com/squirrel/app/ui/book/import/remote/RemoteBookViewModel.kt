package com.squirrel.app.ui.book.import.remote

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.squirrel.base.base.BaseViewModel
import com.squirrel.base.constant.AppLog
import com.squirrel.base.constant.BookType
import com.squirrel.base.data.appDb
import com.squirrel.base.exception.NoStackTraceException
import com.squirrel.base.help.AppWebDav
import com.squirrel.base.help.config.AppConfig
import com.squirrel.base.lib.webdav.Authorization
import com.squirrel.base.model.analyzeRule.CustomUrl
import com.squirrel.base.model.localBook.LocalBook
import com.squirrel.base.model.remote.RemoteBook
import com.squirrel.base.model.remote.RemoteBookWebDav
import com.squirrel.base.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Collections

class RemoteBookViewModel(application: Application) : BaseViewModel(application) {
    var sortKey = RemoteBookSort.Default
    var sortAscending = false
    val dirList = arrayListOf<RemoteBook>()
    val permissionDenialLiveData = MutableLiveData<Int>()

    var dataCallback: DataCallback? = null

    val dataFlow = callbackFlow<List<RemoteBook>> {

        val list = Collections.synchronizedList(ArrayList<RemoteBook>())

        dataCallback = object : DataCallback {

            override fun setItems(remoteFiles: List<RemoteBook>) {
                list.clear()
                list.addAll(remoteFiles)
                trySend(list)
            }

            override fun addItems(remoteFiles: List<RemoteBook>) {
                list.addAll(remoteFiles)
                trySend(list)
            }

            override fun clear() {
                list.clear()
                trySend(emptyList())
            }

            override fun screen(key: String?) {
                if (key.isNullOrBlank()) {
                    trySend(list)
                } else {
                    trySend(
                        list.filter { it.filename.contains(key) }
                    )
                }
            }
        }

        awaitClose {
            dataCallback = null
        }
    }.map { list ->
        if (sortAscending) when (sortKey) {
            RemoteBookSort.Name -> list.sortedWith(compareBy({ !it.isDir }, { it.filename }))
            else -> list.sortedWith(compareBy({ !it.isDir }, { it.lastModify }))
        } else when (sortKey) {
            RemoteBookSort.Name -> list.sortedWith { o1, o2 ->
                val compare = -compareValues(o1.isDir, o2.isDir)
                if (compare == 0) {
                    return@sortedWith -compareValues(o1.filename, o2.filename)
                }
                return@sortedWith compare
            }

            else -> list.sortedWith { o1, o2 ->
                val compare = -compareValues(o1.isDir, o2.isDir)
                if (compare == 0) {
                    return@sortedWith -compareValues(o1.lastModify, o2.lastModify)
                }
                return@sortedWith compare
            }
        }
    }.flowOn(Dispatchers.IO)

    private var remoteBookWebDav: RemoteBookWebDav? = null

    fun initData(onSuccess: () -> Unit) {
        execute {
            appDb.serverDao.get(AppConfig.remoteServerId)?.getWebDavConfig()?.let {
                val authorization = Authorization(it)
                remoteBookWebDav = RemoteBookWebDav(it.url, authorization, AppConfig.remoteServerId)
                return@execute
            }
            remoteBookWebDav = AppWebDav.defaultBookWebDav
                ?: throw NoStackTraceException("webDav没有配置")
        }.onError {
            context.toastOnUi("初始化webDav出错:${it.localizedMessage}")
        }.onSuccess {
            onSuccess.invoke()
        }
    }

    fun loadRemoteBookList(path: String?, loadCallback: (loading: Boolean) -> Unit) {
        execute {
            val bookWebDav = remoteBookWebDav
                ?: throw NoStackTraceException("没有配置webDav")
            dataCallback?.clear()
            val url = path ?: bookWebDav.rootBookUrl
            val bookList = bookWebDav.getRemoteBookList(url)
            dataCallback?.setItems(bookList)
        }.onError {
            AppLog.put("获取webDav书籍出错\n${it.localizedMessage}", it)
            context.toastOnUi("获取webDav书籍出错\n${it.localizedMessage}")
        }.onStart {
            loadCallback.invoke(true)
        }.onFinally {
            loadCallback.invoke(false)
        }
    }

    fun addToBookshelf(remoteBooks: HashSet<RemoteBook>, finally: () -> Unit) {
        execute {
            val bookWebDav = remoteBookWebDav
                ?: throw NoStackTraceException("没有配置webDav")
            remoteBooks.forEach { remoteBook ->
                val downloadBookUri = bookWebDav.downloadRemoteBook(remoteBook)
                LocalBook.importFiles(downloadBookUri).forEach { book ->
                    book.origin = BookType.webDavTag + CustomUrl(remoteBook.path)
                        .putAttribute("serverID", bookWebDav.serverID)
                        .toString()
                    book.save()
                }
                remoteBook.isOnBookShelf = true
            }
        }.onError {
            AppLog.put("导入出错\n${it.localizedMessage}", it)
            context.toastOnUi("导入出错\n${it.localizedMessage}")
            if (it is SecurityException) {
                permissionDenialLiveData.postValue(1)
            }
        }.onFinally {
            finally.invoke()
        }
    }

    fun updateCallBackFlow(filterKey: String?) {
        dataCallback?.screen(filterKey)
    }

    interface DataCallback {

        fun setItems(remoteFiles: List<RemoteBook>)

        fun addItems(remoteFiles: List<RemoteBook>)

        fun clear()

        fun screen(key: String?)

    }
}