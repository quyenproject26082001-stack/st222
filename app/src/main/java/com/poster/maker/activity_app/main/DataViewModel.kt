package poster.maker.activity_app.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import poster.maker.core.helper.AssetHelper
import poster.maker.core.helper.InternetHelper
import poster.maker.core.helper.MediaHelper
import poster.maker.core.service.RetrofitClient
import poster.maker.core.service.RetrofitPreventive
import poster.maker.core.utils.DataLocal.isFailBaseURL
import poster.maker.core.utils.key.AssetsKey
import poster.maker.core.utils.key.DomainKey
import poster.maker.core.utils.key.ValueKey
import poster.maker.core.utils.state.HandleState
import poster.maker.data.model.DataAPI
import poster.maker.data.model.PartAPI
import poster.maker.data.model.custom.ColorModel
import poster.maker.data.model.custom.CustomizeModel
import poster.maker.data.model.custom.LayerListModel
import poster.maker.data.model.custom.LayerModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.collections.forEachIndexed

class DataViewModel() : ViewModel() {
    private val _allData = MutableStateFlow<ArrayList<CustomizeModel>>(arrayListOf())
    val allData: StateFlow<ArrayList<CustomizeModel>> = _allData.asStateFlow()
    private val _getDataAPI = MutableLiveData<List<PartAPI>>()
    val getDataAPI: LiveData<List<PartAPI>> get() = _getDataAPI

    fun saveAndReadData(context: Context) {
        viewModelScope.launch {
            val timeStart = System.currentTimeMillis()
            val list = withContext(Dispatchers.IO) {
                // Lần đầu vào app -> Load data Asset -> Lưu file internal
                if (!MediaHelper.checkFileInternal(context, ValueKey.DATA_FILE_INTERNAL)) {
                    AssetHelper.getDataFromAsset(context)
                }

                val totalData = MediaHelper.readListFromFile<CustomizeModel>(context, ValueKey.DATA_FILE_INTERNAL)
                    .toCollection(ArrayList())
                var dataApi = MediaHelper.readListFromFile<CustomizeModel>(context, ValueKey.DATA_FILE_API_INTERNAL)
                    ?: arrayListOf()
                if (dataApi.isEmpty() && InternetHelper.checkInternet(context)) {
                    getAllParts(context).collect { state ->
                        when (state) {
                            HandleState.LOADING -> {}
                            HandleState.SUCCESS -> {
                                dataApi = MediaHelper.readListFromFile<CustomizeModel>(context, ValueKey.DATA_FILE_API_INTERNAL)
                            }

                            else -> {}
                        }
                    }
                }
                totalData.addAll(dataApi)
                totalData
            }
            _allData.value = list
            val timeEnd = System.currentTimeMillis()
            Log.d("nbhieu", "time load data: ${timeEnd - timeStart}")
        }
    }

    fun ensureData(context: Context) {
        if (_allData.value.isEmpty()) {
            saveAndReadData(context)
        }
    }

    fun getAllParts(context: Context): Flow<HandleState> = flow {
        Log.d("nbhieu", "API Calling...")
        emit(HandleState.LOADING)

        val response = withTimeoutOrNull(5_000) {
            try {
                RetrofitClient.api.getAllData()
            } catch (e: Exception) {
                Log.e("nbhieu", "BASE_URL failed: ${e.message}")
                null
            }
        } ?: withTimeoutOrNull(5_000) {
            try {
                RetrofitPreventive.api.getAllData()
            } catch (e: Exception) {
                Log.e("nbhieu", "BASE_URL_PREVENTIVE failed: ${e.message}")
                null
            }
        }

        if (response != null && response.isSuccessful && response.body() != null) {
            val dataMap = ArrayList<DataAPI>()
            response.body()?.forEach { (key, dataBody) ->
                dataMap.add(DataAPI(key, dataBody))
            }
            withContext(Dispatchers.IO) {
                getDataAPI(context, dataMap)
            }
            emit(HandleState.SUCCESS)
        } else {
            val file = File(context.filesDir, ValueKey.DATA_FILE_API_INTERNAL)
            if (file.exists()) file.delete()
            emit(HandleState.FAIL)
        }
    }

