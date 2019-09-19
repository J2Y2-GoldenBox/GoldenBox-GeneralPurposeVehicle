package com.leehoyoon.computer.goldenbox;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class LoadingView extends LinearLayout {
    public LinearLayout bg;
    public ImageView imageView;

    public LoadingView(Context context) {
        super(context);
        initView();
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
        getAttrs(attrs);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
        getAttrs(attrs, defStyleAttr);
    }

    public void initView(){
        LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(getContext().LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.loading_view, this, false);
        addView(view);

        bg = findViewById(R.id.linearLayout);
        imageView = findViewById(R.id.loadingImageView);
    }

    private void getAttrs(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.LoadingView);
        setTypeArray(typedArray);
    }
    private void getAttrs(AttributeSet attrs, int defStyle) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.LoadingView, defStyle, 0);
        setTypeArray(typedArray);
    }

    private void setTypeArray(TypedArray typedArray){
        int bg_resID = typedArray.getResourceId(R.styleable.LoadingView_bg, Color.WHITE);
        bg.setBackgroundColor(Color.WHITE);

        int imgsrc_reID = typedArray.getResourceId(R.styleable.LoadingView_img, R.drawable.siren);
        imageView.setImageResource(imgsrc_reID);

        typedArray.recycle();
    }

    public void setBg(int res){
        bg.setBackgroundResource(res);
    }

    public void setBgColor(int color){
        bg.setBackgroundColor(color);
    }

    public void setImageView(int src){
        imageView.setImageResource(src);
    }
}
