package com.feldschmid.subdroid_donate.util;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.feldschmid.subdroid_donate.R;
import com.feldschmid.svn.model.Props;

public class BrowsePropAdapter extends ArrayAdapter<Props> {

	private LayoutInflater mInflater;

	public BrowsePropAdapter(Context context, int resourceId, List<Props> data) {
		super(context, resourceId, data);
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        if (convertView == null) {
            view = mInflater.inflate(R.layout.browse_row, parent, false);
        } else {
            view = convertView;
        }

        Props props = getItem(position);

        // fill view by mapping props attributes to it
        TextView version = (TextView) view.findViewById(R.id.browse_revision);
        version.setText(props.getVersion());

        TextView date = (TextView) view.findViewById(R.id.browse_date);

        if(props.getLastModifiedDateString()!= null && props.getLastModifiedDateString().length() > 2) {
          date.setText(props.getLastModifiedDateString().substring(0, props.getLastModifiedDateString().length()-3));
        }

        TextView author = (TextView) view.findViewById(R.id.browse_author);
        author.setText(props.getAuthor());

        TextView name = (TextView) view.findViewById(R.id.browse_name);
        name.setText(StringUtil.trimPathToName(props.getHref()));

        return view;
	}


}
