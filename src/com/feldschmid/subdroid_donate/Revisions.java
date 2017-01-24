package com.feldschmid.subdroid_donate;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.method.PasswordTransformationMethod;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.feldschmid.subdroid_donate.db.ChangeDbAdapter;
import com.feldschmid.subdroid_donate.db.RepositoryDbAdapter;
import com.feldschmid.subdroid_donate.db.RevisionDbAdapter;
import com.feldschmid.subdroid_donate.pref.Prefs;
import com.feldschmid.subdroid_donate.util.ActionUtil;
import com.feldschmid.subdroid_donate.util.Const;
import com.feldschmid.subdroid_donate.util.DownloadFile;
import com.feldschmid.subdroid_donate.util.FileAlreadyExistsException;
import com.feldschmid.subdroid_donate.util.FileHelper;
import com.feldschmid.subdroid_donate.util.MyExceptionHandler;
import com.feldschmid.subdroid_donate.util.RevisionDataAdapter;
import com.feldschmid.subdroid_donate.util.StringUtil;
import com.feldschmid.subdroid_donate.util.ThemeUtil;
import com.feldschmid.subdroid_donate.util.UserPw;
import com.feldschmid.svn.ReportRetriever;
import com.feldschmid.svn.base.MyException;
import com.feldschmid.svn.cmd.Get;
import com.feldschmid.svn.cmd.Propfind;
import com.feldschmid.svn.model.Action;
import com.feldschmid.svn.model.ChangedPath;
import com.feldschmid.svn.model.LogItem;
import com.feldschmid.svn.model.ReportList;

public class Revisions extends ListActivity {

    private static int padding = 4;
	private static int inputBoxWidth = 200;

	private RevisionDbAdapter mRevisionDbHelper;
	private RepositoryDbAdapter mRepoDbHelper;
	private ChangeDbAdapter mChangeDbHelper;
	private Long mRepositoryId;
	private String mRepositoryName;
	private ReportList mReportList;
	private ProgressDialog mProgDialog;

	private String limit;
	private String url;
	private String user;
	private String ignoreSSL;
	private String retrieveCP;
	private String host;
	private String pass;
	private boolean openAfterDownload;
	private boolean fallbackToTextMimeType;

	private Cursor repoCursor;

	private static final String KEY_SEARCH_QUERY = "SEARCH_QUERY";
    private static final String LIST_STATE = "liststate_rev";

    private Parcelable mListState = null;
    private String mSearchQuery = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ThemeUtil.setTheme(this);
		setContentView(R.layout.revisions);

		padding = this.getResources().getDimensionPixelSize(R.dimen.image_padding);
		inputBoxWidth = this.getResources().getDimensionPixelSize(R.dimen.inputBox);


		mRevisionDbHelper = new RevisionDbAdapter(this);
		mRepoDbHelper = new RepositoryDbAdapter(this);
		mChangeDbHelper = new ChangeDbAdapter(this);

		mRevisionDbHelper.open();
		mRepoDbHelper.open();
		mChangeDbHelper.open();

        registerForContextMenu(getListView());

        // if this is a previous search action, fill data and return
        mSearchQuery = savedInstanceState != null ? savedInstanceState
            .getString(KEY_SEARCH_QUERY) : null;
        if(mSearchQuery != null) {
          fillData(mRevisionDbHelper.search(mSearchQuery));
          setTitle(getString(R.string.search_for)+" "+mSearchQuery);
          return;
        }

