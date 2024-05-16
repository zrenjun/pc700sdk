package com.lepu.pc700

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.Carewell.OmniEcg.jni.JniTraditionalAnalysis
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.EcgDataManager
import com.Carewell.ecg700.LogUtil
import com.Carewell.ecg700.OnECG12DataListener
import com.Carewell.ecg700.ParseEcg12Data
import com.Carewell.ecg700.XmlUtil
import com.Carewell.ecg700.entity.EcgSettingConfigEnum
import com.Carewell.ecg700.entity.PatientInfoBean
import com.Carewell.view.ecg12.*
import com.Carewell.view.other.LoadingForView
import com.lepu.pc700.databinding.FragmentEcg12Binding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import kotlin.properties.Delegates

class Ecg12Fragment : Fragment(R.layout.fragment_ecg12) {
    private var time = 15
    private lateinit var loading: LoadingForView
    private val binding by viewBinding(FragmentEcg12Binding::bind)
    private var leadType = LeadType.LEAD_12
    private var bindingonDestroy = false
    private var isStart by Delegates.observable(false) { _, _, newValue ->
        if (newValue) { // 开始测量
            startRecordData()
        } else { // 停止测量
            countDownJob?.cancel()
            binding.btnStartMeasure.text = getString(R.string.start_measure)
        }
    }
    private var countDownJob: Job? = null
    private var saveDataList = Array(12) { ShortArray(time * 1000) }
    private var isAiECG = false
    private var goHealthRecord = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle("12导心电")
        if (!App.serialStart) {
            App.serial.start()
            LogUtil.v("App.serialStart")
        }

        //屏幕常亮 手动按灭屏幕后 不知道为什么，这个页面屏幕会熄灭
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        isAiECG = arguments?.getBoolean("isAiECG") ?: false
        MainEcgManager.getInstance().init(requireContext())
        val gain = "10"
        updateGain(gain, true)
        val speed = 25.0f
        updateSpeed(speed, true)
        MainEcgManager.getInstance().updateMainEcgShowStyle(leadType)
        MainEcgManager.getInstance().drawEcgRealView = binding.drawEcgRealView

