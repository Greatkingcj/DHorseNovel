package com.squirrel.app.ui.book.toc.rule

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.squirrel.app.R
import com.squirrel.base.base.VMBaseActivity
import com.squirrel.base.constant.AppLog
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.TxtTocRule
import com.squirrel.app.databinding.ActivityTxtTocRuleBinding
import com.squirrel.app.databinding.DialogEditTextBinding
import com.squirrel.base.help.DirectLinkUpload
import com.squirrel.base.lib.dialogs.alert
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.app.ui.association.ImportTxtTocRuleDialog
import com.squirrel.app.ui.file.HandleFileContract
import com.squirrel.base.ui.qrcode.QrCodeResult
import com.squirrel.base.ui.widget.SelectActionBar
import com.squirrel.base.ui.widget.recycler.DragSelectTouchHelper
import com.squirrel.base.ui.widget.recycler.ItemTouchCallback
import com.squirrel.base.ui.widget.recycler.VerticalDivider
import com.squirrel.base.utils.ACache
import com.squirrel.base.utils.GSON
import com.squirrel.base.utils.isAbsUrl
import com.squirrel.base.utils.launch
import com.squirrel.base.utils.readText
import com.squirrel.base.utils.sendToClip
import com.squirrel.base.utils.setEdgeEffectColor
import com.squirrel.base.utils.showDialogFragment
import com.squirrel.base.utils.showHelp
import com.squirrel.base.utils.splitNotBlank
import com.squirrel.base.utils.toastOnUi
import com.squirrel.base.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class TxtTocRuleActivity : VMBaseActivity<ActivityTxtTocRuleBinding, TxtTocRuleViewModel>(),
    TxtTocRuleAdapter.CallBack,
    SelectActionBar.CallBack,
    TxtTocRuleEditDialog.Callback,
    PopupMenu.OnMenuItemClickListener {

    override val viewModel: TxtTocRuleViewModel by viewModels()
    override val binding: ActivityTxtTocRuleBinding by viewBinding(ActivityTxtTocRuleBinding::inflate)
    private val adapter: TxtTocRuleAdapter by lazy {
        TxtTocRuleAdapter(this, this)
    }
    private val importTocRuleKey = "tocRuleUrl"
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(ImportTxtTocRuleDialog(it))
    }
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        kotlin.runCatching {
            it.uri?.readText(this)?.let {
                showDialogFragment(ImportTxtTocRuleDialog(it))
            }
        }.onFailure {
            toastOnUi("readTextError:${it.localizedMessage}")
        }
    }
    private val exportResult = registerForActivityResult(HandleFileContract()) {
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
                    sendToClip(uri.toString())
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initBottomActionBar()
        initData()
    }

    private fun initView() = binding.run {
        recyclerView.setEdgeEffectColor(primaryColor)
        recyclerView.addItemDecoration(VerticalDivider(this@TxtTocRuleActivity))
        recyclerView.adapter = adapter
        // When this page is opened, it is in selection mode
        val dragSelectTouchHelper =
            DragSelectTouchHelper(adapter.dragSelectCallback).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(binding.recyclerView)
        dragSelectTouchHelper.activeSlideSelect()
        // Note: need judge selection first, so add ItemTouchHelper after it.
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initBottomActionBar() {
        binding.selectActionBar.setMainActionText(R.string.delete)
        binding.selectActionBar.inflateMenu(R.menu.txt_toc_rule_sel)
        binding.selectActionBar.setOnMenuItemClickListener(this)
        binding.selectActionBar.setCallBack(this)
    }

    private fun initData() {
        lifecycleScope.launch {
            appDb.txtTocRuleDao.observeAll().catch {
                AppLog.put("TXT目录规则界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect { tocRules ->
                adapter.setItems(tocRules, adapter.diffItemCallBack)
                upCountView()
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.txt_toc_rule, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> showDialogFragment(TxtTocRuleEditDialog())
            R.id.menu_import_local -> importDoc.launch {
                mode = HandleFileContract.FILE
                allowExtensions = arrayOf("txt", "json")
            }
            R.id.menu_import_onLine -> showImportDialog()
            R.id.menu_import_qr -> qrCodeResult.launch()
            R.id.menu_import_default -> viewModel.importDefault()
            R.id.menu_help -> showHelp("txtTocRuleHelp")

        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun del(source: TxtTocRule) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + source.name)
            noButton()
            yesButton {
                viewModel.del(source)
            }
        }
    }

    override fun edit(source: TxtTocRule) {
        showDialogFragment(TxtTocRuleEditDialog(source.id))
    }

    override fun onClickSelectBarMainAction() {
        delSourceDialog()
    }

    override fun revertSelection() {
        adapter.revertSelection()
    }

    override fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            adapter.selectAll()
        } else {
            adapter.revertSelection()
        }
    }

    override fun saveTxtTocRule(txtTocRule: TxtTocRule) {
        viewModel.save(txtTocRule)
    }

    override fun update(vararg source: TxtTocRule) {
        viewModel.update(*source)
    }

    override fun toTop(source: TxtTocRule) {
        viewModel.toTop(source)
    }

    override fun toBottom(source: TxtTocRule) {
        viewModel.toBottom(source)
    }

    override fun upOrder() {
        viewModel.upOrder()
    }

    override fun upCountView() {
        binding.selectActionBar
            .upCountView(adapter.selection.size, adapter.itemCount)
    }

    private fun delSourceDialog() {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            yesButton { viewModel.del(*adapter.selection.toTypedArray()) }
            noButton()
        }
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(cacheDir = false)
        val defaultUrl = "https://gitee.com/fisher52/YueDuJson/raw/master/myTxtChapterRule.json"
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importTocRuleKey)
            ?.splitNotBlank(",")
            ?.toMutableList()
            ?: mutableListOf()
        if (!cacheUrls.contains(defaultUrl)) {
            cacheUrls.add(0, defaultUrl)
        }
        alert(titleResource = R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (!cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                    }
                    showDialogFragment(ImportTxtTocRuleDialog(it))
                }
            }
            cancelButton()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_enable_selection -> viewModel.enableSelection(*adapter.selection.toTypedArray())
            R.id.menu_disable_selection -> viewModel.disableSelection(*adapter.selection.toTypedArray())
            R.id.menu_export_selection -> exportResult.launch {
                mode = HandleFileContract.EXPORT
                fileData = HandleFileContract.FileData(
                    "exportTxtTocRule.json",
                    GSON.toJson(adapter.selection).toByteArray(),
                    "application/json"
                )
            }
        }
        return true
    }

}
