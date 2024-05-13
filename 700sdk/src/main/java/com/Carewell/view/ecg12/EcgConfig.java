package com.Carewell.view.ecg12;

import android.graphics.Color;

/**
 * Created by wuxd
 */
public class EcgConfig {
    /***
     获取1mm对应的像素点
     *****/
    public static float SMALL_GRID_SPACE_FLOAT = 5.88f;
    public static int screenBgColor;
    public static int screenGridThinColor;
    public static int screenGridWideColor;
    public static int screenWaveColor;
    public static int pdfBgColor;
    public static int pdfGridThinColor;
    public static int pdfGridWideColor;
    public static int pdfWaveColor;
    public static int fontColorStandard;
    public static int fontColorHighlight;
    public static int SPEED = 1000;      //每秒1000个点
    public static boolean LARGE_GRID_DIVIDER = false;//是否画 5个大格分割线
    public static final float RATE_MIN = 1.0f / 20;

    static {
        //屏幕画图
        screenBgColor = Color.BLACK;
        screenGridThinColor = Color.parseColor("#0a2f3a");
        screenGridWideColor = Color.parseColor("#126278");
        screenWaveColor = Color.parseColor("#00FF00");

        //pdf报告画图
        pdfBgColor = Color.WHITE;
        pdfGridThinColor = Color.parseColor("#40FF7F50");
        pdfGridWideColor = Color.parseColor("#FFFF7F50");
        pdfWaveColor = Color.BLACK;

        //导联名称颜色
        fontColorStandard = Color.WHITE;
        fontColorHighlight = Color.parseColor("#00FF00");
    }
}
