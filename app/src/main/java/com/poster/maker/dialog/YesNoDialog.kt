package poster.maker.dialog

import android.app.Activity
import poster.maker.core.extensions.gone
import poster.maker.core.extensions.hideNavigation
import poster.maker.core.extensions.setOnSingleClick
import poster.maker.R
import poster.maker.core.base.BaseDialog
import poster.maker.core.extensions.strings
import poster.maker.databinding.DialogConfirmBinding


class YesNoDialog(
    val context: Activity, val title: Int, val description: Int, val isError: Boolean = false
) : BaseDialog<DialogConfirmBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_confirm
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    var onNoClick: (() -> Unit) = {}
    var onYesClick: (() -> Unit) = {}
    var onDismissClick: (() -> Unit) = {}

    override fun initView() {
        initBottom()
        initText()
        if (isError) {
            binding.flBottom.btnBottomLeft.gone()
        }
        context.hideNavigation()
    }

    override fun initAction() {
        binding.apply {
            flBottom.btnBottomLeft.setOnSingleClick {
                onNoClick.invoke()
            }
            flBottom.btnBottomRight.setOnSingleClick {
                onYesClick.invoke()
            }
            // Removed flOutSide click listener - dialog should only close via Yes/No buttons
        }
    }

    override fun onDismissListener() {

    }

    private fun initText() {
        binding.apply {
            tvTitle.text = context.getString(title)
            tvDescription.text = context.getString(description)
        }
    }

    private fun initBottom() {
        binding.flBottom.apply {
            imvBottomLeft.gone()
            imvBottomRight.gone()

            tvBottomLeft.text = context.strings(R.string.no)
            tvBottomRight.text = context.strings(R.string.yes)
        }
    }
}