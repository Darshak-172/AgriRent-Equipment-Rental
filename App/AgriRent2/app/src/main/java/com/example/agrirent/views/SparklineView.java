package com.example.agrirent.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SparklineView extends View {

    private Paint linePaint;
    private Paint gradientPaint;
    private Path linePath;
    private Path areaPath;
    private float[] dataPoints;
    private float maxValue = 100f;
    private int chartColor = Color.parseColor("#0df26c");

    public SparklineView(Context context) {
        super(context);
        init();
    }

    public SparklineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setColor(Color.parseColor("#0df26c"));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setPathEffect(new CornerPathEffect(30));
        linePaint.setShadowLayer(15, 0, 0, Color.parseColor("#800df26c"));

        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gradientPaint.setStyle(Paint.Style.FILL);

        linePath = new Path();
        areaPath = new Path();

        // Default test data
        setData(new float[]{20, 40, 30, 70, 50, 90, 80});
    }

    public void setData(float[] points) {
        this.dataPoints = points;
        this.maxValue = 0;
        for (float p : points) if (p > maxValue) maxValue = p;
        if (maxValue == 0) maxValue = 1;
        invalidate();
    }

    public void setChartColor(int color) {
        this.chartColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints == null || dataPoints.length < 2) return;

        float width = getWidth();
        float height = getHeight();
        float xStep = width / (dataPoints.length - 1);

        linePath.reset();
        areaPath.reset();

        boolean allZero = true;
        for (float p : dataPoints) {
            if (p > 0) { allZero = false; break; }
        }

        for (int i = 0; i < dataPoints.length; i++) {
            float x = i * xStep;
            float y;
            
            if (allZero) {
                y = height / 2f;
            } else {
                y = height - (dataPoints[i] / maxValue * height * 0.8f) - (height * 0.1f);
            }

            if (i == 0) {
                linePath.moveTo(x, y);
                areaPath.moveTo(x, height);
                areaPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                areaPath.lineTo(x, y);
            }
        }

        areaPath.lineTo(width, height);
        areaPath.close();

        // Setup Dynamic Gradient
        int startColor = (chartColor & 0x00FFFFFF) | 0x4D000000; // 30% alpha
        LinearGradient gradient = new LinearGradient(0, 0, 0, height,
                startColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
        gradientPaint.setShader(gradient);

        canvas.drawPath(areaPath, gradientPaint);
        canvas.drawPath(linePath, linePaint);
    }
}
