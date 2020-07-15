package com.pp.scalebar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.pp.scalebar.util.SizeUtils;

import java.text.SimpleDateFormat;

public class VideoTimeBar extends ScalableScaleBar implements ScalableScaleBar.TickMarkStrategy {

    private static final String TAG = "RSVideoTimeBar";
    public static final String MODE_UINT_1_MIN = "scale unit 1 minute";
    public static final String MODE_UINT_5_MIN = "scale unit 5 minute";
    public static final String MODE_UINT_10_MIN = "scale unit 10 minute";
    public static final String MODE_UINT_30_MIN = "scale unit 30 minute";
    public static final String MODE_UINT_1_HOUR = "scale unit 1 hour";

    @StringDef({MODE_UINT_1_MIN, MODE_UINT_5_MIN, MODE_UINT_10_MIN, MODE_UINT_30_MIN, MODE_UINT_1_HOUR})
    public @interface Mode {
    }

    private @Mode
    String mMode = MODE_UINT_1_MIN;

    public static final long MODE_UINT_1_MIN_VALUE = 30 * 60 * 1000;
    public static final long MODE_UINT_5_MIN_VALUE = 1 * 60 * 60 * 1000;
    public static final long MODE_UINT_10_MIN_VALUE = 2 * 60 * 60 * 1000;
    public static final long MODE_UINT_30_MIN_VALUE = 8 * 60 * 60 * 1000;
    public static final long MODE_UINT_1_HOUR_VALUE = 16 * 60 * 60 * 1000;
    private Paint mTickPaint;
    private Paint mColorCursorPaint;
    private float mTriangleHeight = 10;
    private int tickValueColor;
    private float tickValueSize;
    private int cursorBackgroudColor;
    private float cursorValueSize;
    private final int colorScaleBackgroud;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
    float tickValueBoundOffsetH = 20;

    public VideoTimeBar(Context context) {
        this(context, null);
    }

