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
import poster.maker.activity_app.success.SuccessActivity
import poster.maker.activity_app.template.TemplateListActivity
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.*
import poster.maker.core.helper.AssetHelper
import poster.maker.core.helper.MediaHelper
import poster.maker.core.helper.ShadowTransformation
import poster.maker.core.utils.state.SaveState
import poster.maker.core.viewmodel.PosterEditorSharedViewModel
import poster.maker.data.local.entity.FontItem
import poster.maker.data.local.entity.FontSelectorAdapter
import poster.maker.databinding.ActivityMakeScreenBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
//quyen
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
//quyen

/**
 * MakeScreen Activity
 * Main screen after clicking "Create" button from Home
 * Shows poster preview with 3 action buttons: Templates, Import, Edit
 */
class MakeScreenActivity : BaseActivity<ActivityMakeScreenBinding>() {

    //quyen
    var interAll: InterstitialAd? = null
    //quyen

    // Use shared ViewModel for data binding with WantedEditorActivity
    private val viewModel = PosterEditorSharedViewModel.getInstance()

    // Dynamic poster views (inflated from template layouts)
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
                // Mark editing started to switch from avatar.png to item.png
                viewModel.markEditingStarted()
                // Refresh entire preview with item.png and all elements visible
                updatePreviewWithCurrentState()
            }
        }

    // Font adapters for editing sections
    private var fontAdapter: FontSelectorAdapter? = null
    private var bountyFontAdapter: FontSelectorAdapter? = null

    // Font list for font selector
    private val fontList = listOf(
        // Basic fonts
        FontItem("Roboto Bold", R.font.roboto_bold),
        FontItem("Roboto Medium", R.font.roboto_medium),
        FontItem("Roboto Regular", R.font.roboto_regular),
        FontItem("Londrina Solid", R.font.londrina_solid_regular),
        FontItem("Montserrat Bold", R.font.montserrat_bold),
        FontItem("Montserrat Medium", R.font.montserrat_medium),

        // Script/Handwriting
        FontItem("Script Elegant 1", R.font.script_elegant_01),
        FontItem("Script Elegant 2", R.font.script_elegant_02),
        FontItem("Handwriting 1", R.font.script_handwriting_01),
        FontItem("Script Casual", R.font.script_casual),
        FontItem("Brush Style", R.font.brush_01),

        // Horror/Gothic/Halloween
        FontItem("Horror Style 1", R.font.display_horror_02),
        FontItem("Horror Style 2", R.font.display_horror_04),
        FontItem("Halloween", R.font.display_halloween),
        FontItem("Gothic", R.font.display_gothic_01),
        FontItem("Horror Style 5", R.font.display_horror_11),

        // Display/Decorative
        FontItem("Creative 1", R.font.display_creative_01),
        FontItem("Rounded", R.font.display_rounded),
        // Serif Elegant
        FontItem("Serif Classic", R.font.serif_02),
        FontItem("Signature", R.font.serif_signature),

        FontItem("Display Cultural Style", R.font.display_cultural),
        FontItem("Display Festive Style", R.font.display_festive),
        FontItem("Tream Style", R.font.treamd),
        FontItem("Ocean Style", R.font.ocen),
    )

    // Request code for Template selection
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

                    // Update bounty text with new prefix/suffix (keep the number)
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
        // Check if template was selected from Intent (for direct navigation)
        val selectedTemplateFromIntent = intent.getIntExtra("selectedTemplateId", -1)
        if (selectedTemplateFromIntent != -1) {
            // User selected a specific template from another screen
            // Don't mark editing - just selecting template shows avatar.webp preview
            viewModel.setSelectedTemplate(selectedTemplateFromIntent)
        }

        // Load template layout and background from assets
        inflateTemplateLayout(viewModel.selectedTemplate.value)

        // Initialize with default template and preview
        updatePreviewWithCurrentState()

        // Setup editing controls (initially hidden)
        applyGraphemeClusterFilter()
        setupFontSelector()
        setupBountyFontSelector()
        setupEditTexts()
        setupSeekBars()
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

        // Setup autoSize for tvName to handle long text (SAME AS WantedEditor)
        tvName?.apply {
            maxLines = 1
            val config = viewModel.getConfig()

            // Disable autoSize first to clear any XML configuration
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(
                this,
                androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
            )

            // Enable autoSize with same config as WantedEditor
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this,
                6,      // minTextSize: 6sp (allow more shrinking for very long text)
                config.nameSize.toInt(),     // maxTextSize from template config
                1,      // granularity: 1sp step
                android.util.TypedValue.COMPLEX_UNIT_SP
            )

            android.util.Log.d(
                "MakeScreenAutoSize",
                "Setup autoSize for tvName: min=6sp, max=${config.nameSize.toInt()}sp"
            )
        }

        // Load template background
        loadTemplateBackground()
    }

    override fun viewListener() {
        binding.apply {
            // Action bar
            actionBar.apply {
                btnActionBarLeft.setOnSingleClick { showInterAll {  handleBack() } }
                btnActionBarRightText.setOnSingleClick(2000) {  handleSave()  }
            }

            // Templates button - Navigate to Template Selection Screen
            cvTemplates.setOnSingleClick {
                val intent =
                    Intent(this@MakeScreenActivity, TemplateListActivity::class.java).apply {
                        putExtra("currentTemplateId", viewModel.selectedTemplate.value)
                    }
                templateSelectionLauncher.launch(intent)
            }

            // Import button - Pick image from gallery
            cvImport.setOnSingleClick(2000) {
                pickImageLauncher.launch("image/*")
            }

            // Action buttons for editing sections
            cvName.setOnSingleClick {
                showEditingSection(R.id.cardNameSection)
            }

            cvBounty.setOnSingleClick {
                showEditingSection(R.id.cardBountySection)
            }

            cvPhotoFilter.setOnSingleClick {
                showEditingSection(R.id.cardPhotoFilterSection)
            }

            cvPosterShadow.setOnSingleClick {
                showEditingSection(R.id.cardPosterShadowSection)
            }

            // Close button in action bar (when editing section is open)
            actionBar.btnActionBarRight.setOnSingleClick {
                if (isEditingSectionVisible()) {
                    // Close editing section
                    hideEditingSections()
                } else {
                    // This is the save button (handled elsewhere)
                    handleSave()
                }
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
                // Log text size after layout
                tvName?.post {
                    val config = viewModel.getConfig()
                    android.util.Log.d(
                        "MakeScreenSize",
                        "Template ${viewModel.selectedTemplate.value} - MAKESCREEN tvName: configMaxSize=${config.nameSize}f, actualTextSize=${tvName?.textSize}px, text='$text'"
                    )
                }
            }
        }

        // Observe bounty text
        lifecycleScope.launch {
            viewModel.bountyText.collect { text ->
                tvBounty?.text = text
                // Log text size after layout
                tvBounty?.post {
                    val config = viewModel.getConfig()
                    android.util.Log.d(
                        "MakeScreenSize",
                        "Template ${viewModel.selectedTemplate.value} - MAKESCREEN tvBounty: configSize=${config.bountySize}f, actualTextSize=${tvBounty?.textSize}px, text='$text'"
                    )
                }
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

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            // Always show Save button (don't hide it)
            btnActionBarRightText.visible()
            tvRightText.visible()
            tvRightText.isSelected = true  // Activate marquee scrolling
            tvCenter.text = strings(R.string.wanted_poster_maker)
            tvCenter.visible()
            tvCenter.isSelected = true  // Activate marquee for title too
            btnActionBarRight.gone()
            btnActionBarReset.gone()
        }
    }

//    //quyen
//    override fun initAds() {
//        Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_collap_poster), binding.nativeClPoster)
//    }
    //quyen

    //quyen
    override fun onRestart() {
        super.onRestart()
     //   Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_collap_poster), binding.nativeClPoster)
    }
    //quyen

    /**
     * Show Save button (call when editing starts)
     */
    private fun showSaveButton() {
        binding.actionBar.apply {
            btnActionBarRightText.visible()
            tvRightText.visible()
        }
    }

    /**
     * Handle back button with "Discard changes?" dialog if edited
     */
    private fun handleBack() {
        // Check if any editing section is visible
        if (isEditingSectionVisible()) {
            // Close editing section and show action buttons
            hideEditingSections()
            return
        }

        //quyen
                //quyen
                // Check isEditingStarted instead of hasChanges to avoid showing dialog
                // when user only changed template without importing image or editing
                if (viewModel.isEditingStarted.value) {
                    // TODO: Show dialog "Discard changes?"
                    // For now, just finish
                    // Clear all data so when coming back, it shows default state (avatar.webp)
                    viewModel.clearAll()
                    finishAfterTransition()
                } else {
                    // No edits made, just clear and finish
                    viewModel.clearAll()
                    finishAfterTransition()
                }
                //quyen
        //quyen
    }

    /**
     * Handle save button - Save poster and navigate to SuccessActivity
     * Only validates that user has entered a custom name (if template has name field)
     */
    private fun handleSave() {
        // Validation: Check if user has customized the name
        val config = viewModel.getConfig()
        val currentNameText = viewModel.nameText.value

        // Check: Name must be changed and not empty (for templates that have name)
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

        //quyen

                //quyen
        showInterAll {}
            // Capture poster view as bitmap
            val posterView = binding.containerPoster
            if (posterView.width == 0 || posterView.height == 0) {
                showToast(strings(R.string.download_failed_please_try_again_later))
                return
            }

            val bitmap =
                Bitmap.createBitmap(posterView.width, posterView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            posterView.draw(canvas)

            // Save bitmap to internal storage
            lifecycleScope.launch {
                MediaHelper.saveBitmapToInternalStorage(this@MakeScreenActivity, "posters", bitmap)
                    .collectLatest { state ->
                        when (state) {
                            is SaveState.Success -> {
                                // Set path in ViewModel and navigate to SuccessActivity
                                viewModel.setSavedImagePath(state.path)
                                val intent =
                                    Intent(this@MakeScreenActivity, SuccessActivity::class.java)
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
         //quyen
        //quyen
    }

    /**
     * Load template background from assets
     * Uses avatar.png when no edits, item.png after user edits
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
     * Reload image with blur transformation (for Android 8-11)
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
     * Apply shadow effect to imgAvatarShadow layer
     */
    private fun applyShadowEffect(shadowValue: Float) {
        val shadowView = imgAvatarShadow ?: return

        if (shadowValue <= 0) {
            shadowView.visibility = View.GONE
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                shadowView.setRenderEffect(null)
            }
            return
        }

        shadowView.visibility = View.VISIBLE

        // Remap: seekbar 0-100 → effective shadow 35-100
        val effectiveShadowValue = 35f + (shadowValue / 100f * 65f)

        // Reload shadow with new transformation
        val currentUri = viewModel.selectedImageUri.value
        if (currentUri != null) {
            val shadowRadius = effectiveShadowValue / 100f * 15f  // 35-100 → 5.25-15px
            val shadowAlpha = effectiveShadowValue / 100f * 0.9f  // 35-100 → 0.315-0.9

            Glide.with(this)
                .load(currentUri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(shadowView)
        }

        // View properties
        val viewAlpha = (effectiveShadowValue / 100f).coerceIn(0f, 1f)  // 35-100 → 0.35-1.0
        shadowView.alpha = viewAlpha

        val offsetX = effectiveShadowValue / 100f * 5f  // 35-100 → 1.75-5dp
        val offsetY = effectiveShadowValue / 100f * 5f  // 35-100 → 1.75-5dp
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY

        val scale = 1f + (effectiveShadowValue / 100f * 0.15f)  // 35-100 → 1.0525-1.15
        shadowView.scaleX = scale
        shadowView.scaleY = scale

        // Additional blur (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur = effectiveShadowValue / 100f * 10f  // 35-100 → 3.5-10px
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
     * Load image into preview
     */
    private fun loadImageToPreview(uri: Uri) {
        android.util.Log.d("AvatarDebug", "═══════════════════════════════════════")
        android.util.Log.d("AvatarDebug", "loadImageToPreview() called")
        android.util.Log.d("AvatarDebug", "URI: $uri")
        android.util.Log.d("AvatarDebug", "imgAvatar: ${imgAvatar != null}")
        android.util.Log.d("AvatarDebug", "imgAvatarShadow: ${imgAvatarShadow != null}")

        // NOTE: Use Glide BlurTransformation for ALL Android versions (consistent with WantedEditor)
        // Apply blur via Glide transformation regardless of Android version
        val blur = viewModel.filterBlur.value
        android.util.Log.d("AvatarDebug", "blur value: $blur")

        if (blur > 0) {
            android.util.Log.d("AvatarDebug", "Loading with blur to imgAvatar")
            reloadImageWithBlur(uri, blur)
        } else {
            // Load into main avatar without blur
            android.util.Log.d("AvatarDebug", "Loading without blur to imgAvatar")
            imgAvatar?.let { imageView ->
                android.util.Log.d(
                    "AvatarDebug",
                    "imgAvatar visibility BEFORE load: ${imageView.visibility}"
                )
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imageView)
                android.util.Log.d("AvatarDebug", "Glide load started for imgAvatar")
            }
        }

        // Load into shadow layer with ShadowTransformation (contour shadow)
        // Only load if shadow is enabled (> 0) to avoid duplicate avatar display
        val shadowValue = viewModel.filterShadow.value
        android.util.Log.d("AvatarDebug", "shadowValue: $shadowValue")

        if (shadowValue > 0) {
            val shadowRadius = shadowValue / 100f * 15f
            val shadowAlpha = 0.8f
            android.util.Log.d(
                "AvatarDebug",
                "Loading shadow: radius=$shadowRadius, alpha=$shadowAlpha"
            )

            imgAvatarShadow?.let { imageView ->
                android.util.Log.d(
                    "AvatarDebug",
                    "imgAvatarShadow visibility BEFORE load: ${imageView.visibility}"
                )
                imageView.visibility = View.VISIBLE
                Glide.with(this)
                    .load(uri)
                    .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                    .into(imageView)
                android.util.Log.d("AvatarDebug", "Glide load started for imgAvatarShadow")
            }
        } else {
            android.util.Log.d("AvatarDebug", "Shadow disabled - Hiding imgAvatarShadow")
            imgAvatarShadow?.let { imageView ->
                android.util.Log.d(
                    "AvatarDebug",
                    "imgAvatarShadow visibility BEFORE hide: ${imageView.visibility}"
                )
                imageView.visibility = View.GONE
                // Clear any existing image to prevent lingering
                imageView.setImageDrawable(null)
                android.util.Log.d("AvatarDebug", "imgAvatarShadow set to GONE and cleared")
            }
        }
        android.util.Log.d("AvatarDebug", "═══════════════════════════════════════")
    }

    /**
     * Update preview with current ViewModel state
     * Called after receiving edited data from WantedEditorActivity
     */
    private fun updatePreviewWithCurrentState() {
        android.util.Log.d("AvatarDebug", "")
        android.util.Log.d("AvatarDebug", "╔═══════════════════════════════════════╗")
        android.util.Log.d("AvatarDebug", "║   updatePreviewWithCurrentState()    ║")
        android.util.Log.d("AvatarDebug", "╚═══════════════════════════════════════╝")

        // Reload template layout if template changed
        val currentTemplateId = viewModel.selectedTemplate.value
        inflateTemplateLayout(currentTemplateId)

        val isEditing = viewModel.isEditingStarted.value
        val selectedImageUri = viewModel.selectedImageUri.value
        val config = viewModel.getConfig()

        android.util.Log.d("AvatarDebug", "isEditing: $isEditing")
        android.util.Log.d("AvatarDebug", "selectedImageUri: $selectedImageUri")
        android.util.Log.d("AvatarDebug", "filterShadow: ${viewModel.filterShadow.value}")

        // Show/hide editable elements based on editing state
        if (isEditing) {
            android.util.Log.d("SaveDebug", "───────────────────────────────────────")
            android.util.Log.d("SaveDebug", "MAKE SCREEN - updatePreviewWithCurrentState()")
            android.util.Log.d("SaveDebug", "isEditing: $isEditing")
            android.util.Log.d("SaveDebug", "config.hasName: ${config.hasName}")
            android.util.Log.d("SaveDebug", "Setting tvName.text to: '${viewModel.nameText.value}'")
            android.util.Log.d(
                "SaveDebug",
                "Setting tvBounty.text to: '${viewModel.bountyText.value}'"
            )
            android.util.Log.d("SaveDebug", "tvName is null: ${tvName == null}")
            android.util.Log.d("SaveDebug", "tvBounty is null: ${tvBounty == null}")

            // Show name only if template has name field (XML already sets visibility)
            // Just update text values
            if (config.hasName) {
                tvName?.visibility = View.VISIBLE
                tvName?.text = viewModel.nameText.value
                android.util.Log.d("SaveDebug", "After set - tvName.text: '${tvName?.text}'")
            } else {
                tvName?.visibility = View.GONE
            }

            tvBounty?.visibility = View.VISIBLE
            tvBounty?.text = viewModel.bountyText.value
            android.util.Log.d("SaveDebug", "After set - tvBounty.text: '${tvBounty?.text}'")
            android.util.Log.d("SaveDebug", "───────────────────────────────────────")

            imgAvatar?.visibility = View.VISIBLE
            imgAvatarShadow?.visibility = View.VISIBLE

            android.util.Log.d("AvatarDebug", "Set imgAvatar VISIBLE")
            android.util.Log.d("AvatarDebug", "Set imgAvatarShadow VISIBLE")

            // Update image if exists, otherwise show default avatar
            viewModel.selectedImageUri.value?.let { uri ->
                android.util.Log.d("AvatarDebug", "Loading image from URI: $uri")
                loadImageToPreview(uri)
            } ?: run {
                // Load default avatar.webp from drawable
                android.util.Log.d(
                    "AvatarDebug",
                    "No URI - loading default avatar.webp from drawable"
                )
                imgAvatar?.let { imageView ->
                    Glide.with(this)
                        .load(R.drawable.avatar)
                        .centerCrop()
                        .into(imageView)
                }
                imgAvatarShadow?.let { imageView ->
                    val shadowRadius = viewModel.filterShadow.value / 100f * 15f
                    val shadowAlpha = 0.8f
                    Glide.with(this)
                        .load(R.drawable.avatar)
                        .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                        .into(imageView)
                }
            }

            // Apply ALL effects from ViewModel to match Editor
            android.util.Log.d("AvatarDebug", "Calling applyAllEffectsFromViewModel()")
            applyAllEffectsFromViewModel()
            android.util.Log.d(
                "AvatarDebug",
                "After effects - imgAvatar visibility: ${imgAvatar?.visibility}"
            )
            android.util.Log.d(
                "AvatarDebug",
                "After effects - imgAvatarShadow visibility: ${imgAvatarShadow?.visibility}"
            )
        } else {
            android.util.Log.d("AvatarDebug", "isEditing = false - hiding all elements")
            // Hide all editable elements - show only avatar.png preview
            tvName?.visibility = View.GONE
            tvBounty?.visibility = View.GONE
            imgAvatar?.visibility = View.GONE
            imgAvatarShadow?.visibility = View.GONE
            imgTemplateShadow?.visibility = View.GONE
        }
        android.util.Log.d("AvatarDebug", "╚═══════════════════════════════════════╝")
    }

    /**
     * Apply all effects from ViewModel to preview
     * This ensures MakeScreen preview matches Editor preview exactly
     */
    private fun applyAllEffectsFromViewModel() {
        val config = viewModel.getConfig()

        // Apply template colors and base sizes
        try {
            if (config.hasName) {
                tvName?.setTextColor(Color.parseColor(config.nameColor))
                // NOTE: Do NOT set textSize here - autoSize is already configured in inflateTemplateLayout()
                // Setting textSize manually will disable autoSize and cause text clipping
            }
            tvBounty?.setTextColor(Color.parseColor(config.bountyColor))
            // Note: bounty size is overridden below by user's custom size
        } catch (e: Exception) {
            tvName?.setTextColor(Color.BLACK)
            tvBounty?.setTextColor(Color.BLACK)
        }

        // Apply font typeface to tvName
        val fontName = viewModel.nameFont.value
        val fontResId = mapFontNameToResource(fontName)
        if (fontResId != null) {
            val typeface = androidx.core.content.res.ResourcesCompat.getFont(this, fontResId)
            tvName?.typeface = typeface
        }

        // ✅ THÊM ĐOẠN NÀY - Apply font typeface to tvBounty
        val bountyFontName = viewModel.bountyFont.value
        val bountyFontResId = mapFontNameToResource(bountyFontName)
        if (bountyFontResId != null) {
            val bountyTypeface =
                androidx.core.content.res.ResourcesCompat.getFont(this, bountyFontResId)
            tvBounty?.typeface = bountyTypeface
        }

        // Name effects
        tvName?.apply {
            letterSpacing = viewModel.nameSpacing.value

            // Force TextView to recalculate auto-size when spacing changes
            // SAME PATTERN as WantedEditor: Disable → Reset to max → Re-enable
            if (config.hasName) {
                // Hide text to prevent flicker during recalculation
                alpha = 0f

                // Step 1: Disable auto-size
                androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(
                    this,
                    androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
                )

                // Step 2: Force expand to max size
                textSize = config.nameSize

                // Step 3: Re-enable auto-size after TextView has expanded
                post {
                    androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        this,
                        6,      // minTextSize
                        config.nameSize.toInt(),     // maxTextSize from config
                        1,      // granularity: 1sp step
                        android.util.TypedValue.COMPLEX_UNIT_SP
                    )

                    // Show text again after recalculation
                    alpha = 1f
                }
            }
        }

        // Bounty effects (override config values with user's customization)
        tvBounty?.textSize = viewModel.bountySize.value
        tvBounty?.letterSpacing = viewModel.bountySpacing.value
        tvBounty?.translationX = viewModel.bountyPositionX.value
        tvBounty?.translationY = viewModel.bountyPositionY.value

        // Log text sizes for debugging
        tvName?.post {
            val nameTextSizePx = tvName?.textSize ?: 0f
            val nameTextSizeSp = nameTextSizePx / resources.displayMetrics.scaledDensity
            android.util.Log.d("TextSizeDebug", "═══════════════════════════════════════")
            android.util.Log.d("TextSizeDebug", "MAKE SCREEN - applyAllEffectsFromViewModel()")
            android.util.Log.d("TextSizeDebug", "Template: ${config.id}")
            android.util.Log.d(
                "TextSizeDebug",
                "tvName textSize: ${nameTextSizeSp.toInt()}sp (${nameTextSizePx}px)"
            )
            android.util.Log.d("TextSizeDebug", "config.nameSize: ${config.nameSize}sp")
            val bountyTextSizePx = tvBounty?.textSize ?: 0f
            val bountyTextSizeSp = bountyTextSizePx / resources.displayMetrics.scaledDensity
            android.util.Log.d(
                "TextSizeDebug",
                "tvBounty textSize: ${bountyTextSizeSp.toInt()}sp (${bountyTextSizePx}px)"
            )
            android.util.Log.d(
                "TextSizeDebug",
                "viewModel.bountySize: ${viewModel.bountySize.value}sp"
            )
            android.util.Log.d("TextSizeDebug", "═══════════════════════════════════════")
        }

        // Photo filters
        applyPhotoFilters()

        // Shadows
        applyPosterShadow(viewModel.posterShadow.value)
        applyPhotoShadow(viewModel.filterShadow.value)
    }

    /**
     * Apply photo filters to imgAvatar
     * EXACTLY LIKE WantedEditorActivity.applyFilters()
     */
    private fun applyPhotoFilters() {
        val brightness = viewModel.filterBrightness.value
        val contrast = viewModel.filterContrast.value
        val saturation = viewModel.filterSaturate.value
        val grayscale = viewModel.filterGrayscale.value
        val hueRotate = viewModel.filterHueRotate.value
        val sepia = viewModel.filterSepia.value
        val blur = viewModel.filterBlur.value

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
                    invGrayscale + grayscale * 0.299f,
                    grayscale * 0.587f,
                    grayscale * 0.114f,
                    0f,
                    0f,
                    grayscale * 0.299f,
                    invGrayscale + grayscale * 0.587f,
                    grayscale * 0.114f,
                    0f,
                    0f,
                    grayscale * 0.299f,
                    grayscale * 0.587f,
                    invGrayscale + grayscale * 0.114f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    1f,
                    0f
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
                    0.213f + cosA * 0.787f - sinA * 0.213f,
                    0.715f - cosA * 0.715f - sinA * 0.715f,
                    0.072f - cosA * 0.072f + sinA * 0.928f,
                    0f,
                    0f,
                    0.213f - cosA * 0.213f + sinA * 0.143f,
                    0.715f + cosA * 0.285f + sinA * 0.140f,
                    0.072f - cosA * 0.072f - sinA * 0.283f,
                    0f,
                    0f,
                    0.213f - cosA * 0.213f - sinA * 0.787f,
                    0.715f - cosA * 0.715f + sinA * 0.715f,
                    0.072f + cosA * 0.928f + sinA * 0.072f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    1f,
                    0f
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

        // Apply Blur
        // NOTE: Use Glide BlurTransformation for ALL Android versions (consistent with WantedEditor)
        // RenderEffect on Android 12+ was causing zoom artifacts, so we use Glide instead

        // Clear any existing RenderEffect
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            imgAvatar?.setRenderEffect(null)
        }

        // Apply blur using Glide for ALL Android versions
        val currentUri = viewModel.selectedImageUri.value
        if (currentUri != null) {
            reloadImageWithBlur(currentUri, blur)
        }
    }

    /**
     * Apply poster shadow effect (template shadow)
     * EXACTLY LIKE WantedEditorActivity.applyTemplateShadow()
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

        // EXACTLY LIKE Photo Filter: Reload with new transformation parameters
        val shadowRadius = shadowValue / 100f * 15f  // 0-15px blur (SAME as Photo Filter)
        val shadowAlpha = shadowValue / 100f * 0.9f   // Dynamic alpha

        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)

        Glide.with(this)
            .load(templatePath)
            .transform(ShadowTransformation(shadowRadius, shadowAlpha))
            .into(shadowView)

        // View properties
        val viewAlpha = (shadowValue / 100f).coerceIn(0f, 1f)
        shadowView.alpha = viewAlpha

        val offsetX = shadowValue / 100f * 5f   // 0-5dp (SAME as Photo Filter)
        val offsetY = shadowValue / 100f * 5f   // 0-5dp (SAME as Photo Filter)
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY

        val scale = 1f + (shadowValue / 100f * 0.03f)  // 1.0-1.03 (SAME as Photo Filter)
        shadowView.scaleX = scale
        shadowView.scaleY = scale

        // Additional blur (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur =
                shadowValue / 100f * 10f  // 0-10px additional blur (SAME as Photo Filter)
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
     * Apply photo shadow effect (avatar shadow)
     * EXACTLY LIKE WantedEditorActivity.applyShadowEffect()
     */
    private fun applyPhotoShadow(shadowValue: Float) {
        android.util.Log.d("AvatarDebug", "───────────────────────────────────────")
        android.util.Log.d("AvatarDebug", "applyPhotoShadow() called")
        android.util.Log.d("AvatarDebug", "shadowValue: $shadowValue")

        val shadowView = imgAvatarShadow
        android.util.Log.d("AvatarDebug", "imgAvatarShadow is null: ${shadowView == null}")

        if (shadowView == null) return

        android.util.Log.d(
            "AvatarDebug",
            "imgAvatarShadow visibility BEFORE: ${shadowView.visibility}"
        )

        if (shadowValue <= 0) {
            android.util.Log.d("AvatarDebug", "Shadow <= 0 → Setting imgAvatarShadow to GONE")
            shadowView.visibility = View.GONE
            android.util.Log.d(
                "AvatarDebug",
                "imgAvatarShadow visibility AFTER set GONE: ${shadowView.visibility}"
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                shadowView.setRenderEffect(null)
            }
            android.util.Log.d("AvatarDebug", "───────────────────────────────────────")
            return
        }

        android.util.Log.d("AvatarDebug", "Shadow > 0 → Setting imgAvatarShadow to VISIBLE")
        shadowView.visibility = View.VISIBLE
        android.util.Log.d(
            "AvatarDebug",
            "imgAvatarShadow visibility AFTER set VISIBLE: ${shadowView.visibility}"
        )

        // Remap: seekbar 0-100 → effective shadow 35-100
        val effectiveShadowValue = 35f + (shadowValue / 100f * 65f)

        // Reload shadow with new transformation
        val currentUri = viewModel.selectedImageUri.value
        if (currentUri != null) {
            val shadowRadius = effectiveShadowValue / 100f * 15f  // 35-100 → 5.25-15px
            val shadowAlpha = effectiveShadowValue / 100f * 0.9f  // 35-100 → 0.315-0.9

            Glide.with(this)
                .load(currentUri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(shadowView)
        }

        // View properties
        val viewAlpha = (effectiveShadowValue / 100f).coerceIn(0f, 1f)  // 35-100 → 0.35-1.0
        shadowView.alpha = viewAlpha

        val offsetX = effectiveShadowValue / 100f * 5f  // 35-100 → 1.75-5dp
        val offsetY = effectiveShadowValue / 100f * 5f  // 35-100 → 1.75-5dp
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY

        val scale =
            1f + (effectiveShadowValue / 100f * 0.15f)  // 35-100 → 1.0525-1.15 (~10% difference)
        shadowView.scaleX = scale
        shadowView.scaleY = scale

        // Additional blur (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val additionalBlur = effectiveShadowValue / 100f * 10f  // 35-100 → 3.5-10px
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

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * Map font name to font resource ID
     * MUST MATCH the font list in WantedEditorActivity
     */
    private fun mapFontNameToResource(fontName: String): Int? {
        return when (fontName) {
            // Basic fonts
            "Roboto Bold" -> R.font.roboto_bold
            "Roboto Medium" -> R.font.roboto_medium
            "Roboto Regular" -> R.font.roboto_regular
            "Londrina Solid" -> R.font.londrina_solid_regular
            "Montserrat Bold" -> R.font.montserrat_bold
            "Montserrat Medium" -> R.font.montserrat_medium

            // Script/Handwriting (9 fonts)
            "Script Elegant 1" -> R.font.script_elegant_01
            "Script Elegant 2" -> R.font.script_elegant_02
            "Handwriting 1" -> R.font.script_handwriting_01
            "Script Casual" -> R.font.script_casual
            "Brush Style" -> R.font.brush_01

            // Horror/Gothic/Halloween (8 fonts)
            "Horror Style 1" -> R.font.display_horror_02
            "Horror Style 2" -> R.font.display_horror_04
            "Halloween" -> R.font.display_halloween
            "Gothic" -> R.font.display_gothic_01
            "Horror Style 5" -> R.font.display_horror_11

            // Display/Decorative (8 fonts)
            "Creative 1" -> R.font.display_creative_01
            "Rounded" -> R.font.display_rounded

            // Serif Elegant (5 fonts)
            "Serif Classic" -> R.font.serif_02
            "Signature" -> R.font.serif_signature

            "Display Cultural Style" -> R.font.display_cultural
            "Display Festive Style" -> R.font.display_festive
            "Tream Style" -> R.font.treamd
            "Ocean Style" -> R.font.ocen
            else -> null  // Return null for unknown fonts
        }
    }

    // ====== EDITING UTILITY METHODS (from WantedEditorActivity) ======

    /**
     * Setup font selector for Name field
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
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MakeScreenActivity)
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
     * Setup font selector for Bounty field
     */
    private fun setupBountyFontSelector() {
        val savedFontName = viewModel.bountyFont.value
        val selectedIndex = fontList.indexOfFirst { it.name == savedFontName }.takeIf { it >= 0 } ?: 0
        val initialFont = fontList[selectedIndex]

        binding.tvCurrentNameFontBounty.text = initialFont.name
        binding.tvCurrentNameFontBounty.isSelected = true
        val initialTypeface = androidx.core.content.res.ResourcesCompat.getFont(this, initialFont.fontResId)
        tvBounty?.typeface = initialTypeface
        binding.tvCurrentNameFontBounty.typeface = initialTypeface

        bountyFontAdapter = FontSelectorAdapter(fontList, selectedIndex) { fontItem, _ ->
            binding.tvCurrentNameFontBounty.text = fontItem.name
            val typeface = androidx.core.content.res.ResourcesCompat.getFont(this, fontItem.fontResId)
            tvBounty?.typeface = typeface
            binding.tvCurrentNameFontBounty.typeface = typeface
            viewModel.setBountyFont(fontItem.name)

            binding.rvFontBountyList.visibility = View.GONE
            binding.imgFontBountyArrow.rotation = 0f
        }

        binding.rvFontBountyList.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MakeScreenActivity)
            adapter = bountyFontAdapter
            isNestedScrollingEnabled = false
        }

        binding.layoutFontBountySelector.setOnClickListener {
            if (binding.rvFontBountyList.visibility == View.GONE) {
                binding.rvFontBountyList.visibility = View.VISIBLE
                binding.imgFontBountyArrow.rotation = 180f
            } else {
                binding.rvFontBountyList.visibility = View.GONE
                binding.imgFontBountyArrow.rotation = 0f
            }
        }
    }

    /**
     * Apply grapheme cluster filter to name input (max 25 visible characters)
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
     * Setup EditText listeners for Name and Bounty
     */
    private fun setupEditTexts() {
        binding.edtName.setOnEditorActionListener { view, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event?.action == android.view.KeyEvent.ACTION_DOWN && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                view.clearFocus()
                hideKeyboard(view)
                binding.root.requestFocus()
                true
            } else false
        }

        binding.edtBounty.setOnEditorActionListener { view, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event?.action == android.view.KeyEvent.ACTION_DOWN && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                view.clearFocus()
                hideKeyboard(view)
                binding.root.requestFocus()
                true
            } else false
        }

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

        binding.edtBounty.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                if (text.isNotEmpty()) {
                    tvBounty?.visibility = View.VISIBLE
                }
                tvBounty?.text = text
                viewModel.setBountyText(text)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
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

    /**
     * Setup all seekbars for editing controls
     */
    private fun setupSeekBars() {
        // Name Spacing
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

        // Bounty Size
        binding.seekBarBountySize.onProgressChanged { progress ->
            val size = 12f + (progress / 100f) * 48f
            tvBounty?.textSize = size
            viewModel.setBountySize(size)
        }

        // Bounty Weight
        binding.seekBarBountyWeight.onProgressChanged { progress ->
            viewModel.setBountyWeight(progress.toFloat())
        }

        // Bounty Spacing
        binding.seekBarBountySpacing.onProgressChanged { progress ->
            val spacing = progress / 500f
            tvBounty?.letterSpacing = spacing
            viewModel.setBountySpacing(spacing)
        }

        // Bounty Position X
        binding.seekBarBountyPositionX.onProgressChanged { progress ->
            val offsetX = (progress - 50) * 2f
            tvBounty?.translationX = offsetX
            viewModel.setBountyPositionX(offsetX)
        }

        // Bounty Position Y
        binding.seekBarBountyPositionY.onProgressChanged { progress ->
            val offsetY = (progress - 50) * 2f
            tvBounty?.translationY = offsetY
            viewModel.setBountyPositionY(offsetY)
        }

        setupPhotoFilterSeekBars()
        setupPosterShadowSeekBar()
    }

    private fun setupPhotoFilterSeekBars() {
        // Shadow
        binding.seekBarFilterShadow.onProgressChanged { progress ->
            viewModel.setFilterShadow(progress.toFloat())
            applyShadowEffect(progress.toFloat())
        }

        // Blur
        binding.seekBarFilterBlur.onProgressChanged { progress ->
            viewModel.setFilterBlur(progress.toFloat())
            viewModel.selectedImageUri.value?.let { uri ->
                reloadImageWithBlur(uri, progress.toFloat())
            }
            applyFilters()
        }

        // Brightness
        binding.seekBarFilterBrightness.onProgressChanged { progress ->
            val brightness = progress / 100f
            viewModel.setFilterBrightness(brightness)
            applyFilters()
        }

        // Contrast
        binding.seekBarFilterContrast.onProgressChanged { progress ->
            val contrast = progress / 100f
            viewModel.setFilterContrast(contrast)
            applyFilters()
        }

        // Grayscale
        binding.seekBarFilterGrayscale.onProgressChanged { progress ->
            val grayscale = progress / 100f
            viewModel.setFilterGrayscale(grayscale)
            applyFilters()
        }

        // Hue Rotate
        binding.seekBarFilterHueRotate.onProgressChanged { progress ->
            val hueRotate = progress.toFloat()
            viewModel.setFilterHueRotate(hueRotate)
            applyFilters()
        }

        // Saturate
        binding.seekBarFilterSaturate.onProgressChanged { progress ->
            val saturate = progress / 100f
            viewModel.setFilterSaturate(saturate)
            applyFilters()
        }

        // Sepia
        binding.seekBarFilterSepia.onProgressChanged { progress ->
            val sepia = progress / 100f
            viewModel.setFilterSepia(sepia)
            applyFilters()
        }
    }

    private fun setupPosterShadowSeekBar() {
        binding.seekBarPosterShadow.onProgressChanged { progress ->
            applyTemplateShadow(progress.toFloat())
            viewModel.setPosterShadow(progress.toFloat())
        }
    }

    private fun applyFilters() {
        lifecycleScope.launch {
            val brightness = viewModel.filterBrightness.value
            val contrast = viewModel.filterContrast.value
            val saturation = viewModel.filterSaturate.value
            val grayscale = viewModel.filterGrayscale.value
            val hueRotate = viewModel.filterHueRotate.value
            val sepia = viewModel.filterSepia.value

            val colorMatrix = android.graphics.ColorMatrix()

            val brightnessMatrix = android.graphics.ColorMatrix(floatArrayOf(
                brightness, 0f, 0f, 0f, 0f,
                0f, brightness, 0f, 0f, 0f,
                0f, 0f, brightness, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(brightnessMatrix)

            val scale = contrast
            val translate = (1f - contrast) / 2f * 255f
            val contrastMatrix = android.graphics.ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(contrastMatrix)

            val saturationMatrix = android.graphics.ColorMatrix()
            saturationMatrix.setSaturation(saturation)
            colorMatrix.postConcat(saturationMatrix)

            if (grayscale > 0) {
                val invGrayscale = 1 - grayscale
                val grayscaleMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    invGrayscale + grayscale * 0.299f, grayscale * 0.587f, grayscale * 0.114f, 0f, 0f,
                    grayscale * 0.299f, invGrayscale + grayscale * 0.587f, grayscale * 0.114f, 0f, 0f,
                    grayscale * 0.299f, grayscale * 0.587f, invGrayscale + grayscale * 0.114f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(grayscaleMatrix)
            }

            if (hueRotate != 0f) {
                val angle = hueRotate * Math.PI.toFloat() / 180f
                val cosA = kotlin.math.cos(angle.toDouble()).toFloat()
                val sinA = kotlin.math.sin(angle.toDouble()).toFloat()
                val hueRotateMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    0.213f + cosA * 0.787f - sinA * 0.213f, 0.715f - cosA * 0.715f - sinA * 0.715f, 0.072f - cosA * 0.072f + sinA * 0.928f, 0f, 0f,
                    0.213f - cosA * 0.213f + sinA * 0.143f, 0.715f + cosA * 0.285f + sinA * 0.140f, 0.072f - cosA * 0.072f - sinA * 0.283f, 0f, 0f,
                    0.213f - cosA * 0.213f - sinA * 0.787f, 0.715f - cosA * 0.715f + sinA * 0.715f, 0.072f + cosA * 0.928f + sinA * 0.072f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(hueRotateMatrix)
            }

            if (sepia > 0) {
                val invSepia = 1 - sepia
                val sepiaMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    invSepia + sepia * 0.393f, sepia * 0.769f, sepia * 0.189f, 0f, 0f,
                    sepia * 0.349f, invSepia + sepia * 0.686f, sepia * 0.168f, 0f, 0f,
                    sepia * 0.272f, sepia * 0.534f, invSepia + sepia * 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(sepiaMatrix)
            }

            imgAvatar?.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                imgAvatar?.setRenderEffect(null)
            }
        }
    }

    private fun applyTemplateShadow(shadowValue: Float) {
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

    // ====== END EDITING UTILITY METHODS ======

    /**
     * Show specific editing section and hide action buttons
     */
    private fun showEditingSection(sectionId: Int) {
        // Hide all sections first
        binding.cardNameSection.gone()
        binding.cardBountySection.gone()
        binding.cardPhotoFilterSection.gone()
        binding.cardPosterShadowSection.gone()

        // Hide action buttons container
        binding.layoutActionButtons.gone()

        // Show requested section
        when (sectionId) {
            R.id.cardNameSection -> binding.cardNameSection.visible()
            R.id.cardBountySection -> binding.cardBountySection.visible()
            R.id.cardPhotoFilterSection -> binding.cardPhotoFilterSection.visible()
            R.id.cardPosterShadowSection -> binding.cardPosterShadowSection.visible()
        }

        // Show close button in action bar
        showSectionCloseButton()
    }

    /**
     * Hide editing sections and show action buttons
     */
    private fun hideEditingSections() {
        binding.cardNameSection.gone()
        binding.cardBountySection.gone()
        binding.cardPhotoFilterSection.gone()
        binding.cardPosterShadowSection.gone()

        binding.layoutActionButtons.visible()
        hideSectionCloseButton()
    }

    /**
     * Show close button in action bar when editing section is open
     */
    private fun showSectionCloseButton() {
        binding.actionBar.btnActionBarRight.visible()
        binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_back)
    }

    /**
     * Hide close button in action bar
     */
    private fun hideSectionCloseButton() {
        binding.actionBar.btnActionBarRight.gone()
    }

    /**
     * Check if any editing section is currently visible
     */
    private fun isEditingSectionVisible(): Boolean {
        return binding.cardNameSection.visibility == View.VISIBLE ||
                binding.cardBountySection.visibility == View.VISIBLE ||
                binding.cardPhotoFilterSection.visibility == View.VISIBLE ||
                binding.cardPosterShadowSection.visibility == View.VISIBLE
    }
}

