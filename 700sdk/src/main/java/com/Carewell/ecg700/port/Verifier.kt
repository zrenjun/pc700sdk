package com.Carewell.ecg700.port

import java.lang.Exception
import java.util.*

/**
 * CRC校验
 */
class Verifier : BaseProtocol() {
    override fun checkIntactCnt(buffer: Vector<Byte>): Int {
        var cnt = 0
        var begin = 0
        try {
            while (begin < buffer.size - 5) {
                if (buffer[begin] == HEAD1 && buffer[begin + 1] == HEAD2) {
                    val len = buffer[begin + 3].toInt() and 0xff
                    val crc = begin + 3 + len
                    if (buffer.size > crc) {
                        try {
                            if (checkCRC(buffer, begin, crc)) {
                                begin = crc + 1
                                cnt++
                            } else {
                                buffer.removeAt(begin)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            buffer.removeAt(begin)
                        }
                    } else {
                        return cnt
                    }
                } else {
                    buffer.removeAt(begin)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cnt
    }

    companion object {
        private const val HEAD1 = 0xaa.toByte()
        private const val HEAD2 = 0x55.toByte()
    }
}