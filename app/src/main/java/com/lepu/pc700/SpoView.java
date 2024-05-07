package com.lepu.pc700;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * @author fangrf 2020/09/17
 */
public class SpoView extends View {
    //设置波形平滑
    protected CornerPathEffect cornerPathEffect = new CornerPathEffect(20);
    //两点之间的步长 由它控制波形走速(血氧spo2用到)
    protected float step = 1f;
    //波形画笔
    public Paint wavePaint;
    protected Paint scanPaint;
    private Bitmap mDrawBitmap;
    protected Canvas mCanvas;
    //波形增益
    protected int gain = 1;
    private int viewWidth;
    private int viewHeight;

    public SpoView(Context context) {
        super(context);
        init(context);
    }

    public SpoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SpoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected void init(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        wavePaint = new Paint();
        wavePaint.setAntiAlias(true);
        wavePaint.setStyle(Style.STROKE);
        wavePaint.setColor(Color.RED);
        wavePaint.setStrokeWidth(displayMetrics.density * 2);
        wavePaint.setPathEffect(cornerPathEffect);
        scanPaint = new Paint();
        scanPaint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));//或 Mode.CLEAR
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewHeight = h;
        viewWidth = w;
        mDrawBitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        mCanvas = new Canvas(mDrawBitmap);
        //换算比例
        zoom = (float) viewHeight / (nMax - nMin);
        mViewH = zoom * nMax;

        //初始化存储波形的队列
        loopQueueSize = (int) (w / step);
        loopQueue = new int[loopQueueSize];
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mDrawBitmap, 0, 0, wavePaint);
//        System.out.println("============onDraw");
    }

    private final PointF p = new PointF();

    //根据取值范围计算出的0到最大值对应的View高度
    private float mViewH;

    public float getY(int data) {
        return mViewH - zoom * data * gain;
    }

    //血氧波形最大值
    private int nMax = 100;
    //血氧波形最小值
    private int nMin;
    //波形缩放比例
    private float zoom = 1;

    /**
     * 设置血氧波形的取值范围
     *
     * @param max 最大值
     * @param min 最小值
     */
    public void setScope(int max, int min) {
        nMax = max;
        nMin = min;
    }

    public int getGain() {
        return gain;
    }

    //设置增益，在开始绘制之前必须设置
    public void setGain(int gain) {
        this.gain = gain;
    }

    //--------------------- 缓存处理 -------------------------------------
    private int[] loopQueue;
    private int loopQueueSize;
    private int front;// 队列头, 取队列数据的索引
    private int tail;// 队列尾
    private Thread drawTh;

    public void Start() {
        tail = 0;
        front = 0;
        mStartX = 0;
        isDraw = true;
        if (drawTh == null) {
            drawTh = new Thread(new DrawRunnable());
            drawTh.start();
        }
        Thread.State state = drawTh.getState();
        if (state == Thread.State.TERMINATED) {
            drawTh = new Thread(new DrawRunnable());
            drawTh.start();
        }
    }

    public void Stop() {
        isDraw = false;
    }

    public boolean getStartOrStop() {
        return this.isDraw;
    }

    public void setStartOrStop(boolean startorstop) {
        this.isDraw = startorstop;
    }

    public boolean isDraw;

    class DrawRunnable implements Runnable {
        @Override
        public void run() {
            while (isDraw) {
//                System.out.println("============run===" + isDraw);
                if (tail != front) {
                    int size = (tail > front) ? (tail - front) : (loopQueueSize - front);
                    if (loopQueue != null && mCanvas != null && size > 0) {
                        for (int i = 0; i < size; i++) {
                            float startX = mStartX + i * step;
                            int startY = loopQueue[(front + i) % loopQueueSize];
                            float targetX = mStartX + (i + 1) * step;
                            int targetY = loopQueue[(front + i + 1) % loopQueueSize];
                            if (startY != 0) {
                                mCanvas.drawRect(startX, 0, startX + 20, viewHeight, scanPaint);
                                mCanvas.drawLine(startX, getY(startY), targetX, getY(targetY), wavePaint);
                            }
                            postInvalidate();//子线程刷新
                            try {//波形平滑处理
                                if (size > 20) {
                                    Thread.sleep(10);
                                } else {
                                    Thread.sleep(20);//1s个50点
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        front = (front + size) % loopQueueSize;
                        mStartX = front * step;
                    }
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private float mStartX;

    public void addQueueData(int data) {
        if (isDraw) {
            tail++;
            tail %= loopQueueSize;
            loopQueue[tail] = data;
        }
    }

    public void cleanAndRestart() {
        p.x = 0;
        p.y = 0;
        if (mCanvas != null) {
            mCanvas.drawColor(Color.WHITE, Mode.CLEAR);
        }
        tail = 0;
        front = 0;
        mStartX = 0;
        loopQueueSize = (int) (viewWidth / step);
        loopQueue = new int[loopQueueSize];
    }
}
