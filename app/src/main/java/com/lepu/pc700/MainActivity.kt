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
import com.Carewell.OmniEcg.jni.EcgDataManager
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.OmniEcg.jni.XmlUtil
import com.Carewell.ecg700.entity.EcgSettingConfigEnum
import com.Carewell.ecg700.entity.PatientInfoBean
import com.Carewell.ecg700.port.IAPVersionEvent
import com.Carewell.ecg700.port.observeEvent
import com.Carewell.view.ecg12.LeadGainType
import com.Carewell.view.ecg12.LeadSpeedType
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.lepu.pc700.databinding.ActivityMainBinding
import com.lepu.pc700.dialog.PROJECT_DIR
import com.lepu.pc700.fragment.KeepStateNavigator
import com.lepu.pc700.fragment.launchWhenResumed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.withContext
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

    @OptIn(InternalCoroutinesApi::class)
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

        App.serial.mAPI?.getVer(true)
        observeEvent<IAPVersionEvent> {
            LogUtil.v(it.toJson())
            if (it.response.toInt() == 2) {
                if (it.softwareVersion != 0) {
                    App.mcuMainVer = it.softwareVersion
                }
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        LogUtil.e("onSaveInstanceState")
    }


    override fun onResume() {
        super.onResume()
        LogUtil.e("onResume")
//        launchWhenResumed {
//            withContext(Dispatchers.IO) {
//                pdfCreate()
//            }
//        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun pdfCreate() {

        val patientInfoBean = PatientInfoBean()
        patientInfoBean.archivesName = "moArchivesName"
        patientInfoBean.firstName = "moFirstName"
        patientInfoBean.lastName = "moLastName"
        patientInfoBean.middleName = "moMiddleName"
        patientInfoBean.idNumber = "102"
        patientInfoBean.patientNumber = "111"
        patientInfoBean.age = "20"
        patientInfoBean.birthdate = "2003-09-08"
        patientInfoBean.leadoffstate = 0  // 0 导联正常 1 导联有脱落
        val data = XmlUtil.getHl7XmlMvData(
            this,
            "ai_ecg_data.xml"
        )  // I II III aVR aVL aVF V1 V2 V3 V4 V5 V6
        val filePath = "$PROJECT_DIR/test"
        XmlUtil.createDir(filePath)
        val fileName =
            SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis())
        val test = ArrayList<ShortArray>()

        // 导联数据 12导联原始数据中取 I II V1 V2 V3 V4 V5 V6
        data.forEachIndexed { index, shorts ->
            if (index < 2 || index > 5) {
                test.add(shorts.copyOfRange(shorts.size - 1000 * 10, shorts.size))
            }
        }
        //再补7导数据 都是0
        (0..6).forEach {
            test.add(ShortArray(1000 * 10))  //win android linux 统一算法代码 这个地方补7导数据 默认0 一起15导数据
        }
        val xmlPath = "${filePath}/${fileName}.xml"
        //需要15导联数据
        val resultBean = JniTraditionalAnalysis.traditionalAnalysis(
            xmlPath,
            EcgSettingConfigEnum.LeadType.LEAD_12,
            patientInfoBean,
            test.toTypedArray()
        )
        LogUtil.e(resultBean.toJson())

        val imageBitmap = EcgDataManager.instance?.exportBmp(
            this,
            patientInfoBean,
            resultBean,
            data.toTypedArray(),
            System.currentTimeMillis(),
            LeadGainType.GAIN_10,
            LeadSpeedType.FORMFEED_25,
            "35",
            "0.67f",
            "50",
        )

        val stream = FileOutputStream(File("${filePath}/${fileName}.jpg"))
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 25, stream)
        stream.flush()
        stream.close()

        //3.生成心电分析PDF
        imageBitmap?.let {
            EcgDataManager.instance?.exportPdf(it, "${filePath}/${fileName}.pdf")
        }
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