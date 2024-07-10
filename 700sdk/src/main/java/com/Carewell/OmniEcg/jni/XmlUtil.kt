package com.Carewell.OmniEcg.jni

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.ArrayMap
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.ecg700.entity.MacureResultBean
import com.Carewell.view.ecg12.LeadType
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date

object XmlUtil {
    private fun getStringByShortArray(shortArray: ShortArray?): String {
        if (shortArray == null || shortArray.isEmpty()) return ""
        val len = shortArray.size
        val stringBuffer = StringBuffer()
        for (index in 0 until len) {
            stringBuffer.append(shortArray[index].toInt()).append(" ")
        }
        return stringBuffer.toString()
    }

    fun getHl7XmlMvData(context: Context, fileName: String): List<ShortArray> {  //mv 值
        var inputStream: InputStream? = null
        val list = mutableListOf<ShortArray>()
        try {
            val sax = SAXReader()
            inputStream = context.resources.assets.open(fileName)
            val root = sax.read(inputStream).rootElement// 根节点
            var componentElement = root.element("component")
            val seriesElement = componentElement.element("series")
            // 取得某节点下名为"component/series/component/sequenceSet"的所有字节点
            componentElement = seriesElement.element("component")
            val sequenceSetElement = componentElement.element("sequenceSet")
            val nodesComponent = sequenceSetElement.elements("component")
            var element: Element?
            for (obj in nodesComponent) {
                element = obj as Element?
                element?.element("sequence")?.element("value")?.let {
                    // gain
                    val scale = it.element("scale")?.attributeValue("value")?.toFloat() ?: 1f
                    it.element("digits")?.let { digits ->
                        list.add(digits.text.split(" ").map { uv -> uv.toShort() }.toShortArray())
                    }
                }
            }
            return list
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            close(null, null, inputStream)
        }
        return list
    }

