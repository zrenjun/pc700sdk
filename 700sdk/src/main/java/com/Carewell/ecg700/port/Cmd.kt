package com.Carewell.ecg700.port


/**
 *
 *  说明: 命令封装
 *  zrj 2022/3/25 17:56
 *
 */
object Cmd {

    /** 查询电量  */
    val queryBattery = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0xFF.toByte(),
        0x02.toByte(),
        0x03.toByte(),
        0x76.toByte()
    )

    /**
     * 体温模式切换
     * @param  1 表示耳温模式；2 表示成人额温模式； 3 表示儿童额温模式；4 表示物温模式
     * @param  1.摄氏度 , 2:华氏度
     */
    val switchTemperature = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x72.toByte(),
        0x03.toByte(),
        0x03.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    /** 新12心电---开始透传  */
    val startTransfer = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x30.toByte(),
        0x02.toByte(),
        0x01.toByte(),
        0xC6.toByte()
    )

    /** 新12心电---停止透传  */
    val stopTransfer = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x30.toByte(),
        0x02.toByte(),
        0x02.toByte(),
        0x24.toByte()
    )

    /** 新12心电————开始测量  */
    val startECG12Measure = byteArrayOf(
        0x7f.toByte(),
        0xc1.toByte(),
        0x00.toByte(),
        0x01.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x41.toByte()
    )

    /** 新12心电————停止测量  */
    val stopECG12Measure = byteArrayOf(
        0x7f.toByte(),
        0xc1.toByte(),
        0x00.toByte(),
        0x02.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x42.toByte()
    )

    /** 睡眠  */
    val sleepMachine = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0xFF.toByte(),
        0x04.toByte(),
        0x05.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0xC4.toByte()
    )

    /**
     * 握手包
     */
    val handShake = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0xFF.toByte(),
        0x02.toByte(),
        0x01.toByte(),
        0xCA.toByte()
    )

    /**
     * 血压命令————设置血压模块
     */
    val bNIBP_SetPressureMode = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x41.toByte(),
        0x03.toByte(),
        0x03.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )
    /**
     * 血压命令————获取血压模块
     */
    val bNIBP_GetPressureMode = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x41.toByte(),
        0x02.toByte(),
        0x02.toByte(),
        0x00.toByte()
    )

    /**
     * 血压命令————静态压校准开始
     */
    val bNIBP_StaticAdjustingStart = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x02.toByte(),
        0x11.toByte(),
        0x00.toByte()
    )

    /**
     * 血压命令————静态压校准停止
     */
    val bNIBP_StaticAdjustingStop = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x02.toByte(),
        0x12.toByte(),
        0x00.toByte()
    )

    /**
     * 血压命令————动态压(实时袖带压力值)校准开始
     */
    val bNIBP_DynamicAdjustingStart = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x02.toByte(),
        0x13.toByte(),
        0x00.toByte()
    )

    /**
     * 血压命令————动态压校准停止
     */
    val bNIBP_DynamicAdjustingStop = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x02.toByte(),
        0x14.toByte(),
        0x00.toByte()
    )

    /**
     * 血压命令————漏气检测开始
     */
    val bNIBP_CheckLeakageStart = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x02.toByte(),
        0x15.toByte(),
        0x00.toByte()
    )

    /**
     * 血压命令————漏气检测停止
     */
    val bNIBP_CheckLeakageStop = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x02.toByte(),
        0x16.toByte(),
        0x00.toByte()
    )

    /**
     * 血压命令————开始测量
     */
    val bNIBP_StartMeasureNIBP = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x02.toByte(),
        0x01.toByte(),
        0x00.toByte()
    )

    /**
     * ֹͣ血压命令————停止测量
     */
    val bNIBP_StopMeasureNIBP = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x02.toByte(),
        0x02.toByte(),
        0x00.toByte()
    )

    /**
     * ֹͣ血压命令————设置成人
     */
    val bNIBP_SetAdult = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x03.toByte(),
        0x04.toByte(),
        0x00.toByte(),
        0xEB.toByte()
    )

    /**
     * ֹͣ血压命令————设置儿童
     */
    val bNIBP_SetChild = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x03.toByte(),
        0x04.toByte(),
        0x01.toByte(),
        0xB5.toByte()
    )

    /**
     * ֹͣ血压命令————设置婴儿
     */
    val bNIBP_SetInfant = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x03.toByte(),
        0x04.toByte(),
        0x02.toByte(),
        0x57.toByte()
    )

    /**
     * 血糖命令————设置设备类型
     */
    val bGLU_SetType = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0xE0.toByte(),
        0x03.toByte(),
        0x01.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    /**
     * 血糖命令————获取设备类型
     */
    val bGLU_GetType = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0xE0.toByte(),
        0x02.toByte(),
        0x02.toByte(),
        0x3D.toByte()
    )

    /**
     * usb-mode: •0-device(=iA), 1-host.
     * 11•该命令仅在bIDCard_Type =1时有效
     */
    val usb_Mode = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0xFF.toByte(),
        0x03.toByte(),
        0x08.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    /**
     * 卡/证命令————类型设置  0 = 离线； 1 = 在线
     */
    val bIDCard_Type = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0xFF.toByte(),
        0x03.toByte(),
        0x07.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    /**
     * 卡/证命令————开始扫描
     */
    val bIDCard_StartScan = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x60.toByte(),
        0x02.toByte(),
        0x011.toByte(),
        0x00.toByte()
    )

    /**
     * 卡/证命令————停止扫描
     */
    val bIDCard_StopScan = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x60.toByte(),
        0x02.toByte(),
        0x012.toByte(),
        0x00.toByte()
    )

    //------------------   单导联心电   --------------------
    /**
     * 单导联心电-开始测量命令
     */
    var bStartECG = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0x3a.toByte(),
        0x02.toByte(),
        0x01.toByte(),
        0xc6.toByte()
    )

    /**
     * 单导联心电-停止测量命令
     */
    var bStopECG = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0x3a.toByte(),
        0x02.toByte(),
        0x02.toByte(),
        0x24.toByte()
    )


    //获取主固件版本
    val getMainVer = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0xf0.toByte(),
        0x03.toByte(),
        0x01.toByte(),
        0x02.toByte(),
        0x39.toByte()
    )

    //获取子固件版本
    val getSubVer = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0xf0.toByte(),
        0x03.toByte(),
        0x01.toByte(),
        0x12.toByte(),
        0xa4.toByte()
    )

    //主固件进入升级状态
    val enterUpgradeStatusMain = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0xf0.toByte(),
        0x03.toByte(),
        0x01.toByte(),
        0x01.toByte(),
        0xdb.toByte()
    )

    //子固件进入升级状态
    val enterUpgradeStatusSub = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0xf0.toByte(),
        0x03.toByte(),
        0x01.toByte(),
        0x11.toByte(),
        0xdb.toByte()
    )

    //开始升级
    val startUpgradeStatus = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0xf0.toByte(),
        0x03.toByte(),
        0x02.toByte(),
        0x00.toByte(),
        0xd0.toByte()
    )

    //升级结束
    val upgradeStatusEnd = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0xf0.toByte(),
        0x03.toByte(),
        0x03.toByte(),
        0x01.toByte(),
        0x4a.toByte()
    )
}