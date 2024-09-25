package com.Carewell.ecg700.port

import com.Carewell.ecg700.ParseData
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
class SphThreads(private var inputStream: InputStream,
                 private var listener: OnSerialPortDataListener
) {
    private var scope = CoroutineScope(Dispatchers.IO)
    private val buffer = ByteArray(4400) //22倍数  多缓存一点，以防万一 好像最大4095
    private var mReceiveBuffer = Vector<Byte>()

    //普通命令
    //aa, 55, 30, 02, 01, c6,
    private val routineHead1 = 0xaa.toByte()
    private val routineHead2 = 0x55.toByte()

    //12导
    //数据帧
    //7f, 81, 00, fe, ff, 00, 00, fe, ff, 00, 00, 02, 00, 00, 00, 00, 00, 02, 00, 00, 00, fe,
    //回复帧
    //7f, c2, 00, 02, 00, 81, 08, 01, 00, 56, 31, 2e, 30, 2e, 30, 2e, 30, 00, 00, 00, 00, 6e,
    private val ecg12Head1 = 0x7f.toByte()
    private val ecg12DataHead2 = 0x81.toByte() //透传数据
    private val ecg12CmdHead2 = 0xc2.toByte() //命令回复

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
    private var time = 0

//    init {
//        scope.launch(Dispatchers.IO) {
//            while (scope.isActive) {
//                if (flag) {
//                    try {
//                        if (inputStream.available() > 0) {
//                            time = 0
//                            val len = inputStream.read(buffer) // 读取数据
//                            if (len < 22) {
//                                LogUtil.v("队列数据---->${HexUtil.bytesToHexString(mReceiveBuffer.toByteArray())}")
//                                LogUtil.v(
//                                    "读取-->${
//                                        HexUtil.bytesToHexString(
//                                            buffer.copyOfRange(
//                                                0,
//                                                len
//                                            )
//                                        )
//                                    }"
//                                )
//                            }
//                            if (len == buffer.size) {
//                                Arrays.fill(buffer, 0.toByte())
//                            } else {
//                                for (i in 0 until len) {
//                                    mReceiveBuffer.add(buffer[i])
//                                }
//                                while (mReceiveBuffer.size > 5) {
//                                    // 数据尾
//                                    var end = -1
//                                    if (mReceiveBuffer[0] == routineHead1 && mReceiveBuffer[1] == routineHead2) {
//                                        val length = mReceiveBuffer[3].toInt() and 0xFF//长度
//                                        if (mReceiveBuffer.size >= length + 4) {
//                                            end = length + 4
//                                            //验证下一包头
//                                            if (mReceiveBuffer.size > end && mReceiveBuffer[end] != routineHead1 && mReceiveBuffer[end] == ecg12Head1) {
//                                                end = -1
//                                                LogUtil.v(
//                                                    "异常队列数据---->${
//                                                        HexUtil.bytesToHexString(
//                                                            mReceiveBuffer.toByteArray()
//                                                        )
//                                                    }"
//                                                )
//                                            }
//                                        }
//                                    }
//                                    if (mReceiveBuffer[0] == ecg12Head1 && (mReceiveBuffer[1] == ecg12DataHead2 || mReceiveBuffer[1] == ecg12CmdHead2)) {
//                                        if (mReceiveBuffer.size >= 22) {
//                                            end = 22
//                                        }
//                                    }
//                                    if (end > 0) {
//                                        val data = ByteArray(end)
//                                        for (i in 0 until end) {
//                                            val curByte = mReceiveBuffer.removeAt(0)
//                                            data[i] = curByte
//                                            if (end == 22 && mReceiveBuffer.size > 1 && mReceiveBuffer[0] == routineHead1 && mReceiveBuffer[1] == routineHead2) {  //异常数据且不能拼接了
//                                                break
//                                            }
//                                        }
//                                        if (data[0] == ecg12Head1) {
//                                            ParseEcg12Data.addData(data)
//                                        } else {
//                                            listener.onDataReceived(data) //发送下一个命令
//                                            ParseData.processingOrdinaryData(data)
//                                        }
//                                    } else {
//                                        mReceiveBuffer.removeAt(0)
//                                    }
//                                }
//                            }
//                        } else {
//                            delay(3)
//                            time += 3
//                            if (time  > 1000 * 60) {
//                                LogUtil.v("已经一分钟未读到数据了")
//                                time = 0
//                            }
//                        }
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
//            }
//        }
//    }

    init {
        scope.launch(Dispatchers.IO) {
            while (scope.isActive) {
                if (flag) {
                    processInputStream()
                }
            }
        }
    }

    private suspend fun processInputStream() {
        try {
            if (inputStream.available() > 0) {
                time = 0
                val len = inputStream.read(buffer)
                handleReceivedData(len)
            } else {
                delay(3)
                time += 3
                if (time > 60000) { // 60 seconds in milliseconds
                    LogUtil.v("已经一分钟未读到数据了")
                    time = 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleReceivedData(len: Int) {
        if (len < 22) {
            LogUtil.v("队列数据----> ${HexUtil.bytesToHexString(mReceiveBuffer.toByteArray())}")
            LogUtil.v("读取--> ${HexUtil.bytesToHexString(buffer.copyOfRange(0, len))}")
        }

        if (len == buffer.size) {
            Arrays.fill(buffer, 0.toByte())
        } else {
            mReceiveBuffer.addAll(buffer.copyOfRange(0, len).toList())
            processReceiveBuffer()
        }
    }

    private fun processReceiveBuffer() {
        while (mReceiveBuffer.size > 5) {
            val end = findEndOfPacket()
            if (end > 0) {
                val data = ByteArray(end)
                for (i in 0 until end) {
                    data[i] = mReceiveBuffer.removeFirst()
                    if (end == 22 && mReceiveBuffer.size > 1 &&
                        mReceiveBuffer[0] == routineHead1 && mReceiveBuffer[1] == routineHead2
                    ) {//异常数据且不能拼接了
                        break
                    }
                }
                handleParsedData(data)
            } else {
                mReceiveBuffer.removeFirst()
            }
        }
    }

    private fun findEndOfPacket(): Int {
        if (mReceiveBuffer[0] == routineHead1 && mReceiveBuffer[1] == routineHead2) {
            val length = mReceiveBuffer[3].toInt() and 0xFF
            if (mReceiveBuffer.size >= length + 4) {
                val potentialEnd = length + 4
                if (mReceiveBuffer.size > potentialEnd &&
                    mReceiveBuffer[potentialEnd] != routineHead1 &&
                    mReceiveBuffer[potentialEnd] == ecg12Head1
                ) {
                    LogUtil.v("异常队列数据---->${HexUtil.bytesToHexString(mReceiveBuffer.toByteArray())}")
                    return -1
                }
                return potentialEnd
            }
        }

        if (mReceiveBuffer[0] == ecg12Head1 && (mReceiveBuffer[1] == ecg12DataHead2 || mReceiveBuffer[1] == ecg12CmdHead2)) {
            if (mReceiveBuffer.size >= 22) {
                return 22
            }
        }

        return -1
    }

    private fun handleParsedData(data: ByteArray) {
        if (data[0] == ecg12Head1) {
            ParseEcg12Data.addData(data)
        } else {
            listener.onDataReceived(data)//发送下一个命令
            ParseData.processingOrdinaryData(data)
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