    fun getDataAPI(context: Context, dataList: ArrayList<DataAPI>) {
        val allDataAPI: ArrayList<CustomizeModel> = arrayListOf()
        // Character 1, Character 2,...
        dataList.forEachIndexed { indexCharacter, data ->
            ///public/app/ChibiMaker/1/avatar.png
            val baseDomain = if (!isFailBaseURL) DomainKey.BASE_URL else DomainKey.BASE_URL_PREVENTIVE
            val avatarCharacter = "$baseDomain${DomainKey.SUB_DOMAIN}/${data.name}/${DomainKey.AVATAR_CHARACTER_API}"
            val layerList = ArrayList<LayerListModel>(data.parts.size)

            data.parts.forEachIndexed { indexLayer, dataLayer ->
                val layerName = dataLayer.parts.split(AssetsKey.SPLIT_LAYER)
                val positionCustom = layerName.first().toInt() - 1
                val positionNavigation = layerName.last().toInt() - 1
                val imageNavigation = "${baseDomain}${DomainKey.SUB_DOMAIN}/${data.name}/${dataLayer.parts}/${DomainKey.IMAGE_NAVIGATION}"
                val layer = getDataLayer(baseDomain, dataLayer, dataLayer.parts)

                val layerListModel = LayerListModel(
                    positionCustom = positionCustom,
                    positionNavigation = positionNavigation,
                    imageNavigation = imageNavigation,
                    layer = layer
                )
                layerList.add(layerListModel)
            }
            layerList.sortBy { it.positionNavigation }

            val dataApi = CustomizeModel(
                dataName = data.name,
                avatar = avatarCharacter,
                layerList = layerList
            )
            allDataAPI.add(dataApi)
        }
        MediaHelper.writeListToFile(context, ValueKey.DATA_FILE_API_INTERNAL, allDataAPI)
        allDataAPI.forEach {
            Log.d("nbhieu", "avatar: ${it.avatar}")
        }
    }

    private fun getDataLayer(baseDomain: String, partData: PartAPI, layer: String): ArrayList<LayerModel> {
        return if (partData.colorArray != "" || partData.colorArray.isNotEmpty()) {
            getDataAPIColor(baseDomain, partData, layer)
        } else {
            getDataAPINoColor(baseDomain, partData, layer)
        }
    }

    private fun getDataAPINoColor(baseDomain: String, part: PartAPI, layer: String): ArrayList<LayerModel> {
        val layerPath = ArrayList<LayerModel>(part.quantity)
        val prefix = "$baseDomain${DomainKey.SUB_DOMAIN}/${part.position}/${layer}/"
        val suffix = DomainKey.LAYER_EXTENSION
        for (i in 1..part.quantity) {
            layerPath.add(
                LayerModel(
                    "$prefix${i}$suffix",
                    false,
                    arrayListOf()
                )
            )
        }
        return layerPath
    }

    private fun getDataAPIColor(baseDomain: String, part: PartAPI, layer: String): ArrayList<LayerModel> {
        val layerPath = ArrayList<LayerModel>(part.quantity)
        val getColorCode = part.colorArray.split(",")
        val prefix = "$baseDomain${DomainKey.SUB_DOMAIN}/${part.position}/${layer}/"
        val suffix = DomainKey.LAYER_EXTENSION

        for (i in 1..part.quantity) {
            val listColor = ArrayList<ColorModel>(getColorCode.size)
            for (j in 0 until getColorCode.size) {
                listColor.add(
                    ColorModel(
                        "#${getColorCode[j]}",
                        "$prefix${getColorCode[j]}/${i}$suffix"
                    )
                )
            }
            layerPath.add(LayerModel(listColor.first().path, true, listColor))
        }
        return layerPath
    }
}