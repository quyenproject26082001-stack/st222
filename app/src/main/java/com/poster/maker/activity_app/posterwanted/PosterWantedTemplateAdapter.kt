package poster.maker.activity_app.posterwanted

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import poster.maker.R
import poster.maker.data.local.entity.PosterWantedItem
import poster.maker.data.model.TemplateConfigProvider
import poster.maker.databinding.ItemPosterWantedTemplateBinding
import kotlinx.coroutines.*

class PosterWantedTemplateAdapter(
    private val items: List<PosterWantedItem>,
    private val onItemClick: (PosterWantedItem) -> Unit
) : RecyclerView.Adapter<PosterWantedTemplateAdapter.ViewHolder>() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPosterWantedTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelJob()
    }

    fun cleanup() {
        coroutineScope.cancel()
    }

    inner class ViewHolder(
        private val binding: ItemPosterWantedTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentJob: Job? = null

        fun bind(item: PosterWantedItem) {
            val context = binding.root.context

            // Cancel previous job if any
            currentJob?.cancel()
            // ✅ Set margin cho shimmer
            val marginInDp = 6 // margin 16dp
            val marginInPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                marginInDp.toFloat(),
                context.resources.displayMetrics
            ).toInt()

            val params = binding.imgShimmer.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
            binding.imgShimmer.layoutParams = params
            // Show shimmer, hide rendered poster initially
            binding.imgShimmer.visibility = View.VISIBLE
            binding.imgRenderedPoster.visibility = View.INVISIBLE

            // Click listener
            binding.rootContainer.setOnClickListener {
                android.util.Log.d("ThumbnailClick", "=========================================")
                android.util.Log.d("ThumbnailClick", "Template ${item.templateId} CLICKED!")
                android.util.Log.d("ThumbnailClick", "Thumbnail size: ${binding.imgRenderedPoster.width}x${binding.imgRenderedPoster.height}")
                android.util.Log.d("ThumbnailClick", "Opening MakeScreen...")
                android.util.Log.d("ThumbnailClick", "=========================================")
                onItemClick(item)
            }

            // Render bitmap in background
            currentJob = coroutineScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        renderTemplateToBitmap(context, item)
                    }
                    if (isActive) {
                        // Hide shimmer and show rendered poster
                        binding.imgShimmer.visibility = View.GONE
                        binding.imgRenderedPoster.visibility = View.VISIBLE
                        binding.imgRenderedPoster.setImageBitmap(bitmap)

                        // Wait for layout to update then log
                        binding.imgRenderedPoster.post {
                            android.util.Log.d("TemplateAdapter", "Item ${item.templateId} - LOADED: rootContainer height=${binding.rootContainer.height}, image height=${binding.imgRenderedPoster.height}")

                            // Calculate actual displayed text size on screen
                            val bitmapWidth = 1200f
                            val displayWidth = binding.imgRenderedPoster.width.toFloat()
                            val scaleRatio = displayWidth / bitmapWidth

                            val config = TemplateConfigProvider.getConfig(item.templateId)
                            val scaleFactor = 1200f / 1080f  // Same as render code
                            val fixedDensity = 3.5f  // Same as render code

                            // Calculate text sizes in bitmap
                            val nameTextSizeInBitmap = config.nameSize * scaleFactor * fixedDensity
                            val bountyTextSizeInBitmap = config.bountySize * scaleFactor * fixedDensity

                            // Calculate actual displayed text sizes on screen
                            val actualNameSize = nameTextSizeInBitmap * scaleRatio
                            val actualBountySize = bountyTextSizeInBitmap * scaleRatio

                            android.util.Log.d("ActualTextSize", "========================================")
                            android.util.Log.d("ActualTextSize", "Template ${item.templateId} - ACTUAL SIZE ON SCREEN:")
                            android.util.Log.d("ActualTextSize", "  Bitmap size: ${bitmapWidth.toInt()}px → Display size: ${displayWidth.toInt()}px (scale: ${String.format("%.2f", scaleRatio * 100)}%)")
                            if (config.hasName) {
                                android.util.Log.d("ActualTextSize", "  tvName: ${nameTextSizeInBitmap.toInt()}px → ${actualNameSize.toInt()}px")
                            }
                            android.util.Log.d("ActualTextSize", "  tvBounty: ${bountyTextSizeInBitmap.toInt()}px → ${actualBountySize.toInt()}px")
                            android.util.Log.d("ActualTextSize", "========================================")

                            // Force RecyclerView to recalculate layout
                            binding.rootContainer.requestLayout()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun cancelJob() {
            currentJob?.cancel()
        }

        private suspend fun renderTemplateToBitmap(
            context: android.content.Context,
            item: PosterWantedItem
        ): Bitmap {
            return withContext(Dispatchers.Main) {
                // Get the correct template layout resource
                val layoutResId = getTemplateLayoutResId(item.templateId)

                // Inflate the template layout
                val templateView = LayoutInflater.from(context).inflate(layoutResId, null)

                // Find views in the inflated template
                val imgTemplate = templateView.findViewById<ImageView>(R.id.imgTemplate)
                val imgAvatar = templateView.findViewById<ImageView>(R.id.imgAvatar)
                val tvName = templateView.findViewById<TextView>(R.id.tvName)
                val tvBounty = templateView.findViewById<TextView>(R.id.tvBounty)

                // Get template config for text sizes
                val config = TemplateConfigProvider.getConfig(item.templateId)

                // Define render size
                val width = 1200  // Increased for better quality
                val height = 1600

                // Calculate scale factor based on standard design width (1080px)
                // This ensures text size is consistent across all screen densities
                val designWidth = 1080f
                val scaleFactor = width / designWidth

                // ✅ FIX: Use FIXED density for consistent text size across all devices
                // Bitmap size is fixed (1200x1600), so text size should also be fixed
                val fixedDensity = 3.5f  // Standard density for bitmap rendering

                // Set text sizes in PX (density-independent) instead of SP
                // This ensures the text size is always proportional to the view size
                if (tvName != null) {
                    tvName.text = item.name
                    tvName.setTextSize(TypedValue.COMPLEX_UNIT_PX, config.nameSize * scaleFactor * fixedDensity)

                    val nameFontResId = mapFontNameToResource(item.nameFont)
                    if (nameFontResId != null) {
                        val typeface = androidx.core.content.res.ResourcesCompat.getFont(context, nameFontResId)
                        tvName.typeface = typeface
                    }
                }
                // ✅ THÊM: Map font name to resource (COPY từ MakeScreenActivity)

                if (tvBounty != null) {
                    tvBounty.text = item.getFullBountyText()
                    tvBounty.setTextSize(TypedValue.COMPLEX_UNIT_PX, config.bountySize * scaleFactor * fixedDensity)
                    val bountyFontResId = mapFontNameToResource(item.bountyFont)
                    if (bountyFontResId != null) {
                        val typeface = androidx.core.content.res.ResourcesCompat.getFont(context, bountyFontResId)
                        tvBounty.typeface = typeface
                    }
                }

                // Load images synchronously
                val templateBitmap = withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(item.getTemplatePath())
                        .submit()
                        .get()
                }

                val avatarBitmap = withContext(Dispatchers.IO) {
                    Glide.with(context)
                        .asBitmap()
                        .load(item.getAvatarPath())
                        .submit()
                        .get()
                }

                // Set loaded bitmaps to ImageViews
                imgTemplate.setImageBitmap(templateBitmap)
                imgAvatar.setImageBitmap(avatarBitmap)

                // Measure and layout the view with the same dimensions
                val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

                templateView.measure(widthSpec, heightSpec)
                templateView.layout(0, 0, width, height)

                // Create bitmap and draw the view
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                templateView.draw(canvas)

                bitmap
            }
        }

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
    }
}
