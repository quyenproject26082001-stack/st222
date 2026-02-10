package poster.maker.activity_app.oldwest

import android.graphics.BitmapFactory
import android.os.Build
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.util.Admob
import poster.maker.R
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.*
import poster.maker.core.helper.MediaHelper
import poster.maker.core.helper.PermissionHelper
import poster.maker.core.utils.state.HandleState
import poster.maker.databinding.ActivityViewOldwestBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ViewOldWestActivity : BaseActivity<ActivityViewOldwestBinding>() {

    private var assetFileName: String? = null
    private var cachedFilePath: String? = null

    private var downloadPermissionDeniedCount = 0

    // Permission launcher for Android 8-9
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }

        if (allGranted) {
            downloadPermissionDeniedCount = 0
            proceedDownload()
        } else {
            downloadPermissionDeniedCount++

            val canAskAgain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                true
            }

            if (!canAskAgain) {
                goToSettings()
            } else if (downloadPermissionDeniedCount >= 2) {
                goToSettings()
            } else {
                Toast.makeText(
                    this,
                    strings(R.string.download_failed_please_try_again_later),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun setViewBinding(): ActivityViewOldwestBinding {
        return ActivityViewOldwestBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        assetFileName = intent.getStringExtra("assetFileName")

        assetFileName?.let { fileName ->
            try {
                // Load image from assets
                val inputStream = assets.open("oldwest/$fileName")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.imgPoster.setImageBitmap(bitmap)
                inputStream.close()

                // Copy to cache for sharing/downloading
                copyAssetToCache(fileName)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyAssetToCache(fileName: String) {
        try {
            val inputStream = assets.open("oldwest/$fileName")
            val cacheFile = File(cacheDir, "oldwest_$fileName")

            FileOutputStream(cacheFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            cachedFilePath = cacheFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            // Left button - Back
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()

            // Hide other elements (no delete for assets)
            tvCenter.gone()
            btnActionBarRight.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
        }
    }

    override fun viewListener() {
        binding.apply {
            // Back button
            actionBar.btnActionBarLeft.setOnSingleClick {
               showInterAll {  finishAfterTransition() }
            }

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

    private fun shareImage() {
        cachedFilePath?.let { path ->
            val paths = arrayListOf(path)
            shareImagesPaths(paths)
        }
    }

    private fun downloadImage() {
        // Android 10+: No permission needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            proceedDownload()
            return
        }

        // Android 8-9: Need permission
        val storagePermissions = PermissionHelper.storagePermission

        if (checkPermissions(storagePermissions)) {
            proceedDownload()
        } else if (downloadPermissionDeniedCount >= 2) {
            goToSettings()
        } else {
            permissionLauncher.launch(storagePermissions)
        }
    }

//    //quyen
//    override fun initAds() {
//        Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_cl_Old_West_detail), binding.nativeClOldWest)
//    }
    //quyen


    override fun onRestart() {
        super.onRestart()
        Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_cl_Old_West_detail), binding.nativeClOldWest)
    }

    private fun proceedDownload() {
        cachedFilePath?.let { path ->
            lifecycleScope.launch {
                MediaHelper.downloadPartsToExternal(this@ViewOldWestActivity, listOf(path))
                    .collectLatest { state ->
                        when (state) {
                            HandleState.SUCCESS -> {
                                Toast.makeText(
                                    this@ViewOldWestActivity,
                                    strings(R.string.download_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            HandleState.FAIL -> {
                                Toast.makeText(
                                    this@ViewOldWestActivity,
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
