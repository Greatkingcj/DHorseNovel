package com.squirrel.app.ui.welcome

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.core.view.postDelayed
import com.squirrel.base.base.BaseActivity
import com.squirrel.base.constant.PreferKey
import com.squirrel.base.constant.Theme
import com.squirrel.app.databinding.ActivityWelcomeBinding
import com.squirrel.base.help.config.ThemeConfig
import com.squirrel.base.lib.theme.accentColor
import com.squirrel.base.lib.theme.backgroundColor
import com.squirrel.app.ui.book.read.ReadBookActivity
import com.squirrel.app.ui.main.MainActivity
import com.squirrel.base.utils.*
import com.squirrel.base.utils.viewbindingdelegate.viewBinding

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.ivBook.setColorFilter(accentColor)
        binding.vwTitleLine.setBackgroundColor(accentColor)
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
        } else {
            binding.root.postDelayed(600) { startMainActivity() }
        }
    }

    override fun setupSystemBar() {
        fullScreen()
        setStatusBarColorAuto(backgroundColor, true, fullScreen)
        upNavigationBarColor()
    }

    override fun upBackgroundImage() {
        if (getPrefBoolean(PreferKey.customWelcome)) {
            kotlin.runCatching {
                when (ThemeConfig.getTheme()) {
                    Theme.Dark -> getPrefString(PreferKey.welcomeImageDark)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvLegado.visible(getPrefBoolean(PreferKey.welcomeShowTextDark))
                            binding.ivBook.visible(getPrefBoolean(PreferKey.welcomeShowIconDark))
                            binding.tvGzh.visible(getPrefBoolean(PreferKey.welcomeShowTextDark))
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                    else -> getPrefString(PreferKey.welcomeImage)?.let { path ->
                        val size = windowManager.windowSize
                        BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels).let {
                            binding.tvLegado.visible(getPrefBoolean(PreferKey.welcomeShowText))
                            binding.ivBook.visible(getPrefBoolean(PreferKey.welcomeShowIcon))
                            binding.tvGzh.visible(getPrefBoolean(PreferKey.welcomeShowText))
                            window.decorView.background = BitmapDrawable(resources, it)
                            return
                        }
                    }
                }
            }
        }
        super.upBackgroundImage()
    }

    private fun startMainActivity() {
        startActivity<com.squirrel.app.ui.main.MainActivity>()
        if (getPrefBoolean(PreferKey.defaultToRead)) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()
class Launcher4 : WelcomeActivity()
class Launcher5 : WelcomeActivity()
class Launcher6 : WelcomeActivity()