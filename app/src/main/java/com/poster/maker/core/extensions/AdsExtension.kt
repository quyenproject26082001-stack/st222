package poster.maker.core.extensions

import android.app.Activity
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob

fun Activity.showInterAll(onFinishInter: () -> Unit) {
    Admob.getInstance().showInterAll(this, object : InterCallback() {
        override fun onNextAction() {
            super.onNextAction()
            onFinishInter.invoke()
        }
    })
}