package com.feldschmid.subdroid.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.feldschmid.subdroid.R;
import com.feldschmid.subdroid.pref.Prefs;

public class ThemeUtil {

	public static String LIGHT = "Light";
	public static String LIGHT_BIGGER = "Light - bigger revision text";
	public static String LIGHT_NO_GREEN = "Light - no highlight";
	public static String BLACK = "Black";

	public static String currentTheme;

	/**
	 * Should be called once the activity starts or if the currentTheme is null
	 * @param act the activity
	 */
	public static void readPref(Activity act) {
		SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(act);

		if(Build.VERSION.SDK_INT >= 11) {
		  currentTheme = sPref.getString(Prefs.THEME, BLACK);
        } else {
          currentTheme = sPref.getString(Prefs.THEME, LIGHT_BIGGER);
        }
	}

	public static void setTheme(Activity act) {
		if(currentTheme == null) {
			readPref(act);
		}
		if(BLACK.equals(currentTheme))  {
			act.setTheme(R.style.ThemeBlack);
		}
		else if(LIGHT_NO_GREEN.equals(currentTheme)) {
			act.setTheme(R.style.ThemeLightNoGreen);
		}
		else if(LIGHT_BIGGER.equals(currentTheme)) {
		  act.setTheme(R.style.ThemeLightBigger);
		}
		else {
			act.setTheme(R.style.ThemeLight);
		}
	}

}
