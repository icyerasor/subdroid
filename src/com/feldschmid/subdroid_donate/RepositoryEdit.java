/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.feldschmid.subdroid_donate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.feldschmid.subdroid_donate.db.RepositoryDbAdapter;
import com.feldschmid.subdroid_donate.util.ThemeUtil;

public class RepositoryEdit extends Activity {

	private Long mRowId;
	private EditText mNameText;
	private EditText mURLText;
	private EditText mPathText;
	private EditText mUserText;
	private EditText mPassText;
	private Spinner  mRevisonsLimitSpinner;
	private EditText mIntervalText;
	private CheckBox mRememberPassBox;
	private CheckBox mNotificationBox;
	private CheckBox mIgnoreSSLBox;
	private CheckBox mRetrieveChangedPaths;
	private EditText mRegexAuthor;
	private EditText mRegexMessage;
	private ArrayAdapter<String> mLimitArrayAdapter;
	private RepositoryDbAdapter mDbHelper;
	private Cursor repoCursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ThemeUtil.setTheme(this);
		setContentView(R.layout.repository_edit);

		mDbHelper = new RepositoryDbAdapter(this);
		mDbHelper.open();

		mNameText = (EditText) findViewById(R.id.name);
		mURLText = (EditText) findViewById(R.id.url);
		mPathText = (EditText) findViewById(R.id.path);
		mUserText = (EditText) findViewById(R.id.user);
		mPassText = (EditText) findViewById(R.id.pass);
		mRevisonsLimitSpinner = (Spinner) findViewById(R.id.revisions_limit);
		mIntervalText = (EditText) findViewById(R.id.interval);
		mRememberPassBox = (CheckBox) findViewById(R.id.remember_pass);
		mNotificationBox = (CheckBox) findViewById(R.id.notification);
		mIgnoreSSLBox = (CheckBox) findViewById(R.id.ignoreSSL);
		mRetrieveChangedPaths = (CheckBox) findViewById(R.id.retrieveChangedPaths);
		mRegexAuthor = (EditText) findViewById(R.id.regexauthor);
		mRegexMessage = (EditText) findViewById(R.id.regexmessage);

		// adapter is only used to correctly populate the spinner
		mLimitArrayAdapter = new ArrayAdapter<String>(this, R.id.revisions_limit, getResources().getStringArray(R.array.revisions_limit_array));

		Button confirmButton = (Button) findViewById(R.id.confirm);

		mRowId = savedInstanceState != null ? savedInstanceState
				.getLong(RepositoryDbAdapter.KEY_ROWID) : null;
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras
					.getLong(RepositoryDbAdapter.KEY_ROWID) : null;
		}

		populateFields();

		confirmButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				setResult(RESULT_OK);
				finish();
			}

		});

		mIgnoreSSLBox.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mIgnoreSSLBox.isChecked()) {
					Builder builder = new AlertDialog.Builder(RepositoryEdit.this);
					builder.setTitle(R.string.confirm).
							setMessage(R.string.confirmIgnoreSSL).
							setPositiveButton("OK",	null).show();
				}
			}
		});

		mRememberPassBox.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!mRememberPassBox.isChecked()) {
					mPassText.setText("");
				}
			}
		});
	}

	private void populateFields() {
		if (mRowId != null) {
			Cursor repo = mDbHelper.fetchRepository(mRowId);
			setRepoCursor(repo);
			startManagingCursor(repo);
			mNameText.setText(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_NAME)));
			mURLText.setText(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_BASEURL)));
			mPathText.setText(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_PATH)));
			mRevisonsLimitSpinner.setSelection(mLimitArrayAdapter
							.getPosition(repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_LIMIT))));
			mUserText.setText(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_USER)));
			mPassText.setText(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_PASS)));
			mIntervalText.setText(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_INTERVAL)));
			mNotificationBox.setChecked(Boolean.valueOf(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_NOTIFICATION))));
			mIgnoreSSLBox.setChecked(Boolean.valueOf(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_IGNORE_SSL))));
			mRetrieveChangedPaths.setChecked(Boolean.valueOf(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_RETRIEVE_CHANGED_PATHS))));
			mRegexAuthor.setText(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_REGEX_AUTHOR)));
			mRegexMessage.setText(repo.getString(repo
					.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_REGEX_MESSAGE)));
			repo.close();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if(mRowId==null) {
			saveState();
		}
		outState.putLong(RepositoryDbAdapter.KEY_ROWID, mRowId);
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopManagingCursor(repoCursor);
		saveState();
		mDbHelper.close();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mDbHelper.open();
		populateFields();
	}

	private void saveState() {
		String name = mNameText.getText().toString();
		String url = mURLText.getText().toString();
		String path = mPathText.getText().toString();
		String user = mUserText.getText().toString();
		String pass = mPassText.getText().toString();
		String limit = mRevisonsLimitSpinner.getSelectedItem().toString();
		String interval = mIntervalText.getText().toString();
		String notification = String.valueOf(mNotificationBox.isChecked());
		String ignoreSSL = String.valueOf(mIgnoreSSLBox.isChecked());
		String retrieveCP = String.valueOf(mRetrieveChangedPaths.isChecked());
		String regexAuthor = mRegexAuthor.getText().toString();
		String regexMessage = mRegexMessage.getText().toString();

		if (name == null || name.equals("")) {
			name = "Please enter a Name";
		}

		// remove trailing slashes from base url
		if(url.endsWith("/")) {
			url = url.substring(0, url.length()-1);
		}
		// add trailing slash to path
		if(!path.startsWith("/")) {
			path = "/"+path;
		}
		// remove trailing slashes from path - would not be necessary..
		if(path.endsWith("/")) {
			path = path.substring(0, path.length());
		}

		if (mRowId == null) {
			long id = mDbHelper.createRepository(name, url, path, user, pass, limit,
					interval, notification, ignoreSSL, retrieveCP, regexAuthor, regexMessage);
			if (id > 0) {
				mRowId = id;
			}
		} else {
			mDbHelper.updateRepository(mRowId, name, url, path, user, pass, limit,
					interval, notification, ignoreSSL, retrieveCP, regexAuthor, regexMessage);
		}
	}

    private void setRepoCursor(Cursor newCursor) {
      if(this.repoCursor != null) {
        stopManagingCursor(this.repoCursor);
      }
      this.repoCursor = newCursor;
    }
}
