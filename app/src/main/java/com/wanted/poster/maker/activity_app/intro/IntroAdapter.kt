package com.wanted.poster.maker.activity_app.intro

import android.content.Context
import com.wanted.poster.maker.core.base.BaseAdapter
import com.wanted.poster.maker.core.extensions.loadImageGlide
import com.wanted.poster.maker.core.extensions.select
import com.wanted.poster.maker.core.extensions.setTextContent
import com.wanted.poster.maker.core.extensions.strings
import com.wanted.poster.maker.data.model.IntroModel
import com.wanted.poster.maker.databinding.ItemIntroBinding

class IntroAdapter(val context: Context) : BaseAdapter<IntroModel, ItemIntroBinding>(
    ItemIntroBinding::inflate
) {
    override fun onBind(binding: ItemIntroBinding, item: IntroModel, position: Int) {
        binding.apply {
            loadImageGlide(root, item.image, imvImage, false)
            tvContent.text = context.strings(item.content)
            tvContent.select()
        }
    }
}