package poster.maker.core.extensions

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import poster.maker.R
import poster.maker.core.helper.LanguageHelper
import poster.maker.dialog.YesNoDialog


fun Context.checkPermissions(listPermission: Array<String>): Boolean {
    return listPermission.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}

fun Activity.requestPermission(permissions: Array<String>, requestCode: Int) {
    ActivityCompat.requestPermissions(this, permissions, requestCode)
}

fun Activity.goToSettings() {
    val dialog = YesNoDialog(this, R.string.permission, R.string.go_to_setting_message)
    LanguageHelper.setLocale(this)
    dialog.show()

    dialog.onNoClick = {
        dialog.dismiss()
        hideNavigation()
    }
    dialog.onYesClick = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${this@goToSettings.packageName}".toUri()
        }
        this.startActivity(intent)
        dialog.dismiss()
        hideNavigation()
    }
}
