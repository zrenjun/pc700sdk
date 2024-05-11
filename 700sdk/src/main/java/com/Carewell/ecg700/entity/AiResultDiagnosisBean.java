package com.Carewell.ecg700.entity;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by wxd on 2019-07-08.
 */

public class AiResultDiagnosisBean implements Parcelable {
    //是否是新协议。旧协议，结果在一个字段里。新协议在json里，需要自己根据code，拿结果
    private List<MinnesotaCodeItemBean> diagnosis = new LinkedList<>();
    private String[] minnesotacodes;

    //旧协议返回的诊断结果
    //private List<String> listDiagnosis = new ArrayList<>();

    public AiResultDiagnosisBean() {
    }

    public List<MinnesotaCodeItemBean> getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosisCode(List<MinnesotaCodeItemBean> diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String[] getMinnesotacodes() {
        return minnesotacodes;
    }

    public void setMinnesotacodes(String[] minnesotacodes) {
        this.minnesotacodes = minnesotacodes;
    }

    public static AiResultDiagnosisBean parseSimple(String dataString)
    {
        Gson gson = new Gson();
        AiResultDiagnosisBean item = gson.fromJson(dataString,
                new TypeToken<AiResultDiagnosisBean>() {
                }.getType());
        return item;
    }

    public static String makeJson(AiResultDiagnosisBean aiResultDiagnosisBean){
        Gson gson = new Gson();
        return gson.toJson(aiResultDiagnosisBean);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(this.diagnosis);
        dest.writeStringArray(this.minnesotacodes);
    }

    protected AiResultDiagnosisBean(Parcel in) {
        this.diagnosis = new ArrayList<MinnesotaCodeItemBean>();
        in.readList(this.diagnosis, MinnesotaCodeItemBean.class.getClassLoader());
        this.minnesotacodes = in.createStringArray();
    }

    public static final Creator<AiResultDiagnosisBean> CREATOR = new Creator<AiResultDiagnosisBean>() {
        @Override
        public AiResultDiagnosisBean createFromParcel(Parcel source) {
            return new AiResultDiagnosisBean(source);
        }

        @Override
        public AiResultDiagnosisBean[] newArray(int size) {
            return new AiResultDiagnosisBean[size];
        }
    };
}
