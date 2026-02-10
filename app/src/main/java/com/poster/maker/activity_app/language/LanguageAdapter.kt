package poster.maker.activity_app.language

import android.annotation.SuppressLint
import android.content.Context
import poster.maker.R
import poster.maker.core.base.BaseAdapter
import poster.maker.core.extensions.gone
import poster.maker.core.extensions.loadImageGlide
import poster.maker.core.extensions.setOnSingleClick
import poster.maker.core.extensions.visible
import poster.maker.data.model.LanguageModel
import poster.maker.databinding.ItemLanguageBinding

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
                imvFocus.visible()
            } else {
                loadImageGlide(root, R.drawable.ic_not_tick_lang, btnRadio, false)
                imvFocus.gone()
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