package poster.maker.activity_app.makescreen

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for MakeScreen
 * Manages poster data that can be edited and passed to WantedEditorActivity
 */
class MakeScreenViewModel : ViewModel() {

    // Selected template (1-15)
    private val _selectedTemplate = MutableStateFlow(1)
    val selectedTemplate: StateFlow<Int> = _selectedTemplate

    // Image URI
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    // Name text
    private val _nameText = MutableStateFlow("NAME HERE")
    val nameText: StateFlow<String> = _nameText

    // Bounty text
    private val _bountyText = MutableStateFlow("$2,000,000")
    val bountyText: StateFlow<String> = _bountyText

    // Has been edited flag (to show "Discard changes?" dialog on back)
    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges

    // Filter values (from WantedEditorActivity)
    private val _filterBrightness = MutableStateFlow(1f)
    val filterBrightness: StateFlow<Float> = _filterBrightness

    private val _filterContrast = MutableStateFlow(1f)
    val filterContrast: StateFlow<Float> = _filterContrast

    private val _filterSaturate = MutableStateFlow(1f)
    val filterSaturate: StateFlow<Float> = _filterSaturate

    private val _filterGrayscale = MutableStateFlow(0f)
    val filterGrayscale: StateFlow<Float> = _filterGrayscale

    private val _filterHueRotate = MutableStateFlow(0f)
    val filterHueRotate: StateFlow<Float> = _filterHueRotate

    private val _filterSepia = MutableStateFlow(0f)
    val filterSepia: StateFlow<Float> = _filterSepia

    private val _filterBlur = MutableStateFlow(0f)
    val filterBlur: StateFlow<Float> = _filterBlur

    private val _filterShadow = MutableStateFlow(0f)
    val filterShadow: StateFlow<Float> = _filterShadow

    private val _posterShadow = MutableStateFlow(0f)
    val posterShadow: StateFlow<Float> = _posterShadow

    // Name properties
    private val _nameFont = MutableStateFlow("Roboto Bold")
    val nameFont: StateFlow<String> = _nameFont

    private val _nameSpacing = MutableStateFlow(0.1f)
    val nameSpacing: StateFlow<Float> = _nameSpacing

    // Bounty properties
    private val _bountySize = MutableStateFlow(28f)
    val bountySize: StateFlow<Float> = _bountySize

    private val _bountyWeight = MutableStateFlow(0f)
    val bountyWeight: StateFlow<Float> = _bountyWeight

    private val _bountySpacing = MutableStateFlow(0f)
    val bountySpacing: StateFlow<Float> = _bountySpacing

    private val _bountyPositionX = MutableStateFlow(0f)
    val bountyPositionX: StateFlow<Float> = _bountyPositionX

    private val _bountyPositionY = MutableStateFlow(0f)
    val bountyPositionY: StateFlow<Float> = _bountyPositionY

    fun setSelectedTemplate(template: Int) {
        _selectedTemplate.value = template
        _hasChanges.value = true
    }

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
        _hasChanges.value = true
    }

    fun setNameText(text: String) {
        _nameText.value = text
        _hasChanges.value = true
    }

    fun setBountyText(text: String) {
        _bountyText.value = text
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

    fun setFilterSaturate(value: Float) {
        _filterSaturate.value = value
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

    fun setFilterSepia(value: Float) {
        _filterSepia.value = value
        _hasChanges.value = true
    }

    fun setFilterBlur(value: Float) {
        _filterBlur.value = value
        _hasChanges.value = true
    }

    fun setFilterShadow(value: Float) {
        _filterShadow.value = value
        _hasChanges.value = true
    }

    fun setPosterShadow(value: Float) {
        _posterShadow.value = value
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

    fun resetChangesFlag() {
        _hasChanges.value = false
    }

    /**
     * Reset all values to default
     */
    fun resetAll() {
        _selectedTemplate.value = 1
        _selectedImageUri.value = null
        _nameText.value = "NAME HERE"
        _bountyText.value = "$2,000,000"
        _filterBrightness.value = 1f
        _filterContrast.value = 1f
        _filterSaturate.value = 1f
        _filterGrayscale.value = 0f
        _filterHueRotate.value = 0f
        _filterSepia.value = 0f
        _filterBlur.value = 0f
        _filterShadow.value = 0f
        _posterShadow.value = 0f
        _nameFont.value = "Roboto Bold"
        _nameSpacing.value = 0.1f
        _bountySize.value = 28f
        _bountyWeight.value = 0f
        _bountySpacing.value = 0f
        _bountyPositionX.value = 0f
        _bountyPositionY.value = 0f
        _hasChanges.value = false
    }
}

