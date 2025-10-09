package com.lepu.pc700;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;


public class Ecg12SeekBarView extends View {
    private float mSliderX;
    private float mSliderLen = 100;
    private RectF mSliderRect = new RectF();
    private Paint mSliderPaint;
    private float mSliderTop;
    private float mSliderBottom;
    private Bitmap mLeadBitmap;
    private Canvas mCanvas;
    private final Matrix mMatrix = new Matrix();

    public Ecg12SeekBarView(Context context) {
        super(context);
    }

    public Ecg12SeekBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mSliderPaint = new Paint();
        mSliderPaint.setColor(ContextCompat.getColor(context, R.color.color_preview_seek_thumb));
        mSliderPaint.setStyle(Paint.Style.FILL);
        mSliderPaint.setAntiAlias(true);
        Paint mWavePaint = new Paint();
        mWavePaint.setPathEffect(new CornerPathEffect(5));
        mWavePaint.setStyle(Paint.Style.STROKE);
        mWavePaint.setStrokeWidth(0.5f);
        mWavePaint.setAntiAlias(true);
        mWavePaint.setColor(Color.BLUE);
        mCanvas = new Canvas();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = measureDimension(200, widthMeasureSpec);
        int height = measureDimension(200, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    public int measureDimension(int defaultSize, int measureSpec) {
        int result;

        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = defaultSize;   //UNSPECIFIED
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        float mHeight = getHeight();
        mSliderTop = 0;
        mSliderBottom = mHeight;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mLeadBitmap != null) {
            mLeadBitmap.recycle();
            mLeadBitmap = null;
        }
        mLeadBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        mCanvas.setBitmap(mLeadBitmap);
        mMatrix.postTranslate(0, 0);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        canvas.drawRect(mSliderRect, mSliderPaint);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        super.onSaveInstanceState();
        Bundle bundle = new Bundle();
        bundle.putParcelable("sliderRect", mSliderRect);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        Bundle bundle = (Bundle) state;
        mSliderRect = bundle.getParcelable("sliderRect");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                ViewParent viewParent = getParent();
                if (viewParent != null) {
                    viewParent.requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    public void initSliderLen(float diaplayCount, float totalCount) {
        mSliderLen = getWidth() * diaplayCount / totalCount;
        mSliderX = getWidth() - mSliderLen;
        mSliderRect.set(mSliderX, mSliderTop, getWidth(), mSliderBottom);
        postInvalidate();
    }

    public void updateSlider(float ratio) {
        mSliderX = getWidth() * ratio;
        if (mSliderX == mSliderRect.left) {
            return;
        }
        mSliderX = Math.min(mSliderX, getWidth() - mSliderLen);
        mSliderRect.set(mSliderX, mSliderTop, mSliderX + mSliderLen, mSliderBottom);
        postInvalidate();
    }
}
