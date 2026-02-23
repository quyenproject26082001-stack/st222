package poster.maker.activity_app.template

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import poster.maker.core.helper.AssetHelper
import poster.maker.databinding.ItemTemplateBinding

class TemplateAdapter(
    private val templates: List<TemplateItem>,
    private val initialSelectedId: Int = 1,
    private val onItemSelected: (Int) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

    private var selectedPosition = templates.indexOfFirst { it.id == initialSelectedId }.coerceAtLeast(0)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

    override fun onViewRecycled(holder: TemplateViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelJob()
    }

    fun cleanup() {
        coroutineScope.cancel()
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun getSelectedTemplateId(): Int = templates[selectedPosition].id

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

        private var currentJob: Job? = null

        fun bind(item: TemplateItem, isSelected: Boolean) {
            currentJob?.cancel()

            // Show shimmer, hide image initially
            binding.imgShimmer.visibility = View.VISIBLE
            binding.imgTemplate.visibility = View.INVISIBLE

            // Update selection state
            binding.cardTemplate.alpha = if (isSelected) 1.0f else 1f

            // Handle click
            binding.cardTemplate.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = bindingAdapterPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onItemSelected(item.id)
            }

            // Load image async
            currentJob = coroutineScope.launch {
                try {
                    val avatarPath = AssetHelper.getTemplateAvatarPath(item.id)
                    val bitmap = withContext(Dispatchers.IO) {
                        Glide.with(binding.root.context)
                            .asBitmap()
                            .load(avatarPath)
                            .submit()
                            .get()
                    }
                    if (isActive) {
                        binding.imgShimmer.visibility = View.GONE
                        binding.imgTemplate.visibility = View.VISIBLE
                        binding.imgTemplate.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun cancelJob() {
            currentJob?.cancel()
        }
    }
}

data class TemplateItem(
    val id: Int,
    val name: String
)
