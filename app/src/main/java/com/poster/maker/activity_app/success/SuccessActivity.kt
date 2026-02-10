package poster.maker.activity_app.success

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import poster.maker.R
import poster.maker.activity_app.main.MainActivity
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.checkPermissions
import poster.maker.core.extensions.gone
import poster.maker.core.extensions.goToSettings
import poster.maker.core.extensions.setOnSingleClick
import poster.maker.core.extensions.shareImagesPaths
import poster.maker.core.extensions.strings
import poster.maker.core.extensions.visible
import poster.maker.core.helper.MediaHelper
import poster.maker.core.helper.PermissionHelper
import poster.maker.core.utils.state.HandleState
import poster.maker.core.viewmodel.PosterEditorSharedViewModel
import poster.maker.databinding.ActivitySuccessBinding
import poster.maker.dialog.YesNoDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
//quyen
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import poster.maker.core.extensions.showInterAll

//quyen

class SuccessActivity : BaseActivity<ActivitySuccessBinding>() {

    //quyen
    var interAll: InterstitialAd? = null
    //quyen

    private var downloadPermissionDeniedCount = 0

    private val viewModel = PosterEditorSharedViewModel.getInstance()
    private var savedImagePath: String? = null

    // Permission launcher for Android 8-9
    // ✅ BỎ COUNTER - Luôn hỏi quyền cho đến khi "Don't ask again"
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


    override fun setViewBinding(): ActivitySuccessBinding {
        return ActivitySuccessBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Load saved image from ViewModel
        savedImagePath = viewModel.savedImagePath.value

        android.util.Log.d("SuccessActivity", "savedImagePath: $savedImagePath")

        savedImagePath?.let { path ->
            val file = File(path)
            android.util.Log.d("SuccessActivity", "File exists: ${file.exists()}, size: ${file.length()}")
            Glide.with(this)
                .load(file)
                .into(binding.imgPoster)
        } ?: run {
            android.util.Log.e("SuccessActivity", "savedImagePath is null!")
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            // Left button - Home
            btnActionBarLeft.setImageResource(R.drawable.ic_home)
            btnActionBarLeft.visible()

            // Hide other elements
            tvCenter.gone()
            btnActionBarRight.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
            cvLogo.gone()
        }
    }

    override fun viewListener() {
        binding.apply {
            // Home button
            //quyen
            actionBar.btnActionBarLeft.setOnSingleClick {
                showInterAll {
                        goToHome()
                    }

            }
            //quyen

            // Share button
            btnShare.setOnSingleClick(2000) {
                shareImage()
            }

            // Download button
            btnDownload.setOnSingleClick {
                downloadImage()
            }
        }
    }

    override fun dataObservable() {
        // No data observables needed
    }

    //quyen
//    override fun initAds() {
//
//        // Load native collapsible ad
//        Admob.getInstance().loadNativeCollap(this, getString(R.string.native_collap_creation), binding.nativeClCreation)
//
//    }
    //quyen

    private fun goToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)

        // ✅ Chỉ cần gọi 1 lần, ngay sau startActivity
        overridePendingTransition(0, 0)

        finish()

        // ✅ Clear ViewModel SAU KHI finish để tránh trigger UI update
        viewModel.clearAll()
    }

    private fun shareImage() {
        savedImagePath?.let { path ->
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


    // ✅ THÊM MỚI


    /**
     * Proceed with download after permission check
     */
    private fun proceedDownload() {
        savedImagePath?.let { path ->
            lifecycleScope.launch {
                MediaHelper.downloadPartsToExternal(this@SuccessActivity, listOf(path))
                    .collectLatest { state ->
                        when (state) {
                            HandleState.SUCCESS -> {
                                Toast.makeText(
                                    this@SuccessActivity,
                                    strings(R.string.download_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            HandleState.FAIL -> {
                                Toast.makeText(
                                    this@SuccessActivity,
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

    override fun onBackPressed() {
        goToHome()
    }
}
