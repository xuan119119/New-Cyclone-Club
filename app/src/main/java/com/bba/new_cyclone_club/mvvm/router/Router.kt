package com.bba.new_cyclone_club.mvvm.router

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

/**
 * 轻量级路由系统。
 *
 * ### 注册路由（在 Application 或模块初始化时）
 * ```kotlin
 * Router.register("login")  { LoginActivity::class.java }
 * Router.register("home")   { HomeActivity::class.java }
 * Router.register("profile") { ProfileFragment::class.java }
 * ```
 *
 * ### 跳转 Activity
 * ```kotlin
 * Router.navigateTo(context, "home")
 * Router.navigateTo(context, "profile", mapOf("userId" to 42))
 * ```
 *
 * ### 跳转 Fragment
 * ```kotlin
 * Router.showFragment(fragmentManager, R.id.container, "profile",
 *                     mapOf("userId" to 42), addToBackStack = true)
 * ```
 */
object Router {

    private val registry = HashMap<String, () -> Class<*>>()

    // -------------------------------------------------------------------------
    // 注册
    // -------------------------------------------------------------------------

    /** 注册路由 route -> 目标 Class（Activity 或 Fragment）。 */
    fun register(route: String, target: () -> Class<*>) {
        registry[route] = target
    }

    /** 注销路由。 */
    fun unregister(route: String) {
        registry.remove(route)
    }

    // -------------------------------------------------------------------------
    // Activity 导航
    // -------------------------------------------------------------------------

    /**
     * 跳转到已注册的 Activity 路由。
     *
     * @param context  当前 Context
     * @param route    路由名称
     * @param params   额外参数，会作为 Intent extras 传入（支持基本类型 + String + Parcelable）
     * @param flags    Intent flags，如 [Intent.FLAG_ACTIVITY_CLEAR_TOP]
     * @param finishCurrent 跳转后是否 finish 当前 Activity
     */
    fun navigateTo(
        context: Context,
        route: String,
        params: Map<String, Any?> = emptyMap(),
        flags: Int = 0,
        finishCurrent: Boolean = false
    ) {
        val clazz = resolve(route)
        require(Activity::class.java.isAssignableFrom(clazz)) {
            "Route '$route' points to ${clazz.simpleName} which is not an Activity."
        }
        val intent = Intent(context, clazz).apply {
            if (flags != 0) addFlags(flags)
            putExtrasFromMap(params)
        }
        context.startActivity(intent)
        if (finishCurrent && context is Activity) context.finish()
    }

    /**
     * 带返回结果跳转（startActivityForResult 替代，需 Activity 支持 registerForActivityResult）。
     * 这里封装成标准 startActivityForResult 以保持兼容性。
     */
    fun navigateForResult(
        activity: Activity,
        route: String,
        requestCode: Int,
        params: Map<String, Any?> = emptyMap()
    ) {
        val clazz = resolve(route)
        val intent = Intent(activity, clazz).apply { putExtrasFromMap(params) }
        @Suppress("DEPRECATION")
        activity.startActivityForResult(intent, requestCode)
    }

    // -------------------------------------------------------------------------
    // Fragment 导航
    // -------------------------------------------------------------------------

    /**
     * 在指定容器中显示 Fragment。
     *
     * @param fm           FragmentManager
     * @param containerId  容器 View id
     * @param route        路由名称
     * @param params       参数，会放入 Fragment.arguments Bundle
     * @param addToBackStack 是否加入返回栈
     * @param tag          Fragment tag（默认使用 route）
     */
    fun showFragment(
        fm: FragmentManager,
        containerId: Int,
        route: String,
        params: Map<String, Any?> = emptyMap(),
        addToBackStack: Boolean = false,
        tag: String = route
    ) {
        val clazz = resolve(route)
        require(Fragment::class.java.isAssignableFrom(clazz)) {
            "Route '$route' points to ${clazz.simpleName} which is not a Fragment."
        }
        @Suppress("UNCHECKED_CAST")
        val fragment = (clazz as Class<out Fragment>).getDeclaredConstructor().newInstance().apply {
            arguments = bundleFromMap(params)
        }
        fm.beginTransaction()
            .replace(containerId, fragment, tag)
            .apply { if (addToBackStack) addToBackStack(tag) }
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }

    // -------------------------------------------------------------------------
    // 工具
    // -------------------------------------------------------------------------

    private fun resolve(route: String): Class<*> =
        registry[route]?.invoke()
            ?: error("Router: no route registered for '$route'. " +
                     "Did you forget to call Router.register(\"$route\") { ... }?")

    @Suppress("UNCHECKED_CAST")
    private fun Intent.putExtrasFromMap(params: Map<String, Any?>) {
        params.forEach { (key, value) ->
            when (value) {
                is String  -> putExtra(key, value)
                is Int     -> putExtra(key, value)
                is Long    -> putExtra(key, value)
                is Float   -> putExtra(key, value)
                is Double  -> putExtra(key, value)
                is Boolean -> putExtra(key, value)
                is android.os.Parcelable -> putExtra(key, value)
                is java.io.Serializable  -> putExtra(key, value)
                null -> { /* skip null */ }
                else -> putExtra(key, value.toString())
            }
        }
    }

    private fun bundleFromMap(params: Map<String, Any?>): Bundle = Bundle().apply {
        params.forEach { (key, value) ->
            when (value) {
                is String  -> putString(key, value)
                is Int     -> putInt(key, value)
                is Long    -> putLong(key, value)
                is Float   -> putFloat(key, value)
                is Double  -> putDouble(key, value)
                is Boolean -> putBoolean(key, value)
                is android.os.Parcelable -> putParcelable(key, value)
                is java.io.Serializable  -> putSerializable(key, value)
                null -> { /* skip null */ }
                else -> putString(key, value.toString())
            }
        }
    }
}
