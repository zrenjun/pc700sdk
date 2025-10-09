package com.lepu.pc700;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;
import com.lepu.pc700.photoview.OnMoveListener;
import com.lepu.pc700.photoview.PhotoViewAttacher;


public class ECG12ReplayView extends AppCompatImageView {


    public ECG12ReplayView(Context context) {
        this(context, null);
    }

    public ECG12ReplayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private PhotoViewAttacher attacher;
    private ScaleType pendingScaleType;

    private void init() {
        attacher = new PhotoViewAttacher(this);
        //We always pose as a Matrix scale type, though we can change to another scale type
        //via the attacher
        super.setScaleType(ScaleType.MATRIX);
        //apply the previously applied scale type
        if (pendingScaleType != null) {
            setScaleType(pendingScaleType);
            pendingScaleType = null;
        }
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        boolean changed = super.setFrame(l, t, r, b);
        if (changed) {
            attacher.update();
        }
        return changed;
    }

    @Override
    public ScaleType getScaleType() {
        return attacher.getScaleType();
    }

    @Override
    public Matrix getImageMatrix() {
        return attacher.getImageMatrix();
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        attacher.setOnLongClickListener(l);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        attacher.setOnClickListener(l);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (attacher == null) {
            pendingScaleType = scaleType;
        } else {
            attacher.setScaleType(scaleType);
        }
    }

    public float getScale() {
        return attacher.getScale();
    }

    public void setOnOMoveListener(OnMoveListener listener) {
        attacher.setMoveListener(listener);
    }

    public void setScale(float scale) {
        attacher.setScale(scale);
    }

    public void refrshView(Bitmap bitmap) {
        setImageBitmap(bitmap);
        if (attacher != null) {
            attacher.update();
        }
    }
}