	    // if this is an search, retrieve search key, fill data and return
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            mSearchQuery = getIntent().getStringExtra(SearchManager.QUERY);
            fillData(mRevisionDbHelper.search(mSearchQuery));
            setTitle(MessageFormat.format(getString(R.string.search_for).toString(), mSearchQuery));
            TextView emptyText = (TextView) findViewById(android.R.id.empty);
            emptyText.setText(MessageFormat.format(getText(R.string.no_search_results).toString(), mSearchQuery));
            return;
        }



		// try to get previously shown repositoryId
		mRepositoryId = savedInstanceState != null ? savedInstanceState
				.getLong(RepositoryDbAdapter.KEY_ROWID) : null;
		// if none is available, get fresh repo id from intent
		if (mRepositoryId == null) {
			Bundle extras = getIntent().getExtras();
			mRepositoryId = extras != null ? extras
					.getLong(RepositoryDbAdapter.KEY_ROWID) : null;
		}

		// try to get previously shown repositoryName
        mRepositoryName = savedInstanceState != null ? savedInstanceState
            .getString(RepositoryDbAdapter.KEY_NAME) : null;
         // if none is available, get fresh repo name from intent
        if (mRepositoryName == null) {
            Bundle extras = getIntent().getExtras();
            mRepositoryName = extras != null ? extras
                    .getString(RepositoryDbAdapter.KEY_NAME) : null;
        }
        if(mRepositoryName != null && mSearchQuery == null) {
          setTitle(mRepositoryName);
        }

		//refresh is currently only available through menu
