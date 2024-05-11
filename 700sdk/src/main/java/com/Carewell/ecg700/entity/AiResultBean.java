package com.Carewell.ecg700.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.List;

/**
 * Created by wxd on 2019-04-12.
 */

public class AiResultBean implements Parcelable {
    //不序列化
    private short[][] waveForm12;//12 导联 特征波形（short * 1.2秒 * 采样率1000 *12）  人工解析赋值
    private AiResultDiagnosisBean aiResultDiagnosisBean = new AiResultDiagnosisBean();//新协议返回的诊断结果
    private List<AiResultMeasuredValueBean> measuredValueList;

    //都序列化
    private int aiResultType;//不用了

    private int type_result;     // 4 byte  结果标识 FFFF  FF01
    private int length;   // 4 byte  协议数据总长度
    private String MachineID;    // 16 byte 心电图机设备序列号
    private String FileName;     // 18 byte 心电图机内部病例编号
    private String DoctorName;  // 64 byte 诊断医生名字
    private char ucYear;          // 1 byte  检查时间年
    private char ucMonth;       // 1 byte  检查时间月
    private char ucDay;         // 1 byte  检查时间日
    private char ucHour;       // 1 byte  检查时间时
    private char ucMinute;     // 1 byte  检查时间分
    private char ucSecond;     // 1 byte  检查时间秒
    private short HR;        // 2 byte  心率
    private short PR;        // 2 byte  PR间期
    private short QRS;      // 2 byte  QRS间期
    private short QT;       // 2 byte  QT间期
    private short QTc;      //2 byte  QTc间期
    private short Pd;      // 2 byte  P间期
    private short RV5;     // 2 byte  RV5
    private short RV6;     // 2 byte  RV6
    private short SV1;     // 2 byte  SV1
    private short SV2;     // 2 byte  SV2
    private short PAxis;    // 2 byte   P电轴
    private short QRSAxis;  // 2 byte  QRS电轴
    private short TAxis;    // 2 byte   T电轴
    private String arrDiagnosis;   // 1200 byte 诊断结论 (0作为结束符)
    private byte[] signPicture;    // 1600 byte 签名图片
    private String hospitalName;   // 64 byte  医院名称 (0作为结束符)
    private String patientName;  //64 byte  病人姓名 (0作为结束符)
    private char patientGender='M';   //1 byte  病人性别（M（男）,F(女),O（未知））
    private short patientAge;     //2 byte     病人年龄
    private short leadcount;//12（I  II III avL aVR aVF V1 V2 V3 V4 V5 V6）  8  ( I  II V1 V2 V3 V4 V5 V6  )
    private short scale;// 每位微伏数  目前使用1
    private short sample;// 采样率 1000
    private byte[] waveForm12Array;//12 导联 特征波形（short * 1.2秒 * 采样率1000 *12）
    private byte[] macureValueArray;//测量值   12*72*2            864 byte
    private short PB;
    private short PE;
    private short QRSB;
    private short QRSE;
    private short TB;
    private short TE;
    private String minnesotacodes;

    //平均辐值
    private short PA;
    private short QA;
    private short RA;
    private short SA;
    private short TA;
    private short ST20A;
    private short ST40A;
    private short ST60A;
    private short ST80A;


    public AiResultBean(){

    }

