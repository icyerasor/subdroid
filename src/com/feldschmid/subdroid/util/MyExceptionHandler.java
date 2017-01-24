package com.feldschmid.subdroid.util;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.util.Log;

public class MyExceptionHandler {

	public static void handle(Context ctx, Exception e) {
		if (e != null && e.getMessage()!= null && e.getMessage().contains("Not trusted server certificate")) {
			Builder builder = new AlertDialog.Builder(ctx);
			builder
					.setTitle("Error")
					.setMessage(
							"Server certificate is invalid. Probably self signed certificiate. "
									+ "You might need to set to ignore SSL certificates in the options dialog.")
					.setPositiveButton("OK", null).show();
		} else {
			String message;
			if(e.getMessage() == null || e.getMessage().equals("")) {
				message = e.toString();
			}
			else {
				message = e.getMessage();
			}
			Builder builder = new AlertDialog.Builder(ctx);
			builder.setTitle("Error").setMessage(message)
					.setPositiveButton("OK", null).show();
		}
		Log.e("Subdroid", e.toString());
		Log.e("Subdroid", Log.getStackTraceString(e));
	}
}
