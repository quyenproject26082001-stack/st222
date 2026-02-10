package poster.maker.data.model.custom

data class SuggestionModel (
    val avatarPath: String = "",
    val positionColorItemList : ArrayList<Int> = arrayListOf(),
    val itemNavList : ArrayList<ArrayList<ItemNavCustomModel>> = arrayListOf(),
    var colorItemNavList : ArrayList<ArrayList<ItemColorModel>> = arrayListOf(),
    val isSelectedItemList : ArrayList<Boolean> = arrayListOf(),
    val keySelectedItemList : ArrayList<String> = arrayListOf(),
    val isShowColorList : ArrayList<Boolean> = arrayListOf(),
    val pathSelectedList : ArrayList<String> = arrayListOf(),
)