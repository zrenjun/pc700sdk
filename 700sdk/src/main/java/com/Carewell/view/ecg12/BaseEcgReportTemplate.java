package com.Carewell.view.ecg12;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import com.Carewell.ecg700.entity.MacureResultBean;
import com.Carewell.ecg700.entity.PatientInfoBean;
import com.creative.sdkpack.R;
import java.util.List;



public abstract class BaseEcgReportTemplate {

    public static final float TEXT_SIZE_TITLE_BIG = 45.0F;
    public static final float TEXT_SIZE_NORMAL = 35.0F;
    public static final float ONE_PIXEL = 2.08f;
    public  static final int A4_WIDTH = 2100;
    public static final int A4_HEIGHT = 2970;
    /**
     * 打印模式默认为横向,纵向
     */
    boolean isVertical;
    Context context;

    float smartGrid;
    float largeGrid;
    Rect rect;
    int textHeight;
    //左右间距
    int leftMargin;
    //上下间距
    int topMargin;
    Bitmap bgEcg;
    Canvas ecgCanvas;
    Paint ptText;
    Paint ptLine;
    int currentBottomPosition;
    boolean drawGridBg = true;
    int patientInfoHeight;
    int bottomInfoHeight;

    int drawWidth;
    int drawHeight;

    public BaseEcgReportTemplate(Context context, boolean isVertical, boolean drawGridBg) {
        this.context = context;
        this.isVertical = isVertical;
        this.drawGridBg = drawGridBg;

        //old
        smartGrid = 5 * ONE_PIXEL;
        largeGrid = 5 * smartGrid;
        textHeight = (int) largeGrid;
        leftMargin = (int)largeGrid;
        topMargin = (int)largeGrid;

        if (!isVertical) {
            //横版
            drawWidth = A4_HEIGHT - 2 * leftMargin;
            drawHeight = A4_WIDTH - 2 * topMargin;
            bgEcg = Bitmap.createBitmap(A4_HEIGHT, A4_WIDTH, Bitmap.Config.RGB_565);
        } else {
            //竖版
            drawWidth = A4_WIDTH - 2 * leftMargin;
            drawHeight = A4_HEIGHT - 2 * topMargin;

            bgEcg = Bitmap.createBitmap(A4_WIDTH, A4_HEIGHT, Bitmap.Config.RGB_565);
        }
        rect = new Rect(leftMargin, topMargin, drawWidth + leftMargin, drawHeight + topMargin);

        ecgCanvas = new Canvas(bgEcg);
        ecgCanvas.drawColor(Color.WHITE);

        ptText = new Paint();
        ptText.setColor(Color.BLACK);
        ptText.setTextAlign(Paint.Align.LEFT);
        ptText.setTextSize(TEXT_SIZE_NORMAL);

        ptLine = new Paint();
        ptLine.setColor(Color.BLACK);
        ptLine.setStyle(Paint.Style.STROKE);
        ptLine.setStrokeWidth(ONE_PIXEL);
        ptLine.setAntiAlias(true);
    }

    /**
     * 画最顶部信息
     */
    public abstract void drawTitleInfo(String titleInfo);

    /**
     * 画顶部通用患者信息
     *
     */
    public abstract void drawPatientInfoTop(PatientInfoBean patientInfoBean);

    /**
     * 画测量值 诊断结果
     */
    public abstract void drawMacureResult(MacureResultBean macureResultBean);

    /**
     * 画心电图
     */
    public abstract void drawEcgImage(float[] gainArray, LeadSpeedType leadSpeedType,short[][] ecgDataArray,
                                      boolean averageTemplate, List<String> macureValueList);

    /**
     * 画心电参数信息
     */
    public abstract void drawBottomEcgParamInfo(String ecgParamInfo);

    /**
     * 画心电参数信息
     */
    public abstract void drawBottomOtherInfo(String infoTip);

    public Bitmap getBgEcg() {
        return bgEcg;
    }


    /**
     * 画四周表格
     */
    public void drawRoundTable() {
        int left = rect.left;
        int right = rect.right;
        int top = rect.top + (int)ONE_PIXEL;
        int bottom = rect.bottom;

        //top line
        ecgCanvas.drawLine(left, top - ONE_PIXEL, right, top, ptLine);
        //bottom
        ecgCanvas.drawLine(left, bottom - ONE_PIXEL, right, bottom, ptLine);
        //left
        ecgCanvas.drawLine(left, top, left + ONE_PIXEL, bottom, ptLine);
        //right
        ecgCanvas.drawLine(right - ONE_PIXEL, top, right, bottom, ptLine);

        currentBottomPosition = top;
    }

