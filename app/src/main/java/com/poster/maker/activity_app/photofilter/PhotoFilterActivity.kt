package poster.maker.activity_app.photofilter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.launch
import poster.maker.R
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.*
import poster.maker.core.helper.AssetHelper
import poster.maker.core.helper.ShadowTransformation
import poster.maker.core.viewmodel.PosterEditorSharedViewModel
import poster.maker.databinding.ActivityPhotoFilterBinding

/**
 * Photo Filter Activity
 * Allows editing of photo filters: shadow, blur, brightness, contrast, grayscale, hue rotate, saturate, sepia
 */
class PhotoFilterActivity : BaseActivity<ActivityPhotoFilterBinding>() {

    private val viewModel = PosterEditorSharedViewModel.getInstance()

    // Template views
    private var imgTemplate: ImageView? = null
    private var imgAvatar: ImageView? = null
    private var imgAvatarShadow: ImageView? = null
    private var tvName: TextView? = null
    private var tvBounty: TextView? = null

    // Snapshot to restore on cancel
    private var originalFilterShadow: Float = 0f
    private var originalFilterBlur: Float = 0f
    private var originalFilterBrightness: Float = 1f
    private var originalFilterContrast: Float = 1f
    private var originalFilterGrayscale: Float = 0f
    private var originalFilterHueRotate: Float = 0f
    private var originalFilterSaturate: Float = 1f
    private var originalFilterSepia: Float = 0f

