package com.Carewell.view.ecg12;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Vector;

/**
 * @author wxd
 */
public class LeadManager {

    public class Lead {
        private int length;
        private RectF rect;
        private PointF origin;
        private Vector<Integer> buffer;
        private int orderSweep;
        private Path path;
        private Path pathSweep;
        private float addX;
        private float rate;
        private int addCnt = 0;
        private int filterSpeed;
        private int[] filterNum;
        private int filterSweepClear;
        private float curXStart;

        public Lead(RectF rect) {
            this.rect = rect;
            this.origin = new PointF(rect.left, rect.centerY());
            this.buffer = new Vector<>();
            this.path = new Path();
            this.pathSweep = new Path();
            //扫屏模式中清除点后path
            clear();
        }

        public void clear() {
            orderSweep = 0;
            buffer.clear();
            addCnt = 0;
            filterCnt = 0;
            twoCnt = 0;
            min = Integer.MAX_VALUE;
            max = Integer.MIN_VALUE;
            minCnt = 0;
            maxCnt = 0;
            cnt = 0;
            curXStart = 0;//wuxd add

            rate = 1.0f * speedPixels / EcgConfig.SPEED;
            if (rate < EcgConfig.RATE_MIN) {
                filterSpeed = (int) (EcgConfig.SPEED * EcgConfig.RATE_MIN);
            } else if (rate < 1) {
                filterSpeed = (int) (EcgConfig.SPEED * rate);
            } else {
                filterSpeed = EcgConfig.SPEED;
            }

            addX = 1.0f * speedPixels / filterSpeed;
            length = (int) (1.0f * (rect.right - rect.left) * filterSpeed / speedPixels + 0.5f);
            filterSweepClear = (int) (sweepClear * 1.0f * filterSpeed / EcgConfig.SPEED);//test
            if (rate < 1) {
                filterNum = new int[filterSpeed];
                for (int i = 0; i < filterSpeed; i++) {
                    filterNum[i] = i * EcgConfig.SPEED / filterSpeed;
                }
            }
        }

        private int filterCnt = 0;
        private int twoCnt = 0;
        private int min = Integer.MAX_VALUE;
        private int max = Integer.MIN_VALUE;
        private int minCnt = 0;
        private int maxCnt = 0;
        private int cnt = 0;

        /**
         *
         * @param val
         */
        public void addFilterPoint(int val,boolean chestLead) {
            float mvValue = val * Const.SHORT_EXTRA_VALUE * Const.SHORT_MV_GAIN;
            //增益
            if(chestLead){
                //胸导联v1-v6
                mvValue *= gainArray[1];
            }else{
                //肢体导联I - aVF
                mvValue *= gainArray[0];
            }

            float tmpValue = mvValue * 10 * mRangeRate;
            val = (int) (0 - tmpValue);

            if (rate < 1) {
                cnt++;
                if (val < min) {
                    min = val;
                    minCnt = cnt;
                }
                if (val > max) {
                    max = val;
                    maxCnt = cnt;
                }
                if (addCnt == filterNum[filterCnt]) {
                    if (++filterCnt >= filterSpeed) {
                        filterCnt = 0;
                    }

                    if(++twoCnt >= 2){
                        twoCnt = 0;

                        if(minCnt < maxCnt){
                            addPoint(min);
                            addPoint(max);
                        } else {
                            addPoint(max);
                            addPoint(min);
                        }
                        min = Integer.MAX_VALUE;
                        max = Integer.MIN_VALUE;
                        minCnt = 0;
                        maxCnt = 0;
                        cnt = 0;
                    }
                }

                if (++addCnt >= EcgConfig.SPEED) {
                    addCnt = 0;
                }
            } else {
                addPoint(val);
            }
        }

        public void removeFilterPoint(){
            if (rate < 1) {
                if (addCnt == filterNum[filterCnt]) {
                    if (++filterCnt >= filterSpeed) {
                        filterCnt = 0;
                    }

                    if(++twoCnt >= 2){
                        twoCnt = 0;

                        removePoint();
                        removePoint();
                    }
                }

                if (++addCnt >= EcgConfig.SPEED) {
                    addCnt = 0;
                }
            } else {
                removePoint();
            }
        }
        private void removePoint(){
            if (ecgShowModeEnum == EcgShowModeEnum.MODE_SCROLL) {
                //数据只删除
                if(mDirection== EcgScrollDirection.RIGHT){
                    buffer.remove(length - 1);
                }else {
                    buffer.remove(0);
                }
            }
        }
        private void addPoint(int val) {
            //val = (int) (val * mRangeRate);
            if (ecgShowModeEnum == EcgShowModeEnum.MODE_SWEEP) {
                if (buffer.size() == length) {
                    buffer.set(orderSweep, val);
                    if (++orderSweep >= length) {
                        orderSweep = 0;
                    }
                } else {
                    buffer.add(val);
                    orderSweep = 0;
                }
            } else if (ecgShowModeEnum == EcgShowModeEnum.MODE_SCROLL) {
                //数据添加区分左右方向
                if(mDirection==EcgScrollDirection.RIGHT){
                    if (buffer.size() == length) {
                        buffer.remove(length - 1);
                    }
                    buffer.add(0, val);
                }else {
                    if (buffer.size() == length) {
                        buffer.remove(0);
                    }
                    buffer.add(val);
                }
            }
        }

