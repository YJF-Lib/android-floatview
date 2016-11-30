package com.test.jfyuan.floatview;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.lang.reflect.Field;

/**
 * Created by JF on 2016/8/9.
 */
public class FloatView extends LinearLayout {
    private Activity activity;
    private WindowManager.LayoutParams wmLayoutParams;
    private WindowManager windowManager;
    private boolean canTouchMove;
    private float xInScreen;
    private float yInScreen;
    private float xDownInScreen;
    private float yDownInScreen;
    private int statusBarHeight;
    private Point position = new Point(0, 0);
    private AddedListener addedListener;
    private RemovedListener removedListener;

    public interface AddedListener {
        void onAddedToWindow(int x, int y);
    }

    public interface RemovedListener {
        void onRemovedToWindow(int x, int y);
    }

    public FloatView(Context context) {
        super(context);
        init();
    }

    private void init() {
        windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        wmLayoutParams = new WindowManager.LayoutParams();
        wmLayoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        wmLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmLayoutParams.format = PixelFormat.RGBA_8888;
        wmLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
    }

    public void setWidth(int width) {
        if (null != wmLayoutParams)
            wmLayoutParams.width = width;
    }

    public void setHeight(int height) {
        if (null != wmLayoutParams)
            wmLayoutParams.height = height;
    }

    public boolean addToWindow() {
        boolean result;
        if (windowManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (!isAttachedToWindow()) {
                    windowManager.addView(this, wmLayoutParams);
                    result = true;
                } else {
                    result = false;
                }
            } else {
                try {
                    if (getParent() == null) {
                        windowManager.addView(this, wmLayoutParams);
                    }
                    result = true;
                } catch (Exception e) {
                    result = false;
                }
            }
        } else {
            result = false;
        }
        if (result && null != addedListener) {
            addedListener.onAddedToWindow(getPosition().x, getPosition().y);
        }
        return result;
    }

    public boolean removeFromWindow() {
        boolean result;
        if (windowManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (isAttachedToWindow()) {
                    windowManager.removeViewImmediate(this);
                    result = true;
                } else {
                    result = false;
                }
            } else {
                try {
                    if (getParent() != null) {
                        windowManager.removeViewImmediate(this);
                    }
                    result = true;
                } catch (Exception e) {
                    result = false;
                }
            }
        } else {
            result = false;
        }
        if (result && null != removedListener) {
            removedListener.onRemovedToWindow(getPosition().x, getPosition().y);
        }
        return result;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isCanTouchMove()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    xDownInScreen = event.getRawX();
                    yDownInScreen = event.getRawY() - getStatusBarHeight();
                    break;
                case MotionEvent.ACTION_MOVE:
                    xInScreen = event.getRawX();
                    yInScreen = event.getRawY() - getStatusBarHeight();
                    float dx = Math.abs(xInScreen - xDownInScreen);
                    float dy = Math.abs(yInScreen - yDownInScreen);
                    if (dx < 10f && dy < 10f) {
                        return false;
                    } else {
                        //拦截，交给自己的onTouch事件处理
                        return true;
                    }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    xDownInScreen = 0;
                    yDownInScreen = 0;
                    break;
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isCanTouchMove()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    xInScreen = event.getRawX();
                    yInScreen = event.getRawY() - getStatusBarHeight();
                    break;
                case MotionEvent.ACTION_MOVE:
                    xInScreen = event.getRawX();
                    yInScreen = event.getRawY() - getStatusBarHeight();
                    wmLayoutParams.x = (int) (xInScreen - getWidth() / 2);
                    wmLayoutParams.y = (int) (yInScreen - getHeight() / 2);
                    updatePosition();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    updatePosition();
//                    alignSide();
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        alignSide();
    }

    private void alignSide() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        //以屏幕中点为原点，横向为X轴，纵向为Y轴计算
        if (xInScreen < width / 2) {//第二、三象限
            if (yInScreen < height / 2) {//第二象限
                if (xInScreen < yInScreen) {
                    wmLayoutParams.x = 0;
                } else {
                    wmLayoutParams.y = 0;
                }
            } else {//第三象限
                if (xInScreen < height - yInScreen) {
                    wmLayoutParams.x = 0;
                } else {
                    wmLayoutParams.y = height;
                }
            }
        } else {//第一、四象限
            if (yInScreen < height / 2) {//第一象限
                if (width - xInScreen < yInScreen) {
                    wmLayoutParams.x = width;
                } else {
                    wmLayoutParams.y = 0;
                }
            } else {//第四象限
                if (width - xInScreen < height - yInScreen) {
                    wmLayoutParams.x = width;
                } else {
                    wmLayoutParams.y = height;
                }
            }
        }
        updatePosition();
    }

    private void updatePosition() {
        windowManager.updateViewLayout(this, wmLayoutParams);
    }

    public void updatePosition(int x, int y) {
        if (x >= 0)
            wmLayoutParams.x = x;
        if (y >= 0)
            wmLayoutParams.y = y;
        // 刷新
        updatePosition();
    }

    public Point getPosition() {
        position.x = wmLayoutParams.x > 0 ? wmLayoutParams.x : 0;
        position.y = wmLayoutParams.y > 0 ? wmLayoutParams.y : 0;
        return position;
    }

    /**
     * 用于获取状态栏的高度。
     *
     * @return 返回状态栏高度的像素值。
     */
    private int getStatusBarHeight() {
        if (statusBarHeight == 0) {
            try {
                Class<?> c = Class.forName("com.android.internal.R$dimen");
                Object o = c.newInstance();
                Field field = c.getField("status_bar_height");
                int x = (Integer) field.get(o);
                statusBarHeight = getResources().getDimensionPixelSize(x);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return statusBarHeight;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public boolean isCanTouchMove() {
        return canTouchMove;
    }

    public void setCanTouchMove(boolean canTouchMove) {
        this.canTouchMove = canTouchMove;
    }

    public AddedListener getAddedListener() {
        return addedListener;
    }

    public void setAddedListener(AddedListener addedListener) {
        this.addedListener = addedListener;
    }

    public RemovedListener getRemovedListener() {
        return removedListener;
    }

    public void setRemovedListener(RemovedListener removedListener) {
        this.removedListener = removedListener;
    }
}
