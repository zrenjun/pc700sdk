package com.Carewell.view.ecg12;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.annotation.NonNull;


/**
 * @author wxd
 */
public class DrawEcgRealView extends SurfaceView implements SurfaceHolder.Callback {

    private final SurfaceHolder drawHolder;

    private int drawWidth;
    private int drawHeight;
    private DrawWaveThread drawWaveThread;
    private BaseEcgPreviewTemplate baseEcgPreviewTemplate;


    public DrawEcgRealView(Context context) {
        super(context);
        drawHolder = getHolder();
        drawHolder.addCallback(this);
    }

    public DrawEcgRealView(Context context, AttributeSet attrs) {
        super(context, attrs);
        drawHolder = getHolder();
        drawHolder.addCallback(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public boolean isAttachedToWindow() {
        return super.isAttachedToWindow();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        drawWidth = getWidth();
        drawHeight = getHeight();
        resetInitParams();
        startDrawWaveThread();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        stopDrawEcg();
    }

    /**
     * 重新排布局
     */
    private void resetInitParams() {
        baseEcgPreviewTemplate = MainEcgManager.getBaseEcgPreviewTemplate(getContext(), PreviewPageEnum.PAGE_REAL_DRAW, EcgConfig.SMALL_GRID_SPACE_FLOAT, drawWidth, drawHeight,
                MainEcgManager.getInstance().getLeadSpeedType(), MainEcgManager.getInstance().getGainArray(), true, MainEcgManager.getInstance().getRecordOrderType());
        baseEcgPreviewTemplate.initParams();
    }

    /**
     * 重绘波形
     */
    public void resetDrawEcg() {
        resetInitParams();
    }

    /**
     * 停止绘制波形
     */
    private void stopDrawEcg() {
        stopDrawWaveThread();
        if (baseEcgPreviewTemplate != null) {
            baseEcgPreviewTemplate.clearData();
        }
        Canvas canvas = null;
        try {
            canvas = drawHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawColor(EcgConfig.screenBgColor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                drawHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * 启动实时绘制线程
     */
    private void startDrawWaveThread() {
        if (drawWaveThread == null) {
            drawWaveThread = new DrawWaveThread();
            drawWaveThread.isRunning = true;
            drawWaveThread.start();
        }
    }

    /**
     * 停止绘制线程
     */
    private void stopDrawWaveThread() {
        if (drawWaveThread != null) {
            drawWaveThread.isRunning = false;
            drawWaveThread = null;
        }
    }

    class DrawWaveThread extends Thread {

        private boolean isRunning = false;

        public DrawWaveThread() {
        }

        @Override
        public void run() {
            while (isRunning) {
                Canvas canvas = null;
                try {
                    canvas = drawHolder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawBitmap(baseEcgPreviewTemplate.getBgBitmap(), 0, 0, null);
                        baseEcgPreviewTemplate.drawEcgPathRealTime(canvas);
                        //new add 自动增益，需要动态更新
                        //baseEcgPreviewTemplate.drawLeadInfo();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        drawHolder.unlockCanvasAndPost(canvas);
                    }
                }
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public BaseEcgPreviewTemplate getBaseEcgPreviewTemplate() {
        return baseEcgPreviewTemplate;
    }

}
