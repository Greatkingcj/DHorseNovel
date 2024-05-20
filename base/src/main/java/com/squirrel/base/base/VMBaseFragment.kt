package com.squirrel.base.base

import androidx.lifecycle.ViewModel

abstract class VMBaseFragment<VM : ViewModel>(layoutID: Int) : BaseFragment(layoutID) {

    protected abstract val viewModel: VM

}
