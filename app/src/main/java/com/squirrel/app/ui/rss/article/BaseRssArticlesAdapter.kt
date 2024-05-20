package com.squirrel.app.ui.rss.article

import android.content.Context
import androidx.viewbinding.ViewBinding
import com.squirrel.base.base.adapter.RecyclerAdapter
import com.squirrel.base.data.entities.RssArticle


abstract class BaseRssArticlesAdapter<VB : ViewBinding>(context: Context, val callBack: com.squirrel.app.ui.rss.article.BaseRssArticlesAdapter.CallBack) :
    RecyclerAdapter<RssArticle, VB>(context) {

    interface CallBack {
        val isGridLayout: Boolean
        fun readRss(rssArticle: RssArticle)
    }
}