        public void calcPath() {
            if (buffer.size() == 0) {
                path.reset();
                pathSweep.reset();
                return;
            }

            try {
                if (ecgShowModeEnum == EcgShowModeEnum.MODE_SCROLL) {
                    pathSweep.reset();
                    path.reset();
                    path.moveTo(origin.x, origin.y + buffer.get(0));
                    float curX = origin.x;
                    for (int i = 1; i < buffer.size(); i++) {
                        curX += addX;
                        path.lineTo(curX, origin.y + buffer.get(i));
                    }
                } else if (ecgShowModeEnum == EcgShowModeEnum.MODE_SWEEP) {
                    if (orderSweep < 1) {
                        pathSweep.reset();
                        path.reset();

                        if(buffer.size() > 0){
                            path.moveTo(origin.x, origin.y + buffer.get(0));
                            float curX = origin.x;
                            for (int i = 1; i < buffer.size(); i++) {
                                curX += addX;
                                if(i < buffer.size()){
                                    path.lineTo(curX, origin.y + buffer.get(i));
                                }else{
                                    break;
                                }
                            }
                            curXStart = curX;
                        }
                    } else {
                        int orderSweepTemp = orderSweep;
                        path.reset();
                        if(buffer.size() == 0){
                            return;
                        }
                        path.moveTo(origin.x, origin.y + buffer.get(0));
                        float curX = origin.x;
                        for (int i = 1; i < orderSweepTemp; i++) {
                            curX += addX;
                            path.lineTo(curX, origin.y + buffer.get(i));
                        }

                        pathSweep.reset();
                        int sweepClear = filterSweepClear;
                        if (orderSweepTemp < filterSweepClear) {
                            sweepClear = orderSweepTemp;
                        }
                        int orderClear = orderSweepTemp + sweepClear;
                        if (orderClear >= length) {
                            orderClear = length - 1;
                        }
                        curX = origin.x + (addX * orderClear);
                        pathSweep.moveTo(curX, origin.y + buffer.get(orderClear));
                        for (int i = orderClear + 1; i < buffer.size(); i++) {
                            curX += addX;
                            pathSweep.lineTo(curX, origin.y + buffer.get(i));
                        }
                        curXStart = curX;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public RectF getRect() {
            return rect;
        }

        public float getCurXStart() {
            return curXStart;
        }

        public void setCurXStart(float curXStart) {
            this.curXStart = curXStart;
        }
    }

    private ArrayList<Lead> leadList;
    private float[] gainArray;
    private EcgShowModeEnum ecgShowModeEnum = EcgShowModeEnum.MODE_SWEEP;
    private int speedPixels;
    private float mRangeRate;
    // 每网格的像素数
    private float gridSpace = 8;
    private float sweepClearCm = 0.2f;
    private int sweepClear = 0;

    private EcgScrollDirection mDirection=EcgScrollDirection.LEFT;

    public LeadManager() {
        this.leadList = new ArrayList<>();
    }

    public void setRangeLen(float rate){
        mRangeRate = rate;
    }

    public void addLead(Lead lead) {
        this.leadList.add(lead);
    }
    public void setEcgScrollDirection(EcgScrollDirection direction){
        mDirection = direction;
    }

    public EcgScrollDirection getScrollDirection() {
        return mDirection;
    }

    public synchronized void clearLeads() {
        if(this.leadList != null){
            this.leadList.clear();
            this.leadList = null;
        }
    }

    public synchronized void calcPath() {
        if(leadList == null)
        {
            return;
        }
        for (int i = 0; i < leadList.size(); i++) {
            leadList.get(i).calcPath();
        }
    }

    public synchronized void drawEcgPath(Canvas canvas, Paint paint) {
        if(leadList == null){
            return;
        }

        for (int i = 0; i < leadList.size(); i++) {
            Lead lead = leadList.get(i);
            if(lead != null){
                Path path = lead.path;
                if(path != null){
                    canvas.drawPath(path, paint);
                }

                Path pathSweep = lead.pathSweep;
                if(pathSweep != null){
                    canvas.drawPath(pathSweep, paint);
                }
            }
        }
    }

    /**
     * 清理心电数据
     */
    public synchronized void clearEcgData() {
        if(leadList != null && leadList.size() > 0){
            for (int i = 0; i < leadList.size(); i++) {
                leadList.get(i).clear();
            }
        }
    }

    /**
     * 设置ecg显示模式
     * @param ecgShowModeEnum
     */
    public synchronized void setEcgMode(EcgShowModeEnum ecgShowModeEnum) {
        this.ecgShowModeEnum = ecgShowModeEnum;
        clearEcgData();
    }

    /**
     * 设置增益
     * @param gainArray
     */
    public synchronized  void setSensitivity(float[] gainArray){
        this.gainArray = gainArray;
        clearEcgData();
    }

    /**
     * 设置走速
     * @param leadSpeedType
     */
    public synchronized void setSpeed(LeadSpeedType leadSpeedType) {
        speedPixels = getSpeedPixels(leadSpeedType);
        clearEcgData();
    }

    /**
     * 获取走速转换的像素
     * @param leadSpeedType
     * @return
     */
    private int getSpeedPixels(LeadSpeedType leadSpeedType) {
        float speedValue = (float) leadSpeedType.getValue();

        int speedPixels = (int) (25 * gridSpace * (speedValue / 25F));
        sweepClear = (int) (EcgConfig.SPEED * sweepClearCm / (speedValue / 10F));

        return speedPixels;
    }

    public ArrayList<Lead> getLeadList() {
        return leadList;
    }

    public void setleadList(ArrayList<Lead> leadList) {
        this.leadList = leadList;
    }

    public float getGridSpace() {
        return gridSpace;
    }

    public void setGridSpace(float gridSpace) {
        this.gridSpace = gridSpace;
    }
}
