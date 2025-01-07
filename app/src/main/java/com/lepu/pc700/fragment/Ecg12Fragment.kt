package com.lepu.pc700.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.Carewell.OmniEcg.jni.JniTraditionalAnalysis
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.OmniEcg.jni.EcgDataManager
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.ecg700.port.OnECG12DataListener
import com.Carewell.ecg700.port.ParseEcg12Data
import com.Carewell.OmniEcg.jni.XmlUtil
import com.Carewell.ecg700.entity.EcgSettingConfigEnum
import com.Carewell.ecg700.entity.PatientInfoBean
import com.Carewell.view.ecg12.*
import com.Carewell.view.other.LoadingForView
import com.lepu.pc700.App
import com.lepu.pc700.MainActivity
import com.lepu.pc700.R
import com.lepu.pc700.databinding.FragmentEcg12Binding
import com.lepu.pc700.delayOnLifecycle
import com.lepu.pc700.dialog.Ecg12FilterSettingDialog
import com.lepu.pc700.dialog.PROJECT_DIR
import com.lepu.pc700.net.bean.Device
import com.lepu.pc700.net.bean.Ecg
import com.lepu.pc700.net.bean.EcgInfo
import com.lepu.pc700.net.bean.User
import com.lepu.pc700.net.vm.GetPDFViewModel
import com.lepu.pc700.onItemSelectedListener
import com.lepu.pc700.singleClick
import com.lepu.pc700.toast
import com.lepu.pc700.viewBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import kotlin.properties.Delegates
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Date
import java.util.Locale


class Ecg12Fragment : Fragment(R.layout.fragment_ecg12) {
    private var checkTimeStamp = 0L
    private var time = 15
    private lateinit var loading: LoadingForView
    private val binding by viewBinding(FragmentEcg12Binding::bind)
    private var leadType = LeadType.LEAD_12
    private var isPause = false
    private var isStart by Delegates.observable(false) { _, _, newValue ->
        if (newValue) { // 开始测量
            checkTimeStamp = System.currentTimeMillis()
            startRecordData()
        } else { // 停止测量
            countDownJob?.cancel()
            binding.btnStartMeasure.text = getString(R.string.start_measure)
        }
    }
    private var countDownJob: Job? = null
    private var saveDataList = Array(12) { ShortArray(time * 1000) }
    private var lowPassHz = 35
    private var hpHz = 0.67f
    private var acHz = 50

    private val viewModel: GetPDFViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle("12导心电")
        if (!App.serialStart) {
            App.serial.start()
            LogUtil.v("App.serialStart")
        }
        //屏幕常亮
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        MainEcgManager.getInstance().init()
        //默认增益
        val gain = "auto"
//        updateGain(gain, true)
        //默认走速
        val speed = 25.0f
        updateSpeed(speed, true)
        MainEcgManager.getInstance().updateMainEcgShowStyle(leadType)
        MainEcgManager.getInstance().drawEcgRealView = binding.drawEcgRealView

        loading = LoadingForView(requireContext(), binding.viewGroup)
        loading.show()
        binding.btnStartMeasure.singleClick {
            subscript = 0
            if (loading.isShow) return@singleClick
            isStart = !isStart
            if (isStart) {
                binding.spinnerGain.isEnabled = false
                binding.spinnerSpeed.isEnabled = false
                binding.spinnerShow.isEnabled = false
                binding.spinnerTime.isEnabled = false
            } else {
                binding.spinnerGain.isEnabled = true
                binding.spinnerSpeed.isEnabled = true
                binding.spinnerShow.isEnabled = true
                binding.spinnerTime.isEnabled = true
            }
        }
        binding.btnSettings.singleClick {
            if (isStart) {
                toast(R.string.collecting_please_click_to_view_when_finished)
                return@singleClick
            }
            Ecg12FilterSettingDialog().setOnAdoptListener { lowPassHz, hpHz, acHz, isAddPaceMaker ->
                ParseEcg12Data.setFilterParam(hpHz, lowPassHz, 45, acHz.toFloat())
                ParseEcg12Data.setIsAddPacemaker(isAddPaceMaker)
                this.lowPassHz = lowPassHz
                this.hpHz = hpHz
                this.acHz = acHz
            }.show(childFragmentManager, "Ecg12FilterSettingDialog")
        }

