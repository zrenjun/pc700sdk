package com.Carewell.view.ecg12;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * @author wxd
 */
public class DrawEcgRealView extends SurfaceView implements SurfaceHolder.Callback{

    private Context drawContext;
    private SurfaceHolder drawHolder;

    private int drawWidth;
    private int drawHeight;
    private DrawWaveThread drawWaveThread;
    private BaseEcgPreviewTemplate baseEcgPreviewTemplate;


    public DrawEcgRealView(Context context) {
        super(context);
        drawContext = context;
        drawHolder = getHolder();
        drawHolder.addCallback(this);
    }

    public DrawEcgRealView(Context context, AttributeSet attrs) {
        super(context, attrs);
        drawContext = context;
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
    public void surfaceCreated(SurfaceHolder holder) {
        drawWidth = getWidth();
        drawHeight = getHeight();

        resetInitParams();
        startDrawWaveThread();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        boolean screenDirectionChange = drawWidth != width;
        drawWidth = width;
        drawHeight = height;

        //屏幕方向改变 重绘
        if (screenDirectionChange) {
            resetDrawEcg();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopDrawEcg();
    }

    /**
     * 重新排布局
     */
    public void resetInitParams() {
//        int smallGridSpace = (drawHeight / (EcgConfig.BIG_GRID_COUNT * EcgConfig.SMALL_GRID_COUNT));
        float smallGridSpace = EcgConfig.SMALL_GRID_SPACE_FLOAT;
        baseEcgPreviewTemplate = MainEcgManager.getBaseEcgPreviewTemplate(getContext(), PreviewPageEnum.PAGE_REAL_DRAW,smallGridSpace,drawWidth,drawHeight,
                MainEcgManager.getInstance().getLeadSpeedType(),MainEcgManager.getInstance().getGainArray(),true,MainEcgManager.getInstance().getRecordOrderType());
        baseEcgPreviewTemplate.initParams();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                drawBg();
            }
        }, 50);
    }

    /**
     * 先画个背景，效果好点
     */
    private void drawBg() {
        Canvas canvas = null;
        try {
            canvas = drawHolder.lockCanvas();
            if (canvas != null) {
                canvas.drawBitmap(baseEcgPreviewTemplate.getBgBitmap(), 0, 0, null);
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
     * 线程中实时画波形
     */
    private void drawWave() {
        Canvas canvas = null;
        try {
            LeadManager leadManager = baseEcgPreviewTemplate.getLeadManager();
            if(leadManager  != null){
                canvas = drawHolder.lockCanvas();
                if (canvas != null) {
                    canvas.drawBitmap(baseEcgPreviewTemplate.getBgBitmap(), 0, 0, null);
                    baseEcgPreviewTemplate.drawEcgPathRealTime(canvas);
                    //new add 自动增益，需要动态更新
                    //baseEcgPreviewTemplate.drawLeadInfo();
                }
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
     * 重绘波形
     */
    public void resetDrawEcg() {
        stopDrawEcg();
        resetInitParams();
        startDrawWaveThread();
    }

    /**
     * 停止绘制波形
     */
    public void stopDrawEcg() {
        stopDrawWaveThread();
        clearData();
        clearScreen();
    }

    /**
     * 清屏
     */
    private void clearScreen() {
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
     * 清理数据
     */
    public void clearData() {
        if(baseEcgPreviewTemplate != null){
            baseEcgPreviewTemplate.clearData();
        }
    }

    /**
     * 启动实时绘制线程
     */
    public void startDrawWaveThread() {
        if (drawWaveThread == null) {
            drawWaveThread = new DrawWaveThread();
            drawWaveThread.isRunning = true;
            drawWaveThread.start();
        }
    }

    /**
     * 停止绘制线程
     */
    public void stopDrawWaveThread() {
        if (drawWaveThread != null) {
            drawWaveThread.isRunning = false;
            drawWaveThread = null;
        }
    }

    //==================
    class DrawWaveThread extends Thread {
        public boolean isRunning() {
            return isRunning;
        }

        public void setRunning(boolean running) {
            isRunning = running;
        }

        private boolean isRunning = false;

        public DrawWaveThread() {

        }

        @Override
        public void run() {
            while (isRunning) {
                drawWave();

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

    public void setBaseEcgPreviewTemplate(BaseEcgPreviewTemplate baseEcgPreviewTemplate) {
        this.baseEcgPreviewTemplate = baseEcgPreviewTemplate;
    }

    public DrawWaveThread getDrawWaveThread() {
        return drawWaveThread;
    }

    public void setDrawWaveThread(DrawWaveThread drawWaveThread) {
        this.drawWaveThread = drawWaveThread;
    }
}
