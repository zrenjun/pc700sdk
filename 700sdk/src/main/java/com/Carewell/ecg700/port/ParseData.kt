package com.Carewell.ecg700.port

import com.zkteco.android.IDReader.IDPhotoHelper
import com.zkteco.android.IDReader.WLTService
import java.io.UnsupportedEncodingException
import java.math.BigDecimal
import java.nio.charset.Charset
import java.util.Locale


/**
 *
 *  说明: 数据解析
 *  zrj 2022/3/25 18:59
 *
 */
object ParseData {

    init {
        System.loadLibrary("sigleEcg")
        System.loadLibrary("online")
        System.loadLibrary("offline")
    }

    private external fun hpFilter(dataIn: Int, init: Int): Int
    external fun shortFilter(inShorts: ShortArray?): ShortArray
    external fun offlineFilter(f: Double, reset: Boolean): DoubleArray


    //vihealth 滤波算法
    /**
     * 离线滤波，送入整条数据然后输出
     */
    external fun newShortFilter(shorts: ShortArray?): ShortArray

    /**
     * online
     */
    external fun filter(f: Double, reset: Boolean = false): DoubleArray

    fun resetFilter() {
        filter(0.0, true)
    }


    private var isOldMachine = false //默认新机器

    private const val head2 = 0xaa.toByte()
    private const val head3 = 0x55.toByte()

    fun processingOrdinaryData(bytes: ByteArray) {
        if (bytes.size > 1 && bytes[0] == head2 && bytes[1] == head3) {
            val len = bytes[3].toInt() and 0xFF//长度
            val temp = bytes.copyOfRange(0, len + 4)
            ordinaryData(temp)
            if (bytes.size > len + 4) {
                processingOrdinaryData(bytes.copyOfRange(len + 4, bytes.size))
            }
        }
    }

    private const val nibp_type: Byte = 0x00
    private const val temp_type: Byte = 0x01
    private const val ecg_type: Byte = 0x02
    private const val glu_type: Byte = 0x03
    private const val chol_type: Byte = 0x04
    private const val ua_type: Byte = 0x05
    private var preNibpTime = 0L
    private var preTempTime = 0L
    private var preEcgTime = 0L
    private var preGluTime = 0L
    private var preCholTime = 0L
    private var preUaTime = 0L
    private var nowTemp = 0.0
    private var preTemp = 0.0

    /**
     * 过滤回调次数，一次请求只返回一次数据
     */
    private fun filterCallBackCnt(type: Byte): Boolean {
        var bFlag = false
        when (type) {
            nibp_type -> {
                bFlag = System.currentTimeMillis() - preNibpTime > 3000
                preNibpTime = System.currentTimeMillis()
            }

            temp_type -> {
                bFlag = if (System.currentTimeMillis() - preTempTime < 1000) {
                    nowTemp != preTemp
                } else {
                    true
                }
                preTemp = nowTemp
                preTempTime = System.currentTimeMillis()
            }

            ecg_type -> {
                bFlag = System.currentTimeMillis() - preEcgTime > 3000
                preEcgTime = System.currentTimeMillis()
            }

            glu_type -> {
                bFlag = System.currentTimeMillis() - preGluTime > 3000
                preGluTime = System.currentTimeMillis()
            }

            chol_type -> {
                bFlag = System.currentTimeMillis() - preCholTime > 3000
                preCholTime = System.currentTimeMillis()
            }

            ua_type -> {
                bFlag = System.currentTimeMillis() - preUaTime > 3000
                preUaTime = System.currentTimeMillis()
            }
        }
        return bFlag
    }

