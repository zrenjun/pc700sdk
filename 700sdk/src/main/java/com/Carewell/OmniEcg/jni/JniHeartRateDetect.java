package com.Carewell.OmniEcg.jni;


public class JniHeartRateDetect {
    private static JniHeartRateDetect instance = null;

    private JniHeartRateDetect() {

    }

    public static JniHeartRateDetect getInstance() {
        if (instance == null) {
            instance = new JniHeartRateDetect();
        }
        return instance;
    }

    static {
        System.loadLibrary("ecg_common_heartrate_detect");
    }

    //=====================================

    /**
     * 初始化心率检测
     * @param sampleRate 采样率
     */
    public native void initHeartRateDetect(int sampleRate);

    /**
     * 释放心率检测
     */
    public native void closeHeartRateDetect();

    /**
     * 获取心率
     * @param ecgDataArray
     * @return
     */
    public native int getDataHeartRate(short[] ecgDataArray);


}
