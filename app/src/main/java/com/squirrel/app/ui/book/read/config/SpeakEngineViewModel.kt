package com.squirrel.app.ui.book.read.config

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import com.squirrel.base.base.BaseViewModel
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.HttpTTS
import com.squirrel.base.exception.NoStackTraceException
import com.squirrel.base.help.DefaultData
import com.squirrel.base.help.http.newCallResponseBody
import com.squirrel.base.help.http.okHttpClient
import com.squirrel.base.help.http.text
import com.squirrel.base.utils.isJsonArray
import com.squirrel.base.utils.isJsonObject
import com.squirrel.base.utils.readText
import com.squirrel.base.utils.toastOnUi

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    val sysEngines: List<TextToSpeech.EngineInfo> by lazy {
        val tts = TextToSpeech(context, null)
        val engines = tts.engines
        tts.shutdown()
        engines
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultHttpTTS()
        }
    }

    fun importOnLine(url: String) {
        execute {
            okHttpClient.newCallResponseBody {
                url(url)
            }.text("utf-8").let { json ->
                import(json)
            }
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败")
        }
    }

    fun importLocal(uri: Uri) {
        execute {
            import(uri.readText(context))
        }.onSuccess {
            context.toastOnUi("导入成功")
        }.onError {
            context.toastOnUi("导入失败\n${it.localizedMessage}")
        }
    }

    fun import(text: String) {
        when {
            text.isJsonArray() -> {
                HttpTTS.fromJsonArray(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(*it.toTypedArray())
                }
            }
            text.isJsonObject() -> {
                HttpTTS.fromJson(text).getOrThrow().let {
                    appDb.httpTTSDao.insert(it)
                }
            }
            else -> {
                throw NoStackTraceException("格式不对")
            }
        }
    }

}