        binding.spinnerGain.setSelection(
            when (gain) {
                LeadGainType.GAIN_2_P_5.value.toString() -> 0
                LeadGainType.GAIN_5.value.toString() -> 1
                LeadGainType.GAIN_10.value.toString() -> 2
                LeadGainType.GAIN_20.value.toString() -> 3
                LeadGainType.GAIN_40.value.toString() -> 4
                else -> 5
            }
        )
        binding.spinnerGain.onItemSelectedListener {
            val value = when (it) {
                0 -> LeadGainType.GAIN_2_P_5.value.toString()
                1 -> LeadGainType.GAIN_5.value.toString()
                2 -> LeadGainType.GAIN_10.value.toString()
                3 -> LeadGainType.GAIN_20.value.toString()
                4 -> LeadGainType.GAIN_40.value.toString()
                else -> LeadGainType.GAIN_AUTO.value.toString()
            }
            updateGain(value, false)
        }
        binding.spinnerSpeed.setSelection(
            when (speed) {
                LeadSpeedType.FORMFEED_5.value -> 0
                LeadSpeedType.FORMFEED_6_P_25.value -> 1
                LeadSpeedType.FORMFEED_10.value -> 2
                LeadSpeedType.FORMFEED_12_P_5.value -> 3
                LeadSpeedType.FORMFEED_25.value -> 4
                else -> 5
            }
        )
        binding.spinnerSpeed.onItemSelectedListener {
            val value = when (it) {
                0 -> LeadSpeedType.FORMFEED_5.value
                1 -> LeadSpeedType.FORMFEED_6_P_25.value
                2 -> LeadSpeedType.FORMFEED_10.value
                3 -> LeadSpeedType.FORMFEED_12_P_5.value
                4 -> LeadSpeedType.FORMFEED_25.value
                else -> LeadSpeedType.FORMFEED_50.value
            }
            updateSpeed(value, false)
        }
        binding.spinnerShow.setSelection(0)
        binding.spinnerShow.onItemSelectedListener { updateEcgMode(it) }
        binding.spinnerTime.setSelection(0)
        binding.spinnerTime.onItemSelectedListener {
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
            //测量完成分析
            getLocalXML(saveDataList)
//            getAiPdf(saveDataList)
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
        return when (leadType) {
            // 六导联下如果I（LA）和II（LL）脱落则显示，否则将“脱落”字符窜去掉
            LeadType.LEAD_6 -> if (str1.contains("LA") || str1.contains("LL")) str1 else str1.replace(
                getString(R.string.fall),
                ""
            )

            // I导联情况下,不显示II导联情况，将"LL"字符窜去掉
            LeadType.LEAD_I -> if (str1.contains("LA")) str1.replace("LL", "") else str1.replace(
                getString(R.string.fall),
                ""
            )

            // II导联情况下,不显示I导联情况，将"LA"字符窜去掉
            LeadType.LEAD_II -> if (str1.contains("LL")) str1.replace("LA", "") else str1.replace(
                getString(R.string.fall),
                ""
            )

            else -> ""
        }
    }

    private var preHrTime = 0L
    private var preLeadTime = 0L
    private var subscript = 0
    private var isInit = true
    private var timeDelay = 0L
    private var sum = 0

    private var ecg12DataListener = object : OnECG12DataListener {
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
            if (loading.isShow) {
                activity?.runOnUiThread { loading.dismiss() }
            }
            sum++
            if (sum / 1000 > 9) {
                sum = 0
                LogUtil.v("心电数据接收--10s-->")
            }
            MainEcgManager.getInstance().addEcgData(ecgDataArray)
        }

