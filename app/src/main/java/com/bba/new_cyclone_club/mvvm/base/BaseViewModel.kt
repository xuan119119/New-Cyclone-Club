package com.bba.new_cyclone_club.mvvm.base

import androidx.lifecycle.ViewModel
import com.bba.new_cyclone_club.mvvm.event.SingleLiveEvent

/**
 * MVVM 框架 ViewModel 基类。
 *
 * 提供：
 *  - [uiState]    单次 UI 事件总线（导航、Toast、Dialog 等）
 *  - [loading]    全局加载状态
 */
open class BaseViewModel : ViewModel() {

    /** 向 UI 层发出一次性事件（不会在旋转后重播）。 */
    val uiState: SingleLiveEvent<UIEvent> = SingleLiveEvent()

    /** 全局加载遮罩开关。 */
    val loading: SingleLiveEvent<Boolean> = SingleLiveEvent()

    // -------------------------------------------------------------------------
    // 便捷方法
    // -------------------------------------------------------------------------

    /** 发出导航事件（路由跳转）。 */
    protected fun navigate(route: String, params: Map<String, Any?> = emptyMap()) {
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
