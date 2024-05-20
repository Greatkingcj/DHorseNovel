package com.squirrel.base.model

import android.content.Context
import com.squirrel.base.constant.IntentAction
import com.squirrel.base.service.DownloadService
import com.squirrel.base.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}