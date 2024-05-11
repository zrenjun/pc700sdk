package com.Carewell.ecg700.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by wxd on 2019-04-19.
 */

public class RRAnalysisResultBean implements Parcelable, Serializable {
    // 功率谱密度PSD 以及其各项分析参数
    private float[] PSD_specXY;//功率谱密度（x0,y0,x1,y1,....,xn,yn)
    private int PSD_spec_len;
    private double  LF;//低频
    private double HF;//高频
    private double  ULF;//超低频
    private double VLF;//极低频功率（0.00333 Hz ～ 0.040 Hz）（单位：ms2）；
    private double LF_norm;//标准化的LF功率
    private double HF_norm;//标准化的HF功率
    private double Ratio_LF_HF;//低频与高频的比值
    private double[]  RR_interval;//RR间期
    private int  RR_interval_len;//RR间期长度
    private double[] RR_interval_Diff;//RR间期差
    private int RR_interval_Diff_len;//RR间期差长度
    private double[] RR_PoincarePlot_XY;//RR间期混沌分析XY的坐标点（x0,y0,x1,y1,....,xn,yn)
    private int RR_PoincarePlot_XY_len;//RR间期混沌分析X轴和Y轴的数据长度
    private double[] RR_Diff_PoincarePlot_XY;//RR间期差混沌分析XY的坐标点（x0,y0,x1,y1,....,xn,yn)
    private int RR_Diff_PoincarePlot_XY_len;//RR间期差混沌分析X轴和Y轴的数据长度
    private double  MEAN_RR_interval;//RR间期的平均值  60*samplerate/value=rate
    private double  MAX_RR_interval;// RR间期的最大值
    private double  MIN_RR_interval;//RR间期的最小值
    private double  Ratio_MAX_MIN_Rr_interval;//RR间期的最大值与最小值的比值
    private double  SDNN;//正常窦性心搏RR间期的标准差
    private double  RMSSD;//相邻RR间期差值的均方根
    private double SDANN;//standard deviation of the 5-min means in ms
    private double ASDNN;//mean of the 5-min means in ms  平均值
    private int NN50;//在整个心电图记录中RR间期差值超过50ms的次数
    private double PNN50;// NN50 所占心动周期次数的百分比**/
    private double TINN;// triangular index interval 当使用最小方差的方法，以三角形来近似地描述NN间期的直方图时，所得到的近似三角形的底宽，单位为ms
    private int[] RR_hist;  //RR 间期直方图  对应的x轴为[0：20：2000]ms   画RR直方图
    private int macureTime;//测量时间

    public RRAnalysisResultBean(){

    }

    protected RRAnalysisResultBean(Parcel in) {
        PSD_specXY = in.createFloatArray();
        PSD_spec_len = in.readInt();
        LF = in.readDouble();
        HF = in.readDouble();
        ULF = in.readDouble();
        VLF = in.readDouble();
        LF_norm = in.readDouble();
        HF_norm = in.readDouble();
        Ratio_LF_HF = in.readDouble();
        RR_interval = in.createDoubleArray();
        RR_interval_len = in.readInt();
        RR_interval_Diff = in.createDoubleArray();
        RR_interval_Diff_len = in.readInt();
        RR_PoincarePlot_XY = in.createDoubleArray();
        RR_PoincarePlot_XY_len = in.readInt();
        RR_Diff_PoincarePlot_XY = in.createDoubleArray();
        RR_Diff_PoincarePlot_XY_len = in.readInt();
        MEAN_RR_interval = in.readDouble();
        MAX_RR_interval = in.readDouble();
        MIN_RR_interval = in.readDouble();
        Ratio_MAX_MIN_Rr_interval = in.readDouble();
        SDNN = in.readDouble();
        RMSSD = in.readDouble();
        SDANN = in.readDouble();
        ASDNN = in.readDouble();
        NN50 = in.readInt();
        PNN50 = in.readDouble();
        TINN = in.readDouble();
        RR_hist = in.createIntArray();
        macureTime = in.readInt();
    }

