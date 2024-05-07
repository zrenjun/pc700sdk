package com.Carewell.view.ecg12;

import android.content.Context;
import android.graphics.RectF;

import java.util.List;

public class EcgPreviewTemplate1leadL extends BaseEcgPreviewTemplate {

    public EcgPreviewTemplate1leadL(Context context, int width, int height,
                                    boolean isDrawGrid, List<String> leadNameList,
                                    float[] gainArray, LeadSpeedType leadSpeedType) {
        this.context = context;
        this.drawWidth = width;
        this.drawHeight = height;
        this.gainArray = gainArray;
        this.leadSpeedType = leadSpeedType;
        this.leadLines = 1;
        this.leadColumes = 1;
        this.leadNameList = leadNameList;
        this.drawReportGridBg = isDrawGrid;
    }

    /**
     * 添加心电数据
     */
    @Override
    public void addEcgData(short[][] dataArray) {
        int leadNum = 1;
        short value;
        int dataLen = dataArray[0].length;

        //定标数据
        if (scaleBean != null) {
            dataLen = scaleBean.getDataArray().length;
        }

        for (int i = 0; i < leadNum; i++) {//单导联L只处理第一导联的数据
            for (int j = 0; j < dataLen; j++) {
                if (scaleBean != null) {
                    value = scaleBean.getDataArray()[j];
                } else {
                    value = dataArray[i][j];
                }

                leadManager.getLeadList().get(i).addFilterPoint(value, false);
            }
        }

        scaleBean = null;
    }

    /**
     * 初始化画布
     */
    @Override
    public void initParams() {
        initDrawWave();
        drawGridBg(canvasBg);
        drawBase();
        drawLeadInfo();
    }

    /**
     * 初始化导联布局
     */
    private void initDrawWave() {

        float topMargin = largeGridSpace;
        float left = gridRect.left + largeGridSpace;
        float right = (gridRect.left + largeGridSpace + leadWidth);
        float top = (gridRect.top + topMargin);

        leadManager.addLead(leadManager.new Lead(new RectF(left, top, right, top += (leadHeight))));
    }

    /**
     * 绘制导联信息 定标，名称
     */
    public void drawLeadInfo() {
        int num = 0;
        for (float i = (gridRect.top + leadHeight / 2 + largeGridSpace); i < gridRect.bottom; ) {
            if (num < leadLines) {
                drawLeadStandard(canvasBg, leadStandardPaint, gridRect.left + gridSpace * 3, i,
                        false, false, false, false, true, leadLines);
            }

            updateFontPaintColor();
            canvasBg.drawText(leadNameList.get(0), gridRect.left + largeGridSpace, i - leadNameYOffset, fontPaint);
            num++;
            break;
        }
    }
}
