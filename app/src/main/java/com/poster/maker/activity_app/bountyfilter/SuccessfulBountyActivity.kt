package poster.maker.activity_app.bountyfilter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.lvt.ads.util.Admob
import java.io.FileOutputStream
import poster.maker.R
import poster.maker.activity_app.main.MainActivity
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.checkPermissions
import poster.maker.core.extensions.gone
import poster.maker.core.extensions.goToSettings
import poster.maker.core.extensions.select
import poster.maker.core.extensions.setOnSingleClick
import poster.maker.core.extensions.shareImagesPaths
import poster.maker.core.extensions.showInterAll
import poster.maker.core.extensions.strings
import poster.maker.core.extensions.visible
import poster.maker.core.helper.BitmapHelper
import poster.maker.core.helper.MediaHelper
import poster.maker.core.helper.PermissionHelper
import poster.maker.core.helper.SoundHelper
import poster.maker.core.utils.state.HandleState
import poster.maker.core.viewmodel.PosterEditorSharedViewModel
import poster.maker.databinding.SuccessfullBountyBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SuccessfulBountyActivity : BaseActivity<SuccessfullBountyBinding>() {

    companion object {
        private const val REQUEST_CODE_EDIT_STICKER = 1001
    }

    private var photoPath: String? = null
    private var bountyValue: String? = null
    private var downloadPermissionDeniedCount = 0
    private var compositeImagePath: String? = null
    private var hasStickers = false

    private val viewModel = PosterEditorSharedViewModel.getInstance()




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

    override fun setViewBinding(): SuccessfullBountyBinding {
        return SuccessfullBountyBinding.inflate(LayoutInflater.from(this))
    }



    override fun initView() {
        // Get data from intent
        photoPath = intent.getStringExtra("PHOTO_PATH")
        bountyValue = intent.getStringExtra("BOUNTY_VALUE")

        binding.apply {
            // Show bounty value first
            bountyValue?.let {
                tvBountyFilter.text = it
                tvBountyFilter.visible()
            }

            // Load captured photo into imgCamera with listener
            photoPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    Glide.with(this@SuccessfulBountyActivity)
                        .load(file)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                android.util.Log.e("SuccessfulBounty", "Failed to load image", e)
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                // Image loaded successfully, now create composite
                                android.util.Log.d("SuccessfulBounty", "Image loaded, creating composite")
                                binding.containerBounty.post {
                                    createCompositeImage()
                                }
                                return false
                            }
                        })
                        .into(imgCamera)
                } else {
                    android.util.Log.e("SuccessfulBounty", "Photo file not found: $path")
                }
            }
        }
    }
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
    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.setOnSingleClick {
                showInterAll {
                goToHome()
                }
            }

            actionBar.btnActionBarNextToRight.visible()

            actionBar.btnActionBarNextToRight.setOnSingleClick {
                showInterAll {openEditSticker() }
            }



            btnDownload.setOnSingleClick {
                downloadImage()
            }

            btnShare.setOnSingleClick(2000) {
                shareImage()
            }

            actionBar.btnActionBarRight.setOnSingleClick {
               showInterAll { saveToMyDesign() }
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            tvCenter.text = getString(R.string.bountyFilter)
            btnActionBarLeft.setImageResource(R.drawable.ic_home)
            btnActionBarLeft.visible()
            btnActionBarRight.visible()
            tvRightText.select()
            updateActionBarIcons()
        }
    }

    private fun updateActionBarIcons() {
        binding.actionBar.apply {
            if (hasStickers) {
                btnActionBarRight.visible()
            } else {
                btnActionBarRight.visible()
            }
        }
    }

    private fun createCompositeImage() {
        try {
            // Capture the entire containerBounty (background + camera photo + text)
            val view = binding.containerBounty
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            val fileName = "bounty_composite_${System.currentTimeMillis()}.jpg"

            // Save to cache for immediate sharing/downloading
            val cacheFile = File(cacheDir, fileName)
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            compositeImagePath = cacheFile.absolutePath

            // ✅ REMOVED: Auto-save to My Design
            // User must click Save button to save to My Design
            // val bountyDesignsDir = File(filesDir, "bounty_designs")
            // if (!bountyDesignsDir.exists()) {
            //     bountyDesignsDir.mkdirs()
            // }
            // val savedFile = File(bountyDesignsDir, fileName)
            // FileOutputStream(savedFile).use { out ->
            //     bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            // }

            android.util.Log.d("SuccessfulBounty", "Composite image created: $compositeImagePath")
            android.util.Log.d("SuccessfulBounty", "Saved to CACHE only (not My Design yet)")
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("SuccessfulBounty", "Failed to create composite image", e)
        }
    }

    private fun shareImage() {
        val pathToShare = compositeImagePath ?: photoPath
        pathToShare?.let { path ->
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

    override fun onRestart() {
        super.onRestart()
    }

//    override fun initAds() {
//        super.initAds()
//        initNativeCollab()
//    }

//    fun initNativeCollab() {
//        Admob.getInstance().loadNativeCollap(this,
//            getString(R.string.native_cl_fillter_success),
//            binding.nativeCollapSSBounty)
//    }
    private fun openEditSticker() {
        // Show loading immediately for better UX
        // (Optional: Add a progress indicator here)

        // Capture entire containerBounty as bitmap
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val containerBitmap = withContext(Dispatchers.Main) {
                    // Capture on Main thread (required for view access)
                    BitmapHelper.createBimapFromView(binding.containerBounty)
                }

                // Save to temp file - Use JPEG for faster compression (3x faster than PNG)
                val tempFile = File(cacheDir, "temp_container_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { out ->
                    // JPEG with 95% quality = 3x faster, similar visual quality
                    containerBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }

                withContext(Dispatchers.Main) {
                    val intent = Intent(this@SuccessfulBountyActivity,
                        poster.maker.activity_app.editsticker.EditStickerActivity::class.java)
                    intent.putExtra("IMAGE_PATH", tempFile.absolutePath)
                    startActivityForResult(intent, REQUEST_CODE_EDIT_STICKER)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@SuccessfulBountyActivity,
                        "Error capturing poster",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDIT_STICKER && resultCode == RESULT_OK) {
            data?.let {
                val editedPath = it.getStringExtra("EDITED_IMAGE_PATH")
                val hasStickersAdded = it.getBooleanExtra("HAS_STICKERS", false)

                if (editedPath != null) {
                    compositeImagePath = editedPath
                    hasStickers = hasStickersAdded

                    // Hide original views
                    binding.imgCamera.visibility = android.view.View.GONE
                    binding.imgPlaySuccess.visibility = android.view.View.GONE
                    binding.tvBountyFilter.visibility = android.view.View.GONE

                    // Remove any existing composite ImageView
                    binding.containerBounty.findViewWithTag<android.widget.ImageView>("composite_view")?.let { view ->
                        binding.containerBounty.removeView(view)
                    }

                    // Add new ImageView with composite image
                    val compositeImageView = android.widget.ImageView(this).apply {
                        tag = "composite_view"
                        scaleType = android.widget.ImageView.ScaleType.FIT_XY
                        layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
                        )
                    }

                    Glide.with(this)
                        .load(File(editedPath))
                        .into(compositeImageView)

                    binding.containerBounty.addView(compositeImageView)

                    // Update action bar to show save icon
                    updateActionBarIcons()
                }
            }
        }
    }

    private fun saveToMyDesign() {
        val pathToSave = compositeImagePath ?: photoPath
        pathToSave?.let { path ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val sourceFile = File(path)
                    val myDesignDir = File(filesDir, "bounty_designs")
                    if (!myDesignDir.exists()) {
                        myDesignDir.mkdirs()
                    }

                    val fileName = "poster_${System.currentTimeMillis()}.png"
                    val destFile = File(myDesignDir, fileName)
                    sourceFile.copyTo(destFile, overwrite = true)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SuccessfulBountyActivity,
                            getString(R.string.saved_to_my_design),
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to My Design
                        val intent = Intent(this@SuccessfulBountyActivity, poster.maker.activity_app.mycreation.MyCreationActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                         startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SuccessfulBountyActivity,
                            "Save failed!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun proceedDownload() {
        val pathToDownload = compositeImagePath ?: photoPath
        pathToDownload?.let { path ->
            lifecycleScope.launch {
                MediaHelper.downloadPartsToExternal(this@SuccessfulBountyActivity, listOf(path))
                    .collectLatest { state ->
                        when (state) {
                            HandleState.SUCCESS -> {
                                Toast.makeText(
                                    this@SuccessfulBountyActivity,
                                    strings(R.string.download_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            HandleState.FAIL -> {
                                Toast.makeText(
                                    this@SuccessfulBountyActivity,
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