    /***
     * 绘制title信息
     */
    public void drawTitleInfoBase(String titleInfo) {
        ptText.setFakeBoldText(true);
        ptText.setTextSize(TEXT_SIZE_TITLE_BIG);

        int left = rect.left;
        int right = rect.right;
        int top = currentBottomPosition;
        int topY = top + (int)largeGrid;

        ecgCanvas.save();
        ecgCanvas.translate(left, topY - largeGrid);
        String contentResult = String.format("%s", titleInfo);
        StaticLayout staticLayoutResult = new StaticLayout(contentResult, new TextPaint(ptText), right - (int)largeGrid,
                Layout.Alignment.ALIGN_CENTER, 0, 0, false);
        staticLayoutResult.draw(ecgCanvas);
        ecgCanvas.restore();

        currentBottomPosition = topY;
    }


    /**
     * 获取性别
     */
    public String getSex(PatientInfoBean patientInfoBean) {
        String sex = "";
        if (!TextUtils.isEmpty(patientInfoBean.getSex())) {
            sex = patientInfoBean.getSex().equals("0") ?context.getString(R.string.print_export_sex_man):context.getString(R.string.print_export_sex_woman);
        }
        return sex;
    }

    /**
     * 绘制多列，患者信息
     */
    public void drawPatientInfoTopLandscapeMulColume(PatientInfoBean patientInfoBean) {
        int left = rect.left + (int)smartGrid;
        int top = currentBottomPosition + (int)smartGrid;
        //画患者信息
        String patientNumber = String.format("%s:%s", context.getString(R.string.print_export_patient_number),
                patientInfoBean.getPatientNumber() == null ? "" : patientInfoBean.getPatientNumber());
        String patientName = String.format("%s:%s", context.getString(R.string.print_export_patient_name),
                patientInfoBean.getArchivesName() == null ? "" : patientInfoBean.getArchivesName());

        String patientSex = String.format("%s:%s", context.getString(R.string.print_export_patient_sex), getSex(patientInfoBean));
        String age = patientInfoBean.getAge();
        String patientAge = String.format("%s:%s", context.getString(R.string.print_export_patient_age), age);

        String patientApplyDepartment = String.format("%s:%s", context.getString(R.string.print_export_patient_apply_department),
                patientInfoBean.getApplyDepartment() == null ? "" : patientInfoBean.getApplyDepartment());
        String patientApplyDoctor = String.format("%s:%s", context.getString(R.string.print_export_patient_apply_doctor),
                patientInfoBean.getApplyDoctor() == null ? "" : patientInfoBean.getApplyDoctor());
        String patientCheckDepartment = String.format("%s:%s", context.getString(R.string.print_export_patient_check_department),
                patientInfoBean.getCheckDepartment() == null ? "" : patientInfoBean.getCheckDepartment());
        String patientCheckDoctor = String.format("%s:%s", context.getString(R.string.print_export_patient_check_doctor),
                patientInfoBean.getCheckTechnician() == null ? "" : patientInfoBean.getCheckTechnician());
        String patientBedNumber = String.format("%s:%s", context.getString(R.string.print_export_patient_bed_number),
                patientInfoBean.getBedNo() == null ? "" : patientInfoBean.getBedNo());

        ecgCanvas.drawText(patientNumber, left, top + textHeight, ptText);

        ecgCanvas.drawText(patientName, left, top + textHeight * 2, ptText);
        ecgCanvas.drawText(patientSex, left, top + textHeight * 3, ptText);

        ecgCanvas.drawText(patientAge, left, top + textHeight * 4, ptText);
        ecgCanvas.drawText(patientApplyDepartment, left, top + textHeight * 5, ptText);

        ecgCanvas.drawText(patientApplyDoctor, left, top + textHeight * 6, ptText);
        ecgCanvas.drawText(patientCheckDepartment, left, top + textHeight * 7, ptText);

        ecgCanvas.drawText(patientCheckDoctor, left, top + textHeight * 8, ptText);
        ecgCanvas.drawText(patientBedNumber, left, top + textHeight * 9, ptText);
    }

    public void drawBottomEcgParamInfoBase(String ecgParamInfo) {
        ptText.setFakeBoldText(false);
        ptText.setTextSize(TEXT_SIZE_NORMAL);

        int left = rect.left + (int)smartGrid;
        int topY = rect.bottom - textHeight;// - smartGrid*2

        ecgCanvas.drawText(ecgParamInfo, left, topY, ptText);
    }

    public void drawBottomOtherInfoBase(String infoTip) {
        ptText.setFakeBoldText(false);
        ptText.setTextSize(TEXT_SIZE_NORMAL);
        int left = rect.left + (int)smartGrid;
        int topY = rect.bottom - (int)smartGrid;//*3
        ecgCanvas.drawText(infoTip, left, topY, ptText);
        ecgCanvas.save();
        ecgCanvas.translate(left, topY - smartGrid * 3);
        ecgCanvas.restore();
    }
}
