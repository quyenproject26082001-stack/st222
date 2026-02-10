package poster.maker.activity_app.intro

import android.content.Context
import poster.maker.core.base.BaseAdapter
import poster.maker.core.extensions.loadImageGlide
import poster.maker.core.extensions.select
import poster.maker.core.extensions.setTextContent
import poster.maker.core.extensions.strings
import poster.maker.data.model.IntroModel
import poster.maker.databinding.ItemIntroBinding

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