    public static final Creator<RRAnalysisResultBean> CREATOR = new Creator<RRAnalysisResultBean>() {
        @Override
        public RRAnalysisResultBean createFromParcel(Parcel in) {
            return new RRAnalysisResultBean(in);
        }

        @Override
        public RRAnalysisResultBean[] newArray(int size) {
            return new RRAnalysisResultBean[size];
        }
    };

    public float[] getPSD_specXY() {
        return PSD_specXY;
    }

    public void setPSD_specXY(float[] PSD_specXY) {
        this.PSD_specXY = PSD_specXY;
    }

    public int getPSD_spec_len() {
        return PSD_spec_len;
    }

    public void setPSD_spec_len(int PSD_spec_len) {
        this.PSD_spec_len = PSD_spec_len;
    }

    public double getLF() {
        return LF;
    }

    public void setLF(double LF) {
        this.LF = LF;
    }

    public double getHF() {
        return HF;
    }

    public void setHF(double HF) {
        this.HF = HF;
    }
    public double getULF() {
        return ULF;
    }

    public void setULF(double ULF) {
        this.ULF = ULF;
    }
    public double getVLF() {
        return VLF;
    }

    public void setVLF(double VLF) {
        this.VLF = VLF;
    }

    public double getLF_norm() {
        return LF_norm;
    }

    public void setLF_norm(double LF_norm) {
        this.LF_norm = LF_norm;
    }

    public double getHF_norm() {
        return HF_norm;
    }

    public void setHF_norm(double HF_norm) {
        this.HF_norm = HF_norm;
    }

    public double getRatio_LF_HF() {
        return Ratio_LF_HF;
    }

    public void setRatio_LF_HF(double ratio_LF_HF) {
        Ratio_LF_HF = ratio_LF_HF;
    }

    public double[] getRR_interval() {
        return RR_interval;
    }

    public void setRR_interval(double[] RR_interval) {
        this.RR_interval = RR_interval;
    }
    public int getRR_interval_len() {
        return RR_interval_len;
    }

    public void setRR_interval_len(int RR_interval_len) {
        this.RR_interval_len = RR_interval_len;
    }

    public double[] getRR_interval_Diff() {
        return RR_interval_Diff;
    }

    public void setRR_interval_Diff(double[] RR_interval_Diff) {
        this.RR_interval_Diff = RR_interval_Diff;
    }

    public int getRR_interval_Diff_len() {
        return RR_interval_Diff_len;
    }

    public void setRR_interval_Diff_len(int RR_interval_Diff_len) {
        this.RR_interval_Diff_len = RR_interval_Diff_len;
    }

    public double[] getRR_PoincarePlot_XY() {
        return RR_PoincarePlot_XY;
    }

    public void setRR_PoincarePlot_XY(double[] RR_PoincarePlot_XY) {
        this.RR_PoincarePlot_XY = RR_PoincarePlot_XY;
    }

    public int getRR_PoincarePlot_XY_len() {
        return RR_PoincarePlot_XY_len;
    }

    public void setRR_PoincarePlot_XY_len(int RR_PoincarePlot_XY_len) {
        this.RR_PoincarePlot_XY_len = RR_PoincarePlot_XY_len;
    }

    public double[] getRR_Diff_PoincarePlot_XY() {
        return RR_Diff_PoincarePlot_XY;
    }

    public void setRR_Diff_PoincarePlot_XY(double[] RR_Diff_PoincarePlot_XY) {
        this.RR_Diff_PoincarePlot_XY = RR_Diff_PoincarePlot_XY;
    }

    public int getRR_Diff_PoincarePlot_XY_len() {
        return RR_Diff_PoincarePlot_XY_len;
    }

    public void setRR_Diff_PoincarePlot_XY_len(int RR_Diff_PoincarePlot_XY_len) {
        this.RR_Diff_PoincarePlot_XY_len = RR_Diff_PoincarePlot_XY_len;
    }

