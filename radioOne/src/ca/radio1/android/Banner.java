package ca.radio1.android;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class Banner extends View {

    private final Drawable logo;

    public Banner(Context context) {
        super(context);
        logo = getBackground();
    }

    public Banner(Context context, AttributeSet attrs) {
        super(context, attrs);
        logo = getBackground();
    }

    public Banner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        logo = getBackground();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width * logo.getIntrinsicHeight() / logo.getIntrinsicWidth();

        if (height > logo.getIntrinsicHeight()) {
            setMeasuredDimension(logo.getIntrinsicWidth(), logo.getIntrinsicHeight());
        } else {
            setMeasuredDimension(width, height);
        }
    }
}
