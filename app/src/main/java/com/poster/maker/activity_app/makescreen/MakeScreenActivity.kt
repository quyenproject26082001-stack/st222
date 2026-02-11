package poster.maker.activity_app.makescreen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import jp.wasabeef.glide.transformations.BlurTransformation
import poster.maker.R
import poster.maker.activity_app.bountyedit.BountyEditActivity
import poster.maker.activity_app.nameedit.NameEditActivity
import poster.maker.activity_app.photofilter.PhotoFilterActivity
import poster.maker.activity_app.postershadow.PosterShadowActivity
import poster.maker.activity_app.success.SuccessActivity
import poster.maker.activity_app.template.TemplateListActivity
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.*
import poster.maker.core.helper.AssetHelper
import poster.maker.core.helper.MediaHelper
import poster.maker.core.helper.ShadowTransformation
import poster.maker.core.utils.state.SaveState
import poster.maker.core.viewmodel.PosterEditorSharedViewModel
import poster.maker.databinding.ActivityMakeScreenBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.lvt.ads.util.Admob

/**
 * MakeScreen Activity (Simplified)
 * Shows poster preview with action buttons that navigate to dedicated editing screens
 */
class MakeScreenActivity : BaseActivity<ActivityMakeScreenBinding>() {

    var interAll: InterstitialAd? = null

    private val viewModel = PosterEditorSharedViewModel.getInstance()

