package com.lepu.pc700

import android.annotation.SuppressLint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.Carewell.ecg700.*
import com.lepu.pc700.databinding.FragmentRoutineexaminationBinding
import com.lepu.pc700.utils.singleClick
import com.lepu.pc700.utils.viewBinding
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * 常规检测
 *created 2022/1/28 16:58
 */
class RoutineExaminationFragment : Fragment(R.layout.fragment_routineexamination) {
    private val binding by viewBinding(FragmentRoutineexaminationBinding::bind)
    private var mPR = 0
    private var bProSpO2 = false //血氧探头脱落 false:脱落
    private var bNibpStart = false
    private var strGLU: String? = null
    private var gluMgdl: String? = null
    private var strUA: String? = null
    private var uaMgdl: String? = null
    private var strCHOL: String? = null
    private var cholMgdl: String? = null
    private var strBK: String? = null
    private var bkMgdl: String? = null

    private var arrSpo2Rank: Array<String>? = null
    private var arrPrRank: Array<String>? = null
    private var arrTempRank: Array<String>? = null
    private var arrGluRank: Array<String>? = null
    private var arrNibpERR: Array<String>? = null
    private var arrNibpRank: Array<String>? = null
    private var arrCholRank: Array<String>? = null
    private var arrUARank: Array<String>? = null
    private var arrBKRank: Array<String>? = null
    private var speechMsg = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //屏幕常亮  手动按灭屏幕后 不知道为什么，这个页面屏幕会熄灭
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //初始化等级
        arrSpo2Rank = resources.getStringArray(R.array.spo2_rank)
        arrPrRank = resources.getStringArray(R.array.pr_rank)
        arrTempRank = resources.getStringArray(R.array.temp_rank)
        arrNibpRank = resources.getStringArray(R.array.nibp_result)
        arrNibpERR = resources.getStringArray(R.array.nibp_errors)
        arrGluRank = resources.getStringArray(R.array.glu_rank)
        arrCholRank = resources.getStringArray(R.array.chol_rank)
        arrUARank = resources.getStringArray(R.array.ua_rank)
        arrBKRank = resources.getStringArray(R.array.bk_rank)
        App.serial.mAPI?.setPressureMode(getNibpDevice())
        with(binding) {
            setUnitSp()
            spoView.setScope(150, 0)
            spoView.Start()
            btnNibp.singleClick {
                bNibpStart = !bNibpStart
                if (bNibpStart) {
                    binding.btnNibp.setBackgroundResource(R.drawable.start_measure_nibp_click)
                    App.serial.mAPI?.startNIBPMeasure()
                } else {
                    binding.btnNibp.setBackgroundResource(R.drawable.start_measure_nibp)
                    App.serial.mAPI?.stopNIBPMeasure()
                }
            }
            clUrine.singleClick { findNavController().navigate(R.id.urineFragment) }

            rbAfterMeal.singleClick { setGLUMeasureType(1) }

            rbBeforeMeal.singleClick { setGLUMeasureType(0) }

            fab.singleClick {
                val multiDialog = MultiBottomDialog()
                multiDialog.show(childFragmentManager, "MultiCheckDialog")
                multiDialog.setOKClickListener(
                    object : MultiBottomDialog.IOKClickListener {
                        override fun onClickCallback(checkType: MultiBottomDialog.CheckType?) {
                            when (checkType) {
                                MultiBottomDialog.CheckType.MMOL,
                                MultiBottomDialog.CheckType.MGDL -> {
                                    setGLU()
                                    setUA()
                                    setCHOL()
                                    setBK()
                                }

                                MultiBottomDialog.CheckType.MMHG,
                                MultiBottomDialog.CheckType.KPA -> setNIBP()

                                MultiBottomDialog.CheckType.YICHENG -> {
                                    binding.tvBaijie.text = resources.getString(R.string.yicheng)
                                    binding.clBloodKetones.visibility = View.GONE
                                }

                                MultiBottomDialog.CheckType.BAIJIE -> {
                                    binding.realplayPc300TvChol.visibility = View.VISIBLE
                                    binding.ivBloodfatedit.visibility = View.VISIBLE
                                    realplayPc300TvUa.visibility = View.VISIBLE
                                    ivUaedit.visibility = View.VISIBLE
                                    setImageview(true, ivUa, tvUaname, realplayPc300TvUaUnit)
                                    binding.tvBaijie.text = resources.getString(R.string.baijie)
                                    setImageview(
                                        true,
                                        ivBloodfat,
                                        tvBloodfatname,
                                        realplayPc300TvCholUnit
                                    )
                                    binding.clBloodKetones.visibility = View.GONE
                                }

                                MultiBottomDialog.CheckType.AOAILE -> {


                                    binding.realplayPc300TvChol.visibility = View.GONE
                                    binding.ivBloodfatedit.visibility = View.GONE

                                    setImageview(false, ivUa, tvUaname, realplayPc300TvUaUnit)
                                    realplayPc300TvUa.visibility = View.INVISIBLE
                                    ivUaedit.visibility = View.INVISIBLE
                                    binding.tvBaijie.text = resources.getString(R.string.aoaile)
                                    binding.clBloodKetones.visibility = View.GONE
                                }

                                MultiBottomDialog.CheckType.LEPU -> {
                                    binding.realplayPc300TvChol.visibility = View.VISIBLE
                                    binding.ivBloodfatedit.visibility = View.VISIBLE
                                    realplayPc300TvUa.visibility = View.VISIBLE
                                    ivUaedit.visibility = View.VISIBLE
                                    setImageview(true, ivUa, tvUaname, realplayPc300TvUaUnit)
                                    binding.tvBaijie.text = resources.getString(R.string.lepu)
                                    setImageview(
                                        true,
                                        ivBloodfat,
                                        tvBloodfatname,
                                        realplayPc300TvCholUnit
                                    )
                                    binding.clBloodKetones.visibility = View.VISIBLE
                                }

                                else -> setUnitSp()
                            }
                        }
                    })
            }

            initData()
        }
    }

    override fun onPause() {
        super.onPause()
        App.serial.mAPI?.stopNIBPMeasure() //防止打气时意外退出
    }

    private var nIBPGetMeasureResultEvent: NIBPGetMeasureResultEvent? = null

    @SuppressLint("SetTextI18n")
    @OptIn(InternalCoroutinesApi::class)
    private fun initData() {
        //Spo2值
        observeEvent<SPOGetParamEvent> {
            if (it.nSpO2 != 0 || it.nPR != 0 || it.nPI.toDouble() != 0.0) {
                binding.realplayPc300TvSpo.text = it.nSpO2.toString() + ""
                binding.realplayPc300TvPr.text = it.nPR.toString() + ""
                binding.realplayPc300TvPi.text = it.nPI.toString() + ""
                bProSpO2 = it.bProbe
                mPR = it.nPR
            }

            if (!it.bProbe) { //脱落状态
                binding.spoView.cleanAndRestart()
                //脱落
                isNewSpoData = true
                mSpo2List.clear()
                mPRList.clear()
                mPiList.clear()
                binding.tvResult.text = getString(R.string.spo2_probe_off)
                binding.tvLeadOffStatus.text = getString(R.string.probe_off)
                binding.pbSpo2.progress = 0
            } else { //非脱落状态
                binding.tvLeadOffStatus.text = ""
                if (it.nSpO2 == 0 && it.nPR == 0 && it.nPI.toDouble() == 0.0) {
                    cleanTextViewSPO2()
                }
            }
        }
        //Spo2柱状缓存释放
        observeEvent<SPOGetWaveEvent> {
            binding.drawSpo2Rect.setSPORect(it.waves)
            for (i in it.waves.indices) {
                binding.spoView.addQueueData(it.waves[i].data)
                if (mPR > 0 && it.waves[i].flag == 1) {
                    binding.ivHeartPulse.visibility = View.VISIBLE
                    lifecycleScope.launchWhenResumed {
                        delay((((60 / mPR) * 1000) / 3).toLong())
                        binding.ivHeartPulse.visibility = View.INVISIBLE
                    }
                }
            }
        }
        //血压回调
        observeEvent<NIBPGetMeasureResultEvent> {
            nIBPGetMeasureResultEvent = it
            setNIBP()
            if (it.rank > 0) {
                binding.realplayPc300TvSys.text = "${it.sys}"
                binding.realplayPc300TvDia.text = "${it.dia}"
                binding.realplayPc300TvMap.text = "${it.plus}"
                //  当脉率来自于血氧时，不显示血压脉率
                if (!bProSpO2) {
                    //血氧探头脱落
                    binding.realplayPc300TvPr.text = it.plus.toString()
                }
                //                    设置血压测量状态值：最佳，正常...
                binding.insView.setProgress(it.rank - 1, true)
                val hrNormal = if (getNIBPUnit()) {
                    getString(R.string.hr_noral_nibp)
                } else {
                    getString(R.string.hr_not_noral_nibp)
                }
                val speechMsg = if (getNIBPUnit()) {
                    getString(R.string.measure_result) + (getString(R.string.measure_over) + ", " + getString(
                        R.string.const_sys_text
                    ) + ": "
                            + it.sys + getString(R.string.mmHg_speech) + ", "
                            + getString(R.string.const_dia_text) + ":"
                            + it.dia + getString(R.string.mmHg_speech) + ", "
                            + getString(R.string.const_pr_text) + ":" + getString(R.string.bpm_speech)
                            + binding.realplayPc300TvPr.text + "次" + "," + resources.getStringArray(
                        R.array.nibp_result
                    )[it.rank - 1] + "," + hrNormal)
                } else {
                    getString(R.string.measure_result) + (getString(R.string.measure_over) + ", " + getString(
                        R.string.const_sys_text
                    ) + ": "
                            + changeNibp2kpa(it.sys) + getString(R.string.kpa_speech) + ", "
                            + getString(R.string.const_dia_text) + ":"
                            + changeNibp2kpa(it.dia) + getString(R.string.kpa_speech) + ", "
                            + getString(R.string.const_pr_text) + ":" + getString(R.string.bpm_speech)
                            + binding.realplayPc300TvPr.text + "次" + "," + resources.getStringArray(
                        R.array.nibp_result
                    )[it.rank - 1] + "," + hrNormal)
                }
                binding.tvResult.text = speechMsg //显示结果
            } else {
                binding.realplayPc300TvSys.text = "- -"
                binding.realplayPc300TvDia.text = "- -"
                binding.realplayPc300TvMap.text = "- -"
                binding.insView.setProgress(0, false)
            }
            bNibpStart = false
            binding.btnNibp.setBackgroundResource(R.drawable.start_measure_nibp)

        }

        observeEvent<NIBPGetMeasureErrorEvent> {
            /** 血压测量错误  */
            binding.tvSys.text = resources.getString(R.string.const_sys_text)
            if (it.error > 0) {
                val errMsg: String = resources.getStringArray(R.array.nibp_errors)[it.error - 1]
                binding.tvResult.text = errMsg
            }
            binding.insView.setProgress(0, false)
            binding.realplayPc300TvSys.text = "- -"
            binding.realplayPc300TvDia.text = "- -"
            binding.realplayPc300TvMap.text = "- -"
            bNibpStart = false
            binding.btnNibp.setBackgroundResource(R.drawable.start_measure_nibp)
        }
        observeEvent<NIBPGetRealDataEvent> {
            /** 血压实时数据  */

            binding.tvSys.text = resources.getString(R.string.cuff_pressure)
            binding.insView.setProgress(it.realData, false)
            binding.realplayPc300TvDia.text = "- -"
            binding.realplayPc300TvMap.text = "- -"
            if (!bProSpO2) {
                binding.realplayPc300TvPr.text = "- -"
            }
            if (it.realData != 0) {
                if (!getNIBPUnit()) {
                    binding.realplayPc300TvSys.text = "" + (it.realData * 4 / 30).toFloat()
                } else {
                    binding.realplayPc300TvSys.text = "" + it.realData
                }
            } else {
                binding.realplayPc300TvSys.text = "- -"
            }
        }
        observeEvent<NIBPStopMeasureEvent> {
            binding.tvSys.text = resources.getString(R.string.const_sys_text)
            binding.realplayPc300TvSys.text = "- -"
            bNibpStart = false
            binding.insView.setProgress(0, false)
            binding.btnNibp.setBackgroundResource(R.drawable.start_measure_nibp)
        }

        //体温回调
        observeEvent<GetTMPResult> {
            binding.realplayPc300TvTemp.text = it.strC
            if (it.strC.toFloat() in 32.0f..43.0f) {

                val tempRank = getTempRank(it.strC.toFloat())
                when (it.type) {
                    0 -> {
                        speechMsg =
                            (getString(R.string.measure_over) + ", " + getString(R.string.const_temp_text) + ": " + it.strC
                                    + getString(R.string.const_temp_unit_text_sp)
                                    + "," + arrTempRank!![tempRank])
                    }

                    1 -> {
                        speechMsg = arrTempRank!![0]
                    }

                    2 -> {
                        speechMsg = arrTempRank!![4]
                    }
                }
                binding.tvResult.text = getString(R.string.measure_result) + speechMsg
            } else {
                binding.tvResult.text =
                    getString(R.string.the_measurement_result_is_wrong_please_try_again)
            }
        }
        //血糖回调
        observeEvent<GetGLUResult> {
            val gluNormalType = it.type
            if (gluNormalType == 0) {
                strGLU = it.data  //mmol/L
                gluMgdl = it.unit  //mg/dl
                val isMmol = getGLUUnit()
                binding.realplayPc300TvGlu.text = if (isMmol) it.data else it.unit
                val glurank = getGLURank(java.lang.Float.valueOf(it.data), getGLUMeasureType())
                speechMsg = "${getString(R.string.measure_over)}, ${arrGluRank!![glurank]}"
                if (glurank == 3) { //血糖过低，超出测量范围
                    binding.realplayPc300TvGlu.text = "L"
                } else if (glurank == 4) { //血糖过高，超出测量范围
                    binding.realplayPc300TvGlu.text = "H"
                }
                if (glurank != 3 && glurank != 4)
                    speechMsg =
                        "${getString(R.string.measure_over)}, ${getString(R.string.gluname)}: ${if (isMmol) it.data else it.unit}${
                            getString(
                                if (isMmol) R.string.const_mmol_speech else R.string.const_mgdl_speech
                            )
                        }, ${arrGluRank!![glurank]}"

            } else if (gluNormalType == 1) {
                binding.realplayPc300TvGlu.text = "L"
                speechMsg = getString(R.string.measure_over) + arrGluRank!![3]
            } else if (gluNormalType == 2) {
                speechMsg = getString(R.string.measure_over) + arrGluRank!![4]
                binding.realplayPc300TvGlu.text = "H"
            }
            binding.tvResult.text = getString(R.string.measure_result) + speechMsg
        }
        //尿酸
        observeEvent<GetUAResult> {
            var valuef = it.data
            var unit = it.unit
            if (getGluDeviceType() == 3) { //乐普诊断 umol/L
                valuef = it.data / 10 // 通用解析已经除了2次10
                unit = 0
            }

            strUA = String.format(Locale.US, "%.2f", valuef / if (unit == 0) 1f else 16.81f)
            uaMgdl = String.format(Locale.US, "%.2f", valuef * if (unit == 0) 16.81f else 1f)
            if (getGLUUnit()) {
                binding.realplayPc300TvUa.text = strUA
            } else {
                binding.realplayPc300TvUa.text = uaMgdl
            }

            val uaRank =
                getUARank(getSexAndAge(curPatient.identityno), java.lang.Float.valueOf(strUA!!))
            speechMsg = getString(R.string.measure_over) + ", " + arrUARank!![uaRank]
            if (uaRank == 3) { //过低，超出测量范围
                binding.realplayPc300TvUa.text = "L"
                if (isCheckExceptionWaringTone()) {
                    playMp3(Constant.MP3_WARNING, false, requireActivity())
                }
            } else if (uaRank == 4) { //过高，超出测量范围
                binding.realplayPc300TvUa.text = "H"
            }
            if (it.type == 0 && uaRank == 0 || uaRank == 1 || uaRank == 2) {//正常，或者偏高，偏低
                speechMsg = if (getGLUUnit()) {
                    (getString(R.string.measure_over) + ", " + (getString(R.string.uaname) + ": " + strUA + getString(
                        R.string.const_mmol_speech
                    )) + ", " + arrUARank!![uaRank])
                } else {
                    (getString(R.string.measure_over) + ", " + getString(R.string.uaname) + ": " + uaMgdl + getString(
                        R.string.const_mgdl_speech
                    ) + ", " + arrUARank!![uaRank])
                }
            }
            binding.tvResult.text = getString(R.string.measure_result) + speechMsg
        }
        //总胆固醇
        observeEvent<GetCHOLResult> {
//                0:怡成，1:百捷 ,2:爱奥乐 3.乐普诊断
            when (getGluDeviceType()) {
                1, 2 -> {
                    //存储
                    val bean = CHOL()
                    if (it.unit == 0) { // mmol/L
                        bean.mmol = it.data.toString()
                        cholMgdl =
                            String.format(
                                Locale.US,
                                "%.2f",
                                it.data * 38.67f
                            )
                        bean.mgdl = cholMgdl.toString()
                        strCHOL = bean.mmol

                    } else if (it.unit == 1) { // mg/dL
                        val cholMmol =
                            String.format(
                                Locale.US,
                                "%.2f",
                                it.data / 38.67f
                            )
                        bean.mmol = cholMmol
                        strCHOL = bean.mmol
                        cholMgdl = it.data.toString()
                        bean.mgdl = cholMgdl
                    } else {
                        binding.tvResult.text =
                            getString(R.string.the_measurement_result_is_wrong_please_try_again)
                        return@observeEvent
                    }
                    if (getGLUUnit()) {
                        binding.realplayPc300TvChol.text = bean.mmol
                    } else {
                        binding.realplayPc300TvChol.text = bean.mgdl
                    }
                    val cholrank = getCHOLRank(bean.mmol.toFloat())
                    if (cholrank == 2) { //过低，超出测量范围
                        binding.realplayPc300TvChol.text = "L"
                    } else if (cholrank == 3) { //过高，超出测量范围
                        binding.realplayPc300TvChol.text = "H"
                    }
                    if (cholrank == 0 || cholrank == 1) {
                        speechMsg = if (getGLUUnit()) {
                            (getString(R.string.measure_over) + ", " + getString(R.string.totalcholesterol) + ": " +
                                    bean.mmol + getString(R.string.const_mmol_speech)
                                    + ", " + arrCholRank!![cholrank])
                        } else {
                            (getString(R.string.measure_over) + ", " + getString(R.string.totalcholesterol) + ": " +

                                    bean.mgdl + getString(R.string.const_mgdl_speech)
                                    + ", " + arrCholRank!![cholrank])
                        }
                    } else if (cholrank == 2) {
                        speechMsg = getString(R.string.measure_over) + ", " + arrCholRank!![2]
                    } else if (cholrank == 3) {
                        speechMsg = getString(R.string.measure_over) + ", " + arrCholRank!![3]
                    }
                }
                //血酮
                3 -> {
                    //乐普诊断时为血酮
                    if (it.unit == 0) { // mmol/L
                        strBK = it.data.toString()
                        bkMgdl = String.format(Locale.US, "%.2f", it.data * 10.04f) //保留2位小数

                    } else {
                        bkMgdl = it.data.toString()
                        strBK = String.format(Locale.US, "%.2f", it.data / 10.04f)


                    }
                    if (getGLUUnit()) {
                        binding.realplayPc300TvBloodKetones.text = strBK
                    } else {
                        binding.realplayPc300TvBloodKetones.text = bkMgdl
                    }


                    if (!TextUtils.isEmpty(strBK)) {
                        val bkRank = getBKRank(java.lang.Float.valueOf(strBK!!))
                        speechMsg =
                            (getString(R.string.measure_over) + ", 血酮: " + arrBKRank!![bkRank])
                        if (bkRank == 2) { //过低，超出测量范围
                            binding.realplayPc300TvBloodKetones.text = "L"
                        } else if (bkRank == 3) { //过高，超出测量范围
                            binding.realplayPc300TvBloodKetones.text = "H"
                        }
                    }
                }
            }
            binding.tvResult.text = getString(R.string.measure_result) + speechMsg
        }
    }

    private fun cleanTextViewSPO2() {
        binding.realplayPc300TvSpo.text = "- -"
        binding.realplayPc300TvPr.text = "- -"
        binding.realplayPc300TvPi.text = "- -"
    }
    //-------------------- 血氧点测 ---------------------
    /**
     * 血氧点测,记录保存数据的最大值
     */
    private val sopCnt = 20
    private var mSpo2List: ArrayList<Int> = ArrayList()
    private var mPRList: ArrayList<Int> = ArrayList()
    private var mPiList: ArrayList<Int> = ArrayList()
    private var isNewSpoData = true
    private fun saveSPOData(nSpO2: Int, nPR: Int, nPI: Float) {
        if (nSpO2 <= 0 || nPR <= 0 || nPI <= 0) return
        if (isNewSpoData) {
            mSpo2List.add(nSpO2)
            mPRList.add(nPR)
            mPiList.add((nPI * 10).toInt())
            binding.pbSpo2.visibility = View.VISIBLE
            binding.pbSpo2.progress = mSpo2List.size * 5
            if (mSpo2List.size >= sopCnt) { //显示点测结果
                var spoSum = 0
                var prSum = 0
                var piSum = 0
                for (i in mSpo2List.indices) {
                    spoSum += mSpo2List[i]
                    prSum += mPRList[i]
                    piSum += mPiList[i]
                }
                //血氧点测平均值
                val avgSpo2 = spoSum / mSpo2List.size
                val avgPr = prSum / mSpo2List.size
                val iAvgPi = piSum / mSpo2List.size
                val avgPi = iAvgPi / 10f
                App.isRoutineExamination = true
                val spo2Rank = getSpO2Rank(avgSpo2)
                val prRank = getPRRank(avgPr)
                isNewSpoData = false
                mSpo2List.clear()
                mPRList.clear()
                mPiList.clear()
                val arrSpo2Rank = resources.getStringArray(R.array.spo2_rank)
                val arrPrRank = resources.getStringArray(R.array.pr_rank)
                val speechMsg =
                    getString(R.string.measure_result) + (getString(R.string.measure_over) + ", " + getString(
                        R.string.const_spo2_text
                    ) + avgSpo2 + "%"
                            + "," + arrSpo2Rank[spo2Rank] + ", "
                            + getString(R.string.const_pr_text) + ": " + getString(R.string.bpm_speech) + avgPr + "次" + ","
                            + arrPrRank[prRank])
                binding.tvResult.text = speechMsg //显示结果
            }
        }
    }

    /**
     * ------------   TextView 单位转换   ----------------------------------
     */
    private fun setNIBP() {
        if (getNIBPUnit()) {
            binding.realplayPc300TvNibpUnit.text = getString(R.string.const_nibp_text_unit)
        } else {
            binding.realplayPc300TvNibpUnit.text = getString(R.string.const_nibp_text_unit_kpa)
        }
        if (nIBPGetMeasureResultEvent == null) {
            return
        }
        val sysKpa = changeNibp2kpa(nIBPGetMeasureResultEvent!!.sys)
        val diaKpa = changeNibp2kpa(nIBPGetMeasureResultEvent!!.dia)
        val mapKpa = changeNibp2kpa(nIBPGetMeasureResultEvent!!.map)

        binding.tvSys.text = resources.getString(R.string.const_sys_text)
        if (getNIBPUnit()) {
            if (nIBPGetMeasureResultEvent!!.sys == 0 || nIBPGetMeasureResultEvent!!.dia == 0 || nIBPGetMeasureResultEvent!!.map == 0) {
                binding.realplayPc300TvSys.text = "- -"
                binding.realplayPc300TvDia.text = "- -"
                binding.realplayPc300TvMap.text = "- -"
            } else {
                binding.realplayPc300TvSys.text = nIBPGetMeasureResultEvent!!.sys.toString()
                binding.realplayPc300TvDia.text = nIBPGetMeasureResultEvent!!.dia.toString()
                binding.realplayPc300TvMap.text = nIBPGetMeasureResultEvent!!.map.toString()
            }
        } else {

            if (sysKpa == 0.0f || diaKpa == 0.0f || mapKpa == 0.0f) {
                binding.realplayPc300TvSys.text = "- -"
                binding.realplayPc300TvDia.text = "- -"
                binding.realplayPc300TvMap.text = "- -"
            } else {
                binding.realplayPc300TvSys.text = sysKpa.toString()
                binding.realplayPc300TvDia.text = diaKpa.toString()
                binding.realplayPc300TvMap.text = mapKpa.toString()
            }
        }
    }

    private fun setGLU() {
        if (getGLUUnit()) { //切换单位
            binding.realplayPc300TvGluUnit.text = getString(R.string.const_mmol_unit_text)
        } else {
            binding.realplayPc300TvGluUnit.text = getString(R.string.const_mgdl_unit_text)
        }
        //切换数值
        if (getGLUUnit()) {
            if (!TextUtils.isEmpty(strGLU) && strGLU != "0.0") {
                if (!"LH".contains(binding.realplayPc300TvGlu.text)) {
                    binding.realplayPc300TvGlu.text = strGLU
                }
            }
        } else {
            if (!TextUtils.isEmpty(gluMgdl) && strGLU != "0.0") {
                if (!"LH".contains(binding.realplayPc300TvGlu.text)) {
                    binding.realplayPc300TvGlu.text = gluMgdl
                }
            }
        }
    }

    private fun setUA() {
        if (getGLUUnit()) { //切换单位
            binding.realplayPc300TvUaUnit.text = getString(R.string.const_mmol_unit_text)
        } else {
            binding.realplayPc300TvUaUnit.text = getString(R.string.const_mgdl_unit_text)
        }
        //切换数值
        if (getGLUUnit()) {
            if (!TextUtils.isEmpty(strUA) && strUA != "0.0") {
                if (!"LH".contains(binding.realplayPc300TvUa.text)) {
                    binding.realplayPc300TvUa.text = strUA
                }
            }
        } else {
            if (!TextUtils.isEmpty(uaMgdl) && uaMgdl != "0.0") {
                if (!"LH".contains(binding.realplayPc300TvUa.text)) {
                    binding.realplayPc300TvUa.text = uaMgdl
                }
            }
        }
    }

    private fun setCHOL() {
        if (getGLUUnit()) { //切换单位
            binding.realplayPc300TvCholUnit.text = getString(R.string.const_mmol_unit_text)
        } else {
            binding.realplayPc300TvCholUnit.text = getString(R.string.const_mgdl_unit_text)
        }

        if (getGLUUnit()) {
            if (!TextUtils.isEmpty(strCHOL) && strCHOL != "0.0") {
                if (!"LH".contains(binding.realplayPc300TvChol.text)) {
                    binding.realplayPc300TvChol.text = strCHOL
                }
            }
        } else {
            if (!TextUtils.isEmpty(cholMgdl) && cholMgdl != "0.0") {
                if (!"LH".contains(binding.realplayPc300TvChol.text)) {
                    binding.realplayPc300TvChol.text = cholMgdl
                }
            }
        }
    }

    private fun setBK() {
        if (getGLUUnit()) { //切换单位
            binding.realplayPc300TvBloodKetonesUnit.text = getString(R.string.const_mmol_unit_text)
        } else {
            binding.realplayPc300TvBloodKetonesUnit.text = getString(R.string.const_mgdl_unit_text)
        }
        //切换数值
        if (getGLUUnit()) {
            if (!TextUtils.isEmpty(strBK) && strBK != "0.0") {
                if (!"LH".contains(binding.realplayPc300TvBloodKetones.text)) {
                    binding.realplayPc300TvBloodKetones.text = strBK
                }
            }
        } else {
            if (!TextUtils.isEmpty(bkMgdl) && bkMgdl != "0.0") {
                if (!"LH".contains(binding.realplayPc300TvBloodKetones.text)) {
                    binding.realplayPc300TvBloodKetones.text = bkMgdl
                }
            }
        }
    }

    private fun setUnitSp() {
        with(binding) {
            when (getGLUMeasureType()) {
                0 -> rgMeal.check(R.id.rb_before_meal)
                1 -> rgMeal.check(R.id.rb_after_meal)
            }
            val bGLUUnitMmol = getGLUUnit()
            //mmol单位设置
            if (bGLUUnitMmol) {
                realplayPc300TvGluUnit.text = getString(R.string.const_mmol_unit_text)
                realplayPc300TvUaUnit.text = getString(R.string.const_mmol_unit_text)
                realplayPc300TvCholUnit.text = getString(R.string.const_mmol_unit_text)
                realplayPc300TvBloodKetonesUnit.text = getString(R.string.const_mmol_unit_text)

            } else {
                realplayPc300TvGluUnit.text = getString(R.string.const_mgdl_unit_text)
                realplayPc300TvUaUnit.text = getString(R.string.const_mgdl_unit_text)
                realplayPc300TvCholUnit.text = getString(R.string.const_mgdl_unit_text)
                realplayPc300TvBloodKetonesUnit.text = getString(R.string.const_mgdl_unit_text)
            }
            //百捷奥爱乐
            val initGluMode = getGluDeviceType()
            binding.tvBaijie.text = getString(
                when (initGluMode) {
                    0 -> R.string.yicheng
                    1 -> R.string.baijie
                    2 -> R.string.aoaile
                    else -> R.string.lepu
                }
            )

            when (initGluMode) {
                1 -> {
                    binding.realplayPc300TvChol.visibility = View.VISIBLE
                    binding.ivBloodfatedit.visibility = View.VISIBLE
                    realplayPc300TvUa.visibility = View.VISIBLE
                    ivUaedit.visibility = View.VISIBLE
                    setImageview(true, ivUa, tvUaname, realplayPc300TvUaUnit)
                    binding.tvBaijie.text = resources.getString(R.string.baijie)
                    setImageview(true, ivBloodfat, tvBloodfatname, realplayPc300TvCholUnit)

                }

                2 -> {
                    binding.realplayPc300TvChol.visibility = View.GONE
                    binding.ivBloodfatedit.visibility = View.GONE
                    binding.tvBaijie.text = resources.getString(R.string.aoaile)
                    setImageview(false, ivUa, tvUaname, realplayPc300TvUaUnit)
                    realplayPc300TvUa.visibility = View.INVISIBLE
                    ivUaedit.visibility = View.INVISIBLE
                }
            }
            val bNIBPUnit = getNIBPUnit()
            //血压单位设置
            if (bNIBPUnit) {
                realplayPc300TvNibpUnit.setText(R.string.const_nibp_text_unit)
            } else {
                realplayPc300TvNibpUnit.setText(R.string.const_nibp_text_unit_kpa)
            }
            //温度模式


            val tempMode = getString(
                when (getTempMode()) {
                    1 -> R.string.erwen
                    2, 3 -> R.string.ewen
                    else -> R.string.wuwen
                }
            )
            if (tempMode != binding.tvTempMode.text) {
                binding.realplayPc300TvTemp.text = "- -"
                binding.tvTempMode.text = tempMode
            }
        }
    }

    private fun setImageview(
        isShow: Boolean,
        imageview: ImageView,
        tvName: TextView,
        tvUnit: TextView
    ) {
        val matrix = ColorMatrix()
        if (isShow) {
            matrix.setSaturation(1f) //原色

            tvName.setTextColor(resources.getColor(R.color.color666666, null))
            tvUnit.setTextColor(
                resources.getColor(
                    R.color.color666666,
                    null
                )
            )
        } else {
            matrix.setSaturation(0f) //灰色

            tvName.setTextColor(resources.getColor(R.color.colorcacaca, null))
            tvUnit.setTextColor(
                resources.getColor(
                    R.color.colorcacaca,
                    null
                )
            )
        }
        val filter = ColorMatrixColorFilter(matrix)
        imageview.colorFilter = filter


    }

    override fun onStop() {
        super.onStop()
        binding.drawSpo2Rect.setStartOrStop(true)
        binding.spoView.setStartOrStop(false)
    }


    override fun onResume() {
        super.onResume()
        if (!binding.spoView.startOrStop) {
            binding.spoView.setScope(150, 0)
            binding.spoView.Start()
            binding.spoView.cleanAndRestart()
        }
        if (binding.drawSpo2Rect.startOrStop) {
            binding.drawSpo2Rect.setStartOrStop(false)
            binding.drawSpo2Rect.invalidate()
        }

        App.isRoutineExamination = false
        lifecycleScope.launch {
            delay(2000)
            LogUtil.json("===============onResume")
            when (getGluDeviceType()) {
                0 -> App.serial.mAPI?.setGluType(1)
                1 -> App.serial.mAPI?.setGluType(2)
                2 -> App.serial.mAPI?.setGluType(3)
                3 -> App.serial.mAPI?.setGluType(4)
            }
        }
    }


    /**
     * 获取血氧等级
     * 0:血氧正常 
     * 1:血氧值偏低，请注意休息。 
     * 2:血氧值过低，请咨询医生。 
     */
    private fun getSpO2Rank(spo2: Int): Int {
        return when {
            spo2 > 95 -> 0
            spo2 in 91..95 -> 1
            spo2 <= 90 -> 2
            else -> 0
        }
    }

    /**
     * 获取脉率等级
     * 0:脉率正常
     * 1:脉率偏低，请注意休息。 
     * 2:脉率过低，请咨询医生。 
     * 3:脉率偏高，请注意休息。 
     * 4:脉率过高，请咨询医生。
     */
    private fun getPRRank(pr: Int): Int {
        return when {
            pr in 61..99 -> 0
            pr <= 50 -> 2
            pr <= 60 -> 1
            pr >= 120 -> 4
            pr >= 100 -> 3
            else -> 0
        }
    }
}


