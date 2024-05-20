package com.squirrel.base.model

import com.google.gson.reflect.TypeToken
import com.script.SimpleBindings
import com.script.rhino.RhinoScriptEngine
import com.squirrel.base.exception.NoStackTraceException
import com.squirrel.base.help.http.newCallStrResponse
import com.squirrel.base.help.http.okHttpClient
import com.squirrel.base.utils.ACache
import com.squirrel.base.utils.GSON
import com.squirrel.base.utils.MD5Utils
import com.squirrel.base.utils.isAbsUrl
import com.squirrel.base.utils.isJsonObject
import kotlinx.coroutines.runBlocking
import org.mozilla.javascript.Scriptable
import splitties.init.appCtx
import java.io.File
import java.lang.ref.WeakReference
import kotlin.collections.set

object SharedJsScope {

    private val cacheFolder = File(appCtx.filesDir, "shareJs")
    private val aCache = ACache.get(cacheFolder)

    private val scopeMap = hashMapOf<String, WeakReference<Scriptable>>()

    fun getScope(jsLib: String?): Scriptable? {
        if (jsLib.isNullOrBlank()) {
            return null
        }
        val key = MD5Utils.md5Encode(jsLib)
        var scope = scopeMap[key]?.get()
        if (scope == null) {
            scope = RhinoScriptEngine.run {
                getRuntimeScope(getScriptContext(SimpleBindings()))
            }
            if (jsLib.isJsonObject()) {
                val jsMap: Map<String, String> = GSON.fromJson(
                    jsLib,
                    TypeToken.getParameterized(
                        Map::class.java,
                        String::class.java,
                        String::class.java
                    ).type
                )
                jsMap.values.forEach { value ->
                    if (value.isAbsUrl()) {
                        val fileName = MD5Utils.md5Encode(value)
                        var js = aCache.getAsString(fileName)
                        if (js == null) {
                            js = runBlocking {
                                okHttpClient.newCallStrResponse {
                                    url(value)
                                }.body
                            }
                            if (js !== null) {
                                aCache.put(fileName, js)
                            } else {
                                throw NoStackTraceException("下载jsLib-${value}失败")
                            }
                        }
                        RhinoScriptEngine.eval(js, scope)
                    }
                }
            } else {
                RhinoScriptEngine.eval(jsLib, scope)
            }
            scopeMap[key] = WeakReference(scope)
        }
        return scope
    }

}