package poster.maker.activity_app.editsticker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import poster.maker.core.helper.AssetHelper
import poster.maker.databinding.ItemStickerCategoryBinding

class StickerCategoryAdapter(
    private val categories: List<StickerCategory>,
    private val initialSelectedId: Int = 1,
    private val onCategorySelected: (Int) -> Unit
) : RecyclerView.Adapter<StickerCategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = categories.indexOfFirst { it.id == initialSelectedId }.coerceAtLeast(0)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemStickerCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = categories.size

    fun getSelectedCategoryId(): Int = categories[selectedPosition].id

    fun setSelectedPosition(position: Int) {
        if (position in categories.indices && position != selectedPosition) {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    inner class CategoryViewHolder(
        private val binding: ItemStickerCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StickerCategory, isSelected: Boolean) {
            // Load category thumbnail (first sticker image)
            val thumbnailPath = AssetHelper.getStickerCategoryThumbnail(item.id)
            Glide.with(binding.root.context)
                .load(thumbnailPath)
                .centerInside()
                .into(binding.imgCategoryThumb)

            // Update selection state - change background
            if (isSelected) {
                binding.imgCategoryThumb.setBackgroundResource(poster.maker.R.drawable.bg_cateris_selected)
            } else {
                binding.imgCategoryThumb.setBackgroundResource(poster.maker.R.drawable.bg_item_category)
            }

            // Handle click
            binding.root.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = bindingAdapterPosition

                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)

                onCategorySelected(item.id)
            }
        }
    }
}

data class StickerCategory(
    val id: Int,
    val name: String = "Category $id"
)
