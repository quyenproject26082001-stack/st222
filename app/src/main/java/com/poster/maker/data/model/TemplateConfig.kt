package poster.maker.data.model

import android.graphics.RectF

/**
 * Configuration for each wanted poster template
 * Defines positions and properties of editable elements
 */
data class TemplateConfig(
    val id: Int,

    // Name field configuration
    val hasName: Boolean = false,
    val nameDefaultText: String = "NAME HERE",
    val namePositionX: Float = 0.5f,  // 0-1 relative to width (0.5 = center)
    val namePositionY: Float = 0.65f, // 0-1 relative to height
    val nameColor: String = "#000000",
    val nameSize: Float = 24f,
    val maxNameLength: Int? = null,  // Max name text length for validation/random (null = no limit)

    // Bounty field configuration
    val hasBounty: Boolean = true,
    val bountyDefaultText: String = "2,000,000",
    val bountyPrefix: String = "$",   // "$", "₿", ""
    val bountySuffix: String = "",    // "$", " CASH", ""
    val bountyPositionX: Float = 0.5f,
    val bountyPositionY: Float = 0.9f,
    val bountyColor: String = "#000000",
    val bountySize: Float = 20f,
    val maxBountyLength: Int? = null,  // Max bounty text length for random generation (null = no limit)

    // Photo area configuration (relative coordinates 0-1)
    val photoLeft: Float = 0.15f,
    val photoTop: Float = 0.25f,
    val photoRight: Float = 0.85f,
    val photoBottom: Float = 0.6f
) {
    /**
     * Get photo area as RectF (relative coordinates)
     */
    fun getPhotoRect(): RectF {
        return RectF(photoLeft, photoTop, photoRight, photoBottom)
    }
}

/**
 * Provider for template configurations
 */
object TemplateConfigProvider {

