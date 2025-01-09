package com.Carewell.view.ecg12;

import android.content.Context;


public class PreviewManager {
    public static final int SAMPLE_RATE = 1000;

    private static PreviewManager instance = null;

    private BaseEcgPreviewTemplate baseEcgPreviewTemplate;
    //增益
    private float[] gainArray = new float[]{1.0F, 1.0F};

    private PreviewManager() {

    }

    public static PreviewManager getInstance() {
        if (instance == null) {
            instance = new PreviewManager();
        }
        return instance;
    }

    public void init() {

    }



    /**
     * 重新绘图
     */
    public void resetDrawEcg( int imageWidth, int imageHeight) {
        float smallGridSpace = EcgConfig.SMALL_GRID_SPACE_FLOAT;
        baseEcgPreviewTemplate = MainEcgManager.getBaseEcgPreviewTemplate(PreviewPageEnum.PAGE_PREVIEW, smallGridSpace,
                imageWidth, imageHeight, MainEcgManager.getInstance().getLeadSpeedType(), MainEcgManager.getInstance().getGainArray(), true, MainEcgManager.getInstance().getRecordOrderType());
        baseEcgPreviewTemplate.initParams();
        baseEcgPreviewTemplate.setEcgMode(EcgShowModeEnum.MODE_SCROLL);
    }


    public float[] getGainArray() {
        return gainArray;
    }

    public void setGainArray(float[] gainArray) {
        this.gainArray = gainArray;
    }

    public BaseEcgPreviewTemplate getBaseEcgPreviewTemplate() {
        return baseEcgPreviewTemplate;
    }


    /**
     * 获取当前屏幕可画的数据
     *
     * @return
     */
    public synchronized short[][] getCurrentScrrenDrawData(short[][] ecgDataArrayAll, int ecgImageWidth, float length, LeadSpeedType leadSpeedType) {
        mAllDrawEcgDataLen=ecgDataArrayAll[0].length;

        float speed=(float)leadSpeedType.getValue();
        float gridSpace = EcgConfig.SMALL_GRID_SPACE_FLOAT;
        float screenCanDrawSecond = (ecgImageWidth - gridSpace * 5) / baseEcgPreviewTemplate.getLeadColumes() / gridSpace / speed;
        int needData = (int)(SAMPLE_RATE * screenCanDrawSecond);
        short[][] dataArray;
        //内存中的数据不够，屏幕画的数据
        if (ecgDataArrayAll[0].length <= needData) {
            needData = ecgDataArrayAll[0].length;
            dataArray = new short[ecgDataArrayAll.length][needData];
            refreshData(needData / (float) SAMPLE_RATE * speed, true, ecgDataArrayAll,speed);
            isFirst=false;
            for (int i = 0; i < dataArray.length; i++) {
                System.arraycopy(ecgDataArrayAll[i], ecgDataArrayAll[0].length - dataArray[0].length, dataArray[i], 0, dataArray[0].length);
            }
        } else {
            if (isFirst){
                length = (ecgImageWidth - gridSpace * 5) / baseEcgPreviewTemplate.getLeadColumes() /  gridSpace;
                mScreenDrawEcgDataLen= (int) (ecgDataArrayAll[0].length * (ecgImageWidth - gridSpace * 5) /   gridSpace / (speed* ecgDataArrayAll[0].length / SAMPLE_RATE));
            }
            else
                length = length / gridSpace;
            dataArray = new short[ecgDataArrayAll.length][(int)(ecgDataArrayAll[0].length * ((ecgImageWidth - gridSpace * 5) / baseEcgPreviewTemplate.getLeadColumes() /  gridSpace) / (speed * ecgDataArrayAll[0].length / SAMPLE_RATE))];
            refreshData(length, isFirst, ecgDataArrayAll,speed);
            isFirst = false;
            for (int i = 0; i < dataArray.length; i++) {
                System.arraycopy(ecgDataArrayAll[i], firstIndex, dataArray[i], 0, dataArray[0].length);
            }
        }
        return dataArray;
    }

    /**
     * 所画数据的总长度
     */
    private int mAllDrawEcgDataLen;
    /**
     * 整个屏幕能画的数据
     */
    private int mScreenDrawEcgDataLen;
    private int firstIndex_rth,lastIndex_rth;
    private int firstIndex, lastIndex;
    private float dataRatio = 0;
    private int mScreenShowCount;
    private boolean isFirst = true;
    private short[][] onePointData;

    public void clearCurrentScreenData() {
        firstIndex = 0;
        lastIndex = 0;
        isFirst = true;
        mAllDrawEcgDataLen=0;
        firstIndex_rth=0;
        lastIndex_rth=0;
    }

    public float getDataRatio() {
        return dataRatio;
    }

    /**
     * 刷新正常导联数据
     * @param length
     * @param isFirst
     * @param data
     * @param speed
     * @return
     */
    private void refreshData(float length, boolean isFirst, short[][] data,float speed) {
        int count = (int) (data[0].length * length / (speed* data[0].length / SAMPLE_RATE));
        if (count == 0)
            return;

        if (length == 0 || baseEcgPreviewTemplate == null || baseEcgPreviewTemplate.getLeadManager() == null) {
            return;
        }
        if ((count >= 0 && lastIndex >= data[0].length) || (count <= 0 && firstIndex <= 0)) {
            return;
        }
        if (isFirst) {
            onePointData = new short[data.length][1];
            lastIndex = data[0].length;
            firstIndex = data[0].length - count;
            mScreenShowCount = count;

        } else {

            firstIndex += count;
            lastIndex += count;
        }
        if (count > 0 && lastIndex > data[0].length) {
            count = data[0].length-lastIndex+count;
            lastIndex = data[0].length;
            firstIndex = lastIndex - mScreenShowCount;
        }

        if (count < 0 && firstIndex < 0) {
            count = -(firstIndex - count);
            firstIndex = 0;
            lastIndex = firstIndex + mScreenShowCount;
        }

        if (count < 0) {

            for (int i = firstIndex - count; i > firstIndex; i--) {
                for (int j = 0; j < data.length; j++) {
                    onePointData[j][0] = data[j][i];
                }
                baseEcgPreviewTemplate.addEcgData(onePointData);
            }
        } else {
            for (int i = lastIndex - count; i < lastIndex; i++) {

                for (int j = 0; j < data.length; j++) {
                    onePointData[j][0] = data[j][i];
                }
                baseEcgPreviewTemplate.addEcgData(onePointData);
            }
        }
        dataRatio = firstIndex / ((float) data[0].length);
    }
}
