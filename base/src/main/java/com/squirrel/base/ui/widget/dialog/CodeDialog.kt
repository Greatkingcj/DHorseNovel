package com.squirrel.base.ui.widget.dialog

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.squirrel.base.R
import com.squirrel.base.base.BaseDialogFragment
import com.squirrel.base.databinding.DialogCodeViewBinding
import com.squirrel.base.help.IntentData
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.base.ui.widget.code.addJsPattern
import com.squirrel.base.ui.widget.code.addJsonPattern
import com.squirrel.base.ui.widget.code.addLegadoPattern
import com.squirrel.base.utils.applyTint
import com.squirrel.base.utils.disableEdit
import com.squirrel.base.utils.setLayout
import com.squirrel.base.utils.viewbindingdelegate.viewBinding

class CodeDialog() : BaseDialogFragment(R.layout.dialog_code_view) {

    constructor(code: String, disableEdit: Boolean = true, requestId: String? = null) : this() {
        arguments = Bundle().apply {
            putBoolean("disableEdit", disableEdit)
            putString("code", IntentData.put(code))
            putString("requestId", requestId)
        }
    }

    val binding by viewBinding(DialogCodeViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        if (arguments?.getBoolean("disableEdit") == true) {
            binding.toolBar.title = "code view"
            binding.codeView.disableEdit()
        } else {
            initMenu()
        }
        binding.codeView.addLegadoPattern()
        binding.codeView.addJsonPattern()
        binding.codeView.addJsPattern()
        arguments?.getString("code")?.let {
            binding.codeView.text = IntentData.get(it)
        }
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.code_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_save -> {
                    binding.codeView.text?.toString()?.let { code ->
                        val requestId = arguments?.getString("requestId")
                        (parentFragment as? Callback)?.onCodeSave(code, requestId)
                            ?: (activity as? Callback)?.onCodeSave(code, requestId)
                    }
                    dismiss()
                }
            }
            return@setOnMenuItemClickListener true
        }
    }


    interface Callback {

        fun onCodeSave(code: String, requestId: String?)

    }

}