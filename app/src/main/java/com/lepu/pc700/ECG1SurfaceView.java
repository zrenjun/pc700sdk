package com.lepu.pc700;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.lepu.pc700.utils.Fifo;


/**
 * Created by fangrf on 2019/1/3.
 */
public class ECG1SurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    protected SurfaceHolder mHolder;
    protected Paint mPaint, mCalPaint;
    protected Canvas mCanvas;
    public int mSurfaceWidth;
    public int mSurfaceHeight;
    private float ecgYOffset; // 一行对应的像素点
    private final int ecgYOffsetCount = 10; //一行高有多少个大格子
    public int mBackColor = Color.WHITE;//画布背景色
    protected int mGridColor = Color.RED;
    public int mWaveColor = 0xff2E8B57;
    public int divisionLineColor = 0xffCACACA;
    protected int mPosition = 0;
    protected int mBufSize = 0;  //队列缓存
    protected Fifo<Float> mWaveFifo; //波形队列
    protected Bitmap mBackBitmap = null;
    public float mXScale = 0.982f; // 走速 25mm/s   硬件 0.9824< <0.9826 , 30s ->0.884f;
    private boolean bViewed = false; //surface 是否初始化完毕
    /**
     * 屏幕每1mm占有的像素点, 单位：像素点/mm
     */
    private final float resx = 5.88f;//1mm对应的像素点
    public float mGridSize = 0f; //5mm ，一个大格子 对应的像素点
    private final Rect freshRect = new Rect(); //刷新的绘制矩形区域
    private int ecgYrow;//心电换行
    public boolean isShowCal = true;//是否显示定标

    public ECG1SurfaceView(Context context) {
        super(context);
        init();
    }

    public ECG1SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ECG1SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        bViewed = false;
        mHolder = this.getHolder();
        mHolder.addCallback(this);

        mPaint = new Paint();
        mPaint.setStrokeWidth(1);
        mPaint.setAntiAlias(true); //抗锯齿
        //定标
        mCalPaint = new Paint();
        mCalPaint.setStrokeWidth(1);
        mCalPaint.setAntiAlias(true); //抗锯齿
        mCalPaint.setColor(Color.BLUE);

        mGridSize = resx * 5; //5 mm 像素点
        ecgYOffset = ecgYOffsetCount * mGridSize;

        //        refreshCurrentHardwareGain();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceWidth = getWidth();
        mSurfaceHeight = getHeight();
        //初始化存储波形的队列
        mBufSize = (int) ((mSurfaceWidth + 1) / mXScale); //1202
        mWaveFifo = new Fifo<>(Float.class, mBufSize);

        //把网格背景画到透明图片的画布上,还未显示
        mBackBitmap = Bitmap.createBitmap(mSurfaceWidth, mSurfaceHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBackBitmap);//用图片做画布
        mCanvas.drawColor(mBackColor); //设置图片背景色

        drawGrid(mSurfaceWidth, mSurfaceHeight);

        //把画好背景的图片显示
        screenClear();
        bViewed = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        startDraw();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        bViewed = false;
        stopDraw();
        mWaveFifo.clear();
        mPosition = 0;
    }

    private float mXStart = 0.0f;

    public void autoDraw() {
        if (!bViewed) {
            return;
        }
        while (mWaveFifo.offset() != mPosition) {
            int dlength = (((mWaveFifo.offset() > mPosition) ? (mWaveFifo.offset() - mPosition) : (mBufSize - mPosition)));
            if (beginPaint(mXStart - 2 * mXScale, (dlength * mXScale + 10), (int) (ecgYrow * ecgYOffset), (int) ((ecgYrow + 1) * ecgYOffset)) != 0)
                return;
            mPaint.setColor(mWaveColor);
            mPaint.setStrokeWidth(1);
            if (mPosition > 1) {//防止边界出现断点,往前多画一点
                drawWaveLine(
                        (int) (mXStart - 2 * mXScale),
                        mWaveFifo.getAbs((mPosition - 2) % mBufSize),
                        (int) (mXStart - 1 * mXScale),
                        mWaveFifo.getAbs(mPosition - 1));
            }
            for (int i = 0; i < dlength; i++) {
                if ((mPosition == 0) && (i == 0))
                    continue;
                drawWaveLine(
                        (int) (mXStart + (i - 1) * mXScale),
                        mWaveFifo.getAbs((mPosition + i - 1) % mBufSize),
                        (int) (mXStart + i * mXScale),
                        mWaveFifo.getAbs(mPosition + i));
            }
            endPaint();
            if (mPosition + dlength >= mBufSize) {
                ecgYrow++;
                ecgYrow = ecgYrow % 2;
            }

            mPosition = (mPosition + dlength) % mBufSize;
            mXStart = mPosition * mXScale;
        }
    }

    protected void drawWaveLine(float xs, Object ys, float xd, Object yd) {
        if (ys == null || yd == null || mCanvas == null)
            return;
        float mYs = (Float) ys;
        float mYd = (Float) yd;
        mPaint.setStrokeWidth(2);
        mCanvas.drawLine(xs, mYs + ecgYrow * ecgYOffset, xd, mYd + ecgYrow * ecgYOffset, mPaint);
    }

    //画背景图片
    protected int beginPaint(float offset, float dlength, int top, int bottom) {
        freshRect.set((int) offset, top, (int) (offset + dlength + 20), bottom + 1);
        mCanvas = mHolder.lockCanvas(freshRect);
        if ((mCanvas == null) || (mHolder == null)) {
            return -1;
        }
        mPaint.setColor(mBackColor);
        mPaint.setStrokeWidth(1);
        mCanvas.drawRect(freshRect, mPaint);
        mCanvas.drawBitmap(mBackBitmap, freshRect, freshRect, mPaint);
        return 0;
    }

    protected void endPaint() {
        if ((mHolder != null) && (mCanvas != null))
            mHolder.unlockCanvasAndPost(mCanvas);
    }

    private float mCalHeight = 0f;//第一行定标中心高度
    public float mCalScale = 4f; //定标网格，默认4网格(5mm *4)

    protected void drawGrid(int right, int bottom) {
        mPaint.setStrokeWidth(0.5f);
        for (float dh = 0, index = 0; dh <= bottom; dh += resx, index++) {//画横线
            if (index % 5 == 0 && index > 0) {//画每一行的分割线,1行
                mPaint.setColor(mGridColor);
                mPaint.setPathEffect(new DashPathEffect(new float[]{0.5f, 0}, 0));
            } else {
                mPaint.setColor(divisionLineColor);
                mPaint.setPathEffect(new DashPathEffect(new float[]{1f, 0}, 0));
            }
            //第一个定标中间高度 top Y
            if (index == 5 * 5) {
                mCalHeight = dh;
            }
            if (index == 5 * ecgYOffsetCount) {
                mPaint.setColor(mWaveColor);
            }
            mCanvas.drawLine(0, dh, right, dh, mPaint);
        }
        mPaint.setColor(divisionLineColor);
        for (float dw = 0, index = 0; dw <= right; dw += resx, index++) {//画竖线  6.32*5 = 31.6
            if (index % 5 == 0 && index > 0) {//画每一行的分割线,1行
                mPaint.setColor(mGridColor);
                mPaint.setPathEffect(new DashPathEffect(new float[]{0.5f, 0}, 0));
            } else {
                mPaint.setColor(divisionLineColor);
                mPaint.setPathEffect(new DashPathEffect(new float[]{1f, 0}, 0));
            }
            mCanvas.drawLine(dw, 0, dw, bottom, mPaint);
        }

        //画定标
        if (isShowCal) {
            for (int k = 1; k < 3; k++) {//mCalHeight*2 -->2个定标的间距
                //中间竖线
                mCanvas.drawLine(mGridSize,
                        (k - 1) * mCalHeight * 2 + mCalHeight - mGridSize / 2 * mCalScale,
                        mGridSize,
                        (k - 1) * mCalHeight * 2 + mCalHeight + mGridSize / 2 * mCalScale, mCalPaint);
                //上边横线
                mCanvas.drawLine(mGridSize - 10,
                        (k - 1) * mCalHeight * 2 + mCalHeight - mGridSize / 2 * mCalScale,
                        mGridSize + 10,
                        (k - 1) * mCalHeight * 2 + mCalHeight - mGridSize / 2 * mCalScale, mCalPaint);
                //下边横线
                mCanvas.drawLine(mGridSize - 10,
                        (k - 1) * mCalHeight * 2 + mCalHeight + mGridSize / 2 * mCalScale,
                        mGridSize + 10,
                        (k - 1) * mCalHeight * 2 + mCalHeight + mGridSize / 2 * mCalScale, mCalPaint);
            }
        }
    }

    /**
     * 清屏,重画背景
     */
    public void screenClear() {
        beginPaint(0, mSurfaceWidth, 0, mSurfaceHeight);
        Rect srcRect = new Rect(0, 0, mBackBitmap.getWidth(), mBackBitmap.getHeight());
        mCanvas.drawBitmap(mBackBitmap, srcRect, srcRect, mPaint);
        endPaint();
        ecgYrow = 0;
        mPosition = 0;
        mXStart = 0;
        mWaveFifo.clear();
    }

    //刷新定标
    public void refreshCal() {
        //把网格背景画到透明图片的画布上,还未显示
        mBackBitmap = Bitmap.createBitmap(mSurfaceWidth, mSurfaceHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBackBitmap);//用图片做画布
        mCanvas.drawColor(mBackColor); //设置图片背景色
        drawGrid(mSurfaceWidth, mSurfaceHeight);
        //把画好背景的图片显示
        screenClear();
    }

    /**
     * 开始绘图
     */
    public void startDraw() {
        if (mDrawTh != null) {
            bStopDraw = true;
            mDrawTh = null;
        }
        mDrawTh = new DrawThread("DrawECG12Wave");
        mDrawTh.start();
    }

    /**
     * 停止绘图
     */
    public void stopDraw() {
        if (mDrawTh != null) {
            bStopDraw = true;
            mDrawTh = null;
        }
    }

    int sleepTime = 25; //设置帧率
    boolean bStopDraw = false;
    DrawThread mDrawTh;

    class DrawThread extends Thread {
        public DrawThread(String threadName) {
            bStopDraw = false;
            setName(threadName);
        }

        @Override
        public void run() {
            while (!bStopDraw) {
                long startTime = System.currentTimeMillis();
                autoDraw();
                long endTime = System.currentTimeMillis();
                if (endTime - startTime < sleepTime) {
                    try {
                        Thread.sleep(sleepTime - (endTime - startTime));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void setSpeed(float speed) {
        bViewed = false;
        mPosition = 0;
        mWaveFifo.clear();
        mXScale = speed;
        mBufSize = (int) ((mSurfaceWidth + 1) / mXScale);
        mWaveFifo = new Fifo<>(Float.class, mBufSize);
        screenClear();
        bViewed = true;
    }

    private float conversionFormula2(int ecgY) {
        float temp = ecgY *2/ 355f;  //转mV
        return ecgYOffset / 2 - temp * mGridSize * mCalScale / 2;
    }

    public void addWaveDate(int ecgData) {
        insert(conversionFormula2(ecgData));
    }

    public void insert(float data) {
        if (mWaveFifo == null)
            return;
        if (mWaveFifo.space() == 0) {
            mWaveFifo.pop();
        }
        mWaveFifo.push(data);
    }
}