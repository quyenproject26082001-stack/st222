package poster.maker.activity_app.mycreation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import poster.maker.databinding.ItemMyDesignBinding
import java.io.File

class MyCreationAdapter(
    private var items: List<File>,
    private var isMyWantedTab: Boolean = false,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<MyCreationAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemMyDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File, isMyWanted: Boolean) {
            Glide.with(binding.root.context)
                .load(file)
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .into(binding.imgDesign)

            // Set margins based on tab type
            val layoutParams = binding.rootContainer.layoutParams as ViewGroup.MarginLayoutParams
            if (isMyWanted) {
                // MY_WANTED tab: set margins to 0
                layoutParams.setMargins(0, 0, 0, 0)
            } else {
                // MY_DESIGN tab: use default margins
                val horizontalMargin = binding.root.context.resources.displayMetrics.density * 4
                val bottomMargin = binding.root.context.resources.displayMetrics.density * 8
                layoutParams.setMargins(
                    horizontalMargin.toInt(),
                    0,
                    horizontalMargin.toInt(),
                    bottomMargin.toInt()
                )
            }
            binding.rootContainer.layoutParams = layoutParams

            binding.root.setOnClickListener {
                onItemClick(file)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyDesignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], isMyWantedTab)
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<File>, isMyWanted: Boolean = false) {
        items = newItems
        isMyWantedTab = isMyWanted
        notifyDataSetChanged()
    }
}
