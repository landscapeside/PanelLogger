package com.landside.panellogger;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;

import java.lang.reflect.Field;

/**
 * 可拖动的悬浮按钮
 * Created by linqs on 2017/12/21.
 */
class SonnyJackDragView implements View.OnTouchListener {
    private Builder mBuilder;

    private int mStatusBarHeight, mScreenWidth, mScreenHeight;

    //手指按下位置
    private int mStartX, mStartY, mLastX, mLastY;
    private boolean mTouchResult = false;

    private SonnyJackDragView(SonnyJackDragView.Builder builder) {
        mBuilder = builder;
        initDragView();
    }

    public View getDragView() {
        return mBuilder.view;
    }

    public Activity getActivity() {
        return mBuilder.activity;
    }

    public View getRootView(){
        return mBuilder.rootView;
    }

    public boolean getNeedNearEdge() {
        return mBuilder.needNearEdge;
    }

    public void setNeedNearEdge(boolean needNearEdge) {
        mBuilder.needNearEdge = needNearEdge;
        if (mBuilder.needNearEdge) {
            moveNearEdge();
        }
    }

    private void initDragView() {
        if (null == getActivity()) {
            throw new NullPointerException("the activity is null");
        }
        if (null == mBuilder.view) {
            throw new NullPointerException("the dragView is null");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && mBuilder.activity.isDestroyed()) {
            return;
        }

        //屏幕宽高
        WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        if (null != windowManager) {
            DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
            mScreenWidth = displayMetrics.widthPixels;
            mScreenHeight = displayMetrics.heightPixels;
        }

        //状态栏高度
        Rect frame = new Rect();
        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        mStatusBarHeight = frame.top;
        if (mStatusBarHeight <= 0) {
            try {
                Class<?> c = Class.forName("com.android.internal.R$dimen");
                Object obj = c.newInstance();
                Field field = c.getField("status_bar_height");
                int x = Integer.parseInt(field.get(obj).toString());
                mStatusBarHeight = getActivity().getResources().getDimensionPixelSize(x);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        int left = mBuilder.needNearEdge ? 0 : mBuilder.defaultLeft;
        FrameLayout.LayoutParams layoutParams = createLayoutParams(left, mBuilder.defaultTop, mBuilder.defaultRight, mBuilder.defaultBottom);
        if (getRootView() != null && getRootView() instanceof FrameLayout) {
            FrameLayout rootLayout = (FrameLayout) getRootView();
            rootLayout.addView(getDragView(), layoutParams);
        } else {
            FrameLayout rootLayout = (FrameLayout) getActivity().getWindow().getDecorView().findViewById(android.R.id.content);
            rootLayout.addView(getDragView(), layoutParams);
        }
        getDragView().setOnTouchListener(this);
    }

    private static SonnyJackDragView createDragView(SonnyJackDragView.Builder builder) {
        if (null == builder) {
            throw new NullPointerException("the param builder is null when execute method createDragView");
        }
        if (null == builder.activity) {
            throw new NullPointerException("the activity is null");
        }
        if (null == builder.view) {
            throw new NullPointerException("the view is null");
        }
        SonnyJackDragView sonnyJackDragView = new SonnyJackDragView(builder);
        return sonnyJackDragView;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchResult = false;
                mStartX = mLastX = (int) event.getRawX();
                mStartY = mLastY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                int left, top, right, bottom;
                int dx = (int) event.getRawX() - mLastX;
                int dy = (int) event.getRawY() - mLastY;
                left = v.getLeft() + dx;
                if (left < 0) {
                    left = 0;
                }
                right = left + v.getWidth();
                if (right > mScreenWidth) {
                    right = mScreenWidth;
                    left = right - v.getWidth();
                }
                top = v.getTop() + dy;
                if (top < mStatusBarHeight + 2) {
                    top = mStatusBarHeight + 2;
                }
                bottom = top + v.getHeight();
                if (bottom > mScreenHeight) {
                    bottom = mScreenHeight;
                    top = bottom - v.getHeight();
                }
                v.layout(left, top, right, bottom);
                mLastX = (int) event.getRawX();
                mLastY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                //这里需设置LayoutParams，不然按home后回再到页面等view会回到原来的地方
                v.setLayoutParams(createLayoutParams(v.getLeft(), v.getTop(), 0, 0));
                float endX = event.getRawX();
                float endY = event.getRawY();
                if (Math.abs(endX - mStartX) > 5 || Math.abs(endY - mStartY) > 5) {
                    //防止点击的时候稍微有点移动点击事件被拦截了
                    mTouchResult = true;
                }
                if (mTouchResult && mBuilder.needNearEdge) {
                    //是否每次都移至屏幕边沿
                    moveNearEdge();
                }
                break;
        }
        return mTouchResult;
    }

    /**
     * 移至最近的边沿
     */
    private void moveNearEdge() {
        int left = getDragView().getLeft();
        int lastX;
        if (left + getDragView().getWidth() / 2 <= mScreenWidth / 2) {
            lastX = 0;
        } else {
            lastX = mScreenWidth - getDragView().getWidth();
        }
        ValueAnimator valueAnimator = ValueAnimator.ofInt(left, lastX);
        valueAnimator.setDuration(1000);
        valueAnimator.setRepeatCount(0);
        valueAnimator.setInterpolator(new BounceInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int left = (int) animation.getAnimatedValue();
                getDragView().setLayoutParams(createLayoutParams(left, getDragView().getTop(), 0, 0));
            }
        });
        valueAnimator.start();
    }

    boolean b = true;

    private FrameLayout.LayoutParams createLayoutParams(int left, int top, int right, int bottom) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(mBuilder.size, mBuilder.size);
        if (b) {
            b = false;
            layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        } else {

        }
        layoutParams.setMargins(left, top, right, bottom);
        return layoutParams;
    }

    public static class Builder {
        private Activity activity;
        private View rootView;
        private int size = FrameLayout.LayoutParams.WRAP_CONTENT;
        private int defaultTop = 0;
        private int defaultLeft = 0;
        private int defaultBottom = 0;
        private int defaultRight = 0;
        private boolean needNearEdge = false;
        private View view;

        public Builder setActivity(Activity activity) {
            this.activity = activity;
            return this;
        }

        public Builder setRootView(View rootView){
            this.rootView = rootView;
            return this;
        }

        public Builder setSize(int size) {
            this.size = size;
            return this;
        }

        public Builder setDefaultTop(int top) {
            this.defaultTop = top;
            return this;
        }

        public Builder setDefaultRight(int Right) {
            this.defaultRight = Right;
            return this;
        }

        public Builder setDefaultBottom(int bottom) {
            this.defaultBottom = bottom;
            return this;
        }

        public Builder setDefaultLeft(int left) {
            this.defaultLeft = left;
            return this;
        }

        public Builder setNeedNearEdge(boolean needNearEdge) {
            this.needNearEdge = needNearEdge;
            return this;
        }

        public Builder setView(View view) {
            this.view = view;
            return this;
        }

        public SonnyJackDragView build() {
            return createDragView(this);
        }
    }
}
