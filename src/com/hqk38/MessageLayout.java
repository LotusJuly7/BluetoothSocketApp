package com.hqk38;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MessageLayout extends LinearLayout {
	public MessageLayout(Context context, String nick_name, String message, boolean isSend) {
		super(context);
		setOrientation(VERTICAL);
		float scale = this.getResources().getDisplayMetrics().density;
		if (isSend) {
			TextView text1 = new TextView(context);
			LayoutParams lp1 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp1.topMargin = (int) (10f * scale);
			lp1.rightMargin = (int) (14f * scale);
			lp1.gravity = Gravity.RIGHT;
			text1.setIncludeFontPadding(false);
			text1.setTextColor(0xff878b99);
			text1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
			text1.setText(nick_name);
			addView(text1, lp1);
			
			TextView text2 = new TextView(context);
			LayoutParams lp2 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp2.bottomMargin = (int) (2f * scale);
			lp2.gravity = Gravity.RIGHT;
			text2.setMinWidth((int) (65 * scale));
			text2.setMinHeight((int) (44f * scale));
			text2.setBackgroundResource(R.drawable.ef);
			text2.setPadding((int) (22f * scale), (int) (17f * scale), (int) (22f * scale), (int) (17f * scale));
			text2.setTextColor(0xffffffff);
			text2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f);
			text2.setText(message);
			text2.setClickable(true);
			text2.setTextIsSelectable(true);
			addView(text2, lp2);
		} else {
			TextView text1 = new TextView(context);
			LayoutParams lp1 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp1.topMargin = (int) (10f * scale);
			lp1.leftMargin = (int) (14f * scale);
			text1.setIncludeFontPadding(false);
			text1.setTextColor(0xff878b99);
			text1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
			text1.setText(nick_name);
			addView(text1, lp1);
			
			TextView text2 = new TextView(context);
			LayoutParams lp2 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp2.bottomMargin = (int) (2f * scale);
			text2.setMinWidth((int) (65 * scale));
			text2.setMinHeight((int) (44f * scale));
			text2.setBackgroundResource(R.drawable.ao);
			text2.setPadding((int) (22f * scale), (int) (17f * scale), (int) (22f * scale), (int) (17f * scale));
			text2.setTextColor(0xff03081a);
			text2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f);
			text2.setText(message);
			text2.setClickable(true);
			text2.setTextIsSelectable(true);
			addView(text2, lp2);
		}
	}
	
	boolean isFirst = true;
	@Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (isFirst) { // 来消息时，滚动到底部
			LinearLayout listView1 = (LinearLayout) getParent();
			ScrollView listScroll = (ScrollView) listView1.getParent();
			listScroll.smoothScrollTo(0, Math.max(listView1.getHeight() - listScroll.getHeight(), 0));
			isFirst = false;
		}
	}
}
