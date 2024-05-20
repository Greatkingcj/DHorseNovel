package com.squirrel.app.ui.config

import android.app.Application
import android.content.Context
import com.squirrel.app.R
import com.squirrel.base.base.BaseViewModel
import com.squirrel.base.data.appDb
import com.squirrel.base.help.AppWebDav
import com.squirrel.base.help.book.BookHelp
import com.squirrel.base.utils.FileUtils
import com.squirrel.base.utils.restart
import com.squirrel.base.utils.toastOnUi
import kotlinx.coroutines.delay
import splitties.init.appCtx

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }

    fun clearWebViewData() {
        execute {
            FileUtils.delete(context.getDir("webview", Context.MODE_PRIVATE))
            context.toastOnUi(R.string.clear_webview_data_success)
            delay(3000)
            appCtx.restart()
        }
    }

    fun shrinkDatabase() {
        execute {
            appDb.openHelper.writableDatabase.execSQL("VACUUM")
        }.onSuccess {
            context.toastOnUi(R.string.success)
        }
    }

}
