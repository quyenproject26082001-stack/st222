package com.wanted.poster.maker.activity_app.language

import android.annotation.SuppressLint
import android.content.Context
import com.wanted.poster.maker.R
import com.wanted.poster.maker.core.base.BaseAdapter
import com.wanted.poster.maker.core.extensions.gone
import com.wanted.poster.maker.core.extensions.loadImageGlide
import com.wanted.poster.maker.core.extensions.setOnSingleClick
import com.wanted.poster.maker.core.extensions.visible
import com.wanted.poster.maker.data.model.LanguageModel
import com.wanted.poster.maker.databinding.ItemLanguageBinding

class LanguageAdapter(val context: Context) : BaseAdapter<LanguageModel, ItemLanguageBinding>(
    ItemLanguageBinding::inflate
) {
    var onItemClick: ((String) -> Unit) = {}
    override fun onBind(
        binding: ItemLanguageBinding, item: LanguageModel, position: Int
    ) {
        binding.apply {
            loadImageGlide(root, item.flag, imvFlag, false)
            tvLang.text = item.name

            if (item.activate) {
                loadImageGlide(root, R.drawable.ic_tick_lang, btnRadio, false)
                imvFocus.setBackgroundResource(R.drawable.frame_language_slt)
            } else {
                loadImageGlide(root, R.drawable.ic_not_tick_lang, btnRadio, false)
                imvFocus.setBackgroundResource(R.drawable.bg_100_stroke_white)
            }

            root.setOnSingleClick {
                onItemClick.invoke(item.code)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItem(position: Int) {
        items.forEach { it.activate = false }
        items[position].activate = true
        notifyDataSetChanged()
    }
}