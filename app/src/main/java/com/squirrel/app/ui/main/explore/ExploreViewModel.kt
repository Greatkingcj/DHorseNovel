package com.squirrel.app.ui.main.explore

import android.app.Application
import com.squirrel.base.base.BaseViewModel
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.BookSourcePart
import com.squirrel.base.help.config.SourceConfig

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSourcePart) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.upOrder(bookSource)
        }
    }

    fun deleteSource(source: BookSourcePart) {
        execute {
            appDb.bookSourceDao.delete(source.bookSourceUrl)
            SourceConfig.removeSource(source.bookSourceUrl)
        }
    }

}