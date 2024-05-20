package com.squirrel.base.lib.permission

interface OnPermissionsDeniedCallback {

    fun onPermissionsDenied(deniedPermissions: Array<String>)

}
