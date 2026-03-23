package com.bba.new_cyclone_club.mvvm.command

import android.view.View
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import androidx.databinding.BindingAdapter

/**
 * DataBinding 自定义属性适配器。
 *
 * 使这些属性可直接在 XML 中使用：
 *  - `app:onClickCommand`     单击命令
 *  - `app:onLongClickCommand` 长按命令
 *  - `app:onTextChanged`      文字变化命令（EditText）
 */
object BindingAdapters {

    /**
     * 将无参 [BindingCommand] 绑定到 View 的点击事件。
     *
     * XML: `app:onClickCommand="@{vm.onSomeClick}"`
     */
    @JvmStatic
    @BindingAdapter("onClickCommand")
    fun bindClickCommand(view: View, command: BindingCommand<Unit>?) {
        view.setOnClickListener {
            command?.execute()
        }
    }

    /**
     * 将无参 [BindingCommand] 绑定到 View 的长按事件。
     *
     * XML: `app:onLongClickCommand="@{vm.onSomeLongClick}"`
     */
    @JvmStatic
    @BindingAdapter("onLongClickCommand")
    fun bindLongClickCommand(view: View, command: BindingCommand<Unit>?) {
        view.setOnLongClickListener {
            command?.execute()
            true
        }
    }

    /**
     * 将携带 [String] 参数的 [BindingCommand] 绑定到 EditText 的文字变化事件。
     *
     * XML: `app:onTextChanged="@{vm.onInput}"`
     */
    @JvmStatic
    @BindingAdapter("onTextChanged")
    fun bindTextChangedCommand(editText: EditText, command: BindingCommand<String>?) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                command?.execute(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }
}
