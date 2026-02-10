package poster.maker.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import poster.maker.data.local.entity.FontItem
import poster.maker.data.local.entity.FontSelectorAdapter
import poster.maker.databinding.BottomSheetFontSelectorBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FontSelectorBottomSheet(
    private val fonts: List<FontItem>,
    private val currentFontName: String,
    private val onFontSelected: (FontItem) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFontSelectorBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FontSelectorAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFontSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set current font name in header
        binding.tvCurrentFont.text = currentFontName

        // Find the currently selected font index
        val selectedIndex = fonts.indexOfFirst { it.name == currentFontName }.takeIf { it >= 0 } ?: 0

        // Setup RecyclerView
        adapter = FontSelectorAdapter(fonts, selectedIndex) { fontItem, _ ->
            binding.tvCurrentFont.text = fontItem.name
            onFontSelected(fontItem)
            dismiss()
        }

        binding.rvFontList.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@FontSelectorBottomSheet.adapter
        }

        // Close button
        binding.imgCollapse.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FontSelectorBottomSheet"
    }
}
