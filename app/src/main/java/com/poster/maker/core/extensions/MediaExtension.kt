package poster.maker.core.extensions

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import poster.maker.core.utils.key.RequestKey
import kotlin.collections.forEach


fun AppCompatActivity.deleteTempDataFolder(folder: String) {
    lifecycleScope.launch(Dispatchers.IO) {
        val targetDir = File(filesDir, folder)
        if (targetDir.exists() && targetDir.isDirectory) {
            val listFile = targetDir.listFiles()?.toCollection(ArrayList())
            listFile?.let {
                listFile.forEach {
                    it.delete()
                }
            }
        }
//        val dataTemp = getImageInternal(context, folder)
//        if (dataTemp.isNotEmpty()) {
//            dataTemp.forEach {
//                val file = File(it)
//                file.delete()
//            }
//        }
    }
}

fun Activity.openImagePicker() {
    val intent = Intent(Intent.ACTION_PICK)
    intent.type = "image/*"
    startActivityForResult(intent, RequestKey.PICK_IMAGE_REQUEST_CODE)
}



