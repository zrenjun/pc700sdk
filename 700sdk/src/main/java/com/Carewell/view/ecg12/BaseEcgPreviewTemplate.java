package com.Carewell.view.ecg12;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import java.util.List;

public abstract class BaseEcgPreviewTemplate {

    /**
     * 添加心电数据
     */
    public abstract void addEcgData(short[][] dataArray);

    /**
     * 初始化参数
     */
    public abstract void initParams();

    /**
     * 绘制导联信息 定标，名称
     */
    public abstract void drawLeadInfo();

    float drawWidth;
    float drawHeight;
    float[] gainArray;
    LeadSpeedType leadSpeedType;
    LeadManager leadManager;
    Bitmap bgBitmap;
    Canvas canvasBg;
    Paint wavePaint;
    Paint fontPaint;
    Paint leadStandardPaint;
    Paint timeRulerPaint;
    Path timeRulerPath;
    //小网格
    float gridSpace;
    float largeGridSpace;
    RectF gridRect;
    //子类赋值
    float leadWidth;
    float leadHeight;
    int leadLines;
    int leadColumes;

    public int getLeadColumes() {
        return leadColumes;
    }

    //定标符号
    ScaleBean scaleBean;
    PreviewPageEnum previewPageEnum;
    List<String> leadNameList;
    //数据多列显示时的顺序
    RecordOrderType recordOrderType;
    boolean drawReportGridBg = true;
    boolean averageTemplateMode = false;
    int timeRulerHeight;
    float leadNameYOffset = 0;//导联名称 y 偏移量
    int reportWaveOffsetLeft = 0;

    public BaseEcgPreviewTemplate() {

    }

    public void init(PreviewPageEnum previewPageEnum, float smallGridSpace, RecordOrderType recordOrderType) {
        this.previewPageEnum = previewPageEnum;
        this.recordOrderType = recordOrderType;
        if (smallGridSpace == 0) {
            smallGridSpace = 5.88f;
        }
        if (drawWidth == 0) {
            drawWidth = 1152;
        }
        if (drawHeight == 0) {
            drawHeight = 650;
        }
        gridSpace = smallGridSpace;
        largeGridSpace = gridSpace * 5;
        Log.e("zrj", "gridSpace:" + gridSpace);
        Log.e("zrj", "largeGridSpace:" + largeGridSpace);
        bgBitmap = Bitmap.createBitmap((int) drawWidth, (int) drawHeight, Bitmap.Config.RGB_565);

        wavePaint = new Paint();
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setAntiAlias(true);
        wavePaint.setFilterBitmap(true);

        leadStandardPaint = new Paint();
        leadStandardPaint.setStyle(Paint.Style.STROKE);

        fontPaint = new Paint();
        fontPaint.setStyle(Paint.Style.FILL);

        leadManager = new LeadManager();
        leadManager.setRangeLen(gridSpace);
        leadManager.setGridSpace(gridSpace);
        leadManager.setEcgMode(EcgShowModeEnum.MODE_SWEEP);
        leadManager.setSpeed(leadSpeedType);
        leadManager.setSensitivity(gainArray);


        canvasBg = new Canvas();
        canvasBg.setBitmap(bgBitmap);

        if (previewPageEnum == PreviewPageEnum.PAGE_REPORT) {
            canvasBg.drawColor(EcgConfig.pdfBgColor);
            wavePaint.setColor(EcgConfig.pdfWaveColor);
            wavePaint.setStrokeWidth(BaseEcgReportTemplate.ONE_PIXEL);
            fontPaint.setTextSize(BaseEcgReportTemplate.TEXT_SIZE_NORMAL);
            fontPaint.setColor(EcgConfig.pdfWaveColor);
            leadStandardPaint.setColor(EcgConfig.pdfWaveColor);
            leadStandardPaint.setStrokeWidth(BaseEcgReportTemplate.ONE_PIXEL);
            reportWaveOffsetLeft = (int) (largeGridSpace * 3);
        } else {
            canvasBg.drawColor(EcgConfig.screenBgColor);
            wavePaint.setColor(EcgConfig.screenWaveColor);
            wavePaint.setStrokeWidth(2.0F);
            fontPaint.setTextSize(16f);
            fontPaint.setColor(EcgConfig.fontColorStandard);
            leadStandardPaint.setColor(EcgConfig.fontColorStandard);
            leadStandardPaint.setStrokeWidth(1.1F);
        }

        float leftSpacing = largeGridSpace;
        float nWidth = leftSpacing * (drawWidth / leftSpacing);
        float nHeight = leftSpacing * (drawHeight / leftSpacing);

        float left = (drawWidth - nWidth) / 2f;
        float right = left + nWidth;
        float top = (drawHeight - nHeight) / 2f;
        float bottom = top + nHeight;
        gridRect = new RectF(left, top, right, bottom);

        if (previewPageEnum == PreviewPageEnum.PAGE_REPORT) {
            timeRulerHeight = 50;
            leadWidth = (50 * largeGridSpace) / leadColumes;
        } else {
            timeRulerHeight = 30;
            leadWidth = (gridRect.right - gridRect.left - largeGridSpace) / leadColumes;
        }
        leadHeight = (gridRect.bottom - timeRulerHeight - gridRect.top) / (leadLines);

        leadNameYOffset = largeGridSpace;
    }

