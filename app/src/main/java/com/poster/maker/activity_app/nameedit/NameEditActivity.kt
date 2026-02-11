package poster.maker.activity_app.nameedit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import jp.wasabeef.glide.transformations.BlurTransformation
import poster.maker.core.helper.ShadowTransformation
import kotlinx.coroutines.launch
import poster.maker.R
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.*
import poster.maker.core.helper.AssetHelper
import poster.maker.core.viewmodel.PosterEditorSharedViewModel
import poster.maker.data.local.entity.FontItem
import poster.maker.data.local.entity.FontSelectorAdapter
import poster.maker.databinding.ActivityNameEditBinding

/**
 * Name Edit Activity
 * Allows editing of name text, font, and spacing
 */
class NameEditActivity : BaseActivity<ActivityNameEditBinding>() {

    private val viewModel = PosterEditorSharedViewModel.getInstance()

    // Template views
    private var imgTemplate: ImageView? = null
    private var imgTemplateShadow: ImageView? = null
    private var imgAvatar: ImageView? = null
    private var imgAvatarShadow: ImageView? = null
    private var tvName: TextView? = null
    private var tvBounty: TextView? = null

    // Snapshot to restore on cancel
    private var originalNameText: String = ""
    private var originalNameFont: String = ""
    private var originalNameSpacing: Float = 0f

    // Font adapter
    private var fontAdapter: FontSelectorAdapter? = null

    // Font list
    private val fontList = listOf(
        FontItem("Roboto Bold", R.font.roboto_bold),
        FontItem("Roboto Medium", R.font.roboto_medium),
        FontItem("Roboto Regular", R.font.roboto_regular),
        FontItem("Londrina Solid", R.font.londrina_solid_regular),
        FontItem("Montserrat Bold", R.font.montserrat_bold),
        FontItem("Montserrat Medium", R.font.montserrat_medium),
        FontItem("Script Elegant 1", R.font.script_elegant_01),
        FontItem("Script Elegant 2", R.font.script_elegant_02),
        FontItem("Handwriting 1", R.font.script_handwriting_01),
        FontItem("Script Casual", R.font.script_casual),
        FontItem("Brush Style", R.font.brush_01),
        FontItem("Horror Style 1", R.font.display_horror_02),
        FontItem("Horror Style 2", R.font.display_horror_04),
        FontItem("Halloween", R.font.display_halloween),
        FontItem("Gothic", R.font.display_gothic_01),
        FontItem("Horror Style 5", R.font.display_horror_11),
        FontItem("Creative 1", R.font.display_creative_01),
        FontItem("Rounded", R.font.display_rounded),
        FontItem("Serif Classic", R.font.serif_02),
        FontItem("Signature", R.font.serif_signature),
        FontItem("Display Cultural Style", R.font.display_cultural),
        FontItem("Display Festive Style", R.font.display_festive),
        FontItem("Tream Style", R.font.treamd),
        FontItem("Ocean Style", R.font.ocen),
    )

