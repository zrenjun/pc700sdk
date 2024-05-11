package com.Carewell.ecg700;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;

import com.Carewell.view.ecg12.LeadType;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class XmlUtil {

    public static String TAG = "pc700";

    public static String currentTime(Date date, String formate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formate);
        return simpleDateFormat.format(date);

    }

    public static InputStream getFromAssetss(Context context, String fileName) { //android里边读取streamingAsset目录的文件
        try {
            //得到资源中的Raw数据流
            InputStream in = context.getResources().getAssets().open(fileName);
            return in;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getStringByShortArray(short[] shortArray) {
        if (shortArray == null || shortArray.length == 0)
            return "";
        int len = shortArray.length;
        StringBuffer stringBuffer = new StringBuffer();
        for (int index = 0; index < len; index++) {
            stringBuffer.append(shortArray[index]).append(" ");
        }
        return stringBuffer.toString();
    }

    /**
     * @param filePathAI   生成xml文件的路径
     * @param fileNameAI   生成xml文件名
     * @param ecgDataArray 心电数据
     * @param name         patientNumber  生成xml文件所需的测试人信息（姓名、年龄、性别、身份证号）
     */
    public static String makeHl7Xml(Context context, String filePathAI, String fileNameAI,
                                    String name, String patientNumber, short[][] ecgDataArray,
                                    LeadType leadType, String lowPassHz, String hpHz, String acHz) {
        String lead1String = "";
        String lead2String = "";
        String lead3String = "";
        String leadaVRString = "";
        String leadaVLString = "";
        String leadaVFString = "";
        String leadV1String = "";
        String leadV2String = "";
        String leadV3String = "";
        String leadV4String = "";
        String leadV5String = "";
        String leadV6String = "";
        lead1String = getStringByShortArray(ecgDataArray[0]);
        lead2String = getStringByShortArray(ecgDataArray[1]);
        lead3String = getStringByShortArray(ecgDataArray[2]);
        leadaVRString = getStringByShortArray(ecgDataArray[3]);
        leadaVLString = getStringByShortArray(ecgDataArray[4]);
        leadaVFString = getStringByShortArray(ecgDataArray[5]);
        leadV1String = getStringByShortArray(ecgDataArray[6]);
        leadV2String = getStringByShortArray(ecgDataArray[7]);
        leadV3String = getStringByShortArray(ecgDataArray[8]);
        leadV4String = getStringByShortArray(ecgDataArray[9]);
        leadV5String = getStringByShortArray(ecgDataArray[10]);
        leadV6String = getStringByShortArray(ecgDataArray[11]);
        String sample = "0.001";// 采样率
        @SuppressLint("DefaultLocale")
        String gain = String.format("%.4f", 0.9536F);  //todo
        String timeString = currentTime(new Date(), "yyyyMMddHHmmss.SSS");
        // 修改xml中的节点内容
        InputStream is = null;
        OutputStream out = null;
        XMLWriter xmlwriter = null;
        Document doc = null;
        try {
            SAXReader sax = new SAXReader();
            is = getFromAssetss(context, "ceshi.xml");
            doc = sax.read(is);
            // 根节点
            Element root = doc.getRootElement();
            // 取得某节点下名为"effectiveTime"的字节点
            Element effectiveTimeElement = root.element("effectiveTime");
            /** 修改addAttribute的value值 <low value=“修改值”> text </low> */
            effectiveTimeElement.element("low").addAttribute("value", timeString);
            effectiveTimeElement.element("high").addAttribute("value", timeString);
            /** 修改 <low value=“修改值”> text </low> 中text 的值 */
            effectiveTimeElement.element("low").setText(timeString);
            effectiveTimeElement.element("high").setText(timeString);
            // =====================
            // 取得某节点下名为"componentOf/timepointEvent/effectiveTime"的所有字节点
            Element componentOfElement = root.element("componentOf");
            Element timepointEventElement = componentOfElement
                    .element("timepointEvent");
            effectiveTimeElement = timepointEventElement
                    .element("effectiveTime");
            //设置attribute值,分析中心要求此字段必填
            effectiveTimeElement.element("low").addAttribute("value", timeString);
            effectiveTimeElement.element("high").addAttribute("value", timeString);
            effectiveTimeElement.element("low")
                    .setText(timeString);
            effectiveTimeElement.element("high")
                    .setText(timeString);
            // =====================
            // 取得某节点下名为"componentOf/timepointEvent/componentOf/subjectAssignment/subject/trialSubject/id"的所有字节点
            componentOfElement = timepointEventElement.element("componentOf");
            Element subjectAssignmentElement = componentOfElement
                    .element("subjectAssignment");
            Element subjectElement = subjectAssignmentElement
                    .element("subject");
            Element trialSubjectElement = subjectElement
                    .element("trialSubject");
            Element idElement = trialSubjectElement.element("id");
            idElement.attribute("extension").setText(patientNumber);
            // =====================
            // 取得某节点下名为"component/series/effectiveTime"的所有字节点
            Element componentElement = root.element("component");
            Element seriesElement = componentElement.element("series");
            effectiveTimeElement = seriesElement.element("effectiveTime");
            effectiveTimeElement.element("low").addAttribute("value", timeString);
            effectiveTimeElement.element("high").addAttribute("value", timeString);
            effectiveTimeElement.element("low")
                    .setText(timeString);
            effectiveTimeElement.element("high")
                    .setText(timeString);
            // =====================
            // 取得某节点下名为"component/series/author/manufacturedProduct/manufacturerOrganization/name"的所有字节点
            Element authorElement = seriesElement.element("author");
            Element manufacturedProductElement = authorElement
                    .element("manufacturedProduct");
            Element manufacturerOrganizationElement = manufacturedProductElement
                    .element("manufacturerOrganization");
            Element nameElement = manufacturerOrganizationElement
                    .element("name");
            nameElement.setText(name);
            // =====================
            // 取得某节点下名为"component/series/component/sequenceSet"的所有字节点
            componentElement = seriesElement.element("component");
            Element sequenceSetElement = componentElement
                    .element("sequenceSet");
            List nodesComponent = sequenceSetElement.elements("component");
            Element element = null;
            for (Object obj : nodesComponent) {
                element = (Element) obj;
                Element sequenceElement = element.element("sequence");
                Element codeElement = sequenceElement.element("code");
                String codeValue = codeElement.attribute("code").getValue();
                if ("TIME_ABSOLUTE".equals(codeValue)) {
                    // sample
                    Element valueElement = sequenceElement.element("value");
                    Element incrementElement = valueElement
                            .element("increment");
                    incrementElement.attribute("value").setText(sample);
                } else if ("MDC_ECG_LEAD_I".equals(codeValue)
                        || "MDC_ECG_LEAD_II".equals(codeValue)
                        || "MDC_ECG_LEAD_III".equals(codeValue)
                        || "MDC_ECG_LEAD_AVR".equals(codeValue)
                        || "MDC_ECG_LEAD_AVL".equals(codeValue)
                        || "MDC_ECG_LEAD_AVF".equals(codeValue)
                        || "MDC_ECG_LEAD_V1".equals(codeValue)
                        || "MDC_ECG_LEAD_V2".equals(codeValue)
                        || "MDC_ECG_LEAD_V3".equals(codeValue)
                        || "MDC_ECG_LEAD_V4".equals(codeValue)
                        || "MDC_ECG_LEAD_V5".equals(codeValue)
                        || "MDC_ECG_LEAD_V6".equals(codeValue)) {
                    Element valueElement = sequenceElement.element("value");
                    // gain
                    Element scaleElement = valueElement.element("scale");
                    scaleElement.attribute("value").setText(gain);
                    // data
                    Element digitsElement = valueElement.element("digits");
                    if ("MDC_ECG_LEAD_I".equals(codeValue)) {
                        digitsElement.setText(lead1String);
                    } else if ("MDC_ECG_LEAD_II".equals(codeValue)) {
                        digitsElement.setText(lead2String);
                    } else if ("MDC_ECG_LEAD_III".equals(codeValue)) {
                        digitsElement.setText(lead3String);
                    } else if ("MDC_ECG_LEAD_AVR".equals(codeValue)) {
                        digitsElement.setText(leadaVRString);
                    } else if ("MDC_ECG_LEAD_AVL".equals(codeValue)) {
                        digitsElement.setText(leadaVLString);
                    } else if ("MDC_ECG_LEAD_AVF".equals(codeValue)) {
                        digitsElement.setText(leadaVFString);
                    } else if ("MDC_ECG_LEAD_V1".equals(codeValue)) {
                        digitsElement.setText(leadV1String);
                    } else if ("MDC_ECG_LEAD_V2".equals(codeValue)) {
                        digitsElement.setText(leadV2String);
                    } else if ("MDC_ECG_LEAD_V3".equals(codeValue)) {
                        digitsElement.setText(leadV3String);
                    } else if ("MDC_ECG_LEAD_V4".equals(codeValue)) {
                        digitsElement.setText(leadV4String);
                    } else if ("MDC_ECG_LEAD_V5".equals(codeValue)) {
                        digitsElement.setText(leadV5String);
                    } else if ("MDC_ECG_LEAD_V6".equals(codeValue)) {
                        digitsElement.setText(leadV6String);
                    }
                }
            }
            //删除没用的节点
            for (Object obj : nodesComponent) {
                element = (Element) obj;
                Element sequenceElement = element.element("sequence");
                Element codeElement = sequenceElement.element("code");
                String codeValue = codeElement.attribute("code").getValue();
                if ("MDC_ECG_LEAD_I".equals(codeValue)
                        || "MDC_ECG_LEAD_II".equals(codeValue)
                        || "MDC_ECG_LEAD_III".equals(codeValue)
                        || "MDC_ECG_LEAD_AVR".equals(codeValue)
                        || "MDC_ECG_LEAD_AVL".equals(codeValue)
                        || "MDC_ECG_LEAD_AVF".equals(codeValue)
                        || "MDC_ECG_LEAD_V1".equals(codeValue)
                        || "MDC_ECG_LEAD_V2".equals(codeValue)
                        || "MDC_ECG_LEAD_V3".equals(codeValue)
                        || "MDC_ECG_LEAD_V4".equals(codeValue)
                        || "MDC_ECG_LEAD_V5".equals(codeValue)
                        || "MDC_ECG_LEAD_V6".equals(codeValue)) {
                    switch (leadType) {
                        case LEAD_6:
                            if ("MDC_ECG_LEAD_V1".equals(codeValue)
                                    || "MDC_ECG_LEAD_V2".equals(codeValue)
                                    || "MDC_ECG_LEAD_V3".equals(codeValue)
                                    || "MDC_ECG_LEAD_V4".equals(codeValue)
                                    || "MDC_ECG_LEAD_V5".equals(codeValue)
                                    || "MDC_ECG_LEAD_V6".equals(codeValue)) {
                                element.detach();
                            }
                            break;
                        case LEAD_I:
                            if ("MDC_ECG_LEAD_II".equals(codeValue)
                                    || "MDC_ECG_LEAD_III".equals(codeValue)
                                    || "MDC_ECG_LEAD_AVR".equals(codeValue)
                                    || "MDC_ECG_LEAD_AVL".equals(codeValue)
                                    || "MDC_ECG_LEAD_AVF".equals(codeValue)
                                    || "MDC_ECG_LEAD_V1".equals(codeValue)
                                    || "MDC_ECG_LEAD_V2".equals(codeValue)
                                    || "MDC_ECG_LEAD_V3".equals(codeValue)
                                    || "MDC_ECG_LEAD_V4".equals(codeValue)
                                    || "MDC_ECG_LEAD_V5".equals(codeValue)
                                    || "MDC_ECG_LEAD_V6".equals(codeValue)) {
                                element.detach();
                            }
                            break;
                        case LEAD_II:
                            if ("MDC_ECG_LEAD_I".equals(codeValue)
                                    || "MDC_ECG_LEAD_III".equals(codeValue)
                                    || "MDC_ECG_LEAD_AVR".equals(codeValue)
                                    || "MDC_ECG_LEAD_AVL".equals(codeValue)
                                    || "MDC_ECG_LEAD_AVF".equals(codeValue)
                                    || "MDC_ECG_LEAD_V1".equals(codeValue)
                                    || "MDC_ECG_LEAD_V2".equals(codeValue)
                                    || "MDC_ECG_LEAD_V3".equals(codeValue)
                                    || "MDC_ECG_LEAD_V4".equals(codeValue)
                                    || "MDC_ECG_LEAD_V5".equals(codeValue)
                                    || "MDC_ECG_LEAD_V6".equals(codeValue)) {
                                element.detach();
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            //保存滤波信息
            nodesComponent = sequenceSetElement.elements("controlVariable");
            for (Object obj : nodesComponent) {
                element = (Element) obj;
                Element sequenceElement = element.element("controlVariable");
                Element codeElement = sequenceElement.element("code");
                String codeValue = codeElement.attribute("code").getValue();

                Attribute attribute = sequenceElement.element("value").attribute("value");
                if ("LOWPASS_FILTER".equals(codeValue)) {
                    attribute.setText(lowPassHz);
                } else if ("HIGHPASS_FILTER".equals(codeValue)) {
                    attribute.setText(hpHz);
                } else if ("AC_FILTER".equals(codeValue)) {
                    attribute.setText(acHz);
                }
            }
            // 输出格式
            OutputFormat outformat = new OutputFormat();
            // 指定XML编码
            outformat.setEncoding("UTF-8");
            outformat.setNewlines(true);
            outformat.setIndent(true);
            outformat.setTrimText(true);
            //保存到本地
            File file = createDir(filePathAI);
            Log.d(TAG, "file" + file.getPath() + "/" + fileNameAI + ".xml");
            file = new File(file.getPath() + "/" + fileNameAI + ".xml");
            out = new FileOutputStream(file);
            xmlwriter = new XMLWriter(out, outformat);
            xmlwriter.write(doc);
            scamFile(context, file);
        } catch (Exception e) {
            Log.d(TAG, "Exception" + e.toString());
        } finally {
            close(xmlwriter, out, is);
        }

        return doc.asXML();
    }

    /**
     * @param xmlwriter
     * @param out
     * @param is
     */
    private static void close(XMLWriter xmlwriter, OutputStream out, InputStream is) {
        if (null != xmlwriter) {
            try {
                xmlwriter.close();
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            xmlwriter = null;
        }
        if (null != out) {
            try {
                out.close();
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            out = null;
        }
        if (null != is) {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            is = null;
        }
    }

    /**
     * 递归创建文件夹
     *
     * @param path 文件夹路径
     */
    public static File createDir(String path) throws Exception {
        if (path == null || path.equals("")) {
            new NullPointerException();
        }
        File file = new File(path);
        if (file.exists())
            return file;
        File p = file.getParentFile();
        if (!p.exists()) {
            createDir(p.getPath());
        }
        if (file.mkdir())
            return file;
        return null;
    }

    /*
     * 扫描文件
     * 使文件能被发现
     * */
    public static void scamFile(Context context, File file) {
        MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()},
                null, (path, uri) -> {

                });
    }
}
