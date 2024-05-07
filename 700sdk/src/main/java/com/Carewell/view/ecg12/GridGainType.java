package com.Carewell.view.ecg12;

/****
 //========显示增益定义==================
 value -每毫伏 有多少采样点
 maxDrawmV 一行从0开始最多能画多少个毫伏
 #define grid_5mm_mv 4096/6     //1mV每格
 #define grid_10mm_mv  4096/3   //0.5mV每格
 #define grid_20mm_mv  4096*2/3 //0.25mV每格

 displayGain -显示增益
 *****/
public enum GridGainType {
    grid_2d5mm_mv("grid_2d5mm_mv", 4096.0f / 12, 12.0f, 2.5f),
    grid_5mm_mv("grid_5mm_mv", 4096.0f / 6, 6.0f, 5.0f),
    grid_10mm_mv("grid_10mm_mv", 4096.0f / 3, 3.0f, 10.0f),
    grid_20mm_mv("grid_20mm_mv", 4096.0f * 2 / 3, 3.0f / 2, 20.0f),
    grid_40mm_mv("grid_40mm_mv", 4096.0f * 4 / 3, 3.0f / 4, 40.0f),
    ;
    private String name;
    private float value;
    private float maxDrawmV;
    private float displayGain;

    GridGainType(String name, float value, float maxDrawmV, float displayGain) {
        this.name = name;
        this.value = value;
        this.maxDrawmV = maxDrawmV;
        this.displayGain = displayGain;
    }

    public float getDisplayGain() {
        return displayGain;
    }

    public float getMaxDrawmV() {
        return maxDrawmV;
    }

    public String getName() {
        return name;
    }

    public float getValue() {
        return value;
    }
}
