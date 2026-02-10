package poster.maker.core.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import poster.maker.core.helper.LanguageHelper

abstract class BaseFragment<T : ViewBinding> : Fragment() {

    protected lateinit var binding: T

    protected abstract fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?): T

    protected abstract fun initView()

    protected abstract fun viewListener()

    open fun dataObservable() {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        LanguageHelper.setLocale(requireContext())
        binding = setViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        viewListener()
        dataObservable()
    }

}
