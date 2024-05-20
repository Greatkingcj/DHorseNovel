package com.squirrel.base.utils.canvasrecorder.pools

import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi
import com.squirrel.base.utils.objectpool.BaseObjectPool

@RequiresApi(Build.VERSION_CODES.Q)
class RenderNodePool : BaseObjectPool<RenderNode>(64) {

    override fun recycle(target: RenderNode) {
        target.discardDisplayList()
        super.recycle(target)
    }

    override fun create(): RenderNode = RenderNode("CanvasRecorder")

}
