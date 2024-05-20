package com.squirrel.base.web.socket

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import com.squirrel.base.R
import com.squirrel.base.data.entities.SearchBook
import com.squirrel.base.help.config.AppConfig
import com.squirrel.base.model.webBook.SearchModel
import com.squirrel.base.ui.book.search.SearchScope
import com.squirrel.base.utils.GSON
import com.squirrel.base.utils.fromJsonObject
import com.squirrel.base.utils.isJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.init.appCtx
import java.io.IOException

class BookSearchWebSocket(handshakeRequest: NanoHTTPD.IHTTPSession) :
    NanoWSD.WebSocket(handshakeRequest),
    CoroutineScope by MainScope(),
    SearchModel.CallBack {

    private val normalClosure = NanoWSD.WebSocketFrame.CloseCode.NormalClosure
    private val searchModel = SearchModel(this, this)

    private val SEARCH_FINISH = "Search finish"

    override fun onOpen() {
        launch(IO) {
            kotlin.runCatching {
                while (isOpen) {
                    ping("ping".toByteArray())
                    delay(30000)
                }
            }
        }
    }

    override fun onClose(
        code: NanoWSD.WebSocketFrame.CloseCode,
        reason: String,
        initiatedByRemote: Boolean
    ) {
        cancel()
        searchModel.close()
    }

    override fun onMessage(message: NanoWSD.WebSocketFrame) {
        launch(IO) {
            kotlin.runCatching {
                if (!message.textPayload.isJson()) {
                    send("数据必须为Json格式")
                    close(normalClosure, SEARCH_FINISH, false)
                    return@launch
                }
                val searchMap =
                    GSON.fromJsonObject<Map<String, String>>(message.textPayload).getOrNull()
                if (searchMap != null) {
                    val key = searchMap["key"]
                    if (key.isNullOrBlank()) {
                        send(appCtx.getString(R.string.cannot_empty))
                        close(normalClosure, SEARCH_FINISH, false)
                        return@launch
                    }
                    searchModel.search(System.currentTimeMillis(), key)
                }
            }
        }
    }

    override fun onPong(pong: NanoWSD.WebSocketFrame) {

    }

    override fun onException(exception: IOException) {

    }

    override fun getSearchScope(): SearchScope = SearchScope(AppConfig.searchScope)

    override fun onSearchStart() {

    }

    override fun onSearchSuccess(searchBooks: List<SearchBook>) {
        send(GSON.toJson(searchBooks))
    }

    override fun onSearchFinish(isEmpty: Boolean) = close(normalClosure, SEARCH_FINISH, false)

    override fun onSearchCancel(exception: Throwable?) = close(normalClosure, exception?.toString() ?: SEARCH_FINISH, false)

}