    //普通数据
    private fun ordinaryData(bytes: ByteArray) {
//        LogUtil.v("receive  ---->  " + HexUtil.bytesToHexString(bytes))
        val len = bytes[3].toInt() and 0xFF //长度
        val type = bytes[4].toInt() and 0xFF //类型
        when (bytes[2]) { //令牌
            //aa, 55, 30, 02, 02, aa,
            (0x30).toByte() -> {
                if (type == 0x02) {
                    postEvent(EcgStopTransferEvent())
                }
            }

            (0xFF).toByte() -> when (type) {
                0x02 -> { // 版本信息
                    //其中版本V_h:为硬件版本号；V_Sotf为软件版本号，均用压缩BCD码表示。例：V_Hard = 0x11,
                    //表示硬件版本号为V1.1
                    val temp1 = bytes[5].toInt() and 0xFF //硬件版本号
                    val temp2 = bytes[6].toInt() and 0xFF //软件版本号
                    var uuid = ""
                    if (len == 4) {
                        isOldMachine = true
                    }
                    for (k in 0 until len - 4) { //旧PC600无UUID
                        uuid += String.format("%02x", bytes[7 + k].toInt() and 0xff)
                    }
                    postEvent(GetDeviceVersionEvent(temp1, temp2))
                }

                0x03 -> { //充电标识
                    //充电|电量：   0 0 000 111
                    //bit7：充电标志，为 1 时表示正在充电
                    //bit6：AC 插入标志，为 1 时表示 AC 供电接入
                    //bit5-bit3：保留
                    //bit0-bit2：电量等级
                    val temp1 = bytes[5].toInt() and 0xff
                    val chargeStatus = temp1 shr 7 and 0x01 //充电状态
                    val ac = temp1 shr 6 and 0x01 //ac充电线插入
                    val chargeLvl = temp1 and 0x07 //电量等级
                    postEvent(BatteryStatusEvent(chargeLvl, chargeStatus, ac))
                }
            }
            //MCU固件版本查询
            (0xf0).toByte() -> if (type == 1) {
                val state = bytes[5].toInt()
                if (state and 0xf0 == 0x00) { //高4位MCU序号,单个MCU,主MCU
                    analyseVersion(bytes, state)
                } else if (state shr 4 and 0x01 == 0x01) { //高4位第2个MCU序号,子MCU
                    analyseVersion(bytes, state)
                }
            }
            // 血压命令
            (0x40).toByte() -> when (type) {
                0x01 -> postEvent(NIBPStartMeasureEvent())
                0x02 -> postEvent(NIBPStopMeasureEvent())
                0x03 -> postEvent(NIBPPressureSetEvent())
                0x04 -> postEvent(NIBPatientTypeSetEvent())
                0x11 -> postEvent(NIBPStartStaticAdjustingEvent())
                0x13 -> postEvent(NIBPStartDynamicAdjustingEvent())
                0x15 -> postEvent(NIBPStartCheckLeakageEvent())
                0x16 -> postEvent(NIBPStopCheckLeakageEvent())
                0x17 -> {
                    val temp1 = bytes[5].toInt() and 0xFF
                    val temp2 = bytes[6].toInt() and 0xFF
                    postEvent(NIBPCheckLeakageResultEvent((temp1 shl 8) + temp2))
                }
            }
            // 测量结束并获取到测量结果   aa, 55, 43, 07, 01, 00, 77, 00, 4d, 51, be
            (0x43).toByte() -> if (type == 0x01) { //测量结果
                val temp1 = bytes[5].toInt() and 0xFF //低
                val temp2 = bytes[6].toInt() and 0xFF //高
                val bHr = temp1 ushr 7 == 0 //心率结果
                val sys = (temp1 and 0x7f shl 8) + temp2 //收缩压
                //int sys =((temp2 & 0x7f) << 8) + temp1;//硬件bug，和协议相反
                val map = bytes[7].toInt() and 0xFF //平均压
                val dia = bytes[8].toInt() and 0xFF //舒张压
                val plus = bytes[9].toInt() and 0xFF //脉率
                postEvent(NIBPGetMeasureResultEvent(sys, map, dia, plus, bHr, getGrade(sys, dia)))
            } else if (type == 0x02) { //错误结果
                postEvent(NIBPGetMeasureErrorEvent(bytes[5].toInt() and 0x0F))
            }
            // 血压
            (0x41).toByte() -> when (type) {
                0x01 -> postEvent(NIBPGetStateEvent(bytes[5].toInt() and 0x0F)) //状态
                0x02 -> {
                    val module = bytes[5].toInt() //模块
                    val temp1 = bytes[6].toInt() and 0xFF //软件版本号
                    val temp2 = bytes[7].toInt() and 0xFF //硬件版本号
                    postEvent(
                        NIBPGetModuleEvent(
                            module,
                            Integer.toHexString(temp1).toString(),
                            Integer.toHexString(temp2).toString()
                        )
                    )
                }

                0x03 -> postEvent(NIBPSetModeResultEvent(bytes[5].toInt() and 0x0F)) //设置血压应答结果
            }
            // 血压测量实时数据   aa, 55, 42, 04, 01, 00, 4a, c1,
            (0x42).toByte() -> {
                val temp1 = bytes[5].toInt() and 0xFF //数据高字节
                val temp2 = bytes[6].toInt() and 0xFF //数据低字节
                postEvent(NIBPGetRealDataEvent((temp1 and 0x0f shl 8) + temp2))
            }
            // 血氧工作模式
            (0x50).toByte() -> if (type == 0x01) {
                postEvent(SPOGetModeEvent(bytes[5].toInt() and 0x0F))
            }
            // 血氧波形
            (0x52).toByte() -> {
                val waveData = mutableListOf<Wave>()
                for (j in 0 until len - 2) {
                    val temp = bytes[5 + j].toInt()
                    waveData.add(Wave(temp and 0x7f, temp ushr 7 and 0xFF))
                }
                postEvent(SPOGetWaveEvent(waveData))
            }
            // 血氧参数
            (0x53).toByte() -> {
                val spo2 = bytes[5].toInt() and 0xFF
                var temp1 = bytes[6].toInt() and 0xFF
                val temp2 = bytes[7].toInt() and 0xFF
                val pr = (temp2 shl 8) + temp1
                val pi = (bytes[8].toInt() and 0xFF) / 10f
                temp1 = bytes[9].toInt() and 0xFF
                postEvent(
                    SPOGetParamEvent(
                        spo2,
                        pr,
                        pi,
                        temp1 ushr 1 and 0x0001 == 0,
                        temp1 ushr 6 and 0x03
                    )
                )
            }
            // 血氧测量状态
            (0x54).toByte() -> {
                val state = bytes[5].toInt() and 0xFF
                val temp1 = bytes[6].toInt() and 0xFF
                val temp2 = bytes[7].toInt() and 0xFF
                postEvent(
                    SPOGetStateEvent(
                        state,
                        Integer.toHexString(temp1),
                        Integer.toHexString(temp2)
                    )
                )
            }
            // 血糖仪器类别
            (0xE0).toByte() -> postEvent(GetGLUType(bytes[5].toInt() and 0x0F))
            // 仪成和百捷--血糖结果
            (0xE2).toByte() -> {
                LogUtil.json("============仪成和百捷--血糖结果")
                //测量结果包含3 个字节。Result Data_Hi Data_Lo。数据解析时，应先判定 Result的最高位
                //bit7，是否检测到有效的存储记录。当标记没有存储记录时，则后面参数均无实际意义；当有存
                //储记录时，再判定血糖测量结果正常、偏高还是偏低，当正常时，则依据单位，解析后面的数据。
                //偏高或偏低时，则后 2字节数据无效
                val result = bytes[5].toInt() and 0xFF
                val temp1 = bytes[6].toInt() and 0xFF
                val temp2 = bytes[7].toInt() and 0xFF
                LogUtil.v("temp1  ---->  " + temp1 + "  temp2---->  " + temp2)
                //aa 55 e2 05 01   c2 00 93 96 ,怡成 c2>>7 ==1
                //Bit7:  Bit7=0：表示设备有存储记录；  =1表示设备中无有效存储
                if (result ushr 7 and 0x01 == 1) { // 结果无效
                    return
                }
                val a = result ushr 4 and 0x03
                if (a != 0) { // 测量错误，结果低/高 数据无效
                    if (type == 0x01) {
                        if (filterCallBackCnt(glu_type)) {
                            setGlU(a, 0f, 0)
                        }
                    } else if (type == 0x02) {
                        if (filterCallBackCnt(ua_type)) {
                            postEvent(GetUAResult(0, 0f, 0))
                        }
                    } else if (type == 0x03) {//血酮回调
                        if (filterCallBackCnt(chol_type)) {
                            postEvent(GetCHOLResult(0, 0f, 0))
                        }
                    }
                    return
                }
                val unit = result and 0x01  //Bit0：血糖值的单位  0：mmol/L  1-mg/dL
                var data = 0f
                //血糖值，两个字节，高字节在前。血糖值单位不同，计算方法不同。
                if (unit == 0) { // mmol/L
                    //血糖值单位为mmol/L时（Byte1的 bit0为 0）：使用 BCD码的格式，高字节在前，低字节在后，测量值精度为0.1，即测量结果的 10 进制数除以 10即为所得结果，
                    // 例如血糖值0x00, 0x82，则测量值为8.2， 血糖值 0x01，0x08，则测量值为10.8
                    data =
                        ((temp1 shr 4 and 0x0F) * 1000 + (temp1 and 0x0F) * 100 + (temp2 shr 4 and 0x0F) * 10 + (temp2 and 0x0F)) / 10f
                }
                if (unit == 1) { // mg/dL
                    //血糖值单位为mg/dL时（Byte1的bit0 为 1）：血糖值=(high<<8) + low。例如：0x00，0x82，则表示为130mg/dL。
                    // 【备注：因血糖、总胆固醇的值较大，而尿酸参数值偏小，当为尿酸结果时，上传的结果值为扩大 10倍的数据。血糖，总胆固醇参数未放大处理】
                    data = (temp1 shl 8) + temp2 + 0f
                }
                if (type == 0x01) {
                    if (filterCallBackCnt(glu_type)) {
                        setGlU(a, data, unit)
                    }
                } else if (type == 0x02) {
                    if (filterCallBackCnt(ua_type)) {
                        postEvent(GetUAResult(0, data / 10, unit))
                        LogUtil.json("============尿酸结果==" + (data / 10))
                    }
                } else if (type == 0x03) {//血酮回调
                    if (filterCallBackCnt(chol_type)) {
                        LogUtil.json("============胆固醇结果==" + data)
                        postEvent(GetCHOLResult(0, data, unit))
                    }
                }
            }
            //爱奥乐--血糖结果
            (0xE3).toByte() -> {
                LogUtil.json("============爱奥乐血糖结果")
                val result = bytes[5].toInt() and 0xFF
                val temp1 = bytes[6].toInt() and 0xFF //H
                val temp2 = bytes[7].toInt() and 0xFF //L
                val h4 = result ushr 4 and 0x0f
                val l4 = result and 0x0f
                var resultType = 0
                var dataMgdl = 0f
                var dataMmol = 0f
                when (h4) {
                    5 -> resultType = 1 //偏低
                    6 -> resultType = 2 //偏高
                    else -> { //正常
                        dataMgdl = temp1 * 256f + temp2 // mg/dL
                        val strMmol = String.format(Locale.US, "%.1f", dataMgdl / 18f)
                        dataMmol = strMmol.toFloat()
                    }
                }
                val data = if (l4 == 1) { //mg/dL ,当前设备显示的单位
                    dataMgdl
                } else { //mmol/L
                    dataMmol
                }
                setGlU(resultType, data, l4)
            }
            // 体温结果
            (0x74).toByte() -> {
                val result = bytes[5].toInt() and 0xFF
                val temp1 = bytes[6].toInt() and 0xFF
                val temp2 = bytes[7].toInt() and 0xFF
                if (result ushr 1 and 0x03 != 0) {
                    if (filterCallBackCnt(temp_type)) {
                        postEvent(GetTMPResult(result ushr 1 and 0x03, "0", "0", 0))
                    }
                    return
                }
                val unit = result and 0x01
                val data = if (isOldMachine) { //老机器
                    ((temp1 shl 8) + temp2) / 10f
                } else { //新机器
                    ((temp1 shl 8) + temp2) / 100f
                }
                //不四舍五入，保留1位小数
//                var dd = floor((data * 10).toDouble())
//                dd /= 10
                val dd =
                    BigDecimal(data.toString()).setScale(1, BigDecimal.ROUND_HALF_UP).toDouble()
                nowTemp = dd
                if (filterCallBackCnt(temp_type)) {
                    val strC: String
                    val strF: String
                    if (unit == 0) { // 摄氏度
                        strC = "$data"
                        val temp = data * 1.8f + 32
                        //不四舍五入，保留1位小数
                        val t = BigDecimal(temp.toString()).setScale(1, BigDecimal.ROUND_HALF_UP)
                            .toDouble()
//                        var t = floor((temp * 10).toDouble())
//                        t /= 10
                        strF = "$t"
                    } else { //华氏度
                        strF = "$data"
                        strC = "${(data - 32) / 1.8f}"
                    }
                    postEvent(GetTMPResult(0, strC, strF, unit))
                }
            }

            (0x72).toByte() -> if (type == 0x03) { //体温模式切换应答
                postEvent(GetTMPMode(0, 0))
            } else if (type == 0x04) { //体温模式查询
                val data = bytes[5].toInt()
                val pos = data shr 4 and 0x0f
                val unit = pos and 0x0f
                postEvent(GetTMPMode(pos, unit))
            }
            // 单导联心电测量的实时时数据
            (0x32).toByte() -> if (len == 0x37) {
                val ecgData = ECGData()
                ecgData.frameNum = bytes[5].toInt() and 0xff // 帧号
                for (j in 0 until 50 step 2) { // 数据每包25
                    var temp1 = bytes[6 + j].toInt() and 0xff //状态字节
                    val temp2 = bytes[7 + j].toInt() and 0xff //波形数据
                    val flag = temp1 and 0x40 ushr 6
                    temp1 = temp1 and 0x0f
                    var data = (temp1 shl 8) + temp2
                    data = hpFilter(data, 0)
                    ecgData.data.add(Wave(data, flag))
                }
                val temp3 =
                    bytes[bytes.size - 2].toInt() and 0x80 ushr 7 // 导联脱落标记
                postEvent(GetSingleECGRealTime(ecgData, temp3 == 0))
            }
            // 单导联心电测量结果
            (0x33).toByte() -> {
                val temp1 = bytes[5].toInt() and 0xff // 测量结果
                val temp2 = bytes[7].toInt() and 0xff // 测量心率
                postEvent(GetSingleECGResult(temp1, temp2))
            }
            // 单导联心电 硬件增益数据包
            (0x34).toByte() -> {
                //硬件增益：2个字节，GAIN = ((High << 8) +  (Low)); 代表1mv对应的AD 值，同一硬件设备，此参数始终固定
                val high = bytes[5].toInt() and 0xff shl 8
                val low = bytes[6].toInt() and 0xff
                //显示增益：在一次心电测量过程中，此显示增益固定
                val display = bytes[7].toInt() and 0xff
                postEvent(GetSingleECGGain(high + low, display))
            }

            (0x60).toByte() -> when (type) {
                0x12 ->//下位机主动发送电量过低，中止省份证扫描。
                    if (len == 3) {
                        postEvent(GetSMAInfo(IDCardErrorCode.ERROR_CHARGE_LOW, null))
                    }

                0x30 ->// SAM模块故障或未接入
                    postEvent(GetSMAInfo(IDCardErrorCode.ERROR_MODEERROR, null))

                0x32 ->// 忙于上一条指令
                    postEvent(GetSMAInfo(IDCardErrorCode.ERROR_MODEBUSY, null))

                0x33 ->// 未扫描到身份证信息
                    postEvent(GetSMAInfo(IDCardErrorCode.ERROR_NOFIND, null))

                0x40 -> {
                    // 扫描到身份证并成功读取信息，返回身份证信息(文字+图像)
                    // aa, 55, 60, 28, 40, 01, 00, 04, 00, 28, 01, 31, 67, 10, 62, 20, 00, 20, 00, 20, 00, 20, 00, 20, 00, 20, 00, 20, 00, 20, 00, 20, 00, 20, 00, 20, 00, 20, 00, 20, 00, 31, 00, 5f,
                    var temp1 = bytes[5].toInt() and 0xFF // H
                    var temp2 = bytes[6].toInt() and 0xFF // L
                    idCardInfoTextLength = (temp1 shl 8) + temp2 // 文本信息长度
                    temp1 = bytes[7].toInt() and 0xFF // H
                    temp2 = bytes[8].toInt() and 0xFF // L
                    idCardInfoIMGLength = (temp1 shl 8) + temp2 // 图像信息长度
                    idCardPackageCount = bytes[9].toInt() and 0xFF //总包数,身份证信息总共分为几包上传
                    temp1 = bytes[10].toInt() and 0xFF // 包序号
                    if (temp1 == 0x01) idCardInfo.clear()
                    var i = 0
                    while (i < len - 8) {
                        idCardInfo.add(bytes[11 + i])
                        i++
                    }
                    if (temp1 == idCardPackageCount) { // 开始解析身份证数据
                        getIDCardInfo()
                    }
                }
            }
        }
    }

