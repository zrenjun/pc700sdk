package com.Carewell.view.ecg12;

import android.content.Context;
import android.graphics.RectF;

import java.util.List;

public class EcgPreviewTemplate12Lead6X2 extends BaseEcgPreviewTemplate {

    public EcgPreviewTemplate12Lead6X2(Context context, int width, int height,
                                       boolean isDrawGrid, List<String> leadNameList,
                                       float[] gainArray, LeadSpeedType leadSpeedType){
        this.context = context;
        this.drawWidth = width;
        this.drawHeight = height;
        this.gainArray = gainArray;
        this.leadSpeedType = leadSpeedType;
        this.leadLines = 6;
        this.leadColumes = 2;
        this.leadNameList=leadNameList;
        this.drawReportGridBg = isDrawGrid;
    }

    /**
     * 添加心电数据
     */
    @Override
    public void addEcgData(short[][] dataArray) {
        int leadNum = dataArray.length;
        short value;
        //是否是胸导联
        boolean chestLead;
        int dataLen = dataArray[0].length;

        //定标数据
        if (scaleBean != null){
            dataLen = scaleBean.getDataArray().length;
        }
        //数据顺序相关
        int perColumeDataLen = dataLen / leadColumes;
        int beginDataLen;

        for (int i = 0; i < leadNum; i++) {
            for(int j=0;j<dataLen;j++){
                if(scaleBean != null){
                    value = scaleBean.getDataArray()[j];
                }else{
                    value = dataArray[i][j];
                }
                chestLead = i > 5;
                if(previewPageEnum != PreviewPageEnum.PAGE_REPORT){
                    if( i < leadManager.getLeadList().size() )
                        leadManager.getLeadList().get(i).addFilterPoint(value,chestLead);
                }else{
                    //预览，报告模式
                    if(recordOrderType == RecordOrderType.ORDER_INORDER){
                        //顺序，拿每个分段的数据
                        beginDataLen = (i / 6) * perColumeDataLen;
                        if(j >= beginDataLen && j < beginDataLen+perColumeDataLen){
                            leadManager.getLeadList().get(i).addFilterPoint(value,chestLead);
                        }
                    }else{
                        //同步，只拿最前面的数据
                        if(j < perColumeDataLen){
                            leadManager.getLeadList().get(i).addFilterPoint(value,chestLead);
                        }
                    }
                }
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
    private void initDrawWave(){
        float topMargin = largeGridSpace;
        float left = gridRect.left + largeGridSpace;
        float right = (gridRect.left + largeGridSpace + leadWidth);
        float top = (gridRect.top + topMargin);

        for(int i=0;i<leadLines;i++){
            leadManager.addLead(leadManager.new Lead(new RectF(left, top, right, top += (leadHeight))));
        }

        left = gridRect.left + largeGridSpace + leadWidth;
        right = (gridRect.right);
        top = (gridRect.top + topMargin);
        for(int i=0;i<leadLines;i++){
            leadManager.addLead(leadManager.new Lead(new RectF(left, top, right, top += (leadHeight))));
        }
    }


    /**
     * 绘制导联信息 定标，名称
     */
    public void drawLeadInfo(){
        int num = 0;
        for (float i = (gridRect.top + leadHeight/2 + largeGridSpace); i <= gridRect.bottom; i += leadHeight) {
            //left 1
            if (num < leadLines) {
                drawLeadStandard(canvasBg, leadStandardPaint, gridRect.left + gridSpace*3,i,
                        false,false,false,true, false,leadLines);
            }
            updateFontPaintColor();
            canvasBg.drawText(leadNameList.get(num), gridRect.left + largeGridSpace ,i - leadNameYOffset, fontPaint);

            //right 1
            if (num < leadLines) {
                drawLeadStandard(canvasBg, leadStandardPaint, gridRect.left + gridSpace*6 + leadWidth,i,
                        true,true,false,true, false,leadLines);
            }
            updateFontPaintColor();
            canvasBg.drawText(leadNameList.get(num + leadLines), gridRect.left + gridSpace*7 + leadWidth ,i - leadNameYOffset, fontPaint);
            num ++;
        }
    }
}
