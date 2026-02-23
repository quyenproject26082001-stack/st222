package poster.maker.activity_app.template

import android.content.Intent
import android.view.LayoutInflater
import androidx.recyclerview.widget.GridLayoutManager
import poster.maker.R
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.gone
import poster.maker.core.extensions.setOnSingleClick
import poster.maker.core.extensions.visible
import poster.maker.databinding.ActivityTemplateListBinding
//quyen
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import poster.maker.core.extensions.showInterAll

//quyen

class TemplateListActivity : BaseActivity<ActivityTemplateListBinding>() {

    private lateinit var adapter: TemplateAdapter
    private var selectedTemplateId = 1
    //quyen
    var interAll: InterstitialAd? = null
    //quyen

    override fun setViewBinding(): ActivityTemplateListBinding {
        return ActivityTemplateListBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        // Get current template ID from intent
        selectedTemplateId = intent.getIntExtra("currentTemplateId", 1)

        setupRecyclerView()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            // Left button - Back (Home icon)
            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()

            // Center text - Title

            // Right button - Done (Check icon)
            btnActionBarRight.setImageResource(R.drawable.ic_select)
            btnActionBarRight.visible()

            // Hide others
            btnActionBarRightText.gone()
            tvRightText.gone()
        }
    }

    override fun viewListener() {
        binding.actionBar.apply {
            // Back button
            btnActionBarLeft.setOnSingleClick {
                finish()
            }

            // Done button
            btnActionBarRight.setOnSingleClick {
                //quyen
              showInterAll {
                  // Return selected template ID to caller
                  val resultIntent = Intent().apply {
                      putExtra("selectedTemplateId", selectedTemplateId)
                  }
                  setResult(RESULT_OK, resultIntent)
                  finish()
              }
                //quyen
            }
        }
    }

    //quyen
//    override fun initAds() {
//
//
//        // Load native ad with button on top
//        Admob.getInstance().loadNativeAd(
//            this,
//            getString(R.string.native_template),
//            binding.nativeTemplate,
//            R.layout.ads_native_big_btn_top
//        )
//    }
    //quyen

    private fun setupRecyclerView() {
        // Create 16 templates (matching assets/template/1 to assets/template/16)
        val templates = (1..16).map { id ->
            TemplateItem(id, "Template $id")
        }

        adapter = TemplateAdapter(templates, selectedTemplateId) { templateId ->
            selectedTemplateId = templateId
        }

        binding.rvTemplates.adapter = adapter
    }
}