    public void drawBase() {
        drawModeTip(canvasBg);
    }

    /**
     * 绘制模式提示
     */
    public void drawModeTip(Canvas canvasCell) {

        //报告 提示文字 暂时不想在中间这个位置画。会挡住波形
        if (previewPageEnum == PreviewPageEnum.PAGE_REPORT) {
            return;
        }

        if (averageTemplateMode) {
            return;
        }

        int colorId = 0;
        float textSize = 0;
        float x = 0;
        float y = 0;

        colorId = Color.parseColor("#899ba0");
        textSize = 16;

        x = 0;
        y = (float) drawHeight / 2 - largeGridSpace * 3;

        Paint modePaint = new Paint();
        modePaint.setColor(colorId);
        modePaint.setTextSize(textSize);
        String text = "";
        canvasCell.save();
        canvasCell.translate(x, y);
        String contentResult = String.format("%s", text);
        StaticLayout staticLayoutResult = new StaticLayout(contentResult, new TextPaint(modePaint), (int) drawWidth,
                Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        staticLayoutResult.draw(canvasCell);
        canvasCell.restore();
    }

    /**
     * 画背景表格
     */
    public void drawGridBg(Canvas canvas) {
        if (previewPageEnum == PreviewPageEnum.PAGE_REPORT) {
            if (drawReportGridBg) {
                drawPdfGridBg(canvas);
            }
        } else {
            drawScreenGridBg(canvas);
        }
    }

    public void drawScreenGridBg(Canvas canvas) {
        Paint paintWide = new Paint();
        paintWide.setPathEffect(new DashPathEffect(new float[]{1, 0}, 0));
        paintWide.setColor(EcgConfig.screenGridWideColor);

        Paint paintThin = new Paint();
        paintThin.setPathEffect(new DashPathEffect(new float[]{0.5F, 0}, 0));
        paintThin.setColor(EcgConfig.screenGridThinColor);

        float i;
        for (i = gridRect.left; i <= gridRect.right; i += gridSpace) {
            canvas.drawLine(i, gridRect.top, i, gridRect.bottom, paintThin);
        }
        for (i = gridRect.top; i <= gridRect.bottom; i += gridSpace) {
            canvas.drawLine(gridRect.left, i, gridRect.right, i, paintThin);
        }

        int numGrid = 0;
        for (i = gridRect.left; i <= gridRect.right; i += largeGridSpace) {
            if (EcgConfig.LARGE_GRID_DIVIDER) {
                if (numGrid % 5 == 0) {
                    paintWide.setColor(Color.RED);
                } else {
                    paintWide.setColor(EcgConfig.screenGridWideColor);
                }
            }
            canvas.drawLine(i, gridRect.top, i, gridRect.bottom, paintWide);
            numGrid++;
        }

        numGrid = 0;
        for (i = gridRect.top; i <= gridRect.bottom; i += largeGridSpace) {
            if (EcgConfig.LARGE_GRID_DIVIDER) {
                if (numGrid % 5 == 0) {
                    paintWide.setColor(Color.RED);
                } else {
                    paintWide.setColor(EcgConfig.screenGridWideColor);
                }
            }
            canvas.drawLine(gridRect.left, i, gridRect.right, i, paintWide);
            numGrid++;
        }
    }

    public void drawPdfGridBg(Canvas canvas) {

        float smallGridPx = gridSpace;
        float largeGridPx = gridSpace * 5;
        // 绘制小网格（使用圆点）
        Paint paintSmall = new Paint();
        paintSmall.setColor(EcgConfig.pdfGridWideColor);
        paintSmall.setStrokeWidth(2);
        paintSmall.setStyle(Paint.Style.FILL); // 使用填充模式绘制圆点

        int k = 0;
        // 绘制水平小网格线（圆点）
        for (float y = gridRect.top; y <= gridRect.bottom; y += smallGridPx) {
            if (k % 5 != 0) {
                int j = 0;
                for (float x = gridRect.left; x <= gridRect.right; x += smallGridPx) {
                    if(j % 5 != 0){
                        // 绘制圆点，半径为1像素
                        canvas.drawCircle(x, y, 1, paintSmall);
                    }
                    j++;
                }
            }
            k++;
        }

        //
        Paint paintBig = new Paint();
        paintBig.setColor(EcgConfig.pdfGridWideColor);
        paintBig.setStrokeWidth(1);
        paintBig.setStyle(Paint.Style.STROKE);
        for (float i = gridRect.left; i <= gridRect.right; i += largeGridPx) {
            canvas.drawLine(i, gridRect.top, i, gridRect.bottom, paintBig);
        }

        for (float i = gridRect.top; i <= gridRect.bottom; i += largeGridPx) {
            canvas.drawLine(gridRect.left, i, gridRect.right, i, paintBig);
        }
    }

    /**
     * 画导联定标符号
     */
    public void drawLeadStandard(Canvas canvas, Paint paint, float x, float y,
                                 boolean chestLead, boolean drawVerticalBar, boolean rthLead, boolean mulColume, boolean needScaleMove, int leadLines) {
        //20
        float[] af = new float[40];
        //mStandardLen的4/5，在平分4份
        float xDistance = largeGridSpace * 4 / 5 / 4;

        float sensitivity = 1.0F;
        float sensitivityBody = gainArray[0];
        float sensitivityChest = gainArray[1];
        float minusHeight = gridSpace * 10;

        if (chestLead) {
            //v1-v6
            sensitivity *= sensitivityChest;
        } else {
            //I II ..
            sensitivity *= sensitivityBody;
        }

        //增益高了，定标符号会显示不全。特殊处理，往下拉
        y = doWithY(needScaleMove, leadLines, sensitivity, y);

        if (drawVerticalBar) {
            minusHeight *= sensitivity;

            af[4] = x - xDistance;
            af[5] = y;
            af[6] = x - xDistance;
            af[7] = y - minusHeight;
        } else {
            boolean sensitivitySame = sensitivityBody == sensitivityChest;
            if (rthLead || !mulColume || sensitivitySame) {
                minusHeight *= sensitivity;

                //节律导联 || 竖排单列 || (多列 并且 增益相同)。左排用1个定标符号
                af[0] = x - xDistance * 2;
                af[1] = y;
                af[2] = x - xDistance;
                af[3] = y;

                af[4] = x - xDistance;
                af[5] = y;
                af[6] = x - xDistance;
                af[7] = y - minusHeight;

                af[8] = x - xDistance;
                af[9] = y - minusHeight;
                af[10] = x + xDistance;
                af[11] = y - minusHeight;

                af[12] = x + xDistance;
                af[13] = y - minusHeight;
                af[14] = x + xDistance;
                af[15] = y;

                af[16] = x + xDistance;
                af[17] = y;
                af[18] = x + xDistance * 2;
                af[19] = y;
            } else {
                //左排用2个定标符号
                //1定标 ======================================
                x -= gridSpace;

                af[0] = x - xDistance;
                af[1] = y;
                af[2] = x - xDistance / 2;
                af[3] = y;

                af[4] = x - xDistance / 2;
                af[5] = y;
                af[6] = x - xDistance / 2;
                af[7] = y - minusHeight * sensitivityBody;

                af[8] = x - xDistance / 2;
                af[9] = y - minusHeight * sensitivityBody;
                af[10] = x + xDistance / 2;
                af[11] = y - minusHeight * sensitivityBody;

                af[12] = x + xDistance / 2;
                af[13] = y - minusHeight * sensitivityBody;
                af[14] = x + xDistance / 2;
                af[15] = y;

                af[16] = x + xDistance / 2;
                af[17] = y;
                af[18] = x + xDistance;
                af[19] = y;

                //2定标 ==================================
                af[20] = x + xDistance / 2;
                af[21] = y;
                af[22] = x + xDistance;
                af[23] = y;

                af[24] = x + xDistance;
                af[25] = y;
                af[26] = x + xDistance;
                af[27] = y - minusHeight * sensitivityChest;

                af[28] = x + xDistance;
                af[29] = y - minusHeight * sensitivityChest;
                af[30] = x + xDistance * 2;
                af[31] = y - minusHeight * sensitivityChest;

                af[32] = x + xDistance * 2;
                af[33] = y - minusHeight * sensitivityChest;
                af[34] = x + xDistance * 2;
                af[35] = y;

                af[36] = x + xDistance * 2;
                af[37] = y;
                af[38] = x + xDistance * 2.5f;
                af[39] = y;
            }
        }

        canvas.drawLines(af, paint);
    }

    /**
     * 绘制时间标尺
     */
    public void drawTimeRuler(float timeLen, LeadSpeedType leadSpeedType, float startTime) {
        if (timeRulerHeight == 0)
            return;

        float x = LeadSpeedType.FORMFEED_25.getValue() / leadSpeedType.getValue();

        float nTimeScaleBitmapW = timeLen * gridSpace * leadSpeedType.getValue() * x;
        float len = nTimeScaleBitmapW - 2;
        float baseY = gridRect.bottom - timeRulerHeight;
        float baseX = gridRect.left + largeGridSpace + leadColumes == 1 ? (largeGridSpace * 3) : largeGridSpace;
        float yOffset = 0f, verticalLineHeight = 0;
        if (timeRulerPaint == null) {
            timeRulerPaint = new Paint();
            timeRulerPath = new Path();
            if (previewPageEnum == PreviewPageEnum.PAGE_REPORT) {
                PathEffect effects = new DashPathEffect(new float[]{10, 10}, 10);
                timeRulerPaint.setPathEffect(effects);
                timeRulerPaint.setColor(Color.BLACK);
                timeRulerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                timeRulerPaint.setTextSize(25);
                timeRulerPaint.setStrokeWidth(4.0f);
            }
        }

        if (previewPageEnum == PreviewPageEnum.PAGE_REPORT) {
            yOffset = 20;
            verticalLineHeight = 40;
        }
        Canvas canvas = canvasBg;
        float horizontalLineY = baseY + yOffset;
        timeRulerPath.moveTo(baseX + 1, horizontalLineY - verticalLineHeight / 2);
        timeRulerPath.lineTo(baseX + 1, horizontalLineY + verticalLineHeight / 2);
        for (int i = 1; i < (timeLen); i++) {
            timeRulerPath.moveTo(baseX + gridSpace * leadSpeedType.getValue() * i, horizontalLineY - verticalLineHeight / 2);
            timeRulerPath.lineTo(baseX + gridSpace * leadSpeedType.getValue() * i, horizontalLineY + verticalLineHeight / 2);
        }
        timeRulerPath.moveTo(baseX + len, horizontalLineY - verticalLineHeight / 2);
        timeRulerPath.lineTo(baseX + len, horizontalLineY + verticalLineHeight / 2);
        timeRulerPaint.setStyle(Paint.Style.STROKE);

        timeRulerPath.moveTo(baseX + 1, horizontalLineY);
        timeRulerPath.lineTo(baseX + len, horizontalLineY);
        timeRulerPaint.setStrokeWidth(1.0f);
        canvas.drawPath(timeRulerPath, timeRulerPaint);

        timeRulerPaint.setStyle(Paint.Style.FILL);
        timeRulerPaint.setTextAlign(Paint.Align.LEFT);
        String formatStr = previewPageEnum == PreviewPageEnum.PAGE_REPORT ? "%.1f" : "%.2f";
        canvas.drawText(String.format(formatStr + "秒", 0 + startTime), baseX + 3, horizontalLineY + verticalLineHeight - 15, timeRulerPaint);
        if (leadColumes == 1) {
            timeRulerPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.format(formatStr + "秒", startTime + timeLen / leadColumes), baseX + nTimeScaleBitmapW / leadColumes + 2, horizontalLineY + verticalLineHeight - 15, timeRulerPaint);
        } else {
            for (int i = 1; i < leadColumes; i++) {
                canvas.drawText(String.format(formatStr + "秒", startTime), baseX + nTimeScaleBitmapW / leadColumes * i + 2, horizontalLineY + verticalLineHeight - 15, timeRulerPaint);
            }
            timeRulerPaint.setTextAlign(Paint.Align.RIGHT);
            if (timeLen % leadColumes == 0) {
                canvas.drawText(String.format(formatStr + "秒", startTime + timeLen * x / leadColumes), baseX + nTimeScaleBitmapW, horizontalLineY + verticalLineHeight - 15, timeRulerPaint);
            } else {
                canvas.drawText(String.format(formatStr + "秒", startTime + timeLen * x / leadColumes), baseX + nTimeScaleBitmapW, horizontalLineY + verticalLineHeight - 15, timeRulerPaint);
            }
        }
    }

