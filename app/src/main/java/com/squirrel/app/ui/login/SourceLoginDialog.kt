package com.squirrel.app.ui.login

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.squirrel.app.R
import com.squirrel.base.base.BaseDialogFragment
import com.squirrel.base.constant.AppLog
import com.squirrel.base.data.entities.BaseSource
import com.squirrel.base.data.entities.rule.RowUi
import com.squirrel.app.databinding.DialogLoginBinding
import com.squirrel.app.databinding.ItemFilletTextBinding
import com.squirrel.app.databinding.ItemSourceEditBinding
import com.squirrel.base.help.coroutine.Coroutine
import com.squirrel.base.lib.dialogs.alert
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.app.ui.about.AppLogDialog
import com.squirrel.base.utils.GSON
import com.squirrel.base.utils.applyTint
import com.squirrel.base.utils.dpToPx
import com.squirrel.base.utils.isAbsUrl
import com.squirrel.base.utils.openUrl
import com.squirrel.base.utils.printOnDebug
import com.squirrel.base.utils.setLayout
import com.squirrel.base.utils.showDialogFragment
import com.squirrel.base.utils.toastOnUi
import com.squirrel.base.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.views.onClick
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.forEachIndexed
import kotlin.collections.hashMapOf
import kotlin.collections.set


class SourceLoginDialog : BaseDialogFragment(R.layout.dialog_login, true) {

    private val binding by viewBinding(DialogLoginBinding::bind)
    private val viewModel by activityViewModels<SourceLoginViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val source = viewModel.source ?: return
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = getString(R.string.login_source, source.getTag())
        val loginInfo = source.getLoginInfoMap()
        val loginUi = source.loginUi()
        loginUi?.forEachIndexed { index, rowUi ->
            when (rowUi.type) {
                RowUi.Type.text -> ItemSourceEditBinding.inflate(
                    layoutInflater,
                    binding.root,
                    false
                ).let {
                    binding.flexbox.addView(it.root)
                    it.root.id = index + 1000
                    it.textInputLayout.hint = rowUi.name
                    it.editText.setText(loginInfo?.get(rowUi.name))
                }
                RowUi.Type.password -> ItemSourceEditBinding.inflate(
                    layoutInflater,
                    binding.root,
                    false
                ).let {
                    binding.flexbox.addView(it.root)
                    it.root.id = index + 1000
                    it.textInputLayout.hint = rowUi.name
                    it.editText.inputType =
                        InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
                    it.editText.setText(loginInfo?.get(rowUi.name))
                }
                RowUi.Type.button -> ItemFilletTextBinding.inflate(
                    layoutInflater,
                    binding.root,
                    false
                ).let {
                    binding.flexbox.addView(it.root)
                    it.root.id = index + 1000
                    it.textView.text = rowUi.name
                    it.textView.setPadding(16.dpToPx())
                    it.root.onClick {
                        Coroutine.async {
                            if (rowUi.action.isAbsUrl()) {
                                context?.openUrl(rowUi.action!!)
                            } else {
                                // JavaScript
                                rowUi.action?.let { buttonFunctionJS ->
                                    kotlin.runCatching {
                                        source.getLoginJs()?.let { loginJS ->
                                            source.evalJS("$loginJS\n$buttonFunctionJS") {
                                                put("result", getLoginData(loginUi))
                                            }
                                        }
                                    }.onFailure { e ->
                                        AppLog.put(
                                            "LoginUI Button ${rowUi.name} JavaScript error",
                                            e
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        binding.toolBar.inflateMenu(R.menu.source_login)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_ok -> {
                    val loginData = getLoginData(loginUi)
                    login(source, loginData)
                }
                R.id.menu_show_login_header -> alert {
                    setTitle(R.string.login_header)
                    source.getLoginHeader()?.let { loginHeader ->
                        setMessage(loginHeader)
                    }
                }
                R.id.menu_del_login_header -> source.removeLoginHeader()
                R.id.menu_log -> showDialogFragment<com.squirrel.app.ui.about.AppLogDialog>()
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun getLoginData(loginUi: List<RowUi>?): HashMap<String, String> {
        val loginData = hashMapOf<String, String>()
        loginUi?.forEachIndexed { index, rowUi ->
            when (rowUi.type) {
                "text", "password" -> {
                    val rowView = binding.root.findViewById<View>(index + 1000)
                    ItemSourceEditBinding.bind(rowView).editText.text?.let {
                        loginData[rowUi.name] = it.toString()
                    }
                }
            }
        }
        return loginData
    }

    private fun login(source: BaseSource, loginData: HashMap<String, String>) {
        lifecycleScope.launch(IO) {
            if (loginData.isEmpty()) {
                source.removeLoginInfo()
                withContext(Main) {
                    dismiss()
                }
            } else if (source.putLoginInfo(GSON.toJson(loginData))) {
                try {
                    source.login()
                    context?.toastOnUi(R.string.success)
                    withContext(Main) {
                        dismiss()
                    }
                } catch (e: Exception) {
                    AppLog.put("登录出错\n${e.localizedMessage}", e)
                    context?.toastOnUi("登录出错\n${e.localizedMessage}")
                    e.printOnDebug()
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }

}