    override fun setViewBinding(): ActivityPhotoFilterBinding {
        return ActivityPhotoFilterBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Save original values for cancel
        originalFilterShadow = viewModel.filterShadow.value
        originalFilterBlur = viewModel.filterBlur.value
        originalFilterBrightness = viewModel.filterBrightness.value
        originalFilterContrast = viewModel.filterContrast.value
        originalFilterGrayscale = viewModel.filterGrayscale.value
        originalFilterHueRotate = viewModel.filterHueRotate.value
        originalFilterSaturate = viewModel.filterSaturate.value
        originalFilterSepia = viewModel.filterSepia.value

        // Inflate template layout
        inflateTemplateLayout(viewModel.selectedTemplate.value)

        // Setup seekbars
        setupSeekBars()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            btnActionBarRight.setImageResource(R.drawable.ic_tick)
            btnActionBarRight.visible()
            tvCenter.text = strings(R.string.photo_filter)
            tvCenter.visible()
            tvCenter.isSelected = true
            tvRightText.gone()
            btnActionBarReset.gone()
        }
    }

    override fun viewListener() {
        binding.actionBar.apply {
            // Back button - Cancel changes
            btnActionBarLeft.setOnSingleClick {
                handleCancel()
            }

            // Tick button - Confirm changes
            btnActionBarRight.setOnSingleClick {
                handleConfirm()
            }
        }
    }

    override fun dataObservable() {
        // Observe filter changes
        lifecycleScope.launch {
            viewModel.filterShadow.collect { shadowValue ->
                applyPhotoShadow(shadowValue)
            }
        }
    }

    /**
     * Inflate template layout
     */
    private fun inflateTemplateLayout(templateId: Int) {
        binding.containerPoster.removeAllViews()

        val layoutResId = getTemplateLayoutResId(templateId)
        val posterView = layoutInflater.inflate(layoutResId, binding.containerPoster, true)

        imgTemplate = posterView.findViewById(R.id.imgTemplate)
        imgAvatar = posterView.findViewById(R.id.imgAvatar)
        imgAvatarShadow = posterView.findViewById(R.id.imgAvatarShadow)
        tvName = posterView.findViewById(R.id.tvName)
        tvBounty = posterView.findViewById(R.id.tvBounty)

        // Load template background
        loadTemplateBackground()

        // Load avatar image
        loadAvatarImage()

        // Apply current Name and Bounty from ViewModel
        applyTextStyling()

        // Apply current filters
        applyPhotoFilters()
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
     * Load template background
     */
    private fun loadTemplateBackground() {
        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)

        imgTemplate?.let { imageView ->
            Glide.with(this)
                .load(templatePath)
                .error(R.drawable.template)
                .into(imageView)
        }
    }

    /**
     * Load avatar image
     */
    private fun loadAvatarImage() {
        imgAvatar?.visibility = View.VISIBLE
        imgAvatarShadow?.visibility = View.VISIBLE
        loadImageToPreview(getCurrentImageModel())
    }

    /**
     * Load image to preview
     */
    private fun loadImageToPreview(model: Any) {
        val blur = viewModel.filterBlur.value

        if (blur > 0) {
            reloadImageWithBlur(model, blur)
        } else {
            imgAvatar?.let { imageView ->
                Glide.with(this)
                    .load(model)
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
     * Reload image with blur
     */
    private fun reloadImageWithBlur(model: Any, blurValue: Float) {
        imgAvatar?.let { imageView ->
            if (blurValue > 0) {
                val blurRadius = (blurValue / 100f * 25f).toInt().coerceAtLeast(1)
                Glide.with(this)
                    .load(model)
                    .transform(CenterCrop(), BlurTransformation(blurRadius, 3))
                    .into(imageView)
            } else {
                Glide.with(this)
                    .load(model)
                    .centerCrop()
                    .into(imageView)
            }
        }
    }

    /**
     * Apply photo shadow
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

        val shadowRadius = effectiveShadowValue / 100f * 15f
        val shadowAlpha = effectiveShadowValue / 100f * 0.9f

        Glide.with(this)
            .load(getCurrentImageModel())
            .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
            .into(shadowView)

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
     * Apply photo filters
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

        // Re-apply blur (or normal) using the current image model
        val blur = viewModel.filterBlur.value
        reloadImageWithBlur(getCurrentImageModel(), blur)
    }

    /**
     * Setup all SeekBars
     */
    private fun setupSeekBars() {
        // Shadow
        binding.seekBarFilterShadow.progress = viewModel.filterShadow.value.toInt()
        binding.seekBarFilterShadow.onProgressChanged { progress ->
            viewModel.setFilterShadow(progress.toFloat())
            applyPhotoShadow(progress.toFloat())
        }

        // Blur
        binding.seekBarFilterBlur.progress = viewModel.filterBlur.value.toInt()
        binding.seekBarFilterBlur.onProgressChanged { progress ->
            viewModel.setFilterBlur(progress.toFloat())
            reloadImageWithBlur(getCurrentImageModel(), progress.toFloat())
            applyPhotoFilters()
        }

        // Brightness
        binding.seekBarFilterBrightness.progress = (viewModel.filterBrightness.value * 100f).toInt()
        binding.seekBarFilterBrightness.onProgressChanged { progress ->
            val brightness = progress / 100f
            viewModel.setFilterBrightness(brightness)
            applyPhotoFilters()
        }

        // Contrast
        binding.seekBarFilterContrast.progress = (viewModel.filterContrast.value * 100f).toInt()
        binding.seekBarFilterContrast.onProgressChanged { progress ->
            val contrast = progress / 100f
            viewModel.setFilterContrast(contrast)
            applyPhotoFilters()
        }

        // Grayscale
        binding.seekBarFilterGrayscale.progress = (viewModel.filterGrayscale.value * 100f).toInt()
        binding.seekBarFilterGrayscale.onProgressChanged { progress ->
            val grayscale = progress / 100f
            viewModel.setFilterGrayscale(grayscale)
            applyPhotoFilters()
        }

        // Hue Rotate
        binding.seekBarFilterHueRotate.progress = viewModel.filterHueRotate.value.toInt()
        binding.seekBarFilterHueRotate.onProgressChanged { progress ->
            val hueRotate = progress.toFloat()
            viewModel.setFilterHueRotate(hueRotate)
            applyPhotoFilters()
        }

        // Saturate
        binding.seekBarFilterSaturate.progress = (viewModel.filterSaturate.value * 100f).toInt()
        binding.seekBarFilterSaturate.onProgressChanged { progress ->
            val saturate = progress / 100f
            viewModel.setFilterSaturate(saturate)
            applyPhotoFilters()
        }

        // Sepia
        binding.seekBarFilterSepia.progress = (viewModel.filterSepia.value * 100f).toInt()
        binding.seekBarFilterSepia.onProgressChanged { progress ->
            val sepia = progress / 100f
            viewModel.setFilterSepia(sepia)
            applyPhotoFilters()
        }
    }

    private fun getCurrentImageModel(): Any {
        return viewModel.selectedImageUri.value ?: R.drawable.avatar
    }

    /**
     * Apply text styling from ViewModel
     */
    private fun applyTextStyling() {
        val config = viewModel.getConfig()

        // Apply NAME values
        tvName?.text = viewModel.nameText.value
        tvName?.letterSpacing = viewModel.nameSpacing.value
        try {
            tvName?.setTextColor(android.graphics.Color.parseColor(config.nameColor))
        } catch (e: Exception) {
            tvName?.setTextColor(android.graphics.Color.BLACK)
        }
        val nameFontResId = mapFontNameToResource(viewModel.nameFont.value)
        if (nameFontResId != null) {
            tvName?.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, nameFontResId)
        }

        // Apply BOUNTY values
        tvBounty?.text = viewModel.bountyText.value
        tvBounty?.textSize = viewModel.bountySize.value
        tvBounty?.letterSpacing = viewModel.bountySpacing.value
        tvBounty?.translationX = viewModel.bountyPositionX.value
        tvBounty?.translationY = viewModel.bountyPositionY.value
        try {
            tvBounty?.setTextColor(android.graphics.Color.parseColor(config.bountyColor))
        } catch (e: Exception) {
            tvBounty?.setTextColor(android.graphics.Color.BLACK)
        }
        val bountyFontResId = mapFontNameToResource(viewModel.bountyFont.value)
        if (bountyFontResId != null) {
            tvBounty?.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, bountyFontResId)
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
     * Handle cancel - Restore original values
     */
    private fun handleCancel() {
        viewModel.setFilterShadow(originalFilterShadow)
        viewModel.setFilterBlur(originalFilterBlur)
        viewModel.setFilterBrightness(originalFilterBrightness)
        viewModel.setFilterContrast(originalFilterContrast)
        viewModel.setFilterGrayscale(originalFilterGrayscale)
        viewModel.setFilterHueRotate(originalFilterHueRotate)
        viewModel.setFilterSaturate(originalFilterSaturate)
        viewModel.setFilterSepia(originalFilterSepia)
        finish()
    }

    /**
     * Handle confirm - Keep changes and finish
     */
    private fun handleConfirm() {
        setResult(RESULT_OK)
        finish()
    }
}
