package poster.maker.activity_app.intro

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import poster.maker.core.base.BaseActivity
import poster.maker.core.utils.DataLocal
import poster.maker.databinding.ActivityIntroBinding
import poster.maker.activity_app.main.MainActivity
import poster.maker.activity_app.permission.PermissionActivity
import poster.maker.core.extensions.hideNavigation
import poster.maker.core.extensions.setOnSingleClick
//quyen
import com.lvt.ads.util.Admob
import poster.maker.R
import poster.maker.core.extensions.visible
import poster.maker.core.extensions.gone
import androidx.viewpager2.widget.ViewPager2
//quyen
import kotlin.system.exitProcess

class IntroActivity : BaseActivity<ActivityIntroBinding>() {
    private val introAdapter by lazy { IntroAdapter(this) }

    override fun setViewBinding(): ActivityIntroBinding {
        return ActivityIntroBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initVpg()
    }

    override fun onResume() {
        super.onResume()
        hideNavigation(false)
    }

    override fun viewListener() {
        binding.btnNext.setOnSingleClick { handleNext() }
    }

    override fun initText() {}

    override fun initActionBar() {}

    //quyen
    override fun initAds() {
        // Load native ad
//        Admob.getInstance().loadNativeAd(
//            this,
//            getString(R.string.native_intro),
//            binding.nativeIntro,
//            R.layout.ads_native_avg2_btn_bottom
//        )
    }
    //quyen

    private fun initVpg() {
        binding.apply {
            binding.vpgTutorial.adapter = introAdapter
            binding.dotsIndicator.attachTo(binding.vpgTutorial)
            introAdapter.submitList(DataLocal.itemIntroList)

            //quyen
            // Show/hide native ad based on current page (show only on page 0 and 2)
            vpgTutorial.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == 0 || position == 2) {
                        //nativeIntro.visible()
                    } else {
                       // nativeIntro.gone()
                    }
                }
            })
            //quyen
        }
    }

    private fun handleNext() {
        binding.apply {
            val nextItem = binding.vpgTutorial.currentItem + 1
            if (nextItem < DataLocal.itemIntroList.size) {
                vpgTutorial.setCurrentItem(nextItem, true)
            } else {
                val intent =
                    if (sharePreference.getIsFirstPermission()) {
                        Intent(this@IntroActivity, PermissionActivity::class.java)
                    } else {
                        Intent(this@IntroActivity, MainActivity::class.java)
                    }
                startActivity(intent)
                finish()
            }
        }
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() { exitProcess(0) }
}