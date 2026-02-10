package poster.maker.dialog

import android.app.Activity
import android.view.Gravity
import android.widget.Toast
import poster.maker.core.extensions.setOnSingleClick
import poster.maker.R
import poster.maker.core.base.BaseDialog
import poster.maker.core.extensions.strings
import poster.maker.databinding.DialogRateBinding

class RateDialog(context: Activity) : BaseDialog<DialogRateBinding>(context, Gravity.CENTER, true, true) {
    override val layoutId: Int = R.layout.dialog_rate
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false
    var i = 0
    var onRateGreater3: (() -> Unit)? = null
    var onRateLess3: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null



    override fun initView() {
    }

    override fun initAction() {
        binding.btnCancel.setOnClickListener { onCancel?.invoke() }

        binding.btnVote.setOnClickListener {
            when (i) {
                0 -> {
                    Toast.makeText(context, context.getText(R.string.rate_us_0), Toast.LENGTH_SHORT).show()
                }

                in 1..3 -> {
                    onRateLess3?.invoke()
                }

                else -> {
                    onRateGreater3?.invoke()
                }
            }
        }

        binding.ll1.setOnRatingChangeListener { ratingBar, rating, fromUser ->
            i = rating.toInt()
            when (i) {
                0 -> {
                    setView(R.string.zero_star_title, R.string.zero_star, R.drawable.ic_rate_zero)
                }

                1 -> {
                    setView(R.string.one_star_title, R.string.one_star, R.drawable.ic_rate_one)
                }

                2 -> {
                    setView(R.string.two_star_title, R.string.two_star, R.drawable.ic_rate_two)
                }

                3 -> {
                    setView(R.string.three_star_title, R.string.three_star, R.drawable.ic_rate_three)
                }

                4 -> {
                    setView(R.string.four_star_title, R.string.four_star, R.drawable.ic_rate_four)
                }

                5 -> {
                    setView(R.string.five_star_title, R.string.five_star, R.drawable.ic_rate_five)
                }
            }
        }
    }

    override fun onDismissListener() {

    }

    fun setView(tv1: Int, tv2: Int, img: Int) {
        binding.tv1.text = context.strings(tv1)
        binding.tv2.text = context.strings(tv2)
        binding.imvAvtRate.setImageResource(img)
    }
}