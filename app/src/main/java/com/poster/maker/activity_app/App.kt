package poster.maker.activity_app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.lvt.ads.util.AdsApplication
import com.lvt.ads.util.AppOpenManager
import poster.maker.R
import poster.maker.activity_app.splash.SplashActivity

class App : AdsApplication() {
    override fun onCreate() {
        super.onCreate()
        // Initialize AdMob SDK manually
        MobileAds.initialize(this) {}
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
    }

    override fun enableAdsResume(): Boolean {
        return false
    }

    override fun getListTestDeviceId(): MutableList<String>? {
        return null
    }

    override fun getResumeAdId(): String {
        return getString(R.string.open_resume)
    }



    override fun buildDebug(): Boolean {
        return true
    }
}

