package com.feldschmid.subdroid_donate.util;

import android.util.Log;

public class UserPw {
	
	public static boolean isConsistent(String user, String pass) {
		// if no user is set, its consistent
		if(user.length() == 0) {
			return true;
		}
		// if user is set and password is set, its consistent
		if(pass.length() > 0) {
			return true;
		}
		Log.d("UserPW", "User and Password not consistent, skipping update!");
		return false;
	}

}
