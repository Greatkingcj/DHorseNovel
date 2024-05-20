package com.squirrel.base.ui.qrcode

import com.google.zxing.Result

interface ScanResultCallback {

    fun onScanResultCallback(result: Result?)

}