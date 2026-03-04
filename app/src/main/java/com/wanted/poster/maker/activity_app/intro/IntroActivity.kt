package com.wanted.poster.maker.activity_app.intro

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import com.wanted.poster.maker.core.base.BaseActivity
import com.wanted.poster.maker.core.utils.DataLocal
import com.wanted.poster.maker.databinding.ActivityIntroBinding
import com.wanted.poster.maker.activity_app.main.MainActivity
import com.wanted.poster.maker.activity_app.permission.PermissionActivity
import com.wanted.poster.maker.core.extensions.hideNavigation
import com.wanted.poster.maker.core.extensions.setOnSingleClick
//quyen
import com.lvt.ads.util.Admob
import com.wanted.poster.maker.R
import com.wanted.poster.maker.core.extensions.visible
import com.wanted.poster.maker.core.extensions.gone
import androidx.viewpager2.widget.ViewPager2
//quyen
import kotlin.system.exitProcess
import kotlin.text.toInt

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
                    val params = binding.btnNext.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

                    if (position == 0) {
                        binding.nativeIntro.gone()
                        // Set lại width về 100dp
                        params.width = ViewGroup.LayoutParams.MATCH_PARENT
                        params.height = (48 * resources.displayMetrics.density).toInt()
                        params.bottomMargin = (5 * resources.displayMetrics.density).toInt()
                        params.marginEnd = (5 * resources.displayMetrics.density).toInt()

                        // Chiều cao
                        binding.btnNext.layoutParams = params
                        binding.btnNext.setTextColor(resources.getColor(R.color.white, null))
                        binding.btnNext.setBackgroundResource(R.drawable.bg_next_button)
                    } else {
                        binding.nativeIntro.visible()
                        // Đổi sang wrap_content
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        params.marginEnd = (16 * resources.displayMetrics.density).toInt()
                        params.bottomMargin = (0 * resources.displayMetrics.density).toInt()
                        binding.btnNext.layoutParams = params
                        binding.btnNext.setTextColor(Color.parseColor("#6DDDD4"))
                        binding.btnNext.background = null
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