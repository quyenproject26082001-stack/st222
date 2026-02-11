package poster.maker.activity_app.postershadow

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import poster.maker.R
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.*
import poster.maker.core.helper.AssetHelper
import poster.maker.core.helper.ShadowTransformation
import poster.maker.core.viewmodel.PosterEditorSharedViewModel
import poster.maker.databinding.ActivityPosterShadowBinding

/**
 * Poster Shadow Activity
 * Allows editing of poster shadow effect
 */
class PosterShadowActivity : BaseActivity<ActivityPosterShadowBinding>() {

    private val viewModel = PosterEditorSharedViewModel.getInstance()

    // Template views
    private var imgTemplate: ImageView? = null
    private var imgTemplateShadow: ImageView? = null
    private var tvName: TextView? = null
    private var tvBounty: TextView? = null

    // Snapshot to restore on cancel
    private var originalPosterShadow: Float = 0f

    override fun setViewBinding(): ActivityPosterShadowBinding {
        return ActivityPosterShadowBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Save original value for cancel
        originalPosterShadow = viewModel.posterShadow.value

        // Inflate template layout
        inflateTemplateLayout(viewModel.selectedTemplate.value)

        // Setup seekbar
        setupSeekBar()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            btnActionBarRight.setImageResource(R.drawable.ic_tick)
            btnActionBarRight.visible()
            tvCenter.text = strings(R.string.poster_shadow)
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
        // Observe poster shadow changes
        lifecycleScope.launch {
            viewModel.posterShadow.collect { shadowValue ->
                applyPosterShadow(shadowValue)
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
        tvName = posterView.findViewById(R.id.tvName)
        tvBounty = posterView.findViewById(R.id.tvBounty)

        // Load template background
        loadTemplateBackground()

        // Apply text styling from ViewModel
        applyTextStyling()

        // Apply current shadow
        applyPosterShadow(viewModel.posterShadow.value)
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

        // Reload with transformation
        val shadowRadius = shadowValue / 100f * 15f
        val shadowAlpha = shadowValue / 100f * 0.9f

        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)

        Glide.with(this)
            .load(templatePath)
            .transform(ShadowTransformation(shadowRadius, shadowAlpha))
            .into(shadowView)

        // View properties
        val viewAlpha = (shadowValue / 100f).coerceIn(0f, 1f)
        shadowView.alpha = viewAlpha

        val offsetX = shadowValue / 100f * 5f
        val offsetY = shadowValue / 100f * 5f
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY

        val scale = 1f + (shadowValue / 100f * 0.03f)
        shadowView.scaleX = scale
        shadowView.scaleY = scale

        // Additional blur (API 31+)
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
     * Setup SeekBar
     */
    private fun setupSeekBar() {
        binding.seekBarPosterShadow.progress = viewModel.posterShadow.value.toInt()
        binding.seekBarPosterShadow.onProgressChanged { progress ->
            applyPosterShadow(progress.toFloat())
            viewModel.setPosterShadow(progress.toFloat())
        }
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
     * Handle cancel - Restore original value
     */
    private fun handleCancel() {
        viewModel.setPosterShadow(originalPosterShadow)
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
