package poster.maker.data.model

import android.os.Parcelable

data class PartAPI(
    val position: String,
    val parts: String,
    val colorArray: String,
    val quantity: Int
)

data class DataAPI(val name: String, val parts: List<PartAPI>)
