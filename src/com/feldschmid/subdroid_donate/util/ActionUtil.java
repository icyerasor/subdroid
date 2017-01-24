package com.feldschmid.subdroid_donate.util;

import android.view.View;

import com.feldschmid.subdroid_donate.R;
import com.feldschmid.svn.model.Action;


public class ActionUtil {

	public static int getImageResourceForActionString(String actionString) {
		for(Action action : Action.values()) {
			if(action.toString().equals(actionString)) {
				switch (action) {
				case ADD:
					return R.drawable.added;
				case MODIFY:
					return R.drawable.modified;
				case REPLACE:
					return R.drawable.replaced;
				case DELETE:
					return R.drawable.deleted;
				default:
					//todo: this should never happen..
					return 0;
				}
			}
		}
		//todo: this should never happen..
		return 0;
	}

	public static int getVisibility(String actions, Action action) {
		if(actions.contains(action.toString())) {
			return View.VISIBLE;
		}
		return View.INVISIBLE;
	}
}
