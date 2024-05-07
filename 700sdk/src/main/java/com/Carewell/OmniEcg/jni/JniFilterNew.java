package com.Carewell.OmniEcg.jni;

/**
 * Created by wxd
 * @author wxd
 */

public class JniFilterNew {

    private static JniFilterNew instance = null;

    static {
        System.loadLibrary("ecg_filter_new");
    }

    private JniFilterNew() {

    }

    public static JniFilterNew getInstance() {
        if (instance == null) {
            instance = new JniFilterNew();
        }
        return instance;
    }
    //=============================DC 基线跳变问题，添加
    public native void DCRecover(short[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum,int[] leadOffArr);

    
    /**
     * 重置滤波器
     */
    public native void resetFilter();

    /**
     * 获取算法版本
     * @return
     */
    public native int[] getAlgorithmVersion();

    /**
     * 初始化滤波器
     * @param versionFlag  1 old version;0 new version
     * @return
     */
    public native int InitDCRecover(int versionFlag);


    //==================================================交流 工频

    /**
     *
     * @param ecgDataArray
     * @param dataLen
     * @param notifyFilterBean
     * @param filterLeadNum 需要滤波的导联长度
     */
    public native void powerFrequency50(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    public native void powerFrequency60(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    //===========================肌电  这个滤波开了，就不调用低通了。肌电和低通是一种滤波
    public native void electromyography25(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    public native void electromyography35(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    public native void electromyography45(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    //===========================低通
    public native void lowPass75(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    /**
     *
     * @param ecgDataArray
     * @param dataLen 每个通道的数据长度
     * @param notifyFilterBean
     */
    public native void lowPass90(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    public native void lowPass100(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    public native void lowPass150(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    public native void lowPass300(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    //===========================高通
    public native void HP0p01(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    public native void HP0p05(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    public native void HP0p32(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);

    public native void HP0p67(int[][] ecgDataArray, int dataLen,NotifyFilterBean notifyFilterBean,int filterLeadNum);
}
