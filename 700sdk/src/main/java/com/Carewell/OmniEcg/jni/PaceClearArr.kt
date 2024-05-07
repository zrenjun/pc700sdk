package com.Carewell.OmniEcg.jni

import java.util.ArrayList
import kotlin.jvm.Synchronized
import kotlin.math.abs

@Suppress("unused")
object PaceClearArr {
    /**
     * 存储最近x个点的数据，超过x点开始返回
     *
     */
    private var src: MutableList<List<Short>> = ArrayList()
    private var pace: MutableList<Int> = ArrayList()
    private var lead: MutableList<Int> = ArrayList()
    private var x = 8 //必须大于6
    @Synchronized
    fun feed(ss: ShortArray, isLeadOff: Int, isPace: Int): ShortArray? {
        val temp = mutableListOf<Short>()
        for (s in ss) {
            temp.add(s)
        }
        src.add(temp)
        pace.add(isPace)
        lead.add(isLeadOff)
        /**
         * 数据不足，不处理
         */
        if (src.size <= x) {
            return null
        }
        val out = src[0]
        val outLead = lead[0]
        val outPace = pace[0]
        src.removeAt(0)
        pace.removeAt(0)
        lead.removeAt(0)
        /**
         * 如果当前数组中没 pace 标记，则直接返回
         * 只处理当pace的位置在当前数组的中央
         */
        val key = x / 2
        if (pace[key] == 1) {
            for (i in key - 3 until key + 3) {
                if (abs(src[i - 1][0] - src[i][0]) > 150) {
                    // 差异过大
                    src[i] = src[i - 1]
                }
            }
        }
        val size = out.size + 2
        val so = ShortArray(size)
        for (i in out.indices) {
            so[i] = out[i]
        }
        so[size - 2] = outLead.toShort()
        so[size - 1] = outPace.toShort()
        return so
    }

    fun reset() {
        src.clear()
        pace.clear()
    }
}