    public double getMEAN_RR_interval() {
        return MEAN_RR_interval;
    }

    public void setMEAN_RR_interval(double MEAN_RR_interval) {
        this.MEAN_RR_interval = MEAN_RR_interval;
    }

    public double getMAX_RR_interval() {
        return MAX_RR_interval;
    }

    public void setMAX_RR_interval(double MAX_RR_interval) {
        this.MAX_RR_interval = MAX_RR_interval;
    }

    public double getMIN_RR_interval() {
        return MIN_RR_interval;
    }

    public void setMIN_RR_interval(double MIN_RR_interval) {
        this.MIN_RR_interval = MIN_RR_interval;
    }

    public double getRatio_MAX_MIN_Rr_interval() {
        return Ratio_MAX_MIN_Rr_interval;
    }

    public void setRatio_MAX_MIN_Rr_interval(double ratio_MAX_MIN_Rr_interval) {
        Ratio_MAX_MIN_Rr_interval = ratio_MAX_MIN_Rr_interval;
    }

    public double getSDNN() {
        return SDNN;
    }

    public void setSDNN(double SDNN) {
        this.SDNN = SDNN;
    }

    public double getRMSSD() {
        return RMSSD;
    }

    public void setRMSSD(double RMSSD) {
        this.RMSSD = RMSSD;
    }

    public double getSDANN() {
        return SDANN;
    }

    public void setSDANN(double SDANN) {
        this.SDANN = SDANN;
    }

    public double getASDNN() {
        return ASDNN;
    }

    public void setASDNN(double ASDNN) {
        this.ASDNN = ASDNN;
    }

    public int getNN50() {
        return NN50;
    }

    public void setNN50(int NN50) {
        this.NN50 = NN50;
    }

    public double getPNN50() {
        return PNN50;
    }

    public void setPNN50(double PNN50) {
        this.PNN50 = PNN50;
    }

    public double getTINN() {
        return TINN;
    }

    public void setTINN(double TINN) {
        this.TINN = TINN;
    }

    public int[] getRR_hist() {
        return RR_hist;
    }

    public void setRR_hist(int[] RR_hist) {
        this.RR_hist = RR_hist;
    }

    public int getMacureTime() {
        return macureTime;
    }

    public void setMacureTime(int macureTime) {
        this.macureTime = macureTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloatArray(PSD_specXY);
        dest.writeInt(PSD_spec_len);
        dest.writeDouble(LF);
        dest.writeDouble(HF);
        dest.writeDouble(ULF);
        dest.writeDouble(VLF);
        dest.writeDouble(LF_norm);
        dest.writeDouble(HF_norm);
        dest.writeDouble(Ratio_LF_HF);
        dest.writeDoubleArray(RR_interval);
        dest.writeInt(RR_interval_len);
        dest.writeDoubleArray(RR_interval_Diff);
        dest.writeInt(RR_interval_Diff_len);
        dest.writeDoubleArray(RR_PoincarePlot_XY);
        dest.writeInt(RR_PoincarePlot_XY_len);
        dest.writeDoubleArray(RR_Diff_PoincarePlot_XY);
        dest.writeInt(RR_Diff_PoincarePlot_XY_len);
        dest.writeDouble(MEAN_RR_interval);
        dest.writeDouble(MAX_RR_interval);
        dest.writeDouble(MIN_RR_interval);
        dest.writeDouble(Ratio_MAX_MIN_Rr_interval);
        dest.writeDouble(SDNN);
        dest.writeDouble(RMSSD);
        dest.writeDouble(SDANN);
        dest.writeDouble(ASDNN);
        dest.writeInt(NN50);
        dest.writeDouble(PNN50);
        dest.writeDouble(TINN);
        dest.writeIntArray(RR_hist);
        dest.writeInt(macureTime);
    }
}
