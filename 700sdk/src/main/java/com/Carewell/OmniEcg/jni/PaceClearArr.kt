package com.Carewell.OmniEcg.jni

import kotlin.jvm.Synchronized
import kotlin.math.abs

@Suppress("unused")
object PaceClearArr {
    /**
     * 存储最近 x 个点的数据，超过 x 点开始返回。
     *
     * 零分配重写说明：
     * 原实现每帧都 new 一个 MutableList<Short>（8 个 short 逐个自动装箱成 Short 对象）、
     * 塞进 List<List<Short>> 再 removeAt(0)（ArrayList 头删要整体搬移），每秒 1000 次，
     * 在 1000Hz 热路径上是明显的分配/装箱/GC 压力，高温降频时尤甚。
     * 现改为固定容量的原始数组环形缓冲：每帧只写入几个基本类型槽位，无对象分配、无装箱、无头部搬移。
     * 对外行为（攒够 x+1 帧才出数、取最老帧输出、对移除队头后的窗口中央做起搏平滑、
     * 返回数组末两位为该最老帧的 lead/pace）与原实现保持一致。
     */
    private const val LEAD_NUM = 8            // 每帧导联数（原 ss.size 恒为 8）
    private var x = 8                          // 必须大于 6（与原实现一致）
    private const val CAP = 9                  // 环容量 = x + 1，可同时容纳新入帧与待判断窗口

    // 环形缓冲：波形按 [槽位][导联] 存放；lead/pace 各一环。
    private val ringWave = Array(CAP) { ShortArray(LEAD_NUM) }
    private val ringLead = IntArray(CAP)
    private val ringPace = IntArray(CAP)
    private var head = 0     // 指向当前最老元素
    private var size = 0     // 当前窗口内元素个数

    // 复用的输出缓冲：末两位放 lead/pace，长度 = 导联数 + 2
    private val out = ShortArray(LEAD_NUM + 2)

    // 逻辑下标 i（0 = 最老）映射到物理环下标
    private fun idx(i: Int): Int {
        var p = head + i
        if (p >= CAP) p -= CAP
        return p
    }

    @Synchronized
    fun feed(ss: ShortArray, isLeadOff: Int, isPace: Int): ShortArray? {
        // 入队新帧（写入 head+size 位置）
        val writePos = idx(size)
        val w = ringWave[writePos]
        val n = if (ss.size < LEAD_NUM) ss.size else LEAD_NUM
        for (i in 0 until n) w[i] = ss[i]
        // 若来帧不足 8 导，剩余置 0，保证槽位不残留上一轮数据
        for (i in n until LEAD_NUM) w[i] = 0
        ringLead[writePos] = isLeadOff
        ringPace[writePos] = isPace
        size++

        // 数据不足（未超过 x），不处理。等价于原来的 src.size <= x 才继续。
        if (size <= x) return null

        // 取最老一帧作为输出（等价于原 src[0]/lead[0]/pace[0]）
        val oldWavePos = head
        val outLead = ringLead[oldWavePos]
        val outPace = ringPace[oldWavePos]
        // 先把最老帧波形拷到输出缓冲（在下面移除队头前取，语义等价于原 out = src[0]）
        val oldWave = ringWave[oldWavePos]
        for (i in 0 until LEAD_NUM) out[i] = oldWave[i]

        // 移除队头：head 前移、size 减一（等价于三个 list 的 removeAt(0)）
        head = idx(1)
        size--

        // 移除队头后，对窗口中央的起搏点做平滑修正。
        // 原实现：key = x/2；若 pace[key] == 1，则对 src[key-3 until key+3]（即下标 key-3..key+2）
        // 逐点判断相邻差值 > 150 则用前一点覆盖当前点。此处 src 指移除队头后的窗口，逻辑下标从 0 起。
        val key = x / 2
        if (key < size && ringPace[idx(key)] == 1) {
            for (i in key - 3 until key + 3) {
                // 保护：i-1 与 i 必须都在当前窗口范围内（原实现下标固定，这里显式护边避免越界）
                if (i - 1 < 0 || i >= size) continue
                val cur = ringWave[idx(i)]
                val prev = ringWave[idx(i - 1)]
                // 原实现只比较每帧第 0 导（src[i][0]）的差值，超阈值则整帧用前一帧替换
                if (abs(prev[0] - cur[0]) > 150) {
                    for (d in 0 until LEAD_NUM) cur[d] = prev[d]
                }
            }
        }

        out[LEAD_NUM] = outLead.toShort()       // 末第二位：lead
        out[LEAD_NUM + 1] = outPace.toShort()   // 末位：pace
        return out
    }

    @Synchronized
    fun reset() {
        head = 0
        size = 0
        // 清空槽位，避免复位后残留上一轮数据（原实现漏清 lead，这里一并清全，功能更正确且不影响 feed 语义）
        for (i in 0 until CAP) {
            ringLead[i] = 0
            ringPace[i] = 0
            val w = ringWave[i]
            for (d in 0 until LEAD_NUM) w[d] = 0
        }
    }
}