    private fun analyseVersion(bytes: ByteArray, state: Int) {
        var temp1: Int
        var temp2: Int
        //硬件版本
        val h1 = bytes[6]
        val h2 = bytes[7]
        temp1 = getH4(h1)
        temp2 = getL4(h1)
        var verHard = temp1 * 1000 + temp2 * 100
        temp1 = getH4(h2)
        temp2 = getL4(h2)
        verHard += temp1 * 10 + temp2
        //软件版本
        val s1 = bytes[8]
        val s2 = bytes[9]
        temp1 = getH4(s1)
        temp2 = getL4(s1)
        var verSoft = temp1 * 1000 + temp2 * 100
        temp1 = getH4(s2)
        temp2 = getL4(s2)
        verSoft += temp1 * 10 + temp2
        val response = (state and 0x0f).toByte() //低4位
//        if (response.toInt() == 0x01) { //下位机已经准备好
//        } else if (response.toInt() == 0x02) { //只获取版本号
//        }
        postEvent(IAPVersionEvent(verHard, verSoft, response))
    }

    /**
     * 获取byte的高4位
     */
    fun getH4(data: Byte) = (data.toInt() ushr 4) and 0x0f

    /**
     * 获取byte的低4位
     */
    fun getL4(data: Byte) = data.toInt() and 0x0f


