package com.squirrel.base.ui.widget.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.squirrel.base.R
import com.squirrel.base.base.BaseDialogFragment
import com.squirrel.base.databinding.DialogPhotoViewBinding
import com.squirrel.base.help.book.BookHelp
import com.squirrel.base.help.glide.ImageLoader
import com.squirrel.base.help.glide.OkHttpModelLoader
import com.squirrel.base.model.BookCover
import com.squirrel.base.model.ImageProvider
import com.squirrel.base.model.ReadBook
import com.squirrel.base.utils.setLayout
import com.squirrel.base.utils.viewbindingdelegate.viewBinding

/**
 * 显示图片
 */
class PhotoDialog() : BaseDialogFragment(R.layout.dialog_photo_view) {

    constructor(src: String, sourceOrigin: String? = null) : this() {
        arguments = Bundle().apply {
            putString("src", src)
            putString("sourceOrigin", sourceOrigin)
        }
    }

    private val binding by viewBinding(DialogPhotoViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, 1f)
    }

    @SuppressLint("CheckResult")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val arguments = arguments ?: return
        arguments.getString("src")?.let { src ->
            ImageProvider.bitmapLruCache.get(src)?.let {
                binding.photoView.setImageBitmap(it)
                return
            }
            val file = com.squirrel.base.model.ReadBook.book?.let { book ->
                BookHelp.getImage(book, src)
            }
            if (file?.exists() == true) {
                ImageLoader.load(requireContext(), file)
                    .error(R.drawable.image_loading_error)
                    .into(binding.photoView)
            } else {
                ImageLoader.load(requireContext(), src).apply {
                    arguments.getString("sourceOrigin")?.let { sourceOrigin ->
                        apply(
                            RequestOptions().set(
                                OkHttpModelLoader.sourceOriginOption,
                                sourceOrigin
                            )
                        )
                    }
                }.error(BookCover.defaultDrawable)
                    .into(binding.photoView)
            }
        }
    }

}
