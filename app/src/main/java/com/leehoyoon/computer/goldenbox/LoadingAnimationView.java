package com.leehoyoon.computer.goldenbox;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LoadingAnimationView extends FrameLayout {
    private static Context context;
    private static LoadingView loadingView1 = null;
    private static LoadingView loadingView2 = null;
    private static LoadingView loadingView3 = null;
    private static LoadingView loadingView4 = null;
    private static long animationDuring = 1000;
    private static float startLocation = 530;
    private static float endLocation=0;
    private boolean animationFlag = false;
    private AnimationThread animationThread;

    public LoadingAnimationView(@NonNull Context context) {
        super(context);
        this.context = context;
        initLoadingView();
        startAnimation();
    }

    public LoadingAnimationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initLoadingView();
        startAnimation();
    }

    public LoadingAnimationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initLoadingView();
        startAnimation();
    }

    public void initLoadingView(){
        LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(getContext().LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.loading_animation_view, this, false);
        addView(view);

        loadingView1 = findViewById(R.id.loadingView1);
        loadingView1.setImageView(R.drawable.siren);
        loadingView1.setBackground(new ShapeDrawable(new OvalShape()));
        loadingView1.setClipToOutline(true);
        loadingView1.setBgColor(Color.RED);
        loadingView1.setTranslationX(startLocation);

        loadingView2 = findViewById(R.id.loadingView2);
        loadingView2.setImageView(R.drawable.siren);
        loadingView2.setBackground(new ShapeDrawable(new OvalShape()));
        loadingView2.setClipToOutline(true);
        loadingView2.setBgColor(Color.YELLOW);
        loadingView2.setTranslationY(startLocation);

        loadingView3 = findViewById(R.id.loadingView3);
        loadingView3.setImageView(R.drawable.siren);
        loadingView3.setBackground(new ShapeDrawable(new OvalShape()));
        loadingView3.setClipToOutline(true);
        loadingView3.setBgColor(Color.GREEN);
        loadingView3.setTranslationX(startLocation);

        loadingView4 = findViewById(R.id.loadingView4);
        loadingView4.setImageView(R.drawable.siren);
        loadingView4.setBackground(new ShapeDrawable(new OvalShape()));
        loadingView4.setClipToOutline(true);
        loadingView4.setBgColor(Color.BLUE);
    }

    public void startAnimation(){
        animationThread = new AnimationThread();
        animationThread.start();
        animationFlag = true;
    }

    public void stopAnimation(){
        animationThread.interrupt();
        animationFlag = false;
    }

    public void resumeAnimation(){
        animationThread.notify();
        animationFlag = true;
    }

    public boolean checkAnimation(){
        return animationFlag;
    }

    class AnimationThread extends Thread {
        public AnimationThread() {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    startAnimation1(context);
                }
            });
        }
    }

    public static void startAnimation1(final Context context){
        loadingView1.animate().translationX(endLocation)
                .setDuration(animationDuring)
                .setInterpolator(AnimationUtils.loadInterpolator(context, android.R.anim.decelerate_interpolator))
                .withEndAction(new Runnable() {
            @Override
            public void run() {
                loadingView2.bringToFront();
                loadingView2.setTranslationY(startLocation);
                startAnimation2(context);
            }
        }).start();
    }

    public static void startAnimation2(final Context context){
        loadingView2.animate().
                translationY(endLocation).
                setDuration(animationDuring).
                setInterpolator(AnimationUtils.loadInterpolator(context, android.R.anim.decelerate_interpolator)).
                withEndAction(new Runnable() {
            @Override
            public void run() {
                loadingView3.bringToFront();
                loadingView3.setTranslationX(startLocation);
                startAnimation3(context);
            }
        }).start();
    }

    public static void startAnimation3(final Context context){
        loadingView3.animate()
                .translationX(endLocation)
                .setDuration(animationDuring)
                .setInterpolator(AnimationUtils.loadInterpolator(context, android.R.anim.decelerate_interpolator))
                .withEndAction(new Runnable() {
            @Override
            public void run() {
                loadingView4.bringToFront();
                loadingView4.setTranslationY(startLocation);
                startAnimation4(context);
            }
        }).start();
    }

    public static void startAnimation4(final Context context){
        loadingView4.animate()
                .translationY(endLocation)
                .setDuration(animationDuring)
                .setInterpolator(AnimationUtils.loadInterpolator(context, android.R.anim.decelerate_interpolator))
                .withEndAction(new Runnable() {
            @Override
            public void run() {
                loadingView1.bringToFront();
                loadingView1.setTranslationX(startLocation);
                startAnimation1(context);
            }
        }).start();
    }

    public void setDuring(int during){
        animationDuring = (long)during;
    }
}
