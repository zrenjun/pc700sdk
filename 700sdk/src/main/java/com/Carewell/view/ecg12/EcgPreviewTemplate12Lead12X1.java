package com.Carewell.view.ecg12;

import android.graphics.RectF;
import java.util.List;

public class EcgPreviewTemplate12Lead12X1 extends BaseEcgPreviewTemplate {

    public EcgPreviewTemplate12Lead12X1(int width, int height,
                                        boolean isDrawGrid, List<String> leadNameList,
                                        float[] gainArray, LeadSpeedType leadSpeedType) {
        this.drawWidth = width;
        this.drawHeight = height;
        this.gainArray = gainArray;
        this.leadSpeedType = leadSpeedType;
        this.leadLines = 12;
        this.leadColumes =1;
        this.leadNameList = leadNameList;
        this.drawReportGridBg = isDrawGrid;
    }

    /**
     * 添加心电数据
     */
    @Override
    public void addEcgData(short[][] dataArray) {
        int leadNum = dataArray.length;
        short value = 0;
        //是否是胸导联
        boolean chestLead;
        int dataLen = dataArray[0].length;
        for (int i = 0; i < leadNum; i++) {
            for (int j = 0; j < dataLen; j++) {
                value = dataArray[i][j];
                chestLead = i > 5;
                leadManager.getLeadList().get(i).addFilterPoint(value, chestLead);
            }
        }
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
        float left = gridRect.left + largeGridSpace + reportWaveOffsetLeft;
        float right = gridRect.right;
        float top = (gridRect.top + topMargin);
        for (int i = 0; i < leadLines; i++) {
            leadManager.addLead(leadManager.new Lead(new RectF(left, top, right, top += (leadHeight))));
        }
    }

    /**
     * 绘制导联信息 定标，名称
     */
    public void drawLeadInfo() {
        int num = 0;
        boolean needScaleMove;
        for (int i = (int) (gridRect.top + leadHeight); i <= gridRect.bottom - timeRulerHeight; i += (int) leadHeight) {
            needScaleMove = num <= 3;
            if (num < leadLines) {
                drawLeadStandard(canvasBg, leadStandardPaint, gridRect.left + gridSpace * 3 + reportWaveOffsetLeft, i,
                        num > 5, false, false, false, needScaleMove, leadLines);
            }
            canvasBg.drawText(leadNameList.get(num), gridRect.left + largeGridSpace  * 2 , i - leadNameYOffset, fontPaint);
            num++;
        }
    }
}
