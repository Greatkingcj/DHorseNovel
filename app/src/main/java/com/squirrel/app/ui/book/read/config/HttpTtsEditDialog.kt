package com.squirrel.app.ui.book.read.config

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import com.squirrel.app.R
import com.squirrel.base.base.BaseDialogFragment
import com.squirrel.base.data.entities.HttpTTS
import com.squirrel.app.databinding.DialogHttpTtsEditBinding
import com.squirrel.base.lib.dialogs.alert
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.app.ui.about.AppLogDialog
import com.squirrel.app.ui.login.SourceLoginActivity
import com.squirrel.base.ui.widget.code.addJsPattern
import com.squirrel.base.ui.widget.code.addJsonPattern
import com.squirrel.base.ui.widget.code.addLegadoPattern
import com.squirrel.base.utils.GSON
import com.squirrel.base.utils.applyTint
import com.squirrel.base.utils.sendToClip
import com.squirrel.base.utils.setLayout
import com.squirrel.base.utils.showDialogFragment
import com.squirrel.base.utils.showHelp
import com.squirrel.base.utils.startActivity
import com.squirrel.base.utils.toastOnUi
import com.squirrel.base.utils.viewbindingdelegate.viewBinding

class HttpTtsEditDialog() : BaseDialogFragment(R.layout.dialog_http_tts_edit, true),
    Toolbar.OnMenuItemClickListener {

    constructor(id: Long) : this() {
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val binding by viewBinding(DialogHttpTtsEditBinding::bind)
    private val viewModel by viewModels<HttpTtsEditViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.tvUrl.run {
            addLegadoPattern()
            addJsonPattern()
            addJsPattern()
        }
        binding.tvLoginUrl.run {
            addLegadoPattern()
            addJsonPattern()
            addJsPattern()
        }
        binding.tvLoginUi.addJsonPattern()
        binding.tvLoginCheckJs.addJsPattern()
        binding.tvHeaders.run {
            addLegadoPattern()
            addJsonPattern()
            addJsPattern()
        }
        viewModel.initData(arguments) {
            initView(httpTTS = it)
        }
        initMenu()
    }

    fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.speak_engine_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    fun initView(httpTTS: HttpTTS) {
        binding.tvName.setText(httpTTS.name)
        binding.tvUrl.setText(httpTTS.url)
        binding.tvContentType.setText(httpTTS.contentType)
        binding.tvConcurrentRate.setText(httpTTS.concurrentRate)
        binding.tvLoginUrl.setText(httpTTS.loginUrl)
        binding.tvLoginUi.setText(httpTTS.loginUi)
        binding.tvLoginCheckJs.setText(httpTTS.loginCheckJs)
        binding.tvHeaders.setText(httpTTS.header)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> viewModel.save(dataFromView()) {
                toastOnUi("保存成功")
            }
            R.id.menu_login -> dataFromView().let { httpTts ->
                if (httpTts.loginUrl.isNullOrBlank()) {
                    toastOnUi("登录url不能为空")
                } else {
                    viewModel.save(httpTts) {
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "httpTts")
                            putExtra("key", httpTts.id.toString())
                        }
                    }
                }
            }
            R.id.menu_show_login_header -> alert {
                setTitle(R.string.login_header)
                dataFromView().getLoginHeader()?.let { loginHeader ->
                    setMessage(loginHeader)
                }
            }
            R.id.menu_del_login_header -> dataFromView().removeLoginHeader()
            R.id.menu_copy_source -> dataFromView().let {
                context?.sendToClip(GSON.toJson(it))
            }
            R.id.menu_paste_source -> viewModel.importFromClip {
                initView(it)
            }
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
            R.id.menu_help -> showHelp("httpTTSHelp")
        }
        return true
    }

    private fun dataFromView(): HttpTTS {
        return HttpTTS(
            id = viewModel.id ?: System.currentTimeMillis(),
            name = binding.tvName.text.toString(),
            url = binding.tvUrl.text.toString(),
            contentType = binding.tvContentType.text?.toString(),
            concurrentRate = binding.tvConcurrentRate.text?.toString(),
            loginUrl = binding.tvLoginUrl.text?.toString(),
            loginUi = binding.tvLoginUi.text?.toString(),
            loginCheckJs = binding.tvLoginCheckJs.text?.toString(),
            header = binding.tvHeaders.text?.toString()
        )
    }

}