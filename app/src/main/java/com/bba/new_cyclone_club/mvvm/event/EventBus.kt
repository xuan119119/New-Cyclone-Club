package com.bba.new_cyclone_club.mvvm.event

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bba.new_cyclone_club.mvvm.logger.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于 LiveData 的轻量级事件总线。
 *
 * ### 特性
 * - **普通事件**：订阅后只收到订阅之后发出的事件（不回放历史）
 * - **粘性事件**：先发后订也能收到最近一条（类似 EventBus sticky）
 * - **生命周期安全**：依托 LiveData，自动跟随 LifecycleOwner，不会内存泄漏
 * - **多观察者**：同一事件类型支持多个观察者同时监听
 * - **类型安全**：以事件 Class 作为 Key，无需手动定义频道字符串
 *
 * ### 发送事件
 * ```kotlin
 * // 普通事件（只有订阅后才收到）
 * EventBus.post(LoginEvent(userId = "123"))
 *
 * // 粘性事件（订阅前发送，订阅后仍可收到最近一条）
 * EventBus.postSticky(NetworkStatusEvent(online = true))
 * ```
 *
 * ### 订阅事件（在 Activity / Fragment 中）
 * ```kotlin
 * // 普通事件
 * EventBus.observe<LoginEvent>(this) { event ->
 *     navigateToHome(event.userId)
 * }
 *
 * // 粘性事件
 * EventBus.observeSticky<NetworkStatusEvent>(this) { event ->
 *     updateNetworkBanner(event.online)
 * }
 * ```
 *
 * ### 在 ViewModel 中发送事件（无生命周期，用 observeForever）
 * ```kotlin
 * EventBus.observeForever<LogoutEvent> { logout() }
 * // 记得在 onCleared() 中调用 EventBus.removeObserver(observer)
 * ```
 */
object EventBus {

    // 普通事件频道：SingleLiveEvent 保证每条事件只下发一次
    private val channels = ConcurrentHashMap<Class<*>, SingleLiveEvent<*>>()

    // 粘性事件频道：普通 MutableLiveData，新订阅者会立即收到最后一条值
    private val stickyChannels = ConcurrentHashMap<Class<*>, MutableLiveData<*>>()

    // -------------------------------------------------------------------------
    // 发送
    // -------------------------------------------------------------------------

    /**
     * 发送普通事件。
     * 只有在调用此方法之后订阅的观察者，或已订阅但尚未收到的观察者，才会收到该事件。
     */
    fun <T : Any> post(event: T) {
        Logger.d("EventBus post: ${event::class.java.simpleName} -> $event", tag = "EventBus")
        @Suppress("UNCHECKED_CAST")
        val channel = getOrCreateChannel(event::class.java) as SingleLiveEvent<T>
        channel.postValue(event)
    }

    /**
     * 发送粘性事件。
     * 新观察者订阅时会立即收到最近一条粘性事件（若存在）。
     */
    fun <T : Any> postSticky(event: T) {
        Logger.d("EventBus postSticky: ${event::class.java.simpleName} -> $event", tag = "EventBus")
        @Suppress("UNCHECKED_CAST")
        val channel = getOrCreateStickyChannel(event::class.java) as MutableLiveData<T>
        channel.postValue(event)
    }

    // -------------------------------------------------------------------------
    // 订阅（生命周期感知）
    // -------------------------------------------------------------------------

    /**
     * 订阅普通事件，自动跟随 [owner] 生命周期，无需手动取消订阅。
     *
     * @param owner    LifecycleOwner（Activity / Fragment）
     * @param observer 事件回调
     */
    inline fun <reified T : Any> observe(
        owner: LifecycleOwner,
        noinline observer: (T) -> Unit
    ) = observe(owner, T::class.java, observer)

    fun <T : Any> observe(
        owner: LifecycleOwner,
        clazz: Class<T>,
        observer: (T) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        val channel = getOrCreateChannel(clazz) as SingleLiveEvent<T>
        channel.observe(owner, Observer { event ->
            if (event != null) {
                Logger.d("EventBus receive: ${clazz.simpleName} -> $event", tag = "EventBus")
                observer(event)
            }
        })
    }

    /**
     * 订阅粘性事件，自动跟随 [owner] 生命周期。
     * 若粘性频道已有值，订阅后会立即回调一次。
     */
    inline fun <reified T : Any> observeSticky(
        owner: LifecycleOwner,
        noinline observer: (T) -> Unit
    ) = observeSticky(owner, T::class.java, observer)

    fun <T : Any> observeSticky(
        owner: LifecycleOwner,
        clazz: Class<T>,
        observer: (T) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        val channel = getOrCreateStickyChannel(clazz) as MutableLiveData<T>
        channel.observe(owner, Observer { event ->
            if (event != null) {
                Logger.d("EventBus receiveSticky: ${clazz.simpleName} -> $event", tag = "EventBus")
                observer(event)
            }
        })
    }

    // -------------------------------------------------------------------------
    // 订阅（永久，需手动取消）
    // -------------------------------------------------------------------------

    /**
     * 永久订阅普通事件（不绑定生命周期）。
     * 适用于 ViewModel 或全局单例中，**必须**在不需要时调用 [removeObserver] 取消。
     *
     * @return 返回内部 Observer 实例，供 [removeObserver] 使用
     */
    inline fun <reified T : Any> observeForever(
        noinline observer: (T) -> Unit
    ): Observer<T> = observeForever(T::class.java, observer)

    fun <T : Any> observeForever(clazz: Class<T>, observer: (T) -> Unit): Observer<T> {
        @Suppress("UNCHECKED_CAST")
        val channel = getOrCreateChannel(clazz) as SingleLiveEvent<T>
        val liveObserver = Observer<T> { event ->
            if (event != null) observer(event)
        }
        channel.observeForever(liveObserver)
        return liveObserver
    }

    /**
     * 取消通过 [observeForever] 注册的观察者。
     */
    inline fun <reified T : Any> removeObserver(observer: Observer<T>) {
        removeObserver(T::class.java, observer)
    }

    fun <T : Any> removeObserver(clazz: Class<T>, observer: Observer<T>) {
        @Suppress("UNCHECKED_CAST")
        (channels[clazz] as? SingleLiveEvent<T>)?.removeObserver(observer)
    }

    // -------------------------------------------------------------------------
    // 粘性事件清除
    // -------------------------------------------------------------------------

    /** 清除指定类型的粘性事件（新订阅者将不再立即收到回调）。 */
    inline fun <reified T : Any> clearSticky() = clearSticky(T::class.java)

    fun clearSticky(clazz: Class<*>) {
        stickyChannels[clazz]?.value = null
        Logger.d("EventBus clearSticky: ${clazz.simpleName}", tag = "EventBus")
    }

    /** 清除所有粘性事件。 */
    fun clearAllSticky() {
        stickyChannels.values.forEach { it.value = null }
        Logger.d("EventBus clearAllSticky", tag = "EventBus")
    }

    // -------------------------------------------------------------------------
    // 内部工具
    // -------------------------------------------------------------------------

    private fun <T : Any> getOrCreateChannel(clazz: Class<T>): SingleLiveEvent<T> {
        @Suppress("UNCHECKED_CAST")
        return channels.getOrPut(clazz) { SingleLiveEvent<T>() } as SingleLiveEvent<T>
    }

    private fun <T : Any> getOrCreateStickyChannel(clazz: Class<T>): MutableLiveData<T> {
        @Suppress("UNCHECKED_CAST")
        return stickyChannels.getOrPut(clazz) { MutableLiveData<T>() } as MutableLiveData<T>
    }
}
