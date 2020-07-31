package com.app.player.view;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.player.R;

import java.lang.ref.WeakReference;

public class LoadingRingView extends FrameLayout {

    private final long mDuration = 800; // Animation duration

    private int width;
    private int height;

    private Context context;
    private ImageView ivLoading;
    private float space;
    private Paint paintCircle;
    private Paint paintRing;
    private Rect rect;
    private RectF rectF;
    private ValueAnimator animation;
    private float factor;
    private boolean isFirst;

    static class RingUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private final WeakReference<LoadingRingView> reference;

        RingUpdateListener(LoadingRingView view) {
            this.reference = new WeakReference<>(view);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            LoadingRingView view = reference.get();
            if (view == null || view.context == null
                    || view.context instanceof Activity && ((Activity) view.context).isFinishing()) {
                return;
            }
            view.factor = (float) animation.getAnimatedValue();
            view.invalidate();
        }
    }

    static class BeatUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private final WeakReference<LoadingRingView> reference;
        private float factor = 0.4f;

        BeatUpdateListener(LoadingRingView view) {
            this.reference = new WeakReference<>(view);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            LoadingRingView view = reference.get();
            if (view == null || view.context == null
                    || view.context instanceof Activity && ((Activity) view.context).isFinishing()) {
                return;
            }
            view.factor = (float) animation.getAnimatedValue();
            float scale, alpha;
            if (view.factor > 0.5f) {
                scale = 0.89f + 0.22f * getAccelerateDecelerateInterpolation((view.factor - 0.5f) * 2);
            } else {
                scale = 1.11f - 0.22f * getAccelerateDecelerateInterpolation((view.factor) * 2);
            }
            scale = Math.max(scale, 0.89f);
            scale = Math.min(scale, 1.11f);

            alpha = scale - 0.11f;
            alpha = Math.max(alpha, 0.78f);
            alpha = Math.min(alpha, 1f);
            view.ivLoading.setScaleX(scale);
            view.ivLoading.setScaleY(scale);
            view.ivLoading.setAlpha(alpha);
        }

        float getAccelerateDecelerateInterpolation(float input) {
            return (float) (Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
        }

        float getDecelerateInterpolator(float input) {
            float result;
            if (factor == 1.0f) {
                result = (float) (1.0f - (1.0f - input) * (1.0f - input));
            } else {
                result = (float) (1.0f - Math.pow((1.0f - input), 2 * factor));
            }
            return result;
        }

        float getSpringInterpolation(float input) {
            return (float) (Math.pow(2, -10 * input) * Math.sin((input - factor / 4) * (2 * Math.PI) / factor) + 1);
        }
    }

    public LoadingRingView(@NonNull Context context) {
        this(context, null);
    }

    public LoadingRingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingRingView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.isFirst = true;
        this.context = context;
        setWillNotDraw(false);
        View root = LayoutInflater.from(context).inflate(R.layout.loading_ring_view, this);
        ivLoading = root.findViewById(R.id.iv_loading_ring);

        space = dip2px(context, 3f);
        paintCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCircle.setColor(Color.parseColor("#EEF3FF"));
        paintCircle.setStrokeWidth(dip2px(context, 3));
        paintCircle.setStyle(Paint.Style.STROKE);

        paintRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRing.setColor(Color.parseColor("#5E88FF"));
        paintRing.setStrokeCap(Paint.Cap.ROUND);
        paintRing.setStrokeWidth(dip2px(context, 3));
        paintRing.setStyle(Paint.Style.STROKE);

        rect = new Rect();
        rectF = new RectF();

        animation = ValueAnimator.ofFloat(0f, 1f);
        animation.setRepeatMode(ValueAnimator.RESTART);
        animation.setRepeatCount(ValueAnimator.INFINITE);
        animation.setDuration(mDuration);
        animation.setInterpolator(new LinearInterpolator());
        animation.addUpdateListener(new RingUpdateListener(this));
        animation.addUpdateListener(new BeatUpdateListener(this));
    }

    public void setLoadingBackgroudColor(int loadingBackgroudColor) {
        paintCircle.setColor(loadingBackgroudColor);
    }

    public void setLoadingForegroudColor(int loadingForegroudColor) {
        paintRing.setColor(loadingForegroudColor);
    }
    public void setLoadingDrawable(int resId) {
        ivLoading.setImageResource(resId);
    }

    @Override
    public void setVisibility(int visibility) {
        switch (visibility) {
            case VISIBLE:
                reStart();
                break;
            case GONE:
            case INVISIBLE:
                stop();
                break;
        }
        super.setVisibility(visibility);
    }

    @Override
    protected void onAttachedToWindow() {
        if (!isFirst) {
            reStart();
        }
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    public void reStart() {
        stop();
        if (animation != null) {
            animation.start();
        }
    }

    public void stop() {
        if (animation != null) {
            animation.cancel();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float h = Math.min(width, height);
        float w = height;
        float startX = (width - w) / 2;
        float startY = (height - h) / 2;
        rect.set((int) (startX + space), (int) (startY + space), (int) (startX + w - space), (int) (startY + h - space));
        rectF.set(rect);
        canvas.drawArc(rectF, 0, 360, false, paintCircle);

        float startAngle = 225f + (585f - 225f) * factor;
        startAngle = Math.max(startAngle, 225f);
        startAngle = Math.min(startAngle, 585f);
        canvas.drawArc(rectF, startAngle, 90, false, paintRing);

        if (isFirst) {
            isFirst = false;
            reStart();
        }
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (dpValue * (metrics.densityDpi / 160f));
    }
}
