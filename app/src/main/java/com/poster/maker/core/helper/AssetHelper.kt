package poster.maker.core.helper

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import poster.maker.core.utils.key.AssetsKey
import poster.maker.core.utils.key.ValueKey
import poster.maker.data.model.custom.ColorModel
import poster.maker.data.model.custom.CustomizeModel
import poster.maker.data.model.custom.LayerListModel
import poster.maker.data.model.custom.LayerModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream


object AssetHelper {
    // Read sub folder
    fun getSubfoldersAsset(context: Context, path: String): ArrayList<String> {
        val allData = context.assets.list(path)
        val sortedData = MediaHelper.sortAsset(allData)?.map { "${AssetsKey.ASSET_MANAGER}/$path/$it" }?.toCollection(ArrayList())
        return sortedData ?: arrayListOf()
    }

    // Read sub folder
    fun getSubfoldersNotDomainAsset(context: Context, path: String): ArrayList<String> {
        val allData = context.assets.list(path)
        val sortedData = MediaHelper.sortAsset(allData)?.map { "${AssetsKey.DATA}/$it" }?.toCollection(ArrayList())
        return sortedData ?: arrayListOf()
    }

    // Read file txt -> json -> T
    inline fun <reified T> readJsonAsset(context: Context, path: String): T? {
        return try {
            val json = context.assets.open(path).bufferedReader().use { it.readText() }
            Gson().fromJson(json, T::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Read file txt -> json -> list
    inline fun <reified T> readTextToJsonAssets(context: Context, path: String): ArrayList<T> {
        return try {
            val json = context.assets.open(path).bufferedReader().use { it.readText() }
            val type = object : TypeToken<ArrayList<T>>() {}.type
            Gson().fromJson(json, type) ?: arrayListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            arrayListOf()
        }
    }

    // Read file -> bitmap
    fun getBitmapFromAsset(context: Context, fileName: String): Bitmap? {
        return try {
            context.assets.open(fileName).use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // file asset -> internal
    fun copyAssetToInternal(context: Context, fileName: String): File? {
        return try {
            val outFile = File(context.filesDir, fileName)
            outFile.parentFile?.mkdirs()

            if (!outFile.exists()) {
                context.assets.open(fileName).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            outFile
        } catch (e: Exception) {
            Log.e("nbhieu", "Copy asset failed: ${e.message}")
            null
        }
    }


    // ---------------------------------------------------------------------------------------------

    fun getDataFromAsset(context: Context) : ArrayList<CustomizeModel> {
        val start = System.currentTimeMillis()
        val customList = ArrayList<CustomizeModel>()
        val assetManager = context.assets

        // "character_1, character_2,..."
        val characterList = assetManager.list(AssetsKey.DATA)
        val sortedCharacter = MediaHelper.sortAsset(characterList)
        Log.d("nbhieu", "----------------------------------------------------------------------------------")

        sortedCharacter!!.forEach {
            Log.d("nbhieu", "sortedCharacter: $it")
        }

        Log.d("nbhieu", "----------------------------------------------------------------------------------")

        sortedCharacter.forEachIndexed { indexCharacter, character ->
            val layerListModelList = ArrayList<LayerListModel>()
            Log.d("nbhieu", "indexCharacter: $indexCharacter")
            // "1.30, 2.4, 3.1, 4.22,..."
            val layer = assetManager.list("${AssetsKey.DATA}/${character}")
            val sortedLayer = MediaHelper.sortAsset(layer)?.toCollection(ArrayList()) ?: arrayListOf()

            val avatar = "${AssetsKey.DATA_ASSET}${character}/${sortedLayer.last()}"
            sortedLayer.removeAt(sortedLayer.size - 1)
            Log.d("nbhieu", "avatar: $avatar")

            Log.d("nbhieu", "----------------------------------------------------------------------------------")

            for (i in 0 until sortedLayer.size) {
                // Tách 1 và 30 (1.30)
                val position = sortedLayer[i].split(AssetsKey.SPLIT_LAYER)
                val positionCustom = position[0].toInt() - 1
                val positionNavigation = position[1].toInt() - 1

                // Lấy folder màu hoặc lấy ảnh nếu không có màu, lấy ảnh navigation
                // data/character_1/1.30
                val folderOrImageList = assetManager.list("${AssetsKey.DATA}/${character}/${sortedLayer[i]}")
                val folderOrImageSortedList =
                    MediaHelper.sortAsset(folderOrImageList)?.toCollection(ArrayList()) ?: arrayListOf()
                //Lấy navigation
                val navigationImage =
                    "${AssetsKey.DATA_ASSET}${character}/${sortedLayer[i]}/${folderOrImageSortedList.last()}"
                folderOrImageSortedList.removeAt(folderOrImageSortedList.size - 1)
                // Nếu không có folder -> không có màu
                val layer = if (AssetsKey.FIRST_IMAGE.any { it in folderOrImageSortedList[0] }) {
                    getDataNoColor(character, folderOrImageSortedList, sortedLayer[i])
                } else {
                    getDataColor(assetManager, character, folderOrImageSortedList, sortedLayer[i])
                }
                val layerListModel = LayerListModel(positionCustom, positionNavigation, navigationImage, layer)
                layerListModelList.add(layerListModel)
            }
            layerListModelList.sortBy { it.positionNavigation }
            customList.add(CustomizeModel(character, avatar, layerListModelList))
            Log.d("nbhieu", "----------------------------------------------------------------------------------")
        }
        MediaHelper.writeListToFile(context, ValueKey.DATA_FILE_INTERNAL, customList)
        customList.forEach {
            Log.d("nbhieu", "customList: ${it}")
        }
        Log.d("nbhieu", "count time: ${System.currentTimeMillis() - start}")
        return customList
    }

    private fun getDataNoColor(character: String, filesList: List<String>, folder: String): ArrayList<LayerModel> {
        val layerPath = ArrayList<LayerModel>()
        for (fileName in filesList) {
            // file:///android_asset/nuggts/ + nuggts1 + body + 1.png
            layerPath.add(
                LayerModel(
                    image = "${AssetsKey.DATA_ASSET}$character/$folder/$fileName",
                    isMoreColors = false,
                    listColor = arrayListOf()
                )
            )
        }
        return layerPath
    }

    private fun getDataColor(
        assetManager: AssetManager, character: String, folderList: List<String>, folder: String
    ): ArrayList<LayerModel> {
        val colorNames = folderList.map { "#$it" }
        val fileList = folderList.map { colorFolder ->
            assetManager.list("${AssetsKey.DATA}/$character/$folder/$colorFolder")?.let {
                MediaHelper.sortAsset(
                    it
                )
            }?.map { "${AssetsKey.DATA_ASSET}$character/$folder/$colorFolder/$it" } ?: emptyList()
        }

        // Khởi tạo danh sách màu và ghép danh sách file theo index
        val colorList = Array(fileList.first().size) { index ->
            Array(folderList.size) { folderIndex ->
                ColorModel(color = colorNames[folderIndex], path = fileList[folderIndex][index])
            }.toCollection(ArrayList())
        }.toCollection(ArrayList())

        return fileList.first().mapIndexed { index, file ->
            LayerModel(image = file, isMoreColors = true, listColor = colorList[index])
        }.toCollection(ArrayList())
    }

    /**
     * Get template item (background) asset path for Glide
     * @param templateId: 1-16
     * @return: "file:///android_asset/template/{templateId}/item.png"
     */
    fun getTemplateItemPath(templateId: Int): String {
        return "file:///android_asset/template/$templateId/item.png"
    }

    /**
     * Get template avatar (default avatar) asset path for Glide
     * @param templateId: 1-16
     * @return: "file:///android_asset/template/{templateId}/avatar.png"
     */
    fun getTemplateAvatarPath(templateId: Int): String {
        return "file:///android_asset/template/$templateId/avatar.png"
    }

    /**
     * Get sticker category thumbnail (first sticker image) for navigation
     * @param categoryId: 1-16
     * @return: "file:///android_asset/sticker/{categoryId}/1.png"
     */
    fun getStickerCategoryThumbnail(categoryId: Int): String {
        return "file:///android_asset/sticker/$categoryId/1.png"
    }

    /**
     * Get all stickers in a specific category
     * @param context: Android context
     * @param categoryId: 1-16
     * @return: ArrayList of sticker paths
     */
    fun getStickersByCategory(context: Context, categoryId: Int): ArrayList<String> {
        val categoryPath = "sticker/$categoryId"
        val allFiles = context.assets.list(categoryPath)
        val sortedFiles = MediaHelper.sortAsset(allFiles)
            ?.filter { !it.contains("Thumbs.db") && (it.endsWith(".png") || it.endsWith(".webp") || it.endsWith(".jpg")) }
            ?.map { "file:///android_asset/sticker/$categoryId/$it" }
            ?.toCollection(ArrayList())
        return sortedFiles ?: arrayListOf()
    }

    /**
     * Get all sticker category IDs (1-16)
     * @return: ArrayList of category IDs
     */
    fun getAllStickerCategories(): ArrayList<Int> {
        return (1..16).toCollection(ArrayList())
    }

    /**
     * Get specific sticker path
     * @param categoryId: 1-16
     * @param fileName: sticker file name (e.g., "1.png")
     * @return: "file:///android_asset/sticker/{categoryId}/{fileName}"
     */
    fun getStickerPath(categoryId: Int, fileName: String): String {
        return "file:///android_asset/sticker/$categoryId/$fileName"
    }
}