    // Dynamic poster views
    private var imgTemplate: ImageView? = null
    private var imgTemplateShadow: ImageView? = null
    private var imgAvatar: ImageView? = null
    private var imgAvatarShadow: ImageView? = null
    private var tvName: TextView? = null
    private var tvBounty: TextView? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                viewModel.setSelectedImageUri(it)
                viewModel.markEditingStarted()
                updatePreviewWithCurrentState()
            }
        }

    // Activity launchers for editing screens
    private val nameEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                updatePreviewWithCurrentState()
            }
        }

    private val bountyEditLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                updatePreviewWithCurrentState()
            }
        }

    private val photoFilterLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                updatePreviewWithCurrentState()
            }
        }

    private val posterShadowLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                updatePreviewWithCurrentState()
            }
        }

    private val templateSelectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { data ->
                    val selectedTemplateId = data.getIntExtra("selectedTemplateId", 1)

                    // Get old config before changing template
                    val oldConfig = viewModel.getConfig()

                    // Extract bounty number by removing old prefix/suffix
                    val currentBountyText = viewModel.bountyText.value
                    val bountyNumber = currentBountyText
                        .removePrefix(oldConfig.bountyPrefix)
                        .removeSuffix(oldConfig.bountySuffix)

                    // Change template
                    viewModel.setSelectedTemplate(selectedTemplateId)

                    // Get new config
                    val newConfig = viewModel.getConfig()

                    // Update bountySize to new template's default size
                    viewModel.setBountySize(newConfig.bountySize)

                    // Update bounty text with new prefix/suffix
                    viewModel.setBountyText("${newConfig.bountyPrefix}${bountyNumber}${newConfig.bountySuffix}")

                    // Refresh preview with new template
                    updatePreviewWithCurrentState()
                }
            }
        }

    override fun setViewBinding(): ActivityMakeScreenBinding {
        return ActivityMakeScreenBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Check if template was selected from Intent
        val selectedTemplateFromIntent = intent.getIntExtra("selectedTemplateId", -1)
        if (selectedTemplateFromIntent != -1) {
            viewModel.setSelectedTemplate(selectedTemplateFromIntent)
        }

        // Load template layout and background from assets
        inflateTemplateLayout(viewModel.selectedTemplate.value)

        // Initialize with default template and preview
        updatePreviewWithCurrentState()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            btnActionBarRight.visible()
            btnActionBarRight.setImageResource(R.drawable.ic_save)
            tvCenter.text = strings(R.string.wanted_poster_maker)
            tvCenter.visible()
            tvCenter.isSelected = true
            tvRightText.gone()
            btnActionBarReset.gone()
        }
    }

    override fun viewListener() {
        binding.apply {
            // Action bar
            actionBar.apply {
                btnActionBarLeft.setOnSingleClick { showInterAll { handleBack() } }
                btnActionBarRight.setOnSingleClick { handleSave() }
            }

            // Templates button
            cvTemplates.setOnSingleClick {
                val intent = Intent(this@MakeScreenActivity, TemplateListActivity::class.java).apply {
                    putExtra("currentTemplateId", viewModel.selectedTemplate.value)
                }
                templateSelectionLauncher.launch(intent)
            }

            // Import button
            cvImport.setOnSingleClick(2000) {
                pickImageLauncher.launch("image/*")
            }

            // Action buttons - Navigate to dedicated editing screens
            cvName.setOnSingleClick {
                // Mark editing started before launching
                if (!viewModel.isEditingStarted.value) {
                    viewModel.markEditingStarted()
                    updatePreviewWithCurrentState()
                }
                val intent = Intent(this@MakeScreenActivity, NameEditActivity::class.java)
                nameEditLauncher.launch(intent)
            }

            cvBounty.setOnSingleClick {
                // Mark editing started before launching
                if (!viewModel.isEditingStarted.value) {
                    viewModel.markEditingStarted()
                    updatePreviewWithCurrentState()
                }
                val intent = Intent(this@MakeScreenActivity, BountyEditActivity::class.java).apply {
                    // Ensure BountyEdit preview uses the latest name values
                    putExtra(BountyEditActivity.EXTRA_NAME_TEXT, viewModel.nameText.value)
                    putExtra(BountyEditActivity.EXTRA_NAME_FONT, viewModel.nameFont.value)
                    putExtra(BountyEditActivity.EXTRA_NAME_SPACING, viewModel.nameSpacing.value)
                }
                bountyEditLauncher.launch(intent)
            }

            cvPhotoFilter.setOnSingleClick {
                // Mark editing started before launching
                if (!viewModel.isEditingStarted.value) {
                    viewModel.markEditingStarted()
                    updatePreviewWithCurrentState()
                }
                val intent = Intent(this@MakeScreenActivity, PhotoFilterActivity::class.java)
                photoFilterLauncher.launch(intent)
            }

            cvPosterShadow.setOnSingleClick {
                // Mark editing started before launching
                if (!viewModel.isEditingStarted.value) {
                    viewModel.markEditingStarted()
                    updatePreviewWithCurrentState()
                }
                val intent = Intent(this@MakeScreenActivity, PosterShadowActivity::class.java)
                posterShadowLauncher.launch(intent)
            }
        }
    }

    override fun dataObservable() {
        // Observe selected image URI
        lifecycleScope.launch {
            viewModel.selectedImageUri.collect { uri ->
                uri?.let {
                    loadImageToPreview(it)
                }
            }
        }

        // Observe name text
        lifecycleScope.launch {
            viewModel.nameText.collect { text ->
                tvName?.text = text
            }
        }

        // Observe bounty text
        lifecycleScope.launch {
            viewModel.bountyText.collect { text ->
                tvBounty?.text = text
            }
        }

        // Observe poster shadow
        lifecycleScope.launch {
            viewModel.posterShadow.collect { shadowValue ->
                applyPosterShadow(shadowValue)
            }
        }

        // Observe photo shadow
        lifecycleScope.launch {
            viewModel.filterShadow.collect { shadowValue ->
                applyPhotoShadow(shadowValue)
            }
        }
    }

    /**
     * Get layout resource ID for template
     */
    private fun getTemplateLayoutResId(templateId: Int): Int {
        return when (templateId) {
            1 -> R.layout.layout_poster_template_1
            2 -> R.layout.layout_poster_template_2
            3 -> R.layout.layout_poster_template_3
            4 -> R.layout.layout_poster_template_4
            5 -> R.layout.layout_poster_template_5
            6 -> R.layout.layout_poster_template_6
            7 -> R.layout.layout_poster_template_7
            8 -> R.layout.layout_poster_template_8
            9 -> R.layout.layout_poster_template_9
            10 -> R.layout.layout_poster_template_10
            11 -> R.layout.layout_poster_template_11
            12 -> R.layout.layout_poster_template_12
            13 -> R.layout.layout_poster_template_13
            14 -> R.layout.layout_poster_template_14
            15 -> R.layout.layout_poster_template_15
            16 -> R.layout.layout_poster_template_16
            else -> R.layout.layout_poster_template_1
        }
    }

    /**
     * Inflate template layout and bind views
     */
    private fun inflateTemplateLayout(templateId: Int) {
        // Remove old layout
        binding.containerPoster.removeAllViews()

        // Inflate new layout
        val layoutResId = getTemplateLayoutResId(templateId)
        val posterView = layoutInflater.inflate(layoutResId, binding.containerPoster, true)

        // Bind views
        imgTemplate = posterView.findViewById(R.id.imgTemplate)
        imgTemplateShadow = posterView.findViewById(R.id.imgTemplateShadow)
        imgAvatar = posterView.findViewById(R.id.imgAvatar)
        imgAvatarShadow = posterView.findViewById(R.id.imgAvatarShadow)
        tvName = posterView.findViewById(R.id.tvName)
        tvBounty = posterView.findViewById(R.id.tvBounty)

        // Setup autoSize for tvName
        tvName?.apply {
            maxLines = 1
            val config = viewModel.getConfig()
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(
                this,
                androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
            )
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this,
                6,
                config.nameSize.toInt(),
                1,
                android.util.TypedValue.COMPLEX_UNIT_SP
            )
        }

        // Load template background
        loadTemplateBackground()
    }

    /**
     * Load template background from assets
     */
    private fun loadTemplateBackground() {
        val templateId = viewModel.selectedTemplate.value
        val isEditing = viewModel.isEditingStarted.value

        // Use avatar.png for initial preview, item.png after editing
        val templatePath = if (isEditing) {
            AssetHelper.getTemplateItemPath(templateId)
        } else {
            AssetHelper.getTemplateAvatarPath(templateId)
        }

        imgTemplate?.let { imageView ->
            Glide.with(this)
                .load(templatePath)
                .error(R.drawable.template)
                .into(imageView)
        }
    }

    /**
     * Update preview with current ViewModel state
     */
    private fun updatePreviewWithCurrentState() {
        // Reload template layout if template changed
        val currentTemplateId = viewModel.selectedTemplate.value
        inflateTemplateLayout(currentTemplateId)

        val isEditing = viewModel.isEditingStarted.value
        val config = viewModel.getConfig()

        // Show/hide editable elements based on editing state
        if (isEditing) {
            // Show name only if template has name field
            if (config.hasName) {
                tvName?.visibility = View.VISIBLE
                tvName?.text = viewModel.nameText.value
            } else {
                tvName?.visibility = View.GONE
            }

            tvBounty?.visibility = View.VISIBLE
            tvBounty?.text = viewModel.bountyText.value

            imgAvatar?.visibility = View.VISIBLE
            imgAvatarShadow?.visibility = View.VISIBLE

            // Update image if exists, otherwise show default avatar
            viewModel.selectedImageUri.value?.let { uri ->
                loadImageToPreview(uri)
            } ?: run {
                // Load default avatar.webp from drawable
                imgAvatar?.let { imageView ->
                    Glide.with(this)
                        .load(R.drawable.avatar)
                        .centerCrop()
                        .into(imageView)
                }
            }

            // Apply ALL effects from ViewModel
            applyAllEffectsFromViewModel()
        } else {
            // Hide all editable elements - show only avatar.png preview
            tvName?.visibility = View.GONE
            tvBounty?.visibility = View.GONE
            imgAvatar?.visibility = View.GONE
            imgAvatarShadow?.visibility = View.GONE
            imgTemplateShadow?.visibility = View.GONE
        }
    }

    /**
     * Apply all effects from ViewModel to preview
     */
    private fun applyAllEffectsFromViewModel() {
        val config = viewModel.getConfig()

        // Apply colors
        try {
            if (config.hasName) {
                tvName?.setTextColor(Color.parseColor(config.nameColor))
            }
            tvBounty?.setTextColor(Color.parseColor(config.bountyColor))
        } catch (e: Exception) {
            tvName?.setTextColor(Color.BLACK)
            tvBounty?.setTextColor(Color.BLACK)
        }

        // Apply fonts
        val fontName = viewModel.nameFont.value
        val fontResId = mapFontNameToResource(fontName)
        if (fontResId != null) {
            val typeface = androidx.core.content.res.ResourcesCompat.getFont(this, fontResId)
            tvName?.typeface = typeface
        }

        val bountyFontName = viewModel.bountyFont.value
        val bountyFontResId = mapFontNameToResource(bountyFontName)
        if (bountyFontResId != null) {
            val bountyTypeface = androidx.core.content.res.ResourcesCompat.getFont(this, bountyFontResId)
            tvBounty?.typeface = bountyTypeface
        }

        // Name effects
        tvName?.letterSpacing = viewModel.nameSpacing.value

        // Bounty effects
        tvBounty?.textSize = viewModel.bountySize.value
        tvBounty?.letterSpacing = viewModel.bountySpacing.value
        tvBounty?.translationX = viewModel.bountyPositionX.value
        tvBounty?.translationY = viewModel.bountyPositionY.value

        // Photo filters
        applyPhotoFilters()

        // Shadows
        applyPosterShadow(viewModel.posterShadow.value)
        applyPhotoShadow(viewModel.filterShadow.value)
    }

    /**
     * Load image into preview
     */
    private fun loadImageToPreview(uri: Uri) {
        val blur = viewModel.filterBlur.value

        if (blur > 0) {
            reloadImageWithBlur(uri, blur)
        } else {
            imgAvatar?.let { imageView ->
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imageView)
            }
        }

        // Load shadow
        val shadowValue = viewModel.filterShadow.value
        if (shadowValue > 0) {
            applyPhotoShadow(shadowValue)
        } else {
            imgAvatarShadow?.visibility = View.GONE
        }
    }

    /**
     * Reload image with blur transformation
     */
    private fun reloadImageWithBlur(uri: Uri, blurValue: Float) {
        imgAvatar?.let { imageView ->
            if (blurValue > 0) {
                val blurRadius = (blurValue / 100f * 25f).toInt().coerceAtLeast(1)
                Glide.with(this)
                    .load(uri)
                    .transform(CenterCrop(), BlurTransformation(blurRadius, 3))
                    .into(imageView)
            } else {
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imageView)
            }
        }
    }

    /**
     * Apply photo filters to imgAvatar
     */
    private fun applyPhotoFilters() {
        val brightness = viewModel.filterBrightness.value
        val contrast = viewModel.filterContrast.value
        val saturation = viewModel.filterSaturate.value
        val grayscale = viewModel.filterGrayscale.value
        val hueRotate = viewModel.filterHueRotate.value
        val sepia = viewModel.filterSepia.value

        val colorMatrix = android.graphics.ColorMatrix()

        // Brightness
        val brightnessMatrix = android.graphics.ColorMatrix(
            floatArrayOf(
                brightness, 0f, 0f, 0f, 0f,
                0f, brightness, 0f, 0f, 0f,
                0f, 0f, brightness, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(brightnessMatrix)

        // Contrast
        val scale = contrast
        val translate = (1f - contrast) / 2f * 255f
        val contrastMatrix = android.graphics.ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        // Saturation
        val saturationMatrix = android.graphics.ColorMatrix()
        saturationMatrix.setSaturation(saturation)
        colorMatrix.postConcat(saturationMatrix)

        // Grayscale
        if (grayscale > 0) {
            val invGrayscale = 1 - grayscale
            val grayscaleMatrix = android.graphics.ColorMatrix(
                floatArrayOf(
                    invGrayscale + grayscale * 0.299f, grayscale * 0.587f, grayscale * 0.114f, 0f, 0f,
                    grayscale * 0.299f, invGrayscale + grayscale * 0.587f, grayscale * 0.114f, 0f, 0f,
                    grayscale * 0.299f, grayscale * 0.587f, invGrayscale + grayscale * 0.114f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(grayscaleMatrix)
        }

        // Hue Rotation
        if (hueRotate != 0f) {
            val angle = hueRotate * Math.PI.toFloat() / 180f
            val cosA = kotlin.math.cos(angle.toDouble()).toFloat()
            val sinA = kotlin.math.sin(angle.toDouble()).toFloat()

            val hueRotateMatrix = android.graphics.ColorMatrix(
                floatArrayOf(
                    0.213f + cosA * 0.787f - sinA * 0.213f, 0.715f - cosA * 0.715f - sinA * 0.715f, 0.072f - cosA * 0.072f + sinA * 0.928f, 0f, 0f,
                    0.213f - cosA * 0.213f + sinA * 0.143f, 0.715f + cosA * 0.285f + sinA * 0.140f, 0.072f - cosA * 0.072f - sinA * 0.283f, 0f, 0f,
                    0.213f - cosA * 0.213f - sinA * 0.787f, 0.715f - cosA * 0.715f + sinA * 0.715f, 0.072f + cosA * 0.928f + sinA * 0.072f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(hueRotateMatrix)
        }

        // Sepia
        if (sepia > 0) {
            val invSepia = 1 - sepia
            val sepiaMatrix = android.graphics.ColorMatrix(
                floatArrayOf(
                    invSepia + sepia * 0.393f, sepia * 0.769f, sepia * 0.189f, 0f, 0f,
                    sepia * 0.349f, invSepia + sepia * 0.686f, sepia * 0.168f, 0f, 0f,
                    sepia * 0.272f, sepia * 0.534f, invSepia + sepia * 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            colorMatrix.postConcat(sepiaMatrix)
        }

        // Apply ColorMatrix filter
        imgAvatar?.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)

        // Clear RenderEffect
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            imgAvatar?.setRenderEffect(null)
        }

        // Apply blur using Glide
        val currentUri = viewModel.selectedImageUri.value
        if (currentUri != null) {
            val blur = viewModel.filterBlur.value
            reloadImageWithBlur(currentUri, blur)
        }
    }

    /**
     * Apply poster shadow effect
     */
    private fun applyPosterShadow(shadowValue: Float) {
        val shadowView = imgTemplateShadow ?: return

        if (shadowValue <= 0) {
            shadowView.visibility = View.GONE
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                shadowView.setRenderEffect(null)
            }
            return
        }

        shadowView.visibility = View.VISIBLE

        val shadowRadius = shadowValue / 100f * 15f
        val shadowAlpha = shadowValue / 100f * 0.9f

        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)

        Glide.with(this)
            .load(templatePath)
            .transform(ShadowTransformation(shadowRadius, shadowAlpha))
            .into(shadowView)

        val viewAlpha = (shadowValue / 100f).coerceIn(0f, 1f)
        shadowView.alpha = viewAlpha

        val offsetX = shadowValue / 100f * 5f
        val offsetY = shadowValue / 100f * 5f
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY

        val scale = 1f + (shadowValue / 100f * 0.03f)
        shadowView.scaleX = scale
        shadowView.scaleY = scale

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur = shadowValue / 100f * 10f
            if (additionalBlur > 0) {
                val blurEffect = android.graphics.RenderEffect.createBlurEffect(
                    additionalBlur, additionalBlur, android.graphics.Shader.TileMode.CLAMP
                )
                shadowView.setRenderEffect(blurEffect)
            } else {
                shadowView.setRenderEffect(null)
            }
        }
    }

    /**
     * Apply photo shadow effect
     */
    private fun applyPhotoShadow(shadowValue: Float) {
        val shadowView = imgAvatarShadow ?: return

        if (shadowValue <= 0) {
            shadowView.visibility = View.GONE
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                shadowView.setRenderEffect(null)
            }
            return
        }

        shadowView.visibility = View.VISIBLE

        val effectiveShadowValue = 35f + (shadowValue / 100f * 65f)

        val currentUri = viewModel.selectedImageUri.value
        if (currentUri != null) {
            val shadowRadius = effectiveShadowValue / 100f * 15f
            val shadowAlpha = effectiveShadowValue / 100f * 0.9f

            Glide.with(this)
                .load(currentUri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(shadowView)
        }

        val viewAlpha = (effectiveShadowValue / 100f).coerceIn(0f, 1f)
        shadowView.alpha = viewAlpha

        val offsetX = effectiveShadowValue / 100f * 5f
        val offsetY = effectiveShadowValue / 100f * 5f
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY

        val scale = 1f + (effectiveShadowValue / 100f * 0.15f)
        shadowView.scaleX = scale
        shadowView.scaleY = scale

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur = effectiveShadowValue / 100f * 10f
            if (additionalBlur > 0) {
                val blurEffect = android.graphics.RenderEffect.createBlurEffect(
                    additionalBlur, additionalBlur, android.graphics.Shader.TileMode.CLAMP
                )
                shadowView.setRenderEffect(blurEffect)
            } else {
                shadowView.setRenderEffect(null)
            }
        }
    }

    /**
     * Map font name to resource ID
     */
    private fun mapFontNameToResource(fontName: String): Int? {
        return when (fontName) {
            "Roboto Bold" -> R.font.roboto_bold
            "Roboto Medium" -> R.font.roboto_medium
            "Roboto Regular" -> R.font.roboto_regular
            "Londrina Solid" -> R.font.londrina_solid_regular
            "Montserrat Bold" -> R.font.montserrat_bold
            "Montserrat Medium" -> R.font.montserrat_medium
            "Script Elegant 1" -> R.font.script_elegant_01
            "Script Elegant 2" -> R.font.script_elegant_02
            "Handwriting 1" -> R.font.script_handwriting_01
            "Script Casual" -> R.font.script_casual
            "Brush Style" -> R.font.brush_01
            "Horror Style 1" -> R.font.display_horror_02
            "Horror Style 2" -> R.font.display_horror_04
            "Halloween" -> R.font.display_halloween
            "Gothic" -> R.font.display_gothic_01
            "Horror Style 5" -> R.font.display_horror_11
            "Creative 1" -> R.font.display_creative_01
            "Rounded" -> R.font.display_rounded
            "Serif Classic" -> R.font.serif_02
            "Signature" -> R.font.serif_signature
            "Display Cultural Style" -> R.font.display_cultural
            "Display Festive Style" -> R.font.display_festive
            "Tream Style" -> R.font.treamd
            "Ocean Style" -> R.font.ocen
            else -> null
        }
    }

    /**
     * Handle back button
     */
    private fun handleBack() {
        if (viewModel.isEditingStarted.value) {
            viewModel.clearAll()
            finishAfterTransition()
        } else {
            viewModel.clearAll()
            finishAfterTransition()
        }
    }

    /**
     * Handle save button
     */
    private fun handleSave() {
        val config = viewModel.getConfig()
        val currentNameText = viewModel.nameText.value

        // Validation: Check if user has customized the name
        if (config.hasName) {
            if (currentNameText.isBlank()) {
                showToast(getString(R.string.vui_l_ng_nh_p_t_n_tr_c_khi_l_u))
                return
            }
            if (currentNameText == config.nameDefaultText) {
                showToast(getString(R.string.vui_l_ng_nh_p_t_n_tr_c_khi_l_u))
                return
            }
        }

        showInterAll {}

        // Capture poster view as bitmap
        val posterView = binding.containerPoster
        if (posterView.width == 0 || posterView.height == 0) {
            showToast(strings(R.string.download_failed_please_try_again_later))
            return
        }

        val bitmap = Bitmap.createBitmap(posterView.width, posterView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        posterView.draw(canvas)

        // Save bitmap to internal storage
        lifecycleScope.launch {
            MediaHelper.saveBitmapToInternalStorage(this@MakeScreenActivity, "posters", bitmap)
                .collectLatest { state ->
                    when (state) {
                        is SaveState.Success -> {
                            viewModel.setSavedImagePath(state.path)
                            val intent = Intent(this@MakeScreenActivity, SuccessActivity::class.java)
                            startActivity(intent)
                        }

                        is SaveState.Error -> {
                            showToast(strings(R.string.download_failed_please_try_again_later))
                        }

                        SaveState.Loading -> {
                            // Show loading indicator if needed
                        }
                    }
                }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showInterAll(callback: () -> Unit) {
        // Placeholder for interstitial ad
        callback()
    }
}
