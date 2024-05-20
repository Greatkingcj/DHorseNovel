package com.squirrel.app.ui.book.read.config

import android.app.Application
import android.os.Bundle
import com.squirrel.base.base.BaseViewModel
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.HttpTTS
import com.squirrel.base.exception.NoStackTraceException
import com.squirrel.base.model.ReadAloud
import com.squirrel.base.utils.getClipText
import com.squirrel.base.utils.isJsonArray
import com.squirrel.base.utils.isJsonObject
import com.squirrel.base.utils.toastOnUi

class HttpTtsEditViewModel(app: Application) : BaseViewModel(app) {

    var id: Long? = null

    fun initData(arguments: Bundle?, success: (httpTTS: HttpTTS) -> Unit) {
        execute {
            if (id == null) {
                val argumentId = arguments?.getLong("id")
                if (argumentId != null && argumentId != 0L) {
                    id = argumentId
                    return@execute appDb.httpTTSDao.get(argumentId)
                }
            }
            return@execute null
        }.onSuccess {
            it?.let {
                success.invoke(it)
            }
        }
    }

    fun save(httpTTS: HttpTTS, success: (() -> Unit)? = null) {
        id = httpTTS.id
        execute {
            appDb.httpTTSDao.insert(httpTTS)
            if (ReadAloud.ttsEngine == httpTTS.id.toString()) ReadAloud.upReadAloudClass()
        }.onSuccess {
            success?.invoke()
        }
    }

    fun importFromClip(onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text = context.getClipText()
        if (text.isNullOrBlank()) {
            context.toastOnUi("剪贴板为空")
        } else {
            importSource(text, onSuccess)
        }
    }

    fun importSource(text: String, onSuccess: (httpTTS: HttpTTS) -> Unit) {
        val text1 = text.trim()
        execute {
            when {
                text1.isJsonObject() -> {
                    HttpTTS.fromJson(text1).getOrThrow()
                }
                text1.isJsonArray() -> {
                    HttpTTS.fromJsonArray(text1).getOrThrow().first()
                }
                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onSuccess {
            onSuccess.invoke(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

}