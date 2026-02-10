package poster.maker.activity_app

import android.view.LayoutInflater
import poster.maker.R
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.gone
import poster.maker.core.extensions.handleBackLeftToRight
import poster.maker.core.extensions.policy
import poster.maker.core.extensions.rateApp
import poster.maker.core.extensions.select
import poster.maker.core.extensions.shareApp
import poster.maker.core.extensions.startIntentRightToLeft
import poster.maker.core.extensions.visible
import poster.maker.core.utils.key.IntentKey
import poster.maker.core.utils.state.RateState
import poster.maker.databinding.ActivitySettingsBinding
import poster.maker.activity_app.language.LanguageActivity
import poster.maker.core.extensions.setOnSingleClick
import poster.maker.core.extensions.strings
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