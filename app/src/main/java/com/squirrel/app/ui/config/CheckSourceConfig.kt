package com.squirrel.app.ui.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.squirrel.app.R
import com.squirrel.base.base.BaseDialogFragment
import com.squirrel.base.constant.PreferKey
import com.squirrel.app.databinding.DialogCheckSourceConfigBinding
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.base.model.CheckSource
import com.squirrel.base.utils.putPrefString
import com.squirrel.base.utils.setLayout
import com.squirrel.base.utils.toastOnUi
import com.squirrel.base.utils.viewbindingdelegate.viewBinding
import splitties.views.onClick

class CheckSourceConfig : BaseDialogFragment(R.layout.dialog_check_source_config) {

    private val binding by viewBinding(DialogCheckSourceConfigBinding::bind)

    //允许的最小超时时间，秒
    private val minTimeout = 0L

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.run {
            checkSearch.onClick {
                if (!checkSearch.isChecked && !checkDiscovery.isChecked) {
                    checkDiscovery.isChecked = true
                }
            }
            checkDiscovery.onClick {
                if (!checkSearch.isChecked && !checkDiscovery.isChecked) {
                    checkSearch.isChecked = true
                }
            }
            checkInfo.onClick {
                if (!checkInfo.isChecked) {
                    checkCategory.isChecked = false
                    checkContent.isChecked = false
                    checkCategory.isEnabled = false
                    checkContent.isEnabled = false
                } else {
                    checkCategory.isEnabled = true
                }
            }
            checkCategory.onClick {
                if (!checkCategory.isChecked) {
                    checkContent.isChecked = false
                    checkContent.isEnabled = false
                } else {
                    checkContent.isEnabled = true
                }
            }
        }
        CheckSource.run {
            binding.checkSourceTimeout.setText((timeout / 1000).toString())
            binding.checkSearch.isChecked = checkSearch
            binding.checkDiscovery.isChecked = checkDiscovery
            binding.checkInfo.isChecked = checkInfo
            binding.checkCategory.isChecked = checkCategory
            binding.checkContent.isChecked = checkContent
            binding.checkCategory.isEnabled = checkInfo
            binding.checkContent.isEnabled = checkCategory
            binding.tvCancel.onClick {
                dismiss()
            }
            binding.tvOk.onClick {
                val text = binding.checkSourceTimeout.text.toString()
                when {
                    text.isBlank() -> {
                        toastOnUi("${getString(R.string.timeout)}${getString(R.string.cannot_empty)}")
                        return@onClick
                    }
                    text.toLong() <= minTimeout -> {
                        toastOnUi(
                            "${getString(R.string.timeout)}${getString(R.string.less_than)}${minTimeout}${
                                getString(
                                    R.string.seconds
                                )
                            }"
                        )
                        return@onClick
                    }
                    else -> timeout = text.toLong() * 1000
                }
                checkSearch = binding.checkSearch.isChecked
                checkDiscovery = binding.checkDiscovery.isChecked
                checkInfo = binding.checkInfo.isChecked
                checkCategory = binding.checkCategory.isChecked
                checkContent = binding.checkContent.isChecked
                putConfig()
                putPrefString(PreferKey.checkSource, summary)
                dismiss()
            }
        }
    }
}