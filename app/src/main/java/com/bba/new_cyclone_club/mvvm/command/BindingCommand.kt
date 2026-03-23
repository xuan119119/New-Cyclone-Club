package com.bba.new_cyclone_club.mvvm.command

/**
 * MVVM Command 命令封装。
 *
 * 用途：将 View 的点击/长按/输入变化等事件绑定到 ViewModel 的业务方法，
 * 避免 View 直接依赖 ViewModel 接口，实现彻底解耦。
 *
 * ### 使用示例
 *
 * ViewModel 中定义：
 * ```kotlin
 * val onLoginClick = BindingCommand {
 *     login()
 * }
 *
 * val onInputChange = BindingCommand<String> { text ->
 *     username.set(text)
 * }
 * ```
 *
 * XML DataBinding 中绑定：
 * ```xml
 * android:onClick="@{() -> vm.onLoginClick.execute()}"
 * android:afterTextChanged="@{(s) -> vm.onInputChange.execute(s.toString())}"
 * ```
 *
 * @param T 命令携带的参数类型，无参命令使用 [BindingCommand] 不带泛型。
 */
class BindingCommand<T>(private val execute: ((T) -> Unit)? = null) {

    /** 携带参数执行命令。 */
    fun execute(parameter: T) {
        execute?.invoke(parameter)
    }
}

/**
 * 无参数 Command——最常用的点击命令。
 *
 * ### 使用示例
 * ```kotlin
 * val onSubmit = BindingCommand { submit() }
 * ```
 * XML:
 * ```xml
 * android:onClick="@{() -> vm.onSubmit.execute()}"
 * ```
 */
@Suppress("FunctionName")
fun BindingCommand(execute: () -> Unit): BindingCommand<Unit> =
    BindingCommand { _: Unit -> execute() }

/**
 * 扩展：无参调用语法糖，避免 execute(Unit)。
 */
fun BindingCommand<Unit>.execute() = execute(Unit)
