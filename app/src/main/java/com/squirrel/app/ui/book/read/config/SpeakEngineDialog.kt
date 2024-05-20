package com.squirrel.app.ui.book.read.config

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.squirrel.app.R
import com.squirrel.base.base.BaseDialogFragment
import com.squirrel.base.base.adapter.ItemViewHolder
import com.squirrel.base.base.adapter.RecyclerAdapter
import com.squirrel.base.constant.AppLog
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.HttpTTS
import com.squirrel.app.databinding.DialogEditTextBinding
import com.squirrel.app.databinding.DialogRecyclerViewBinding
import com.squirrel.app.databinding.ItemHttpTtsBinding
import com.squirrel.base.help.DirectLinkUpload
import com.squirrel.base.help.config.AppConfig
import com.squirrel.base.lib.dialogs.SelectItem
import com.squirrel.base.lib.dialogs.alert
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.base.model.ReadAloud
import com.squirrel.base.model.ReadBook
import com.squirrel.app.ui.association.ImportHttpTtsDialog
import com.squirrel.app.ui.file.HandleFileContract
import com.squirrel.app.ui.login.SourceLoginActivity
import com.squirrel.base.utils.ACache
import com.squirrel.base.utils.GSON
import com.squirrel.base.utils.applyTint
import com.squirrel.base.utils.fromJsonObject
import com.squirrel.base.utils.gone
import com.squirrel.base.utils.isAbsUrl
import com.squirrel.base.utils.isJsonObject
import com.squirrel.base.utils.sendToClip
import com.squirrel.base.utils.setEdgeEffectColor
import com.squirrel.base.utils.setLayout
import com.squirrel.base.utils.showDialogFragment
import com.squirrel.base.utils.splitNotBlank
import com.squirrel.base.utils.startActivity
import com.squirrel.base.utils.viewbindingdelegate.viewBinding
import com.squirrel.base.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * tts引擎管理
 */
