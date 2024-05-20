package com.squirrel.base.utils.canvasrecorder

import android.os.Build
import com.squirrel.base.help.config.AppConfig

object CanvasRecorderFactory {

    private val atLeastApi24 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    private val atLeastApi29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    // issue 3868
    fun create(locked: Boolean = false): CanvasRecorder {
        val impl = when {
            !AppConfig.optimizeRender -> CanvasRecorderImpl()
            atLeastApi29 -> CanvasRecorderApi29Impl()
            atLeastApi24 -> CanvasRecorderApi23Impl()
            else -> CanvasRecorderImpl()
        }
        return if (locked) {
            CanvasRecorderLocked(impl)
        } else {
            impl
        }
    }

}
