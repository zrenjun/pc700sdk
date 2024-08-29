@file:Suppress("unused")

package com.Carewell.ecg700.port

/**
 * 固件升级 结束
 */
class IAPEndEvent

/**
 * 12导结束
 */
class EcgStopTransferEvent


/**
 * 固件升级 结束后 唤醒下位机 握手
 */
class ShakeHandsEvent

/**
 * 固件升级 错误提示
 */
data class IAPCheckEvent(val err: String)
/**
 * 固件升级 h进度
 */
data class IAPProgramingEvent(val type: Int)

/**
 * 获取到设备版本信息  返回字符串用压缩BCD码表示。
 * @param hardwareVersion         硬件版本-> 例：V_Hard = 0x11,表示硬件版本号为V1.1
 * @param softwareVersion        软版本
 */
data class GetDeviceVersionEvent(val hardwareVersion: Int, val softwareVersion: Int)

/**
 * 电池状态
 * @param chargeLevel 电量等级 ,1-4
 * @param chargeStatus  充电状态：1:充电中，0:充满电
 * @param ac 为1时表示AC供电接入
 */
data class BatteryStatusEvent(val chargeLevel: Int, val chargeStatus: Int, val ac: Int)

/**
 * IPA版本
 * @param hardwareVersion 硬件版本
 * @param softwareVersion 软件版本
 * @param response
 * 		  0x00:下位机需要先执行软复位
 * 		  0x01:下位机已经准备好，可以执行下面的步骤，启动固件更新。
 * 		  0x02:下位机仅回应版本号。
 *        0x0F:无法升级指定MCU。
 */
data class IAPVersionEvent(val hardwareVersion: Int, val softwareVersion: Int, val response: Byte)


/**
 * 血氧参数
 * @param nSpO2
 * 血氧值 &nbsp&nbsp 范围在0-100
 * @param nPR
 * 脉率值 &nbsp&nbsp 范围在0-511
 * @param nPI
 * 血流灌注值 &nbsp&nbsp 范围在0-25.5
 * @param bProbe
 * 探头状态 true正常 &nbsp&nbsp false探头脱落
 * @param mode
 * 0x00 成人模式&nbsp 0x01 新生儿模式 &nbsp 0x02 动物模式
 */
data class SPOGetParamEvent(
    val nSpO2: Int,
    val nPR: Int,
    val nPI: Float,
    val bProbe: Boolean,
    val mode: Int
)

/**
 * 血氧测量状态
 * @param state    0x00 测量结束 0x01模块忙或测量正在进行中 0xFF模块故障或未接入
 * @param softwareVersion    血氧软件版本——软件版本
 * @param hardwareVersion  血氧软件版本——硬件版本
 */
data class SPOGetStateEvent(
    val state: Int,
    val softwareVersion: String,
    val hardwareVersion: String
)

/**
 * 血氧工作模式
 *
 * @param mode      0x00 成人 0x01新生儿 0xFF 故障或是未接入
 */
data class SPOGetModeEvent(val mode: Int)

/**
 *  波形
 *  @param data    数据
 *  @param flag    是否有搏动标记
 */
data class Wave(val data: Int, val flag: Int)

/**
 *  Spo2柱状缓存释放
 */
data class SPOGetWaveEvent(val waves: MutableList<Wave>)

/**
 * 血压测量结束并获取到测量结果
 *
 * @param sys    收缩压
 * @param map   平均压
 * @param dia  舒张压
 * @param plus  脉率
 * @param bHr true心率正常 false心率不齐
 * @param rank   血压结果等级 数值为1-6 对应 最佳、正常、临高、轻高、中高、重高
 */
data class NIBPGetMeasureResultEvent(
    val sys: Int,
    val map: Int,
    val dia: Int,
    val plus: Int,
    val bHr: Boolean,
    val rank: Int
)

/**
 *  血压错误
 */
data class NIBPGetMeasureErrorEvent(val error: Int)

