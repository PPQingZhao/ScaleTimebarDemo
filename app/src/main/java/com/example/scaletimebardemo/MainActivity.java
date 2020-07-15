package com.example.scaletimebardemo;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.graphics.Color;
import android.os.Bundle;

import com.pp.scalebar.ScalableScaleBar;
import com.pp.scalebar.VideoTimeBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private MainBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initTimebar();
        initNormalVideoTimebar();
        initVideoTimebar2();
    }

    private void initTimebar() {
        Calendar calendar = Calendar.getInstance();
        // 00:00:00 000
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        // 23:59:59 999
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endTime = calendar.getTimeInMillis();
        // 设置刻度尺范围
        mBinding.mainTimebar.setRange(startTime, endTime);

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        // 设置刻度值描述策略
        mBinding.mainTimebar.setTickMarkStrategy(new ScalableScaleBar.TickMarkStrategy() {
            @Override
            public boolean disPlay(long scaleValue, boolean keyScale) {
                // 只显示关键刻度描述
                return keyScale;
            }

            @NonNull
            @Override
            public String getScaleValue(long scaleValue, boolean keyScale) {
                // 获取刻度描述
                return simpleDateFormat.format(scaleValue);
            }

            @Override
            public int getColor(long scaleValue, boolean keyScale) {
                return Color.RED;
            }

            @Override
            public float getSize(long scaleValue, boolean keyScale, float maxScaleValueSize) {
                return 18;
            }
        });
    }

    private void initVideoTimebar2() {
        Calendar calendar = Calendar.getInstance();
        // 00:00:00 000
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        // 23:59:59 999
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endTime = calendar.getTimeInMillis();
        final List<ColorData> datas = new ArrayList<>();
        long start = startTime;
        long end;
        int color = Color.RED;
        for (int i = 0; i < 100; i++) {
            if (i % 4 == 0) {
                color = Color.BLUE;
            } else if (i % 3 == 0) {
                color = Color.YELLOW;
            } else if (i % 2 == 0) {
                color = Color.GREEN;
            }
            end = start + i * 1000 * 60 * 2;

            if (end > endTime) {
                break;
            }
            datas.add(new ColorData(start, end, color));
            start = end + (i % 3) * 1000 * 60 * 2;
        }

        // 设置刻度表示范围
        mBinding.mainVideotimebar2.setRange(startTime, endTime);
        // 设置颜色刻度尺策略
        mBinding.mainVideotimebar2.setColorScale(new TestColorScale(datas));
    }


    private void initNormalVideoTimebar() {
        Calendar calendar = Calendar.getInstance();
        // 00:00:00 000
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        // 23:59:59 999
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endTime = calendar.getTimeInMillis();
        final List<ColorData> datas = new ArrayList<>();
        long start = startTime;
        long end;
        int color = Color.RED;
        for (int i = 0; i < 100; i++) {
            if (i % 3 == 0) {
                color = Color.YELLOW;
            } else if (i % 2 == 0) {
                color = Color.GREEN;
            }
            end = start + i * 1000 * 60 * 5;

            if (end > endTime) {
                break;
            }
            datas.add(new ColorData(start, end, color));
            start = end + (i % 3) * 1000 * 60 * 5;
        }


        // 设置刻度表示范围
        mBinding.mainVideotimebarNormal.setRange(startTime, endTime);
        // 设置初始化模式
        mBinding.mainVideotimebarNormal.setMode(VideoTimeBar.MODE_UINT_10_MIN);
        // 设置颜色刻度尺策略
        mBinding.mainVideotimebarNormal.setColorScale(new TestColorScale(datas));
    }

    class TestColorScale implements VideoTimeBar.ColorScale {
        List<ColorData> datas;

        public TestColorScale(@NonNull List<ColorData> datas) {
            this.datas = datas;
        }

        @Override
        public int getSize() {
            return datas.size();
        }

        @Override
        public long getStart(int index) {
            return datas.get(index).start;
        }

        @Override
        public long getEnd(int index) {
            return datas.get(index).end;
        }

        @Override
        public int getColor(int index) {
            return datas.get(index).color;
        }
    }

    class ColorData {
        long start;
        long end;
        @ColorInt
        int color;

        public ColorData(long start, long end, @ColorInt int color) {
            this.start = start;
            this.end = end;
            this.color = color;
        }
    }
}
