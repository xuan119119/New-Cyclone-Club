package com.bba.new_cyclone_club.mvvm.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bba.new_cyclone_club.mvvm.event.EventBus
import com.bba.new_cyclone_club.mvvm.logger.Logger
import com.bba.new_cyclone_club.mvvm.router.Router

/**
 * MVVM 框架 Fragment 基类。
 *
 * 与 [BaseActivity] 用法对称，Fragment 中使用 [viewModel] 和 [binding]。
 *
 * 额外提供：
 *  - Logger 便捷方法：[logD] / [logI] / [logW] / [logE]
 *  - EventBus 订阅：[observeEvent] / [observeStickyEvent]
 *
 * ### 示例
 * ```kotlin
 * class ProfileFragment : BaseFragment<FragmentProfileBinding, ProfileViewModel>() {
 *     override val layoutId   = R.layout.fragment_profile
 *     override val variableId = BR.vm
 *
 *     override fun initView() { /* 控件初始化 */ }
 *     override fun initData() {
 *         viewModel.loadProfile()
 *         observeEvent<RefreshEvent> { viewModel.refresh() }
 *     }
 * }
 * ```
 *
 * @param VDB ViewDataBinding 子类型
 * @param VM  BaseViewModel 子类型
 */
abstract class BaseFragment<VDB : ViewDataBinding, VM : BaseViewModel> : Fragment() {

    abstract val layoutId: Int
    abstract val variableId: Int

    private var _binding: VDB? = null
    val binding: VDB get() = _binding!!

    lateinit var viewModel: VM
        private set

    /** 当前 Fragment 使用的日志 Tag，默认为类名，子类可重写。 */
    open val logTag: String get() = this::class.java.simpleName

    // -------------------------------------------------------------------------
    // 生命周期
    // -------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        _binding!!.lifecycleOwner = viewLifecycleOwner
        return _binding!!.root
    }

    @Suppress("UNCHECKED_CAST")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logD("onViewCreated")

        val vmClass = resolveVMClass<VM>()
        viewModel = ViewModelProvider(this)[vmClass]

        binding.setVariable(variableId, viewModel)

        observeUIEvents()
        initView()
        initData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        logD("onDestroyView")
        _binding = null
    }

    // -------------------------------------------------------------------------
    // 子类钩子
    // -------------------------------------------------------------------------

    open fun initView() = Unit
    open fun initData() = Unit

    // -------------------------------------------------------------------------
    // 公共 UI 事件
    // -------------------------------------------------------------------------

    private fun observeUIEvents() {
        viewModel.uiState.observe(viewLifecycleOwner) { event ->
            when (event) {
                is UIEvent.Navigate   -> handleNavigate(event)
                is UIEvent.Toast      -> Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                is UIEvent.ShowDialog -> showSimpleDialog(event.title, event.message)
                is UIEvent.Back       -> requireActivity().onBackPressedDispatcher.onBackPressed()
                is UIEvent.Custom     -> onCustomEvent(event)
            }
        }
        viewModel.loading.observe(viewLifecycleOwner) { show ->
            onLoadingChanged(show)
        }
    }

    open fun handleNavigate(event: UIEvent.Navigate) {
        Router.navigateTo(requireContext(), event.route, event.params)
    }

    open fun onCustomEvent(event: UIEvent.Custom) = Unit
    open fun onLoadingChanged(show: Boolean) = Unit

    // -------------------------------------------------------------------------
    // EventBus 便捷方法
    // -------------------------------------------------------------------------

    /**
     * 订阅全局普通事件，自动跟随 Fragment viewLifecycleOwner，无需手动取消。
     *
     * ```kotlin
     * observeEvent<RefreshEvent> { viewModel.refresh() }
     * ```
     */
    protected inline fun <reified T : Any> observeEvent(noinline observer: (T) -> Unit) {
        EventBus.observe<T>(viewLifecycleOwner, observer)
    }

    /**
     * 订阅全局粘性事件，自动跟随 Fragment viewLifecycleOwner。
     *
     * ```kotlin
     * observeStickyEvent<NetworkStatusEvent> { updateBanner(it.online) }
     * ```
     */
    protected inline fun <reified T : Any> observeStickyEvent(noinline observer: (T) -> Unit) {
        EventBus.observeSticky<T>(viewLifecycleOwner, observer)
    }

    // -------------------------------------------------------------------------
    // Logger 便捷方法
    // -------------------------------------------------------------------------

    protected fun logV(msg: String) = Logger.v(msg, logTag)
    protected fun logD(msg: String) = Logger.d(msg, logTag)
    protected fun logI(msg: String) = Logger.i(msg, logTag)
    protected fun logW(msg: String, throwable: Throwable? = null) = Logger.w(msg, logTag, throwable)
    protected fun logE(msg: String, throwable: Throwable? = null) = Logger.e(msg, logTag, throwable)

    // -------------------------------------------------------------------------
    // 工具
    // -------------------------------------------------------------------------

    private fun showSimpleDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <VM : BaseViewModel> resolveVMClass(): Class<VM> {
        var cls: Class<*> = javaClass
        while (cls.genericSuperclass !is java.lang.reflect.ParameterizedType) {
            cls = cls.superclass
        }
        val type = cls.genericSuperclass as java.lang.reflect.ParameterizedType
        return type.actualTypeArguments[1] as Class<VM>
    }
}
