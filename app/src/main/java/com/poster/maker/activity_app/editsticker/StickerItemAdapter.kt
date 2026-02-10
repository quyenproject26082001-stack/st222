package poster.maker.activity_app.editsticker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import poster.maker.databinding.ItemStickerBinding

class StickerItemAdapter(
    private var stickers: List<String> = emptyList(),
    private val onStickerClick: (String) -> Unit
) : RecyclerView.Adapter<StickerItemAdapter.StickerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val binding = ItemStickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.bind(stickers[position])
    }

    override fun getItemCount(): Int = stickers.size

    fun updateStickers(newStickers: List<String>) {
        stickers = newStickers
        notifyDataSetChanged()
    }

    inner class StickerViewHolder(
        private val binding: ItemStickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stickerPath: String) {
            Glide.with(binding.root.context)
                .load(stickerPath)
                .centerInside()
                .into(binding.imgSticker)

            binding.root.setOnClickListener {
                onStickerClick(stickerPath)
            }
        }
    }
}
