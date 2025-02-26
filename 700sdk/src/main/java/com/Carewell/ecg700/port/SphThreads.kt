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




/**
 *
 *  说明: 数据读取
 *  zrj 2022/3/25 15:03
 *
 */
class SphThreads(
    private var inputStream: InputStream,
    private var isDebug: Boolean = false,
    private var listener: OnSerialPortDataListener
) {
    private var scope = CoroutineScope(Dispatchers.IO)
    private val buffer = ByteArray(4400) //22倍数  最大4095
    private var mReceiveBuffer = ByteArray(4400)

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
    private var index = 0 //当前有效数据长度

    init {
        scope.launch(Dispatchers.IO) {
            while (scope.isActive) {
                if (flag) {
                    try {
                        if (inputStream.available() > 0) {
                            time = 0
                            val len = inputStream.read(mReceiveBuffer)
                            if (len > 0) {
                                if (index + len <= buffer.size) {
                                    System.arraycopy(mReceiveBuffer, 0, buffer, index, len)
                                    index += len
                                } else {
                                    LogUtil.v("Buffer overflow, resetting buffer")
                                    index = 0 // 重置缓冲区
                                }
                                if (isDebug) {
                                    LogUtil.v("当前队列数据---->${HexUtil.bytesToHexString(buffer.copyOfRange(0, index))}")
                                }
                                while (index > 5) {
                                    //查找数据头和数据尾
                                    var start = -1
                                    var end = -1
                                    for (i in 0 until index) {
                                        //非心电
                                        if (buffer[i] == routineHead1 && i + 3 < index && buffer[i + 1] == routineHead2) {
                                            start = i
                                            val length = buffer[i + 3].toInt() and 0xFF
                                            if (index > i + 4 + length) {
                                                // 数据帧长度足够
                                                val potentialEnd = length + 4
                                                if (index > potentialEnd && (buffer[i + potentialEnd] == routineHead1 || buffer[i + potentialEnd] == ecg12Head1)) {
                                                    end = i + 3 + length
                                                } else {
                                                    val pair = deleteStart()
                                                    start = pair.first
                                                    end = pair.second
                                                }
                                            }else if (index == i + length + 4) {
                                                end = i + 3 + length
                                            } else {
                                                // 数据帧不完整，等待更多数据
                                                break
                                            }
                                        }

                                        //心电
                                        if (buffer[i] == ecg12Head1 && i + 1 < index && (buffer[i + 1] == ecg12DataHead2 || buffer[i + 1] == ecg12CmdHead2)) {
                                            start = i
                                            if (index == 22) {
                                                //刚好22   校验失败-->7f, 81, 05, af, 00, de, 00, 61, 01, aa, 55, 30, 02, 02, 24, aa, 55, ff, 03, 03, 44, a9,   校验值-->93
                                                if (checkSum(buffer[i + 21], buffer.copyOfRange(i, i + 22))) {
                                                    end = i + 21
                                                } else {
                                                    val pair = deleteStart()
                                                    start = pair.first
                                                    end = pair.second
                                                }
                                            } else if (index < 22) {
                                                if (buffer.indexOf(routineHead1) + 1 == buffer.indexOf(routineHead2)) {
                                                    val pair = deleteStart()
                                                    start = pair.first
                                                    end = pair.second
                                                }else{//  等待拼接
                                                    break
                                                }
                                            } else {
                                                //大于22
                                                if (buffer[i + 22] == ecg12Head1) {
                                                    end = i + 21
                                                } else if (buffer[i + 22] == routineHead1) {
                                                    if (checkSum(buffer[i + 21], buffer.copyOfRange(i, i + 22))) {  //最后一包也要校验
                                                        end = i + 21
                                                    } else {
                                                        val pair = deleteStart()
                                                        start = pair.first
                                                        end = pair.second
                                                    }
                                                } else {
                                                    val pair = deleteStart()
                                                    start = pair.first
                                                    end = pair.second
                                                }
                                            }
                                        }
                                        //获取到一包数据
                                        if (end != -1) {
                                            break
                                        }
                                    }
                                    //说明前面有脏数据，把数据前移start位
                                    if (start > 0) {
                                        LogUtil.v("异常数据---->${HexUtil.bytesToHexString(buffer.copyOfRange(0, index))}")
                                        var i = 0
                                        while (i < index && i + start < index) {
                                            buffer[i] = buffer[i + start]
                                            i++
                                        }
                                        end -= start
                                        index -= start
                                        start = 0
                                        LogUtil.v("清理后数据---->${HexUtil.bytesToHexString(buffer.copyOfRange(0, index))}")
                                    }
                                    //如果找到了
                                    if (start == 0 && end > 0) {
                                        //先把数据写入真实数据区域
                                        val data = buffer.copyOfRange(0, end + 1)
                                        //然后向左移动数据
                                        for (i in buffer.indices) {
                                            if (i + data.size < index) {
                                                buffer[i] = buffer[i + data.size]
                                            } else {
                                                break
                                            }
                                        }
                                        index -= data.size  //把index前移
                                        handleParsedData(data)
                                    } else {
                                        LogUtil.v("等待拼接---->${HexUtil.bytesToHexString(buffer.copyOfRange(0, index))}")
                                        break
                                    }
                                }
                            }
                        } else {
                            delay(10)
                            time += 10
                            if (time > 60000) {
                                LogUtil.v("已经一分钟未读到数据了")
                                time = 0
                            }
                        }
                    } catch (e: Exception) {
                        index = 0
                        LogUtil.v("当前队列数据---->${HexUtil.bytesToHexString(buffer.copyOfRange(0, index))}")
                        Arrays.fill(buffer, 0.toByte())
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun deleteStart(): Pair<Int, Int> {
        var start = 0
        var end = 0
        val temp = buffer.copyOfRange(2, index) // 默认从2开始 因为前两个字节是固定的
        if (temp.indexOf(routineHead1) + 1 == temp.indexOf(routineHead2)) {
            start = temp.indexOf(routineHead1) + 2
            val length = buffer[start + 3].toInt() and 0xFF
            end = start + 3 + length
        } else if (temp.indexOf(ecg12Head1) + 1 == temp.indexOf(ecg12DataHead2) || temp.indexOf(ecg12Head1) + 1 == temp.indexOf(ecg12CmdHead2)) {
            start = temp.indexOf(ecg12Head1) + 2
            end = start + 21
        } else {
            // 未找到有效数据，丢弃整个缓冲区
            index = 0
            Arrays.fill(buffer, 0.toByte())
        }
        return Pair(start, end)
    }


    private fun handleParsedData(data: ByteArray) {
        if (data.size < 2) {
            LogUtil.v("Invalid data frame: too short")
            return
        }
        if (data[0] == ecg12Head1) {
            if (data.size >= 22 && data[1] == ecg12CmdHead2) {
                LogUtil.v("心电回复帧----> ${HexUtil.bytesToHexString(data)}")
                if (data[3] == 0x01.toByte() || data[3] == 0x02.toByte()) {
                    listener.onDataReceived(data)
                }
            }
            ParseEcg12Data.addData(data)
        } else {
            listener.onDataReceived(data) //发送下一个命令
            ParseData.processingOrdinaryData(data)
        }
    }

    fun stop() {
        pause()
        try {
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
    val check = checkSum == crc
    if (check) {
        return true
    } else {
        LogUtil.v(
            "校验失败-->${HexUtil.bytesToHexString(curByteBuffer)}  校验值-->${
                HexUtil.bytesToHexString(
                    byteArrayOf(checkSum)
                )
            }"
        )
        return false
    }
}