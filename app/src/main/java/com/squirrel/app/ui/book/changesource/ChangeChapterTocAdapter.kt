package com.squirrel.app.ui.book.changesource

import android.content.Context
import android.view.ViewGroup
import com.squirrel.app.R
import com.squirrel.base.base.adapter.ItemViewHolder
import com.squirrel.base.base.adapter.RecyclerAdapter
import com.squirrel.base.data.entities.BookChapter
import com.squirrel.app.databinding.ItemChapterListBinding
import com.squirrel.base.lib.theme.ThemeUtils
import com.squirrel.base.lib.theme.accentColor
import com.squirrel.base.utils.getCompatColor
import com.squirrel.base.utils.gone
import com.squirrel.base.utils.visible

class ChangeChapterTocAdapter(context: Context, val callback: Callback) :
    RecyclerAdapter<BookChapter, ItemChapterListBinding>(context) {

    var durChapterIndex = 0

    override fun getViewBinding(parent: ViewGroup): ItemChapterListBinding {
        return ItemChapterListBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemChapterListBinding,
        item: BookChapter,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val isDur = durChapterIndex == item.index
            if (isDur) {
                tvChapterName.setTextColor(context.accentColor)
            } else {
                tvChapterName.setTextColor(context.getCompatColor(R.color.primaryText))
            }
            tvChapterName.text = item.title
            if (item.isVolume) {
                //卷名，如第一卷 突出显示
                tvChapterItem.setBackgroundColor(context.getCompatColor(R.color.btn_bg_press))
            } else {
                //普通章节 保持不变
                tvChapterItem.background =
                    ThemeUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
            }
            if (!item.tag.isNullOrEmpty() && !item.isVolume) {
                //卷名不显示tag(更新时间规则)
                tvTag.text = item.tag
                tvTag.visible()
            } else {
                tvTag.gone()
            }
            ivChecked.setImageResource(R.drawable.ic_check)
            ivChecked.visible(isDur)
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemChapterListBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callback.clickChapter(it, getItem(holder.layoutPosition + 1)?.url)
            }
        }
    }

    interface Callback {
        fun clickChapter(bookChapter: BookChapter, nextChapterUrl: String?)
    }
}