    override fun setViewBinding(): ActivityNameEditBinding {
        return ActivityNameEditBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Save original values for cancel
        originalNameText = viewModel.nameText.value
        originalNameFont = viewModel.nameFont.value
        originalNameSpacing = viewModel.nameSpacing.value

        // Inflate template layout
        inflateTemplateLayout(viewModel.selectedTemplate.value)

        // Setup controls
        binding.edtName.setText(viewModel.nameText.value)
        applyGraphemeClusterFilter()
        setupFontSelector()
        setupSeekBar()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            btnActionBarRight.setImageResource(R.drawable.ic_tick)
            btnActionBarRight.visible()
            tvCenter.text = strings(R.string.name)
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
        // Observe name text changes
        lifecycleScope.launch {
            viewModel.nameText.collect { text ->
                tvName?.text = text
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

        // Load avatar image and apply effects
        loadAvatarImage()

        // Apply poster shadow
        applyPosterShadow(viewModel.posterShadow.value)

        // Apply current styling
        applyCurrentStyling()
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
     * Apply current styling from ViewModel
     */
    private fun applyCurrentStyling() {
        val config = viewModel.getConfig()

        // Set text
        tvName?.text = viewModel.nameText.value

        // Set color
        try {
            tvName?.setTextColor(android.graphics.Color.parseColor(config.nameColor))
        } catch (e: Exception) {
            tvName?.setTextColor(android.graphics.Color.BLACK)
        }

        // Apply font
        val fontName = viewModel.nameFont.value
        val fontResId = mapFontNameToResource(fontName)
        if (fontResId != null) {
            val typeface = androidx.core.content.res.ResourcesCompat.getFont(this, fontResId)
            tvName?.typeface = typeface
        }

        // Apply spacing
        tvName?.apply {
            letterSpacing = viewModel.nameSpacing.value
            alpha = 0f
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(
                this,
                androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
            )
            textSize = config.nameSize
            post {
                androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    this,
                    6,
                    config.nameSize.toInt(),
                    1,
                    android.util.TypedValue.COMPLEX_UNIT_SP
                )
                alpha = 1f
            }
        }

        // Apply BOUNTY values from ViewModel
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
        val bountyFontName = viewModel.bountyFont.value
        val bountyFontResId = mapFontNameToResource(bountyFontName)
        if (bountyFontResId != null) {
            val bountyTypeface = androidx.core.content.res.ResourcesCompat.getFont(this, bountyFontResId)
            tvBounty?.typeface = bountyTypeface
        }
    }

    /**
     * Setup font selector
     */
    private fun setupFontSelector() {
        val savedFontName = viewModel.nameFont.value
        val selectedIndex = fontList.indexOfFirst { it.name == savedFontName }.takeIf { it >= 0 } ?: 0
        val initialFont = fontList[selectedIndex]

        binding.tvCurrentNameFont.text = initialFont.name
        binding.tvCurrentNameFont.isSelected = true
        val initialTypeface = androidx.core.content.res.ResourcesCompat.getFont(this, initialFont.fontResId)
        tvName?.typeface = initialTypeface
        binding.tvCurrentNameFont.typeface = initialTypeface

        fontAdapter = FontSelectorAdapter(fontList, selectedIndex) { fontItem, _ ->
            binding.tvCurrentNameFont.text = fontItem.name
            val typeface = androidx.core.content.res.ResourcesCompat.getFont(this, fontItem.fontResId)
            tvName?.typeface = typeface
            binding.tvCurrentNameFont.typeface = typeface
            viewModel.setNameFont(fontItem.name)

            // Recalculate autoSize
            tvName?.apply {
                val config = viewModel.getConfig()
                alpha = 0f
                androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(
                    this, androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
                )
                textSize = config.nameSize
                post {
                    androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        this, 6, config.nameSize.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP
                    )
                    alpha = 1f
                }
            }

            binding.rvFontList.visibility = View.GONE
            binding.imgFontArrow.rotation = 0f
        }

        binding.rvFontList.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@NameEditActivity)
            adapter = fontAdapter
            isNestedScrollingEnabled = false
        }

