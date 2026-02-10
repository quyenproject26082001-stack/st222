package poster.maker.dialog

import android.app.Activity
import poster.maker.R
import poster.maker.core.base.BaseDialog
import poster.maker.core.extensions.setBackgroundConnerSmooth
import poster.maker.databinding.DialogLoadingBinding

class WaitingDialog(val context: Activity) :
    BaseDialog<DialogLoadingBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_loading
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    override fun initView() {
    }

    override fun initAction() {}

    override fun onDismissListener() {}

}