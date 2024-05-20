package com.squirrel.app.ui.association

import android.os.Bundle
import com.squirrel.base.base.BaseActivity
import com.squirrel.app.databinding.ActivityTranslucenceBinding
import com.squirrel.base.utils.showDialogFragment
import com.squirrel.base.utils.viewbindingdelegate.viewBinding

/**
 * 验证码
 */
class VerificationCodeActivity :
    BaseActivity<ActivityTranslucenceBinding>() {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("imageUrl")?.let {
            val sourceOrigin = intent.getStringExtra("sourceOrigin")
            val sourceName = intent.getStringExtra("sourceName")
            showDialogFragment(
                VerificationCodeDialog(it, sourceOrigin, sourceName)
            )
        } ?: finish()
    }

}