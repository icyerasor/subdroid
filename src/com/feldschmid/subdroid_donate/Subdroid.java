package com.feldschmid.subdroid_donate;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.feldschmid.subdroid_donate.db.ChangeDbAdapter;
import com.feldschmid.subdroid_donate.db.RepositoryDbAdapter;
import com.feldschmid.subdroid_donate.db.RevisionDbAdapter;
import com.feldschmid.subdroid_donate.pref.Prefs;
import com.feldschmid.subdroid_donate.service.CheckUpdateService;
import com.feldschmid.subdroid_donate.util.RepositoryDataAdapter;
import com.feldschmid.subdroid_donate.util.ThemeUtil;

public class Subdroid extends ListActivity {

	public static final boolean PAID_VERSION = true;

    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;

    private RepositoryDbAdapter mRepoDbHelper;
    private RevisionDbAdapter mRevDbHelper;
	private ChangeDbAdapter mChangeDbHelper;

	private Cursor repoCursor;

	private static boolean SERVICE_RUNNING;
	private static boolean SHOW_SERVICE_NOTIFICATION;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ThemeUtil.readPref(this);
		ThemeUtil.setTheme(this);
		setContentView(R.layout.main);
        mRepoDbHelper = new RepositoryDbAdapter(this);
        mRepoDbHelper.open();
        fillData();

        // enable longPress Menu
        registerForContextMenu(getListView());
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopManagingCursor(this.repoCursor);
		mRepoDbHelper.close();
	}

	@Override
    protected void onResume() {
        super.onResume();

        mRepoDbHelper.open();

        fillData();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SERVICE_RUNNING = prefs.getBoolean(CheckUpdateService.SERVICE_RUNNING_KEY, false);
		SHOW_SERVICE_NOTIFICATION = prefs.getBoolean(Prefs.SHOW_SERVICE_NOTIFICATION, true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.repository_menu, menu);
    	return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
	    menu.removeItem(R.id.repository_menu_stop_check_update_service);
	    menu.removeItem(R.id.repository_menu_start_check_update_service);

	    if(PAID_VERSION) {
	    	if(SERVICE_RUNNING) {
				menu.add(0, R.id.repository_menu_stop_check_update_service, 2,
						R.string.repository_menu_stop_check_update_service).setIcon(android.R.drawable.ic_menu_recent_history);
	    	}
	    	else {
				menu.add(0, R.id.repository_menu_start_check_update_service, 2,
						R.string.repository_menu_start_check_update_service).setIcon(android.R.drawable.ic_menu_recent_history);
	    	}
    	}
		return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case R.id.repository_menu_add:
            createRepository();
            return true;
        case R.id.repository_menu_prefs:
        	startActivity(new Intent(this, Prefs.class));
        	return true;
        case R.id.repository_menu_help:
        	showHelp();
        	return true;
        case R.id.repository_menu_start_check_update_service:
        	startCheckUpdateService();
        	return true;
		case R.id.repository_menu_stop_check_update_service:
			stopCheckUpdateService();
			return true;
        case R.id.repository_menu_search:
          onSearchRequested();
		}

        return super.onMenuItemSelected(featureId, item);
    }

	private void createRepository() {
        Intent i = new Intent(this, RepositoryEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    private void startCheckUpdateService() {
    	CheckUpdateService.scheduleAlarm(this.getApplicationContext());
        SERVICE_RUNNING = true;
        if(SHOW_SERVICE_NOTIFICATION) {
        	CheckUpdateService.setNotification(this.getApplicationContext());
        }
    }

    private void stopCheckUpdateService() {
    	CheckUpdateService.unScheduleAlarm(this.getApplicationContext());
        SERVICE_RUNNING = false;
        CheckUpdateService.removeNotification(this.getApplicationContext());
    }

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.repository_longpress, menu);
	}

    @Override
	public boolean onContextItemSelected(MenuItem item) {
    	final long id = ((AdapterContextMenuInfo) item.getMenuInfo()).id;
		String name = ((TextView) ((LinearLayout) ((AdapterContextMenuInfo) item
				.getMenuInfo()).targetView).getChildAt(0)).getText().toString();
    	Intent i;

		switch(item.getItemId()) {
    	case R.id.repository_longpress_delete:

			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.confirm).setMessage(R.string.confirmDelete)
					.setNegativeButton("Cancel", null).setPositiveButton("OK", new OnClickListener() {
								public void onClick(DialogInterface dialog,	int which) {
									deleteRepository(id);
								}
							}).show();
			break;
    	case R.id.repository_longpress_modify:
            i = new Intent(this, RepositoryEdit.class);
            i.putExtra(RepositoryDbAdapter.KEY_ROWID, id);
            startActivityForResult(i, ACTIVITY_EDIT);
            break;
    	case R.id.repository_longpress_log:
            i = new Intent(this, Revisions.class);
            i.putExtra(RepositoryDbAdapter.KEY_ROWID, id);
            i.putExtra(RepositoryDbAdapter.KEY_NAME, name);
            startActivity(i);
            break;
    	case R.id.repository_longpress_browse:
    		i = new Intent(this, Browse.class);
            i.putExtra(RepositoryDbAdapter.KEY_ROWID, id);
            i.putExtra(RepositoryDbAdapter.KEY_NAME, name);
            startActivity(i);
    		break;
    	case R.id.repository_longpress_copy:
    		mRepoDbHelper.copyRepoInfo(id);
    		fillData();
    		break;
		}
		return super.onContextItemSelected(item);
	}

    private void deleteRepository(long id) {
		mRevDbHelper = new RevisionDbAdapter(this);
		mRevDbHelper.open();
		mRevDbHelper.deleteRevisionForRepositoryId(id);
		mRevDbHelper.close();

		mChangeDbHelper = new ChangeDbAdapter(this);
		mChangeDbHelper.open();
		mChangeDbHelper.deleteChangesForRepositoryId(id);
		mChangeDbHelper.close();

        mRepoDbHelper.deleteRepository(id);
        fillData();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String name = ((TextView)((LinearLayout)v).getChildAt(0)).getText().toString();
        Intent i = new Intent(this, Revisions.class);
        i.putExtra(RepositoryDbAdapter.KEY_ROWID, id);
        i.putExtra(RepositoryDbAdapter.KEY_NAME, name);
        startActivity(i);
    }

    private void fillData() {
        // Get all of the rows from the database and create the item list
    	Cursor repoCursor = mRepoDbHelper.fetchAllRepositories();
    	setRepoCursor(repoCursor);
        startManagingCursor(repoCursor);

        // Create an array to specify the fields we want to display in the list
        String[] from = new String[]{RepositoryDbAdapter.KEY_NAME};

        // and an array of the fields we want to bind those fields to
        int[] to = new int[]{R.id.repository_name};

		// Now create a repository adapter and set it to display
		RepositoryDataAdapter repos = new RepositoryDataAdapter(this,
				R.layout.repository_row, repoCursor, from, to);
		setListAdapter(repos);
    }

    private void showHelp() {
    	startActivity(new Intent(this, About.class));
	}

    private void setRepoCursor(Cursor newCursor) {
      if(this.repoCursor != null) {
        stopManagingCursor(this.repoCursor);
      }
      this.repoCursor = newCursor;
    }
}