    private fun close(xmlWriter: XMLWriter?, out: OutputStream?, ipPut: InputStream?) {
        if (null != xmlWriter) {
            try {
                xmlWriter.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (null != out) {
            try {
                out.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (null != ipPut) {
            try {
                ipPut.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    @SuppressLint("SimpleDateFormat")
    fun makeHl7Xml(
        context: Context,
        patientNumber: String,
        result: MacureResultBean?,
        ecgDataArray: Array<ShortArray>,
        leadType: LeadType,
        filePath: String,
        fileName: String,
        startTime: Long,
        endTime: Long,
        lowPassHz: String,
        hpHz: String,
        acHz: String
    ) {
        val lead1String = getStringByShortArray(ecgDataArray[0])
        val lead2String = getStringByShortArray(ecgDataArray[1])
        val lead3String = getStringByShortArray(ecgDataArray[2])
        val leadaVRString = getStringByShortArray(ecgDataArray[3])
        val leadaVLString = getStringByShortArray(ecgDataArray[4])
        val leadaVFString = getStringByShortArray(ecgDataArray[5])
        val leadV1String = getStringByShortArray(ecgDataArray[6])
        val leadV2String = getStringByShortArray(ecgDataArray[7])
        val leadV3String = getStringByShortArray(ecgDataArray[8])
        val leadV4String = getStringByShortArray(ecgDataArray[9])
        val leadV5String = getStringByShortArray(ecgDataArray[10])
        val leadV6String = getStringByShortArray(ecgDataArray[11])
        val sample = "0.001" // 采样率
        val gain = " 0.9536"
        // 修改xml中的节点内容
        var input: InputStream? = null
        var out: OutputStream? = null
        var writer: XMLWriter? = null
        var doc: Document? = null
        val effectiveTimeLow = SimpleDateFormat("yyyyMMddHHmmss").format(Date(startTime))
        val effectiveTimeHigh = SimpleDateFormat("yyyyMMddHHmmss").format(Date(endTime))

        try {
            val sax = SAXReader()
            input = context.resources.assets.open("ceshi.xml")
            doc = sax.read(input)
            // 根节点
            val root = doc.rootElement
            // 时间戳
            var effectiveTime = root.element("effectiveTime")
            effectiveTime.element("center").addAttribute("value", effectiveTimeLow)
            //id
            var componentOf = root.element("componentOf")
            val timepointEvent = componentOf.element("timepointEvent")
            componentOf = timepointEvent.element("componentOf")
            val subjectAssignment = componentOf.element("subjectAssignment")
            val subject = subjectAssignment.element("subject")
            val trialSubject = subject.element("trialSubject")
            val id = trialSubject.element("id")
            id.attribute("extension").text = patientNumber
            // 采集时间段
            var component = root.element("component")
            val series = component.element("series")
            effectiveTime = series.element("effectiveTime")
            effectiveTime.element("low").addAttribute("value", effectiveTimeLow)
            effectiveTime.element("high").addAttribute("value", effectiveTimeHigh)
            effectiveTime.element("low").text = effectiveTimeLow
            effectiveTime.element("high").text = effectiveTimeHigh
            //保存滤波信息
            val nodes = series.elements("controlVariable")
            for (obj in nodes) {
                val element = obj as Element
                val controlVariable = element.element("controlVariable")
                val code = controlVariable.element("code")
                val codeValue = code.attribute("code").value
                val attribute =
                    controlVariable.element("component").element("controlVariable").element("value")
                        .attribute("value")
                attribute.text = when (codeValue) {
                    "MDC_ECG_CTL_VBL_ATTR_FILTER_LOW_PASS" -> lowPassHz
                    "MDC_ECG_CTL_VBL_ATTR_FILTER_HIGH_PASS" -> hpHz
                    else -> acHz
                }
            }
            //保存原始数据
            component = series.element("component")
            val sequenceSet = component.element("sequenceSet")
            val nodesComponent = sequenceSet.elements("component")
            for (obj in nodesComponent) {
                val element = obj as Element
                val sequenceElement = element.element("sequence")
                val codeElement = sequenceElement.element("code")
                val codeValue = codeElement.attribute("code").value
                if ("TIME_ABSOLUTE" == codeValue) {
                    // 采样
                    val valueElement = sequenceElement.element("value")
                    val incrementElement = valueElement.element("increment")
                    incrementElement.attribute("value").text = sample
                } else if ("MDC_ECG_LEAD_I" == codeValue || "MDC_ECG_LEAD_II" == codeValue
                    || "MDC_ECG_LEAD_III" == codeValue || "MDC_ECG_LEAD_AVR" == codeValue
                    || "MDC_ECG_LEAD_AVL" == codeValue || "MDC_ECG_LEAD_AVF" == codeValue
                    || "MDC_ECG_LEAD_V1" == codeValue || "MDC_ECG_LEAD_V2" == codeValue
                    || "MDC_ECG_LEAD_V3" == codeValue || "MDC_ECG_LEAD_V4" == codeValue
                    || "MDC_ECG_LEAD_V5" == codeValue || "MDC_ECG_LEAD_V6" == codeValue
                ) {
                    val valueElement = sequenceElement.element("value")
                    // gain
                    val scaleElement = valueElement.element("scale")
                    scaleElement.attribute("value").text = gain
                    // data
                    val digitsElement = valueElement.element("digits")
                    digitsElement.text = when (codeValue) {
                        "MDC_ECG_LEAD_I" -> lead1String
                        "MDC_ECG_LEAD_II" -> lead2String
                        "MDC_ECG_LEAD_III" -> lead3String
                        "MDC_ECG_LEAD_AVR" -> leadaVRString
                        "MDC_ECG_LEAD_AVL" -> leadaVLString
                        "MDC_ECG_LEAD_AVF" -> leadaVFString
                        "MDC_ECG_LEAD_V1" -> leadV1String
                        "MDC_ECG_LEAD_V2" -> leadV2String
                        "MDC_ECG_LEAD_V3" -> leadV3String
                        "MDC_ECG_LEAD_V4" -> leadV4String
                        "MDC_ECG_LEAD_V5" -> leadV5String
                        "MDC_ECG_LEAD_V6" -> leadV6String
                        else -> leadV6String
                    }
                }
            }
            //删除没用的节点
            for (obj in nodesComponent) {
                val element = obj as Element
                val sequenceElement = element.element("sequence")
                val codeElement = sequenceElement.element("code")
                val codeValue = codeElement.attribute("code").value
                if ("MDC_ECG_LEAD_I" == codeValue || "MDC_ECG_LEAD_II" == codeValue
                    || "MDC_ECG_LEAD_III" == codeValue || "MDC_ECG_LEAD_AVR" == codeValue
                    || "MDC_ECG_LEAD_AVL" == codeValue || "MDC_ECG_LEAD_AVF" == codeValue
                    || "MDC_ECG_LEAD_V1" == codeValue || "MDC_ECG_LEAD_V2" == codeValue
                    || "MDC_ECG_LEAD_V3" == codeValue || "MDC_ECG_LEAD_V4" == codeValue
                    || "MDC_ECG_LEAD_V5" == codeValue || "MDC_ECG_LEAD_V6" == codeValue
                ) {
                    when (leadType) {
                        LeadType.LEAD_6 -> if ("MDC_ECG_LEAD_V1" == codeValue || "MDC_ECG_LEAD_V2" == codeValue
                            || "MDC_ECG_LEAD_V3" == codeValue || "MDC_ECG_LEAD_V4" == codeValue
                            || "MDC_ECG_LEAD_V5" == codeValue || "MDC_ECG_LEAD_V6" == codeValue
                        ) {
                            element.detach()
                        }

                        LeadType.LEAD_I -> if ("MDC_ECG_LEAD_II" == codeValue || "MDC_ECG_LEAD_III" == codeValue
                            || "MDC_ECG_LEAD_AVR" == codeValue || "MDC_ECG_LEAD_AVL" == codeValue
                            || "MDC_ECG_LEAD_AVF" == codeValue || "MDC_ECG_LEAD_V1" == codeValue
                            || "MDC_ECG_LEAD_V2" == codeValue || "MDC_ECG_LEAD_V3" == codeValue
                            || "MDC_ECG_LEAD_V4" == codeValue || "MDC_ECG_LEAD_V5" == codeValue
                            || "MDC_ECG_LEAD_V6" == codeValue
                        ) {
                            element.detach()
                        }

                        LeadType.LEAD_II -> if ("MDC_ECG_LEAD_I" == codeValue || "MDC_ECG_LEAD_III" == codeValue
                            || "MDC_ECG_LEAD_AVR" == codeValue || "MDC_ECG_LEAD_AVL" == codeValue
                            || "MDC_ECG_LEAD_AVF" == codeValue || "MDC_ECG_LEAD_V1" == codeValue
                            || "MDC_ECG_LEAD_V2" == codeValue || "MDC_ECG_LEAD_V3" == codeValue
                            || "MDC_ECG_LEAD_V4" == codeValue || "MDC_ECG_LEAD_V5" == codeValue
                            || "MDC_ECG_LEAD_V6" == codeValue
                        ) {
                            element.detach()
                        }

                        else -> {}
                    }
                }
            }

           result?.let {
               //注释集
               val subjectOf = series.element("subjectOf")
               val annotationSet = subjectOf.element("annotationSet")
               annotationSet.element("activityTime").attribute("value").text = effectiveTimeLow
               val components = annotationSet.elements("component")
               for (obj in components) {
                   val element = obj as Element
                   val annotation = element.element("annotation")
                   val codeValue = annotation.element("code").attribute("code").value
                   val attribute = annotation.element("value")?.attribute("value")
                   when (codeValue) {
                       "MDC_ECG_HEART_RATE" -> attribute?.text = "${result.aiResultBean.hr}"
                       "MDC_ECG_TIME_PD_PR" -> attribute?.text = "${result.aiResultBean.pr}"
                       "MDC_ECG_TIME_PD_QRS" -> attribute?.text = "${result.aiResultBean.qrs}"
                       "MDC_ECG_TIME_PD_QT" -> attribute?.text = "${result.aiResultBean.qt}"
                       "MDC_ECG_TIME_PD_QTc" -> attribute?.text = "${result.aiResultBean.qTc}"
                       "MDC_ECG_ANGLE_P_FRONT" -> attribute?.text = "${result.aiResultBean.pAxis}"
                       "MDC_ECG_ANGLE_QRS_FRONT" -> attribute?.text = "${result.aiResultBean.qrsAxis}"
                       "MDC_ECG_ANGLE_T_FRONT" -> attribute?.text = "${result.aiResultBean.tAxis}"
                       "ZONCARE_ECG_RV5" -> attribute?.text = "${result.aiResultBean.rV5 / 1000f}"
                       "ZONCARE_ECG_SV1" -> attribute?.text = "${result.aiResultBean.sV1 / 1000f}"
                       //结论
                       "MDC_ECG_INTERPRETATION" -> {
                           annotation.elements("component").forEachIndexed { index, any ->
                               val diagnosis = result.aiResultBean.aiResultDiagnosisBean.diagnosis
                               val ele = any as Element
                               ele.element("annotation").element("value").text =
                                   if (index < diagnosis.size) {
                                       "${map[diagnosis[index].code]}"
                                   } else {
                                       ""
                                   }
                           }
                       }
                   }
               }
               val derivation = series.element("derivation")
               val derivedSeries = derivation.element("derivedSeries")
               effectiveTime = derivedSeries.element("effectiveTime")
               effectiveTime.element("low").text = effectiveTimeLow
               effectiveTime.element("high").text = effectiveTimeHigh
               val sequences = derivedSeries.element("component").elements("sequenceSet")
               for (obj in sequences) {
                   val element = obj as Element
                   val comp = element.element("component")
                   val sequence = comp.element("sequence")
                   val codeValue = sequence.element("code").attribute("code").value
                   val value = sequence.element("value")
                   when (codeValue) {
                       "TIME_RELATIVE" -> value.element("increment").attribute("value").text = sample
                       "MDC_ECG_LEAD_I" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[0]}"

                       "MDC_ECG_LEAD_II" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[1]}"

                       "MDC_ECG_LEAD_III" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[2]}"

                       "MDC_ECG_LEAD_AVR" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[3]}"

                       "MDC_ECG_LEAD_AVL" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[4]}"

                       "MDC_ECG_LEAD_AVF" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[5]}"

                       "MDC_ECG_LEAD_V1" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[6]}"

                       "MDC_ECG_LEAD_V2" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[7]}"

                       "MDC_ECG_LEAD_V3" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[8]}"

                       "MDC_ECG_LEAD_V4" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[9]}"

                       "MDC_ECG_LEAD_V5" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[10]}"

                       "MDC_ECG_LEAD_V6" -> value.element("digits").text =
                           "${result.aiResultBean.waveForm12[11]}"
                   }
               }
           }

            // 输出格式
            val outformat = OutputFormat()
            // 指定XML编码
            outformat.encoding = "UTF-8"
            outformat.isNewlines = true
            outformat.setIndent(true)
            outformat.isTrimText = true
            //保存到本地
            var file = createDir(filePath)
            LogUtil.v("file" + file!!.path + "/" + fileName + ".xml")
            file = File(file.path + "/" + fileName + ".xml")
            out = FileOutputStream(file)
            writer = XMLWriter(out, outformat)
            writer.write(doc)
            scamFile(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                writer?.close()
                out?.close()
                input?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        doc?.asXML()
    }


    /**
     * 递归创建文件夹
     *
     * @param path 文件夹路径
     */
    fun createDir(path: String?): File? {
        if (path == null || path == "") {
            return null
        }
        val file = File(path)
        if (file.exists()) return file
        val p = file.parentFile
        if (p?.exists() == false) {
            createDir(p.path)
        }
        if (file.mkdir()) return file
        return null
    }

    /*
     * 扫描文件
     * 使文件能被发现
     * */
    private fun scamFile(context: Context?, file: File) {
        MediaScannerConnection.scanFile(
            context, arrayOf(file.absolutePath),
            null
        ) { _: String?, _: Uri? -> }
    }

    var map = ArrayMap<String, String>().apply {
        put("872", "心房扑动")
        put("871", "心房颤动")
        put("870", "房性心动过速")
        put("869", "加速性室性自主心律")
        put("868", "非阵发性交界性心动过速")
        put("867", "室性期前收缩二连发")
        put("866", "室上性期前收缩二连发")
        put("863", "室性心动过速")
        put("861", "室上性心动过速")
        put("848", "室性期前收缩二联律")
        put("847", "室上性期前收缩二联律")
        put("844", "室性期前收缩三联律")
        put("843", "室上性期前收缩三联律")
        put("842", "室性期前收缩")
        put("841", "室上性期前收缩")
        put("837", "室性逸搏心律")
        put("836", "交界性逸搏心律")
        put("835", "房性逸搏心律")
        put("834", "室性逸搏")
        put("833", "交界性逸搏")
        put("832", "房性逸搏")
        put("823", "窦性心动过速伴不齐")
        put("822", "窦性心动过缓伴不齐")
        put("821", "窦性心律不齐")
        put("812", "窦性心动过速")
        put("811", "窦性心动过缓")
        put("805", "心室停博")
        put("768", "右心室心肌梗塞（可能急性）")
        put("758", "右心室心肌梗塞（不确定时期）")
        put("748", "可能右心室心肌梗塞")
        put("766", "前侧壁心肌梗塞（可能急性）")
        put("756", "前侧壁心肌梗塞（不确定时期）")
        put("746", "可能前侧壁心肌梗塞")
        put("765", "后壁心肌梗塞（可能急性）")
        put("755", "后壁心肌梗塞（不确定时期）")
        put("745", "可能后壁心肌梗塞")
        put("735", "怀疑后壁心肌梗塞")
        put("764", "前间壁心肌梗死（可能近期）")
        put("754", "前间壁心肌梗死")
        put("744", "可能前间壁心肌梗死")
        put("734", "怀疑前间壁心肌梗死")
        put("773", "下壁心肌梗死（可能急性）")
        put("763", "下壁心肌梗死（可能近期）")
        put("753", "下壁心肌梗死")
        put("743", "可能下壁心肌梗死")
        put("733", "怀疑下壁心肌梗死")
        put("772", "侧壁心肌梗死（可能急性）")
        put("762", "侧壁心肌梗死（可能近期）")
        put("752", "侧壁心肌梗死")
        put("742", "可能侧壁心肌梗死")
        put("732", "怀疑侧壁心肌梗死")
        put("791", "可能广泛前壁心肌梗死")
        put("781", "广泛前壁心肌梗死")
        put("771", "前壁心肌梗死（可能急性）")
        put("761", "前壁心肌梗死（可能近期）")
        put("751", "前壁心肌梗死")
        put("741", "可能前壁心肌梗死")
        put("731", "怀疑前壁心肌梗死")
        put("711", "异常Q波")
        put("701", "R波递增不良")
        put("673", "显著ST段抬高")
        put("672", "中度ST段抬高")
        put("671", "轻度ST段抬高")
        put("663", "显著ST段压低")
        put("662", "中度ST段压低")
        put("661", "轻度ST段压低")
        put("651", "T波高尖")
        put("631", "T波双向")
        put("623", "巨大T波倒置")
        put("622", "T波深倒置")
        put("621", "T波倒置")
        put("611", "T波低平")
        put("512", "左后束支阻滞")
        put("511", "左前束支阻滞")
        put("510", "怀疑左前束支阻滞")
        put("505", "完全性左束支阻滞")
        put("504", "完全性右束支阻滞")
        put("502", "心室内阻滞")
        put("501", "不完全右束支阻滞")
        put("421", "人工心脏起搏器")
        put("417", "二度II型窦房阻滞（莫氏型）")
        put("416", "二度I型窦房阻滞（文氏型）")
        put("415", "三度房室阻滞")
        put("414", "房室阻滞（2:1）")
        put("413", "二度II型房室阻滞（莫氏型）")
        put("412", "二度I型房室阻滞（文氏型）")
        put("411", "一度房室阻滞")
        put("410", "P-R间期延长")
        put("403", "怀疑WPW征候群")
        put("402", "WPW征候群")
        put("401", "P-R间期缩短")
        put("315", "左心室肥大")
        put("314", "左右心室肥大")
        put("313", "左右心房扩大")
        put("308", "右心房扩大")
        put("307", "左心房扩大")
        put("306", "右心室肥大")
        put("304", "怀疑左心室肥大")
        put("303", "怀疑右心室肥大")
        put("301", "左心室高电压")
        put("206", "S1，S2，S3图形")
        put("205", "心电轴左偏")
        put("204", "显著心电轴右偏")
        put("203", "心电轴右偏")
        put("201", "无法确定的心电轴")
        put("151", "右位心（请检查电极位置）")
        put("142", "QT缩短")
        put("141", "QT延长")
        put("133", "低电位差（全部导联）")
        put("132", "低电位差（胸部导联）")
        put("131", "低电位差（四肢导联）")
        put("122", "顺钟向转位")
        put("121", "逆钟向转位")
        put("113", "导联脱落")
        put("112", "左右上肢电极接反")
        put("111", "记录不良")
        put("110", "窦性心律")
        put("101", "正常范围内")
    }
}

