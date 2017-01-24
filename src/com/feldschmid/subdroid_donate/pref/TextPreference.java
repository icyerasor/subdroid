package com.feldschmid.subdroid_donate.pref;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.feldschmid.subdroid_donate.R;

public class TextPreference extends Preference {
	
	private static int padding = 8;
		
	public TextPreference(Context context) {
		super(context);
	}

	public TextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public TextPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected View onCreateView(ViewGroup parent) {
		padding = getContext().getResources().getDimensionPixelSize(R.dimen.preference_padding_left);
		return super.onCreateView(parent);
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		
		LinearLayout ll = (LinearLayout) view;
		ll.removeAllViews();
		ll.setPadding(padding, 0, 0, 0);
		
		LayoutParams layoutParams = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		TextView tv = new TextView(this.getContext());
		tv.setLayoutParams(layoutParams);
		tv.setText(getContext().getString(R.string.paid_notice));
		tv.setGravity(Gravity.CENTER);
		ll.addView(tv, layoutParams);
	}
}
