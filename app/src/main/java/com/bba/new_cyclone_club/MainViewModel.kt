package com.bba.new_cyclone_club

import com.bba.new_cyclone_club.mvvm.base.BaseViewModel
import com.bba.new_cyclone_club.mvvm.command.BindingCommand
import com.bba.new_cyclone_club.mvvm.command.execute
import com.bba.new_cyclone_club.mvvm.observable.ObservableField

/**
 * 演示 Demo：MainActivity 对应的 ViewModel。
 *
 * 展示 MVVM 框架的全部核心特性：
 *  1. ObservableField   — 响应式数据双向绑定
 *  2. BindingCommand    — 无参命令（点击）
 *  3. BindingCommand<T> — 带参命令（输入框文字变化）
 *  4. SingleLiveEvent   — Toast / Dialog / 路由（通过 BaseViewModel 辅助方法）
 *  5. DI               — 通过 inject() 委托获取依赖（示例为 GreetingService）
 *  6. Router           — 通过 navigate() 发出路由事件
 */
class MainViewModel : BaseViewModel() {

    // -------------------------------------------------------------------------
    // 1. 响应式数据
    // -------------------------------------------------------------------------

    /** 用户在输入框中实时输入的文字（双向绑定）。 */
    val inputText = ObservableField("")

    /** 展示在屏幕中央的计数器值。 */
    val counter = ObservableField(0)

    // -------------------------------------------------------------------------
    // 2. 无参命令 — 点击按钮
    // -------------------------------------------------------------------------

    /** 点击「+1」按钮 */
    val onIncrement = BindingCommand {
        counter.set(counter.get() + 1)
    }

    /** 点击「重置」按钮 */
    val onReset = BindingCommand {
        counter.set(0)
        inputText.set("")
    }

    /** 点击「Toast」按钮 — 演示 SingleLiveEvent 消息 */
    val onShowToast = BindingCommand {
        val text = inputText.get().ifBlank { "Hello, MVVM!" }
        toast("当前输入：$text  |  计数：${counter.get()}")
    }

    /** 点击「Dialog」按钮 */
    val onShowDialog = BindingCommand {
        showDialog("框架信息", "这是一个基于 DataBinding + LiveData 实现的 MVVM 框架Demo。\n" +
            "计数器当前值：${counter.get()}")
    }

    /** 长按「+1」按钮 — 演示长按命令 */
    val onLongPress = BindingCommand {
        counter.set(counter.get() + 10)
        toast("+10（长按触发）")
    }

    // -------------------------------------------------------------------------
    // 3. 带参命令 — 输入框文字变化
    // -------------------------------------------------------------------------

    /** EditText 文字变化时触发，将输入内容同步到 inputText */
    val onTextChanged = BindingCommand<String> { text ->
        inputText.set(text)
    }

    // -------------------------------------------------------------------------
    // 4. 路由跳转演示（目标页面未创建时会抛出异常，实际使用时先注册路由）
    // -------------------------------------------------------------------------

    /** 点击「路由跳转」按钮 */
    val onNavigate = BindingCommand {
        // 实际项目中先在 Application.onCreate() 注册：
        // Router.register("detail") { DetailActivity::class.java }
        // 此处仅演示调用方式，未注册时会 toast 错误信息
        try {
            navigate("detail", mapOf("counter" to counter.get()))
        } catch (e: IllegalStateException) {
            toast("路由未注册（演示）：${e.message}")
        }
    }
}