    /**
     * @param sys 收缩压
     * @param dia 舒张压
     */
    private fun getGrade(sys: Int, dia: Int): Int {
        return if (getLevelSYS(sys) >= getLevelDIA(dia)) {
            getLevelSYS(sys)
        } else {
            getLevelDIA(dia)
        }
    }

    //获取收缩压等级
    private fun getLevelSYS(sys: Int): Int {
        return when {
            sys < 120 -> 1 //理想血压
            sys < 130 -> 2 //正常血压
            sys in 130..139 -> 3 //正常高值
            sys in 140..159 -> 4 //1级高血压
            sys in 160..179 -> 5 //2级高血压
            sys >= 180 -> 6 //3级高血压
            else -> 2
        }
    }

    //获取舒张压等级
    private fun getLevelDIA(dia: Int): Int {
        return when {
            dia < 80 -> 1 //理想血压
            dia < 85 -> 2 //正常血压
            dia in 85..89 -> 3 //正常高值
            dia in 90..99 -> 4 //1级高血压
            dia in 100..109 -> 5 //2级高血压
            dia >= 110 -> 6 //3级高血压
            else -> 2
        }
    }

    /**
     * 身份证信息
     */
    private val idCardInfo = mutableListOf<Byte>()

    /**
     * 身份证信息中文本信息总长度
     */
    private var idCardInfoTextLength = 0

