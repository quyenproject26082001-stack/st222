package poster.maker.activity_app.mycreation

import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.lvt.ads.util.Admob
import poster.maker.R
import poster.maker.activity_app.editsticker.EditStickerActivity
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.*
import poster.maker.core.helper.MediaHelper
import poster.maker.core.helper.PermissionHelper
import poster.maker.core.utils.state.HandleState
import poster.maker.databinding.ActivityViewBinding
import poster.maker.dialog.YesNoDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ViewCreationActivity : BaseActivity<ActivityViewBinding>() {

    private var imagePath: String? = null

    private var isMyDesign: Boolean = false
    private var downloadPermissionDeniedCount = 0

    // Permission launcher for Android 8-9
    // ✅ BỎ COUNTER - Luôn hỏi quyền cho đến khi "Don't ask again"
    // Permission launcher for Android 8-9
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }

        if (allGranted) {
            // ✅ Granted: Reset counter và proceed download
            downloadPermissionDeniedCount = 0
            proceedDownload()
        } else {
            // ❌ Denied: Tăng counter
            downloadPermissionDeniedCount++

            // Check if "Don't ask again" was clicked
            val canAskAgain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                true
            }

            if (!canAskAgain) {
                // 🚫 User clicked "Don't ask again" → Hiện dialog Settings
                goToSettings()
            } else if (downloadPermissionDeniedCount >= 2) {
                // ✅ Từ chối 2 lần → Hiện dialog Settings
            } else {
                // Show error toast
                Toast.makeText(
                    this,
                    strings(R.string.download_failed_please_try_again_later),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun setViewBinding(): ActivityViewBinding {
        return ActivityViewBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        imagePath = intent.getStringExtra("imagePath")

        isMyDesign = intent.getBooleanExtra("isMyDesign", false)

        imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))  // ✅ Dùng timestamp
                    .into(binding.imgPoster)
            }
        }

        if (isMyDesign) {
            binding.actionBar.btnActionBarNextToRight.visible()
        } else {
            binding.actionBar.btnActionBarNextToRight.gone()
        }
    }

    override fun onResume() {
        super.onResume()

        imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))  // ✅ Dùng timestamp
                    .into(binding.imgPoster)
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            // Left button - Back
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()

            // Right button - Delete
            btnActionBarRight.setImageResource(R.drawable.ic_delete)
            btnActionBarRight.visible()

            // Hide other elements
            tvCenter.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
            cvLogo.gone()
        }
    }

    override fun viewListener() {
        binding.apply {
            // Back button
            actionBar.btnActionBarLeft.setOnSingleClick {
                showInterAll { finishAfterTransition() }
            }

            // Delete button
            actionBar.btnActionBarRight.setOnSingleClick {
                showDeleteConfirmation()
            }

            // Share button
            btnShare.setOnSingleClick(2000) {
                shareImage()
            }

            // Download button
            btnDownload.setOnSingleClick {
                if (!isMyDesign) {
                    showInterAll { downloadImage() }
                }
                else{
                    downloadImage()
                }
            }
            actionBar.btnActionBarNextToRight.setOnSingleClick {
                openEditSticker()
            }
        }

    }

    private fun openEditSticker() {
        imagePath?.let { path ->
            val intent = Intent(this, EditStickerActivity::class.java).apply {
                putExtra("IMAGE_PATH", path)
                putExtra("IS_EDITING_EXISTING", true)
            }
            showInterAll { startActivity(intent) }
        }
    }

    override fun dataObservable() {
        // No data observables needed
    }

    private fun showDeleteConfirmation() {
        val dialog = YesNoDialog(
            context = this,
            title = R.string.delete,
            description = R.string.delete_design_confirmation
        )
        dialog.onYesClick = {
            deleteImage()
            dialog.dismiss()
        }
        dialog.onNoClick = {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun deleteImage() {
        imagePath?.let { path ->
            val file = File(path)
            if (file.exists() && file.delete()) {
                Toast.makeText(this, R.string.design_deleted, Toast.LENGTH_SHORT).show()
                finishAfterTransition()
            } else {
                Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareImage() {
        imagePath?.let { path ->
            val paths = arrayListOf(path)
            shareImagesPaths(paths)
        }
    }

    // ✅ BỎ COUNTER - Luôn hỏi quyền cho đến khi "Don't ask again"
    fun downloadImage() {
        // Android 10+: No permission needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            proceedDownload()
            return
        }

        // Android 8-9: Need permission
        val storagePermissions = PermissionHelper.storagePermission

        if (checkPermissions(storagePermissions)) {
            // Đã có quyền → Download
            proceedDownload()
        } else if (downloadPermissionDeniedCount >= 2) {
            // Đã từ chối 2 lần → Hiện dialog Settings
            goToSettings()
        } else {
            // Hỏi quyền lần đầu hoặc lần 2
            permissionLauncher.launch(storagePermissions)
        }
    }

    override fun onRestart() {
        super.onRestart()
//        if (isMyDesign) {
////            Admob.getInstance().loadNativeCollapNotBanner(
////                this,
////                getString(R.string.native_cl_Old_West_detail),
////                binding.nativeCollapDetailDesgin
////            )
//        }
    }

//    override fun initAds() {
//
//        if (isMyDesign) {
//           binding.nativeDetail.gone()
//            Admob.getInstance().loadNativeCollapNotBanner(
//                this,
//                getString(R.string.native_cl_Old_West_detail),
//                binding.nativeCollapDetailDesgin
//            )
//        } else {
//            binding.nativeDetail.visible()
//
//            Admob.getInstance().loadNativeAd(
//                this,
//                getString(R.string.native_detail),
//                binding.nativeDetail,
//                R.layout.ads_native_big_btn_top
//            )
//        }
//    }


    /**
     * Proceed with download after permission check
     */
    private fun proceedDownload() {
        imagePath?.let { path ->
            lifecycleScope.launch {
                MediaHelper.downloadPartsToExternal(this@ViewCreationActivity, listOf(path))
                    .collectLatest { state ->
                        when (state) {
                            HandleState.SUCCESS -> {
                                Toast.makeText(
                                    this@ViewCreationActivity,
                                    strings(R.string.download_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            HandleState.FAIL -> {
                                Toast.makeText(
                                    this@ViewCreationActivity,
                                    strings(R.string.download_failed_please_try_again_later),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {}
                        }
                    }
            }
        }
    }

}
