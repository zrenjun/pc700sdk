package com.Carewell.ecg700

import com.lepu.forwarder.util.HexUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Arrays
import java.util.Vector


/**
 *
 *  说明: 数据读取
 *  zrj 2022/3/25 15:03
 *
 */
class SphThreads(inputStream: InputStream, listener: OnSerialPortDataListener) {
    private var scope = CoroutineScope(Dispatchers.IO)
    private val buffer = ByteArray(4400) //22倍数  多缓存一点，以防万一 好像最大4095
    private var mReceiveBuffer = Vector<Byte>()

    //普通命令
    //aa, 55, 30, 02, 01, c6,
    private val head1 = 0xaa.toByte()
    private val head2 = 0x55.toByte()

    //12导
    //数据帧
    //7f, 81, 00, fe, ff, 00, 00, fe, ff, 00, 00, 02, 00, 00, 00, 00, 00, 02, 00, 00, 00, fe,
    //回复帧
    //7f, c2, 00, 02, 00, 81, 08, 01, 00, 56, 31, 2e, 30, 2e, 30, 2e, 30, 00, 00, 00, 00, 6e,
    private val head3 = 0x7f.toByte()
    private val head4 = 0x81.toByte() //透传数据
    private val head5 = 0xc2.toByte() //命令回复

    //发送12导停止测量
    //    7f c1 00 02 00 00 00 00 00 00 00 42
    //接收到12导停止测量
    //    7f, c2, 00, 02, 00, 81, 08, 01, 00, 56, 31, 2e, 30, 2e, 30, 2e, 30, 00, 00, 00, 00, 6e,

    //发送12导停止透传
    //    aa 55 30 02 02 24
    //接收到12导停止透传
    //   aa, 55, 30, 02, 02, aa,
    //单导
    //aa, 55, 32, 37, 01, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 88, 00, 00, 01, 78,


    @Volatile
    private var flag = true

    init {
        scope.launch(Dispatchers.IO) {
            while (scope.isActive) {
                if (flag) {
                    try {
                        if (inputStream.available() > 0) {
                            val len = inputStream.read(buffer) // 读取数据
                            if (len == buffer.size) {
                                LogUtil.v(
                                    "缓存  len ---->  $len 数据---->${
                                        HexUtil.bytesToHexString(
                                            buffer
                                        )
                                    }"
                                )
                                Arrays.fill(buffer, 0.toByte())
                            } else {
                                if (len % 22 != 0 && buffer[0] != head1) {
                                    LogUtil.v(
                                        "len ---->  $len  需要拼接${if (mReceiveBuffer.size == 0) "下一包" else "上一包"} ${
                                            HexUtil.bytesToHexString(buffer.copyOfRange(0, len))
                                        }"
                                    )
                                }
                                for (i in 0 until len) {
                                    mReceiveBuffer.add(buffer[i])
                                }
                                while (true) {
                                    if (mReceiveBuffer.size > 5) {
                                        // 数据尾
                                        var end = -1
                                        if (mReceiveBuffer[0] == head1 && mReceiveBuffer[1] == head2) {
                                            val length = mReceiveBuffer[3].toInt() and 0xFF//长度
                                            end = length + 4
                                        }
                                        if (mReceiveBuffer[0] == head3 && (mReceiveBuffer[1] == head4 || mReceiveBuffer[1] == head5)) {
                                            end = 22
                                        }
                                        if (end > 0) {
                                            if (end <= mReceiveBuffer.size) {
                                                val data = ByteArray(end)
                                                for (i in 0 until end) {
                                                    val curByte = mReceiveBuffer.removeAt(0)
                                                    data[i] = curByte
                                                    if (end == 22 && mReceiveBuffer.size > 1 && mReceiveBuffer[0] == head1 && mReceiveBuffer[1] == head2) {  //异常数据且不能拼接了
                                                        break
                                                    }
                                                }
                                                if (data[0] == head3) {
                                                    if (!checkSum(data[21], data)) {
                                                        //异常数据 ---->  7f, 81, 01, 00, 00, 00, 00, 00, 00, 00, 00, aa, 55, ff, 08, 01, 50, 43, 37, 30, 30, 00,
                                                        LogUtil.v(
                                                            "异常数据 ---->  ${
                                                                HexUtil.bytesToHexString(
                                                                    data
                                                                )
                                                            }"
                                                        )
                                                        break
                                                    }
                                                    ParseEcg12Data.addData(data)
                                                    if (data[1] == head5) {
                                                        listener.onDataReceived(data)
                                                        if (data[3] == 0x02.toByte()) { //停止12导测量 回复
                                                            ParseEcg12Data.clear()
                                                        }
                                                    }
                                                } else {
                                                    listener.onDataReceived(data)
                                                    ParseData.processingOrdinaryData(data)
                                                }
                                            } else {
                                                break
                                            }
                                        } else {
                                            mReceiveBuffer.removeAt(0)
                                        }
                                    } else {
                                        break
                                    }
                                }
                            }
                        } else {
                            delay(3)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun stop() {
        try {
            mReceiveBuffer.clear()
            if (scope.isActive) {
                scope.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        flag = false
    }

    fun reStart() {
        flag = true
    }
}

//12导校验
fun checkSum(crc: Byte, curByteBuffer: ByteArray): Boolean {
    var checkSum: Byte = 0x00
    for (i in 0..20) {
        checkSum = (curByteBuffer[i] + checkSum).toByte()
    }
    return checkSum == crc
}