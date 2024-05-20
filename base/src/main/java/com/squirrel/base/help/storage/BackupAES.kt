package com.squirrel.base.help.storage

import cn.hutool.crypto.symmetric.AES
import com.squirrel.base.help.config.LocalConfig
import com.squirrel.base.utils.MD5Utils

class BackupAES : AES(
    MD5Utils.md5Encode(LocalConfig.password ?: "").encodeToByteArray(0, 16)
)