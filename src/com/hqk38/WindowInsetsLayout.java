package com.hqk38;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class WindowInsetsLayout extends FrameLayout {

	public WindowInsetsLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected boolean fitSystemWindows(Rect insets) {
        insets.top = 0;
        super.fitSystemWindows(insets);
        return false;
    }
}
