package com.Carewell.ecg700


/**
 *
 *  说明: 命令封装
 *  zrj 2022/3/25 17:56
 *
 */
object Cmd {

    /**
     * 体温模式切换
     * @param position 1 表示耳温模式；2 表示成人额温模式； 3 表示儿童额温模式；4 表示物温模式
     * @param unit 1.摄氏度 , 2:华氏度
     */
    val switchTemperature = byteArrayOf(0xaa.toByte(), 0x55, 0x72, 0x03, 0x03, 0, 0)

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
     * 查询版本
     */
    val queryVersion = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0xFF.toByte(),
        0x02.toByte(),
        0x02.toByte(),
        0x28.toByte()
    )

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
     * 血压命令————设置压力值
     */
    val bNIBP_PressureSetting = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x03.toByte(),
        0x03.toByte(),
        0x00.toByte(),
        0x00.toByte()
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
     * 血压命令————设置病人类型
     */
    val bNIBP_PatientType = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x40.toByte(),
        0x03.toByte(),
        0x04.toByte(),
        0x00.toByte(),
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
     * 血压命令————查询测量结果
     */
    val bNIBP_QueryMeasureNIBP = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x43.toByte(),
        0x02.toByte(),
        0x01.toByte(),
        0x00.toByte()
    )

    /**
     * 血压命令————查询血压模块状态
     */
    val bNIBP_QueryNIBPState = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x41.toByte(),
        0x02.toByte(),
        0x01.toByte(),
        0x00.toByte()
    )


    /**
     * 血氧命令————设置工作模式
     */
    val bSPO_SetWorkMode = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x50.toByte(),
        0x03.toByte(),
        0x01.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    /**
     * 血氧命令————查询状态
     */
    val bSPO_QueryState = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x54.toByte(),
        0x02.toByte(),
        0x01.toByte(),
        0x00.toByte()
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
     * 血糖命令————查询设备类型
     */
    val bGLU_QueryType = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0xE0.toByte(),
        0x02.toByte(),
        0x02.toByte(),
        0x00.toByte()
    )

    /**
     * 血糖命令————查询结果
     */
    val bGLU_QueryResult = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0xE2.toByte(),
        0x02.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    /**
     * 12心电命令————开始测量
     */
    val bECG12_StartMeasure = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x30.toByte(),
        0x02.toByte(),
        0x01.toByte(),
        0x00.toByte()
    )

    /**
     * 12心电命令————停止测量
     */
    val bECG12_StopMeasure = byteArrayOf(
        0xAA.toByte(),
        0x55.toByte(),
        0x30.toByte(),
        0x02.toByte(),
        0x02.toByte(),
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

    /**
     * 单导联心电-查询版本命令
     */
    var bVerECG = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0x31.toByte(),
        0x02.toByte(),
        0x01.toByte(),
        0x6d.toByte()
    )

    /**
     * 单导联心电-查询工作状态及测量结果命令
     */
    var bStatusECG = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0x31.toByte(),
        0x02.toByte(),
        0x02.toByte(),
        0x8f.toByte()
    )

    /**
     * 开始心电算法滤波/硬件信号质量,测试模式
     */
    var ECG_filterOrHardware_mode = byteArrayOf(
        0xaa.toByte(),
        0x55.toByte(),
        0x30.toByte(),
        0x02.toByte(),
        0x03.toByte(),
        0x00.toByte()
    )

    //查询mcu固件版本
    var MCU_Version = byteArrayOf(0xaa.toByte(), 0x55.toByte(), 0xf0.toByte(), 0x03, 0x01, 0x02, 0)


    //--------------------------------------蓝牙---------------------------------------------------------

    //系统信息
    val systemInfo = byteArrayOf(
        0x93.toByte(),
        0x8e.toByte(),
        0x05.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x8E.toByte()
    )

    //设备确认
    val devAck = byteArrayOf(
        0x93.toByte(),
        0x8e.toByte(),
        0x08.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x01.toByte(),
        0x43.toByte(),
        0x4f.toByte(),
        0x4e.toByte(),
        0x54.toByte(),
        0x46.toByte()
    )

    //时钟同步
    val synTime = byteArrayOf(
        0x93.toByte(),
        0x8e.toByte(),
        0x0a.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x02.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    //时钟读取
    val readTime = byteArrayOf(
        0x93.toByte(),
        0x8e.toByte(),
        0x04.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x03.toByte(),
        0x10.toByte()
    )

    //单条数据传输
    val oneTrans = byteArrayOf(
        0x93.toByte(),
        0x8e.toByte(),
        0x04.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x04.toByte(),
        0x11.toByte()
    )

    //全部数据传输
    val allTrans = byteArrayOf(
        0x93.toByte(),
        0x8e.toByte(),
        0x04.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x05.toByte(),
        0x12.toByte()
    )

    //数据删除指令
    val delData = byteArrayOf(
        0x93.toByte(),
        0x8e.toByte(),
        0x04.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x06.toByte(),
        0x13.toByte()
    )

    //关闭蓝牙指令
    val offBle = byteArrayOf(
        0x93.toByte(),
        0x8e.toByte(),
        0x04.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x09.toByte(),
        0x16.toByte()
    )

    //仪器关机指令
    val offDev = byteArrayOf(
        0x93.toByte(),
        0x8e.toByte(),
        0x04.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x0a.toByte(),
        0x17.toByte()
    )

    //开始测试指令
    val startMeasure = byteArrayOf(
        0x93.toByte(),
        0x8e.toByte(),
        0x04.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x0b.toByte(),
        0x18.toByte()
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