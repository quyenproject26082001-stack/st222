package poster.maker.activity_app.template

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import poster.maker.core.helper.AssetHelper
import poster.maker.databinding.ItemTemplateBinding

class TemplateAdapter(
    private val templates: List<TemplateItem>,
    private val initialSelectedId: Int = 1,
    private val onItemSelected: (Int) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

    private var selectedPosition = templates.indexOfFirst { it.id == initialSelectedId }.coerceAtLeast(0)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(templates[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = templates.size

    fun getSelectedPosition(): Int = selectedPosition

    fun getSelectedTemplateId(): Int = templates[selectedPosition].id

    /**
     * Set selected position from outside (e.g., when scroll stops)
     */
    fun setSelectedPosition(position: Int) {
        if (position in templates.indices && position != selectedPosition) {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    inner class TemplateViewHolder(
        private val binding: ItemTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TemplateItem, isSelected: Boolean) {
            // Load template avatar from assets
            val avatarPath = AssetHelper.getTemplateAvatarPath(item.id)
            Glide.with(binding.root.context)
                .load(avatarPath)
                .centerInside()
                .into(binding.imgTemplate)

            // Update selection state
            binding.cardTemplate.alpha = if (isSelected) 1.0f else 0.7f
            binding.cardTemplate.elevation = if (isSelected) 12f else 8f

            // Handle click
            binding.root.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = bindingAdapterPosition

                // Notify changes for visual feedback
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)

                // Callback to activity
                onItemSelected(item.id)
            }
        }
    }
}

data class TemplateItem(
    val id: Int,
    val name: String
)

