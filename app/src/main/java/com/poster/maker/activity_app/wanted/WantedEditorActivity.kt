package poster.maker.activity_app.wanted

import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import java.text.BreakIterator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import jp.wasabeef.glide.transformations.BlurTransformation
import poster.maker.R
import poster.maker.data.local.entity.FontItem
import poster.maker.data.local.entity.FontSelectorAdapter
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.*
import poster.maker.core.helper.AssetHelper
import poster.maker.core.helper.BitmapHelper
import poster.maker.core.helper.ShadowTransformation
import poster.maker.core.viewmodel.PosterEditorSharedViewModel
import poster.maker.databinding.ActivityWantedEditorBinding
import poster.maker.dialog.YesNoDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
//quyen
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
//quyen

class WantedEditorActivity : BaseActivity<ActivityWantedEditorBinding>() {

    //quyen
    var interAll: InterstitialAd? = null
    //quyen

    // Use shared ViewModel for data binding with MakeScreenActivity
    private val viewModel = PosterEditorSharedViewModel.getInstance()

    // Dynamic poster views (inflated from template layouts)
    private var imgTemplate: ImageView? = null
    private var imgTemplateShadow: ImageView? = null
    private var imgAvatar: ImageView? = null
    private var imgAvatarShadow: ImageView? = null
    private var tvName: TextView? = null
    private var tvBounty: TextView? = null

    // LOCAL TEMPORARY STATE - Only saved to ViewModel when user clicks SAVE button
    // If user clicks BACK, these values are discarded and ViewModel remains unchanged
    private var tempSelectedImageUri: Uri? = null
    private var tempNameText: String = ""
    private var tempBountyText: String = ""
    private var tempNameFont: String = "Roboto Bold"

    private var tempBountyFont: String = "Roboto Bold"
    private var tempNameSpacing: Float = 0f
    private var tempBountySize: Float = 24f
    private var tempBountyWeight: Float = 0f
    private var tempBountySpacing: Float = 0f
    private var tempBountyPositionX: Float = 0f
    private var tempBountyPositionY: Float = 0f
    private var tempFilterShadow: Float = 0f
    private var tempFilterBlur: Float = 0f
    private var tempFilterBrightness: Float = 1f
    private var tempFilterContrast: Float = 1f
    private var tempFilterGrayscale: Float = 0f
    private var tempFilterHueRotate: Float = 0f
    private var tempFilterSaturate: Float = 1f
    private var tempFilterSepia: Float = 0f
    private var tempPosterShadow: Float = 0f