    /**
     * 身份证信息中图像信息总长度
     */
    private var idCardInfoIMGLength = 0

    /**
     * 身份证信息总包数
     */
    private var idCardPackageCount = 0

    /**
     * 获取身份证信息
     */
    private fun getIDCardInfo() {
        if (idCardInfo.size != idCardInfoTextLength + idCardInfoIMGLength) return
        val idCard = IDCard()
        val textInfo = ByteArray(idCardInfoTextLength)
        for (i in 0 until idCardInfoTextLength) {
            textInfo[i] = idCardInfo.removeAt(0)
        }
        val imgInfo = ByteArray(idCardInfoIMGLength)
        for (i in 0 until idCardInfoIMGLength) {
            imgInfo[i] = idCardInfo.removeAt(0)
        }
        idCard.name = getInfo(textInfo, 30, 0)
        idCard.sex = getInfo(textInfo, 2, 30)
        idCard.setNation(getInfo(textInfo, 4, 32))
        idCard.birthday = getInfo(textInfo, 16, 36)
        idCard.address = getInfo(textInfo, 70, 52)
        idCard.iDCardNo = getInfo(textInfo, 36, 122)
        idCard.grantDept = getInfo(textInfo, 30, 158)
        idCard.userLifeBegin = getInfo(textInfo, 16, 188)
        idCard.userLifeEnd = getInfo(textInfo, 16, 204)
        val buf = ByteArray(WLTService.imgLength)
        if (1 == WLTService.wlt2Bmp(imgInfo, buf)) {
            idCard.headBitmap = IDPhotoHelper.Bgr2Bitmap(buf)
        }
        postEvent(GetSMAInfo(IDCardErrorCode.ERROR_NORMAL, idCard))
    }

    private fun getInfo(src: ByteArray, len: Int, start: Int): String? {
        val info = ByteArray(len)
        System.arraycopy(src, start, info, 0, len)
        try {
            return String(info, Charset.forName("UTF-16LE"))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return null
    }

    private fun setGlU(type: Int, data: Float, unit: Int) {
        var glu = ""
        var gluMgdl = ""
        if (type == 0) {
            //正常
            if (unit == 0 || unit == 2) { //mmol/L , unit == 0  百捷，怡成; unit == 2 爱奥乐
                glu = "$data"
                gluMgdl = String.format(Locale.US, "%.1f", data * 18f)
            } else { //mg/dl
                gluMgdl = "$data"
                glu = String.format(Locale.US, "%.1f", data / 18f)
            }
        }
        postEvent(GetGLUResult(type, glu, gluMgdl))
        LogUtil.v("type  ---->  " + type + "  glu---->  " + glu + "   gluMgdl---->  " + gluMgdl)
    }

}



