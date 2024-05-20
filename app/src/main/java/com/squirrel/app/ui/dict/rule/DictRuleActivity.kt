package com.squirrel.app.ui.dict.rule

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.squirrel.app.R
import com.squirrel.base.base.VMBaseActivity
import com.squirrel.base.constant.AppLog
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.DictRule
import com.squirrel.app.databinding.ActivityDictRuleBinding
import com.squirrel.app.databinding.DialogEditTextBinding
import com.squirrel.base.help.DirectLinkUpload
import com.squirrel.base.lib.dialogs.alert
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.app.ui.association.ImportDictRuleDialog
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class DictRuleActivity : VMBaseActivity<ActivityDictRuleBinding, DictRuleViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    SelectActionBar.CallBack,
    DictRuleAdapter.CallBack {

    override val viewModel by viewModels<DictRuleViewModel>()
    override val binding by viewBinding(ActivityDictRuleBinding::inflate)
    private val importRecordKey = "dictRuleUrls"
    private val adapter by lazy { DictRuleAdapter(this, this) }
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(
            ImportDictRuleDialog(it)
        )
    }
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        kotlin.runCatching {
            it.uri?.readText(this)?.let {
                showDialogFragment(
                    ImportDictRuleDialog(it)
                )
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
        initRecyclerView()
        initSelectActionView()
        observeDictRuleData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.dict_rule, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        val dragSelectTouchHelper: DragSelectTouchHelper =
            DragSelectTouchHelper(adapter.dragSelectCallback).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(binding.recyclerView)
        // When this page is opened, it is in selection mode
        dragSelectTouchHelper.activeSlideSelect()

        // Note: need judge selection first, so add ItemTouchHelper after it.
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }


    private fun initSelectActionView() {
        binding.selectActionBar.setMainActionText(R.string.delete)
        binding.selectActionBar.inflateMenu(R.menu.dict_rule_sel)
        binding.selectActionBar.setOnMenuItemClickListener(this)
        binding.selectActionBar.setCallBack(this)
    }

    private fun observeDictRuleData() {
        lifecycleScope.launch {
            appDb.dictRuleDao.flowAll().catch {
                AppLog.put("字典规则获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                adapter.setItems(it, adapter.diffItemCallBack)
            }
        }
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> showDialogFragment<DictRuleEditDialog>()
            R.id.menu_import_local -> importDoc.launch {
                mode = HandleFileContract.FILE
                allowExtensions = arrayOf("txt", "json")
            }
            R.id.menu_import_onLine -> showImportDialog()
            R.id.menu_import_qr -> qrCodeResult.launch()
            R.id.menu_import_default -> viewModel.importDefault()
            R.id.menu_help -> showHelp("dictRuleHelp")
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_enable_selection -> viewModel.enableSelection(*adapter.selection.toTypedArray())
            R.id.menu_disable_selection -> viewModel.disableSelection(*adapter.selection.toTypedArray())
            R.id.menu_export_selection -> exportResult.launch {
                mode = HandleFileContract.EXPORT
                fileData = HandleFileContract.FileData(
                    "exportDictRule.json",
                    GSON.toJson(adapter.selection).toByteArray(),
                    "application/json"
                )
            }
        }
        return true
    }

    override fun onClickSelectBarMainAction() {
        viewModel.delete(*adapter.selection.toTypedArray())
    }

    override fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            adapter.selectAll()
        } else {
            adapter.revertSelection()
        }
    }

    override fun revertSelection() {
        adapter.revertSelection()
    }

    override fun update(vararg rule: DictRule) {
        viewModel.update(*rule)
    }

    override fun delete(rule: DictRule) {
        viewModel.delete(rule)
    }

    override fun edit(rule: DictRule) {
        showDialogFragment(DictRuleEditDialog(rule.name))
    }

    override fun upOrder() {
        viewModel.upSortNumber()
    }

    override fun upCountView() {
        binding.selectActionBar.upCountView(
            adapter.selection.size,
            adapter.itemCount
        )
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importRecordKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(titleResource = R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importRecordKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (!cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importRecordKey, cacheUrls.joinToString(","))
                    }
                    showDialogFragment(
                        ImportDictRuleDialog(it)
                    )
                }
            }
            cancelButton()
        }
    }
}
