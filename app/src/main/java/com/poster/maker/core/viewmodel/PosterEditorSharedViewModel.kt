package poster.maker.core.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import poster.maker.data.model.TemplateConfig
import poster.maker.data.model.TemplateConfigProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared ViewModel for MakeScreenActivity and WantedEditorActivity
 * Provides synchronized data binding between the two screens
 */
class PosterEditorSharedViewModel : ViewModel() {

    // Current template configuration
    private val _currentConfig = MutableStateFlow(TemplateConfigProvider.getConfig(1))
    val currentConfig: StateFlow<TemplateConfig> = _currentConfig.asStateFlow()

    // Image URI
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // Selected template (1-16)
    private val _selectedTemplate = MutableStateFlow(1)
    val selectedTemplate: StateFlow<Int> = _selectedTemplate.asStateFlow()

    // Track if there are unsaved changes
    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    // Track if editing has started (switch from avatar.png to item.png)
    private val _isEditingStarted = MutableStateFlow(false)
    val isEditingStarted: StateFlow<Boolean> = _isEditingStarted.asStateFlow()

    // Section expansion states (UI state for WantedEditorActivity)
    private val _isNameSectionExpanded = MutableStateFlow(false)
    val isNameSectionExpanded: StateFlow<Boolean> = _isNameSectionExpanded.asStateFlow()

    private val _isBountySectionExpanded = MutableStateFlow(false)
    val isBountySectionExpanded: StateFlow<Boolean> = _isBountySectionExpanded.asStateFlow()

    private val _isPhotoFilterSectionExpanded = MutableStateFlow(false)
    val isPhotoFilterSectionExpanded: StateFlow<Boolean> = _isPhotoFilterSectionExpanded.asStateFlow()

    private val _isPosterShadowSectionExpanded = MutableStateFlow(false)
    val isPosterShadowSectionExpanded: StateFlow<Boolean> = _isPosterShadowSectionExpanded.asStateFlow()

    // Name properties
    private val _nameText = MutableStateFlow("NAME HERE")
    val nameText: StateFlow<String> = _nameText.asStateFlow()

    private val _nameFont = MutableStateFlow("Old Town")
    val nameFont: StateFlow<String> = _nameFont.asStateFlow()

    private val _nameSpacing = MutableStateFlow(0f)
    val nameSpacing: StateFlow<Float> = _nameSpacing.asStateFlow()

    // Bounty properties
    private val _bountyText = MutableStateFlow("$2,000,000")
    val bountyText: StateFlow<String> = _bountyText.asStateFlow()

    private val _bountyFont = MutableStateFlow("Old Town")
    val bountyFont: StateFlow<String> = _bountyFont.asStateFlow()



    private val _bountySize = MutableStateFlow(24f)
    val bountySize: StateFlow<Float> = _bountySize.asStateFlow()

    private val _bountyWeight = MutableStateFlow(400f)
    val bountyWeight: StateFlow<Float> = _bountyWeight.asStateFlow()

    private val _bountySpacing = MutableStateFlow(0f)
    val bountySpacing: StateFlow<Float> = _bountySpacing.asStateFlow()

    private val _bountyPositionX = MutableStateFlow(0f)
    val bountyPositionX: StateFlow<Float> = _bountyPositionX.asStateFlow()

    private val _bountyPositionY = MutableStateFlow(0f)
    val bountyPositionY: StateFlow<Float> = _bountyPositionY.asStateFlow()

    // Photo Filter properties
    private val _filterShadow = MutableStateFlow(0f)
    val filterShadow: StateFlow<Float> = _filterShadow.asStateFlow()

    private val _filterBlur = MutableStateFlow(0f)
    val filterBlur: StateFlow<Float> = _filterBlur.asStateFlow()

    private val _filterBrightness = MutableStateFlow(1f)
    val filterBrightness: StateFlow<Float> = _filterBrightness.asStateFlow()

    private val _filterContrast = MutableStateFlow(1f)
    val filterContrast: StateFlow<Float> = _filterContrast.asStateFlow()

    private val _filterGrayscale = MutableStateFlow(0f)
    val filterGrayscale: StateFlow<Float> = _filterGrayscale.asStateFlow()

    private val _filterHueRotate = MutableStateFlow(0f)
    val filterHueRotate: StateFlow<Float> = _filterHueRotate.asStateFlow()

    private val _filterSaturate = MutableStateFlow(1f)
    val filterSaturate: StateFlow<Float> = _filterSaturate.asStateFlow()

    private val _filterSepia = MutableStateFlow(0f)
    val filterSepia: StateFlow<Float> = _filterSepia.asStateFlow()

    // Poster Shadow
    private val _posterShadow = MutableStateFlow(0f)
    val posterShadow: StateFlow<Float> = _posterShadow.asStateFlow()

    // Saved image path (for SuccessActivity)
    private val _savedImagePath = MutableStateFlow<String?>(null)
    val savedImagePath: StateFlow<String?> = _savedImagePath.asStateFlow()

