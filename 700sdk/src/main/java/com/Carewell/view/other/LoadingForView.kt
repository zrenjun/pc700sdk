package com.Carewell.view.other

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.RotateDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources
import com.creative.sdkpack.R

class LoadingForView(
    context: Context,
    private val viewGroup: ViewGroup,
    showBg: Boolean = false,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 0f
) {
    private val loading = FrameLayout(context)

    init {
        val view = ProgressBar(context)
        view.indeterminateDrawable = getLoadingDrawable(context)
        val dp120 = dip2px(context,120f)
        view.layoutParams = FrameLayout.LayoutParams(dp120, dp120).run {
            gravity = Gravity.CENTER
            this
        }
        loading.addView(view)
        loading.setBackgroundColor(Color.argb((255 * alpha).toInt(), 0, 0, 0))
        loading.isEnabled = true
        loading.isClickable = true
        loading.isFocusable = true
        loading.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (showBg) {
            loading.background = AppCompatResources.getDrawable(context, R.drawable.dialog_white_bg)
        }
    }

    var isShow = false
        private set

    @MainThread
    fun show() {
        if (viewGroup.indexOfChild(loading) == -1) {
            viewGroup.addView(loading)
            isShow = true
        }
    }

    @MainThread
    fun dismiss() {
        viewGroup.removeView(loading)
        isShow = false
    }

    private fun getLoadingDrawable(context: Context): RotateDrawable {
        val drawable = RotateDrawable()
        drawable.fromDegrees = 0f
        drawable.toDegrees = 180f
        drawable.pivotX = 0.5f
        drawable.pivotY = 0.5f
        drawable.drawable = AppCompatResources.getDrawable(context, R.drawable.ic_loading)
        return drawable
    }

    private fun dip2px(context: Context, dip: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dip * scale + 0.5f).toInt()
    }
}