        override fun onHrReceived(hr: Int) {
            if (loading.isShow || hr == -1) return

            val now = System.currentTimeMillis()
            if (now - preHrTime > 500) {//心电刷新率  500毫秒刷新一次。
                preHrTime = now
                if (!isPause) {
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
                if (!isPause) {
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
    }

    private fun initData() {
        App.serial.mAPI?.setEcgListener(ecg12DataListener)

        ParseEcg12Data.setFilterParam(hpHz, lowPassHz, 45, acHz.toFloat())
        App.serial.mAPI?.apply {
            startTransfer() // 透传
            if (App.mcuMainVer < 1324) {
                startECG12Measure() // 启动心电线程并发开始命令
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isPause = false
    }

    override fun onPause() {
        super.onPause()
        isPause = true
    }

    override fun onStop() {
        super.onStop()
        isStart = false
        countDownJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        MainEcgManager.getInstance().drawEcgRealView = null
        App.serial.mAPI?.setEcgListener(null)
        App.serial.mAPI?.stopTransfer()
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
        patientInfoBean.leadoffstate = 0  // 0 导联正常 1 导联有脱落
        launchWhenResumed {
            withContext(Dispatchers.IO) {
                try {
                    //I/II/ III/aVR/aVL/aVF /V1/V2/V3/V4/V5/V6
                    val data = ArrayList<ShortArray>()
                    var index: Int
                    for (i in 0..7) {
                        index = i
                        if (i > 1) {
                            index = i + 4
                        }
                        val temp = ecgDataArray[index]
                        data.add(temp.copyOfRange(temp.size - 1000 * 10, temp.size))
                    }
                    //再补7导数据 都是0
                    (0..6).forEach {
                        data.add(ShortArray(1000 * 10))  //win android linux 统一算法代码 这个地方补7导数据 默认0 一起15导数据
                    }
                    val filePath = "$PROJECT_DIR/test"
                    XmlUtil.createDir(filePath)
                    val fileName =
                        SimpleDateFormat("yyyyMMddHHmmss").format(checkTimeStamp)
                    //本地算法分析，分析出来数据
                    val xmlPath = "${filePath}/${fileName}.xml"
                    //只需要8导联数据
                    val resultBean = JniTraditionalAnalysis.traditionalAnalysis(
                        xmlPath,
                        EcgSettingConfigEnum.LeadType.LEAD_12,
                        patientInfoBean,
                        data.toTypedArray()
                    )
                    LogUtil.e(resultBean.toJson())
                    //诊断结论
                    resultBean.aiResultBean.aiResultDiagnosisBean.diagnosis.forEach {
                        val result = XmlUtil.map[it.code]
                    }
                    //1.生成心电分析xml  参数根据UI设置
                    XmlUtil.makeHl7Xml(
                        requireContext(),
                        "610423198612206399",
                        resultBean,
                        ecgDataArray,
                        LeadType.LEAD_12,
                        filePath,
                        fileName,
                        checkTimeStamp,
                        checkTimeStamp + time * 1000L,
                        "$lowPassHz",
                        "$hpHz", "$acHz"
                    )

                    //2.返回Bitmap 写文件保存或是直接展示
                    val imageBitmap = EcgDataManager.instance?.exportBmp(
                        requireContext(),
                        patientInfoBean,
                        resultBean,
                        ecgDataArray,
                        checkTimeStamp,
                        "$lowPassHz",
                        "$hpHz",
                        "$acHz",
                    )

                    val stream = FileOutputStream(File("${filePath}/${fileName}.jpg"))
                    imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 25, stream)
                    stream.flush()
                    stream.close()

                    //3.生成心电分析PDF
                    imageBitmap?.let {
                        EcgDataManager.instance?.exportPdf(it, "${filePath}/${fileName}.pdf")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getAiPdf(ecgDataArray: Array<ShortArray>) {
        viewModel.getAIPdf(
            requireContext(),
            ecgDataArray,
            EcgInfo().apply {
                ecg = Ecg().apply {
                    duration = time
                    measure_time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(
                        Date(checkTimeStamp)
                    )
                }
                user = User().apply {
                    name = "test"
                    phone = "18627222014"
                    gender = "1"
                    birthday = "1986-12-06"
                    id_number = "421023198902223431"
                }
                device = Device().apply {
                    sn = "519d9ccb40a0d478"
                }
            },
            checkTimeStamp,
            checkTimeStamp + time * 1000L,
        )

        viewModel.mECGPdf.observe(viewLifecycleOwner) {
            if (it != null)
                LogUtil.e("pdf $it")
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


