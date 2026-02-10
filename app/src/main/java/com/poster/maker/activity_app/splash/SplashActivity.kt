package poster.maker.activity_app.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import poster.maker.core.base.BaseActivity
import poster.maker.core.extensions.initNetworkMonitor
import poster.maker.databinding.ActivitySplashBinding
import poster.maker.activity_app.intro.IntroActivity
import poster.maker.activity_app.language.LanguageActivity
//quyen
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
//quyen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    var intentActivity: Intent? = null
    //quyen
    private var check = false
    var interCallBack: InterCallback? = null
    //quyen

    override fun setViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        if (!isTaskRoot &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
            intent.action != null &&
            intent.action.equals(Intent.ACTION_MAIN)) {
            finish(); return
        }

        intentActivity = if (sharePreference.getIsFirstLang()) {
            Intent(this, LanguageActivity::class.java)
        } else {
            Intent(this, IntroActivity::class.java)
        }

        // Start rotation animation for loading icon
        val rotateAnimation = android.view.animation.AnimationUtils.loadAnimation(this, poster.maker.R.anim.rotate_loading)
        binding.imvLoading.startAnimation(rotateAnimation)

        initNetworkMonitor()
        Admob.getInstance().setOpenShowAllAds(false)
        Admob.getInstance().setTimeLimitShowAds(20000)
        Admob.getInstance().setTimeCountdownNativeCollab(15000)

         //Simple delay then navigate
        navigateAfterDelay()
        interCallBack = object : InterCallback() {
            override fun onNextAction() {
                super.onNextAction()
                startActivity(intentActivity)
                finishAffinity()
            }
        }
    }

    private fun navigateAfterDelay() {
        lifecycleScope.launch {
            // Delay 2 seconds for splash screen
            delay(2500)
            //quyen
            moveNextScreen()
            //quyen
        }
    }

    //quyen
    private fun moveNextScreen() {
        val nextIntent = if (sharePreference.getIsFirstLang()) {
            Intent(this, LanguageActivity::class.java)
        } else {
            Intent(this, IntroActivity::class.java)
        }

        interCallBack = object : InterCallback() {
            override fun onNextAction() {
                super.onNextAction()
                startActivity(nextIntent)
                check = true
                finishAffinity()
            }
        }

//        Admob.getInstance().loadSplashInterAds(
//            this,
//            getString(poster.maker.R.string.inter_splash),
//            30000,
//            3000,
//            interCallBack
//        )

        // Since ads are disabled, call the callback directly
        interCallBack?.onNextAction()
    }
    //quyen

    override fun dataObservable() {

        var hasNavigated = false

        lifecycleScope.launch {
            // Delay 3 giây hiển thị splash screen
            kotlinx.coroutines.delay(3000)

            if (!hasNavigated && !isFinishing) {
                hasNavigated = true
                moveNextScreen()
            }
        }
        // No data observation needed for Wanted Poster Maker
    }

    override fun viewListener() {
    }

    override fun initText() {}

    override fun initActionBar() {}

    @SuppressLint("GestureBackNavigation", "MissingSuperCall")
    override fun onBackPressed() {
        //quyen
        if (check) {
            // super.onBackPressed()
        } else {
            check = false
        }
        //quyen
    }

    //quyen
    override fun onResume() {
        super.onResume()
     //   Admob.getInstance().onCheckShowSplashWhenFail(this, interCallBack, 1000)
    }
    //quyen
}