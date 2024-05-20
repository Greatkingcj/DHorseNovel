package com.squirrel.app.ui.main.rss

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.request.RequestOptions
import com.squirrel.app.R
import com.squirrel.base.base.adapter.ItemViewHolder
import com.squirrel.base.base.adapter.RecyclerAdapter
import com.squirrel.base.data.entities.RssSource
import com.squirrel.app.databinding.ItemRssBinding
import com.squirrel.base.help.glide.ImageLoader
import com.squirrel.base.help.glide.OkHttpModelLoader
import splitties.views.onLongClick

class RssAdapter(context: Context, val callBack: CallBack, val lifecycle: Lifecycle) :
    RecyclerAdapter<RssSource, ItemRssBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemRssBinding {
        return ItemRssBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemRssBinding,
        item: RssSource,
        payloads: MutableList<Any>
    ) {
        binding.apply {
            tvName.text = item.sourceName
            val options = RequestOptions()
                .set(OkHttpModelLoader.sourceOriginOption, item.sourceUrl)
            ImageLoader.load(lifecycle, item.sourceIcon)
                .apply(options)
                .centerCrop()
                .placeholder(R.drawable.image_rss)
                .error(R.drawable.image_rss)
                .into(ivIcon)
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemRssBinding) {
        binding.apply {
            root.setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let {
                    callBack.openRss(it)
                }
            }
            root.onLongClick {
                getItemByLayoutPosition(holder.layoutPosition)?.let {
                    showMenu(ivIcon, it)
                }
            }
        }
    }

    private fun showMenu(view: View, rssSource: RssSource) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.rss_main_item)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_top -> callBack.toTop(rssSource)
                R.id.menu_edit -> callBack.edit(rssSource)
                R.id.menu_del -> callBack.del(rssSource)
                R.id.menu_disable -> callBack.disable(rssSource)
            }
            true
        }
        popupMenu.show()
    }

    interface CallBack {
        fun openRss(rssSource: RssSource)
        fun toTop(rssSource: RssSource)
        fun edit(rssSource: RssSource)
        fun del(rssSource: RssSource)
        fun disable(rssSource: RssSource)
    }
}