package com.Carewell.view.ecg12;

import android.content.Context;

import com.Carewell.ecg700.port.LogUtil;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MainEcgManager {
    private static MainEcgManager instance = null;
    private static LeadType leadType;

    private DrawEcgRealView drawEcgRealView;
    //走速
    private LeadSpeedType leadSpeedType = LeadSpeedType.FORMFEED_25;
    //增益
    private LeadGainType leadGainType = LeadGainType.GAIN_10;
    private float[] gainArray = new float[]{1.0F, 1.0F};
    //顺序类型
    private RecordOrderType recordOrderType = RecordOrderType.ORDER_SYNC;
    //重置绘图
    private boolean resetDraw = false;

    private MainEcgManager() {

    }

    public static MainEcgManager getInstance() {
        if (instance == null) {
            instance = new MainEcgManager();
        }
        return instance;
    }

    //=================================
    public void init() {
        gainArray = updateGain();
    }

    public LeadSpeedType getLeadSpeedType() {
        return leadSpeedType;
    }

    public void setLeadSpeedType(LeadSpeedType leadSpeedType) {
        this.leadSpeedType = leadSpeedType;
    }

    public RecordOrderType getRecordOrderType() {
        return recordOrderType;
    }

    public void setRecordOrderType(RecordOrderType recordOrderType) {
        this.recordOrderType = recordOrderType;
    }

    public DrawEcgRealView getDrawEcgRealView() {
        return drawEcgRealView;
    }

    public void setDrawEcgRealView(DrawEcgRealView drawEcgRealView) {
        this.drawEcgRealView = drawEcgRealView;
    }

    public float[] getGainArray() {
        return gainArray;
    }

    public void setGainArray(float[] gainArray) {
        this.gainArray = gainArray;
    }

    /**
     * 初始化增益
     */
    private float[] updateGain() {
        float[] gainArrayTemp = new float[]{1.0F, 1.0F};
        switch (leadGainType) {
            case GAIN_2_P_5:
                gainArrayTemp[0] = 0.25F;
                gainArrayTemp[1] = 0.25F;
                break;
            case GAIN_5:
                gainArrayTemp[0] = 0.5F;
                gainArrayTemp[1] = 0.5F;
                break;
            case GAIN_20:
                gainArrayTemp[0] = 2.0F;
                gainArrayTemp[1] = 2.0F;
                break;
            case GAIN_40:
                gainArrayTemp[0] = 4.0F;
                gainArrayTemp[1] = 4.0F;
                break;
            case GAIN_10:
            default:
                break;
        }
        return gainArrayTemp;
    }

    /**
     * 重新绘制波形
     */
    public void resetDrawEcg() {
        if (drawEcgRealView != null) {
            resetDraw = true;
            drawEcgRealView.resetDrawEcg();
            resetDraw = false;
        }
    }

    /**
     * 获取画图模板
     */
    public static BaseEcgPreviewTemplate getBaseEcgPreviewTemplate(Context context, PreviewPageEnum previewPageEnum,
                                                                   float smallGridSpace, int drawWidth, int drawHeight,
                                                                   LeadSpeedType leadSpeedType, float[] gainArray,
                                                                   boolean drawReportGridBg, RecordOrderType recordOrderType) {
        String[] leadNameArray = new String[]{"I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"};
        List<String> leadNameList = new LinkedList<>(Arrays.asList(leadNameArray));
        BaseEcgPreviewTemplate baseEcgPreviewTemplate = null;
        switch (leadType) {
            case LEAD_12: {//6*2
                baseEcgPreviewTemplate = new EcgPreviewTemplate12Lead6X2(context, drawWidth, drawHeight, drawReportGridBg,
                        leadNameList, gainArray, leadSpeedType);
            }
            break;
            case LEAD_6: {//6*1
                baseEcgPreviewTemplate = new EcgPreviewTemplate12Lead6X1(context, drawWidth, drawHeight, drawReportGridBg,
                        leadNameList, gainArray, leadSpeedType);
            }
            break;
            case LEAD_I: {//L*1
                baseEcgPreviewTemplate = new EcgPreviewTemplate1leadL(context, drawWidth, drawHeight, drawReportGridBg,
                        leadNameList, gainArray, leadSpeedType);
            }
            break;
            case LEAD_II: {//F*1
                baseEcgPreviewTemplate = new EcgPreviewTemplate1leadF(context, drawWidth, drawHeight, drawReportGridBg,
                        leadNameList, gainArray, leadSpeedType);
            }
            break;
            default:
                break;
        }
        baseEcgPreviewTemplate.init(previewPageEnum, smallGridSpace, recordOrderType);

        return baseEcgPreviewTemplate;
    }

    /**
     * 添加数据
     */
    public void addEcgData(short[][] ecgDataArray) {  //12 x 1
        if (drawEcgRealView == null) {
            return;
        } else {
            drawEcgRealView.getBaseEcgPreviewTemplate();
        }

        if (drawEcgRealView.getBaseEcgPreviewTemplate() == null
                || drawEcgRealView.getBaseEcgPreviewTemplate().getLeadManager() == null
                || drawEcgRealView.getBaseEcgPreviewTemplate().getLeadManager().getLeadList().size() <= 0) {
            return;
        }

        if (resetDraw) {
            return;
        }

        drawEcgRealView.getBaseEcgPreviewTemplate().addEcgData(ecgDataArray);

        for (int i = 0; i < ecgDataArray.length; i++) {
            float tempValue = Math.abs(ecgDataArray[i][0] * Const.SHORT_MV_GAIN);
            if (i < 2) {
                //I II
                if (tempValue > bodyMaxValue) {
                    bodyMaxValue = tempValue;
                }
            } else {
                //v1-v6
                if (tempValue > chestMaxValue) {
                    chestMaxValue = tempValue;
                }
            }
        }
    }

    /**
     * 清理数据
     */
    public void clearEcgData() {
        if (drawEcgRealView == null || drawEcgRealView.getBaseEcgPreviewTemplate() == null) {
            return;
        }
        drawEcgRealView.getBaseEcgPreviewTemplate().clearData();
    }


    /**
     * 更新布局显示方式
     */
    public void updateMainEcgShowStyle(LeadType Type) {
        leadType = Type;
        //重新绘图
        if (drawEcgRealView == null || drawEcgRealView.getBaseEcgPreviewTemplate() == null) {
            return;
        }
        drawEcgRealView.getBaseEcgPreviewTemplate();
    }

    /**
     * 更新走速
     */
    public void updateMainSpeed(int enumType) {
        leadSpeedType = LeadSpeedType.values()[enumType];

        //重新绘图
        if (drawEcgRealView == null || drawEcgRealView.getBaseEcgPreviewTemplate() == null) {
            return;
        }
        drawEcgRealView.getBaseEcgPreviewTemplate().setSpeed(leadSpeedType);
    }

    //更新走速数据不画图  add by frf 2021/4/29
    public void updateMainSpeedOnlyData(int enumType) {
        leadSpeedType = LeadSpeedType.values()[enumType];
    }

    /**
     * 更新增益
     */
    public void updateMainGain(int enumType) {
        updateMainGainOnlyData(enumType);
        resetDrawEcg();
    }

    //更新增益数据不画图  add by frf 2021/4/29
    public void updateMainGainOnlyData(int enumType) {
        leadGainType = LeadGainType.values()[enumType];
        gainArray = updateGain();
    }

    /**
     *实时计算自动增益
     */
    public void realCalculateAutoSensitivity(){
        if (drawEcgRealView != null) {
            float[] newGain = calculateAutoSensitivity( 12);
            if (newGain[0] != gainArray[0] || newGain[1] != gainArray[1]) {
                gainArray = newGain;
                resetDrawEcg();
            }
        }
    }

    //肢体
    private float bodyMaxValue = 0F;
    //胸导联
    private float chestMaxValue = 0F;

    /**
     * 计算自动增益
     */
    public float[] calculateAutoSensitivity( int leadLines) {
        float[] gainArrayTemp = new float[]{1.0F, 1.0F};
        if (bodyMaxValue == 0f && chestMaxValue == 0f) {
            return gainArrayTemp;
        }
        //计算自动增益  有22个大格，每2个格子 1mv
        float maxShowMv = (22f / leadLines / 2f) * 0.5f;  //1.83
        if (bodyMaxValue > maxShowMv) {
            if (bodyMaxValue / 2 > maxShowMv) {
                gainArrayTemp[0] = 0.25f;
            } else {
                gainArrayTemp[0] = 0.5f;
            }
        }
        if (chestMaxValue > maxShowMv) {
            if (chestMaxValue / 2 > maxShowMv) {
                gainArrayTemp[1] = 0.25F;
            } else {
                gainArrayTemp[1] = 0.5F;
            }
        }
        LogUtil.INSTANCE.e(""+bodyMaxValue,"bodyMaxValue");
        LogUtil.INSTANCE.e(""+chestMaxValue,"chestMaxValue");
        LogUtil.INSTANCE.e("gainArrayTemp[0]:"+gainArrayTemp[0],"gainArrayTemp[1]:"+gainArrayTemp[1]);
        bodyMaxValue = 0F;
        chestMaxValue = 0F;
        return gainArrayTemp;
    }
}