    public Bitmap getBgBitmap() {
        return bgBitmap;
    }

    public LeadManager getLeadManager() {
        return leadManager;
    }


    /**
     * 实时画图
     */
    public void drawEcgPathRealTime(Canvas canvas) {
        if (leadManager == null) {
            return;
        }
        leadManager.calcPath();
        leadManager.drawEcgPath(canvas, wavePaint);
    }

    /**
     * 预览画图
     */
    public void drawEcgPathPreview() {
        if (leadManager == null) {
            return;
        }
        drawPerviewGridBg(canvasBg);
        leadManager.calcPath();
        leadManager.drawEcgPath(canvasBg, wavePaint);
    }

    Bitmap gridCellBitmap;

    /**
     * 画预览背景表格
     */
    public void drawPerviewGridBg(Canvas canvas) {
        if (gridCellBitmap == null) {
            gridCellBitmap = Bitmap.createBitmap((int) drawWidth, (int) drawHeight, Bitmap.Config.RGB_565);
            Canvas canvasCell = new Canvas(gridCellBitmap);
            canvasCell.drawColor(EcgConfig.screenBgColor);
            drawScreenGridBg(canvasCell);
            drawModeTip(canvasCell);
        }
        canvas.drawBitmap(gridCellBitmap, 0, 0, new Paint());
    }


