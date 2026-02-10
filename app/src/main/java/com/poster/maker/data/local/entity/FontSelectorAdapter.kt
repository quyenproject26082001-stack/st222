package poster.maker.data.local.entity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import poster.maker.R
import poster.maker.databinding.ItemFontSelectorBinding

data class FontItem(
    val name: String,
    val fontResId: Int
)

class FontSelectorAdapter(
    private val fonts: List<FontItem>,
    private var selectedPosition: Int = 0,
    private val onFontSelected: (FontItem, Int) -> Unit
) : RecyclerView.Adapter<FontSelectorAdapter.FontViewHolder>() {

    inner class FontViewHolder(private val binding: ItemFontSelectorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(fontItem: FontItem, position: Int) {
            binding.tvFontName.text = fontItem.name

            // Set tick image based on selection
            if (position == selectedPosition) {
                binding.imgFontTick.setImageResource(R.drawable.tick_font)
            } else {
                binding.imgFontTick.setImageResource(R.drawable.not_tick_font)
            }

            // Apply the actual font to the text view
            val typeface = ResourcesCompat.getFont(binding.root.context, fontItem.fontResId)
            binding.tvFontName.typeface = typeface

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onFontSelected(fontItem, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding = ItemFontSelectorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(fonts[position], position)
    }

    override fun getItemCount() = fonts.size

    fun getSelectedPosition() = selectedPosition

    /**
     * Update selected position and notify adapter to refresh UI
     */
    fun setSelectedPosition(newPosition: Int) {
        if (newPosition in 0 until fonts.size && newPosition != selectedPosition) {
            val previousPosition = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
        }
    }
}
