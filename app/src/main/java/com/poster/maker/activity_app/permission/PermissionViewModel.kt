package poster.maker.activity_app.permission

import androidx.lifecycle.ViewModel
import poster.maker.core.helper.PermissionHelper
import poster.maker.core.helper.SharePreferenceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PermissionViewModel : ViewModel() {

    private val _storageGranted = MutableStateFlow(false)
    val storageGranted: StateFlow<Boolean> = _storageGranted

    private val _notificationGranted = MutableStateFlow(false)
    val notificationGranted: StateFlow<Boolean> = _notificationGranted

    // ✅ BỎ SESSION COUNTER - Luôn hỏi quyền cho đến khi "Don't ask again"

    fun updateStorageGranted(sharePrefer: SharePreferenceHelper, granted: Boolean) {
        _storageGranted.value = granted
        sharePrefer.setStoragePermission(if (granted) 0 else sharePrefer.getStoragePermission() + 1)

        // Không còn counter nữa
    }

    fun updateNotificationGranted(sharePrefer: SharePreferenceHelper, granted: Boolean) {
        _notificationGranted.value = granted
        sharePrefer.setNotificationPermission(if (granted) 0 else sharePrefer.getNotificationPermission() + 1)
    }

    // ✅ CHECK COUNTER THAY VÌ FLAG "Don't ask again"
    fun needGoToSettings(sharePrefer: SharePreferenceHelper, storage: Boolean): Boolean {
        return if (storage) {
            sharePrefer.getStoragePermission() > 2 && !_storageGranted.value
        } else {
            sharePrefer.getNotificationPermission() > 2 && !_notificationGranted.value
        }
    }
    // ✅ THÊM LẠI COUNTER


    fun getStoragePermissions() = PermissionHelper.storagePermission
    fun getNotificationPermissions() = PermissionHelper.notificationPermission
}