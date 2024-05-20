package com.squirrel.base.help

import com.squirrel.base.help.coroutine.Coroutine
import kotlinx.coroutines.CoroutineScope

object AppUpdate {

    val gitHubUpdate by lazy {
        kotlin.runCatching {
            Class.forName("com.squirrel.base.help.AppUpdateGitHub")
                .kotlin.objectInstance as AppUpdateInterface
        }.getOrNull()
    }

    data class UpdateInfo(
        val tagName: String,
        val updateLog: String,
        val downloadUrl: String,
        val fileName: String
    )

    interface AppUpdateInterface {

        fun check(scope: CoroutineScope): Coroutine<UpdateInfo>

    }

}