    // Font selector adapter - need to keep reference to update selection on reset
    private var fontAdapter: FontSelectorAdapter? = null
    private var bountyFontAdapter: FontSelectorAdapter? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Write to local variable instead of ViewModel
            tempSelectedImageUri = it
            // Load image into both avatar and shadow ImageViews
            loadImageToAvatars(it)
        }
    }

    private val fontList = listOf(
        // Basic fonts
        FontItem("Roboto Bold", R.font.roboto_bold),
        FontItem("Roboto Medium", R.font.roboto_medium),
        FontItem("Roboto Regular", R.font.roboto_regular),
        FontItem("Londrina Solid", R.font.londrina_solid_regular),
        FontItem("Montserrat Bold", R.font.montserrat_bold),
        FontItem("Montserrat Medium", R.font.montserrat_medium),

        // Script/Handwriting (9 fonts)
        FontItem("Script Elegant 1", R.font.script_elegant_01),
        FontItem("Script Elegant 2", R.font.script_elegant_02),
        FontItem("Handwriting 1", R.font.script_handwriting_01),
        FontItem("Script Casual", R.font.script_casual),
        FontItem("Brush Style", R.font.brush_01),

        // Horror/Gothic/Halloween (8 fonts)
        FontItem("Horror Style 1", R.font.display_horror_02),
        FontItem("Horror Style 2", R.font.display_horror_04),
        FontItem("Halloween", R.font.display_halloween),
        FontItem("Gothic", R.font.display_gothic_01),
        FontItem("Horror Style 5", R.font.display_horror_11),

        // Display/Decorative (8 fonts)
        FontItem("Creative 1", R.font.display_creative_01),
        FontItem("Rounded", R.font.display_rounded),
        // Serif Elegant (5 fonts)
        FontItem("Serif Classic", R.font.serif_02),
        FontItem("Signature", R.font.serif_signature),

        FontItem("Display Cultural Style", R.font.display_cultural),
        FontItem("Display Festive Style", R.font.display_festive),
        FontItem("Tream Style", R.font.treamd),
        FontItem("Ocean Style", R.font.ocen),


    )

    override fun setViewBinding(): ActivityWantedEditorBinding {
        return ActivityWantedEditorBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Inflate template layout dynamically
        inflateTemplateLayout(viewModel.selectedTemplate.value)

        // IMPORTANT: Setup listeners FIRST before restoring values
        // This ensures that when we restore values, listeners are already attached
        // and will trigger to update the preview (tvName, tvBounty, filters, etc.)

        // Apply grapheme cluster filter to edtName to prevent combining character issues
        applyGraphemeClusterFilter()

        setupFontSelector()
        setupBountyFontSelector()
        setupSeekBars()
        setupEditTexts()

        // Enable marquee for labels
        binding.tvFilterGrayscaleLabel.isSelected = true
        binding.tvFilterHueRotateLabel.isSelected = true
        binding.tvSpacingLabel.isSelected = true
        binding.tvBountySpacingLabel.isSelected = true
        // Apply text colors from template config
        applyTemplateColors()

        // Check if this is first time entering Editor (no edits yet)
        val isFirstTime = !viewModel.isEditingStarted.value
        val config = viewModel.getConfig()

        android.util.Log.d("SaveDebug", "═══════════════════════════════════════")
        android.util.Log.d("SaveDebug", "EDITOR initView()")
        android.util.Log.d("SaveDebug", "isFirstTime: $isFirstTime")
        android.util.Log.d("SaveDebug", "viewModel.isEditingStarted: ${viewModel.isEditingStarted.value}")
        android.util.Log.d("SaveDebug", "viewModel.selectedImageUri: ${viewModel.selectedImageUri.value}")

        if (isFirstTime) {
            loadDefaultAvatarWebp()

            android.util.Log.d("SaveDebug", "First time - loading default avatar.webp")
            // First time: Load default avatar.webp from drawable
            // Show all elements with default avatar preview
            tvName?.visibility = if (config.hasName) View.VISIBLE else View.GONE
            tvBounty?.visibility = View.VISIBLE
            imgAvatar?.visibility = View.VISIBLE
            imgAvatarShadow?.visibility = View.VISIBLE
            imgTemplateShadow?.visibility = View.GONE

            // Load default avatar.webp from drawable
        } else {
            android.util.Log.d("SaveDebug", "Already editing - showing current values")
            // Already editing: Show elements with current values
            tvName?.visibility = if (config.hasName) View.VISIBLE else View.GONE
            tvBounty?.visibility = View.VISIBLE
            imgAvatar?.visibility = View.VISIBLE
        }

        android.util.Log.d("SaveDebug", "Calling restoreUIFromViewModel()")
        // IMPORTANT: Always restore UI values from ViewModel to initialize temp variables
        // This ensures temp variables have correct default values even on first time
        restoreUIFromViewModel()
        android.util.Log.d("SaveDebug", "After restore - tempSelectedImageUri: $tempSelectedImageUri")
        android.util.Log.d("SaveDebug", "═══════════════════════════════════════")

        // Setup listener to auto-hide navigation bar when keyboard closes
        setupKeyboardListener()
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

        // Enable auto-resize for tvName to handle long text + high spacing
        // SINGLE LINE ONLY - auto-shrink when text is too long
        tvName?.apply {
            maxLines = 1
            // Note: Do NOT use setSingleLine(true) - it conflicts with autoSize
            // Use TextViewCompat for API 24+ compatibility
            val config = viewModel.getConfig()

            // IMPORTANT: Disable autoSize first to clear any XML-defined autoSize configuration
            // This prevents XML autoSize from conflicting with programmatic autoSize
            TextViewCompat.setAutoSizeTextTypeWithDefaults(
                this,
                TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
            )

            // Now set autoSize with correct configuration
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this,
                8,      // minTextSize: 8sp (allow more shrinking for long text)
                config.nameSize.toInt(),     // maxTextSize:
                1,      // granularity: 1sp step
                android.util.TypedValue.COMPLEX_UNIT_SP
            )

            // Log after setting autoSize
            post {
                val textSizePx = tvName?.textSize ?: 0f
                val textSizeSp = textSizePx / resources.displayMetrics.scaledDensity
                android.util.Log.d("TextSizeDebug", "───────────────────────────────────────")
                android.util.Log.d("TextSizeDebug", "WANTED EDITOR - After setAutoSize")
                android.util.Log.d("TextSizeDebug", "Template: ${config.id}")
                android.util.Log.d("TextSizeDebug", "tvName textSize: ${textSizeSp.toInt()}sp (${textSizePx}px)")
                android.util.Log.d("TextSizeDebug", "AutoSize maxTextSize set to: ${config.nameSize.toInt()}sp")
                android.util.Log.d("TextSizeDebug", "───────────────────────────────────────")
            }
        }

        // Load template background
        loadTemplateBackground()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let { view ->
                if (view is EditText && !isTouchInsideView(view, ev)) {
                    view.clearFocus()
                    hideKeyboard(view)
                    binding.root.requestFocus() // ← Quan trọng!
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isTouchInsideView(view: View, event: MotionEvent): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val touchX = event.rawX.toInt()
        val touchY = event.rawY.toInt()
        return touchX in location[0]..(location[0] + view.width) &&
                touchY in location[1]..(location[1] + view.height)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Setup listener to detect keyboard open/close and hide navigation bar when keyboard closes
     * This fixes the issue where some older devices don't auto-hide navigation bar with keyboard
     */
    private fun setupKeyboardListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+): Use modern WindowInsets API
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val imeVisible = insets.isVisible(android.view.WindowInsets.Type.ime())

                if (!imeVisible) {
                    // Keyboard is closed - hide navigation bar
                    window.insetsController?.let { controller ->
                        controller.hide(android.view.WindowInsets.Type.navigationBars())
                        controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }

                view.onApplyWindowInsets(insets)
            }
        } else {
            // Android 10 and below: Use legacy ViewTreeObserver approach
            val contentView = findViewById<View>(android.R.id.content)
            contentView.viewTreeObserver.addOnGlobalLayoutListener {
                val rect = Rect()
                contentView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = contentView.rootView.height
                val keypadHeight = screenHeight - rect.bottom

                // If keyboard height is less than 15% of screen, keyboard is closed
                if (keypadHeight < screenHeight * 0.15) {
                    // Keyboard is closed - hide navigation bar using legacy method
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
                }
            }
        }
    }


    override fun viewListener() {
        binding.apply {
            // Action bar
            actionBar.apply {
                btnActionBarLeft.setOnSingleClick {  handleBack() }
                btnActionBarRight.setOnSingleClick {

                    showInterAll {  handleSave() }
                }
                btnActionBarReset.setOnSingleClick { showResetConfirmation() }
            }

            // Import photo button
            btnImportPhoto.setOnSingleClick {
                pickImageLauncher.launch("image/*")
            }


            // Name section toggle
            layoutNameHeader.setOnSingleClick {
                viewModel.toggleNameSection()
            }

            // Bounty section toggle
            layoutBountyHeader.setOnSingleClick {
                viewModel.toggleBountySection()
            }

            // Photo Filter section toggle
            layoutPhotoFilterHeader.setOnSingleClick {
                viewModel.togglePhotoFilterSection()
            }

            // Poster Shadow section toggle
            layoutPosterShadowHeader.setOnSingleClick {
                viewModel.togglePosterShadowSection()
            }
        }
    }

    override fun dataObservable() {
        // NOTE: selectedImageUri observer removed - now using local variable tempSelectedImageUri
        // Image loading is handled directly in pickImageLauncher callback

        // Observe template config changes to show/hide sections
        lifecycleScope.launch {
            viewModel.currentConfig.collect { config ->
                // Show/hide Name section CardView based on template config
                binding.cardNameSection.visibility = if (config.hasName) android.view.View.VISIBLE else android.view.View.GONE
                binding.layoutNameContent.visibility = android.view.View.GONE

                // Show/hide Bounty section based on template config
                binding.layoutBountyHeader.visibility = if (config.hasBounty) android.view.View.VISIBLE else android.view.View.GONE
                binding.layoutBountyContent.visibility = android.view.View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.isNameSectionExpanded.collect { isExpanded ->
                // Only show content if section is expanded AND template has name
                val config = viewModel.getConfig()
                binding.layoutNameContent.visibility = if (isExpanded && config.hasName) android.view.View.VISIBLE else android.view.View.GONE
                binding.imgNameArrow.rotation = if (isExpanded) 180f else 0f
            }
        }

        lifecycleScope.launch {
            viewModel.isBountySectionExpanded.collect { isExpanded ->
                // Only show content if section is expanded AND template has bounty
                val config = viewModel.getConfig()
                binding.layoutBountyContent.visibility = if (isExpanded && config.hasBounty) android.view.View.VISIBLE else android.view.View.GONE
                binding.imgBountyArrow.rotation = if (isExpanded) 180f else 0f
            }
        }

        lifecycleScope.launch {
            viewModel.isPhotoFilterSectionExpanded.collect { isExpanded ->
                binding.layoutPhotoFilterContent.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
                binding.imgPhotoFilterArrow.rotation = if (isExpanded) 180f else 0f
            }
        }

        lifecycleScope.launch {
            viewModel.isPosterShadowSectionExpanded.collect { isExpanded ->
                binding.layoutPosterShadowContent.visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
                binding.imgPosterShadowArrow.rotation = if (isExpanded) 180f else 0f
            }
        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            tvCenter.text = strings(R.string.wanted_poster_maker)
            tvCenter.gone()
            btnActionBarReset.visible()
            btnActionBarRight.setImageResource(R.drawable.ic_done)
            btnActionBarRight.visible()
        }
    }

    //quyen
//    override fun initAds() {
//        // Load interstitial ad
//        Admob.getInstance().loadNativeCollapNotBanner(this,
//            getString(R.string.native_collap_edit),
//            binding.nativeClEdit)
//    }
    //quyen

    //quyen
    override fun onRestart() {
        super.onRestart()
     //       initAds()
    }
    //quyen

    /**
     * Handle Back button
     */
    private fun handleBack() {
        // Data is already in shared ViewModel - just go back
        handleBackLeftToRight()
    }

    /**
     * Handle Save button - Copy all local variables to ViewModel and mark editing started
     */
    private fun handleSave() {

                //quyen
                // Copy ALL local variables to ViewModel (19 properties)
                android.util.Log.d("SaveDebug", "═══════════════════════════════════════")
                android.util.Log.d("SaveDebug", "WANTED EDITOR - handleSave()")
                android.util.Log.d("SaveDebug", "tempNameText: '$tempNameText'")
                android.util.Log.d("SaveDebug", "tempBountyText: '$tempBountyText'")
                android.util.Log.d("SaveDebug", "tempSelectedImageUri: $tempSelectedImageUri")
                android.util.Log.d("SaveDebug", "hasChanges BEFORE save: ${viewModel.hasChanges.value}")
                android.util.Log.d("SaveDebug", "isEditingStarted BEFORE save: ${viewModel.isEditingStarted.value}")
                android.util.Log.d("SaveDebug", "═══════════════════════════════════════")

                tempSelectedImageUri?.let {
                    android.util.Log.d("SaveDebug", "Setting selectedImageUri: $it")
                    viewModel.setSelectedImageUri(it)
                } ?: android.util.Log.d("SaveDebug", "tempSelectedImageUri is NULL - NOT setting to ViewModel")

                viewModel.setNameText(tempNameText)
                viewModel.setBountyFont(tempBountyFont)
                viewModel.setBountyText(tempBountyText)
                viewModel.setNameFont(tempNameFont)
                viewModel.setBountyFont(tempBountyFont)  // ← THÊM DÒNG NÀY
                viewModel.setNameSpacing(tempNameSpacing)
                viewModel.setBountySize(tempBountySize)
                viewModel.setBountyWeight(tempBountyWeight)
                viewModel.setBountySpacing(tempBountySpacing)
                viewModel.setBountyPositionX(tempBountyPositionX)
                viewModel.setBountyPositionY(tempBountyPositionY)
                viewModel.setFilterShadow(tempFilterShadow)
                viewModel.setFilterBlur(tempFilterBlur)
                viewModel.setFilterBrightness(tempFilterBrightness)
                viewModel.setFilterContrast(tempFilterContrast)
                viewModel.setFilterGrayscale(tempFilterGrayscale)
                viewModel.setFilterHueRotate(tempFilterHueRotate)
                viewModel.setFilterSaturate(tempFilterSaturate)
                viewModel.setFilterSepia(tempFilterSepia)
                viewModel.setPosterShadow(tempPosterShadow)

                android.util.Log.d("SaveDebug", "hasChanges AFTER all setters: ${viewModel.hasChanges.value}")
                android.util.Log.d("SaveDebug", "isEditingStarted BEFORE mark: ${viewModel.isEditingStarted.value}")

                // Mark editing as started if user made any changes
                // This will switch MakeScreen from avatar.png to item.png display
                if (viewModel.hasChanges.value) {
                    android.util.Log.d("SaveDebug", "hasChanges = true → Calling markEditingStarted()")
                    viewModel.markEditingStarted()
                } else {
                    android.util.Log.d("SaveDebug", "hasChanges = false → NOT marking editing started")
                }

                android.util.Log.d("SaveDebug", "isEditingStarted AFTER mark: ${viewModel.isEditingStarted.value}")
                android.util.Log.d("SaveDebug", "selectedImageUri in ViewModel: ${viewModel.selectedImageUri.value}")
                android.util.Log.d("SaveDebug", "═══════════════════════════════════════")

                // ViewModel now has all saved data - MakeScreenActivity will automatically have access
                setResult(RESULT_OK)
                finish()
                //quyen
        //quyen
    }

    /**
     * Show confirmation dialog before resetting
     */
    private fun showResetConfirmation() {
        val dialog = YesNoDialog(
            context = this,
            title = R.string.reset,
            description = R.string.change_your_whole_design_are_you_sure
        )
        dialog.onYesClick = {
            showInterAll{
            handleReset()
            dialog.dismiss()
        }
        }
        dialog.onNoClick = {
            dialog.dismiss()
        }
        dialog.show()
    }

    /**
     * Apply text colors from template config
     * Each template has specific colors for name and bounty text
     */
    private fun applyTemplateColors() {
        val config = viewModel.getConfig()

        try {
            // Apply name color
            if (config.hasName) {
                tvName?.setTextColor(Color.parseColor(config.nameColor))
            }

            // Apply bounty color
            tvBounty?.setTextColor(Color.parseColor(config.bountyColor))

            // Apply text sizes
            // Note: tvName textSize is already configured via autoSize in inflateTemplateLayout()
            // Setting textSize here would conflict with autoSize configuration
            tvBounty?.textSize = config.bountySize

            // Log text sizes for debugging
            tvName?.post {
                val nameTextSizePx = tvName?.textSize ?: 0f
                val nameTextSizeSp = nameTextSizePx / resources.displayMetrics.scaledDensity
                android.util.Log.d("TextSizeDebug", "═══════════════════════════════════════")
                android.util.Log.d("TextSizeDebug", "WANTED EDITOR - applyTemplateColors()")
                android.util.Log.d("TextSizeDebug", "Template: ${config.id}")
                android.util.Log.d("TextSizeDebug", "tvName textSize: ${nameTextSizeSp.toInt()}sp (${nameTextSizePx}px)")
                android.util.Log.d("TextSizeDebug", "config.nameSize: ${config.nameSize}sp")
                val bountyTextSizePx = tvBounty?.textSize ?: 0f
                val bountyTextSizeSp = bountyTextSizePx / resources.displayMetrics.scaledDensity
                android.util.Log.d("TextSizeDebug", "tvBounty textSize: ${bountyTextSizeSp.toInt()}sp (${bountyTextSizePx}px)")
                android.util.Log.d("TextSizeDebug", "config.bountySize: ${config.bountySize}sp")
                android.util.Log.d("TextSizeDebug", "═══════════════════════════════════════")
            }
        } catch (e: Exception) {
            // Fallback to default colors if parsing fails
            tvName?.setTextColor(Color.BLACK)
            tvBounty?.setTextColor(Color.BLACK)
        }
    }

    /**
     * Restore all UI values from ViewModel
     * Called when returning to Editor after saving
     *
     * IMPORTANT: This must be called AFTER setupSeekBars() and setupEditTexts()
     * so that listeners are already attached and will trigger when values are set
     */
    private fun restoreUIFromViewModel() {
        android.util.Log.d("SaveDebug", "restoreUIFromViewModel() - START")
        android.util.Log.d("SaveDebug", "tempSelectedImageUri BEFORE restore: $tempSelectedImageUri")
        android.util.Log.d("SaveDebug", "viewModel.selectedImageUri: ${viewModel.selectedImageUri.value}")

        // STEP 1: Load all ViewModel values into local variables
        // IMPORTANT: Only restore tempSelectedImageUri if ViewModel has a value
        // Don't override default avatar loaded in loadDefaultAvatarWebp()
        if (tempSelectedImageUri == null) {
            tempSelectedImageUri = viewModel.selectedImageUri.value
            android.util.Log.d("SaveDebug", "tempSelectedImageUri was null - restored from ViewModel: $tempSelectedImageUri")
        } else {
            android.util.Log.d("SaveDebug", "tempSelectedImageUri already has value - keeping it: $tempSelectedImageUri")
        }

        tempNameText = viewModel.nameText.value
        tempBountyText = viewModel.bountyText.value
        tempNameFont = viewModel.nameFont.value
        tempBountyFont = viewModel.bountyFont.value
        tempNameSpacing = viewModel.nameSpacing.value
        tempBountySize = viewModel.bountySize.value
        tempBountyWeight = viewModel.bountyWeight.value
        tempBountySpacing = viewModel.bountySpacing.value
        tempBountyPositionX = viewModel.bountyPositionX.value
        tempBountyPositionY = viewModel.bountyPositionY.value
        tempFilterShadow = viewModel.filterShadow.value
        tempFilterBlur = viewModel.filterBlur.value
        tempFilterBrightness = viewModel.filterBrightness.value
        tempFilterContrast = viewModel.filterContrast.value
        tempFilterGrayscale = viewModel.filterGrayscale.value
        tempFilterHueRotate = viewModel.filterHueRotate.value
        tempFilterSaturate = viewModel.filterSaturate.value
        tempFilterSepia = viewModel.filterSepia.value
        tempPosterShadow = viewModel.posterShadow.value

        // Load image if exists (using tempSelectedImageUri)
        tempSelectedImageUri?.let { uri ->
            loadImageToAvatars(uri)
        } ?: run {
            // Load default avatar.webp from drawable if no Uri
            imgAvatar?.let { imageView ->
                Glide.with(this)
                    .load(R.drawable.avatar)
                    .centerCrop()
                    .into(imageView)
            }
            imgAvatarShadow?.let { imageView ->
                val shadowRadius = tempFilterShadow / 100f * 15f
                val shadowAlpha = 0.8f
                Glide.with(this)
                    .load(R.drawable.avatar)
                    .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                    .into(imageView)
            }
        }

        // STEP 2: Restore EditText values
        // TextWatcher will trigger and update tvName/tvBounty automatically
        binding.edtName.setText(viewModel.nameText.value)
        binding.edtBounty.setText(viewModel.bountyText.value)

        // Restore Name section
        // Listener will trigger and apply letterSpacing to tvName
        binding.seekBarNameSpacing.progress = (viewModel.nameSpacing.value * 500f).toInt()  // spacing 0-0.2 → progress 0-100

        // Restore Bounty section
        // Listeners will trigger and apply size/spacing/position to tvBounty
        val bountySizeProgress = ((viewModel.bountySize.value - 12f) / 48f * 100f).toInt()
        binding.seekBarBountySize.progress = bountySizeProgress

        binding.seekBarBountyWeight.progress = viewModel.bountyWeight.value.toInt()

        binding.seekBarBountySpacing.progress = (viewModel.bountySpacing.value * 500f).toInt()  // spacing 0-0.2 → progress 0-100

        binding.seekBarBountyPositionX.progress = ((viewModel.bountyPositionX.value / 2f) + 50).toInt()

        binding.seekBarBountyPositionY.progress = ((viewModel.bountyPositionY.value / 2f) + 50).toInt()

        // Restore Photo Filter section
        // Listeners will trigger and apply shadow/blur/filters to imgAvatar
        binding.seekBarFilterShadow.progress = viewModel.filterShadow.value.toInt()

        binding.seekBarFilterBlur.progress = viewModel.filterBlur.value.toInt()

        binding.seekBarFilterBrightness.progress = (viewModel.filterBrightness.value * 100f).toInt()

        binding.seekBarFilterContrast.progress = (viewModel.filterContrast.value * 100f).toInt()

        binding.seekBarFilterGrayscale.progress = (viewModel.filterGrayscale.value * 100f).toInt()

        binding.seekBarFilterHueRotate.progress = viewModel.filterHueRotate.value.toInt()

        binding.seekBarFilterSaturate.progress = (viewModel.filterSaturate.value * 100f).toInt()

        binding.seekBarFilterSepia.progress = (viewModel.filterSepia.value * 100f).toInt()

        // Restore Poster Shadow section
        // Listener will trigger and apply template shadow
        binding.seekBarPosterShadow.progress = viewModel.posterShadow.value.toInt()

        // Font restoration is handled by setupFontSelector()

        // IMPORTANT: Apply text properties DIRECTLY to ensure synchronization with MakeScreen
        // Seekbar listeners may not trigger immediately, causing property mismatch between screens
        tvName?.letterSpacing = viewModel.nameSpacing.value
        tvBounty?.textSize = viewModel.bountySize.value
        tvBounty?.letterSpacing = viewModel.bountySpacing.value
        tvBounty?.translationX = viewModel.bountyPositionX.value
        tvBounty?.translationY = viewModel.bountyPositionY.value
    }

    /**
     * Handle reset button - Reset all values to default
     */
    private fun handleReset() {
        //quyen

                //quyen
                // Reset ViewModel data to template config defaults
                viewModel.resetAll()

                // Reset local variables to match ViewModel defaults
                tempSelectedImageUri = viewModel.selectedImageUri.value
                tempNameText = viewModel.nameText.value
                tempBountyText = viewModel.bountyText.value
                tempNameFont = viewModel.nameFont.value
                tempBountyFont = viewModel.bountyFont.value
                tempNameSpacing = viewModel.nameSpacing.value
                tempBountySize = viewModel.bountySize.value
                tempBountyWeight = viewModel.bountyWeight.value
                tempBountySpacing = viewModel.bountySpacing.value
                tempBountyPositionX = viewModel.bountyPositionX.value
                tempBountyPositionY = viewModel.bountyPositionY.value
                tempFilterShadow = viewModel.filterShadow.value
                tempFilterBlur = viewModel.filterBlur.value
                tempFilterBrightness = viewModel.filterBrightness.value
                tempFilterContrast = viewModel.filterContrast.value
                tempFilterGrayscale = viewModel.filterGrayscale.value
                tempFilterHueRotate = viewModel.filterHueRotate.value
                tempFilterSaturate = viewModel.filterSaturate.value
                tempFilterSepia = viewModel.filterSepia.value
                tempPosterShadow = viewModel.posterShadow.value

                // Reset UI components to match ViewModel defaults (from template config)
                binding.apply {
            // Reset EditTexts to template config defaults (NOT hardcoded values!)
            edtName.setText(viewModel.nameText.value)
            edtBounty.setText(viewModel.bountyText.value)

            // Reset Name section
            tvCurrentNameFont.text = fontList[0].name
            val initialTypeface = ResourcesCompat.getFont(this@WantedEditorActivity, fontList[0].fontResId)
            tvName?.typeface = initialTypeface
            tvCurrentNameFont.typeface = initialTypeface
            seekBarNameSpacing.progress = 0

            // Update font adapter to select first font and scroll to it
            fontAdapter?.setSelectedPosition(0)
            rvFontList.scrollToPosition(0)


            //Reset Bounty Font
            binding.tvCurrentNameFontBounty.text = fontList[0].name
            val bountyTypeface = ResourcesCompat.getFont(this@WantedEditorActivity, fontList[0].fontResId)
            tvBounty?.typeface = bountyTypeface
            binding.tvCurrentNameFontBounty.typeface = bountyTypeface

            // Update bounty font adapter
            bountyFontAdapter?.setSelectedPosition(0)
            binding.rvFontBountyList.scrollToPosition(0)

            // Reset Bounty section
            // Calculate progress from config bounty size (12-60sp range)
            val bountySizeProgress = ((viewModel.bountySize.value - 12f) / 48f * 100f).toInt()
            seekBarBountySize.progress = bountySizeProgress
            seekBarBountyWeight.progress = 0
            seekBarBountySpacing.progress = 0
            seekBarBountyPositionX.progress = 50 // Center (0f offset)
            seekBarBountyPositionY.progress = 50 // Center (0f offset)

            // Reset Photo Filter section
            seekBarFilterShadow.progress = 0
            seekBarFilterBlur.progress = 0
            seekBarFilterBrightness.progress = 100 // 1f = 100%
            seekBarFilterContrast.progress = 100 // 1f = 100%
            seekBarFilterGrayscale.progress = 0
            seekBarFilterHueRotate.progress = 0
            seekBarFilterSaturate.progress = 100 // 1f = 100%
            seekBarFilterSepia.progress = 0

            // Reset Poster Shadow section
            seekBarPosterShadow.progress = 0
        }

        // Reset TextViews to template config defaults
        // Note: EditText listeners will also update these, but we set them here for immediate feedback
        tvName?.text = viewModel.nameText.value
        tvBounty?.text = viewModel.bountyText.value
        tvName?.letterSpacing = 0f
        tvBounty?.textSize = viewModel.bountySize.value  // Use config default size
        tvBounty?.letterSpacing = 0f
        tvBounty?.translationX = 0f
        tvBounty?.translationY = 0f

        // Apply filter reset
        imgAvatar?.colorFilter = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            imgAvatar?.setRenderEffect(null)
        }

        // Reset shadow effects
        applyShadowEffect(0f)
        applyTemplateShadow(0f)

                // Reload images after reset
                tempSelectedImageUri?.let { uri ->
                    loadImageToAvatars(uri)
                } ?: run {
                    // Load default avatar.webp from drawable
                    imgAvatar?.let { imageView ->
                        Glide.with(this@WantedEditorActivity)
                            .load(R.drawable.avatar)
                            .centerCrop()
                            .into(imageView)
                    }
                    imgAvatarShadow?.let { imageView ->
                        val shadowRadius = tempFilterShadow / 100f * 15f
                        val shadowAlpha = 0.8f
                        Glide.with(this@WantedEditorActivity)
                            .load(R.drawable.avatar)
                            .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                            .into(imageView)
                    }
                }

                // showToast(R.string.reset_to_default_values)
                //quyen

        //quyen
    }

    /**
     * Load template background from assets
     */
    private fun loadTemplateBackground() {
        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)

        imgTemplate?.let { imageView ->
            Glide.with(this)
                .load(templatePath)
                .into(imageView)
        }
    }

    private fun setupFontSelector() {
        // Find font index from ViewModel (for restoring state)
        val savedFontName = viewModel.nameFont.value
        val selectedIndex = fontList.indexOfFirst { it.name == savedFontName }.takeIf { it >= 0 } ?: 0
        val initialFont = fontList[selectedIndex]

        // Set initial/restored font name and apply font
        binding.tvCurrentNameFont.text = initialFont.name
        binding.tvCurrentNameFont.isSelected = true  // Activate marquee scrolling
        val initialTypeface = ResourcesCompat.getFont(this, initialFont.fontResId)
        tvName?.typeface = initialTypeface
        binding.tvCurrentNameFont.typeface = initialTypeface

        // Initialize local variable with current font
        tempNameFont = initialFont.name

        // Setup RecyclerView with adapter
        fontAdapter = FontSelectorAdapter(fontList, selectedIndex) { fontItem, _ ->
            // Update current font display
            binding.tvCurrentNameFont.text = fontItem.name
            val typeface = ResourcesCompat.getFont(this, fontItem.fontResId)
            tvName?.typeface = typeface
            binding.tvCurrentNameFont.typeface = typeface

            // Write to local variable instead of ViewModel
            tempNameFont = fontItem.name

            tvName?.apply {
                val config = viewModel.getConfig()

                // Hide text to prevent flicker
                alpha = 0f

                // Step 1: Disable auto-size
                TextViewCompat.setAutoSizeTextTypeWithDefaults(
                    this,
                    TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
                )

                // Step 2: Force expand to max size
                textSize = config.nameSize

                // Step 3: Re-enable auto-size
                post {
                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        this,
                        6,      // minTextSize
                        config.nameSize.toInt(),
                        1,      // granularity
                        android.util.TypedValue.COMPLEX_UNIT_SP
                    )

                    // Show text again
                    alpha = 1f

                    // Log new size
                    post {
                        val newSize = tvName?.textSize ?: 0f
                        val newSizeSp = newSize / resources.displayMetrics.scaledDensity
                        android.util.Log.d("FontChange", "Font changed to '${fontItem.name}' → New textSize: ${newSizeSp.toInt()}sp")
                    }
                }
            }

            // Collapse the font list after selection
            binding.rvFontList.visibility = View.GONE
            binding.imgFontArrow.rotation = 0f
        }

        binding.rvFontList.apply {
            layoutManager = LinearLayoutManager(this@WantedEditorActivity)
            this.adapter = fontAdapter

            // Disable nested scrolling to prevent conflict with parent
            isNestedScrollingEnabled = false

            // Add custom touch handling
            addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Disable parent scrolling when touching RecyclerView
                            rv.parent.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                    return false
                }
            })
        }

        // Toggle expand/collapse on click
        binding.layoutFontSelector.setOnClickListener {
            if (binding.rvFontList.visibility == View.GONE) {
                // Expand - disable parent scroll
                binding.rvFontList.visibility = View.VISIBLE
                binding.imgFontArrow.rotation = 180f
                binding.nestedScrollView.isNestedScrollingEnabled = false
            } else {
                // Collapse - enable parent scroll
                binding.rvFontList.visibility = View.GONE
                binding.imgFontArrow.rotation = 0f
                binding.nestedScrollView.isNestedScrollingEnabled = true
            }
        }
    }

    private fun setupBountyFontSelector() {
        // Find font index from ViewModel (for restoring state)
        val savedFontName = viewModel.bountyFont.value  // ← CẦN THÊM bountyFont vào ViewModel
        val selectedIndex = fontList.indexOfFirst { it.name == savedFontName }.takeIf { it >= 0 } ?: 0
        val initialFont = fontList[selectedIndex]

        // Set initial/restored font name and apply font
        binding.tvCurrentNameFontBounty.text = initialFont.name
        binding.tvCurrentNameFontBounty.isSelected = true  // Activate marquee
        val initialTypeface = ResourcesCompat.getFont(this, initialFont.fontResId)
        tvBounty?.typeface = initialTypeface
        binding.tvCurrentNameFontBounty.typeface = initialTypeface

        // Initialize local variable with current font
        tempBountyFont = initialFont.name

        // Setup RecyclerView with adapter
        bountyFontAdapter = FontSelectorAdapter(fontList, selectedIndex) { fontItem, _ ->
            // Update current font display
            binding.tvCurrentNameFontBounty.text = fontItem.name
            val typeface = ResourcesCompat.getFont(this, fontItem.fontResId)
            tvBounty?.typeface = typeface
            binding.tvCurrentNameFontBounty.typeface = typeface

            // Write to local variable instead of ViewModel
            tempBountyFont = fontItem.name

            // Collapse the font list after selection
            binding.rvFontBountyList.visibility = View.GONE
            binding.imgFontBountyArrow.rotation = 0f
        }

        binding.rvFontBountyList.apply {
            layoutManager = LinearLayoutManager(this@WantedEditorActivity)
            this.adapter = bountyFontAdapter

            // Disable nested scrolling to prevent conflict with parent
            isNestedScrollingEnabled = false

            // Add custom touch handling
            addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> {
                            rv.parent.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                    return false
                }
            })
        }

        // Toggle expand/collapse on click
        binding.layoutFontBountySelector.setOnClickListener {
            if (binding.rvFontBountyList.visibility == View.GONE) {
                // Expand
                binding.rvFontBountyList.visibility = View.VISIBLE
                binding.imgFontBountyArrow.rotation = 180f
                binding.nestedScrollView.isNestedScrollingEnabled = false
            } else {
                // Collapse
                binding.rvFontBountyList.visibility = View.GONE
                binding.imgFontBountyArrow.rotation = 0f
                binding.nestedScrollView.isNestedScrollingEnabled = true
            }
        }
    }
    /**
     * Apply InputFilter to count grapheme clusters (visible characters) instead of code units
     * This prevents issues with combining diacritical marks (Vietnamese accents), emojis, etc.
     */
    private fun applyGraphemeClusterFilter() {
        val maxGraphemeClusters = 25

        val graphemeFilter = InputFilter { source, start, end, dest, dstart, dend ->
            val existingText = dest.toString()
            val sourceText = source.subSequence(start, end).toString()

            // DEBUG LOGGING
            android.util.Log.d("InputFilter", "=== InputFilter called ===")
            android.util.Log.d("InputFilter", "dest: '$existingText' (length=${existingText.length})")
            android.util.Log.d("InputFilter", "source: '$sourceText'")
            android.util.Log.d("InputFilter", "dstart=$dstart, dend=$dend")
            android.util.Log.d("InputFilter", "start=$start, end=$end")

            // Build the result string to see what will happen
            val resultText = existingText.substring(0, dstart) +
                           sourceText +
                           existingText.substring(dend)
            val resultCount = countGraphemeClusters(resultText)

            android.util.Log.d("InputFilter", "resultText would be: '$resultText' (count=$resultCount)")

            if (resultCount <= maxGraphemeClusters) {
                // Accept the input - result fits within limit
                android.util.Log.d("InputFilter", "ACCEPT: resultCount ($resultCount) <= max ($maxGraphemeClusters)")
                null
            } else {
                // Result would exceed limit - need to truncate or reject
                val currentCount = countGraphemeClusters(existingText)
                android.util.Log.d("InputFilter", "EXCEED: currentCount=$currentCount, resultCount=$resultCount")

                if (dstart == dend) {
                    // Insertion - try to fit what we can
                    val availableSpace = maxGraphemeClusters - currentCount
                    if (availableSpace > 0) {
                        val truncated = truncateToGraphemeClusters(sourceText, availableSpace)
                        android.util.Log.d("InputFilter", "TRUNCATE: Accepting '$truncated' (available=$availableSpace)")
                        truncated
                    } else {
                        // No space - reject and clear composition buffer
                        android.util.Log.d("InputFilter", "REJECT: No space available")
                        // Clear IME composition buffer to prevent backspace bug
                        binding.edtName.post {
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.restartInput(binding.edtName)
                        }
                        ""
                    }
                } else {
                    // Replacement would exceed limit
                    // Try to truncate the source to fit
                    val textBeingReplaced = existingText.substring(dstart, dend)
                    val textBeforeReplacement = existingText.substring(0, dstart) + existingText.substring(dend)
                    val availableSpace = maxGraphemeClusters - countGraphemeClusters(textBeforeReplacement)

                    if (availableSpace > 0) {
                        // Can fit some of the replacement
                        val truncated = truncateToGraphemeClusters(sourceText, availableSpace)
                        android.util.Log.d("InputFilter", "TRUNCATE replacement: '$truncated' (available=$availableSpace)")
                        truncated
                    } else {
                        // Cannot fit any - keep the original text being replaced
                        android.util.Log.d("InputFilter", "REJECT replacement: Keeping original '$textBeingReplaced'")
                        textBeingReplaced
                    }
                }
            }
        }

        binding.edtName.filters = arrayOf(graphemeFilter)
    }

    /**
     * Truncate text to a specific number of grapheme clusters
     */
    private fun truncateToGraphemeClusters(text: String, maxClusters: Int): String {
        if (text.isEmpty() || maxClusters <= 0) return ""

        val breakIterator = BreakIterator.getCharacterInstance()
        breakIterator.setText(text)

        var count = 0
        var boundary = breakIterator.first()

        while (count < maxClusters && boundary != BreakIterator.DONE) {
            boundary = breakIterator.next()
            count++
        }

        // If we completed all clusters or ran out of text, return up to current boundary
        return if (boundary != BreakIterator.DONE) {
            text.substring(0, boundary)
        } else {
            text  // All text fits
        }
    }

    /**
     * Count grapheme clusters (visible characters) in a string
     * Properly handles combining characters, emojis, etc.
     */
    private fun countGraphemeClusters(text: String): Int {
        if (text.isEmpty()) return 0

        val breakIterator = BreakIterator.getCharacterInstance()
        breakIterator.setText(text)

        var count = 0
        var start = breakIterator.first()
        while (breakIterator.next() != BreakIterator.DONE) {
            count++
        }

        return count
    }

    private fun setupEditTexts() {

        // ✅ Thêm listener để ẩn bàn phím khi ấn Enter
        binding.edtName.setOnEditorActionListener { view, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT ||
                (event?.action == android.view.KeyEvent.ACTION_DOWN &&
                        event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {

                view.clearFocus()
                hideKeyboard(view)
                binding.root.requestFocus()
                true  // Consume the event
            } else {
                false
            }
        }

        binding.edtBounty.setOnEditorActionListener { view, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT ||
                (event?.action == android.view.KeyEvent.ACTION_DOWN &&
                        event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {

                view.clearFocus()
                hideKeyboard(view)
                binding.root.requestFocus()
                true  // Consume the event
            } else {
                false
            }
        }

        // Auto scroll when EditText gets focus (for adjustNothing mode)
        binding.edtName.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.post {
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    val viewY = location[1]

                    // Calculate keyboard height (approximate)
                    val displayMetrics = resources.displayMetrics
                    val keyboardHeight = (displayMetrics.heightPixels * 0.4).toInt()

                    // If EditText is below keyboard position, scroll up
                    val screenHeight = displayMetrics.heightPixels
                    if (viewY + view.height > screenHeight - keyboardHeight) {
                        val scrollY = viewY - (screenHeight - keyboardHeight) + view.height + 100
                        binding.nestedScrollView.smoothScrollBy(0, scrollY)
                    }
                }
            }
        }

        binding.edtBounty.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.post {
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    val viewY = location[1]

                    // Calculate keyboard height (approximate)
                    val displayMetrics = resources.displayMetrics
                    val keyboardHeight = (displayMetrics.heightPixels * 0.4).toInt()

                    // If EditText is below keyboard position, scroll up
                    val screenHeight = displayMetrics.heightPixels
                    if (viewY + view.height > screenHeight - keyboardHeight) {
                        val scrollY = viewY - (screenHeight - keyboardHeight) + view.height + 100
                        binding.nestedScrollView.smoothScrollBy(0, scrollY)
                    }
                }
            }
        }

        binding.edtName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                // Show tvName when user starts typing (only if template has name field)
                val config = viewModel.getConfig()
                if (text.isNotEmpty() && config.hasName) {
                    tvName?.visibility = View.VISIBLE
                }
                tvName?.text = text

                // Force TextView to recalculate auto-resize properly when deleting text
                // Solution: Set to max size first to expand height, then re-enable auto-size
                tvName?.apply {
                    val config = viewModel.getConfig()

                    // Hide text to prevent flicker during recalculation
                    alpha = 0f

                    // Step 1: Disable auto-size
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(
                        this,
                        TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
                    )

                    // Step 2: Set to max text size to force TextView expand height
                    textSize = config.nameSize  // maxTextSize from config

                    // Step 3: Re-enable auto-size after TextView has expanded
                    post {
                        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                            this,
                            6,      // minTextSize: 8sp
                            config.nameSize.toInt(),     // maxTextSize from config
                            1,      // granularity: 1sp step
                            android.util.TypedValue.COMPLEX_UNIT_SP
                        )

                        // Show text again after recalculation
                        alpha = 1f
                    }
                }

                // Write to local variable instead of ViewModel
                tempNameText = text
            }
            override fun afterTextChanged(s: Editable?) {
                // Log textSize after auto-resize calculation
                tvName?.post {
                    val currentSize = tvName?.textSize ?: 0f
                    val currentSizeSp = currentSize / resources.displayMetrics.scaledDensity
                    val textLength = s?.length ?: 0
                    val letterSpacing = tvName?.letterSpacing ?: 0f
                    android.util.Log.d("AutoResize", "═══════════════════════════════")
                    android.util.Log.d("AutoResize", "Text: '${s.toString()}'")
                    android.util.Log.d("AutoResize", "Length: $textLength characters")
                    android.util.Log.d("AutoResize", "TextSize: ${currentSizeSp.toInt()}sp (${currentSize}px)")
                    android.util.Log.d("AutoResize", "LetterSpacing: $letterSpacing")
                    android.util.Log.d("AutoResize", "Width: ${tvName?.width}px, Height: ${tvName?.height}px")
                    android.util.Log.d("AutoResize", "═══════════════════════════════")
                }
            }
        })

        binding.edtBounty.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                // Show tvBounty when user starts typing
                if (text.isNotEmpty()) {
                    tvBounty?.visibility = View.VISIBLE
                }
                tvBounty?.text = text
                // Write to local variable instead of ViewModel
                tempBountyText = text
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSeekBars() {
        // Name Spacing (0-0.2 for text spacing)
        binding.seekBarNameSpacing.onProgressChanged { progress ->
            val spacing = progress / 500f  // 0-100 → 0-0.2
            tvName?.apply {
                letterSpacing = spacing

                // Hide text to prevent flicker during recalculation
                alpha = 0f

                // Force TextView to recalculate auto-size when spacing changes
                // SAME PATTERN as TextWatcher: Disable → Reset to max → Re-enable
                val config = viewModel.getConfig()

                // Step 1: Disable auto-size
                TextViewCompat.setAutoSizeTextTypeWithDefaults(
                    this,
                    TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
                )

                // Step 2: Force expand to max size (KEY STEP - same as TextWatcher!)
                textSize = config.nameSize

                // Step 3: Re-enable auto-size after TextView has expanded
                post {
                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        this,
                        6,      // minTextSize
                        config.nameSize.toInt(),     // maxTextSize from config
                        1,      // granularity: 1sp step
                        android.util.TypedValue.COMPLEX_UNIT_SP
                    )

                    // Show text again after recalculation
                    alpha = 1f

                    // Log textSize after recalculation
                    post {
                        val currentSize = tvName?.textSize ?: 0f
                        val currentSizeSp = currentSize / resources.displayMetrics.scaledDensity
                        android.util.Log.d("AutoResize", "───────────────────────────────")
                        android.util.Log.d("AutoResize", "SPACING CHANGED: $spacing")
                        android.util.Log.d("AutoResize", "TextSize after spacing: ${currentSizeSp.toInt()}sp")
                        android.util.Log.d("AutoResize", "───────────────────────────────")
                    }
                }
            }
            // Write to local variable instead of ViewModel
            tempNameSpacing = spacing
        }

        // Bounty Size
        binding.seekBarBountySize.onProgressChanged { progress ->
            val size = 12f + (progress / 100f) * 48f // 12sp to 60sp
            tvBounty?.textSize = size
            // Write to local variable instead of ViewModel
            tempBountySize = size
        }

        // Bounty Weight
        binding.seekBarBountyWeight.onProgressChanged { progress ->
            // Weight doesn't directly map to Android, but we can use different font styles
            // For simplicity, we'll just store the value
            // Write to local variable instead of ViewModel
            tempBountyWeight = progress.toFloat()
        }

        // Bounty Spacing (0-0.2 for text spacing)
        binding.seekBarBountySpacing.onProgressChanged { progress ->
            val spacing = progress / 500f  // 0-100 → 0-0.2
            tvBounty?.letterSpacing = spacing
            // Write to local variable instead of ViewModel
            tempBountySpacing = spacing
        }

        // Bounty Position X
        binding.seekBarBountyPositionX.onProgressChanged { progress ->
            val offsetX = (progress - 50) * 2f // -100 to 100
            tvBounty?.translationX = offsetX
            // Write to local variable instead of ViewModel
            tempBountyPositionX = offsetX
        }

        // Bounty Position Y
        binding.seekBarBountyPositionY.onProgressChanged { progress ->
            val offsetY = (progress - 50) * 2f // -100 to 100
            tvBounty?.translationY = offsetY
            // Write to local variable instead of ViewModel
            tempBountyPositionY = offsetY
        }

        setupPhotoFilterSeekBars()
        setupPosterShadowSeekBar()
    }

    private fun setupPhotoFilterSeekBars() {
        // Shadow - Use dedicated shadow effect function
        binding.seekBarFilterShadow.onProgressChanged { progress ->
            // Write to local variable instead of ViewModel
            tempFilterShadow = progress.toFloat()
            applyShadowEffect(progress.toFloat())  // Use shadow layer approach instead of elevation
        }

        // Blur
        binding.seekBarFilterBlur.onProgressChanged { progress ->
            // Write to local variable instead of ViewModel
            tempFilterBlur = progress.toFloat()

            // Use Glide BlurTransformation for ALL Android versions
            // RenderEffect on Android 12+ causes zoom artifacts, so we use Glide instead
            if (tempSelectedImageUri != null) {
                reloadImageWithBlur(tempSelectedImageUri!!, progress.toFloat())
            }

            applyFilters()
        }

        // Brightness
        binding.seekBarFilterBrightness.onProgressChanged { progress ->
            val brightness = progress / 100f // 0 to 2
            // Write to local variable instead of ViewModel
            tempFilterBrightness = brightness
            applyFilters()
        }

        // Contrast
        binding.seekBarFilterContrast.onProgressChanged { progress ->
            val contrast = progress / 100f // 0 to 2
            // Write to local variable instead of ViewModel
            tempFilterContrast = contrast
            applyFilters()
        }

        // Grayscale
        binding.seekBarFilterGrayscale.onProgressChanged { progress ->
            val grayscale = progress / 100f // 0 to 1
            // Write to local variable instead of ViewModel
            tempFilterGrayscale = grayscale
            applyFilters()
        }

        // Hue Rotate
        binding.seekBarFilterHueRotate.onProgressChanged { progress ->
            val hueRotate = progress.toFloat() // 0 to 360
            // Write to local variable instead of ViewModel
            tempFilterHueRotate = hueRotate
            applyFilters()
        }

        // Saturate
        binding.seekBarFilterSaturate.onProgressChanged { progress ->
            val saturate = progress / 100f // 0 to 2
            // Write to local variable instead of ViewModel
            tempFilterSaturate = saturate
            applyFilters()
        }

        // Sepia
        binding.seekBarFilterSepia.onProgressChanged { progress ->
            val sepia = progress / 100f // 0 to 1
            // Write to local variable instead of ViewModel
            tempFilterSepia = sepia
            applyFilters()
        }
    }

    private fun setupPosterShadowSeekBar() {
        binding.seekBarPosterShadow.onProgressChanged { progress ->
            // Control template shadow instead of CardView elevation
            applyTemplateShadow(progress.toFloat())
            // Write to local variable instead of ViewModel
            tempPosterShadow = progress.toFloat()
        }
    }

    /**
     * Reload image with blur transformation (for Android 8-11)
     * Android 12+ uses RenderEffect which is faster
     */
    private fun reloadImageWithBlur(uri: Uri, blurValue: Float) {
        android.util.Log.d("PhotoBlur", "=== reloadImageWithBlur ===")
        android.util.Log.d("PhotoBlur", "blurValue: $blurValue")

        imgAvatar?.let { imageView ->
            android.util.Log.d("PhotoBlur", "imgAvatar size BEFORE: ${imageView.width}x${imageView.height}")
            android.util.Log.d("PhotoBlur", "imgAvatar scaleType: ${imageView.scaleType}")

            if (blurValue > 0) {
                // Calculate blur radius (0-25)
                val blurRadius = (blurValue / 100f * 25f).toInt().coerceAtLeast(1)
                android.util.Log.d("PhotoBlur", "Applying BlurTransformation: radius=$blurRadius, sampling=3")

                Glide.with(this)
                    .load(uri)
                    .transform(CenterCrop(), BlurTransformation(blurRadius, 3)) // radius, sampling
                    .into(imageView)
            } else {
                android.util.Log.d("PhotoBlur", "No blur - loading with centerCrop only")
                // No blur - reload normal
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imageView)
            }

            imageView.post {
                android.util.Log.d("PhotoBlur", "imgAvatar size AFTER: ${imageView.width}x${imageView.height}")
            }
        } ?: run {
            android.util.Log.e("PhotoBlur", "imgAvatar is NULL!")
        }
    }

    /**
     * Load image into both avatar and shadow ImageViews
     * Shadow uses ShadowTransformation to follow alpha channel (contour shadow like icon)
     */
    private fun loadImageToAvatars(uri: Uri) {
        // Load into main avatar with centerCrop
        imgAvatar?.let { imageView ->
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(imageView)
        }

        // ✅ CHỈ load shadow khi shadow được bật
        if (tempFilterShadow > 0) {
            val shadowRadius = tempFilterShadow / 100f * 15f
            val shadowAlpha = 0.8f

            imgAvatarShadow?.let { imageView ->
                Glide.with(this)
                    .load(uri)
                    .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                    .into(imageView)
            }

            imgAvatarShadow?.visibility = View.VISIBLE
        } else {
            // ✅ Ẩn shadow layer khi shadow = 0
            imgAvatarShadow?.visibility = View.GONE
        }

        // Re-enable shadow seekbar when loading new image
        binding.seekBarFilterShadow.isEnabled = true
    }

    /**
     * Load default avatar.webp from drawable when first entering Editor without imported image
     * Creates URI from drawable resource and saves it to tempSelectedImageUri
     */
    private fun loadDefaultAvatarWebp() {
        android.util.Log.d("SaveDebug", "═══════════════════════════════════════")
        android.util.Log.d("SaveDebug", "loadDefaultAvatarWebp() called")

        // Create URI from drawable resource
        val defaultAvatarUri = Uri.parse("android.resource://${packageName}/${R.drawable.avatar}")
        android.util.Log.d("SaveDebug", "Created URI: $defaultAvatarUri")

        // Save to local variable
        tempSelectedImageUri = defaultAvatarUri
        android.util.Log.d("SaveDebug", "Set tempSelectedImageUri: $tempSelectedImageUri")

        // Load into imgAvatar and imgAvatarShadow
        loadImageToAvatars(defaultAvatarUri)

        android.util.Log.d("SaveDebug", "Loaded default avatar.webp from drawable")
        android.util.Log.d("SaveDebug", "═══════════════════════════════════════")
    }

    /**
     * Apply shadow effect to imgAvatarShadow layer
     * Reloads shadow with transformation that follows alpha channel (contour shadow like icon)
     */
    private fun applyShadowEffect(shadowValue: Float) {
        android.util.Log.d("PhotoShadow", "=== applyShadowEffect called ===")
        android.util.Log.d("PhotoShadow", "seekbar value: $shadowValue")

        // Remap: seekbar 0-100 → effective shadow 35-100
        val effectiveShadowValue = 35f + (shadowValue / 100f * 65f)
        android.util.Log.d("PhotoShadow", "effectiveShadowValue (35-100): $effectiveShadowValue")

        val shadowView = imgAvatarShadow
        android.util.Log.d("PhotoShadow", "shadowView is null: ${shadowView == null}")
        if (shadowView == null) return

        if (shadowValue <= 0) {
            android.util.Log.d("PhotoShadow", "shadowValue <= 0, hiding shadow")
            shadowView.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                shadowView.setRenderEffect(null)
            }
            return
        }

        android.util.Log.d("PhotoShadow", "Setting visibility to VISIBLE")
        shadowView.visibility = View.VISIBLE

        // Reload shadow with new transformation parameters
        // Read from local variable instead of ViewModel
        val currentUri = tempSelectedImageUri
        android.util.Log.d("PhotoShadow", "currentUri: $currentUri")
        if (currentUri != null) {
            val shadowRadius = effectiveShadowValue / 100f * 15f // 35-100 → 5.25-15px
            val shadowAlpha = effectiveShadowValue / 100f * 0.9f  // 35-100 → 0.315-0.9
            android.util.Log.d("PhotoShadow", "Loading shadow: radius=$shadowRadius, alpha=$shadowAlpha")

            Glide.with(this)
                .load(currentUri)
                .transform(CenterCrop(), ShadowTransformation(shadowRadius, shadowAlpha))
                .into(shadowView)
        } else {
            android.util.Log.e("PhotoShadow", "currentUri is NULL - cannot load shadow!")
        }

        // 1. Alpha - overall shadow visibility
        val viewAlpha = (effectiveShadowValue / 100f).coerceIn(0f, 1f)  // 35-100 → 0.35-1.0
        shadowView.alpha = viewAlpha
        android.util.Log.d("PhotoShadow", "viewAlpha: $viewAlpha")

        // 2. Offset - shadow displacement
        val offsetX = effectiveShadowValue / 100f * 5f   // 35-100 → 1.75-5dp
        val offsetY = effectiveShadowValue / 100f * 5f   // 35-100 → 1.75-5dp
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY
        android.util.Log.d("PhotoShadow", "offset: X=$offsetX, Y=$offsetY")

        // 3. Scale - noticeable shadow width increase
        val scale = 1f + (effectiveShadowValue / 100f * 0.15f)  // 35-100 → 1.0525-1.15 (~10% difference)
        shadowView.scaleX = scale
        shadowView.scaleY = scale
        android.util.Log.d("PhotoShadow", "scale: $scale")

        // 4. Additional blur via RenderEffect (API 31+) - DISABLED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            shadowView.setRenderEffect(null)
            android.util.Log.d("PhotoShadow", "RenderEffect disabled (using Glide blur only)")
        }

        android.util.Log.d("PhotoShadow", "=== applyShadowEffect done ===")
    }

    private fun applyFilters() {
        lifecycleScope.launch {
            // Read from local variables instead of ViewModel
            val brightness = tempFilterBrightness
            val contrast = tempFilterContrast
            val saturation = tempFilterSaturate
            val grayscale = tempFilterGrayscale
            val hueRotate = tempFilterHueRotate
            val sepia = tempFilterSepia
            val blur = tempFilterBlur
            // Note: shadow is now handled separately by applyShadowEffect()

            val colorMatrix = ColorMatrix()

            // Brightness
            val brightnessMatrix = ColorMatrix(floatArrayOf(
                brightness, 0f, 0f, 0f, 0f,
                0f, brightness, 0f, 0f, 0f,
                0f, 0f, brightness, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(brightnessMatrix)

            // Contrast
            val scale = contrast
            val translate = (1f - contrast) / 2f * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(contrastMatrix)

            // Saturation
            val saturationMatrix = ColorMatrix()
            saturationMatrix.setSaturation(saturation)
            colorMatrix.postConcat(saturationMatrix)

            // Grayscale
            if (grayscale > 0) {
                val invGrayscale = 1 - grayscale
                val grayscaleMatrix = ColorMatrix(floatArrayOf(
                    invGrayscale + grayscale * 0.299f, grayscale * 0.587f, grayscale * 0.114f, 0f, 0f,
                    grayscale * 0.299f, invGrayscale + grayscale * 0.587f, grayscale * 0.114f, 0f, 0f,
                    grayscale * 0.299f, grayscale * 0.587f, invGrayscale + grayscale * 0.114f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(grayscaleMatrix)
            }

            // Hue Rotation
            if (hueRotate != 0f) {
                val angle = hueRotate * Math.PI.toFloat() / 180f
                val cosA = kotlin.math.cos(angle.toDouble()).toFloat()
                val sinA = kotlin.math.sin(angle.toDouble()).toFloat()

                // Hue rotation matrix
                val hueRotateMatrix = ColorMatrix(floatArrayOf(
                    0.213f + cosA * 0.787f - sinA * 0.213f, 0.715f - cosA * 0.715f - sinA * 0.715f, 0.072f - cosA * 0.072f + sinA * 0.928f, 0f, 0f,
                    0.213f - cosA * 0.213f + sinA * 0.143f, 0.715f + cosA * 0.285f + sinA * 0.140f, 0.072f - cosA * 0.072f - sinA * 0.283f, 0f, 0f,
                    0.213f - cosA * 0.213f - sinA * 0.787f, 0.715f - cosA * 0.715f + sinA * 0.715f, 0.072f + cosA * 0.928f + sinA * 0.072f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(hueRotateMatrix)
            }

            // Sepia
            if (sepia > 0) {
                val invSepia = 1 - sepia
                val sepiaMatrix = ColorMatrix(floatArrayOf(
                    invSepia + sepia * 0.393f, sepia * 0.769f, sepia * 0.189f, 0f, 0f,
                    sepia * 0.349f, invSepia + sepia * 0.686f, sepia * 0.168f, 0f, 0f,
                    sepia * 0.272f, sepia * 0.534f, invSepia + sepia * 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(sepiaMatrix)
            }

            // Apply ColorMatrix filter
            imgAvatar?.colorFilter = ColorMatrixColorFilter(colorMatrix)

            // Note: Shadow is now handled by applyShadowEffect() using shadow layer approach
            // Old elevation-based shadow code removed as it didn't work properly with ImageView

            // Apply Blur
            // NOTE: Blur is now handled by Glide BlurTransformation for ALL Android versions
            // RenderEffect on Android 12+ caused zoom artifacts, so we disabled it
            android.util.Log.d("PhotoBlur", "=== applyFilters() - Blur section ===")
            android.util.Log.d("PhotoBlur", "Blur is handled by Glide BlurTransformation in reloadImageWithBlur()")

            // Make sure no RenderEffect is applied
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                imgAvatar?.setRenderEffect(null)
                android.util.Log.d("PhotoBlur", "Cleared any existing RenderEffect")
            }
        }
    }


    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * Apply shadow effect to template background
     * Controlled by Poster Shadow seekbar
     * WORKS EXACTLY LIKE Photo Filter Shadow:
     * - Photo Filter: Load user image → ShadowTransformation → imgAvatarShadow
     * - Poster Shadow: Load template drawable → ShadowTransformation → imgTemplateShadow
     * Both create contour shadow following the object shape!
     */
    private fun applyTemplateShadow(shadowValue: Float) {
        android.util.Log.d("PosterShadow", "=== applyTemplateShadow called ===")
        android.util.Log.d("PosterShadow", "seekbar value: $shadowValue")

        val shadowView = imgTemplateShadow
        android.util.Log.d("PosterShadow", "shadowView is null: ${shadowView == null}")
        if (shadowView == null) return

        if (shadowValue <= 0) {
            android.util.Log.d("PosterShadow", "shadowValue <= 0, hiding shadow")
            shadowView.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                shadowView.setRenderEffect(null)
            }
            return
        }

        android.util.Log.d("PosterShadow", "Setting visibility to VISIBLE")
        shadowView.visibility = View.VISIBLE

        // EXACTLY LIKE Photo Filter: Reload with new transformation parameters
        val shadowRadius = shadowValue / 100f * 15f  // 0-15px blur (SAME as Photo Filter)
        val shadowAlpha = shadowValue / 100f * 0.9f   // Dynamic alpha
        android.util.Log.d("PosterShadow", "shadowRadius: $shadowRadius, shadowAlpha: $shadowAlpha")

        // Load template from assets with ShadowTransformation
        // This creates shadow following the template's alpha channel/contour
        val templateId = viewModel.selectedTemplate.value
        val templatePath = AssetHelper.getTemplateItemPath(templateId)
        android.util.Log.d("PosterShadow", "templateId: $templateId, templatePath: $templatePath")

        Glide.with(this)
            .load(templatePath)
            .transform(ShadowTransformation(shadowRadius, shadowAlpha))
            .into(shadowView)

        // 1. Alpha - overall shadow visibility (MATCHED with Photo Filter)
        val viewAlpha = (shadowValue / 100f).coerceIn(0f, 1f)
        shadowView.alpha = viewAlpha
        android.util.Log.d("PosterShadow", "viewAlpha: $viewAlpha")

        // 2. Offset - shadow displacement (MATCHED with Photo Filter)
        val offsetX = shadowValue / 100f * 5f   // 0-5dp (SAME as Photo Filter)
        val offsetY = shadowValue / 100f * 5f   // 0-5dp (SAME as Photo Filter)
        shadowView.translationX = offsetX
        shadowView.translationY = offsetY
        android.util.Log.d("PosterShadow", "offset: X=$offsetX, Y=$offsetY")

        // 3. Scale - very minimal to maintain shape accuracy (MATCHED)
        val scale = 1f + (shadowValue / 100f * 0.03f)  // 1.0-1.03 (SAME as Photo Filter)
        shadowView.scaleX = scale
        shadowView.scaleY = scale
        android.util.Log.d("PosterShadow", "scale: $scale")

        // 4. Additional blur via RenderEffect (API 31+) for extra softness (MATCHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val additionalBlur = shadowValue / 100f * 10f  // 0-10px additional blur (SAME as Photo Filter)
            android.util.Log.d("PosterShadow", "Android 12+: additionalBlur=$additionalBlur")
            if (additionalBlur > 0) {
                val blurEffect = RenderEffect.createBlurEffect(
                    additionalBlur, additionalBlur, Shader.TileMode.CLAMP
                )
                shadowView.setRenderEffect(blurEffect)
            } else {
                shadowView.setRenderEffect(null)
            }
        } else {
            android.util.Log.d("PosterShadow", "Android < 12: No RenderEffect")
        }
    }
}

