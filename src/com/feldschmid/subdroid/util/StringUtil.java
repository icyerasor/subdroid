package com.feldschmid.subdroid.util;

public class StringUtil {

	public static CharSequence trimPathToName(String href) {
		if(href.endsWith("/") && href.endsWith("\\")) {
			return href;
		}
		
		int pos =  href.lastIndexOf('/');
		if(pos != -1) {
			return href.substring(pos+1);
		}
		pos = href.lastIndexOf('\\');
		if(pos != -1) {
			return href.substring(pos+1);
		}
		return href;
	}
	
	public static String previousRevision(String revision) {
		int value = Integer.valueOf(revision).intValue();
		if(value > 1) {
			return String.valueOf(value-1);
		}
		return String.valueOf(value);
	}
	
}
