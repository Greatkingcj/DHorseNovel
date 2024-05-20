package com.squirrel.app.ui.main.bookshelf.style1.books

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.squirrel.base.base.adapter.ItemViewHolder
import com.squirrel.base.data.entities.Book
import com.squirrel.app.databinding.ItemBookshelfGridBinding
import com.squirrel.base.help.book.isLocal
import com.squirrel.base.help.config.AppConfig
import com.squirrel.base.utils.invisible
import splitties.views.onLongClick

class BooksAdapterGrid(context: Context, private val callBack: CallBack) :
    BaseBooksAdapter<ItemBookshelfGridBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBookshelfGridBinding {
        return ItemBookshelfGridBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookshelfGridBinding,
        item: Book,
        payloads: MutableList<Any>
    ) = binding.run {
        val bundle = payloads.getOrNull(0) as? Bundle
        if (bundle == null) {
            tvName.text = item.name
            ivCover.load(item.getDisplayCover(), item.name, item.author, false, item.origin)
            upRefresh(binding, item)
        } else {
            bundle.keySet().forEach {
                when (it) {
                    "name" -> tvName.text = item.name
                    "cover" -> ivCover.load(item.getDisplayCover(), item.name, item.author, false, item.origin)
                    "refresh" -> upRefresh(binding, item)
                }
            }
        }
    }

    private fun upRefresh(binding: ItemBookshelfGridBinding, item: Book) {
        if (!item.isLocal && callBack.isUpdate(item.bookUrl)) {
            binding.bvUnread.invisible()
            binding.rlLoading.visible()
        } else {
            binding.rlLoading.inVisible()
            if (AppConfig.showUnread) {
                binding.bvUnread.setBadgeCount(item.getUnreadChapterNum())
                binding.bvUnread.setHighlight(item.lastCheckCount > 0)
            } else {
                binding.bvUnread.invisible()
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookshelfGridBinding) {
        holder.itemView.apply {
            setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.open(it)
                }
            }

            onLongClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.openBookInfo(it)
                }
            }
        }
    }
}