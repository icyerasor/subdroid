package com.feldschmid.subdroid_donate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.feldschmid.subdroid_donate.db.RepositoryDbAdapter;
import com.feldschmid.subdroid_donate.pref.Prefs;
import com.feldschmid.subdroid_donate.util.BrowsePropAdapter;
import com.feldschmid.subdroid_donate.util.Const;
import com.feldschmid.subdroid_donate.util.DownloadFile;
import com.feldschmid.subdroid_donate.util.FileAlreadyExistsException;
import com.feldschmid.subdroid_donate.util.FileHelper;
import com.feldschmid.subdroid_donate.util.MyExceptionHandler;
import com.feldschmid.subdroid_donate.util.StringUtil;
import com.feldschmid.subdroid_donate.util.ThemeUtil;
import com.feldschmid.subdroid_donate.util.UserPw;
import com.feldschmid.svn.base.MyException;
import com.feldschmid.svn.cmd.Get;
import com.feldschmid.svn.cmd.Propfind;
import com.feldschmid.svn.model.Props;
import com.feldschmid.svn.util.PropsComparator;

public class Browse extends ListActivity {

	private final static String DIRUP = "..";

	private RepositoryDbAdapter mRepoDbHelper;
	private Long mRepositoryId;
	private String mRepositoryName;

	private static int inputBoxWidth = 200;

	private String host;
	private String path;
	private String user;
	private String ignoreSSL;
	private String pass;

	private List<Props> propList;

	private boolean openAfterDownload;
	private boolean fallbackToTextMimeType;
	private boolean backButtonNavigate;

	private ProgressDialog mProgDialog;

	private int numBack = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ThemeUtil.setTheme(this);
		setContentView(R.layout.browse);

		inputBoxWidth = this.getResources().getDimensionPixelSize(R.dimen.inputBox);

		mRepoDbHelper = new RepositoryDbAdapter(this);

		mRepoDbHelper.open();

		mRepositoryId = savedInstanceState != null ? savedInstanceState
				.getLong(RepositoryDbAdapter.KEY_ROWID) : null;
		if (mRepositoryId == null) {
			Bundle extras = getIntent().getExtras();
			mRepositoryId = extras != null ? extras
					.getLong(RepositoryDbAdapter.KEY_ROWID) : null;
		}

		mRepositoryName = savedInstanceState != null ? savedInstanceState
				.getString(RepositoryDbAdapter.KEY_NAME) : null;
		if (mRepositoryName == null) {
			Bundle extras = getIntent().getExtras();
			mRepositoryName = extras != null ? extras
					.getString(RepositoryDbAdapter.KEY_NAME) : null;
		}
		setTitle(mRepositoryName);

		path = savedInstanceState != null ? savedInstanceState
				.getString(RepositoryDbAdapter.KEY_PATH) : null;

