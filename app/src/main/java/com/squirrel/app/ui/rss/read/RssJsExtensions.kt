package com.squirrel.app.ui.rss.read

import com.squirrel.base.data.entities.BaseSource
import com.squirrel.base.help.JsExtensions
import com.squirrel.app.ui.association.AddToBookshelfDialog
import com.squirrel.app.ui.book.search.SearchActivity
import com.squirrel.base.utils.showDialogFragment

@Suppress("unused")
class RssJsExtensions(private val activity: ReadRssActivity) : JsExtensions {

    override fun getSource(): BaseSource? {
        return activity.getSource()
    }

    fun searchBook(key: String) {
        SearchActivity.start(activity, key)
    }

    fun addBook(bookUrl: String) {
        activity.showDialogFragment(com.squirrel.app.ui.association.AddToBookshelfDialog(bookUrl))
    }

}
