package com.Carewell.OmniEcg.jni;


public class JniNoiseDetect {
    private static JniNoiseDetect instance = null;

    private JniNoiseDetect() {

    }

    public static JniNoiseDetect getInstance() {
        if (instance == null) {
            instance = new JniNoiseDetect();
        }
        return instance;
    }

    static {
        System.loadLibrary("ecg_common_noise_detect");
    }

    //=====================================

    /**
     * 初始化干扰检测
     */
    public native void initNoiseDetection();

    /**
     * 噪声类型signed int
     * #define NOISE_NORMAL    0   //无噪声
     * #define NOISE_50HZ60HZ  1   //工频噪声
     * #define NOISE_EMG       2   //肌电噪声
     * #define NOISE_BASELINE  4   //基线漂移
     * 干扰检测
     * @param data
     * @param channel
     * @param reset 传0
     * @return
     */
    public native int noiseDetection(short data,short channel,char reset);

}
