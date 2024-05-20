package com.squirrel.app.ui.book.toc.rule

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.squirrel.app.R
import com.squirrel.base.base.BaseDialogFragment
import com.squirrel.base.base.adapter.ItemViewHolder
import com.squirrel.base.base.adapter.RecyclerAdapter
import com.squirrel.base.constant.AppLog
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.TxtTocRule
import com.squirrel.app.databinding.DialogEditTextBinding
import com.squirrel.app.databinding.DialogTocRegexBinding
import com.squirrel.app.databinding.ItemTocRegexBinding
import com.squirrel.base.lib.dialogs.alert
import com.squirrel.base.lib.theme.backgroundColor
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.app.ui.association.ImportTxtTocRuleDialog
import com.squirrel.app.ui.file.HandleFileContract
import com.squirrel.base.ui.qrcode.QrCodeResult
import com.squirrel.base.ui.widget.recycler.ItemTouchCallback
import com.squirrel.base.ui.widget.recycler.VerticalDivider
import com.squirrel.base.utils.ACache
import com.squirrel.base.utils.applyTint
import com.squirrel.base.utils.launch
import com.squirrel.base.utils.readText
import com.squirrel.base.utils.setLayout
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

/**
 * txt目录规则
 */
class TxtTocRuleDialog() : BaseDialogFragment(R.layout.dialog_toc_regex),
    Toolbar.OnMenuItemClickListener,
    TxtTocRuleEditDialog.Callback {

    constructor(tocRegex: String?) : this() {
        arguments = Bundle().apply {
            putString("tocRegex", tocRegex)
        }
    }

    private val importTocRuleKey = "tocRuleUrl"
    private val viewModel: TxtTocRuleViewModel by viewModels()
    private val binding by viewBinding(DialogTocRegexBinding::bind)
    private val adapter by lazy { TocRegexAdapter(requireContext()) }
    var selectedName: String? = null
    private var durRegex: String? = null
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(ImportTxtTocRuleDialog(it))
    }
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        kotlin.runCatching {
            it.uri?.readText(requireContext())?.let {
                showDialogFragment(ImportTxtTocRuleDialog(it))
            }
        }.onFailure {
            toastOnUi("readTextError:${it.localizedMessage}")
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.8f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        durRegex = arguments?.getString("tocRegex")
        binding.toolBar.setTitle(R.string.txt_toc_rule)
        binding.toolBar.inflateMenu(R.menu.txt_toc_rule)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        initView()
        initData()
    }

    private fun initView() = binding.run {
        recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recyclerView)
        tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        tvOk.setOnClickListener {
            adapter.getItems().forEach { tocRule ->
                if (selectedName == tocRule.name) {
                    val callBack = activity as? CallBack
                    callBack?.onTocRegexDialogResult(tocRule.rule)
                    dismissAllowingStateLoss()
                    return@setOnClickListener
                }
            }
        }
    }

    private fun initData() {
        lifecycleScope.launch {
            appDb.txtTocRuleDao.observeAll().catch {
                AppLog.put("TXT目录规则对话框获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect { tocRules ->
                initSelectedName(tocRules)
                adapter.setItems(tocRules, adapter.diffItemCallBack)
            }
        }
    }

    private fun initSelectedName(tocRules: List<TxtTocRule>) {
        if (selectedName == null && durRegex != null) {
            tocRules.forEach {
                if (durRegex == it.rule) {
                    selectedName = it.name
                    return@forEach
                }
            }
            if (selectedName == null) {
                selectedName = ""
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
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
        return false
    }

    override fun saveTxtTocRule(txtTocRule: TxtTocRule) {
        viewModel.save(txtTocRule)
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
        requireContext().alert(titleResource = R.string.import_on_line) {
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

    inner class TocRegexAdapter(context: Context) :
        RecyclerAdapter<TxtTocRule, ItemTocRegexBinding>(context),
        ItemTouchCallback.Callback {

        val diffItemCallBack = object : DiffUtil.ItemCallback<TxtTocRule>() {

            override fun areItemsTheSame(oldItem: TxtTocRule, newItem: TxtTocRule): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TxtTocRule, newItem: TxtTocRule): Boolean {
                if (oldItem.name != newItem.name) {
                    return false
                }
                if (oldItem.enable != newItem.enable) {
                    return false
                }
                if (oldItem.example != newItem.example) {
                    return false
                }
                return true
            }

            override fun getChangePayload(oldItem: TxtTocRule, newItem: TxtTocRule): Any? {
                val payload = Bundle()
                if (oldItem.name != newItem.name) {
                    payload.putBoolean("upName", true)
                }
                if (oldItem.enable != newItem.enable) {
                    payload.putBoolean("enabled", newItem.enable)
                }
                if (oldItem.example != newItem.example) {
                    payload.putBoolean("upExample", true)
                }
                if (payload.isEmpty) {
                    return null
                }
                return payload
            }
        }

        override fun getViewBinding(parent: ViewGroup): ItemTocRegexBinding {
            return ItemTocRegexBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemTocRegexBinding,
            item: TxtTocRule,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                val bundle = payloads.getOrNull(0) as? Bundle
                if (bundle == null) {
                    root.setBackgroundColor(context.backgroundColor)
                    rbRegexName.text = item.name
                    titleExample.text = item.example
                    rbRegexName.isChecked = item.name == selectedName
                    swtEnabled.isChecked = item.enable
                } else {
                    bundle.keySet().map {
                        when (it) {
                            "upName" -> rbRegexName.text = item.name
                            "upExample" -> titleExample.text = item.example
                            "enabled" -> swtEnabled.isChecked = item.enable
                            "upSelect" -> rbRegexName.isChecked = item.name == selectedName
                        }
                    }
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemTocRegexBinding) {
            binding.apply {
                rbRegexName.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed && isChecked) {
                        selectedName = getItem(holder.layoutPosition)?.name
                        updateItems(0, itemCount - 1, bundleOf("upSelect" to null))
                    }
                }
                swtEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        getItem(holder.layoutPosition)?.let {
                            it.enable = isChecked
                            viewModel.update(it)
                        }
                    }
                }
                ivEdit.setOnClickListener {
                    showDialogFragment(TxtTocRuleEditDialog(getItem(holder.layoutPosition)?.id))
                }
                ivDelete.setOnClickListener {
                    getItem(holder.layoutPosition)?.let { item ->
                        viewModel.del(item)
                    }
                }
            }
        }

        private var isMoved = false

        override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
            swapItem(srcPosition, targetPosition)
            isMoved = true
            return super.swap(srcPosition, targetPosition)
        }

        override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.onClearView(recyclerView, viewHolder)
            if (isMoved) {
                for ((index, item) in getItems().withIndex()) {
                    item.serialNumber = index + 1
                }
                viewModel.update(*getItems().toTypedArray())
            }
            isMoved = false
        }
    }

    interface CallBack {
        fun onTocRegexDialogResult(tocRegex: String) {}
    }

}