    protected AiResultBean(Parcel in) {
        aiResultType = in.readInt();

        type_result = in.readInt();
        length = in.readInt();
        MachineID = in.readString();
        FileName = in.readString();
        DoctorName = in.readString();
        ucYear = (char) in.readInt();
        ucMonth = (char) in.readInt();
        ucDay = (char) in.readInt();
        ucHour = (char) in.readInt();
        ucMinute = (char) in.readInt();
        ucSecond = (char) in.readInt();
        HR = (short) in.readInt();
        PR = (short) in.readInt();
        QRS = (short) in.readInt();
        QT = (short) in.readInt();
        QTc = (short) in.readInt();
        Pd = (short) in.readInt();
        RV5 = (short) in.readInt();
        RV6 = (short) in.readInt();
        SV1 = (short) in.readInt();
        SV2 = (short) in.readInt();
        PAxis = (short) in.readInt();
        QRSAxis = (short) in.readInt();
        TAxis = (short) in.readInt();
        arrDiagnosis = in.readString();

        int len = in.readInt();
        if(len > 0){
            signPicture = in.createByteArray();
        }

        hospitalName = in.readString();
        patientName = in.readString();
        patientGender = (char) in.readInt();
        patientAge = (short) in.readInt();
        leadcount = (short) in.readInt();
        scale = (short) in.readInt();
        sample = (short) in.readInt();

        len = in.readInt();
        if(len > 0){
            waveForm12Array = in.createByteArray();
        }

        len = in.readInt();
        if(len > 0){
            macureValueArray = in.createByteArray();
        }

        PB = (short) in.readInt();
        PE= (short) in.readInt();
        QRSB = (short) in.readInt();
        QRSE = (short) in.readInt();
        TB = (short) in.readInt();
        TE = (short) in.readInt();

        minnesotacodes = in.readString();

        PA = (short) in.readInt();
        QA = (short) in.readInt();
        RA = (short) in.readInt();
        SA = (short) in.readInt();
        TA = (short) in.readInt();
        ST20A = (short) in.readInt();
        ST40A = (short) in.readInt();
        ST60A = (short) in.readInt();
        ST80A = (short) in.readInt();
    }

    public static final Creator<AiResultBean> CREATOR = new Creator<AiResultBean>() {
        @Override
        public AiResultBean createFromParcel(Parcel in) {
            AiResultBean aiResultBean = new AiResultBean(in);
            aiResultBean = manualSetValue(aiResultBean);
            return aiResultBean;
        }

        @Override
        public AiResultBean[] newArray(int size) {
            return new AiResultBean[size];
        }
    };

    public int getType_result() {
        return type_result;
    }

