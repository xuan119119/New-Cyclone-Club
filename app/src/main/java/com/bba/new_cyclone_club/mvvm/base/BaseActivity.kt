package com.bba.new_cyclone_club.mvvm.base

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.bba.new_cyclone_club.mvvm.event.EventBus
import com.bba.new_cyclone_club.mvvm.logger.Logger
import com.bba.new_cyclone_club.mvvm.router.Router

/**
 * MVVM 框架 Activity 基类。
 *
 * 子类只需声明泛型参数并实现 [layoutId] / [variableId]，无需手动创建
 * ViewDataBinding 或 ViewModel，框架自动完成绑定。
 *
 * 额外提供：
 *  - Logger 便捷方法：[logD] / [logI] / [logW] / [logE]
 *  - EventBus 订阅：[observeEvent] / [observeStickyEvent]
 *
 * ### 示例
 * ```kotlin
 * class LoginActivity : BaseActivity<ActivityLoginBinding, LoginViewModel>() {
 *     override val layoutId   = R.layout.activity_login
 *     override val variableId = BR.vm
 *
 *     override fun initView() { /* 控件初始化 */ }
 *     override fun initData() {
 *         viewModel.loadUser()
 *         observeEvent<LogoutEvent> { finish() }
 *     }
 * }
 * ```
 *
 * @param VDB ViewDataBinding 子类型
 * @param VM  BaseViewModel 子类型
 */
abstract class BaseActivity<VDB : ViewDataBinding, VM : BaseViewModel> : AppCompatActivity() {

    /** layout 资源 id，例如 R.layout.activity_main */
    abstract val layoutId: Int

    /**
     * DataBinding 变量 id，对应 layout 中 <variable> 的名称，
     * 例如 BR.vm（layout 中写 name="vm"）。
     */
    abstract val variableId: Int

    lateinit var binding: VDB
        private set

    lateinit var viewModel: VM
        private set

    /** 当前 Activity 使用的日志 Tag，默认为类名，子类可重写。 */
    open val logTag: String get() = this::class.java.simpleName

    // -------------------------------------------------------------------------
    // 生命周期
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logD("onCreate")

        // 1. DataBinding inflate
        binding = DataBindingUtil.setContentView(this, layoutId)
        binding.lifecycleOwner = this

        // 2. 通过反射获取泛型 VM 类型并创建 ViewModel
        val vmClass = resolveVMClass<VM>()
        viewModel = ViewModelProvider(this)[vmClass]

        // 3. 将 ViewModel 注入 DataBinding
        binding.setVariable(variableId, viewModel)

        // 4. 订阅公共 UI 事件
        observeUIEvents()

        // 5. 子类钩子
        initView()
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        logD("onDestroy")
    }

    // -------------------------------------------------------------------------
    // 子类钩子（可选重写）
    // -------------------------------------------------------------------------

    open fun initView() = Unit
    open fun initData() = Unit

    // -------------------------------------------------------------------------
    // 公共 UI 事件
    // -------------------------------------------------------------------------

    private fun observeUIEvents() {
        viewModel.uiState.observe(this) { event ->
            when (event) {
                is UIEvent.Navigate   -> handleNavigate(event)
                is UIEvent.Toast      -> Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
                is UIEvent.ShowDialog -> showSimpleDialog(event.title, event.message)
                is UIEvent.Back       -> onBackPressedDispatcher.onBackPressed()
                is UIEvent.Custom     -> onCustomEvent(event)
            }
        }
        viewModel.loading.observe(this) { show ->
            onLoadingChanged(show)
        }
    }

    /** 路由导航，子类可重写以添加自定义过渡动画。 */
    open fun handleNavigate(event: UIEvent.Navigate) {
        Router.navigateTo(this, event.route, event.params)
    }

    /** 自定义 UIEvent.Custom 处理，子类按需重写。 */
    open fun onCustomEvent(event: UIEvent.Custom) = Unit

    /** Loading 状态变化，子类按需重写（默认无 UI）。 */
    open fun onLoadingChanged(show: Boolean) = Unit

    // -------------------------------------------------------------------------
    // EventBus 便捷方法
    // -------------------------------------------------------------------------

    /**
     * 订阅全局普通事件，自动跟随 Activity 生命周期，无需手动取消。
     *
     * ```kotlin
     * observeEvent<LogoutEvent> { navigateToLogin() }
     * ```
     */
    protected inline fun <reified T : Any> observeEvent(noinline observer: (T) -> Unit) {
        EventBus.observe<T>(this, observer)
    }

    /**
     * 订阅全局粘性事件，自动跟随 Activity 生命周期。
     * 若粘性频道已有值，订阅后立即回调一次。
     *
     * ```kotlin
     * observeStickyEvent<NetworkStatusEvent> { updateBanner(it.online) }
     * ```
     */
    protected inline fun <reified T : Any> observeStickyEvent(noinline observer: (T) -> Unit) {
        EventBus.observeSticky<T>(this, observer)
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
        AlertDialog.Builder(this)
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
