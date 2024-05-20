package com.squirrel.app.ui.browser

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.URLUtil
import androidx.documentfile.provider.DocumentFile
import com.squirrel.base.base.BaseViewModel
import com.squirrel.base.constant.AppConst
import com.squirrel.base.data.appDb
import com.squirrel.base.exception.NoStackTraceException
import com.squirrel.base.help.CacheManager
import com.squirrel.base.help.IntentData
import com.squirrel.base.help.http.newCallResponseBody
import com.squirrel.base.help.http.okHttpClient
import com.squirrel.base.help.source.SourceVerificationHelp
import com.squirrel.base.model.analyzeRule.AnalyzeUrl
import com.squirrel.base.utils.DocumentUtils
import com.squirrel.base.utils.FileUtils
import com.squirrel.base.utils.isContentScheme
import com.squirrel.base.utils.printOnDebug
import com.squirrel.base.utils.toastOnUi
import com.squirrel.base.utils.writeBytes
import java.io.File
import java.util.Date

class WebViewModel(application: Application) : BaseViewModel(application) {
    var baseUrl: String = ""
    var html: String? = null
    val headerMap: HashMap<String, String> = hashMapOf()
    var sourceVerificationEnable: Boolean = false
    var sourceOrigin: String = ""
    var key = ""

    fun initData(
        intent: Intent,
        success: () -> Unit
    ) {
        execute {
            val url = intent.getStringExtra("url")
                ?: throw NoStackTraceException("url不能为空")
            sourceOrigin = intent.getStringExtra("sourceOrigin") ?: ""
            key = SourceVerificationHelp.getKey(sourceOrigin)
            sourceVerificationEnable = intent.getBooleanExtra("sourceVerificationEnable", false)
            val headerMapF = IntentData.get<Map<String, String>>(url)
            val analyzeUrl = AnalyzeUrl(url, headerMapF = headerMapF)
            baseUrl = analyzeUrl.url
            headerMap.putAll(analyzeUrl.headerMap)
            if (analyzeUrl.isPost()) {
                html = analyzeUrl.getStrResponseAwait(useWebView = false).body
            }
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("error\n${it.localizedMessage}")
            it.printOnDebug()
        }
    }

    fun saveImage(webPic: String?, path: String) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            webData2bitmap(webPic)?.let { biteArray ->
                if (path.isContentScheme()) {
                    val uri = Uri.parse(path)
                    DocumentFile.fromTreeUri(context, uri)?.let { doc ->
                        DocumentUtils.createFileIfNotExist(doc, fileName)
                            ?.writeBytes(context, biteArray)
                    }
                } else {
                    val file = FileUtils.createFileIfNotExist(File(path), fileName)
                    file.writeBytes(biteArray)
                }
            } ?: throw Throwable("NULL")
        }.onError {
            context.toastOnUi("保存图片失败:${it.localizedMessage}")
        }.onSuccess {
            context.toastOnUi("保存成功")
        }
    }

    private suspend fun webData2bitmap(data: String): ByteArray? {
        return if (URLUtil.isValidUrl(data)) {
            okHttpClient.newCallResponseBody {
                url(data)
            }.bytes()
        } else {
            Base64.decode(data.split(",").toTypedArray()[1], Base64.DEFAULT)
        }
    }

    fun saveVerificationResult(intent: Intent, success: () -> Unit) {
        execute {
            if (sourceVerificationEnable) {
                val url = intent.getStringExtra("url")!!
                val source = appDb.bookSourceDao.getBookSource(sourceOrigin)
                val key = "${sourceOrigin}_verificationResult"
                html = AnalyzeUrl(
                    url,
                    headerMapF = headerMap,
                    source = source
                ).getStrResponseAwait(useWebView = false).body
                CacheManager.putMemory(key, html ?: "")
            }
        }.onSuccess {
            success.invoke()
        }
    }

}