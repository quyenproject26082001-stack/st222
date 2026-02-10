package poster.maker.core.helper

import android.Manifest
import android.os.Build

object PermissionHelper {

    val storagePermission = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> emptyArray()

        else -> arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

    val cameraPermission = arrayOf(Manifest.permission.CAMERA)
}