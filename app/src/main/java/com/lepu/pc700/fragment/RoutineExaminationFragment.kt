package com.lepu.pc700.fragment

import android.annotation.SuppressLint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.port.GetBKResult
import com.Carewell.ecg700.port.GetCHOLResult
import com.Carewell.ecg700.port.GetGLUResult
import com.Carewell.ecg700.port.GetTMPResult
import com.Carewell.ecg700.port.GetUAResult
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.ecg700.port.NIBPGetMeasureErrorEvent
import com.Carewell.ecg700.port.NIBPGetMeasureResultEvent
import com.Carewell.ecg700.port.NIBPGetRealDataEvent
import com.Carewell.ecg700.port.NIBPStopMeasureEvent
import com.Carewell.ecg700.port.SPOGetParamEvent
import com.Carewell.ecg700.port.SPOGetWaveEvent
import com.Carewell.ecg700.port.observeEvent
import com.lepu.pc700.App
import com.lepu.pc700.MainActivity
import com.lepu.pc700.R
import com.lepu.pc700.databinding.FragmentRoutineexaminationBinding
import com.lepu.pc700.dialog.MultiBottomDialog
import com.lepu.pc700.singleClick
import com.lepu.pc700.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
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

    private var speechMsg = ""
    private var bpUnit = true//true为mmHg false为kpa
    private var gluUnit = true//true为mmol/L false为mg/dl
    private var gluType = 0// 0：空腹，1：餐后2小时
    private var gluDeviceType = 0// 0:怡成，1:百捷 ,2:爱奥乐,3:乐普
    private var tempMode = 1// 1 表示耳温模式；2 表示成人额温模式； 3 表示儿童额温模式；4 表示物温模式

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).setMainTitle("常规检测")
        //0：科瑞康血压模块 ,1：景新浩血压
        App.serial.mAPI?.setPressureMode(1)  //咨询销售确认设备模块供应商
        App.serial.mAPI?.setGluType(gluDeviceType + 1)
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
            rbAfterMeal.singleClick { gluType = 1 }
            rbBeforeMeal.singleClick { gluType = 0 }
            clUrine.singleClick { findNavController().navigate(R.id.urineFragment) }
            llBloodFat.singleClick { findNavController().navigate(R.id.bloodFragment) }

            fab.singleClick {
                val multiDialog = MultiBottomDialog()
                multiDialog.show(childFragmentManager, "MultiCheckDialog")
                multiDialog.setOKClickListener(
                    object : MultiBottomDialog.IOKClickListener {
                        override fun onClickCallback(checkType: MultiBottomDialog.CheckType?) {
                            when (checkType) {
                                MultiBottomDialog.CheckType.MMOL,
                                MultiBottomDialog.CheckType.MGDL -> {
                                    gluUnit = checkType == MultiBottomDialog.CheckType.MMOL
                                    setGLU()
                                    setUA()
                                    setCHOL()
                                    setBK()
                                }

                                MultiBottomDialog.CheckType.MMHG,
                                MultiBottomDialog.CheckType.KPA -> {
                                    bpUnit = checkType == MultiBottomDialog.CheckType.MMHG
                                    setNIBP()
                                }

                                MultiBottomDialog.CheckType.YICHENG -> {
                                    gluDeviceType = 0
                                    binding.tvBaijie.text = resources.getString(R.string.yicheng)
                                    binding.clBloodKetones.visibility = View.GONE
                                }

                                MultiBottomDialog.CheckType.BAIJIE -> {
                                    gluDeviceType = 1
                                    binding.realplayPc300TvChol.visibility = View.VISIBLE
                                    realplayPc300TvUa.visibility = View.VISIBLE
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
                                    gluDeviceType = 2
                                    binding.realplayPc300TvChol.visibility = View.GONE
                                    setImageview(false, ivUa, tvUaname, realplayPc300TvUaUnit)
                                    realplayPc300TvUa.visibility = View.INVISIBLE
                                    binding.tvBaijie.text = resources.getString(R.string.aoaile)
                                    binding.clBloodKetones.visibility = View.GONE
                                }

                                MultiBottomDialog.CheckType.LEPU -> {
                                    gluDeviceType = 3
                                    binding.realplayPc300TvChol.visibility = View.VISIBLE
                                    realplayPc300TvUa.visibility = View.VISIBLE
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

                                MultiBottomDialog.CheckType.ER_TEMP -> {
                                    tempMode = 1
                                    setTempUnit()
                                }

                                MultiBottomDialog.CheckType.E_TEMP -> {
                                    tempMode = 2
                                    setTempUnit()
                                }

                                MultiBottomDialog.CheckType.CHILD_TEMP -> {
                                    tempMode = 3
                                    setTempUnit()
                                }

                                MultiBottomDialog.CheckType.OGJECT_TEMP -> {
                                    tempMode = 4
                                    setTempUnit()
                                }

                                else -> {}
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
            LogUtil.e(it.toJson())
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
            saveSPOData(it.nSpO2, it.nPR, it.nPI)
        }
        //Spo2柱状缓存释放
        observeEvent<SPOGetWaveEvent> {
            LogUtil.e(it.toJson())
            binding.drawSpo2Rect.setSPORect(it.waves)
            for (i in it.waves.indices) {
                binding.spoView.addQueueData(it.waves[i].data)
                if (mPR > 0 && it.waves[i].flag == 1) {
                    binding.ivHeartPulse.visibility = View.VISIBLE
                    launchWhenResumed {
                        delay((((60 / mPR) * 1000) / 3).toLong())
                        binding.ivHeartPulse.visibility = View.INVISIBLE
                    }
                }
            }
        }
        //血压
        observeEvent<NIBPGetMeasureResultEvent> {
            LogUtil.e(it.toJson())
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
                // 设置血压测量状态值：最佳，正常...
                binding.insView.setProgress(it.rank - 1, true)
                val hrNormal = if (bpUnit) {
                    getString(R.string.hr_noral_nibp)
                } else {
                    getString(R.string.hr_not_noral_nibp)
                }
                val speechMsg = if (bpUnit) {
                    getString(R.string.measure_result) + (getString(R.string.measure_over) + ", " + getString(
                        R.string.const_sys_text
                    ) + ": " + it.sys + getString(R.string.mmHg_speech) + ", " + getString(R.string.const_dia_text) + ":" + it.dia + getString(
                        R.string.mmHg_speech
                    ) + ", " + getString(R.string.const_pr_text) + ":" + getString(R.string.bpm_speech) + binding.realplayPc300TvPr.text + "次" + "," + resources.getStringArray(
                        R.array.nibp_result
                    )[it.rank - 1] + "," + hrNormal)
                } else {
                    getString(R.string.measure_result) + (getString(R.string.measure_over) + ", " + getString(
                        R.string.const_sys_text
                    ) + ": " + changeNibp2kpa(it.sys) + getString(R.string.kpa_speech) + ", " + getString(
                        R.string.const_dia_text
                    ) + ":" + changeNibp2kpa(it.dia) + getString(R.string.kpa_speech) + ", " + getString(
                        R.string.const_pr_text
                    ) + ":" + getString(R.string.bpm_speech) + binding.realplayPc300TvPr.text + "次" + "," + resources.getStringArray(
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

        /** 血压测量错误  */
        observeEvent<NIBPGetMeasureErrorEvent> {
            LogUtil.e(it.toJson())
            binding.tvSys.text = resources.getString(R.string.const_sys_text)
            if (it.error > 0) {
                val errMsg = resources.getStringArray(R.array.nibp_errors)[it.error - 1]
                binding.tvResult.text = errMsg
            }
            binding.insView.setProgress(0, false)
            binding.realplayPc300TvSys.text = "- -"
            binding.realplayPc300TvDia.text = "- -"
            binding.realplayPc300TvMap.text = "- -"
            bNibpStart = false
            binding.btnNibp.setBackgroundResource(R.drawable.start_measure_nibp)
        }
        /** 血压实时数据  */
        observeEvent<NIBPGetRealDataEvent> {
            LogUtil.e(it.toJson())
            binding.tvSys.text = resources.getString(R.string.cuff_pressure)
            binding.insView.setProgress(it.realData, false)
            binding.realplayPc300TvDia.text = "- -"
            binding.realplayPc300TvMap.text = "- -"
            if (!bProSpO2) {
                binding.realplayPc300TvPr.text = "- -"
            }
            if (it.realData != 0) {
                if (!bpUnit) {
                    binding.realplayPc300TvSys.text = "" + (it.realData * 4 / 30).toFloat()
                } else {
                    binding.realplayPc300TvSys.text = "" + it.realData
                }
            } else {
                binding.realplayPc300TvSys.text = "- -"
            }
        }
        observeEvent<NIBPStopMeasureEvent> {
            LogUtil.e(it.toJson())
            binding.tvSys.text = resources.getString(R.string.const_sys_text)
            binding.realplayPc300TvSys.text = "- -"
            bNibpStart = false
            binding.insView.setProgress(0, false)
            binding.btnNibp.setBackgroundResource(R.drawable.start_measure_nibp)
        }

        //体温
        observeEvent<GetTMPResult> {
            LogUtil.e(it.toJson())
            binding.realplayPc300TvTemp.text = it.strC
            if (it.strC.toFloat() in 32.0f..43.0f) {
                val arrTempRank = resources.getStringArray(R.array.temp_rank)
                val tempRank = getTempRank(it.strC.toFloat())
                speechMsg = when (it.type) {
                    0 -> getString(R.string.measure_over) + ", " + getString(R.string.const_temp_text) + ": " + it.strC + getString(
                        R.string.const_temp_unit_text_sp
                    ) + "," + arrTempRank[tempRank]

                    1 -> arrTempRank[0]
                    else -> arrTempRank[4]
                }
                binding.tvResult.text = getString(R.string.measure_result) + speechMsg
            } else {
                binding.tvResult.text =
                    getString(R.string.the_measurement_result_is_wrong_please_try_again)
            }
        }
        //血糖
        observeEvent<GetGLUResult> {
            LogUtil.e(it.toJson())
            val gluNormalType = it.type
            val arrGluRank = resources.getStringArray(R.array.glu_rank)
            when (gluNormalType) {
                0 -> {
                    strGLU = it.mmol  //mmol/L
                    gluMgdl = it.mgdl  //mg/dl
                    binding.realplayPc300TvGlu.text = if (gluUnit) it.mmol else it.mgdl
                    val gluRank = getGLURank(it.mmol.toFloat(), gluType)
                    speechMsg = "${getString(R.string.measure_over)}, ${arrGluRank[gluRank]}"
                    when (gluRank) {
                        3 -> binding.realplayPc300TvGlu.text = "L" //血糖过低，超出测量范围
                        4 -> binding.realplayPc300TvGlu.text = "H"  //血糖过高，超出测量范围
                        else -> speechMsg =
                            "${getString(R.string.measure_over)}, ${getString(R.string.gluname)}: ${if (gluUnit) it.mmol else it.mgdl}${
                                getString(if (gluUnit) R.string.const_mmol_speech else R.string.const_mgdl_speech)
                            }, ${arrGluRank[gluRank]}"
                    }
                }

                1 -> {
                    binding.realplayPc300TvGlu.text = "L"
                    speechMsg = getString(R.string.measure_over) + arrGluRank[3]
                }

                2 -> {
                    speechMsg = getString(R.string.measure_over) + arrGluRank[4]
                    binding.realplayPc300TvGlu.text = "H"
                }
            }
            binding.tvResult.text = getString(R.string.measure_result) + speechMsg
        }
        //尿酸
        observeEvent<GetUAResult> {
            LogUtil.e(it.toJson())
            val arrUARank = resources.getStringArray(R.array.ua_rank)
//            var valuef = it.data
//            var unit = it.unit
//            if (gluDeviceType == 3) { //乐普诊断 umol/L
//                valuef = it.data / 10 // 通用解析已经除了2次10
//                unit = 0
//            }
            strUA = it.mmol
            uaMgdl = it.mgdl
            if (gluUnit) {
                binding.realplayPc300TvUa.text = strUA
            } else {
                binding.realplayPc300TvUa.text = uaMgdl
            }
            val uaRank = getUARank(0, strUA?.toFloat() ?: 0f)
            speechMsg = getString(R.string.measure_over) + ", " + arrUARank[uaRank]
            if (uaRank == 3) { //过低，超出测量范围
                binding.realplayPc300TvUa.text = "L"
            } else if (uaRank == 4) { //过高，超出测量范围
                binding.realplayPc300TvUa.text = "H"
            }
            if (it.type == 0 && uaRank == 0 || uaRank == 1 || uaRank == 2) {//正常，或者偏高，偏低
                speechMsg = if (gluUnit) {
                    (getString(R.string.measure_over) + ", " + (getString(R.string.uaname) + ": " + strUA + getString(
                        R.string.const_mmol_speech
                    )) + ", " + arrUARank[uaRank])
                } else {
                    (getString(R.string.measure_over) + ", " + getString(R.string.uaname) + ": " + uaMgdl + getString(
                        R.string.const_mgdl_speech
                    ) + ", " + arrUARank[uaRank])
                }
            }
            binding.tvResult.text = getString(R.string.measure_result) + speechMsg
        }
        //总胆固醇
        observeEvent<GetCHOLResult> {
            LogUtil.e(it.toJson())
            val arrCholRank = resources.getStringArray(R.array.chol_rank)
            //存储
            strCHOL = it.mmol
            cholMgdl = it.mgdl
            if (gluUnit) {
                binding.realplayPc300TvChol.text = strCHOL
            } else {
                binding.realplayPc300TvChol.text = cholMgdl
            }
            val cholrank = getCHOLRank(strCHOL?.toFloat() ?: 0f)
            if (cholrank == 2) { //过低，超出测量范围
                binding.realplayPc300TvChol.text = "L"
            } else if (cholrank == 3) { //过高，超出测量范围
                binding.realplayPc300TvChol.text = "H"
            }
            speechMsg = when (cholrank) {
                0, 1 -> if (gluUnit) {
                    (getString(R.string.measure_over) + ", " + getString(R.string.totalcholesterol) + ": " + strCHOL + getString(
                        R.string.const_mmol_speech
                    ) + ", " + arrCholRank[cholrank])
                } else {
                    (getString(R.string.measure_over) + ", " + getString(R.string.totalcholesterol) + ": " + cholMgdl + getString(
                        R.string.const_mgdl_speech
                    ) + ", " + arrCholRank[cholrank])
                }

                2 -> getString(R.string.measure_over) + ", " + arrCholRank[2]
                else -> getString(R.string.measure_over) + ", " + arrCholRank[3]
            }
            binding.tvResult.text = getString(R.string.measure_result) + speechMsg
        }
        //血酮
        observeEvent<GetBKResult> {
            LogUtil.e(it.toJson())
            //乐普诊断时为血酮
            bkMgdl = it.mgdl
            strBK = it.mmol
            if (gluUnit) {
                binding.realplayPc300TvBloodKetones.text = strBK
            } else {
                binding.realplayPc300TvBloodKetones.text = bkMgdl
            }

            if (!TextUtils.isEmpty(strBK)) {
                val arrBKRank = resources.getStringArray(R.array.bk_rank)
                val bkRank = getBKRank(strBK?.toFloat() ?: 0f)
                speechMsg =
                    (getString(R.string.measure_over) + ", 血酮: " + arrBKRank[bkRank])
                if (bkRank == 2) { //过低，超出测量范围
                    binding.realplayPc300TvBloodKetones.text = "L"
                } else if (bkRank == 3) { //过高，超出测量范围
                    binding.realplayPc300TvBloodKetones.text = "H"
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
        if (bpUnit) {
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
        if (bpUnit) {
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
        if (gluUnit) { //切换单位
            binding.realplayPc300TvGluUnit.text = getString(R.string.const_mmol_unit_text)
        } else {
            binding.realplayPc300TvGluUnit.text = getString(R.string.const_mgdl_unit_text)
        }
        //切换数值
        if (gluUnit) {
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
        if (gluUnit) { //切换单位
            binding.realplayPc300TvUaUnit.text = getString(R.string.const_mmol_unit_text)
        } else {
            binding.realplayPc300TvUaUnit.text = getString(R.string.const_mgdl_unit_text)
        }
        //切换数值
        if (gluUnit) {
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
        if (gluUnit) { //切换单位
            binding.realplayPc300TvCholUnit.text = getString(R.string.const_mmol_unit_text)
        } else {
            binding.realplayPc300TvCholUnit.text = getString(R.string.const_mgdl_unit_text)
        }

        if (gluUnit) {
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
        if (gluUnit) { //切换单位
            binding.realplayPc300TvBloodKetonesUnit.text = getString(R.string.const_mmol_unit_text)
        } else {
            binding.realplayPc300TvBloodKetonesUnit.text = getString(R.string.const_mgdl_unit_text)
        }
        //切换数值
        if (gluUnit) {
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

    private fun setTempUnit() {
        //温度模式
        val tempMode = getString(
            when (tempMode) {
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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setUnitSp() {
        with(binding) {
            when (gluType) {
                0 -> rgMeal.check(R.id.rb_before_meal)
                1 -> rgMeal.check(R.id.rb_after_meal)
            }
            val bGLUUnitMmol = gluUnit
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
            val initGluMode = gluDeviceType
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
                    realplayPc300TvUa.visibility = View.VISIBLE
                    setImageview(true, ivUa, tvUaname, realplayPc300TvUaUnit)
                    binding.tvBaijie.text = resources.getString(R.string.baijie)
                    setImageview(true, ivBloodfat, tvBloodfatname, realplayPc300TvCholUnit)
                }

                2 -> {
                    binding.realplayPc300TvChol.visibility = View.GONE
                    binding.tvBaijie.text = resources.getString(R.string.aoaile)
                    setImageview(false, ivUa, tvUaname, realplayPc300TvUaUnit)
                    realplayPc300TvUa.visibility = View.INVISIBLE
                }
            }
            val bNIBPUnit = bpUnit
            //血压单位设置
            if (bNIBPUnit) {
                realplayPc300TvNibpUnit.setText(R.string.const_nibp_text_unit)
            } else {
                realplayPc300TvNibpUnit.setText(R.string.const_nibp_text_unit_kpa)
            }
            //温度模式
            val tempMode = getString(
                when (tempMode) {
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

    @RequiresApi(Build.VERSION_CODES.M)
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
            tvUnit.setTextColor(resources.getColor(R.color.color666666, null))
        } else {
            matrix.setSaturation(0f) //灰色
            tvName.setTextColor(resources.getColor(R.color.colorcacaca, null))
            tvUnit.setTextColor(resources.getColor(R.color.colorcacaca, null))
        }
        val filter = ColorMatrixColorFilter(matrix)
        imageview.colorFilter = filter
    }

    override fun onStop() {
        super.onStop()
        binding.drawSpo2Rect.startOrStop = true
        binding.spoView.startOrStop = false
    }


    override fun onResume() {
        super.onResume()
        if (!binding.spoView.startOrStop) {
            binding.spoView.setScope(150, 0)
            binding.spoView.Start()
            binding.spoView.cleanAndRestart()
        }
        if (binding.drawSpo2Rect.startOrStop) {
            binding.drawSpo2Rect.startOrStop = false
            binding.drawSpo2Rect.invalidate()
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
            else -> 2
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
            pr in 51..60 -> 1
            pr <= 50 -> 2
            pr in 100..119 -> 3
            else -> 4
        }
    }
}


fun changeNibp2kpa(mmHg: Int): Float {
    var fmmHg = mmHg * 0.133f
    val b = BigDecimal(fmmHg.toDouble())
    fmmHg = b.setScale(1, BigDecimal.ROUND_HALF_UP).toFloat()
    return fmmHg
}


/**
* 获取体温结果
*
* @param temp 摄氏度
* @return 0:温度低于测量范围，请重新测量 <br></br>
* 1:体温偏低 <br></br>
* 2:体温正常 <br></br>
* 3:体温偏高 <br></br>
* 4:温度高于测量范围，请重新测量
*/
fun getTempRank(temp: Float): Int {
    if (temp < 32) {
        return 0
    } else if (temp >= 32 && temp < 35.8f) {
        return 1
    } else if (temp in 35.5f..37.5f) {
        return 2
    } else if (temp > 37.5f && temp <= 43) {
        return 3
    } else if (temp > 43) {
        return 4
    }
    return 0
}

/**
 * 获取血糖等级
 *
 * @param gluMmol 血糖值，单位 mmol
 * @param type    0:空腹，1：餐后2小时
 * @return 0:血糖正常<br></br>
 * 1:血糖稍高，请咨询医生 <br></br>
 * 2:血糖偏高，请咨询医生<br></br>
 * 3:血糖低于测量范围，请重新测量 <br></br>
 * 4:血糖高于测量范围，请重新测量 <br></br>
 * 5:血糖偏低
 */
fun getGLURank(gluMmol: Float, type: Int): Int {
    if (type == 0) { //空腹
        if (gluMmol in 3.9f..6.1f) {
            return 0
        } else if (gluMmol > 6.1f && gluMmol < 7) {
            return 1
        } else if (gluMmol >= 7 && gluMmol < 33.3f) {
            return 2
        } else if (gluMmol < 1.1f) {
            return 3
        } else if (gluMmol >= 33.3f) {
            return 4
        } else if (gluMmol >= 1.1f && gluMmol < 3.9f) {
            return 5
        }
    } else if (type == 1) { //餐后2小时
        if (gluMmol in 3.9f..7.8f) {
            return 0
        } else if (gluMmol > 7.8f && gluMmol < 11.1) { //稍高
            return 1
        } else if (gluMmol >= 11.1 && gluMmol < 33.3f) { //偏高
            return 2
        } else if (gluMmol < 1.1f) { //低于测量范围
            return 3
        } else if (gluMmol >= 33.3f) { //高于测量范围
            return 4
        } else if (gluMmol >= 1.1f && gluMmol < 3.9f) { //偏低
            return 5
        }
    }
    return 0
}

/**
 * @param cholMMOL 总胆固醇值，单位mmol
 * @return 0:总胆固醇正常<br></br>
 * 1:总胆固醇偏高，请咨询医生<br></br>
 * 2:总胆固醇低于测量范围，请重新测量<br></br>
 * 3:总胆固醇高于测量范围，请重新测量
 */
fun getCHOLRank(cholMMOL: Float): Int {
    if (cholMMOL in 2.8f..5.2f) {
        return 0
    } else if (cholMMOL > 10.35f) {
        return 3
    } else if (cholMMOL > 5.2f) {
        return 1
    } else if (cholMMOL < 2.8f) {
        return 2
    }
    return 0
}

/**
 * @param bkMMOL 血酮，单位mmol
 * @return 0:血酮正常<br></br>
 * 1:血酮偏高，请咨询医生<br></br>
 * 2:血酮低于测量范围，请重新测量<br></br>
 * 3:血酮高于测量范围，请重新测量
 */
fun getBKRank(bkMMOL: Float): Int {
    if (bkMMOL in 0.03f..0.3f) {
        return 0
    } else if (bkMMOL > 1.0f) {
        return 3
    } else if (bkMMOL >= 0.3f) {
        return 1
    } else if (bkMMOL < 0.03f) {
        return 2
    }
    return 0
}


/**
 * 获取尿酸等级
 *
 * @param style  测量类型 0：成人男性，1：成人女性
 * @param uaMmol 尿酸值，单位 mmol
 * @return 0:尿酸正常<br></br>
 * 1:尿酸偏低，请咨询医生<br></br>
 * 2:尿酸偏高，请咨询医生<br></br>
 */
fun getUARank(style: Int, uaMmol: Float): Int {
    when (style) {
        0 -> {
            if (uaMmol in 0.149f..0.42f) {
                return 0
            } else if (uaMmol < 0.149f) {
                return 1
            } else if (uaMmol > 0.42f) {
                return 2
            }
        }
        1 -> {
            if (uaMmol in 0.089f..0.36f) {
                return 0
            } else if (uaMmol < 0.089f) {
                return 1
            } else if (uaMmol > 0.36f) {
                return 2
            }
        }
    }
    return 0
}

@SuppressLint("RepeatOnLifecycleWrongUsage")
inline fun LifecycleOwner.launchWhenResumed(
    retryTime: Int = 1,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        var retryCount = 0
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                block()
                this@launch.cancel()
            } finally {
                if (retryTime != -1) {
                    retryCount += 1
                    if (retryCount >= retryTime) {
                        this@launch.cancel()
                    }
                }
            }
        }
    }
}