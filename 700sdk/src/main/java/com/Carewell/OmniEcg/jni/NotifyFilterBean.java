package com.Carewell.OmniEcg.jni;

/**
 * Created by wxd on 2019-05-24.
 */

public class NotifyFilterBean {

    private int outDataLen;//输出数据长度
    private short[][] shortDataArray;

    private int [][] intDataArray;

    public int getOutDataLen() {
        return outDataLen;
    }

    public void setOutDataLen(int outDataLen) {
        this.outDataLen = outDataLen;
    }

    public short[][] getShortDataArray() {
        return shortDataArray;
    }

    public void setShortDataArray(short[][] shortDataArray) {
        this.shortDataArray = shortDataArray;
    }

    public int[][] getIntDataArray() {
        return intDataArray;
    }


    public void setIntDataArray(int[][] intDataArray) {
        this.intDataArray = intDataArray;
    }
}
