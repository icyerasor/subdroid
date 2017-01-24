package com.feldschmid.subdroid_donate.util;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.feldschmid.subdroid_donate.R;

public class RevisionDataAdapter extends SimpleCursorAdapter {

	public RevisionDataAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);

		setViewBinder(new RevisionDataViewBinder());
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// when converting an old view, make sure to remove all changesets first!
		if (convertView != null) {
			TextView changes = (TextView) convertView
					.findViewById(R.id.internal);
			if (changes != null) {
				while (changes != null) {
					LinearLayout ll = (LinearLayout) changes.getParent();
					ll.removeView(changes);
					changes = (TextView) ll.findViewById(R.id.internal);
				}
			}
		}
		return super.getView(position, convertView, parent);
	}


}
