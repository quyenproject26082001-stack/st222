package com.wanted.poster.maker.data.model.custom

import com.wanted.poster.maker.data.model.custom.ColorModel

data class LayerModel(
    val image: String,
    val isMoreColors: Boolean = false,
    var listColor: ArrayList<ColorModel> = arrayListOf()
)