    // Setters
    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
        _hasChanges.value = true
    }

    fun setSelectedTemplate(template: Int) {
        _selectedTemplate.value = template
        _currentConfig.value = TemplateConfigProvider.getConfig(template)
        // Only reset text to defaults if editing hasn't started yet
        // This preserves user's edited name/bounty when switching templates
        if (!_isEditingStarted.value) {
            initFromConfig()
        }
        _hasChanges.value = true
    }

    /**
     * Initialize values from current template config
     */
    fun initFromConfig() {
        val config = _currentConfig.value
        _nameText.value = config.nameDefaultText
        _bountyText.value = "${config.bountyPrefix}${config.bountyDefaultText}${config.bountySuffix}"
        _bountySize.value = config.bountySize
    }

    /**
     * Get current template config
     */
    fun getConfig(): TemplateConfig = _currentConfig.value

    // Section toggle functions
    fun toggleNameSection() {
        _isNameSectionExpanded.value = !_isNameSectionExpanded.value
    }

    fun toggleBountySection() {
        _isBountySectionExpanded.value = !_isBountySectionExpanded.value
    }

    fun togglePhotoFilterSection() {
        _isPhotoFilterSectionExpanded.value = !_isPhotoFilterSectionExpanded.value
    }

    fun togglePosterShadowSection() {
        _isPosterShadowSectionExpanded.value = !_isPosterShadowSectionExpanded.value
    }

    fun setNameText(text: String) {
        _nameText.value = text
        _hasChanges.value = true
    }

    fun setNameFont(font: String) {
        _nameFont.value = font
        _hasChanges.value = true
    }



    fun setNameSpacing(spacing: Float) {
        _nameSpacing.value = spacing
        _hasChanges.value = true
    }

    fun setBountyText(text: String) {
        _bountyText.value = text
        _hasChanges.value = true
    }

    fun setBountyFont(font: String) {
        _bountyFont.value = font
        _hasChanges.value = true
    }

    fun setBountySize(size: Float) {
        _bountySize.value = size
        _hasChanges.value = true
    }

    fun setBountyWeight(weight: Float) {
        _bountyWeight.value = weight
        _hasChanges.value = true
    }

    fun setBountySpacing(spacing: Float) {
        _bountySpacing.value = spacing
        _hasChanges.value = true
    }

    fun setBountyPositionX(x: Float) {
        _bountyPositionX.value = x
        _hasChanges.value = true
    }

    fun setBountyPositionY(y: Float) {
        _bountyPositionY.value = y
        _hasChanges.value = true
    }

    fun setFilterShadow(value: Float) {
        _filterShadow.value = value
        _hasChanges.value = true
    }

    fun setFilterBlur(value: Float) {
        _filterBlur.value = value
        _hasChanges.value = true
    }

    fun setFilterBrightness(value: Float) {
        _filterBrightness.value = value
        _hasChanges.value = true
    }

    fun setFilterContrast(value: Float) {
        _filterContrast.value = value
        _hasChanges.value = true
    }

    fun setFilterGrayscale(value: Float) {
        _filterGrayscale.value = value
        _hasChanges.value = true
    }

    fun setFilterHueRotate(value: Float) {
        _filterHueRotate.value = value
        _hasChanges.value = true
    }

    fun setFilterSaturate(value: Float) {
        _filterSaturate.value = value
        _hasChanges.value = true
    }

    fun setFilterSepia(value: Float) {
        _filterSepia.value = value
        _hasChanges.value = true
    }

    fun setPosterShadow(value: Float) {
        _posterShadow.value = value
        _hasChanges.value = true
    }

    fun setSavedImagePath(path: String?) {
        _savedImagePath.value = path
    }

    /**
     * Mark that editing has started (user made first edit)
     * This switches MakeScreen from avatar.png to item.png display
     */
    fun markEditingStarted() {
        _isEditingStarted.value = true
    }

    fun resetAll() {
        val config = _currentConfig.value
        // Reset to config defaults
        _nameText.value = config.nameDefaultText
        _nameFont.value = "Old Town"
        _bountyFont.value = "Roboto Bold"
        _nameSpacing.value = 0f
        _bountyText.value = "${config.bountyPrefix}${config.bountyDefaultText}${config.bountySuffix}"
        _bountyFont.value = "Old Town"
        _bountySize.value = config.bountySize
        _bountyWeight.value = 400f
        _bountySpacing.value = 0f
        _bountyPositionX.value = 0f
        _bountyPositionY.value = 0f
        _filterShadow.value = 0f
        _filterBlur.value = 0f
        _filterBrightness.value = 1f
        _filterContrast.value = 1f
        _filterGrayscale.value = 0f
        _filterHueRotate.value = 0f
        _filterSaturate.value = 1f
        _filterSepia.value = 0f
        _posterShadow.value = 0f
        _hasChanges.value = true
    }

    /**
     * Clear all state (call when leaving the poster editor flow)
     */
    fun clearAll() {
        _selectedImageUri.value = null
        _selectedTemplate.value = 1
        _currentConfig.value = TemplateConfigProvider.getConfig(1)
        _hasChanges.value = false
        _isEditingStarted.value = false
        _savedImagePath.value = null
        resetAll()
        _hasChanges.value = false
    }

    companion object {
        @Volatile
        private var instance: PosterEditorSharedViewModel? = null

        fun getInstance(): PosterEditorSharedViewModel {
            return instance ?: synchronized(this) {
                instance ?: PosterEditorSharedViewModel().also { instance = it }
            }
        }

        /**
         * Clear the singleton instance (call when the flow is completely done)
         */
        fun clearInstance() {
            instance?.clearAll()
            instance = null
        }
    }
}
