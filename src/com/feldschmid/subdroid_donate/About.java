package com.feldschmid.subdroid_donate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.feldschmid.subdroid_donate.util.ThemeUtil;

public class About extends Activity {
	
	private Button mCloseButton;
	private Button mFeedbackButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ThemeUtil.setTheme(this);
		setContentView(R.layout.about);
        
		mFeedbackButton = (Button) findViewById(R.id.about_feedback);
		mFeedbackButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent sendMail = new Intent(android.content.Intent.ACTION_SEND);
				sendMail.setType("plain/text");
				sendMail.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"subdroid@feldschmid.com"});
				sendMail.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subdroid 0.8");
				About.this.startActivity(sendMail);
			}
		});
		
		mCloseButton = (Button) findViewById(R.id.about_close);
		mCloseButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
				
			}
		});
	}
}
