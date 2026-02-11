package poster.maker.activity_app.mycreation

import android.content.Intent
import android.view.LayoutInflater
import poster.maker.R
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.*
import poster.maker.databinding.ActivityMyCreationBinding
import java.io.File
//quyen
import com.lvt.ads.util.Admob
import poster.maker.activity_app.main.MainActivity

//quyen

class MyCreationActivity : BaseActivity<ActivityMyCreationBinding>() {

    private lateinit var adapter: MyCreationAdapter
    private var currentTab = TabType.MY_DESIGN // Default to My Design

    enum class TabType {
        MY_DESIGN,  // Bounty photos
        MY_WANTED   // Wanted posters
    }

    override fun setViewBinding(): ActivityMyCreationBinding {
        return ActivityMyCreationBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        setupTabs()
        loadDesigns()
    }

    override fun onResume() {
        super.onResume()
        // Reload designs when returning from ViewDesignActivity (in case of deletion)
        if (::adapter.isInitialized) {
            loadDesigns()
        }
    }

    private fun setupTabs() {
        // Set initial tab selection
        updateTabSelection(TabType.MY_DESIGN)

        binding.apply {
            tabMyDesign.setOnSingleClick {
                if (currentTab != TabType.MY_DESIGN) {
                    currentTab = TabType.MY_DESIGN
                    updateTabSelection(TabType.MY_DESIGN)
                    loadDesigns()
                }
            }

            tabMyWanted.setOnSingleClick {
                if (currentTab != TabType.MY_WANTED) {
                    currentTab = TabType.MY_WANTED
                    updateTabSelection(TabType.MY_WANTED)
                    loadDesigns()
                }
            }
        }
    }

    private fun updateTabSelection(selectedTab: TabType) {
        binding.apply {
            when (selectedTab) {
                TabType.MY_DESIGN -> {
                    tabMyDesign.isSelected = true
                    tabMyWanted.isSelected = false
                }

                TabType.MY_WANTED -> {
                    tabMyDesign.isSelected = false
                    tabMyWanted.isSelected = true
                }
            }
        }
    }

    private fun loadDesigns() {
        // Select folder based on current tab
        val folderName = when (currentTab) {
            TabType.MY_DESIGN -> "bounty_designs"  // Bounty photos
            TabType.MY_WANTED -> "posters"          // Wanted posters
        }

        val postersDir = File(filesDir, folderName)
        val designs = if (postersDir.exists()) {
            postersDir.listFiles()
                ?.filter { it.isFile && (it.extension == "png" || it.extension == "jpg" || it.extension == "jpeg") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else {
            emptyList()
        }

        if (designs.isEmpty()) {
            binding.rvDesigns.gone()
            binding.tvEmpty.visible()
        } else {
            binding.rvDesigns.visible()
            binding.tvEmpty.gone()

            val isMyWanted = currentTab == TabType.MY_WANTED

            if (::adapter.isInitialized) {
                adapter.updateItems(designs, isMyWanted)
            } else {
                adapter = MyCreationAdapter(designs, isMyWanted) { file ->
                    onDesignClicked(file)
                }
                binding.rvDesigns.adapter = adapter
            }
        }
    }

    override fun viewListener() {
        binding.actionBar.apply {
            btnActionBarLeft.setOnSingleClick {
                goToHome()
            }
        }
    }

    private fun goToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    override fun dataObservable() {
        // No data observables needed
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
            tvCenter.text = strings(R.string.my_design)
            tvCenter.gone()
            btnActionBarRight.gone()
            btnActionBarRightText.gone()
            btnActionBarReset.gone()
        }
    }

    override fun onRestart() {
        super.onRestart()
        //  Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_collap_myDesgin), binding.nativeCollapMyDesgin)

    }
    //quyen
//    override fun initAds() {
//        // Load native regular ad above back button and list
//        Admob.getInstance().loadNativeAd(this, getString(R.string.native_myDesgin), binding.nativeMyDesgin, R.layout.ads_native_collap_banner_1)
//
//        // Load native collapsible ad at bottom
//        Admob.getInstance().loadNativeCollapNotBanner(this, getString(R.string.native_collap_myDesgin), binding.nativeCollapMyDesgin)
//    }
    //quyen

    private fun onDesignClicked(file: File) {
        val intent = Intent(this, ViewCreationActivity::class.java).apply {
            putExtra("imagePath", file.absolutePath)

            putExtra("isMyDesign", currentTab == TabType.MY_DESIGN)
        }
        startActivity(intent)
    }
}
