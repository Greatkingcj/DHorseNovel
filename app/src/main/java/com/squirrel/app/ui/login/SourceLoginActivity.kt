package com.squirrel.app.ui.login

import android.os.Bundle
import androidx.activity.viewModels
import com.squirrel.app.R
import com.squirrel.base.base.VMBaseActivity
import com.squirrel.base.data.entities.BaseSource
import com.squirrel.app.databinding.ActivitySourceLoginBinding
import com.squirrel.base.utils.showDialogFragment
import com.squirrel.base.utils.viewbindingdelegate.viewBinding


class SourceLoginActivity : VMBaseActivity<ActivitySourceLoginBinding, SourceLoginViewModel>() {

    override val binding by viewBinding(ActivitySourceLoginBinding::inflate)
    override val viewModel by viewModels<SourceLoginViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.initData(intent) { source ->
            initView(source)
        }
    }

    private fun initView(source: BaseSource) {
        if (source.loginUi.isNullOrEmpty()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fl_fragment, WebViewLoginFragment(), "webViewLogin")
                .commit()
        } else {
            showDialogFragment<SourceLoginDialog>()
        }
    }

}