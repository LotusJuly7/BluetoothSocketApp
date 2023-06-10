package com.hqk38;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class StatusBarInsetsLayout extends LinearLayout {

	public StatusBarInsetsLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected boolean fitSystemWindows(Rect insets) {
        insets.bottom = 0;
        super.fitSystemWindows(insets);
        return false;
    }
}
