package com.bba.new_cyclone_club

import com.bba.new_cyclone_club.databinding.ActivityMainBinding
import com.bba.new_cyclone_club.mvvm.base.BaseActivity

/**
 * MVVM 框架演示 Activity。
 *
 * 继承 [BaseActivity]，只需指定：
 *  - 泛型参数  <ActivityMainBinding, MainViewModel>
 *  - [layoutId]   layout 资源 id
 *  - [variableId] DataBinding 变量 id（对应 layout 中 <variable name="vm">）
 *
 * 所有 ViewModel 创建、DataBinding 绑定、UI 事件订阅均由基类自动完成。
 */
class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>() {

    override val layoutId   = R.layout.activity_main
    override val variableId = BR.vm

    override fun initView() {
        // 如需手动访问 binding / viewModel，在此处理
        // 例如：binding.recyclerView.adapter = MyAdapter()
    }

    override fun initData() {
        // 触发初始数据加载
        // 例如：viewModel.fetchData()
    }
}
