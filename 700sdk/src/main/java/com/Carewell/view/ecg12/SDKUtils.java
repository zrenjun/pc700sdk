package com.Carewell.view.ecg12;


import java.io.FileOutputStream;
import java.io.IOException;

public class SDKUtils {


    public static void writeEcg12File(short[][] ecgDataArray, String url) {
        mIndex = 0;
//        ecgData0.length=180095      12 表示有12组数据 后面乘以2 ，是每个数字 占两个（resetEcfdase方法里面）

        int arraySize = ecgDataArray.length;

        if (arraySize > 0) {
//            ecgData0.length=180095
            short[] ecgData0 = ecgDataArray[0];
            mData = new byte[ecgData0.length * 12 * 2 + 10240 * 20];
            short[] ecgData1 = ecgDataArray[1];
            short[] ecgData2 = ecgDataArray[2];
            short[] ecgData3 = ecgDataArray[3];
            short[] ecgData4 = ecgDataArray[4];
            short[] ecgData5 = ecgDataArray[5];
            short[] ecgData6 = ecgDataArray[6];
            short[] ecgData7 = ecgDataArray[7];
            short[] ecgData8 = ecgDataArray[8];
            short[] ecgData9 = ecgDataArray[9];
            short[] ecgData10 = ecgDataArray[10];
            short[] ecgData11 = ecgDataArray[11];
            for (int ecgIndex = 0; ecgIndex < ecgData0.length; ++ecgIndex) {

                resetEcfdase(ecgData0[ecgIndex]);
                if (ecgData1.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData1[ecgIndex]);
                if (ecgData2.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData2[ecgIndex]);

                if (ecgData3.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData3[ecgIndex]);
                if (ecgData4.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData4[ecgIndex]);
                if (ecgData5.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData5[ecgIndex]);

                if (ecgData6.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData6[ecgIndex]);
                if (ecgData7.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData7[ecgIndex]);
                if (ecgData8.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData8[ecgIndex]);

                if (ecgData9.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData9[ecgIndex]);
                if (ecgData10.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData10[ecgIndex]);
                if (ecgData11.length - 1 >= ecgIndex)
                    resetEcfdase(ecgData11[ecgIndex]);
            }

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(url, true);
                fos.write(mData, 0, mIndex);
                fos.flush();
            } catch (IOException var12) {
                var12.printStackTrace();
            } finally {
                try {
                    if (fos != null) {
                        fos.flush();
                        fos.close();
                    }
                    fos = null;
                } catch (IOException var11) {

                    var11.printStackTrace();

                }

            }

        }
    }

    private static byte[] mData = new byte[10240 * 20 * 24];
    static int mIndex = 0;

    /**
     * 重置心电图
     */
    private static void resetEcfdase(short fda) {

        mData[mIndex++] = (byte) (fda & 255);
        mData[mIndex++] = (byte) (fda >> 8 & 255);
    }


}