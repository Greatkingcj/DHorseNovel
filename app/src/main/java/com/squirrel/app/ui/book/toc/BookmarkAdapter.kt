package com.squirrel.app.ui.book.toc

import android.content.Context
import android.view.ViewGroup
import com.squirrel.base.base.adapter.ItemViewHolder
import com.squirrel.base.base.adapter.RecyclerAdapter
import com.squirrel.base.data.entities.Bookmark
import com.squirrel.app.databinding.ItemBookmarkBinding
import com.squirrel.base.utils.gone
import splitties.views.onLongClick

class BookmarkAdapter(context: Context, val callback: Callback) :
    RecyclerAdapter<Bookmark, ItemBookmarkBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBookmarkBinding {
        return ItemBookmarkBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookmarkBinding,
        item: Bookmark,
        payloads: MutableList<Any>
    ) {
        binding.tvChapterName.text = item.chapterName
        binding.tvBookText.gone(item.bookText.isEmpty())
        binding.tvBookText.text = item.bookText
        binding.tvContent.gone(item.content.isEmpty())
        binding.tvContent.text = item.content
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookmarkBinding) {
        binding.root.setOnClickListener {
            getItem(holder.layoutPosition)?.let { bookmark ->
                callback.onClick(bookmark)
            }
        }
        binding.root.onLongClick {
            getItem(holder.layoutPosition)?.let { bookmark ->
                callback.onLongClick(bookmark, holder.layoutPosition)
            }
        }

    }

    interface Callback {
        fun onClick(bookmark: Bookmark)
        fun onLongClick(bookmark: Bookmark, pos: Int)
    }

}