		// enable longPress Menu
		registerForContextMenu(getListView());
	}

	@Override
	protected void onPause() {
		super.onPause();
		mRepoDbHelper.close();
	}

	@Override
	protected void onResume() {
		super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        openAfterDownload = prefs.getBoolean(Prefs.OPEN_AFTER_DOWNLOAD, true);
        fallbackToTextMimeType = prefs.getBoolean(Prefs.FALLBACK_TO_TEXT_MIMETYPE, true);
        backButtonNavigate = prefs.getBoolean(Prefs.BACK_BUTTON_NAVIGATE, true);

		mRepoDbHelper.open();

		readDataFromDb();

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
							retrieveData();
							fillData();
						}
					});
			alert.setTitle(R.string.pass);
			alert.show();
		}
		else {
			retrieveData();
			fillData();
		}
	}

	private void readDataFromDb() {
		Cursor repo = mRepoDbHelper.fetchRepository(mRepositoryId);
		host = repo.getString(repo
				.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_BASEURL));
		if(path == null) {
			path = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_PATH));
		}
		user = repo.getString(repo
				.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_USER));
		if(pass == null) {
			pass = repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_PASS));
		}
		ignoreSSL = repo.getString(repo
				.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_IGNORE_SSL));
		repo.close();
	}

	@Override
  protected void onSaveInstanceState(Bundle outState) {
		outState.putLong(RepositoryDbAdapter.KEY_ROWID, mRepositoryId);
		outState.putString(RepositoryDbAdapter.KEY_NAME, mRepositoryName);
		outState.putString(RepositoryDbAdapter.KEY_PATH, path);
		super.onSaveInstanceState(outState);
	}

	@Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && backButtonNavigate) {
			numBack--;
			if(numBack == 0) {
				Toast.makeText(Browse.this, getString(R.string.press_one_more_to_exit), Toast.LENGTH_SHORT).show();
			}
			if(numBack < 0) {
				super.onKeyDown(keyCode, event);
				return false;
			}
			navigateUp();
			retrieveData();
			fillData();
			return true;
		}
		super.onKeyDown(keyCode, event);
		return false;
	}


	private void retrieveData() {
		Propfind p = new Propfind(host+path, user, pass, Boolean.valueOf(ignoreSSL));
		// ensure to get full directory listing
		p.setDepth(1);
		try {
			propList = p.execute();
			// re-use the self props
			Props self = propList.get(0);
			self.setHref(DIRUP);
		} catch (MyException e) {
			MyExceptionHandler.handle(Browse.this, e);
			// make sure the propList gets initialized anyway!
			if(propList == null) {
				propList = new ArrayList<Props>();
			}
		}
	}

	private void fillData() {
		Collections.sort(propList, new PropsComparator());
		setListAdapter(new BrowsePropAdapter(this, R.id.browse_row_layout,
				propList));
	}

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
    	View targetView = ((AdapterContextMenuInfo) menuInfo).targetView;
		String fileName = ((TextView) targetView.findViewById(R.id.browse_name)).getText().toString();
		String revision = ((TextView) targetView.findViewById(R.id.browse_revision)).getText().toString();
		if(!fileName.equals(DIRUP)) {
			doShowDownloadOptions(revision, path+"/"+fileName, true);
		}
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		String fileName = ((TextView) v.findViewById(R.id.browse_name)).getText().toString();
		String revision = ((TextView) v.findViewById(R.id.browse_revision)).getText().toString();

		Props prop = propList.get(position);
		if(prop.isCollection()) {
          if (fileName.equals(DIRUP)) {
            navigateUp();
          } else {
            path = path + "/" + fileName;
            numBack++;
            retrieveData();
            fillData();
          }
		} else {
          doShowDownloadOptions(revision, path+"/"+fileName, false);
		}
	}

	private void navigateUp() {
		if(path.lastIndexOf('/') != -1) {
			path = path.substring(0, path.lastIndexOf('/'));
		}
		retrieveData();
		fillData();
	}

	private void doShowDownloadOptions(final String revision, final String filePath, boolean navigate) {
		String[] options;
		if(navigate) {
			options = new String[] {
					getString(R.string.download_head),
					getString(R.string.download_revision) + " "
							+ StringUtil.previousRevision(revision),
					getString(R.string.open_head),
					getString(R.string.open_revision) + " "
							+ StringUtil.previousRevision(revision),
					getString(R.string.navigate) };
		} else {
			options = new String[] {
					getString(R.string.download_head),
					getString(R.string.download_revision) + " "
							+ StringUtil.previousRevision(revision),
					getString(R.string.open_head),
					getString(R.string.open_revision) + " "
							+ StringUtil.previousRevision(revision) };
		}

		Builder builder = new AlertDialog.Builder(Browse.this);
		builder.setTitle(getString(R.string.choose_action))
				.setSingleChoiceItems(options, -1,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case 0:
									doDownload(host + filePath, false);
									break;
								case 1:
									doDownload(host	+ Const.SVN_VERSION_PART + StringUtil.previousRevision(revision) + filePath, false);
									break;
								case 2:
									doOpen(host + filePath);
									break;
								case 3:
									doOpen(host + Const.SVN_VERSION_PART + StringUtil.previousRevision(revision) + filePath);
									break;
								case 4:
									numBack++;
									path = filePath;
									retrieveData();
									fillData();
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

		// start download right away - user-pw check is already done before..
        mProgDialog = ProgressDialog.show(Browse.this, getString(R.string.working), getString(R.string.downloading_file), true, false);
        t.start();
	}

	private Handler handler = new Handler() {
		@Override
    public void handleMessage(Message msg) {
			if(msg.what == Const.downloadOK) {
				Toast.makeText(Browse.this, getString(R.string.download_complete), Toast.LENGTH_SHORT).show();
			} else if(msg.what == Const.downloadOKActivityNotFound) {
				Toast.makeText(Browse.this, getString(R.string.download_complete_activity_not_found), Toast.LENGTH_SHORT).show();
			} else if(msg.what == Const.downloadFileExists) {
				final FileAlreadyExistsException e = (FileAlreadyExistsException) msg.obj;
				final String changedPathUrl = e.getUrl();
				Builder builder = new AlertDialog.Builder(Browse.this);
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
				MyExceptionHandler.handle(Browse.this, e);
			}

			if(mProgDialog != null) {
				mProgDialog.dismiss();
			}
		}
	};

}
