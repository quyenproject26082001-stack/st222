package poster.maker.core.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import poster.maker.R
import poster.maker.core.utils.key.ValueKey
import poster.maker.core.utils.state.HandleState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URL
import androidx.core.graphics.scale
import poster.maker.core.utils.state.SaveState
import kotlinx.coroutines.flow.flowOn

object MediaHelper {
    // Sort file (folder)
    fun sortAsset(listFiles: Array<String>?): List<String>? {
        val sortedFiles = listFiles?.sortedWith(compareBy { fileName ->
            val matchResult = Regex("\\d+").find(fileName)
            matchResult?.value?.toIntOrNull() ?: Int.MAX_VALUE
        })
        return sortedFiles
    }

    // Get file from internal
    fun getImageInternal(context: Context, album: String): ArrayList<String> {
        val imagePaths = ArrayList<String>()
        val targetDir = File(context.filesDir, album)

        if (targetDir.exists() && targetDir.isDirectory) {
            targetDir.listFiles()?.filter { isImageFile(it) }
                ?.sortedByDescending { it.lastModified() }
                ?.forEach { file ->
                    imagePaths.add(file.absolutePath)
                }
        }
        return imagePaths
    }

    // is file?
    fun isImageFile(file: File): Boolean {
        val imageExtensions = listOf("jpg", "jpeg", "png", "bmp", "webp")
        val extension = file.extension.lowercase()
        return file.isFile && imageExtensions.contains(extension)
    }

    fun deleteFileByPath(pathList: ArrayList<String>): Flow<HandleState> = flow {
        emit(HandleState.LOADING)
        try {
            for (i in 0 until pathList.size) {
                val file = File(pathList[i])
                if (file.exists()) {
                    file.delete()
                }
            }
            emit(HandleState.SUCCESS)
        } catch (e: Exception) {
            emit(HandleState.FAIL)
        }
    }.flowOn(Dispatchers.IO)