/**
 * 血压测量错误结果
 */
object NIBP_RESULT {
    /**
     * 血压自检测失败
     */
    const val NIBP_RESULT_01 = 1

    /**
     * 袖带错误
     */
    const val NIBP_RESULT_02 = 2

    /**
     * 漏气
     */
    const val NIBP_RESULT_03 = 3

    /**
     * 压力错误
     */
    const val NIBP_RESULT_04 = 4

    /**
     * 血压信号微弱
     */
    const val NIBP_RESULT_05 = 5

    /**
     * 血压超出范围
     */
    const val NIBP_RESULT_06 = 6

    /**
     * 过度运动
     */
    const val NIBP_RESULT_07 = 7

    /**
     * 检测到过压
     */
    const val NIBP_RESULT_08 = 8

    /**
     * 血压信号饱和
     */
    const val NIBP_RESULT_09 = 9

    /**
     * 血压测试漏气
     */
    const val NIBP_RESULT_10 = 10

    /**
     * 血压模块错误
     */
    const val NIBP_RESULT_11 = 11

    /**
     * 血压测量超时
     */
    const val NIBP_RESULT_12 = 12

    /**
     * 电量过低，暂停使用
     */
    const val NIBP_RESULT_14 = 14

    /**
     * 袖带类型错误
     */
    const val NIBP_RESULT_15 = 15
}


/**
 *  血压等级进度表盘
 */
data class NIBPGetRealDataEvent(val realData: Int)

/**
 * 血压漏气检测结果
 *   10秒的漏气量,单位mmHg 下位机返回结果无效
 */
data class NIBPCheckLeakageResultEvent(val leak: Int)

/**
 * 血压开始测量
 */
class NIBPStartMeasureEvent

/**
 * 血压停止测量
 */
class NIBPStopMeasureEvent

/**
 * 初始压力设置成功
 */
class NIBPPressureSetEvent

/**
 * 病人类型设置成功
 */
class NIBPatientTypeSetEvent

/**
 * 血压开始静态压校准
 */
class NIBPStartStaticAdjustingEvent

/**
 * 血压开始动态压校准
 */
class NIBPStartDynamicAdjustingEvent

/**
 * 血压开始漏气检测
 */
class NIBPStartCheckLeakageEvent

/**
 * 血压停止漏气检测
 */
class NIBPStopCheckLeakageEvent

/**
 * 获取血压状态
 *
 * @param state
 *            0x00: 测量结束
 *            0x01: 模块忙或测量正在进行中
 *            0xFF: 模块故障或未接入
 *            0xD0：模块接入
 *            0xD1：模块拔出
 */
class NIBPGetStateEvent(val state: Int)

/**
 * 设置血压模块应答
 * @param rep  1/0  成功/失败
 */
class NIBPSetModeResultEvent(val rep: Int)

/**
 * @param module 血压模块类型: 1血压模块JXH,2血压模块KRK
 * @param softwareVersion 软件版本
 * @param hardwareVersion 硬件版本
 */
data class NIBPGetModuleEvent(
    val module: Int,
    val softwareVersion: String,
    val hardwareVersion: String
)


//--------------------------------------------------------------------------------------------------------------

/**
 * 血糖仪器类别
 * @param type 0x01台湾怡成 ;0x02 百捷BeneCheck三合一[血糖、尿酸、总胆固醇];0x03 爱奥乐  0x04 乐普三合一
 */
data class GetGLUType(val type: Int)

/**
 * 血糖测量结果(GLU)
 *
 * @param type 0结果正常 1结果偏低 2结果偏高
 * @param data 血糖值
 * @param unit 单位 0: mmol/L , 1: mg/dL
 */
data class GetGLUResult(val type: Int, val data: String, val unit: String)

/**
 * 尿酸(UA)
 *
 * @param type 0结果正常 1结果偏低 2结果偏高
 * @param data 尿酸值
 * @param unit 单位 0 mmol/L  1 mg/dL
 */