    /**
     * 报告画图
     */
    public void drawEcgReport() {
        if (leadManager == null) {
            return;
        }
        leadManager.calcPath();
        leadManager.drawEcgPath(canvasBg, wavePaint);
    }

    /**
     * 清理心电数据
     * 导联list 不清理
     */
    public void clearData() {
        if (leadManager == null) {
            return;
        }
        leadManager.clearEcgData();
    }

    /**
     * 设置走速
     */
    public void setSpeed(LeadSpeedType leadSpeedType) {
        leadManager.setSpeed(leadSpeedType);
    }

    /**
     * 设置ecg显示模式
     */
    public void setEcgMode(EcgShowModeEnum ecgShowModeEnum) {
        leadManager.setEcgMode(ecgShowModeEnum);
    }

    public void updateFontPaintColor() {
        fontPaint.setColor(EcgConfig.fontColorStandard);
    }

    private float doWithY(boolean needScaleMove, int leadLines, float sensitivity, float y) {
        float newY = 0;
        if (needScaleMove) {
            if (leadLines > 12) {
                //1.0增益也需要处理
                if (sensitivity == 1.0) {
                    y += largeGridSpace * 1;
                } else if (sensitivity == 2.0) {
                    y += largeGridSpace * 3;
                } else if (sensitivity == 4.0) {
                    y += largeGridSpace * 20;
                }
            } else {
                //只处理2.0  ；  4.0增益
                if (sensitivity == 2.0) {
                    if (leadLines >= 6) {
                        y += largeGridSpace * 2;
                    } else {
                        y += largeGridSpace * 1;
                    }
                } else if (sensitivity == 4.0) {
                    if (leadLines >= 6) {
                        y += largeGridSpace * 6;
                    } else {
                        y += largeGridSpace * 5;
                    }
                }
            }
            newY = y;
        }

        if (newY == 0) {
            return y;
        } else {
            return newY;
        }
    }
}
