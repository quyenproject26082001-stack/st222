    package poster.maker.activity_app.permission

    import android.content.pm.PackageManager
    import android.os.Build
    import android.text.TextUtils
    import android.util.Log
    import android.view.LayoutInflater
    import androidx.activity.viewModels
    import androidx.core.graphics.toColorInt
    import androidx.lifecycle.Lifecycle
    import androidx.lifecycle.lifecycleScope
    import androidx.lifecycle.repeatOnLifecycle
    import poster.maker.R
    import poster.maker.core.base.BaseActivity
    import poster.maker.core.extensions.checkPermissions
    import poster.maker.core.extensions.goToSettings
    import poster.maker.core.extensions.gone
    import poster.maker.core.extensions.requestPermission
    import poster.maker.core.extensions.select
    import poster.maker.core.extensions.showToast
    import poster.maker.core.extensions.startIntentRightToLeft
    import poster.maker.core.extensions.visible
    import poster.maker.core.helper.StringHelper
    import poster.maker.core.utils.key.RequestKey
    import poster.maker.databinding.ActivityPermissionBinding
    import poster.maker.activity_app.main.MainActivity
    import poster.maker.core.extensions.setGradientTextHeightColor
    import poster.maker.core.extensions.setOnSingleClick
    //quyen
    import com.google.android.gms.ads.interstitial.InterstitialAd
    import com.lvt.ads.callback.InterCallback
    import com.lvt.ads.util.Admob
    //quyen
    import kotlinx.coroutines.launch
    import kotlin.text.toInt

    class PermissionActivity : BaseActivity<ActivityPermissionBinding>() {

        private val viewModel: PermissionViewModel by viewModels()
        //quyen
        var interPer: InterstitialAd? = null
        //quyen

        override fun setViewBinding() = ActivityPermissionBinding.inflate(LayoutInflater.from(this))

        override fun initView() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                binding.btnStorage.visible()
                binding.btnNotification.gone()
            } else {
                binding.btnNotification.visible()
                binding.btnStorage.gone()
            }
           // Admob.getInstance().setTimeLimitShowAds(0)

        }

        override fun initText() {
            binding.actionBar.tvCenter.select()
            setGradientTextHeightColor(binding.tvContinue, "#C4561B".toColorInt(), "#C4561B".toColorInt())
            val textRes =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) R.string.to_access_13 else R.string.to_access

            binding.txtPer.text = TextUtils.concat(
                createColoredText(R.string.allow, R.color.white),
                " ",
                createColoredText(R.string.app_name, R.color.white),
                " ",
                createColoredText(textRes, R.color.white)
            )
        }

        override fun viewListener() {
            binding.swPermission.setOnSingleClick { handlePermissionRequest(isStorage = true) }
            binding.swNotification.setOnSingleClick { handlePermissionRequest(isStorage = false) }
            binding.tvContinue.setOnSingleClick(1500) { handleContinue() }
        }

        override fun dataObservable() {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.storageGranted.collect { granted ->
                        updatePermissionUI(granted, true)
                    }
                }
            }

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.notificationGranted.collect { granted ->
                        updatePermissionUI(granted, false)
                    }
                }
            }
        }

        private fun handlePermissionRequest(isStorage: Boolean) {
            val perms = if (isStorage) viewModel.getStoragePermissions() else viewModel.getNotificationPermissions()
            if (checkPermissions(perms)) {
                showToast(if (isStorage) R.string.granted_storage else R.string.granted_notification)
            } else if (viewModel.needGoToSettings(sharePreference, isStorage)) {
                goToSettings()
            } else {
                val requestCode = if (isStorage) RequestKey.STORAGE_PERMISSION_CODE else RequestKey.NOTIFICATION_PERMISSION_CODE
                requestPermission(perms, requestCode)
            }
        }

        private fun updatePermissionUI(granted: Boolean, isStorage: Boolean) {
            val imageView = if (isStorage) binding.swPermission else binding.swNotification
            imageView.setImageResource(if (granted) R.drawable.ic_sw_on else R.drawable.ic_sw_off)

            // Thay đổi kích thước trực tiếp (đơn vị dp)
            val params = imageView.layoutParams
            val density = resources.displayMetrics.density
            if (granted) {
                params.width = (52 * density).toInt()  // 60dp
                params.height = (26 * density).toInt() // 30dp
            } else {
                params.width = (51 * density).toInt()  // 50dp
                params.height = (29 * density).toInt() // 25dp
            }
            imageView.layoutParams = params
        }

        override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            when (requestCode) {
                RequestKey.STORAGE_PERMISSION_CODE -> viewModel.updateStorageGranted(sharePreference, granted)

                RequestKey.NOTIFICATION_PERMISSION_CODE -> viewModel.updateNotificationGranted(sharePreference, granted)
            }
            if (granted) {
                showToast(if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) R.string.granted_storage else R.string.granted_notification)
            }
        }

        override fun onStart() {
            super.onStart()
            viewModel.updateStorageGranted(
                sharePreference, checkPermissions(viewModel.getStoragePermissions())
            )
            viewModel.updateNotificationGranted(
                sharePreference, checkPermissions(viewModel.getNotificationPermissions())
            )
        }


        //quyen
//        override fun initAds() {
//            // Load interstitial ad
//            Admob.getInstance().loadInterAds(this, getString(R.string.inter_per), object : InterCallback() {
//                override fun onAdLoadSuccess(interstitialAd: InterstitialAd?) {
//                    super.onAdLoadSuccess(interstitialAd)
//                    interPer = interstitialAd
//                }
//            })
//
//            // Load native ad with button on top
//            Admob.getInstance().loadNativeAd(
//                this,
//                getString(R.string.native_per),
//                binding.nativePer,
//                R.layout.ads_native_big
//            )
//        }
        //quyen

        override fun initActionBar() {
            binding.actionBar.tvCenter.apply {
                text = getString(R.string.permission)
                gone()
            }
        }

        private fun createColoredText(
            @androidx.annotation.StringRes textRes: Int,
            @androidx.annotation.ColorRes colorRes: Int,
            font: Int = R.font.roboto_regular
        ) = StringHelper.changeColor(this, getString(textRes), colorRes, font)

        private fun handleContinue() {
            //quyen
            sharePreference.setIsFirstPermission(false)
            Admob.getInstance().showInterAds(this, interPer, object : InterCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    Log.d("d","bug")
                    startIntentRightToLeft(MainActivity::class.java)
                    finishAffinity()
                }
            })
            //quyen
        }
    }