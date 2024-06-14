package com.lepu.pc700

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.Carewell.OmniEcg.jni.JniTraditionalAnalysis
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.EcgDataManager
import com.Carewell.ecg700.LogUtil
import com.Carewell.ecg700.XmlUtil
import com.Carewell.ecg700.entity.EcgSettingConfigEnum
import com.Carewell.ecg700.entity.MacureResultBean
import com.Carewell.ecg700.entity.PatientInfoBean
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.lepu.pc700.databinding.ActivityMainBinding
import com.lepu.pc700.dialog.PROJECT_DIR
import com.lepu.pc700.fragment.KeepStateNavigator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat


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

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        LogUtil.e("onSaveInstanceState")
    }


    @SuppressLint("SimpleDateFormat")
    override fun onResume() {
        super.onResume()
        LogUtil.e("onResume")
//        val patientInfoBean = PatientInfoBean()
//        patientInfoBean.archivesName = "moArchivesName"
//        patientInfoBean.firstName = "moFirstName"
//        patientInfoBean.lastName = "moLastName"
//        patientInfoBean.middleName = "moMiddleName"
//        patientInfoBean.idNumber = "102"
//        patientInfoBean.patientNumber = "111"
//        patientInfoBean.age = "20"
//        patientInfoBean.birthdate = "2003-09-08"
//        patientInfoBean.leadoffstate = 0  // 0 导联正常 1 导联有脱落
//        val data = XmlUtil.getHl7XmlMvData(this,"11_2024-06-13 14-56-50.xml")  // I II III aVR aVL aVF V1 V2 V3 V4 V5 V6
//        val filePath = "$PROJECT_DIR/test"
//        XmlUtil.createDir(filePath)
//        val fileName =
//            SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis())
//        val test = ArrayList<ShortArray>()
//        var index: Int
//        for (i in 0..7) {
//            index = i
//            if (i > 1) {
//                index = i + 4
//            }
//            test.add(data[index])
//        }
//
//        val xmlPath = "${filePath}/${fileName}.xml"
//        //只需要8导联数据
//        val resultBean = JniTraditionalAnalysis.traditionalAnalysis(
//            xmlPath,
//            EcgSettingConfigEnum.LeadType.LEAD_12,
//            patientInfoBean,
//            data.toTypedArray()
//        )
//        LogUtil.e(resultBean.toJson())
//
//        val imageBitmap = EcgDataManager.instance?.exportBmp(
//            this,
//            patientInfoBean,
//            resultBean,
//            data.toTypedArray(),
//            System.currentTimeMillis(),
//            "35",
//            "0.67f",
//            "50",
//        )
//
//        val stream = FileOutputStream(File("${filePath}/${fileName}.jpg"))
//        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 25, stream)
//        stream.flush()
//        stream.close()
//
//        //3.生成心电分析PDF
//        imageBitmap?.let {
//            EcgDataManager.instance?.exportPdf(it, "${filePath}/${fileName}.pdf")
//        }

    }

    override fun onPause() {
        super.onPause()
        LogUtil.e("onPause")
    }


    fun setMainTitle(string: String) {
        binding.tvMiddle.text = string
        title.add(string)
        binding.tvLeft.isVisible = true
    }
}


inline fun <reified T> String.toBean(
    dateFormat: String = "yyyy-MM-dd HH:mm:ss",
    lenient: Boolean = false
) = GsonBuilder().disableHtmlEscaping().setDateFormat(dateFormat)
    .apply {
        if (lenient) setLenient()
    }.create()
    .fromJson<T>(this, object : TypeToken<T>() {}.type)