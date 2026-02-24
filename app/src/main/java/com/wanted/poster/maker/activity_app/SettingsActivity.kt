package com.wanted.poster.maker.activity_app

import android.view.LayoutInflater
import com.wanted.poster.maker.R
import com.wanted.poster.maker.core.base.BaseActivity
import com.wanted.poster.maker.core.extensions.gone
import com.wanted.poster.maker.core.extensions.handleBackLeftToRight
import com.wanted.poster.maker.core.extensions.policy
import com.wanted.poster.maker.core.extensions.rateApp
import com.wanted.poster.maker.core.extensions.select
import com.wanted.poster.maker.core.extensions.shareApp
import com.wanted.poster.maker.core.extensions.startIntentRightToLeft
import com.wanted.poster.maker.core.extensions.visible
import com.wanted.poster.maker.core.utils.key.IntentKey
import com.wanted.poster.maker.core.utils.state.RateState
import com.wanted.poster.maker.databinding.ActivitySettingsBinding
import com.wanted.poster.maker.activity_app.language.LanguageActivity
import com.wanted.poster.maker.core.extensions.setOnSingleClick
import com.wanted.poster.maker.core.extensions.strings
import kotlin.jvm.java

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {
    override fun setViewBinding(): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        initRate()
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.setOnSingleClick { handleBackLeftToRight() }
            btnLang.setOnSingleClick { startIntentRightToLeft(LanguageActivity::class.java, IntentKey.INTENT_KEY) }
            btnShare.setOnSingleClick(1500) { shareApp() }
            btnRate.setOnSingleClick {
                rateApp(sharePreference) { state ->
                    if (state != RateState.CANCEL) {
                        binding.btnRate.gone()
                    }
                }
            }
            btnPolicy.setOnSingleClick(1500) { policy() }
        }
    }

    override fun initText() {
        binding.actionBar.tvCenter.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            tvCenter.text = strings(R.string.settings)
            tvCenter.visible()

            btnActionBarLeft.setImageResource(R.drawable.ic_back)
            btnActionBarLeft.visible()
        }
    }

    private fun initRate() {
        if (sharePreference.getIsRate(this)) {
            binding.btnRate.gone()
        } else {
            binding.btnRate.visible()
        }
    }
}