package poster.maker.activity_app.oldwest

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import poster.maker.databinding.ItemOldwestBinding

class OldWestAdapter(
    private val context: Context,
    private var items: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<OldWestAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemOldwestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(assetFileName: String) {
            try {
                // Load image from assets folder
                val inputStream = context.assets.open("oldwest/$assetFileName")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.imgOldWest.setImageBitmap(bitmap)
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            binding.root.setOnClickListener {
                onItemClick(assetFileName)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOldwestBinding.inflate(
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

    fun updateItems(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}
