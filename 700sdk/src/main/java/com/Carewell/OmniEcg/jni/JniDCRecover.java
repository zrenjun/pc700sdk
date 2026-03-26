package com.Carewell.OmniEcg.jni;

/**
 * DC基线恢复 JNI 接口
 * 独立库: ecg_dc_recover
 */
public class JniDCRecover {

    private static JniDCRecover instance = null;

    static {
        System.loadLibrary("ecg_dc_recover");
    }

    private JniDCRecover() {
    }

    public static JniDCRecover getInstance() {
        if (instance == null) {
            instance = new JniDCRecover();
        }
        return instance;
    }

    /**
     * 初始化 DCRecover
     * @param versionFlag 1 old version; 0 new version
     * @return 0 成功
     */
    public native int InitDCRecover(int versionFlag);

    /**
     * 基线恢复处理
     * @param ecgDataArray    short[filterLeadNum][dataLen] 输入ECG数据
     * @param dataLen         每个通道的数据长度
     * @param notifyFilterBean 输出结果回调对象
     * @param filterLeadNum   需要处理的导联数
     * @param leadOffArr      导联脱落标志数组
     */
    public native void DCRecover(short[][] ecgDataArray, int dataLen,
                                 NotifyFilterBean notifyFilterBean,
                                 int filterLeadNum, int[] leadOffArr);
}
