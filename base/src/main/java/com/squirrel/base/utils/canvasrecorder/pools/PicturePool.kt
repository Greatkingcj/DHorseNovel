package com.squirrel.base.utils.canvasrecorder.pools

import android.graphics.Picture
import com.squirrel.base.utils.objectpool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>(64) {

    override fun create(): Picture = Picture()

}