class SpeakEngineDialog(val callBack: CallBack) : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel: SpeakEngineViewModel by viewModels()
    private val ttsUrlKey = "ttsUrlKey"
    private val adapter by lazy { Adapter(requireContext()) }
    private var ttsEngine: String? = ReadAloud.ttsEngine
    private val sysTtsViews = arrayListOf<RadioButton>()
    private val importDocResult = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            viewModel.importLocal(uri)
        }
    }
    private val exportDirResult = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            alert(R.string.export_success) {
                if (uri.toString().isAbsUrl()) {
                    setMessage(DirectLinkUpload.getSummary())
                }
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = getString(R.string.path)
                    editView.setText(uri.toString())
                }
                customView { alertBinding.root }
                okButton {
                    requireContext().sendToClip(uri.toString())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initMenu()
        initData()
    }

    private fun initView() = binding.run {
        toolBar.setBackgroundColor(primaryColor)
        toolBar.setTitle(R.string.speak_engine)
        recyclerView.setEdgeEffectColor(primaryColor)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        adapter.addHeaderView {
            ItemHttpTtsBinding.inflate(layoutInflater, recyclerView, false).apply {
                sysTtsViews.add(cbName)
                ivEdit.gone()
                ivMenuDelete.gone()
                labelSys.visible()
                cbName.text = "系统默认"
                cbName.tag = ""
                cbName.isChecked = ttsEngine == null || ttsEngine!!.isJsonObject()
                        && GSON.fromJsonObject<SelectItem<String>>(ttsEngine)
                    .getOrNull()?.value.isNullOrEmpty()
                cbName.setOnClickListener {
                    upTts(GSON.toJson(SelectItem("系统默认", "")))
                }
            }
        }
        viewModel.sysEngines.forEach { engine ->
            adapter.addHeaderView {
                ItemHttpTtsBinding.inflate(layoutInflater, recyclerView, false).apply {
                    sysTtsViews.add(cbName)
                    ivEdit.gone()
                    ivMenuDelete.gone()
                    labelSys.visible()
                    cbName.text = engine.label
                    cbName.tag = engine.name
                    cbName.isChecked = GSON.fromJsonObject<SelectItem<String>>(ttsEngine)
                        .getOrNull()?.value == cbName.tag
                    cbName.setOnClickListener {
                        upTts(GSON.toJson(SelectItem(engine.label, engine.name)))
                    }
                }
            }
        }
        tvFooterLeft.setText(R.string.book)
        tvFooterLeft.visible()
        tvFooterLeft.setOnClickListener {
            ReadBook.book?.setTtsEngine(ttsEngine)
            callBack.upSpeakEngineSummary()
            ReadAloud.upReadAloudClass()
            dismissAllowingStateLoss()
        }
        tvOk.setText(R.string.general)
        tvOk.visible()
        tvOk.setOnClickListener {
            ReadBook.book?.setTtsEngine(null)
            AppConfig.ttsEngine = ttsEngine
            callBack.upSpeakEngineSummary()
            ReadAloud.upReadAloudClass()
            dismissAllowingStateLoss()
        }
        tvCancel.visible()
        tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun initMenu() = binding.run {
        toolBar.inflateMenu(R.menu.speak_engine)
        toolBar.menu.applyTint(requireContext())
        toolBar.setOnMenuItemClickListener(this@SpeakEngineDialog)
    }

    private fun initData() {
        lifecycleScope.launch {
            appDb.httpTTSDao.flowAll().catch {
                AppLog.put("朗读引擎界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect {
                adapter.setItems(it)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> showDialogFragment<HttpTtsEditDialog>()
            R.id.menu_default -> viewModel.importDefault()
            R.id.menu_import_local -> importDocResult.launch {
                mode = HandleFileContract.FILE
                allowExtensions = arrayOf("txt", "json")
            }

            R.id.menu_import_onLine -> importAlert()
            R.id.menu_export -> exportDirResult.launch {
                mode = HandleFileContract.EXPORT
                fileData = HandleFileContract.FileData(
                    "httpTts.json",
                    GSON.toJson(adapter.getItems()).toByteArray(),
                    "application/json"
                )
            }
        }
        return true
    }

    private fun importAlert() {
        val aCache = ACache.get(cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(ttsUrlKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(ttsUrlKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let { url ->
                    if (!cacheUrls.contains(url)) {
                        cacheUrls.add(0, url)
                        aCache.put(ttsUrlKey, cacheUrls.joinToString(","))
                    }
                    showDialogFragment(ImportHttpTtsDialog(url))
                }
            }
        }
    }

    private fun upTts(tts: String) {
        ttsEngine = tts
        sysTtsViews.forEach {
            it.isChecked = GSON.fromJsonObject<SelectItem<String>>(ttsEngine)
                .getOrNull()?.value == it.tag
        }
        adapter.notifyItemRangeChanged(adapter.getHeaderCount(), adapter.itemCount)
    }

    inner class Adapter(context: Context) :
        RecyclerAdapter<HttpTTS, ItemHttpTtsBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemHttpTtsBinding {
            return ItemHttpTtsBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemHttpTtsBinding,
            item: HttpTTS,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                cbName.text = item.name
                cbName.isChecked = item.id.toString() == ttsEngine
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemHttpTtsBinding) {
            binding.run {
                cbName.setOnClickListener {
                    getItemByLayoutPosition(holder.layoutPosition)?.let { httpTTS ->
                        val id = httpTTS.id.toString()
                        upTts(id)
                        if (!httpTTS.loginUrl.isNullOrBlank()
                            && httpTTS.getLoginInfo().isNullOrBlank()
                        ) {
                            startActivity<SourceLoginActivity> {
                                putExtra("type", "httpTts")
                                putExtra("key", id)
                            }
                        }
                    }
                }
                ivEdit.setOnClickListener {
                    val id = getItemByLayoutPosition(holder.layoutPosition)!!.id
                    showDialogFragment(HttpTtsEditDialog(id))
                }
                ivMenuDelete.setOnClickListener {
                    getItemByLayoutPosition(holder.layoutPosition)?.let { httpTTS ->
                        appDb.httpTTSDao.delete(httpTTS)
                    }
                }
            }
        }

    }

    interface CallBack {
        fun upSpeakEngineSummary()
    }

}