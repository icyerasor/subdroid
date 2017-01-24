package com.feldschmid.subdroid_donate.util;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.feldschmid.subdroid_donate.Browse;
import com.feldschmid.subdroid_donate.db.RepositoryDbAdapter;

public class RepositoryDataAdapter extends SimpleCursorAdapter {

	public RepositoryDataAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);

		setViewBinder(new RevisionDataViewBinder());
	}

	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		if(view != null) {
			ImageView browse = (ImageView) ((LinearLayout)view).getChildAt(1);
			browse.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
		    		Intent i = new Intent(view.getContext(), Browse.class);
		            i.putExtra(RepositoryDbAdapter.KEY_ROWID, ((ListView)parent).getItemIdAtPosition(position));
		            i.putExtra(RepositoryDbAdapter.KEY_NAME, ((TextView)((LinearLayout)view).getChildAt(0)).getText());
		            view.getContext().startActivity(i);
				}
			});
		}
		return view;
	}

}
