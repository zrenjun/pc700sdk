package com.Carewell.ecg700

/**
 * PC700,新PC600,900 的IAP固件升级回调
 */
interface IIAPCallBack {
    /**
     * 下位机进入IAP模式
     */
    fun onIAP_enter()

    /**
     * 启动IAP
     */
    fun onIAP_start()

    /**
     * 正在烧录固件
     * @param program 进度
     */
    fun onIAP_programing(program: Int)

    /**
     * 检测烧录的每一帧,出错回调
     * @param err 出错信息
     */
    fun onIAP_check(err: String)

    /**
     * 结束IAP
     */
    fun onIAP_end()

    /**
     * IPA版本
     * @param hardVer 硬件版本
     * @param softVer 软件版本
     * @param response
     * 0x00:下位机需要先执行软复位 <br></br>
     * 0x01:下位机已经准备好，可以执行下面的步骤，启动固件更新。 <br></br>
     * 0x02:下位机仅回应版本号。<br></br>
     * 0x0F:无法升级指定MCU。
     */
    fun onIAP_version(hardVer: Int, softVer: Int, response: Byte)
}