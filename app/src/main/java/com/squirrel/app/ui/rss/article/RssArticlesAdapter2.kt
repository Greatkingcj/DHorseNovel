package com.squirrel.app.ui.rss.article

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.squirrel.app.R
import com.squirrel.base.base.adapter.ItemViewHolder
import com.squirrel.base.data.entities.RssArticle
import com.squirrel.app.databinding.ItemRssArticle2Binding
import com.squirrel.base.help.glide.ImageLoader
import com.squirrel.base.help.glide.OkHttpModelLoader
import com.squirrel.base.utils.getCompatColor
import com.squirrel.base.utils.gone
import com.squirrel.base.utils.visible


class RssArticlesAdapter2(context: Context, callBack: CallBack) :
    com.squirrel.app.ui.rss.article.BaseRssArticlesAdapter<ItemRssArticle2Binding>(context, callBack) {

    override fun getViewBinding(parent: ViewGroup): ItemRssArticle2Binding {
        return ItemRssArticle2Binding.inflate(inflater, parent, false)
    }

    @SuppressLint("CheckResult")
    override fun convert(
        holder: ItemViewHolder,
        binding: ItemRssArticle2Binding,
        item: RssArticle,
        payloads: MutableList<Any>
    ) {
        binding.run {
            tvTitle.text = item.title
            tvPubDate.text = item.pubDate
            if (item.image.isNullOrBlank() && !callBack.isGridLayout) {
                imageView.gone()
            } else {
                val options =
                    RequestOptions().set(OkHttpModelLoader.sourceOriginOption, item.origin)
                ImageLoader.load(context, item.image).apply(options).apply {
                    if (callBack.isGridLayout) {
                        placeholder(R.drawable.image_rss_article)
                    } else {
                        addListener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                imageView.gone()
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                imageView.visible()
                                return false
                            }

                        })
                    }
                }.into(imageView)
            }
            if (item.read) {
                tvTitle.setTextColor(context.getCompatColor(R.color.tv_text_summary))
            } else {
                tvTitle.setTextColor(context.getCompatColor(R.color.primaryText))
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemRssArticle2Binding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.readRss(it)
            }
        }
    }

}