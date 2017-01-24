package com.feldschmid.subdroid.util;


import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter.ViewBinder;

import com.feldschmid.subdroid.R;
import com.feldschmid.subdroid.db.RevisionDbAdapter;
import com.feldschmid.svn.model.Action;

public class RevisionDataViewBinder implements ViewBinder {

	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		int nImageIndex = cursor.getColumnIndex(RevisionDbAdapter.KEY_ACTION);
		
		if (nImageIndex == columnIndex) {
			String actions = cursor.getString(nImageIndex);
			
			view.findViewById(R.id.revision_modified).setVisibility(ActionUtil.getVisibility(actions, Action.MODIFY));
			view.findViewById(R.id.revision_added).setVisibility(ActionUtil.getVisibility(actions, Action.ADD));
			view.findViewById(R.id.revision_deleted).setVisibility(ActionUtil.getVisibility(actions, Action.DELETE));
			view.findViewById(R.id.revision_replaced).setVisibility(ActionUtil.getVisibility(actions, Action.REPLACE));
			return true;
		}
		return false;
	}
}