        with(binding) {
            loading = LoadingForView(requireContext(), viewGroup)
            loading.show()
            btnStartMeasure.singleClick {

                subscript = 0
                if (loading.isShow) return@singleClick
                isStart = !isStart
                if (isStart) {
                    spinnerGain.isEnabled = false
                    spinnerSpeed.isEnabled = false
                    spinnerShow.isEnabled = false
                    spinnerTime.isEnabled = false
                } else {
                    spinnerGain.isEnabled = true
                    spinnerSpeed.isEnabled = true
                    spinnerShow.isEnabled = true
                    spinnerTime.isEnabled = true
                }
            }
            btnSettings.singleClick {
                if (isStart) {
                    toast(R.string.collecting_please_click_to_view_when_finished)
                    return@singleClick
                }
                Ecg12FilterSettingDialog().show(childFragmentManager, "Ecg12FilterSettingDialog")
            }

            spinnerGain.setSelection(
                when (gain) {
                    LeadGainType.GAIN_2_P_5.value.toString() -> 0
                    LeadGainType.GAIN_5.value.toString() -> 1
                    LeadGainType.GAIN_10.value.toString() -> 2
                    LeadGainType.GAIN_20.value.toString() -> 3
                    LeadGainType.GAIN_40.value.toString() -> 4
                    else -> 5
                }
            )
            spinnerGain.onItemSelectedListener {
                val value = when (it) {
                    0 -> LeadGainType.GAIN_2_P_5.value.toString()
                    1 -> LeadGainType.GAIN_5.value.toString()
                    2 -> LeadGainType.GAIN_10.value.toString()
                    3 -> LeadGainType.GAIN_20.value.toString()
                    else -> LeadGainType.GAIN_40.value.toString()
                }
                updateGain(value, false)
            }
            spinnerSpeed.setSelection(
                when (speed) {
                    LeadSpeedType.FORMFEED_6_P_25.value -> 0
                    LeadSpeedType.FORMFEED_12_P_5.value -> 1
                    LeadSpeedType.FORMFEED_25.value -> 2
                    else -> 3
                }
            )
            spinnerSpeed.onItemSelectedListener {
                val value = when (it) {
                    0 -> LeadSpeedType.FORMFEED_6_P_25.value
                    1 -> LeadSpeedType.FORMFEED_12_P_5.value
                    2 -> LeadSpeedType.FORMFEED_25.value
                    else -> LeadSpeedType.FORMFEED_50.value
                }
                updateSpeed(value, false)
            }
            spinnerShow.setSelection(0)
            spinnerShow.onItemSelectedListener { updateEcgMode(it) }
            spinnerTime.setSelection(0)
            spinnerTime.onItemSelectedListener {
                val value = when (it) {
                    0 -> 15
                    1 -> 30
                    2 -> 60
                    3 -> 180
                    else -> 15
                }
                time = value
                saveDataList = Array(12) { ShortArray(time * 1000) }
            }
        }
        initData()
    }


    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    private fun startRecordData() {
        countDownJob = countDownFlow(time, lifecycleScope, {
            binding.btnStartMeasure.text = "$it/${time}S"
        }, onFinish = {
            isStart = false
            binding.spinnerGain.isEnabled = true
            binding.spinnerSpeed.isEnabled = true
            binding.spinnerShow.isEnabled = true
            binding.spinnerTime.isEnabled = true
            subscript = 0
            getLocalXML(saveDataList)
        }, onStart = {

        })
    }

    /**
     * 更新增益
     */
    private fun updateGain(gain: String, isOnlyUpdateGainData: Boolean) {
        var leadGainType: LeadGainType? = null
        for (item in LeadGainType.values()) {
            if (gain == item.value) {
                leadGainType = item
                break
            }
        }
        leadGainType?.let {
            if (isOnlyUpdateGainData) {
                MainEcgManager.getInstance().updateMainGainOnlyData(it.ordinal)
            } else {
                MainEcgManager.getInstance().updateMainGain(it.ordinal)
            }
        }
    }

    /**
     * 更新走速
     */
    private fun updateSpeed(speed: Float, isOnlyUpdateSpeedData: Boolean) {
        var leadSpeedType: LeadSpeedType? = null
        for (item in LeadSpeedType.values()) {
            if (speed == item.value) {
                leadSpeedType = item
                break
            }
        }
        leadSpeedType?.let {
            if (isOnlyUpdateSpeedData) {
                MainEcgManager.getInstance().updateMainSpeedOnlyData(it.ordinal)
            } else {
                MainEcgManager.getInstance().updateMainSpeed(it.ordinal)
            }
        }
    }

    private fun updateEcgMode(displayMode: Int) {
        leadType = when (displayMode) {
            1 -> LeadType.LEAD_6
            2 -> LeadType.LEAD_I
            3 -> LeadType.LEAD_II
            else -> LeadType.LEAD_12
        }
        if (displayMode != 2) {
            ParseEcg12Data.setLeadHrMode(true)
        } else {
            ParseEcg12Data.setLeadHrMode(false)
        }
        MainEcgManager.getInstance().updateMainEcgShowStyle(leadType)
        MainEcgManager.getInstance().resetDrawEcg()
    }

    private fun getStrByLeadFall(str: String, fall: Boolean): String {
        // 如果是str是导联正常或者是十二导联模式，直接显示脱落情况
        if (!fall || leadType == LeadType.LEAD_12) return str
        // 后面单导和肢体导联不显示V1-V6的脱落情况，这里将V1-V6的字符窜用空字符窜代替, "|"表示间隔字符
        val str1 = str.replace("V1| V2| V3| V4| V5| V6".toRegex(), "")
        var str2 = ""
        when (leadType) {
            LeadType.LEAD_6 -> { // 六导联下如果I（LA）和II（LL）脱落则显示，否则将“脱落”字符窜去掉
                str2 = if (str1.contains("LA") || str1.contains("LL")) {
                    str1
                } else {
                    str1.replace(getString(R.string.fall), "")
                }
            }

            LeadType.LEAD_I -> { // I导联情况下,不显示II导联情况，将"LL"字符窜去掉
                str2 = if (str1.contains("LA")) {
                    str1.replace("LL", "")
                } else {
                    str1.replace(getString(R.string.fall), "")
                }
            }

            LeadType.LEAD_II -> { // II导联情况下,不显示I导联情况，将"LA"字符窜去掉
                str2 = if (str1.contains("LL")) {
                    str1.replace("LA", "")
                } else {
                    str1.replace(getString(R.string.fall), "")
                }
            }

            else -> {
            }
        }
        return str2
    }

    private var preHrTime = 0L
    private var preLeadTime = 0L
    private var subscript = 0
    private var isInit = true
    private var isupFileLoading = true
    private var timeDelay = 0L

    private fun initData() {
        App.serial.mAPI?.setEcgListener(object : OnECG12DataListener {
            override fun onECG12DataReceived(ecg12Data: IntArray) {
                val ecgDataArray = Array(12) { ShortArray(1) }
                ecg12Data.forEachIndexed { index, i ->
                    ecgDataArray[index][0] = i.toShort()
                    if (isStart && subscript < saveDataList[0].size) {
                        saveDataList[index][subscript] = i.toShort()
                    }
                }
                if (isStart) {
                    subscript++
                }

                if (isInit) {
                    timeDelay = System.currentTimeMillis()
                    isInit = false
                }
                if (System.currentTimeMillis() - timeDelay < 2000L) {
                    return
                }
                if (isupFileLoading) {
                    isupFileLoading = false
                    //第一次进来这个页面的时候接收到数据后关闭。
                    // 上传文件的时候也会弹出转圈圈，
                    // 所以这个地方只能用一次就不能每次接收到数据都关闭
                    activity?.runOnUiThread {
                        loading.dismiss()
                    }
                }

                MainEcgManager.getInstance().addEcgData(ecgDataArray)
            }

            override fun onHrReceived(hr: Int) {
                if (loading.isShow || hr == -1) return

                val now = System.currentTimeMillis()
                if (now - preHrTime > 500) {//心电刷新率  500毫秒刷新一次。
                    preHrTime = now
                    if (!bindingonDestroy) {
                        binding.tvHr.delayOnLifecycle {
                            val preHr = binding.tvHr.text.toString()
                            if (preHr != "--" && preHr.toInt() != hr) {
                                LogUtil.v("心率  ---->  $hr")
                            }
                            binding.tvHr.text = if (hr == 0) "--" else "$hr"
                        }
                    }
                }
            }

            override fun onLeadFailReceived(leadFail: String, fall: Boolean) {
                val now = System.currentTimeMillis()
                if (now - preLeadTime > 100) {
                    preLeadTime = now
                    if (!bindingonDestroy) {
                        //联导脱落
                        binding.tvLeadFall.delayOnLifecycle {
                            binding.tvLeadFall.text = getStrByLeadFall(
                                leadFail + getString(if (fall) R.string.fall else R.string.lead_normal),
                                fall
                            )
                        }
                    }
                }
            }
        })

        ParseEcg12Data.setFilterParam(0.67f, 35, 50.0f)
        App.serial.mAPI?.apply {
            startTransfer() // 透传
            startECG12Measure() // 启动心电线程并发开始命令
        }
    }

    override fun onResume() {
        super.onResume()
        App.serial.mAPI?.apply {
            startECG12Measure() // 启动心电线程并发开始命令
        }
        bindingonDestroy = false
    }

    override fun onPause() {
        super.onPause()
        bindingonDestroy = true
    }

    override fun onStop() {
        super.onStop()
        isStart = false
        countDownJob?.cancel()
        if (!goHealthRecord) {
            App.serial.mAPI?.apply {
                stopECG12Measure()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!goHealthRecord) {
            App.serial.mAPI?.apply {
                stopTransfer()
            }
        }
        MainEcgManager.getInstance().clearEcgData()
    }

    @SuppressLint("SimpleDateFormat")
    private fun getLocalXML(ecgDataArray: Array<ShortArray>) {
        //病人信息设置
        val patientInfoBean = PatientInfoBean()
        patientInfoBean.archivesName = "moArchivesName"
        patientInfoBean.firstName = "moFirstName"
        patientInfoBean.lastName = "moLastName"
        patientInfoBean.middleName = "moMiddleName"
        patientInfoBean.idNumber = "102"
        patientInfoBean.patientNumber = "111"
        patientInfoBean.age = "20"
        patientInfoBean.birthdate = "2003-09-08"
        patientInfoBean.leadoffstate = 0
        launchWhenResumed {
            //I/II/III/aVR/aVL/aVF/V1/V2/V3/V4/V5/V6
            val data = ArrayList<ShortArray>()
            var index = 0
            for (i in 0..7) {
                index = i
                if (i > 1) {
                    index = i + 4
                }
                data.add(ecgDataArray[index])
            }
            val dir = Environment.getExternalStorageDirectory().absolutePath
            val filePath = "$dir/PC700/test"
            XmlUtil.createDir(filePath)
            val fileName =
                SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis())
            //本地算法分析，分析出来数据
            val xmlPath = "${filePath}/${fileName}.xml"
            val resultBean = JniTraditionalAnalysis.traditionalAnalysis(
                xmlPath,
                EcgSettingConfigEnum.LeadType.LEAD_12,
                patientInfoBean,
                data.toTypedArray()
            )
            LogUtil.e(resultBean.toJson())
            //2.生成心电分析xml  参数根据UI设置
            XmlUtil.makeHl7Xml(
                requireContext(),
                "610423198612206399",
                resultBean,
                saveDataList,
                LeadType.LEAD_12,
                filePath,
                fileName,
                "35",
                "0.67",
                "50"
            )
            //2.生成心电分析PDF
            EcgDataManager.instance?.exportPdf(
                requireContext(),
                patientInfoBean,
                resultBean,
                saveDataList,
                System.currentTimeMillis(),
                "${filePath}/${fileName}.pdf",
                "35",
                "0.67",
                "50"
            )
        }
    }
}


fun countDownFlow(
    total: Int,
    scope: CoroutineScope,
    onTick: (Int) -> Unit,
    onStart: (() -> Unit)? = null,
    onFinish: (() -> Unit)? = null
): Job = flow {
    for (i in total downTo 0) {
        if (i == 0) break
        emit(i)
        delay(1000)
    }
}.flowOn(Dispatchers.Main)
    .onStart { onStart?.invoke() }
    .onCompletion { if (it == null) onFinish?.invoke() }
    .onEach { onTick.invoke(it) }
    .launchIn(scope)


/**
 * 递归创建文件夹
 */
fun createDir(path: String): File? {
    val file = File(path)
    if (file.exists()) return file
    val p = file.parentFile
    if (!p.exists()) {
        createDir(p.path)
    }
    return if (file.mkdir()) file else null
}

/**
 * 递归创建文件夹
 */
@Throws(IOException::class)
fun createFile(path: String, fileName: String): File? {
    val filePath = createDir(path)
    if (filePath != null) {
        val file = File(filePath, fileName)
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        return file
    }
    return null
}