data class GetUAResult(val type: Int, val data: Float, val unit: Int)

/**
 * 总胆固醇(CHOL)
 *
 * @param type 0结果正常 1结果偏低 2结果偏高
 * @param data 胆固醇值
 * @param unit 单位 0 mmol/L  1 mg/dL
 */
data class GetCHOLResult(val type: Int, val data: Float, val unit: Int)


/**
 * 体温测量结果
 *
 * @param type 0结果正常 1结果偏低 2结果偏高
 * @param strC 体温值
 * @param unit 单位 0 摄氏度 , 1 华氏度
 */
data class GetTMPResult(val type: Int,val strC: String, val strF: String, val unit: Int)

/**
 * 查询体温模式应答
 * @param position 0 设置体温模式成功,1 表示耳温模式；2 表示成人额温模式； 3 表示儿童额温模式；4 表示物温模式
 * @param unit 1.摄氏度 , 2:华氏度
 */
data class GetTMPMode(val position: Int, val unit: Int)


class ECGData {
    var frameNum: Int = 0
    val data = mutableListOf<Wave>()
}


/**
 * 单导联心电实时数据
 *
 * @param data 心电波形
 * @param leadOff 导联脱落标记
 */
data class GetSingleECGRealTime(val data: ECGData, val leadOff: Boolean)

/**
 * 单导联心电测量结果
 * @param nResult
 * 测量结果
 * 0X00      波形未见异常 ;
 * 0X01      波形疑似心跳稍快请注意休息;
 * 0X02      波形疑似心跳过快请注意休息;
 * 0X03      波形疑似阵发性心跳过快请咨询医生;
 * 0X04      波形疑似心跳稍缓请注意休息;
 * 0X05      波形疑似心跳过缓请注意休息;
 * 0X06      波形疑似偶发心跳间期缩短请咨询医生;
 * 0X07      波形疑似心跳间期不规则请咨询医生;
 * 0X08      波形疑似心跳稍快伴有偶发心跳间期缩短请咨询医生;
 * 0X09      波形疑似心跳稍缓伴有偶发心跳间期缩短请咨询医生;
 * 0X0A  波形疑似心跳稍缓伴有心跳间期不规则请咨询医生;
 * 0X0B  波形有漂移请重新测量;
 * 0X0C  波形疑似心跳过快伴有波形漂移请咨询医生;
 * 0X0D  波形疑似心跳过缓伴有波形漂移请咨询医生;
 * 0X0E  波形疑似偶发心跳间期缩短伴有波形漂移请咨询医生;
 * 0X0F  波形疑似心跳间期不规则伴有波形漂移请咨询医生;
 * 0XFF  信号较差请重新测量.
 * @param nHR
 * 心率值 0~255
 */
data class GetSingleECGResult(val nResult: Int, val nHR: Int)
data class GetSingleECGGain(val gain: Int, val display: Int)

/**
 * 卡/证信息
 *
 * @param code 结果代码  结果正确[IDCardErrorCode.ERROR_NORMAL]
 * 错误结果
 * [IDCardErrorCode.ERROR_MODEERROR]
 * [IDCardErrorCode.ERROR_MODEBUSY]
 * [IDCardErrorCode.ERROR_NOFIND]
 * @param info 信息
 */
data class GetSMAInfo(val code: Int, val info: IDCard?)

/**
 * 扫描身份证信息的错误代码
 */
object IDCardErrorCode {
    /**
     * 没有错误
     */
    const val ERROR_NORMAL = 0

    /**
     * SAM模块故障或未接入
     */
    const val ERROR_MODEERROR = 1

    /**
     * 忙于上一条指令
     */
    const val ERROR_MODEBUSY = 2

    /**
     * 未扫描到身份证信息
     */
    const val ERROR_NOFIND = 3

    /**
     * 电量过低，身份证扫描启动失败
     */
    const val ERROR_CHARGE_LOW = 4
}

