package com.Carewell.ecg700.port

import android.graphics.Bitmap
import java.io.Serializable
import java.lang.Exception
import java.util.*
import java.util.regex.Pattern

class IDCard : Serializable {
    /**
     * 姓名
     */
    var name: String? = null

    /**
     * 性别
     * 1 男 2 女
     */
    var sex: String? = null

    /**
     * 名族
     */
    private var nation: String? = null

    /**
     * 出生日期
     */
    var birthday: String? = null

    /**
     * 住址
     */
    var address: String? = null

    /**
     * 身份证编号
     */
    var iDCardNo: String? = null
    /**
     * 获取发证机关
     */
    /**
     * 设置发证机关
     */
    /**
     * 发证机关
     */
    var grantDept: String? = null

    /**
     * 有效期开始
     */
    var userLifeBegin: String? = null

    /**
     * 有效期结束
     */
    var userLifeEnd: String? = null

    /**
     * 图像信息
     */
    var headBitmap: Bitmap? = null

    /**
     * 名族
     */
    private val nations = arrayOf(
        "解码错",  // 00
        "汉",  // 01
        "蒙古",  // 02
        "回",  // 03
        "藏",  // 04
        "维吾尔",  // 05
        "苗",  // 06
        "彝",  // 07
        "壮",  // 08
        "布依",  // 09
        "朝鲜",  // 10
        "满",  // 11
        "侗",  // 12
        "瑶",  // 13
        "白",  // 14
        "土家",  // 15
        "哈尼",  // 16
        "哈萨克",  // 17
        "傣",  // 18
        "黎",  // 19
        "傈僳",  // 20
        "佤",  // 21
        "畲",  // 22
        "高山",  // 23
        "拉祜",  // 24
        "水",  // 25
        "东乡",  // 26
        "纳西",  // 27
        "景颇",  // 28
        "柯尔克孜",  // 29
        "土",  // 30
        "达斡尔",  // 31
        "仫佬",  // 32
        "羌",  // 33
        "布朗",  // 34
        "撒拉",  // 35
        "毛南",  // 36
        "仡佬",  // 37
        "锡伯",  // 38
        "阿昌",  // 39
        "普米",  // 40
        "塔吉克",  // 41
        "怒",  // 42
        "乌孜别克",  // 43
        "俄罗斯",  // 44
        "鄂温克",  // 45
        "德昴",  // 46
        "保安",  // 47
        "裕固",  // 48
        "京",  // 49
        "塔塔尔",  // 50
        "独龙",  // 51
        "鄂伦春",  // 52
        "赫哲",  // 53
        "门巴",  // 54
        "珞巴",  // 55
        "基诺",  // 56
        "编码错",  // 57
        "其他",  // 97
        "外国血统" // 98
    )

    /**
     * 获取民族信息
     *
     * @return
     * @see .getNationName
     */
    fun getNation(): String? {
        val pattern = Pattern.compile("[0-9]*")
        val isNum = pattern.matcher(nation)
        return if (isNum.matches()) {
            getNationName(nation)
        } else {
            nation
        }
    }

    /**
     * 获取名族
     */
    fun getNationName(nation: String?): String? {
        if (nation == null) return null
        if (!nation.matches(Regex("[0-9]{2}"))) Exception("民族代码错误")
        when (val nationCode = nation.toInt()) {
            in 1..56 -> this.nation = nations[nationCode]
            97 -> this.nation = "其他"
            98 -> this.nation = "外国血统中国籍人士"
            else -> this.nation = "编码错误"
        }
        return this.nation
    }

    /**
     * 设置民族
     */
    fun setNation(nation: String?) {
        this.nation = nation
    }

    override fun toString(): String {
        return ("IDCard [name=" + name + ", sex=" + sex + ", nation=" + nation + ", birthday=" + birthday + ", address="
                + address + ", idcardno=" + iDCardNo + ", grantdept=" + grantDept + ", " + "userlifebegin="
                + userLifeBegin + ", userlifeend=" + userLifeEnd + ", nations=" + Arrays.toString(
            nations
        ) + "wlt "
                + "size=" + "]")
    }

    companion object {
        private const val serialVersionUID = -3779329360662121573L
    }
}

