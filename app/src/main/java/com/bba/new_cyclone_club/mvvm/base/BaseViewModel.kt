package com.bba.new_cyclone_club.mvvm.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.Observer
import com.bba.new_cyclone_club.mvvm.event.EventBus
import com.bba.new_cyclone_club.mvvm.event.SingleLiveEvent
import com.bba.new_cyclone_club.mvvm.logger.Logger

/**
 * MVVM 框架 ViewModel 基类。
 *
 * 提供：
 *  - [uiState]    单次 UI 事件总线（导航、Toast、Dialog 等）
 *  - [loading]    全局加载状态
 *  - Logger       统一日志输出（[logD] / [logI] / [logW] / [logE]）
 *  - EventBus     全局事件总线发送 & 永久订阅（[postEvent] / [postStickyEvent] / [onEvent]）
 */
open class BaseViewModel : ViewModel() {

    /** 向 UI 层发出一次性事件（不会在旋转后重播）。 */
    val uiState: SingleLiveEvent<UIEvent> = SingleLiveEvent()

    /** 全局加载遮罩开关。 */
    val loading: SingleLiveEvent<Boolean> = SingleLiveEvent()

    /** 当前 ViewModel 使用的日志 Tag，默认为类名，子类可重写。 */
    open val logTag: String get() = this::class.java.simpleName

    // 记录通过 onEvent 注册的永久观察者，用于在 onCleared 时自动取消
    private val foreverObservers = mutableListOf<Pair<Class<*>, Observer<*>>>()

    // -------------------------------------------------------------------------
    // UI 事件便捷方法
    // -------------------------------------------------------------------------

    /** 发出导航事件（路由跳转）。 */
    protected fun navigate(route: String, params: Map<String, Any?> = emptyMap()) {
        logD("navigate -> route=$route, params=$params")
        uiState.value = UIEvent.Navigate(route, params)
    }

    /** 发出 Toast 消息。 */
    protected fun toast(message: String) {
        uiState.value = UIEvent.Toast(message)
    }

    /** 显示/隐藏加载框。 */
    protected fun showLoading(show: Boolean = true) {
        loading.value = show
    }

    /** 发出弹窗事件。 */
    protected fun showDialog(title: String, message: String) {
        uiState.value = UIEvent.ShowDialog(title, message)
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
    // EventBus 便捷方法
    // -------------------------------------------------------------------------

    /**
     * 发送普通事件到全局事件总线。
     */
    protected fun <T : Any> postEvent(event: T) {
        EventBus.post(event)
    }

    /**
     * 发送粘性事件到全局事件总线。
     */
    protected fun <T : Any> postStickyEvent(event: T) {
        EventBus.postSticky(event)
    }

    /**
     * 在 ViewModel 中永久订阅全局事件总线事件。
     * 无需手动取消，[onCleared] 时框架自动移除。
     *
     * ```kotlin
     * init {
     *     onEvent<LogoutEvent> { clearUserData() }
     * }
     * ```
     */
    protected inline fun <reified T : Any> onEvent(noinline observer: (T) -> Unit) {
        val liveObserver = EventBus.observeForever(T::class.java, observer)
        foreverObservers.add(Pair(T::class.java, liveObserver))
    }

    // -------------------------------------------------------------------------
    // 生命周期
    // -------------------------------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        // 自动移除所有通过 onEvent 注册的永久观察者
        foreverObservers.forEach { (clazz, observer) ->
            @Suppress("UNCHECKED_CAST")
            EventBus.removeObserver(clazz as Class<Any>, observer as Observer<Any>)
        }
        foreverObservers.clear()
        logD("onCleared")
    }
}

/**
 * 封装从 ViewModel 发往 View 层的所有一次性 UI 事件。
 */
sealed class UIEvent {
    data class Navigate(val route: String, val params: Map<String, Any?> = emptyMap()) : UIEvent()
    data class Toast(val message: String) : UIEvent()
    data class ShowDialog(val title: String, val message: String) : UIEvent()
    object Back : UIEvent()
    data class Custom(val action: String, val data: Any? = null) : UIEvent()
}
