package com.lepu.pc700

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.Carewell.ecg700.LogUtil
import com.lepu.pc700.databinding.ActivityMainBinding
import com.lepu.pc700.utils.singleClick
import com.lepu.pc700.utils.viewBinding


/**
 *
 *  说明:
 *  zrj 2024/3/22 18:10
 *
 */
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private var title = mutableListOf<String>()
    private val binding by viewBinding(ActivityMainBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //添加自定义的KeepStateNavigator
        val navController = findNavController(R.id.my_nav_host_fragment)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment
        val navigator =
            KeepStateNavigator(this, navHostFragment.childFragmentManager, navHostFragment.id)
        navController.navigatorProvider.addNavigator(navigator)
        val navGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)
        navGraph.startDestination = R.id.mainFragment
        navController.graph = navGraph
        with(binding) {
            //返回键
            tvLeft.singleClick {
                val back = navController.popBackStack()
                if (!back) {
                    navController.popBackStack(R.id.mainFragment, true)
                    navController.navigate(R.id.mainFragment)
                }
                if (title.isNotEmpty()) {
                    title.removeLast()
                } else {
                    LogUtil.e("title is empty")
                }
                if (title.isNotEmpty()) {
                    tvMiddle.text = title.last()
                } else {
                    tvMiddle.text = "Demo"
                    tvLeft.isVisible = false
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        LogUtil.e("onSaveInstanceState")
    }


    override fun onResume() {
        super.onResume()
        LogUtil.e("onResume")
        App.serial.mAPI?.wakeUp()
    }

    override fun onPause() {
        super.onPause()
        LogUtil.e("onPause")
        App.serial.mAPI?.sleep()
    }


    fun setMainTitle(string: String) {
        binding.tvMiddle.text = string
        title.add(string)
        binding.tvLeft.isVisible = true
    }
}