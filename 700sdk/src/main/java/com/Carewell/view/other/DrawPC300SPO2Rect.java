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
import com.Carewell.ecg700.port.Wave;
import com.creative.sdkpack.R;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

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
     * 数据队列最大容量。原实现用无界 ArrayList.remove(0) 出队，是 O(n) 操作，
     * 一旦生产速度长期大于消费速度（队列堆积），remove(0) 的耗时会随队列长度线性增长，
     * 长时间运行会显著占用 CPU（曾在 ANR dump 中观察到该线程 CPU 占用异常高）。
     * 改为 LinkedBlockingDeque 实现 O(1) 出队且线程安全，同时限制最大长度避免无界堆积。
     */
    private static final int MAX_QUEUE_SIZE = 200;

    /**
     * 设置新数据
     */
    public final LinkedBlockingDeque<Wave> mSPORect = new LinkedBlockingDeque<>();

    public void setSPORect(List<Wave> waves) {
        for (Wave wave : waves) {
            if (mSPORect.size() >= MAX_QUEUE_SIZE) {
                mSPORect.pollFirst(); // 队列已满，丢弃最旧的数据，给新数据让位
            }
            mSPORect.offerLast(wave);
        }
    }

    private class MyThread extends Thread {
        @Override
        public void run() {
            super.run();
                while (!stop) {
                    try {
                        Wave data = mSPORect.pollFirst(); // O(1)，线程安全，取不到返回null
                        if (data != null) {
                            spo = data.getData();
                            postInvalidate();
                            Thread.sleep(mSPORect.size() > 25 ? 17 : 20);
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

    @Override
    protected void onDetachedFromWindow() {
        this.stop = true;
        if (mThread != null) {
            mThread.interrupt(); // 中断线程（避免 sleep 阻塞）
            mThread = null;      // 清除引用
        }
        mSPORect.clear(); // 清空残留数据，避免下次重新附着窗口时消费到过期波形
        super.onDetachedFromWindow();
    }
}