    suspend fun downloadVideoCompat(context: Context, videoUrl: String): HandleState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadUsingMediaStore(context, videoUrl)
        } else {
            downloadUsingDownloadManager(context, videoUrl)
        }
    }

    private suspend fun downloadUsingMediaStore(
        context: Context, videoUrl: String
    ): HandleState {
        return try {
            val resolver = context.contentResolver
            val fileName = StringHelper.generateRandomVideoFileName()
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + "/" + ValueKey.DOWNLOAD_ALBUM
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val videoUri =
                resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return HandleState.FAIL

            URL(videoUrl).openStream().use { input ->
                resolver.openOutputStream(videoUri)?.use { output ->
                    input.copyTo(output)
                } ?: return HandleState.FAIL
            }

            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(videoUri, contentValues, null, null)
            HandleState.SUCCESS
        } catch (e: Exception) {
            Log.e("nbhieu", "downloadUsingMediaStore: ${e.message}")
            HandleState.FAIL
        }
    }


    private suspend fun downloadUsingDownloadManager(
        context: Context, videoUrl: String
    ): HandleState {
        return try {
            val fileName = StringHelper.generateRandomVideoFileName()
            val request = DownloadManager.Request(videoUrl.toUri()).apply {
                setTitle(context.getString(R.string.downloading))
                setDescription(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MOVIES, "/${ValueKey.DOWNLOAD_ALBUM}/$fileName"
                )
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            }

            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            HandleState.SUCCESS
        } catch (e: Exception) {
            HandleState.FAIL
        }
    }

    inline fun <reified T> writeListToFile(context: Context, fileName: String, list: List<T>) {
        try {
            val json = Gson().toJson(list)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(json.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inline fun <reified T> readListFromFile(context: Context, fileName: String): List<T> {
        return try {
            val json = context.openFileInput(fileName).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<T>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: FileNotFoundException) {
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun checkFileInternal(context: Context, fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return file.exists() || file.length() > 0
    }

    suspend fun downloadVideoToCache(context: Context, videoUrl: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = context.cacheDir


                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".mp4")) {
                        file.delete()
                    }
                }

                val fileName = "wallpaper_${System.currentTimeMillis()}.mp4"
                val file = File(cacheDir, fileName)

                val url = URL(videoUrl)
                url.openStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    suspend fun Activity.saveVideoToInternalStorage(album: String, videoUrl: String): String? {
        val fileName = StringHelper.generateRandomVideoFileName()

        return try {
            val directory = File(filesDir, album)
            if (!directory.exists()) {
                directory.mkdir()
            }

            val file = File(directory, fileName)

            val url = URL(videoUrl)
            url.openStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun Activity.saveBitmapToCache(bitmap: Bitmap): File {
        val cachePath = File(cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "shared_image.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    fun saveBitmapToInternalStorage(
        context: Context,
        album: String,
        bitmap: Bitmap
    ): Flow<SaveState> = flow {
        emit(SaveState.Loading)

        try {
            val name = StringHelper.generateRandomImageFileName()
            val directory = File(context.filesDir, album)

            if (!directory.exists()) {
                directory.mkdir()
            }

            val file = File(directory, name)

            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                output.flush()
            }

            bitmap.recycle()

            emit(SaveState.Success(file.absolutePath))
        } catch (e: Exception) {
            emit(SaveState.Error(e))
        }
    }.flowOn(Dispatchers.IO)


    fun Activity.saveBitmapToInternalStorage(
        album: String,
        bitmap: Bitmap,
        nameInput: Int
    ): String? {
        val name = "$nameInput.png"

        return try {
            val directory = File(filesDir, album)
            if (!directory.exists()) {
                directory.mkdir()
            }

            val file = File(directory, name)

            val fileOutputStream = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)

            fileOutputStream.flush()
            fileOutputStream.close()

            bitmap.recycle()
            file.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun Activity.saveBitmapToInternalStorageZip(bitmap: Bitmap): String? {
        val name = StringHelper.generateRandomImageFileName()
        // Giảm kích thước ảnh xuống 512x512 px
        val resizedBitmap = bitmap.scale(512, 512)

        return try {
            val directory = File(filesDir, ValueKey.DOWNLOAD_ALBUM)

            if (!directory.exists()) {
                directory.mkdir()
            }

            val file = File(directory, "$name.png")

            val fileOutputStream = FileOutputStream(file)

            var quality = 100
            do {
                fileOutputStream.flush()
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, quality, fileOutputStream)
                quality -= 5 // Giảm chất lượng sau mỗi lần nén
            } while (file.length() > 512 * 1024 && quality > 5) // 512 KB và chất lượng không dưới 5%

            fileOutputStream.flush()
            fileOutputStream.close()

            resizedBitmap.recycle()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun downloadPartsToExternal(activity: Activity, pathList: List<String>): Flow<HandleState> =
        flow {
            emit(HandleState.LOADING)

            if (pathList.isEmpty()) {
                emit(HandleState.FAIL)
                return@flow
            }

            val bitmapList = BitmapHelper.convertPathsToBitmaps(activity, pathList)

            if (bitmapList.size == 1) {
                emitAll(saveBitmapToExternal(activity, bitmapList.first()))
            } else {
                var allSuccess = true
                for (bitmap in bitmapList) {
                    val state = saveBitmapToExternal(activity, bitmap).last()
                    if (state == HandleState.FAIL) {
                        allSuccess = false
                        break
                    }
                }
                emit(if (allSuccess) HandleState.SUCCESS else HandleState.FAIL)
            }
        }

    // bitmap -> external storage
    fun saveBitmapToExternal(activity: Activity, bitmap: Bitmap): Flow<HandleState> = flow {
        emit(HandleState.LOADING)

        val state = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+: MUST use MediaStore (Scoped Storage)
                    saveBitmapViaMediaStore(activity, bitmap)
                } else {
                    // Android 8-9: Try MediaStore first, fallback to FileOutputStream for buggy OEMs
                    try {
                        saveBitmapViaMediaStore(activity, bitmap)
                    } catch (e: Exception) {
                        Log.e("MediaHelper", "MediaStore failed on Android ${Build.VERSION.SDK_INT}, trying FileOutputStream fallback", e)
                        // Fallback for Oppo/Vivo ColorOS that blocks MediaStore.DATA
                        saveBitmapViaFileOutputStream(activity, bitmap)
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaHelper", "saveBitmapToExternal failed", e)
                e.printStackTrace()
                HandleState.FAIL
            }
        }

        emit(state)
    }

    /**
     * Save bitmap using MediaStore API (works on all Android versions)
     */
    private fun saveBitmapViaMediaStore(activity: Activity, bitmap: Bitmap): HandleState {
        return try {
            val resolver = activity.contentResolver
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "image_${System.currentTimeMillis()}.png"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "Pictures/${ValueKey.DOWNLOAD_ALBUM}"
                    )
                } else {
                    val directory = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        ValueKey.DOWNLOAD_ALBUM
                    )
                    if (!directory.exists()) {
                        val created = directory.mkdirs()
                        if (!created) {
                            Log.e("MediaHelper", "Failed to create directory: ${directory.absolutePath}")
                            throw Exception("Failed to create directory")
                        }
                    }
                    val filePath =
                        File(directory, "image_${System.currentTimeMillis()}.png").absolutePath
                    put(MediaStore.Images.Media.DATA, filePath)
                }
            }

            val imageUri = resolver.insert(imageCollection, contentValues)
                ?: throw Exception("Failed to insert into MediaStore")

            resolver.openOutputStream(imageUri)?.use { outputStream ->
                val isSaved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                if (isSaved) {
                    Log.d("MediaHelper", "Image saved via MediaStore: $imageUri")
                    HandleState.SUCCESS
                } else {
                    throw Exception("Bitmap compress failed")
                }
            } ?: throw Exception("Failed to open output stream")

        } catch (e: Exception) {
            Log.e("MediaHelper", "saveBitmapViaMediaStore failed", e)
            throw e
        }
    }

    /**
     * Fallback method using direct FileOutputStream for Android 8-9
     * Used when MediaStore fails on buggy OEMs (Oppo/Vivo ColorOS)
     * Note: This does NOT work on Android 10+ due to Scoped Storage
     */
    private fun saveBitmapViaFileOutputStream(activity: Activity, bitmap: Bitmap): HandleState {
        return try {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                ValueKey.DOWNLOAD_ALBUM
            )

            // Check if directory exists or can be created
            if (!directory.exists()) {
                val created = directory.mkdirs()
                if (!created) {
                    Log.e("MediaHelper", "Failed to create directory: ${directory.absolutePath}")
                    return HandleState.FAIL
                }
                Log.d("MediaHelper", "Created directory: ${directory.absolutePath}")
            }

            val file = File(directory, "image_${System.currentTimeMillis()}.png")

            // Save file directly using FileOutputStream
            FileOutputStream(file).use { output ->
                val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                if (!success) {
                    Log.e("MediaHelper", "Bitmap compress failed")
                    return HandleState.FAIL
                }
            }

            Log.d("MediaHelper", "Image saved via FileOutputStream: ${file.absolutePath}")

            // IMPORTANT: Scan file into Gallery so it appears immediately
            android.media.MediaScannerConnection.scanFile(
                activity,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )

            HandleState.SUCCESS
        } catch (e: Exception) {
            Log.e("MediaHelper", "saveBitmapViaFileOutputStream failed", e)
            e.printStackTrace()
            HandleState.FAIL
        }
    }

    // get image external storage
    @SuppressLint("Recycle")
    fun getAllImages(context: Context): List<Uri> {
        val images = mutableListOf<Uri>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val query = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                images.add(contentUri)
            }
        }

        return images
    }

}