package com.squirrel.app.ui.book.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.squirrel.app.R
import com.squirrel.base.base.VMBaseFragment
import com.squirrel.base.constant.AppLog
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.Bookmark
import com.squirrel.app.databinding.FragmentBookmarkBinding
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.app.ui.book.bookmark.BookmarkDialog
import com.squirrel.base.ui.widget.recycler.UpLinearLayoutManager
import com.squirrel.base.ui.widget.recycler.VerticalDivider
import com.squirrel.base.utils.setEdgeEffectColor
import com.squirrel.base.utils.showDialogFragment
import com.squirrel.base.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BookmarkFragment : VMBaseFragment<TocViewModel>(R.layout.fragment_bookmark),
    BookmarkAdapter.Callback,
    TocViewModel.BookmarkCallBack {
    override val viewModel by activityViewModels<TocViewModel>()
    private val binding by viewBinding(FragmentBookmarkBinding::bind)
    private val mLayoutManager by lazy { UpLinearLayoutManager(requireContext()) }
    private val adapter by lazy { BookmarkAdapter(requireContext(), this) }
    private var durChapterIndex = 0

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.bookMarkCallBack = this
        initRecyclerView()
        viewModel.bookData.observe(this) {
            durChapterIndex = it.durChapterIndex
            upBookmark(null)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
    }

    override fun upBookmark(searchKey: String?) {
        val book = viewModel.bookData.value ?: return
        lifecycleScope.launch {
            when {
                searchKey.isNullOrBlank() -> appDb.bookmarkDao.flowByBook(book.name, book.author)
                else -> appDb.bookmarkDao.flowSearch(book.name, book.author, searchKey)
            }.catch {
                AppLog.put("目录界面获取书签数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
                var scrollPos = 0
                withContext(Dispatchers.Default) {
                    adapter.getItems().forEachIndexed { index, bookmark ->
                        if (bookmark.chapterIndex >= durChapterIndex) {
                            return@withContext
                        }
                        scrollPos = index
                    }
                }
                mLayoutManager.scrollToPositionWithOffset(scrollPos, 0)
            }
        }
    }


    override fun onClick(bookmark: Bookmark) {
        activity?.run {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("index", bookmark.chapterIndex)
                putExtra("chapterPos", bookmark.chapterPos)
            })
            finish()
        }
    }

    override fun onLongClick(bookmark: Bookmark, pos: Int) {
        showDialogFragment(BookmarkDialog(bookmark, pos))
    }

}