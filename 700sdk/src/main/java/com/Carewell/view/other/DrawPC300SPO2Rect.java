package com.Carewell.view.other;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import com.Carewell.ecg700.Wave;
import com.creative.sdkpack.R;
import java.util.ArrayList;
import java.util.List;

/**
 * 血氧竖直柱状图
 */
public class DrawPC300SPO2Rect extends View {

    /**
     * 血氧柱状图
     */
    private RectF spoRect;
    private final Paint mPaint = new Paint();

    /**
     * 血氧数据缩放比例
     */
    private float scaleSPO = 0.0f;

    /**
     * 当前血氧值
     */
    private int spo = 0;
    private MyThread mThread;

    public DrawPC300SPO2Rect(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawPC300SPO2Rect(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public DrawPC300SPO2Rect(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        WindowManager wmManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wmManager.getDefaultDisplay().getMetrics(dm);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(dm.density * 3);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        spoRect = new RectF(0, 0, w, h);
        scaleSPO = spoRect.height() / 127f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mThread == null) {
            mThread = new MyThread();
            mThread.start();
        }
        drawSpo(canvas);
        Thread.State state = mThread.getState();
        if(state==Thread.State.TERMINATED){
            mThread = new MyThread();
            mThread.start();
        }
    }

    private void drawSpo(Canvas canvas) {
        mPaint.setColor(getResources().getColor(R.color.data_spo2));
        mPaint.setStyle(Style.STROKE);
        canvas.drawRect(spoRect, mPaint);
        mPaint.setColor(Color.rgb(0x03, 0x87, 0x06));
        mPaint.setStyle(Style.FILL);
        canvas.drawRect(spoRect.left + 5, getSPO(spo), spoRect.right - 5, spoRect.bottom - 5, mPaint);
    }

    /**
     * 计算 血氧数据的绘制高度
     */
    private float getSPO(int d) {
        return spoRect.bottom - 5 - scaleSPO * d;
    }

    /**
     * 设置新数据
     */
    public   List<Wave> mSPORect = new ArrayList<>();

    public void setSPORect(List<Wave> waves) {
        mSPORect.addAll(waves);
    }

    private class MyThread extends Thread {
        @Override
        public void run() {
            super.run();
                while (!stop) {
                    try {
                        if (!mSPORect.isEmpty()) {
                            Wave data = mSPORect.remove(0);
                            spo = data.getData();
                            postInvalidate();
                            if (mSPORect.size() > 25) {
                                Thread.sleep(17);
                            } else {
                                Thread.sleep(20);
                            }
                        } else {
                            Thread.sleep(100);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
    }

    private boolean stop = false;

    public   boolean getStartOrStop( ) {
        return this.stop ;
    }
    public void stop() {
        this.stop = true;
    }
    public void setStartOrStop(boolean startorstop ) {
        this.stop = startorstop;
    }
}
