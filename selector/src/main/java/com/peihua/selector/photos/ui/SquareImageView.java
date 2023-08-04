package com.peihua.selector.photos.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.fz.imageloader.widget.RatioImageView;

/**
 * Ensures that imageView is always square.
 */
public class SquareImageView extends RatioImageView {
    public SquareImageView(Context context) {
        super(context);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
