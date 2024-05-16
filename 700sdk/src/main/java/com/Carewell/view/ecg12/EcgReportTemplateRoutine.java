package com.Carewell.view.ecg12;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.Carewell.ecg700.XmlUtil;
import com.Carewell.ecg700.entity.MinnesotaCodeItemBean;
import com.creative.sdkpack.R;
import com.Carewell.ecg700.EcgDataManager;
import com.Carewell.ecg700.entity.AiResultBean;
import com.Carewell.ecg700.entity.MacureResultBean;
import com.Carewell.ecg700.entity.PatientInfoBean;
import java.util.ArrayList;
import java.util.List;

public class EcgReportTemplateRoutine extends BaseEcgReportTemplate {

    public EcgReportTemplateRoutine(Context context, boolean isVertical, boolean drawGridBg) {
        super(context, isVertical, drawGridBg);
        patientInfoHeight = textHeight * 10;
        bottomInfoHeight = textHeight * 2 + smartGrid;
        //画四周表格
        drawRoundTable();
    }

    @Override
    public void drawTitleInfo(String titleInfo) {
        drawTitleInfoBase(titleInfo);
    }

    @Override
    public void drawPatientInfoTop(PatientInfoBean patientInfoBean) {
        ptText.setFakeBoldText(false);
        ptText.setTextSize(TEXT_SIZE_NORMAL);

        //画患者信息
        drawPatientInfoTopLandscapeMulColume(patientInfoBean);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void drawMacureResult(MacureResultBean macureResultBean) {
        ptText.setTextSize(TEXT_SIZE_NORMAL);

        int left = rect.left;
        int right = rect.right;
        int top = currentBottomPosition + smartGrid;
        int bottom = top + patientInfoHeight + ONE_PIXEL;

        currentBottomPosition = bottom;

        //画表格线
        int perRectWidth = (drawWidth - largeGrid) / 3;
        //middle line
        ecgCanvas.drawLine(perRectWidth, top, perRectWidth + ONE_PIXEL, bottom, ptLine);
        ecgCanvas.drawLine(perRectWidth * 2, top, perRectWidth * 2 + ONE_PIXEL, bottom, ptLine);
        //top line
        ecgCanvas.drawLine(left, top - ONE_PIXEL, right, top, ptLine);
        //bottom line
        ecgCanvas.drawLine(left, bottom - ONE_PIXEL, right, bottom, ptLine);

        int marginX = perRectWidth + smartGrid;

        //================测量值 ====================

        AiResultBean aiResultBean =  macureResultBean.getAiResultBean();
        float marginValue = largeGrid * 3F;
        float marginUnit = largeGrid * 14F;

        ecgCanvas.drawText(context.getString(R.string.print_measure_hr), marginX, top + largeGrid, ptText);
        ecgCanvas.drawText("P", marginX, top + 2 * largeGrid, ptText);
        ecgCanvas.drawText("PR", marginX, top + 3 * largeGrid, ptText);
        ecgCanvas.drawText("QRS", marginX, top + 4 * largeGrid, ptText);
        ecgCanvas.drawText("QT/QTC", marginX, top + 5 * largeGrid, ptText);
        ecgCanvas.drawText("P/QRS/T", marginX, top + 6 * largeGrid, ptText);
        ecgCanvas.drawText("RV5/SV1", marginX, top + 7 * largeGrid, ptText);
        ecgCanvas.drawText("RV5+SV1", marginX, top + 8 * largeGrid, ptText);
        ecgCanvas.drawText("RV6/SV2", marginX, top + 9 * largeGrid, ptText);

        boolean isQBQ = EcgDataManager.checkIfPacemaker(aiResultBean);
        if (isQBQ) {
            ecgCanvas.drawText("         : --", (marginX + marginValue), top + largeGrid, ptText);
            ecgCanvas.drawText(String.format("%s : --", context.getString(R.string.print_measure_time_limit)), (marginX + marginValue), top + 2 * largeGrid, ptText);
            ecgCanvas.drawText(String.format("%s : --", context.getString(R.string.print_measure_time_interval)), (marginX + marginValue), top + 3 * largeGrid, ptText);
            ecgCanvas.drawText(String.format("%s : --", context.getString(R.string.print_measure_time_limit)), (marginX + marginValue), top + 4 * largeGrid, ptText);
            ecgCanvas.drawText(String.format("%s : -- / --", context.getString(R.string.print_measure_time_limit)), (marginX + marginValue), top + 5 * largeGrid, ptText);
            ecgCanvas.drawText(String.format("%s : --/ --/ --", context.getString(R.string.print_measure_electric_axis)), (marginX + marginValue), top + 6 * largeGrid, ptText);
            ecgCanvas.drawText(String.format("%s : -- / --", context.getString(R.string.print_measure_amplitude)), (marginX + marginValue), top + 7 * largeGrid, ptText);
            ecgCanvas.drawText(String.format("%s : --", context.getString(R.string.print_measure_amplitude)), (marginX + marginValue), top + 8 * largeGrid, ptText);
            ecgCanvas.drawText(String.format("%s : -- / --", context.getString(R.string.print_measure_amplitude)), (marginX + marginValue), top + 9 * largeGrid, ptText);
        } else {
            ecgCanvas.drawText(String.format("         : %d", aiResultBean.getHR()), (marginX + marginValue), top + largeGrid, ptText);
            ecgCanvas.drawText(String.format(context.getString(R.string.print_measure_time_limit) + " : %d", aiResultBean.getPd()), (marginX + marginValue), top + 2 * largeGrid, ptText);
            ecgCanvas.drawText(String.format(context.getString(R.string.print_measure_time_interval) + " : %d", aiResultBean.getPR()), (marginX + marginValue), top + 3 * largeGrid, ptText);
            ecgCanvas.drawText(String.format(context.getString(R.string.print_measure_time_limit) + " : %d", aiResultBean.getQRS()), (marginX + marginValue), top + 4 * largeGrid, ptText);
            ecgCanvas.drawText(String.format(context.getString(R.string.print_measure_time_limit) + " : %d / %d", aiResultBean.getQT(), aiResultBean.getQTc()), (marginX + marginValue), top + 5 * largeGrid, ptText);
            ecgCanvas.drawText(String.format(context.getString(R.string.print_measure_electric_axis) + " : %d/ %d/ %d", aiResultBean.getPAxis(), aiResultBean.getQRSAxis(), aiResultBean.getTAxis()), (marginX + marginValue), top + 6 * largeGrid, ptText);
            ecgCanvas.drawText(String.format(context.getString(R.string.print_measure_amplitude) + " : %.3f / %.3f", aiResultBean.getRV5() / 1000f, aiResultBean.getSV1() / 1000f), (marginX + marginValue), top + 7 * largeGrid, ptText);
            ecgCanvas.drawText(String.format(context.getString(R.string.print_measure_amplitude) + " : %.3f", aiResultBean.getRV5() / 1000f + aiResultBean.getSV1() / 1000f), (marginX + marginValue), top + 8 * largeGrid, ptText);
            ecgCanvas.drawText(String.format(context.getString(R.string.print_measure_amplitude) + " : %.3f / %.3f", aiResultBean.getRV6() / 1000f, aiResultBean.getSV2() / 1000f), (marginX + marginValue), top + 9 * largeGrid, ptText);
        }
        ecgCanvas.drawText("bpm", (marginX + marginUnit), top + largeGrid, ptText);
        ecgCanvas.drawText("ms", (marginX + marginUnit), top + 2 * largeGrid, ptText);
        ecgCanvas.drawText("ms", (marginX + marginUnit), top + 3 * largeGrid, ptText);
        ecgCanvas.drawText("ms", (marginX + marginUnit), top + 4 * largeGrid, ptText);
        ecgCanvas.drawText("ms", (marginX + marginUnit), top + 5 * largeGrid, ptText);
        ecgCanvas.drawText("°", (marginX + marginUnit), top + 6 * largeGrid, ptText);
        ecgCanvas.drawText("mV", (marginX + marginUnit), top + 7 * largeGrid, ptText);
        ecgCanvas.drawText("mV", (marginX + marginUnit), top + 8 * largeGrid, ptText);
        ecgCanvas.drawText("mV", (marginX + marginUnit), top + 9 * largeGrid, ptText);

        //===============画 右边 诊断结果=============================
        marginX = (marginX + perRectWidth);

        StringBuilder codeSb = new StringBuilder();
        if (aiResultBean.getAiResultDiagnosisBean() != null) {
            String[] codes = aiResultBean.getAiResultDiagnosisBean().getMinnesotacodes();
            if (codes != null) {
                for (String code : codes) {
                    codeSb.append(code).append("\t \t");
                }
            }
        }
        ecgCanvas.save();
        ecgCanvas.translate(marginX, top + smartGrid * 2);
        String contentCode = String.format("%s : %s", context.getString(R.string.print_minnesota_code), codeSb);
        StaticLayout staticLayoutCode = new StaticLayout(contentCode, new TextPaint(ptText), (largeGrid * 19),
                Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        staticLayoutCode.draw(ecgCanvas);
        ecgCanvas.restore();


        StringBuilder diaResultSb = new StringBuilder();
        List<MinnesotaCodeItemBean> diagnosis = aiResultBean.getAiResultDiagnosisBean().getDiagnosis();
        if (diagnosis != null && !diagnosis.isEmpty()) {
            for (int i = 0; i < diagnosis.size(); i++) {
                diaResultSb.append(String.format("%d . ", i + 1));
                diaResultSb.append(XmlUtil.INSTANCE.getMap().get(diagnosis.get(i).getCode())).append("\t \t ");
            }
        }

        ecgCanvas.save();
        ecgCanvas.translate(marginX, top + largeGrid * 2);
        String contentResult = String.format("%s : %s", context.getString(R.string.print_analysis_result), diaResultSb);
        StaticLayout staticLayoutResult = new StaticLayout(contentResult, new TextPaint(ptText), (largeGrid * 19),
                Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        staticLayoutResult.draw(ecgCanvas);
        ecgCanvas.restore();

        //医生确认
        ecgCanvas.drawText(String.format("%s : %s", context.getString(R.string.print_report_to_confirm), ""), (marginX), top + patientInfoHeight - smartGrid * 2, ptText);
    }


    @Override
    public void drawEcgImage(float[] gainArray, short[][] ecgDataArray, boolean averageTemplate, List<String> valueList) {
        int left = rect.left + smartGrid;
        int right = rect.right - smartGrid;
        int top = currentBottomPosition + ONE_PIXEL;
        int bottom = rect.bottom - bottomInfoHeight;
        int gridWidth = (right - left);
        int gridHeight = (bottom - top) + smartGrid * 3;
        BaseEcgPreviewTemplate baseEcgPreviewTemplate = getBaseEcgPreviewTemplate(context, PreviewPageEnum.PAGE_REPORT, smartGrid, gridWidth, gridHeight,
                gainArray, drawGridBg);

        baseEcgPreviewTemplate.initParams();
        baseEcgPreviewTemplate.setEcgMode(EcgShowModeEnum.MODE_SCROLL);
        baseEcgPreviewTemplate.addEcgData(ecgDataArray);
        baseEcgPreviewTemplate.drawEcgReport();
        baseEcgPreviewTemplate.drawTimeRuler(context,ecgDataArray[0].length / 1000f, 25f, 0);

        ecgCanvas.drawBitmap(baseEcgPreviewTemplate.getBgBitmap(), left, top, ptLine);
    }

    @Override
    public void drawBottomEcgParamInfo(String ecgParamInfo) {
        drawBottomEcgParamInfoBase(ecgParamInfo);
    }

    @Override
    public void drawBottomOtherInfo(String infoTip) {
        drawBottomOtherInfoBase(infoTip);
    }


    /**
     * 获取画图模板
     */
    public static BaseEcgPreviewTemplate getBaseEcgPreviewTemplate(Context context, PreviewPageEnum previewPageEnum, float smallGridSpace, int drawWidth, int drawHeight,
                                                                   float[] gainArray, boolean drawReportGridBg) {
        List<String> leadNameList = new ArrayList<>();
        leadNameList.add("I");
        leadNameList.add("II");
        leadNameList.add("III");
        leadNameList.add("aVR");
        leadNameList.add("aVL");
        leadNameList.add("aVF");
        leadNameList.add("V1");
        leadNameList.add("V2");
        leadNameList.add("V3");
        leadNameList.add("V4");
        leadNameList.add("V5");
        leadNameList.add("V6");
        BaseEcgPreviewTemplate baseEcgPreviewTemplate = new EcgPreviewTemplate12Lead12X1(context, drawWidth, drawHeight, drawReportGridBg, leadNameList, gainArray, LeadSpeedType.FORMFEED_25);
        baseEcgPreviewTemplate.init(previewPageEnum, smallGridSpace, RecordOrderType.ORDER_SYNC);
        return baseEcgPreviewTemplate;
    }
}
