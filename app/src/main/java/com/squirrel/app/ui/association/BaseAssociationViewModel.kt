package com.squirrel.app.ui.association

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.squirrel.base.base.BaseViewModel
import com.squirrel.base.utils.contains
import com.squirrel.base.utils.inputStream
import com.squirrel.base.utils.readText

abstract class BaseAssociationViewModel(application: Application) : BaseViewModel(application) {

    val successLive = MutableLiveData<Pair<String, String>>()
    val errorLive = MutableLiveData<String>()

    fun importJson(uri: Uri) {
        when {
            uri.inputStream(context).getOrNull().contains("bookSourceUrl") ->
                successLive.postValue(Pair("bookSource", uri.toString()))
            else -> importJson(uri.readText(context))
        }
    }

    private fun importJson(json: String) {
        //暂时根据文件内容判断属于什么
        when {
            json.contains("sourceUrl") ->
                successLive.postValue(Pair("rssSource", json))
            json.contains("pattern") ->
                successLive.postValue(Pair("replaceRule", json))
            json.contains("themeName") ->
                successLive.postValue(Pair("theme", json))
            json.contains("urlRule") && json.contains("showRule") ->
                successLive.postValue(Pair("dictRule", json))
            json.contains("name") && json.contains("rule") ->
                successLive.postValue(Pair("txtRule", json))
            json.contains("name") && json.contains("url") ->
                successLive.postValue(Pair("httpTts", json))
            else -> errorLive.postValue("格式不对")
        }
    }

}