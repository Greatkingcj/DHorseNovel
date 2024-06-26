package com.squirrel.app.ui.config

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.squirrel.app.R
import com.squirrel.base.base.BaseDialogFragment
import com.squirrel.app.databinding.DialogDirectLinkUploadConfigBinding
import com.squirrel.base.help.DirectLinkUpload
import com.squirrel.base.lib.dialogs.alert
import com.squirrel.base.lib.dialogs.selector
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.base.utils.GSON
import com.squirrel.base.utils.applyTint
import com.squirrel.base.utils.fromJsonObject
import com.squirrel.base.utils.getClipText
import com.squirrel.base.utils.sendToClip
import com.squirrel.base.utils.setLayout
import com.squirrel.base.utils.toastOnUi
import com.squirrel.base.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx
import splitties.views.onClick

class DirectLinkUploadConfig : BaseDialogFragment(R.layout.dialog_direct_link_upload_config),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogDirectLinkUploadConfigBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.inflateMenu(R.menu.direct_link_upload_config)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        upView(DirectLinkUpload.getRule())
        binding.tvCancel.onClick {
            dismiss()
        }
        binding.tvFooterLeft.onClick {
            test()
        }
        binding.tvOk.onClick {
            getRule()?.let { rule ->
                DirectLinkUpload.putConfig(rule)
                dismiss()
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_import_default -> importDefault()
            R.id.menu_copy_rule -> getRule()?.let { rule ->
                requireContext().sendToClip(GSON.toJson(rule))
            }

            R.id.menu_paste_rule -> runCatching {
                requireContext().getClipText()!!.let {
                    val rule = GSON.fromJsonObject<DirectLinkUpload.Rule>(it).getOrThrow()
                    upView(rule)
                }
            }.onFailure {
                toastOnUi("剪贴板为空或格式不对")
            }
        }
        return true
    }

    private fun upView(rule: DirectLinkUpload.Rule) {
        binding.editUploadUrl.setText(rule.uploadUrl)
        binding.editDownloadUrlRule.setText(rule.downloadUrlRule)
        binding.editSummary.setText(rule.summary)
        binding.cbCompress.isChecked = rule.compress
    }

    private fun getRule(): DirectLinkUpload.Rule? {
        val uploadUrl = binding.editUploadUrl.text?.toString()
        val downloadUrlRule = binding.editDownloadUrlRule.text?.toString()
        val summary = binding.editSummary.text?.toString()
        val compress = binding.cbCompress.isChecked
        if (uploadUrl.isNullOrBlank()) {
            toastOnUi("上传Url不能为空")
            return null
        }
        if (downloadUrlRule.isNullOrBlank()) {
            toastOnUi("下载Url规则不能为空")
            return null
        }
        if (summary.isNullOrBlank()) {
            toastOnUi("注释不能为空")
            return null
        }
        return DirectLinkUpload.Rule(uploadUrl, downloadUrlRule, summary, compress)
    }

    private fun importDefault() {
        requireContext().selector(DirectLinkUpload.defaultRules) { _, rule, _ ->
            upView(rule)
        }
    }

    private fun test() {
        val rule = getRule() ?: return
        execute {
            DirectLinkUpload.upLoad("test.json", "{}", "application/json", rule)
        }.onError {
            alertTestResult(it.localizedMessage ?: "ERROR")
        }.onSuccess { result ->
            alertTestResult(result)
        }
    }

    private fun alertTestResult(result: String) {
        alert {
            setTitle("result")
            setMessage(result)
            okButton()
            negativeButton(R.string.copy_text) {
                appCtx.sendToClip(result)
            }
        }
    }

}