package com.Carewell.view.other;

import static android.R.attr.baseline;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.creative.sdkpack.R;

/**
 **绘制血压扇形柱状图
 * Created by li jie on 17-8-22.
 */
public class InstrumentView extends View {
    private final int color_outcircle = 0XDEDEDE;
    private int color_progress;

    /**
     * 血压等级颜色定义
     */
    private final int[] nibpGradeColor = {
            getResources().getColor(R.color.color_main_histogram1),
            getResources().getColor(R.color.color_main_histogram2),
            getResources().getColor(R.color.color_main_histogram3),
            getResources().getColor(R.color.color_main_histogram4),
            getResources().getColor(R.color.color_main_histogram5),
            getResources().getColor(R.color.color_main_histogram6)
    };

    /**
     * *  血压等级文字定义
     * */


    private final String[] nibpGradeRange = {
            getResources().getString(R.string.best),
            getResources().getString(R.string.normal),
            getResources().getString(R.string.Inthehigh),
            getResources().getString(R.string.light),
            getResources().getString(R.string.crowning),
            getResources().getString(R.string.Heavy_high)
    };
    /**
     * 是否是测量结果
     */
    private boolean isResult = false;
    /**     * 当前血压     */
    private int nibp = 0;
    /**     * 血压最大值     */
    private int maxNibp = 250;
    /**     * 外环线的宽度     */
    private final int outCircleWidth = 1;
    /**     * 外环的半径     */
    private int outCircleRadius = 0;
    /**     * 内环的半径     */
    private int inCircleRedius = 0;
    /**     * 内环的宽度     */
    private int inCircleWidth = 0;
    /**     * 内容中心的坐标     */
    private final int[] centerPoint = new int[2];
    /**     * 刻度线的数量     */
    private int dialCount = 0;
    /**     * 长线的长度     */
    private int dialLongLength = 0;
    /**     * 短线的长度     */
    private int dialShortLength = 0;
    /**     * 刻度线距离圆心最远的距离     */
    private int dialRadius = 0;
    /**     * 圆弧开始的角度     */
    private int startAngle = 0;
    /**     * 圆弧划过的角度    */
    private int allAngle = 0;
    private Paint mPaint;

