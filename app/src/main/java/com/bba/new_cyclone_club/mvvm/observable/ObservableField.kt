package com.bba.new_cyclone_club.mvvm.observable

import androidx.lifecycle.MutableLiveData

/**
 * 双向绑定字段封装，对 MutableLiveData 的轻量包装。
 * 在 XML DataBinding 表达式中使用 @{vm.field} 单向绑定，
 * 或使用 @={vm.field} 实现双向绑定（EditText 等）。
 */
class ObservableField<T>(initialValue: T) : MutableLiveData<T>(initialValue) {

    /** 同步读取当前值（非空断言，初始值已在构造时提供）。 */
    fun get(): T = value!!

    /** 同步写入新值。 */
    fun set(newValue: T) {
        value = newValue
    }

    /**
     * 在后台线程安全地发送新值（内部调用 postValue）。
     */
    fun postSet(newValue: T) {
        postValue(newValue)
    }
}

/**
 * 可空版 ObservableField，初始值允许为 null。
 */
class ObservableNullableField<T>(initialValue: T? = null) : MutableLiveData<T>(initialValue)