    public void setType_result(int type_result) {
        this.type_result = type_result;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getMachineID() {
        return MachineID;
    }

    public void setMachineID(String machineID) {
        MachineID = machineID;
    }

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public String getDoctorName() {
        return DoctorName;
    }

    public void setDoctorName(String doctorName) {
        DoctorName = doctorName;
    }

    public char getUcYear() {
        return ucYear;
    }

    public void setUcYear(char ucYear) {
        this.ucYear = ucYear;
    }

    public char getUcMonth() {
        return ucMonth;
    }

    public void setUcMonth(char ucMonth) {
        this.ucMonth = ucMonth;
    }

    public char getUcDay() {
        return ucDay;
    }

    public void setUcDay(char ucDay) {
        this.ucDay = ucDay;
    }

    public char getUcHour() {
        return ucHour;
    }

    public void setUcHour(char ucHour) {
        this.ucHour = ucHour;
    }

    public char getUcMinute() {
        return ucMinute;
    }

    public void setUcMinute(char ucMinute) {
        this.ucMinute = ucMinute;
    }

    public char getUcSecond() {
        return ucSecond;
    }

    public void setUcSecond(char ucSecond) {
        this.ucSecond = ucSecond;
    }

    public short getHR() {
        return HR;
    }

    public void setHR(short HR) {
        this.HR = HR;
    }

    public short getPR() {
        return PR;
    }

    public void setPR(short PR) {
        this.PR = PR;
    }

    public short getQRS() {
        return QRS;
    }

    public void setQRS(short QRS) {
        this.QRS = QRS;
    }

    public short getQT() {
        return QT;
    }

    public void setQT(short QT) {
        this.QT = QT;
    }

    public short getQTc() {
        return QTc;
    }

    public void setQTc(short QTc) {
        this.QTc = QTc;
    }

    public short getPd() {
        return Pd;
    }

    public void setPd(short pd) {
        Pd = pd;
    }

    public short getRV5() {
        return RV5;
    }

    public void setRV5(short RV5) {
        this.RV5 = RV5;
    }

    public short getRV6() {
        return RV6;
    }

    public void setRV6(short RV6) {
        this.RV6 = RV6;
    }

    public short getSV1() {
        return SV1;
    }

    public void setSV1(short SV1) {
        this.SV1 = SV1;
    }

    public short getSV2() {
        return SV2;
    }

    public void setSV2(short SV2) {
        this.SV2 = SV2;
    }

    public short getPAxis() {
        return PAxis;
    }

    public void setPAxis(short PAxis) {
        this.PAxis = PAxis;
    }

    public short getQRSAxis() {
        return QRSAxis;
    }

    public void setQRSAxis(short QRSAxis) {
        this.QRSAxis = QRSAxis;
    }

    public short getTAxis() {
        return TAxis;
    }

    public void setTAxis(short TAxis) {
        this.TAxis = TAxis;
    }

    public byte[] getSignPicture() {
        return signPicture;
    }

    public void setSignPicture(byte[] signPicture) {
        this.signPicture = signPicture;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public char getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(char patientGender) {
        this.patientGender = patientGender;
    }

    public short getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(short patientAge) {
        this.patientAge = patientAge;
    }

    public short getLeadcount() {
        return leadcount;
    }

    public void setLeadcount(short leadcount) {
        this.leadcount = leadcount;
    }

    public short getScale() {
        return scale;
    }

    public void setScale(short scale) {
        this.scale = scale;
    }

    public short getSample() {
        return sample;
    }

    public void setSample(short sample) {
        this.sample = sample;
    }

    public short[][] getWaveForm12() {
        return waveForm12;
    }

    public void setWaveForm12(short[][] waveForm12) {
        this.waveForm12 = waveForm12;
    }

    public AiResultDiagnosisBean getAiResultDiagnosisBean() {
        return aiResultDiagnosisBean;
    }

    public void setAiResultDiagnosisBean(AiResultDiagnosisBean aiResultDiagnosisBean) {
        this.aiResultDiagnosisBean = aiResultDiagnosisBean;
    }

    public byte[] getWaveForm12Array() {
        return waveForm12Array;
    }

    public void setWaveForm12Array(byte[] waveForm12Array) {
        this.waveForm12Array = waveForm12Array;
    }

    public String getArrDiagnosis() {
        return arrDiagnosis;
    }

    public void setArrDiagnosis(String arrDiagnosis) {
        this.arrDiagnosis = arrDiagnosis;
    }

    public byte[] getMacureValueArray() {
        return macureValueArray;
    }

    public void setMacureValueArray(byte[] macureValueArray) {
        this.macureValueArray = macureValueArray;
    }

    public short getPB() {
        return PB;
    }

    public void setPB(short PB) {
        this.PB = PB;
    }

    public short getPE() {
        return PE;
    }

    public void setPE(short PE) {
        this.PE = PE;
    }

    public short getQRSB() {
        return QRSB;
    }

    public void setQRSB(short QRSB) {
        this.QRSB = QRSB;
    }

    public short getQRSE() {
        return QRSE;
    }

    public void setQRSE(short QRSE) {
        this.QRSE = QRSE;
    }

    public short getTB() {
        return TB;
    }

    public void setTB(short TB) {
        this.TB = TB;
    }

    public short getTE() {
        return TE;
    }

    public void setTE(short TE) {
        this.TE = TE;
    }

    public List<AiResultMeasuredValueBean> getMeasuredValueList() {
        return measuredValueList;
    }

    public void setMeasuredValueList(List<AiResultMeasuredValueBean> measuredValueList) {
        this.measuredValueList = measuredValueList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(aiResultType);

        dest.writeInt(type_result);
        dest.writeInt(length);
        dest.writeString(MachineID);
        dest.writeString(FileName);
        dest.writeString(DoctorName);
        dest.writeInt((int) ucYear);
        dest.writeInt((int) ucMonth);
        dest.writeInt((int) ucDay);
        dest.writeInt((int) ucHour);
        dest.writeInt((int) ucMinute);
        dest.writeInt((int) ucSecond);
        dest.writeInt((int) HR);
        dest.writeInt((int) PR);
        dest.writeInt((int) QRS);
        dest.writeInt((int) QT);
        dest.writeInt((int) QTc);
        dest.writeInt((int) Pd);
        dest.writeInt((int) RV5);
        dest.writeInt((int) RV6);
        dest.writeInt((int) SV1);
        dest.writeInt((int) SV2);
        dest.writeInt((int) PAxis);
        dest.writeInt((int) QRSAxis);
        dest.writeInt((int) TAxis);
        dest.writeString(arrDiagnosis);

        if(signPicture == null){
            dest.writeInt(0);
        }else{
            dest.writeInt(signPicture.length);
            dest.writeByteArray(signPicture);
        }

        dest.writeString(hospitalName);
        dest.writeString(patientName);
        dest.writeInt((int) patientGender);
        dest.writeInt((int) patientAge);
        dest.writeInt((int) leadcount);
        dest.writeInt((int) scale);
        dest.writeInt((int) sample);

        if(waveForm12Array == null){
            dest.writeInt(0);
        }else{
            dest.writeInt(waveForm12Array.length);
            dest.writeByteArray(waveForm12Array);
        }

        if(macureValueArray == null){
            dest.writeInt(0);
        }else{
            dest.writeInt(macureValueArray.length);
            dest.writeByteArray(macureValueArray);
        }

        dest.writeInt(PB);
        dest.writeInt(PE);
        dest.writeInt(QRSB);
        dest.writeInt(QRSE);
        dest.writeInt(TB);
        dest.writeInt(TE);

        dest.writeString(minnesotacodes);

        dest.writeInt(PA);
        dest.writeInt(QA);
        dest.writeInt(RA);
        dest.writeInt(SA);
        dest.writeInt(TA);
        dest.writeInt(ST20A);
        dest.writeInt(ST40A);
        dest.writeInt(ST60A);
        dest.writeInt(ST80A);
    }

    public static AiResultBean manualSetValue(AiResultBean aiResultBean){
        int receiveLeadNum = aiResultBean.getLeadcount();
        //自己解析赋值
        String arrDiagnosis = aiResultBean.getArrDiagnosis();
        if(!TextUtils.isEmpty(arrDiagnosis)){
            if(arrDiagnosis.startsWith("JSON")){
                //新协议
                StringBuilder sb = new StringBuilder(arrDiagnosis);
                //删除JSON:  前缀
                sb.delete(0,5);
                AiResultDiagnosisBean aiResultDiagnosisBean = AiResultDiagnosisBean.parseSimple(sb.toString());
                aiResultBean.setAiResultDiagnosisBean(aiResultDiagnosisBean);
            }
//            else{
                //远程诊断，需要使用
//                LinkedList<MinnesotaCodeItemBean> resultDataList = new LinkedList<MinnesotaCodeItemBean>();
//                String[] resultArray = arrDiagnosis.split("\n");
//                if(resultArray != null){
//                    for (int i=0;i<resultArray.length;i++){
//                        MinnesotaCodeItemBean item = new MinnesotaCodeItemBean();
//                        item.setTitle(resultArray[i]);
//                        resultDataList.add(item);
//                    }
//                }
//
//                String minneCodes = aiResultBean.getMinnesotacodes();
//                String[] codeArray = null;
//                if(!TextUtils.isEmpty(minneCodes)){
//                    codeArray = minneCodes.split("\n");
//                }
//
//                AiResultDiagnosisBean aiResultDiagnosisBean = new AiResultDiagnosisBean();
//                aiResultDiagnosisBean.setDiagnosisCode(resultDataList);
//                aiResultDiagnosisBean.setMinnesotacodes(codeArray);
//                aiResultBean.setAiResultDiagnosisBean(aiResultDiagnosisBean);
//            }
        }
        return aiResultBean;
    }


    public String getMinnesotacodes() {
        return minnesotacodes;
    }

    public void setMinnesotacodes(String minnesotacodes) {
        this.minnesotacodes = minnesotacodes;
    }

    public short getPA() {
        return PA;
    }

    public void setPA(short PA) {
        this.PA = PA;
    }

    public short getQA() {
        return QA;
    }

    public void setQA(short QA) {
        this.QA = QA;
    }

    public short getRA() {
        return RA;
    }

    public void setRA(short RA) {
        this.RA = RA;
    }

    public short getSA() {
        return SA;
    }

    public void setSA(short SA) {
        this.SA = SA;
    }

    public short getTA() {
        return TA;
    }

    public void setTA(short TA) {
        this.TA = TA;
    }

    public short getST20A() {
        return ST20A;
    }

    public void setST20A(short ST20A) {
        this.ST20A = ST20A;
    }

    public short getST40A() {
        return ST40A;
    }

    public void setST40A(short ST40A) {
        this.ST40A = ST40A;
    }

    public short getST60A() {
        return ST60A;
    }

    public void setST60A(short ST60A) {
        this.ST60A = ST60A;
    }

    public short getST80A() {
        return ST80A;
    }

    public void setST80A(short ST80A) {
        this.ST80A = ST80A;
    }
}
