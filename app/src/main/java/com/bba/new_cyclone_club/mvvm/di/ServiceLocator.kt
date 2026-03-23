package com.bba.new_cyclone_club.mvvm.di

import kotlin.reflect.KClass

/**
 * 轻量级依赖注入容器（Service Locator 模式）。
 *
 * ### 注册依赖
 * ```kotlin
 * // Application.onCreate() 中注册
 * ServiceLocator.register<UserRepository> { UserRepositoryImpl() }
 * ServiceLocator.registerSingleton<ApiService>(RetrofitApiService())
 * ```
 *
 * ### 注入依赖
 * ```kotlin
 * // 在 ViewModel / Activity / Fragment 中
 * private val repo: UserRepository by inject()
 * ```
 *
 * ### 单例 vs 工厂
 * - [registerSingleton]：容器持有同一实例，多次 [get] 返回相同对象。
 * - [register]（工厂）：每次 [get] 调用提供的工厂 lambda，创建新实例。
 */
object ServiceLocator {

    private val singletons = HashMap<KClass<*>, Any>()
    private val factories  = HashMap<KClass<*>, () -> Any>()

    // -------------------------------------------------------------------------
    // 注册 API
    // -------------------------------------------------------------------------

    /** 注册单例（立即持有实例）。 */
    fun <T : Any> registerSingleton(clazz: KClass<T>, instance: T) {
        singletons[clazz] = instance
    }

    /** 注册单例（立即持有实例）— reified 版本。 */
    inline fun <reified T : Any> registerSingleton(instance: T) =
        registerSingleton(T::class, instance)

    /** 注册工厂（懒加载，每次 get 创建新实例）。 */
    fun <T : Any> register(clazz: KClass<T>, factory: () -> T) {
        factories[clazz] = factory
    }

    /** 注册工厂（懒加载）— reified 版本。 */
    inline fun <reified T : Any> register(noinline factory: () -> T) =
        register(T::class, factory)

    /** 注册懒加载单例（首次 get 时才创建，之后复用）。 */
    inline fun <reified T : Any> registerLazySingleton(noinline factory: () -> T) {
        val lazy = lazy(factory)
        register<T> { lazy.value }
    }

    // -------------------------------------------------------------------------
    // 获取 API
    // -------------------------------------------------------------------------

    /**
     * 获取依赖实例。
     * 优先返回单例；若无单例则调用工厂；两者皆无则抛出异常。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: KClass<T>): T {
        singletons[clazz]?.let { return it as T }
        factories[clazz]?.let  { return it() as T }
        error("ServiceLocator: no binding found for ${clazz.qualifiedName}. " +
              "Did you forget to call register() or registerSingleton()?")
    }

    /** 获取依赖实例 — reified 版本。 */
    inline fun <reified T : Any> get(): T = get(T::class)

    // -------------------------------------------------------------------------
    // 清理（测试用）
    // -------------------------------------------------------------------------

    /** 清除所有注册（单测隔离用）。 */
    fun reset() {
        singletons.clear()
        factories.clear()
    }

    /** 移除指定类型的注册。 */
    inline fun <reified T : Any> unregister() {
        singletons.remove(T::class)
        factories.remove(T::class)
    }
}

// =============================================================================
// 委托扩展：在任意类中使用 by inject()
// =============================================================================

/**
 * 属性委托，用于在 Activity / Fragment / ViewModel 中懒注入依赖。
 *
 * ```kotlin
 * class LoginViewModel : BaseViewModel() {
 *     private val repo: UserRepository by inject()
 * }
 * ```
 */
inline fun <reified T : Any> inject(): Lazy<T> = lazy { ServiceLocator.get<T>() }