//{
//    "aiResultBean": {
//        "HR": 60,
//        "PA": 0,
//        "PAxis": 46,
//        "PB": 0,
//        "PE": 0,
//        "PR": 159,
//        "Pd": 86,
//        "QA": 0,
//        "QRS": 83,
//        "QRSAxis": 46,
//        "QRSB": 0,
//        "QRSE": 0,
//        "QT": 369,
//        "QTc": 369,
//        "RA": 0,
//        "RV5": 1234,
//        "RV6": 1231,
//        "SA": 0,
//        "ST20A": 0,
//        "ST40A": 0,
//        "ST60A": 0,
//        "ST80A": 0,
//        "SV1": 975,
//        "SV2": 1318,
//        "TA": 0,
//        "TAxis": 47,
//        "TB": 0,
//        "TE": 0,
//        "aiResultDiagnosisBean": {
//            "diagnosis": [
//                {
//                    "code": "110",
//                    "createTime": 0,
//                    "id": 0,
//                    "lead": ""
//                },
//                {
//                    "code": "308",
//                    "createTime": 0,
//                    "id": 0,
//                    "lead": ""
//                }
//            ],
//            "minnesotacodes": [
//                "9-3-1"
//            ]
//        },
//        "aiResultType": 0,
//        "arrDiagnosis": "JSON:{\"diagnosis\":[{\"code\":\"110\",\"lead\":\"\"},{\"code\":\"308\",\"lead\":\"\"}],\"minnesotacodes\":[\"9-3-1\"]}",
//        "leadcount": 12,
//        "length": 0,
//        "macureValueArray": [      113,      0  ],
//        "patientAge": 20,
//        "patientGender": "M",
//        "sample": 0,
//        "scale": 0,
//        "type_result": -255,
//        "ucDay": "\u0000",
//        "ucHour": "\u0000",
//        "ucMinute": "\u0000",
//        "ucMonth": "\u0000",
//        "ucSecond": "\u0000",
//        "ucYear": "\u0000",
//        "waveForm12": [
//            [
//
//                -25
//            ]
//        ]
//    },
//    "ecgMacureResultEnum": "TYPE_LOCAL_TRADITIONAL_RESULT"
//}