package com.squirrel.app.ui.main.my

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
import com.squirrel.app.R
import com.squirrel.base.base.BaseFragment
import com.squirrel.base.constant.EventBus
import com.squirrel.base.constant.PreferKey
import com.squirrel.app.databinding.FragmentMyConfigBinding
import com.squirrel.base.help.config.ThemeConfig
import com.squirrel.base.lib.dialogs.selector
import com.squirrel.base.lib.prefs.NameListPreference
import com.squirrel.base.lib.prefs.SwitchPreference
import com.squirrel.base.lib.prefs.fragment.PreferenceFragment
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.base.service.WebService
import com.squirrel.app.ui.about.AboutActivity
import com.squirrel.app.ui.about.ReadRecordActivity
import com.squirrel.app.ui.book.bookmark.AllBookmarkActivity
import com.squirrel.app.ui.book.source.manage.BookSourceActivity
import com.squirrel.app.ui.book.toc.rule.TxtTocRuleActivity
import com.squirrel.app.ui.config.ConfigActivity
import com.squirrel.app.ui.config.ConfigTag
import com.squirrel.app.ui.dict.rule.DictRuleActivity
import com.squirrel.app.ui.file.FileManageActivity
import com.squirrel.app.ui.main.MainFragmentInterface
import com.squirrel.app.ui.replace.ReplaceRuleActivity
import com.squirrel.base.utils.LogUtils
import com.squirrel.base.utils.getPrefBoolean
import com.squirrel.base.utils.observeEventSticky
import com.squirrel.base.utils.openUrl
import com.squirrel.base.utils.putPrefBoolean
import com.squirrel.base.utils.sendToClip
import com.squirrel.base.utils.setEdgeEffectColor
import com.squirrel.base.utils.showHelp
import com.squirrel.base.utils.startActivity
import com.squirrel.base.utils.viewbindingdelegate.viewBinding

class MyFragment() : BaseFragment(R.layout.fragment_my_config), MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = MyPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> showHelp("appHelp")
        }
    }

    /**
     * 配置
     */
    class MyPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            putPrefBoolean(PreferKey.webService, WebService.isRun)
            addPreferencesFromResource(R.xml.pref_main)
            findPreference<SwitchPreference>("webService")?.onLongClick {
                if (!WebService.isRun) {
                    return@onLongClick false
                }
                context?.selector(arrayListOf("复制地址", "浏览器打开")) { _, i ->
                    when (i) {
                        0 -> context?.sendToClip(it.summary.toString())
                        1 -> context?.openUrl(it.summary.toString())
                    }
                }
                true
            }
            observeEventSticky<String>(EventBus.WEB_SERVICE) {
                findPreference<SwitchPreference>(PreferKey.webService)?.let {
                    it.isChecked = WebService.isRun
                    it.summary = if (WebService.isRun) {
                        WebService.hostAddress
                    } else {
                        getString(R.string.web_service_desc)
                    }
                }
            }
            findPreference<NameListPreference>(PreferKey.themeMode)?.let {
                it.setOnPreferenceChangeListener { _, _ ->
                    view?.post { ThemeConfig.applyDayNight(requireContext()) }
                    true
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.webService -> {
                    if (requireContext().getPrefBoolean("webService")) {
                        WebService.start(requireContext())
                    } else {
                        WebService.stop(requireContext())
                    }
                }

                "recordLog" -> LogUtils.upLevel()
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "bookSourceManage" -> startActivity<BookSourceActivity>()
                "replaceManage" -> startActivity<ReplaceRuleActivity>()
                "dictRuleManage" -> startActivity<DictRuleActivity>()
                "txtTocRuleManage" -> startActivity<TxtTocRuleActivity>()
                "bookmark" -> startActivity<AllBookmarkActivity>()
                "setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.OTHER_CONFIG)
                }

                "web_dav_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.BACKUP_CONFIG)
                }

                "theme_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.THEME_CONFIG)
                }

                "fileManage" -> startActivity<FileManageActivity>()
                "readRecord" -> startActivity<com.squirrel.app.ui.about.ReadRecordActivity>()
                "about" -> startActivity<com.squirrel.app.ui.about.AboutActivity>()
                "exit" -> activity?.finish()
            }
            return super.onPreferenceTreeClick(preference)
        }


    }
}