package com.squirrel.base.ui.widget.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.squirrel.base.R
import com.squirrel.base.constant.AppConst
import com.squirrel.base.databinding.DialogUrlOptionEditBinding
import com.squirrel.base.model.analyzeRule.AnalyzeUrl
import com.squirrel.base.utils.GSON
import com.squirrel.base.utils.setLayout

class UrlOptionDialog(context: Context, private val success: (String) -> Unit) : Dialog(context) {

    val binding = DialogUrlOptionEditBinding.inflate(layoutInflater)

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
        window?.setBackgroundDrawableResource(R.color.transparent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.root.setOnClickListener { dismiss() }
        binding.vwBg.setOnClickListener(null)
        binding.editMethod.setFilterValues("POST", "GET")
        binding.editCharset.setFilterValues(AppConst.charsets)
        binding.tvOk.setOnClickListener {
            success.invoke(GSON.toJson(getUrlOption()))
            dismiss()
        }
    }

    private fun getUrlOption(): AnalyzeUrl.UrlOption {
        val urlOption = AnalyzeUrl.UrlOption()
        urlOption.useWebView(binding.cbUseWebView.isChecked)
        urlOption.setMethod(binding.editMethod.text.toString())
        urlOption.setCharset(binding.editCharset.text.toString())
        urlOption.setHeaders(binding.editHeaders.text.toString())
        urlOption.setBody(binding.editBody.text.toString())
        urlOption.setRetry(binding.editRetry.text.toString())
        urlOption.setType(binding.editType.text.toString())
        urlOption.setWebJs(binding.editWebJs.text.toString())
        urlOption.setJs(binding.editJs.text.toString())
        return urlOption
    }

}