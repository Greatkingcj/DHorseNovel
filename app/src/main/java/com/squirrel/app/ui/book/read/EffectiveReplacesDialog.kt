package com.squirrel.app.ui.book.read

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.squirrel.app.R
import com.squirrel.base.base.BaseDialogFragment
import com.squirrel.base.base.adapter.ItemViewHolder
import com.squirrel.base.base.adapter.RecyclerAdapter
import com.squirrel.base.data.entities.ReplaceRule
import com.squirrel.app.databinding.DialogRecyclerViewBinding
import com.squirrel.app.databinding.Item1lineTextBinding
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.base.model.ReadBook
import com.squirrel.app.ui.replace.edit.ReplaceEditActivity
import com.squirrel.base.utils.setLayout
import com.squirrel.base.utils.viewbindingdelegate.viewBinding

/**
 * 起效的替换规则
 */
class EffectiveReplacesDialog : BaseDialogFragment(R.layout.dialog_recycler_view) {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by activityViewModels<ReadBookViewModel>()
    private val adapter by lazy { ReplaceAdapter(requireContext()) }

    private var isEdit = false

    private val editActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                isEdit = true
            }
        }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.run {
            toolBar.setBackgroundColor(primaryColor)
            toolBar.setTitle(R.string.effective_replaces)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
        }
        ReadBook.curTextChapter?.effectiveReplaceRules?.let {
            adapter.setItems(it)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (isEdit) {
            viewModel.replaceRuleChanged()
        }
    }

    private inner class ReplaceAdapter(context: Context) :
        RecyclerAdapter<ReplaceRule, Item1lineTextBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): Item1lineTextBinding {
            return Item1lineTextBinding.inflate(inflater, parent, false)
        }

        override fun registerListener(holder: ItemViewHolder, binding: Item1lineTextBinding) {
            binding.root.setOnClickListener {
                getItem(holder.layoutPosition)?.let { item ->
                    editActivity.launch(ReplaceEditActivity.startIntent(requireContext(), item.id))
                }
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: Item1lineTextBinding,
            item: ReplaceRule,
            payloads: MutableList<Any>
        ) {
            binding.textView.text = item.name
        }

    }

}