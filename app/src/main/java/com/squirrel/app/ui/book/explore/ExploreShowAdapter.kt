package com.squirrel.app.ui.book.explore

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.squirrel.app.R
import com.squirrel.base.base.adapter.ItemViewHolder
import com.squirrel.base.base.adapter.RecyclerAdapter
import com.squirrel.base.data.entities.Book
import com.squirrel.base.data.entities.SearchBook
import com.squirrel.app.databinding.ItemSearchBinding
import com.squirrel.base.help.config.AppConfig
import com.squirrel.base.utils.gone
import com.squirrel.base.utils.visible


class ExploreShowAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<SearchBook, ItemSearchBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemSearchBinding {
        return ItemSearchBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemSearchBinding,
        item: SearchBook,
        payloads: MutableList<Any>
    ) {
        val bundle = payloads.getOrNull(0) as? Bundle
        if (bundle == null) {
            bind(binding, item)
        } else {
            bindChange(binding, item, bundle)
        }

    }

    private fun bind(binding: ItemSearchBinding, item: SearchBook) {
        binding.run {
            tvName.text = item.name
            tvAuthor.text = context.getString(R.string.author_show, item.author)
            ivInBookshelf.isVisible = callBack.isInBookshelf(item.name, item.author)
            if (item.latestChapterTitle.isNullOrEmpty()) {
                tvLasted.gone()
            } else {
                tvLasted.text = context.getString(R.string.lasted_show, item.latestChapterTitle)
                tvLasted.visible()
            }
            tvIntroduce.text = item.trimIntro(context)
            val kinds = item.getKindList()
            if (kinds.isEmpty()) {
                llKind.gone()
            } else {
                llKind.visible()
                llKind.setLabels(kinds)
            }
            ivCover.load(
                item.coverUrl,
                item.name,
                item.author,
                AppConfig.loadCoverOnlyWifi,
                item.origin
            )
        }
    }

    private fun bindChange(binding: ItemSearchBinding, item: SearchBook, bundle: Bundle) {
        binding.run {
            bundle.keySet().forEach {
                when (it) {
                    "isInBookshelf" -> ivInBookshelf.isVisible =
                        callBack.isInBookshelf(item.name, item.author)
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemSearchBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.showBookInfo(it.toBook())
            }
        }
    }

    interface CallBack {
        /**
         * 是否已经加入书架
         */
        fun isInBookshelf(name: String, author: String): Boolean

        fun showBookInfo(book: Book)
    }
}