    public InstrumentView(Context context) {
        this(context, null);
    }
    public InstrumentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public InstrumentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
    }    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        initValues();
    }
    /**
     * * 初始化尺寸
     * */
    private void initValues() {
        /*     * view的实际宽度     */
        int viewWidth = getMeasuredWidth();
        /*     * view的实际高度     */
        int viewHeight = getMeasuredHeight();
        /*     * 要画的内容的实际宽度  */
        int contentWidth = Math.min(viewWidth, viewHeight);
        outCircleRadius = contentWidth / 2 - outCircleWidth;
        /*     * 内环与外环的距离     */
        int outAndInDistance = (int) (contentWidth / 26.5);
        inCircleWidth = (int) (contentWidth / 18.7);
        centerPoint[0] = viewWidth / 2;
        centerPoint[1] = viewHeight / 2;
        inCircleRedius = outCircleRadius - outAndInDistance - inCircleWidth / 2;
        startAngle = 150;
        allAngle = 240;
        /*     * 刻度盘距离它外面的圆的距离     */
        int dialOutCircleDistance = inCircleWidth;
        dialCount = 50;
        /*     * 每隔几次出现一个长线     */
        dialLongLength = (int) (dialOutCircleDistance / 1.2);
        dialShortLength = (int) (dialLongLength / 1.8);
        dialRadius = inCircleRedius - dialOutCircleDistance;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawStatic(canvas);
        drawDynamic(canvas);
    }
    /**
     * * 绘制静态的部分
     * *
     * * @param canvas
     * */
    private void drawStatic(Canvas canvas) {
        drawOutCircle(canvas);
        drawCircleWithRound(startAngle, allAngle, inCircleWidth, inCircleRedius, color_outcircle, canvas);
        drawDial(startAngle, allAngle, dialCount, dialLongLength, dialShortLength, dialRadius, canvas);
        drawBackGround(canvas);
        /*
         * 刻度盘上数字的数量
         */
        int figureCount = 6;
        drawFigure(canvas, figureCount);
    }
    private void drawFigure(Canvas canvas, int count) {//血压等级文字
        String range;
        int angle;
        for (int i = 0; i < count; i++) {
            range = nibpGradeRange[i];
            angle = (int) ((allAngle) / ((count-1) * 1f) * i) + startAngle;
            int[] pointFromAngleAndRadius = getPointFromAngleAndRadius(angle, dialRadius - dialLongLength * 2 );
            mPaint.setTextSize(25);
            mPaint.setColor(nibpGradeColor[i]);
            mPaint.setTextAlign(Paint.Align.CENTER);
            canvas.save();
            canvas.rotate(angle+90,pointFromAngleAndRadius[0],pointFromAngleAndRadius[1]);
            canvas.drawText(range,pointFromAngleAndRadius[0],pointFromAngleAndRadius[1],mPaint);
            canvas.restore();
        }
    }
    /**
     * * 画内层背景
     * *
     * * @param canvas
     * */
    private void drawBackGround(Canvas canvas) {
        String color_bg_outcircle = "#2690F8";
        mPaint.setColor(Color.parseColor(color_bg_outcircle));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(outCircleRadius / 3f / 2f);
        canvas.drawCircle(centerPoint[0], centerPoint[1], outCircleRadius / 3f, mPaint);
        String color_bg_incircle = "#58ADE4";
        mPaint.setColor(Color.parseColor(color_bg_incircle));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerPoint[0], centerPoint[1], (outCircleRadius / 3f / 2f), mPaint);
    }
    /**
     * * 画刻度盘*
     * * @param startAngle  开始画的角度
     * * @param allAngle    总共划过的角度
     * * @param dialCount   总共的线的数量
     * * @param per         每隔几个出现一次长线
     * * @param longLength  长仙女的长度
     * * @param shortLength 短线的长度
     * * @param radius      距离圆心最远的地方的半径
     * */
    private void drawDial(int startAngle, int allAngle, int dialCount, int longLength, int shortLength, int radius, Canvas canvas) {
        int length;
        int angle;
        for (int i = 0; i <= dialCount; i++) {
            angle = (int) ((allAngle) / (dialCount * 1f) * i) + startAngle;
            if (i % 5 == 0) {
                length = longLength;
            } else {
                length = shortLength;
            }
            drawSingleDial(angle, length, radius, canvas);
        }
    }
    /**
     * * 画刻度中的一条线
     * *
     * * @param angle  所处的角度
     * * @param length 线的长度
     * * @param radius 距离圆心最远的地方的半径
     * */
    private void drawSingleDial(int angle, int length, int radius, Canvas canvas) {
        int[] startP = getPointFromAngleAndRadius(angle, radius);
        int[] endP = getPointFromAngleAndRadius(angle, radius - length);
        canvas.drawLine(startP[0], startP[1], endP[0], endP[1], mPaint);
    }
    /**
     * * 画最外层的圆
     * *     * @param canvas
     * */
    private void drawOutCircle(Canvas canvas) {
        mPaint.setStrokeWidth(outCircleWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(color_outcircle);
        canvas.drawCircle(centerPoint[0], centerPoint[1], outCircleRadius, mPaint);
    }
    /**
     *  绘制动态的部分
     *  *
     *  * @param canvas
     *  */
    private void drawDynamic(Canvas canvas) {
        if (!isResult) {//测量过程（动态值）
            maxNibp = 250;
            drawProgress(nibp, canvas);
            drawIndicator(canvas);
        }else {//测量结果（静态值）
            maxNibp = 5;
            drawIndicator(canvas);
            drawCurrentProgressTv(nibp, canvas);
        }
    }
    /**
     * 绘制当前进度的文字
     * *
     * @param nibp
     * * @param canvas
     * */
    private void drawCurrentProgressTv(int nibp, Canvas canvas) {
        canvas.drawText(getResources().getString(R.string.Blood_pressure_level)+nibpGradeRange[nibp],centerPoint[0],baseline,mPaint);
        mPaint.setTextSize(25);
        mPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float baseLine1 = centerPoint[1] + (outCircleRadius / 20f * 11 - fontMetrics.top - fontMetrics.bottom);
        canvas.drawText(getResources().getString(R.string.Blood_pressure_level), centerPoint[0], baseLine1, mPaint);
        float baseLine2 = outCircleRadius / 20f * 11 - 3 * (fontMetrics.bottom + fontMetrics.top) + centerPoint[1];
        canvas.drawText(nibpGradeRange[nibp], centerPoint[0], baseLine2, mPaint);
    }
    /**
     * *画指针以及他的背景
     */
    private void drawIndicator(Canvas canvas) {
        drawPointer(canvas);
        drawIndicatorBg(canvas);
    }
    /**
     * 指针的最远处的半径和刻度线的一样
     */
    private void drawPointer(Canvas canvas) {
        int angle = (int) ((allAngle) / (maxNibp * 1f) * (nibp)) + startAngle;
        // 指针的定点坐标
        int[] peakPoint = getPointFromAngleAndRadius(angle, dialRadius);
        Path path = new Path();
        //右侧指针
        Paint mPaint1 = new Paint();
        String color_indicator_right = "#D88635";
        mPaint1.setColor(Color.parseColor(color_indicator_right));
        mPaint1.setStyle(Paint.Style.STROKE);
        mPaint1.setStrokeWidth(5);
        path.reset();
        path.moveTo(centerPoint[0], centerPoint[1]);
        path.lineTo(peakPoint[0], peakPoint[1]);
        canvas.drawPath(path, mPaint1);
    }
    private void drawIndicatorBg(Canvas canvas) {
        String color_smart_circle = "#C2B9B0";
        mPaint.setColor(Color.parseColor(color_smart_circle));
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerPoint[0], centerPoint[1], (outCircleRadius / 3f / 2 / 4), mPaint);
    }
    /**
     * 根据进度画进度条
     * *
     * @param nibp 最大进度为100.最小为0
     */
    private void drawProgress(int nibp, Canvas canvas) {
        float ratio = (float) (nibp) / maxNibp;
        int angle = (int) (allAngle * ratio);
        if (0<ratio && ratio<=0.2){
            color_progress = nibpGradeColor[0];
        }else if (0.2<ratio && ratio<=0.4){
            color_progress = nibpGradeColor[1];
        }else if (0.4<ratio && ratio<=0.6){
            color_progress = nibpGradeColor[2];
        }else if (0.6<ratio && ratio<=0.8){
            color_progress = nibpGradeColor[3];
        }else if (0.8<ratio && ratio<=0.9){
            color_progress = nibpGradeColor[4];
        }else if (0.9<ratio && ratio<=1.0){
            color_progress = nibpGradeColor[5];
        }
        drawCircleWithRound(startAngle, angle, inCircleWidth, inCircleRedius, color_progress, canvas);
    }

    //设置当前血压
    public void setProgress(int nibp,boolean isResult) {
        if (nibp>250){
            nibp = 250;
        }
        this.nibp = nibp;
        this.isResult = isResult;
        postInvalidate();
    }
    /**
     * 画一个两端为圆弧的圆形曲
     * *
     * @param startAngle 曲线开始的角度
     * @param allAngle   曲线走过的角度
     * @param radius     曲线的半径
     * @param width      曲线的厚度
     */
    private void drawCircleWithRound(int startAngle, int allAngle, int width, int radius, int color, Canvas canvas) {
        mPaint.setStrokeWidth(width);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(color);
        RectF rectF = new RectF(centerPoint[0] - radius, centerPoint[1] - radius, centerPoint[0] + radius, centerPoint[1] + radius);
        canvas.drawArc(rectF, startAngle, allAngle, false, mPaint);
        drawArcRoune(radius, startAngle, width, canvas);
        drawArcRoune(radius, startAngle + allAngle, width, canvas);
    }
    /**
     * 绘制圆弧两端的圆
     * *
     * @param radius 圆弧的半径
     * @param angle  所处于圆弧的多少度的位置
     * @param width  圆弧的宽度
     */
    private void drawArcRoune(int radius, int angle, int width, Canvas canvas) {
        int[] point = getPointFromAngleAndRadius(angle, radius);
        mPaint.setStrokeWidth(0);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(point[0], point[1], width / 2f, mPaint);
    }
    /**
     * 根据角度和半径，求一个点的坐标
     * *
     */
    private int[] getPointFromAngleAndRadius(int angle, int radius) {
        double x = radius * Math.cos(angle * Math.PI / 180) + centerPoint[0];
        double y = radius * Math.sin(angle * Math.PI / 180) + centerPoint[1];
        return new int[]{(int) x, (int) y};
    }
}
