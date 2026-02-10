package poster.maker.core.utils

import androidx.lifecycle.MutableLiveData
import poster.maker.R
import poster.maker.core.custom.layout.LayoutPresets
import poster.maker.data.model.IntroModel
import poster.maker.data.model.LanguageModel
import poster.maker.data.model.custom.CustomizeModel
import com.facebook.shimmer.Shimmer
import poster.maker.core.utils.key.NavigationLayerKey
import poster.maker.data.model.custom.NavigationModel

object DataLocal {
    val shimmer =
        Shimmer.AlphaHighlightBuilder().setDuration(1800).setBaseAlpha(0.7f).setHighlightAlpha(0.6f)
            .setDirection(Shimmer.Direction.LEFT_TO_RIGHT).setAutoStart(true).build()

    var lastClickTime = 0L
    var currentDate = ""
    var isConnectInternet = MutableLiveData<Boolean>()
    var isFailBaseURL = false
    var isCallDataAlready = false

    fun getLanguageList(): ArrayList<LanguageModel> {
        return arrayListOf(
            LanguageModel("hi", "Hindi", R.drawable.ic_flag_hindi),
            LanguageModel("es", "Spanish", R.drawable.ic_flag_spanish),
            LanguageModel("fr", "French", R.drawable.ic_flag_french),
            LanguageModel("en", "English", R.drawable.ic_flag_english),
            LanguageModel("pt", "Portuguese", R.drawable.ic_flag_portugeese),
            LanguageModel("in", "Indonesian", R.drawable.ic_flag_indo),
            LanguageModel("de", "German", R.drawable.ic_flag_germani),
        )
    }

    val itemIntroList = listOf(
        IntroModel(R.drawable.img_intro_1, R.string.title_1),
        IntroModel(R.drawable.img_intro_2, R.string.title_2),
        IntroModel(R.drawable.img_intro_3, R.string.title_3)
    )
}