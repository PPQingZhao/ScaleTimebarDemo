package com.pp.scalebar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import com.pp.scalebar.util.SizeUtils;

import java.util.Calendar;

public class ScalableScaleBar extends View implements ScaleGestureDetector.OnScaleGestureListener,
        GestureDetector.OnGestureListener {
    private static final String TAG = "ScalableScaleBar";

    /* 缩放比例*/
    private float mScaleRatio = 1.0f;
    /* 关键刻度高度*/
    private float mKeyTickHeight = 35;
    /* 刻度高度*/
    private float mTickHeight;
    /* 普通刻度线与关键刻度线的比*/
    private float mNormalTickAndKeyTickRatio = 0.67f;
    /* 刻度间距*/
    private float mTickSpacing;
    private ScaleGestureDetector mScaleGestureDetector;
    private Paint mScalePaint;
    private ScaleMode mScaleInfo;
    private float unitPixel;
    //
    private long minScreenSpanValue;
    private long maxScreenSpanValue;
    private long mCursorValue;
    private GestureDetectorCompat mGestureDetectorCompat;
    private boolean scrollHappened;
    private float mCursorPosition;
    private float mCursorPositionProportion = 0.5f;
    // 刻度尺横线位置
    private float mBaselinePosition;
    private float mBaselinePositionProportion = 0.67f;
    private float maxUnitPixel;
    private float minUnitPiexl;
    private int tickColor;
    private final int cursorLineColor;
    private boolean showCursorLine;
    private boolean showTickValue;
    private Scroller mScroller;
    /**
     * 状态
     */
    private int status = STATUS_NONE;
    /**
     * 默认状态
     */
    public static final int STATUS_NONE = 0;

    /**
     * 按下
     */
    public static final int STATUS_DOWN = 1;
    /**
     * 拖拽滚动
     */
    public static final int STATUS_SCROLL = STATUS_DOWN + 1;
    /**
     * 甩动滚动(惯性)
     */
    public static final int STATUS_SCROLL_FLING = STATUS_SCROLL + 1;
    /**
     * 缩放
     */
    public static final int STATUS_ZOOM = STATUS_SCROLL_FLING + 1;
    /* 刻度值 最大规格*/
    private float maxScaleValueSize;
    private float tickValueOffset = 15;

    public ScalableScaleBar(Context context) {
        this(context, null);
    }

    public ScalableScaleBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScalableScaleBar);
        mKeyTickHeight = typedArray.getDimension(R.styleable.ScalableScaleBar_keyTickHeight, SizeUtils.dp2px(getContext(), 10));
        mNormalTickAndKeyTickRatio = typedArray.getFloat(R.styleable.ScalableScaleBar_normalTickRatio, 0.67f);

        tickColor = typedArray.getColor(R.styleable.ScalableScaleBar_tickColor, Color.BLACK);
        cursorLineColor = typedArray.getColor(R.styleable.ScalableScaleBar_cursorLineColor, Color.YELLOW);
        showCursorLine = typedArray.getBoolean(R.styleable.ScalableScaleBar_showCursorLine, true);
        showTickValue = typedArray.getBoolean(R.styleable.ScalableScaleBar_showTickValue, true);
        maxScaleValueSize = typedArray.getDimension(R.styleable.ScalableScaleBar_maxScaleValueSize, SizeUtils.sp2px(getContext(), 15));

        float position = typedArray.getFloat(R.styleable.ScalableScaleBar_cursorPosition, 0.5f);
        if (position != 0) {
            mCursorPositionProportion = position;
        }
        position = typedArray.getFloat(R.styleable.ScalableScaleBar_baselinePosition, 0.67f);
        if (position != 0) {
            mBaselinePositionProportion = position;
        }
        // 释放
        typedArray.recycle();


        init();
    }

    private void init() {
        tickValueOffset = SizeUtils.dp2px(getContext(), 5);

        mScalePaint = new Paint();
        mScalePaint.setAntiAlias(true);
        mScalePaint.setStrokeWidth(1.0f);
        mScalePaint.setDither(true);
        mScalePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        minScreenSpanValue = 30 * 60 * 1000;
        maxScreenSpanValue = 24 * 60 * 60 * 1000;
        mScaleInfo = new ScaleMode();
        //  单位刻度 1分钟
        mScaleInfo.unitValue = 60000;
        // 关键刻度差值 5分钟
        mScaleInfo.keyScaleRange = 5 * 60 * 1000;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        mScaleInfo.startValue = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        mScaleInfo.endValue = calendar.getTimeInMillis();
        mCursorValue = mScaleInfo.startValue;
    }

    protected void setScaleRatio(@FloatRange(from = 0.0f, to = 1.0f, fromInclusive = false) float scaleRatio) {
        this.mScaleRatio = scaleRatio;
    }

    protected long getMaxScreenSpanValue() {
        return maxScreenSpanValue;
    }

    protected long getMinScreenSpanValue() {
        return minScreenSpanValue;
    }

    protected float getKeyTickHeight() {
        return mKeyTickHeight;
    }

    protected float getCursorPosition() {
        return mCursorPosition;
    }

    public long getCursorValue() {
        return mCursorValue;
    }

    protected float getUnitPixel() {
        return unitPixel;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // 计算普通刻度线高度
        mTickHeight = mKeyTickHeight * mNormalTickAndKeyTickRatio;
        // 计算游标位置
        mCursorPosition = w * mCursorPositionProportion;
        // 计算刻度尺横线位置
        mBaselinePosition = h * mBaselinePositionProportion;

        // 计算最大单位像素
        maxUnitPixel = w * 1.0f / minScreenSpanValue;
        // 计算最小单位像素
        minUnitPiexl = w * 1.0f / maxScreenSpanValue;

        // 计算单位像素 -- 根据最大缩放状态和当前缩放率计算
        unitPixel = maxUnitPixel * mScaleRatio;
        // 计算刻度线间距
        mTickSpacing = mScaleInfo.unitValue * unitPixel;
    }

    public void setRange(long start, long end) {
        if (start >= end) {
            Log.e(TAG, "start cannot be greater than end.  start: " + start + "  end: " + end);
            return;
        }
        mScaleInfo.startValue = start;
        mScaleInfo.endValue = end;
        mCursorValue = start;
        invalidate();
    }

    public void setCursorValue(long cursorValue) {
        if (status != STATUS_NONE) {
            Log.e(TAG, "scalebar onTouched");
            return;
        }
        if (cursorValue < mScaleInfo.startValue || cursorValue > mScaleInfo.endValue) {
            Log.e(TAG, "Illegal parameter. startValue: " + mScaleInfo.startValue + "  endValue: " + mScaleInfo.endValue + "   cursorValue: " + cursorValue);
            return;
        }
        this.mCursorValue = cursorValue;
        invalidate();
    }

    protected void updateScaleInfo(long keyScaleRange, long unitValue) {
        mScaleInfo.keyScaleRange = keyScaleRange;
        mScaleInfo.unitValue = unitValue;
    }

    protected float getBaselinePosition() {
        return mBaselinePosition;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), getHeightSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    private int getHeightSize(int size, int heightMeasureSpec) {
        int result = size;
        // 计算内容高度
        int contentHeight = calcContentHeight(mBaselinePositionProportion);
        int specMode = MeasureSpec.getMode(heightMeasureSpec);
        int specSize = MeasureSpec.getSize(heightMeasureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                // 必须显示内容
                result = size > contentHeight ? size : contentHeight;
                break;
            case MeasureSpec.AT_MOST:
                // 包裹内容:内容高度
                result = contentHeight;
                break;
            case MeasureSpec.EXACTLY:
                // 必须显示内容
                result = specSize > contentHeight ? specSize : contentHeight;
                break;
        }
        return result;
    }

    protected int calcContentHeight(float baselinePositionProportion) {
        int tickValueHeight = 0;
        if (showTickValue && null != mTickMarkStrategy) {
            mScalePaint.setTextSize(maxScaleValueSize);
            Paint.FontMetrics fontMetrics = mScalePaint.getFontMetrics();
            double ceil = Math.ceil(fontMetrics.bottom - fontMetrics.top);
            tickValueHeight = (int) (ceil + tickValueOffset);
        }
        return (int) ((mKeyTickHeight + tickValueHeight) / baselinePositionProportion + 0.5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // 1. 绘制横线
        float baselinePosition = getBaselinePosition();
        mScalePaint.setColor(tickColor);
        canvas.drawLine(getScrollX(), baselinePosition, getScrollX() + getWidth(), baselinePosition, mScalePaint);

        // 2. 绘制刻度线
        // 2.1 绘制游标左边刻度
        // 左边刻度范围
        long leftRange = mCursorValue - mScaleInfo.startValue;
        // 游标与左边相邻刻度差值
        long leftNeighborOffest = leftRange % mScaleInfo.unitValue;
        // 左边相邻刻度值
        long leftNeighborTickValue = mCursorValue - leftNeighborOffest;
        // 左边相邻刻度位置
        float leftNeighborPosition = mCursorPosition - leftNeighborOffest * unitPixel;
        // 左边能够绘制刻度最大数量
        int leftCount = (int) (mCursorPosition / mTickSpacing + 0.5f);
        // 正在绘制的刻度线的位置
        float onDrawTickPosition;
        // 正在绘制的刻度线的值
        long onDrawTickValue;
        for (int i = 0; i < leftCount; i++) {
            onDrawTickValue = leftNeighborTickValue - mScaleInfo.unitValue * i;
            if (onDrawTickValue < mScaleInfo.startValue) {
                break;
            }
            onDrawTickPosition = leftNeighborPosition - mTickSpacing * i;
            // 绘制关键刻度线
            if ((onDrawTickValue - mScaleInfo.startValue) % mScaleInfo.keyScaleRange == 0) {
                canvas.drawLine(onDrawTickPosition, baselinePosition - mKeyTickHeight, onDrawTickPosition, baselinePosition, mScalePaint);
                drawTickValue(canvas, onDrawTickPosition, baselinePosition - mKeyTickHeight, onDrawTickValue, true);
            } else {
                canvas.drawLine(onDrawTickPosition, baselinePosition - mTickHeight, onDrawTickPosition, baselinePosition, mScalePaint);
                drawTickValue(canvas, onDrawTickPosition, baselinePosition - mTickHeight, onDrawTickValue, false);
            }
        }

        // 2.2 绘制游标右边刻度
        // 游标右边相邻刻度值
        long rightNeighborTickValue = leftNeighborTickValue + mScaleInfo.unitValue;
        // 右边相邻刻度位置
        float rightNeighborPosition = leftNeighborPosition + mTickSpacing;
        // 右边绘制刻度最大数量
        int rightCount = (int) ((getWidth() - mCursorPosition) / mTickSpacing + 0.5f);
        for (int i = 0; i < rightCount; i++) {
            // 当前绘制刻度值
            onDrawTickValue = rightNeighborTickValue + mScaleInfo.unitValue * i;
            if (onDrawTickValue > mScaleInfo.endValue) {
                break;
            }
            onDrawTickPosition = rightNeighborPosition + mTickSpacing * i;
            // 关键刻度
            if ((onDrawTickValue - mScaleInfo.startValue) % mScaleInfo.keyScaleRange == 0) {
                canvas.drawLine(onDrawTickPosition, baselinePosition - mKeyTickHeight, onDrawTickPosition, baselinePosition, mScalePaint);
                drawTickValue(canvas, onDrawTickPosition, baselinePosition - mKeyTickHeight, onDrawTickValue, true);
            } else {
                canvas.drawLine(onDrawTickPosition, baselinePosition - mTickHeight, onDrawTickPosition, baselinePosition, mScalePaint);
                drawTickValue(canvas, onDrawTickPosition, baselinePosition - mTickHeight, onDrawTickValue, false);
            }
        }

        onEndTickDraw(canvas);

        // 4. 绘制游标
        drawCursor(canvas, mCursorPosition, mCursorValue);
    }

    protected void onEndTickDraw(Canvas canvas) {
    }

    /**
     * 绘制游标
     *
     * @param canvas
     * @param cursorPosition
     */
    protected void drawCursor(Canvas canvas, float cursorPosition, long cursorValue) {
        if (showCursorLine) {
            mScalePaint.setColor(cursorLineColor);
            canvas.drawLine(cursorPosition, 0, cursorPosition, getHeight(), mScalePaint);
        }
    }

    /**
     * 绘制刻度线描述
     *
     * @param canvas
     * @param x
     * @param y
     * @param scaleValue
     * @param keyScale
     */
    private void drawTickValue(Canvas canvas, float x, float y, long scaleValue, boolean keyScale) {
        if (showTickValue) {
            if (null != mTickMarkStrategy) {
                if (mTickMarkStrategy.disPlay(scaleValue, keyScale)) {
                    mScalePaint.setColor(mTickMarkStrategy.getColor(scaleValue, keyScale));
                    mScalePaint.setTextAlign(Paint.Align.CENTER);
                    float size = mTickMarkStrategy.getSize(scaleValue, keyScale, maxScaleValueSize);
                    size = Math.min(maxScaleValueSize, size);
                    mScalePaint.setTextSize(size);
                    canvas.drawText(mTickMarkStrategy.getScaleValue(scaleValue, keyScale), x, y - tickValueOffset, mScalePaint);
                }
            }
        }
    }

    ScaleGestureDetector getScaleGestureDetect() {
        if (null == mScaleGestureDetector) {
            mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        }
        return mScaleGestureDetector;
    }

    GestureDetectorCompat getGestureDetectorCompat() {
        if (null == mGestureDetectorCompat) {
            mGestureDetectorCompat = new GestureDetectorCompat(getContext(), this);
        }
        return mGestureDetectorCompat;
    }

    Scroller getScroller() {
        if (null == mScroller) {
            mScroller = new Scroller(getContext());
        }
        return mScroller;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getScaleGestureDetect().onTouchEvent(event);
        getGestureDetectorCompat().onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                if (scrollHappened && status != STATUS_SCROLL_FLING) {
                    if (null != mOnCursorListener) {
                        mOnCursorListener.onEnd(mCursorValue);
                    }
                }
                scrollHappened = false;
                if (status == STATUS_DOWN
                        || status == STATUS_SCROLL
                        || status == STATUS_ZOOM) {
                    status = STATUS_NONE;
                }
                break;
        }
        return true;
    }

    @Override
    public final boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor = detector.getScaleFactor();
        status = STATUS_ZOOM;
        unitPixel *= scaleFactor;
        if (unitPixel > maxUnitPixel) {
            unitPixel = maxUnitPixel;
            scaleFactor = 1.0f;
        } else if (unitPixel < minUnitPiexl) {
            unitPixel = minUnitPiexl;
            scaleFactor = 1.0f;
        }
        onScale(mScaleInfo, unitPixel);
        mScaleRatio *= scaleFactor;
        mTickSpacing = mScaleInfo.unitValue * unitPixel;
        invalidate();
        return unitPixel < maxUnitPixel || unitPixel > minUnitPiexl;
    }

    protected void onScale(ScaleMode info, float unitPixel) {
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    @Override
    public final boolean onDown(MotionEvent e) {
        if (status == STATUS_SCROLL_FLING) {
            getScroller().forceFinished(true);
        } else {
            scrollHappened = false;
        }
        status = STATUS_DOWN;
        // 返回出 拦截事件
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // do nothing
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // do nothing
        return false;
    }

    @Override
    public final boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (e2.getPointerCount() > 1) {
            return false;
        }
        if (getScaleGestureDetect().isInProgress()) {
            return false;
        }
        // TODO: 处理第一次触发滚动产生的距离过大,呈现滚动突兀不友好体验问题 ------ 待优化
        if (!scrollHappened) {
            scrollHappened = true;
            if (null != mOnCursorListener) {
                mOnCursorListener.onStart(mCursorValue);
            }
            return true;
        }
        status = STATUS_SCROLL;
        // 游标刻度值增量
        long courseIncrement = (long) (distanceX / unitPixel);
        mCursorValue += courseIncrement;
        boolean result = true;
        if (mCursorValue < mScaleInfo.startValue) {
            mCursorValue = mScaleInfo.startValue;
            result = false;
        } else if (mCursorValue > mScaleInfo.endValue) {
            mCursorValue = mScaleInfo.endValue;
            result = false;
        }
        if (null != mOnCursorListener) {
            mOnCursorListener.onMoving(mCursorValue);
        }
        invalidate();
        return result;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // do nothing
    }

    @Override
    public void computeScroll() {
        if (getScroller().computeScrollOffset()) {
            int currX = getScroller().getCurrX();
            mCursorValue = mScaleInfo.startValue + (long) (currX / unitPixel);
            if (mCursorValue < mScaleInfo.startValue) {
                mCursorValue = mScaleInfo.startValue;
            } else if (mCursorValue > mScaleInfo.endValue) {
                mCursorValue = mScaleInfo.endValue;
            }
            invalidate();
        } else {
            if (status == STATUS_SCROLL_FLING) {
                status = STATUS_NONE;
                if (null != mOnCursorListener) {
                    mOnCursorListener.onEnd(mCursorValue);
                }
            }
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        status = STATUS_SCROLL_FLING;
        int startX = (int) ((mCursorValue - mScaleInfo.startValue) * unitPixel);
        int maX = (int) ((mScaleInfo.endValue - mScaleInfo.startValue) * unitPixel);
        getScroller().fling(startX,
                0,
                -(int) velocityX,
                0,
                0,
                maX,
                0,
                0);
        invalidate();
        return true;
    }

    protected class ScaleMode {
        public long unitValue;
        public long startValue;
        public long endValue;
        public long keyScaleRange;
    }

    private OnCursorListener mOnCursorListener;

    public void setOnCursorListener(OnCursorListener l) {
        this.mOnCursorListener = l;
    }

    public interface OnCursorListener {
        void onStart(long cursorValue);

        void onMoving(long cursorValue);

        void onEnd(long cursorValue);
    }

    private TickMarkStrategy mTickMarkStrategy;

    public void setTickMarkStrategy(TickMarkStrategy tickMarkStrategy) {
        this.mTickMarkStrategy = tickMarkStrategy;
    }

    public interface TickMarkStrategy {
        /**
         * 是否显示刻度值
         *
         * @param scaleValue 刻度值
         * @param keyScale   是否是关键刻度
         * @return
         */
        boolean disPlay(long scaleValue, boolean keyScale);

        /**
         * 获取显示的刻度值
         *
         * @param scaleValue
         * @param keyScale
         * @return
         */
        @NonNull
        String getScaleValue(long scaleValue, boolean keyScale);

        /**
         * 获取当前刻度值显示颜色
         *
         * @param scaleValue
         * @param keyScale
         * @return
         */
        @ColorInt
        int getColor(long scaleValue, boolean keyScale);

        /**
         * 获取当前刻度值显示大小
         *
         * @param scaleValue
         * @param keyScale
         * @param maxScaleValueSize
         * @return
         */
        @Dimension
        float getSize(long scaleValue, boolean keyScale, float maxScaleValueSize);
    }
}
