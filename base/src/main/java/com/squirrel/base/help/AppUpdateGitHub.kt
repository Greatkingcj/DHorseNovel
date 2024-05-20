package com.squirrel.base.help

import androidx.annotation.Keep
import com.squirrel.base.constant.AppConst
import com.squirrel.base.exception.NoStackTraceException
import com.squirrel.base.help.coroutine.Coroutine
import com.squirrel.base.help.http.newCallStrResponse
import com.squirrel.base.help.http.okHttpClient
import com.squirrel.base.utils.channel
import com.squirrel.base.utils.jsonPath
import com.squirrel.base.utils.readString
import kotlinx.coroutines.CoroutineScope
import splitties.init.appCtx

@Keep
@Suppress("unused")
object AppUpdateGitHub : AppUpdate.AppUpdateInterface {

    override fun check(
        scope: CoroutineScope,
    ): Coroutine<AppUpdate.UpdateInfo> {
        return Coroutine.async(scope) {
            val lastReleaseUrl = "https://api.github.com/repos/gedoor/legado/releases/latest"
            val body = okHttpClient.newCallStrResponse {
                url(lastReleaseUrl)
            }.body
            if (body.isNullOrBlank()) {
                throw NoStackTraceException("获取新版本出错")
            }
            val rootDoc = jsonPath.parse(body)
            val tagName = rootDoc.readString("$.tag_name")
                ?: throw NoStackTraceException("获取新版本出错")
            if (tagName > AppConst.appInfo.versionName) {
                val updateBody = rootDoc.readString("$.body")
                    ?: throw NoStackTraceException("获取新版本出错")
                val path = "\$.assets[?(@.name =~ /legado_${appCtx.channel}_.*?apk\$/)]"
                val downloadUrl = rootDoc.read<List<String>>("${path}.browser_download_url")
                    .firstOrNull()
                    ?: throw NoStackTraceException("获取新版本出错")
                val fileName = rootDoc.read<List<String>>("${path}.name")
                    .firstOrNull()
                    ?: throw NoStackTraceException("获取新版本出错")
                return@async AppUpdate.UpdateInfo(tagName, updateBody, downloadUrl, fileName)
            } else {
                throw NoStackTraceException("已是最新版本")
            }
        }.timeout(10000)
    }


}