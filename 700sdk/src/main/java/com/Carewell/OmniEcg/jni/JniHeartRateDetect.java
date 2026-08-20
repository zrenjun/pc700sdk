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
     * @param thresh     基线位置，c120 传200.其它项目先传200，看看准确度怎么样
     */
    public native void initHeartRateDetect(int sampleRate, int thresh);


    /**
     * 获取心率
     * @param ecgDataArray
     * @return 数组：0 心率值 ；1 qrsposlen
     */
    public native int[] getDataHeartRate(int[] ecgDataArray);
}
