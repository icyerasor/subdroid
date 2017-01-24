package com.feldschmid.subdroid.pref;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.feldschmid.subdroid.R;
import com.feldschmid.subdroid.Subdroid;

public class TextPreference extends Preference{

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
		if(Subdroid.PAID_VERSION) {
			return new LinearLayout(this.getContext());
		}
		else {
			return super.onCreateView(parent);
		}
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		
		LinearLayout ll = (LinearLayout) view;
		ll.removeAllViews();
		
		if(Subdroid.PAID_VERSION) {
			ll.setVisibility(View.GONE);
		}
		
		LayoutParams layoutParams = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		TextView tv = new TextView(this.getContext());
		tv.setLayoutParams(layoutParams);
		tv.setText(getContext().getString(R.string.paid_notice));
		tv.setGravity(Gravity.CENTER);
		ll.addView(tv, layoutParams);
	}
}
