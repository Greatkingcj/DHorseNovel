package com.squirrel.app.ui.book.import.remote

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.squirrel.app.R
import com.squirrel.base.base.BaseDialogFragment
import com.squirrel.base.base.adapter.ItemViewHolder
import com.squirrel.base.base.adapter.RecyclerAdapter
import com.squirrel.base.constant.AppConst.DEFAULT_WEBDAV_ID
import com.squirrel.base.constant.AppLog
import com.squirrel.base.data.appDb
import com.squirrel.base.data.entities.Server
import com.squirrel.app.databinding.DialogRecyclerViewBinding
import com.squirrel.app.databinding.ItemServerSelectBinding
import com.squirrel.base.help.config.AppConfig
import com.squirrel.base.lib.dialogs.alert
import com.squirrel.base.lib.theme.backgroundColor
import com.squirrel.base.lib.theme.primaryColor
import com.squirrel.base.ui.widget.recycler.VerticalDivider
import com.squirrel.base.utils.applyTint
import com.squirrel.base.utils.setLayout
import com.squirrel.base.utils.showDialogFragment
import com.squirrel.base.utils.viewbindingdelegate.viewBinding
import com.squirrel.base.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 服务器配置
 */
class ServersDialog : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    val binding by viewBinding(DialogRecyclerViewBinding::bind)
    val viewModel by viewModels<ServersViewModel>()

    private val callback get() = (activity as? Callback)
    private val adapter by lazy { ServersAdapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }


    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.server_config)
        initView()
        initData()
    }

    private fun initView() {
        binding.toolBar.inflateMenu(R.menu.servers)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
        binding.tvFooterLeft.text = getString(R.string.text_default)
        binding.tvFooterLeft.visible()
        binding.tvFooterLeft.setOnClickListener {
            AppConfig.remoteServerId = DEFAULT_WEBDAV_ID
            dismissAllowingStateLoss()
        }
        binding.tvCancel.visible()
        binding.tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvOk.visible()
        binding.tvOk.setOnClickListener {
            AppConfig.remoteServerId = adapter.selectServerId
            dismissAllowingStateLoss()
        }
    }

    private fun initData() {
        lifecycleScope.launch {
            appDb.serverDao.observeAll().catch {
                AppLog.put("服务器配置界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> showDialogFragment(ServerConfigDialog())
        }
        return true
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback?.onDialogDismiss("serversDialog")
    }

    inner class ServersAdapter(context: Context) :
        RecyclerAdapter<Server, ItemServerSelectBinding>(context) {

        var selectServerId: Long = AppConfig.remoteServerId

        override fun getViewBinding(parent: ViewGroup): ItemServerSelectBinding {
            return ItemServerSelectBinding.inflate(inflater, parent, false)
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemServerSelectBinding) {
            binding.rbServer.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed && isChecked) {
                    selectServerId = getItemByLayoutPosition(holder.layoutPosition)!!.id
                    adapter.updateItems(0, itemCount - 1, "upSelect")
                }
            }
            binding.ivEdit.setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let { server ->
                    showDialogFragment(ServerConfigDialog(server.id))
                }
            }
            binding.ivDelete.setOnClickListener {
                alert {
                    setTitle(R.string.draw)
                    setMessage(R.string.sure_del)
                    yesButton {
                        getItemByLayoutPosition(holder.layoutPosition)?.let { server ->
                            viewModel.delete(server)
                        }
                    }
                    noButton()
                }
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemServerSelectBinding,
            item: Server,
            payloads: MutableList<Any>
        ) {
            if (payloads.isEmpty()) {
                binding.root.setBackgroundColor(context.backgroundColor)
                binding.rbServer.text = item.name
                binding.rbServer.isChecked = item.id == selectServerId
            } else {
                binding.rbServer.isChecked = item.id == selectServerId
            }
        }

    }

    interface Callback {

        fun onDialogDismiss(tag: String)

    }

}