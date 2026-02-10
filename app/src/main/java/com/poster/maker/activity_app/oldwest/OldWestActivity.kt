package poster.maker.activity_app.oldwest

import android.content.Intent
import android.view.LayoutInflater
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import poster.maker.R
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.*

//quyen
import com.lvt.ads.util.Admob
import poster.maker.databinding.ActivityOldWestBinding

//quyen

class OldWestActivity : BaseActivity<ActivityOldWestBinding>() {

    private lateinit var adapter: OldWestAdapter

    override fun setViewBinding(): ActivityOldWestBinding {
        return ActivityOldWestBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Setup StaggeredGridLayoutManager with 2 columns
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.rvDesigns.layoutManager = layoutManager

        loadDesigns()
    }

    override fun onResume() {
        super.onResume()
        // Reload designs when returning from ViewOldWestActivity
        if (::adapter.isInitialized) {
            loadDesigns()
        }
    }

    private fun loadDesigns() {
        try {
            // Load images from assets/oldwest folder
            val assetFiles = assets.list("oldwest")
                ?.filter { it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") }
                ?.sorted()
                ?: emptyList()

            if (assetFiles.isEmpty()) {
                binding.rvDesigns.gone()
                binding.tvEmpty.visible()
            } else {
                binding.rvDesigns.visible()
                binding.tvEmpty.gone()

                if (::adapter.isInitialized) {
                    adapter.updateItems(assetFiles)
                } else {
                    adapter = OldWestAdapter(this, assetFiles) { fileName ->
                       showInterAll {   onDesignClicked (fileName) }
                    }
                    binding.rvDesigns.adapter = adapter
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.rvDesigns.gone()
            binding.tvEmpty.visible()
        }
    }

    override fun viewListener() {
        binding.actionBar.apply {
            btnActionBarLeft.setOnSingleClick {
               showInterAll { finishAfterTransition() }
            }
        }
    }

    override fun dataObservable() {
        // No data observables needed
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            tvCenter.text = getString(R.string.old_west_outlwaw)
            tvCenter.visible()
            btnActionBarRight.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
        }
    }

    //quyen
//    override fun initAds() {
//        // Load native regular ad above back button and list
//        // Load native collapsible ad at bottom
//        Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_cl_Old_West), binding.nativeCollapOldWestActivity)
//    }
    //quyen

    override fun onRestart() {
        super.onRestart()
       // initAds()
    }
    private fun onDesignClicked(fileName: String) {
        val intent = Intent(this, ViewOldWestActivity::class.java).apply {
            putExtra("assetFileName", fileName)
        }
        startActivity(intent)
    }
}