    private val configs = mapOf(
        // Template 1: Classic with NAME HERE
        1 to TemplateConfig(
            id = 1,
            hasName = true,
            nameDefaultText = "NAME HERE",
            namePositionX = 0.5f,
            namePositionY = 0.68f,
            nameColor = "#3D3D3D",
            nameSize = 28f,
            maxNameLength = 9,  // "NAME HERE" = 9 chars, allow up to 20
            hasBounty = true,
            bountyDefaultText = "2,000,000",
            bountyPrefix = "$",
            bountySuffix = "",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.92f,
            bountyColor = "#3D3D3D",
            bountySize = 32f,
            maxBountyLength = 10,  // "$2,000,000" = 10 chars, allow up to 12
            photoLeft = 0.12f,
            photoTop = 0.28f,
            photoRight = 0.88f,
            photoBottom = 0.62f
        ),

        // Template 2: Grunge style, no name
        2 to TemplateConfig(
            id = 2,
            hasName = false,
            hasBounty = true,
            bountyDefaultText = "20000",
            bountyPrefix = "",
            bountySuffix = "$",
            bountyPositionX = 0.65f,
            bountyPositionY = 0.92f,
            bountyColor = "#3D3D3D",
            bountySize = 36f,
            maxBountyLength = 7,
            photoLeft = 0.15f,
            photoTop = 0.30f,
            photoRight = 0.85f,
            photoBottom = 0.75f
        ),

        // Template 3: Dark/Pirate with skulls, no name
        3 to TemplateConfig(
            id = 3,
            hasName = false,
            hasBounty = true,
            bountyDefaultText = "1,000,000",
            bountyPrefix = "$",
            bountySuffix = "",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.93f,
            bountyColor = "#776955",
            bountySize = 24f,
            photoLeft = 0.15f,
            photoTop = 0.18f,  // Adjusted from 0.25f
            photoRight = 0.85f,
            photoBottom = 0.78f
        ),

        // Template 4: Victorian style with NAME HERE
        4 to TemplateConfig(
            id = 4,
            hasName = true,
            nameDefaultText = "NAME HERE",
            namePositionX = 0.5f,
            namePositionY = 0.58f,  // Adjusted from 0.52f
            nameColor = "#3D3D3D",
            nameSize = 26f,
            hasBounty = true,
            bountyDefaultText = "100,000",
            bountyPrefix = "",
            bountySuffix = " CASH",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.92f,
            bountyColor = "#3D3D3D",
            bountySize = 17f,
            photoLeft = 0.20f,
            photoTop = 0.22f,
            photoRight = 0.80f,
            photoBottom = 0.55f  // Adjusted from 0.48f
        ),

        // Template 5: Simple with NAME HERE
        5 to TemplateConfig(
            id = 5,
            hasName = true,
            nameDefaultText = "NAME HERE",
            namePositionX = 0.5f,
            namePositionY = 0.66f,  // Adjusted from 0.62f
            nameColor = "#3D3D3D",
            nameSize = 22f,
            maxNameLength = 9,
            hasBounty = true,
            bountyDefaultText = "1,000,000",
            bountyPrefix = "$",
            bountySuffix = "",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.88f,
            bountyColor = "#3D3D3D",
            bountySize = 20f,
            photoLeft = 0.18f,
            photoTop = 0.15f,  // Adjusted from 0.18f
            photoRight = 0.82f,
            photoBottom = 0.62f  // Adjusted from 0.58f
        ),

        // Template 6: One Piece / Marine style
        6 to TemplateConfig(
            id = 6,
            hasName = true,
            nameDefaultText = "NAME HERE",
            namePositionX = 0.5f,
            namePositionY = 0.76f,  // Adjusted from 0.72f
            nameColor = "#000000",
            nameSize = 24f,
            hasBounty = true,
            bountyDefaultText = "330,000,000",
            bountyPrefix = "",
            maxBountyLength = 14,
            bountySuffix = "-",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.84f,  // Adjusted from 0.82f
            bountyColor = "#000000",
            bountySize = 20f,
            photoLeft = 0.15f,
            photoTop = 0.18f,  // Adjusted from 0.20f
            photoRight = 0.85f,
            photoBottom = 0.68f
        ),

        // Template 7: State Police detailed
        7 to TemplateConfig(
            id = 7,
            hasName = true,
            nameDefaultText = "NAME HERE",
            namePositionX = 0.5f,
            namePositionY = 0.68f,  // Adjusted from 0.60f
            nameColor = "#5D4E37",
            nameSize = 22f,
            maxBountyLength = 6,

            hasBounty = true,
            bountyDefaultText = "20,000",
            bountyPrefix = "",
            bountySuffix = "",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.75f,  // Adjusted from 0.66f
            bountyColor = "#5D4E37",
            bountySize = 28f,
            photoLeft = 0.18f,
            photoTop = 0.28f,
            photoRight = 0.82f,
            photoBottom = 0.62f  // Adjusted from 0.56f
        ),

        // Template 8: Clean design, no name
        8 to TemplateConfig(
            id = 8,
            hasName = false,
            hasBounty = true,
            bountyDefaultText = "10,000",
            bountyPrefix = "",
            bountySuffix = "",
            maxBountyLength = 6,

            bountyPositionX = 0.5f,
            bountyPositionY = 0.90f,  // Adjusted from 0.88f
            bountyColor = "#3D3D3D",
            bountySize = 32f,
            photoLeft = 0.12f,
            photoTop = 0.25f,  // Adjusted from 0.30f
            photoRight = 0.88f,
            photoBottom = 0.72f  // Adjusted from 0.75f
        ),

        // Template 9: Parchment red text, no name
        9 to TemplateConfig(
            id = 9,
            hasName = false,
            hasBounty = true,
            bountyDefaultText = "15,000",
            bountyPrefix = "",
            maxBountyLength = 6,

            bountySuffix = "",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.92f,  // Adjusted from 0.90f
            bountyColor = "#8B4513",
            bountySize = 28f,
            photoLeft = 0.15f,
            photoTop = 0.22f,  // Adjusted from 0.28f
            photoRight = 0.85f,
            photoBottom = 0.78f
        ),

        // Template 10: Parchment brown, no name
        10 to TemplateConfig(
            id = 10,
            hasName = false,
            hasBounty = true,
            bountyDefaultText = "150,000",
            bountyPrefix = "$ ",
            bountySuffix = "",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.90f,
            bountyColor = "#5D4037",
            bountySize = 28f,
            photoLeft = 0.15f,
            photoTop = 0.22f,  // Adjusted from 0.28f
            photoRight = 0.85f,
            photoBottom = 0.78f
        ),

        // Template 11: Sheriff's Office ornate, no name
        11 to TemplateConfig(
            id = 11,
            hasName = false,
            hasBounty = true,
            bountyDefaultText = "10,000",
            bountyPrefix = "$",
            bountySuffix = "",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.88f,
            bountyColor = "#5D4037",
            bountySize = 32f,
            photoLeft = 0.18f,
            photoTop = 0.28f,  // Adjusted from 0.32f
            photoRight = 0.82f,
            photoBottom = 0.75f
        ),

        // Template 12: Compact, no name
        12 to TemplateConfig(
            id = 12,
            hasName = false,
            hasBounty = true,
            bountyDefaultText = "20,000",
            bountyPrefix = "$",
            maxBountyLength = 6,

            bountySuffix = "",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.85f,  // Adjusted from 0.82f
            bountyColor = "#5D4037",
            bountySize = 24f,
            photoLeft = 0.20f,
            photoTop = 0.25f,  // Adjusted from 0.30f
            photoRight = 0.80f,
            photoBottom = 0.72f  // Adjusted from 0.70f
        ),

        // Template 13: Retro with double border, no name
        13 to TemplateConfig(
            id = 13,
            hasName = false,
            hasBounty = true,
            bountyDefaultText = "1,000,000",
            bountyPrefix = "",
            maxBountyLength = 12,

            bountySuffix = "",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.92f,  // Adjusted from 0.90f
            bountyColor = "#8B4513",
            bountySize = 24f,
            photoLeft = 0.15f,
            photoTop = 0.18f,  // Adjusted from 0.28f
            photoRight = 0.85f,
            photoBottom = 0.72f
        ),

        // Template 14: Modern with stars and NAME HERE
        14 to TemplateConfig(
            id = 14,
            hasName = true,
            nameDefaultText = "NAME HERE",
            namePositionX = 0.5f,
            namePositionY = 0.64f,  // Adjusted from 0.62f
            nameColor = "#3D3D3D",
            nameSize = 28f,
            hasBounty = true,
            bountyDefaultText = "2,000,000",
            bountyPrefix = "$ ",
            bountySuffix = "",
            bountyPositionX = 0.5f,
            bountyPositionY = 0.92f,  // Adjusted from 0.88f
            bountyColor = "#3D3D3D",
            bountySize = 24f,
            photoLeft = 0.15f,
            photoTop = 0.18f,  // Adjusted from 0.22f
            photoRight = 0.85f,
            photoBottom = 0.58f
        ),

        // Template 15: Dark frame with pins, no name
        15 to TemplateConfig(
            id = 15,
            hasName = false,
            hasBounty = true,
            bountyDefaultText = "100,000",
            bountyPrefix = "$ ",
            bountySuffix = "",
            maxBountyLength = 15,
            bountyPositionX = 0.5f,
            bountyPositionY = 0.82f,  // Adjusted from 0.78f
            bountyColor = "#F5F5DC",
            bountySize = 32f,
            photoLeft = 0.12f,
            photoTop = 0.18f,
            photoRight = 0.88f,
            photoBottom = 0.72f
        ),

        // Template 16: Compact dark, no name
        16 to TemplateConfig(
            id = 16,
            hasName = false,
            hasBounty = true,
            bountyDefaultText = "10,000",
            bountyPrefix = "$ ",
            bountySuffix = "",
            maxBountyLength = 8,
            bountyPositionX = 0.5f,
            bountyPositionY = 0.72f,  // Adjusted from 0.68f
            bountyColor = "#3D2B1F",
            bountySize = 28f,
            photoLeft = 0.22f,
            photoTop = 0.22f,  // Adjusted from 0.28f
            photoRight = 0.78f,
            photoBottom = 0.62f
        )
    )

    /**
     * Get configuration for a specific template
     * @param templateId Template ID (1-16)
     * @return TemplateConfig or default config if not found
     */
    fun getConfig(templateId: Int): TemplateConfig {
        return configs[templateId] ?: getDefaultConfig(templateId)
    }

    /**
     * Get default config for unknown template
     */
    private fun getDefaultConfig(templateId: Int): TemplateConfig {
        return TemplateConfig(
            id = templateId,
            hasName = true,
            hasBounty = true
        )
    }

    /**
     * Get all available template IDs
     */
    fun getAllTemplateIds(): List<Int> {
        return configs.keys.sorted()
    }

    /**
     * Check if template has name field
     */
    fun hasNameField(templateId: Int): Boolean {
        return getConfig(templateId).hasName
    }

    /**
     * Check if template has bounty field
     */
    fun hasBountyField(templateId: Int): Boolean {
        return getConfig(templateId).hasBounty
    }
}