        binding.layoutFontSelector.setOnClickListener {
            if (binding.rvFontList.visibility == View.GONE) {
                binding.rvFontList.visibility = View.VISIBLE
                binding.imgFontArrow.rotation = 180f
            } else {
                binding.rvFontList.visibility = View.GONE
                binding.imgFontArrow.rotation = 0f
            }
        }
    }

    /**
     * Setup SeekBar for spacing
     */
    private fun setupSeekBar() {
        // Set initial progress
        val initialProgress = (viewModel.nameSpacing.value * 500f).toInt()
        binding.seekBarNameSpacing.progress = initialProgress

        binding.seekBarNameSpacing.onProgressChanged { progress ->
            val spacing = progress / 500f
            tvName?.apply {
                letterSpacing = spacing
                alpha = 0f
                val config = viewModel.getConfig()
                androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(
                    this, androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
                )
                textSize = config.nameSize
                post {
                    androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        this, 6, config.nameSize.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP
                    )
                    alpha = 1f
                }
            }
            viewModel.setNameSpacing(spacing)
        }
    }

    /**
     * Apply grapheme cluster filter (max 25 characters)
     */
    private fun applyGraphemeClusterFilter() {
        val maxGraphemeClusters = 25
        val graphemeFilter = android.text.InputFilter { source, start, end, dest, dstart, dend ->
            val existingText = dest.toString()
            val sourceText = source.subSequence(start, end).toString()
            val resultText = existingText.substring(0, dstart) + sourceText + existingText.substring(dend)
            val resultCount = countGraphemeClusters(resultText)

            if (resultCount <= maxGraphemeClusters) {
                null
            } else {
                val currentCount = countGraphemeClusters(existingText)
                if (dstart == dend) {
                    val availableSpace = maxGraphemeClusters - currentCount
                    if (availableSpace > 0) {
                        truncateToGraphemeClusters(sourceText, availableSpace)
                    } else {
                        binding.edtName.post {
                            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.restartInput(binding.edtName)
                        }
                        ""
                    }
                } else {
                    val textBeforeReplacement = existingText.substring(0, dstart) + existingText.substring(dend)
                    val availableSpace = maxGraphemeClusters - countGraphemeClusters(textBeforeReplacement)
                    if (availableSpace > 0) {
                        truncateToGraphemeClusters(sourceText, availableSpace)
                    } else {
                        existingText.substring(dstart, dend)
                    }
                }
            }
        }
        binding.edtName.filters = arrayOf(graphemeFilter)

        // Text change listener
        binding.edtName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                val config = viewModel.getConfig()
                if (text.isNotEmpty() && config.hasName) {
                    tvName?.visibility = View.VISIBLE
                }
                tvName?.text = text
                tvName?.apply {
                    alpha = 0f
                    androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(
                        this, androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
                    )
                    textSize = config.nameSize
                    post {
                        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                            this, 6, config.nameSize.toInt(), 1, android.util.TypedValue.COMPLEX_UNIT_SP
                        )
                        alpha = 1f
                    }
                }
                viewModel.setNameText(text)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun truncateToGraphemeClusters(text: String, maxClusters: Int): String {
        if (text.isEmpty() || maxClusters <= 0) return ""
        val breakIterator = java.text.BreakIterator.getCharacterInstance()
        breakIterator.setText(text)
        var count = 0
        var boundary = breakIterator.first()
        while (count < maxClusters && boundary != java.text.BreakIterator.DONE) {
            boundary = breakIterator.next()
            count++
        }
        return if (boundary != java.text.BreakIterator.DONE) text.substring(0, boundary) else text
    }

    private fun countGraphemeClusters(text: String): Int {
        if (text.isEmpty()) return 0
        val breakIterator = java.text.BreakIterator.getCharacterInstance()
        breakIterator.setText(text)
        var count = 0
        breakIterator.first()
        while (breakIterator.next() != java.text.BreakIterator.DONE) {
            count++
        }
        return count
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

    private fun getCurrentImageModel(): Any {
        return viewModel.selectedImageUri.value ?: R.drawable.avatar
    }

    private fun loadAvatarImage() {
        imgAvatar?.visibility = View.VISIBLE
        val model = getCurrentImageModel()
        val blur = viewModel.filterBlur.value

        if (blur > 0) {
            val blurRadius = (blur / 100f * 25f).toInt().coerceAtLeast(1)
            imgAvatar?.let {
                Glide.with(this)
                    .load(model)
                    .transform(CenterCrop(), BlurTransformation(blurRadius, 3))
                    .into(it)
            }
        } else {
            imgAvatar?.let {
                Glide.with(this)
                    .load(model)
                    .centerCrop()
                    .into(it)
            }
        }

        applyPhotoFilters()
        applyPhotoShadow(viewModel.filterShadow.value)
    }

    private fun applyPhotoFilters() {
        val brightness = viewModel.filterBrightness.value
        val contrast = viewModel.filterContrast.value
        val saturation = viewModel.filterSaturate.value
        val grayscale = viewModel.filterGrayscale.value
        val hueRotate = viewModel.filterHueRotate.value
        val sepia = viewModel.filterSepia.value

        val colorMatrix = android.graphics.ColorMatrix()

        val brightnessMatrix = android.graphics.ColorMatrix(
            floatArrayOf(
                brightness, 0f, 0f, 0f, 0f,
                0f, brightness, 0f, 0f, 0f,
                0f, 0f, brightness, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(brightnessMatrix)

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

        val saturationMatrix = android.graphics.ColorMatrix()
        saturationMatrix.setSaturation(saturation)
        colorMatrix.postConcat(saturationMatrix)

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

        imgAvatar?.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
    }

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

        val scaleVal = 1f + (effectiveShadowValue / 100f * 0.15f)
        shadowView.scaleX = scaleVal
        shadowView.scaleY = scaleVal

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
     * Handle cancel - Restore original values
     */
    private fun handleCancel() {
        viewModel.setNameText(originalNameText)
        viewModel.setNameFont(originalNameFont)
        viewModel.setNameSpacing(originalNameSpacing)
        finish()
    }

    /**
     * Handle confirm - Keep changes and finish
     */
    private fun handleConfirm() {
        setResult(RESULT_OK)
        finish()
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.action == android.view.MotionEvent.ACTION_DOWN) {
            currentFocus?.let { view ->
                if (view is android.widget.EditText && !isTouchInsideView(view, ev)) {
                    view.clearFocus()
                    hideKeyboard(view)
                    binding.root.requestFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isTouchInsideView(view: View, event: android.view.MotionEvent): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val touchX = event.rawX.toInt()
        val touchY = event.rawY.toInt()
        return touchX in location[0]..(location[0] + view.width) &&
                touchY in location[1]..(location[1] + view.height)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
