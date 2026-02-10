package poster.maker.core.custom.layout

import android.widget.ImageView
import poster.maker.core.custom.imageview.StrokeImageView

interface EventRatioFrame {
    fun onImageClick(image: StrokeImageView, btnEdit: ImageView)
}