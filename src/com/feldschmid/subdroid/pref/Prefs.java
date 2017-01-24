package com.feldschmid.subdroid.pref;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.feldschmid.subdroid.R;
import com.feldschmid.subdroid.util.ThemeUtil;

public class Prefs extends PreferenceActivity {
	
	public static final String SHOW_LIGHTS = "show_lights";
	public static final String DEFAULT_SOUND = "default_sound";
	public static final String VIBRATE = "vibrate";
	public static final String UPDATE_INTERVAL = "update_interval_str";
	public static final String BATTERY_FRIENDLY = "battery_friendly";
	public static final String SHOW_SERVICE_NOTIFICATION = "show_service_notification";
	public static final String OPEN_AFTER_DOWNLOAD = "open_after_download";
	public static final String FALLBACK_TO_TEXT_MIMETYPE = "fallback_to_text_mimetype";
	public static final String BACK_BUTTON_NAVIGATE = "back_button_navigate";
	public static final String THEME = "theme";

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);
		ThemeUtil.setTheme(this);
		addPreferencesFromResource(R.layout.prefs);
		// some more special handling to set the background color right. The
		// problem here is, that when the theme is applied programmatically, the
		// background color is not changed. In other activities the background
		// is changed by specifying a style in the xmls, but this is not
		// possible here.
		getListView().setCacheColorHint(Color.TRANSPARENT);
		if(ThemeUtil.BLACK.equals(ThemeUtil.currentTheme)) {
			getWindow().setBackgroundDrawable(getResources().getDrawable(android.R.drawable.screen_background_dark));
			//might be handy for other themes:
//			getListView().setBackgroundColor(Color.BLACK);
		}
		else {
			getWindow().setBackgroundDrawable(getResources().getDrawable(android.R.drawable.screen_background_light));
		}
	}
	
	
}
