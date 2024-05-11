package com.Carewell.ecg700.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class MacureResultBean implements Parcelable{

    //算法结果类型
    private EcgMacureResultEnum ecgMacureResultEnum;
    //RR分析，HRV分析结果
    private RRAnalysisResultBean rrAnalysisResultBean;
    //本地传统算法分析结果 / AI 本地算法结果
    private AiResultBean aiResultBean;

    public MacureResultBean(){

    }

    protected MacureResultBean(Parcel in) {
        ecgMacureResultEnum = EcgMacureResultEnum.values()[in.readInt()];
        rrAnalysisResultBean = in.readParcelable(RRAnalysisResultBean.class.getClassLoader());
        aiResultBean = in.readParcelable(AiResultBean.class.getClassLoader());
    }

    public static final Creator<MacureResultBean> CREATOR = new Creator<MacureResultBean>() {
        @Override
        public MacureResultBean createFromParcel(Parcel in) {
            return new MacureResultBean(in);
        }

        @Override
        public MacureResultBean[] newArray(int size) {
            return new MacureResultBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ecgMacureResultEnum.ordinal());
        dest.writeParcelable(rrAnalysisResultBean,flags);
        dest.writeParcelable(aiResultBean, flags);
    }

    public EcgMacureResultEnum getEcgMacureResultEnum() {
        return ecgMacureResultEnum;
    }

    public void setEcgMacureResultEnum(EcgMacureResultEnum ecgMacureResultEnum) {
        this.ecgMacureResultEnum = ecgMacureResultEnum;
    }

    public RRAnalysisResultBean getRrAnalysisResultBean() {
        return rrAnalysisResultBean;
    }

    public void setRrAnalysisResultBean(RRAnalysisResultBean rrAnalysisResultBean) {
        this.rrAnalysisResultBean = rrAnalysisResultBean;
    }

    public AiResultBean getAiResultBean() {
        return aiResultBean;
    }

    public void setAiResultBean(AiResultBean aiResultBean) {
        this.aiResultBean = aiResultBean;
    }
}