    public VideoTimeBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VideoTimeBar);

        tickValueColor = typedArray.getColor(R.styleable.VideoTimeBar_tickValueColor, Color.BLACK);
        tickValueSize = typedArray.getDimension(R.styleable.VideoTimeBar_tickValueSize, SizeUtils.sp2px(getContext(), 8));
        cursorBackgroudColor = typedArray.getColor(R.styleable.VideoTimeBar_cursorBackgroudColor, Color.RED);
        cursorValueSize = typedArray.getDimension(R.styleable.VideoTimeBar_cursorValueSize, SizeUtils.sp2px(getContext(), 10));
        colorScaleBackgroud = typedArray.getColor(R.styleable.VideoTimeBar_colorScaleBackgroud, Color.WHITE);
        typedArray.recycle();
        init();
    }

    void init() {
        tickValueBoundOffsetH = SizeUtils.dp2px(getContext(), 6);

        mTickPaint = new Paint();
        mTickPaint.setColor(tickValueColor);
        mTickPaint.setAntiAlias(true);
        mTickPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mTickPaint.setTextAlign(Paint.Align.CENTER);
        mTickPaint.setTextSize(tickValueSize);
        mTickPaint.setStrokeWidth(1);
        mTickPaint.setDither(true);

        mColorCursorPaint = new Paint();
        mColorCursorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mColorCursorPaint.setDither(true);

        setTickMarkStrategy(this);
    }

    public void setMode(@Mode String mode) {
        setMode(mode, true);
    }

    @Override
    public void setRange(long start, long end) {
        super.setRange(start, end);
        if (mMode != MODE_UINT_1_MIN) {
            setMode(mMode, true);
        }
    }

    void setMode(@Mode String m, boolean setScaleRatio) {
        if (mMode == m) {
//            Log.e(TAG, " same mode.");
            return;
        }
        long spanValue;
        switch (m) {
            case MODE_UINT_1_MIN:
                this.mMode = m;
                updateScaleInfo(5 * 60 * 1000, 1 * 60 * 1000);
                spanValue = MODE_UINT_1_MIN_VALUE;
                break;
            case MODE_UINT_5_MIN:
                this.mMode = m;
                updateScaleInfo(10 * 60 * 1000, 5 * 60 * 1000);
                spanValue = MODE_UINT_5_MIN_VALUE;
                break;
            case MODE_UINT_10_MIN:
                this.mMode = m;
                updateScaleInfo(30 * 60 * 1000, 10 * 60 * 1000);
                spanValue = MODE_UINT_10_MIN_VALUE;
                break;
            case MODE_UINT_30_MIN:
                this.mMode = m;
                updateScaleInfo(1 * 60 * 60 * 1000, 30 * 60 * 1000);
                spanValue = MODE_UINT_30_MIN_VALUE;
                break;
            case MODE_UINT_1_HOUR:
                this.mMode = m;
                updateScaleInfo(2 * 60 * 60 * 1000, 1 * 60 * 60 * 1000);
                spanValue = MODE_UINT_1_HOUR_VALUE;
                break;
            default:
                throw new RuntimeException("not support mode: " + m);
        }
//        Log.e("TAG", " mode: " + mMode);
        if (setScaleRatio) {
            setScaleRatio(getMinScreenSpanValue() * 1.0f / spanValue);
        }
    }

    @Override
    public boolean disPlay(long scaleValue, boolean keyScale) {
        return keyScale;
    }

    @NonNull
    @Override
    public String getScaleValue(long scaleValue, boolean keyScale) {
        return simpleDateFormat.format(scaleValue);
    }

    @Override
    public int getColor(long scaleValue, boolean keyScale) {
        return tickValueColor;
    }

    @Override
    public float getSize(long scaleValue, boolean keyScale, float maxScaleValueSize) {
        return tickValueSize;
    }

    @Override
    protected void onEndTickDraw(Canvas canvas) {
        int startLimit = getScrollX();
        int endLimit = getScrollX() + getWidth();
        float startY = getBaselinePosition() + 1.0f;
        float endY = getHeight();
        // ① 绘制背景
        mColorCursorPaint.setColor(colorScaleBackgroud);
        canvas.drawRect(startLimit, startY, endLimit, endY, mColorCursorPaint);
        //  绘制颜色刻度尺
        if (null != mColorScale) {
            float cursorPosition = getCursorPosition();
            long cursorValue = getCursorValue();
            float unitPixel = getUnitPixel();
            int size = mColorScale.getSize();
            RectF rect = new RectF();
            rect.top = startY;
            rect.bottom = endY;
            long startValue;
            long endValue;
            float startPiexl;
            float endPiexl;
            // ② 绘制颜色刻度
            for (int i = 0; i < size; i++) {
                startValue = mColorScale.getStart(i);
                endValue = mColorScale.getEnd(i);
                startPiexl = cursorPosition + (startValue - cursorValue) * unitPixel;
                endPiexl = cursorPosition + (endValue - cursorValue) * unitPixel;
                if (endPiexl < startLimit) {
                    continue;
                }
                if (startPiexl > endLimit) {
                    continue;
                }

                rect.left = startPiexl;
                rect.right = endPiexl;

                mColorCursorPaint.setColor(mColorScale.getColor(i));
                canvas.drawRect(rect, mColorCursorPaint);
            }
        }
    }

    @Override
    protected int calcContentHeight(float baselinePositionProportion) {
        int contentHeight = super.calcContentHeight(baselinePositionProportion);

        mColorCursorPaint.setTextSize(cursorValueSize);
        Paint.FontMetrics fontMetrics = mColorCursorPaint.getFontMetrics();
        double ceil = Math.ceil(fontMetrics.bottom - fontMetrics.top);
        int cursorValueHeight = (int) (ceil + mTriangleHeight + tickValueBoundOffsetH) + 5;
        int cursorContentHeight = (int) ((getKeyTickHeight() + cursorValueHeight) / baselinePositionProportion + 0.5f);
        return Math.max(contentHeight, cursorContentHeight);
    }

    @Override
    protected void onScale(ScaleMode info, float unitPixel) {
        int width = getWidth();
        // 计算一屏刻度值跨度
        float screenSpanValue = width / unitPixel;
        updateMode(screenSpanValue);
    }

    protected void updateMode(float screenSpanValue) {
        if (screenSpanValue > MODE_UINT_1_HOUR_VALUE) {
            setMode(MODE_UINT_1_HOUR, false);
        } else if (screenSpanValue > MODE_UINT_30_MIN_VALUE) {
            setMode(MODE_UINT_30_MIN, false);
        } else if (screenSpanValue > MODE_UINT_10_MIN_VALUE) {
            setMode(MODE_UINT_10_MIN, false);
        } else if (screenSpanValue > MODE_UINT_5_MIN_VALUE) {
            setMode(MODE_UINT_5_MIN, false);
        } else {
            setMode(MODE_UINT_1_MIN, false);
        }
    }

    SimpleDateFormat cursorDateFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    protected void drawCursor(Canvas canvas, float cursorPosition, long cursorValue) {
        super.drawCursor(canvas, cursorPosition, cursorValue);
        float keyTickHeight = getKeyTickHeight();
        float baselinePosition = getBaselinePosition();
        // ①绘制倒三角
        Path path = new Path();
        float startX = cursorPosition;
        float statY = baselinePosition - keyTickHeight;
        // 倒三角形顶边的 y
        float topSidePosition = statY - mTriangleHeight;
        path.moveTo(startX, statY);
        path.lineTo(startX - 3.5f, topSidePosition);
        path.lineTo(startX + 3.5f, topSidePosition);
        path.close();
        mTickPaint.setColor(cursorBackgroudColor);
        canvas.drawPath(path, mTickPaint);

        String content = cursorDateFormat.format(cursorValue);
        Rect textBound = new Rect();
        mTickPaint.setTextSize(cursorValueSize);
        // 测量内容大小
        mTickPaint.getTextBounds(content, 0, content.length(), textBound);

        // ②绘制内容背景
        // 创建包裹内容的背景大小
        RectF rectF = new RectF(0, 0, textBound.width() + 20, textBound.height() + tickValueBoundOffsetH);
        // 背景位置
        // x方向： 关于游标居中  y方向:在倒三角形上边
        rectF.offset(cursorPosition - rectF.width() * 0.5f, topSidePosition + 0.5f - rectF.height());
        float rx = rectF.width() * 0.5f;
        float ry = rx;
        mTickPaint.setColor(cursorBackgroudColor);
        canvas.drawRoundRect(rectF, rx, ry, mTickPaint);

        mTickPaint.setColor(tickValueColor);
        // ③ 绘制内容
        // 使内容绘制在背景内,达到包裹效果
        float textY = rectF.centerY() + textBound.height() * 0.5f;
        canvas.drawText(content, cursorPosition, textY, mTickPaint);
    }

    private ColorScale mColorScale;

    public void setColorScale(ColorScale scale) {
        this.mColorScale = scale;
    }

    public interface ColorScale {
        int getSize();

        long getStart(int index);

        long getEnd(int index);

        @ColorInt
        int getColor(int index);
    }

}