//		Button refresh = (Button) findViewById(R.id.revisions_refresh);
//		refresh.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				refreshRevisions(null);
//			}
//		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopManagingCursor(repoCursor);
		mRevisionDbHelper.close();
		mRepoDbHelper.close();
		mChangeDbHelper.close();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mRevisionDbHelper.open();
		mRepoDbHelper.open();
		mChangeDbHelper.open();

		// if this is no search
		if(mSearchQuery == null) {
		  setupRepositoryDetails();
          fillData(mRevisionDbHelper.fetchRevisionsForRepositoryId(mRepositoryId));
		} else {
		  fillData(mRevisionDbHelper.search(mSearchQuery));
		}

		if (mListState != null) {
            getListView().onRestoreInstanceState(mListState);
        }
        mListState = null;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        openAfterDownload = prefs.getBoolean(Prefs.OPEN_AFTER_DOWNLOAD, true);
        fallbackToTextMimeType = prefs.getBoolean(Prefs.FALLBACK_TO_TEXT_MIMETYPE, true);
	}

    private void setupRepositoryDetails() {
      Cursor repo = mRepoDbHelper.fetchRepository(mRepositoryId);
      host = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_BASEURL));
      url = host + repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_PATH));
      limit = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_LIMIT));
      user = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_USER));
      pass = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_PASS));
      ignoreSSL = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_IGNORE_SSL));
      retrieveCP = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_RETRIEVE_CHANGED_PATHS));
      repo.close();
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		LinearLayout revisionRowLayout = (LinearLayout) v.findViewById(R.id.revision_row_layout);

		// retrieve and set current repId
		TextView repId = (TextView) revisionRowLayout.findViewById(R.id.revision_fk_repo_id);
		mRepositoryId = Long.valueOf(repId.getText().toString());

		// and also re-set all the info for the repository
		setupRepositoryDetails();

		TextView changes = (TextView) revisionRowLayout.findViewById(R.id.internal);

		if(changes != null) {
			while(changes != null) {
				revisionRowLayout.removeView(changes);
				changes = (TextView) revisionRowLayout.findViewById(R.id.internal);
			}
		}
		else {

			Cursor c = mChangeDbHelper.fetchChangesForRevisionId(mRepositoryId,
					mRevisionDbHelper.getRevisionByRowId(id));
			if (c.getCount() > 0) {
				int mPathIndex = c.getColumnIndex(ChangeDbAdapter.KEY_PATH);
				int mActionIndex = c.getColumnIndex(ChangeDbAdapter.KEY_ACTION);
				int mRevisionIndex = c.getColumnIndex(ChangeDbAdapter.KEY_FK_REVISIONID);

				LayoutParams layoutParams = new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

				while (!c.isAfterLast()) {

					TextView tv = new TextView(this);
					final String filePath = c.getString(mPathIndex);
					final String revision = c.getString(mRevisionIndex);

					// Linkify.addLinks(tv, Pattern.compile(".*"), host);
					// as linkyfiy would interfere with the onclick listener of the linearlayout, we use manual links
					SpannableString content = new SpannableString(filePath);
					content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
					tv.setText(content);
					//tv.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
					tv.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							doOnChangedPathClicked(revision, filePath, mRepositoryId);
						}
					});
					tv.setTextAppearance(this, R.style.pathTextLinked);


					// set action icon
					String actionString = c.getString(mActionIndex);
					tv.setCompoundDrawablesWithIntrinsicBounds(ActionUtil
							.getImageResourceForActionString(actionString), 0,
							0, 0);
					tv.setCompoundDrawablePadding(padding);

					// add tv to layout
					tv.setId(R.id.internal);
					revisionRowLayout.addView(tv, layoutParams);
					c.moveToNext();
				}
			}
			c.close();
		}
	}

	public void doOnChangedPathClicked(final String revision, final String filePath, final long repIdOfRevision) {

	  // in case of a search, the host and mRepositoryName is not initialized yet!
	  if(host == null || mRepositoryName == null) {
	    Cursor repo = mRepoDbHelper.fetchRepository(repIdOfRevision);
        host = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_BASEURL));
        mRepositoryName = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_NAME));
        repo.close();
	  }

		String[] options = new String[] {
				getString(R.string.download_head),
				getString(R.string.download_revision) + " " + revision,
				getString(R.string.download_revision) + " " + StringUtil.previousRevision(revision),
				getString(R.string.open_head),
				getString(R.string.open_revision) + " " + revision,
				getString(R.string.open_revision) + " " + StringUtil.previousRevision(revision) };
		Builder builder = new AlertDialog.Builder(Revisions.this);
		builder.setTitle(getString(R.string.choose_action))
				.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							doDownload(host+filePath, false);
							break;
						case 1:
							doDownload(host+Const.SVN_VERSION_PART+revision+filePath, false);
							break;
						case 2:
							doDownload(host+Const.SVN_VERSION_PART+StringUtil.previousRevision(revision)+filePath, false);
							break;
						case 3:
							doOpen(host+filePath);
							break;
						case 4:
							doOpen(host+Const.SVN_VERSION_PART+revision+filePath);
							break;
						case 5:
							doOpen(host+Const.SVN_VERSION_PART+StringUtil.previousRevision(revision)+filePath);
							break;
						default:
							doOpen(host+filePath);
							break;
						}
						dialog.dismiss();
					}
				}).show();
	}


	private void doOpen(String url) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
	}

	public void doDownload(final String url, final boolean force) {
		final Thread t = new Thread() {
			@Override
      public void run() {
				final Get get = new Get(url, user, pass, Boolean.parseBoolean(ignoreSSL));
				DownloadFile dFile = null;
				try {
					dFile = FileHelper.save(get, mRepositoryName+mRepositoryId, force);
				} catch (FileAlreadyExistsException e) {
					Message m = Message.obtain();
					m.obj = e;
					m.what = Const.downloadFileExists;
					handler.sendMessage(m);
					return;
				} catch (IOException e) {
					Message m = Message.obtain();
					m.obj = e;
					handler.sendMessage(m);
					return;
				} catch (MyException e) {
					Message m = Message.obtain();
					m.obj = e;
					handler.sendMessage(m);
					return;
				}

				if(dFile != null && openAfterDownload) {
					try {
						startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(dFile.getData(), dFile.getMimeType()));
					} catch(ActivityNotFoundException e) {
						if(fallbackToTextMimeType) {
							startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(dFile.getData(), "text/*"));
							handler.sendEmptyMessage(Const.downloadOK);
							return;
						}
						handler.sendEmptyMessage(Const.downloadOKActivityNotFound);
						return;
					}
				}
				handler.sendEmptyMessage(Const.downloadOK);
			}
		};

        // if an user was entered, but no password provided
		if (!UserPw.isConsistent(user, pass)) {
			// ask for the password every time
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			final EditText input = new EditText(this);
			input.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
			input.setTransformationMethod(new PasswordTransformationMethod());
			input.setWidth(inputBoxWidth);
			alert.setView(input);

			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							pass = input.getText().toString();
							mProgDialog = ProgressDialog.show(Revisions.this, getString(R.string.working), getString(R.string.downloading_file), true, false);
							t.start();
						}
					});
			alert.setTitle(R.string.pass);
			alert.show();
		} else {
			// start download right away
	        mProgDialog = ProgressDialog.show(Revisions.this, getString(R.string.working), getString(R.string.downloading_file), true, false);
	        t.start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        if(mSearchQuery == null) {
            MenuInflater inflater = getMenuInflater(); //from activity
            inflater.inflate(R.menu.revisions_menu, menu);
            return true;
        }
        // if this is a search result page, don't show the menu
        return false;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.revisions_menu_refresh:
			refreshRevisions(null);
			return true;
		case R.id.revisions_menu_clear:
			deleteRevisions();
			return true;
		case R.id.revisions_menu_more_revisions:
			Integer lowestRevision = mRevisionDbHelper.getLowestRevisionForRepositoryId(mRepositoryId);
			// start at the revision below the lowest inside db, but only if at least one is inside
			if(lowestRevision != null) {
				lowestRevision -= 1;
			}
			refreshRevisions(lowestRevision);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	private void deleteRevisions() {
		mRevisionDbHelper.deleteRevisionForRepositoryId(mRepositoryId);
		mChangeDbHelper.deleteChangesForRepositoryId(mRepositoryId);
		fillData(mRevisionDbHelper.fetchRevisionsForRepositoryId(mRepositoryId));
	}

	/**
	 * refreshes the revisions for this repository
	 *
	 * @param start
	 *            if null, gets the revisions from the most recent revision and
	 *            deletes all old revisions. If not null, this will fetch new
	 *            revisions starting at the given start revision
	 */
	private void refreshRevisions(final Integer start) {
        // if an user was entered, but no password provided
		if (!UserPw.isConsistent(user, pass)) {
			// ask for the password every time
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			final EditText input = new EditText(this);
			input.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
			input.setTransformationMethod(new PasswordTransformationMethod());
			input.setWidth(inputBoxWidth);
			alert.setView(input);

			alert.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							pass = input.getText().toString();
							doRefresh(start);
						}
					});
			alert.setTitle(R.string.pass);
			alert.show();
		}
		else {
			// start the refreshing right away
			doRefresh(start);
		}
	}

	private void doRefresh(final Integer start) {
        Thread t = new Thread() {
        	@Override
          public void run() {
    			try {
    				if(start == null) {
   						String highestRemoteRev = new Propfind(url, user, pass, Boolean.valueOf(ignoreSSL)).execute().get(0).getVersion();
   						int remoteRev = Integer.valueOf(highestRemoteRev);

   						Integer highestLocalRevision = mRevisionDbHelper.getHighestRevisionForRepositoryId(mRepositoryId);
   						if(highestLocalRevision == null) {
   							highestLocalRevision = remoteRev-Integer.valueOf(limit);
   						}
   						int iLimit = remoteRev-highestLocalRevision;

   						if(iLimit==0) {
   							handler.sendEmptyMessage(Const.refreshOK);
   							return;
   						}

    					mReportList = ReportRetriever.retrieveReport(url, user, pass, Boolean.valueOf(ignoreSSL),
    							remoteRev, iLimit, Boolean.valueOf(retrieveCP));
    				}
    				else {
    					mReportList = ReportRetriever.retrieveReport(url, user, pass, Boolean.valueOf(ignoreSSL),
    							start, Integer.valueOf(limit), Boolean.valueOf(retrieveCP));
    				}
					handler.sendEmptyMessage(Const.refreshOK);
				} catch (Exception e) {
					Message m = Message.obtain();
					m.obj = e;
					handler.sendMessage(m);
				}
        	}
        };

        mProgDialog = ProgressDialog.show(this, getString(R.string.working), getString(R.string.retrieving_revisions), true, false);
        t.start();
	}

	private void fillData(Cursor repoCursor) {
	    setRepoCursor(repoCursor);
		startManagingCursor(repoCursor);

		// Create an array to specify the fields we want to display in the list
		String[] from = new String[] { RevisionDbAdapter.KEY_REVISION,
				RevisionDbAdapter.KEY_ACTION, RevisionDbAdapter.KEY_AUTHOR,
				RevisionDbAdapter.KEY_MESSAGE, RevisionDbAdapter.KEY_DATE, RevisionDbAdapter.KEY_FK_REPID };

		// and an array of the fields we want to bind those fields to
		int[] to = new int[] { R.id.revision_revision, R.id.revision_actions,
				R.id.revision_author, R.id.revision_message, R.id.revision_date, R.id.revision_fk_repo_id };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter revisions = new RevisionDataAdapter(this,
				R.layout.revision_row, repoCursor, from, to);
		setListAdapter(revisions);

	}

	private Handler handler = new Handler() {
		@Override
    public void handleMessage(Message msg) {
			if (msg.what == Const.refreshOK) {
				if(mReportList != null) {
					for (LogItem item : mReportList) {
						Set<Action> actions = new HashSet<Action>();
						if(item.getChangedPaths() != null){
							for(ChangedPath cp : item.getChangedPaths()) {
								mChangeDbHelper.createChange(mRepositoryId, Integer.valueOf(item
								.getVersion()), cp.getAction().toString(), cp.getPath());
								actions.add(cp.getAction());
							}
						}
						mRevisionDbHelper.createRevision(mRepositoryId, Integer.valueOf(item
								.getVersion()), actions.toString(), item.getAuthor(),
								item.getDate(), item.getComment());
					}
				mReportList.clear();
				}
				fillData(mRevisionDbHelper.fetchRevisionsForRepositoryId(mRepositoryId));
			} else if(msg.what == Const.downloadOK) {
				Toast.makeText(Revisions.this, getString(R.string.download_complete), Toast.LENGTH_SHORT).show();
			} else if(msg.what == Const.downloadOKActivityNotFound) {
				Toast.makeText(Revisions.this, getString(R.string.download_complete_activity_not_found), Toast.LENGTH_SHORT).show();
			} else if(msg.what == Const.downloadFileExists) {
				final FileAlreadyExistsException e = (FileAlreadyExistsException) msg.obj;
				final String changedPathUrl = e.getUrl();
				Builder builder = new AlertDialog.Builder(Revisions.this);
				builder.setTitle(R.string.file_exists).setMessage(
						getString(R.string.overwrite)+" "+e.getMessage()+" ?")
						.setPositiveButton(getString(R.string.overwrite), new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								doDownload(changedPathUrl, true);
							}
						}).setNegativeButton(getString(R.string.cancel), null).show();
			} else {
				Exception e = (Exception) msg.obj;
				MyExceptionHandler.handle(Revisions.this, e);
			}

			if(mProgDialog != null) {
              try {
                mProgDialog.dismiss();
              } catch (Exception e) {
                //
              }
			}
		}
	};

    @Override
    protected void onRestoreInstanceState(Bundle state) {
      super.onRestoreInstanceState(state);
      if(state != null) {
        mListState = state.getParcelable(LIST_STATE);
      }
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
	  if(outState != null) {
		outState.putLong(RepositoryDbAdapter.KEY_ROWID, mRepositoryId);
		outState.putString(RepositoryDbAdapter.KEY_NAME, mRepositoryName);
		outState.putString(Revisions.KEY_SEARCH_QUERY, mSearchQuery);

	    // save position
        mListState = getListView().onSaveInstanceState();
        outState.putParcelable(LIST_STATE, mListState);
	  }
	  super.onSaveInstanceState(outState);
	}

    private void setRepoCursor(Cursor newCursor) {
      if(this.repoCursor != null) {
        stopManagingCursor(this.repoCursor);
      }
      this.repoCursor = newCursor;
    }
}